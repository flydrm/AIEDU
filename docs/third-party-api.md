# 第三方API集成文档

文档版本: v1.0  
创建日期: 2025-01-03  
作者: 架构助手  
审核人: 待定

## 1. 概述

本应用采用纯客户端架构，所有外部服务通过第三方API调用实现。主要集成：
- OpenAI兼容的LLM服务（文本生成、嵌入向量）
- CDN/OSS服务（内容下载）
- 分析与监控服务（可选）

## 2. OpenAI兼容API集成

### 2.1 服务端点
```kotlin
object ApiEndpoints {
    // 基础配置
    const val DEFAULT_BASE_URL = "https://api.openai.com/v1/"
    const val DEFAULT_TIMEOUT = 30_000L // 30秒
    
    // API路径
    const val CHAT_COMPLETIONS = "chat/completions"
    const val EMBEDDINGS = "embeddings"
    const val MODELS = "models"
}
```

### 2.2 API客户端实现
```kotlin
@Singleton
class OpenAIClient @Inject constructor(
    private val configManager: AiConfigManager,
    private val errorHandler: ApiErrorHandler
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor())
        .addInterceptor(LoggingInterceptor())
        .addInterceptor(RetryInterceptor())
        .build()
    
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(configManager.getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
    
    private val api: OpenAIApi by lazy {
        retrofit.create(OpenAIApi::class.java)
    }
    
    // 聊天完成
    suspend fun chatCompletion(
        messages: List<ChatMessage>,
        model: String = "gpt-3.5-turbo",
        temperature: Float = 0.7f,
        maxTokens: Int = 150
    ): Result<ChatCompletionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = ChatCompletionRequest(
                    model = model,
                    messages = messages,
                    temperature = temperature,
                    max_tokens = maxTokens
                )
                val response = api.chatCompletion(request)
                Result.success(response)
            } catch (e: Exception) {
                errorHandler.handleError(e)
                Result.failure(e)
            }
        }
    }
    
    // 生成嵌入向量
    suspend fun createEmbedding(
        input: List<String>,
        model: String = "text-embedding-ada-002"
    ): Result<EmbeddingResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = EmbeddingRequest(
                    model = model,
                    input = input
                )
                val response = api.createEmbedding(request)
                Result.success(response)
            } catch (e: Exception) {
                errorHandler.handleError(e)
                Result.failure(e)
            }
        }
    }
}
```

### 2.3 认证拦截器
```kotlin
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val apiKey = AiConfigManager.getApiKey()
        
        if (apiKey.isNullOrEmpty()) {
            throw IllegalStateException("API Key not configured")
        }
        
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .build()
        
        return chain.proceed(newRequest)
    }
}
```

### 2.4 重试机制
```kotlin
class RetryInterceptor : Interceptor {
    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF = 1000L // 1秒
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var exception: IOException? = null
        
        for (i in 0 until MAX_RETRIES) {
            try {
                response = chain.proceed(request)
                
                if (response.isSuccessful) {
                    return response
                }
                
                // 处理特定错误码
                when (response.code) {
                    429 -> { // Rate limit
                        val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 
                                       (INITIAL_BACKOFF * (i + 1))
                        Thread.sleep(retryAfter)
                    }
                    500, 502, 503, 504 -> { // 服务器错误
                        Thread.sleep(INITIAL_BACKOFF * (i + 1))
                    }
                    else -> return response
                }
            } catch (e: IOException) {
                exception = e
                if (i < MAX_RETRIES - 1) {
                    Thread.sleep(INITIAL_BACKOFF * (i + 1))
                }
            }
        }
        
        throw exception ?: IOException("Max retries exceeded")
    }
}
```

## 3. 内容下载服务（CDN/OSS）

### 3.1 下载管理器
```kotlin
@Singleton
class ContentDownloadManager @Inject constructor(
    private val context: Context,
    private val database: AppDatabase,
    private val fileManager: FileManager
) {
    private val downloadScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )
    
    private val activeDownloads = mutableMapOf<String, Job>()
    
    // 下载主题包
    suspend fun downloadTheme(
        themeId: String,
        downloadUrl: String,
        priority: Int = 0
    ): Flow<DownloadProgress> = flow {
        val download = Download(
            themeId = themeId,
            url = downloadUrl,
            localPath = fileManager.getThemePath(themeId),
            status = "pending",
            totalBytes = 0,
            priority = priority,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        database.downloadDao().insert(download)
        
        // 开始下载
        val job = downloadScope.launch {
            downloadFile(download) { progress ->
                emit(progress)
            }
        }
        
        activeDownloads[download.downloadId] = job
    }.flowOn(Dispatchers.IO)
    
    private suspend fun downloadFile(
        download: Download,
        onProgress: suspend (DownloadProgress) -> Unit
    ) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        try {
            // 支持断点续传
            val file = File(download.localPath)
            val downloadedBytes = if (file.exists()) file.length() else 0L
            
            val request = Request.Builder()
                .url(download.url)
                .apply {
                    if (downloadedBytes > 0) {
                        addHeader("Range", "bytes=$downloadedBytes-")
                    }
                }
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("Download failed: ${response.code}")
            }
            
            val totalBytes = response.body?.contentLength() ?: 0L
            val inputStream = response.body?.byteStream() 
                ?: throw IOException("Empty response body")
            
            // 更新总大小
            database.downloadDao().updateDownload(
                download.copy(
                    totalBytes = totalBytes + downloadedBytes,
                    status = "downloading"
                )
            )
            
            // 写入文件
            val outputStream = FileOutputStream(file, downloadedBytes > 0)
            outputStream.use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = downloadedBytes
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    
                    val progress = DownloadProgress(
                        downloadId = download.downloadId,
                        bytesDownloaded = totalRead,
                        totalBytes = totalBytes + downloadedBytes,
                        progress = totalRead.toFloat() / (totalBytes + downloadedBytes)
                    )
                    
                    onProgress(progress)
                    
                    // 更新数据库
                    database.downloadDao().updateProgress(
                        download.downloadId,
                        totalRead,
                        progress.progress
                    )
                }
            }
            
            // 验证完整性
            if (download.checksum != null) {
                val actualChecksum = fileManager.calculateMD5(file)
                if (actualChecksum != download.checksum) {
                    throw IOException("Checksum verification failed")
                }
            }
            
            // 标记完成
            database.downloadDao().updateDownload(
                download.copy(
                    status = "completed",
                    completedAt = System.currentTimeMillis()
                )
            )
            
        } catch (e: Exception) {
            // 更新失败状态
            database.downloadDao().updateDownload(
                download.copy(
                    status = "failed",
                    error = e.message
                )
            )
            throw e
        }
    }
    
    // 暂停下载
    fun pauseDownload(downloadId: String) {
        activeDownloads[downloadId]?.cancel()
        activeDownloads.remove(downloadId)
    }
    
    // 恢复下载
    suspend fun resumeDownload(downloadId: String) {
        val download = database.downloadDao().getDownload(downloadId)
        if (download != null && download.status != "completed") {
            downloadTheme(download.themeId, download.url, download.priority)
        }
    }
}
```

### 3.2 文件完整性校验
```kotlin
class FileManager {
    fun calculateMD5(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        val inputStream = FileInputStream(file)
        val buffer = ByteArray(8192)
        var bytesRead: Int
        
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            md.update(buffer, 0, bytesRead)
        }
        
        val digest = md.digest()
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    fun verifyAndExtract(zipFile: File, outputDir: File): Boolean {
        try {
            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    val entryFile = File(outputDir, entry.name)
                    
                    if (entry.isDirectory) {
                        entryFile.mkdirs()
                    } else {
                        entryFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            entryFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract zip file")
            return false
        }
    }
}
```

## 4. 应用场景实现

### 4.1 家长报告生成
```kotlin
class ParentalReportGenerator @Inject constructor(
    private val openAIClient: OpenAIClient,
    private val database: AppDatabase
) {
    suspend fun generateDailyReport(userId: String): Result<ParentalReport> {
        // 收集数据
        val todayStats = collectTodayStats(userId)
        
        // 构建prompt
        val prompt = buildReportPrompt(todayStats)
        
        // 调用LLM
        val messages = listOf(
            ChatMessage(
                role = "system",
                content = """你是一个儿童教育专家，请根据孩子今天的学习数据，
                |生成一份简短的家长报告（2-3句话）和一条建议。
                |使用温暖、积极的语气。""".trimMargin()
            ),
            ChatMessage(
                role = "user",
                content = prompt
            )
        )
        
        return openAIClient.chatCompletion(
            messages = messages,
            temperature = 0.7f,
            maxTokens = 150
        ).map { response ->
            val content = response.choices.firstOrNull()?.message?.content ?: ""
            parseReport(content)
        }
    }
    
    private suspend fun collectTodayStats(userId: String): TodayStats {
        val startOfDay = getTodayStartTime()
        val sessions = database.sessionLogDao()
            .getSessionsAfter(userId, startOfDay)
        val progress = database.progressDao()
            .getProgressAfter(userId, startOfDay)
        
        return TodayStats(
            totalTime = sessions.sumOf { it.durationMs },
            cardsCompleted = progress.count { it.isCompleted },
            themesExplored = progress.map { it.cardId }
                .mapNotNull { database.cardDao().getCard(it)?.themeId }
                .distinct()
                .size,
            interactionSuccessRate = calculateSuccessRate(progress),
            favoriteTheme = findFavoriteTheme(progress)
        )
    }
    
    private fun buildReportPrompt(stats: TodayStats): String {
        return """
        |孩子今天的学习数据：
        |- 学习时长：${stats.totalTime / 60000}分钟
        |- 完成卡片：${stats.cardsCompleted}张
        |- 探索主题：${stats.themesExplored}个
        |- 互动成功率：${(stats.interactionSuccessRate * 100).toInt()}%
        |- 最喜欢的主题：${stats.favoriteTheme}
        |
        |请生成简短的报告和建议。
        """.trimMargin()
    }
}
```

### 4.2 内容安全审核
```kotlin
class ContentModerationService @Inject constructor(
    private val openAIClient: OpenAIClient
) {
    suspend fun moderateContent(text: String): Result<ModerationResult> {
        val messages = listOf(
            ChatMessage(
                role = "system",
                content = "你是内容审核专家，判断以下内容是否适合3岁儿童。"
            ),
            ChatMessage(
                role = "user",
                content = """请审核以下内容：
                |"$text"
                |
                |回复格式：
                |适合: 是/否
                |原因: （如果不适合，说明原因）
                """.trimMargin()
            )
        )
        
        return openAIClient.chatCompletion(
            messages = messages,
            temperature = 0.3f,
            maxTokens = 100
        ).map { response ->
            parseModerationResult(response.choices.first().message.content)
        }
    }
}
```

## 5. 缓存策略

### 5.1 LLM响应缓存
```kotlin
class LLMResponseCache @Inject constructor(
    private val context: Context
) {
    private val cacheDir = File(context.cacheDir, "llm_responses")
    private val maxCacheSize = 50 * 1024 * 1024 // 50MB
    private val cache = DiskLruCache.open(cacheDir, 1, 1, maxCacheSize)
    
    suspend fun getCachedResponse(
        prompt: String,
        maxAge: Long = TimeUnit.HOURS.toMillis(6)
    ): String? {
        val key = prompt.toMD5()
        val snapshot = cache.get(key) ?: return null
        
        return snapshot.use {
            val metadata = it.getString(0)
            val (timestamp, response) = parseMetadata(metadata)
            
            if (System.currentTimeMillis() - timestamp > maxAge) {
                cache.remove(key)
                null
            } else {
                response
            }
        }
    }
    
    suspend fun cacheResponse(prompt: String, response: String) {
        val key = prompt.toMD5()
        val editor = cache.edit(key) ?: return
        
        try {
            val metadata = "${System.currentTimeMillis()}|$response"
            editor.setString(0, metadata)
            editor.commit()
        } catch (e: Exception) {
            editor.abort()
        }
    }
}
```

## 6. 错误处理

### 6.1 统一错误处理
```kotlin
sealed class ApiError : Exception() {
    data class NetworkError(override val message: String) : ApiError()
    data class RateLimitError(val retryAfter: Long) : ApiError()
    data class AuthenticationError(override val message: String) : ApiError()
    data class ServerError(val code: Int, override val message: String) : ApiError()
    data class UnknownError(override val cause: Throwable) : ApiError()
}

class ApiErrorHandler {
    fun handleError(throwable: Throwable): ApiError {
        return when (throwable) {
            is UnknownHostException -> ApiError.NetworkError("无网络连接")
            is SocketTimeoutException -> ApiError.NetworkError("请求超时")
            is HttpException -> {
                when (throwable.code()) {
                    401 -> ApiError.AuthenticationError("API密钥无效")
                    429 -> {
                        val retryAfter = throwable.response()?.headers()
                            ?.get("Retry-After")?.toLongOrNull() ?: 60
                        ApiError.RateLimitError(retryAfter)
                    }
                    in 500..599 -> ApiError.ServerError(
                        throwable.code(),
                        "服务器错误"
                    )
                    else -> ApiError.UnknownError(throwable)
                }
            }
            else -> ApiError.UnknownError(throwable)
        }
    }
}
```

### 6.2 降级策略
```kotlin
class FallbackManager {
    // LLM服务降级
    suspend fun generateReportFallback(stats: TodayStats): ParentalReport {
        // 使用本地模板生成
        val template = when {
            stats.cardsCompleted > 10 -> "今天学习很认真，完成了${stats.cardsCompleted}张卡片！"
            stats.cardsCompleted > 5 -> "今天有不错的进步，继续加油！"
            else -> "今天刚刚开始，明天继续努力！"
        }
        
        val suggestion = when (stats.interactionSuccessRate) {
            in 0.8..1.0 -> "可以尝试更有挑战的内容"
            in 0.5..0.8 -> "保持现在的学习节奏"
            else -> "可以选择更简单的内容开始"
        }
        
        return ParentalReport(
            summary = template,
            suggestion = suggestion,
            generatedAt = System.currentTimeMillis()
        )
    }
}
```

## 7. 配置管理

### 7.1 多环境配置
```kotlin
@Singleton
class AiConfigManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_PROVIDER_CHAIN = "provider_chain"
    }
    
    private val configFlow = dataStore.data.map { preferences ->
        AiConfig(
            baseUrl = preferences[stringPreferencesKey(KEY_API_BASE_URL)] 
                ?: getDefaultBaseUrl(),
            apiKey = preferences[stringPreferencesKey(KEY_API_KEY)]
                ?: getDefaultApiKey(),
            providerChain = preferences[stringPreferencesKey(KEY_PROVIDER_CHAIN)]
                ?.split(",") ?: listOf("primary")
        )
    }
    
    fun getConfig(): Flow<AiConfig> = configFlow
    
    suspend fun updateConfig(config: AiConfig) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(KEY_API_BASE_URL)] = config.baseUrl
            preferences[stringPreferencesKey(KEY_API_KEY)] = config.apiKey
            preferences[stringPreferencesKey(KEY_PROVIDER_CHAIN)] = 
                config.providerChain.joinToString(",")
        }
    }
    
    private fun getDefaultBaseUrl(): String {
        // 从BuildConfig或远程配置获取
        return BuildConfig.AI_API_BASE_URL
    }
    
    private fun getDefaultApiKey(): String {
        // 从安全存储获取
        return SecureStorage.getApiKey() ?: ""
    }
}
```

## 8. 监控与日志

### 8.1 API调用监控
```kotlin
class ApiMonitor {
    private val metrics = mutableMapOf<String, ApiMetrics>()
    
    fun recordApiCall(
        endpoint: String,
        duration: Long,
        success: Boolean,
        errorCode: Int? = null
    ) {
        val key = endpoint
        val metric = metrics.getOrPut(key) { ApiMetrics() }
        
        metric.totalCalls++
        metric.totalDuration += duration
        if (success) {
            metric.successCalls++
        } else {
            metric.failedCalls++
            errorCode?.let { metric.errorCodes[it] = (metric.errorCodes[it] ?: 0) + 1 }
        }
        
        // 定期上报
        if (metric.totalCalls % 100 == 0) {
            reportMetrics(key, metric)
        }
    }
    
    private fun reportMetrics(endpoint: String, metrics: ApiMetrics) {
        // 上报到分析服务
        Analytics.logEvent("api_metrics", bundleOf(
            "endpoint" to endpoint,
            "success_rate" to (metrics.successCalls.toFloat() / metrics.totalCalls),
            "avg_duration" to (metrics.totalDuration / metrics.totalCalls),
            "error_distribution" to metrics.errorCodes.toString()
        ))
    }
}

data class ApiMetrics(
    var totalCalls: Int = 0,
    var successCalls: Int = 0,
    var failedCalls: Int = 0,
    var totalDuration: Long = 0,
    val errorCodes: MutableMap<Int, Int> = mutableMapOf()
)
```
# AI服务配置（OpenAI兼容：apiBaseUrl + apiKey 可定制）

文档版本: v1.0  
创建日期: 2025-09-03  
作者: 架构助手  
审核人: 待定

## 1. 目标
- 在国内网络环境下稳定使用 OpenAI 兼容协议的 LLM/Embedding/Rerank/Image 服务。
- 通过可配置的 apiBaseUrl + apiKey 实现多提供方切换与故障回退（自建/代理网关优先）。

### 1.1 核心AI模型
- **主模型**: GEMINI-2.5-PRO（Google最新模型，中文能力强）
- **备用模型**: GPT-5-PRO（OpenAI旗舰模型，作为故障回退）
- **嵌入模型**: Qwen3-Embedding-8B（阿里千问，中文语义理解优秀）
- **重排模型**: BAAI/bge-reranker-v2-m3（北京人工智能研究院，中文优化）
- **图像生成**: grok-4-imageGen（最新图像生成能力）

## 2. 配置来源优先级
1) 运行时远程配置（仅内测/运维可见）  
2) 环境变量或构建参数（CI/CD 注入）  
3) 本地配置文件（assets/ai-config.json，用于线下调试）

## 3. 配置项
- apiBaseUrl: OpenAI 兼容网关 Base URL（例：`https://llm-gw.internal/`）
- apiKey: 访问令牌，HTTP 头 `Authorization: Bearer <apiKey>`
- apiOrg: 可选组织标识（若服务方要求）
- timeoutsMs: { connect, read, write }
- retries: 次数；retryBackoffMs: 退避毫秒
- routes: { chat, embeddings, rerank, images } 各接口路径
- models: { chat, embed, rerank, image } 缺省模型名
- providerChain: ["primary", "backup", ...]（地域/供应链路回退顺序）

示例（assets/ai-config.json）
```json
{
  "apiBaseUrl": "https://llm-gw.internal/",
  "apiKey": "${RUNTIME_SET}",
  "apiOrg": null,
  "timeoutsMs": { "connect": 1500, "read": 3000, "write": 3000 },
  "retries": 1,
  "retryBackoffMs": 200,
  "routes": {
    "chat": "/v1/chat/completions",
    "embeddings": "/v1/embeddings",
    "rerank": "/v1/rerank",
    "images": "/v1/images/generations"
  },
  "models": {
    "chat": "gemini-2.5-pro",
    "chat_backup": "gpt-5-pro",
    "embed": "qwen3-embedding-8b",
    "rerank": "bge-reranker-v2-m3",
    "image": "grok-4-imagegen"
  },
  "providerChain": ["primary", "backup"]
}
```

## 4. 环境变量（构建/运行时）
- AI_API_BASE_URL
- AI_API_KEY
- AI_API_ORG（可选）
- AI_API_CONNECT_TIMEOUT_MS、AI_API_READ_TIMEOUT_MS、AI_API_WRITE_TIMEOUT_MS
- AI_API_RETRIES、AI_API_RETRY_BACKOFF_MS
- AI_API_PROVIDER_CHAIN（逗号分隔）

CI 注入示例：
```bash
export AI_API_BASE_URL="https://llm-gw.internal/"
export AI_API_KEY="sk-***"
export AI_API_PROVIDER_CHAIN="primary,backup"
```

## 5. 请求示例（OpenAI兼容）
Chat：
```bash
curl "$AI_API_BASE_URL/v1/chat/completions" \
 -H "Authorization: Bearer $AI_API_KEY" -H "Content-Type: application/json" \
 -d '{
  "model":"gemini-2.5-pro",
  "temperature":0.3,
  "messages":[{"role":"user","content":"用两句话总结今天的学习"}]
}'
```

Embeddings：
```bash
curl "$AI_API_BASE_URL/v1/embeddings" \
 -H "Authorization: Bearer $AI_API_KEY" -H "Content-Type: application/json" \
 -d '{"model":"qwen3-embed","input":["红色消防车"]}'
```

## 6. Android 客户端集成要点（示例）
- Retrofit 读取 baseUrl；OkHttp 拦截器自动加 `Authorization` 头；
- DataStore 保存运行时覆盖配置；BuildConfig 存默认占位符；
- 超时/重试与 providerChain 在客户端或服务端网关实现容错。

Kotlin 伪代码：
```kotlin
data class AiConfig(
  val baseUrl: String,
  val apiKey: String,
  val timeoutsMs: Timeouts = Timeouts(1500, 3000, 3000)
)

val client = OkHttpClient.Builder()
  .connectTimeout(config.timeoutsMs.connect.toLong(), TimeUnit.MILLISECONDS)
  .readTimeout(config.timeoutsMs.read.toLong(), TimeUnit.MILLISECONDS)
  .writeTimeout(config.timeoutsMs.write.toLong(), TimeUnit.MILLISECONDS)
  .addInterceptor { chain ->
    val req = chain.request().newBuilder()
      .addHeader("Authorization", "Bearer ${config.apiKey}")
      .build()
    chain.proceed(req)
  }
  .build()

val retrofit = Retrofit.Builder()
  .baseUrl(config.baseUrl)
  .client(client)
  .addConverterFactory(MoshiConverterFactory.create())
  .build()
```

## 7. 区域化与合规
- CN 区域禁用被墙域名；优先企业自建或国内代理网关；
- 不上传语音/图像原始数据；仅上传摘要所需的聚合字段；
- 禁止在客户端硬编码生产 apiKey；使用远程配置或安全代理下发。

## 8. 故障回退与缓存
- Timeout ≤3s，失败 1 次重试；
- providerChain 顺序尝试；
- 对等 Query 结果缓存 1–6 小时；

## 9. QA 检查清单
- 修改 baseUrl 后 1 分钟内生效（重建客户端或重启 App）；
- Chat/Embedding 接口 200 成功率 ≥99%，P95 延迟达标；
- 日志不打印 apiKey/明文输入输出；崩溃无敏感信息。

## 10. 相关文档
- 需求：requirements.md（AI配置要求）
- 架构：architecture.md（AI云/路由/回退）
- 开发：development.md（环境变量与构建）
- 测试：testing.md（配置化测试）
- 发布：release.md（区域化发布清单）

# AI服务配置（OpenAI兼容：apiBaseUrl + apiKey 可定制）

文档版本: v1.0  
创建日期: 2025-09-03  
作者: 架构助手  
审核人: 待定

## 1. 目标
- 在国内网络环境下稳定使用 OpenAI 兼容协议的 LLM/Embedding/Rerank/Image 服务。
- 通过"可配置的 apiBaseUrl + apiKey"实现多提供方切换与故障回退。

## 2. 配置优先级（从高到低）
1) 运行时远程配置/隐藏开关（仅内测/运维可用）  
2) 环境变量/构建参数（CI/CD 或打包机注入）  
3) 本地配置文件（assets/ai-config.json，线下调试）

## 3. 配置项
- apiBaseUrl: OpenAI 兼容网关的 Base URL（示例：`https://llm-gw.internal/v1/` 或 `https://dashscope.openai-proxy.cn/v1/`）
- apiKey: 访问令牌，HTTP Header `Authorization: Bearer <apiKey>`
- apiOrg: 可选组织标识（如提供方要求）
- timeoutsMs: { connect, read, write }；retries: 次数；retryBackoffMs: 退避
- routes: { chat: "/v1/chat/completions", embeddings: "/v1/embeddings", rerank: "/v1/rerank", images: "/v1/images/generations" }
- models: { chat, embed, rerank, image } 缺省模型名
- providerChain: ["primary", "backup1", ...]（按地域切换，例如 CN 不包含被墙域名）

示例（assets/ai-config.json）：
```json
{
  "apiBaseUrl": "https://llm-gw.internal/",
  "apiKey": "${RUNTIME_SET}",
  "apiOrg": null,
  "timeoutsMs": { "connect": 1500, "read": 3000, "write": 3000 },
  "retries": 1,
  "retryBackoffMs": 200,
  "routes": {
    "chat": "/v1/chat/completions",
    "embeddings": "/v1/embeddings",
    "rerank": "/v1/rerank",
    "images": "/v1/images/generations"
  },
  "models": {
    "chat": "gemini-2.5-pro",
    "chat_backup": "gpt-5-pro",
    "embed": "qwen3-embedding-8b",
    "rerank": "bge-reranker-v2-m3",
    "image": "grok-4-imagegen"
  },
  "providerChain": ["primary", "backup"]
}
```

## 4. 环境变量（构建/运行时）
- AI_API_BASE_URL
- AI_API_KEY
- AI_API_ORG（可选）
- AI_API_TIMEOUT_MS、AI_API_CONNECT_TIMEOUT_MS、AI_API_RETRIES
- AI_API_PROVIDER_CHAIN（逗号分隔）

CI 示例：
```bash
export AI_API_BASE_URL="https://llm-gw.internal/"
export AI_API_KEY="sk-***"
export AI_API_PROVIDER_CHAIN="primary,backup"
```

## 5. 请求示例（OpenAI兼容）
- Chat
```bash
curl "$AI_API_BASE_URL/v1/chat/completions" \
 -H "Authorization: Bearer $AI_API_KEY" -H "Content-Type: application/json" \
 -d '{"model":"gemini-2.5-pro","messages":[{"role":"user","content":"总结学习进度"}]}'
```

- Embeddings
```bash
curl "$AI_API_BASE_URL/v1/embeddings" \
 -H "Authorization: Bearer $AI_API_KEY" -H "Content-Type: application/json" \
 -d '{"model":"qwen3-embed","input":["红色消防车"]}'
```

## 6. 客户端实现示例
```kotlin
@Singleton
class AIServiceClient @Inject constructor(
    private val configManager: AiConfigManager
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(configManager.getConnectTimeout(), TimeUnit.MILLISECONDS)
        .readTimeout(configManager.getReadTimeout(), TimeUnit.MILLISECONDS)
        .addInterceptor(AuthInterceptor(configManager))
        .addInterceptor(RetryInterceptor(configManager))
        .build()
        
    private val retrofit = Retrofit.Builder()
        .baseUrl(configManager.getBaseUrl())
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        
    private val api = retrofit.create(OpenAIApi::class.java)
    
    suspend fun chatCompletion(request: ChatRequest): ChatResponse {
        return withContext(Dispatchers.IO) {
            api.chatCompletion(request)
        }
    }
    
    suspend fun createEmbedding(request: EmbeddingRequest): EmbeddingResponse {
        return withContext(Dispatchers.IO) {
            api.createEmbedding(request)
        }
    }
}

class AuthInterceptor(private val config: AiConfigManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${config.getApiKey()}")
            .build()
        return chain.proceed(request)
    }
}
```

## 7. 配置管理器
```kotlin
@Singleton
class AiConfigManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfig: RemoteConfigService
) {
    private var config: AiConfig = loadDefaultConfig()
    
    init {
        // 优先级1：远程配置
        remoteConfig.getAiConfig()?.let { config = it }
        
        // 优先级2：环境变量
        System.getenv("AI_API_BASE_URL")?.let { 
            config = config.copy(baseUrl = it)
        }
        System.getenv("AI_API_KEY")?.let {
            config = config.copy(apiKey = it)
        }
        
        // 优先级3：本地配置文件
        loadLocalConfig()?.let { localConfig ->
            if (config.apiKey == "\${RUNTIME_SET}") {
                config = localConfig
            }
        }
    }
    
    private fun loadLocalConfig(): AiConfig? {
        return try {
            context.assets.open("ai-config.json").use { stream ->
                Gson().fromJson(stream.reader(), AiConfig::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun getBaseUrl() = config.baseUrl
    fun getApiKey() = config.apiKey
    fun getConnectTimeout() = config.timeoutsMs.connect
    fun getReadTimeout() = config.timeoutsMs.read
}
```

## 8. 故障处理与降级
```kotlin
class AIServiceWithFallback @Inject constructor(
    private val primaryClient: AIServiceClient,
    private val cache: ResponseCache
) {
    suspend fun generateContent(prompt: String): String {
        // 1. 尝试从缓存获取
        cache.get(prompt)?.let { return it }
        
        // 2. 调用AI服务
        return try {
            val response = primaryClient.chatCompletion(
                ChatRequest(
                    model = "gemini-2.5-pro",
                    messages = listOf(Message("user", prompt)),
                    temperature = 0.7f
                )
            )
            val content = response.choices.first().message.content
            cache.put(prompt, content)
            content
        } catch (e: Exception) {
            // 3. 降级到预设内容
            getPresetContent(prompt)
        }
    }
}
```

## 9. 监控与日志
```kotlin
class AIMetricsCollector {
    fun recordApiCall(
        endpoint: String,
        success: Boolean,
        latency: Long,
        error: String? = null
    ) {
        // 记录但不包含敏感信息
        val event = mapOf(
            "endpoint" to endpoint,
            "success" to success,
            "latency" to latency,
            "error_type" to error?.substringBefore(":")
        )
        Analytics.logEvent("ai_api_call", event)
    }
}
```

## 10. 模型能力与应用场景

### 10.1 GEMINI-2.5-PRO / GPT-5-PRO（内容生成）
```kotlin
// 生成教育内容
suspend fun generateEducationalContent(topic: String): String {
    val prompt = """
    为3岁儿童创作关于"$topic"的教育内容：
    - 语言简单，句子简短
    - 包含互动元素
    - 时长15-20秒朗读
    - 融入"勇敢"或"逻辑"主题
    """
    
    return aiClient.chatCompletion(
        model = "gemini-2.5-pro",  // 主模型
        fallbackModel = "gpt-5-pro",  // 备用
        messages = listOf(Message("user", prompt)),
        temperature = 0.7f
    )
}

// 生成个性化学习建议
suspend fun generateLearningAdvice(progress: LearningProgress): String {
    val prompt = """
    基于以下学习数据生成家长建议（2-3句）：
    - 完成卡片：${progress.completedCards}
    - 学习主题：${progress.topics}
    - 偏好颜色：红色
    - 互动成功率：${progress.successRate}%
    """
    
    return aiClient.chatCompletion(model = "gemini-2.5-pro", ...)
}
```

### 10.2 Qwen3-Embedding-8B（语义理解）
```kotlin
// 内容相似度计算
suspend fun findSimilarContent(query: String, contents: List<String>): List<Pair<String, Float>> {
    // 生成查询向量
    val queryEmbedding = aiClient.createEmbedding(
        model = "qwen3-embedding-8b",
        input = query
    )
    
    // 批量生成内容向量
    val contentEmbeddings = aiClient.createEmbedding(
        model = "qwen3-embedding-8b",
        input = contents
    )
    
    // 计算余弦相似度
    return contents.zip(contentEmbeddings).map { (content, embedding) ->
        content to cosineSimilarity(queryEmbedding, embedding)
    }.sortedByDescending { it.second }
}

// 理解孩子的语音意图
suspend fun understandIntent(transcript: String): Intent {
    val embedding = aiClient.createEmbedding(
        model = "qwen3-embedding-8b",
        input = transcript
    )
    
    // 与预定义意图比较
    return matchIntent(embedding)
}
```

### 10.3 BAAI/bge-reranker-v2-m3（精准排序）
```kotlin
// 重排推荐内容
suspend fun rerankContent(query: String, candidates: List<Card>): List<Card> {
    val reranked = aiClient.rerank(
        model = "bge-reranker-v2-m3",
        query = query,
        documents = candidates.map { it.description },
        topK = 10
    )
    
    return reranked.results.map { result ->
        candidates[result.index]
    }
}

// 优化搜索结果
suspend fun optimizeSearchResults(
    userPreference: String,
    searchResults: List<Content>
): List<Content> {
    // 根据用户偏好（如"红色"、"勇敢"）重排内容
    return aiClient.rerank(
        model = "bge-reranker-v2-m3",
        query = userPreference,
        documents = searchResults.map { it.summary }
    ).sortedResults()
}
```

### 10.4 grok-4-imageGen（创意图像）
```kotlin
// 生成教育图片
suspend fun generateEducationalImage(concept: String): ByteArray? {
    val prompt = """
    儿童教育插图：
    - 主题：$concept
    - 风格：卡通、温暖、适合3岁儿童
    - 色彩：明亮，突出红色元素
    - 要求：简洁、安全、正面
    """
    
    val response = aiClient.generateImage(
        model = "grok-4-imagegen",
        prompt = prompt,
        size = "1024x1024",
        quality = "standard",
        style = "cartoon"
    )
    
    return response.imageData
}

// 生成奖励贴纸
suspend fun generateRewardSticker(achievement: String): ByteArray? {
    return aiClient.generateImage(
        model = "grok-4-imagegen",
        prompt = "可爱的奖励贴纸：$achievement，红色星星主题，适合儿童",
        size = "512x512"
    )
}
```

## 11. 安全最佳实践
1. **密钥管理**
   - 禁止硬编码 apiKey
   - 使用加密存储或安全配置服务
   - 定期轮换密钥

2. **数据隐私**
   - 不上传个人身份信息
   - 敏感内容本地脱敏
   - 遵守COPPA（儿童隐私保护）

3. **内容安全**
   - 输出内容过滤
   - 长度限制
   - 主题适龄性检查
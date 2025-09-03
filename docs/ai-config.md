# AI服务配置（OpenAI兼容：apiBaseUrl + apiKey 可定制）

文档版本: v1.0  
创建日期: 2025-09-03  
作者: 架构助手  
审核人: 待定

## 1. 目标
- 在国内网络环境下稳定使用 OpenAI 兼容协议的 LLM/Embedding/Rerank/Image 服务。
- 通过可配置的 apiBaseUrl + apiKey 实现多提供方切换与故障回退（自建/代理网关优先）。

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
    "embed": "qwen3-embed",
    "rerank": "baai-reranker",
    "image": "grok-4-image"
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
    "embed": "qwen3-embed",
    "rerank": "baai-reranker",
    "image": "grok-4-image"
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

## 10. 安全最佳实践
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
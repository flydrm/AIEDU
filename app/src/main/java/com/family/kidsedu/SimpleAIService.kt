package com.family.kidsedu

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 简化版AI服务
 * 
 * 功能说明：
 * 1. 调用AI接口生成教育内容
 * 2. 生成个性化鼓励语
 * 3. 文本转语音（TTS）
 * 4. 内容审核和过滤
 * 
 * 实现原理：
 * - 使用OpenAI兼容的API接口
 * - 支持多种AI模型（GEMINI、GPT等）
 * - 本地缓存减少API调用
 * - 失败时使用默认内容
 * 
 * 注意事项：
 * - API密钥需要保密，不要提交到代码库
 * - 网络请求需要在协程中执行
 * - 考虑API调用成本，使用缓存
 * 
 * @author AI启蒙时光开发团队
 * @since 2025-01-03
 */
class SimpleAIService(private val context: Context) {
    
    companion object {
        private const val TAG = "SimpleAIService"
        
        /** API请求超时时间（秒） */
        private const val API_TIMEOUT_SECONDS = 10L
        
        /** 最大重试次数 */
        private const val MAX_RETRY_COUNT = 3
        
        /** 缓存有效期（天） */
        private const val CACHE_VALID_DAYS = 7
        
        /** OpenAI兼容API的基础URL */
        private const val API_BASE_URL = "https://api.openai.com/v1/"
        
        /** 默认使用的模型 */
        private const val DEFAULT_MODEL = "gpt-3.5-turbo"
    }
    
    /** HTTP客户端 */
    private val client = OkHttpClient.Builder()
        .connectTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    /** JSON解析器 */
    private val gson = Gson()
    
    /** API配置（从配置文件读取） */
    private val apiConfig: ApiConfig by lazy {
        loadApiConfig()
    }
    
    /**
     * 生成教育内容
     * 
     * @param topic 教育主题，如"红色消防车"、"数字1"等
     * @param age 目标年龄，默认3岁
     * @return 生成的教育内容，失败返回null
     */
    suspend fun generateContent(topic: String, age: Int = 3): String? {
        return withContext(Dispatchers.IO) {
            try {
                // 先检查缓存
                val cached = loadFromCache("content_$topic")
                if (cached != null) {
                    Log.d(TAG, "使用缓存内容: $topic")
                    return@withContext cached
                }
                
                // 构建提示词
                val prompt = buildContentPrompt(topic, age)
                
                // 调用AI接口
                val content = callChatAPI(prompt)
                
                // 内容审核
                if (content != null && isContentSafe(content)) {
                    // 保存到缓存
                    saveToCache("content_$topic", content)
                    return@withContext content
                }
                
                // 内容不安全或生成失败，使用默认内容
                getDefaultContent(topic)
                
            } catch (e: Exception) {
                Log.e(TAG, "生成内容失败: $topic", e)
                getDefaultContent(topic)
            }
        }
    }
    
    /**
     * 生成鼓励语
     * 
     * @param progress 当前进度百分比（0-100）
     * @return 鼓励语
     */
    suspend fun generateEncouragement(progress: Int = 50): String {
        return withContext(Dispatchers.IO) {
            try {
                // 简单实现：根据进度返回不同的鼓励语
                // 也可以调用AI生成更丰富的内容
                when {
                    progress < 30 -> listOf("继续加油！", "你可以的！", "慢慢来，不着急！").random()
                    progress < 60 -> listOf("真棒！继续努力！", "做得很好！", "你真聪明！").random()
                    progress < 90 -> listOf("太厉害了！", "快完成了！", "你是最棒的！").random()
                    else -> listOf("恭喜你！全部完成！", "你真的太棒了！", "完美！").random()
                }
            } catch (e: Exception) {
                Log.e(TAG, "生成鼓励语失败", e)
                "你真棒！"
            }
        }
    }
    
    /**
     * 文本转语音（TTS）
     * 
     * @param text 要转换的文本
     * @param voice 语音类型，默认使用童声
     * @return 音频数据，失败返回null
     */
    suspend fun textToSpeech(text: String, voice: String = "child"): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                // TODO: 实现TTS功能
                // 可以使用：
                // 1. 百度语音合成API
                // 2. 讯飞语音合成API
                // 3. Google Cloud TTS
                // 4. Azure Cognitive Services
                
                Log.d(TAG, "TTS功能暂未实现: $text")
                null
                
            } catch (e: Exception) {
                Log.e(TAG, "TTS失败", e)
                null
            }
        }
    }
    
    /**
     * 构建生成教育内容的提示词
     */
    private fun buildContentPrompt(topic: String, age: Int): String {
        return """
        请为${age}岁的孩子创作关于"${topic}"的教育内容。
        
        要求：
        1. 语言简单易懂，每句话不超过10个字
        2. 内容积极正面，充满童趣
        3. 包含一个简单的知识点
        4. 结尾有一个互动问题
        5. 总字数控制在30-50字
        6. 不要包含任何不适合儿童的内容
        
        格式示例：
        这是红色消防车。
        它会喷水灭火。
        消防员叔叔很勇敢。
        你见过消防车吗？
        """.trimIndent()
    }
    
    /**
     * 调用Chat API（OpenAI兼容接口）
     */
    private fun callChatAPI(prompt: String): String? {
        val requestBody = ChatRequest(
            model = apiConfig.model ?: DEFAULT_MODEL,
            messages = listOf(
                Message(role = "system", content = "你是一个专业的儿童教育内容创作者。"),
                Message(role = "user", content = prompt)
            ),
            temperature = 0.7,
            maxTokens = 150
        )
        
        val json = gson.toJson(requestBody)
        val body = json.toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("${apiConfig.baseUrl}chat/completions")
            .addHeader("Authorization", "Bearer ${apiConfig.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
        
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
                    chatResponse.choices.firstOrNull()?.message?.content
                } else {
                    Log.e(TAG, "API请求失败: ${response.code}")
                    null
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "网络请求失败", e)
            null
        }
    }
    
    /**
     * 内容安全检查
     * 
     * @param content 要检查的内容
     * @return 是否安全
     */
    private fun isContentSafe(content: String): Boolean {
        // 敏感词列表（简化版）
        val sensitiveWords = listOf(
            "暴力", "恐怖", "死亡", "血腥", "武器",
            "打架", "杀", "炸弹", "战争", "受伤"
        )
        
        // 检查是否包含敏感词
        if (sensitiveWords.any { content.contains(it) }) {
            Log.w(TAG, "内容包含敏感词: $content")
            return false
        }
        
        // 检查长度是否合适
        if (content.length > 100) {
            Log.w(TAG, "内容过长: ${content.length}")
            return false
        }
        
        return true
    }
    
    /**
     * 获取默认内容
     */
    private fun getDefaultContent(topic: String): String {
        // 预定义的安全内容
        val defaultContents = mapOf(
            "红色消防车" to "这是红色消防车。它会救火。消防员很勇敢。你喜欢红色吗？",
            "数字1" to "这是数字1。像一根棍子。我们有1个太阳。你会数到1吗？",
            "勇敢的小狮子" to "小狮子很勇敢。它是森林之王。狮子会吼叫。你能学狮子叫吗？",
            "颜色认知" to "世界有很多颜色。红黄蓝绿都很美。彩虹有七种颜色。你最喜欢什么颜色？",
            "动物朋友" to "动物是我们的朋友。小狗会汪汪叫。小猫喜欢喝牛奶。你喜欢什么动物？"
        )
        
        return defaultContents[topic] ?: "这个很有趣。让我们一起学习。你觉得怎么样？"
    }
    
    /**
     * 加载API配置
     */
    private fun loadApiConfig(): ApiConfig {
        return try {
            // 从assets目录读取配置文件
            // 注意：实际项目中API密钥应该更安全地存储
            context.assets.open("ai_config.json").use { stream ->
                gson.fromJson(stream.reader(), ApiConfig::class.java)
            }
        } catch (e: Exception) {
            Log.w(TAG, "加载API配置失败，使用默认配置", e)
            // 返回默认配置
            ApiConfig(
                apiKey = "YOUR_API_KEY_HERE",
                baseUrl = API_BASE_URL,
                model = DEFAULT_MODEL
            )
        }
    }
    
    /**
     * 从缓存加载内容
     */
    private fun loadFromCache(key: String): String? {
        // TODO: 实现缓存功能
        // 可以使用SharedPreferences或文件存储
        return null
    }
    
    /**
     * 保存内容到缓存
     */
    private fun saveToCache(key: String, content: String) {
        // TODO: 实现缓存功能
        // 保存时记录时间戳，用于判断是否过期
    }
    
    /**
     * API配置数据类
     */
    data class ApiConfig(
        @SerializedName("api_key")
        val apiKey: String,
        
        @SerializedName("base_url")
        val baseUrl: String = API_BASE_URL,
        
        @SerializedName("model")
        val model: String = DEFAULT_MODEL,
        
        @SerializedName("api_secret")
        val apiSecret: String? = null
    )
    
    /**
     * Chat请求数据类
     */
    data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double = 0.7,
        
        @SerializedName("max_tokens")
        val maxTokens: Int = 150
    )
    
    /**
     * 消息数据类
     */
    data class Message(
        val role: String,
        val content: String
    )
    
    /**
     * Chat响应数据类
     */
    data class ChatResponse(
        val choices: List<Choice>
    )
    
    /**
     * 选择项数据类
     */
    data class Choice(
        val message: Message,
        val index: Int,
        
        @SerializedName("finish_reason")
        val finishReason: String?
    )
}
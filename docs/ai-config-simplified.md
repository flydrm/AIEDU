# AI服务配置指南（精简版）

文档版本: v1.0  
创建日期: 2025-01-03  
作者: 架构助手

## 1. 为什么需要AI

在这个为3岁男孩设计的教育应用中，AI可以提供：
- **智能内容生成**：根据孩子的兴趣动态生成故事
- **语音合成**：将文字转换为自然的童声朗读
- **学习建议**：基于学习进度生成个性化建议
- **内容理解**：通过AI理解孩子的互动，提供更好的反馈

## 2. 使用哪些AI服务

### 2.1 核心AI能力
```
必需的：
1. TTS（文字转语音）- 生成自然的童声朗读
2. LLM（大语言模型）- 生成教育内容和学习建议

可选的：
3. 语音识别 - 识别孩子的简单语音命令
4. 图像理解 - 分析孩子的涂鸦或选择
```

### 2.2 推荐服务商
```
国内可用方案：
1. 百度文心一言 - 稳定，有儿童内容安全过滤
2. 阿里通义千问 - API简单，价格合理
3. 讯飞开放平台 - TTS效果好，有童声选项
4. Azure OpenAI (中国区) - 如果有企业账号

国外方案（需要代理）：
1. OpenAI GPT-3.5/4 - 效果最好
2. Google Gemini - 免费额度较多
```

## 3. 简化的集成方案

### 3.1 配置文件
```kotlin
// 在 assets/ai_config.json
{
  "provider": "baidu",  // 或 "aliyun", "openai"
  "apiKey": "${从环境变量读取}",
  "apiSecret": "${从环境变量读取}",
  "endpoints": {
    "tts": "https://aip.baidubce.com/rpc/2.0/tts/v1/synthesis",
    "chat": "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions"
  },
  "settings": {
    "voice": "child_female",  // 童声
    "speed": 0.8,  // 语速慢一点
    "temperature": 0.7  // 创造性适中
  }
}
```

### 3.2 简单的AI服务类
```kotlin
class SimpleAIService(private val context: Context) {
    
    private val config by lazy { loadConfig() }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()
    
    // 文字转语音
    suspend fun textToSpeech(text: String): ByteArray? {
        return try {
            val request = Request.Builder()
                .url(config.endpoints.tts)
                .post(createTTSRequestBody(text))
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.bytes()
            } else null
        } catch (e: Exception) {
            Log.e("AI", "TTS failed", e)
            null
        }
    }
    
    // 生成学习内容
    suspend fun generateContent(
        topic: String, 
        difficulty: String = "3岁"
    ): String? {
        val prompt = """
        为${difficulty}的孩子生成一个关于"${topic}"的简短教育内容：
        1. 时长15-20秒的朗读文本
        2. 使用简单词汇
        3. 包含一个互动问题
        4. 语气温暖友好
        """.trimIndent()
        
        return try {
            // 调用LLM API
            val response = callLLM(prompt)
            response?.content
        } catch (e: Exception) {
            Log.e("AI", "Content generation failed", e)
            null  // 失败时使用预设内容
        }
    }
    
    // 生成学习建议（给家长）
    suspend fun generateSuggestion(
        progress: LearningProgress
    ): String {
        val prompt = """
        孩子今天学习了${progress.cardsCompleted}张卡片，
        主题包括：${progress.topics.joinToString("、")}，
        请用1-2句话给出学习建议。
        """.trimIndent()
        
        return callLLM(prompt)?.content 
            ?: "继续保持，明天可以尝试新的内容！"
    }
}
```

## 4. 在MVP中的应用

### 4.1 内容生成助手
```kotlin
// 开发时使用，批量生成内容
class ContentGenerator {
    suspend fun generateCardContent(theme: String): List<Card> {
        val topics = when(theme) {
            "勇敢" -> listOf("消防员", "警察", "医生", "小狮子")
            "数字" -> listOf("1", "2", "3", "4", "5")
            "颜色" -> listOf("红色", "蓝色", "黄色", "绿色")
            else -> emptyList()
        }
        
        return topics.map { topic ->
            val content = aiService.generateContent(topic)
            val audio = aiService.textToSpeech(content)
            // 保存生成的内容
            Card(
                title = topic,
                text = content,
                audioPath = saveAudio(audio)
            )
        }
    }
}
```

### 4.2 运行时AI增强
```kotlin
// 在MainActivity中的应用
class MainActivity : AppCompatActivity() {
    
    private val aiService by lazy { SimpleAIService(this) }
    
    // 动态生成鼓励语
    private suspend fun generateEncouragement(): String {
        return aiService.generateContent(
            topic = "鼓励完成学习", 
            difficulty = "3岁"
        ) ?: "你真棒！"
    }
    
    // 每日学习总结
    private suspend fun generateDailySummary() {
        val progress = getLearningProgress()
        val suggestion = aiService.generateSuggestion(progress)
        
        // 显示给家长
        showDialog("今日学习建议", suggestion)
    }
}
```

## 5. 成本控制

### 5.1 API调用优化
```kotlin
object AICache {
    private val cache = LruCache<String, String>(50)
    
    fun getCached(key: String): String? = cache.get(key)
    
    fun cache(key: String, value: String) {
        cache.put(key, value)
    }
}

// 使用缓存减少API调用
suspend fun getContent(topic: String): String {
    return AICache.getCached(topic) 
        ?: aiService.generateContent(topic)?.also {
            AICache.cache(topic, it)
        }
        ?: getDefaultContent(topic)
}
```

### 5.2 费用估算
```
按每天使用30分钟计算：
- TTS: 约20-30次调用 ≈ ¥0.5
- LLM: 约5-10次调用 ≈ ¥0.2
- 月成本: 约¥20-30

建议：
1. 开发时批量生成内容，减少运行时调用
2. 使用缓存避免重复生成
3. 设置每日调用上限
```

## 6. 隐私和安全

### 6.1 数据处理原则
- **不上传**：孩子的语音、照片等个人数据
- **匿名化**：学习进度数据去除个人信息
- **本地优先**：能本地处理的不调用API

### 6.2 内容安全
```kotlin
// 内容过滤
fun filterContent(text: String): String {
    // 基础敏感词过滤
    val filtered = text.replace(sensitiveWords, "***")
    
    // 确保内容适合儿童
    return if (isChildFriendly(filtered)) filtered 
           else getDefaultSafeContent()
}
```

## 7. 快速开始

### 7.1 最小化集成（1小时）
1. 注册百度AI开放平台
2. 获取API Key
3. 复制上面的SimpleAIService类
4. 在需要的地方调用

### 7.2 测试代码
```kotlin
// 测试TTS
lifecycleScope.launch {
    val audio = aiService.textToSpeech("小朋友你好")
    audio?.let { playAudio(it) }
}

// 测试内容生成
lifecycleScope.launch {
    val content = aiService.generateContent("红色消防车")
    Log.d("AI", "Generated: $content")
}
```

## 8. 故障处理

### 8.1 降级方案
```kotlin
class AIServiceWithFallback {
    suspend fun getContent(topic: String): String {
        return try {
            // 优先使用AI
            aiService.generateContent(topic) 
        } catch (e: Exception) {
            // 降级到预设内容
            getPresetContent(topic)
        } ?: getDefaultContent(topic)
    }
}
```

### 8.2 离线模式
- 预生成常用内容
- 本地存储生成结果
- 有网时更新内容库

## 9. 未来扩展

### 9.1 短期（1个月）
- 根据孩子反应调整语音语速
- 记住孩子的喜好，个性化内容

### 9.2 中期（3个月）
- 简单的语音识别（是/不是）
- 根据学习进度动态调整难度

### 9.3 长期（6个月）
- 生成个性化故事
- 智能学习路径规划

## 总结

AI不是为了炫技，而是真正提升教育价值：
1. **更自然的交互**：童声朗读比机械音更亲切
2. **更丰富的内容**：可以持续生成新内容
3. **更个性化的体验**：根据孩子特点调整
4. **更省心的维护**：自动生成比手工制作高效

记住：AI是工具，教育是目的，孩子的快乐学习是核心！
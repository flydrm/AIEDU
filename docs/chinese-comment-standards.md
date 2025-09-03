# 中文注释规范

文档版本: v1.0  
创建日期: 2025-01-03  
作者: 架构助手  
重要性: 🔴 最高优先级

> **核心原则：代码注释是必需品，不是奢侈品！**

## 1. 为什么必须写中文注释

### 1.1 提高开发效率
- 降低理解成本：中文注释让代码意图一目了然
- 减少沟通时间：团队成员快速理解业务逻辑
- 加速问题定位：出问题时能快速找到关键代码

### 1.2 保证代码质量
- 强制思考：写注释时会重新审视代码逻辑
- 知识传承：新人能快速上手项目
- 减少bug：清晰的注释减少理解偏差

### 1.3 本项目特殊性
- **教育类应用**：业务逻辑需要详细说明
- **AI功能集成**：复杂的AI调用需要解释
- **儿童安全**：安全相关代码必须说明原因

## 2. 注释规范详解

### 2.1 类注释（必须）

```kotlin
/**
 * 教育卡片播放器
 * 
 * 功能说明：
 * 1. 管理卡片内容的播放流程
 * 2. 处理用户交互事件
 * 3. 记录学习进度
 * 4. 生成学习报告
 * 
 * 使用场景：
 * - 用户点击卡片后进入播放页面
 * - 支持音频播放、动画展示、互动反馈
 * 
 * 注意事项：
 * - 需要在Activity的onDestroy中调用release()释放资源
 * - 播放器同时只能播放一个内容
 * 
 * @author 张三
 * @since 2025-01-03
 */
class CardPlayer {
    // 实现代码...
}
```

### 2.2 方法注释（复杂方法必须）

```kotlin
/**
 * 播放教育卡片内容
 * 
 * 功能流程：
 * 1. 检查上一个播放任务是否完成
 * 2. 加载卡片资源（图片、音频）
 * 3. 显示卡片动画
 * 4. 播放音频内容
 * 5. 等待用户互动
 * 6. 记录完成状态
 * 
 * @param card 要播放的卡片对象，不能为null
 * @param onComplete 播放完成的回调，可选参数
 * @param options 播放选项，如自动播放、循环次数等
 * 
 * @return 播放任务ID，用于后续控制
 * 
 * @throws IllegalStateException 当播放器未初始化时抛出
 * @throws ResourceNotFoundException 当卡片资源不存在时抛出
 * 
 * 示例：
 * ```
 * val taskId = player.play(card) {
 *     // 播放完成后的处理
 *     showNextCard()
 * }
 * ```
 */
fun play(
    card: Card,
    onComplete: (() -> Unit)? = null,
    options: PlayOptions = PlayOptions.default()
): String {
    // 实现代码...
}
```

### 2.3 业务逻辑注释（必须）

```kotlin
fun processLearningProgress(cards: List<Card>) {
    // 1. 筛选今日学习的卡片
    val todayCards = cards.filter { card ->
        // 判断条件：最后学习时间是今天
        card.lastLearnTime?.isToday() == true
    }
    
    // 2. 计算学习进度
    val progress = todayCards.count { it.isCompleted } / todayCards.size.toFloat()
    
    // 3. 生成鼓励语（基于进度）
    val encouragement = when {
        progress < 0.3 -> "继续加油，你可以的！"
        progress < 0.7 -> "真棒！已经完成一半了！"
        progress < 1.0 -> "太厉害了！马上就要完成了！"
        else -> "恭喜你！今天的任务全部完成！"
    }
    
    // 4. 更新UI显示
    updateProgressUI(progress, encouragement)
    
    // 5. 如果全部完成，播放奖励动画
    if (progress >= 1.0) {
        playRewardAnimation()
    }
}
```

### 2.4 交互逻辑注释（必须）

```kotlin
/**
 * 设置卡片点击事件
 * 交互流程：用户点击 -> 缩放动画 -> 播放音效 -> 进入详情
 */
private fun setupCardClickListener() {
    cardView.setOnClickListener { view ->
        // 1. 播放点击动画（缩小再放大）
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                // 2. 恢复原始大小
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
        
        // 3. 播放点击音效
        soundPool.play(clickSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
        
        // 4. 延迟300ms后跳转（等待动画完成）
        view.postDelayed({
            // 5. 跳转到卡片详情页
            navigateToCardDetail(card)
        }, 300)
    }
}
```

### 2.5 算法注释（必须详细）

```kotlin
/**
 * 计算下一张推荐卡片
 * 
 * 算法说明：
 * 基于艾宾浩斯遗忘曲线，结合用户偏好推荐下一张学习卡片
 * 
 * 权重分配：
 * - 遗忘程度：40%（距离上次学习的时间）
 * - 用户偏好：30%（颜色、主题偏好）
 * - 学习进度：20%（未学 > 学习中 > 已完成）
 * - 随机因素：10%（避免推荐过于单一）
 */
fun recommendNextCard(cards: List<Card>, userPreference: UserPreference): Card? {
    return cards
        .filter { !it.isCompleted } // 只推荐未完成的
        .map { card ->
            // 计算遗忘分数（时间越久分数越高）
            val forgetScore = calculateForgetScore(card.lastLearnTime) * 0.4
            
            // 计算偏好分数（匹配度越高分数越高）
            val preferenceScore = calculatePreferenceScore(card, userPreference) * 0.3
            
            // 计算进度分数（未学习的分数最高）
            val progressScore = when (card.progress) {
                0 -> 1.0    // 未学习
                in 1..99 -> 0.5  // 学习中
                else -> 0.1      // 已完成（不应该出现）
            } * 0.2
            
            // 添加随机因素
            val randomScore = Random.nextFloat() * 0.1
            
            // 总分 = 各项分数之和
            val totalScore = forgetScore + preferenceScore + progressScore + randomScore
            
            card to totalScore
        }
        .sortedByDescending { it.second } // 按分数降序
        .firstOrNull()?.first // 返回分数最高的卡片
}

/**
 * 计算遗忘分数
 * 公式：f(t) = e^(-t/S)，其中t是天数，S是稳定性参数
 */
private fun calculateForgetScore(lastLearnTime: Long?): Float {
    if (lastLearnTime == null) return 1.0f // 从未学习，返回最高分
    
    val daysSinceLearn = (System.currentTimeMillis() - lastLearnTime) / (24 * 60 * 60 * 1000)
    val stabilityParameter = 1.2f // 稳定性参数，可调整
    
    return exp(-daysSinceLearn / stabilityParameter).toFloat()
}
```

### 2.6 异常处理注释（必须说明原因）

```kotlin
fun loadCardResources(card: Card) {
    try {
        // 加载图片资源
        val image = loadImage(card.imageUrl)
        
        // 加载音频资源
        val audio = loadAudio(card.audioUrl)
        
    } catch (e: NetworkException) {
        // 网络异常：可能是没网或服务器问题
        // 处理方案：使用本地缓存资源
        Log.e(TAG, "网络加载失败，尝试使用缓存", e)
        loadFromCache(card)
        
    } catch (e: OutOfMemoryError) {
        // 内存不足：图片太大或内存泄漏
        // 处理方案：降低图片质量并清理内存
        Log.e(TAG, "内存不足，降低图片质量", e)
        System.gc() // 建议GC
        loadLowQualityImage(card)
        
    } catch (e: Exception) {
        // 其他未知异常：记录并展示友好提示
        // 注意：不能让应用崩溃，影响孩子使用
        Log.e(TAG, "加载资源失败", e)
        showErrorMessage("哎呀，出了点小问题，我们再试一次吧！")
    }
}
```

### 2.7 配置和常量注释

```kotlin
object Constants {
    // ========== 时间相关配置 ==========
    
    /** 每日学习时长限制（分钟） - 保护孩子视力 */
    const val DAILY_STUDY_LIMIT_MINUTES = 30
    
    /** 单次学习时长（分钟） - 符合儿童注意力特点 */
    const val SESSION_DURATION_MINUTES = 15
    
    /** 休息提醒间隔（分钟） - 每15分钟提醒休息 */
    const val REST_REMINDER_INTERVAL = 15
    
    // ========== UI配置 ==========
    
    /** 最小点击区域（dp） - 适合儿童手指 */
    const val MIN_TOUCH_SIZE_DP = 48
    
    /** 动画时长（毫秒） - 不要太快，孩子需要时间反应 */
    const val ANIMATION_DURATION_MS = 300
    
    /** 自动播放延迟（毫秒） - 给孩子准备时间 */
    const val AUTO_PLAY_DELAY_MS = 1000
    
    // ========== AI配置 ==========
    
    /** AI请求超时时间（秒） - 避免等待太久 */
    const val AI_REQUEST_TIMEOUT_SECONDS = 10
    
    /** AI重试次数 - 失败后自动重试 */
    const val AI_RETRY_COUNT = 3
    
    /** AI缓存有效期（天） - 减少API调用成本 */
    const val AI_CACHE_DAYS = 7
}
```

### 2.8 TODO注释（临时代码必须标记）

```kotlin
fun generateLearningReport(): Report {
    // TODO: 2025-01-15 添加图表展示功能
    // 需求：用饼图展示各主题学习时长
    // 负责人：李四
    
    val report = Report()
    
    // TODO: 优化性能
    // 当前问题：数据量大时计算较慢
    // 解决方案：使用协程并行计算
    report.calculateStatistics()
    
    // FIXME: 2025-01-10 修复时区问题
    // BUG：国外用户的统计时间不正确
    report.adjustTimeZone()
    
    // HACK: 临时方案，等待后端接口
    // 正式方案：调用后端生成报告API
    return mockReport()
}
```

## 3. 注释模板

### 3.1 文件头部模板

```kotlin
/**
 * 文件名：CardPlayer.kt
 * 
 * 功能描述：
 * 教育卡片播放器的核心实现，负责管理卡片内容的播放流程，
 * 包括资源加载、动画展示、音频播放、用户交互等功能。
 * 
 * 主要类：
 * - CardPlayer: 播放器主类
 * - PlayOptions: 播放选项配置
 * - PlayCallback: 播放回调接口
 * 
 * 使用示例：
 * ```
 * val player = CardPlayer(context)
 * player.play(card) {
 *     // 播放完成
 * }
 * ```
 * 
 * @author 张三
 * @date 2025-01-03
 * @version 1.0.0
 */
```

### 3.2 复杂功能模板

```kotlin
/**
 * [功能名称]
 * 
 * 功能说明：
 * [详细描述功能的作用和目的]
 * 
 * 实现原理：
 * 1. [步骤1]
 * 2. [步骤2]
 * 3. [步骤3]
 * 
 * 注意事项：
 * - [注意点1]
 * - [注意点2]
 * 
 * 性能考虑：
 * - [性能相关说明]
 * 
 * @param [参数名] [参数说明]
 * @return [返回值说明]
 * @throws [异常类型] [异常说明]
 */
```

## 4. 最佳实践

### 4.1 注释原则

1. **准确性**：注释必须与代码保持一致
2. **简洁性**：用最少的文字说清楚
3. **及时性**：代码修改时同步更新注释
4. **必要性**：不要注释显而易见的代码

### 4.2 什么时候必须写注释

- ✅ 业务逻辑复杂的地方
- ✅ 算法实现
- ✅ 异常处理
- ✅ 性能优化的代码
- ✅ 临时解决方案
- ✅ 与需求相关的实现
- ✅ API调用和数据处理
- ✅ 安全相关的代码

### 4.3 避免的注释方式

```kotlin
// ❌ 错误：废话注释
val count = 0 // 设置count为0

// ❌ 错误：过时注释
// 2024-01-01 修复了XXX问题（但代码已经改了）

// ❌ 错误：英文注释
// This function calculates the score

// ✅ 正确：有意义的注释
// 初始化计数器，用于统计完成的卡片数量
val completedCardCount = 0
```

## 5. 工具支持

### 5.1 IDE配置

```xml
<!-- .idea/codeStyles/codeStyleConfig.xml -->
<component name="CodeStyleSettingsManager">
  <option name="PREFERRED_PROJECT_CODE_STYLE" value="KotlinStyle" />
</component>

<!-- 代码模板设置 -->
<!-- File > Settings > Editor > File and Code Templates -->
```

### 5.2 代码检查

```gradle
// 集成 detekt 进行代码检查
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.22.0"
}

detekt {
    config = files("config/detekt/detekt.yml")
    
    rules {
        comments {
            // 检查是否有类注释
            UndocumentedPublicClass { active = true }
            // 检查是否有方法注释
            UndocumentedPublicFunction { active = true }
        }
    }
}
```

## 6. 团队约定

### 6.1 代码评审标准

- 没有中文注释的代码不能通过评审
- 复杂逻辑必须有详细说明
- 修改代码时必须同步更新注释

### 6.2 新人培训

- 第一天：学习注释规范
- 代码提交前：自查注释完整性
- 导师检查：重点关注注释质量

## 7. 示例代码

```kotlin
/**
 * AI教育内容管理器
 * 
 * 负责管理所有AI生成的教育内容，包括：
 * 1. 内容生成：调用AI接口生成教育文案
 * 2. 内容缓存：缓存生成的内容，减少API调用
 * 3. 内容审核：确保内容适合儿童
 * 4. 内容更新：定期更新过时内容
 * 
 * @author 王五
 * @since 2025-01-03
 */
class AIContentManager(
    private val context: Context,
    private val aiService: AIService
) {
    
    /**
     * 为指定主题生成教育内容
     * 
     * 生成流程：
     * 1. 检查本地缓存
     * 2. 如果没有缓存或已过期，调用AI生成
     * 3. 审核生成的内容
     * 4. 保存到本地缓存
     * 
     * @param topic 教育主题，如"消防安全"、"动物认知"等
     * @param age 目标年龄，用于调整内容难度
     * @return 生成的教育内容，如果生成失败返回默认内容
     */
    suspend fun generateContent(topic: String, age: Int = 3): EducationContent {
        // 1. 尝试从缓存获取
        val cached = loadFromCache(topic, age)
        if (cached != null && !cached.isExpired()) {
            Log.d(TAG, "使用缓存内容: $topic")
            return cached
        }
        
        try {
            // 2. 构建AI提示词
            val prompt = buildPrompt(topic, age)
            
            // 3. 调用AI服务生成内容
            val aiResponse = withContext(Dispatchers.IO) {
                aiService.generateContent(prompt)
            }
            
            // 4. 内容安全审核
            val content = if (isContentSafe(aiResponse)) {
                aiResponse
            } else {
                // 内容不适合，使用预设的安全内容
                getDefaultContent(topic)
            }
            
            // 5. 保存到缓存
            saveToCache(topic, age, content)
            
            return content
            
        } catch (e: Exception) {
            // AI服务异常，使用默认内容保证可用性
            Log.e(TAG, "AI生成失败，使用默认内容", e)
            return getDefaultContent(topic)
        }
    }
    
    /**
     * 构建AI提示词
     * 
     * 提示词包含：
     * - 目标年龄
     * - 教育主题  
     * - 内容要求（字数、风格等）
     * - 安全要求
     */
    private fun buildPrompt(topic: String, age: Int): String {
        return """
        |请为${age}岁的孩子创作关于"${topic}"的教育内容：
        |
        |要求：
        |1. 语言简单易懂，句子不超过10个字
        |2. 内容积极正面，充满童趣
        |3. 包含一个简单的互动问题
        |4. 总字数控制在50-80字
        |5. 不包含任何暴力、恐怖内容
        |
        |示例格式：
        |[主题介绍]
        |[特点描述]
        |[互动问题]
        """.trimMargin()
    }
    
    /**
     * 内容安全检查
     * 
     * 检查项目：
     * - 是否包含不适合儿童的词汇
     * - 内容长度是否合适
     * - 是否符合教育目的
     */
    private fun isContentSafe(content: EducationContent): Boolean {
        // 敏感词检查
        val sensitiveWords = listOf("暴力", "恐怖", "死亡", "受伤")
        if (sensitiveWords.any { content.text.contains(it) }) {
            Log.w(TAG, "内容包含敏感词: ${content.text}")
            return false
        }
        
        // 长度检查
        if (content.text.length > 100) {
            Log.w(TAG, "内容过长: ${content.text.length}字")
            return false
        }
        
        return true
    }
    
    companion object {
        private const val TAG = "AIContentManager"
        
        /** 缓存有效期（天） */
        private const val CACHE_VALID_DAYS = 7
    }
}
```

## 8. 检查清单

代码提交前，请确认：

- [ ] 所有类都有完整的类注释
- [ ] 复杂方法都有详细的方法注释
- [ ] 业务逻辑都有流程说明
- [ ] 异常处理都有原因说明
- [ ] 算法实现都有原理讲解
- [ ] 临时代码都有TODO标记
- [ ] 注释与代码逻辑一致
- [ ] 使用中文编写注释

记住：**好的注释是送给未来的自己和团队的礼物！**
# 推荐算法实现文档（Bandit + 红色偏好）

文档版本: v1.0  
创建日期: 2025-01-03  
作者: 架构助手  
审核人: 待定

## 1. 算法概述

### 1.1 目标
- 在探索（exploration）和利用（exploitation）之间平衡
- 融入红色偏好特征，但控制曝光上限（≤60%）
- 确保内容多样性，避免过拟合
- 支持冷启动和个性化推荐

### 1.2 算法选择
采用 **Thompson Sampling** 变种，结合：
- 上下文特征（Contextual Bandit）
- 红色偏好权重调整
- 曝光率控制机制
- 探索保证机制（ε-greedy fallback）

## 2. 特征工程

### 2.1 用户特征
```kotlin
data class UserFeatures(
    val userId: String,
    val preferColor: String?,              // "red" | null
    val colorIntensity: String,            // "low" | "medium" | "high"
    val totalPlayTime: Long,               // 总学习时长
    val avgSessionDuration: Long,          // 平均会话时长
    val completionRate: Float,             // 完成率
    val retryRate: Float,                  // 重试率
    val interactionSuccessRate: Float,     // 互动成功率
    val recentThemes: List<String>,        // 最近学习的主题
    val timeOfDay: Int,                    // 一天中的时段（0-23）
    val dayOfWeek: Int                     // 星期几（1-7）
)
```

### 2.2 卡片特征
```kotlin
data class CardFeatures(
    val cardId: String,
    val themeId: String,
    val difficulty: Int,                   // 1-3
    val durationSeconds: Int,              // 20-40
    val hasRedColor: Boolean,              // 是否包含红色
    val redColorRatio: Float,              // 红色占比（0-1）
    val interactionType: String,           // "tap" | "drag" | "select"
    val educationalGoals: List<String>,    // ["勇敢", "逻辑"]
    val contentType: String,               // "animation" | "interactive"
    val popularityScore: Float,            // 全局流行度
    val avgCompletionTime: Long,           // 平均完成时间
    val successRate: Float                 // 全局成功率
)
```

### 2.3 上下文特征
```kotlin
data class ContextFeatures(
    val sessionNumber: Int,                // 今日第几次会话
    val cardsPlayedToday: Int,             // 今日已学卡片数
    val currentStreak: Int,                // 连续学习天数
    val lastCardCompleted: Boolean,        // 上一张卡是否完成
    val currentEnergy: Float,              // 当前精力值（根据时间和历史推断）
    val recentRedExposure: Float           // 最近红色内容曝光率
)
```

## 3. 算法实现

### 3.1 Thompson Sampling 核心
```kotlin
class ThompsonSamplingRecommender(
    private val database: AppDatabase,
    private val preferences: UserPreferences
) {
    // Beta分布参数
    private val armStats = mutableMapOf<String, BetaParams>()
    
    data class BetaParams(
        var alpha: Double = 1.0,  // 成功次数 + 1
        var beta: Double = 1.0    // 失败次数 + 1
    )
    
    suspend fun recommendNextCard(
        userId: String,
        availableCards: List<Card>,
        context: ContextFeatures
    ): Card {
        // 1. 获取用户特征
        val userFeatures = getUserFeatures(userId)
        
        // 2. 计算每张卡片的得分
        val scores = availableCards.map { card ->
            val cardFeatures = getCardFeatures(card)
            val score = calculateScore(userFeatures, cardFeatures, context)
            card to score
        }
        
        // 3. 应用红色偏好和曝光控制
        val adjustedScores = applyColorPreference(scores, userFeatures, context)
        
        // 4. Thompson Sampling
        val samples = adjustedScores.map { (card, score) ->
            val params = armStats.getOrPut(card.cardId) { BetaParams() }
            val sample = sampleFromBeta(params.alpha, params.beta) * score
            card to sample
        }
        
        // 5. 选择最高分的卡片
        return samples.maxByOrNull { it.second }?.first
            ?: availableCards.random() // fallback
    }
    
    private fun calculateScore(
        user: UserFeatures,
        card: CardFeatures,
        context: ContextFeatures
    ): Double {
        var score = 1.0
        
        // 基础分数：根据成功率和流行度
        score *= (0.7 * card.successRate + 0.3 * card.popularityScore)
        
        // 难度匹配
        val difficultyMatch = when {
            user.completionRate > 0.8 -> card.difficulty / 3.0
            user.completionRate > 0.5 -> if (card.difficulty == 2) 1.2 else 0.8
            else -> (4 - card.difficulty) / 3.0
        }
        score *= difficultyMatch
        
        // 时间匹配（短时注意力）
        if (context.sessionNumber > 2 && card.durationSeconds > 30) {
            score *= 0.7 // 降低长内容权重
        }
        
        // 教育目标多样性
        val recentGoals = getRecentEducationalGoals(user.userId)
        val novelty = card.educationalGoals.count { it !in recentGoals } / 
                     card.educationalGoals.size.toDouble()
        score *= (0.7 + 0.3 * novelty)
        
        // 互动类型轮换
        val recentTypes = getRecentInteractionTypes(user.userId)
        if (card.interactionType !in recentTypes.take(2)) {
            score *= 1.2
        }
        
        return score.coerceIn(0.1, 10.0)
    }
    
    private fun applyColorPreference(
        scores: List<Pair<Card, Double>>,
        user: UserFeatures,
        context: ContextFeatures
    ): List<Pair<Card, Double>> {
        if (user.preferColor != "red") return scores
        
        val redBoost = when (user.colorIntensity) {
            "high" -> 2.0
            "medium" -> 1.5
            "low" -> 1.2
            else -> 1.0
        }
        
        // 计算当前红色曝光率
        val currentRedExposure = context.recentRedExposure
        
        return scores.map { (card, score) ->
            val cardFeatures = getCardFeatures(card)
            var adjustedScore = score
            
            if (cardFeatures.hasRedColor) {
                // 如果接近曝光上限，降低红色内容权重
                if (currentRedExposure >= 0.55) {
                    adjustedScore *= 0.5
                } else if (currentRedExposure >= 0.45) {
                    adjustedScore *= (0.8 + 0.2 * cardFeatures.redColorRatio)
                } else {
                    // 正常提升红色内容权重
                    adjustedScore *= (1.0 + (redBoost - 1.0) * cardFeatures.redColorRatio)
                }
            }
            
            card to adjustedScore
        }
    }
    
    private fun sampleFromBeta(alpha: Double, beta: Double): Double {
        // 使用 Apache Commons Math 或自实现
        val distribution = BetaDistribution(alpha, beta)
        return distribution.sample()
    }
    
    fun updateReward(cardId: String, success: Boolean) {
        val params = armStats.getOrPut(cardId) { BetaParams() }
        if (success) {
            params.alpha += 1.0
        } else {
            params.beta += 1.0
        }
        
        // 防止参数过大，使用衰减
        if (params.alpha + params.beta > 100) {
            params.alpha = params.alpha * 0.95 + 1.0
            params.beta = params.beta * 0.95 + 1.0
        }
    }
}
```

### 3.2 探索保证机制
```kotlin
class ExplorationManager {
    private val EXPLORATION_RATE = 0.2  // 20%探索
    private val MIN_PLAYS_BEFORE_PENALTY = 3
    
    fun ensureExploration(
        recommendations: List<Card>,
        playHistory: Map<String, Int>
    ): List<Card> {
        val random = Random.nextDouble()
        
        if (random < EXPLORATION_RATE) {
            // 优先推荐播放次数少的内容
            val underexplored = recommendations.filter { card ->
                playHistory[card.cardId] ?: 0 < MIN_PLAYS_BEFORE_PENALTY
            }
            
            if (underexplored.isNotEmpty()) {
                return underexplored
            }
        }
        
        return recommendations
    }
}
```

### 3.3 红色曝光控制
```kotlin
class RedExposureController {
    private val WINDOW_SIZE = 10  // 滑动窗口大小
    private val MAX_RED_EXPOSURE = 0.6  // 60%上限
    private val recentCards = LinkedList<Boolean>()  // 是否为红色内容
    
    fun checkRedExposure(card: Card): Boolean {
        val cardFeatures = getCardFeatures(card)
        
        if (!cardFeatures.hasRedColor) {
            // 非红色内容，直接通过
            recordCard(false)
            return true
        }
        
        // 计算当前窗口内的红色曝光率
        val redCount = recentCards.count { it }
        val currentExposure = if (recentCards.size > 0) {
            redCount.toDouble() / recentCards.size
        } else {
            0.0
        }
        
        // 如果加入这张卡片后会超过上限，则拒绝
        val futureExposure = (redCount + 1).toDouble() / (recentCards.size + 1)
        if (futureExposure > MAX_RED_EXPOSURE && recentCards.size >= WINDOW_SIZE / 2) {
            return false
        }
        
        recordCard(true)
        return true
    }
    
    private fun recordCard(isRed: Boolean) {
        recentCards.add(isRed)
        if (recentCards.size > WINDOW_SIZE) {
            recentCards.removeFirst()
        }
    }
    
    fun getCurrentRedExposure(): Float {
        if (recentCards.isEmpty()) return 0f
        return recentCards.count { it }.toFloat() / recentCards.size
    }
}
```

## 4. 特征提取实现

### 4.1 用户特征提取
```kotlin
class FeatureExtractor(private val database: AppDatabase) {
    
    suspend fun getUserFeatures(userId: String): UserFeatures {
        val user = database.userDao().getUser(userId)
        val progress = database.progressDao().getUserProgress(userId).first()
        val sessions = database.sessionLogDao().getRecentSessions(userId, 7) // 最近7天
        
        val totalPlayTime = sessions.sumOf { it.durationMs }
        val avgSessionDuration = if (sessions.isNotEmpty()) {
            totalPlayTime / sessions.size
        } else 0L
        
        val completionRate = if (progress.isNotEmpty()) {
            progress.count { it.isCompleted }.toFloat() / progress.size
        } else 0f
        
        val retryRate = calculateRetryRate(progress)
        val interactionSuccessRate = calculateInteractionSuccessRate(userId)
        val recentThemes = getRecentThemes(userId, 5)
        
        return UserFeatures(
            userId = userId,
            preferColor = user?.preferColor,
            colorIntensity = user?.colorIntensity ?: "medium",
            totalPlayTime = totalPlayTime,
            avgSessionDuration = avgSessionDuration,
            completionRate = completionRate,
            retryRate = retryRate,
            interactionSuccessRate = interactionSuccessRate,
            recentThemes = recentThemes,
            timeOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        )
    }
    
    suspend fun getCardFeatures(card: Card): CardFeatures {
        val stats = database.featureLogDao().getCardStats(card.cardId)
        
        return CardFeatures(
            cardId = card.cardId,
            themeId = card.themeId,
            difficulty = card.difficulty,
            durationSeconds = card.durationSeconds,
            hasRedColor = card.dominantColors.contains("red"),
            redColorRatio = calculateRedColorRatio(card.dominantColors),
            interactionType = card.interactionType ?: "tap",
            educationalGoals = card.educationalGoals,
            contentType = card.contentType,
            popularityScore = stats.popularityScore,
            avgCompletionTime = stats.avgCompletionTime,
            successRate = stats.successRate
        )
    }
    
    private fun calculateRedColorRatio(colors: List<String>): Float {
        if (colors.isEmpty()) return 0f
        val redVariants = listOf("red", "crimson", "scarlet", "ruby", "cherry")
        val redCount = colors.count { it.lowercase() in redVariants }
        return redCount.toFloat() / colors.size
    }
}
```

## 5. 推荐流程

### 5.1 完整推荐流程
```kotlin
class RecommendationService(
    private val recommender: ThompsonSamplingRecommender,
    private val exposureController: RedExposureController,
    private val explorationManager: ExplorationManager,
    private val database: AppDatabase
) {
    suspend fun getNextCard(userId: String, currentThemeId: String?): Card? {
        // 1. 获取候选卡片
        val candidates = if (currentThemeId != null) {
            // 主题内推荐
            getCandidatesFromTheme(userId, currentThemeId)
        } else {
            // 跨主题推荐
            getAllCandidates(userId)
        }
        
        if (candidates.isEmpty()) return null
        
        // 2. 获取上下文
        val context = buildContext(userId)
        
        // 3. 获取推荐
        var recommendation = recommender.recommendNextCard(userId, candidates, context)
        
        // 4. 检查红色曝光
        if (!exposureController.checkRedExposure(recommendation)) {
            // 过滤掉红色内容重新推荐
            val nonRedCandidates = candidates.filter { card ->
                !getCardFeatures(card).hasRedColor
            }
            if (nonRedCandidates.isNotEmpty()) {
                recommendation = recommender.recommendNextCard(
                    userId, 
                    nonRedCandidates, 
                    context
                )
            }
        }
        
        // 5. 记录推荐
        logRecommendation(userId, recommendation)
        
        return recommendation
    }
    
    private suspend fun getCandidatesFromTheme(
        userId: String, 
        themeId: String
    ): List<Card> {
        // 获取主题内未完成的卡片
        val cards = database.cardDao().getThemeCards(themeId)
        val progress = database.progressDao().getUserProgressMap(userId)
        
        return cards.filter { card ->
            val cardProgress = progress[card.cardId]
            cardProgress == null || !cardProgress.isCompleted
        }
    }
}
```

### 5.2 冷启动策略
```kotlin
class ColdStartStrategy {
    fun getInitialRecommendations(userAge: Int = 3): List<Card> {
        // 1. 选择适龄的入门内容
        val starterThemes = listOf("勇敢小英雄", "颜色认知", "数字乐园")
        
        // 2. 每个主题选1-2个简单卡片
        val recommendations = mutableListOf<Card>()
        starterThemes.forEach { theme ->
            val cards = database.cardDao()
                .getThemeCards(theme)
                .filter { it.difficulty == 1 }
                .take(2)
            recommendations.addAll(cards)
        }
        
        // 3. 确保包含不同互动类型
        return recommendations.distinctBy { it.interactionType }
            .take(5)
    }
}
```

## 6. 性能优化

### 6.1 缓存策略
```kotlin
class RecommendationCache {
    private val cache = LruCache<String, List<Card>>(10)
    private val featureCache = LruCache<String, UserFeatures>(20)
    
    fun getCachedRecommendations(userId: String): List<Card>? {
        return cache.get(userId)
    }
    
    fun cacheRecommendations(userId: String, cards: List<Card>) {
        cache.put(userId, cards)
        
        // 设置过期时间
        GlobalScope.launch {
            delay(5 * 60 * 1000) // 5分钟
            cache.remove(userId)
        }
    }
}
```

### 6.2 批量预计算
```kotlin
class FeaturePrecomputer {
    @WorkerThread
    suspend fun precomputeCardFeatures() {
        val cards = database.cardDao().getAllCards()
        cards.forEach { card ->
            val features = calculateCardFeatures(card)
            database.cardFeatureDao().upsert(features)
        }
    }
}
```

## 7. 监控与评估

### 7.1 关键指标
```kotlin
data class RecommendationMetrics(
    val clickThroughRate: Float,        // 点击率
    val completionRate: Float,          // 完成率
    val retentionRate: Float,           // 留存率
    val diversityScore: Float,          // 多样性得分
    val redExposureRate: Float,         // 红色曝光率
    val explorationRate: Float,         // 探索率
    val avgSessionLength: Float,        // 平均会话长度
    val userSatisfaction: Float         // 用户满意度（通过行为推断）
)
```

### 7.2 A/B测试框架
```kotlin
class ABTestManager {
    fun getRecommendationStrategy(userId: String): RecommendationStrategy {
        val bucket = userId.hashCode() % 100
        
        return when {
            bucket < 50 -> ThompsonSamplingStrategy()      // 50% Thompson Sampling
            bucket < 80 -> EpsilonGreedyStrategy(0.2)      // 30% ε-greedy
            else -> RandomStrategy()                        // 20% 随机（对照组）
        }
    }
}
```

## 8. 调试工具

### 8.1 推荐解释
```kotlin
data class RecommendationExplanation(
    val cardId: String,
    val baseScore: Double,
    val featureContributions: Map<String, Double>,
    val finalScore: Double,
    val rank: Int,
    val debugInfo: Map<String, Any>
)

fun explainRecommendation(userId: String, cardId: String): RecommendationExplanation {
    // 生成推荐解释，用于调试和优化
}
```

### 8.2 模拟器
```kotlin
class RecommendationSimulator {
    fun simulateUserJourney(
        userProfile: UserProfile,
        days: Int
    ): SimulationResult {
        // 模拟用户学习路径，评估推荐效果
    }
}
```
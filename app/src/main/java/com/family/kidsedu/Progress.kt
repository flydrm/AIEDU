package com.family.kidsedu

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

/**
 * 学习进度管理器
 * 
 * 功能说明：
 * 使用SharedPreferences管理用户的学习进度，包括：
 * 1. 当前学习的卡片位置
 * 2. 每日学习时长统计
 * 3. 卡片完成状态记录
 * 
 * 使用场景：
 * - 应用启动时恢复上次学习位置
 * - 记录每日学习时长，用于时长控制
 * - 保存学习成果
 * 
 * 注意事项：
 * - 所有操作都是同步的，适合小数据量
 * - 使用单例模式，全局访问
 * 
 * @author AI启蒙时光开发团队
 * @since 2025-01-03
 */
object Progress {
    
    private const val PREF_NAME = "kidsedu_progress"
    private const val KEY_CURRENT_CARD = "current_card"
    private const val KEY_PLAY_TIME_PREFIX = "play_time_"
    private const val KEY_COMPLETED_CARDS = "completed_cards"
    private const val KEY_TOTAL_STUDY_DAYS = "total_study_days"
    private const val KEY_LAST_STUDY_DATE = "last_study_date"
    
    /** SharedPreferences实例，延迟初始化 */
    private lateinit var prefs: SharedPreferences
    
    /** 日期格式化工具 */
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    
    /**
     * 初始化进度管理器
     * 必须在使用其他方法前调用
     * 
     * @param context 应用上下文
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 获取当前学习的卡片索引
     * 
     * @return 卡片索引，从0开始
     */
    fun getCurrentCardIndex(): Int {
        return prefs.getInt(KEY_CURRENT_CARD, 0)
    }
    
    /**
     * 保存当前学习的卡片索引
     * 
     * @param index 卡片索引
     */
    fun saveCurrentCardIndex(index: Int) {
        prefs.edit().putInt(KEY_CURRENT_CARD, index).apply()
    }
    
    /**
     * 获取今日已学习时长（分钟）
     * 
     * @return 今日学习时长
     */
    fun getTodayPlayTime(): Int {
        val today = dateFormat.format(Date())
        return prefs.getInt("$KEY_PLAY_TIME_PREFIX$today", 0)
    }
    
    /**
     * 增加今日学习时长
     * 
     * @param minutes 要增加的分钟数
     */
    fun addPlayTime(minutes: Int) {
        val today = dateFormat.format(Date())
        val current = getTodayPlayTime()
        val newTime = current + minutes
        
        prefs.edit()
            .putInt("$KEY_PLAY_TIME_PREFIX$today", newTime)
            .apply()
        
        // 更新最后学习日期
        updateLastStudyDate()
    }
    
    /**
     * 检查今日是否已达到学习时长限制
     * 
     * @param limitMinutes 时长限制（分钟）
     * @return 是否已达到限制
     */
    fun hasReachedDailyLimit(limitMinutes: Int = 20): Boolean {
        return getTodayPlayTime() >= limitMinutes
    }
    
    /**
     * 记录卡片完成状态
     * 
     * @param cardId 卡片ID
     */
    fun markCardCompleted(cardId: Int) {
        val completed = getCompletedCards().toMutableSet()
        completed.add(cardId)
        
        // 保存为逗号分隔的字符串
        prefs.edit()
            .putString(KEY_COMPLETED_CARDS, completed.joinToString(","))
            .apply()
    }
    
    /**
     * 获取所有已完成的卡片ID
     * 
     * @return 已完成的卡片ID集合
     */
    fun getCompletedCards(): Set<Int> {
        val completedString = prefs.getString(KEY_COMPLETED_CARDS, "") ?: ""
        return if (completedString.isEmpty()) {
            emptySet()
        } else {
            completedString.split(",")
                .mapNotNull { it.toIntOrNull() }
                .toSet()
        }
    }
    
    /**
     * 检查卡片是否已完成
     * 
     * @param cardId 卡片ID
     * @return 是否已完成
     */
    fun isCardCompleted(cardId: Int): Boolean {
        return getCompletedCards().contains(cardId)
    }
    
    /**
     * 获取学习进度百分比
     * 
     * @param totalCards 总卡片数
     * @return 进度百分比（0-100）
     */
    fun getProgressPercentage(totalCards: Int): Int {
        if (totalCards == 0) return 0
        val completed = getCompletedCards().size
        return (completed * 100) / totalCards
    }
    
    /**
     * 更新最后学习日期
     */
    private fun updateLastStudyDate() {
        val today = dateFormat.format(Date())
        val lastDate = prefs.getString(KEY_LAST_STUDY_DATE, "")
        
        if (lastDate != today) {
            // 新的一天，增加学习天数
            val totalDays = prefs.getInt(KEY_TOTAL_STUDY_DAYS, 0) + 1
            prefs.edit()
                .putString(KEY_LAST_STUDY_DATE, today)
                .putInt(KEY_TOTAL_STUDY_DAYS, totalDays)
                .apply()
        }
    }
    
    /**
     * 获取总学习天数
     * 
     * @return 总学习天数
     */
    fun getTotalStudyDays(): Int {
        return prefs.getInt(KEY_TOTAL_STUDY_DAYS, 0)
    }
    
    /**
     * 重置所有进度（谨慎使用）
     * 
     * 使用场景：
     * - 完成所有内容后重新开始
     * - 切换使用者
     */
    fun resetAllProgress() {
        prefs.edit().clear().apply()
    }
    
    /**
     * 获取学习统计信息
     * 
     * @param totalCards 总卡片数
     * @return 统计信息字符串
     */
    fun getStatisticsSummary(totalCards: Int): String {
        val completed = getCompletedCards().size
        val percentage = getProgressPercentage(totalCards)
        val todayTime = getTodayPlayTime()
        val totalDays = getTotalStudyDays()
        
        return """
            学习进度：$completed/$totalCards ($percentage%)
            今日学习：$todayTime 分钟
            坚持天数：$totalDays 天
        """.trimIndent()
    }
}
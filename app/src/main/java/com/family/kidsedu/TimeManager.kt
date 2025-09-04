package com.family.kidsedu

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import java.util.*

/**
 * 学习时长管理器
 * 
 * 功能说明：
 * 1. 监控每日学习时长
 * 2. 定时提醒休息
 * 3. 记录学习时间统计
 * 4. 保护儿童视力健康
 * 
 * 提醒策略：
 * - 每15分钟温和提醒
 * - 20分钟强烈建议休息
 * - 30分钟强制休息
 * 
 * 注意事项：
 * - 提醒方式要温和友好
 * - 允许家长灵活控制
 * - 记录详细的时间统计
 * 
 * @author AI启蒙时光开发团队
 * @since 2025-01-03
 */
class TimeManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TimeManager"
        
        /** 提醒间隔（分钟） */
        const val GENTLE_REMINDER_MINUTES = 15
        const val STRONG_REMINDER_MINUTES = 20
        const val FORCE_REST_MINUTES = 30
        
        /** 建议休息时长（分钟） */
        const val SUGGESTED_REST_MINUTES = 5
        
        /** 检查间隔（毫秒） */
        private const val CHECK_INTERVAL_MS = 60_000L // 1分钟
    }
    
    /** 消息处理器 */
    private val handler = Handler(Looper.getMainLooper())
    
    /** 本次学习开始时间 */
    private var sessionStartTime = 0L
    
    /** 本次学习已暂停的时间 */
    private var pausedDuration = 0L
    
    /** 暂停开始时间 */
    private var pauseStartTime = 0L
    
    /** 是否正在学习 */
    private var isStudying = false
    
    /** 是否已暂停 */
    private var isPaused = false
    
    /** 时长监听器 */
    private var timeListener: TimeListener? = null
    
    /** 定时检查任务 */
    private val checkRunnable = object : Runnable {
        override fun run() {
            checkStudyTime()
            // 继续下一次检查
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }
    
    /**
     * 开始学习计时
     */
    fun startStudying() {
        if (isStudying && !isPaused) return
        
        if (isPaused) {
            // 从暂停恢复
            pausedDuration += System.currentTimeMillis() - pauseStartTime
            isPaused = false
        } else {
            // 新的学习会话
            sessionStartTime = System.currentTimeMillis()
            pausedDuration = 0
            isStudying = true
        }
        
        // 开始定时检查
        handler.removeCallbacks(checkRunnable)
        handler.post(checkRunnable)
        
        Log.d(TAG, "学习计时开始")
    }
    
    /**
     * 暂停学习计时
     */
    fun pauseStudying() {
        if (!isStudying || isPaused) return
        
        isPaused = true
        pauseStartTime = System.currentTimeMillis()
        
        // 停止定时检查
        handler.removeCallbacks(checkRunnable)
        
        Log.d(TAG, "学习计时暂停")
    }
    
    /**
     * 停止学习计时
     * 
     * @return 本次学习时长（分钟）
     */
    fun stopStudying(): Int {
        if (!isStudying) return 0
        
        val studyMinutes = getSessionMinutes()
        
        // 保存本次学习时长
        Progress.addPlayTime(studyMinutes)
        
        // 重置状态
        isStudying = false
        isPaused = false
        sessionStartTime = 0
        pausedDuration = 0
        
        // 停止定时检查
        handler.removeCallbacks(checkRunnable)
        
        Log.d(TAG, "学习计时结束，本次学习: ${studyMinutes}分钟")
        
        return studyMinutes
    }
    
    /**
     * 获取本次学习时长（分钟）
     */
    fun getSessionMinutes(): Int {
        if (!isStudying) return 0
        
        val currentTime = System.currentTimeMillis()
        val actualDuration = if (isPaused) {
            (pauseStartTime - sessionStartTime - pausedDuration)
        } else {
            (currentTime - sessionStartTime - pausedDuration)
        }
        
        return (actualDuration / 60_000).toInt()
    }
    
    /**
     * 获取今日总学习时长（分钟）
     */
    fun getTodayTotalMinutes(): Int {
        return Progress.getTodayPlayTime() + getSessionMinutes()
    }
    
    /**
     * 检查学习时长并提醒
     */
    private fun checkStudyTime() {
        val sessionMinutes = getSessionMinutes()
        val todayMinutes = getTodayTotalMinutes()
        
        Log.d(TAG, "学习时长检查 - 本次: ${sessionMinutes}分钟, 今日: ${todayMinutes}分钟")
        
        // 通知监听器
        timeListener?.onTimeUpdate(sessionMinutes, todayMinutes)
        
        // 根据时长决定提醒级别
        when {
            sessionMinutes >= FORCE_REST_MINUTES -> {
                showForceRestDialog()
            }
            sessionMinutes >= STRONG_REMINDER_MINUTES -> {
                showStrongReminderDialog()
            }
            sessionMinutes >= GENTLE_REMINDER_MINUTES && sessionMinutes % GENTLE_REMINDER_MINUTES == 0 -> {
                showGentleReminder()
            }
        }
    }
    
    /**
     * 显示温和提醒
     */
    private fun showGentleReminder() {
        timeListener?.onGentleReminder()
        
        // 可以显示一个简单的Toast或者Snackbar
        Log.d(TAG, "温和提醒：已学习${getSessionMinutes()}分钟")
    }
    
    /**
     * 显示强烈建议休息对话框
     */
    private fun showStrongReminderDialog() {
        if (context !is Activity) return
        
        AlertDialog.Builder(context)
            .setTitle("该休息一下了")
            .setMessage("小朋友已经学习${getSessionMinutes()}分钟了，眼睛需要休息一下哦！")
            .setPositiveButton("休息5分钟") { _, _ ->
                startRestTimer()
            }
            .setNegativeButton("再学一会") { _, _ ->
                // 继续学习，但5分钟后会再次提醒
                handler.postDelayed({
                    checkStudyTime()
                }, 5 * 60_000L)
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * 显示强制休息对话框
     */
    private fun showForceRestDialog() {
        if (context !is Activity) return
        
        pauseStudying()
        
        AlertDialog.Builder(context)
            .setTitle("必须休息了！")
            .setMessage("已经学习${getSessionMinutes()}分钟了，为了保护眼睛，必须休息一下！\n\n去喝点水，看看远处的风景吧！")
            .setPositiveButton("好的，去休息") { _, _ ->
                startRestTimer()
                timeListener?.onForceRest()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * 开始休息计时器
     */
    private fun startRestTimer() {
        pauseStudying()
        
        // 显示休息倒计时
        showRestCountdown()
    }
    
    /**
     * 显示休息倒计时
     */
    private fun showRestCountdown() {
        if (context !is Activity) return
        
        var remainingSeconds = SUGGESTED_REST_MINUTES * 60
        
        val dialog = AlertDialog.Builder(context)
            .setTitle("休息中...")
            .setMessage(formatRestTime(remainingSeconds))
            .setPositiveButton("结束休息", null)
            .setCancelable(false)
            .create()
        
        dialog.show()
        
        // 倒计时更新
        val countdownRunnable = object : Runnable {
            override fun run() {
                remainingSeconds--
                
                if (remainingSeconds > 0) {
                    dialog.setMessage(formatRestTime(remainingSeconds))
                    handler.postDelayed(this, 1000)
                } else {
                    dialog.dismiss()
                    showRestCompleteDialog()
                }
            }
        }
        
        handler.postDelayed(countdownRunnable, 1000)
        
        // 允许提前结束休息
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            handler.removeCallbacksAndMessages(null)
            dialog.dismiss()
            onRestComplete()
        }
    }
    
    /**
     * 格式化休息时间显示
     */
    private fun formatRestTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("请休息一下，还剩 %d:%02d\n\n可以：\n• 闭上眼睛休息\n• 看看窗外远处\n• 活动一下身体", minutes, secs)
    }
    
    /**
     * 显示休息完成对话框
     */
    private fun showRestCompleteDialog() {
        if (context !is Activity) return
        
        AlertDialog.Builder(context)
            .setTitle("休息完成！")
            .setMessage("休息得怎么样？准备好继续学习了吗？")
            .setPositiveButton("继续学习") { _, _ ->
                onRestComplete()
            }
            .setNegativeButton("结束学习") { _, _ ->
                timeListener?.onEndStudy()
            }
            .show()
    }
    
    /**
     * 休息完成处理
     */
    private fun onRestComplete() {
        // 重置本次学习时长（休息后重新计算）
        sessionStartTime = System.currentTimeMillis()
        pausedDuration = 0
        
        startStudying()
        timeListener?.onRestComplete()
    }
    
    /**
     * 设置时长监听器
     */
    fun setTimeListener(listener: TimeListener) {
        this.timeListener = listener
    }
    
    /**
     * 获取学习统计信息
     */
    fun getStudyStatistics(): StudyStatistics {
        val calendar = Calendar.getInstance()
        val todayMinutes = getTodayTotalMinutes()
        val sessionMinutes = getSessionMinutes()
        val totalDays = Progress.getTotalStudyDays()
        
        return StudyStatistics(
            todayMinutes = todayMinutes,
            sessionMinutes = sessionMinutes,
            totalDays = totalDays,
            averageMinutesPerDay = if (totalDays > 0) todayMinutes / totalDays else 0,
            isHealthyDuration = todayMinutes <= STRONG_REMINDER_MINUTES
        )
    }
    
    /**
     * 释放资源
     */
    fun release() {
        handler.removeCallbacksAndMessages(null)
        timeListener = null
    }
    
    /**
     * 时长监听器接口
     */
    interface TimeListener {
        /** 时间更新 */
        fun onTimeUpdate(sessionMinutes: Int, todayMinutes: Int)
        
        /** 温和提醒 */
        fun onGentleReminder()
        
        /** 强制休息 */
        fun onForceRest()
        
        /** 休息完成 */
        fun onRestComplete()
        
        /** 结束学习 */
        fun onEndStudy()
    }
    
    /**
     * 学习统计数据
     */
    data class StudyStatistics(
        val todayMinutes: Int,
        val sessionMinutes: Int,
        val totalDays: Int,
        val averageMinutesPerDay: Int,
        val isHealthyDuration: Boolean
    )
}
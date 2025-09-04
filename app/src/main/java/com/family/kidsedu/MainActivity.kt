package com.family.kidsedu

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.family.kidsedu.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 主界面Activity
 * 
 * 功能说明：
 * 1. 展示教育卡片内容
 * 2. 管理卡片播放流程
 * 3. 处理用户交互
 * 4. 记录学习进度
 * 5. 控制学习时长
 * 
 * 使用的技术：
 * - ViewBinding: 简化View操作
 * - Coroutines: 处理异步任务
 * - MediaPlayer: 播放音频
 * - SharedPreferences: 保存进度
 * 
 * 注意事项：
 * - 支持手机和平板的横竖屏切换
 * - 音频资源需要在onDestroy中释放
 * - 时长控制20分钟自动提醒
 * 
 * @author AI启蒙时光开发团队
 * @since 2025-01-03
 */
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        
        /** 每日学习时长限制（分钟） */
        private const val DAILY_STUDY_LIMIT_MINUTES = 20
        
        /** 自动显示互动提示的延迟（毫秒） */
        private const val INTERACTION_HINT_DELAY = 3000L
        
        /** 奖励动画显示时长（毫秒） */
        private const val REWARD_ANIMATION_DURATION = 2000L
    }
    
    /** 视图绑定对象 */
    private lateinit var binding: ActivityMainBinding
    
    /** 当前显示的卡片索引 */
    private var currentCardIndex = 0
    
    /** 音频播放器 */
    private var mediaPlayer: MediaPlayer? = null
    
    /** 开始学习的时间戳 */
    private var startTime = 0L
    
    /** 消息处理器 */
    private val handler = Handler(Looper.getMainLooper())
    
    /** 是否为平板设备 */
    private var isTablet = false
    
    /** 是否已完成当前卡片的互动 */
    private var isInteractionComplete = false
    
    /** AI服务（延迟初始化） */
    private val aiService by lazy { SimpleAIService(this) }
    
    /** 时长管理器 */
    private lateinit var timeManager: TimeManager
    
    /**
     * 教育卡片列表
     * 使用推荐顺序，优先展示孩子感兴趣的内容
     */
    private val cards = CardDataProvider.getRecommendedOrder()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化视图绑定
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 检测设备类型
        detectDeviceType()
        
        // 设置屏幕方向（平板推荐横屏）
        configureScreenOrientation()
        
        // 初始化时长管理器
        initTimeManager()
        
        // 恢复学习进度
        restoreProgress()
        
        // 设置点击监听器
        setupClickListeners()
        
        // 显示当前卡片
        showCard(currentCardIndex)
        
        // 启动学习计时
        timeManager.startStudying()
        
        Log.d(TAG, "MainActivity创建完成，当前卡片索引: $currentCardIndex")
    }
    
    /**
     * 检测设备类型（手机或平板）
     */
    private fun detectDeviceType() {
        // 通过最小屏幕宽度判断是否为平板
        isTablet = resources.configuration.smallestScreenWidthDp >= 600
        Log.d(TAG, "设备类型: ${if (isTablet) "平板" else "手机"}")
    }
    
    /**
     * 配置屏幕方向
     * 平板默认横屏，手机默认竖屏
     */
    private fun configureScreenOrientation() {
        requestedOrientation = if (isTablet) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
    }
    
    /**
     * 初始化时长管理器
     */
    private fun initTimeManager() {
        timeManager = TimeManager(this).apply {
            setTimeListener(object : TimeManager.TimeListener {
                override fun onTimeUpdate(sessionMinutes: Int, todayMinutes: Int) {
                    // 可以在UI上显示学习时长
                    Log.d(TAG, "学习时长更新 - 本次: $sessionMinutes 分钟, 今日: $todayMinutes 分钟")
                }
                
                override fun onGentleReminder() {
                    // 温和提醒可以用Toast或Snackbar
                    Log.d(TAG, "温和提醒：注意休息")
                }
                
                override fun onForceRest() {
                    // 强制休息
                    pauseAudio()
                }
                
                override fun onRestComplete() {
                    // 休息完成，恢复播放
                    resumeAudio()
                }
                
                override fun onEndStudy() {
                    // 结束学习
                    finish()
                }
            })
        }
    }
    
    /**
     * 恢复学习进度
     */
    private fun restoreProgress() {
        currentCardIndex = Progress.getCurrentCardIndex()
        startTime = System.currentTimeMillis()
    }
    
    /**
     * 设置点击监听器
     */
    private fun setupClickListeners() {
        // 卡片容器点击事件
        binding.cardContainer.setOnClickListener {
            handleCardClick()
        }
        
        // 奖励容器点击事件（点击关闭）
        binding.rewardContainer.setOnClickListener {
            hideReward()
            nextCard()
        }
    }
    
    /**
     * 显示指定索引的卡片
     * 
     * @param index 卡片索引
     */
    private fun showCard(index: Int) {
        // 检查是否已完成所有卡片
        if (index >= cards.size) {
            showCompletionDialog()
            return
        }
        
        val card = cards[index]
        
        // 更新进度显示
        updateProgressUI(index)
        
        // 显示卡片内容
        displayCardContent(card)
        
        // 播放音频
        playCardAudio(card)
        
        // 重置互动状态
        isInteractionComplete = false
        
        // 延迟显示互动提示
        scheduleInteractionHint()
        
        Log.d(TAG, "显示卡片: ${card.title} (${index + 1}/${cards.size})")
    }
    
    /**
     * 更新进度UI
     */
    private fun updateProgressUI(index: Int) {
        binding.progressText.text = getString(R.string.progress_format, index + 1, cards.size)
        binding.progressBar.progress = ((index + 1) * 100) / cards.size
    }
    
    /**
     * 显示卡片内容
     */
    private fun displayCardContent(card: Card) {
        // 显示图片
        binding.cardImage.setImageResource(card.imageResId)
        
        // 显示AI生成的文本（如果有）
        if (!card.textContent.isNullOrEmpty()) {
            binding.aiTextContent.apply {
                text = card.textContent
                visibility = View.VISIBLE
            }
        } else {
            binding.aiTextContent.visibility = View.GONE
        }
        
        // 添加淡入动画
        binding.cardImage.alpha = 0f
        binding.cardImage.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }
    
    /**
     * 播放卡片音频
     */
    private fun playCardAudio(card: Card) {
        // 停止之前的音频
        stopAudio()
        
        try {
            mediaPlayer = MediaPlayer.create(this, card.audioResId).apply {
                setOnCompletionListener {
                    Log.d(TAG, "音频播放完成")
                    // 音频播放完成后可以进行互动
                    isInteractionComplete = true
                }
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放音频失败", e)
            // 音频播放失败也允许继续
            isInteractionComplete = true
        }
    }
    
    /**
     * 停止音频播放
     */
    private fun stopAudio() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }
    
    /**
     * 延迟显示互动提示
     */
    private fun scheduleInteractionHint() {
        // 先隐藏提示
        binding.interactionHint.visibility = View.GONE
        
        // 清除之前的延迟任务
        handler.removeCallbacksAndMessages("hint")
        
        // 延迟显示提示
        handler.postDelayed({
            showInteractionHint()
        }, INTERACTION_HINT_DELAY)
    }
    
    /**
     * 显示互动提示动画
     */
    private fun showInteractionHint() {
        binding.interactionHint.apply {
            visibility = View.VISIBLE
            
            // 创建缩放动画（从小到大）
            val scaleAnimation = ScaleAnimation(
                0.5f, 1.2f, 0.5f, 1.2f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 500
                repeatCount = ScaleAnimation.INFINITE
                repeatMode = ScaleAnimation.REVERSE
            }
            
            startAnimation(scaleAnimation)
        }
    }
    
    /**
     * 处理卡片点击事件
     */
    private fun handleCardClick() {
        // 隐藏互动提示
        binding.interactionHint.apply {
            clearAnimation()
            visibility = View.GONE
        }
        
        // 播放点击动画
        playClickAnimation()
        
        // 如果互动完成，显示奖励
        if (isInteractionComplete) {
            showReward()
        }
    }
    
    /**
     * 播放点击动画
     */
    private fun playClickAnimation() {
        // 播放点击音效
        App.soundManager.playClickSound()
        
        // 播放点击动画
        AnimationUtils.playClickAnimation(binding.cardImage)
    }
    
    /**
     * 显示奖励动画
     */
    private fun showReward() {
        // 标记当前卡片为已完成
        Progress.markCardCompleted(cards[currentCardIndex].id)
        
        // 播放奖励音效
        App.soundManager.playRewardSound()
        
        // 生成鼓励语（可以使用AI生成）
        lifecycleScope.launch {
            val encouragement = generateEncouragement()
            binding.encouragementText.text = encouragement
        }
        
        // 显示奖励容器
        binding.rewardContainer.visibility = View.VISIBLE
        
        // 使用动画工具类播放星星动画
        AnimationUtils.popAndRotate(binding.starReward)
        
        // 自动隐藏奖励（可选）
        handler.postDelayed({
            if (binding.rewardContainer.visibility == View.VISIBLE) {
                hideReward()
                nextCard()
            }
        }, REWARD_ANIMATION_DURATION)
    }
    
    /**
     * 隐藏奖励动画
     */
    private fun hideReward() {
        binding.rewardContainer.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.rewardContainer.visibility = View.GONE
                binding.rewardContainer.alpha = 1f
            }
            .start()
    }
    
    /**
     * 生成鼓励语
     * 可以使用AI生成，这里先用预定义的
     */
    private suspend fun generateEncouragement(): String {
        return try {
            // 尝试使用AI生成
            aiService.generateEncouragement()
        } catch (e: Exception) {
            // AI失败时使用预定义的鼓励语
            val encouragements = listOf(
                getString(R.string.encouragement_great),
                getString(R.string.encouragement_awesome),
                getString(R.string.encouragement_continue)
            )
            encouragements.random()
        }
    }
    
    /**
     * 进入下一张卡片
     */
    private fun nextCard() {
        currentCardIndex++
        Progress.saveCurrentCardIndex(currentCardIndex)
        showCard(currentCardIndex)
    }
    

    
    /**
     * 显示完成对话框
     */
    private fun showCompletionDialog() {
        val message = if (currentCardIndex >= cards.size) {
            getString(R.string.completion_all_cards)
        } else {
            getString(R.string.completion_message)
        }
        
        AlertDialog.Builder(this)
            .setTitle(R.string.completion_title)
            .setMessage(message)
            .setPositiveButton(R.string.button_ok) { _, _ ->
                // 重置进度并退出
                Progress.saveCurrentCardIndex(0)
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * 暂停音频
     */
    private fun pauseAudio() {
        mediaPlayer?.pause()
    }
    
    /**
     * 恢复音频
     */
    private fun resumeAudio() {
        mediaPlayer?.start()
    }
    
    override fun onPause() {
        super.onPause()
        // 暂停音频播放
        mediaPlayer?.pause()
        
        // 暂停学习计时
        timeManager.pauseStudying()
        
        // 保存当前进度
        Progress.saveCurrentCardIndex(currentCardIndex)
        
        Log.d(TAG, "onPause: 保存进度 $currentCardIndex")
    }
    
    override fun onResume() {
        super.onResume()
        // 恢复音频播放
        mediaPlayer?.start()
        
        // 恢复学习计时
        timeManager.startStudying()
        
        Log.d(TAG, "onResume: 恢复播放")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 释放音频资源
        stopAudio()
        
        // 停止学习计时并保存
        timeManager.stopStudying()
        timeManager.release()
        
        // 清除所有延迟任务
        handler.removeCallbacksAndMessages(null)
        
        Log.d(TAG, "onDestroy: 释放资源")
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 处理屏幕旋转
        Log.d(TAG, "屏幕方向变化: ${if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) "横屏" else "竖屏"}")
    }
}
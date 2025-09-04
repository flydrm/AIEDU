package com.family.kidsedu

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.*
import androidx.core.view.ViewCompat

/**
 * 动画工具类
 * 
 * 功能说明：
 * 提供各种常用的动画效果，专为儿童应用设计
 * 
 * 动画特点：
 * 1. 速度适中，不会太快让孩子眼花
 * 2. 效果柔和，不会太突兀
 * 3. 有趣生动，吸引孩子注意力
 * 
 * 包含动画：
 * - 缩放动画（点击反馈）
 * - 淡入淡出
 * - 弹性动画
 * - 摇摆动画
 * - 星星闪烁
 * 
 * @author AI启蒙时光开发团队
 * @since 2025-01-03
 */
object AnimationUtils {
    
    /** 默认动画时长（毫秒） */
    private const val DEFAULT_DURATION = 300L
    
    /** 儿童友好的动画时长（稍慢） */
    private const val CHILD_FRIENDLY_DURATION = 400L
    
    /**
     * 点击缩放动画
     * 
     * 效果：先缩小再恢复，模拟按压效果
     * 使用场景：按钮点击、卡片点击
     * 
     * @param view 要执行动画的视图
     * @param scale 缩放比例，默认0.95f
     * @param duration 动画时长
     * @param onEnd 动画结束回调
     */
    fun playClickAnimation(
        view: View,
        scale: Float = 0.95f,
        duration: Long = 150,
        onEnd: (() -> Unit)? = null
    ) {
        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(duration)
                    .setInterpolator(OvershootInterpolator())
                    .withEndAction {
                        onEnd?.invoke()
                    }
                    .start()
            }
            .start()
    }
    
    /**
     * 弹性出现动画
     * 
     * 效果：从小变大，带有弹性效果
     * 使用场景：奖励出现、新内容展示
     * 
     * @param view 要执行动画的视图
     * @param duration 动画时长
     * @param delay 延迟时间
     */
    fun bounceIn(
        view: View,
        duration: Long = CHILD_FRIENDLY_DURATION,
        delay: Long = 0
    ) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.scaleX = 0.3f
        view.scaleY = 0.3f
        
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(duration)
            .setStartDelay(delay)
            .setInterpolator(OvershootInterpolator(2f))
            .start()
    }
    
    /**
     * 淡入动画
     * 
     * 效果：从透明到不透明
     * 使用场景：内容切换、文字出现
     * 
     * @param view 要执行动画的视图
     * @param duration 动画时长
     * @param onEnd 动画结束回调
     */
    fun fadeIn(
        view: View,
        duration: Long = DEFAULT_DURATION,
        onEnd: (() -> Unit)? = null
    ) {
        view.apply {
            alpha = 0f
            visibility = View.VISIBLE
            
            animate()
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onEnd?.invoke()
                    }
                })
                .start()
        }
    }
    
    /**
     * 淡出动画
     * 
     * 效果：从不透明到透明
     * 使用场景：内容隐藏、切换过渡
     * 
     * @param view 要执行动画的视图
     * @param duration 动画时长
     * @param hideOnEnd 动画结束后是否隐藏视图
     */
    fun fadeOut(
        view: View,
        duration: Long = DEFAULT_DURATION,
        hideOnEnd: Boolean = true
    ) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (hideOnEnd) {
                        view.visibility = View.GONE
                    }
                }
            })
            .start()
    }
    
    /**
     * 摇摆动画
     * 
     * 效果：左右摇摆，像在跳舞
     * 使用场景：吸引注意、错误提示
     * 
     * @param view 要执行动画的视图
     * @param angle 摇摆角度
     * @param duration 单次摇摆时长
     * @param repeatCount 重复次数
     */
    fun wiggle(
        view: View,
        angle: Float = 10f,
        duration: Long = 100,
        repeatCount: Int = 3
    ) {
        val rotation = ObjectAnimator.ofFloat(view, "rotation", -angle, angle).apply {
            this.duration = duration
            this.repeatCount = repeatCount * 2 - 1
            this.repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        rotation.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // 确保最终回到原始位置
                view.rotation = 0f
            }
        })
        
        rotation.start()
    }
    
    /**
     * 脉冲动画
     * 
     * 效果：像心跳一样的缩放
     * 使用场景：提示点击、强调重要内容
     * 
     * @param view 要执行动画的视图
     * @param scale 最大缩放比例
     * @param duration 动画时长
     */
    fun pulse(
        view: View,
        scale: Float = 1.1f,
        duration: Long = 1000
    ): Animator {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, scale, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, scale, 1f)
        
        return AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            // 无限循环
            scaleX.repeatCount = ObjectAnimator.INFINITE
            scaleY.repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }
    
    /**
     * 星星闪烁动画
     * 
     * 效果：透明度变化模拟闪烁
     * 使用场景：奖励星星、装饰元素
     * 
     * @param view 要执行动画的视图
     * @param minAlpha 最小透明度
     * @param duration 闪烁周期
     */
    fun twinkle(
        view: View,
        minAlpha: Float = 0.3f,
        duration: Long = 1500
    ): Animator {
        return ObjectAnimator.ofFloat(view, "alpha", 1f, minAlpha, 1f).apply {
            this.duration = duration
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
    
    /**
     * 组合动画：弹出并旋转
     * 
     * 效果：同时执行缩放、旋转和淡入
     * 使用场景：奖励出现、成就解锁
     * 
     * @param view 要执行动画的视图
     * @param duration 动画时长
     * @param rotation 旋转角度
     */
    fun popAndRotate(
        view: View,
        duration: Long = CHILD_FRIENDLY_DURATION,
        rotation: Float = 360f
    ) {
        view.apply {
            visibility = View.VISIBLE
            alpha = 0f
            scaleX = 0f
            scaleY = 0f
            this.rotation = 0f
        }
        
        val animatorSet = AnimatorSet()
        
        val alphaAnim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val scaleXAnim = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f)
        val scaleYAnim = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f)
        val rotationAnim = ObjectAnimator.ofFloat(view, "rotation", 0f, rotation)
        
        animatorSet.apply {
            playTogether(alphaAnim, scaleXAnim, scaleYAnim, rotationAnim)
            this.duration = duration
            interpolator = OvershootInterpolator()
            start()
        }
    }
    
    /**
     * 滑入动画
     * 
     * 效果：从指定方向滑入
     * 使用场景：页面切换、内容更新
     * 
     * @param view 要执行动画的视图
     * @param direction 滑入方向
     * @param duration 动画时长
     */
    fun slideIn(
        view: View,
        direction: SlideDirection,
        duration: Long = DEFAULT_DURATION
    ) {
        view.visibility = View.VISIBLE
        
        val translationProperty = when (direction) {
            SlideDirection.LEFT, SlideDirection.RIGHT -> "translationX"
            SlideDirection.TOP, SlideDirection.BOTTOM -> "translationY"
        }
        
        val startValue = when (direction) {
            SlideDirection.LEFT -> -view.width.toFloat()
            SlideDirection.RIGHT -> view.width.toFloat()
            SlideDirection.TOP -> -view.height.toFloat()
            SlideDirection.BOTTOM -> view.height.toFloat()
        }
        
        ViewCompat.setTranslationX(view, if (direction == SlideDirection.LEFT || direction == SlideDirection.RIGHT) startValue else 0f)
        ViewCompat.setTranslationY(view, if (direction == SlideDirection.TOP || direction == SlideDirection.BOTTOM) startValue else 0f)
        
        ObjectAnimator.ofFloat(view, translationProperty, startValue, 0f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            start()
        }
    }
    
    /**
     * 为多个视图依次执行动画
     * 
     * 效果：视图依次出现，形成波浪效果
     * 使用场景：列表项动画、多个元素展示
     * 
     * @param views 视图列表
     * @param animation 要执行的动画
     * @param delay 每个视图之间的延迟
     */
    fun animateSequentially(
        views: List<View>,
        animation: (View) -> Unit,
        delay: Long = 100
    ) {
        views.forEachIndexed { index, view ->
            view.postDelayed({
                animation(view)
            }, delay * index)
        }
    }
    
    /**
     * 停止视图的所有动画
     * 
     * @param view 要停止动画的视图
     */
    fun stopAnimation(view: View) {
        view.animate().cancel()
        view.clearAnimation()
    }
    
    /**
     * 滑动方向枚举
     */
    enum class SlideDirection {
        LEFT, RIGHT, TOP, BOTTOM
    }
}
package com.family.kidsedu

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log

/**
 * 音效管理器
 * 
 * 功能说明：
 * 1. 管理应用中的所有音效播放
 * 2. 提供简单的音效播放接口
 * 3. 处理音效资源的加载和释放
 * 4. 支持背景音乐和短音效
 * 
 * 音效类型：
 * - 点击音效
 * - 完成音效
 * - 奖励音效
 * - 背景音乐（可选）
 * 
 * 注意事项：
 * - 使用SoundPool播放短音效
 * - 使用MediaPlayer播放背景音乐
 * - 及时释放资源防止内存泄漏
 * 
 * @author AI启蒙时光开发团队
 * @since 2025-01-03
 */
class SoundManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SoundManager"
        
        /** 最大音效流数量 */
        private const val MAX_STREAMS = 5
        
        /** 音效类型 */
        const val SOUND_CLICK = "click"
        const val SOUND_SUCCESS = "success"
        const val SOUND_REWARD = "reward"
        const val SOUND_ERROR = "error"
        const val SOUND_TRANSITION = "transition"
    }
    
    /** SoundPool实例（用于短音效） */
    private val soundPool: SoundPool
    
    /** 音效ID映射 */
    private val soundMap = mutableMapOf<String, Int>()
    
    /** 背景音乐播放器 */
    private var backgroundMusic: MediaPlayer? = null
    
    /** 音效开关状态 */
    private var isSoundEnabled = true
    
    /** 音量设置（0.0f - 1.0f） */
    private var soundVolume = 0.7f
    private var musicVolume = 0.5f
    
    init {
        // 初始化SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(audioAttributes)
            .build()
        
        // 加载默认音效
        loadDefaultSounds()
    }
    
    /**
     * 加载默认音效
     * 
     * 注意：实际项目中需要添加真实的音效文件
     */
    private fun loadDefaultSounds() {
        try {
            // 使用占位音频文件，实际开发时替换为真实音效
            val clickSoundId = soundPool.load(context, R.raw.notification, 1)
            val successSoundId = soundPool.load(context, R.raw.notification, 1)
            val rewardSoundId = soundPool.load(context, R.raw.notification, 1)
            
            soundMap[SOUND_CLICK] = clickSoundId
            soundMap[SOUND_SUCCESS] = successSoundId
            soundMap[SOUND_REWARD] = rewardSoundId
            
            Log.d(TAG, "音效加载完成")
        } catch (e: Exception) {
            Log.e(TAG, "加载音效失败", e)
        }
    }
    
    /**
     * 播放音效
     * 
     * @param soundType 音效类型
     * @param loop 是否循环播放（0不循环，-1无限循环）
     */
    fun playSound(soundType: String, loop: Int = 0) {
        if (!isSoundEnabled) return
        
        soundMap[soundType]?.let { soundId ->
            try {
                soundPool.play(
                    soundId,
                    soundVolume,  // 左声道音量
                    soundVolume,  // 右声道音量
                    1,            // 优先级
                    loop,         // 循环次数
                    1.0f          // 播放速率
                )
            } catch (e: Exception) {
                Log.e(TAG, "播放音效失败: $soundType", e)
            }
        }
    }
    
    /**
     * 播放点击音效
     */
    fun playClickSound() {
        playSound(SOUND_CLICK)
    }
    
    /**
     * 播放成功音效
     */
    fun playSuccessSound() {
        playSound(SOUND_SUCCESS)
    }
    
    /**
     * 播放奖励音效
     */
    fun playRewardSound() {
        playSound(SOUND_REWARD)
    }
    
    /**
     * 播放背景音乐
     * 
     * @param musicResId 音乐资源ID
     * @param loop 是否循环
     */
    fun playBackgroundMusic(musicResId: Int, loop: Boolean = true) {
        if (!isSoundEnabled) return
        
        try {
            // 停止之前的背景音乐
            stopBackgroundMusic()
            
            backgroundMusic = MediaPlayer.create(context, musicResId).apply {
                isLooping = loop
                setVolume(musicVolume, musicVolume)
                
                setOnPreparedListener {
                    start()
                    Log.d(TAG, "背景音乐开始播放")
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "背景音乐播放错误: what=$what, extra=$extra")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放背景音乐失败", e)
        }
    }
    
    /**
     * 暂停背景音乐
     */
    fun pauseBackgroundMusic() {
        backgroundMusic?.apply {
            if (isPlaying) {
                pause()
            }
        }
    }
    
    /**
     * 恢复背景音乐
     */
    fun resumeBackgroundMusic() {
        backgroundMusic?.apply {
            if (!isPlaying) {
                start()
            }
        }
    }
    
    /**
     * 停止背景音乐
     */
    fun stopBackgroundMusic() {
        backgroundMusic?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        backgroundMusic = null
    }
    
    /**
     * 设置音效开关
     * 
     * @param enabled 是否启用音效
     */
    fun setSoundEnabled(enabled: Boolean) {
        isSoundEnabled = enabled
        
        if (!enabled) {
            // 关闭音效时停止所有声音
            soundPool.autoPause()
            pauseBackgroundMusic()
        } else {
            // 开启音效时恢复背景音乐
            resumeBackgroundMusic()
        }
    }
    
    /**
     * 设置音效音量
     * 
     * @param volume 音量（0.0f - 1.0f）
     */
    fun setSoundVolume(volume: Float) {
        soundVolume = volume.coerceIn(0.0f, 1.0f)
    }
    
    /**
     * 设置背景音乐音量
     * 
     * @param volume 音量（0.0f - 1.0f）
     */
    fun setMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0.0f, 1.0f)
        backgroundMusic?.setVolume(musicVolume, musicVolume)
    }
    
    /**
     * 释放所有音效资源
     * 必须在Activity的onDestroy中调用
     */
    fun release() {
        try {
            // 停止并释放背景音乐
            stopBackgroundMusic()
            
            // 释放SoundPool
            soundPool.release()
            
            // 清空音效映射
            soundMap.clear()
            
            Log.d(TAG, "音效资源已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放音效资源失败", e)
        }
    }
    
    /**
     * 获取音效设置信息
     * 
     * @return 设置信息
     */
    fun getSoundSettings(): SoundSettings {
        return SoundSettings(
            soundEnabled = isSoundEnabled,
            soundVolume = soundVolume,
            musicVolume = musicVolume
        )
    }
    
    /**
     * 应用音效设置
     * 
     * @param settings 音效设置
     */
    fun applySoundSettings(settings: SoundSettings) {
        setSoundEnabled(settings.soundEnabled)
        setSoundVolume(settings.soundVolume)
        setMusicVolume(settings.musicVolume)
    }
    
    /**
     * 音效设置数据类
     */
    data class SoundSettings(
        val soundEnabled: Boolean,
        val soundVolume: Float,
        val musicVolume: Float
    )
}
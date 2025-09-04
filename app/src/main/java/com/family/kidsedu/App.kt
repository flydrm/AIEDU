package com.family.kidsedu

import android.app.Application
import android.content.Context

/**
 * 应用程序类
 * 
 * 功能说明：
 * 1. 提供全局Context访问
 * 2. 初始化全局组件
 * 3. 管理应用生命周期
 * 
 * 注意事项：
 * - 需要在AndroidManifest.xml中声明
 * - 不要在这里做耗时操作
 * 
 * @author AI启蒙时光开发团队
 * @since 2025-01-03
 */
class App : Application() {
    
    companion object {
        /** 全局Context实例 */
        lateinit var context: Context
            private set
        
        /** 全局音效管理器 */
        lateinit var soundManager: SoundManager
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 保存全局Context
        context = applicationContext
        
        // 初始化进度管理器
        Progress.init(this)
        
        // 初始化音效管理器
        soundManager = SoundManager(this)
        
        // 清理过期缓存
        CacheManager(this).clearExpiredCache()
        
        // 初始化崩溃处理器（仅在Release版本启用）
        if (!BuildConfig.DEBUG) {
            CrashHandler.init(this)
        }
        
        // 初始化日志工具
        LogUtils.init(
            enableFileLog = BuildConfig.DEBUG,
            logFilePath = File(filesDir, "logs/app.log")
        )
    }
}
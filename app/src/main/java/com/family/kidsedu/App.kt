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
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 保存全局Context
        context = applicationContext
        
        // 初始化进度管理器
        Progress.init(this)
        
        // 其他全局初始化可以放在这里
        // 例如：日志配置、崩溃收集等
    }
}
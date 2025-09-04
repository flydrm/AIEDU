package com.family.kidsedu

import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

/**
 * 崩溃处理器
 * 
 * 功能说明：
 * 1. 捕获未处理的异常
 * 2. 记录崩溃日志到文件
 * 3. 收集设备信息
 * 4. 友好的崩溃处理
 * 
 * 使用方式：
 * 在Application的onCreate中初始化
 * 
 * 注意事项：
 * - 只在Release版本启用
 * - 日志文件定期清理
 * - 保护用户隐私
 * 
 * @author AI启蒙时光开发团队
 * @since 2025-01-03
 */
class CrashHandler private constructor(private val context: Context) : 
    Thread.UncaughtExceptionHandler {
    
    companion object {
        private const val TAG = "CrashHandler"
        
        /** 日志文件夹名 */
        private const val CRASH_LOG_DIR = "crash_logs"
        
        /** 日志文件前缀 */
        private const val CRASH_LOG_PREFIX = "crash_"
        
        /** 日志文件后缀 */
        private const val CRASH_LOG_SUFFIX = ".log"
        
        /** 最多保留的日志文件数 */
        private const val MAX_LOG_FILES = 10
        
        /** 单例实例 */
        @Volatile
        private var instance: CrashHandler? = null
        
        /**
         * 初始化崩溃处理器
         * 
         * @param context 应用上下文
         */
        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = CrashHandler(context.applicationContext)
                        instance?.setup()
                    }
                }
            }
        }
    }
    
    /** 系统默认的异常处理器 */
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    
    /** 日期格式化 */
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    /** 文件名日期格式化 */
    private val fileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    /**
     * 设置崩溃处理器
     */
    private fun setup() {
        // 保存系统默认处理器
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        // 设置自定义处理器
        Thread.setDefaultUncaughtExceptionHandler(this)
        
        // 清理旧日志
        cleanOldLogs()
        
        Log.d(TAG, "崩溃处理器已初始化")
    }
    
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        Log.e(TAG, "捕获到未处理异常", exception)
        
        // 处理异常
        if (!handleException(exception)) {
            // 如果自定义处理失败，使用系统默认处理
            defaultHandler?.uncaughtException(thread, exception)
        } else {
            try {
                // 给用户提示的时间
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                Log.e(TAG, "线程中断", e)
            }
            
            // 结束进程
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }
    
    /**
     * 处理异常
     * 
     * @param exception 异常对象
     * @return true表示处理成功
     */
    private fun handleException(exception: Throwable?): Boolean {
        if (exception == null) return false
        
        try {
            // 收集崩溃信息
            val crashInfo = collectCrashInfo(exception)
            
            // 保存到文件
            saveCrashLog(crashInfo)
            
            // 显示友好提示（可选）
            showCrashToast()
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "处理崩溃失败", e)
            return false
        }
    }
    
    /**
     * 收集崩溃信息
     * 
     * @param exception 异常对象
     * @return 崩溃信息字符串
     */
    private fun collectCrashInfo(exception: Throwable): String {
        val sb = StringBuilder()
        
        // 时间信息
        sb.appendLine("========== 崩溃信息 ==========")
        sb.appendLine("时间: ${dateFormat.format(Date())}")
        sb.appendLine()
        
        // 应用信息
        sb.appendLine("========== 应用信息 ==========")
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            sb.appendLine("应用包名: ${packageInfo.packageName}")
            sb.appendLine("版本名称: ${packageInfo.versionName}")
            sb.appendLine("版本代码: ${packageInfo.versionCode}")
        } catch (e: Exception) {
            sb.appendLine("获取应用信息失败: ${e.message}")
        }
        sb.appendLine()
        
        // 设备信息
        sb.appendLine("========== 设备信息 ==========")
        sb.appendLine("设备型号: ${Build.MODEL}")
        sb.appendLine("设备厂商: ${Build.MANUFACTURER}")
        sb.appendLine("系统版本: Android ${Build.VERSION.RELEASE}")
        sb.appendLine("API级别: ${Build.VERSION.SDK_INT}")
        sb.appendLine("CPU架构: ${Build.SUPPORTED_ABIS.joinToString()}")
        sb.appendLine()
        
        // 内存信息
        sb.appendLine("========== 内存信息 ==========")
        val runtime = Runtime.getRuntime()
        sb.appendLine("最大内存: ${runtime.maxMemory() / 1024 / 1024}MB")
        sb.appendLine("总内存: ${runtime.totalMemory() / 1024 / 1024}MB")
        sb.appendLine("空闲内存: ${runtime.freeMemory() / 1024 / 1024}MB")
        sb.appendLine()
        
        // 异常信息
        sb.appendLine("========== 异常信息 ==========")
        sb.appendLine("异常类型: ${exception.javaClass.name}")
        sb.appendLine("异常消息: ${exception.message}")
        sb.appendLine()
        
        // 堆栈信息
        sb.appendLine("========== 堆栈跟踪 ==========")
        val writer = StringWriter()
        val printWriter = PrintWriter(writer)
        exception.printStackTrace(printWriter)
        sb.append(writer.toString())
        printWriter.close()
        
        return sb.toString()
    }
    
    /**
     * 保存崩溃日志
     * 
     * @param crashInfo 崩溃信息
     */
    private fun saveCrashLog(crashInfo: String) {
        try {
            // 创建日志目录
            val logDir = File(context.filesDir, CRASH_LOG_DIR)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            // 创建日志文件
            val fileName = "$CRASH_LOG_PREFIX${fileNameFormat.format(Date())}$CRASH_LOG_SUFFIX"
            val logFile = File(logDir, fileName)
            
            // 写入文件
            logFile.writeText(crashInfo)
            
            Log.d(TAG, "崩溃日志已保存: ${logFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "保存崩溃日志失败", e)
        }
    }
    
    /**
     * 显示崩溃提示
     * 由于在崩溃时可能无法显示Toast，这里只是预留接口
     */
    private fun showCrashToast() {
        // 在崩溃时UI可能已经无法响应，所以这个方法可能不会生效
        // 可以考虑使用其他方式通知用户，如下次启动时提示
    }
    
    /**
     * 清理旧的日志文件
     */
    private fun cleanOldLogs() {
        try {
            val logDir = File(context.filesDir, CRASH_LOG_DIR)
            if (!logDir.exists()) return
            
            val logFiles = logDir.listFiles { file ->
                file.name.startsWith(CRASH_LOG_PREFIX) && file.name.endsWith(CRASH_LOG_SUFFIX)
            } ?: return
            
            // 按修改时间排序
            val sortedFiles = logFiles.sortedByDescending { it.lastModified() }
            
            // 删除超出数量的旧文件
            if (sortedFiles.size > MAX_LOG_FILES) {
                for (i in MAX_LOG_FILES until sortedFiles.size) {
                    sortedFiles[i].delete()
                    Log.d(TAG, "删除旧日志: ${sortedFiles[i].name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理日志失败", e)
        }
    }
    
    /**
     * 获取所有崩溃日志
     * 
     * @return 日志文件列表
     */
    fun getCrashLogs(): List<File> {
        val logDir = File(context.filesDir, CRASH_LOG_DIR)
        if (!logDir.exists()) return emptyList()
        
        return logDir.listFiles { file ->
            file.name.startsWith(CRASH_LOG_PREFIX) && file.name.endsWith(CRASH_LOG_SUFFIX)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    /**
     * 读取崩溃日志内容
     * 
     * @param logFile 日志文件
     * @return 日志内容
     */
    fun readCrashLog(logFile: File): String? {
        return try {
            logFile.readText()
        } catch (e: Exception) {
            Log.e(TAG, "读取日志失败: ${logFile.name}", e)
            null
        }
    }
    
    /**
     * 删除所有崩溃日志
     */
    fun clearAllLogs() {
        try {
            val logDir = File(context.filesDir, CRASH_LOG_DIR)
            if (logDir.exists()) {
                logDir.deleteRecursively()
                Log.d(TAG, "已清空所有崩溃日志")
            }
        } catch (e: Exception) {
            Log.e(TAG, "清空日志失败", e)
        }
    }
}
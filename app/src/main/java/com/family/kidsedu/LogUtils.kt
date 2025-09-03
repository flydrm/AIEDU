package com.family.kidsedu

import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日志工具类
 * 
 * 功能说明：
 * 1. 统一的日志输出接口
 * 2. 支持日志级别控制
 * 3. Release版本自动关闭详细日志
 * 4. 可选的文件日志记录
 * 
 * 使用方式：
 * LogUtils.d("MainActivity", "这是调试日志")
 * LogUtils.e("Network", "网络错误", exception)
 * 
 * @author AI启蒙时光开发团队
 * @since 2025-01-03
 */
object LogUtils {
    
    /** 是否是调试模式 */
    private val isDebug = BuildConfig.DEBUG
    
    /** 是否启用文件日志 */
    private var isFileLogEnabled = false
    
    /** 日志文件路径 */
    private var logFile: File? = null
    
    /** 日期格式化 */
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    /** 日志级别 */
    enum class Level(val value: Int) {
        VERBOSE(2),
        DEBUG(3),
        INFO(4),
        WARN(5),
        ERROR(6)
    }
    
    /** 当前日志级别 */
    private var currentLevel = if (isDebug) Level.VERBOSE else Level.INFO
    
    /**
     * 初始化日志工具
     * 
     * @param enableFileLog 是否启用文件日志
     * @param logFilePath 日志文件路径
     */
    fun init(enableFileLog: Boolean = false, logFilePath: File? = null) {
        isFileLogEnabled = enableFileLog
        logFile = logFilePath
        
        if (isFileLogEnabled && logFile != null) {
            // 确保日志目录存在
            logFile?.parentFile?.mkdirs()
        }
    }
    
    /**
     * 设置日志级别
     * 
     * @param level 日志级别
     */
    fun setLogLevel(level: Level) {
        currentLevel = level
    }
    
    /**
     * Verbose级别日志
     */
    fun v(tag: String, msg: String) {
        if (currentLevel.value <= Level.VERBOSE.value) {
            Log.v(tag, msg)
            writeToFile(Level.VERBOSE, tag, msg)
        }
    }
    
    /**
     * Debug级别日志
     */
    fun d(tag: String, msg: String) {
        if (currentLevel.value <= Level.DEBUG.value) {
            Log.d(tag, msg)
            writeToFile(Level.DEBUG, tag, msg)
        }
    }
    
    /**
     * Info级别日志
     */
    fun i(tag: String, msg: String) {
        if (currentLevel.value <= Level.INFO.value) {
            Log.i(tag, msg)
            writeToFile(Level.INFO, tag, msg)
        }
    }
    
    /**
     * Warning级别日志
     */
    fun w(tag: String, msg: String, throwable: Throwable? = null) {
        if (currentLevel.value <= Level.WARN.value) {
            if (throwable != null) {
                Log.w(tag, msg, throwable)
                writeToFile(Level.WARN, tag, "$msg\n${throwable.stackTraceToString()}")
            } else {
                Log.w(tag, msg)
                writeToFile(Level.WARN, tag, msg)
            }
        }
    }
    
    /**
     * Error级别日志
     */
    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        if (currentLevel.value <= Level.ERROR.value) {
            if (throwable != null) {
                Log.e(tag, msg, throwable)
                writeToFile(Level.ERROR, tag, "$msg\n${throwable.stackTraceToString()}")
            } else {
                Log.e(tag, msg)
                writeToFile(Level.ERROR, tag, msg)
            }
        }
    }
    
    /**
     * 写入文件日志
     * 
     * @param level 日志级别
     * @param tag 标签
     * @param msg 消息
     */
    private fun writeToFile(level: Level, tag: String, msg: String) {
        if (!isFileLogEnabled || logFile == null) return
        
        try {
            val timestamp = dateFormat.format(Date())
            val logLine = "$timestamp ${level.name}/$tag: $msg\n"
            
            // 使用追加模式写入
            FileWriter(logFile, true).use { writer ->
                writer.write(logLine)
            }
        } catch (e: Exception) {
            // 写入文件失败，但不应该影响正常日志
            Log.e("LogUtils", "写入日志文件失败", e)
        }
    }
    
    /**
     * 记录方法执行时间
     * 
     * @param tag 标签
     * @param methodName 方法名
     * @param block 要执行的代码块
     * @return 代码块的返回值
     */
    inline fun <T> logTime(tag: String, methodName: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        d(tag, "$methodName 执行耗时: ${duration}ms")
        return result
    }
    
    /**
     * 记录内存使用
     * 
     * @param tag 标签
     * @param message 附加消息
     */
    fun logMemory(tag: String, message: String = "") {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        val usedMemory = totalMemory - freeMemory
        
        val memInfo = "内存使用: $usedMemory/$maxMemory MB (${(usedMemory * 100 / maxMemory)}%)"
        d(tag, if (message.isNotEmpty()) "$message - $memInfo" else memInfo)
    }
    
    /**
     * 清空日志文件
     */
    fun clearLogFile() {
        try {
            logFile?.writeText("")
            d("LogUtils", "日志文件已清空")
        } catch (e: Exception) {
            e("LogUtils", "清空日志文件失败", e)
        }
    }
    
    /**
     * 获取日志文件大小
     * 
     * @return 文件大小（字节）
     */
    fun getLogFileSize(): Long {
        return logFile?.length() ?: 0
    }
    
    /**
     * 压缩并导出日志文件
     * 
     * @param outputFile 输出文件
     * @return 是否成功
     */
    fun exportLogs(outputFile: File): Boolean {
        return try {
            logFile?.copyTo(outputFile, overwrite = true)
            true
        } catch (e: Exception) {
            e("LogUtils", "导出日志失败", e)
            false
        }
    }
}
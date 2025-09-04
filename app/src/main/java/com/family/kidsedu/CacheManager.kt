package com.family.kidsedu

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit

/**
 * AI内容缓存管理器
 * 
 * 功能说明：
 * 1. 缓存AI生成的内容，减少API调用
 * 2. 管理缓存过期时间
 * 3. 提供缓存的增删改查功能
 * 4. 支持不同类型的缓存数据
 * 
 * 实现原理：
 * - 使用SharedPreferences存储JSON格式的缓存数据
 * - 每个缓存项都有时间戳，用于判断是否过期
 * - 支持设置不同的过期时间
 * 
 * 使用场景：
 * - 缓存AI生成的教育内容
 * - 缓存鼓励语列表
 * - 缓存学习建议
 * 
 * @author AI启蒙时光开发团队
 * @since 2025-01-03
 */
class CacheManager(context: Context) {
    
    companion object {
        private const val PREF_NAME = "ai_cache"
        private const val TAG = "CacheManager"
        
        /** 默认缓存有效期（天） */
        private const val DEFAULT_CACHE_DAYS = 7
        
        /** 缓存类型前缀 */
        private const val PREFIX_CONTENT = "content_"
        private const val PREFIX_ENCOURAGEMENT = "encouragement_"
        private const val PREFIX_SUGGESTION = "suggestion_"
    }
    
    /** SharedPreferences实例 */
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    /** JSON解析器 */
    private val gson = Gson()
    
    /**
     * 缓存数据类
     * 
     * @param data 缓存的数据
     * @param timestamp 缓存时间戳
     * @param expireDays 过期天数
     */
    data class CacheItem<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
        val expireDays: Int = DEFAULT_CACHE_DAYS
    ) {
        /**
         * 检查缓存是否过期
         * 
         * @return true表示已过期
         */
        fun isExpired(): Boolean {
            val expireTime = timestamp + TimeUnit.DAYS.toMillis(expireDays.toLong())
            return System.currentTimeMillis() > expireTime
        }
        
        /**
         * 获取剩余有效时间（小时）
         * 
         * @return 剩余小时数，如果已过期返回0
         */
        fun getRemainingHours(): Long {
            val expireTime = timestamp + TimeUnit.DAYS.toMillis(expireDays.toLong())
            val remaining = expireTime - System.currentTimeMillis()
            return if (remaining > 0) {
                TimeUnit.MILLISECONDS.toHours(remaining)
            } else {
                0
            }
        }
    }
    
    /**
     * 保存内容到缓存
     * 
     * @param key 缓存键
     * @param content 要缓存的内容
     * @param expireDays 过期天数，默认7天
     */
    fun saveContent(key: String, content: String, expireDays: Int = DEFAULT_CACHE_DAYS) {
        val cacheItem = CacheItem(
            data = content,
            expireDays = expireDays
        )
        
        val json = gson.toJson(cacheItem)
        prefs.edit()
            .putString("$PREFIX_CONTENT$key", json)
            .apply()
    }
    
    /**
     * 从缓存获取内容
     * 
     * @param key 缓存键
     * @return 缓存的内容，如果不存在或已过期返回null
     */
    fun getContent(key: String): String? {
        val json = prefs.getString("$PREFIX_CONTENT$key", null) ?: return null
        
        return try {
            val type = object : TypeToken<CacheItem<String>>() {}.type
            val cacheItem: CacheItem<String> = gson.fromJson(json, type)
            
            if (cacheItem.isExpired()) {
                // 缓存已过期，删除并返回null
                removeContent(key)
                null
            } else {
                cacheItem.data
            }
        } catch (e: Exception) {
            // 解析失败，删除损坏的缓存
            removeContent(key)
            null
        }
    }
    
    /**
     * 删除指定的缓存内容
     * 
     * @param key 缓存键
     */
    fun removeContent(key: String) {
        prefs.edit()
            .remove("$PREFIX_CONTENT$key")
            .apply()
    }
    
    /**
     * 保存鼓励语列表到缓存
     * 
     * @param encouragements 鼓励语列表
     * @param expireDays 过期天数
     */
    fun saveEncouragements(encouragements: List<String>, expireDays: Int = DEFAULT_CACHE_DAYS) {
        val cacheItem = CacheItem(
            data = encouragements,
            expireDays = expireDays
        )
        
        val json = gson.toJson(cacheItem)
        prefs.edit()
            .putString("${PREFIX_ENCOURAGEMENT}list", json)
            .apply()
    }
    
    /**
     * 获取缓存的鼓励语列表
     * 
     * @return 鼓励语列表，如果不存在或已过期返回null
     */
    fun getEncouragements(): List<String>? {
        val json = prefs.getString("${PREFIX_ENCOURAGEMENT}list", null) ?: return null
        
        return try {
            val type = object : TypeToken<CacheItem<List<String>>>() {}.type
            val cacheItem: CacheItem<List<String>> = gson.fromJson(json, type)
            
            if (cacheItem.isExpired()) {
                prefs.edit().remove("${PREFIX_ENCOURAGEMENT}list").apply()
                null
            } else {
                cacheItem.data
            }
        } catch (e: Exception) {
            prefs.edit().remove("${PREFIX_ENCOURAGEMENT}list").apply()
            null
        }
    }
    
    /**
     * 清除所有缓存
     * 
     * 使用场景：
     * - 用户手动清理缓存
     * - 版本更新后清理旧缓存
     */
    fun clearAllCache() {
        prefs.edit().clear().apply()
    }
    
    /**
     * 清除过期的缓存
     * 
     * 遍历所有缓存项，删除已过期的内容
     * 建议在应用启动时调用
     */
    fun clearExpiredCache() {
        val editor = prefs.edit()
        var hasChanges = false
        
        prefs.all.forEach { (key, value) ->
            if (value is String && (key.startsWith(PREFIX_CONTENT) || 
                key.startsWith(PREFIX_ENCOURAGEMENT) || 
                key.startsWith(PREFIX_SUGGESTION))) {
                
                try {
                    // 尝试解析为CacheItem
                    val type = object : TypeToken<CacheItem<Any>>() {}.type
                    val cacheItem: CacheItem<Any> = gson.fromJson(value, type)
                    
                    if (cacheItem.isExpired()) {
                        editor.remove(key)
                        hasChanges = true
                    }
                } catch (e: Exception) {
                    // 解析失败，删除损坏的缓存
                    editor.remove(key)
                    hasChanges = true
                }
            }
        }
        
        if (hasChanges) {
            editor.apply()
        }
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计信息
     */
    fun getCacheStatistics(): CacheStatistics {
        var totalCount = 0
        var expiredCount = 0
        var contentCount = 0
        var encouragementCount = 0
        var totalSize = 0L
        
        prefs.all.forEach { (key, value) ->
            if (value is String && (key.startsWith(PREFIX_CONTENT) || 
                key.startsWith(PREFIX_ENCOURAGEMENT) || 
                key.startsWith(PREFIX_SUGGESTION))) {
                
                totalCount++
                totalSize += value.length
                
                when {
                    key.startsWith(PREFIX_CONTENT) -> contentCount++
                    key.startsWith(PREFIX_ENCOURAGEMENT) -> encouragementCount++
                }
                
                try {
                    val type = object : TypeToken<CacheItem<Any>>() {}.type
                    val cacheItem: CacheItem<Any> = gson.fromJson(value, type)
                    if (cacheItem.isExpired()) {
                        expiredCount++
                    }
                } catch (e: Exception) {
                    expiredCount++
                }
            }
        }
        
        return CacheStatistics(
            totalCount = totalCount,
            expiredCount = expiredCount,
            contentCount = contentCount,
            encouragementCount = encouragementCount,
            totalSizeBytes = totalSize
        )
    }
    
    /**
     * 缓存统计信息
     */
    data class CacheStatistics(
        val totalCount: Int,
        val expiredCount: Int,
        val contentCount: Int,
        val encouragementCount: Int,
        val totalSizeBytes: Long
    ) {
        /**
         * 获取格式化的大小字符串
         */
        fun getFormattedSize(): String {
            return when {
                totalSizeBytes < 1024 -> "$totalSizeBytes B"
                totalSizeBytes < 1024 * 1024 -> "${totalSizeBytes / 1024} KB"
                else -> "${totalSizeBytes / (1024 * 1024)} MB"
            }
        }
    }
}
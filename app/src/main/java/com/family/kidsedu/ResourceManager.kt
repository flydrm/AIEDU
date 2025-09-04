package com.family.kidsedu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import java.lang.ref.WeakReference

/**
 * 资源管理器
 * 
 * 功能说明：
 * 1. 管理图片资源的加载和缓存
 * 2. 优化内存使用
 * 3. 防止内存泄漏
 * 4. 提供资源释放机制
 * 
 * 优化策略：
 * - 使用LruCache缓存常用图片
 * - 自动缩放大图片
 * - 及时释放不用的资源
 * - 使用弱引用防止泄漏
 * 
 * @author AI启蒙时光开发团队
 * @since 2025-01-03
 */
class ResourceManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ResourceManager"
        
        /** 最大内存使用比例 */
        private const val MAX_MEMORY_RATIO = 0.25f // 使用25%的可用内存
        
        /** 默认采样率 */
        private const val DEFAULT_SAMPLE_SIZE = 2
        
        /** 目标图片最大尺寸 */
        private const val MAX_IMAGE_SIZE = 1024 // 像素
    }
    
    /** 图片缓存 */
    private val imageCache: LruCache<Int, Bitmap>
    
    /** 弱引用缓存（用于二级缓存） */
    private val weakCache = mutableMapOf<Int, WeakReference<Bitmap>>()
    
    /** 内存信息 */
    private val runtime = Runtime.getRuntime()
    
    init {
        // 计算缓存大小
        val maxMemory = (runtime.maxMemory() / 1024).toInt() // KB
        val cacheSize = (maxMemory * MAX_MEMORY_RATIO).toInt()
        
        Log.d(TAG, "最大内存: ${maxMemory}KB, 缓存大小: ${cacheSize}KB")
        
        // 初始化LruCache
        imageCache = object : LruCache<Int, Bitmap>(cacheSize) {
            override fun sizeOf(key: Int, bitmap: Bitmap): Int {
                // 返回图片占用的KB数
                return bitmap.byteCount / 1024
            }
            
            override fun entryRemoved(
                evicted: Boolean,
                key: Int,
                oldValue: Bitmap,
                newValue: Bitmap?
            ) {
                // 被移除时放入弱引用缓存
                if (evicted && oldValue != null) {
                    weakCache[key] = WeakReference(oldValue)
                }
            }
        }
    }
    
    /**
     * 加载图片资源
     * 
     * @param resId 资源ID
     * @param reqWidth 需要的宽度（0表示不限制）
     * @param reqHeight 需要的高度（0表示不限制）
     * @return 加载的图片，失败返回null
     */
    fun loadBitmap(resId: Int, reqWidth: Int = 0, reqHeight: Int = 0): Bitmap? {
        // 1. 先从强引用缓存查找
        imageCache.get(resId)?.let {
            Log.d(TAG, "从LruCache加载图片: $resId")
            return it
        }
        
        // 2. 从弱引用缓存查找
        weakCache[resId]?.get()?.let { bitmap ->
            if (!bitmap.isRecycled) {
                Log.d(TAG, "从弱引用缓存加载图片: $resId")
                // 重新放入强引用缓存
                imageCache.put(resId, bitmap)
                return bitmap
            } else {
                // 已回收，移除弱引用
                weakCache.remove(resId)
            }
        }
        
        // 3. 从资源加载
        return try {
            val bitmap = if (reqWidth > 0 && reqHeight > 0) {
                decodeSampledBitmapFromResource(resId, reqWidth, reqHeight)
            } else {
                BitmapFactory.decodeResource(context.resources, resId)
            }
            
            bitmap?.let {
                // 放入缓存
                imageCache.put(resId, it)
                Log.d(TAG, "从资源加载图片: $resId, 大小: ${it.byteCount / 1024}KB")
            }
            
            bitmap
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "加载图片内存不足: $resId", e)
            // 清理缓存后重试
            clearCache()
            null
        } catch (e: Exception) {
            Log.e(TAG, "加载图片失败: $resId", e)
            null
        }
    }
    
    /**
     * 高效加载指定尺寸的图片
     * 
     * @param resId 资源ID
     * @param reqWidth 需要的宽度
     * @param reqHeight 需要的高度
     * @return 缩放后的图片
     */
    private fun decodeSampledBitmapFromResource(
        resId: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        // 第一次解析：获取图片尺寸
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeResource(context.resources, resId, options)
        
        // 计算采样率
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        
        // 第二次解析：加载缩放后的图片
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeResource(context.resources, resId, options)
    }
    
    /**
     * 计算图片采样率
     * 
     * @param options 图片选项
     * @param reqWidth 需要的宽度
     * @param reqHeight 需要的高度
     * @return 采样率
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            // 计算最大的采样率，保证缩放后的尺寸大于需要的尺寸
            while ((halfHeight / inSampleSize) >= reqHeight &&
                   (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        Log.d(TAG, "图片原始尺寸: ${width}x${height}, 采样率: $inSampleSize")
        return inSampleSize
    }
    
    /**
     * 预加载图片
     * 
     * @param resIds 要预加载的资源ID列表
     */
    fun preloadImages(resIds: List<Int>) {
        resIds.forEach { resId ->
            loadBitmap(resId, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)
        }
    }
    
    /**
     * 清理缓存
     * 
     * @param level 清理级别（0-轻度，1-中度，2-重度）
     */
    fun clearCache(level: Int = 0) {
        when (level) {
            0 -> {
                // 轻度清理：只清理弱引用
                weakCache.clear()
                Log.d(TAG, "清理弱引用缓存")
            }
            1 -> {
                // 中度清理：清理部分LruCache
                imageCache.trimToSize(imageCache.size() / 2)
                weakCache.clear()
                Log.d(TAG, "清理50%缓存")
            }
            2 -> {
                // 重度清理：清理所有缓存
                imageCache.evictAll()
                weakCache.clear()
                System.gc()
                Log.d(TAG, "清理所有缓存")
            }
        }
    }
    
    /**
     * 获取内存使用情况
     * 
     * @return 内存统计信息
     */
    fun getMemoryInfo(): MemoryInfo {
        val maxMemory = runtime.maxMemory() / 1024 / 1024 // MB
        val totalMemory = runtime.totalMemory() / 1024 / 1024 // MB
        val freeMemory = runtime.freeMemory() / 1024 / 1024 // MB
        val usedMemory = totalMemory - freeMemory
        
        val cacheSize = imageCache.size() / 1024 // MB
        val cacheMaxSize = imageCache.maxSize() / 1024 // MB
        
        return MemoryInfo(
            maxMemory = maxMemory,
            totalMemory = totalMemory,
            freeMemory = freeMemory,
            usedMemory = usedMemory,
            cacheSize = cacheSize,
            cacheMaxSize = cacheMaxSize,
            cacheCount = imageCache.snapshot().size,
            weakCacheCount = weakCache.size
        )
    }
    
    /**
     * 检查内存压力
     * 
     * @return true表示内存紧张
     */
    fun isLowMemory(): Boolean {
        val memoryInfo = getMemoryInfo()
        val memoryUsageRatio = memoryInfo.usedMemory.toFloat() / memoryInfo.maxMemory
        return memoryUsageRatio > 0.8f // 使用超过80%认为内存紧张
    }
    
    /**
     * 释放指定图片资源
     * 
     * @param resId 资源ID
     */
    fun releaseBitmap(resId: Int) {
        imageCache.remove(resId)
        weakCache.remove(resId)
    }
    
    /**
     * 释放所有资源
     * 在Activity销毁时调用
     */
    fun release() {
        clearCache(2)
        Log.d(TAG, "资源管理器已释放")
    }
    
    /**
     * 内存信息数据类
     */
    data class MemoryInfo(
        val maxMemory: Long,      // 最大内存(MB)
        val totalMemory: Long,    // 总内存(MB)
        val freeMemory: Long,     // 空闲内存(MB)
        val usedMemory: Long,     // 已用内存(MB)
        val cacheSize: Int,       // 缓存大小(MB)
        val cacheMaxSize: Int,    // 缓存最大值(MB)
        val cacheCount: Int,      // 缓存图片数量
        val weakCacheCount: Int   // 弱引用缓存数量
    ) {
        /**
         * 获取内存使用百分比
         */
        fun getMemoryUsagePercent(): Int {
            return ((usedMemory.toFloat() / maxMemory) * 100).toInt()
        }
        
        /**
         * 获取缓存使用百分比
         */
        fun getCacheUsagePercent(): Int {
            return if (cacheMaxSize > 0) {
                ((cacheSize.toFloat() / cacheMaxSize) * 100).toInt()
            } else {
                0
            }
        }
        
        /**
         * 格式化为字符串
         */
        override fun toString(): String {
            return """
                内存信息:
                - 最大内存: ${maxMemory}MB
                - 已用内存: ${usedMemory}MB (${getMemoryUsagePercent()}%)
                - 缓存大小: ${cacheSize}MB (${getCacheUsagePercent()}%)
                - 缓存数量: $cacheCount (强引用) + $weakCacheCount (弱引用)
            """.trimIndent()
        }
    }
}
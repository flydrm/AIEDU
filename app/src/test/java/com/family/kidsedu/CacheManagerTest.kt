package com.family.kidsedu

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * CacheManager缓存管理器单元测试
 * 
 * 功能说明：
 * 测试缓存的保存、读取、过期等功能
 * 
 * @author AI启蒙时光开发团队
 * @since 2025-01-03
 */
class CacheManagerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    private lateinit var cacheManager: CacheManager
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // 设置mock行为
        `when`(mockContext.getSharedPreferences(anyString(), anyInt()))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.remove(anyString())).thenReturn(mockEditor)
        
        // 创建缓存管理器
        cacheManager = CacheManager(mockContext)
    }
    
    /**
     * 测试缓存项创建
     */
    @Test
    fun testCacheItemCreation() {
        val cacheItem = CacheManager.CacheItem(
            data = "测试数据",
            timestamp = System.currentTimeMillis(),
            expireDays = 7
        )
        
        assertEquals("测试数据", cacheItem.data)
        assertEquals(7, cacheItem.expireDays)
        assertFalse(cacheItem.isExpired())
    }
    
    /**
     * 测试缓存过期判断
     */
    @Test
    fun testCacheExpiration() {
        // 创建一个已过期的缓存项（8天前）
        val expiredTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(8)
        val expiredItem = CacheManager.CacheItem(
            data = "过期数据",
            timestamp = expiredTimestamp,
            expireDays = 7
        )
        
        assertTrue(expiredItem.isExpired())
        assertEquals(0, expiredItem.getRemainingHours())
        
        // 创建一个未过期的缓存项（1天前）
        val validTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        val validItem = CacheManager.CacheItem(
            data = "有效数据",
            timestamp = validTimestamp,
            expireDays = 7
        )
        
        assertFalse(validItem.isExpired())
        assertTrue(validItem.getRemainingHours() > 0)
    }
    
    /**
     * 测试内容保存
     */
    @Test
    fun testSaveContent() {
        // 保存内容
        cacheManager.saveContent("test_key", "测试内容", 7)
        
        // 验证调用
        verify(mockEditor).putString(
            eq("content_test_key"),
            anyString()
        )
        verify(mockEditor).apply()
    }
    
    /**
     * 测试内容读取
     */
    @Test
    fun testGetContent() {
        // 模拟缓存的JSON数据
        val validJson = """
            {
                "data": "测试内容",
                "timestamp": ${System.currentTimeMillis()},
                "expireDays": 7
            }
        """.trimIndent()
        
        `when`(mockSharedPreferences.getString("content_test_key", null))
            .thenReturn(validJson)
        
        // 读取内容
        val content = cacheManager.getContent("test_key")
        assertEquals("测试内容", content)
    }
    
    /**
     * 测试过期内容返回null
     */
    @Test
    fun testGetExpiredContent() {
        // 模拟过期的缓存数据
        val expiredTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(8)
        val expiredJson = """
            {
                "data": "过期内容",
                "timestamp": $expiredTimestamp,
                "expireDays": 7
            }
        """.trimIndent()
        
        `when`(mockSharedPreferences.getString("content_test_key", null))
            .thenReturn(expiredJson)
        
        // 读取内容应该返回null
        val content = cacheManager.getContent("test_key")
        assertNull(content)
        
        // 验证删除了过期缓存
        verify(mockEditor).remove("content_test_key")
        verify(mockEditor).apply()
    }
    
    /**
     * 测试缓存统计
     */
    @Test
    fun testCacheStatistics() {
        // 模拟缓存数据
        val cacheData = mapOf(
            "content_key1" to createCacheJson("内容1", false),
            "content_key2" to createCacheJson("内容2", false),
            "content_key3" to createCacheJson("内容3", true), // 过期
            "encouragement_list" to createCacheJson(listOf("鼓励1", "鼓励2"), false)
        )
        
        `when`(mockSharedPreferences.all).thenReturn(cacheData)
        
        // 获取统计信息
        val stats = cacheManager.getCacheStatistics()
        
        assertEquals(4, stats.totalCount)
        assertEquals(1, stats.expiredCount)
        assertEquals(3, stats.contentCount)
        assertEquals(1, stats.encouragementCount)
        assertTrue(stats.totalSizeBytes > 0)
    }
    
    /**
     * 创建缓存JSON（辅助方法）
     */
    private fun createCacheJson(data: Any, expired: Boolean): String {
        val timestamp = if (expired) {
            System.currentTimeMillis() - TimeUnit.DAYS.toMillis(8)
        } else {
            System.currentTimeMillis()
        }
        
        return """
            {
                "data": ${if (data is String) "\"$data\"" else data},
                "timestamp": $timestamp,
                "expireDays": 7
            }
        """.trimIndent()
    }
}
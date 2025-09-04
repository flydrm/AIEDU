package com.family.kidsedu

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Progress进度管理单元测试
 * 
 * 功能说明：
 * 测试Progress对象的各种功能
 * 
 * 注意：这是一个简化的测试示例
 * 实际测试需要使用Robolectric或AndroidX Test
 * 
 * @author AI启蒙时光开发团队
 * @since 2025-01-03
 */
class ProgressTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // 设置mock行为
        `when`(mockContext.getSharedPreferences(anyString(), anyInt()))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
    }
    
    /**
     * 测试进度保存和读取
     */
    @Test
    fun testProgressSaveAndLoad() {
        // 模拟返回值
        `when`(mockSharedPreferences.getInt("current_card", 0)).thenReturn(5)
        
        // 初始化Progress
        Progress.init(mockContext)
        
        // 验证读取
        val cardIndex = Progress.getCurrentCardIndex()
        assertEquals(5, cardIndex)
        
        // 验证保存
        Progress.saveCurrentCardIndex(10)
        verify(mockEditor).putInt("current_card", 10)
        verify(mockEditor).apply()
    }
    
    /**
     * 测试今日学习时长
     */
    @Test
    fun testTodayPlayTime() {
        // 模拟今日时间为30分钟
        val todayKey = "play_time_${getCurrentDateString()}"
        `when`(mockSharedPreferences.getInt(todayKey, 0)).thenReturn(30)
        
        Progress.init(mockContext)
        
        // 验证读取
        val playTime = Progress.getTodayPlayTime()
        assertEquals(30, playTime)
        
        // 验证时长限制检查
        assertTrue(Progress.hasReachedDailyLimit(20))
        assertFalse(Progress.hasReachedDailyLimit(40))
    }
    
    /**
     * 测试卡片完成状态
     */
    @Test
    fun testCardCompletion() {
        // 模拟已完成的卡片
        `when`(mockSharedPreferences.getString("completed_cards", ""))
            .thenReturn("1,3,5")
        
        Progress.init(mockContext)
        
        // 验证获取已完成卡片
        val completedCards = Progress.getCompletedCards()
        assertEquals(3, completedCards.size)
        assertTrue(completedCards.contains(1))
        assertTrue(completedCards.contains(3))
        assertTrue(completedCards.contains(5))
        
        // 验证单个卡片状态
        assertTrue(Progress.isCardCompleted(1))
        assertFalse(Progress.isCardCompleted(2))
    }
    
    /**
     * 测试进度百分比计算
     */
    @Test
    fun testProgressPercentage() {
        // 模拟5张卡片完成
        `when`(mockSharedPreferences.getString("completed_cards", ""))
            .thenReturn("1,2,3,4,5")
        
        Progress.init(mockContext)
        
        // 总共20张卡片，完成5张
        val percentage = Progress.getProgressPercentage(20)
        assertEquals(25, percentage)
        
        // 边界情况：0张卡片
        val zeroPercentage = Progress.getProgressPercentage(0)
        assertEquals(0, zeroPercentage)
    }
    
    /**
     * 获取当前日期字符串（辅助方法）
     */
    private fun getCurrentDateString(): String {
        return java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
}
package com.family.kidsedu

import org.junit.Assert.*
import org.junit.Test

/**
 * Card数据模型单元测试
 * 
 * 功能说明：
 * 测试Card类的各种功能和边界情况
 * 
 * @author AI启蒙时光开发团队
 * @since 2025-01-03
 */
class CardTest {
    
    /**
     * 测试Card创建
     */
    @Test
    fun testCardCreation() {
        // 创建测试卡片
        val card = Card(
            id = 1,
            title = "测试卡片",
            imageResId = 123,
            audioResId = 456,
            category = "测试",
            textContent = "这是测试内容"
        )
        
        // 验证属性
        assertEquals(1, card.id)
        assertEquals("测试卡片", card.title)
        assertEquals(123, card.imageResId)
        assertEquals(456, card.audioResId)
        assertEquals("测试", card.category)
        assertEquals("这是测试内容", card.textContent)
    }
    
    /**
     * 测试默认值
     */
    @Test
    fun testDefaultValues() {
        val card = Card(
            id = 1,
            title = "测试",
            imageResId = 123,
            audioResId = 456
        )
        
        // 验证默认值
        assertEquals("tap", card.interactionType)
        assertNull(card.interactionTarget)
        assertFalse(card.aiGenerated)
        assertNull(card.textContent)
        assertEquals("通识", card.category)
        assertEquals(3, card.recommendAge)
        assertEquals(30, card.duration)
    }
    
    /**
     * 测试资源验证
     */
    @Test
    fun testResourceValidation() {
        // 有效资源
        val validCard = Card(
            id = 1,
            title = "有效卡片",
            imageResId = 123,
            audioResId = 456
        )
        assertTrue(validCard.isResourceValid())
        
        // 无效资源 - 缺少图片
        val invalidCard1 = Card(
            id = 2,
            title = "无效卡片1",
            imageResId = 0,
            audioResId = 456
        )
        assertFalse(invalidCard1.isResourceValid())
        
        // 无效资源 - 缺少音频
        val invalidCard2 = Card(
            id = 3,
            title = "无效卡片2",
            imageResId = 123,
            audioResId = 0
        )
        assertFalse(invalidCard2.isResourceValid())
    }
    
    /**
     * 测试内容描述
     */
    @Test
    fun testContentDescription() {
        val card = Card(
            id = 1,
            title = "红色消防车",
            imageResId = 123,
            audioResId = 456,
            recommendAge = 3
        )
        
        val description = card.getContentDescription()
        assertTrue(description.contains("红色消防车"))
        assertTrue(description.contains("3岁"))
    }
}
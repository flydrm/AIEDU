package com.family.kidsedu

/**
 * 教育卡片数据模型
 * 
 * 功能说明：
 * 表示一张教育卡片的所有信息，包括标题、图片、音频等资源
 * 
 * 使用场景：
 * - 卡片列表展示
 * - 卡片内容播放
 * - 进度记录
 * 
 * @author AI启蒙时光开发团队
 * @since 2025-01-03
 */
data class Card(
    /** 卡片唯一标识 */
    val id: Int,
    
    /** 卡片标题，如"红色消防车"、"数字1"等 */
    val title: String,
    
    /** 卡片图片资源ID，指向drawable中的图片 */
    val imageResId: Int,
    
    /** 卡片音频资源ID，指向raw中的音频文件 */
    val audioResId: Int,
    
    /** 
     * 互动类型
     * - "tap": 点击互动（默认）
     * - "drag": 拖拽互动（后续版本）
     * - "voice": 语音互动（后续版本）
     */
    val interactionType: String = "tap",
    
    /** 
     * 互动目标
     * 例如："red_car" 表示点击红色汽车
     * null 表示点击任意位置即可
     */
    val interactionTarget: String? = null,
    
    /** 是否为AI生成的内容 */
    val aiGenerated: Boolean = false,
    
    /** AI生成的文本内容（如果有） */
    val textContent: String? = null,
    
    /** 卡片主题分类，如"动物"、"数字"、"颜色"等 */
    val category: String = "通识",
    
    /** 建议学习年龄 */
    val recommendAge: Int = 3,
    
    /** 学习时长（秒） */
    val duration: Int = 30
) {
    
    /**
     * 检查卡片资源是否有效
     * 
     * @return 资源是否完整
     */
    fun isResourceValid(): Boolean {
        // 基础验证：必须有图片和音频
        return imageResId != 0 && audioResId != 0
    }
    
    /**
     * 获取卡片的显示描述
     * 用于无障碍功能
     */
    fun getContentDescription(): String {
        return "教育卡片：$title，适合${recommendAge}岁儿童学习"
    }
}
package com.family.kidsedu

/**
 * 教育卡片数据提供者
 * 
 * 功能说明：
 * 提供预定义的教育卡片数据，包含20-30张适合3岁儿童的学习卡片
 * 
 * 内容分类：
 * 1. 交通工具（特别是红色消防车）
 * 2. 数字认知（1-10）
 * 3. 颜色认知（重点红色）
 * 4. 动物认知
 * 5. 日常物品
 * 6. 勇敢品质
 * 
 * 注意事项：
 * - 所有内容都经过儿童适宜性审核
 * - 文本简单易懂，适合3岁儿童
 * - 图片和音频资源需要对应添加
 * 
 * @author AI启蒙时光开发团队
 * @since 2025-01-03
 */
object CardDataProvider {
    
    /**
     * 获取所有教育卡片数据
     * 
     * @return 教育卡片列表
     */
    fun getAllCards(): List<Card> {
        return listOf(
            // ========== 交通工具类 ==========
            Card(
                id = 1,
                title = "红色消防车",
                imageResId = R.drawable.ic_launcher_foreground, // TODO: 替换为真实图片
                audioResId = R.raw.notification, // TODO: 替换为真实音频
                category = "交通工具",
                textContent = "这是红色消防车。它会救火。消防员叔叔很勇敢。你看到消防车了吗？",
                recommendAge = 3,
                duration = 25
            ),
            
            Card(
                id = 2,
                title = "警车",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "交通工具",
                textContent = "警车会呜呜叫。警察叔叔保护我们。警车跑得很快。你喜欢警车吗？",
                recommendAge = 3,
                duration = 25
            ),
            
            Card(
                id = 3,
                title = "救护车",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "交通工具",
                textContent = "白色救护车。医生阿姨很温柔。救护车救助病人。听到救护车要让路哦！",
                recommendAge = 3,
                duration = 25
            ),
            
            // ========== 数字认知类 ==========
            Card(
                id = 4,
                title = "数字1",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "数字",
                textContent = "这是数字1。像一根棍子。我们有1个鼻子。你会数到1吗？",
                recommendAge = 3,
                duration = 20
            ),
            
            Card(
                id = 5,
                title = "数字2",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "数字",
                textContent = "这是数字2。像小鸭子。我们有2只手。来数一数吧！",
                recommendAge = 3,
                duration = 20
            ),
            
            Card(
                id = 6,
                title = "数字3",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "数字",
                textContent = "这是数字3。像耳朵。小朋友3岁了。生日快乐！",
                recommendAge = 3,
                duration = 20
            ),
            
            Card(
                id = 7,
                title = "数字4",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "数字",
                textContent = "这是数字4。像小旗子。桌子有4条腿。数数看！",
                recommendAge = 3,
                duration = 20
            ),
            
            Card(
                id = 8,
                title = "数字5",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "数字",
                textContent = "这是数字5。像钩子。一只手有5个手指。伸出小手数一数！",
                recommendAge = 3,
                duration = 20
            ),
            
            // ========== 颜色认知类 ==========
            Card(
                id = 9,
                title = "红色",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "颜色",
                textContent = "这是红色。苹果是红色。消防车也是红色。你喜欢红色吗？",
                recommendAge = 3,
                duration = 25
            ),
            
            Card(
                id = 10,
                title = "黄色",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "颜色",
                textContent = "这是黄色。太阳是黄色。香蕉也是黄色。黄色真明亮！",
                recommendAge = 3,
                duration = 25
            ),
            
            Card(
                id = 11,
                title = "蓝色",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "颜色",
                textContent = "这是蓝色。天空是蓝色。大海也是蓝色。蓝色真美丽！",
                recommendAge = 3,
                duration = 25
            ),
            
            Card(
                id = 12,
                title = "绿色",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "颜色",
                textContent = "这是绿色。树叶是绿色。小草也是绿色。绿色真清新！",
                recommendAge = 3,
                duration = 25
            ),
            
            // ========== 动物认知类 ==========
            Card(
                id = 13,
                title = "勇敢的小狮子",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "动物",
                textContent = "小狮子很勇敢。它是森林之王。狮子会吼叫。你能学狮子叫吗？",
                recommendAge = 3,
                duration = 25
            ),
            
            Card(
                id = 14,
                title = "大象",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "动物",
                textContent = "大象有长鼻子。它很大很强壮。大象喜欢洗澡。你见过大象吗？",
                recommendAge = 3,
                duration = 25
            ),
            
            Card(
                id = 15,
                title = "小兔子",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "动物",
                textContent = "小兔子蹦蹦跳。它有长耳朵。兔子爱吃萝卜。小兔子真可爱！",
                recommendAge = 3,
                duration = 25
            ),
            
            Card(
                id = 16,
                title = "小狗",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "动物",
                textContent = "小狗汪汪叫。它是好朋友。小狗很忠诚。你喜欢小狗吗？",
                recommendAge = 3,
                duration = 25
            ),
            
            Card(
                id = 17,
                title = "小猫",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "动物",
                textContent = "小猫喵喵叫。它爱喝牛奶。小猫很温柔。摸摸小猫咪！",
                recommendAge = 3,
                duration = 25
            ),
            
            // ========== 日常物品类 ==========
            Card(
                id = 18,
                title = "红苹果",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "水果",
                textContent = "红红的苹果。苹果很甜。吃苹果身体好。你今天吃苹果了吗？",
                recommendAge = 3,
                duration = 20
            ),
            
            Card(
                id = 19,
                title = "香蕉",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "水果",
                textContent = "黄黄的香蕉。香蕉弯弯的。猴子爱吃香蕉。剥开皮就能吃！",
                recommendAge = 3,
                duration = 20
            ),
            
            Card(
                id = 20,
                title = "皮球",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "玩具",
                textContent = "圆圆的皮球。皮球会弹跳。踢球真好玩。我们一起玩球吧！",
                recommendAge = 3,
                duration = 20
            ),
            
            // ========== 品质培养类 ==========
            Card(
                id = 21,
                title = "勇敢",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "品质",
                textContent = "勇敢的孩子不怕黑。勇敢的孩子会尝试。你是勇敢的孩子！",
                recommendAge = 3,
                duration = 25
            ),
            
            Card(
                id = 22,
                title = "分享",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "品质",
                textContent = "好东西要分享。分享让人快乐。你会分享玩具吗？",
                recommendAge = 3,
                duration = 25
            ),
            
            Card(
                id = 23,
                title = "礼貌",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "品质",
                textContent = "见面说你好。离开说再见。谢谢和对不起。你真有礼貌！",
                recommendAge = 3,
                duration = 25
            ),
            
            // ========== 生活常识类 ==========
            Card(
                id = 24,
                title = "洗手",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "生活",
                textContent = "饭前要洗手。用肥皂搓搓搓。洗手保健康。你会洗手吗？",
                recommendAge = 3,
                duration = 20
            ),
            
            Card(
                id = 25,
                title = "刷牙",
                imageResId = R.drawable.ic_launcher_foreground,
                audioResId = R.raw.notification,
                category = "生活",
                textContent = "早晚要刷牙。牙刷刷刷刷。牙齿白又亮。保护小牙齿！",
                recommendAge = 3,
                duration = 20
            )
        )
    }
    
    /**
     * 根据类别获取卡片
     * 
     * @param category 类别名称
     * @return 该类别的卡片列表
     */
    fun getCardsByCategory(category: String): List<Card> {
        return getAllCards().filter { it.category == category }
    }
    
    /**
     * 获取所有类别
     * 
     * @return 类别列表
     */
    fun getAllCategories(): List<String> {
        return getAllCards().map { it.category }.distinct()
    }
    
    /**
     * 获取推荐的学习顺序
     * 
     * 推荐顺序考虑：
     * 1. 孩子的兴趣（红色、消防车优先）
     * 2. 由易到难
     * 3. 类别交替，避免枯燥
     * 
     * @return 排序后的卡片列表
     */
    fun getRecommendedOrder(): List<Card> {
        val allCards = getAllCards()
        val ordered = mutableListOf<Card>()
        
        // 优先添加孩子最感兴趣的内容
        ordered.add(allCards.find { it.id == 1 }!!) // 红色消防车
        ordered.add(allCards.find { it.id == 9 }!!) // 红色
        ordered.add(allCards.find { it.id == 4 }!!) // 数字1
        ordered.add(allCards.find { it.id == 13 }!!) // 勇敢的小狮子
        
        // 添加其他卡片，保持类别交替
        val remaining = allCards.filter { card -> ordered.none { it.id == card.id } }
        val categories = remaining.groupBy { it.category }
        
        while (ordered.size < allCards.size) {
            categories.forEach { (_, cards) ->
                val unaddedCards = cards.filter { card -> ordered.none { it.id == card.id } }
                if (unaddedCards.isNotEmpty()) {
                    ordered.add(unaddedCards.first())
                }
            }
        }
        
        return ordered
    }
}
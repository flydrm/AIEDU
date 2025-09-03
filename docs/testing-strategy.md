# 测试策略文档

文档版本: v1.0  
创建日期: 2025-01-03  
作者: 架构助手  
项目阶段: MVP开发

## 1. 测试目标

### 1.1 主要目标
- **功能完整性**：确保所有教育功能正常工作
- **儿童安全性**：保证应用对3岁儿童安全友好
- **设备兼容性**：手机和平板都能正常使用
- **性能稳定性**：流畅运行，无卡顿崩溃

### 1.2 质量标准
- 崩溃率 < 0.1%
- 核心功能可用性 100%
- 响应时间 < 300ms
- 内存占用 < 350MB

## 2. 测试范围

### 2.1 功能测试

#### 核心功能
- [ ] 卡片列表展示
- [ ] 卡片内容播放
- [ ] 音频播放控制
- [ ] 进度保存/恢复
- [ ] 完成状态记录

#### AI功能
- [ ] 内容生成测试
- [ ] 鼓励语生成
- [ ] TTS语音合成
- [ ] 错误降级处理
- [ ] 离线模式测试

#### UI交互
- [ ] 触摸响应
- [ ] 滑动操作
- [ ] 动画效果
- [ ] 屏幕旋转
- [ ] 多任务切换

### 2.2 兼容性测试

#### 设备类型
```kotlin
// 最小测试矩阵
val testDevices = listOf(
    // 手机
    "5寸手机 (320dp宽)",     // 小屏手机
    "6寸手机 (360dp宽)",     // 主流手机
    "6.7寸手机 (400dp宽)",   // 大屏手机
    
    // 平板
    "7寸平板 (600dp宽)",     // 小平板
    "10寸平板 (800dp宽)",    // 标准平板
    "12寸平板 (840dp宽)"     // 大平板
)
```

#### Android版本
- Android 7.0 (API 24) - 最低支持
- Android 9.0 (API 28) - 目标版本
- Android 13 (API 33) - 最新稳定版
- Android 14 (API 34) - 最新版本

### 2.3 性能测试

#### 启动性能
```kotlin
// 测试用例
@Test
fun testColdStartTime() {
    // 期望：冷启动时间 < 2.5秒
    val startTime = System.currentTimeMillis()
    
    // 启动应用
    activityRule.launchActivity(Intent())
    
    // 等待首页加载完成
    onView(withId(R.id.card_list))
        .check(matches(isDisplayed()))
    
    val duration = System.currentTimeMillis() - startTime
    assertTrue("冷启动时间超过2.5秒", duration < 2500)
}
```

#### 内存使用
- 空闲状态：< 150MB
- 播放状态：< 250MB
- 峰值使用：< 350MB

#### 流畅度
- 列表滑动：60fps
- 动画播放：60fps
- 页面切换：< 300ms

## 3. 测试方法

### 3.1 手动测试

#### 冒烟测试清单
```markdown
## 每日冒烟测试（15分钟）

### 启动测试
- [ ] 应用正常启动
- [ ] 无白屏/黑屏
- [ ] 启动时间 < 3秒

### 核心路径
- [ ] 查看卡片列表
- [ ] 选择一张卡片
- [ ] 播放卡片内容
- [ ] 完成后返回列表
- [ ] 进度正确更新

### 基础交互
- [ ] 点击响应正常
- [ ] 滑动流畅
- [ ] 返回键正常
- [ ] 横竖屏切换

### 异常处理
- [ ] 断网提示
- [ ] 错误提示友好
- [ ] 无崩溃闪退
```

#### 儿童测试场景
```markdown
## 儿童使用测试（需要真实儿童参与）

### 测试准备
- 测试人员：3岁男孩
- 测试环境：安静房间
- 测试时长：15-20分钟
- 家长陪同：必须

### 观察要点
1. **理解度**
   - [ ] 能否理解界面
   - [ ] 能否找到想要的内容
   - [ ] 是否需要家长帮助

2. **操作性**
   - [ ] 点击是否准确
   - [ ] 滑动是否顺畅
   - [ ] 是否有误操作

3. **吸引力**
   - [ ] 注意力保持时间
   - [ ] 主动使用意愿
   - [ ] 情绪反应

4. **安全性**
   - [ ] 无不当内容
   - [ ] 无外部链接
   - [ ] 时长控制有效
```

### 3.2 自动化测试

#### 单元测试
```kotlin
// 示例：卡片数据测试
class CardRepositoryTest {
    
    @Test
    fun `测试卡片加载`() {
        // Given
        val repository = CardRepository()
        
        // When
        val cards = repository.loadCards()
        
        // Then
        assertTrue(cards.isNotEmpty())
        assertEquals(20, cards.size) // MVP版本20-30张
    }
    
    @Test
    fun `测试进度保存`() {
        // Given
        val repository = CardRepository()
        val cardId = "card_001"
        
        // When
        repository.markAsCompleted(cardId)
        
        // Then
        assertTrue(repository.isCompleted(cardId))
    }
}
```

#### UI测试
```kotlin
// 示例：主界面测试
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    
    @Rule
    @JvmField
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun `测试卡片列表显示`() {
        // 检查列表显示
        onView(withId(R.id.card_list))
            .check(matches(isDisplayed()))
        
        // 检查至少有一个卡片
        onView(withId(R.id.card_list))
            .check(matches(hasMinimumChildCount(1)))
    }
    
    @Test
    fun `测试卡片点击`() {
        // 点击第一张卡片
        onView(withId(R.id.card_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<ViewHolder>(0, click()))
        
        // 检查进入播放页面
        onView(withId(R.id.play_button))
            .check(matches(isDisplayed()))
    }
}
```

### 3.3 AI功能测试

#### 模拟测试
```kotlin
class AIServiceTest {
    
    @Mock
    lateinit var mockAIClient: AIClient
    
    @Test
    fun `测试内容生成降级`() {
        // Given: AI服务不可用
        whenever(mockAIClient.generateContent(any()))
            .thenThrow(NetworkException())
        
        val service = AIService(mockAIClient)
        
        // When: 请求生成内容
        val content = service.generateContentWithFallback("消防车")
        
        // Then: 返回预设内容
        assertNotNull(content)
        assertTrue(content.contains("消防车"))
    }
}
```

## 4. 测试环境

### 4.1 开发环境测试
- 测试设备：开发者手机/平板
- 测试账号：测试专用账号
- API环境：开发环境
- 日志级别：DEBUG

### 4.2 预发布测试
- 测试设备：多种真实设备
- 测试账号：真实账号
- API环境：生产环境
- 日志级别：INFO

## 5. 缺陷管理

### 5.1 缺陷分级
- **P0 - 阻塞**：应用崩溃、核心功能不可用
- **P1 - 严重**：功能错误、数据丢失
- **P2 - 一般**：UI问题、性能问题
- **P3 - 轻微**：文案错误、样式偏差

### 5.2 处理流程
1. 发现问题 → 记录详情
2. 判断级别 → 分配处理
3. 修复问题 → 验证测试
4. 关闭问题 → 回归测试

### 5.3 问题模板
```markdown
## 问题标题
[简要描述问题]

## 问题详情
- **发现版本**：v1.0.0
- **设备信息**：小米12 / Android 13
- **复现步骤**：
  1. 打开应用
  2. 点击第一张卡片
  3. 快速点击播放按钮
- **预期结果**：正常播放
- **实际结果**：应用崩溃
- **复现概率**：100%

## 附件
[截图/日志/视频]
```

## 6. 测试计划（2周MVP）

### 第一周
- Day 1-3：单元测试编写
- Day 4-5：核心功能测试
- Day 6-7：UI自动化测试

### 第二周  
- Day 8-9：兼容性测试
- Day 10：性能测试
- Day 11：儿童真实测试
- Day 12：问题修复
- Day 13：回归测试
- Day 14：发布测试

## 7. 测试工具

### 7.1 测试框架
```gradle
dependencies {
    // 单元测试
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:4.8.0'
    
    // UI测试
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    
    // 性能测试
    debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.10'
}
```

### 7.2 辅助工具
- **Android Studio Profiler**：性能分析
- **Layout Inspector**：UI调试
- **Monkey测试**：稳定性测试
- **Firebase Crashlytics**：崩溃收集

## 8. 特殊考虑

### 8.1 儿童特性测试
- 大按钮点击测试（最小48dp）
- 误操作防护测试
- 时长限制测试
- 内容安全测试

### 8.2 家长功能测试
- 进度查看
- 时长控制
- 内容管理
- 设置功能

## 9. 发布标准

### 9.1 必须通过
- [ ] 所有P0/P1问题已修复
- [ ] 核心功能测试100%通过
- [ ] 无内存泄漏
- [ ] 启动时间达标
- [ ] 儿童测试反馈良好

### 9.2 建议完成
- [ ] P2问题修复80%以上
- [ ] 自动化测试覆盖60%
- [ ] 性能优化完成
- [ ] 多设备测试通过

## 10. 持续改进

- 收集用户反馈
- 分析崩溃数据
- 优化测试用例
- 提升自动化率

记住：对于儿童应用，安全和稳定性是第一位的！
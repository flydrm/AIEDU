# 代码审查规范

文档版本: v1.0  
创建日期: 2025-01-03  
作者: 架构助手  
审核人: 待定

## 1. 代码审查目标

### 1.1 质量保证
- 确保代码符合项目规范
- 发现潜在的bug和性能问题
- 验证业务逻辑的正确性
- 保证代码的可维护性

### 1.2 知识传递
- 团队成员了解代码变更
- 分享最佳实践
- 统一编码风格
- 促进技术成长

## 2. 审查前准备

### 2.1 提交者责任
- [ ] 代码自测通过
- [ ] 单元测试覆盖
- [ ] 注释完整清晰（中文）
- [ ] 无编译警告
- [ ] 遵循命名规范

### 2.2 PR要求
```markdown
## 变更说明
[简要描述本次变更的内容和目的]

## 改动类型
- [ ] 新功能
- [ ] Bug修复
- [ ] 性能优化
- [ ] 重构
- [ ] 文档更新

## 测试情况
- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] 手动测试通过

## 影响范围
[说明本次改动可能影响的模块]

## 截图/演示
[如有UI变更，请附上截图]
```

## 3. 审查重点（针对本项目）

### 3.1 🔴 中文注释（最高优先级）
```kotlin
// ✅ 好的注释示例
/**
 * 卡片播放管理器
 * 负责控制教育卡片的播放流程，包括：
 * 1. 音频播放控制
 * 2. 动画展示
 * 3. 互动响应
 * 4. 进度记录
 */
class CardPlayerManager {
    /**
     * 播放指定卡片
     * @param card 要播放的卡片
     * @param onComplete 播放完成的回调
     * 注意：会自动处理音频资源的释放
     */
    fun playCard(card: Card, onComplete: () -> Unit) {
        // 1. 先停止当前播放
        stopCurrentPlayback()
        
        // 2. 加载新卡片资源
        loadCardResources(card)
        
        // 3. 开始播放流程
        startPlayback(onComplete)
    }
}
```

### 3.2 童趣UI实现
- [ ] 圆角卡片样式正确
- [ ] 动画流畅自然
- [ ] 颜色符合设计规范
- [ ] 触摸反馈及时
- [ ] 适配手机和平板

### 3.3 儿童安全
- [ ] 无外部链接
- [ ] 家长控制有效
- [ ] 时长限制功能正常
- [ ] 内容审核通过
- [ ] 无隐私数据收集

### 3.4 AI功能集成
- [ ] API调用错误处理
- [ ] 降级方案可用
- [ ] 缓存机制有效
- [ ] 成本控制合理
- [ ] 响应时间达标

## 4. 代码审查清单

### 4.1 功能正确性
- [ ] 业务逻辑正确
- [ ] 边界条件处理
- [ ] 异常情况处理
- [ ] 数据验证完整
- [ ] 状态管理正确

### 4.2 代码质量
- [ ] 命名清晰易懂
- [ ] 函数职责单一
- [ ] 无重复代码
- [ ] 合理的代码结构
- [ ] 适当的抽象层级

### 4.3 性能考虑
- [ ] 避免内存泄漏
- [ ] 合理的资源管理
- [ ] 异步操作正确
- [ ] 列表优化（如有）
- [ ] 图片加载优化

### 4.4 用户体验
- [ ] 加载状态提示
- [ ] 错误提示友好
- [ ] 操作反馈及时
- [ ] 动画流畅
- [ ] 无卡顿现象

## 5. 常见问题

### 5.1 资源管理
```kotlin
// ❌ 错误：未释放资源
fun playAudio(audioResId: Int) {
    val mediaPlayer = MediaPlayer.create(context, audioResId)
    mediaPlayer.start()
}

// ✅ 正确：正确管理生命周期
class AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    
    fun playAudio(audioResId: Int) {
        // 释放旧资源
        mediaPlayer?.release()
        
        // 创建新播放器
        mediaPlayer = MediaPlayer.create(context, audioResId).apply {
            setOnCompletionListener {
                release()
                mediaPlayer = null
            }
            start()
        }
    }
    
    fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
```

### 5.2 异步处理
```kotlin
// ❌ 错误：主线程执行耗时操作
fun generateContent(topic: String) {
    val content = aiService.generateContent(topic) // 阻塞主线程
    updateUI(content)
}

// ✅ 正确：使用协程处理
fun generateContent(topic: String) {
    lifecycleScope.launch {
        try {
            // 显示加载状态
            showLoading()
            
            // 异步生成内容
            val content = withContext(Dispatchers.IO) {
                aiService.generateContent(topic)
            }
            
            // 更新UI
            updateUI(content)
        } catch (e: Exception) {
            // 错误处理
            showError("生成失败，请重试")
        } finally {
            hideLoading()
        }
    }
}
```

## 6. 审查流程

### 6.1 审查步骤
1. **快速浏览**：了解改动范围
2. **详细审查**：逐行检查代码
3. **运行测试**：验证功能正确
4. **提出建议**：友好地指出问题
5. **跟进修改**：确认问题解决

### 6.2 反馈原则
- 具体明确：指出具体问题和改进建议
- 建设性：提供解决方案，不只是批评
- 及时响应：24小时内完成审查
- 友好交流：保持专业和尊重

### 6.3 评论示例
```markdown
# 👍 好的评论
"这里的资源管理可能有内存泄漏风险。建议在 onDestroy 中释放 mediaPlayer。
可以参考 AudioPlayer 类的实现方式。"

# 👎 不好的评论
"这代码写得不行。"
```

## 7. 特殊考虑

### 7.1 儿童应用特性
- 界面元素足够大（最小48dp）
- 操作简单直观
- 反馈及时明确
- 错误处理友好
- 家长控制完善

### 7.2 性能要求
- 冷启动 < 2.5秒
- 动画保持60fps
- 内存占用 < 350MB
- 响应时间 < 300ms

## 8. 工具支持

### 8.1 静态检查
```gradle
// build.gradle
android {
    lintOptions {
        enable 'WrongThread'
        enable 'ResourceLeak'
        enable 'Deprecated'
        warning 'HardcodedText'
        error 'NewApi'
    }
}
```

### 8.2 代码格式化
- Android Studio: `Ctrl+Alt+L`
- 使用项目统一的代码样式配置
- 提交前运行格式化

## 9. 审查记录

### 9.1 记录模板
```markdown
## 审查记录
- 审查人：[姓名]
- 审查时间：[YYYY-MM-DD HH:mm]
- 审查结果：通过/需修改
- 主要问题：
  1. [问题描述]
  2. [问题描述]
- 改进建议：
  1. [建议内容]
  2. [建议内容]
```

## 10. 持续改进

- 定期回顾审查效果
- 更新审查标准
- 分享最佳实践
- 优化审查流程

记住：代码审查是为了共同进步，而不是找茬。保持开放和学习的心态！
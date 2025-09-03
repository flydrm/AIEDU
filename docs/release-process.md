# 发布流程文档

文档版本: v1.0  
创建日期: 2025-01-03  
作者: 架构助手  
适用范围: AI启蒙时光 Android应用

## 1. 发布概述

### 1.1 发布类型
- **内部测试版**：家庭内部使用，快速迭代
- **正式版**：稳定版本，充分测试

### 1.2 发布周期
- MVP版本：2周开发 + 3天测试
- 后续迭代：每2周一个小版本
- 大版本：每2个月一次

## 2. 发布前准备

### 2.1 代码准备
```bash
# 发布检查清单
- [ ] 所有功能开发完成
- [ ] 代码审查全部通过
- [ ] 单元测试全部通过
- [ ] UI测试全部通过
- [ ] 无编译警告
- [ ] 删除所有TODO和临时代码
- [ ] 更新版本号
```

### 2.2 版本号管理
```kotlin
// app/build.gradle
android {
    defaultConfig {
        versionCode 1      // 内部版本号，每次发布+1
        versionName "1.0.0" // 展示版本号：主版本.功能版本.修复版本
    }
}

// 版本号规则
// 1.0.0 - MVP版本
// 1.1.0 - 新增功能
// 1.1.1 - Bug修复
// 2.0.0 - 重大更新
```

### 2.3 资源检查
```kotlin
// 检查项目
class ReleaseChecklist {
    // 1. 图片资源优化
    fun checkImages() {
        // - 所有图片已压缩
        // - 使用WebP格式
        // - 不同分辨率适配
    }
    
    // 2. 音频资源检查
    fun checkAudio() {
        // - 音频格式统一（MP3）
        // - 比特率适中（128kbps）
        // - 音量标准化
    }
    
    // 3. 代码混淆配置
    fun checkProguard() {
        // - 混淆规则完整
        // - 保留必要的类
        // - 测试混淆后功能
    }
}
```

## 3. 构建配置

### 3.1 签名配置
```kotlin
// app/build.gradle
android {
    signingConfigs {
        release {
            storeFile file("../keystore/release.keystore")
            storePassword System.getenv("KEYSTORE_PASSWORD")
            keyAlias "ai_education"
            keyPassword System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 
                         'proguard-rules.pro'
        }
    }
}
```

### 3.2 混淆规则
```proguard
# proguard-rules.pro

# 保留AI服务相关类
-keep class com.education.ai.** { *; }

# 保留数据模型
-keep class com.education.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# 保留用于反射的类
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
```

### 3.3 构建优化
```kotlin
// 启用R8优化
android {
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true // 移除未使用的资源
            
            // 优化选项
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'),
                         'proguard-rules.pro'
            
            // 资源优化
            crunchPngs false // 使用aapt2
        }
    }
}
```

## 4. 发布流程

### 4.1 构建APK
```bash
# 1. 清理项目
./gradlew clean

# 2. 构建发布版本
./gradlew assembleRelease

# 3. 输出位置
# app/build/outputs/apk/release/app-release.apk
```

### 4.2 APK检查
```bash
# 1. 检查APK大小
# 目标：< 50MB

# 2. 检查签名
jarsigner -verify -verbose app-release.apk

# 3. 安装测试
adb install -r app-release.apk
```

### 4.3 多渠道打包（如需要）
```kotlin
// app/build.gradle
android {
    flavorDimensions "channel"
    productFlavors {
        family {
            dimension "channel"
            applicationIdSuffix ".family"
            versionNameSuffix "-family"
        }
    }
}
```

## 5. 测试验证

### 5.1 发布测试清单
```markdown
## 发布前测试（必须）

### 安装测试
- [ ] 全新安装成功
- [ ] 覆盖安装成功
- [ ] 首次启动正常
- [ ] 权限申请正常

### 核心功能
- [ ] 卡片列表加载
- [ ] 卡片播放功能
- [ ] 音频播放正常
- [ ] 进度保存功能
- [ ] AI功能可用

### 兼容性
- [ ] 最低版本设备（Android 7.0）
- [ ] 目标版本设备（Android 9.0）
- [ ] 最新版本设备（Android 14）
- [ ] 手机/平板适配

### 性能指标
- [ ] 启动时间 < 2.5秒
- [ ] 内存占用 < 350MB
- [ ] 无明显卡顿
- [ ] 电量消耗正常
```

### 5.2 回归测试
- 执行完整测试用例
- 重点测试修改部分
- 验证已知问题修复

## 6. 发布方式

### 6.1 家庭内部发布（推荐）
```kotlin
// 1. 直接安装
// - 通过USB安装
// - 通过文件传输安装

// 2. 家庭云盘
// - 上传到家庭NAS
// - 通过局域网下载

// 3. 二维码分享
// - 生成下载二维码
// - 扫码下载安装
```

### 6.2 应用商店发布（可选）
```markdown
## 应用商店准备

### 基础信息
- 应用名称：AI启蒙时光
- 包名：com.education.ai.kids
- 分类：教育/儿童
- 年龄分级：3+

### 商店素材
- 应用图标：512x512px
- 功能图：1080x1920px (3-5张)
- 应用介绍：< 80字
- 详细描述：< 4000字
- 隐私政策：必需

### 截图要求
1. 主界面 - 卡片列表
2. 学习界面 - 卡片播放
3. 完成界面 - 鼓励反馈
4. 设置界面 - 家长控制
```

## 7. 发布后监控

### 7.1 崩溃监控
```kotlin
// 集成Bugly（可选）
dependencies {
    implementation 'com.tencent.bugly:crashreport:4.1.9'
}

// Application中初始化
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!BuildConfig.DEBUG) {
            CrashReport.initCrashReport(this, "YOUR_APP_ID", false)
        }
    }
}
```

### 7.2 用户反馈
- 应用内反馈入口
- 家长微信群
- 邮箱反馈

### 7.3 数据收集（最小化）
```kotlin
// 仅收集必要数据
data class UsageStats {
    val appVersion: String     // 版本号
    val deviceModel: String    // 设备型号
    val androidVersion: Int    // 系统版本
    val crashCount: Int        // 崩溃次数
    val dailyUsageMinutes: Int // 每日使用时长
}
```

## 8. 版本记录

### 8.1 版本日志模板
```markdown
# 版本 1.0.0
发布日期：2025-01-15

## 新功能
- 20个精选教育卡片
- AI智能内容生成
- 童趣界面设计
- 手机平板适配

## 优化
- 启动速度提升30%
- 内存占用减少20%

## 修复
- 修复音频播放中断问题
- 修复横屏切换崩溃

## 已知问题
- 部分设备TTS声音较小
```

### 8.2 Git标签
```bash
# 创建版本标签
git tag -a v1.0.0 -m "MVP版本发布"

# 推送标签
git push origin v1.0.0
```

## 9. 回滚方案

### 9.1 问题评估
- P0问题：立即回滚
- P1问题：紧急修复
- P2问题：计划修复

### 9.2 回滚步骤
1. 停止当前版本分发
2. 通知用户暂缓更新
3. 恢复上一稳定版本
4. 分析问题原因
5. 修复后重新发布

## 10. 注意事项

### 10.1 儿童应用特殊要求
- **隐私保护**：不收集儿童个人信息
- **内容审核**：确保内容适龄
- **家长控制**：提供必要的控制选项
- **时长限制**：防止过度使用

### 10.2 发布原则
- **稳定优先**：宁可延期不可带病发布
- **小步快跑**：频繁发布小版本
- **用户至上**：快速响应用户反馈
- **持续改进**：每个版本都有进步

## 11. 紧急联系

- 技术负责人：[姓名] [电话]
- 测试负责人：[姓名] [电话]
- 产品负责人：[姓名] [电话]

记住：发布不是结束，而是新的开始！
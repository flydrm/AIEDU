# 构建和运行指南

文档版本: v1.0  
创建日期: 2025-01-03  
作者: AI启蒙时光开发团队

## 前置要求

### 开发环境
- **Android Studio**: Arctic Fox (2020.3.1) 或更新版本
- **JDK**: 11 或更高版本
- **Android SDK**: API Level 34
- **Kotlin**: 1.9.20

### 硬件要求
- 至少 8GB RAM（推荐 16GB）
- 至少 10GB 可用磁盘空间

## 项目设置

### 1. 克隆项目
```bash
git clone [repository-url]
cd ai-kidsedu
```

### 2. 配置本地环境
```bash
# 复制本地配置文件
cp local.properties.example local.properties

# 编辑 local.properties，设置你的 SDK 路径
# sdk.dir=/path/to/your/Android/Sdk
```

### 3. 配置AI服务（可选）
```bash
# 复制AI配置文件
cp app/src/main/assets/ai_config.json.example app/src/main/assets/ai_config.json

# 编辑 ai_config.json，填入你的API密钥
```

## 构建项目

### 使用Android Studio
1. 打开Android Studio
2. 选择 "Open an Existing Project"
3. 选择项目根目录
4. 等待Gradle同步完成
5. 点击 "Run" 按钮或使用快捷键 Shift+F10

### 使用命令行
```bash
# 构建Debug版本
./gradlew assembleDebug

# 构建Release版本（需要签名配置）
./gradlew assembleRelease

# 清理构建
./gradlew clean

# 构建并安装到设备
./gradlew installDebug
```

## 运行项目

### 在真机上运行
1. 启用开发者选项和USB调试
2. 连接手机到电脑
3. 运行 `adb devices` 确认设备已连接
4. 在Android Studio中选择你的设备
5. 点击运行按钮

### 在模拟器上运行
1. 打开AVD Manager
2. 创建或选择一个虚拟设备
   - 推荐配置：Pixel 4 (手机测试)
   - 推荐配置：Pixel C (平板测试)
3. 启动模拟器
4. 运行应用

## 测试不同屏幕

### 手机测试配置
- **竖屏**: Pixel 4 (5.7")
- **横屏**: 旋转设备测试
- **小屏**: Nexus S (4.0")

### 平板测试配置
- **7寸平板**: Nexus 7
- **10寸平板**: Pixel C
- **横竖屏**: 都需要测试

## 常见问题

### Gradle同步失败
```bash
# 清理Gradle缓存
./gradlew clean
rm -rf ~/.gradle/caches/

# 重新同步
./gradlew --refresh-dependencies
```

### 构建速度慢
1. 在 `gradle.properties` 中确保启用了：
   ```
   org.gradle.parallel=true
   org.gradle.caching=true
   ```
2. 增加JVM内存：
   ```
   org.gradle.jvmargs=-Xmx4096m
   ```

### 找不到Android SDK
1. 检查 `local.properties` 中的 `sdk.dir` 路径
2. 确保已安装必要的SDK组件：
   ```bash
   sdkmanager "platforms;android-34"
   sdkmanager "build-tools;34.0.0"
   ```

### APK安装失败
```bash
# 检查设备存储空间
adb shell df

# 卸载旧版本
adb uninstall com.family.kidsedu

# 重新安装
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 发布准备

### 1. 生成签名密钥
```bash
keytool -genkey -v -keystore release.keystore -alias kidsedu -keyalg RSA -keysize 2048 -validity 10000
```

### 2. 配置签名
在 `app/build.gradle.kts` 中添加：
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("path/to/release.keystore")
        storePassword = "your-store-password"
        keyAlias = "kidsedu"
        keyPassword = "your-key-password"
    }
}
```

### 3. 构建发布版本
```bash
./gradlew assembleRelease
```

### 4. 优化APK大小
```bash
# 分析APK大小
./gradlew app:analyzeReleaseBundle

# 使用App Bundle
./gradlew bundleRelease
```

## 性能优化建议

1. **启用ProGuard**：减小APK大小
2. **使用WebP图片**：比PNG小30-50%
3. **按需加载资源**：避免一次性加载所有内容
4. **使用向量图**：可缩放且体积小

## 调试技巧

### 查看日志
```bash
# 查看所有日志
adb logcat

# 只看应用日志
adb logcat -s MainActivity:V SimpleAIService:V

# 清空日志
adb logcat -c
```

### 性能分析
1. 使用Android Studio的Profiler
2. 监控内存使用
3. 检查UI渲染性能

## 持续集成（可选）

### GitHub Actions示例
```yaml
name: Android CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Set up JDK 11
      uses: actions/setup-java@v2
      with:
        java-version: '11'
    - name: Build with Gradle
      run: ./gradlew build
```

---

如有其他问题，请查看项目文档或提交Issue。
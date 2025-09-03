# 快速实现指南（2周完成MVP）

文档版本: v1.0  
创建日期: 2025-01-03  
作者: 架构助手

## 1. 项目初始化（Day 1）

### 1.1 创建项目
```bash
# Android Studio 创建新项目
- 选择 "Empty Activity"
- 最低 SDK: API 24 (Android 7.0)
- 语言: Kotlin
- 不要选择 Compose（使用传统View）
```

### 1.2 基础配置
```kotlin
// build.gradle (app)
android {
    compileSdk 34
    
    defaultConfig {
        applicationId "com.family.kidsedu"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }
    
    buildFeatures {
        viewBinding true  // 简化View操作
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    // 就这些，不要添加复杂依赖
}
```

## 2. 核心代码实现（Day 2-7）

### 2.1 数据模型（最简单）
```kotlin
// Card.kt
data class Card(
    val id: Int,
    val title: String,
    val imageResId: Int,
    val audioResId: Int,
    val interactionType: String = "tap",  // 暂时只支持tap
    val interactionTarget: String? = null  // 例如"red_car"表示点击红色汽车
)

// Progress.kt
object Progress {
    private val prefs by lazy { 
        App.context.getSharedPreferences("progress", Context.MODE_PRIVATE) 
    }
    
    fun getCurrentCardIndex(): Int = prefs.getInt("current_card", 0)
    
    fun saveCurrentCardIndex(index: Int) {
        prefs.edit().putInt("current_card", index).apply()
    }
    
    fun getTodayPlayTime(): Int {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        return prefs.getInt("play_time_$today", 0)
    }
    
    fun addPlayTime(minutes: Int) {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val current = getTodayPlayTime()
        prefs.edit().putInt("play_time_$today", current + minutes).apply()
    }
}
```

### 2.2 主界面布局
```xml
<!-- activity_main.xml -->
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background">

    <!-- 卡片内容区域 -->
    <FrameLayout
        android:id="@+id/cardContainer"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_above="@id/bottomBar">
        
        <!-- 卡片图片 -->
        <ImageView
            android:id="@+id/cardImage"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:scaleType="fitCenter"
            android:padding="16dp" />
        
        <!-- 互动提示 -->
        <ImageView
            android:id="@+id/interactionHint"
            android:layout_width="80dp"
            android:layout_height="80dp"
            android:src="@drawable/tap_hint"
            android:visibility="gone" />
            
    </FrameLayout>
    
    <!-- 底部进度条 -->
    <LinearLayout
        android:id="@+id/bottomBar"
        android:layout_width="match_parent"
        android:layout_height="60dp"
        android:layout_alignParentBottom="true"
        android:orientation="horizontal"
        android:background="@color/bottom_bar_bg"
        android:gravity="center_vertical"
        android:padding="10dp">
        
        <TextView
            android:id="@+id/progressText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="1 / 20"
            android:textSize="18sp" />
            
        <ProgressBar
            android:id="@+id/progressBar"
            style="@style/Widget.AppCompat.ProgressBar.Horizontal"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="10dp"
            android:layout_marginEnd="10dp" />
            
    </LinearLayout>
    
    <!-- 奖励动画容器 -->
    <FrameLayout
        android:id="@+id/rewardContainer"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:visibility="gone"
        android:background="#80000000">
        
        <ImageView
            android:id="@+id/starReward"
            android:layout_width="200dp"
            android:layout_height="200dp"
            android:layout_gravity="center"
            android:src="@drawable/star" />
            
    </FrameLayout>

</RelativeLayout>
```

### 2.3 主Activity实现
```kotlin
// MainActivity.kt
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var currentCardIndex = 0
    private var mediaPlayer: MediaPlayer? = null
    private var startTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    
    // 预定义的卡片列表
    private val cards = listOf(
        Card(1, "红色消防车", R.drawable.card_01_fire_truck, R.raw.audio_01),
        Card(2, "数字1", R.drawable.card_02_number_1, R.raw.audio_02),
        Card(3, "勇敢的小狮子", R.drawable.card_03_lion, R.raw.audio_03),
        // ... 添加20-30张卡片
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 恢复进度
        currentCardIndex = Progress.getCurrentCardIndex()
        startTime = System.currentTimeMillis()
        
        // 设置点击监听
        binding.cardContainer.setOnClickListener {
            handleCardClick()
        }
        
        // 显示第一张卡片
        showCard(currentCardIndex)
        
        // 启动时长检查
        startTimeCheck()
    }
    
    private fun showCard(index: Int) {
        if (index >= cards.size) {
            // 所有卡片完成
            showCompletion()
            return
        }
        
        val card = cards[index]
        
        // 更新进度显示
        binding.progressText.text = "${index + 1} / ${cards.size}"
        binding.progressBar.progress = ((index + 1) * 100) / cards.size
        
        // 显示图片
        binding.cardImage.setImageResource(card.imageResId)
        
        // 播放音频
        playAudio(card.audioResId)
        
        // 3秒后显示互动提示
        handler.postDelayed({
            showInteractionHint()
        }, 3000)
    }
    
    private fun playAudio(audioResId: Int) {
        // 停止之前的音频
        mediaPlayer?.release()
        
        mediaPlayer = MediaPlayer.create(this, audioResId).apply {
            setOnCompletionListener {
                // 音频播放完成
            }
            start()
        }
    }
    
    private fun showInteractionHint() {
        // 显示点击提示动画
        binding.interactionHint.apply {
            visibility = View.VISIBLE
            animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .alpha(0.5f)
                .setDuration(500)
                .withEndAction {
                    animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(500)
                        .start()
                }
                .start()
        }
    }
    
    private fun handleCardClick() {
        // 隐藏提示
        binding.interactionHint.visibility = View.GONE
        
        // 显示奖励
        showReward {
            // 进入下一张卡片
            nextCard()
        }
    }
    
    private fun showReward(onComplete: () -> Unit) {
        binding.rewardContainer.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction {
                    // 星星旋转动画
                    binding.starReward.animate()
                        .rotation(360f)
                        .scaleX(1.5f)
                        .scaleY(1.5f)
                        .setDuration(1000)
                        .withEndAction {
                            animate()
                                .alpha(0f)
                                .setDuration(300)
                                .withEndAction {
                                    visibility = View.GONE
                                    binding.starReward.rotation = 0f
                                    binding.starReward.scaleX = 1f
                                    binding.starReward.scaleY = 1f
                                    onComplete()
                                }
                                .start()
                        }
                        .start()
                }
                .start()
        }
    }
    
    private fun nextCard() {
        currentCardIndex++
        Progress.saveCurrentCardIndex(currentCardIndex)
        showCard(currentCardIndex)
    }
    
    private fun startTimeCheck() {
        handler.postDelayed({
            val elapsedMinutes = (System.currentTimeMillis() - startTime) / 60000
            if (elapsedMinutes >= 20) {
                // 20分钟提醒
                showTimeUpDialog()
            } else {
                startTimeCheck()
            }
        }, 60000) // 每分钟检查一次
    }
    
    private fun showTimeUpDialog() {
        AlertDialog.Builder(this)
            .setTitle("休息一下吧！")
            .setMessage("已经学习20分钟了，让眼睛休息一下吧！")
            .setPositiveButton("好的") { _, _ ->
                finish()
            }
            .setNegativeButton("再玩5分钟") { _, _ ->
                startTime = System.currentTimeMillis() - 15 * 60000 // 重置为15分钟
                startTimeCheck()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showCompletion() {
        AlertDialog.Builder(this)
            .setTitle("太棒了！")
            .setMessage("今天的学习完成了！")
            .setPositiveButton("好的") { _, _ ->
                Progress.saveCurrentCardIndex(0) // 重置进度
                finish()
            }
            .show()
    }
    
    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
        
        // 保存播放时间
        val elapsedMinutes = ((System.currentTimeMillis() - startTime) / 60000).toInt()
        Progress.addPlayTime(elapsedMinutes)
    }
    
    override fun onResume() {
        super.onResume()
        mediaPlayer?.start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
    }
}
```

## 3. 资源准备（Day 8-10）

### 3.1 图片资源
```
res/drawable/
├── card_01_fire_truck.png    # 红色消防车
├── card_02_number_1.png       # 数字1
├── card_03_lion.png          # 勇敢的小狮子
├── star.png                  # 奖励星星
├── tap_hint.png             # 点击提示
└── ... (20-30张卡片图片)

图片要求：
- 格式：PNG (透明背景更好)
- 尺寸：1024x1024 或 800x600
- 大小：每张 < 500KB
```

### 3.2 音频资源
```
res/raw/
├── audio_01.mp3    # "这是一辆红色的消防车，它会帮助大家灭火"
├── audio_02.mp3    # "这是数字1，像一根小棍子"
├── audio_03.mp3    # "小狮子很勇敢，它不怕黑暗"
└── ... (20-30个音频文件)

音频制作：
- 使用手机录音App
- 时长：15-30秒
- 语速：慢一点，清晰
- 背景：安静环境
```

### 3.3 免费资源网站
```
图片：
- https://pixabay.com (免费高质量图片)
- https://www.freepik.com (需要标注来源)
- https://unsplash.com (高质量照片)

音效（可选）：
- https://freesound.org
- https://www.zapsplat.com

工具：
- Audacity (免费音频编辑)
- GIMP (免费图片编辑)
- Canva (在线设计，有免费版)
```

## 4. 测试和优化（Day 11-14）

### 4.1 测试要点
```kotlin
// 1. 进度保存测试
- 播放到第5张，退出
- 重新打开，应该从第5张继续

// 2. 时长控制测试
- 修改系统时间或等待20分钟
- 应该弹出休息提醒

// 3. 音频播放测试
- 检查所有音频文件能正常播放
- 退出应用时音频应该停止

// 4. 内存泄漏测试
- 连续播放所有卡片
- 使用Profiler检查内存使用
```

### 4.2 性能优化
```kotlin
// 1. 图片加载优化
- 使用合适的图片尺寸
- 考虑使用 WebP 格式（更小）

// 2. 音频预加载（可选）
private fun preloadNextAudio(nextIndex: Int) {
    if (nextIndex < cards.size) {
        // 预加载下一个音频
        MediaPlayer.create(this, cards[nextIndex].audioResId)
    }
}
```

## 5. 打包发布（家用）

### 5.1 生成APK
```bash
# 1. Build -> Build Bundle(s) / APK(s) -> Build APK(s)
# 2. 或使用命令行
./gradlew assembleDebug

# APK位置
app/build/outputs/apk/debug/app-debug.apk
```

### 5.2 安装到设备
```bash
# 通过 ADB
adb install app-debug.apk

# 或直接复制APK到手机安装
```

## 6. 后续迭代计划

### 6.1 第二版（1个月后）
- 添加更多卡片（到50张）
- 简单的卡片分类（动物/数字/颜色）
- 记录每张卡片的学习次数

### 6.2 第三版（3个月后）
- 添加简单的拖拽互动
- 学习日历（哪天学了）
- 家长查看总学习时长

## 7. 注意事项

### 7.1 保持简单
- 不要过度优化
- 不要添加复杂功能
- 够用就好

### 7.2 内容为王
- 把时间花在制作优质内容上
- 图片清晰、音频清楚最重要
- 内容符合孩子兴趣

### 7.3 快速迭代
- 有问题就改
- 孩子不喜欢就换
- 不要追求完美

## 总结

这个实现方案：
- **2周可完成**：专注核心功能
- **代码简单**：总共不超过500行
- **易于维护**：单Activity结构
- **方便扩展**：随时可以加内容

记住：这是给自己孩子用的，实用就好！
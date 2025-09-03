# 数据库设计文档（本地存储方案）

文档版本: v1.0  
创建日期: 2025-01-03  
作者: 架构助手  
审核人: 待定

## 1. 概述

本应用采用纯客户端架构，所有数据存储在本地。使用以下存储方案：
- **Room Database**: 结构化数据存储（学习进度、卡片信息、下载记录等）
- **DataStore**: 轻量级键值对存储（用户偏好、配置项等）
- **File Storage**: 媒体文件存储（卡片资源、音频文件等）

## 2. Room Database 设计

### 2.1 数据库配置
```kotlin
@Database(
    entities = [
        User::class,
        Theme::class,
        Card::class,
        Progress::class,
        Download::class,
        Reward::class,
        SessionLog::class,
        FeatureLog::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    // DAOs
}
```

### 2.2 实体（Entity）设计

#### 2.2.1 用户表（users）
```kotlin
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val userId: String = UUID.randomUUID().toString(), // 本地生成的匿名ID
    val preferColor: String? = null,                   // 偏好颜色（如"red"）
    val colorIntensity: String = "medium",             // low|medium|high
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
```

#### 2.2.2 主题表（themes）
```kotlin
@Entity(
    tableName = "themes",
    indices = [Index(value = ["sortOrder"])]
)
data class Theme(
    @PrimaryKey
    val themeId: String,
    val title: String,
    val description: String,
    val coverImagePath: String?,
    val sortOrder: Int,
    val totalCards: Int,
    val requiredAge: Int = 3,
    val tags: List<String>,              // ["勇敢", "逻辑", "红色"]
    val dominantColors: List<String>,    // ["red", "blue"]
    val isPreloaded: Boolean = false,    // 是否预装主题
    val createdAt: Long,
    val updatedAt: Long
)
```

#### 2.2.3 卡片表（cards）
```kotlin
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = Theme::class,
            parentColumns = ["themeId"],
            childColumns = ["themeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["themeId"]),
        Index(value = ["sortOrder"])
    ]
)
data class Card(
    @PrimaryKey
    val cardId: String,
    val themeId: String,
    val title: String,
    val contentType: String,              // "animation" | "interactive" | "story"
    val durationSeconds: Int,             // 20-40秒
    val sortOrder: Int,
    val difficulty: Int,                  // 1-3
    val interactionType: String?,         // "tap" | "drag" | "select"
    val interactionConfig: String?,       // JSON配置
    val resourcePath: String?,            // 本地资源路径
    val audioPath: String?,               // 音频文件路径
    val subtitlePath: String?,            // 字幕文件路径
    val dominantColors: List<String>,     // 主要颜色
    val objects: List<String>,            // ["消防车", "勇敢"]
    val educationalGoals: List<String>,   // ["勇敢", "颜色认知"]
    val createdAt: Long,
    val updatedAt: Long
)
```

#### 2.2.4 学习进度表（progress）
```kotlin
@Entity(
    tableName = "progress",
    primaryKeys = ["userId", "cardId"],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Card::class,
            parentColumns = ["cardId"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["cardId"]),
        Index(value = ["lastPlayedAt"])
    ]
)
data class Progress(
    val userId: String,
    val cardId: String,
    val isCompleted: Boolean = false,
    val playCount: Int = 0,
    val totalDurationMs: Long = 0,        // 累计学习时长
    val lastDurationMs: Long = 0,         // 最近一次学习时长
    val interactionSuccess: Boolean = false,
    val firstPlayedAt: Long? = null,
    val lastPlayedAt: Long? = null,
    val completedAt: Long? = null
)
```

#### 2.2.5 下载管理表（downloads）
```kotlin
@Entity(
    tableName = "downloads",
    indices = [
        Index(value = ["status"]),
        Index(value = ["priority"])
    ]
)
data class Download(
    @PrimaryKey
    val downloadId: String = UUID.randomUUID().toString(),
    val themeId: String,
    val url: String,
    val localPath: String,
    val status: String,                   // "pending"|"downloading"|"completed"|"failed"|"cancelled"
    val totalBytes: Long,
    val downloadedBytes: Long = 0,
    val progress: Float = 0f,
    val priority: Int = 0,                // 优先级
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val checksum: String?,                // MD5校验值
    val error: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null
)
```

#### 2.2.6 奖励记录表（rewards）
```kotlin
@Entity(
    tableName = "rewards",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["earnedAt"])
    ]
)
data class Reward(
    @PrimaryKey
    val rewardId: String = UUID.randomUUID().toString(),
    val userId: String,
    val type: String,                     // "star" | "sticker" | "badge"
    val name: String,
    val description: String?,
    val iconPath: String?,
    val relatedCardId: String?,
    val relatedThemeId: String?,
    val displayPosition: String?,         // 贴纸墙位置JSON
    val isSpecial: Boolean = false,       // 特殊奖励（如红色徽章）
    val earnedAt: Long
)
```

#### 2.2.7 会话日志表（session_logs）
```kotlin
@Entity(
    tableName = "session_logs",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["startTime"])
    ]
)
data class SessionLog(
    @PrimaryKey
    val sessionId: String = UUID.randomUUID().toString(),
    val userId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val durationMs: Long = 0,
    val cardsPlayed: Int = 0,
    val cardsCompleted: Int = 0,
    val interactionCount: Int = 0,
    val kwsCommandCount: Int = 0,         // 语音命令使用次数
    val pauseCount: Int = 0,
    val deviceInfo: String?                // JSON格式的设备信息
)
```

#### 2.2.8 特征日志表（feature_logs）
```kotlin
@Entity(
    tableName = "feature_logs",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["timestamp"])
    ]
)
data class FeatureLog(
    @PrimaryKey(autoGenerate = true)
    val logId: Long = 0,
    val userId: String,
    val sessionId: String?,
    val cardId: String?,
    val eventType: String,                // "card_start"|"card_complete"|"interaction"|"kws_command"|"reward_earned"
    val eventData: String?,               // JSON格式的事件数据
    val features: String?,                // JSON格式的特征数据（用于推荐算法）
    val timestamp: Long
)
```

### 2.3 类型转换器（TypeConverters）
```kotlin
class Converters {
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        return value?.split(",") ?: emptyList()
    }

    @TypeConverter
    fun fromListString(list: List<String>?): String {
        return list?.joinToString(",") ?: ""
    }
}
```

### 2.4 数据访问对象（DAO）示例

#### 2.4.1 进度DAO
```kotlin
@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE userId = :userId ORDER BY lastPlayedAt DESC")
    fun getUserProgress(userId: String): Flow<List<Progress>>

    @Query("""
        SELECT COUNT(*) FROM progress 
        WHERE userId = :userId AND isCompleted = 1
    """)
    fun getCompletedCount(userId: String): Flow<Int>

    @Query("""
        SELECT c.* FROM cards c
        LEFT JOIN progress p ON c.cardId = p.cardId AND p.userId = :userId
        WHERE c.themeId = :themeId
        ORDER BY c.sortOrder
    """)
    fun getThemeCardsWithProgress(userId: String, themeId: String): Flow<List<CardWithProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: Progress)

    @Transaction
    suspend fun completeCard(userId: String, cardId: String, durationMs: Long) {
        val progress = getProgress(userId, cardId) ?: Progress(userId, cardId)
        upsertProgress(
            progress.copy(
                isCompleted = true,
                playCount = progress.playCount + 1,
                totalDurationMs = progress.totalDurationMs + durationMs,
                lastDurationMs = durationMs,
                lastPlayedAt = System.currentTimeMillis(),
                completedAt = progress.completedAt ?: System.currentTimeMillis()
            )
        )
    }
}
```

## 3. DataStore 设计

### 3.1 用户偏好设置
```kotlin
// UserPreferences.proto
syntax = "proto3";

message UserPreferences {
    string prefer_color = 1;              // 偏好颜色
    ColorIntensity color_intensity = 2;   // 颜色强度
    int32 daily_limit_minutes = 3;        // 每日时长限制
    int32 session_limit_minutes = 4;      // 单次时长限制
    bool enable_kws = 5;                  // 启用语音控制
    bool enable_subtitles = 6;            // 启用字幕
    bool enable_word_highlight = 7;       // 启用单词点读
    string subtitle_language = 8;         // 字幕语言
    bool enable_haptic = 9;               // 启用触觉反馈
    float animation_speed = 10;           // 动画速度
    
    enum ColorIntensity {
        LOW = 0;
        MEDIUM = 1;
        HIGH = 2;
    }
}
```

### 3.2 应用配置
```kotlin
// AppConfig.proto
syntax = "proto3";

message AppConfig {
    string api_base_url = 1;              // AI服务基础URL
    string api_key = 2;                   // API密钥（加密存储）
    repeated string provider_chain = 3;    // 提供商链
    int32 connect_timeout_ms = 4;        // 连接超时
    int32 read_timeout_ms = 5;            // 读取超时
    bool enable_analytics = 6;            // 启用分析
    string device_id = 7;                 // 设备ID
    int64 last_sync_time = 8;            // 最后同步时间
    map<string, string> feature_flags = 9; // 功能开关
}
```

## 4. 文件存储结构

### 4.1 目录结构
```
/data/data/com.aiedu.app/
├── databases/
│   └── app.db                        # Room数据库
├── datastore/
│   ├── user_preferences.pb           # 用户偏好
│   └── app_config.pb                # 应用配置
├── files/
│   ├── themes/                      # 主题资源
│   │   ├── theme_001/              # 预装主题
│   │   │   ├── cards/
│   │   │   │   ├── card_001/
│   │   │   │   │   ├── animation.json
│   │   │   │   │   ├── audio_zh.mp3
│   │   │   │   │   ├── audio_en.mp3
│   │   │   │   │   └── subtitle.vtt
│   │   │   │   └── card_002/
│   │   │   └── cover.webp
│   │   └── theme_002/              # 下载的主题
│   ├── downloads/                   # 临时下载目录
│   ├── rewards/                     # 奖励资源
│   │   ├── stickers/
│   │   └── badges/
│   └── models/                      # AI模型文件
│       ├── kws_model.tflite
│       └── intent_model.onnx
└── cache/                          # 缓存目录
    ├── images/
    └── temp/
```

## 5. 数据库操作最佳实践

### 5.1 事务处理
```kotlin
@Dao
abstract class ThemeDao {
    @Transaction
    open suspend fun downloadThemeComplete(
        themeId: String,
        cards: List<Card>,
        downloadId: String
    ) {
        // 1. 插入所有卡片
        insertCards(cards)
        
        // 2. 更新下载状态
        updateDownloadStatus(downloadId, "completed")
        
        // 3. 更新主题状态
        updateThemeDownloaded(themeId, true)
    }
}
```

### 5.2 数据迁移
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 添加新字段
        database.execSQL(
            "ALTER TABLE users ADD COLUMN age_group TEXT DEFAULT '3' NOT NULL"
        )
    }
}
```

### 5.3 性能优化
- 使用索引优化查询性能
- 批量操作使用事务
- 大数据查询使用分页
- 使用Flow观察数据变化
- 定期清理过期数据

## 6. 数据安全

### 6.1 敏感数据加密
```kotlin
// API Key加密存储
val encryptedFile = EncryptedFile.Builder(
    File(filesDir, "api_config"),
    context,
    "api_config",
    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
).build()
```

### 6.2 数据备份
- 不备份敏感数据（API Key等）
- 学习进度支持本地备份
- 使用Android Backup Service

## 7. 存储空间管理

### 7.1 存储配额
- 预装内容：约100MB
- 每个主题：30-50MB
- 总存储上限：建议500MB

### 7.2 清理策略
```kotlin
// 定期清理
- 30天未使用的下载主题
- 已完成的下载任务记录
- 过期的会话日志（保留30天）
- 缓存文件（LRU策略）
```

## 8. 监控指标

### 8.1 数据库性能
- 查询响应时间
- 事务执行时间
- 数据库文件大小
- 索引使用率

### 8.2 存储使用
- 各类文件占用空间
- 存储增长趋势
- 清理效果

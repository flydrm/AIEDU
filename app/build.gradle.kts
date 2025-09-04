/**
 * 应用模块构建文件
 * 
 * 功能说明：
 * 配置应用模块的编译选项、依赖项和构建特性
 * 
 * @author AI启蒙时光开发团队
 * @date 2025-01-03
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.family.kidsedu"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.family.kidsedu"
        minSdk = 24  // Android 7.0
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            
            // Debug构建配置
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
            buildConfigField("String", "API_BASE_URL", "\"https://api.openai.com/v1/\"")
            
            // 资源配置
            resValue("string", "app_name", "AI启蒙时光(开发版)")
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Release构建配置
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
            buildConfigField("String", "API_BASE_URL", "\"https://api.openai.com/v1/\"")
            
            // 资源配置
            resValue("string", "app_name", "AI启蒙时光")
            
            // 签名配置（需要配置签名文件）
            // signingConfig = signingConfigs.getByName("release")
        }
    }
    
    // 产品风味（可选，用于不同版本）
    flavorDimensions += "version"
    productFlavors {
        create("standard") {
            dimension = "version"
            // 标准版配置
        }
        
        create("premium") {
            dimension = "version"
            applicationIdSuffix = ".premium"
            versionNameSuffix = "-高级版"
            
            // 高级版特有配置
            buildConfigField("boolean", "PREMIUM_FEATURES", "true")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    buildFeatures {
        viewBinding = true  // 启用视图绑定，简化View操作
    }
}

dependencies {
    // AndroidX核心库
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")  // 响应式布局
    
    // 协程支持（用于异步操作和AI调用）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // 网络请求（用于AI API调用）
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // JSON解析（处理AI响应）
    implementation("com.google.code.gson:gson:2.10.1")
    
    // 图片加载库（可选，用于加载网络图片）
    implementation("io.coil-kt:coil:2.5.0")
    
    // 测试依赖
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
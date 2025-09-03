/**
 * 根项目构建文件
 * 
 * 功能说明：
 * 定义整个项目的构建配置和插件版本
 * 
 * @author AI启蒙时光开发团队
 * @date 2025-01-03
 */

// 顶级构建文件，可以在此添加所有子项目/模块通用的配置选项
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
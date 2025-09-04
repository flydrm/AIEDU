/**
 * Gradle设置文件
 * 
 * 功能说明：
 * 定义项目的模块结构和仓库配置
 * 
 * @author AI启蒙时光开发团队
 * @date 2025-01-03
 */

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AI启蒙时光"
include(":app")
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // Web 版：Kotlin/JS 浏览器目标（与 android/ios/jvm 共享 commonMain UI）
    js {
        browser()
    }

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    android {
       namespace = "com.xlin.travel.app.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_17
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }

    // 非 Web 平台共享的 SQLite 实现（ChatDatabase / DAO / SqliteChatRepository）：
    // 浏览器端没有可同步调用的 SQLite，这部分代码不能进 js 目标的 commonMain 编译，
    // 用中间源集挂到 android / ios / jvm 三个 leaf 之下（expect/actual 仍由 leaf 提供）。
    // 注：Gradle 会报「Explicit .dependsOn edges」警告，但该写法在本项目 js/android/jvm 编译均验证通过；
    // applyDefaultHierarchyTemplate 的 group 方案在 Kotlin 2.4.20 + android() 库 DSL 下无法把
    // dbSqliteMain 纳入 compileAndroidMain 的源集，故保留手写方式。
    sourceSets {
        val dbSqliteMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain { dependsOn(dbSqliteMain) }
        iosMain { dependsOn(dbSqliteMain) }
        jvmMain { dependsOn(dbSqliteMain) }

        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.androidx.activity.compose)
            // 拍照走 ACTION_IMAGE_CAPTURE + FileProvider（androidx.core.content.FileProvider）
            implementation(libs.androidx.core.ktx)
            implementation(libs.koog.http.client.ktor)
            implementation(libs.ktor.clientCio)
            implementation(libs.androidx.sqlite.bundled)

        }
        commonMain.dependencies {
            api(project(":core"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.koog.agents)
            implementation(libs.koog.agents.additions)
            implementation(libs.koog.http.client.ktor)
            // 数据库驱动接口跨平台共享；具体 BundledSQLiteDriver 由各平台源集引入（Web 无该产物）
            implementation(libs.androidx.sqlite)
            implementation(libs.koog.agents.features.memory)
            // 登录 / 注册接口客户端（引擎由各平台 actual 提供）
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientContentNegotiation)
            implementation(libs.ktor.serializationKotlinxJsonMp)
            // TTS 语音合成走 DashScope WebSocket（run-task 协议），引擎由各平台 actual 提供
            implementation(libs.ktor.clientWebsockets)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        // iOS 公共 actual 放 iosMain：由 Kotlin 默认层级模板自动挂到 commonMain 之下，
        // 不能再用 `by creating` + 手写 dependsOn，否则 iosMain 脱离 commonMain，
        // expect/actual 互相看不见，编译出一串 "has no corresponding 'expect'" 级联错误
        iosMain.dependencies {
            implementation(libs.koog.agents)
            implementation(libs.koog.agents.additions)
            implementation(libs.koog.http.client.ktor)
            implementation(libs.ktor.clientDarwin)
            implementation(libs.androidx.sqlite.bundled)
        }

        jvmMain.dependencies {
            // JVM
            implementation(libs.ktor.clientCio)
            implementation(libs.androidx.sqlite.bundled)
        }

        // Web（Kotlin/JS）：ktor 浏览器引擎；无本地 SQLite / 录音 / TTS，对应 actual 为内存库与桩实现
        jsMain.dependencies {
            implementation(libs.ktor.clientJs)
        }
        jvmTest.dependencies {
            implementation(libs.koog.agents)
            implementation(libs.koog.agents.additions)
            implementation(libs.koog.http.client.ktor)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
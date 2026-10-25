import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
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

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koog.http.client.ktor)
            implementation(libs.ktor.clientCio)

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
            implementation(libs.androidx.sqlite)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.koog.agents.features.memory)
            // 登录 / 注册接口客户端（引擎由各平台 actual 提供）
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientContentNegotiation)
            implementation(libs.ktor.serializationKotlinxJsonMp)
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
        }

        jvmMain.dependencies {
            // JVM
            implementation(libs.ktor.clientCio)
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
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
dependencies {
    implementation(project(":app:shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.xlin.travel"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        create("travelRelease") {
            keyAlias = providers.gradleProperty("sign.keyAlias").orNull ?: ""
            keyPassword = providers.gradleProperty("sign.keyPassword").orNull ?: ""
            storeFile = providers.gradleProperty("sign.keyStore").orNull?.let { file(it) }
            storePassword = providers.gradleProperty("sign.storePassword").orNull ?: ""
        }
    }

    defaultConfig {
        applicationId = "com.xlin.travel"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // 版本号：CI（GitHub Actions）通过 -Prelease.versionCode/-Prelease.versionName 注入，本地不传时保持默认
        versionCode = providers.gradleProperty("release.versionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("release.versionName").orNull ?: "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 签名：CI 通过 -Psign.* 属性注入；本地未配置时保持未签名（assembleRelease 仍可通过 compileSdk 检查）
            val signingProps = providers.gradleProperty("sign.keyStore")
            if (signingProps.orNull != null) {
                signingConfig = signingConfigs.getByName("travelRelease")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

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

    // 签名属性查找顺序：-Psign.* / gradle.properties（CI 注入）优先，再回退到 local.properties（仅本地，不入版本库）
    // 注意：providers.gradleProperty 读不到 local.properties，所以需手动加载
    val localProperties = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    fun signingProp(name: String): String? =
        providers.gradleProperty("sign.$name").orNull ?: localProperties.getProperty("sign.$name")

    signingConfigs {
        create("travelRelease") {
            keyAlias = signingProp("keyAlias") ?: ""
            keyPassword = signingProp("keyPassword") ?: ""
            storeFile = signingProp("keyStore")?.let { rootProject.file(it) }
            storePassword = signingProp("storePassword") ?: ""
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
            // 签名：CI 通过 -Psign.* 注入，本地配在 local.properties；两者都没有时保持未签名
            if (signingProp("keyStore") != null) {
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
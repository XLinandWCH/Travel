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

    // 签名属性查找顺序：-Psign.*（命令行）→ signing.properties（CI 在工作区根目录生成）
    // → local.properties（仅本地，不入版本库）；providers.gradleProperty 读不到后两个文件，需手动加载
    fun signingProps(fileName: String) = Properties().apply {
        val f = rootProject.file(fileName)
        if (f.exists()) f.inputStream().use { load(it) }
    }
    val ciProperties = signingProps("signing.properties")
    val localProperties = signingProps("local.properties")

    // 空白值按“未配置”处理，避免 -Psign.keyAlias= 这种空参数把签名器配成半残状态
    fun signingProp(name: String): String? {
        val key = "sign.$name"
        return providers.gradleProperty(key).orNull?.takeIf { it.isNotBlank() }
            ?: ciProperties.getProperty(key)?.takeIf { it.isNotBlank() }
            ?: localProperties.getProperty(key)?.takeIf { it.isNotBlank() }
    }

    val releaseSigning = signingConfigs.create("travelRelease") {
        keyAlias = signingProp("keyAlias")
        keyPassword = signingProp("keyPassword")
        // storeFile 必须是绝对路径或相对仓库根目录的路径，rootProject.file 两种都能解析
        storeFile = signingProp("keyStore")?.let { rootProject.file(it) }
        storePassword = signingProp("storePassword")
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
            // 签名：证书 + 口令 + 别名齐全才启用，否则产物是 *-unsigned.apk（装不上）
            val signed = signingProp("keyStore") != null && signingProp("keyAlias") != null &&
                signingProp("keyPassword") != null && signingProp("storePassword") != null
            if (signed) signingConfig = releaseSigning
            logger.lifecycle(
                "[签名检查] Release " + if (signed) {
                    "已配置证书 ${releaseSigning.storeFile?.name}，产物可直接安装"
                } else {
                    "未配置完整签名，产物为未签名包（安装会报 INSTALL_PARSE_FAILED_NO_CERTIFICATES）"
                }
            )
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
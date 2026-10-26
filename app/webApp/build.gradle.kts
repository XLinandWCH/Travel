plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // 网页版入口：仅 Kotlin/JS 目标（wasmJs 需要 WasmGC，鸿蒙旧内核浏览器不支持，暂不启用）
    js {
        browser {
            // CI / 无浏览器环境下跳过 karma 测试
            testTask {
                enabled = false
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":app:shared"))

            implementation(libs.compose.ui)
        }
    }
}

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktor) apply false
}

// Kotlin/JS 测试工作区默认引用 JetBrains 自维护的 github karma 分支（github:Kotlin/karma 版本串），
// 国内网络常无法访问 raw.githubusercontent.com，导致 kotlinNpmInstall 整体失败。
// 网页版不跑 js 单测：安装前把测试包里的 github 引用换成 npm 镜像上的同名普通版本，
// 并把项目根的镜像配置（.yarnrc / .npmrc）复制到 yarn 的工作目录 build/js。
// kotlinNpmInstall 由 NodeJsRootPlugin 注册在根项目上（配置脚本早期尚不存在，需用 matching 懒匹配），
// 用 allprojects 确保根项目与各子项目的同名任务都能挂上补丁。
allprojects {
    tasks.matching { it.name == "kotlinNpmInstall" }.configureEach {
        val jsDir = layout.buildDirectory.dir("js")
        val rootDir = rootProject.layout.projectDirectory.asFile
        doFirst {
            val packages = jsDir.get().asFile.resolve("packages")
            packages.listFiles { f: java.io.File -> f.isDirectory && f.name.endsWith("-test") }?.forEach { dir ->
                val pkg = File(dir, "package.json")
                if (pkg.isFile) {
                    pkg.writeText(pkg.readText().replace(Regex("\"github:Kotlin/karma#[^\"]+\""), "\"6.4.4\""))
                }
            }
            sequenceOf(".yarnrc", ".npmrc").forEach { name ->
                val src = File(rootDir, name)
                if (src.isFile) src.copyTo(File(jsDir.get().asFile, name), overwrite = true)
            }
        }
    }
}
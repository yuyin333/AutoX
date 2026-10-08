plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    id("de.undercouch.download") version "5.6.0"
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(versions.javaVersionInt))
    }
}
android {
    namespace = "com.aiselp.autojs.codeeditor"
    compileSdk = versions.compile

    defaultConfig {
        minSdk = versions.mini

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = compose_version
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.constraintlayout)
    implementation(libs.kotlinx.coroutines.android)
    api(libs.androidx.webkit)
    implementation(libs.google.gson)
    implementation(libs.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(project(":autojs"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

tasks.register("downloadEditor") {
    // 编辑器产物由本仓库自己的 fork 提供，不再依赖原作者仓库
    val repo = "yuyin333/vscode-mobile"
    val tag = "v0.4.0"
    val uri = "https://github.com/$repo/releases/download/$tag/dist.zip"
    val assetsDir = File(projectDir, "/src/main/assets/codeeditor")
    val versionFile = File(assetsDir, "version.txt")
    // 缓存标记必须带上仓库来源：只记 tag 的话，换仓库后仍会命中旧缓存，
    // 于是一份来自旧仓库（甚至已失效）的产物会被静默复用 —— 构建看着正常，
    // 实际用的却不是当前配置的来源。
    val cacheMarker = "$repo@$tag"
    doFirst {
        logger.log(LogLevel.LIFECYCLE, "start downloadEditor")
        assetsDir.mkdirs()
        if (versionFile.isFile && versionFile.readText().trim() == cacheMarker) {
            logger.log(LogLevel.LIFECYCLE, "skip download")
            return@doFirst
        }
        download.run {
            src(uri)
            dest(File(assetsDir, "dist.zip"))
        }
        versionFile.writeText(cacheMarker)
    }
}
tasks.findByName("preBuild")?.dependsOn("downloadEditor")
tasks.findByName("preDebugBuild")?.dependsOn("downloadEditor")
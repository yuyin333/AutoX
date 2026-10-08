import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.io.FileNotFoundException
import java.util.Base64
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.jakewharton.butterknife")
    id("kotlin-kapt")//Deprecated!!
    id("com.google.devtools.ksp")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(versions.javaVersionInt))
    }
}

android {
    namespace = "org.autojs.autoxjs"
    compileSdk = versions.compile
    defaultConfig {
        applicationId = "org.autojs.autoxjs"
        minSdk = versions.mini
        targetSdk = versions.target
        versionCode = versions.appVersionCode
        versionName = versions.appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//        multiDexEnabled = true
        buildConfigField("boolean", "isMarket", "false")

        resourceConfigurations.addAll(
            listOf("zh-rCN", "en", "es", "ar", "ja", "zh_TW", "fr", "de", "it", "ko", "ru", "tr", "lt")
        )
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = compose_version
    }
    lint {
        abortOnError = false
        disable.addAll(listOf("MissingTranslation", "ExtraTranslation"))
    }

    splits {
        // Configures multiple APKs based on ABI.
        abi {
            // Enables building multiple APKs per ABI.
            isEnable = true
            // By default all ABIs are included, so use reset() and include to specify that we only
            // want APKs for x86 and x86_64.
            // Resets the list of ABIs that Gradle should create APKs for to none.
            reset()
            // Specifies a list of ABIs that Gradle should create APKs for.
            include("arm64-v8a")
            // Specifies that we do not want to also generate a universal APK that includes all ABIs.
            isUniversalApk = false
        }
    }
    // 签名配置优先级：① CI 环境变量(KEYSTORE_BASE64) ② 本地 app/signing.properties ③ 本地环境变量(KEYSTORE_FILE)
    val signing = run {
        // ① CI：从 Base64 解码 keystore
        if (System.getenv("CI") == "true" && !System.getenv("KEYSTORE_BASE64").isNullOrEmpty()) {
            val file = File.createTempFile("key", "jks")
            file.writeBytes(Base64.getDecoder().decode(System.getenv("KEYSTORE_BASE64")))
            signingConfigs.create("release") {
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                this.storeFile = file
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
        // ② 本地：app/signing.properties（不入库，字段见 README「本地一键构建与签名」）
        else {
            val propsFile = rootProject.file("app/signing.properties")
            if (propsFile.exists()) {
                val props = Properties().apply { load(propsFile.inputStream()) }
                val storeFilePath = props.getProperty("STORE_FILE")
                val storePassword = props.getProperty("STORE_PASSWORD")
                val keyAlias = props.getProperty("KEY_ALIAS")
                val keyPassword = props.getProperty("KEY_PASSWORD")
                // 相对路径必须按仓库根解析：File(path) 用的是 JVM 工作目录，
                // 一旦从 app/ 目录启动 Gradle 就会解析失败 → storeFile.exists() 为
                // false → 静默退化成未签名 release（只有一行 warn，极易漏看）。
                val storeFile = storeFilePath?.takeIf { it.isNotBlank() }?.let { rootProject.file(it) }
                if (storeFile != null && storeFile.exists() && !storePassword.isNullOrBlank() && !keyAlias.isNullOrBlank()) {
                    signingConfigs.create("release") {
                        enableV1Signing = true
                        enableV2Signing = true
                        enableV3Signing = true
                        this.storeFile = storeFile
                        this.storePassword = storePassword
                        this.keyAlias = keyAlias
                        this.keyPassword = keyPassword ?: storePassword
                    }
                } else {
                    logger.warn("⚠️ app/signing.properties 存在，但 STORE_FILE($storeFilePath) 缺失或密码/别名不完整，release 将不签名")
                    null
                }
            } else null
        }
    }

    buildTypes {
        named("debug") {
            isShrinkResources = false
            isMinifyEnabled = false
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"
                )
            )
        }
        named("release") {
            if (signing != null) {
                signingConfig = signing
            }
            // 代码混淆 + 精简：减小体积，同时抹掉类名/字符串里可被风控识别的特征。
            // 资源压缩（isShrinkResources）单独一步再开 —— 它依赖动态资源查找，
            // 需要真机验证后再启用，避免混淆与资源压缩两个变量混在一起排查。
            isShrinkResources = false
            isMinifyEnabled = true
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"
                )
            )
        }
    }

    flavorDimensions.add("channel")
    productFlavors {
        create("common") {
            applicationIdSuffix = ".common"
            versionCode = versions.appVersionCode
            versionName = versions.appVersionName
            buildConfigField("String", "CHANNEL", "\"common\"")
            manifestPlaceholders.putAll(mapOf("appName" to "@string/app_name"))
        }
        create("v7") {
            applicationIdSuffix = ".v7"
            versionCode = versions.devVersionCode
            versionName = versions.devVersionName
            buildConfigField("String", "CHANNEL", "\"v7\"")
            manifestPlaceholders.putAll(mapOf("appName" to "Autox.js v7"))
        }
        create("v7_mini") {
            applicationIdSuffix = ".v7"
            buildConfigField("String", "CHANNEL", "\"v7\"")
            manifestPlaceholders.putAll(mapOf("appName" to "Autox.js v7"))
        }
    }
    applicationVariants.all {
        val variant = this
        if (variant.flavorName == "v7_mini") {
            // 只裁掉 codeeditor（体积大头）。template.apk 必须保留：
            // 打包功能要靠它自举 —— v7_mini 若不带内置模板，一旦用户手机上那份
            // 「导入模板」失效，就只剩「必须手动导入才能打包」一条死路。
            mergeAssetsProvider.configure {
                doLast {
                    // 整个目录递归删除。原先用 fileTree(outputDir) { include("codeeditor/**/*") }
                    // 只会删文件、留下空的 codeeditor 目录树。
                    delete(outputDir.get().asFile.resolve("codeeditor"))
                }
            }
        }
    }
    sourceSets {
        getByName("main") {
            res.srcDirs("src/main/res", "src/main/res-i18n")
            aidl {
                srcDirs("src/main/aidl", "src/main/java")
            }
        }
    }
    configurations.all {
        resolutionStrategy.force("com.google.code.findbugs:jsr305:3.0.1")
        exclude(group = "org.jetbrains", module = "annotations-java5")
//        exclude(group = "com.atlassian.commonmark",) module = "commonmark"
        exclude(group = "com.github.atlassian.commonmark-java", module = "commonmark")
    }

    packaging {
        //ktor netty implementation("io.ktor:ktor-server-netty:2.0.1")
        resources.pickFirsts.addAll(
            listOf(
                "META-INF/io.netty.versions.properties", "META-INF/INDEX.LIST"
            )
        )
    }

}

dependencies {

    implementation(platform(libs.compose.bom))
    // Deprecated!!
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.webkit)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.mlkit.common)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)

    androidTestImplementation(libs.espresso.core)
    testImplementation(libs.junit)
    // Kotlin携程
    implementation(libs.kotlinx.coroutines.android)
    // ButterKnife Deprecated!!
    implementation("com.jakewharton:butterknife:10.2.1")
    kapt("com.jakewharton:butterknife-compiler:10.2.3")
    // Android supports
    implementation(libs.preference.ktx)
    implementation(libs.appcompat) //

    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window.size)
    implementation(libs.compose.material3.adaptive.navigation.suite)
    // Personal libraries  Deprecated!!
    implementation("com.github.hyb1996:MutableTheme:1.0.0")
    // Material Dialogs  Deprecated!!
    implementation("com.afollestad.material-dialogs:core:0.9.2.3")
    // Common Markdown
    implementation("com.github.atlassian:commonmark-java:commonmark-parent-0.9.0")
    // Android issue reporter (a github issue reporter)
    implementation("com.heinrichreimersoftware:android-issue-reporter:1.3.1")
    //MultiLevelListView
    implementation("com.github.hyb1996:android-multi-level-listview:1.1")
    //Licenses Dialog  Deprecated!!
    implementation("de.psdev.licensesdialog:licensesdialog:2.2.0")
    //Expandable RecyclerView
    implementation("com.bignerdranch.android:expandablerecyclerview:3.0.0-RC1")
    //FlexibleDivider
    implementation("com.yqritc:recyclerview-flexibledivider:1.4.0")

    // RxJava  Deprecated!!
    implementation(libs.rxjava2)
    implementation(libs.rxjava2.rxandroid)
    // Retrofit
    implementation(libs.retrofit2.retrofit)
    implementation(libs.retrofit2.converter.gson)
    debugImplementation(libs.leakcanary.android)
    //Glide
    implementation(libs.glide)
    ksp(libs.glide.ksp)
    //joda time
    implementation("net.danlew:android.joda:2.10.14")
    // Tasker Plugin
    implementation("com.twofortyfouram:android-plugin-client-sdk-for-locale:4.0.3")
    // MaterialDialogCommon
    implementation("com.afollestad.material-dialogs:commons:0.9.2.3")
    // WorkManager
    implementation(libs.androidx.work)
    // Optional, if you use support library fragments:
    implementation(project(":autojs"))
    implementation(project(":apkbuilder"))
    implementation(project(":codeeditor"))

    // ViewModel
    implementation(libs.lifecycle.viewmodel.ktx)
    // ViewModel utilities for Compose
    implementation(libs.lifecycle.viewmodel.compose)
    // Lifecycles only (without ViewModel or LiveData)
    implementation(libs.lifecycle.runtime.ktx)
    // Saved state module for ViewModel
    implementation(libs.lifecycle.viewmodel.savedstate)
    implementation(libs.lifecycle.service)
    // Annotation processor
    ksp(libs.lifecycle.compiler)

    implementation(libs.androidx.savedstate.ktx)
    implementation(libs.androidx.savedstate)

    implementation(libs.bundles.ktor)
    //qr scan
    implementation(libs.quickie.bundled)
    //Fab button with menu, please do not upgrade, download dependencies will be error after upgrade
    //noinspection GradleDependency
    implementation("com.leinardi.android:speed-dial.compose:1.0.0-alpha03")
    //TextView markdown
    implementation("io.noties.markwon:core:4.6.2")
    implementation(libs.androidx.viewpager2)
    implementation(libs.coil.compose)
}

fun copyTemplateToAPP(isDebug: Boolean, to: File) {
    val outName = if (isDebug) "template-debug" else "template-release"
    val outFile = project(":inrt").buildOutputs.named(outName).get().outputFile
//    logger.error("buildTemplate from: $outFile")
    copy {
        from(outFile)
        into(to)
        delete(File(to, "template.apk"))
        rename(outFile.name, "template.apk")
    }
    logger.info("buildTemplate success, debugMode: $isDebug")
}

val assetsDir = File(projectDir, "src/main/assets")

// 无条件依赖：模板 APK 与主 App 是同一份源码的产物，改了 autojs/inrt 就必须让
// 模板跟着重建。原先「文件已存在就不加依赖」的配置期判断会让模板永久停留在旧
// 版本（改源码后打包出的 App 仍是旧服务名/旧 dex），且 gradlew clean assemble
// 单次调用时 clean 已在执行期删掉模板、却不重建，产物直接缺 template.apk。
// 是否真的重新执行交给 Gradle 的 up-to-date 机制决定：输入未变时毫秒级跳过。
tasks.named("preBuild").dependsOn("buildTemplateApp")

tasks.register("buildTemplateApp") {
    group = "build"
    description = "把 :inrt 的模板 APK 复制为 assets/template.apk，供 App 内打包功能使用"
    dependsOn(":inrt:assembleTemplateRelease")
    // 声明输入输出后，Gradle 才能在「inrt 产物未变」时跳过本任务（否则每次白拷 20MB）。
    // 输入必须用 provider 惰性求值：本工程开启了 org.gradle.configureondemand，配置期
    // :inrt 尚未被配置，直接访问它的 buildOutputs 会抛
    // "Extension with name 'buildOutputs' does not exist"。provider 的求值发生在
    // 本任务的输入快照阶段，此时 dependsOn 的上游已经构建完毕，扩展也早已注册。
    inputs.file(provider { project(":inrt").buildOutputs.named("template-release").get().outputFile })
    outputs.file(File(assetsDir, "template.apk"))
    doFirst {
        copyTemplateToAPP(false, assetsDir)
    }
}
tasks.register("buildDebugTemplateApp") {
    group = "build"
    dependsOn(":inrt:assembleTemplateDebug")
    doFirst {
        copyTemplateToAPP(true, assetsDir)
    }
}
tasks.named("clean").configure {
    doFirst {
        delete(File(assetsDir, "template.apk"))
    }
}
tasks.register("buildDocs") {
    group = "build"
    doLast {
        val v2DocDir = File(rootProject.projectDir, "docs/v2")
        val jsApiDir = File(rootProject.projectDir, "autojs/src/main/js/v7-api")
        if (!v2DocDir.isDirectory) {
            logger.error("run command: `git submodule update --init --recursive` install docs/v2")
            throw FileNotFoundException("${v2DocDir.path} not found")
        }
        val buildFile = File.createTempFile("buildJs", ".mjs")
        exec {
            workingDir(jsApiDir)
            buildFile.writeText(
                """
                import { execSync } from 'child_process'
                execSync('npm install', { stdio: 'inherit' })
                execSync('npm run docs', { stdio: 'inherit' })
            """.trimIndent()
            )
            execCommand("node " + buildFile.path)
        }
        copy {
            from(File(jsApiDir, "docs"))
            delete(File(v2DocDir, "docs/nodejs/modules"))
            into(File(v2DocDir, "docs/nodejs/modules"))
        }
        exec {
            workingDir(v2DocDir)
            buildFile.writeText(
                """
                import { execSync } from 'child_process'
                execSync('npm install', { stdio: 'inherit' })
                execSync('npm run build', { stdio: 'inherit' })
            """.trimIndent()
            )
            execCommand("node " + buildFile.path)
        }
        copy {
            from(File(v2DocDir, "build"))
            into(File(projectDir, "src/main/assets/docs/v2"))
        }
        buildFile.delete()
    }
}

// ===== 本地一键构建（串联：JS 模块编译 -> 模板 APK -> 签名 release） =====
// 用法：
//   ./gradlew buildV7ReleaseLocal          # 构建 v7 签名 release
//   ./gradlew buildV7MiniReleaseLocal       # 构建 v7_mini 签名 release（体积更小）
// 需先配置 app/signing.properties 或 CI 环境变量，否则产物为【未签名】release。
listOf("V7" to "v7", "V7Mini" to "v7_mini").forEach { (taskSuffix, flavor) ->
    tasks.register("build${taskSuffix}ReleaseLocal") {
        group = "build"
        description = "本地一键构建 $flavor 签名 release（依赖 :autojs:buildJsModule 与 app:buildTemplateApp）"
        dependsOn(":autojs:buildJsModule")
        dependsOn("buildTemplateApp")
        dependsOn(":app:assemble${flavor.replaceFirstChar { it.uppercase() }}Release")
    }
}
# AutoX (setup-v7) — AI 编码上下文

> 本文件是**可迁移的项目上下文**：给 GitHub Copilot / 其它 AI 编码工具读，也可直接拷到别的仓库当模板。
> VS Code 会自动加载 `.github/copilot-instructions.md`。其它工具可把它复制/改名为根目录 `AGENTS.md`。
>
> 生效方式：把本文件当**硬约束**读，不要当参考建议。凡标「实测」的结论都是踩过坑得到的，
> 不要用"一般来说""通常"去推翻它。凡标「禁止」的，是已经证明会出事或会浪费几十分钟的。

---

## 0. 三十秒速览（先看这段）

| 项 | 值 |
|---|---|
| 项目 | AutoX = Auto.js 的 fork（上游 `aiselp/AutoX`），Android 自动化脚本 App |
| 当前分支 | `setup-v7`；包名 `org.autojs.autoxjs.v7`，版本 `7.2.4 (724)` |
| 工具链 | JDK 17 / Gradle 8.7 / AGP 8.5.0 / Kotlin 1.9.25 / compileSdk 34 / minSdk 27 |
| release 状态 | **R8 已开启**（`isMinifyEnabled=true`），资源压缩仍未开 |
| 最易踩的坑 | R8 打断"按名字查找"（反射 / 类名 / 泛型签名 / Rhino 调 Java），共 5 类症状 |
| 构建入口 | `./gradlew buildV7ReleaseLocal`（一键：JS 模块 → 模板 APK → 签名 release） |
| 仓库必须放 | **纯 ASCII 路径**（中文路径会让 aidl depfile 以 GBK 写出、Gradle 以 UTF-8 读，抛 `MalformedInputException`） |

三条最容易造成事故的规则：

1. **不要为了让某个功能"先跑起来"就调宽 R8 规则**。调宽有实测成本（`-keep class io.netty.** { *; }` 会让 R8 从 2min 涨到 22min）。
2. **改完代码必须真机验证**，本项目的功能大量依赖 Manifest、R8 产物、模板 APK、无障碍绑定，编译通过 ≠ 能用。
3. **检索代码只用 `git grep`**，禁止 `--exclude-dir=build`（原因见 §11）。

---

## 1. 项目定位与分支拓扑

- `origin` = `yuyin333/AutoX`（个人 fork）；`upstream` = `aiselp/AutoX`（原作者 7.2.x 主线）。
- **本仓库是浅克隆**：`.git/shallow` 只含 `f1066706`，它被本地当作根提交。
  因此 `git rev-list --count` 比上游少几千个提交是**假象**。
  判断真实差异只能用 `git diff $(git merge-base A B) B --stat`。
- 合并上游流程（已验证可行）：
  ```bash
  git fetch upstream setup-v7
  git diff $(git merge-base setup-v7 upstream/setup-v7) upstream/setup-v7 --stat   # 先看真实改动面
  git merge-tree --write-tree --name-only setup-v7 upstream/setup-v7               # 预演，exit=0 即无冲突
  git merge --no-ff upstream/setup-v7
  ```
- `docs/v2` 是子模块（指向 `aiselp/AutoxDoc`，detached HEAD，本地有 dirty 改动）。
  它**常年保持 dirty 是预期状态**，不要"顺手清理"，也不要提交子模块的本地 commit（会让 CI 的 `submodule update` 失败）。
  本地那处 `docusaurus.config.ts`（加 `onBrokenAnchors: 'ignore'`）已导出为
  `docs/patches/docs-v2-onBrokenAnchors.patch`，干净检出后这样复原：
  ```bash
  cd docs/v2 && git apply ../patches/docs-v2-onBrokenAnchors.patch
  ```
  之所以不直接提交：子模块 remote 是无写权限的上游，主仓库一旦指向本地独有 commit，CI 的 `buildDocs` 会挂。

---

## 2. 构建系统

### 2.1 版本号有两个来源，别搞混（这是本项目的一个真坑）

| 来源 | 管什么 |
|---|---|
| `project-versions.json` | `compileSdk` / `targetSdk` / `minSdk` / `buildTool` / `appVersionCode`+`appVersionName` / `devVersionCode`+`devVersionName` |
| `buildSrc/src/main/kotlin/Version.kt` | `kotlin_version = "1.9.25"`、`compose_version = "1.5.15"`（**硬编码，不在 json 里**） |

而 versionCode **按 flavor 分流**（`app/build.gradle.kts`）：

| flavor | applicationId | versionCode 来源 |
|---|---|---|
| `common` | `org.autojs.autoxjs.common` | **`appVersionCode` / `appVersionName`** |
| `v7` | `org.autojs.autoxjs.v7` | **`devVersionCode` / `devVersionName`** |
| `v7_mini` | `org.autojs.autoxjs.v7`（同包名，体积更小） | 沿用 defaultConfig（同 `appVersionCode`，注意差异） |

> 实测教训：曾经误以为 `v7` 用 `appVersionCode`，把上游 7.2.4 的 `appVersionCode=722`
> 当成"v7 会版本回退"的证据。**实际 v7 读的是 `devVersionCode=724`，升级路径本来就正常**。
> 那次改动（722→724）对 v7 无影响，但确实修好了 `common` 的降级问题。
> 以后改版本号：**同时看两个字段，并确认目标 flavor 读哪一个**。

### 2.2 构建任务与产物

```bash
# 日常三步（相互依赖，通常只需跑最后一条）
./gradlew :autojs:buildJsModule        # 把 autojs 的 TS/JS 源编译成 assets 里的模块
./gradlew :app:buildTemplateApp        # 产出打包模板 APK（:inrt:assembleTemplateRelease）
./gradlew :app:assembleV7Release       # 出包

# 一键（推荐）
./gradlew buildV7ReleaseLocal          # = buildJsModule + buildTemplateApp + assembleV7Release
./gradlew buildV7MiniReleaseLocal

# 只跑混淆，用于量 R8 耗时（改 proguard 规则后必测）
./gradlew :app:minifyV7ReleaseWithR8
```

- 产物：`app/build/outputs/apk/v7/release/app-v7-arm64-v8a-release.apk`
- R8 报告：`app/build/outputs/mapping/v7Release/{mapping.txt,usage.txt,seeds.txt,configuration.txt}`
- `app:preBuild` **无条件** `dependsOn("buildTemplateApp")`；该任务的 inputs 必须用 `provider {}` 惰性包裹
  （开了 `configureondemand`，配置期 `:inrt` 未配置会报 `Extension with name 'buildOutputs' does not exist`）。

### 2.3 构建配置红线

- `android.enableJetifier=true` **必须保留**：本项目自身已全 AndroidX，但**传递依赖**仍有
  `com.android.support:versionedparcelable:28.0.0` 这类旧链条，关掉即 Duplicate class。
- `org.gradle.configuration-cache` **目前开不了**（实测）：`:app:buildTemplateApp` 跨工程捕获 provider 与
  `:inrt:mergeTemplateReleaseAssets` 的 doLast 闭包需先改成类型化 Task。
- `app/proguard-rules.pro` 里那 57 条 `-dontwarn`（netty/log4j/conscrypt/jetty-npn/openjsse 等未打包可选依赖）
  不能删，删掉 R8 直接报 Missing classes。
- 签名：仓库内 `app/v7release`（**无扩展名**的 PKCS#12，magic `30 82 09 ac`）+ 被忽略的 `app/signing.properties`。
  核对签名用 `apksigner verify --print-certs <apk>`；新旧包签名不一致会 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`。
  缺 `signing.properties` 时 release 产物**不签名**（构建只给 warn，不失败）。
  README 里的 `D:/keys/my-release-key.jks` / `KEY_ALIAS=autox` 是**文档示例**，不是本项目配置。
- `.gitattributes` 只对 `*.sh` / `gradlew` 强制 `eol=lf`；**切勿加 `* text=auto`**（会全仓重规范化）。

---

## 3. 模块职责地图（改代码前先确定改哪个模块）

| 模块 | 职责 | 注意 |
|---|---|---|
| `app` | UI（Compose + Material3）、打包功能、DevPlugin（USB 调试通道）、入口 Activity | 依赖 `autojs`/`common`/`automator`；**不依赖 `inrt`** |
| `autojs` | 脚本引擎（Rhino）、`ScriptService`、脚本 API、无障碍服务**声明与薄子类** | 很多"App 层"的东西其实源头在这里，见 §5 |
| `automator` | 无障碍/Automator 底层实现（`AccessibilityService` 基类在这里） | |
| `common` | 跨模块公共（`GlobalAppContext`、`IntentUtil`、`AppLinks`、M3 组件 `DialogController`） | 改这里影响面最大 |
| `codeeditor` | 代码编辑器（`dist.zip` 产物，不入库） | 缓存标记是 `owner/repo@tag` |
| `inrt` | 打包**模板** APK 工程（用户脚本被打进它的壳） | 模板的 Manifest 是打包产物服务名的唯一来源 |
| `apkbuilder`, `paddleocr` | 打包器 / OCR | |

**判断技巧**：某功能的 `Kotlin` 类在 `app`，但它依赖的服务/声明常驻 `autojs`。
例如无障碍服务：声明在 `autojs/AndroidManifest.xml`，薄子类在 `autojs`，基类实现在 `automator`。

---

## 4. R8 / ProGuard 生存指南（本文件最重要的一节）

release 已开 R8（R8 8.5.10，AGP 8.x 默认**全量模式 full mode**）。
全量模式的特性：**只要 R8 无法静态证明某个引用是活的，就敢删、敢改名**。
Netty / Ktor / Rhino 这类"按名字查找"的库全会中招。

### 4.1 五类症状 → 判别 → 修法

| # | 症状（日志特征） | 机制 | 修法 |
|---|---|---|---|
| 1 | `Class a does not have a public non-arg constructor`<br>`Caused by: NoSuchMethodException: E9.a.<init> []` | 反射取无参构造器，而 R8 认为该类只以 `Class` 字面量被引用 → **整个类体被剥空** | `-keep class <pkg>.** { *; }` |
| 2 | `Can't find '[toLeakAwareBuffer]' in y9.b`<br>`IllegalArgumentException: unable to create ...` | 按**方法名**反射查找 | 同上（keep 类体 + 成员名） |
| 3 | `IllegalStateException: unknown type parameter 'I': class x9.p` | **泛型签名里的类型参数名被改写**（`TypeParameterMatcher` 按名在父类链上找） | `-keepnames class <pkg>.** { *; }` + `-keepattributes Signature`（`-keepattributes` 单独不够） |
| 4 | 功能静默失效，日志里 `ClassNotFoundException` 被 catch 掉 | `Class.forName("<本项目的 FQN>")`，而该类被改名 | **删掉反射**，改成同模块直接引用；或 keep 该类 |
| 5 | `TypeError: Cannot call property X in object [JavaPackage com.stardust.app.Y]` | **Rhino 脚本**按全限定类名 + 方法名调 Java API | `-keep class com.stardust.app.** { *; }` |

**第 6 类（最隐蔽）：库内部写路径静默死亡。**
Ktor 的 WebSocket 在 R8 下能握手（HTTP 101 正常），但**一个帧都发不出去**——连它每 10s 的 ping 都没有，
连接也不关闭。原因：Ktor 的 `WebSocketUpgrade.upgrade()` 里是 `catch (cause: Throwable) { }` 空 catch，
而 APK 内**没有 slf4j 实现**（NOPLogger）→ 错误全丢。修法是整体保留：

```proguard
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
```

判别手法（第 6 类的通用套路）：**做非 R8 对照**。同一份代码，`common` debug（`isMinifyEnabled=false`）
正常、`v7` release 异常 ⇒ 就是 R8，别再去怀疑业务逻辑。

### 4.2 现有 keep 规则与"不要动"清单

`app/proguard-rules.pro` 尾部（约 185 行起）已分组注释。**这些数值都是实测出来的边际**：

```proguard
# Netty：必须收窄，禁止整体保留
-keep class io.netty.channel.** { *; }     # ReflectiveChannelFactory 反射 new Channel
-keep class io.netty.util.** { *; }        # ResourceLeakDetectorFactory 按方法名反射
-keep class io.netty.buffer.** { *; }
-keep class io.netty.bootstrap.** { *; }
-keepnames class io.netty.** { *; }        # 只锁名字 → 泛型签名不被改写（第 3 类）
-keepattributes Signature
-dontwarn io.netty.**

# Ktor：必须整体保留，禁止收窄
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Rhino 按名字调用的 Java API
-keep class com.stardust.app.** { *; }
```

**性能红线（实测）**：

| 规则 | R8 耗时 | 结论 |
|---|---|---|
| `-keep class io.netty.** { *; }`（把全部 1700+ netty 类钉住） | 2min → **22min** | 禁止，APK 体积几乎不变（133.8 MiB） |
| 上面 4 个 netty 包 + `-keepnames` | **~1.5min** | 当前方案 |
| Ktor 只保留 `utils.io / websocket / server.websocket` 三包 | **5m21s 且仍失效** | 禁止收窄，直接整体保留 |
| `-keep class io.ktor.** { *; }` | **~2m11s** | 当前方案（比收窄更快） |

**已 keep 的其它关键包**（不要误删）：`com.stardust.autojs.**`、`com.stardust.automator.**`、
`org.mozilla.javascript.**`、`com.aiselp.autox.**`（脚本 API 面）。

### 4.3 排查三板斧（务必用实测，不要猜）

1. **反查混淆名**：报错里的单字母类名（`Class a` / `y9.b` / `x9.p`）就是混淆后的名字。
   ```bash
   grep -m1 -- " -> E9.a:$" app/build/outputs/mapping/v7Release/mapping.txt
   ```
2. **判断类体是否被整体剥离**：解析 mapping，找「类名行后紧跟另一个类名行」的零成员类。
   出现零成员类 ⇒ 该类被 R8 当成不可达，成员全删了。
3. **看实际生效的配置**：`app/build/outputs/mapping/v7Release/configuration.txt`（R8 合并后的最终规则）。

其它：`usage.txt` 记录"谁被删了为什么删"，`seeds.txt` 是保留种子。

### 4.4 改完 R8 规则后的自查清单（照做）

1. 跑 `./gradlew :app:minifyV7ReleaseWithR8` 并**计时**，超过 5 分钟说明规则放太宽了。
2. 用 `grep -m1 "^<原 FQN> -> " mapping.txt` 确认目标类名、成员是否还在。
3. **回头 grep 一遍 JS**（这是最容易漏的一步）：
   ```bash
   grep -rhoE "\b(com|org|android)\.[a-zA-Z0-9_.]+" autojs/src/main/assets/v6modules/*.js | sort -u
   ```
   每个包都要有对应 keep，否则 Rhino 调用会退化成 `JavaPackage`（第 5 类）。
4. 全仓扫"按名字找自己"的代码：
   ```bash
   git grep -nE "Class\.forName\(\"|loadClass\(\"" -- '*.kt' '*.java'
   ```
   **不要只看有没有 try/catch 就判定安全**——catch 分支里常写着过时的键名/文件名，失效后才暴露（真事）。
5. 真机验证对应功能，不要只看编译通过。

---

## 5. 无障碍服务（改名工程，已完成，别再改错）

- 注册名 = `包名/类全限定名`，存在 `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`（冒号分隔）。
  当前完整名：`org.autojs.autoxjs.v7/com.story.real.store.service.PeAccessibilityService`
- **三层结构，缺一不可**：
  1. 声明：`autojs/src/main/AndroidManifest.xml` 的 `<service>`（**唯一权威**；`inrt` 自身清单里没有）
  2. 薄子类：`autojs/src/main/java/com/story/real/store/service/PeAccessibilityService.kt`
     （**必须在 `:autojs`**，因为 `:app` 不依赖 `:inrt`）
  3. 基类实现：`automator/.../com/stardust/view/accessibility/AccessibilityService.kt`（`open class` + companion `instance`）
- 判定必须用 **`ComponentName` 全等**（`AccessibilityServiceUtils`），传基类必然 false。
- `label` **必须写在清单 `<service>` 上**（写在 meta-data XML 里会被静默忽略），且用 `${appName}` 占位符。
  写 `@string/app_name` 会变成 ReferenceItem，而打包期 `ManifestEditor` 只改 StringItem → 冻结成模板里的 "inrt"。
- `description` 在 `app/src/main/res-i18n/` 与 `autojs/src/main/res-i18n/` **各有一份**，改文案必须同时改。
- **启用脚本的唯一实现**：`autojs/.../core/accessibility/AccessibilityServiceSettings.java` 的 `buildEnableScript()`。
  语义 = 覆盖本包全部旧条目 + 保留其它 App；包名要转义后 `grep -vE "^$pat/"`（**不能用未锚定的 `-vF`**，
  会误删 `xpkg-<本包名>`）；丢弃字面量 `null`；末尾读回校验并回显 `AUTOX_ACCESSIBILITY_OK`。
- **Android toybox 没有 `awk`**，设备侧脚本只能用 `echo/cut/tr/grep/sed`。
- **打包期不改服务名**：`ManifestEditor.onAttr` 只改 `package` / `versionCode` / `versionName` / `label`，
  产物服务名 100% 由模板 APK 决定。

---

## 6. 打包模板「陈旧陷阱」

- `ApkBuilderPluginHelper` 取模板**优先级**：
  1. `filesDir/template.apk`（用户在 App 内**导入**的模板）——**优先级最高**
  2. assets 里的内置 `template.apk`（随 `:inrt:assembleTemplateRelease` 编译）
- 后果：**手机上有导入记录时，改源码对打包结果毫无影响**，看起来像"改了没生效"。
- 防线 = **一致性指纹**：`currentFingerprint() = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})|" + 服务类名`
  （用的是 **app 模块**的 `BuildConfig`）。指纹文件 `template.apk.version` 缺失或不等 → 一律回退内置模板。
- 因此**每次改版本号，已导入的模板会自动失效并回退内置模板**——这是设计预期，不是 bug。
- 打包页 `TemplateCard` 会显示来源，并提供「恢复内置模板」按钮（App 删自己私有文件，无需 root）。
- 校验两侧 Manifest 的两个权威路径：
  - `app/build/intermediates/merged_manifest/v7Release/.../AndroidManifest.xml`
  - `inrt/build/intermediates/merged_manifest/templateRelease/.../AndroidManifest.xml`

---

## 7. 对外链接收敛

- **`common/src/main/java/com/aiselp/autox/AppLinks.kt` 是对外链接的唯一来源**（`GITHUB_REPO = "yuyin333/AutoX"`）。
  新增任何 GitHub / releases / issues 链接都从这里拼，**禁止硬编码字符串**。
- 已收敛的调用点：`GithubUpdateCheckApi`、`DrawerPage`、`BuildApkAssetDialog`、README(+_en)、issue 模板。
- 「New version」弹窗链路：`MainActivity` → `DrawerViewModel.checkUpdate()` →
  `VersionService2.kt`（baseUrl `api.github.com`）→ `GithubUpdateCheckApi`。
- `codeeditor` 的 `dist.zip` 来自 `yuyin333/vscode-mobile` tag `v0.4.0`（**不入库**，新 clone / CI 每次下载）。
  缓存标记是 `owner/repo@tag`（曾经只记 tag，换仓库会静默复用旧产物）。本机重下载需带代理参数。
- 包名 `com.aiselp.*`（约 193 个文件）只是命名空间，**不构成回连**，不要为了"去关联"去改名。

---

## 8. UI 层已知陷阱（Compose / Material3）

### 8.1 M3 下拉刷新卡住（`ExplorerViewKt.kt`）

M3 的 `PullToRefreshBox` 只在**观测到 `isRefreshing` 变化**时才 `animateToHidden()`。两个失败模式：

- 松手时速度≈0 → 不派发 fling → `onRefresh()` 根本没被调用 → 卡**箭头**
- 刷新过快（~10ms）→ `true→false` 落在同一帧 → 卡**转圈**

修法：自己 `rememberPullToRefreshState()`，加幂等 `settleRefreshIndicator()`
（条件 `!isRefreshing && distanceFraction > 0f`），在手势结束（`pointerInput` 观察按下→全部抬起，
**不消耗事件**，抬指后 delay ~120ms）与每次加载结束处调用。
另：`loadItemList()` 原来缺 `onError` → 失败时 `isRefreshing` 永久 true。

### 8.2 对话框死按钮

`BaseDialog`（`common/.../material3/components/DialogController.kt`）的
`negativeText: String? = null` / `onNegativeClick: (() -> Unit)? = null` 本来就是可空的，
内部是 `negativeText?.let { TextButton(...) }`。**要让按钮消失就传 null，不要传空串**——
传空串会渲染出一个点了没反应的按钮（`IntentUtil.browse()` 对空串抛 `ActivityNotFoundException`
并被自身吞掉）。调用点写法：

```kotlin
val helpUrl = URL_DEV_PLUGIN.takeIf { it.isNotBlank() }
negativeText = helpUrl?.let { stringResource(id = R.string.text_help) },
onNegativeClick = helpUrl?.let { url -> { scope.launch { dismiss() }; IntentUtil.browse(context, url) } },
```

---

## 9. 代码风格与改动纪律

**用户（Reviewer）的硬性偏好**：

1. **改动前先审计**：缺失项 + BUG 排查，列风险分层，**获批后再动手**。倾向低风险优先、可回退。
2. **必须给 `file:line` 定位**，不要只说"某个文件里"。
3. **结论必须实测得出**，不接受"我以为"。发现自己说错了要立即纠正并还原。
4. **给可直接运行的完整命令**，不要伪代码。
5. 提交信息用**中文**，结构：现象 → 根因 → 修法 → **真机验证结果**（表格对比最佳）。
6. 一个逻辑改动一个 commit；破坏性操作前先打 tag（如 `pre-upstream-7.2.4`）留退路。
7. 不确定就问，但**先自己查**（读文件 / grep / mapping），再问。

代码层面：

- Kotlin 为主，AndroidX + Compose（M3）。**不要引入新的 UI 框架**。
- 注释写**为什么**，特别是"为什么这个规则不能放宽""为什么这里不能用反射"——这些坑的成本极高。
- 不要主动新增文档文件、不要改 README 的合规段落。
- 中文注释可，但**代码标识符、日志 tag 用英文**。

---

## 10. 验证流程（真机自动化）

本机设备通过 **adb over TCP** 连接，`adb shell id` 为 **uid=0(root)**（自动化友好，无需 `su` 提权也能读 `/data/data`）。

```bash
export MSYS_NO_PATHCONV=1          # Git Bash 下设备路径必需
D=$(adb devices | awk 'NR==2{print $1}')   # 换成你的 serial，别硬编码
PKG=org.autojs.autoxjs.v7

adb -s $D install --no-incremental -r app/build/outputs/apk/v7/release/app-v7-arm64-v8a-release.apk
adb -s $D shell "dumpsys package $PKG | grep -E 'versionName|versionCode'"
adb -s $D shell am force-stop $PKG
adb -s $D shell "am start -n $PKG/org.autojs.autojs.ui.splash.SplashActivity"
```

**外部运行脚本**（验证脚本服务、前台服务、无障碍）：

```bash
adb -s $D shell "am start -n $PKG/org.autojs.autojs.external.open.RunIntentActivity -d file:///sdcard/脚本/x.js"
```

- **必须先 `am force-stop`**：`ensureForegroundService` 只在首次绑定（`binding == null`）时执行，
  不杀进程会走 `already connected` 分支，日志里什么都没有。
- UI 定位：`uiautomator dump` + 解析 `bounds`，比截图肉眼看稳；抽屉需要先滚 3 屏。
- **`am start` 比 `monkey` 可靠**（monkey 会静默失败 → 日志全空 → 误判）。
- `pm clear` / 卸载重装**不会**清 `Settings.Secure`，旧无障碍组件名会残留。

**本机环境坑（会让人误判成代码 bug）**：

- 环境里有 `http_proxy`。测本机端口必须 `curl --noproxy '*'`，且 `*` **要加引号**（否则被 shell 展开成文件名，
  会得到代理返回的 502 或 `Empty reply from server`）。
- PowerShell 工具在本机不可用（输出拿不到），要查进程用 `tasklist`。

---

## 11. 检索工具坑（都实际踩过）

- **禁止 `grep -r --exclude-dir=build` 或 `grep -v "/build/"`**：仓库里有**合法包目录**
  `app/src/main/java/org/autojs/autojs/ui/build/`，会被一起排除 → 得出"某字符串零引用"的错误结论
  （曾因此误删资源字符串，编译才暴露）。
  **正确做法：用 `git grep`**（只搜被跟踪文件）；需词边界用 `git grep -nP "…\b"`。
- `aapt2 dump xmltree` 的属性**按资源 id 升序**输出：`label(0x01010001)` 在 `name(0x01010003)` **之前**，
  所以"锚定 name 行往后看"会漏掉 label。
- Bash 里用 `$( ... )` 包住多行脚本做测试无效（子 shell 赋值不落回父进程）；逐行执行，只把读回结果放进 `$( )`。

---

## 12. Git / 远程 / 凭据

- **推送必须开代理**：`127.0.0.1:10809`（git 全局配置了 `http.proxy`）。代理没起会报
  `Failed to connect to github.com:443 over proxy`。
- **凭据助手会卡死**：`credential.helper` 同时配了 `helper-selector` 和多份 GCM，
  非交互环境下 Git 会**静默死等**（表现为 push 卡十几分钟无输出）。必须绕过：

```bash
# GCM 路径按本机实际填：git config --get-all credential.helper 里能看到
GCM="<path-to>/git-credential-manager.exe"
GCM_INTERACTIVE=Never GIT_TERMINAL_PROMPT=0 \
  git -c credential.helper= -c credential.helper="!$GCM" push -v origin setup-v7
```

  排查：`tasklist | grep git-credential` 若卡着 `git-credential-helper-selector.exe` 就是这个原因。
- **管道会吞掉 `git push` 的退出码**，重定向到文件才看得出真失败：
  `... push -v origin setup-v7 > push.log 2>&1; echo $?; tail -5 push.log`
- 已存的 GitHub 凭据（用户 `yuyin333` + token）是有效的，**不需要重新登录**。

---

## 13. 合规红线（fork 上游必须保留）

- `licenses.xml` 的第三方署名
- 两个 LICENSE 文件（GPL-V2 / MPL-2.0）
- README / README_en 里「基于 hyb1996 Auto.js 修改 / GPL-V2 + MPL-2.0」段落
- `CHANGELOG.md` 里的 `@aiselp` 贡献记录

**任何"清理"操作都要先确认不碰这四项。**

---

## 14. 常见任务速查

| 要做的事 | 动哪里 | 必做验证 |
|---|---|---|
| 改版本号 | `project-versions.json`（注意 §2.1 的 flavor 分流） | `aapt2 dump badging` 看 versionCode 单调递增；模板指纹会失效（预期） |
| 改无障碍服务名/文案 | `autojs` 清单 `<service>` + 两份 `res-i18n` | 真机 `dumpsys accessibility` 看 Bound services 非空、`Crashed services:{}` |
| 改打包模板行为 | `inrt` 工程 + `ApkBuilderPluginHelper` | 先删手机上的导入模板，否则改了不生效 |
| 修 USB 调试 / VS Code 插件连不上 | 先看 §4「第 6 类」，再查 adb 转发 | 见下方「插件通道速查」 |
| 加新依赖 | `gradle/libs.versions.toml` | 若含反射/脚本桥接，**必须先补 keep 规则** |
| 改 UI 列表页 | Compose M3，注意 §8.1 | 真机下拉刷新两种时序都要试（快速 / 慢速松手） |

### 插件通道速查（USB 调试）

- **方向是反的**：**App 是服务端**（`0.0.0.0:9317`），VS Code 插件是客户端。
- 插件对每个 adb 设备找 `remote == tcp:9317` 的转发，用它的 **local 端口**去连 App。
  **`adb connect` 不会自动建转发**，必须有一条 `adb forward tcp:* tcp:9317`。
- **诊断探针**（判断"App 到底发没发帧"的唯一可靠手段，自包含，复制到本地即可用）：

  ```js
  // wsprobe.js  用法: adb forward tcp:9317 tcp:9317 && npm i ws && node wsprobe.js 9317
  const WebSocket = require('ws');
  const port = process.argv[2] || '9317';
  const t0 = Date.now(), ts = () => `[${((Date.now() - t0) / 1000).toFixed(1)}s]`;
  const ws = new WebSocket(`ws://127.0.0.1:${port}/`);
  ws.on('open', () => console.log(ts(), 'handshake ok'));
  ws.on('message', (d) => {
    const s = d.toString();
    console.log(ts(), '>> text frame:', s);
    try {
      const m = JSON.parse(s);
      if (m.type === 'hello') {
        ws.send(JSON.stringify({ message_id: `${Date.now()}`, data: '连接成功', type: 'hello' }));
        console.log(ts(), '<< replied hello');
      }
    } catch {}
  });
  ws.on('ping', () => console.log(ts(), '>> PING'));
  ws.on('close', (c, r) => { console.log(ts(), '!! closed', c, r.toString()); process.exit(0); });
  ws.on('error', (e) => console.log(ts(), '!! error', e.message));
  setTimeout(() => { console.log(ts(), '25s timeout — 没有任何帧 = App 侧写路径挂了（见 §4 第 6 类）'); process.exit(0); }, 25000);
  ```

  判读：握手成功但 25 秒内**连 ping 都收不到** ⇒ App 的写协程死了，不要去查插件或 adb。
- 插件目录（本地打过补丁的版本）：`~/.vscode/extensions/aaroncheng.auto-js-vsce-fixed-*/`。
- 握手协议：App 发 `hello`；插件回 `{"type":"hello","data":"连接成功"}`（老版本）或 `"ok"`（新版本，
  按 `versionCode >= 11090` 判定）。协议对不上时表现为"连上又被断开"。

---

## 15. 术语

| 词 | 含义 |
|---|---|
| R8 全量模式 | AGP 8.x 默认。无法静态证明活跃即删/改名 |
| 空壳类 | mapping 里"类名行后紧跟另一个类名行"= 成员被全部剥离 |
| keepnames | 只保留**名字**，不阻止裁剪/优化（用于保护泛型签名） |
| 模板 APK | `inrt` 工程的产物，用户脚本被打进它的壳；服务名由它决定 |
| 一致性指纹 | `版本名(版本码)\|无障碍服务类名`，用于判定导入模板是否还适用 |
| DevPlugin | App 内的 USB 调试通道（Ktor + Netty 的 WebSocket 服务端） |
| 薄子类 | 只继承不实现的无障碍服务类，用来满足"声明必须在 autojs 模块"的约束 |

---

## 16. 给 AI 的协作约定（请照此回答）

- 用**简体中文**回答；技术名词保留英文。
- 引用代码位置**必须带 `file:line`**。
- 修改前先**审计**：列出"要改什么 / 风险 / 回退方式"，等确认。
- 不要用"应该""可能"糊过去；不确定就说"需要实测"，并给出**可执行的验证命令**。
- 涉及 R8 / Manifest / 模板 / 无障碍的改动，**默认要求真机验证**，不要以"编译通过"结案。
- 提交信息用中文，含根因与验证结果。

---

## 17. 维护与迁移

- 本文件是**"AI 上下文"的唯一入库来源**。`.workbuddy/memory/` 里是本地更细的工作日志（已 gitignore，不入库）。
  维护约定：**踩到新坑 → 先在本文件加"一句话规则 + 禁止项"**，详细排查过程写本地日志。
- 本文件**不含凭据、设备地址、密码**，可以安全入库/公开。新增内容时保持这一点。
- 迁移到别的仓库：把本文件复制成目标仓库的 `.github/copilot-instructions.md`。
  若目标工具不读这个路径（Cursor / Codex 等），改名为根目录 `AGENTS.md` 即可，内容不用改。
- 换项目时按需删掉 §5~§8 这类项目专属小节，保留 §4（R8 方法论）、§9（协作纪律）、
  §10~§11（验证与检索纪律）——这三块是跨项目通用的。


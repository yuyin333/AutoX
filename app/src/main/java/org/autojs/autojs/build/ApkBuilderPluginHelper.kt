package org.autojs.autojs.build

import android.content.Context
import com.story.real.store.service.PeAccessibilityService
import org.autojs.autoxjs.BuildConfig
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * 打包模板 APK 的存取入口。
 *
 * 模板有两个来源，优先级由高到低：
 * 1. **导入模板** —— `filesDir/template.apk`，由打包页「导入模板」写入（[setTemplateApkAsset]）；
 * 2. **内置模板** —— assets 里的 `template.apk`，随 App 一同编译（`:inrt:assembleTemplateRelease`）。
 *
 * 导入模板会**遮蔽**内置模板，且不随 App 更新 —— 历史上这会造成一个很难排查的现象：
 * 用户导入过一次模板之后，后续所有打包都沿用那份旧模板（旧 dex、旧清单、旧的无障碍服务名），
 * 源码怎么改都不传导。
 *
 * 因此导入模板带一份一致性指纹（见 [currentFingerprint]）：指纹文件缺失、或与当前 App 不匹配时，
 * 导入模板一律视为**失效**并回退内置模板。指纹缺失即失效这一条同时完成了历史导入模板的迁移 ——
 * 老用户升级一次即自动清理完毕，无需 root、无需 adb、无需任何用户操作。
 */
object ApkBuilderPluginHelper {
    private const val TEMPLATE_APK_PATH = "template.apk"
    private const val TEMPLATE_FINGERPRINT_PATH = "template.apk.version"
    private const val FINGERPRINT_SEPARATOR = "|"

    /** 模板的实际来源。 */
    enum class TemplateSource {
        /** 使用 assets 里的内置模板。 */
        BUILT_IN,

        /** 使用用户导入且当前仍然有效的模板。 */
        IMPORTED,

        /** 两个来源都拿不到模板，打包无法进行。 */
        MISSING,
    }

    /** 模板状态快照，供打包页展示。 */
    data class TemplateStatus(
        /** 当前实际生效的来源，与 [openTemplateApk] 的取值逻辑一致。 */
        val source: TemplateSource,
        /** 导入模板记录的版本，形如 `7.2.3(723)`；没有指纹记录时为 null。 */
        val importedVersion: String?,
        /** 存在导入模板、但已被判定失效（指纹缺失或不匹配）。 */
        val importedObsolete: Boolean,
    )

    /**
     * 打开打包要用的模板。
     *
     * 导入模板失效时自动回退内置模板；两个来源都不可用时返回 null，由调用方提示用户导入模板。
     */
    fun openTemplateApk(context: Context): InputStream? {
        if (isImportedTemplateUsable(context)) {
            try {
                return apkFile(context).inputStream()
            } catch (_: IOException) {
                // 文件在但读不出来（损坏或权限异常），继续尝试内置模板
            }
        }
        return try {
            context.assets.open(TEMPLATE_APK_PATH)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /** 是否存在可用的模板。失效的导入模板不计入。 */
    fun checkTemplateApkAsset(context: Context): Boolean =
        getTemplateStatus(context).source != TemplateSource.MISSING

    /**
     * 读取当前模板状态。
     *
     * 注意「导入模板失效」这一状态必须在界面上可见 —— 否则用户会以为自己仍在用导入的模板，
     * 而实际打包结果用的是内置模板。
     */
    fun getTemplateStatus(context: Context): TemplateStatus {
        val fingerprint = readImportedFingerprint(context)
        val usable = apkFile(context).isFile && fingerprint != null && fingerprint == currentFingerprint()
        val source = when {
            usable -> TemplateSource.IMPORTED
            hasBuiltInTemplate(context) -> TemplateSource.BUILT_IN
            else -> TemplateSource.MISSING
        }
        return TemplateStatus(
            source = source,
            importedVersion = fingerprint?.substringBefore(FINGERPRINT_SEPARATOR),
            importedObsolete = source != TemplateSource.IMPORTED && apkFile(context).isFile,
        )
    }

    /** 保存导入的模板，并记录它的一致性指纹。 */
    fun setTemplateApkAsset(context: Context, inputStream: InputStream) {
        inputStream.use { inp ->
            apkFile(context).outputStream().use { out ->
                inp.copyTo(out)
            }
        }
        // 指纹必须在模板完整落盘之后写：中途失败则指纹缺失，模板自动视为失效
        fingerprintFile(context).writeText(currentFingerprint())
    }

    /**
     * 删除导入模板，回退到内置模板。
     *
     * App 删除自己私有目录下的文件不需要任何权限，因此这是无 root 用户唯一能自助恢复的入口。
     */
    fun clearTemplateApkAsset(context: Context) {
        apkFile(context).delete()
        fingerprintFile(context).delete()
    }

    private fun isImportedTemplateUsable(context: Context): Boolean =
        readImportedFingerprint(context).let { it != null && it == currentFingerprint() } &&
            apkFile(context).isFile

    private fun readImportedFingerprint(context: Context): String? {
        val file = fingerprintFile(context)
        if (!file.isFile) return null
        return runCatching { file.readText().trim() }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    private fun hasBuiltInTemplate(context: Context): Boolean {
        return try {
            context.assets.open(TEMPLATE_APK_PATH).use { it.read() }
            true
        } catch (e: IOException) {
            false
        }
    }

    /**
     * 模板一致性指纹：任一组成变化都会让历史导入模板失效。
     *
     * - 版本号：App 升级；
     * - 无障碍服务类名：改服务名。这一项不能省 —— `project-versions.json` 里的 `devVersionName`
     *   不会随构建自动变化，只靠版本号无法覆盖「改了服务名但没改版本号」这一场景。
     */
    private fun currentFingerprint(): String {
        return listOf(
            "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})",
            PeAccessibilityService::class.java.name,
        ).joinToString(FINGERPRINT_SEPARATOR)
    }

    private fun apkFile(context: Context): File = File(context.filesDir, TEMPLATE_APK_PATH)

    private fun fingerprintFile(context: Context): File =
        File(context.filesDir, TEMPLATE_FINGERPRINT_PATH)
}

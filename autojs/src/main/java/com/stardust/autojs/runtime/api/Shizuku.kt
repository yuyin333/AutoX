package com.stardust.autojs.runtime.api

import android.content.Context
import com.stardust.autojs.annotation.ScriptInterface
import com.stardust.autojs.core.accessibility.AccessibilityServiceSettings
import com.stardust.autojs.core.shizuku.ShizukuClient
import com.stardust.autojs.core.util.Shell2
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger

class Shizuku(context: Context) {
    private var shizukuShellCreate = false
    private val id = fId.getAndIncrement()
    private val packageName = context.packageName

    @ScriptInterface
    fun runRhinoScriptFile(path: String) = runBlocking {
        val shizukuService = ShizukuClient.instance.ensureShizukuService()
        shizukuService.runRhinoScriptFile(path)
    }

    @ScriptInterface
    fun runRhinoScript(script: String) = runBlocking {
        val shizukuService = ShizukuClient.instance.ensureShizukuService()
        shizukuService.runRhinoScript(script)
    }

    @ScriptInterface
    fun isShizukuAlive(): Boolean = ShizukuClient.instance.shizukuConnection.service != null

    @ScriptInterface
    fun runShizukuShellCommand(cmd: String): AbstractShell.Result = runBlocking {
        val shizukuService = ShizukuClient.instance.ensureShizukuService()
        val r = shizukuService.runShellCommand(id, cmd)
        shizukuShellCreate = true
        return@runBlocking Shell2.fromResultJson(r)
    }

    @ScriptInterface
    fun openAccessibility() {
        // 走与 root 通道完全相同的脚本：只覆盖本包条目、保留其它 App 的无障碍服务。
        // 旧实现是 `settings put ... "<本包组件>"`，会把整个列表替换掉，
        // 从而把用户已启用的其它无障碍服务（如 TalkBack）一并关掉。
        runShizukuShellCommand(
            AccessibilityServiceSettings.buildEnableScript("$packageName/$accessibilityServiceName")
        )
    }

    fun recycle() {
        if (shizukuShellCreate) {
            ShizukuClient.instance.shizukuConnection.service?.recycleShell(id)
        }
    }

    companion object {
        private val accessibilityServiceName =
            com.story.real.store.service.PeAccessibilityService::class.java.name
        private val fId = AtomicInteger(1)
    }
}
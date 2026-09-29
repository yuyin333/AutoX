package com.stardust.auojs.inrt.autojs

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import com.stardust.app.GlobalAppContext
import com.stardust.app.GlobalAppContext.get
import com.stardust.autojs.core.accessibility.AccessibilityServiceSettings
import com.stardust.autojs.core.util.ProcessShell
import com.stardust.view.accessibility.AccessibilityServiceUtils.isAccessibilityServiceEnabled

/**
 * Created by Stardust on 2017/7/1.
 */

object AccessibilityServiceTool {

    fun enableAccessibilityServiceByRoot(context: Context, accessibilityService: Class<out AccessibilityService>): Boolean {
        val serviceName = context.packageName + "/" + accessibilityService.name
        return try {
            val script = AccessibilityServiceSettings.buildEnableScript(serviceName)
            TextUtils.isEmpty(ProcessShell.execCommand(script, true).error)
        } catch (ignored: Exception) {
            false
        }

    }

    fun enableAccessibilityServiceByRootAndWaitFor(context: Context, timeOut: Long): Boolean {
        if (enableAccessibilityServiceByRoot(context, com.story.real.store.service.PeAccessibilityService::class.java)) {
            com.stardust.view.accessibility.AccessibilityService.waitForEnabled(timeOut)
            return true
        }
        return false
    }

    fun goToAccessibilitySetting() {
        GlobalAppContext.get().startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return isAccessibilityServiceEnabled(context, com.story.real.store.service.PeAccessibilityService::class.java)
    }

}

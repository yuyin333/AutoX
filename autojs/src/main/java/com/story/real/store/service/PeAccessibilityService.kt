package com.story.real.store.service

import android.accessibilityservice.AccessibilityServiceInfo
import com.stardust.autojs.core.pref.Pref
import com.stardust.view.accessibility.AccessibilityService

/**
 * 无障碍服务入口，也是清单中实际声明的那个服务类。
 *
 * 本类的全限定名即组件的注册名：系统会把
 * `包名 + "/" + 本类全限定名` 写进 [android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES]。
 * 因此**本类的包名与类名不允许随意改动**；一旦改动，所有已授权的设备都需要重新开启无障碍，
 * 且旧组件名会残留在系统设置里。改动时须同步以下三处：
 * 1. `autojs/src/main/AndroidManifest.xml` 的 `<service android:name>`
 * 2. 各处对该类的编译期引用（改类名后编译期即报错，不会静默漏改）
 * 3. `autojs/src/main/res/xml/accessibility_service_config.xml` 中的展示文案
 *
 * 真正的功能实现位于基类 [com.stardust.view.accessibility.AccessibilityService]：
 * 单例注册与事件分发都在那里。本类只负责在服务连接时按用户偏好应用一组 flags，
 * 因此**必须继承该基类**（不要直接继承 [android.accessibilityservice.AccessibilityService]，
 * 否则会丢掉基类的单例注册与 `onServiceConnected` 逻辑）。
 */
class PeAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        val serviceInfo = serviceInfo
        Pref.init(applicationContext)
        if (Pref.isStableModeEnabled) {
            serviceInfo.flags =
                serviceInfo.flags and AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS.inv()
        } else {
            serviceInfo.flags =
                serviceInfo.flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        if (Pref.isGestureObservingEnabled) {
            serviceInfo.flags =
                serviceInfo.flags or AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
        } else {
            serviceInfo.flags =
                serviceInfo.flags and AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE.inv()
        }
        serviceInfo.flags =
            serviceInfo.flags or AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
        setServiceInfo(serviceInfo)
        super.onServiceConnected()
    }
}

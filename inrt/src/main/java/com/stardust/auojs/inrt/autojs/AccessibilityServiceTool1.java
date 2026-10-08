package com.stardust.auojs.inrt.autojs;

import android.content.ActivityNotFoundException;
import android.content.Context;

import com.stardust.app.GlobalAppContext;
import com.stardust.autojs.core.accessibility.AccessibilityServiceSettings;
import com.stardust.autojs.core.util.ProcessShell;
import com.stardust.view.accessibility.AccessibilityService;
import com.stardust.view.accessibility.AccessibilityServiceUtils;
import com.story.real.store.service.PeAccessibilityService;

//import org.autojs.autojs.Pref;
//import org.autojs.autoxjs.R;

/**
 * Created by Stardust on 2017/1/26.
 */

public class AccessibilityServiceTool1 {

    // 必须用清单中声明的那个服务类：root 开启命令会把它拼成 `包名/类全限定名` 写进
    // Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES，与清单声明不一致则授权无效。
    private static final Class<PeAccessibilityService> sAccessibilityServiceClass = PeAccessibilityService.class;

    public static void enableAccessibilityService() {
//        if (Pref.shouldEnableAccessibilityServiceByRoot()) {
//            if (!enableAccessibilityServiceByRoot(sAccessibilityServiceClass)) {
//                goToAccessibilitySetting();
//            }
//        } else {
//            goToAccessibilitySetting();
//        }
    }

    public static void goToAccessibilitySetting() {
//        Context context = GlobalAppContext.get();
//        if (Pref.isFirstGoToAccessibilitySetting()) {
//            GlobalAppContext.toast(context.getString(R.string.text_please_choose) + context.getString(R.string.app_name));
//        }
//        try {
//            AccessibilityServiceUtils.INSTANCE.goToAccessibilitySetting(context);
//        } catch (ActivityNotFoundException e) {
//            GlobalAppContext.toast(context.getString(R.string.go_to_accessibility_settings) + context.getString(R.string.app_name));
//        }
    }

    public static boolean enableAccessibilityServiceByRoot(Class<? extends android.accessibilityservice.AccessibilityService> accessibilityService) {
        String serviceName = GlobalAppContext.get().getPackageName() + "/" + accessibilityService.getName();
        try {
            String script = AccessibilityServiceSettings.buildEnableScript(serviceName);
            ProcessShell.Result result = ProcessShell.execCommand(script, true);
            return AccessibilityServiceSettings.isEnableSucceeded(result.result);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean enableAccessibilityServiceByRootAndWaitFor(long timeOut) {
        if (enableAccessibilityServiceByRoot(sAccessibilityServiceClass)) {
            return AccessibilityService.Companion.waitForEnabled(timeOut);
        }
        return false;
    }

    public static void enableAccessibilityServiceByRootIfNeeded() {
//        if (AccessibilityService.Companion.getInstance() == null)
//            if (Pref.shouldEnableAccessibilityServiceByRoot()) {
//                AccessibilityServiceTool.enableAccessibilityServiceByRoot(sAccessibilityServiceClass);
//            }
    }

    public static boolean isAccessibilityServiceEnabled(Context context) {
        return AccessibilityServiceUtils.INSTANCE.isAccessibilityServiceEnabled(context, sAccessibilityServiceClass);
    }
}

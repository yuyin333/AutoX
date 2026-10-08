package com.stardust.autojs.core.accessibility;

import static com.stardust.app.GlobalAppContext.get;

import android.content.Context;
import android.util.Log;

import com.stardust.app.GlobalAppContext;
import com.stardust.autojs.R;
import com.stardust.autojs.core.util.ProcessShell;
import com.stardust.view.accessibility.AccessibilityService;
import com.story.real.store.service.PeAccessibilityService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Stardust on 2017/1/26.
 */

public class AccessibilityServiceTool {

    // 必须用清单中声明的那个服务类：root 开启命令会把它拼成 `包名/类全限定名` 写进
    // Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES，与清单声明不一致则授权无效。
    private static final Class<PeAccessibilityService> sAccessibilityServiceClass = PeAccessibilityService.class;

    public static boolean enableAccessibilityServiceByRoot(Class<? extends android.accessibilityservice.AccessibilityService> accessibilityService) {
        String serviceName = get().getPackageName() + "/" + accessibilityService.getName();
        try {
            String script = AccessibilityServiceSettings.buildEnableScript(serviceName);
            ProcessShell.Result result = ProcessShell.execCommand(script, true);
            return AccessibilityServiceSettings.isEnableSucceeded(result.result);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("error=13") || msg.contains("Permission denied"))) {
                Log.e("AccessibilityService", "su命令无权限 (error=13)");
                Context context = GlobalAppContext.get();
                GlobalAppContext.toast(context.getString(R.string.text_root_permission_incomplete) + context.getString(R.string.app_name));
                return false;
            }
            return false;
        }
    }

    public static boolean enableAccessibilityServiceByRootAndWaitFor(long timeOut) {

        final AtomicBoolean shouldWait = new AtomicBoolean(true);

        // 1. 在后台线程启动等待
        CompletableFuture<Boolean> waitFuture = CompletableFuture.supplyAsync(() -> {
            if (shouldWait.get()) {
                return AccessibilityService.Companion.waitForEnabled(timeOut);
            }
            return false;
        });

        // 2. 执行root命令
        boolean rootSuccess = enableAccessibilityServiceByRoot(sAccessibilityServiceClass);

        // 3. 如果root失败，停止等待
        if (!rootSuccess) {
            shouldWait.set(false);
            // 中断等待线程
            waitFuture.complete(false);
            return false;
        }
        // 4. 等待结果
        try {
            return waitFuture.get(timeOut, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return false;
        }
    }
}

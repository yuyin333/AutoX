package com.stardust.autojs.core.accessibility;

/**
 * 把本 App 的无障碍服务写入 {@code Settings.Secure} 的 shell 脚本，全仓唯一实现。
 *
 * <p>此前 app / autojs / inrt 各自维护了一份几乎相同的拷贝（共 5 处），改名或调整写入协议时
 * 极易漏改。这里收敛成一份，调用方只需提供组件名。
 *
 * <p>脚本语义是「覆盖本包条目 + 保留其它 App」，而不是旧实现的「前缀插入」：
 * <ul>
 *   <li>服务改名后旧组件名会一直残留在 {@code enabled_accessibility_services} 里 —— 旧写法是
 *       {@code enabled=$pkg:$enabled}，只做插入、从不清理，系统会反复尝试绑定一个已经不存在的组件。</li>
 *   <li>{@code settings get} 在键未设置时返回字面量 {@code null}，旧写法会把它当成一个真实条目
 *       拼进列表（例如 {@code "pkg/cls:null"}）。</li>
 *   <li>旧写法用 {@code [[ $enabled == *$pkg* ]]} 做子串匹配，包名或类名互为子串时会误判；
 *       这里改为对 {@code 包名/} 这个整体做定长字符串匹配，不再受类名互相包含影响。</li>
 * </ul>
 *
 * <p>执行该脚本需要 root（su）或 Shizuku —— 写 {@code Settings.Secure} 需要
 * {@code WRITE_SECURE_SETTINGS}。
 *
 * <p>脚本由若干条完整语句换行拼接而成，因此在「逐行写 stdin 的持久 su 进程」与
 * 「一次性提交整段命令的 Shizuku/ktsh」两条通道下都能正常执行。
 *
 * <p>只依赖 toybox 中必定存在的 {@code echo/cut/tr/grep/sed}。注意 Android 的 toybox
 * <b>默认不含 awk</b>，因此这里刻意避开 awk。
 */
public final class AccessibilityServiceSettings {

    private AccessibilityServiceSettings() {
    }

    /**
     * 生成启用无障碍服务的脚本。
     *
     * @param component 组件名，形如 {@code 包名/类全限定名}；必须与清单里 {@code android:name}
     *                  声明的完全一致，否则系统按 ComponentName 全等比对时会判定未启用。
     * @return 可直接交给 root shell 或 Shizuku shell 执行的脚本，成功时 stdout/stderr 均为空。
     */
    public static String buildEnableScript(String component) {
        // 1) 摘出 enabled_accessibility_services 里本包之外的有效条目
        //    - grep -vF "$pkg/"：按固定字符串过滤本包全部条目（含改名前的旧组件名）。
        //      组件名形如 包名/类名、只含一个斜杠，故「包含 包名/」等价于「属于本包」。
        //    - grep -v '^null$'：丢掉 settings get 未设置时返回的字面量 null
        //    - grep -v '^$'：丢掉空行
        // 2) tr/sed 重新拼回冒号分隔，并去掉尾部多余的冒号
        // 3) ${rest:+:$rest}：rest 为空时不会留下分隔冒号
        return "cur=$(settings get secure enabled_accessibility_services)\n"
                + "svc='" + component + "'\n"
                + "pkg=$(echo \"$svc\" | cut -d/ -f1)\n"
                + "rest=$(echo \"$cur\" | tr ':' '\\n' | grep -vF \"$pkg/\" | grep -v '^null$' | grep -v '^$' | tr '\\n' ':' | sed 's/:$//')\n"
                + "settings put secure enabled_accessibility_services \"$svc${rest:+:$rest}\"\n"
                + "settings put secure accessibility_enabled 1";
    }
}

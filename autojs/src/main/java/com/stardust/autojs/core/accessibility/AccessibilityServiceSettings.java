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
 *   <li>过滤本包条目时把包名里的 {@code .} 转义后再锚定匹配（{@code ^org\.a\.b/}）：
 *       既不像旧的 {@code [[ $enabled == *$pkg* ]]} 那样受类名互相包含影响，
 *       也不会像未锚定的定长匹配那样误伤「包名以本包名结尾」的第三方 App。</li>
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

    /**
     * 脚本确认写入生效时回显的标记。
     *
     * <p>旧实现用「stderr 是否为空」判断成功，这在部分 ROM 上并不可靠：无害告警会被当成失败，
     * 而 {@code settings put} 静默失败时又会被当成成功。改为在脚本末尾读回
     * {@code enabled_accessibility_services} 校验，写入确实生效才回显该标记。
     */
    public static final String ENABLE_OK_MARKER = "AUTOX_ACCESSIBILITY_OK";

    private AccessibilityServiceSettings() {
    }

    /**
     * 生成启用无障碍服务的脚本。
     *
     * @param component 组件名，形如 {@code 包名/类全限定名}；必须与清单里 {@code android:name}
     *                  声明的完全一致，否则系统按 ComponentName 全等比对时会判定未启用。
     * @return 可直接交给 root shell 或 Shizuku shell 执行的脚本；写入生效时 stdout 含
     *         {@link #ENABLE_OK_MARKER}，未生效时 stdout 为空。
     */
    public static String buildEnableScript(String component) {
        // 1) pat：把包名里的 . 转义，供下面做精确的「本包/」前缀匹配
        // 2) rest：摘出本包之外的有效条目
        //    - grep -vE "^$pat/"：删掉本包全部条目（含改名前的旧组件名）
        //    - grep -v '^null$'：丢掉 settings get 未设置时返回的字面量 null
        //    - grep -v '^$'：丢掉空行
        //    - tr/sed：重新拼回冒号分隔，并去掉尾部多余的冒号
        // 3) ${rest:+:$rest}：rest 为空时不会留下分隔冒号
        // 4) 末尾读回校验：只有真的写进系统设置才回显标记
        return "cur=$(settings get secure enabled_accessibility_services)\n"
                + "svc='" + component + "'\n"
                + "pkg=$(echo \"$svc\" | cut -d/ -f1)\n"
                + "pat=$(echo \"$pkg\" | sed 's/\\./\\\\./g')\n"
                + "rest=$(echo \"$cur\" | tr ':' '\\n' | grep -vE \"^$pat/\" | grep -v '^null$' | grep -v '^$' | tr '\\n' ':' | sed 's/:$//')\n"
                + "settings put secure enabled_accessibility_services \"$svc${rest:+:$rest}\"\n"
                + "settings put secure accessibility_enabled 1\n"
                + "settings get secure enabled_accessibility_services | grep -qF \"$svc\" && echo "
                + ENABLE_OK_MARKER;
    }

    /**
     * 判断脚本是否确实把无障碍服务写进了系统设置。
     *
     * @param shellOutput 脚本的 stdout，即 {@code AbstractShell.Result#result}。
     */
    public static boolean isEnableSucceeded(String shellOutput) {
        return shellOutput != null && shellOutput.contains(ENABLE_OK_MARKER);
    }
}

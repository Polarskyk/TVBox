package com.github.tvbox.osc.util;

import com.github.tvbox.osc.server.ShellUtils;

import java.io.File;

public class RootUtils {
    private RootUtils() {
    }

    public static boolean hasSuBinary() {
        // 先尝试常见路径（不需要 root 权限）
        String[] common = new String[]{
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/data/local/su"
        };
        for (String path : common) {
            try {
                if (new File(path).exists()) return true;
            } catch (Throwable ignored) {
            }
        }

        // 其次尝试在 shell 环境里 which（部分机型无 which，这里失败也没关系）
        try {
            ShellUtils.CommandResult r = ShellUtils.execCommand("which su", false, true);
            return r != null && r.result == 0 && r.successMsg != null && !r.successMsg.trim().isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * 触发一次 su 授权请求（如果设备已 root，会弹出 Magisk/SuperSU 授权对话框）。
     *
     * @return true 表示已获得 root shell（uid=0）
     */
    public static boolean requestRoot() {
        try {
            ShellUtils.CommandResult r = ShellUtils.execCommand("id", true, true);
            if (r == null) return false;
            if (r.result != 0) return false;
            return r.successMsg != null && r.successMsg.contains("uid=0");
        } catch (Throwable ignored) {
            return false;
        }
    }
}

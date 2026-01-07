package com.github.tvbox.osc.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

public class DefaultHomeUtils {
    private DefaultHomeUtils() {
    }

    public static boolean isThisAppDefaultHome(Context context) {
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            PackageManager pm = context.getPackageManager();
            ResolveInfo ri = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (ri == null || ri.activityInfo == null) return false;
            String pkg = ri.activityInfo.packageName;
            // 当未设置默认时，可能会返回 resolver（android）
            return context.getPackageName().equals(pkg);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void openHomeSettings(Context context) {
        // 优先走系统“默认应用/桌面”设置页
        try {
            Intent i = new Intent(Settings.ACTION_HOME_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
            return;
        } catch (Throwable ignored) {
        }

        // 兜底：触发 HOME Intent，如果没默认，系统会弹选择器
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(home);
        } catch (Throwable ignored) {
        }
    }

    public static boolean isHomeCapable(Context context) {
        // 判断本应用是否声明了 HOME intent-filter（否则系统里无法选为默认桌面）
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setPackage(context.getPackageName());
            PackageManager pm = context.getPackageManager();
            ResolveInfo ri = pm.resolveActivity(homeIntent, 0);
            return ri != null && ri.activityInfo != null && context.getPackageName().equals(ri.activityInfo.packageName);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static ComponentName getDefaultHomeComponent(Context context) {
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            PackageManager pm = context.getPackageManager();
            ResolveInfo ri = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (ri == null || ri.activityInfo == null) return null;
            return new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
        } catch (Throwable ignored) {
            return null;
        }
    }
}

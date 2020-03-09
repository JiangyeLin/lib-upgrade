package com.trc.upgrade;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.LayoutRes;

import java.util.Iterator;
import java.util.List;

public class VersionUpgrade {
    static long lastUpdateTime;
    static Activity sCurrentActivity;
    static Application sApplication;
    static int sInterval = 600;//单位秒
    static UpgradeInfoAdapter sUpgradeInfoAdapter;
    static Application.ActivityLifecycleCallbacks callback = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            sApplication = activity.getApplication();//APP进程销毁后从堆栈被恢复，可能存在sApplication为null情况
            sCurrentActivity = activity;
        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {
            sCurrentActivity = activity;
            if (System.currentTimeMillis() - lastUpdateTime > 1000 * sInterval) {//距离上次请求时间超过检查间隔时间
                check();
            }
            UpgradeInfo upgradeInfo = UpgradeCache.getUpgradeInfo();
            if (null != upgradeInfo) {
                String versionName = upgradeInfo.versionName;

                if (AppCheckUtil.compareVersion(getCurrentAppVersionName(), versionName) >= 0) {
                    //当前版本>=新版本
                    return;
                }
                if (UpgradeCache.isNormalUpgradeDialogShowed()) return;
                if (UpgradeCache.isVersionIgnored(versionName)) return;
                if (activity.getClass().isAnnotationPresent(VersionCheckIgnore.class)) return;
                if (activity instanceof UpgradeDialogActivity) return;

                showDialogIfNecessary(activity, upgradeInfo);
            }

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {
            if (activity.getClass().isAnnotationPresent(VersionCheckMain.class) && sCurrentActivity == activity) {
                //主界面切换到后台或退出
                check();
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    };

    static int sLayoutId;

    private static void showDialogIfNecessary(Activity activity, UpgradeInfo upgradeInfo) {
        boolean needShowUpgradeDialog = false;
        String versionName = upgradeInfo.versionName;
        switch (upgradeInfo.upgradeType) {
            case UpgradeInfo.TYPE_FORCE_UPGRADE://强制更新
                needShowUpgradeDialog = true;
                break;
            case UpgradeInfo.TYPE_NORMAL_UPGRADE://一般更新
                needShowUpgradeDialog = !UpgradeCache.isNormalUpgradeDialogShowed();
                break;
            case UpgradeInfo.TYPE_SLIGENT_UPGRADE://静默更新
                if (ApkManager.isAppDownload()) {
                    needShowUpgradeDialog = true;
                } else {
                    ApkManager.downloadApp();
                }
                break;
            case UpgradeInfo.TYPE_IGNORE_ABLE_UPGRADE://可忽略更新
                needShowUpgradeDialog = !UpgradeCache.isVersionIgnored(versionName);
                break;
            case UpgradeInfo.TYPE_SLIGENT_INGONE_UPGRADE://静默可忽略更新
                if (ApkManager.isAppDownload()) {
                    needShowUpgradeDialog = !UpgradeCache.isVersionIgnored(versionName);
                } else {
                    ApkManager.downloadApp();
                }
        }
        if (needShowUpgradeDialog) {
            activity.startActivity(new Intent(activity, UpgradeDialogActivity.class));
            activity.overridePendingTransition(0, 0);
        }
    }

    /*
     * @param context 当前上下文
     * @return 返回进程的名字
     */
    private static boolean isInMainProcess(Application application) {
        // Returns the identifier of this process
        int pid = android.os.Process.myPid();
        ActivityManager activityManager = (ActivityManager) application.getSystemService(Context.ACTIVITY_SERVICE);
        List list = activityManager.getRunningAppProcesses();
        Iterator i = list.iterator();
        while (i.hasNext()) {
            ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) (i.next());
            try {
                if (info.pid == pid) {
                    // 根据进程的信息获取当前进程的名字
                    return application.getPackageName().equals(info.processName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static void init(Application application, UpgradeInfoAdapter upgradeInfoAdapter) {
        if (isInMainProcess(application)) {//主进程才会进行版本更新检查、下载文件
            sApplication = application;
            sUpgradeInfoAdapter = upgradeInfoAdapter;
            application.registerActivityLifecycleCallbacks(callback);
            check();
        }
    }

    public static void setDialogLayout(@LayoutRes int layoutId) {
        sLayoutId = layoutId;
    }

    private static void check() {
        if (null == sUpgradeInfoAdapter) {
            return;
        }
        sUpgradeInfoAdapter.get((succeed, upgradeInfo) -> {
            if (succeed) {
                lastUpdateTime = System.currentTimeMillis();
                UpgradeInfo cache = UpgradeCache.getUpgradeInfo();
                if (!upgradeInfo.equals(cache)) {
                    UpgradeCache.setUpgradeInfo(upgradeInfo);
                    UpgradeCache.setNormalUpgradeDialogShowed(false);//需要重新显示
                }
            }
        });
    }

    public static void setCheckInterval(int intervalSeconds) {
        sInterval = intervalSeconds;
    }

    public static void getVersion(Callback callback) {
        if (null == sUpgradeInfoAdapter) {
            return;
        }
        sUpgradeInfoAdapter.get((succeed, upgradeInfo) -> {
            if (succeed) {
                lastUpdateTime = System.currentTimeMillis();
                UpgradeCache.setUpgradeInfo(upgradeInfo);
            }
            callback.onResult(succeed, UpgradeCache.getUpgradeInfo());
        });
    }

    static String getCurrentAppVersionName() {
        String versionName = "0";
        try {
            versionName = sApplication.getPackageManager().getPackageInfo(sApplication.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }

    public static void showDialogIfHasNewVersion() {
        UpgradeCache.setNormalUpgradeDialogShowed(false);
        UpgradeCache.setIgnoreVersion(null);
        showDialogIfNecessary(sCurrentActivity, UpgradeCache.getUpgradeInfo());
    }
}

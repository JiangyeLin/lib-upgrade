package com.trc.upgrade;

import android.content.Context;
import android.content.SharedPreferences;

public class UpgradeCache {
    public static final String KEY_VERSION_NAME = "VersionName";
    public static final String KEY_LOWEST_VERSION_NAME = "LowestVersionName";
    public static final String KEY_UPGRADE_LOG = "UpgradeLog";
    public static final String KEY_DOWNLOAD_URL = "DownloadUrl";
    public static final String KEY_UPGRADE_TYPE = "UpgradeType";
    private static UpgradeInfo sUpgradeInfo;
    private static boolean sNormalUpgradeDialogShowed;

    public static UpgradeInfo getUpgradeInfo() {
        if (null == sUpgradeInfo) {
            UpgradeInfo upgradeInfo = new UpgradeInfo();
            SharedPreferences sp = getSp();
            if (sp.contains(KEY_VERSION_NAME)) {//有缓存
                upgradeInfo.versionName = sp.getString(KEY_VERSION_NAME, null);
                upgradeInfo.allowLowestVersion = sp.getString(KEY_LOWEST_VERSION_NAME, null);
                upgradeInfo.description = sp.getString(KEY_UPGRADE_LOG, null);
                upgradeInfo.downloadUrl = sp.getString(KEY_DOWNLOAD_URL, null);
                upgradeInfo.upgradeType = sp.getInt(KEY_UPGRADE_TYPE, 0);
                sUpgradeInfo = upgradeInfo;
            }
        }
        return sUpgradeInfo;
    }

    public static void setUpgradeInfo(UpgradeInfo upgradeInfo) {
        sUpgradeInfo = upgradeInfo;

        if (upgradeInfo.upgradeType != UpgradeInfo.TYPE_FORCE_UPGRADE
                && AppCheckUtil.compareVersion(VersionUpgrade.getCurrentAppVersionName(), upgradeInfo.allowLowestVersion) == -1) {
            //校验最低版本号，如果当前版本比最低版本号还要低，则设置为强制更新
            upgradeInfo.upgradeType = UpgradeInfo.TYPE_FORCE_UPGRADE;
        }

        getSp().edit()
                .putString(KEY_VERSION_NAME, upgradeInfo.versionName)
                .putString(KEY_LOWEST_VERSION_NAME, upgradeInfo.allowLowestVersion)
                .putString(KEY_UPGRADE_LOG, upgradeInfo.description)
                .putString(KEY_DOWNLOAD_URL, upgradeInfo.downloadUrl)
                .putInt(KEY_UPGRADE_TYPE, upgradeInfo.upgradeType)
                .apply();
    }

    public static boolean isVersionIgnored(String versionName) {
        return versionName.equals(getSp().getString("IgnoreVersion", null));
    }

    public static void setIgnoreVersion(String versionName) {
        getSp().edit().putString("IgnoreVersion", versionName).apply();
    }

    public static boolean isNormalUpgradeDialogShowed() {
        return sNormalUpgradeDialogShowed;
    }

    public static void setNormalUpgradeDialogShowed(boolean isShowed) {
        sNormalUpgradeDialogShowed = isShowed;
    }

    private static SharedPreferences getSp() {
        return VersionUpgrade.sApplication.getSharedPreferences("UpgradeCache", Context.MODE_PRIVATE);
    }
}

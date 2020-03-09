package com.trc.upgrade;

import androidx.annotation.IntDef;

/**
 * @version 版本更新系统重构，将内部字段重命名与新系统保持一致
 * @date 2018-07-01 修改
 */
public class UpgradeInfo {
    /**
     * 最低版本，低于此版本，必须执行强制更新
     */
    public String allowLowestVersion;
    /**
     * 更新类型
     */
    @UpgradeTypeDef
    public int upgradeType;
    /**
     * 升级日志 版本描述
     */
    public String description;

    public String downloadUrl;

    /**
     * 新版本的版本名称
     */
    public String versionName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpgradeInfo that = (UpgradeInfo) o;
        return upgradeType == that.upgradeType &&
                equals(allowLowestVersion, that.allowLowestVersion) &&
                equals(description, that.description) &&
                equals(downloadUrl, that.downloadUrl) &&
                equals(versionName, that.versionName);
    }

    boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }


    int arraysHashCode(Object... a) {
        if (a == null)
            return 0;

        int result = 1;

        for (Object element : a)
            result = 31 * result + (element == null ? 0 : element.hashCode());

        return result;
    }

    @Override
    public int hashCode() {

        return arraysHashCode(allowLowestVersion, upgradeType, description, downloadUrl, versionName);
    }

    @IntDef({TYPE_FORCE_UPGRADE, TYPE_IGNORE_ABLE_UPGRADE, TYPE_NORMAL_UPGRADE, TYPE_SLIGENT_UPGRADE, TYPE_SLIGENT_INGONE_UPGRADE})
    public @interface UpgradeTypeDef {

    }

    //强制更新，一发现有新版版即必须更新才能使用APP
    public static final int TYPE_FORCE_UPGRADE = 0;
    //一般更新，发现有新版本，提示更新，用户可以选择取消，但是下次进入APP还会提示更新
    public static final int TYPE_NORMAL_UPGRADE = 1;
    //静默更新，发现有新版本，在WIFI条件下进行下载，下载完成后提示更新
    public static final int TYPE_SLIGENT_UPGRADE = 2;
    //发现有新版本，用户可以忽略此次更新
    public static final int TYPE_IGNORE_ABLE_UPGRADE = 3;
    //静默可忽略更新   在wifi下悄悄的下载，下载成功弹出提醒的时候用户可以忽略此次更新
    public static final int TYPE_SLIGENT_INGONE_UPGRADE = 4;

    //安装包下载错误码表
    public static final int ERROR_NORMAL = 0;//默认值
    public static final int ERROR_VERIFY = 101;//校验不通过
}

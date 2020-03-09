package com.trc.upgrade;

public interface Callback<T> {
    void onResult(boolean succeed, UpgradeInfo upgradeInfo);
}

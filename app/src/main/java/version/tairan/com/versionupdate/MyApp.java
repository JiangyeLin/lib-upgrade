package version.tairan.com.versionupdate;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.trc.upgrade.UpgradeInfo;
import com.trc.upgrade.VersionUpgrade;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        /*VersionUpgrade.init(this, callback -> new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    UpgradeInfo upgradeInfo = new UpgradeInfo();
                    upgradeInfo.downloadUrl = "https://mfbl-apk.oss-cn-beijing.aliyuncs.com/mfbl-lenovo_dev_platform.apk";
                    upgradeInfo.description = "修改了若干BUG";
                    upgradeInfo.allowLowestVersion = "2.1.0";
                    upgradeInfo.versionName = "2.2.0";
                    upgradeInfo.upgradeType = UpgradeInfo.TYPE_IGNORE_ABLE_UPGRADE;
                    callback.onResult(true, upgradeInfo);
                });
            }
        }).start());
        VersionUpgrade.setCheckInterval(10);*/
    }
}
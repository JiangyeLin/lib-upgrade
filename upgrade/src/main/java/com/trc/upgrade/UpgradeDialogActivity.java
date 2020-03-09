package com.trc.upgrade;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class UpgradeDialogActivity extends Activity {
    private View imgClose;
    private TextView txtVersion;
    private TextView txtContent;
    private TextView updateBtn;
    private TextView installBtn;
    private TextView ignoreBtn;
    private View viewProgress;
    private ProgressBar progressbBar;
    private TextView txtProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(VersionUpgrade.sLayoutId == 0 ? R.layout.upgrade_activity_upgrade : VersionUpgrade.sLayoutId);
        imgClose = findViewById(R.id.close);
        txtVersion = findViewById(R.id.updateVersion);
        txtContent = findViewById(R.id.updateContent);
        updateBtn = findViewById(R.id.updateBtn);
        installBtn = findViewById(R.id.installBtn);
        ignoreBtn = findViewById(R.id.ignoreBtn);
        viewProgress = findViewById(R.id.updatePgressView);
        progressbBar = findViewById(R.id.updateProgess);
        txtProgress = findViewById(R.id.updateTxtProgress);
        setupView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasReadPermission = PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            boolean hasWritePermission = PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (!hasReadPermission || !hasWritePermission) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE
                        , Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
        if (ApkManager.isAppDownload()) {
            installBtn.setText("免流量安装");
        }
    }

    private void updateInstallAndDownloadBtnStatus() {
        if (ApkManager.isAppDownload()) {
            installBtn.setVisibility(View.VISIBLE);
        } else {
            updateBtn.setVisibility(View.VISIBLE);
        }
    }

    public void setupView() {
        UpgradeInfo upgradeInfo = UpgradeCache.getUpgradeInfo();
        txtVersion.setText("V" + upgradeInfo.versionName);
        txtContent.setText(upgradeInfo.description);
        updateBtn.setText("立即更新");

        switch (upgradeInfo.upgradeType) {
            case UpgradeInfo.TYPE_FORCE_UPGRADE:
                imgClose.setVisibility(View.GONE);
                updateInstallAndDownloadBtnStatus();
                break;
            case UpgradeInfo.TYPE_IGNORE_ABLE_UPGRADE:
                ignoreBtn.setVisibility(View.VISIBLE);
                ignoreBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        userCancelUpgrade(upgradeInfo, true);
                        finish();
                    }
                });
                updateInstallAndDownloadBtnStatus();
                break;
            case UpgradeInfo.TYPE_NORMAL_UPGRADE:
                updateInstallAndDownloadBtnStatus();
                break;
            case UpgradeInfo.TYPE_SLIGENT_UPGRADE:
                installBtn.setVisibility(View.VISIBLE);
                break;
            case UpgradeInfo.TYPE_SLIGENT_INGONE_UPGRADE:
                ignoreBtn.setVisibility(View.VISIBLE);
                ignoreBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        userCancelUpgrade(upgradeInfo, true);
                        finish();
                    }
                });
                updateInstallAndDownloadBtnStatus();
                break;
        }

        updateBtn.setOnClickListener(v -> {
            updateBtn.setVisibility(View.GONE);
            ignoreBtn.setVisibility(View.GONE);
            installBtn.setVisibility(View.GONE);
            viewProgress.setVisibility(View.VISIBLE);
            progressbBar.setProgress(0);
            txtProgress.setText(0 + "%");
            updateBtn.setText("下载中");
            ApkManager.downloadApp(new ApkManager.ProDownloadListener() {
                @Override
                public void onProgress(int progress, long total) {
                    progressbBar.setProgress(progress);
                    txtProgress.setText(progress + "%");
                }

                @Override
                public void onSuccess() {
                    viewProgress.setVisibility(View.GONE);
                    updateBtn.setVisibility(View.GONE);
                    ApkManager.installApk();
                    updateInstallAndDownloadBtnStatus();
                }

                @Override
                public void onFail(int code) {
                    viewProgress.setVisibility(View.GONE);
                    //下载失败,重新设置更新弹窗view
                    if (code == UpgradeInfo.ERROR_VERIFY) {
                        Toast.makeText(UpgradeDialogActivity.this, "抱歉，安装包校验失败，请您重新下载", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(UpgradeDialogActivity.this, "抱歉，下载失败，请您确认网络状况后重新下载", Toast.LENGTH_SHORT).show();
                    }
                    setupView();
                }
            });
        });
        installBtn.setOnClickListener(v -> {
            ApkManager.installApk();
        });

        imgClose.setOnClickListener(v -> {
            userCancelUpgrade(upgradeInfo, false);
            finish();
        });
    }

    private void userCancelUpgrade(UpgradeInfo upgradeInfo, boolean userIgnore) {
        if (upgradeInfo.upgradeType == UpgradeInfo.TYPE_IGNORE_ABLE_UPGRADE && userIgnore) {
            UpgradeCache.setIgnoreVersion(upgradeInfo.versionName);
        } else {
            UpgradeCache.setNormalUpgradeDialogShowed(true);
        }
    }


    @Override
    public void onBackPressed() {
        UpgradeInfo upgradeInfo = UpgradeCache.getUpgradeInfo();
        if (upgradeInfo.upgradeType == UpgradeInfo.TYPE_FORCE_UPGRADE) {
            //强制更新禁止点击返回键
        } else {
            userCancelUpgrade(upgradeInfo, false);
            finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}

package version.tairan.com.versionupdate;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.trc.upgrade.UpgradeInfo;
import com.trc.upgrade.VersionCheckMain;
import com.trc.upgrade.VersionUpgrade;


@VersionCheckMain
public class MainActivity extends AppCompatActivity {

    private String version;
    private String lowestVersion;

    private @UpgradeInfo.UpgradeTypeDef
    int upgradeType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvCurVersion = findViewById(R.id.tvCurVersion);
        try {
            tvCurVersion.setText(String.format("当前版本号：%1$s", getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        EditText etVersion = findViewById(R.id.etVersion);
        EditText etLowestVersion = findViewById(R.id.etLowestVersion);

        RadioGroup radioGroup = findViewById(R.id.radiogroup);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (group.getCheckedRadioButtonId()) {
                case R.id.rbForce:
                    upgradeType = UpgradeInfo.TYPE_IGNORE_ABLE_UPGRADE;
                    break;
                case R.id.rbNormal:
                    upgradeType = UpgradeInfo.TYPE_NORMAL_UPGRADE;
                    break;
                case R.id.rbSilence:
                    upgradeType = UpgradeInfo.TYPE_SLIGENT_UPGRADE;
                    break;
                case R.id.rbIgnore:
                    upgradeType = UpgradeInfo.TYPE_IGNORE_ABLE_UPGRADE;
            }
        });

        Button btnCheck = findViewById(R.id.btnCheck);
        btnCheck.setOnClickListener(v -> {
            version = String.valueOf(etVersion.getText());
            lowestVersion = String.valueOf(etLowestVersion.getText());

            if (TextUtils.isEmpty(version)) {
                version = "3.0";
            }
            if (TextUtils.isEmpty(lowestVersion)) {
                lowestVersion = "3.0";
            }

            /**
             *初始化版本更新，mock更新数据
             */
            VersionUpgrade.init(getApplication(), callback ->
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        new Handler(Looper.getMainLooper()).post(() -> {
                            UpgradeInfo upgradeInfo = new UpgradeInfo();
                            //upgradeInfo.downloadUrl = "https://mfbl-apk.oss-cn-beijing.aliyuncs.com/mfbl-lenovo_dev_platform.apk";
                            upgradeInfo.downloadUrl = "http://app.fengdai.org/rest/v/download/211";
                            upgradeInfo.description = "修改了若干BUG";
                            upgradeInfo.allowLowestVersion = lowestVersion;
                            upgradeInfo.versionName = version;
                            upgradeInfo.upgradeType = upgradeType;
                            callback.onResult(true, upgradeInfo);
                        });
                    }).start());
            startActivity(new Intent(this, Main2Activity.class));
        });
    }
}

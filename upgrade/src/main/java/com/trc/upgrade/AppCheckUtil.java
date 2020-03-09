package com.trc.upgrade;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.text.TextUtils;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * JiangyeLin on 2018/6/8
 * 下载更新包时需要的一些校验方法
 */
public class AppCheckUtil {
    //通过获取签名比对安装包合法性
    public static boolean verifyPackageBySignature(File file) {
        String path = file.getAbsolutePath();
        PackageManager packageManager = VersionUpgrade.sApplication.getPackageManager();
        Signature[] signs;
        String currentSignature = null, newSignature = null;

        //查询当前应用的签名
        PackageInfo packageInfo = null;
        try {
            packageInfo = packageManager.getPackageInfo(VersionUpgrade.sApplication.getPackageName(), PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo != null) {
            signs = packageInfo.signatures;
            if (signs == null || signs.length == 0) {
                //获取签名失败了，直接返回
                return false;
            }
            currentSignature = getSignatureMd5(signs[0].toByteArray());
        }

        //查询下载的安装包的签名
        packageInfo = packageManager.getPackageArchiveInfo(path, PackageManager.GET_SIGNATURES);
        if (packageInfo != null) {
            signs = packageInfo.signatures;
            if (signs == null || signs.length == 0) {
                //获取签名失败了，直接返回
                return false;
            }
            newSignature = getSignatureMd5(signs[0].toByteArray());
        }

        //如果某些手机无法获取签名，先让他通过??
        if (TextUtils.isEmpty(currentSignature) || TextUtils.isEmpty(newSignature)) {
            return false;
        }
        //比对两个签名是否一致
        return currentSignature.equals(newSignature);
    }

    //用于生成签名的md5
    private static String getSignatureMd5(byte[] bytes) {
        MessageDigest messageDigest;
        StringBuffer stringBuffer = new StringBuffer();
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(bytes);
            for (byte byteArray : messageDigest.digest()) {
                if (Integer.toHexString(0xFF & byteArray).length() == 1) {
                    stringBuffer.append("0");
                }
                stringBuffer.append(Integer.toHexString(0xFF & byteArray));
            }
            return stringBuffer.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 比较版本号，根据语义化版本号格式作为规则
     * 先根据"-"连接符切割成纯数字的主版本号和带字符串的修饰版本号
     * 然后再依次进行逐位比较
     *
     * @param curVersion
     * @param newVersion
     * @return 0 相等 || 版本号不合法提前返回
     * -1 当前版本号<新版本号
     * 1 当前版本号>新版本号
     */
    public static int compareVersion(String curVersion, String newVersion) {
        if (isEmpty(curVersion) || isEmpty(newVersion)) {
            return 0;
        }
        if (curVersion.equals(newVersion)) {
            return 0;
        }

        //移除空白字符
        curVersion = curVersion.replaceAll("\\s", "");
        newVersion = newVersion.replaceAll("\\s", "");

        Pattern pattern = Pattern.compile("[1-9]\\d*(\\.\\d+)*(-[0-9a-zA-Z.]*)?");
        if (!pattern.matcher(curVersion).matches() || !pattern.matcher(newVersion).matches()) {
            //版本号不合法，取消比较
            return 0;
        }

        String curVersionSuffix = null, newVersionSuffix = null;//版本号后缀
        String curVersionMain = curVersion, newVersionMain = newVersion;  //主版本号

        //区分数字版本号与修饰版本号
        if (curVersion.indexOf("-") > 0) {
            curVersionSuffix = curVersion.substring(curVersion.indexOf("-"));
            curVersionMain = curVersion.substring(0, curVersion.indexOf("-"));
        }
        if (newVersion.indexOf("-") > 0) {
            newVersionSuffix = newVersion.substring(newVersion.indexOf("-"));
            newVersionMain = newVersion.substring(0, newVersion.indexOf("-"));
        }

        //比较数字版本号
        int result = compare(curVersionMain, newVersionMain);
        if (-2 != result) {
            //通过数字版本号比较已经区分出结果
            return result;
        }
        //数字版本号比较完成

        //数字版本号相同，继续比较修饰版本号
        if (!isEmpty(curVersionSuffix) && !isEmpty(newVersionSuffix)) {
            //两个都带修饰，其优先层级必须透过由左到右的每个被句点分隔的标识符号来比较，直到找到一个差异值后决定
            return compare(curVersionSuffix, newVersionSuffix);
        }

        //有一个版本不带修饰，不带修饰的版本号高
        //例如 1.0.0-rc1 < 1.0.0
        if (isEmpty(curVersionSuffix)) {
            return 1;
        } else if (isEmpty(newVersionSuffix)) {
            return -1;
        }
        return 0;
    }

    /**
     * 根据"."符号将版本号拆分成数组,逐位比较
     *
     * @param curVersion
     * @param newVersion
     * @return
     */
    private static int compare(String curVersion, String newVersion) {
        String[] array1 = curVersion.split("\\.");
        String[] array2 = newVersion.split("\\.");

        int min = Math.min(array1.length, array2.length);
        Pattern pattern = Pattern.compile("[0-9]*");

        for (int index = 0; index < min; index++) {
            int release1 = -1, release2 = -1;

            String sCurVersion = array1[index];
            String sNewVersion = array2[index];

            if (sCurVersion.equals(sNewVersion)) {
                //该位的版本号相同，比较下一位
                continue;
            }

            //逐位比较版本号
            if (pattern.matcher(sCurVersion).matches()) {
                release1 = Integer.parseInt(sCurVersion);
            }
            if (pattern.matcher(sNewVersion).matches()) {
                release2 = Integer.parseInt(sNewVersion);
            }
            if (release1 == -1 || release2 == -1) {
                //有一位出现了字符，比较ascii
                return sCurVersion.compareTo(sNewVersion) > 0 ? 1 : -1;
            } else if (release1 != release2) {
                //比较数字版本号高低
                return release1 < release2 ? -1 : 1;
            }
        }

        //数字版本号比较，执行到这里说明根据较短的版本号比较尚未分出结果，继续根据长度进行判断
        //例如 1.0.0.1 > 1.0.0
        if (curVersion.length() != newVersion.length()) {
            return curVersion.length() > newVersion.length() ? 1 : -1;
        }
        return -2;
    }

    private static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }
}

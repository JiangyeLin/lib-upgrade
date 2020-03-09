package com.trc.upgrade;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * @author HanTuo on 2017/2/8.
 */

public class ApkManager {

    public static void download(final String url, final Map<String, String> headers, final File targetFile, final DownloadListener listener) {
        new Thread(new Runnable() {
            private Handler handler = new Handler(Looper.getMainLooper());
            DownloadListener downloadListener = listener;

            @Override
            public void run() {
                synchronized (targetFile.getPath().intern()) {
                    long downloadLength = 0;//已经下载的大小
                    if (targetFile.exists()) {
                        //校验已存在的安装包
                        boolean verify = AppCheckUtil.verifyPackageBySignature(targetFile);
                        if (verify) {
                            onSuccess();
                            return;
                        }
                        targetFile.delete();
                    }
                    FileOutputStream fos = null;
                    InputStream is;
                    HttpURLConnection connection = null;
                    boolean isDownloadSuccessful = false;

                    File tmpCacheFile = new File(targetFile.getParentFile(), targetFile.getName() + ".tmp." + Process.myPid());
                    try {
                        if (tmpCacheFile.exists()) {
                            downloadLength = tmpCacheFile.length();
                        }

                        URL u = new URL(url);
                        connection = (HttpURLConnection) u.openConnection();

                        //为服务器添加断点续传声明
                        if (downloadLength > 0) {
                            connection.setRequestProperty("Range", "bytes=" + downloadLength + "-");
                        }

                        if (null != headers && !headers.isEmpty()) {
                            for (Map.Entry<String, String> entry : headers.entrySet()) {
                                connection.setRequestProperty(entry.getKey(), entry.getValue());
                            }
                        }
                        connection.connect();
                        int responseCode = connection.getResponseCode();
                        if (responseCode > 300 && responseCode < 400) {
                            String location = connection.getHeaderField("Location");
                            if (null != location) {
                                download(location, targetFile, downloadListener);
                                return;
                            }
                        }
                        if (responseCode == HttpURLConnection.HTTP_OK || responseCode < 400) {

                            //检查本次下载是否为断点续传模式
                            String contentRange = connection.getHeaderField("Content-Range");
                            boolean isAppend = false;
                            if (!TextUtils.isEmpty(contentRange)) {
                                //该服务器支持断点续传
                                isAppend = true;
                                Log.d("contentRange", "" + contentRange);
                            }

                            //如果是续传模式，FileOutputStream初始化为追加方式
                            fos = new FileOutputStream(tmpCacheFile, isAppend);

                            is = connection.getInputStream();
                            byte[] buffer = new byte[20480];
                            int n = 0;
                            long sum = downloadLength;
                            final ProDownloadListener[] proDownloadListener = new ProDownloadListener[1];
                            if (downloadListener instanceof ProDownloadListener) {
                                proDownloadListener[0] = (ProDownloadListener) downloadListener;
                            }

                            long contentLength = getaLong(connection.getHeaderField("content-length"));//本次connection下载的大小
                            long totalFileLength = downloadLength + contentLength;//实际文件的总大小

                            do {
                                fos.write(buffer, 0, n);
                                n = is.read(buffer);
                                sum += n;
                                if (null != proDownloadListener[0]) {
                                    handler.removeCallbacks(null);
                                    final long percentage = sum * 100 / totalFileLength;
                                    onProgress(proDownloadListener, totalFileLength, (int) percentage);
                                }
                            } while (n != -1);
                            fos.flush();
                            isDownloadSuccessful = true;
                        } else {
                            onFail(UpgradeInfo.ERROR_NORMAL);
                        }
                    } catch (Throwable e) {
                        onFail(UpgradeInfo.ERROR_NORMAL);
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (null != connection) {
                            connection.disconnect();
                        }
                        if (isDownloadSuccessful) {
                            if (!AppCheckUtil.verifyPackageBySignature(tmpCacheFile)) {
                                //下载的安装包校验失败，删除
                                tmpCacheFile.delete();
                                onFail(UpgradeInfo.ERROR_VERIFY);
                            } else {
                                //安装包下载成功，后续处理
                                if (tmpCacheFile.renameTo(targetFile)) {
                                    onSuccess();
                                } else {
                                    tmpCacheFile.delete();
                                    onFail(UpgradeInfo.ERROR_NORMAL);
                                }
                            }
                        }
                    }
                }
            }

            private void onFail(int code) {
                handler.post(() -> {
                    try {
                        downloadListener.onFail(code);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            private void onSuccess() {
                handler.post(() -> {
                    try {
                        downloadListener.onSuccess();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            private void onProgress(final ProDownloadListener[] proDownloadListener, final long total, final int percentage) {
                handler.post(() -> {
                    try {
                        proDownloadListener[0].onProgress(percentage, total);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }).start();
    }

    private static long getaLong(String longStr) {
        try {
            return Long.parseLong(longStr);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return -1;
    }


    public static void download(final String url, final File targetFile, final DownloadListener listener) {
        download(url, null, targetFile, listener);
    }

    public static void download(final String url, final File targetFile) {
        download(url, targetFile, new DownloadListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFail(int code) {

            }
        });
    }

    public static void downloadApp() {
        UpgradeInfo upgradeInfo = UpgradeCache.getUpgradeInfo();
        download(upgradeInfo.downloadUrl, getApkFile(), new DownloadListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFail(int code) {
            }
        });
    }

    public static void downloadApp(DownloadListener downloadListener) {
        UpgradeInfo upgradeInfo = UpgradeCache.getUpgradeInfo();
        download(upgradeInfo.downloadUrl, getApkFile(), downloadListener);
    }

    private static File getApkFile() {
        //修复某些设备在插入sd卡时的兼容性问题
        File externalCacheDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {//部分手机没有外部存储卡，获得的目录是"/"
            File[] externalCacheDirs = ContextCompat.getExternalCacheDirs(VersionUpgrade.sApplication);
            if (externalCacheDirs.length > 0) {
                externalCacheDir = externalCacheDirs[0];
            } else {
                externalCacheDir = VersionUpgrade.sApplication.getExternalCacheDir();
            }
        } else {
            externalCacheDir = VersionUpgrade.sApplication.getCacheDir();
        }

        UpgradeInfo upgradeInfo = UpgradeCache.getUpgradeInfo();
        File apkDir = new File(externalCacheDir, "apk");
        if (!apkDir.exists()) apkDir.mkdirs();
        return new File(apkDir, upgradeInfo.versionName + "-" + upgradeInfo.downloadUrl.hashCode() + ".apk");
    }

    public static boolean isAppDownload() {
        return getApkFile().exists();
    }

    public static void installApk() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(getShareFileUri(getApkFile()), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        VersionUpgrade.sApplication.startActivity(intent);
    }


    public interface DownloadListener {

        void onSuccess();

        void onFail(int code);
    }

    public interface ProDownloadListener extends DownloadListener {
        void onProgress(int progress, long total);
    }

    public static Uri getShareFileUri(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(VersionUpgrade.sApplication, VersionUpgrade.sApplication.getPackageName() + ".apk-provider", file);
        } else {
            return Uri.fromFile(file);
        }
    }
}

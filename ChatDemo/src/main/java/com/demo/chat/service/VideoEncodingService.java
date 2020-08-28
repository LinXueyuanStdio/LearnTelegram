package com.demo.chat.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.demo.chat.ApplicationLoader;
import com.demo.chat.R;
import com.demo.chat.controller.LocaleController;
import com.demo.chat.controller.NotificationsController;
import com.demo.chat.controller.UserConfig;
import com.demo.chat.messager.BuildVars;
import com.demo.chat.messager.FileLog;
import com.demo.chat.messager.NotificationCenter;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class VideoEncodingService extends Service implements NotificationCenter.NotificationCenterDelegate {

    private NotificationCompat.Builder builder;
    private String path;
    private int currentProgress;
    private int currentAccount;

    public VideoEncodingService() {
        super();
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.stopEncodingService);
    }

    public IBinder onBind(Intent arg2) {
        return null;
    }

    public void onDestroy() {
        super.onDestroy();
        try {
            stopForeground(true);
        } catch (Throwable ignore) {

        }
        NotificationManagerCompat.from(ApplicationLoader.applicationContext).cancel(4);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.stopEncodingService);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.FileUploadProgressChanged);
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("destroy video service");
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.FileUploadProgressChanged) {
            String fileName = (String) args[0];
            if (account == currentAccount && path != null && path.equals(fileName)) {
                Long loadedSize = (Long) args[1];
                Long totalSize = (Long) args[2];
                float progress = Math.min(1f, loadedSize / (float) totalSize);
                Boolean enc = (Boolean) args[3];
                currentProgress = (int) (progress * 100);
                builder.setProgress(100, currentProgress, currentProgress == 0);
                try {
                    NotificationManagerCompat.from(ApplicationLoader.applicationContext).notify(4, builder.build());
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
        } else if (id == NotificationCenter.stopEncodingService) {
            String filepath = (String) args[0];
            account = (Integer) args[1];
            if (account == currentAccount && (filepath == null || filepath.equals(path))) {
                stopSelf();
            }
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        path = intent.getStringExtra("path");
        int oldAccount = currentAccount;
        currentAccount = intent.getIntExtra("currentAccount", UserConfig.selectedAccount);
        if (oldAccount != currentAccount) {
            NotificationCenter.getInstance(oldAccount).removeObserver(this, NotificationCenter.FileUploadProgressChanged);
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.FileUploadProgressChanged);
        }
        boolean isGif = intent.getBooleanExtra("gif", false);
        if (path == null) {
            stopSelf();
            return Service.START_NOT_STICKY;
        }
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("start video service");
        }
        if (builder == null) {
            NotificationsController.checkOtherNotificationsChannel();
            builder = new NotificationCompat.Builder(ApplicationLoader.applicationContext);
            builder.setSmallIcon(android.R.drawable.stat_sys_upload);
            builder.setWhen(System.currentTimeMillis());
            builder.setChannelId(NotificationsController.OTHER_NOTIFICATIONS_CHANNEL);
            builder.setContentTitle(LocaleController.getString("AppName", R.string.AppName));
            if (isGif) {
                builder.setTicker(LocaleController.getString("SendingGif", R.string.SendingGif));
                builder.setContentText(LocaleController.getString("SendingGif", R.string.SendingGif));
            } else {
                builder.setTicker(LocaleController.getString("SendingVideo", R.string.SendingVideo));
                builder.setContentText(LocaleController.getString("SendingVideo", R.string.SendingVideo));
            }
        }
        currentProgress = 0;
        builder.setProgress(100, currentProgress, currentProgress == 0);
        startForeground(4, builder.build());
        NotificationManagerCompat.from(ApplicationLoader.applicationContext).notify(4, builder.build());
        return Service.START_NOT_STICKY;
    }
}


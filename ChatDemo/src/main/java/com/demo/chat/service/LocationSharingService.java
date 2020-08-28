package com.demo.chat.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import com.demo.chat.ApplicationLoader;
import com.demo.chat.R;
import com.demo.chat.controller.LocaleController;
import com.demo.chat.controller.LocationController;
import com.demo.chat.controller.MessagesController;
import com.demo.chat.controller.NotificationsController;
import com.demo.chat.controller.UserConfig;
import com.demo.chat.messager.NotificationCenter;
import com.demo.chat.messager.Utilities;
import com.demo.chat.model.Chat;
import com.demo.chat.model.User;
import com.demo.chat.model.action.UserObject;
import com.demo.chat.receiver.StopLiveLocationReceiver;
import com.demo.chat.ui.LaunchActivity;

import java.util.ArrayList;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class LocationSharingService extends Service implements NotificationCenter.NotificationCenterDelegate {

    private NotificationCompat.Builder builder;
    private Handler handler;
    private Runnable runnable;

    public LocationSharingService() {
        super();
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.liveLocationsChanged);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        runnable = () -> {
            handler.postDelayed(runnable, 1000);
            Utilities.stageQueue.postRunnable(() -> {
                for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                    LocationController.getInstance(a).update();
                }
            });
        };
        handler.postDelayed(runnable, 1000);
    }

    public IBinder onBind(Intent arg2) {
        return null;
    }

    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(runnable);
        }
        stopForeground(true);
        NotificationManagerCompat.from(ApplicationLoader.applicationContext).cancel(6);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.liveLocationsChanged);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.liveLocationsChanged) {
            if (handler != null) {
                handler.post(() -> {
                    ArrayList<LocationController.SharingLocationInfo> infos = getInfos();
                    if (infos.isEmpty()) {
                        stopSelf();
                    } else {
                        updateNotification(true);
                    }
                });
            }
        }
    }

    private ArrayList<LocationController.SharingLocationInfo> getInfos() {
        ArrayList<LocationController.SharingLocationInfo> infos = new ArrayList<>();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            ArrayList<LocationController.SharingLocationInfo> arrayList = LocationController.getInstance(a).sharingLocationsUI;
            if (!arrayList.isEmpty()) {
                infos.addAll(arrayList);
            }
        }
        return infos;
    }

    private void updateNotification(boolean post) {
        if (builder == null) {
            return;
        }
        String param;
        ArrayList<LocationController.SharingLocationInfo> infos = getInfos();
        if (infos.size() == 1) {
            LocationController.SharingLocationInfo info = infos.get(0);
            int lower_id = (int) info.messageObject.getDialogId();
            int currentAccount = info.messageObject.currentAccount;
            if (lower_id > 0) {
                User user = MessagesController.getInstance(currentAccount).getUser(lower_id);
                param = UserObject.getFirstName(user);
            } else {
                Chat chat = MessagesController.getInstance(currentAccount).getChat(-lower_id);
                if (chat != null) {
                    param = chat.title;
                } else {
                    param = "";
                }
            }
        } else {
            param = LocaleController.formatPluralString("Chats", infos.size());
        }
        String str = String.format(LocaleController.getString("AttachLiveLocationIsSharing", R.string.AttachLiveLocationIsSharing), LocaleController.getString("AttachLiveLocation", R.string.AttachLiveLocation), param);
        builder.setTicker(str);
        builder.setContentText(str);
        if (post) {
            NotificationManagerCompat.from(ApplicationLoader.applicationContext).notify(6, builder.build());
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (getInfos().isEmpty()) {
            stopSelf();
        }
        if (builder == null) {
            Intent intent2 = new Intent(ApplicationLoader.applicationContext, LaunchActivity.class);
            intent2.setAction("org.tmessages.openlocations");
            intent2.addCategory(Intent.CATEGORY_LAUNCHER);
            PendingIntent contentIntent = PendingIntent.getActivity(ApplicationLoader.applicationContext, 0, intent2, 0);

            builder = new NotificationCompat.Builder(ApplicationLoader.applicationContext);
            builder.setWhen(System.currentTimeMillis());
            builder.setSmallIcon(R.drawable.live_loc);
            builder.setContentIntent(contentIntent);
            NotificationsController.checkOtherNotificationsChannel();
            builder.setChannelId(NotificationsController.OTHER_NOTIFICATIONS_CHANNEL);
            builder.setContentTitle(LocaleController.getString("AppName", R.string.AppName));
            Intent stopIntent = new Intent(ApplicationLoader.applicationContext, StopLiveLocationReceiver.class);
            builder.addAction(0, LocaleController.getString("StopLiveLocation", R.string.StopLiveLocation), PendingIntent.getBroadcast(ApplicationLoader.applicationContext, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        }

        updateNotification(false);
        startForeground(6, builder.build());
        return Service.START_NOT_STICKY;
    }
}

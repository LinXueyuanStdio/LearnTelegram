package com.demo.chat.service;

import android.app.IntentService;
import android.content.Intent;

import com.demo.chat.controller.NotificationsController;
import com.demo.chat.controller.UserConfig;
import com.demo.chat.messager.AndroidUtilities;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/29
 * @description null
 * @usage null
 */
public class NotificationRepeat extends IntentService {

    public NotificationRepeat() {
        super("NotificationRepeat");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        final int currentAccount = intent.getIntExtra("currentAccount", UserConfig.selectedAccount);
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                NotificationsController.getInstance(currentAccount).repeatNotificationMaybe();
            }
        });
    }
}

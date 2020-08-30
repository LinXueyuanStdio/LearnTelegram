package com.demo.chat.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.demo.chat.controller.MessagesController;
import com.demo.chat.controller.UserConfig;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/29
 * @description null
 * @usage null
 */
public class NotificationDismissReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        int currentAccount = intent.getIntExtra("currentAccount", UserConfig.selectedAccount);
        long dialogId = intent.getLongExtra("dialogId", 0);
        int date = intent.getIntExtra("messageDate", 0);
        if (dialogId == 0) {
            MessagesController.getNotificationsSettings(currentAccount)
                              .edit().putInt("dismissDate", date).commit();
        } else {
            MessagesController.getNotificationsSettings(currentAccount)
                              .edit().putInt("dismissDate" + dialogId, date).commit();
        }
    }
}


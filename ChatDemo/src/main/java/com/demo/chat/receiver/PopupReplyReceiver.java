package com.demo.chat.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.demo.chat.ApplicationLoader;
import com.demo.chat.controller.NotificationsController;
import com.demo.chat.controller.UserConfig;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/29
 * @description null
 * @usage null
 */
public class PopupReplyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        ApplicationLoader.postInitApplication();
        NotificationsController.getInstance(intent.getIntExtra("currentAccount", UserConfig.selectedAccount)).forceShowPopupForReply();
    }
}

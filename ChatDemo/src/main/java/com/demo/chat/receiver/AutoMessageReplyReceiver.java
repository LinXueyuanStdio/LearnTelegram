package com.demo.chat.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.demo.chat.ApplicationLoader;
import com.demo.chat.controller.MessagesController;
import com.demo.chat.controller.NotificationsController;
import com.demo.chat.controller.SendMessagesHelper;

import androidx.core.app.RemoteInput;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/30
 * @description null
 * @usage null
 */
public class AutoMessageReplyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ApplicationLoader.postInitApplication();
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput == null) {
            return;
        }
        CharSequence text = remoteInput.getCharSequence(NotificationsController.EXTRA_VOICE_REPLY);
        if (text == null || text.length() == 0) {
            return;
        }
        long dialog_id = intent.getLongExtra("dialog_id", 0);
        int max_id = intent.getIntExtra("max_id", 0);
        int currentAccount = intent.getIntExtra("currentAccount", 0);
        if (dialog_id == 0 || max_id == 0) {
            return;
        }
        SendMessagesHelper.getInstance(currentAccount).sendMessage(text.toString(), dialog_id, null, null, true, null, null, null, true, 0);
        MessagesController.getInstance(currentAccount).markDialogAsRead(dialog_id, max_id, max_id, 0, false, 0, true, 0);
    }
}

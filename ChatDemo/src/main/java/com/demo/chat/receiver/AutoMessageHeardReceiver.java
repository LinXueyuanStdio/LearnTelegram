package com.demo.chat.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.demo.chat.ApplicationLoader;
import com.demo.chat.controller.AccountInstance;
import com.demo.chat.controller.MessagesController;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.Utilities;
import com.demo.chat.model.Chat;
import com.demo.chat.model.User;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/29
 * @description null
 * @usage null
 */
public class AutoMessageHeardReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ApplicationLoader.postInitApplication();
        long dialog_id = intent.getLongExtra("dialog_id", 0);
        int max_id = intent.getIntExtra("max_id", 0);
        int currentAccount = intent.getIntExtra("currentAccount", 0);
        if (dialog_id == 0 || max_id == 0) {
            return;
        }
        int lowerId = (int) dialog_id;
        int highId = (int) (dialog_id >> 32);
        AccountInstance accountInstance = AccountInstance.getInstance(currentAccount);
        if (lowerId > 0) {
            User user = accountInstance.getMessagesController().getUser(lowerId);
            if (user == null) {
                Utilities.globalQueue.postRunnable(() -> {
                    User user1 = accountInstance.getMessagesStorage().getUserSync(lowerId);
                    AndroidUtilities.runOnUIThread(() -> {
                        accountInstance.getMessagesController().putUser(user1, true);
                        MessagesController.getInstance(currentAccount).markDialogAsRead(dialog_id, max_id, max_id, 0, false, 0, true, 0);
                    });
                });
                return;
            }
        } else if (lowerId < 0) {
            Chat chat = accountInstance.getMessagesController().getChat(-lowerId);
            if (chat == null) {
                Utilities.globalQueue.postRunnable(() -> {
                    Chat chat1 = accountInstance.getMessagesStorage().getChatSync(-lowerId);
                    AndroidUtilities.runOnUIThread(() -> {
                        accountInstance.getMessagesController().putChat(chat1, true);
                        MessagesController.getInstance(currentAccount).markDialogAsRead(dialog_id, max_id, max_id, 0, false, 0, true, 0);
                    });
                });
                return;
            }
        }
        MessagesController.getInstance(currentAccount).markDialogAsRead(dialog_id, max_id, max_id, 0, false, 0, true, 0);
    }
}

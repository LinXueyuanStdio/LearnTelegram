package com.demo.chat.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.demo.chat.R;
import com.demo.chat.controller.LocaleController;
import com.demo.chat.messager.AndroidUtilities;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class CustomTabsCopyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String url = intent.getDataString();
        if (url != null) {
            AndroidUtilities.addToClipboard(url);
            Toast.makeText(context, LocaleController.getString("LinkCopied", R.string.LinkCopied), Toast.LENGTH_SHORT).show();
        }
    }
}

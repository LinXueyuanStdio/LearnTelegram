package com.demo.chat.receiver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.demo.chat.ui.LaunchActivity;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/30
 * @description null
 * @usage null
 */
public class OpenChatReceiver extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null) {
            finish();
        }
        if (intent.getAction() == null || !intent.getAction().startsWith("com.tmessages.openchat")) {
            finish();
            return;
        }
        try {
            int chatId = intent.getIntExtra("chatId", 0);
            int userId = intent.getIntExtra("userId", 0);
            int encId = intent.getIntExtra("encId", 0);
            if (chatId == 0 && userId == 0 && encId == 0) {
                return;
            }
        } catch (Throwable e) {
            return;
        }
        Intent intent2 = new Intent(this, LaunchActivity.class);
        intent2.setAction(intent.getAction());
        intent2.putExtras(intent);
        startActivity(intent2);
        finish();

        /*

         */
    }
}

package com.demo.chat.service;

import android.app.IntentService;
import android.content.Intent;

import com.demo.chat.ui.LaunchActivity;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class BringAppForegroundService extends IntentService {

    public BringAppForegroundService() {
        super("BringAppForegroundService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Intent intent2 = new Intent(this, LaunchActivity.class);
        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent2.setAction(Intent.ACTION_MAIN);
        startActivity(intent2);
    }
}

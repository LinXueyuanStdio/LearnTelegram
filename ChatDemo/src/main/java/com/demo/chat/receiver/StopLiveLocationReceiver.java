package com.demo.chat.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.demo.chat.controller.LocationController;
import com.demo.chat.controller.UserConfig;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class StopLiveLocationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            LocationController.getInstance(a).removeAllLocationSharings();
        }
    }
}

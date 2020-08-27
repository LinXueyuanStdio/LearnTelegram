package com.demo.chat.controller;

import android.content.SharedPreferences;

import com.demo.chat.messager.NotificationCenter;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 * 账户数据管理
 */
public class AccountInstance {

    private int currentAccount;
    private static volatile AccountInstance[] Instance = new AccountInstance[UserConfig.MAX_ACCOUNT_COUNT];

    public static AccountInstance getInstance(int num) {
        AccountInstance localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (AccountInstance.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new AccountInstance(num);
                }
            }
        }
        return localInstance;
    }

    public AccountInstance(int instance) {
        currentAccount = instance;
    }

    //用户设置
    public UserConfig getUserConfig() {
        return UserConfig.getInstance(currentAccount);
    }

    //消息管理
    public MessagesController getMessagesController() {
        return MessagesController.getInstance(currentAccount);
    }

    //消息存储
    public MessagesStorage getMessagesStorage() {
        return MessagesStorage.getInstance(currentAccount);
    }

    //联系人
    public ContactsController getContactsController() {
        return ContactsController.getInstance(currentAccount);
    }

    //媒体数据
    public MediaDataController getMediaDataController() {
        return MediaDataController.getInstance(currentAccount);
    }

    //连接管理
    public ConnectionsManager getConnectionsManager() {
        return ConnectionsManager.getInstance(currentAccount);
    }

    //通知
    public NotificationsController getNotificationsController() {
        return NotificationsController.getInstance(currentAccount);
    }

    //通知中心
    public NotificationCenter getNotificationCenter() {
        return NotificationCenter.getInstance(currentAccount);
    }

    //位置
    public LocationController getLocationController() {
        return LocationController.getInstance(currentAccount);
    }

    //下载
    public DownloadController getDownloadController() {
        return DownloadController.getInstance(currentAccount);
    }

    //发送消息
    public SendMessagesHelper getSendMessagesHelper() {
        return SendMessagesHelper.getInstance(currentAccount);
    }
//
//    //私密聊天
//    public SecretChatHelper getSecretChatHelper() {
//        return SecretChatHelper.getInstance(currentAccount);
//    }
//
//    //状态
//    public StatsController getStatsController() {
//        return StatsController.getInstance(currentAccount);
//    }

    //文件加载器
    public FileLoader getFileLoader() {
        return FileLoader.getInstance(currentAccount);
    }
//
//    //文件转发
//    public FileRefController getFileRefController() {
//        return FileRefController.getInstance(currentAccount);
//    }

    //通知设置
    public SharedPreferences getNotificationsSettings() {
        return MessagesController.getNotificationsSettings(currentAccount);
    }
}

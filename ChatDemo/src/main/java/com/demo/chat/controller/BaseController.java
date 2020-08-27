package com.demo.chat.controller;

import com.demo.chat.messager.NotificationCenter;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class BaseController {

    protected int currentAccount;
    private AccountInstance parentAccountInstance;

    //破坏了设计原则，因为XXXController是BaseController的子类
    public BaseController(int num) {
        parentAccountInstance = AccountInstance.getInstance(num);
        currentAccount = num;
    }

    //账户管理，一堆关于账户的Controller的集合
    protected final AccountInstance getAccountInstance() {
        return parentAccountInstance;
    }
    //用户设置
    protected final UserConfig getUserConfig() {
        return parentAccountInstance.getUserConfig();
    }
    //消息管理
    protected final MessagesController getMessagesController() {
        return parentAccountInstance.getMessagesController();
    }
    //联系人
    protected final ContactsController getContactsController() {
        return parentAccountInstance.getContactsController();
    }
    //媒体数据
    protected final MediaDataController getMediaDataController() {
        return parentAccountInstance.getMediaDataController();
    }
    //连接管理
    protected final ConnectionsManager getConnectionsManager() {
        return parentAccountInstance.getConnectionsManager();
    }
//    //位置
//    protected final LocationController getLocationController() {
//        return parentAccountInstance.getLocationController();
//    }
    //通知
    protected final NotificationsController getNotificationsController() {
        return parentAccountInstance.getNotificationsController();
    }
    //通知中心
    protected final NotificationCenter getNotificationCenter() {
        return parentAccountInstance.getNotificationCenter();
    }
    //消息存储
    protected final MessagesStorage getMessagesStorage() {
        return parentAccountInstance.getMessagesStorage();
    }
    //下载
    protected final DownloadController getDownloadController() {
        return parentAccountInstance.getDownloadController();
    }
    //发送消息
    protected final SendMessagesHelper getSendMessagesHelper() {
        return parentAccountInstance.getSendMessagesHelper();
    }
//    //私密聊天
//    protected final SecretChatHelper getSecretChatHelper() {
//        return parentAccountInstance.getSecretChatHelper();
//    }
//    //状态
//    protected final StatsController getStatsController() {
//        return parentAccountInstance.getStatsController();
//    }
    //文件加载器
    protected final FileLoader getFileLoader() {
        return parentAccountInstance.getFileLoader();
    }
//    //文件转发
//    protected final FileRefController getFileRefController() {
//        return parentAccountInstance.getFileRefController();
//    }
}

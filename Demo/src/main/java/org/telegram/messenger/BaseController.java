package org.telegram.messenger;

import org.telegram.tgnet.ConnectionsManager;

public class BaseController {

    protected int currentAccount;
    private AccountInstance parentAccountInstance;

    //破坏了设计原则，因为XXXController是BaseController的子类
    public BaseController(int num) {
        parentAccountInstance = AccountInstance.getInstance(num);
        currentAccount = num;
    }

    /**
     * @return 账户管理，一堆关于账户的Controller的集合
     */
    protected final AccountInstance getAccountInstance() {
        return parentAccountInstance;
    }
    /**
     * @return
     */
    protected final MessagesController getMessagesController() {
        return parentAccountInstance.getMessagesController();
    }
    /**
     * @return
     */
    protected final ContactsController getContactsController() {
        return parentAccountInstance.getContactsController();
    }
    /**
     * @return
     */
    protected final MediaDataController getMediaDataController() {
        return parentAccountInstance.getMediaDataController();
    }
    /**
     * @return
     */
    protected final ConnectionsManager getConnectionsManager() {
        return parentAccountInstance.getConnectionsManager();
    }
    /**
     * @return
     */
    protected final LocationController getLocationController() {
        return parentAccountInstance.getLocationController();
    }
    /**
     * @return
     */
    protected final NotificationsController getNotificationsController() {
        return parentAccountInstance.getNotificationsController();
    }
    /**
     * @return
     */
    protected final NotificationCenter getNotificationCenter() {
        return parentAccountInstance.getNotificationCenter();
    }
    /**
     * @return
     */
    protected final UserConfig getUserConfig() {
        return parentAccountInstance.getUserConfig();
    }
    /**
     * @return
     */
    protected final MessagesStorage getMessagesStorage() {
        return parentAccountInstance.getMessagesStorage();
    }
    /**
     * @return
     */
    protected final DownloadController getDownloadController() {
        return parentAccountInstance.getDownloadController();
    }
    /**
     * @return
     */
    protected final SendMessagesHelper getSendMessagesHelper() {
        return parentAccountInstance.getSendMessagesHelper();
    }
    /**
     * @return
     */
    protected final SecretChatHelper getSecretChatHelper() {
        return parentAccountInstance.getSecretChatHelper();
    }
    /**
     * @return
     */
    protected final StatsController getStatsController() {
        return parentAccountInstance.getStatsController();
    }
    /**
     * @return
     */
    protected final FileLoader getFileLoader() {
        return parentAccountInstance.getFileLoader();
    }
    /**
     * @return
     */
    protected final FileRefController getFileRefController() {
        return parentAccountInstance.getFileRefController();
    }
}

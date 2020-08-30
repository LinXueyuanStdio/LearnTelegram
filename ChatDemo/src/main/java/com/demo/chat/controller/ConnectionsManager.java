package com.demo.chat.controller;

import android.content.pm.PackageInfo;
import android.os.Build;
import android.text.TextUtils;

import com.demo.chat.ApplicationLoader;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.BuildVars;
import com.demo.chat.messager.EmuDetector;
import com.demo.chat.messager.NotificationCenter;
import com.demo.chat.messager.SharedConfig;
import com.demo.chat.service.KeepAliveJob;
import com.demo.chat.tgnet.QuickAckDelegate;
import com.demo.chat.tgnet.RequestDelegateInternal;
import com.demo.chat.tgnet.RequestTimeDelegate;
import com.demo.chat.tgnet.WriteToSocketDelegate;

import java.io.File;
import java.util.TimeZone;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class ConnectionsManager extends BaseController {
    public final static int FileTypePhoto = 0x01000000;
    public final static int FileTypeVideo = 0x02000000;
    public final static int FileTypeAudio = 0x03000000;
    public final static int FileTypeFile = 0x04000000;

    private static int lastClassGuid = 1;

    public static int generateClassGuid() {
        return lastClassGuid++;
    }

    public static void cancelRequestsForGuid(int classGuid){
        //todo 之后再研究telegram是怎么实现的
    }


    private static volatile ConnectionsManager[] Instance = new ConnectionsManager[UserConfig.MAX_ACCOUNT_COUNT];
    public static ConnectionsManager getInstance(int num) {
        ConnectionsManager localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (ConnectionsManager.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new ConnectionsManager(num);
                }
            }
        }
        return localInstance;
    }

    public ConnectionsManager(int instance) {
        super(instance);
        String deviceModel;
        String systemLangCode;
        String langCode;
        String appVersion;
        String systemVersion;
        File config = ApplicationLoader.getFilesDirFixed();
        if (instance != 0) {
            config = new File(config, "account" + instance);
            config.mkdirs();
        }
        String configPath = config.toString();
        boolean enablePushConnection = isPushConnectionEnabled();
        try {
            systemLangCode = LocaleController.getSystemLocaleStringIso639().toLowerCase();
            langCode = LocaleController.getLocaleStringIso639().toLowerCase();
            deviceModel = Build.MANUFACTURER + Build.MODEL;
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            appVersion = pInfo.versionName + " (" + pInfo.versionCode + ")";
            if (BuildVars.DEBUG_PRIVATE_VERSION) {
                appVersion += " pbeta";
            } else if (BuildVars.DEBUG_VERSION) {
                appVersion += " beta";
            }
            systemVersion = "SDK " + Build.VERSION.SDK_INT;
        } catch (Exception e) {
            systemLangCode = "en";
            langCode = "";
            deviceModel = "Android unknown";
            appVersion = "App version unknown";
            systemVersion = "SDK " + Build.VERSION.SDK_INT;
        }
        if (systemLangCode.trim().length() == 0) {
            systemLangCode = "en";
        }
        if (deviceModel.trim().length() == 0) {
            deviceModel = "Android unknown";
        }
        if (appVersion.trim().length() == 0) {
            appVersion = "App version unknown";
        }
        if (systemVersion.trim().length() == 0) {
            systemVersion = "SDK Unknown";
        }
        getUserConfig().loadConfig();
        String pushString = SharedConfig.pushString;
        if (TextUtils.isEmpty(pushString) && !TextUtils.isEmpty(SharedConfig.pushStringStatus)) {
            pushString = SharedConfig.pushStringStatus;
        }
        String fingerprint = AndroidUtilities.getCertificateSHA256Fingerprint();

        int timezoneOffset = (TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings()) / 1000;
    }


    public boolean isPushConnectionEnabled() {
        return false;
    }


    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    public int getCurrentTime() {
        return (int) (getCurrentTimeMillis() / 1000);
    }


    public static void onUnparsedMessageReceived(long address, final int currentAccount) {

    }

    public static void onUpdate(final int currentAccount) {
    }

    public static void onSessionCreated(final int currentAccount) {
    }

    public static void onConnectionStateChanged(final int state, final int currentAccount) {
        AndroidUtilities.runOnUIThread(() -> {
        });
    }

    public static void onLogout(final int currentAccount) {
        AndroidUtilities.runOnUIThread(() -> {
            AccountInstance accountInstance = AccountInstance.getInstance(currentAccount);
            if (accountInstance.getUserConfig().getClientUserId() != 0) {
                accountInstance.getUserConfig().clearConfig();
                accountInstance.getMessagesController().performLogout(0);
            }
        });
    }

    public static int getInitFlags() {
        int flags = 0;
        EmuDetector detector = EmuDetector.with(ApplicationLoader.applicationContext);
        if (detector.detect()) {
            flags |= 1024;
        }
        try {
            String installer = ApplicationLoader.applicationContext.getPackageManager().getInstallerPackageName(ApplicationLoader.applicationContext.getPackageName());
            if ("com.android.vending".equals(installer)) {
                flags |= 2048;
            }
        } catch (Throwable ignore) {

        }
        return flags;
    }

    public static void onBytesSent(int amount, int networkType, final int currentAccount) {

    }

    public static void onRequestNewServerIpAndPort(final int second, final int currentAccount) {
    }

    public static void onProxyError() {
        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needShowAlert, 3));
    }

    public static void getHostByName(String hostName, long address) {
    }

    public static void onBytesReceived(int amount, int networkType, final int currentAccount) {
    }

    public static void onUpdateConfig(long address, final int currentAccount) {
    }
    public static void onInternalPushReceived(final int currentAccount) {
        KeepAliveJob.startJob();
    }
    public static void setProxySettings(boolean enabled, String address, int port, String username, String password, String secret) {
        if (address == null) {
            address = "";
        }
        if (username == null) {
            username = "";
        }
        if (password == null) {
            password = "";
        }
        if (secret == null) {
            secret = "";
        }
    }
    public static native void native_switchBackend(int currentAccount);
    public static native int native_isTestBackend(int currentAccount);
    public static native void native_pauseNetwork(int currentAccount);
    public static native void native_setUseIpv6(int currentAccount, boolean value);
    public static native void native_updateDcSettings(int currentAccount);
    public static native void native_setNetworkAvailable(int currentAccount, boolean value, int networkType, boolean slow);
    public static native void native_resumeNetwork(int currentAccount, boolean partial);
    public static native long native_getCurrentTimeMillis(int currentAccount);
    public static native int native_getCurrentTime(int currentAccount);
    public static native int native_getTimeDifference(int currentAccount);
    public static native void native_sendRequest(int currentAccount, long object, RequestDelegateInternal onComplete, QuickAckDelegate onQuickAck, WriteToSocketDelegate onWriteToSocket, int flags, int datacenterId, int connetionType, boolean immediate, int requestToken);
    public static native void native_cancelRequest(int currentAccount, int token, boolean notifyServer);
    public static native void native_cleanUp(int currentAccount, boolean resetKeys);
    public static native void native_cancelRequestsForGuid(int currentAccount, int guid);
    public static native void native_bindRequestToGuid(int currentAccount, int requestToken, int guid);
    public static native void native_applyDatacenterAddress(int currentAccount, int datacenterId, String ipAddress, int port);
    public static native int native_getConnectionState(int currentAccount);
    public static native void native_setUserId(int currentAccount, int id);
    public static native void native_init(int currentAccount, int version, int layer, int apiId, String deviceModel, String systemVersion, String appVersion, String langCode, String systemLangCode, String configPath, String logPath, String regId, String cFingerprint, int timezoneOffset, int userId, boolean enablePushConnection, boolean hasNetwork, int networkType);
    public static native void native_setProxySettings(int currentAccount, String address, int port, String username, String password, String secret);
    public static native void native_setLangCode(int currentAccount, String langCode);
    public static native void native_setRegId(int currentAccount, String regId);
    public static native void native_setSystemLangCode(int currentAccount, String langCode);
    public static native void native_seSystemLangCode(int currentAccount, String langCode);
    public static native void native_setJava(boolean useJavaByteBuffers);
    public static native void native_setPushConnectionEnabled(int currentAccount, boolean value);
    public static native void native_applyDnsConfig(int currentAccount, long address, String phone, int date);
    public static native long native_checkProxy(int currentAccount, String address, int port, String username, String password, String secret, RequestTimeDelegate requestTimeDelegate);
    public static native void native_onHostNameResolved(String host, long address, String ip);
}

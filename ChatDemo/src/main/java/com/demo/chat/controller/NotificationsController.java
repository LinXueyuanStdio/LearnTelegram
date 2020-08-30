package com.demo.chat.controller;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.demo.chat.ApplicationLoader;
import com.demo.chat.R;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.BuildConfig;
import com.demo.chat.messager.BuildVars;
import com.demo.chat.messager.DispatchQueue;
import com.demo.chat.messager.FileLog;
import com.demo.chat.messager.ImageLoader;
import com.demo.chat.messager.NotificationCenter;
import com.demo.chat.messager.SharedConfig;
import com.demo.chat.messager.Utilities;
import com.demo.chat.messager.support.SparseLongArray;
import com.demo.chat.model.Chat;
import com.demo.chat.model.Message;
import com.demo.chat.model.User;
import com.demo.chat.model.action.ChatObject;
import com.demo.chat.model.action.MessageObject;
import com.demo.chat.model.action.UserObject;
import com.demo.chat.model.bot.KeyboardButton;
import com.demo.chat.model.reply.ReplyMarkup;
import com.demo.chat.model.small.FileLocation;
import com.demo.chat.model.small.MessageMedia;
import com.demo.chat.receiver.NotificationCallbackReceiver;
import com.demo.chat.receiver.NotificationDismissReceiver;
import com.demo.chat.receiver.PopupReplyReceiver;
import com.demo.chat.service.NotificationRepeat;
import com.demo.chat.ui.LaunchActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.IconCompat;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/26
 * @description null
 * @usage null
 */
public class NotificationsController extends BaseController {

    public static final String EXTRA_VOICE_REPLY = "extra_voice_reply";
    public static String OTHER_NOTIFICATIONS_CHANNEL = null;

    private static DispatchQueue notificationsQueue = new DispatchQueue("notificationsQueue");
    private ArrayList<MessageObject> pushMessages = new ArrayList<>();
    private ArrayList<MessageObject> delayedPushMessages = new ArrayList<>();
    private LongSparseArray<MessageObject> pushMessagesDict = new LongSparseArray<>();
    private LongSparseArray<MessageObject> fcmRandomMessagesDict = new LongSparseArray<>();
    private LongSparseArray<Point> smartNotificationsDialogs = new LongSparseArray<>();
    private static NotificationManagerCompat notificationManager = null;
    private static NotificationManager systemNotificationManager = null;
    private LongSparseArray<Integer> pushDialogs = new LongSparseArray<>();
    private LongSparseArray<Integer> wearNotificationsIds = new LongSparseArray<>();
    private LongSparseArray<Integer> lastWearNotifiedMessageId = new LongSparseArray<>();
    private LongSparseArray<Integer> pushDialogsOverrideMention = new LongSparseArray<>();
    public ArrayList<MessageObject> popupMessages = new ArrayList<>();
    public ArrayList<MessageObject> popupReplyMessages = new ArrayList<>();
    private long opened_dialog_id = 0;
    private int lastButtonId = 5000;
    private int total_unread_count = 0;
    private int personal_count = 0;
    private boolean notifyCheck = false;
    private int lastOnlineFromOtherDevice = 0;
    private boolean inChatSoundEnabled;
    private int lastBadgeCount = -1;
    private String launcherClassName;

    public static long globalSecretChatId = -(1L << 32);

    public boolean showBadgeNumber;
    public boolean showBadgeMuted;
    public boolean showBadgeMessages;

    private Runnable notificationDelayRunnable;
    private PowerManager.WakeLock notificationDelayWakelock;

    private long lastSoundPlay;
    private long lastSoundOutPlay;
    private SoundPool soundPool;
    private int soundIn;
    private int soundOut;
    private int soundRecord;
    private boolean soundInLoaded;
    private boolean soundOutLoaded;
    private boolean soundRecordLoaded;
    protected static AudioManager audioManager;
    private AlarmManager alarmManager;

    private int notificationId;
    private String notificationGroup;

    static {
        if (Build.VERSION.SDK_INT >= 26 && ApplicationLoader.applicationContext != null) {
            notificationManager = NotificationManagerCompat.from(ApplicationLoader.applicationContext);
            systemNotificationManager = (NotificationManager) ApplicationLoader.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
            checkOtherNotificationsChannel();
        }
        audioManager = (AudioManager) ApplicationLoader.applicationContext.getSystemService(Context.AUDIO_SERVICE);
    }

    private static volatile NotificationsController[] Instance = new NotificationsController[UserConfig.MAX_ACCOUNT_COUNT];

    public static NotificationsController getInstance(int num) {
        NotificationsController localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (NotificationsController.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new NotificationsController(num);
                }
            }
        }
        return localInstance;
    }

    public NotificationsController(int instance) {
        super(instance);

        notificationId = currentAccount + 1;
        notificationGroup = "messages" + (currentAccount == 0 ? "" : currentAccount);
        SharedPreferences preferences = getAccountInstance().getNotificationsSettings();
        inChatSoundEnabled = preferences.getBoolean("EnableInChatSound", true);
        showBadgeNumber = preferences.getBoolean("badgeNumber", true);
        showBadgeMuted = preferences.getBoolean("badgeNumberMuted", false);
        showBadgeMessages = preferences.getBoolean("badgeNumberMessages", true);

        notificationManager = NotificationManagerCompat.from(ApplicationLoader.applicationContext);
        systemNotificationManager = (NotificationManager) ApplicationLoader.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);

        try {
            audioManager = (AudioManager) ApplicationLoader.applicationContext.getSystemService(Context.AUDIO_SERVICE);
        } catch (Exception e) {
            FileLog.e(e);
        }
        try {
            alarmManager = (AlarmManager) ApplicationLoader.applicationContext.getSystemService(Context.ALARM_SERVICE);
        } catch (Exception e) {
            FileLog.e(e);
        }

        try {
            PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
            notificationDelayWakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "telegram:notification_delay_lock");
            notificationDelayWakelock.setReferenceCounted(false);
        } catch (Exception e) {
            FileLog.e(e);
        }

        notificationDelayRunnable = () -> {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("delay reached");
            }
            if (!delayedPushMessages.isEmpty()) {
                showOrUpdateNotification(true);
                delayedPushMessages.clear();
            }
            try {
                if (notificationDelayWakelock.isHeld()) {
                    notificationDelayWakelock.release();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        };
    }

    public static void checkOtherNotificationsChannel() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        SharedPreferences preferences = null;
        if (OTHER_NOTIFICATIONS_CHANNEL == null) {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
            OTHER_NOTIFICATIONS_CHANNEL = preferences.getString("OtherKey", "Other3");
        }
        NotificationChannel notificationChannel = systemNotificationManager.getNotificationChannel(OTHER_NOTIFICATIONS_CHANNEL);
        if (notificationChannel != null && notificationChannel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
            systemNotificationManager.deleteNotificationChannel(OTHER_NOTIFICATIONS_CHANNEL);
            OTHER_NOTIFICATIONS_CHANNEL = null;
            notificationChannel = null;
        }
        if (OTHER_NOTIFICATIONS_CHANNEL == null) {
            if (preferences == null) {
                preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
            }
            OTHER_NOTIFICATIONS_CHANNEL = "Other" + Utilities.random.nextLong();
            preferences.edit().putString("OtherKey", OTHER_NOTIFICATIONS_CHANNEL).commit();
        }
        if (notificationChannel == null) {
            notificationChannel = new NotificationChannel(OTHER_NOTIFICATIONS_CHANNEL, "Other", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setSound(null, null);
            systemNotificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public void cleanup() {
        popupMessages.clear();
        popupReplyMessages.clear();
        notificationsQueue.postRunnable(() -> {
            opened_dialog_id = 0;
            total_unread_count = 0;
            personal_count = 0;
            pushMessages.clear();
            pushMessagesDict.clear();
            fcmRandomMessagesDict.clear();
            pushDialogs.clear();
            wearNotificationsIds.clear();
            lastWearNotifiedMessageId.clear();
            delayedPushMessages.clear();
            notifyCheck = false;
            lastBadgeCount = 0;
            try {
                if (notificationDelayWakelock.isHeld()) {
                    notificationDelayWakelock.release();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
            dismissNotification();
            setBadge(getTotalAllUnreadCount());
            SharedPreferences preferences = getAccountInstance().getNotificationsSettings();
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();

            if (Build.VERSION.SDK_INT >= 26) {
                try {
                    String keyStart = currentAccount + "channel";
                    List<NotificationChannel> list = systemNotificationManager.getNotificationChannels();
                    int count = list.size();
                    for (int a = 0; a < count; a++) {
                        NotificationChannel channel = list.get(a);
                        String id = channel.getId();
                        if (id.startsWith(keyStart)) {
                            systemNotificationManager.deleteNotificationChannel(id);
                        }
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
        });
    }

    public void setInChatSoundEnabled(boolean value) {
        inChatSoundEnabled = value;
    }

    public void setOpenedDialogId(final long dialog_id) {
        notificationsQueue.postRunnable(() -> opened_dialog_id = dialog_id);
    }

    public void setLastOnlineFromOtherDevice(final int time) {
        notificationsQueue.postRunnable(() -> {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("set last online from other device = " + time);
            }
            lastOnlineFromOtherDevice = time;
        });
    }

    public void removeNotificationsForDialog(long did) {
        processReadMessages(null, did, 0, Integer.MAX_VALUE, false);
        LongSparseArray<Integer> dialogsToUpdate = new LongSparseArray<>();
        dialogsToUpdate.put(did, 0);
        processDialogsUpdateRead(dialogsToUpdate);
    }

    public boolean hasMessagesToReply() {
        for (int a = 0; a < pushMessages.size(); a++) {
            MessageObject messageObject = pushMessages.get(a);
            long dialog_id = messageObject.getDialogId();
            if (messageObject.messageOwner.mentioned && messageObject.messageOwner.action.isPinMessage() ||
                    (int) dialog_id == 0 || messageObject.messageOwner.to_id != 0 && !messageObject.isMegagroup()) {
                continue;
            }
            return true;
        }
        return false;
    }

    public void forceShowPopupForReply() {
        notificationsQueue.postRunnable(() -> {
            final ArrayList<MessageObject> popupArray = new ArrayList<>();
            for (int a = 0; a < pushMessages.size(); a++) {
                MessageObject messageObject = pushMessages.get(a);
                long dialog_id = messageObject.getDialogId();
                if (messageObject.messageOwner.mentioned && messageObject.messageOwner.action.isPinMessage() ||
                        (int) dialog_id == 0 || messageObject.messageOwner.to_id != 0 && !messageObject.isMegagroup()) {
                    continue;
                }
                popupArray.add(0, messageObject);
            }
            if (!popupArray.isEmpty() && !AndroidUtilities.needShowPasscode() && !SharedConfig.isWaitingForPasscodeEnter) {
                AndroidUtilities.runOnUIThread(() -> {
                    popupReplyMessages = popupArray;
//                    Intent popupIntent = new Intent(ApplicationLoader.applicationContext, PopupNotificationActivity.class);
//                    popupIntent.putExtra("force", true);
//                    popupIntent.putExtra("currentAccount", currentAccount);
//                    popupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_FROM_BACKGROUND);
//                    ApplicationLoader.applicationContext.startActivity(popupIntent);
//                    Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
//                    ApplicationLoader.applicationContext.sendBroadcast(it);
                });
            }
        });
    }

    public void removeDeletedMessagesFromNotifications(final SparseArray<ArrayList<Integer>> deletedMessages) {
        final ArrayList<MessageObject> popupArrayRemove = new ArrayList<>(0);
        notificationsQueue.postRunnable(() -> {
            int old_unread_count = total_unread_count;
            SharedPreferences preferences = getAccountInstance().getNotificationsSettings();
            for (int a = 0; a < deletedMessages.size(); a++) {
                int key = deletedMessages.keyAt(a);
                ArrayList<Integer> mids = deletedMessages.get(key);
                for (int b = 0; b < mids.size(); b++) {
                    long mid = mids.get(b);
                    if (key != 0) {
                        mid |= ((long) key) << 32;
                    }
                    MessageObject messageObject = pushMessagesDict.get(mid);
                    if (messageObject != null) {
                        long dialogId = messageObject.getDialogId();
                        Integer currentCount = pushDialogs.get(dialogId);
                        if (currentCount == null) {
                            currentCount = 0;
                        }
                        Integer newCount = currentCount - 1;
                        if (newCount <= 0) {
                            newCount = 0;
                            smartNotificationsDialogs.remove(dialogId);
                        }
                        if (!newCount.equals(currentCount)) {
                            total_unread_count -= currentCount;
                            total_unread_count += newCount;
                            pushDialogs.put(dialogId, newCount);
                        }
                        if (newCount == 0) {
                            pushDialogs.remove(dialogId);
                            pushDialogsOverrideMention.remove(dialogId);
                        }

                        pushMessagesDict.remove(mid);
                        delayedPushMessages.remove(messageObject);
                        pushMessages.remove(messageObject);
                        if (isPersonalMessage(messageObject)) {
                            personal_count--;
                        }
                        popupArrayRemove.add(messageObject);
                    }
                }
            }
            if (!popupArrayRemove.isEmpty()) {
                AndroidUtilities.runOnUIThread(() -> {
                    for (int a = 0, size = popupArrayRemove.size(); a < size; a++) {
                        popupMessages.remove(popupArrayRemove.get(a));
                    }
                });
            }
            if (old_unread_count != total_unread_count) {
                if (!notifyCheck) {
                    delayedPushMessages.clear();
                    showOrUpdateNotification(notifyCheck);
                } else {
                    scheduleNotificationDelay(lastOnlineFromOtherDevice > getConnectionsManager().getCurrentTime());
                }
                final int pushDialogsCount = pushDialogs.size();
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.notificationsCountUpdated, currentAccount);
                    getNotificationCenter().postNotificationName(NotificationCenter.dialogsUnreadCounterChanged, pushDialogsCount);
                });
            }
            notifyCheck = false;
            if (showBadgeNumber) {
                setBadge(getTotalAllUnreadCount());
            }
        });
    }

    public void removeDeletedHisoryFromNotifications(final SparseIntArray deletedMessages) {
        final ArrayList<MessageObject> popupArrayRemove = new ArrayList<>(0);
        notificationsQueue.postRunnable(() -> {
            int old_unread_count = total_unread_count;
            SharedPreferences preferences = getAccountInstance().getNotificationsSettings();

            for (int a = 0; a < deletedMessages.size(); a++) {
                int key = deletedMessages.keyAt(a);
                long dialog_id = -key;
                int id = deletedMessages.get(key);
                Integer currentCount = pushDialogs.get(dialog_id);
                if (currentCount == null) {
                    currentCount = 0;
                }
                Integer newCount = currentCount;

                for (int c = 0; c < pushMessages.size(); c++) {
                    MessageObject messageObject = pushMessages.get(c);
                    if (messageObject.getDialogId() == dialog_id && messageObject.getId() <= id) {
                        pushMessagesDict.remove(messageObject.getIdWithChannel());
                        delayedPushMessages.remove(messageObject);
                        pushMessages.remove(messageObject);
                        c--;
                        if (isPersonalMessage(messageObject)) {
                            personal_count--;
                        }
                        popupArrayRemove.add(messageObject);
                        newCount--;
                    }
                }

                if (newCount <= 0) {
                    newCount = 0;
                    smartNotificationsDialogs.remove(dialog_id);
                }
                if (!newCount.equals(currentCount)) {
                    total_unread_count -= currentCount;
                    total_unread_count += newCount;
                    pushDialogs.put(dialog_id, newCount);
                }
                if (newCount == 0) {
                    pushDialogs.remove(dialog_id);
                    pushDialogsOverrideMention.remove(dialog_id);
                }
            }
            if (popupArrayRemove.isEmpty()) {
                AndroidUtilities.runOnUIThread(() -> {
                    for (int a = 0, size = popupArrayRemove.size(); a < size; a++) {
                        popupMessages.remove(popupArrayRemove.get(a));
                    }
                });
            }
            if (old_unread_count != total_unread_count) {
                if (!notifyCheck) {
                    delayedPushMessages.clear();
                    showOrUpdateNotification(notifyCheck);
                } else {
                    scheduleNotificationDelay(lastOnlineFromOtherDevice > getConnectionsManager().getCurrentTime());
                }
                final int pushDialogsCount = pushDialogs.size();
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.notificationsCountUpdated, currentAccount);
                    getNotificationCenter().postNotificationName(NotificationCenter.dialogsUnreadCounterChanged, pushDialogsCount);
                });
            }
            notifyCheck = false;
            if (showBadgeNumber) {
                setBadge(getTotalAllUnreadCount());
            }
        });
    }

    public void processReadMessages(final SparseLongArray inbox, final long dialog_id, final int max_date, final int max_id, final boolean isPopup) {
        final ArrayList<MessageObject> popupArrayRemove = new ArrayList<>(0);
        notificationsQueue.postRunnable(() -> {
            if (inbox != null) {
                for (int b = 0; b < inbox.size(); b++) {
                    int key = inbox.keyAt(b);
                    long messageId = inbox.get(key);
                    for (int a = 0; a < pushMessages.size(); a++) {
                        MessageObject messageObject = pushMessages.get(a);
                        if (!messageObject.messageOwner.from_scheduled && messageObject.getDialogId() == key && messageObject.getId() <= (int) messageId) {
                            if (isPersonalMessage(messageObject)) {
                                personal_count--;
                            }
                            popupArrayRemove.add(messageObject);
                            long mid = messageObject.getId();
                            if (messageObject.messageOwner.to_id != 0) {
                                mid |= ((long) messageObject.messageOwner.to_id) << 32;
                            }
                            pushMessagesDict.remove(mid);
                            delayedPushMessages.remove(messageObject);
                            pushMessages.remove(a);
                            a--;
                        }
                    }
                }
            }
            if (dialog_id != 0 && (max_id != 0 || max_date != 0)) {
                for (int a = 0; a < pushMessages.size(); a++) {
                    MessageObject messageObject = pushMessages.get(a);
                    if (messageObject.getDialogId() == dialog_id) {
                        boolean remove = false;
                        if (max_date != 0) {
                            if (messageObject.messageOwner.date <= max_date) {
                                remove = true;
                            }
                        } else {
                            if (!isPopup) {
                                if (messageObject.getId() <= max_id || max_id < 0) {
                                    remove = true;
                                }
                            } else {
                                if (messageObject.getId() == max_id || max_id < 0) {
                                    remove = true;
                                }
                            }
                        }
                        if (remove) {
                            if (isPersonalMessage(messageObject)) {
                                personal_count--;
                            }
                            pushMessages.remove(a);
                            delayedPushMessages.remove(messageObject);
                            popupArrayRemove.add(messageObject);
                            long mid = messageObject.getId();
                            if (messageObject.messageOwner.to_id != 0) {
                                mid |= ((long) messageObject.messageOwner.to_id) << 32;
                            }
                            pushMessagesDict.remove(mid);
                            a--;
                        }
                    }
                }
            }
            if (!popupArrayRemove.isEmpty()) {
                AndroidUtilities.runOnUIThread(() -> {
                    for (int a = 0, size = popupArrayRemove.size(); a < size; a++) {
                        popupMessages.remove(popupArrayRemove.get(a));
                    }
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.pushMessagesUpdated);
                });
            }
        });
    }

    private int addToPopupMessages(final ArrayList<MessageObject> popupArrayAdd, MessageObject messageObject, int lower_id, long dialog_id, boolean isChannel, SharedPreferences preferences) {
        int popup = 0;
        if (lower_id != 0) {
            if (preferences.getBoolean("custom_" + dialog_id, false)) {
                popup = preferences.getInt("popup_" + dialog_id, 0);
            } else {
                popup = 0;
            }
            if (popup == 0) {
                if (isChannel) {
                    popup = preferences.getInt("popupChannel", 0);
                } else {
                    popup = preferences.getInt((int) dialog_id < 0 ? "popupGroup" : "popupAll", 0);
                }
            } else if (popup == 1) {
                popup = 3;
            } else if (popup == 2) {
                popup = 0;
            }
        }
        if (popup != 0 && messageObject.messageOwner.to_id != 0 && !messageObject.isMegagroup()) {
            popup = 0;
        }
        if (popup != 0) {
            popupArrayAdd.add(0, messageObject);
        }
        return popup;
    }

    public void processNewMessages(final ArrayList<MessageObject> messageObjects, final boolean isLast, final boolean isFcm, CountDownLatch countDownLatch) {
        if (messageObjects.isEmpty()) {
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
            return;
        }
        final ArrayList<MessageObject> popupArrayAdd = new ArrayList<>(0);
        notificationsQueue.postRunnable(() -> {
            boolean added = false;
            boolean edited = false;

            LongSparseArray<Boolean> settingsCache = new LongSparseArray<>();
            SharedPreferences preferences = getAccountInstance().getNotificationsSettings();
            boolean allowPinned = preferences.getBoolean("PinnedMessages", true);
            int popup = 0;
            boolean hasScheduled = false;

            for (int a = 0; a < messageObjects.size(); a++) {
                MessageObject messageObject = messageObjects.get(a);
                if (messageObject.messageOwner != null && messageObject.messageOwner.silent && (messageObject.messageOwner.action.isContactSignUp() || messageObject.messageOwner.action.isUserJoined())) {
                    continue;
                }
                long mid = messageObject.getId();
                long random_id = messageObject.isFcmMessage() ? messageObject.messageOwner.random_id : 0;
                long dialog_id = messageObject.getDialogId();
                int lower_id = (int) dialog_id;
                boolean isChannel;
                if (messageObject.isFcmMessage()) {
                    isChannel = messageObject.localChannel;
                } else if (lower_id < 0) {
                    Chat chat = getMessagesController().getChat(-lower_id);
                    isChannel = ChatObject.isChannel(chat) && !chat.megagroup;
                } else {
                    isChannel = false;
                }
                if (messageObject.messageOwner.to_id != 0) {
                    mid |= ((long) messageObject.messageOwner.to_id) << 32;
                }

                MessageObject oldMessageObject = pushMessagesDict.get(mid);
                if (oldMessageObject == null && messageObject.messageOwner.random_id != 0) {
                    oldMessageObject = fcmRandomMessagesDict.get(messageObject.messageOwner.random_id);
                    if (oldMessageObject != null) {
                        fcmRandomMessagesDict.remove(messageObject.messageOwner.random_id);
                    }
                }
                if (oldMessageObject != null) {
                    if (oldMessageObject.isFcmMessage()) {
                        pushMessagesDict.put(mid, messageObject);
                        int idxOld = pushMessages.indexOf(oldMessageObject);
                        if (idxOld >= 0) {
                            pushMessages.set(idxOld, messageObject);
                            popup = addToPopupMessages(popupArrayAdd, messageObject, lower_id, dialog_id, isChannel, preferences);
                        }
                        if (isFcm && (edited = messageObject.localEdit)) {
                            getMessagesStorage().putPushMessage(messageObject);
                        }
                    }
                    continue;
                }
                if (edited) {
                    continue;
                }
                if (isFcm) {
                    getMessagesStorage().putPushMessage(messageObject);
                }

                long original_dialog_id = dialog_id;
                if (dialog_id == opened_dialog_id && ApplicationLoader.isScreenOn) {
                    if (!isFcm) {
                        playInChatSound();
                    }
                    continue;
                }
                if (messageObject.messageOwner.mentioned) {
                    if (!allowPinned && messageObject.messageOwner.action.isPinMessage()) {
                        continue;
                    }
                    dialog_id = messageObject.messageOwner.from_id;
                }
                if (isPersonalMessage(messageObject)) {
                    personal_count++;
                }
                added = true;

                boolean isChat = lower_id < 0;
                int index = settingsCache.indexOfKey(dialog_id);
                boolean value;
                if (index >= 0) {
                    value = settingsCache.valueAt(index);
                } else {
                    int notifyOverride = getNotifyOverride(preferences, dialog_id);
                    if (notifyOverride == -1) {
                        value = isGlobalNotificationsEnabled(dialog_id, isChannel);
                        /*if (BuildVars.DEBUG_PRIVATE_VERSION && BuildVars.LOGS_ENABLED) {
                            FileLog.d("global notify settings for " + dialog_id + " = " + value);
                        }*/
                    } else {
                        value = notifyOverride != 2;
                    }

                    settingsCache.put(dialog_id, value);
                }

                if (value) {
                    if (!isFcm) {
                        popup = addToPopupMessages(popupArrayAdd, messageObject, lower_id, dialog_id, isChannel, preferences);
                    }
                    if (!hasScheduled) {
                        hasScheduled = messageObject.messageOwner.from_scheduled;
                    }
                    delayedPushMessages.add(messageObject);
                    pushMessages.add(0, messageObject);
                    if (mid != 0) {
                        pushMessagesDict.put(mid, messageObject);
                    } else if (random_id != 0) {
                        fcmRandomMessagesDict.put(random_id, messageObject);
                    }
                    if (original_dialog_id != dialog_id) {
                        Integer current = pushDialogsOverrideMention.get(original_dialog_id);
                        pushDialogsOverrideMention.put(original_dialog_id, current == null ? 1 : current + 1);
                    }
                }
            }

            if (added) {
                notifyCheck = isLast;
            }

            if (!popupArrayAdd.isEmpty() && !AndroidUtilities.needShowPasscode() && !SharedConfig.isWaitingForPasscodeEnter) {
                final int popupFinal = popup;
                AndroidUtilities.runOnUIThread(() -> {
                    popupMessages.addAll(0, popupArrayAdd);
                    if (ApplicationLoader.mainInterfacePaused || !ApplicationLoader.isScreenOn) {
                        if (popupFinal == 3 || popupFinal == 1 && ApplicationLoader.isScreenOn || popupFinal == 2 && !ApplicationLoader.isScreenOn) {
//                            Intent popupIntent = new Intent(ApplicationLoader.applicationContext, PopupNotificationActivity.class);
//                            popupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_FROM_BACKGROUND);
//                            try {
//                                ApplicationLoader.applicationContext.startActivity(popupIntent);
//                            } catch (Throwable ignore) {
//
//                            }
                        }
                    }
                });
            }
            if (isFcm || hasScheduled) {
                if (edited) {
                    delayedPushMessages.clear();
                    showOrUpdateNotification(notifyCheck);
                } else if (added) {
                    MessageObject messageObject = messageObjects.get(0);
                    long dialog_id = messageObject.getDialogId();
                    Boolean isChannel;
                    if (messageObject.isFcmMessage()) {
                        isChannel = messageObject.localChannel;
                    } else {
                        isChannel = null;
                    }
                    int old_unread_count = total_unread_count;

                    int notifyOverride = getNotifyOverride(preferences, dialog_id);
                    boolean canAddValue;
                    if (notifyOverride == -1) {
                        canAddValue = isGlobalNotificationsEnabled(dialog_id, isChannel);
                        /*if (BuildVars.DEBUG_PRIVATE_VERSION && BuildVars.LOGS_ENABLED) {
                            FileLog.d("global notify settings for " + dialog_id + " = " + canAddValue);
                        }*/
                    } else {
                        canAddValue = notifyOverride != 2;
                    }

                    Integer currentCount = pushDialogs.get(dialog_id);
                    Integer newCount = currentCount != null ? currentCount + 1 : 1;

                    if (notifyCheck && !canAddValue) {
                        Integer override = pushDialogsOverrideMention.get(dialog_id);
                        if (override != null && override != 0) {
                            canAddValue = true;
                            newCount = override;
                        }
                    }

                    if (canAddValue) {
                        if (currentCount != null) {
                            total_unread_count -= currentCount;
                        }
                        total_unread_count += newCount;
                        pushDialogs.put(dialog_id, newCount);
                    }
                    if (old_unread_count != total_unread_count) {
                        delayedPushMessages.clear();
                        showOrUpdateNotification(notifyCheck);
                        final int pushDialogsCount = pushDialogs.size();
                        AndroidUtilities.runOnUIThread(() -> {
                            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.notificationsCountUpdated, currentAccount);
                            getNotificationCenter().postNotificationName(NotificationCenter.dialogsUnreadCounterChanged, pushDialogsCount);
                        });
                    }
                    notifyCheck = false;
                    if (showBadgeNumber) {
                        setBadge(getTotalAllUnreadCount());
                    }
                }
            }
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
        });
    }

    public int getTotalUnreadCount() {
        return total_unread_count;
    }

    public void processDialogsUpdateRead(final LongSparseArray<Integer> dialogsToUpdate) {
        final ArrayList<MessageObject> popupArrayToRemove = new ArrayList<>();
        notificationsQueue.postRunnable(() -> {
            int old_unread_count = total_unread_count;
            SharedPreferences preferences = getAccountInstance().getNotificationsSettings();
            for (int b = 0; b < dialogsToUpdate.size(); b++) {
                long dialog_id = dialogsToUpdate.keyAt(b);

                int notifyOverride = getNotifyOverride(preferences, dialog_id);
                boolean canAddValue;
                if (notifyOverride == -1) {
                    canAddValue = isGlobalNotificationsEnabled(dialog_id);
                } else {
                    canAddValue = notifyOverride != 2;
                }
                Integer currentCount = pushDialogs.get(dialog_id);
                Integer newCount = dialogsToUpdate.get(dialog_id);

                if (notifyCheck && !canAddValue) {
                    Integer override = pushDialogsOverrideMention.get(dialog_id);
                    if (override != null && override != 0) {
                        canAddValue = true;
                        newCount = override;
                    }
                }

                if (newCount == 0) {
                    smartNotificationsDialogs.remove(dialog_id);
                }

                if (newCount < 0) {
                    if (currentCount == null) {
                        continue;
                    }
                    newCount = currentCount + newCount;
                }
                if (canAddValue || newCount == 0) {
                    if (currentCount != null) {
                        total_unread_count -= currentCount;
                    }
                }
                if (newCount == 0) {
                    pushDialogs.remove(dialog_id);
                    pushDialogsOverrideMention.remove(dialog_id);
                    for (int a = 0; a < pushMessages.size(); a++) {
                        MessageObject messageObject = pushMessages.get(a);
                        if (!messageObject.messageOwner.from_scheduled && messageObject.getDialogId() == dialog_id) {
                            if (isPersonalMessage(messageObject)) {
                                personal_count--;
                            }
                            pushMessages.remove(a);
                            a--;
                            delayedPushMessages.remove(messageObject);
                            long mid = messageObject.getId();
                            if (messageObject.messageOwner.to_id != 0) {
                                mid |= ((long) messageObject.messageOwner.to_id) << 32;
                            }
                            pushMessagesDict.remove(mid);
                            popupArrayToRemove.add(messageObject);
                        }
                    }
                } else if (canAddValue) {
                    total_unread_count += newCount;
                    pushDialogs.put(dialog_id, newCount);
                }
            }
            if (!popupArrayToRemove.isEmpty()) {
                AndroidUtilities.runOnUIThread(() -> {
                    for (int a = 0, size = popupArrayToRemove.size(); a < size; a++) {
                        popupMessages.remove(popupArrayToRemove.get(a));
                    }
                });
            }
            if (old_unread_count != total_unread_count) {
                if (!notifyCheck) {
                    delayedPushMessages.clear();
                    showOrUpdateNotification(notifyCheck);
                } else {
                    scheduleNotificationDelay(lastOnlineFromOtherDevice > getConnectionsManager().getCurrentTime());
                }
                final int pushDialogsCount = pushDialogs.size();
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.notificationsCountUpdated, currentAccount);
                    getNotificationCenter().postNotificationName(NotificationCenter.dialogsUnreadCounterChanged, pushDialogsCount);
                });
            }
            notifyCheck = false;
            if (showBadgeNumber) {
                setBadge(getTotalAllUnreadCount());
            }
        });
    }

    public void processLoadedUnreadMessages(final LongSparseArray<Integer> dialogs, final ArrayList<Message> messages, ArrayList<MessageObject> push, final ArrayList<User> users, final ArrayList<Chat> chats) {
        getMessagesController().putUsers(users, true);
        getMessagesController().putChats(chats, true);

        notificationsQueue.postRunnable(() -> {
            pushDialogs.clear();
            pushMessages.clear();
            pushMessagesDict.clear();
            total_unread_count = 0;
            personal_count = 0;
            SharedPreferences preferences = getAccountInstance().getNotificationsSettings();
            LongSparseArray<Boolean> settingsCache = new LongSparseArray<>();

            if (messages != null) {
                for (int a = 0; a < messages.size(); a++) {
                    Message message = messages.get(a);
                    if (message != null && message.silent && (message.action.isContactSignUp() || message.action.isUserJoined())) {
                        continue;
                    }
                    long mid = message.id;
                    if (message.to_id != 0) {
                        mid |= ((long) message.to_id) << 32;
                    }
                    if (pushMessagesDict.indexOfKey(mid) >= 0) {
                        continue;
                    }
                    MessageObject messageObject = new MessageObject(currentAccount, message, false);
                    if (isPersonalMessage(messageObject)) {
                        personal_count++;
                    }
                    long dialog_id = messageObject.getDialogId();
                    long original_dialog_id = dialog_id;
                    if (messageObject.messageOwner.mentioned) {
                        dialog_id = messageObject.messageOwner.from_id;
                    }
                    int index = settingsCache.indexOfKey(dialog_id);
                    boolean value;
                    if (index >= 0) {
                        value = settingsCache.valueAt(index);
                    } else {
                        int notifyOverride = getNotifyOverride(preferences, dialog_id);
                        if (notifyOverride == -1) {
                            value = isGlobalNotificationsEnabled(dialog_id);
                        } else {
                            value = notifyOverride != 2;
                        }
                        settingsCache.put(dialog_id, value);
                    }
                    if (!value || dialog_id == opened_dialog_id && ApplicationLoader.isScreenOn) {
                        continue;
                    }
                    pushMessagesDict.put(mid, messageObject);
                    pushMessages.add(0, messageObject);
                    if (original_dialog_id != dialog_id) {
                        Integer current = pushDialogsOverrideMention.get(original_dialog_id);
                        pushDialogsOverrideMention.put(original_dialog_id, current == null ? 1 : current + 1);
                    }
                }
            }
            for (int a = 0; a < dialogs.size(); a++) {
                long dialog_id = dialogs.keyAt(a);
                int index = settingsCache.indexOfKey(dialog_id);
                boolean value;
                if (index >= 0) {
                    value = settingsCache.valueAt(index);
                } else {
                    int notifyOverride = getNotifyOverride(preferences, dialog_id);
                    if (notifyOverride == -1) {
                        value = isGlobalNotificationsEnabled(dialog_id);
                    } else {
                        value = notifyOverride != 2;
                    }

                    /*if (!value) {
                        Integer override = pushDialogsOverrideMention.get(dialog_id);
                        if (override != null && override != 0) {
                            value = true;
                            newCount = override;
                        }
                    }*/

                    settingsCache.put(dialog_id, value);
                }
                if (!value) {
                    continue;
                }
                int count = dialogs.valueAt(a);
                pushDialogs.put(dialog_id, count);
                total_unread_count += count;
            }

            if (push != null) {
                for (int a = 0; a < push.size(); a++) {
                    MessageObject messageObject = push.get(a);
                    long mid = messageObject.getId();
                    if (messageObject.messageOwner.to_id != 0) {
                        mid |= ((long) messageObject.messageOwner.to_id) << 32;
                    }
                    if (pushMessagesDict.indexOfKey(mid) >= 0) {
                        continue;
                    }
                    if (isPersonalMessage(messageObject)) {
                        personal_count++;
                    }
                    long dialog_id = messageObject.getDialogId();
                    long original_dialog_id = dialog_id;
                    long random_id = messageObject.messageOwner.random_id;
                    if (messageObject.messageOwner.mentioned) {
                        dialog_id = messageObject.messageOwner.from_id;
                    }
                    int index = settingsCache.indexOfKey(dialog_id);
                    boolean value;
                    if (index >= 0) {
                        value = settingsCache.valueAt(index);
                    } else {
                        int notifyOverride = getNotifyOverride(preferences, dialog_id);
                        if (notifyOverride == -1) {
                            value = isGlobalNotificationsEnabled(dialog_id);
                        } else {
                            value = notifyOverride != 2;
                        }
                        settingsCache.put(dialog_id, value);
                    }
                    if (!value || dialog_id == opened_dialog_id && ApplicationLoader.isScreenOn) {
                        continue;
                    }
                    if (mid != 0) {
                        pushMessagesDict.put(mid, messageObject);
                    } else if (random_id != 0) {
                        fcmRandomMessagesDict.put(random_id, messageObject);
                    }
                    pushMessages.add(0, messageObject);
                    if (original_dialog_id != dialog_id) {
                        Integer current = pushDialogsOverrideMention.get(original_dialog_id);
                        pushDialogsOverrideMention.put(original_dialog_id, current == null ? 1 : current + 1);
                    }

                    Integer currentCount = pushDialogs.get(dialog_id);
                    Integer newCount = currentCount != null ? currentCount + 1 : 1;

                    if (currentCount != null) {
                        total_unread_count -= currentCount;
                    }
                    total_unread_count += newCount;
                    pushDialogs.put(dialog_id, newCount);
                }
            }

            final int pushDialogsCount = pushDialogs.size();
            AndroidUtilities.runOnUIThread(() -> {
                if (total_unread_count == 0) {
                    popupMessages.clear();
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.pushMessagesUpdated);
                }
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.notificationsCountUpdated, currentAccount);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogsUnreadCounterChanged, pushDialogsCount);
            });
            showOrUpdateNotification(SystemClock.elapsedRealtime() / 1000 < 60);

            if (showBadgeNumber) {
                setBadge(getTotalAllUnreadCount());
            }
        });
    }

    private int getTotalAllUnreadCount() {
        int count = 0;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                NotificationsController controller = getInstance(a);
                if (controller.showBadgeNumber) {
                    if (controller.showBadgeMessages) {
                        if (controller.showBadgeMuted) {
                        } else {
                            count += controller.total_unread_count;
                        }
                    } else {
                        if (controller.showBadgeMuted) {
                            try {
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                        } else {
                            count += controller.pushDialogs.size();
                        }
                    }
                }
            }
        }
        return count;
    }

    public void updateBadge() {
        notificationsQueue.postRunnable(() -> setBadge(getTotalAllUnreadCount()));
    }

    private void setBadge(final int count) {
        if (lastBadgeCount == count) {
            return;
        }
        lastBadgeCount = count;
    }

    private String getShortStringForMessage(MessageObject messageObject, String[] userName, boolean[] preview) {
        if (AndroidUtilities.needShowPasscode() || SharedConfig.isWaitingForPasscodeEnter) {
            return LocaleController.getString("NotificationHiddenMessage", R.string.NotificationHiddenMessage);
        }
        long dialog_id = messageObject.messageOwner.dialog_id;
        int chat_id = messageObject.messageOwner.to_id;
        int from_id = messageObject.messageOwner.to_id;
        if (preview != null) {
            preview[0] = true;
        }
        SharedPreferences preferences = getAccountInstance().getNotificationsSettings();
        boolean dialogPreviewEnabled = preferences.getBoolean("content_preview_" + dialog_id, true);
        if (messageObject.isFcmMessage()) {
            if (chat_id == 0 && from_id != 0) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                    userName[0] = messageObject.localName;
                }
                if (!dialogPreviewEnabled || !preferences.getBoolean("EnablePreviewAll", true)) {
                    if (preview != null) {
                        preview[0] = false;
                    }
                    return LocaleController.getString("Message", R.string.Message);
                }
            } else if (chat_id != 0) {
                if (messageObject.messageOwner.to_id == 0 || messageObject.isMegagroup()) {
                    userName[0] = messageObject.localUserName;
                } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                    userName[0] = messageObject.localName;
                }
                if (!dialogPreviewEnabled || !messageObject.localChannel && !preferences.getBoolean("EnablePreviewGroup", true) || messageObject.localChannel && !preferences.getBoolean("EnablePreviewChannel", true)) {
                    if (preview != null) {
                        preview[0] = false;
                    }
                    if (!messageObject.isMegagroup() && messageObject.messageOwner.to_id != 0) {
                        return LocaleController.formatString("ChannelMessageNoText", R.string.ChannelMessageNoText, messageObject.localName);
                    } else {
                        return LocaleController.formatString("NotificationMessageGroupNoText", R.string.NotificationMessageGroupNoText, messageObject.localUserName, messageObject.localName);
                    }
                }
            }
            return messageObject.messageOwner.message;
        }
        if (from_id == 0) {
            if (messageObject.isFromUser() || messageObject.getId() < 0) {
                from_id = messageObject.messageOwner.from_id;
            } else {
                from_id = -chat_id;
            }
        } else if (from_id == getUserConfig().getClientUserId()) {
            from_id = messageObject.messageOwner.from_id;
        }

        if (dialog_id == 0) {
            if (chat_id != 0) {
                dialog_id = -chat_id;
            } else if (from_id != 0) {
                dialog_id = from_id;
            }
        }

        String name = null;
        if (from_id > 0) {
            User user = getMessagesController().getUser(from_id);
            if (user != null) {
                name = UserObject.getUserName(user);
                if (chat_id != 0) {
                    userName[0] = name;
                } else {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                        userName[0] = name;
                    } else {
                        userName[0] = null;
                    }
                }
            }
        } else {
            Chat chat = getMessagesController().getChat(-from_id);
            if (chat != null) {
                name = chat.title;
                userName[0] = name;
            }
        }

        if (name == null) {
            return null;
        }
        Chat chat = null;
        if (chat_id != 0) {
            chat = getMessagesController().getChat(chat_id);
            if (chat == null) {
                return null;
            } else if (ChatObject.isChannel(chat) && !chat.megagroup) {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                    userName[0] = null;
                }
            }
        }

        String msg = null;
        if ((int) dialog_id == 0) {
            userName[0] = null;
            return LocaleController.getString("NotificationHiddenMessage", R.string.NotificationHiddenMessage);
        } else {
            boolean isChannel = ChatObject.isChannel(chat) && !chat.megagroup;
            if (dialogPreviewEnabled && (chat_id == 0 && from_id != 0 && preferences.getBoolean("EnablePreviewAll", true) || chat_id != 0 && (!isChannel && preferences.getBoolean("EnablePreviewGroup", true) || isChannel && preferences.getBoolean("EnablePreviewChannel", true)))) {
                if (messageObject.messageOwner != null) {
                    userName[0] = null;
                    if (messageObject.messageOwner.action.isUserJoined() || messageObject.messageOwner.action.isContactSignUp()) {
                        return LocaleController.formatString("NotificationContactJoined", R.string.NotificationContactJoined, name);
                    } else if (messageObject.messageOwner.action.isUserUpdatedPhoto()) {
                        return LocaleController.formatString("NotificationContactNewPhoto", R.string.NotificationContactNewPhoto, name);
                    } else if (messageObject.messageOwner.action.isLoginUnknownLocation()) {
                        String date = LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, LocaleController.getInstance().formatterYear.format(((long) messageObject.messageOwner.date) * 1000), LocaleController.getInstance().formatterDay.format(((long) messageObject.messageOwner.date) * 1000));
                        return LocaleController.formatString("NotificationUnrecognizedDevice", R.string.NotificationUnrecognizedDevice, getUserConfig().getCurrentUser().first_name, date, messageObject.messageOwner.action.title, messageObject.messageOwner.action.address);
                    } else if (messageObject.messageOwner.action.isPhoneCall()) {
                        return LocaleController.getString("CallMessageIncomingMissed", R.string.CallMessageIncomingMissed);
                    } else if (messageObject.messageOwner.action.isChatAddUser()) {
                        int singleUserId = messageObject.messageOwner.action.user_id;
                        if (singleUserId == 0 && messageObject.messageOwner.action.users.size() == 1) {
                            singleUserId = messageObject.messageOwner.action.users.get(0);
                        }
                        if (singleUserId != 0) {
                            if (messageObject.messageOwner.to_id != 0 && !chat.megagroup) {
                                return LocaleController.formatString("ChannelAddedByNotification", R.string.ChannelAddedByNotification, name, chat.title);
                            } else {
                                if (singleUserId == getUserConfig().getClientUserId()) {
                                    return LocaleController.formatString("NotificationInvitedToGroup", R.string.NotificationInvitedToGroup, name, chat.title);
                                } else {
                                    User u2 = getMessagesController().getUser(singleUserId);
                                    if (u2 == null) {
                                        return null;
                                    }
                                    if (from_id == u2.id) {
                                        if (chat.megagroup) {
                                            return LocaleController.formatString("NotificationGroupAddSelfMega", R.string.NotificationGroupAddSelfMega, name, chat.title);
                                        } else {
                                            return LocaleController.formatString("NotificationGroupAddSelf", R.string.NotificationGroupAddSelf, name, chat.title);
                                        }
                                    } else {
                                        return LocaleController.formatString("NotificationGroupAddMember", R.string.NotificationGroupAddMember, name, chat.title, UserObject.getUserName(u2));
                                    }
                                }
                            }
                        } else {
                            StringBuilder names = new StringBuilder();
                            for (int a = 0; a < messageObject.messageOwner.action.users.size(); a++) {
                                User user = getMessagesController().getUser(messageObject.messageOwner.action.users.get(a));
                                if (user != null) {
                                    String name2 = UserObject.getUserName(user);
                                    if (names.length() != 0) {
                                        names.append(", ");
                                    }
                                    names.append(name2);
                                }
                            }
                            return LocaleController.formatString("NotificationGroupAddMember", R.string.NotificationGroupAddMember, name, chat.title, names.toString());
                        }
                    } else if (messageObject.messageOwner.action.isChatJoinedByLink()) {
                        return LocaleController.formatString("NotificationInvitedToGroupByLink", R.string.NotificationInvitedToGroupByLink, name, chat.title);
                    } else if (messageObject.messageOwner.action.isChatEditTitle()) {
                        return LocaleController.formatString("NotificationEditedGroupName", R.string.NotificationEditedGroupName, name, messageObject.messageOwner.action.title);
                    } else if (messageObject.messageOwner.action.isChatEditPhoto() || messageObject.messageOwner.action.isChatDeletePhoto()) {
                        if (messageObject.messageOwner.to_id != 0 && !chat.megagroup) {
                            return LocaleController.formatString("ChannelPhotoEditNotification", R.string.ChannelPhotoEditNotification, chat.title);
                        } else {
                            return LocaleController.formatString("NotificationEditedGroupPhoto", R.string.NotificationEditedGroupPhoto, name, chat.title);
                        }
                    } else if (messageObject.messageOwner.action.isChatDeleteUser()) {
                        if (messageObject.messageOwner.action.user_id == getUserConfig().getClientUserId()) {
                            return LocaleController.formatString("NotificationGroupKickYou", R.string.NotificationGroupKickYou, name, chat.title);
                        } else if (messageObject.messageOwner.action.user_id == from_id) {
                            return LocaleController.formatString("NotificationGroupLeftMember", R.string.NotificationGroupLeftMember, name, chat.title);
                        } else {
                            User u2 = getMessagesController().getUser(messageObject.messageOwner.action.user_id);
                            if (u2 == null) {
                                return null;
                            }
                            return LocaleController.formatString("NotificationGroupKickMember", R.string.NotificationGroupKickMember, name, chat.title, UserObject.getUserName(u2));
                        }
                    } else if (messageObject.messageOwner.action.isChatCreate()) {
                        return messageObject.messageText.toString();
                    } else if (messageObject.messageOwner.action.isScreenshotTaken()) {
                        return messageObject.messageText.toString();
                    } else if (messageObject.messageOwner.action.isPinMessage()) {
                        if (chat != null && (!ChatObject.isChannel(chat) || chat.megagroup)) {
                            if (messageObject.replyMessageObject == null) {
                                return LocaleController.formatString("NotificationActionPinnedNoText", R.string.NotificationActionPinnedNoText, name, chat.title);
                            } else {
                                MessageObject object = messageObject.replyMessageObject;
                                if (object.isMusic()) {
                                    return LocaleController.formatString("NotificationActionPinnedMusic", R.string.NotificationActionPinnedMusic, name, chat.title);
                                } else if (object.isVideo()) {
                                    if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(object.messageOwner.message)) {
                                        String message = "\uD83D\uDCF9 " + object.messageOwner.message;
                                        return LocaleController.formatString("NotificationActionPinnedText", R.string.NotificationActionPinnedText, name, message, chat.title);
                                    } else {
                                        return LocaleController.formatString("NotificationActionPinnedVideo", R.string.NotificationActionPinnedVideo, name, chat.title);
                                    }
                                } else if (object.isGif()) {
                                    if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(object.messageOwner.message)) {
                                        String message = "\uD83C\uDFAC " + object.messageOwner.message;
                                        return LocaleController.formatString("NotificationActionPinnedText", R.string.NotificationActionPinnedText, name, message, chat.title);
                                    } else {
                                        return LocaleController.formatString("NotificationActionPinnedGif", R.string.NotificationActionPinnedGif, name, chat.title);
                                    }
                                } else if (object.isVoice()) {
                                    return LocaleController.formatString("NotificationActionPinnedVoice", R.string.NotificationActionPinnedVoice, name, chat.title);
                                } else if (object.isRoundVideo()) {
                                    return LocaleController.formatString("NotificationActionPinnedRound", R.string.NotificationActionPinnedRound, name, chat.title);
                                } else if (object.isSticker() || object.isAnimatedSticker()) {
                                    String emoji = object.getStickerEmoji();
                                    if (emoji != null) {
                                        return LocaleController.formatString("NotificationActionPinnedStickerEmoji", R.string.NotificationActionPinnedStickerEmoji, name, chat.title, emoji);
                                    } else {
                                        return LocaleController.formatString("NotificationActionPinnedSticker", R.string.NotificationActionPinnedSticker, name, chat.title);
                                    }
                                } else if (object.messageOwner.media.isDocument()) {
                                    if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(object.messageOwner.message)) {
                                        String message = "\uD83D\uDCCE " + object.messageOwner.message;
                                        return LocaleController.formatString("NotificationActionPinnedText", R.string.NotificationActionPinnedText, name, message, chat.title);
                                    } else {
                                        return LocaleController.formatString("NotificationActionPinnedFile", R.string.NotificationActionPinnedFile, name, chat.title);
                                    }
                                } else if (object.messageOwner.media.isGeo() || object.messageOwner.media.isVenue()) {
                                    return LocaleController.formatString("NotificationActionPinnedGeo", R.string.NotificationActionPinnedGeo, name, chat.title);
                                } else if (object.messageOwner.media.isGeoLive()) {
                                    return LocaleController.formatString("NotificationActionPinnedGeoLive", R.string.NotificationActionPinnedGeoLive, name, chat.title);
                                } else if (object.messageOwner.media.isContact()) {
                                    MessageMedia mediaContact = (MessageMedia) object.messageOwner.media;
                                    return LocaleController.formatString("NotificationActionPinnedContact2", R.string.NotificationActionPinnedContact2, name, chat.title, UserObject.formatName(mediaContact.first_name, mediaContact.last_name));
                                } else if (object.messageOwner.media.isPhoto()) {
                                    if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(object.messageOwner.message)) {
                                        String message = "\uD83D\uDDBC " + object.messageOwner.message;
                                        return LocaleController.formatString("NotificationActionPinnedText", R.string.NotificationActionPinnedText, name, message, chat.title);
                                    } else {
                                        return LocaleController.formatString("NotificationActionPinnedPhoto", R.string.NotificationActionPinnedPhoto, name, chat.title);
                                    }
                                } else if (object.messageText != null && object.messageText.length() > 0) {
                                    CharSequence message = object.messageText;
                                    if (message.length() > 20) {
                                        message = message.subSequence(0, 20) + "...";
                                    }
                                    return LocaleController.formatString("NotificationActionPinnedText", R.string.NotificationActionPinnedText, name, message, chat.title);
                                } else {
                                    return LocaleController.formatString("NotificationActionPinnedNoText", R.string.NotificationActionPinnedNoText, name, chat.title);
                                }
                            }
                        } else {
                            if (messageObject.replyMessageObject == null) {
                                return LocaleController.formatString("NotificationActionPinnedNoTextChannel", R.string.NotificationActionPinnedNoTextChannel, chat.title);
                            } else {
                                MessageObject object = messageObject.replyMessageObject;
                                if (object.isMusic()) {
                                    return LocaleController.formatString("NotificationActionPinnedMusicChannel", R.string.NotificationActionPinnedMusicChannel, chat.title);
                                } else if (object.isVideo()) {
                                    if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(object.messageOwner.message)) {
                                        String message = "\uD83D\uDCF9 " + object.messageOwner.message;
                                        return LocaleController.formatString("NotificationActionPinnedTextChannel", R.string.NotificationActionPinnedTextChannel, chat.title, message);
                                    } else {
                                        return LocaleController.formatString("NotificationActionPinnedVideoChannel", R.string.NotificationActionPinnedVideoChannel, chat.title);
                                    }
                                } else if (object.isGif()) {
                                    if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(object.messageOwner.message)) {
                                        String message = "\uD83C\uDFAC " + object.messageOwner.message;
                                        return LocaleController.formatString("NotificationActionPinnedTextChannel", R.string.NotificationActionPinnedTextChannel, chat.title, message);
                                    } else {
                                        return LocaleController.formatString("NotificationActionPinnedGifChannel", R.string.NotificationActionPinnedGifChannel, chat.title);
                                    }
                                } else if (object.isVoice()) {
                                    return LocaleController.formatString("NotificationActionPinnedVoiceChannel", R.string.NotificationActionPinnedVoiceChannel, chat.title);
                                } else if (object.isRoundVideo()) {
                                    return LocaleController.formatString("NotificationActionPinnedRoundChannel", R.string.NotificationActionPinnedRoundChannel, chat.title);
                                } else if (object.isSticker() || object.isAnimatedSticker()) {
                                    String emoji = object.getStickerEmoji();
                                    if (emoji != null) {
                                        return LocaleController.formatString("NotificationActionPinnedStickerEmojiChannel", R.string.NotificationActionPinnedStickerEmojiChannel, chat.title, emoji);
                                    } else {
                                        return LocaleController.formatString("NotificationActionPinnedStickerChannel", R.string.NotificationActionPinnedStickerChannel, chat.title);
                                    }
                                } else if (object.messageOwner.media.isDocument()) {
                                    if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(object.messageOwner.message)) {
                                        String message = "\uD83D\uDCCE " + object.messageOwner.message;
                                        return LocaleController.formatString("NotificationActionPinnedTextChannel", R.string.NotificationActionPinnedTextChannel, chat.title, message);
                                    } else {
                                        return LocaleController.formatString("NotificationActionPinnedFileChannel", R.string.NotificationActionPinnedFileChannel, chat.title);
                                    }
                                } else if (object.messageOwner.media.isGeo() || object.messageOwner.media.isVenue()) {
                                    return LocaleController.formatString("NotificationActionPinnedGeoChannel", R.string.NotificationActionPinnedGeoChannel, chat.title);
                                } else if (object.messageOwner.media.isGeoLive()) {
                                    return LocaleController.formatString("NotificationActionPinnedGeoLiveChannel", R.string.NotificationActionPinnedGeoLiveChannel, chat.title);
                                } else if (object.messageOwner.media.isContact()) {
                                    MessageMedia mediaContact = (MessageMedia) object.messageOwner.media;
                                    return LocaleController.formatString("NotificationActionPinnedContactChannel2", R.string.NotificationActionPinnedContactChannel2, chat.title, UserObject.formatName(mediaContact.first_name, mediaContact.last_name));
                                } else if (object.messageOwner.media.isPhoto()) {
                                    if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(object.messageOwner.message)) {
                                        String message = "\uD83D\uDDBC " + object.messageOwner.message;
                                        return LocaleController.formatString("NotificationActionPinnedTextChannel", R.string.NotificationActionPinnedTextChannel, chat.title, message);
                                    } else {
                                        return LocaleController.formatString("NotificationActionPinnedPhotoChannel", R.string.NotificationActionPinnedPhotoChannel, chat.title);
                                    }
                                } else if (object.messageText != null && object.messageText.length() > 0) {
                                    CharSequence message = object.messageText;
                                    if (message.length() > 20) {
                                        message = message.subSequence(0, 20) + "...";
                                    }
                                    return LocaleController.formatString("NotificationActionPinnedTextChannel", R.string.NotificationActionPinnedTextChannel, chat.title, message);
                                } else {
                                    return LocaleController.formatString("NotificationActionPinnedNoTextChannel", R.string.NotificationActionPinnedNoTextChannel, chat.title);
                                }
                            }
                        }
                    }
                } else {
                    if (messageObject.isMediaEmpty()) {
                        if (!TextUtils.isEmpty(messageObject.messageOwner.message)) {
                            return messageObject.messageOwner.message;
                        } else {
                            return LocaleController.getString("Message", R.string.Message);
                        }
                    } else if (messageObject.messageOwner.media.isPhoto()) {
                        if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                            return "\uD83D\uDDBC " + messageObject.messageOwner.message;
                        } else if (messageObject.messageOwner.media.ttl_seconds != 0) {
                            return LocaleController.getString("AttachDestructingPhoto", R.string.AttachDestructingPhoto);
                        } else {
                            return LocaleController.getString("AttachPhoto", R.string.AttachPhoto);
                        }
                    } else if (messageObject.isVideo()) {
                        if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                            return "\uD83D\uDCF9 " + messageObject.messageOwner.message;
                        } else if (messageObject.messageOwner.media.ttl_seconds != 0) {
                            return LocaleController.getString("AttachDestructingVideo", R.string.AttachDestructingVideo);
                        } else {
                            return LocaleController.getString("AttachVideo", R.string.AttachVideo);
                        }
                    } else if (messageObject.isGame()) {
                        return LocaleController.getString("AttachGame", R.string.AttachGame);
                    } else if (messageObject.isVoice()) {
                        return LocaleController.getString("AttachAudio", R.string.AttachAudio);
                    } else if (messageObject.isRoundVideo()) {
                        return LocaleController.getString("AttachRound", R.string.AttachRound);
                    } else if (messageObject.isMusic()) {
                        return LocaleController.getString("AttachMusic", R.string.AttachMusic);
                    } else if (messageObject.messageOwner.media.isContact()) {
                        return LocaleController.getString("AttachContact", R.string.AttachContact);
                    } else if (messageObject.messageOwner.media.isGeo() || messageObject.messageOwner.media.isVenue()) {
                        return LocaleController.getString("AttachLocation", R.string.AttachLocation);
                    } else if (messageObject.messageOwner.media.isGeoLive()) {
                        return LocaleController.getString("AttachLiveLocation", R.string.AttachLiveLocation);
                    } else if (messageObject.messageOwner.media.isDocument()) {
                        if (messageObject.isSticker() || messageObject.isAnimatedSticker()) {
                            String emoji = messageObject.getStickerEmoji();
                            if (emoji != null) {
                                return emoji + " " + LocaleController.getString("AttachSticker", R.string.AttachSticker);
                            } else {
                                return LocaleController.getString("AttachSticker", R.string.AttachSticker);
                            }
                        } else if (messageObject.isGif()) {
                            if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                                return "\uD83C\uDFAC " + messageObject.messageOwner.message;
                            } else {
                                return LocaleController.getString("AttachGif", R.string.AttachGif);
                            }
                        } else {
                            if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                                return "\uD83D\uDCCE " + messageObject.messageOwner.message;
                            } else {
                                return LocaleController.getString("AttachDocument", R.string.AttachDocument);
                            }
                        }
                    } else if (!TextUtils.isEmpty(messageObject.messageText)) {
                        return messageObject.messageText.toString();
                    } else {
                        return LocaleController.getString("Message", R.string.Message);
                    }
                }
            } else {
                if (preview != null) {
                    preview[0] = false;
                }
                return LocaleController.getString("Message", R.string.Message);
            }
        }
        return null;
    }

    private String getStringForMessage(MessageObject messageObject, boolean shortMessage, boolean[] text, boolean[] preview) {
        if (AndroidUtilities.needShowPasscode() || SharedConfig.isWaitingForPasscodeEnter) {
            return LocaleController.getString("YouHaveNewMessage", R.string.YouHaveNewMessage);
        }
        long dialog_id = messageObject.messageOwner.dialog_id;
        int chat_id = messageObject.messageOwner.to_id;
        int from_id = messageObject.messageOwner.to_id;
        if (preview != null) {
            preview[0] = true;
        }
        SharedPreferences preferences = getAccountInstance().getNotificationsSettings();
        boolean dialogPreviewEnabled = preferences.getBoolean("content_preview_" + dialog_id, true);
        if (messageObject.isFcmMessage()) {
            if (chat_id == 0 && from_id != 0) {
                if (!dialogPreviewEnabled || !preferences.getBoolean("EnablePreviewAll", true)) {
                    if (preview != null) {
                        preview[0] = false;
                    }
                    return LocaleController.formatString("NotificationMessageNoText", R.string.NotificationMessageNoText, messageObject.localName);
                }
            } else if (chat_id != 0) {
                if (!dialogPreviewEnabled || !messageObject.localChannel && !preferences.getBoolean("EnablePreviewGroup", true) || messageObject.localChannel && !preferences.getBoolean("EnablePreviewChannel", true)) {
                    if (preview != null) {
                        preview[0] = false;
                    }
                    if (!messageObject.isMegagroup() && messageObject.messageOwner.to_id != 0) {
                        return LocaleController.formatString("ChannelMessageNoText", R.string.ChannelMessageNoText, messageObject.localName);
                    } else {
                        return LocaleController.formatString("NotificationMessageGroupNoText", R.string.NotificationMessageGroupNoText, messageObject.localUserName, messageObject.localName);
                    }
                }
            }
            text[0] = true;
            return (String) messageObject.messageText;
        }
        int selfUsedId = getUserConfig().getClientUserId();
        if (from_id == 0) {
            if (messageObject.isFromUser() || messageObject.getId() < 0) {
                from_id = messageObject.messageOwner.from_id;
            } else {
                from_id = -chat_id;
            }
        } else if (from_id == selfUsedId) {
            from_id = messageObject.messageOwner.from_id;
        }

        if (dialog_id == 0) {
            if (chat_id != 0) {
                dialog_id = -chat_id;
            } else if (from_id != 0) {
                dialog_id = from_id;
            }
        }

        String name = null;
        if (from_id > 0) {
            if (messageObject.messageOwner.from_scheduled) {
                if (dialog_id == selfUsedId) {
                    name = LocaleController.getString("MessageScheduledReminderNotification", R.string.MessageScheduledReminderNotification);
                } else {
                    name = LocaleController.getString("NotificationMessageScheduledName", R.string.NotificationMessageScheduledName);
                }
            } else {
                User user = getMessagesController().getUser(from_id);
                if (user != null) {
                    name = UserObject.getUserName(user);
                }
            }
        } else {
            Chat chat = getMessagesController().getChat(-from_id);
            if (chat != null) {
                name = chat.title;
            }
        }

        if (name == null) {
            return null;
        }
        Chat chat = null;
        if (chat_id != 0) {
            chat = getMessagesController().getChat(chat_id);
            if (chat == null) {
                return null;
            }
        }

        String msg = null;
        if ((int) dialog_id == 0) {
            msg = LocaleController.getString("YouHaveNewMessage", R.string.YouHaveNewMessage);
        } else {
            if (chat_id == 0 && from_id != 0) {
                if (dialogPreviewEnabled && preferences.getBoolean("EnablePreviewAll", true)) {
                    if (messageObject.messageOwner !=null) {
                        if (messageObject.messageOwner.action.isUserJoined() || messageObject.messageOwner.action.isContactSignUp()) {
                            msg = LocaleController.formatString("NotificationContactJoined", R.string.NotificationContactJoined, name);
                        } else if (messageObject.messageOwner.action.isUserUpdatedPhoto()) {
                            msg = LocaleController.formatString("NotificationContactNewPhoto", R.string.NotificationContactNewPhoto, name);
                        } else if (messageObject.messageOwner.action.isLoginUnknownLocation()) {
                            String date = LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, LocaleController.getInstance().formatterYear.format(((long) messageObject.messageOwner.date) * 1000), LocaleController.getInstance().formatterDay.format(((long) messageObject.messageOwner.date) * 1000));
                            msg = LocaleController.formatString("NotificationUnrecognizedDevice", R.string.NotificationUnrecognizedDevice, getUserConfig().getCurrentUser().first_name, date, messageObject.messageOwner.action.title, messageObject.messageOwner.action.address);
                        } else if (messageObject.messageOwner.action.isPhoneCall()) {
                            msg = LocaleController.getString("CallMessageIncomingMissed", R.string.CallMessageIncomingMissed);
                        }
                    } else {
                        if (messageObject.isMediaEmpty()) {
                            if (!shortMessage) {
                                if (!TextUtils.isEmpty(messageObject.messageOwner.message)) {
                                    msg = LocaleController.formatString("NotificationMessageText", R.string.NotificationMessageText, name, messageObject.messageOwner.message);
                                    text[0] = true;
                                } else {
                                    msg = LocaleController.formatString("NotificationMessageNoText", R.string.NotificationMessageNoText, name);
                                }
                            } else {
                                msg = LocaleController.formatString("NotificationMessageNoText", R.string.NotificationMessageNoText, name);
                            }
                        } else if (messageObject.messageOwner.media.isPhoto()) {
                            if (!shortMessage && Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                                msg = LocaleController.formatString("NotificationMessageText", R.string.NotificationMessageText, name, "\uD83D\uDDBC " + messageObject.messageOwner.message);
                                text[0] = true;
                            } else {
                                if (messageObject.messageOwner.media.ttl_seconds != 0) {
                                    msg = LocaleController.formatString("NotificationMessageSDPhoto", R.string.NotificationMessageSDPhoto, name);
                                } else {
                                    msg = LocaleController.formatString("NotificationMessagePhoto", R.string.NotificationMessagePhoto, name);
                                }
                            }
                        } else if (messageObject.isVideo()) {
                            if (!shortMessage && Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                                msg = LocaleController.formatString("NotificationMessageText", R.string.NotificationMessageText, name, "\uD83D\uDCF9 " + messageObject.messageOwner.message);
                                text[0] = true;
                            } else {
                                if (messageObject.messageOwner.media.ttl_seconds != 0) {
                                    msg = LocaleController.formatString("NotificationMessageSDVideo", R.string.NotificationMessageSDVideo, name);
                                } else {
                                    msg = LocaleController.formatString("NotificationMessageVideo", R.string.NotificationMessageVideo, name);
                                }
                            }
                        } else if (messageObject.isVoice()) {
                            msg = LocaleController.formatString("NotificationMessageAudio", R.string.NotificationMessageAudio, name);
                        } else if (messageObject.isRoundVideo()) {
                            msg = LocaleController.formatString("NotificationMessageRound", R.string.NotificationMessageRound, name);
                        } else if (messageObject.isMusic()) {
                            msg = LocaleController.formatString("NotificationMessageMusic", R.string.NotificationMessageMusic, name);
                        } else if (messageObject.messageOwner.media.isContact()) {
                            MessageMedia mediaContact = (MessageMedia) messageObject.messageOwner.media;
                            msg = LocaleController.formatString("NotificationMessageContact2", R.string.NotificationMessageContact2, name, UserObject.formatName(mediaContact.first_name, mediaContact.last_name));
                        } else if (messageObject.messageOwner.media.isGeo() || messageObject.messageOwner.media.isVenue()) {
                            msg = LocaleController.formatString("NotificationMessageMap", R.string.NotificationMessageMap, name);
                        } else if (messageObject.messageOwner.media.isGeoLive()) {
                            msg = LocaleController.formatString("NotificationMessageLiveLocation", R.string.NotificationMessageLiveLocation, name);
                        } else if (messageObject.messageOwner.media.isDocument()) {
                            if (messageObject.isSticker() || messageObject.isAnimatedSticker()) {
                                String emoji = messageObject.getStickerEmoji();
                                if (emoji != null) {
                                    msg = LocaleController.formatString("NotificationMessageStickerEmoji", R.string.NotificationMessageStickerEmoji, name, emoji);
                                } else {
                                    msg = LocaleController.formatString("NotificationMessageSticker", R.string.NotificationMessageSticker, name);
                                }
                            } else if (messageObject.isGif()) {
                                if (!shortMessage && Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                                    msg = LocaleController.formatString("NotificationMessageText", R.string.NotificationMessageText, name, "\uD83C\uDFAC " + messageObject.messageOwner.message);
                                    text[0] = true;
                                } else {
                                    msg = LocaleController.formatString("NotificationMessageGif", R.string.NotificationMessageGif, name);
                                }
                            } else {
                                if (!shortMessage && Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                                    msg = LocaleController.formatString("NotificationMessageText", R.string.NotificationMessageText, name, "\uD83D\uDCCE " + messageObject.messageOwner.message);
                                    text[0] = true;
                                } else {
                                    msg = LocaleController.formatString("NotificationMessageDocument", R.string.NotificationMessageDocument, name);
                                }
                            }
                        } else {
                            if (!shortMessage && !TextUtils.isEmpty(messageObject.messageText)) {
                                msg = LocaleController.formatString("NotificationMessageText", R.string.NotificationMessageText, name, messageObject.messageText);
                                text[0] = true;
                            } else {
                                msg = LocaleController.formatString("NotificationMessageNoText", R.string.NotificationMessageNoText, name);
                            }
                        }
                    }
                } else {
                    if (preview != null) {
                        preview[0] = false;
                    }
                    msg = LocaleController.formatString("NotificationMessageNoText", R.string.NotificationMessageNoText, name);
                }
            } else if (chat_id != 0) {
                boolean isChannel = ChatObject.isChannel(chat) && !chat.megagroup;
                if (dialogPreviewEnabled && (!isChannel && preferences.getBoolean("EnablePreviewGroup", true) || isChannel && preferences.getBoolean("EnablePreviewChannel", true))) {
                    if (messageObject.messageOwner !=null) {
                        if (messageObject.messageOwner.action.isChatAddUser()) {
                            int singleUserId = messageObject.messageOwner.action.user_id;
                            if (singleUserId == 0 && messageObject.messageOwner.action.users.size() == 1) {
                                singleUserId = messageObject.messageOwner.action.users.get(0);
                            }
                            if (singleUserId != 0) {
                                if (messageObject.messageOwner.to_id != 0 && !chat.megagroup) {
                                    msg = LocaleController.formatString("ChannelAddedByNotification", R.string.ChannelAddedByNotification, name, chat.title);
                                } else {
                                    if (singleUserId == selfUsedId) {
                                        msg = LocaleController.formatString("NotificationInvitedToGroup", R.string.NotificationInvitedToGroup, name, chat.title);
                                    } else {
                                        User u2 = getMessagesController().getUser(singleUserId);
                                        if (u2 == null) {
                                            return null;
                                        }
                                        if (from_id == u2.id) {
                                            if (chat.megagroup) {
                                                msg = LocaleController.formatString("NotificationGroupAddSelfMega", R.string.NotificationGroupAddSelfMega, name, chat.title);
                                            } else {
                                                msg = LocaleController.formatString("NotificationGroupAddSelf", R.string.NotificationGroupAddSelf, name, chat.title);
                                            }
                                        } else {
                                            msg = LocaleController.formatString("NotificationGroupAddMember", R.string.NotificationGroupAddMember, name, chat.title, UserObject.getUserName(u2));
                                        }
                                    }
                                }
                            } else {
                                StringBuilder names = new StringBuilder();
                                for (int a = 0; a < messageObject.messageOwner.action.users.size(); a++) {
                                    User user = getMessagesController().getUser(messageObject.messageOwner.action.users.get(a));
                                    if (user != null) {
                                        String name2 = UserObject.getUserName(user);
                                        if (names.length() != 0) {
                                            names.append(", ");
                                        }
                                        names.append(name2);
                                    }
                                }
                                msg = LocaleController.formatString("NotificationGroupAddMember", R.string.NotificationGroupAddMember, name, chat.title, names.toString());
                            }
                        } else if (messageObject.messageOwner.action.isChatJoinedByLink()) {
                            msg = LocaleController.formatString("NotificationInvitedToGroupByLink", R.string.NotificationInvitedToGroupByLink, name, chat.title);
                        } else if (messageObject.messageOwner.action.isChatEditTitle()) {
                            msg = LocaleController.formatString("NotificationEditedGroupName", R.string.NotificationEditedGroupName, name, messageObject.messageOwner.action.title);
                        } else if (messageObject.messageOwner.action.isChatEditPhoto() || messageObject.messageOwner.action.isChatDeletePhoto()) {
                            if (messageObject.messageOwner.to_id != 0 && !chat.megagroup) {
                                msg = LocaleController.formatString("ChannelPhotoEditNotification", R.string.ChannelPhotoEditNotification, chat.title);
                            } else {
                                msg = LocaleController.formatString("NotificationEditedGroupPhoto", R.string.NotificationEditedGroupPhoto, name, chat.title);
                            }
                        } else if (messageObject.messageOwner.action.isChatDeleteUser()) {
                            if (messageObject.messageOwner.action.user_id == selfUsedId) {
                                msg = LocaleController.formatString("NotificationGroupKickYou", R.string.NotificationGroupKickYou, name, chat.title);
                            } else if (messageObject.messageOwner.action.user_id == from_id) {
                                msg = LocaleController.formatString("NotificationGroupLeftMember", R.string.NotificationGroupLeftMember, name, chat.title);
                            } else {
                                User u2 = getMessagesController().getUser(messageObject.messageOwner.action.user_id);
                                if (u2 == null) {
                                    return null;
                                }
                                msg = LocaleController.formatString("NotificationGroupKickMember", R.string.NotificationGroupKickMember, name, chat.title, UserObject.getUserName(u2));
                            }
                        } else if (messageObject.messageOwner.action.isChatCreate()) {
                            msg = messageObject.messageText.toString();
                        } else if (messageObject.messageOwner.action.isScreenshotTaken()) {
                            msg = messageObject.messageText.toString();
                        } else if (messageObject.messageOwner.action.isPinMessage()) {
                            if (chat != null && (!ChatObject.isChannel(chat) || chat.megagroup)) {
                                if (messageObject.replyMessageObject == null) {
                                    msg = LocaleController.formatString("NotificationActionPinnedNoText", R.string.NotificationActionPinnedNoText, name, chat.title);
                                } else {
                                    MessageObject object = messageObject.replyMessageObject;
                                    if (object.isMusic()) {
                                        msg = LocaleController.formatString("NotificationActionPinnedMusic", R.string.NotificationActionPinnedMusic, name, chat.title);
                                    } else if (object.isVideo()) {
                                        if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(object.messageOwner.message)) {
                                            String message = "\uD83D\uDCF9 " + object.messageOwner.message;
                                            msg = LocaleController.formatString("NotificationActionPinnedText", R.string.NotificationActionPinnedText, name, message, chat.title);
                                        } else {
                                            msg = LocaleController.formatString("NotificationActionPinnedVideo", R.string.NotificationActionPinnedVideo, name, chat.title);
                                        }
                                    } else if (object.isGif()) {
                                        if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(object.messageOwner.message)) {
                                            String message = "\uD83C\uDFAC " + object.messageOwner.message;
                                            msg = LocaleController.formatString("NotificationActionPinnedText", R.string.NotificationActionPinnedText, name, message, chat.title);
                                        } else {
                                            msg = LocaleController.formatString("NotificationActionPinnedGif", R.string.NotificationActionPinnedGif, name, chat.title);
                                        }
                                    } else if (object.isVoice()) {
                                        msg = LocaleController.formatString("NotificationActionPinnedVoice", R.string.NotificationActionPinnedVoice, name, chat.title);
                                    } else if (object.isRoundVideo()) {
                                        msg = LocaleController.formatString("NotificationActionPinnedRound", R.string.NotificationActionPinnedRound, name, chat.title);
                                    } else if (object.isSticker() || object.isAnimatedSticker()) {
                                        String emoji = object.getStickerEmoji();
                                        if (emoji != null) {
                                            msg = LocaleController.formatString("NotificationActionPinnedStickerEmoji", R.string.NotificationActionPinnedStickerEmoji, name, chat.title, emoji);
                                        } else {
                                            msg = LocaleController.formatString("NotificationActionPinnedSticker", R.string.NotificationActionPinnedSticker, name, chat.title);
                                        }
                                    } else if (object.messageOwner.media.isDocument()) {
                                        if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(object.messageOwner.message)) {
                                            String message = "\uD83D\uDCCE " + object.messageOwner.message;
                                            msg = LocaleController.formatString("NotificationActionPinnedText", R.string.NotificationActionPinnedText, name, message, chat.title);
                                        } else {
                                            msg = LocaleController.formatString("NotificationActionPinnedFile", R.string.NotificationActionPinnedFile, name, chat.title);
                                        }
                                    } else if (object.messageOwner.media.isGeo() || object.messageOwner.media.isVenue()) {
                                        msg = LocaleController.formatString("NotificationActionPinnedGeo", R.string.NotificationActionPinnedGeo, name, chat.title);
                                    } else if (object.messageOwner.media.isGeoLive()) {
                                        msg = LocaleController.formatString("NotificationActionPinnedGeoLive", R.string.NotificationActionPinnedGeoLive, name, chat.title);
                                    } else if (object.messageOwner.media.isContact()) {
                                        MessageMedia mediaContact = (MessageMedia) messageObject.messageOwner.media;
                                        msg = LocaleController.formatString("NotificationActionPinnedContact2", R.string.NotificationActionPinnedContact2, name, chat.title, UserObject.formatName(mediaContact.first_name, mediaContact.last_name));
                                    } else if (object.messageOwner.media.isPhoto()) {
                                        if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(object.messageOwner.message)) {
                                            String message = "\uD83D\uDDBC " + object.messageOwner.message;
                                            msg = LocaleController.formatString("NotificationActionPinnedText", R.string.NotificationActionPinnedText, name, message, chat.title);
                                        } else {
                                            msg = LocaleController.formatString("NotificationActionPinnedPhoto", R.string.NotificationActionPinnedPhoto, name, chat.title);
                                        }
                                    } else if (object.messageText != null && object.messageText.length() > 0) {
                                        CharSequence message = object.messageText;
                                        if (message.length() > 20) {
                                            message = message.subSequence(0, 20) + "...";
                                        }
                                        msg = LocaleController.formatString("NotificationActionPinnedText", R.string.NotificationActionPinnedText, name, message, chat.title);
                                    } else {
                                        msg = LocaleController.formatString("NotificationActionPinnedNoText", R.string.NotificationActionPinnedNoText, name, chat.title);
                                    }
                                }
                            } else {
                                if (messageObject.replyMessageObject == null) {
                                    msg = LocaleController.formatString("NotificationActionPinnedNoTextChannel", R.string.NotificationActionPinnedNoTextChannel, chat.title);
                                } else {
                                    MessageObject object = messageObject.replyMessageObject;
                                    if (object.isMusic()) {
                                        msg = LocaleController.formatString("NotificationActionPinnedMusicChannel", R.string.NotificationActionPinnedMusicChannel, chat.title);
                                    } else if (object.isVideo()) {
                                        if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(object.messageOwner.message)) {
                                            String message = "\uD83D\uDCF9 " + object.messageOwner.message;
                                            msg = LocaleController.formatString("NotificationActionPinnedTextChannel", R.string.NotificationActionPinnedTextChannel, chat.title, message);
                                        } else {
                                            msg = LocaleController.formatString("NotificationActionPinnedVideoChannel", R.string.NotificationActionPinnedVideoChannel, chat.title);
                                        }
                                    } else if (object.isGif()) {
                                        if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(object.messageOwner.message)) {
                                            String message = "\uD83C\uDFAC " + object.messageOwner.message;
                                            msg = LocaleController.formatString("NotificationActionPinnedTextChannel", R.string.NotificationActionPinnedTextChannel, chat.title, message);
                                        } else {
                                            msg = LocaleController.formatString("NotificationActionPinnedGifChannel", R.string.NotificationActionPinnedGifChannel, chat.title);
                                        }
                                    } else if (object.isVoice()) {
                                        msg = LocaleController.formatString("NotificationActionPinnedVoiceChannel", R.string.NotificationActionPinnedVoiceChannel, chat.title);
                                    } else if (object.isRoundVideo()) {
                                        msg = LocaleController.formatString("NotificationActionPinnedRoundChannel", R.string.NotificationActionPinnedRoundChannel, chat.title);
                                    } else if (object.isSticker() || object.isAnimatedSticker()) {
                                        String emoji = object.getStickerEmoji();
                                        if (emoji != null) {
                                            msg = LocaleController.formatString("NotificationActionPinnedStickerEmojiChannel", R.string.NotificationActionPinnedStickerEmojiChannel, chat.title, emoji);
                                        } else {
                                            msg = LocaleController.formatString("NotificationActionPinnedStickerChannel", R.string.NotificationActionPinnedStickerChannel, chat.title);
                                        }
                                    } else if (object.messageOwner.media.isDocument()) {
                                        if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(object.messageOwner.message)) {
                                            String message = "\uD83D\uDCCE " + object.messageOwner.message;
                                            msg = LocaleController.formatString("NotificationActionPinnedTextChannel", R.string.NotificationActionPinnedTextChannel, chat.title, message);
                                        } else {
                                            msg = LocaleController.formatString("NotificationActionPinnedFileChannel", R.string.NotificationActionPinnedFileChannel, chat.title);
                                        }
                                    } else if (object.messageOwner.media.isGeo() || object.messageOwner.media.isVenue()) {
                                        msg = LocaleController.formatString("NotificationActionPinnedGeoChannel", R.string.NotificationActionPinnedGeoChannel, chat.title);
                                    } else if (object.messageOwner.media.isGeoLive()) {
                                        msg = LocaleController.formatString("NotificationActionPinnedGeoLiveChannel", R.string.NotificationActionPinnedGeoLiveChannel, chat.title);
                                    } else if (object.messageOwner.media.isContact()) {
                                        MessageMedia mediaContact = (MessageMedia) messageObject.messageOwner.media;
                                        msg = LocaleController.formatString("NotificationActionPinnedContactChannel2", R.string.NotificationActionPinnedContactChannel2, chat.title, UserObject.formatName(mediaContact.first_name, mediaContact.last_name));
                                    } else if (object.messageOwner.media.isPhoto()) {
                                        if (Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(object.messageOwner.message)) {
                                            String message = "\uD83D\uDDBC " + object.messageOwner.message;
                                            msg = LocaleController.formatString("NotificationActionPinnedTextChannel", R.string.NotificationActionPinnedTextChannel, chat.title, message);
                                        } else {
                                            msg = LocaleController.formatString("NotificationActionPinnedPhotoChannel", R.string.NotificationActionPinnedPhotoChannel, chat.title);
                                        }
                                    } else if (object.messageText != null && object.messageText.length() > 0) {
                                        CharSequence message = object.messageText;
                                        if (message.length() > 20) {
                                            message = message.subSequence(0, 20) + "...";
                                        }
                                        msg = LocaleController.formatString("NotificationActionPinnedTextChannel", R.string.NotificationActionPinnedTextChannel, chat.title, message);
                                    } else {
                                        msg = LocaleController.formatString("NotificationActionPinnedNoTextChannel", R.string.NotificationActionPinnedNoTextChannel, chat.title);
                                    }
                                }
                            }
                        }
                    } else if (ChatObject.isChannel(chat) && !chat.megagroup) {
                        if (messageObject.isMediaEmpty()) {
                            if (!shortMessage && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                                msg = LocaleController.formatString("NotificationMessageText", R.string.NotificationMessageText, name, messageObject.messageOwner.message);
                                text[0] = true;
                            } else {
                                msg = LocaleController.formatString("ChannelMessageNoText", R.string.ChannelMessageNoText, name);
                            }
                        } else if (messageObject.messageOwner.media.isPhoto()) {
                            if (!shortMessage && Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                                msg = LocaleController.formatString("NotificationMessageText", R.string.NotificationMessageText, name, "\uD83D\uDDBC " + messageObject.messageOwner.message);
                                text[0] = true;
                            } else {
                                msg = LocaleController.formatString("ChannelMessagePhoto", R.string.ChannelMessagePhoto, name);
                            }
                        } else if (messageObject.isVideo()) {
                            if (!shortMessage && Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                                msg = LocaleController.formatString("NotificationMessageText", R.string.NotificationMessageText, name, "\uD83D\uDCF9 " + messageObject.messageOwner.message);
                                text[0] = true;
                            } else {
                                msg = LocaleController.formatString("ChannelMessageVideo", R.string.ChannelMessageVideo, name);
                            }
                        } else if (messageObject.isVoice()) {
                            msg = LocaleController.formatString("ChannelMessageAudio", R.string.ChannelMessageAudio, name);
                        } else if (messageObject.isRoundVideo()) {
                            msg = LocaleController.formatString("ChannelMessageRound", R.string.ChannelMessageRound, name);
                        } else if (messageObject.isMusic()) {
                            msg = LocaleController.formatString("ChannelMessageMusic", R.string.ChannelMessageMusic, name);
                        } else if (messageObject.messageOwner.media.isContact()) {
                            MessageMedia mediaContact = (MessageMedia) messageObject.messageOwner.media;
                            msg = LocaleController.formatString("ChannelMessageContact2", R.string.ChannelMessageContact2, name, UserObject.formatName(mediaContact.first_name, mediaContact.last_name));
                        } else if (messageObject.messageOwner.media.isGeo() || messageObject.messageOwner.media.isVenue()) {
                            msg = LocaleController.formatString("ChannelMessageMap", R.string.ChannelMessageMap, name);
                        } else if (messageObject.messageOwner.media.isGeoLive()) {
                            msg = LocaleController.formatString("ChannelMessageLiveLocation", R.string.ChannelMessageLiveLocation, name);
                        } else if (messageObject.messageOwner.media.isDocument()) {
                            if (messageObject.isSticker() || messageObject.isAnimatedSticker()) {
                                String emoji = messageObject.getStickerEmoji();
                                if (emoji != null) {
                                    msg = LocaleController.formatString("ChannelMessageStickerEmoji", R.string.ChannelMessageStickerEmoji, name, emoji);
                                } else {
                                    msg = LocaleController.formatString("ChannelMessageSticker", R.string.ChannelMessageSticker, name);
                                }
                            } else if (messageObject.isGif()) {
                                if (!shortMessage && Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                                    msg = LocaleController.formatString("NotificationMessageText", R.string.NotificationMessageText, name, "\uD83C\uDFAC " + messageObject.messageOwner.message);
                                    text[0] = true;
                                } else {
                                    msg = LocaleController.formatString("ChannelMessageGIF", R.string.ChannelMessageGIF, name);
                                }
                            } else {
                                if (!shortMessage && Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                                    msg = LocaleController.formatString("NotificationMessageText", R.string.NotificationMessageText, name, "\uD83D\uDCCE " + messageObject.messageOwner.message);
                                    text[0] = true;
                                } else {
                                    msg = LocaleController.formatString("ChannelMessageDocument", R.string.ChannelMessageDocument, name);
                                }
                            }
                        } else {
                            if (!shortMessage && !TextUtils.isEmpty(messageObject.messageText)) {
                                msg = LocaleController.formatString("NotificationMessageText", R.string.NotificationMessageText, name, messageObject.messageText);
                                text[0] = true;
                            } else {
                                msg = LocaleController.formatString("ChannelMessageNoText", R.string.ChannelMessageNoText, name);
                            }
                        }
                    } else {
                        if (messageObject.isMediaEmpty()) {
                            if (!shortMessage && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                                msg = LocaleController.formatString("NotificationMessageGroupText", R.string.NotificationMessageGroupText, name, chat.title, messageObject.messageOwner.message);
                            } else {
                                msg = LocaleController.formatString("NotificationMessageGroupNoText", R.string.NotificationMessageGroupNoText, name, chat.title);
                            }
                        } else if (messageObject.messageOwner.media.isPhoto()) {
                            if (!shortMessage && Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                                msg = LocaleController.formatString("NotificationMessageGroupText", R.string.NotificationMessageGroupText, name, chat.title, "\uD83D\uDDBC " + messageObject.messageOwner.message);
                            } else {
                                msg = LocaleController.formatString("NotificationMessageGroupPhoto", R.string.NotificationMessageGroupPhoto, name, chat.title);
                            }
                        } else if (messageObject.isVideo()) {
                            if (!shortMessage && Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                                msg = LocaleController.formatString("NotificationMessageGroupText", R.string.NotificationMessageGroupText, name, chat.title, "\uD83D\uDCF9 " + messageObject.messageOwner.message);
                            } else {
                                msg = LocaleController.formatString(" ", R.string.NotificationMessageGroupVideo, name, chat.title);
                            }
                        } else if (messageObject.isVoice()) {
                            msg = LocaleController.formatString("NotificationMessageGroupAudio", R.string.NotificationMessageGroupAudio, name, chat.title);
                        } else if (messageObject.isRoundVideo()) {
                            msg = LocaleController.formatString("NotificationMessageGroupRound", R.string.NotificationMessageGroupRound, name, chat.title);
                        } else if (messageObject.isMusic()) {
                            msg = LocaleController.formatString("NotificationMessageGroupMusic", R.string.NotificationMessageGroupMusic, name, chat.title);
                        } else if (messageObject.messageOwner.media.isContact()) {
                            MessageMedia mediaContact = (MessageMedia) messageObject.messageOwner.media;
                            msg = LocaleController.formatString("NotificationMessageGroupContact2", R.string.NotificationMessageGroupContact2, name, chat.title, UserObject.formatName(mediaContact.first_name, mediaContact.last_name));
                        } else if (messageObject.messageOwner.media.isGeo() || messageObject.messageOwner.media.isVenue()) {
                            msg = LocaleController.formatString("NotificationMessageGroupMap", R.string.NotificationMessageGroupMap, name, chat.title);
                        } else if (messageObject.messageOwner.media.isGeoLive()) {
                            msg = LocaleController.formatString("NotificationMessageGroupLiveLocation", R.string.NotificationMessageGroupLiveLocation, name, chat.title);
                        } else if (messageObject.messageOwner.media.isDocument()) {
                            if (messageObject.isSticker() || messageObject.isAnimatedSticker()) {
                                String emoji = messageObject.getStickerEmoji();
                                if (emoji != null) {
                                    msg = LocaleController.formatString("NotificationMessageGroupStickerEmoji", R.string.NotificationMessageGroupStickerEmoji, name, chat.title, emoji);
                                } else {
                                    msg = LocaleController.formatString("NotificationMessageGroupSticker", R.string.NotificationMessageGroupSticker, name, chat.title);
                                }
                            } else if (messageObject.isGif()) {
                                if (!shortMessage && Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                                    msg = LocaleController.formatString("NotificationMessageGroupText", R.string.NotificationMessageGroupText, name, chat.title, "\uD83C\uDFAC " + messageObject.messageOwner.message);
                                } else {
                                    msg = LocaleController.formatString("NotificationMessageGroupGif", R.string.NotificationMessageGroupGif, name, chat.title);
                                }
                            } else {
                                if (!shortMessage && Build.VERSION.SDK_INT >= 19 && !TextUtils.isEmpty(messageObject.messageOwner.message)) {
                                    msg = LocaleController.formatString("NotificationMessageGroupText", R.string.NotificationMessageGroupText, name, chat.title, "\uD83D\uDCCE " + messageObject.messageOwner.message);
                                } else {
                                    msg = LocaleController.formatString("NotificationMessageGroupDocument", R.string.NotificationMessageGroupDocument, name, chat.title);
                                }
                            }
                        } else {
                            if (!shortMessage && !TextUtils.isEmpty(messageObject.messageText)) {
                                msg = LocaleController.formatString("NotificationMessageGroupText", R.string.NotificationMessageGroupText, name, chat.title, messageObject.messageText);
                            } else {
                                msg = LocaleController.formatString("NotificationMessageGroupNoText", R.string.NotificationMessageGroupNoText, name, chat.title);
                            }
                        }
                    }
                } else {
                    if (preview != null) {
                        preview[0] = false;
                    }
                    if (ChatObject.isChannel(chat) && !chat.megagroup) {
                        msg = LocaleController.formatString("ChannelMessageNoText", R.string.ChannelMessageNoText, name);
                    } else {
                        msg = LocaleController.formatString("NotificationMessageGroupNoText", R.string.NotificationMessageGroupNoText, name, chat.title);
                    }
                }
            }
        }
        return msg;
    }

    private void scheduleNotificationRepeat() {
        try {
            Intent intent = new Intent(ApplicationLoader.applicationContext, NotificationRepeat.class);
            intent.putExtra("currentAccount", currentAccount);
            PendingIntent pintent = PendingIntent.getService(ApplicationLoader.applicationContext, 0, intent, 0);
            SharedPreferences preferences = getAccountInstance().getNotificationsSettings();
            int minutes = preferences.getInt("repeat_messages", 60);
            if (minutes > 0 && personal_count > 0) {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + minutes * 60 * 1000, pintent);
            } else {
                alarmManager.cancel(pintent);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private boolean isPersonalMessage(MessageObject messageObject) {
        return messageObject.messageOwner.to_id != 0 && messageObject.messageOwner.to_id == 0 && messageObject.messageOwner.to_id == 0
                && (messageObject.messageOwner.action == null || messageObject.messageOwner.action.isEmpty());
    }

    private int getNotifyOverride(SharedPreferences preferences, long dialog_id) {
        int notifyOverride = preferences.getInt("notify2_" + dialog_id, -1);
        if (notifyOverride == 3) {
            int muteUntil = preferences.getInt("notifyuntil_" + dialog_id, 0);
            if (muteUntil >= getConnectionsManager().getCurrentTime()) {
                notifyOverride = 2;
            }
        }
        /*if (BuildVars.LOGS_ENABLED && BuildVars.DEBUG_VERSION) {
            FileLog.d("notify override for " + dialog_id + " = " + notifyOverride);
        }*/
        return notifyOverride;
    }

    public void showNotifications() {
        notificationsQueue.postRunnable(() -> showOrUpdateNotification(false));
    }

    public void hideNotifications() {
        notificationsQueue.postRunnable(() -> {
            notificationManager.cancel(notificationId);
            lastWearNotifiedMessageId.clear();
            for (int a = 0; a < wearNotificationsIds.size(); a++) {
                notificationManager.cancel(wearNotificationsIds.valueAt(a));
            }
            wearNotificationsIds.clear();
        });
    }

    private void dismissNotification() {
        try {
            notificationManager.cancel(notificationId);
            pushMessages.clear();
            pushMessagesDict.clear();
            lastWearNotifiedMessageId.clear();
            for (int a = 0; a < wearNotificationsIds.size(); a++) {
                notificationManager.cancel(wearNotificationsIds.valueAt(a));
            }
            wearNotificationsIds.clear();
            AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.pushMessagesUpdated));
//            if (WearDataLayerListenerService.isWatchConnected()) {
//                try {
//                    JSONObject o = new JSONObject();
//                    o.put("id", getUserConfig().getClientUserId());
//                    o.put("cancel_all", true);
//                    WearDataLayerListenerService.sendMessageToWatch("/notify", o.toString().getBytes(), "remote_notifications");
//                } catch (JSONException ignore) {
//                }
//            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void playInChatSound() {
        if (!inChatSoundEnabled || MediaController.getInstance().isRecordingAudio()) {
            return;
        }
        try {
            if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
                return;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }

        try {
            SharedPreferences preferences = getAccountInstance().getNotificationsSettings();
            int notifyOverride = getNotifyOverride(preferences, opened_dialog_id);
            if (notifyOverride == 2) {
                return;
            }
            notificationsQueue.postRunnable(() -> {
                if (Math.abs(System.currentTimeMillis() - lastSoundPlay) <= 500) {
                    return;
                }
                try {
                    if (soundPool == null) {
                        soundPool = new SoundPool(3, AudioManager.STREAM_SYSTEM, 0);
                        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
                            if (status == 0) {
                                try {
                                    soundPool.play(sampleId, 1.0f, 1.0f, 1, 0, 1.0f);
                                } catch (Exception e) {
                                    FileLog.e(e);
                                }
                            }
                        });
                    }
                    if (soundIn == 0 && !soundInLoaded) {
                        soundInLoaded = true;
                        soundIn = soundPool.load(ApplicationLoader.applicationContext, R.raw.sound_in, 1);
                    }
                    if (soundIn != 0) {
                        try {
                            soundPool.play(soundIn, 1.0f, 1.0f, 1, 0, 1.0f);
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void scheduleNotificationDelay(boolean onlineReason) {
        try {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("delay notification start, onlineReason = " + onlineReason);
            }
            notificationDelayWakelock.acquire(10000);
            notificationsQueue.cancelRunnable(notificationDelayRunnable);
            notificationsQueue.postRunnable(notificationDelayRunnable, (onlineReason ? 3 * 1000 : 1000));
        } catch (Exception e) {
            FileLog.e(e);
            showOrUpdateNotification(notifyCheck);
        }
    }

    public void repeatNotificationMaybe() {
        notificationsQueue.postRunnable(() -> {
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            if (hour >= 11 && hour <= 22) {
                notificationManager.cancel(notificationId);
                showOrUpdateNotification(true);
            } else {
                scheduleNotificationRepeat();
            }
        });
    }

    private boolean isEmptyVibration(long[] pattern) {
        if (pattern == null || pattern.length == 0) {
            return false;
        }
        for (int a = 0; a < pattern.length; a++) {
            if (pattern[a] != 0) {
                return false;
            }
        }
        return true;
    }

    @TargetApi(26)
    public void deleteNotificationChannel(long dialogId) {
        notificationsQueue.postRunnable(() -> {
            if (Build.VERSION.SDK_INT < 26) {
                return;
            }
            try {
                SharedPreferences preferences = getAccountInstance().getNotificationsSettings();
                String key = "org.telegram.key" + dialogId;
                String channelId = preferences.getString(key, null);
                if (channelId != null) {
                    preferences.edit().remove(key).remove(key + "_s").commit();
                    systemNotificationManager.deleteNotificationChannel(channelId);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    @TargetApi(26)
    public void deleteAllNotificationChannels() {
        notificationsQueue.postRunnable(() -> {
            if (Build.VERSION.SDK_INT < 26) {
                return;
            }
            try {
                SharedPreferences preferences = getAccountInstance().getNotificationsSettings();
                Map<String, ?> values = preferences.getAll();
                SharedPreferences.Editor editor = preferences.edit();
                for (Map.Entry<String, ?> entry : values.entrySet()) {
                    String key = entry.getKey();
                    if (key.startsWith("org.telegram.key")) {
                        if (!key.endsWith("_s")) {
                            systemNotificationManager.deleteNotificationChannel((String) entry.getValue());
                        }
                        editor.remove(key);
                    }
                }
                editor.commit();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    @TargetApi(26)
    private String validateChannelId(long dialogId, String name, long[] vibrationPattern, int ledColor, Uri sound, int importance, long[] configVibrationPattern, Uri configSound, int configImportance) {
        SharedPreferences preferences = getAccountInstance().getNotificationsSettings();
        String key = "org.telegram.key" + dialogId;
        String channelId = preferences.getString(key, null);
        String settings = preferences.getString(key + "_s", null);
        boolean edited = false;
        StringBuilder newSettings = new StringBuilder();
        String newSettingsHash;

        /*NotificationChannel existingChannel = systemNotificationManager.getNotificationChannel(channelId);
        if (existingChannel != null) {
            int channelImportance = existingChannel.getImportance();
            Uri channelSound = existingChannel.getSound();
            long[] channelVibrationPattern = existingChannel.getVibrationPattern();
            int channelLedColor = existingChannel.getLightColor();
            if (channelVibrationPattern != null) {
                for (int a = 0; a < channelVibrationPattern.length; a++) {
                    newSettings.append(channelVibrationPattern[a]);
                }
            }
            newSettings.append(channelLedColor);
            if (channelSound != null) {
                newSettings.append(channelSound.toString());
            }
            newSettings.append(channelImportance);
            newSettingsHash = Utilities.MD5(newSettings.toString());
            newSettings.setLength(0);
            if (!settings.equals(newSettingsHash)) {
                SharedPreferences.Editor editor = null;
                if (channelImportance != configImportance) {
                    if (editor == null) {
                        editor = preferences.edit();
                    }
                    int priority;
                    if (channelImportance == NotificationManager.IMPORTANCE_HIGH || channelImportance == NotificationManager.IMPORTANCE_MAX) {
                        priority = 1;
                    } else if (channelImportance == NotificationManager.IMPORTANCE_MIN) {
                        priority = 4;
                    } else if (channelImportance == NotificationManager.IMPORTANCE_LOW) {
                        priority = 5;
                    } else {
                        priority = 0;
                    }
                    editor.putInt("priority_" + dialogId, priority);
                    if (configImportance == importance) {
                        importance = channelImportance;
                        edited = true;
                    }
                }
                if (configSound == null || channelSound != null || configSound != null && channelSound == null || !configSound.equals(channelSound)) {
                    if (editor == null) {
                        editor = preferences.edit();
                    }
                    String newSound;
                    if (channelSound == null) {
                        newSound = "NoSound";
                        editor.putString("sound_" + dialogId, "NoSound");
                    } else {
                        newSound = channelSound.toString();
                        Ringtone rng = RingtoneManager.getRingtone(ApplicationLoader.applicationContext, channelSound);
                        String ringtoneName = null;
                        if (rng != null) {
                            if (channelSound.equals(Settings.System.DEFAULT_RINGTONE_URI)) {
                                ringtoneName = LocaleController.getString("DefaultRingtone", R.string.DefaultRingtone);
                            } else {
                                ringtoneName = rng.getTitle(ApplicationLoader.applicationContext);
                            }
                            rng.stop();
                        }
                        if (ringtoneName != null) {
                            editor.putString("sound_" + dialogId, ringtoneName);
                        }
                    }
                    editor.putString("sound_path_" + dialogId, newSound);
                    if (configSound == null && sound == null || configSound != null && sound != null || configSound.equals(sound)) {
                        sound = channelSound;
                        edited = true;
                    }
                }
                boolean vibrate = existingChannel.shouldVibrate();
                if (isEmptyVibration(configVibrationPattern) != vibrate) {
                    if (editor == null) {
                        editor = preferences.edit();
                    }
                    editor.putInt("vibrate_" + dialogId, vibrate ? 0 : 2);
                }
                if (editor != null) {
                    editor.putBoolean("custom_" + dialogId, true);
                    editor.commit();
                }
            }
        }*/

        boolean secretChat = (int) dialogId == 0;
        for (int a = 0; a < vibrationPattern.length; a++) {
            newSettings.append(vibrationPattern[a]);
        }
        newSettings.append(ledColor);
        if (sound != null) {
            newSettings.append(sound.toString());
        }
        newSettings.append(importance);
        if (secretChat) {
            newSettings.append("secret");
        }

        newSettingsHash = Utilities.MD5(newSettings.toString());
        if (channelId != null && !settings.equals(newSettingsHash)) {
            if (edited) {
                preferences.edit().putString(key, channelId).putString(key + "_s", newSettingsHash).commit();
            } else {
                systemNotificationManager.deleteNotificationChannel(channelId);
                channelId = null;
            }
        }
        if (channelId == null) {
            channelId = currentAccount + "channel" + dialogId + "_" + Utilities.random.nextLong();
            NotificationChannel notificationChannel = new NotificationChannel(channelId, secretChat ? LocaleController.getString("SecretChatName", R.string.SecretChatName) : name, importance);
            if (ledColor != 0) {
                notificationChannel.enableLights(true);
                notificationChannel.setLightColor(ledColor);
            }
            if (!isEmptyVibration(vibrationPattern)) {
                notificationChannel.enableVibration(true);
                if (vibrationPattern != null && vibrationPattern.length > 0) {
                    notificationChannel.setVibrationPattern(vibrationPattern);
                }
            } else {
                notificationChannel.enableVibration(false);
            }
            AudioAttributes.Builder builder = new AudioAttributes.Builder();
            builder.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
            builder.setUsage(AudioAttributes.USAGE_NOTIFICATION);
            if (sound != null) {
                notificationChannel.setSound(sound, builder.build());
            } else {
                notificationChannel.setSound(null, builder.build());
            }
            systemNotificationManager.createNotificationChannel(notificationChannel);
            preferences.edit().putString(key, channelId).putString(key + "_s", newSettingsHash).commit();
        }
        return channelId;
    }

    private void showOrUpdateNotification(boolean notifyAboutLast) {
        if (!getUserConfig().isClientActivated() || pushMessages.isEmpty() || !SharedConfig.showNotificationsForAllAccounts && currentAccount != UserConfig.selectedAccount) {
            dismissNotification();
            return;
        }
        try {
//            getConnectionsManager().resumeNetworkMaybe();

            MessageObject lastMessageObject = pushMessages.get(0);
            SharedPreferences preferences = getAccountInstance().getNotificationsSettings();
            int dismissDate = preferences.getInt("dismissDate", 0);
            if (lastMessageObject.messageOwner.date <= dismissDate) {
                dismissNotification();
                return;
            }

            long dialog_id = lastMessageObject.getDialogId();
            boolean isChannel = false;
            long override_dialog_id = dialog_id;
            if (lastMessageObject.messageOwner.mentioned) {
                override_dialog_id = lastMessageObject.messageOwner.from_id;
            }
            int mid = lastMessageObject.getId();
            int chat_id = lastMessageObject.messageOwner.to_id != 0 ? lastMessageObject.messageOwner.to_id : lastMessageObject.messageOwner.to_id;
            int user_id = lastMessageObject.messageOwner.to_id;
            if (user_id == 0) {
                user_id = lastMessageObject.messageOwner.from_id;
            } else if (user_id == getUserConfig().getClientUserId()) {
                user_id = lastMessageObject.messageOwner.from_id;
            }

            User user = getMessagesController().getUser(user_id);
            Chat chat = null;
            if (chat_id != 0) {
                chat = getMessagesController().getChat(chat_id);
                if (chat == null && lastMessageObject.isFcmMessage()) {
                    isChannel = lastMessageObject.localChannel;
                } else {
                    isChannel = ChatObject.isChannel(chat) && !chat.megagroup;
                }
            }
            FileLocation photoPath = null;

            boolean notifyDisabled = false;
            int needVibrate = 0;
            String choosenSoundPath;
            int ledColor = 0xff0000ff;
            int priority = 0;

            int notifyOverride = getNotifyOverride(preferences, override_dialog_id);
            boolean value;
            if (notifyOverride == -1) {
                value = isGlobalNotificationsEnabled(dialog_id, isChannel);
            } else {
                value = notifyOverride != 2;
            }
            if (!notifyAboutLast || !value) {
                notifyDisabled = true;
            }

            if (!notifyDisabled && dialog_id == override_dialog_id && chat != null) {
                int notifyMaxCount;
                int notifyDelay;
                if (preferences.getBoolean("custom_" + dialog_id, false)) {
                    notifyMaxCount = preferences.getInt("smart_max_count_" + dialog_id, 2);
                    notifyDelay = preferences.getInt("smart_delay_" + dialog_id, 3 * 60);
                } else {
                    notifyMaxCount = 2;
                    notifyDelay = 3 * 60;
                }
                if (notifyMaxCount != 0) {
                    Point dialogInfo = smartNotificationsDialogs.get(dialog_id);
                    if (dialogInfo == null) {
                        dialogInfo = new Point(1, (int) (System.currentTimeMillis() / 1000));
                        smartNotificationsDialogs.put(dialog_id, dialogInfo);
                    } else {
                        int lastTime = dialogInfo.y;
                        if (lastTime + notifyDelay < System.currentTimeMillis() / 1000) {
                            dialogInfo.set(1, (int) (System.currentTimeMillis() / 1000));
                        } else {
                            int count = dialogInfo.x;
                            if (count < notifyMaxCount) {
                                dialogInfo.set(count + 1, (int) (System.currentTimeMillis() / 1000));
                            } else {
                                notifyDisabled = true;
                            }
                        }
                    }
                }
            }

            String defaultPath = Settings.System.DEFAULT_NOTIFICATION_URI.getPath();

            boolean inAppSounds = preferences.getBoolean("EnableInAppSounds", true);
            boolean inAppVibrate = preferences.getBoolean("EnableInAppVibrate", true);
            boolean inAppPreview = preferences.getBoolean("EnableInAppPreview", true);
            boolean inAppPriority = preferences.getBoolean("EnableInAppPriority", false);
            boolean custom;
            int vibrateOverride;
            int priorityOverride;
            if (custom = preferences.getBoolean("custom_" + dialog_id, false)) {
                vibrateOverride = preferences.getInt("vibrate_" + dialog_id, 0);
                priorityOverride = preferences.getInt("priority_" + dialog_id, 3);
                choosenSoundPath = preferences.getString("sound_path_" + dialog_id, null);
            } else {
                vibrateOverride = 0;
                priorityOverride = 3;
                choosenSoundPath = null;
            }
            boolean vibrateOnlyIfSilent = false;

            if (chat_id != 0) {
                if (isChannel) {
                    if (choosenSoundPath != null && choosenSoundPath.equals(defaultPath)) {
                        choosenSoundPath = null;
                    } else if (choosenSoundPath == null) {
                        choosenSoundPath = preferences.getString("ChannelSoundPath", defaultPath);
                    }
                    needVibrate = preferences.getInt("vibrate_channel", 0);
                    priority = preferences.getInt("priority_channel", 1);
                    ledColor = preferences.getInt("ChannelLed", 0xff0000ff);
                } else {
                    if (choosenSoundPath != null && choosenSoundPath.equals(defaultPath)) {
                        choosenSoundPath = null;
                    } else if (choosenSoundPath == null) {
                        choosenSoundPath = preferences.getString("GroupSoundPath", defaultPath);
                    }
                    needVibrate = preferences.getInt("vibrate_group", 0);
                    priority = preferences.getInt("priority_group", 1);
                    ledColor = preferences.getInt("GroupLed", 0xff0000ff);
                }
            } else if (user_id != 0) {
                if (choosenSoundPath != null && choosenSoundPath.equals(defaultPath)) {
                    choosenSoundPath = null;
                } else if (choosenSoundPath == null) {
                    choosenSoundPath = preferences.getString("GlobalSoundPath", defaultPath);
                }
                needVibrate = preferences.getInt("vibrate_messages", 0);
                priority = preferences.getInt("priority_messages", 1);
                ledColor = preferences.getInt("MessagesLed", 0xff0000ff);
            }
            if (custom) {
                if (preferences.contains("color_" + dialog_id)) {
                    ledColor = preferences.getInt("color_" + dialog_id, 0);
                }
            }

            if (priorityOverride != 3) {
                priority = priorityOverride;
            }

            if (needVibrate == 4) {
                vibrateOnlyIfSilent = true;
                needVibrate = 0;
            }
            if (needVibrate == 2 && (vibrateOverride == 1 || vibrateOverride == 3) || needVibrate != 2 && vibrateOverride == 2 || vibrateOverride != 0 && vibrateOverride != 4) {
                needVibrate = vibrateOverride;
            }
            if (!ApplicationLoader.mainInterfacePaused) {
                if (!inAppSounds) {
                    choosenSoundPath = null;
                }
                if (!inAppVibrate) {
                    needVibrate = 2;
                }
                if (!inAppPriority) {
                    priority = 0;
                } else if (priority == 2) {
                    priority = 1;
                }
            }
            if (vibrateOnlyIfSilent && needVibrate != 2) {
                try {
                    int mode = audioManager.getRingerMode();
                    if (mode != AudioManager.RINGER_MODE_SILENT && mode != AudioManager.RINGER_MODE_VIBRATE) {
                        needVibrate = 2;
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }

            Uri configSound = null;
            long[] configVibrationPattern = null;
            int configImportance = 0;
            if (Build.VERSION.SDK_INT >= 26) {
                if (needVibrate == 2) {
                    configVibrationPattern = new long[]{0, 0};
                } else if (needVibrate == 1) {
                    configVibrationPattern = new long[]{0, 100, 0, 100};
                } else if (needVibrate == 0 || needVibrate == 4) {
                    configVibrationPattern = new long[]{};
                } else if (needVibrate == 3) {
                    configVibrationPattern = new long[]{0, 1000};
                }
                if (choosenSoundPath != null && !choosenSoundPath.equals("NoSound")) {
                    if (choosenSoundPath.equals(defaultPath)) {
                        configSound = Settings.System.DEFAULT_NOTIFICATION_URI;
                    } else {
                        configSound = Uri.parse(choosenSoundPath);
                    }
                }
                if (priority == 0) {
                    configImportance = NotificationManager.IMPORTANCE_DEFAULT;
                } else if (priority == 1 || priority == 2) {
                    configImportance = NotificationManager.IMPORTANCE_HIGH;
                } else if (priority == 4) {
                    configImportance = NotificationManager.IMPORTANCE_MIN;
                } else if (priority == 5) {
                    configImportance = NotificationManager.IMPORTANCE_LOW;
                }
            }

            if (notifyDisabled) {
                needVibrate = 0;
                priority = 0;
                ledColor = 0;
                choosenSoundPath = null;
            }

            Intent intent = new Intent(ApplicationLoader.applicationContext, LaunchActivity.class);
            intent.setAction("com.tmessages.openchat" + Math.random() + Integer.MAX_VALUE);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            if ((int) dialog_id != 0) {
                if (pushDialogs.size() == 1) {
                    if (chat_id != 0) {
                        intent.putExtra("chatId", chat_id);
                    } else if (user_id != 0) {
                        intent.putExtra("userId", user_id);
                    }
                }
                if (AndroidUtilities.needShowPasscode() || SharedConfig.isWaitingForPasscodeEnter) {
                    photoPath = null;
                } else {
                    if (pushDialogs.size() == 1 && Build.VERSION.SDK_INT < 28) {
                        if (chat != null) {
                            if (chat.photo != null && chat.photo.photo_small != null && chat.photo.photo_small.volume_id != 0 && chat.photo.photo_small.local_id != 0) {
                                photoPath = chat.photo.photo_small;
                            }
                        } else if (user != null) {
                            if (user.photo != null && user.photo.photo_small != null && user.photo.photo_small.volume_id != 0 && user.photo.photo_small.local_id != 0) {
                                photoPath = user.photo.photo_small;
                            }
                        }
                    }
                }
            } else {
                if (pushDialogs.size() == 1 && dialog_id != globalSecretChatId) {
                    intent.putExtra("encId", (int) (dialog_id >> 32));
                }
            }
            intent.putExtra("currentAccount", currentAccount);
            PendingIntent contentIntent = PendingIntent.getActivity(ApplicationLoader.applicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);

            String name;
            String chatName;
            boolean replace = true;
            if (((chat_id != 0 && chat == null) || user == null) && lastMessageObject.isFcmMessage()) {
                chatName = lastMessageObject.localName;
            } else if (chat != null) {
                chatName = chat.title;
            } else {
                chatName = UserObject.getUserName(user);
            }
            boolean passcode = AndroidUtilities.needShowPasscode() || SharedConfig.isWaitingForPasscodeEnter;
            if ((int) dialog_id == 0 || pushDialogs.size() > 1 || passcode) {
                if (passcode) {
                    if (chat_id != 0) {
                        name = LocaleController.getString("NotificationHiddenChatName", R.string.NotificationHiddenChatName);
                    } else {
                        name = LocaleController.getString("NotificationHiddenName", R.string.NotificationHiddenName);
                    }
                } else {
                    name = LocaleController.getString("AppName", R.string.AppName);
                }
                replace = false;
            } else {
                name = chatName;
            }

            String detailText;
            if (UserConfig.getActivatedAccountsCount() > 1) {
                if (pushDialogs.size() == 1) {
                    detailText = UserObject.getFirstName(getUserConfig().getCurrentUser());
                } else {
                    detailText = UserObject.getFirstName(getUserConfig().getCurrentUser()) + "・";
                }
            } else {
                detailText = "";
            }
            if (pushDialogs.size() != 1 || Build.VERSION.SDK_INT < 23) {
                if (pushDialogs.size() == 1) {
                    detailText += LocaleController.formatPluralString("NewMessages", total_unread_count);
                } else {
                    detailText += LocaleController.formatString("NotificationMessagesPeopleDisplayOrder", R.string.NotificationMessagesPeopleDisplayOrder, LocaleController.formatPluralString("NewMessages", total_unread_count), LocaleController.formatPluralString("FromChats", pushDialogs.size()));
                }
            }

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ApplicationLoader.applicationContext)
                    .setContentTitle(name)
                    .setSmallIcon(R.drawable.notification)
                    .setAutoCancel(true)
                    .setNumber(total_unread_count)
                    .setContentIntent(contentIntent)
                    .setGroup(notificationGroup)
                    .setGroupSummary(true)
                    .setShowWhen(true)
                    .setWhen(((long) lastMessageObject.messageOwner.date) * 1000)
                    .setColor(0xff11acfa);

            long[] vibrationPattern = null;
            int importance = 0;
            Uri sound = null;

            mBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
            if (chat == null && user != null && user.phone != null && user.phone.length() > 0) {
                mBuilder.addPerson("tel:+" + user.phone);
            }

            int silent = 2;
            String lastMessage = null;
            boolean hasNewMessages = false;
            if (pushMessages.size() == 1) {
                MessageObject messageObject = pushMessages.get(0);
                boolean[] text = new boolean[1];
                String message = lastMessage = getStringForMessage(messageObject, false, text, null);
                silent = messageObject.messageOwner.silent ? 1 : 0;
                if (message == null) {
                    return;
                }
                if (replace) {
                    if (chat != null) {
                        message = message.replace(" @ " + name, "");
                    } else {
                        if (text[0]) {
                            message = message.replace(name + ": ", "");
                        } else {
                            message = message.replace(name + " ", "");
                        }
                    }
                }
                mBuilder.setContentText(message);
                mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
            } else {
                mBuilder.setContentText(detailText);
                NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                inboxStyle.setBigContentTitle(name);
                int count = Math.min(10, pushMessages.size());
                boolean[] text = new boolean[1];
                for (int i = 0; i < count; i++) {
                    MessageObject messageObject = pushMessages.get(i);
                    String message = getStringForMessage(messageObject, false, text, null);
                    if (message == null || messageObject.messageOwner.date <= dismissDate) {
                        continue;
                    }
                    if (silent == 2) {
                        lastMessage = message;
                        silent = messageObject.messageOwner.silent ? 1 : 0;
                    }
                    if (pushDialogs.size() == 1) {
                        if (replace) {
                            if (chat != null) {
                                message = message.replace(" @ " + name, "");
                            } else {
                                if (text[0]) {
                                    message = message.replace(name + ": ", "");
                                } else {
                                    message = message.replace(name + " ", "");
                                }
                            }
                        }
                    }
                    inboxStyle.addLine(message);
                }
                inboxStyle.setSummaryText(detailText);
                mBuilder.setStyle(inboxStyle);
            }

            Intent dismissIntent = new Intent(ApplicationLoader.applicationContext, NotificationDismissReceiver.class);
            dismissIntent.putExtra("messageDate", lastMessageObject.messageOwner.date);
            dismissIntent.putExtra("currentAccount", currentAccount);
            mBuilder.setDeleteIntent(PendingIntent.getBroadcast(ApplicationLoader.applicationContext, 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));

            if (photoPath != null) {
                BitmapDrawable img = ImageLoader.getInstance().getImageFromMemory(photoPath, null, "50_50");
                if (img != null) {
                    mBuilder.setLargeIcon(img.getBitmap());
                } else {
                    try {
                        File file = FileLoader.getPathToAttach(photoPath, true);
                        if (file.exists()) {
                            float scaleFactor = 160.0f / AndroidUtilities.dp(50);
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inSampleSize = scaleFactor < 1 ? 1 : (int) scaleFactor;
                            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                            if (bitmap != null) {
                                mBuilder.setLargeIcon(bitmap);
                            }
                        }
                    } catch (Throwable ignore) {

                    }
                }
            }

            if (!notifyAboutLast || silent == 1) {
                mBuilder.setPriority(NotificationCompat.PRIORITY_LOW);
                if (Build.VERSION.SDK_INT >= 26) {
                    importance = NotificationManager.IMPORTANCE_LOW;
                }
            } else {
                if (priority == 0) {
                    mBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                    if (Build.VERSION.SDK_INT >= 26) {
                        importance = NotificationManager.IMPORTANCE_DEFAULT;
                    }
                } else if (priority == 1 || priority == 2) {
                    mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
                    if (Build.VERSION.SDK_INT >= 26) {
                        importance = NotificationManager.IMPORTANCE_HIGH;
                    }
                } else if (priority == 4) {
                    mBuilder.setPriority(NotificationCompat.PRIORITY_MIN);
                    if (Build.VERSION.SDK_INT >= 26) {
                        importance = NotificationManager.IMPORTANCE_MIN;
                    }
                } else if (priority == 5) {
                    mBuilder.setPriority(NotificationCompat.PRIORITY_LOW);
                    if (Build.VERSION.SDK_INT >= 26) {
                        importance = NotificationManager.IMPORTANCE_LOW;
                    }
                }
            }

            if (silent != 1 && !notifyDisabled) {
                if (ApplicationLoader.mainInterfacePaused || inAppPreview) {
                    if (lastMessage.length() > 100) {
                        lastMessage = lastMessage.substring(0, 100).replace('\n', ' ').trim() + "...";
                    }
                    mBuilder.setTicker(lastMessage);
                }
                if (!MediaController.getInstance().isRecordingAudio()) {
                    if (choosenSoundPath != null && !choosenSoundPath.equals("NoSound")) {
                        if (Build.VERSION.SDK_INT >= 26) {
                            if (choosenSoundPath.equals(defaultPath)) {
                                sound = Settings.System.DEFAULT_NOTIFICATION_URI;
                            } else {
                                sound = Uri.parse(choosenSoundPath);
                            }
                        } else {
                            if (choosenSoundPath.equals(defaultPath)) {
                                mBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, AudioManager.STREAM_NOTIFICATION);
                            } else {
                                if (Build.VERSION.SDK_INT >= 24 && choosenSoundPath.startsWith("file://") && !AndroidUtilities.isInternalUri(Uri.parse(choosenSoundPath))) {
                                    try {
                                        Uri uri = FileProvider.getUriForFile(ApplicationLoader.applicationContext, BuildConfig.APPLICATION_ID + ".provider", new File(choosenSoundPath.replace("file://", "")));
                                        ApplicationLoader.applicationContext.grantUriPermission("com.android.systemui", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        mBuilder.setSound(uri, AudioManager.STREAM_NOTIFICATION);
                                    } catch (Exception e) {
                                        mBuilder.setSound(Uri.parse(choosenSoundPath), AudioManager.STREAM_NOTIFICATION);
                                    }
                                } else {
                                    mBuilder.setSound(Uri.parse(choosenSoundPath), AudioManager.STREAM_NOTIFICATION);
                                }
                            }
                        }
                    }
                }
                if (ledColor != 0) {
                    mBuilder.setLights(ledColor, 1000, 1000);
                }
                if (needVibrate == 2 || MediaController.getInstance().isRecordingAudio()) {
                    mBuilder.setVibrate(vibrationPattern = new long[]{0, 0});
                } else if (needVibrate == 1) {
                    mBuilder.setVibrate(vibrationPattern = new long[]{0, 100, 0, 100});
                } else if (needVibrate == 0 || needVibrate == 4) {
                    mBuilder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
                    vibrationPattern = new long[]{};
                } else if (needVibrate == 3) {
                    mBuilder.setVibrate(vibrationPattern = new long[]{0, 1000});
                }
            } else {
                mBuilder.setVibrate(vibrationPattern = new long[]{0, 0});
            }

            boolean hasCallback = false;
            if (!AndroidUtilities.needShowPasscode() && !SharedConfig.isWaitingForPasscodeEnter && lastMessageObject.getDialogId() == 777000) {
                if (lastMessageObject.messageOwner.reply_markup != null) {
                    ArrayList<ReplyMarkup.KeyboardButtonRow> rows = lastMessageObject.messageOwner.reply_markup.rows;
                    for (int a = 0, size = rows.size(); a < size; a++) {
                        ReplyMarkup.KeyboardButtonRow row = rows.get(a);
                        for (int b = 0, size2 = row.buttons.size(); b < size2; b++) {
                            KeyboardButton button = row.buttons.get(b);
                            if (button.isButtonCallback()) {
                                Intent callbackIntent = new Intent(ApplicationLoader.applicationContext, NotificationCallbackReceiver.class);
                                callbackIntent.putExtra("currentAccount", currentAccount);
                                callbackIntent.putExtra("did", dialog_id);
                                if (button.data != null) {
                                    callbackIntent.putExtra("data", button.data);
                                }
                                callbackIntent.putExtra("mid", lastMessageObject.getId());
                                mBuilder.addAction(0, button.text, PendingIntent.getBroadcast(ApplicationLoader.applicationContext, lastButtonId++, callbackIntent, PendingIntent.FLAG_UPDATE_CURRENT));
                                hasCallback = true;
                            }
                        }
                    }
                }
            }

            if (!hasCallback && Build.VERSION.SDK_INT < 24 && SharedConfig.passcodeHash.length() == 0 && hasMessagesToReply()) {
                Intent replyIntent = new Intent(ApplicationLoader.applicationContext, PopupReplyReceiver.class);
                replyIntent.putExtra("currentAccount", currentAccount);
                if (Build.VERSION.SDK_INT <= 19) {
                    mBuilder.addAction(R.drawable.ic_ab_reply2, LocaleController.getString("Reply", R.string.Reply), PendingIntent.getBroadcast(ApplicationLoader.applicationContext, 2, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT));
                } else {
                    mBuilder.addAction(R.drawable.ic_ab_reply, LocaleController.getString("Reply", R.string.Reply), PendingIntent.getBroadcast(ApplicationLoader.applicationContext, 2, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT));
                }
            }
            if (Build.VERSION.SDK_INT >= 26) {
                mBuilder.setChannelId(validateChannelId(dialog_id, chatName, vibrationPattern, ledColor, sound, importance, configVibrationPattern, configSound, configImportance));
            }
            scheduleNotificationRepeat();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    private void loadRoundAvatar(File avatar, Person.Builder personBuilder) {
        if (avatar != null) {
            try {
                Bitmap bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(avatar), (decoder, info, src) -> decoder.setPostProcessor((canvas) -> {
                    Path path = new Path();
                    path.setFillType(Path.FillType.INVERSE_EVEN_ODD);
                    int width = canvas.getWidth();
                    int height = canvas.getHeight();
                    path.addRoundRect(0, 0, width, height, width / 2, width / 2, Path.Direction.CW);
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setColor(Color.TRANSPARENT);
                    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                    canvas.drawPath(path, paint);
                    return PixelFormat.TRANSLUCENT;
                }));
                IconCompat icon = IconCompat.createWithBitmap(bitmap);
                personBuilder.setIcon(icon);
            } catch (Throwable ignore) {

            }
        }
    }

    public void playOutChatSound() {
        if (!inChatSoundEnabled || MediaController.getInstance().isRecordingAudio()) {
            return;
        }
        try {
            if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
                return;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        notificationsQueue.postRunnable(() -> {
            try {
                if (Math.abs(System.currentTimeMillis() - lastSoundOutPlay) <= 100) {
                    return;
                }
                lastSoundOutPlay = System.currentTimeMillis();
                if (soundPool == null) {
                    soundPool = new SoundPool(3, AudioManager.STREAM_SYSTEM, 0);
                    soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
                        if (status == 0) {
                            try {
                                soundPool.play(sampleId, 1.0f, 1.0f, 1, 0, 1.0f);
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                        }
                    });
                }
                if (soundOut == 0 && !soundOutLoaded) {
                    soundOutLoaded = true;
                    soundOut = soundPool.load(ApplicationLoader.applicationContext, R.raw.sound_out, 1);
                }
                if (soundOut != 0) {
                    try {
                        soundPool.play(soundOut, 1.0f, 1.0f, 1, 0, 1.0f);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public static final int SETTING_MUTE_HOUR = 0;
    public static final int SETTING_MUTE_8_HOURS = 1;
    public static final int SETTING_MUTE_2_DAYS = 2;
    public static final int SETTING_MUTE_FOREVER = 3;
    public static final int SETTING_MUTE_UNMUTE = 4;

    public void setDialogNotificationsSettings(long dialog_id, int setting) {
        SharedPreferences preferences = getAccountInstance().getNotificationsSettings();
        SharedPreferences.Editor editor = preferences.edit();
//        TLRPC.Dialog dialog = MessagesController.getInstance(UserConfig.selectedAccount).dialogs_dict.get(dialog_id);
        if (setting == SETTING_MUTE_UNMUTE) {
            boolean defaultEnabled = isGlobalNotificationsEnabled(dialog_id);
            if (defaultEnabled) {
                editor.remove("notify2_" + dialog_id);
            } else {
                editor.putInt("notify2_" + dialog_id, 0);
            }
            getMessagesStorage().setDialogFlags(dialog_id, 0);
//            if (dialog != null) {
//                dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
//            }
        } else {
            int untilTime = ConnectionsManager.getInstance(UserConfig.selectedAccount).getCurrentTime();
            if (setting == SETTING_MUTE_HOUR) {
                untilTime += 60 * 60;
            } else if (setting == SETTING_MUTE_8_HOURS) {
                untilTime += 60 * 60 * 8;
            } else if (setting == SETTING_MUTE_2_DAYS) {
                untilTime += 60 * 60 * 48;
            } else if (setting == SETTING_MUTE_FOREVER) {
                untilTime = Integer.MAX_VALUE;
            }
            long flags;
            if (setting == SETTING_MUTE_FOREVER) {
                editor.putInt("notify2_" + dialog_id, 2);
                flags = 1;
            } else {
                editor.putInt("notify2_" + dialog_id, 3);
                editor.putInt("notifyuntil_" + dialog_id, untilTime);
                flags = ((long) untilTime << 32) | 1;
            }
            NotificationsController.getInstance(UserConfig.selectedAccount).removeNotificationsForDialog(dialog_id);
            MessagesStorage.getInstance(UserConfig.selectedAccount).setDialogFlags(dialog_id, flags);
//            if (dialog != null) {
//                dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
//                dialog.notify_settings.mute_until = untilTime;
//            }
        }
        editor.commit();
        updateServerNotificationsSettings(dialog_id);
    }

    public void updateServerNotificationsSettings(long dialog_id) {
        updateServerNotificationsSettings(dialog_id, true);
    }

    public void updateServerNotificationsSettings(long dialog_id, boolean post) {
        if (post) {
            getNotificationCenter().postNotificationName(NotificationCenter.notificationsSettingsUpdated);
        }
        if ((int) dialog_id == 0) {
            return;
        }
        SharedPreferences preferences = getAccountInstance().getNotificationsSettings();
        //TODO 发起请求
//        TLRPC.TL_account_updateNotifySettings req = new TLRPC.TL_account_updateNotifySettings();
//        req.settings = new TLRPC.TL_inputPeerNotifySettings();
//
//        req.settings.flags |= 1;
//        req.settings.show_previews = preferences.getBoolean("content_preview_" + dialog_id, true);
//
//        req.settings.flags |= 2;
//        req.settings.silent = preferences.getBoolean("silent_" + dialog_id, false);
//
//        int mute_type = preferences.getInt("notify2_" + dialog_id, -1);
//        if (mute_type != -1) {
//            req.settings.flags |= 4;
//            if (mute_type == 3) {
//                req.settings.mute_until = preferences.getInt("notifyuntil_" + dialog_id, 0);
//            } else {
//                req.settings.mute_until = mute_type != 2 ? 0 : Integer.MAX_VALUE;
//            }
//        }
//
//        req.peer = new TLRPC.TL_inputNotifyPeer();
//        ((TLRPC.TL_inputNotifyPeer) req.peer).peer = getMessagesController().getInputPeer((int) dialog_id);
//        getConnectionsManager().sendRequest(req, (response, error) -> {
//
//        });
    }

    public final static int TYPE_GROUP = 0;
    public final static int TYPE_PRIVATE = 1;
    public final static int TYPE_CHANNEL = 2;

    public void updateServerNotificationsSettings(int type) {
        SharedPreferences preferences = getAccountInstance().getNotificationsSettings();
        //TODO 发起请求
//        TLRPC.TL_account_updateNotifySettings req = new TLRPC.TL_account_updateNotifySettings();
//        req.settings = new TLRPC.TL_inputPeerNotifySettings();
//        req.settings.flags = 5;
//        if (type == TYPE_GROUP) {
//            req.peer = new TLRPC.TL_inputNotifyChats();
//            req.settings.mute_until = preferences.getInt("EnableGroup2", 0);
//            req.settings.show_previews = preferences.getBoolean("EnablePreviewGroup", true);
//        } else if (type == TYPE_PRIVATE) {
//            req.peer = new TLRPC.TL_inputNotifyUsers();
//            req.settings.mute_until = preferences.getInt("EnableAll2", 0);
//            req.settings.show_previews = preferences.getBoolean("EnablePreviewAll", true);
//        } else {
//            req.peer = new TLRPC.TL_inputNotifyBroadcasts();
//            req.settings.mute_until = preferences.getInt("EnableChannel2", 0);
//            req.settings.show_previews = preferences.getBoolean("EnablePreviewChannel", true);
//        }
//        getConnectionsManager().sendRequest(req, (response, error) -> {
//
//        });
    }

    public boolean isGlobalNotificationsEnabled(long did) {
        return isGlobalNotificationsEnabled(did, null);
    }

    public boolean isGlobalNotificationsEnabled(long did, Boolean forceChannel) {
        int type;
        int lower_id = (int) did;
        if (lower_id < 0) {
            if (forceChannel != null) {
                if (forceChannel) {
                    type = TYPE_CHANNEL;
                } else {
                    type = TYPE_GROUP;
                }
            } else {
                Chat chat = getMessagesController().getChat(-lower_id);
                if (ChatObject.isChannel(chat) && !chat.megagroup) {
                    type = TYPE_CHANNEL;
                } else {
                    type = TYPE_GROUP;
                }
            }
        } else {
            type = TYPE_PRIVATE;
        }
        return isGlobalNotificationsEnabled(type);
    }

    public boolean isGlobalNotificationsEnabled(int type) {
        return getAccountInstance().getNotificationsSettings().getInt(getGlobalNotificationsKey(type), 0) < getConnectionsManager().getCurrentTime();
    }

    public void setGlobalNotificationsEnabled(int type, int time) {
        getAccountInstance().getNotificationsSettings().edit().putInt(getGlobalNotificationsKey(type), time).commit();
        updateServerNotificationsSettings(type);
        getMessagesStorage().updateMutedDialogsFiltersCounters();
    }

    public String getGlobalNotificationsKey(int type) {
        if (type == TYPE_GROUP) {
            return "EnableGroup2";
        } else if (type == TYPE_PRIVATE) {
            return "EnableAll2";
        } else {
            return "EnableChannel2";
        }
    }
}

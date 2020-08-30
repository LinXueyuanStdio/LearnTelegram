package com.demo.chat.controller;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.demo.chat.ApplicationLoader;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.BuildVars;
import com.demo.chat.messager.FileLog;
import com.demo.chat.messager.ImageLoader;
import com.demo.chat.messager.NotificationCenter;
import com.demo.chat.messager.SerializedData;
import com.demo.chat.messager.Utilities;
import com.demo.chat.model.Chat;
import com.demo.chat.model.Message;
import com.demo.chat.model.User;
import com.demo.chat.model.UserChat;
import com.demo.chat.model.action.ChatObject;
import com.demo.chat.model.action.MessageObject;
import com.demo.chat.model.message.messages_Messages;
import com.demo.chat.model.small.Document;
import com.demo.chat.ui.ActionBar.AlertDialog;
import com.demo.chat.ui.ActionBar.BaseFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class MessagesController extends BaseController implements NotificationCenter.NotificationCenterDelegate {

    public static final int UPDATE_MASK_NAME = 1;
    public static final int UPDATE_MASK_AVATAR = 2;
    public static final int UPDATE_MASK_STATUS = 4;
    public static final int UPDATE_MASK_CHAT_AVATAR = 8;
    public static final int UPDATE_MASK_CHAT_NAME = 16;
    public static final int UPDATE_MASK_CHAT_MEMBERS = 32;
    public static final int UPDATE_MASK_USER_PRINT = 64;
    public static final int UPDATE_MASK_USER_PHONE = 128;
    public static final int UPDATE_MASK_READ_DIALOG_MESSAGE = 256;
    public static final int UPDATE_MASK_SELECT_DIALOG = 512;
    public static final int UPDATE_MASK_PHONE = 1024;
    public static final int UPDATE_MASK_NEW_MESSAGE = 2048;
    public static final int UPDATE_MASK_SEND_STATE = 4096;
    public static final int UPDATE_MASK_CHAT = 8192;
    //public static final int UPDATE_MASK_CHAT_ADMINS = 16384;
    public static final int UPDATE_MASK_MESSAGE_TEXT = 32768;
    public static final int UPDATE_MASK_CHECK = 65536;
    public static final int UPDATE_MASK_REORDER = 131072;
    public static final int UPDATE_MASK_ALL = UPDATE_MASK_AVATAR | UPDATE_MASK_STATUS | UPDATE_MASK_NAME | UPDATE_MASK_CHAT_AVATAR | UPDATE_MASK_CHAT_NAME | UPDATE_MASK_CHAT_MEMBERS | UPDATE_MASK_USER_PRINT | UPDATE_MASK_USER_PHONE | UPDATE_MASK_READ_DIALOG_MESSAGE | UPDATE_MASK_PHONE;


    private int nextPromoInfoCheckTime;
    private boolean checkingPromoInfo;
    private int checkingPromoInfoRequestId;
    private int lastCheckPromoId;
    private long promoDialogId;
    public int promoDialogType;
    public String promoPsaMessage;
    public String promoPsaType;
    public int secretWebpagePreview;
    private String proxyDialogAddress;
    private int nextTosCheckTime;

    public boolean enableJoined;
    public String linkPrefix;
    public int maxGroupCount;
    public int maxBroadcastCount = 100;
    public int maxMegagroupCount;
    public int minGroupConvertSize = 200;
    public int maxEditTime;
    public int ratingDecay;
    public int revokeTimeLimit;
    public int revokeTimePmLimit;
    public boolean canRevokePmInbox;
    public int maxRecentStickersCount;
    public int maxFaveStickersCount;
    public int maxRecentGifsCount;
    public int callReceiveTimeout;
    public int callRingTimeout;
    public int callConnectTimeout;
    public int callPacketTimeout;
    public int maxPinnedDialogsCount;
    public int maxFolderPinnedDialogsCount;
    public int mapProvider;
    public int availableMapProviders;
    public String mapKey;
    public int maxMessageLength;
    public int maxCaptionLength;
    public boolean blockedCountry;
    public boolean preloadFeaturedStickers;
    public String youtubePipType;
    public boolean keepAliveService;
    public boolean backgroundConnection;
    public float animatedEmojisZoom;
    public boolean filtersEnabled;
    public boolean showFiltersTooltip;
    public String venueSearchBot;
    public String gifSearchBot;
    public String imageSearchBot;
    public String suggestedLangCode;
    public boolean qrLoginCamera;
    public boolean saveGifsWithStickers;
    private String installReferer;
    public ArrayList<String> gifSearchEmojies = new ArrayList<>();
    public HashSet<String> diceEmojies;
    public HashMap<String, DiceFrameSuccess> diceSuccess = new HashMap<>();

    public static class DiceFrameSuccess {
        public int frame;
        public int num;

        public DiceFrameSuccess(int f, int n) {
            frame = f;
            num = n;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DiceFrameSuccess)) {
                return false;
            }
            DiceFrameSuccess frameSuccess = (DiceFrameSuccess) obj;
            return frame == frameSuccess.frame && num == frameSuccess.num;
        }
    }

    private SharedPreferences notificationsPreferences;
    private SharedPreferences mainPreferences;
    private SharedPreferences emojiPreferences;

    private static volatile MessagesController[] Instance = new MessagesController[UserConfig.MAX_ACCOUNT_COUNT];

    public static MessagesController getInstance(int num) {
        MessagesController localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (MessagesController.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new MessagesController(num);
                }
            }
        }
        return localInstance;
    }

    public static SharedPreferences getNotificationsSettings(int account) {
        return getInstance(account).notificationsPreferences;
    }

    public static SharedPreferences getGlobalNotificationsSettings() {
        return getInstance(0).notificationsPreferences;
    }

    public static SharedPreferences getMainSettings(int account) {
        return getInstance(account).mainPreferences;
    }

    public static SharedPreferences getGlobalMainSettings() {
        return getInstance(0).mainPreferences;
    }

    public static SharedPreferences getEmojiSettings(int account) {
        return getInstance(account).emojiPreferences;
    }

    public static SharedPreferences getGlobalEmojiSettings() {
        return getInstance(0).emojiPreferences;
    }

    public MessagesController(int num) {
        super(num);
        currentAccount = num;
        ImageLoader.getInstance();
        getMessagesStorage();
        getLocationController();
        AndroidUtilities.runOnUIThread(() -> {
            MessagesController messagesController = getMessagesController();
            getNotificationCenter().addObserver(messagesController, NotificationCenter.FileDidUpload);
            getNotificationCenter().addObserver(messagesController, NotificationCenter.FileDidFailUpload);
            getNotificationCenter().addObserver(messagesController, NotificationCenter.fileDidLoad);
            getNotificationCenter().addObserver(messagesController, NotificationCenter.fileDidFailToLoad);
            getNotificationCenter().addObserver(messagesController, NotificationCenter.messageReceivedByServer);
            getNotificationCenter().addObserver(messagesController, NotificationCenter.updateMessageMedia);
        });

        if (currentAccount == 0) {
            notificationsPreferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
            mainPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
            emojiPreferences = ApplicationLoader.applicationContext.getSharedPreferences("emoji", Activity.MODE_PRIVATE);
        } else {
            notificationsPreferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications" + currentAccount, Activity.MODE_PRIVATE);
            mainPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig" + currentAccount, Activity.MODE_PRIVATE);
            emojiPreferences = ApplicationLoader.applicationContext.getSharedPreferences("emoji" + currentAccount, Activity.MODE_PRIVATE);
        }

        enableJoined = notificationsPreferences.getBoolean("EnableContactJoined", true);
        secretWebpagePreview = mainPreferences.getInt("secretWebpage2", 2);
        maxGroupCount = mainPreferences.getInt("maxGroupCount", 200);
        maxMegagroupCount = mainPreferences.getInt("maxMegagroupCount", 10000);
        maxRecentGifsCount = mainPreferences.getInt("maxRecentGifsCount", 200);
        maxRecentStickersCount = mainPreferences.getInt("maxRecentStickersCount", 30);
        maxFaveStickersCount = mainPreferences.getInt("maxFaveStickersCount", 5);
        maxEditTime = mainPreferences.getInt("maxEditTime", 3600);
        ratingDecay = mainPreferences.getInt("ratingDecay", 2419200);
        linkPrefix = mainPreferences.getString("linkPrefix", "t.me");
        callReceiveTimeout = mainPreferences.getInt("callReceiveTimeout", 20000);
        callRingTimeout = mainPreferences.getInt("callRingTimeout", 90000);
        callConnectTimeout = mainPreferences.getInt("callConnectTimeout", 30000);
        callPacketTimeout = mainPreferences.getInt("callPacketTimeout", 10000);
        maxPinnedDialogsCount = mainPreferences.getInt("maxPinnedDialogsCount", 5);
        maxFolderPinnedDialogsCount = mainPreferences.getInt("maxFolderPinnedDialogsCount", 100);
        maxMessageLength = mainPreferences.getInt("maxMessageLength", 4096);
        maxCaptionLength = mainPreferences.getInt("maxCaptionLength", 1024);
        mapProvider = mainPreferences.getInt("mapProvider", 0);
        availableMapProviders = mainPreferences.getInt("availableMapProviders", 3);
        mapKey = mainPreferences.getString("pk", null);
        installReferer = mainPreferences.getString("installReferer", null);
        revokeTimeLimit = mainPreferences.getInt("revokeTimeLimit", revokeTimeLimit);
        revokeTimePmLimit = mainPreferences.getInt("revokeTimePmLimit", revokeTimePmLimit);
        canRevokePmInbox = mainPreferences.getBoolean("canRevokePmInbox", canRevokePmInbox);
        preloadFeaturedStickers = mainPreferences.getBoolean("preloadFeaturedStickers", false);
        youtubePipType = mainPreferences.getString("youtubePipType", "disabled");
        keepAliveService = mainPreferences.getBoolean("keepAliveService", false);
        backgroundConnection = mainPreferences.getBoolean("keepAliveService", false);
        promoDialogId = mainPreferences.getLong("proxy_dialog", 0);
        nextPromoInfoCheckTime = mainPreferences.getInt("nextPromoInfoCheckTime", 0);
        promoDialogType = mainPreferences.getInt("promo_dialog_type", 0);
        promoPsaMessage = mainPreferences.getString("promo_psa_message", null);
        promoPsaType = mainPreferences.getString("promo_psa_type", null);
        proxyDialogAddress = mainPreferences.getString("proxyDialogAddress", null);
        nextTosCheckTime = notificationsPreferences.getInt("nextTosCheckTime", 0);
        venueSearchBot = mainPreferences.getString("venueSearchBot", "foursquare");
        gifSearchBot = mainPreferences.getString("gifSearchBot", "gif");
        imageSearchBot = mainPreferences.getString("imageSearchBot", "pic");
        blockedCountry = mainPreferences.getBoolean("blockedCountry", false);
        suggestedLangCode = mainPreferences.getString("suggestedLangCode", "en");
        animatedEmojisZoom = mainPreferences.getFloat("animatedEmojisZoom", 0.625f);
        qrLoginCamera = mainPreferences.getBoolean("qrLoginCamera", false);
        saveGifsWithStickers = mainPreferences.getBoolean("saveGifsWithStickers", false);
        filtersEnabled = mainPreferences.getBoolean("filtersEnabled", false);
        showFiltersTooltip = mainPreferences.getBoolean("showFiltersTooltip", false);

        Set<String> emojies = mainPreferences.getStringSet("diceEmojies", null);
        if (emojies == null) {
            diceEmojies = new HashSet<>();
            diceEmojies.add("\uD83C\uDFB2");
            diceEmojies.add("\uD83C\uDFAF");
        } else {
            diceEmojies = new HashSet<>(emojies);
        }
        String text = mainPreferences.getString("diceSuccess", null);
        if (text == null) {
            diceSuccess.put("\uD83C\uDFAF", new DiceFrameSuccess(62, 6));
        } else {
            try {
                byte[] bytes = Base64.decode(text, Base64.DEFAULT);
                if (bytes != null) {
                    SerializedData data = new SerializedData(bytes);
                    int count = data.readInt32(true);
                    for (int a = 0; a < count; a++) {
                        diceSuccess.put(data.readString(true), new DiceFrameSuccess(data.readInt32(true), data.readInt32(true)));
                    }
                    data.cleanup();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }

        text = mainPreferences.getString("gifSearchEmojies", null);
        if (text == null) {
            gifSearchEmojies.add("👍");
            gifSearchEmojies.add("👎");
            gifSearchEmojies.add("😍");
            gifSearchEmojies.add("😂");
            gifSearchEmojies.add("😮");
            gifSearchEmojies.add("🙄");
            gifSearchEmojies.add("😥");
            gifSearchEmojies.add("😡");
            gifSearchEmojies.add("🥳");
            gifSearchEmojies.add("😎");
        } else {
            try {
                byte[] bytes = Base64.decode(text, Base64.DEFAULT);
                if (bytes != null) {
                    SerializedData data = new SerializedData(bytes);
                    int count = data.readInt32(true);
                    for (int a = 0; a < count; a++) {
                        gifSearchEmojies.add(data.readString(true));
                    }
                    data.cleanup();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    //region 聊天
    private ConcurrentHashMap<Integer, Chat> chats = new ConcurrentHashMap<>(100, 1.0f, 2);
    private ConcurrentHashMap<Integer, User> users = new ConcurrentHashMap<>(100, 1.0f, 2);
    private ConcurrentHashMap<String, UserChat> objectsByUsernames = new ConcurrentHashMap<>(100, 1.0f, 2);

    public Chat getChat(Integer id) {
        Chat c = new Chat();

        return c;
//        return chats.get(id);
    }

    public User getUser(Integer id) {
        User u = new User();
        u.id = id;
        u.username = "user_id="+id;
        return u;
//        return users.get(id);
    }

    public boolean putUser(User user, boolean fromCache) {
        if (user == null) {
            return false;
        }
        fromCache = fromCache && user.id / 1000 != 333 && user.id != 777000;
        User oldUser = users.get(user.id);
        if (oldUser == user) {
            return false;
        }
        if (oldUser != null && !TextUtils.isEmpty(oldUser.username)) {
            objectsByUsernames.remove(oldUser.username.toLowerCase());
        }
        if (!TextUtils.isEmpty(user.username)) {
            objectsByUsernames.put(user.username.toLowerCase(), user);
        }
        if (user.min) {
            if (oldUser != null) {
                if (!fromCache) {
                    if (user.bot) {
                        if (user.username != null) {
                            oldUser.username = user.username;
                            oldUser.flags |= 8;
                        } else {
                            oldUser.flags = oldUser.flags & ~8;
                            oldUser.username = null;
                        }
                    }
                    if (user.photo != null) {
                        oldUser.photo = user.photo;
                        oldUser.flags |= 32;
                    } else {
                        oldUser.flags = oldUser.flags & ~32;
                        oldUser.photo = null;
                    }
                }
            } else {
                users.put(user.id, user);
            }
        } else {
            if (!fromCache) {
                users.put(user.id, user);
                if (user.id == getUserConfig().getClientUserId()) {
                    getUserConfig().setCurrentUser(user);
                    getUserConfig().saveConfig(true);
                }
                if (oldUser != null && user.status != null && oldUser.status != null && user.status.expires != oldUser.status.expires) {
                    return true;
                }
            } else if (oldUser == null) {
                users.put(user.id, user);
            } else if (oldUser.min) {
                user.min = false;
                if (oldUser.bot) {
                    if (oldUser.username != null) {
                        user.username = oldUser.username;
                        user.flags |= 8;
                    } else {
                        user.flags = user.flags & ~8;
                        user.username = null;
                    }
                }
                if (oldUser.photo != null) {
                    user.photo = oldUser.photo;
                    user.flags |= 32;
                } else {
                    user.flags = user.flags & ~32;
                    user.photo = null;
                }
                users.put(user.id, user);
            }
        }
        return false;
    }

    public void putUsers(ArrayList<User> users, boolean fromCache) {
        if (users == null || users.isEmpty()) {
            return;
        }
        boolean updateStatus = false;
        int count = users.size();
        for (int a = 0; a < count; a++) {
            User user = users.get(a);
            if (putUser(user, fromCache)) {
                updateStatus = true;
            }
        }
        if (updateStatus) {
            AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, UPDATE_MASK_STATUS));
        }
    }

    public void putChat(final Chat chat, boolean fromCache) {
        if (chat == null) {
            return;
        }
        Chat oldChat = chats.get(chat.id);
        if (oldChat == chat) {
            return;
        }
        if (oldChat != null && !TextUtils.isEmpty(oldChat.username)) {
            objectsByUsernames.remove(oldChat.username.toLowerCase());
        }
        if (!TextUtils.isEmpty(chat.username)) {
            objectsByUsernames.put(chat.username.toLowerCase(), chat);
        }
        if (chat.min) {
            if (oldChat != null) {
                if (!fromCache) {
                    oldChat.title = chat.title;
                    oldChat.photo = chat.photo;
                    oldChat.broadcast = chat.broadcast;
                    oldChat.verified = chat.verified;
                    oldChat.megagroup = chat.megagroup;
                    if (chat.default_banned_rights != null) {
                        oldChat.default_banned_rights = chat.default_banned_rights;
                        oldChat.flags |= 262144;
                    }
                    if (chat.admin_rights != null) {
                        oldChat.admin_rights = chat.admin_rights;
                        oldChat.flags |= 16384;
                    }
                    if (chat.banned_rights != null) {
                        oldChat.banned_rights = chat.banned_rights;
                        oldChat.flags |= 32768;
                    }
                    if (chat.username != null) {
                        oldChat.username = chat.username;
                        oldChat.flags |= 64;
                    } else {
                        oldChat.flags = oldChat.flags & ~64;
                        oldChat.username = null;
                    }
                    if (chat.participants_count != 0) {
                        oldChat.participants_count = chat.participants_count;
                    }
                }
            } else {
                chats.put(chat.id, chat);
            }
        } else {
            if (!fromCache) {
                if (oldChat != null) {
//                    if (chat.version != oldChat.version) {
//                        loadedFullChats.remove((Integer) chat.id);
//                    }
                    if (oldChat.participants_count != 0 && chat.participants_count == 0) {
                        chat.participants_count = oldChat.participants_count;
                        chat.flags |= 131072;
                    }

                    int oldFlags = oldChat.banned_rights != null ? oldChat.banned_rights.flags : 0;
                    int newFlags = chat.banned_rights != null ? chat.banned_rights.flags : 0;
                    int oldFlags2 = oldChat.default_banned_rights != null ? oldChat.default_banned_rights.flags : 0;
                    int newFlags2 = chat.default_banned_rights != null ? chat.default_banned_rights.flags : 0;
                    oldChat.default_banned_rights = chat.default_banned_rights;
                    if (oldChat.default_banned_rights == null) {
                        oldChat.flags &= ~262144;
                    } else {
                        oldChat.flags |= 262144;
                    }
                    oldChat.banned_rights = chat.banned_rights;
                    if (oldChat.banned_rights == null) {
                        oldChat.flags &= ~32768;
                    } else {
                        oldChat.flags |= 32768;
                    }
                    oldChat.admin_rights = chat.admin_rights;
                    if (oldChat.admin_rights == null) {
                        oldChat.flags &= ~16384;
                    } else {
                        oldChat.flags |= 16384;
                    }
                    if (oldFlags != newFlags || oldFlags2 != newFlags2) {
                        AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.channelRightsUpdated, chat));
                    }
                }
                chats.put(chat.id, chat);
            } else if (oldChat == null) {
                chats.put(chat.id, chat);
            } else if (oldChat.min) {
                chat.min = false;
                chat.title = oldChat.title;
                chat.photo = oldChat.photo;
                chat.broadcast = oldChat.broadcast;
                chat.verified = oldChat.verified;
                chat.megagroup = oldChat.megagroup;

                if (oldChat.default_banned_rights != null) {
                    chat.default_banned_rights = oldChat.default_banned_rights;
                    chat.flags |= 262144;
                }
                if (oldChat.admin_rights != null) {
                    chat.admin_rights = oldChat.admin_rights;
                    chat.flags |= 16384;
                }
                if (oldChat.banned_rights != null) {
                    chat.banned_rights = oldChat.banned_rights;
                    chat.flags |= 32768;
                }
                if (oldChat.username != null) {
                    chat.username = oldChat.username;
                    chat.flags |= 64;
                } else {
                    chat.flags = chat.flags & ~64;
                    chat.username = null;
                }
                if (oldChat.participants_count != 0 && chat.participants_count == 0) {
                    chat.participants_count = oldChat.participants_count;
                    chat.flags |= 131072;
                }
                chats.put(chat.id, chat);
            }
        }
    }

    public void putChats(ArrayList<Chat> chats, boolean fromCache) {
        if (chats == null || chats.isEmpty()) {
            return;
        }
        int count = chats.size();
        for (int a = 0; a < count; a++) {
            Chat chat = chats.get(a);
            putChat(chat, fromCache);
        }
    }

    public void loadMessages(long dialogId, long mergeDialogId, boolean loadInfo, int count, int max_id, int offset_date, boolean fromCache, int midDate, int classGuid, int load_type, int last_message_id, boolean isChannel, boolean scheduled, int loadIndex) {
        loadMessages(dialogId, mergeDialogId, loadInfo, count, max_id, offset_date, fromCache, midDate, classGuid, load_type, last_message_id, isChannel, scheduled, loadIndex, 0, 0, 0, false, 0);
    }

    public void loadMessages(long dialogId, long mergeDialogId, boolean loadInfo, int count, int max_id, int offset_date, boolean fromCache, int midDate, int classGuid, int load_type, int last_message_id, boolean isChannel, boolean scheduled, int loadIndex, int first_unread, int unread_count, int last_date, boolean queryFromServer, int mentionsCount) {
        loadMessagesInternal(dialogId, mergeDialogId, loadInfo, count, max_id, offset_date, fromCache, midDate, classGuid, load_type, last_message_id, isChannel, scheduled, loadIndex, first_unread, unread_count, last_date, queryFromServer, mentionsCount, true);
    }

    private void loadMessagesInternal(long dialogId, long mergeDialogId,
            boolean loadInfo, int count, int max_id, int offset_date,
            boolean fromCache, int minDate, int classGuid,
            int load_type, int last_message_id, boolean isChannel,
            boolean scheduled, int loadIndex, int first_unread,
            int unread_count, int last_date, boolean queryFromServer,
            int mentionsCount, boolean loadDialog) {
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("load messages in chat " + dialogId + " count " + count + " max_id " + max_id + " cache " + fromCache + " mindate = " + minDate + " guid " + classGuid + " load_type " + load_type + " last_message_id " + last_message_id + " scheduled " + scheduled + " index " + loadIndex + " firstUnread " + first_unread + " unread_count " + unread_count + " last_date " + last_date + " queryFromServer " + queryFromServer);
        }
        int lower_part = (int) dialogId;
        if (fromCache || lower_part == 0) {
            getMessagesStorage().getMessages(dialogId, mergeDialogId, loadInfo, count, max_id, offset_date, minDate, classGuid, load_type, isChannel, scheduled, loadIndex);
        }
    }


    public void processLoadedMessages(messages_Messages messagesRes, long dialogId, long mergeDialogId, int count, int max_id, int offset_date, boolean isCache, int classGuid,
            int first_unread, int last_message_id, int unread_count, int last_date, int load_type, boolean isChannel, boolean isEnd, boolean scheduled, int loadIndex, boolean queryFromServer, int mentionsCount) {
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("processLoadedMessages size " + messagesRes.messages.size() + " in chat " + dialogId + " count " + count + " max_id " + max_id + " cache " + isCache + " guid " + classGuid + " load_type " + load_type + " last_message_id " + last_message_id + " isChannel " + isChannel + " index " + loadIndex + " firstUnread " + first_unread + " unread_count " + unread_count + " last_date " + last_date + " queryFromServer " + queryFromServer);
        }
        boolean createDialog = false;
        boolean isMegagroup = false;
//        if (messagesRes !=null) {
//            int channelId = -(int) dialogId;
//            if (!scheduled) {
//                int channelPts = channelsPts.get(channelId);
//                if (channelPts == 0) {
//                    channelPts = getMessagesStorage().getChannelPtsSync(channelId);
//                    if (channelPts == 0) {
//                        channelsPts.put(channelId, messagesRes.pts);
//                        createDialog = true;
//                        if (needShortPollChannels.indexOfKey(channelId) >= 0 && shortPollChannels.indexOfKey(channelId) < 0) {
//                            getChannelDifference(channelId, 2, 0, null);
//                        } else {
//                            getChannelDifference(channelId);
//                        }
//                    }
//                }
//            }
//            for (int a = 0; a < messagesRes.chats.size(); a++) {
//                Chat chat = messagesRes.chats.get(a);
//                if (chat.id == channelId) {
//                    isMegagroup = chat.megagroup;
//                    break;
//                }
//            }
//        }
//        int lower_id = (int) dialogId;
//        int high_id = (int) (dialogId >> 32);
//        if (!isCache) {
//            ImageLoader.saveMessagesThumbs(messagesRes.messages);
//        }
//        if (high_id != 1 && lower_id != 0 && isCache && (messagesRes.messages.size() == 0 || scheduled && (SystemClock.elapsedRealtime() - lastScheduledServerQueryTime.get(dialogId, 0L)) > 60 * 1000)) {
//            int hash;
//            if (scheduled) {
//                lastScheduledServerQueryTime.put(dialogId, SystemClock.elapsedRealtime());
//                long h = 0;
//                for (int a = 0, N = messagesRes.messages.size(); a < N; a++) {
//                    Message message = messagesRes.messages.get(a);
//                    if (message.id < 0) {
//                        continue;
//                    }
//                    h = ((h * 20261) + 0x80000000L + message.id) % 0x80000000L;
//                    h = ((h * 20261) + 0x80000000L + message.edit_date) % 0x80000000L;
//                    h = ((h * 20261) + 0x80000000L + message.date) % 0x80000000L;
//                }
//                hash = (int) h - 1;
//            } else {
//                hash = 0;
//            }
//            AndroidUtilities.runOnUIThread(() -> loadMessages(dialogId, mergeDialogId, false, count, load_type == 2 && queryFromServer ? first_unread : max_id, offset_date, false, hash, classGuid, load_type, last_message_id, isChannel, scheduled, loadIndex, first_unread, unread_count, last_date, queryFromServer, mentionsCount));
//            if (messagesRes.messages.isEmpty()) {
//                return;
//            }
//        }
        final SparseArray<User> usersDict = new SparseArray<>();
        final SparseArray<Chat> chatsDict = new SparseArray<>();
        for (int a = 0; a < messagesRes.users.size(); a++) {
            User u = messagesRes.users.get(a);
            usersDict.put(u.id, u);
        }
        for (int a = 0; a < messagesRes.chats.size(); a++) {
            Chat c = messagesRes.chats.get(a);
            chatsDict.put(c.id, c);
        }
        int size = messagesRes.messages.size();
//        if (!isCache) {
//            Integer inboxValue = dialogs_read_inbox_max.get(dialogId);
//            if (inboxValue == null) {
//                inboxValue = getMessagesStorage().getDialogReadMax(false, dialogId);
//                dialogs_read_inbox_max.put(dialogId, inboxValue);
//            }
//
//            Integer outboxValue = dialogs_read_outbox_max.get(dialogId);
//            if (outboxValue == null) {
//                outboxValue = getMessagesStorage().getDialogReadMax(true, dialogId);
//                dialogs_read_outbox_max.put(dialogId, outboxValue);
//            }
//
//            for (int a = 0; a < size; a++) {
//                Message message = messagesRes.messages.get(a);
//                if (isMegagroup) {
//                    message.flags |= TLRPC.MESSAGE_FLAG_MEGAGROUP;
//                }
//
//                if (!scheduled) {
//                    if (message.action instanceof TLRPC.TL_messageActionChatDeleteUser) {
//                        User user = usersDict.get(message.action.user_id);
//                        if (user != null && user.bot) {
//                            message.reply_markup = new TLRPC.TL_replyKeyboardHide();
//                            message.flags |= 64;
//                        }
//                    }
//
//                    message.unread = (message.out ? outboxValue : inboxValue) < message.id;
//                }
//            }
//            getMessagesStorage().putMessages(messagesRes, dialogId, load_type, max_id, createDialog, scheduled);
//        }
//
        final ArrayList<MessageObject> objects = new ArrayList<>();
        final ArrayList<Integer> messagesToReload = new ArrayList<>();
        final HashMap<String, ArrayList<MessageObject>> webpagesToReload = new HashMap<>();
//        InputChannel inputChannel = null;
        size = 50;
        for (int a = 0; a < size; a++) {
            Message message = new Message();//messagesRes.messages.get(a);
            message.dialog_id = dialogId;
            MessageObject messageObject = new MessageObject(currentAccount, message, usersDict, chatsDict, true);
            messageObject.scheduled = scheduled;
            objects.add(messageObject);
            if (isCache) {
                if (message.legacy && message.layer <1) {
                    messagesToReload.add(message.id);
                } else if (message.media.isUnsupported()) {
                    if (message.media.bytes != null && (message.media.bytes.length == 0 || message.media.bytes.length == 1 && message.media.bytes[0] < 1)) {
                        messagesToReload.add(message.id);
                    }
                }
                if (message.media.isWebPage()) {
                    if (message.media.webpage !=null && message.media.webpage.date <= getConnectionsManager().getCurrentTime()) {
                        messagesToReload.add(message.id);
                    } else if (message.media.webpage!=null) {
                        ArrayList<MessageObject> arrayList = webpagesToReload.get(message.media.webpage.url);
                        if (arrayList == null) {
                            arrayList = new ArrayList<>();
                            webpagesToReload.put(message.media.webpage.url, arrayList);
                        }
                        arrayList.add(messageObject);
                    }
                }
            }
        }
        AndroidUtilities.runOnUIThread(() -> {
            putUsers(messagesRes.users, isCache);
            putChats(messagesRes.chats, isCache);
            int first_unread_final = 0;
            getNotificationCenter().postNotificationName(NotificationCenter.messagesDidLoad, dialogId, count, objects, isCache, first_unread_final, last_message_id, unread_count, last_date, load_type, isEnd, classGuid, loadIndex, max_id, mentionsCount, scheduled);
//            if (scheduled) {
//                first_unread_final = 0;
//            } else {
//                first_unread_final = Integer.MAX_VALUE;
//                if (queryFromServer && load_type == 2) {
//                    for (int a = 0; a < messagesRes.messages.size(); a++) {
//                        Message message = messagesRes.messages.get(a);
//                        if ((!message.out || message.from_scheduled) && message.id > first_unread && message.id < first_unread_final) {
//                            first_unread_final = message.id;
//                        }
//                    }
//                }
//                if (first_unread_final == Integer.MAX_VALUE) {
//                    first_unread_final = first_unread;
//                }
//            }
//            if (scheduled && count == 1) {
//                getNotificationCenter().postNotificationName(NotificationCenter.scheduledMessagesUpdated, dialogId, objects.size());
//            }
//
//            if ((int) dialogId != 0) {
//                int finalFirst_unread_final = first_unread_final;
//                getMediaDataController().loadReplyMessagesForMessages(objects, dialogId, scheduled, () -> getNotificationCenter().postNotificationName(NotificationCenter.messagesDidLoad, dialogId, count, objects, isCache, finalFirst_unread_final, last_message_id, unread_count, last_date, load_type, isEnd, classGuid, loadIndex, max_id, mentionsCount, scheduled));
//            } else {
//                getNotificationCenter().postNotificationName(NotificationCenter.messagesDidLoad, dialogId, count, objects, isCache, first_unread_final, last_message_id, unread_count, last_date, load_type, isEnd, classGuid, loadIndex, max_id, mentionsCount, scheduled);
//            }
//
//            if (!messagesToReload.isEmpty()) {
//                reloadMessages(messagesToReload, dialogId, scheduled);
//            }
//            if (!webpagesToReload.isEmpty()) {
//                reloadWebPages(dialogId, webpagesToReload, scheduled);
//            }
        });
    }


    //endregion
    public void markMessageAsRead(final int mid, final int channelId, int ttl, long taskId) {
        if (mid == 0 || ttl <= 0) {
            return;
        }
        int time = getConnectionsManager().getCurrentTime();
        getMessagesStorage().createTaskForMid(mid, channelId, time, time, ttl, false);
    }

    public void markMessageAsRead(final long dialog_id, final long random_id, int ttl) {
        if (random_id == 0 || dialog_id == 0 || ttl <= 0 && ttl != Integer.MIN_VALUE) {
            return;
        }
        int lower_part = (int) dialog_id;
        int high_id = (int) (dialog_id >> 32);
        if (lower_part != 0) {
            return;
        }
    }
    public void markDialogAsRead(final long dialogId, final int maxPositiveId, final int maxNegativeId, final int maxDate, final boolean popup, final int countDiff, final boolean readNow, final int scheduledCount){}

    public void markMessageContentAsRead(final MessageObject messageObject) {
        if (messageObject.scheduled) {
            return;
        }
        ArrayList<Long> arrayList = new ArrayList<>();
        long messageId = messageObject.getId();
        if (messageObject.messageOwner.to_id != 0) {
            messageId |= ((long) messageObject.messageOwner.to_id) << 32;
        }
        if (messageObject.messageOwner.mentioned) {
            getMessagesStorage().markMentionMessageAsRead(messageObject.getId(), messageObject.messageOwner.to_id, messageObject.getDialogId());
        }
        arrayList.add(messageId);
        getMessagesStorage().markMessagesContentAsRead(arrayList, 0);
        getNotificationCenter().postNotificationName(NotificationCenter.messagesReadContent, arrayList);
    }

    public void markMentionMessageAsRead(final int mid, final int channelId, final long did) {
        getMessagesStorage().markMentionMessageAsRead(mid, channelId, did);
    }

    public void deleteDialog(final long did, final int onlyHistory) {
        deleteDialog(did, onlyHistory, false);
    }

    public void deleteDialog(final long did, final int onlyHistory, boolean revoke) {
        deleteDialog(did, true, onlyHistory, 0, revoke, 0);
    }
    protected void deleteDialog(final long did, final boolean first, final int onlyHistory, final int max_id, boolean revoke, final long taskId) {
        if (onlyHistory == 2) {
            getMessagesStorage().deleteDialog(did, onlyHistory);
            return;
        }
//        if (onlyHistory == 0 || onlyHistory == 3) {
//            getMediaDataController().uninstallShortcut(did);
//        }
//        int lower_part = (int) did;
//        int high_id = (int) (did >> 32);
//        int max_id_delete = max_id;
//
//        if (first) {
//            boolean isPromoDialog = false;
//            boolean emptyMax = max_id_delete == 0;
//            if (emptyMax) {
//                int max = getMessagesStorage().getDialogMaxMessageId(did);
//                if (max > 0) {
//                    max_id_delete = Math.max(max, max_id_delete);
//                }
//            }
//            getMessagesStorage().deleteDialog(did, onlyHistory);
//            TLRPC.Dialog dialog = dialogs_dict.get(did);
//            if (onlyHistory == 0 || onlyHistory == 3) {
//                getNotificationsController().deleteNotificationChannel(did);
//            }
//            if (onlyHistory == 0) {
//                getMediaDataController().cleanDraft(did, false);
//            }
//            if (dialog != null) {
//                if (emptyMax) {
//                    max_id_delete = Math.max(0, dialog.top_message);
//                    max_id_delete = Math.max(max_id_delete, dialog.read_inbox_max_id);
//                    max_id_delete = Math.max(max_id_delete, dialog.read_outbox_max_id);
//                }
//                if (onlyHistory == 0 || onlyHistory == 3) {
//                    if (isPromoDialog = (promoDialog != null && promoDialog.id == did)) {
//                        isLeftPromoChannel = true;
//                        if (promoDialog.id < 0) {
//                            TLRPC.Chat chat = getChat(-(int) promoDialog.id);
//                            if (chat != null) {
//                                chat.left = true;
//                            }
//                        }
//                        sortDialogs(null);
//                    } else {
//                        removeDialog(dialog);
//                        int offset = nextDialogsCacheOffset.get(dialog.folder_id, 0);
//                        if (offset > 0) {
//                            nextDialogsCacheOffset.put(dialog.folder_id, offset - 1);
//                        }
//                    }
//                } else {
//                    dialog.unread_count = 0;
//                }
//                if (!isPromoDialog) {
//                    int lastMessageId;
//                    MessageObject object = dialogMessage.get(dialog.id);
//                    dialogMessage.remove(dialog.id);
//                    if (object != null) {
//                        lastMessageId = object.getId();
//                        dialogMessagesByIds.remove(object.getId());
//                    } else {
//                        lastMessageId = dialog.top_message;
//                        object = dialogMessagesByIds.get(dialog.top_message);
//                        dialogMessagesByIds.remove(dialog.top_message);
//                    }
//                    if (object != null && object.messageOwner.random_id != 0) {
//                        dialogMessagesByRandomIds.remove(object.messageOwner.random_id);
//                    }
//                    if (onlyHistory == 1 && lower_part != 0 && lastMessageId > 0) {
//                        TLRPC.TL_messageService message = new TLRPC.TL_messageService();
//                        message.id = dialog.top_message;
//                        message.out = getUserConfig().getClientUserId() == did;
//                        message.from_id = getUserConfig().getClientUserId();
//                        message.flags |= 256;
//                        message.action = new TLRPC.TL_messageActionHistoryClear();
//                        message.date = dialog.last_message_date;
//                        message.dialog_id = lower_part;
//                        if (lower_part > 0) {
//                            message.to_id = new TLRPC.TL_peerUser();
//                            message.to_id = lower_part;
//                        } else {
//                            Chat chat = getChat(-lower_part);
//                            if (ChatObject.isChannel(chat)) {
//                                message.to_id = new TLRPC.TL_peerChannel();
//                                message.to_id = -lower_part;
//                            } else {
//                                message.to_id = new TLRPC.TL_peerChat();
//                                message.to_id = -lower_part;
//                            }
//                        }
//                        final MessageObject obj = new MessageObject(currentAccount, message, createdDialogIds.contains(message.dialog_id));
//                        final ArrayList<MessageObject> objArr = new ArrayList<>();
//                        objArr.add(obj);
//                        ArrayList<Message> arr = new ArrayList<>();
//                        arr.add(message);
//                        updateInterfaceWithMessages(did, objArr, false);
//                        getMessagesStorage().putMessages(arr, false, true, false, 0, false);
//                    } else {
//                        dialog.top_message = 0;
//                    }
//                }
//            }
//            if (emptyMax) {
//                Integer max = dialogs_read_inbox_max.get(did);
//                if (max != null) {
//                    max_id_delete = Math.max(max, max_id_delete);
//                }
//                max = dialogs_read_outbox_max.get(did);
//                if (max != null) {
//                    max_id_delete = Math.max(max, max_id_delete);
//                }
//            }
//
//            if (!dialogsInTransaction) {
//                if (isPromoDialog) {
//                    getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload, true);
//                } else {
//                    getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
//                    getNotificationCenter().postNotificationName(NotificationCenter.removeAllMessagesFromDialog, did, false);
//                }
//            }
//            getMessagesStorage().getStorageQueue().postRunnable(() -> AndroidUtilities.runOnUIThread(() -> getNotificationsController().removeNotificationsForDialog(did)));
//        }

        if (onlyHistory == 3) {
            return;
        }

//        if (onlyHistory == 1) {
//            getSecretChatHelper().sendClearHistoryMessage(getEncryptedChat(high_id), null);
//        } else {
//            getSecretChatHelper().declineSecretChat(high_id);
//        }
    }

    private ArrayList<Long> visibleScheduledDialogMainThreadIds = new ArrayList<>();
    private ArrayList<Long> visibleDialogMainThreadIds = new ArrayList<>();
    public void setLastVisibleDialogId(final long dialog_id, boolean scheduled, final boolean set) {
        ArrayList<Long> arrayList = scheduled ? visibleScheduledDialogMainThreadIds : visibleDialogMainThreadIds;
        if (set) {
            if (arrayList.contains(dialog_id)) {
                return;
            }
            arrayList.add(dialog_id);
        } else {
            arrayList.remove(dialog_id);
        }
    }


    private static class ReadTask {
        public long dialogId;
        public int maxId;
        public int maxDate;
        public long sendRequestTime;
    }

    private ArrayList<ReadTask> readTasks = new ArrayList<>();
    private LongSparseArray<ReadTask> readTasksMap = new LongSparseArray<>();

    public void markDialogAsReadNow(final long dialogId) {
        Utilities.stageQueue.postRunnable(() -> {
            ReadTask currentReadTask = readTasksMap.get(dialogId);
            if (currentReadTask == null) {
                return;
            }
            completeReadTask(currentReadTask);
            readTasks.remove(currentReadTask);
            readTasksMap.remove(dialogId);
        });
    }
    private void completeReadTask(ReadTask task) {
        int lower_part = (int) task.dialogId;
        int high_id = (int) (task.dialogId >> 32);

        if (lower_part != 0) {
            //TODO
        } else {
            //TODO
        }
    }
    public void markMentionsAsRead(long dialogId) {
        if ((int) dialogId == 0) {
            return;
        }
        getMessagesStorage().resetMentionsCount(dialogId, 0);
    }


    public SparseIntArray blockedUsers = new SparseIntArray();
    public int totalBlockedCount = -1;
    public void unblockUser(int user_id) {
        final User user = getUser(user_id);
        if (user == null) {
            return;
        }
        totalBlockedCount--;
        blockedUsers.delete(user.id);
        getNotificationCenter().postNotificationName(NotificationCenter.blockedUsersDidLoad);
    }

    /**
     * 机器人回调 onStart
     * @param user
     * @param botHash
     */
    public void sendBotStart(final User user, String botHash) {
        if (user == null) {
            return;
        }
    }

    public void addUserToChat(final int chat_id, final User user,
            int count_fwd, String botHash,
            final BaseFragment fragment, final Runnable onFinishRunnable) {
        if (user == null) {
            return;
        }

        if (chat_id > 0) {
            //TODO 发起请求
//            final TLObject request;
//
//            final boolean isChannel = ChatObject.isChannel(chat_id, currentAccount);
//            final boolean isMegagroup = isChannel && getChat(chat_id).megagroup;
//            final TLRPC.InputUser inputUser = getInputUser(user);
//            if (botHash == null || isChannel && !isMegagroup) {
//                if (isChannel) {
//                    if (inputUser instanceof TLRPC.TL_inputUserSelf) {
//                        if (joiningToChannels.contains(chat_id)) {
//                            return;
//                        }
//                        TLRPC.TL_channels_joinChannel req = new TLRPC.TL_channels_joinChannel();
//                        req.channel = getInputChannel(chat_id);
//                        request = req;
//                        joiningToChannels.add(chat_id);
//                    } else {
//                        TLRPC.TL_channels_inviteToChannel req = new TLRPC.TL_channels_inviteToChannel();
//                        req.channel = getInputChannel(chat_id);
//                        req.users.add(inputUser);
//                        request = req;
//                    }
//                } else {
//                    TLRPC.TL_messages_addChatUser req = new TLRPC.TL_messages_addChatUser();
//                    req.chat_id = chat_id;
//                    req.fwd_limit = count_fwd;
//                    req.user_id = inputUser;
//                    request = req;
//                }
//            } else {
//                TLRPC.TL_messages_startBot req = new TLRPC.TL_messages_startBot();
//                req.bot = inputUser;
//                if (isChannel) {
//                    req.peer = getInputPeer(-chat_id);
//                } else {
//                    req.peer = new TLRPC.TL_inputPeerChat();
//                    req.peer.chat_id = chat_id;
//                }
//                req.start_param = botHash;
//                req.random_id = Utilities.random.nextLong();
//                request = req;
//            }
//
//            getConnectionsManager().sendRequest(request, (response, error) -> {
//                if (isChannel && inputUser instanceof TLRPC.TL_inputUserSelf) {
//                    AndroidUtilities.runOnUIThread(() -> joiningToChannels.remove((Integer) chat_id));
//                }
//                if (error != null) {
//                    AndroidUtilities.runOnUIThread(() -> {
//                        AlertsCreator.processError(currentAccount, error, fragment, request, isChannel && !isMegagroup);
//                        if (isChannel && inputUser instanceof TLRPC.TL_inputUserSelf) {
//                            getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, UPDATE_MASK_CHAT);
//                        }
//                    });
//                    return;
//                }
//                boolean hasJoinMessage = false;
//                TLRPC.Updates updates = (TLRPC.Updates) response;
//                for (int a = 0; a < updates.updates.size(); a++) {
//                    TLRPC.Update update = updates.updates.get(a);
//                    if (update instanceof TLRPC.TL_updateNewChannelMessage) {
//                        if (((TLRPC.TL_updateNewChannelMessage) update).message.action instanceof TLRPC.TL_messageActionChatAddUser) {
//                            hasJoinMessage = true;
//                            break;
//                        }
//                    }
//                }
//                processUpdates(updates, false);
//                if (isChannel) {
//                    if (!hasJoinMessage && inputUser instanceof TLRPC.TL_inputUserSelf) {
//                        generateJoinMessage(chat_id, true);
//                    }
//                    AndroidUtilities.runOnUIThread(() -> loadFullChat(chat_id, 0, true), 1000);
//                }
//                if (isChannel && inputUser instanceof TLRPC.TL_inputUserSelf) {
//                    getMessagesStorage().updateDialogsWithDeletedMessages(new ArrayList<>(), null, true, chat_id);
//                }
//                if (onFinishRunnable != null) {
//                    AndroidUtilities.runOnUIThread(onFinishRunnable);
//                }
//            });
        } else {
//            if (info instanceof TLRPC.TL_chatFull) {
//                for (int a = 0; a < info.participants.participants.size(); a++) {
//                    if (info.participants.participants.get(a).user_id == user.id) {
//                        return;
//                    }
//                }
//
//                TLRPC.Chat chat = getChat(chat_id);
//                chat.participants_count++;
//                ArrayList<TLRPC.Chat> chatArrayList = new ArrayList<>();
//                chatArrayList.add(chat);
//                getMessagesStorage().putUsersAndChats(null, chatArrayList, true, true);
//
//                TLRPC.TL_chatParticipant newPart = new TLRPC.TL_chatParticipant();
//                newPart.user_id = user.id;
//                newPart.inviter_id = getUserConfig().getClientUserId();
//                newPart.date = getConnectionsManager().getCurrentTime();
//                info.participants.participants.add(0, newPart);
//                getMessagesStorage().updateChatInfo(info, true);
//                getNotificationCenter().postNotificationName(NotificationCenter.chatInfoDidLoad, info, 0, false, null);
//                getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, UPDATE_MASK_CHAT_MEMBERS);
//            }
        }
    }


    public boolean checkCanOpenChat(Bundle bundle, BaseFragment fragment) {
        return checkCanOpenChat(bundle, fragment, null);
    }

    public boolean checkCanOpenChat(final Bundle bundle, final BaseFragment fragment, MessageObject originalMessage) {
        if (bundle == null || fragment == null) {
            return true;
        }
        User user = null;
        Chat chat = null;
        int user_id = bundle.getInt("user_id", 0);
        int chat_id = bundle.getInt("chat_id", 0);
        int messageId = bundle.getInt("message_id", 0);
        if (user_id != 0) {
            user = getUser(user_id);
        } else if (chat_id != 0) {
            chat = getChat(chat_id);
        }
        if (user == null && chat == null) {
            return true;
        }
        String reason = null;
//        if (chat != null) {
//            reason = getRestrictionReason(chat.restriction_reason);
//        } else if (user != null) {
//            reason = getRestrictionReason(user.restriction_reason);
//        }
//        if (reason != null) {
//            showCantOpenAlert(fragment, reason);
//            return false;
//        }
        if (messageId != 0 && originalMessage != null && chat != null && chat.access_hash == 0) {
            int did = (int) originalMessage.getDialogId();
            if (did != 0) {
                final AlertDialog progressDialog = new AlertDialog(fragment.getParentActivity(), 3);
                //TODO 发起请求
//                TLObject req;
//                if (did < 0) {
//                    chat = getChat(-did);
//                }
//                if (did > 0 || !ChatObject.isChannel(chat)) {
//                    TLRPC.TL_messages_getMessages request = new TLRPC.TL_messages_getMessages();
//                    request.id.add(originalMessage.getId());
//                    req = request;
//                } else {
//                    chat = getChat(-did);
//                    TLRPC.TL_channels_getMessages request = new TLRPC.TL_channels_getMessages();
//                    request.channel = getInputChannel(chat);
//                    request.id.add(originalMessage.getId());
//                    req = request;
//                }
//                final int reqId = getConnectionsManager().sendRequest(req, (response, error) -> {
//                    if (response != null) {
//                        AndroidUtilities.runOnUIThread(() -> {
//                            try {
//                                progressDialog.dismiss();
//                            } catch (Exception e) {
//                                FileLog.e(e);
//                            }
//                            TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
//                            putUsers(res.users, false);
//                            putChats(res.chats, false);
//                            getMessagesStorage().putUsersAndChats(res.users, res.chats, true, true);
//                            fragment.presentFragment(new ChatActivity(bundle), true);
//                        });
//                    }
//                });
                progressDialog.setOnCancelListener(dialog -> {
//                    getConnectionsManager().cancelRequest(reqId, true);TODO 取消请求
                    if (fragment != null) {
                        fragment.setVisibleDialog(null);
                    }
                });
                fragment.setVisibleDialog(progressDialog);
                progressDialog.show();
                return false;
            }
        }
        return true;
    }
    public void hidePeerSettingsBar(final long dialogId, User currentUser, Chat currentChat) {
        if (currentUser == null && currentChat == null) {
            return;
        }
        SharedPreferences.Editor editor = notificationsPreferences.edit();
        editor.putInt("dialog_bar_vis3" + dialogId, 3);
        editor.commit();
        if ((int) dialogId != 0) {
//            TLRPC.TL_messages_hidePeerSettingsBar req = new TLRPC.TL_messages_hidePeerSettingsBar();
//            if (currentUser != null) {
//                req.peer = getInputPeer(currentUser.id);
//            } else if (currentChat != null) {
//                req.peer = getInputPeer(-currentChat.id);
//            }
//            getConnectionsManager().sendRequest(req, (response, error) -> {
//
//            });
        }
    }

    public void pinMessage(Chat chat, User user, int id, boolean notify) {
        if (chat == null && user == null) {
            return;
        }
    }

    public void performLogout(int type) {
        getUserConfig().clearConfig();
        getNotificationCenter().postNotificationName(NotificationCenter.appDidLogout);
        getMessagesStorage().cleanup(false);
    }

    public String getAdminRank(int chatId, int uid) {
        return null;
//        SparseArray<String> array = channelAdmins.get(chatId);
//        if (array == null) {
//            return null;
//        }
//        return array.get(uid);
    }


    public boolean isDialogMuted(long dialog_id) {
        return isDialogMuted(dialog_id, null);
    }

    public boolean isDialogMuted(long dialog_id, Chat chat) {
        int mute_type = notificationsPreferences.getInt("notify2_" + dialog_id, -1);
        if (mute_type == -1) {
            Boolean forceChannel;
            if (chat != null) {
                forceChannel = ChatObject.isChannel(chat) && !chat.megagroup;
            } else {
                forceChannel = null;
            }
            return !getNotificationsController().isGlobalNotificationsEnabled(dialog_id, forceChannel);
        }
        if (mute_type == 2) {
            return true;
        } else if (mute_type == 3) {
            int mute_until = notificationsPreferences.getInt("notifyuntil_" + dialog_id, 0);
            if (mute_until >= getConnectionsManager().getCurrentTime()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSupportUser(User user) {
        return user != null && (user.support || user.id == 777000 ||
                user.id == 333000 || user.id == 4240000 || user.id == 4244000 ||
                user.id == 4245000 || user.id == 4246000 || user.id == 410000 ||
                user.id == 420000 || user.id == 431000 || user.id == 431415000 ||
                user.id == 434000 || user.id == 4243000 || user.id == 439000 ||
                user.id == 449000 || user.id == 450000 || user.id == 452000 ||
                user.id == 454000 || user.id == 4254000 || user.id == 455000 ||
                user.id == 460000 || user.id == 470000 || user.id == 479000 ||
                user.id == 796000 || user.id == 482000 || user.id == 490000 ||
                user.id == 496000 || user.id == 497000 || user.id == 498000 ||
                user.id == 4298000);
    }
    public void openByUserName(String username, final BaseFragment fragment, final int type) {
        if (username == null || fragment == null) {
            return;
        }
//        TLObject object = getUserOrChat(username);
//        User user = null;
//        Chat chat = null;
//        if (object instanceof User) {
//            user = (User) object;
//            if (user.min) {
//                user = null;
//            }
//        } else if (object instanceof Chat) {
//            chat = (Chat) object;
//            if (chat.min) {
//                chat = null;
//            }
//        }
//        if (user != null) {
//            openChatOrProfileWith(user, null, fragment, type, false);
//        } else if (chat != null) {
//            openChatOrProfileWith(null, chat, fragment, 1, false);
//        }
    }
    public boolean isChannelAdminsLoaded(int chatId) {
//        return channelAdmins.get(chatId) != null;
        return true;
    }
    public void loadChannelAdmins(final int chatId, final boolean cache) {
//        int loadTime = loadingChannelAdmins.get(chatId);
//        if (SystemClock.elapsedRealtime() - loadTime < 60) {
//            return;
//        }
//        loadingChannelAdmins.put(chatId, (int) (SystemClock.elapsedRealtime() / 1000));
        if (cache) {
            getMessagesStorage().loadChannelAdmins(chatId);
        }
    }

    public void saveGif(Object parentObject, Document document) {
        if (parentObject == null || !MessageObject.isGifDocument(document)) {
            return;
        }
    }
    public void reloadWebPages(final long dialog_id, HashMap<String, ArrayList<MessageObject>> webpagesToReload, boolean scheduled) {
//        HashMap<String, ArrayList<MessageObject>> map = scheduled ? reloadingScheduledWebpages : reloadingWebpages;
//        LongSparseArray<ArrayList<MessageObject>> array = scheduled ? reloadingScheduledWebpagesPending : reloadingWebpagesPending;
//
//        for (HashMap.Entry<String, ArrayList<MessageObject>> entry : webpagesToReload.entrySet()) {
//            final String url = entry.getKey();
//            final ArrayList<MessageObject> messages = entry.getValue();
//            ArrayList<MessageObject> arrayList = map.get(url);
//            if (arrayList == null) {
//                arrayList = new ArrayList<>();
//                map.put(url, arrayList);
//            }
//            arrayList.addAll(messages);
//        }
    }
    public void loadChannelParticipants(final Integer chat_id) {
//        if (loadingFullParticipants.contains(chat_id) || loadedFullParticipants.contains(chat_id)) {
//            return;
//        }
//        loadingFullParticipants.add(chat_id);
    }
    public boolean isJoiningChannel(final int chat_id) {
//        return joiningToChannels.contains(chat_id);
        return false;
    }
    public LongSparseArray<CharSequence> printingStrings = new LongSparseArray<>();
    public LongSparseArray<Integer> printingStringsTypes = new LongSparseArray<>();
    public ConcurrentHashMap<Integer, User> getUsers() {
        return users;
    }   public ConcurrentHashMap<Integer, Chat> getChats() {
        return chats;
    }

    //region NotificationCenter.NotificationCenterDelegate
    @Override
    public void didReceivedNotification(int id, int account, Object... args) {

    }
    //endregion
}

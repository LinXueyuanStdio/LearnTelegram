package com.demo.chat.controller;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteCursor;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.util.LongSparseArray;
import android.util.SparseArray;

import com.demo.chat.ApplicationLoader;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.BuildVars;
import com.demo.chat.messager.Emoji;
import com.demo.chat.messager.FileLog;
import com.demo.chat.messager.ImageLoader;
import com.demo.chat.messager.NotificationCenter;
import com.demo.chat.messager.SerializedData;
import com.demo.chat.messager.Utilities;
import com.demo.chat.model.Chat;
import com.demo.chat.model.Message;
import com.demo.chat.model.MessageObject;
import com.demo.chat.model.User;
import com.demo.chat.model.action.ChatObject;
import com.demo.chat.model.small.Document;
import com.demo.chat.model.small.MessageEntity;
import com.demo.chat.ui.Components.TextStyleSpan;
import com.demo.chat.ui.Components.URLSpanReplacement;
import com.demo.chat.ui.Components.URLSpanUserMention;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
@SuppressWarnings("unchecked")
public class MediaDataController extends BaseController {

    private static volatile MediaDataController[] Instance = new MediaDataController[UserConfig.MAX_ACCOUNT_COUNT];
    public static MediaDataController getInstance(int num) {
        MediaDataController localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (MediaDataController.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new MediaDataController(num);
                }
            }
        }
        return localInstance;
    }

    public MediaDataController(int num) {
        super(num);

        if (currentAccount == 0) {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences("drafts", Activity.MODE_PRIVATE);
        } else {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences("drafts" + currentAccount, Activity.MODE_PRIVATE);
        }
        Map<String, ?> values = preferences.getAll();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            try {
                String key = entry.getKey();
                long did = Utilities.parseLong(key);
                byte[] bytes = Utilities.hexToBytes((String) entry.getValue());
                SerializedData serializedData = new SerializedData(bytes);
                if (key.startsWith("r_")) {
                    Message message = Message.TLdeserialize(serializedData, serializedData.readInt32(true), true);
                    message.readAttachPath(serializedData, getUserConfig().clientUserId);
                    if (message != null) {
                        draftMessages.put(did, message);
                    }
                } else {
                    DraftMessage draftMessage = DraftMessage.TLdeserialize(serializedData, serializedData.readInt32(true), true);
                    if (draftMessage != null) {
                        drafts.put(did, draftMessage);
                    }
                }
                serializedData.cleanup();
            } catch (Exception e) {
                //igonre
            }
        }
    }

    public static final int TYPE_IMAGE = 0;
    public static final int TYPE_MASK = 1;
    public static final int TYPE_FAVE = 2;
    public static final int TYPE_FEATURED = 3;
    public static final int TYPE_EMOJI = 4;

    //region TODO 这里只是复制过来，还需要初始化等处理
    private boolean[] stickersLoaded = new boolean[5];

    public boolean canAddStickerToFavorites() {
        return !stickersLoaded[0] || !recentStickers[TYPE_FAVE].isEmpty();
    }
    private ArrayList<Document>[] recentStickers = new ArrayList[]{new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};
    public boolean isStickerInFavorites(Document document) {
        if (document == null) {
            return false;
        }
        for (int a = 0; a < recentStickers[TYPE_FAVE].size(); a++) {
            Document d = recentStickers[TYPE_FAVE].get(a);
            if (d.id == document.id && d.dc_id == document.dc_id) {
                return true;
            }
        }
        return false;
    }
    //endregion

    //region ---------------- MESSAGE SEARCH ----------------
    private int reqId;
    private int mergeReqId;
    private long lastMergeDialogId;
    private long lastDialogId;
    private int lastReqId;
    private int lastGuid;
    private User lastSearchUser;
    private int[] messagesSearchCount = new int[]{0, 0};
    private boolean[] messagesSearchEndReached = new boolean[]{false, false};
    private ArrayList<MessageObject> searchResultMessages = new ArrayList<>();
    private SparseArray<MessageObject>[] searchResultMessagesMap = new SparseArray[]{new SparseArray<>(), new SparseArray<>()};
    private String lastSearchQuery;
    private int lastReturnedNum;
    private boolean loadingMoreSearchMessages;

    private int getMask() {
        int mask = 0;
        if (lastReturnedNum < searchResultMessages.size() - 1 || !messagesSearchEndReached[0] || !messagesSearchEndReached[1]) {
            mask |= 1;
        }
        if (lastReturnedNum > 0) {
            mask |= 2;
        }
        return mask;
    }

    public ArrayList<MessageObject> getFoundMessageObjects() {
        return searchResultMessages;
    }

    public void clearFoundMessageObjects() {
        searchResultMessages.clear();
    }

    public boolean isMessageFound(final int messageId, boolean mergeDialog) {
        return searchResultMessagesMap[mergeDialog ? 1 : 0].indexOfKey(messageId) >= 0;
    }

    public void searchMessagesInChat(String query, final long dialogId, final long mergeDialogId, final int guid, final int direction, User user) {
        searchMessagesInChat(query, dialogId, mergeDialogId, guid, direction, false, user, true);
    }

    public void jumpToSearchedMessage(int guid, int index) {
        if (index < 0 || index >= searchResultMessages.size()) {
            return;
        }
        lastReturnedNum = index;
        MessageObject messageObject = searchResultMessages.get(lastReturnedNum);
        getNotificationCenter().postNotificationName(NotificationCenter.chatSearchResultsAvailable, guid, messageObject.getId(), getMask(), messageObject.getDialogId(), lastReturnedNum, messagesSearchCount[0] + messagesSearchCount[1], true);
    }

    public void loadMoreSearchMessages() {
        if (loadingMoreSearchMessages || messagesSearchEndReached[0] && lastMergeDialogId == 0 && messagesSearchEndReached[1]) {
            return;
        }
        int temp = searchResultMessages.size();
        lastReturnedNum = searchResultMessages.size();
        searchMessagesInChat(null, lastDialogId, lastMergeDialogId, lastGuid, 1, false, lastSearchUser, false);
        lastReturnedNum = temp;
        loadingMoreSearchMessages = true;
    }

    private void searchMessagesInChat(String query, final long dialogId, final long mergeDialogId, final int guid, final int direction, final boolean internal, final User user, boolean jumpToMessage) {
        int max_id = 0;
        long queryWithDialog = dialogId;
        boolean firstQuery = !internal;
        if (reqId != 0) {
            getConnectionsManager().cancelRequest(reqId, true);
            reqId = 0;
        }
        if (mergeReqId != 0) {
            getConnectionsManager().cancelRequest(mergeReqId, true);
            mergeReqId = 0;
        }
        if (query == null) {
            if (searchResultMessages.isEmpty()) {
                return;
            }
            if (direction == 1) {
                lastReturnedNum++;
                if (lastReturnedNum < searchResultMessages.size()) {
                    MessageObject messageObject = searchResultMessages.get(lastReturnedNum);
                    getNotificationCenter().postNotificationName(NotificationCenter.chatSearchResultsAvailable, guid, messageObject.getId(), getMask(), messageObject.getDialogId(), lastReturnedNum, messagesSearchCount[0] + messagesSearchCount[1], jumpToMessage);
                    return;
                } else {
                    if (messagesSearchEndReached[0] && mergeDialogId == 0 && messagesSearchEndReached[1]) {
                        lastReturnedNum--;
                        return;
                    }
                    firstQuery = false;
                    query = lastSearchQuery;
                    MessageObject messageObject = searchResultMessages.get(searchResultMessages.size() - 1);
                    if (messageObject.getDialogId() == dialogId && !messagesSearchEndReached[0]) {
                        max_id = messageObject.getId();
                        queryWithDialog = dialogId;
                    } else {
                        if (messageObject.getDialogId() == mergeDialogId) {
                            max_id = messageObject.getId();
                        }
                        queryWithDialog = mergeDialogId;
                        messagesSearchEndReached[1] = false;
                    }
                }
            } else if (direction == 2) {
                lastReturnedNum--;
                if (lastReturnedNum < 0) {
                    lastReturnedNum = 0;
                    return;
                }
                if (lastReturnedNum >= searchResultMessages.size()) {
                    lastReturnedNum = searchResultMessages.size() - 1;
                }
                MessageObject messageObject = searchResultMessages.get(lastReturnedNum);
                getNotificationCenter().postNotificationName(NotificationCenter.chatSearchResultsAvailable, guid, messageObject.getId(), getMask(), messageObject.getDialogId(), lastReturnedNum, messagesSearchCount[0] + messagesSearchCount[1], jumpToMessage);
                return;
            } else {
                return;
            }
        } else if (firstQuery) {
            messagesSearchEndReached[0] = messagesSearchEndReached[1] = false;
            messagesSearchCount[0] = messagesSearchCount[1] = 0;
            searchResultMessages.clear();
            searchResultMessagesMap[0].clear();
            searchResultMessagesMap[1].clear();
            getNotificationCenter().postNotificationName(NotificationCenter.chatSearchResultsLoading, guid);
        }
        if (messagesSearchEndReached[0] && !messagesSearchEndReached[1] && mergeDialogId != 0) {
            queryWithDialog = mergeDialogId;
        }
        if (queryWithDialog == dialogId && firstQuery) {
            if (mergeDialogId != 0) {
                InputPeer inputPeer = getMessagesController().getInputPeer((int) mergeDialogId);
                if (inputPeer == null) {
                    return;
                }
                final TL_messages_search req = new TL_messages_search();
                req.peer = inputPeer;
                lastMergeDialogId = mergeDialogId;
                req.limit = 1;
                req.q = query != null ? query : "";
                if (user != null) {
                    req.from_id = getMessagesController().getInputUser(user);
                    req.flags |= 1;
                }
                req.filter = new TL_inputMessagesFilterEmpty();
                mergeReqId = getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                    if (lastMergeDialogId == mergeDialogId) {
                        mergeReqId = 0;
                        if (response != null) {
                            messages_Messages res = (messages_Messages) response;
                            messagesSearchEndReached[1] = res.messages.isEmpty();
                            messagesSearchCount[1] = res instanceof TL_messages_messagesSlice ? res.count : res.messages.size();
                            searchMessagesInChat(req.q, dialogId, mergeDialogId, guid, direction, true, user, jumpToMessage);
                        }
                    }
                }), ConnectionsManager.RequestFlagFailOnServerErrors);
                return;
            } else {
                lastMergeDialogId = 0;
                messagesSearchEndReached[1] = true;
                messagesSearchCount[1] = 0;
            }
        }
        final TL_messages_search req = new TL_messages_search();
        req.peer = getMessagesController().getInputPeer((int) queryWithDialog);
        if (req.peer == null) {
            return;
        }
        lastGuid = guid;
        lastDialogId = dialogId;
        lastSearchUser = user;
        req.limit = 21;
        req.q = query != null ? query : "";
        req.offset_id = max_id;
        if (user != null) {
            req.from_id = getMessagesController().getInputUser(user);
            req.flags |= 1;
        }
        req.filter = new TL_inputMessagesFilterEmpty();
        final int currentReqId = ++lastReqId;
        lastSearchQuery = query;
        final long queryWithDialogFinal = queryWithDialog;
        reqId = getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (currentReqId == lastReqId) {
                reqId = 0;
                if (!jumpToMessage) {
                    loadingMoreSearchMessages = false;
                }
                if (response != null) {
                    messages_Messages res = (messages_Messages) response;
                    for (int a = 0; a < res.messages.size(); a++) {
                        Message message = res.messages.get(a);
                        if (message instanceof TL_messageEmpty || message.action instanceof TL_messageActionHistoryClear) {
                            res.messages.remove(a);
                            a--;
                        }
                    }
                    getMessagesStorage().putUsersAndChats(res.users, res.chats, true, true);
                    getMessagesController().putUsers(res.users, false);
                    getMessagesController().putChats(res.chats, false);
                    if (req.offset_id == 0 && queryWithDialogFinal == dialogId) {
                        lastReturnedNum = 0;
                        searchResultMessages.clear();
                        searchResultMessagesMap[0].clear();
                        searchResultMessagesMap[1].clear();
                        messagesSearchCount[0] = 0;
                        getNotificationCenter().postNotificationName(NotificationCenter.chatSearchResultsLoading, guid);
                    }
                    boolean added = false;
                    int N = Math.min(res.messages.size(), 20);
                    for (int a = 0; a < N; a++) {
                        Message message = res.messages.get(a);
                        added = true;
                        MessageObject messageObject = new MessageObject(currentAccount, message, false);
                        searchResultMessages.add(messageObject);
                        searchResultMessagesMap[queryWithDialogFinal == dialogId ? 0 : 1].put(messageObject.getId(), messageObject);
                    }
                    messagesSearchEndReached[queryWithDialogFinal == dialogId ? 0 : 1] = res.messages.size() != 21;
                    messagesSearchCount[queryWithDialogFinal == dialogId ? 0 : 1] = res instanceof TL_messages_messagesSlice || res instanceof TL_messages_channelMessages ? res.count : res.messages.size();
                    if (searchResultMessages.isEmpty()) {
                        getNotificationCenter().postNotificationName(NotificationCenter.chatSearchResultsAvailable, guid, 0, getMask(), (long) 0, 0, 0, jumpToMessage);
                    } else {
                        if (added) {
                            if (lastReturnedNum >= searchResultMessages.size()) {
                                lastReturnedNum = searchResultMessages.size() - 1;
                            }
                            MessageObject messageObject = searchResultMessages.get(lastReturnedNum);
                            getNotificationCenter().postNotificationName(NotificationCenter.chatSearchResultsAvailable, guid, messageObject.getId(), getMask(), messageObject.getDialogId(), lastReturnedNum, messagesSearchCount[0] + messagesSearchCount[1], jumpToMessage);
                        }
                    }
                    if (queryWithDialogFinal == dialogId && messagesSearchEndReached[0] && mergeDialogId != 0 && !messagesSearchEndReached[1]) {
                        searchMessagesInChat(lastSearchQuery, dialogId, mergeDialogId, guid, 0, true, user, jumpToMessage);
                    }
                }
            }
        }), ConnectionsManager.RequestFlagFailOnServerErrors);
    }

    public String getLastSearchQuery() {
        return lastSearchQuery;
    }
    //endregion ---------------- MESSAGE SEARCH END ----------------


    //region ---------------- MESSAGES ----------------
    private static Comparator<MessageEntity> entityComparator = (entity1, entity2) -> {
        if (entity1.offset > entity2.offset) {
            return 1;
        } else if (entity1.offset < entity2.offset) {
            return -1;
        }
        return 0;
    };

    public MessageObject loadPinnedMessage(final long dialogId, final int channelId, final int mid, boolean useQueue) {
        if (useQueue) {
            getMessagesStorage().getStorageQueue().postRunnable(() -> loadPinnedMessageInternal(dialogId, channelId, mid, false));
        } else {
            return loadPinnedMessageInternal(dialogId, channelId, mid, true);
        }
        return null;
    }

    private MessageObject loadPinnedMessageInternal(final long dialogId, final int channelId, final int mid, boolean returnValue) {
        try {
            long messageId;
            if (channelId != 0) {
                messageId = ((long) mid) | ((long) channelId) << 32;
            } else {
                messageId = mid;
            }

            Message result = null;
            final ArrayList<User> users = new ArrayList<>();
            final ArrayList<Chat> chats = new ArrayList<>();
            ArrayList<Integer> usersToLoad = new ArrayList<>();
            ArrayList<Integer> chatsToLoad = new ArrayList<>();

            SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT data, mid, date FROM messages WHERE mid = %d", messageId));
            if (cursor.next()) {
                NativeByteBuffer data = cursor.byteBufferValue(0);
                if (data != null) {
                    result = Message.TLdeserialize(data, data.readInt32(false), false);
                    result.readAttachPath(data, getUserConfig().clientUserId);
                    data.reuse();
                    if (result.action instanceof TL_messageActionHistoryClear) {
                        result = null;
                    } else {
                        result.id = cursor.intValue(1);
                        result.date = cursor.intValue(2);
                        result.dialog_id = dialogId;
                        MessagesStorage.addUsersAndChatsFromMessage(result, usersToLoad, chatsToLoad);
                    }
                }
            }
            cursor.dispose();

            if (result == null) {
                cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT data FROM chat_pinned WHERE uid = %d", dialogId));
                if (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        result = Message.TLdeserialize(data, data.readInt32(false), false);
                        result.readAttachPath(data, getUserConfig().clientUserId);
                        data.reuse();
                        if (result.id != mid || result.action instanceof TL_messageActionHistoryClear) {
                            result = null;
                        } else {
                            result.dialog_id = dialogId;
                            MessagesStorage.addUsersAndChatsFromMessage(result, usersToLoad, chatsToLoad);
                        }
                    }
                }
                cursor.dispose();
            }

            if (result == null) {
                if (channelId != 0) {
                    final TL_channels_getMessages req = new TL_channels_getMessages();
                    req.channel = getMessagesController().getInputChannel(channelId);
                    req.id.add(mid);
                    getConnectionsManager().sendRequest(req, (response, error) -> {
                        boolean ok = false;
                        if (error == null) {
                            messages_Messages messagesRes = (messages_Messages) response;
                            removeEmptyMessages(messagesRes.messages);
                            if (!messagesRes.messages.isEmpty()) {
                                ImageLoader.saveMessagesThumbs(messagesRes.messages);
                                broadcastPinnedMessage(messagesRes.messages.get(0), messagesRes.users, messagesRes.chats, false, false);
                                getMessagesStorage().putUsersAndChats(messagesRes.users, messagesRes.chats, true, true);
                                savePinnedMessage(messagesRes.messages.get(0));
                                ok = true;
                            }
                        }
                        if (!ok) {
                            getMessagesStorage().updateChatPinnedMessage(channelId, 0);
                        }
                    });
                } else {
                    final TL_messages_getMessages req = new TL_messages_getMessages();
                    req.id.add(mid);
                    getConnectionsManager().sendRequest(req, (response, error) -> {
                        boolean ok = false;
                        if (error == null) {
                            messages_Messages messagesRes = (messages_Messages) response;
                            removeEmptyMessages(messagesRes.messages);
                            if (!messagesRes.messages.isEmpty()) {
                                ImageLoader.saveMessagesThumbs(messagesRes.messages);
                                broadcastPinnedMessage(messagesRes.messages.get(0), messagesRes.users, messagesRes.chats, false, false);
                                getMessagesStorage().putUsersAndChats(messagesRes.users, messagesRes.chats, true, true);
                                savePinnedMessage(messagesRes.messages.get(0));
                                ok = true;
                            }
                        }
                        if (!ok) {
                            getMessagesStorage().updateChatPinnedMessage(channelId, 0);
                        }
                    });
                }
            } else {
                if (returnValue) {
                    return broadcastPinnedMessage(result, users, chats, true, returnValue);
                } else {
                    if (!usersToLoad.isEmpty()) {
                        getMessagesStorage().getUsersInternal(TextUtils.join(",", usersToLoad), users);
                    }
                    if (!chatsToLoad.isEmpty()) {
                        getMessagesStorage().getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
                    }
                    broadcastPinnedMessage(result, users, chats, true, false);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    private void savePinnedMessage(final Message result) {
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                long dialogId;
                if (result.to_id.channel_id != 0) {
                    dialogId = -result.to_id.channel_id;
                } else if (result.to_id.chat_id != 0) {
                    dialogId = -result.to_id.chat_id;
                } else if (result.to_id.user_id != 0) {
                    dialogId = result.to_id.user_id;
                } else {
                    return;
                }
                getMessagesStorage().getDatabase().beginTransaction();
                SQLitePreparedStatement state = getMessagesStorage().getDatabase().executeFast("REPLACE INTO chat_pinned VALUES(?, ?, ?)");
                NativeByteBuffer data = new NativeByteBuffer(result.getObjectSize());
                result.serializeToStream(data);
                state.requery();
                state.bindLong(1, dialogId);
                state.bindInteger(2, result.id);
                state.bindByteBuffer(3, data);
                state.step();
                data.reuse();
                state.dispose();
                getMessagesStorage().getDatabase().commitTransaction();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    private MessageObject broadcastPinnedMessage(final Message result, final ArrayList<User> users, final ArrayList<Chat> chats, final boolean isCache, boolean returnValue) {
        final SparseArray<User> usersDict = new SparseArray<>();
        for (int a = 0; a < users.size(); a++) {
            User user = users.get(a);
            usersDict.put(user.id, user);
        }
        final SparseArray<Chat> chatsDict = new SparseArray<>();
        for (int a = 0; a < chats.size(); a++) {
            Chat chat = chats.get(a);
            chatsDict.put(chat.id, chat);
        }
        if (returnValue) {
            return new MessageObject(currentAccount, result, usersDict, chatsDict, false);
        } else {
            AndroidUtilities.runOnUIThread(() -> {
                getMessagesController().putUsers(users, isCache);
                getMessagesController().putChats(chats, isCache);
                getNotificationCenter().postNotificationName(NotificationCenter.pinnedMessageDidLoad, new MessageObject(currentAccount, result, usersDict, chatsDict, false));
            });
        }
        return null;
    }

    private static void removeEmptyMessages(ArrayList<Message> messages) {
        for (int a = 0; a < messages.size(); a++) {
            Message message = messages.get(a);
            if (message == null || message instanceof TL_messageEmpty || message.action instanceof TL_messageActionHistoryClear) {
                messages.remove(a);
                a--;
            }
        }
    }

    public void loadReplyMessagesForMessages(final ArrayList<MessageObject> messages, final long dialogId, boolean scheduled, Runnable callback) {
        if ((int) dialogId == 0) {
            final ArrayList<Long> replyMessages = new ArrayList<>();
            final LongSparseArray<ArrayList<MessageObject>> replyMessageRandomOwners = new LongSparseArray<>();
            for (int a = 0; a < messages.size(); a++) {
                MessageObject messageObject = messages.get(a);
                if (messageObject.isReply() && messageObject.replyMessageObject == null) {
                    long id = messageObject.messageOwner.reply_to_random_id;
                    ArrayList<MessageObject> messageObjects = replyMessageRandomOwners.get(id);
                    if (messageObjects == null) {
                        messageObjects = new ArrayList<>();
                        replyMessageRandomOwners.put(id, messageObjects);
                    }
                    messageObjects.add(messageObject);
                    if (!replyMessages.contains(id)) {
                        replyMessages.add(id);
                    }
                }
            }
            if (replyMessages.isEmpty()) {
                if (callback != null) {
                    callback.run();
                }
                return;
            }

            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                try {
                    SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT m.data, m.mid, m.date, r.random_id FROM randoms as r INNER JOIN messages as m ON r.mid = m.mid WHERE r.random_id IN(%s)", TextUtils.join(",", replyMessages)));
                    while (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            Message message = Message.TLdeserialize(data, data.readInt32(false), false);
                            message.readAttachPath(data, getUserConfig().clientUserId);
                            data.reuse();
                            message.id = cursor.intValue(1);
                            message.date = cursor.intValue(2);
                            message.dialog_id = dialogId;

                            long value = cursor.longValue(3);
                            ArrayList<MessageObject> arrayList = replyMessageRandomOwners.get(value);
                            replyMessageRandomOwners.remove(value);
                            if (arrayList != null) {
                                MessageObject messageObject = new MessageObject(currentAccount, message, false);
                                for (int b = 0; b < arrayList.size(); b++) {
                                    MessageObject object = arrayList.get(b);
                                    object.replyMessageObject = messageObject;
                                    object.messageOwner.reply_to_msg_id = messageObject.getId();
                                    if (object.isMegagroup()) {
                                        object.replyMessageObject.messageOwner.flags |= MESSAGE_FLAG_MEGAGROUP;
                                    }
                                }
                            }
                        }
                    }
                    cursor.dispose();
                    if (replyMessageRandomOwners.size() != 0) {
                        for (int b = 0; b < replyMessageRandomOwners.size(); b++) {
                            ArrayList<MessageObject> arrayList = replyMessageRandomOwners.valueAt(b);
                            for (int a = 0; a < arrayList.size(); a++) {
                                arrayList.get(a).messageOwner.reply_to_random_id = 0;
                            }
                        }
                    }
                    AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.replyMessagesDidLoad, dialogId));
                    if (callback != null) {
                        callback.run();
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
        } else {
            final ArrayList<Integer> replyMessages = new ArrayList<>();
            final SparseArray<ArrayList<MessageObject>> replyMessageOwners = new SparseArray<>();
            final StringBuilder stringBuilder = new StringBuilder();
            int channelId = 0;
            for (int a = 0; a < messages.size(); a++) {
                MessageObject messageObject = messages.get(a);
                if (messageObject.getId() > 0 && messageObject.isReply() && messageObject.replyMessageObject == null) {
                    int id = messageObject.messageOwner.reply_to_msg_id;
                    long messageId = id;
                    if (messageObject.messageOwner.to_id.channel_id != 0) {
                        messageId |= ((long) messageObject.messageOwner.to_id.channel_id) << 32;
                        channelId = messageObject.messageOwner.to_id.channel_id;
                    }
                    if (stringBuilder.length() > 0) {
                        stringBuilder.append(',');
                    }
                    stringBuilder.append(messageId);
                    ArrayList<MessageObject> messageObjects = replyMessageOwners.get(id);
                    if (messageObjects == null) {
                        messageObjects = new ArrayList<>();
                        replyMessageOwners.put(id, messageObjects);
                    }
                    messageObjects.add(messageObject);
                    if (!replyMessages.contains(id)) {
                        replyMessages.add(id);
                    }
                }
            }
            if (replyMessages.isEmpty()) {
                if (callback != null) {
                    callback.run();
                }
                return;
            }

            final int channelIdFinal = channelId;
            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                try {
                    final ArrayList<Message> result = new ArrayList<>();
                    final ArrayList<User> users = new ArrayList<>();
                    final ArrayList<Chat> chats = new ArrayList<>();
                    ArrayList<Integer> usersToLoad = new ArrayList<>();
                    ArrayList<Integer> chatsToLoad = new ArrayList<>();

                    SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT data, mid, date FROM messages WHERE mid IN(%s)", stringBuilder.toString()));
                    while (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            Message message = Message.TLdeserialize(data, data.readInt32(false), false);
                            message.readAttachPath(data, getUserConfig().clientUserId);
                            data.reuse();
                            message.id = cursor.intValue(1);
                            message.date = cursor.intValue(2);
                            message.dialog_id = dialogId;
                            MessagesStorage.addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad);
                            result.add(message);
                            replyMessages.remove((Integer) message.id);
                        }
                    }
                    cursor.dispose();

                    if (!usersToLoad.isEmpty()) {
                        getMessagesStorage().getUsersInternal(TextUtils.join(",", usersToLoad), users);
                    }
                    if (!chatsToLoad.isEmpty()) {
                        getMessagesStorage().getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
                    }
                    broadcastReplyMessages(result, replyMessageOwners, users, chats, dialogId, true);

                    if (!replyMessages.isEmpty()) {
                        if (channelIdFinal != 0) {
                            final TL_channels_getMessages req = new TL_channels_getMessages();
                            req.channel = getMessagesController().getInputChannel(channelIdFinal);
                            req.id = replyMessages;
                            getConnectionsManager().sendRequest(req, (response, error) -> {
                                if (error == null) {
                                    messages_Messages messagesRes = (messages_Messages) response;
                                    ImageLoader.saveMessagesThumbs(messagesRes.messages);
                                    broadcastReplyMessages(messagesRes.messages, replyMessageOwners, messagesRes.users, messagesRes.chats, dialogId, false);
                                    getMessagesStorage().putUsersAndChats(messagesRes.users, messagesRes.chats, true, true);
                                    saveReplyMessages(replyMessageOwners, messagesRes.messages, scheduled);
                                }
                                if (callback != null) {
                                    AndroidUtilities.runOnUIThread(callback);
                                }
                            });
                        } else {
                            TL_messages_getMessages req = new TL_messages_getMessages();
                            req.id = replyMessages;
                            getConnectionsManager().sendRequest(req, (response, error) -> {
                                if (error == null) {
                                    messages_Messages messagesRes = (messages_Messages) response;
                                    ImageLoader.saveMessagesThumbs(messagesRes.messages);
                                    broadcastReplyMessages(messagesRes.messages, replyMessageOwners, messagesRes.users, messagesRes.chats, dialogId, false);
                                    getMessagesStorage().putUsersAndChats(messagesRes.users, messagesRes.chats, true, true);
                                    saveReplyMessages(replyMessageOwners, messagesRes.messages, scheduled);
                                }
                                if (callback != null) {
                                    AndroidUtilities.runOnUIThread(callback);
                                }
                            });
                        }
                    } else {
                        if (callback != null) {
                            AndroidUtilities.runOnUIThread(callback);
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
        }
    }

    private void saveReplyMessages(final SparseArray<ArrayList<MessageObject>> replyMessageOwners, final ArrayList<Message> result, boolean scheduled) {
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                getMessagesStorage().getDatabase().beginTransaction();
                SQLitePreparedStatement state;
                if (scheduled) {
                    state = getMessagesStorage().getDatabase().executeFast("UPDATE scheduled_messages SET replydata = ? WHERE mid = ?");
                } else {
                    state = getMessagesStorage().getDatabase().executeFast("UPDATE messages SET replydata = ? WHERE mid = ?");
                }
                for (int a = 0; a < result.size(); a++) {
                    Message message = result.get(a);
                    ArrayList<MessageObject> messageObjects = replyMessageOwners.get(message.id);
                    if (messageObjects != null) {
                        NativeByteBuffer data = new NativeByteBuffer(message.getObjectSize());
                        message.serializeToStream(data);
                        for (int b = 0; b < messageObjects.size(); b++) {
                            MessageObject messageObject = messageObjects.get(b);
                            state.requery();
                            long messageId = messageObject.getId();
                            if (messageObject.messageOwner.to_id.channel_id != 0) {
                                messageId |= ((long) messageObject.messageOwner.to_id.channel_id) << 32;
                            }
                            state.bindByteBuffer(1, data);
                            state.bindLong(2, messageId);
                            state.step();
                        }
                        data.reuse();
                    }
                }
                state.dispose();
                getMessagesStorage().getDatabase().commitTransaction();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    private void broadcastReplyMessages(final ArrayList<Message> result, final SparseArray<ArrayList<MessageObject>> replyMessageOwners, final ArrayList<User> users, final ArrayList<Chat> chats, final long dialog_id, final boolean isCache) {
        final SparseArray<User> usersDict = new SparseArray<>();
        for (int a = 0; a < users.size(); a++) {
            User user = users.get(a);
            usersDict.put(user.id, user);
        }
        final SparseArray<Chat> chatsDict = new SparseArray<>();
        for (int a = 0; a < chats.size(); a++) {
            Chat chat = chats.get(a);
            chatsDict.put(chat.id, chat);
        }
        AndroidUtilities.runOnUIThread(() -> {
            getMessagesController().putUsers(users, isCache);
            getMessagesController().putChats(chats, isCache);
            boolean changed = false;
            for (int a = 0; a < result.size(); a++) {
                Message message = result.get(a);
                ArrayList<MessageObject> arrayList = replyMessageOwners.get(message.id);
                if (arrayList != null) {
                    MessageObject messageObject = new MessageObject(currentAccount, message, usersDict, chatsDict, false);
                    for (int b = 0; b < arrayList.size(); b++) {
                        MessageObject m = arrayList.get(b);
                        m.replyMessageObject = messageObject;
                        if (m.messageOwner.action instanceof TL_messageActionPinMessage) {
                            m.generatePinMessageText(null, null);
                        } else if (m.messageOwner.action instanceof TL_messageActionGameScore) {
                            m.generateGameMessageText(null);
                        } else if (m.messageOwner.action instanceof TL_messageActionPaymentSent) {
                            m.generatePaymentSentMessageText(null);
                        }
                        if (m.isMegagroup()) {
                            m.replyMessageObject.messageOwner.flags |= MESSAGE_FLAG_MEGAGROUP;
                        }
                    }
                    changed = true;
                }
            }
            if (changed) {
                getNotificationCenter().postNotificationName(NotificationCenter.replyMessagesDidLoad, dialog_id);
            }
        });
    }

    public static void sortEntities(ArrayList<MessageEntity> entities) {
        Collections.sort(entities, entityComparator);
    }

    private static boolean checkInclusion(int index, ArrayList<MessageEntity> entities, boolean end) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        int count = entities.size();
        for (int a = 0; a < count; a++) {
            MessageEntity entity = entities.get(a);
            if ((end ? entity.offset < index : entity.offset <= index) && entity.offset + entity.length > index) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkIntersection(int start, int end, ArrayList<MessageEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        int count = entities.size();
        for (int a = 0; a < count; a++) {
            MessageEntity entity = entities.get(a);
            if (entity.offset > start && entity.offset + entity.length <= end) {
                return true;
            }
        }
        return false;
    }

    private static void removeOffsetAfter(int start, int countToRemove, ArrayList<MessageEntity> entities) {
        int count = entities.size();
        for (int a = 0; a < count; a++) {
            MessageEntity entity = entities.get(a);
            if (entity.offset > start) {
                entity.offset -= countToRemove;
            }
        }
    }

    public CharSequence substring(CharSequence source, int start, int end) {
        if (source instanceof SpannableStringBuilder) {
            return source.subSequence(start, end);
        } else if (source instanceof SpannedString) {
            return source.subSequence(start, end);
        } else {
            return TextUtils.substring(source, start, end);
        }
    }

    private static CharacterStyle createNewSpan(CharacterStyle baseSpan, TextStyleSpan.TextStyleRun textStyleRun, TextStyleSpan.TextStyleRun newStyleRun, boolean allowIntersection) {
        TextStyleSpan.TextStyleRun run = new TextStyleSpan.TextStyleRun(textStyleRun);
        if (newStyleRun != null) {
            if (allowIntersection) {
                run.merge(newStyleRun);
            } else {
                run.replace(newStyleRun);
            }
        }
        if (baseSpan instanceof TextStyleSpan) {
            return new TextStyleSpan(run);
        } else if (baseSpan instanceof URLSpanReplacement) {
            URLSpanReplacement span = (URLSpanReplacement) baseSpan;
            return new URLSpanReplacement(span.getURL(), run);
        }
        return null;
    }

    public static void addStyleToText(TextStyleSpan span, int start, int end, Spannable editable, boolean allowIntersection) {
        try {
            CharacterStyle[] spans = editable.getSpans(start, end, CharacterStyle.class);
            if (spans != null && spans.length > 0) {
                for (int a = 0; a < spans.length; a++) {
                    CharacterStyle oldSpan = spans[a];
                    TextStyleSpan.TextStyleRun textStyleRun;
                    TextStyleSpan.TextStyleRun newStyleRun = span != null ? span.getTextStyleRun() : new TextStyleSpan.TextStyleRun();
                    if (oldSpan instanceof TextStyleSpan) {
                        TextStyleSpan textStyleSpan = (TextStyleSpan) oldSpan;
                        textStyleRun = textStyleSpan.getTextStyleRun();
                    } else if (oldSpan instanceof URLSpanReplacement) {
                        URLSpanReplacement urlSpanReplacement = (URLSpanReplacement) oldSpan;
                        textStyleRun = urlSpanReplacement.getTextStyleRun();
                        if (textStyleRun == null) {
                            textStyleRun = new TextStyleSpan.TextStyleRun();
                        }
                    } else {
                        continue;
                    }
                    if (textStyleRun == null) {
                        continue;
                    }
                    int spanStart = editable.getSpanStart(oldSpan);
                    int spanEnd = editable.getSpanEnd(oldSpan);
                    editable.removeSpan(oldSpan);
                    if (spanStart > start && end > spanEnd) {
                        editable.setSpan(createNewSpan(oldSpan, textStyleRun, newStyleRun, allowIntersection), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        if (span != null) {
                            editable.setSpan(new TextStyleSpan(new TextStyleSpan.TextStyleRun(newStyleRun)), spanEnd, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        end = spanStart;
                    } else {
                        int startTemp = start;
                        if (spanStart <= start) {
                            if (spanStart != start) {
                                editable.setSpan(createNewSpan(oldSpan, textStyleRun, null, allowIntersection), spanStart, start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            if (spanEnd > start) {
                                if (span != null) {
                                    editable.setSpan(createNewSpan(oldSpan, textStyleRun, newStyleRun, allowIntersection), start, Math.min(spanEnd, end), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }
                                start = spanEnd;
                            }
                        }
                        if (spanEnd >= end) {
                            if (spanEnd != end) {
                                editable.setSpan(createNewSpan(oldSpan, textStyleRun, null, allowIntersection), end, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            if (end > spanStart && spanEnd <= startTemp) {
                                if (span != null) {
                                    editable.setSpan(createNewSpan(oldSpan, textStyleRun, newStyleRun, allowIntersection), spanStart, Math.min(spanEnd, end), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }
                                end = spanStart;
                            }
                        }
                    }
                }
            }
            if (span != null && start < end) {
                editable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static ArrayList<TextStyleSpan.TextStyleRun> getTextStyleRuns(ArrayList<MessageEntity> entities, CharSequence text) {
        ArrayList<TextStyleSpan.TextStyleRun> runs = new ArrayList<>();
        ArrayList<MessageEntity> entitiesCopy = new ArrayList<>(entities);

        Collections.sort(entitiesCopy, (o1, o2) -> {
            if (o1.offset > o2.offset) {
                return 1;
            } else if (o1.offset < o2.offset) {
                return -1;
            }
            return 0;
        });
        for (int a = 0, N = entitiesCopy.size(); a < N; a++) {
            MessageEntity entity = entitiesCopy.get(a);
            if (entity.length <= 0 || entity.offset < 0 || entity.offset >= text.length()) {
                continue;
            } else if (entity.offset + entity.length > text.length()) {
                entity.length = text.length() - entity.offset;
            }

            TextStyleSpan.TextStyleRun newRun = new TextStyleSpan.TextStyleRun();
            newRun.start = entity.offset;
            newRun.end = newRun.start + entity.length;
            MessageEntity urlEntity = null;
            if (entity instanceof TL_messageEntityStrike) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_STRIKE;
            } else if (entity instanceof TL_messageEntityUnderline) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_UNDERLINE;
            } else if (entity instanceof TL_messageEntityBlockquote) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_QUOTE;
            } else if (entity instanceof TL_messageEntityBold) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_BOLD;
            } else if (entity instanceof TL_messageEntityItalic) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_ITALIC;
            } else if (entity instanceof TL_messageEntityCode || entity instanceof TL_messageEntityPre) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_MONO;
            } else if (entity instanceof TL_messageEntityMentionName) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_MENTION;
                newRun.urlEntity = entity;
            } else if (entity instanceof TL_inputMessageEntityMentionName) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_MENTION;
                newRun.urlEntity = entity;
            } else {
                newRun.flags = TextStyleSpan.FLAG_STYLE_URL;
                newRun.urlEntity = entity;
            }

            for (int b = 0, N2 = runs.size(); b < N2; b++) {
                TextStyleSpan.TextStyleRun run = runs.get(b);

                if (newRun.start > run.start) {
                    if (newRun.start >= run.end) {
                        continue;
                    }

                    if (newRun.end < run.end) {
                        TextStyleSpan.TextStyleRun r = new TextStyleSpan.TextStyleRun(newRun);
                        r.merge(run);
                        b++;
                        N2++;
                        runs.add(b, r);

                        r = new TextStyleSpan.TextStyleRun(run);
                        r.start = newRun.end;
                        b++;
                        N2++;
                        runs.add(b, r);
                    } else if (newRun.end >= run.end) {
                        TextStyleSpan.TextStyleRun r = new TextStyleSpan.TextStyleRun(newRun);
                        r.merge(run);
                        r.end = run.end;
                        b++;
                        N2++;
                        runs.add(b, r);
                    }

                    int temp = newRun.start;
                    newRun.start = run.end;
                    run.end = temp;
                } else {
                    if (run.start >= newRun.end) {
                        continue;
                    }
                    int temp = run.start;
                    if (newRun.end == run.end) {
                        run.merge(newRun);
                    } else if (newRun.end < run.end) {
                        TextStyleSpan.TextStyleRun r = new TextStyleSpan.TextStyleRun(run);
                        r.merge(newRun);
                        r.end = newRun.end;
                        b++;
                        N2++;
                        runs.add(b, r);

                        run.start = newRun.end;
                    } else {
                        TextStyleSpan.TextStyleRun r = new TextStyleSpan.TextStyleRun(newRun);
                        r.start = run.end;
                        b++;
                        N2++;
                        runs.add(b, r);

                        run.merge(newRun);
                    }
                    newRun.end = temp;
                }
            }
            if (newRun.start < newRun.end) {
                runs.add(newRun);
            }
        }
        return runs;
    }

    public ArrayList<MessageEntity> getEntities(CharSequence[] message, boolean allowStrike) {
        if (message == null || message[0] == null) {
            return null;
        }
        ArrayList<MessageEntity> entities = null;
        int index;
        int start = -1;
        int lastIndex = 0;
        boolean isPre = false;
        final String mono = "`";
        final String pre = "```";
        final String bold = "**";
        final String italic = "__";
        final String strike = "~~";
        while ((index = TextUtils.indexOf(message[0], !isPre ? mono : pre, lastIndex)) != -1) {
            if (start == -1) {
                isPre = message[0].length() - index > 2 && message[0].charAt(index + 1) == '`' && message[0].charAt(index + 2) == '`';
                start = index;
                lastIndex = index + (isPre ? 3 : 1);
            } else {
                if (entities == null) {
                    entities = new ArrayList<>();
                }
                for (int a = index + (isPre ? 3 : 1); a < message[0].length(); a++) {
                    if (message[0].charAt(a) == '`') {
                        index++;
                    } else {
                        break;
                    }
                }
                lastIndex = index + (isPre ? 3 : 1);
                if (isPre) {
                    int firstChar = start > 0 ? message[0].charAt(start - 1) : 0;
                    boolean replacedFirst = firstChar == ' ' || firstChar == '\n';
                    CharSequence startMessage = substring(message[0], 0, start - (replacedFirst ? 1 : 0));
                    CharSequence content = substring(message[0], start + 3, index);
                    firstChar = index + 3 < message[0].length() ? message[0].charAt(index + 3) : 0;
                    CharSequence endMessage = substring(message[0], index + 3 + (firstChar == ' ' || firstChar == '\n' ? 1 : 0), message[0].length());
                    if (startMessage.length() != 0) {
                        startMessage = AndroidUtilities.concat(startMessage, "\n");
                    } else {
                        replacedFirst = true;
                    }
                    if (endMessage.length() != 0) {
                        endMessage = AndroidUtilities.concat("\n", endMessage);
                    }
                    if (!TextUtils.isEmpty(content)) {
                        message[0] = AndroidUtilities.concat(startMessage, content, endMessage);
                        TL_messageEntityPre entity = new TL_messageEntityPre();
                        entity.offset = start + (replacedFirst ? 0 : 1);
                        entity.length = index - start - 3 + (replacedFirst ? 0 : 1);
                        entity.language = "";
                        entities.add(entity);
                        lastIndex -= 6;
                    }
                } else {
                    if (start + 1 != index) {
                        message[0] = AndroidUtilities.concat(substring(message[0], 0, start), substring(message[0], start + 1, index), substring(message[0], index + 1, message[0].length()));
                        TL_messageEntityCode entity = new TL_messageEntityCode();
                        entity.offset = start;
                        entity.length = index - start - 1;
                        entities.add(entity);
                        lastIndex -= 2;
                    }
                }
                start = -1;
                isPre = false;
            }
        }
        if (start != -1 && isPre) {
            message[0] = AndroidUtilities.concat(substring(message[0], 0, start), substring(message[0], start + 2, message[0].length()));
            if (entities == null) {
                entities = new ArrayList<>();
            }
            TL_messageEntityCode entity = new TL_messageEntityCode();
            entity.offset = start;
            entity.length = 1;
            entities.add(entity);
        }

        if (message[0] instanceof Spanned) {
            Spanned spannable = (Spanned) message[0];
            TextStyleSpan[] spans = spannable.getSpans(0, message[0].length(), TextStyleSpan.class);
            if (spans != null && spans.length > 0) {
                for (int a = 0; a < spans.length; a++) {
                    TextStyleSpan span = spans[a];
                    int spanStart = spannable.getSpanStart(span);
                    int spanEnd = spannable.getSpanEnd(span);
                    if (checkInclusion(spanStart, entities, false) || checkInclusion(spanEnd, entities, true) || checkIntersection(spanStart, spanEnd, entities)) {
                        continue;
                    }
                    if (entities == null) {
                        entities = new ArrayList<>();
                    }
                    int flags = span.getStyleFlags();
                    if ((flags & TextStyleSpan.FLAG_STYLE_BOLD) != 0) {
                        MessageEntity entity = new TL_messageEntityBold();
                        entity.offset = spanStart;
                        entity.length = spanEnd - spanStart;
                        entities.add(entity);
                    }
                    if ((flags & TextStyleSpan.FLAG_STYLE_ITALIC) != 0) {
                        MessageEntity entity = new TL_messageEntityItalic();
                        entity.offset = spanStart;
                        entity.length = spanEnd - spanStart;
                        entities.add(entity);
                    }
                    if ((flags & TextStyleSpan.FLAG_STYLE_MONO) != 0) {
                        MessageEntity entity = new TL_messageEntityCode();
                        entity.offset = spanStart;
                        entity.length = spanEnd - spanStart;
                        entities.add(entity);
                    }
                    if ((flags & TextStyleSpan.FLAG_STYLE_STRIKE) != 0) {
                        MessageEntity entity = new TL_messageEntityStrike();
                        entity.offset = spanStart;
                        entity.length = spanEnd - spanStart;
                        entities.add(entity);
                    }
                    if ((flags & TextStyleSpan.FLAG_STYLE_UNDERLINE) != 0) {
                        MessageEntity entity = new TL_messageEntityUnderline();
                        entity.offset = spanStart;
                        entity.length = spanEnd - spanStart;
                        entities.add(entity);
                    }
                    if ((flags & TextStyleSpan.FLAG_STYLE_QUOTE) != 0) {
                        MessageEntity entity = new TL_messageEntityBlockquote();
                        entity.offset = spanStart;
                        entity.length = spanEnd - spanStart;
                        entities.add(entity);
                    }
                }
            }

            URLSpanUserMention[] spansMentions = spannable.getSpans(0, message[0].length(), URLSpanUserMention.class);
            if (spansMentions != null && spansMentions.length > 0) {
                if (entities == null) {
                    entities = new ArrayList<>();
                }
                for (int b = 0; b < spansMentions.length; b++) {
                    TL_inputMessageEntityMentionName entity = new TL_inputMessageEntityMentionName();
                    entity.user_id = getMessagesController().getInputUser(Utilities.parseInt(spansMentions[b].getURL()));
                    if (entity.user_id != null) {
                        entity.offset = spannable.getSpanStart(spansMentions[b]);
                        entity.length = Math.min(spannable.getSpanEnd(spansMentions[b]), message[0].length()) - entity.offset;
                        if (message[0].charAt(entity.offset + entity.length - 1) == ' ') {
                            entity.length--;
                        }
                        entities.add(entity);
                    }
                }
            }

            URLSpanReplacement[] spansUrlReplacement = spannable.getSpans(0, message[0].length(), URLSpanReplacement.class);
            if (spansUrlReplacement != null && spansUrlReplacement.length > 0) {
                if (entities == null) {
                    entities = new ArrayList<>();
                }
                for (int b = 0; b < spansUrlReplacement.length; b++) {
                    TL_messageEntityTextUrl entity = new TL_messageEntityTextUrl();
                    entity.offset = spannable.getSpanStart(spansUrlReplacement[b]);
                    entity.length = Math.min(spannable.getSpanEnd(spansUrlReplacement[b]), message[0].length()) - entity.offset;
                    entity.url = spansUrlReplacement[b].getURL();
                    entities.add(entity);
                }
            }
        }

        int count = allowStrike ? 3 : 2;
        for (int c = 0; c < count; c++) {
            lastIndex = 0;
            start = -1;
            String checkString;
            char checkChar;
            switch (c) {
                case 0:
                    checkString = bold;
                    checkChar = '*';
                    break;
                case 1:
                    checkString = italic;
                    checkChar = '_';
                    break;
                case 2:
                default:
                    checkString = strike;
                    checkChar = '~';
                    break;
            }
            while ((index = TextUtils.indexOf(message[0], checkString, lastIndex)) != -1) {
                if (start == -1) {
                    char prevChar = index == 0 ? ' ' : message[0].charAt(index - 1);
                    if (!checkInclusion(index, entities, false) && (prevChar == ' ' || prevChar == '\n')) {
                        start = index;
                    }
                    lastIndex = index + 2;
                } else {
                    for (int a = index + 2; a < message[0].length(); a++) {
                        if (message[0].charAt(a) == checkChar) {
                            index++;
                        } else {
                            break;
                        }
                    }
                    lastIndex = index + 2;
                    if (checkInclusion(index, entities, false) || checkIntersection(start, index, entities)) {
                        start = -1;
                        continue;
                    }
                    if (start + 2 != index) {
                        if (entities == null) {
                            entities = new ArrayList<>();
                        }
                        try {
                            message[0] = AndroidUtilities.concat(substring(message[0], 0, start), substring(message[0], start + 2, index), substring(message[0], index + 2, message[0].length()));
                        } catch (Exception e) {
                            message[0] = substring(message[0], 0, start).toString() + substring(message[0], start + 2, index).toString() + substring(message[0], index + 2, message[0].length()).toString();
                        }

                        MessageEntity entity;
                        if (c == 0) {
                            entity = new TL_messageEntityBold();
                        } else if (c == 1) {
                            entity = new TL_messageEntityItalic();
                        } else {
                            entity = new TL_messageEntityStrike();
                        }
                        entity.offset = start;
                        entity.length = index - start - 2;
                        removeOffsetAfter(entity.offset + entity.length, 4, entities);
                        entities.add(entity);
                        lastIndex -= 4;
                    }
                    start = -1;
                }
            }
        }

        return entities;
    }

    //endregion ---------------- MESSAGES END ----------------


    //region ---------------- DRAFT ----------------
    private LongSparseArray<Integer> draftsFolderIds = new LongSparseArray<>();
    private LongSparseArray<DraftMessage> drafts = new LongSparseArray<>();
    private LongSparseArray<Message> draftMessages = new LongSparseArray<>();
    private boolean inTransaction;
    private SharedPreferences preferences;
    private boolean loadingDrafts;

    public void loadDraftsIfNeed() {
        if (getUserConfig().draftsLoaded || loadingDrafts) {
            return;
        }
        loadingDrafts = true;
        getConnectionsManager().sendRequest(new TL_messages_getAllDrafts(), (response, error) -> {
            if (error != null) {
                AndroidUtilities.runOnUIThread(() -> loadingDrafts = false);
            } else {
                getMessagesController().processUpdates((Updates) response, false);
                AndroidUtilities.runOnUIThread(() -> {
                    loadingDrafts = false;
                    final UserConfig userConfig = getUserConfig();
                    userConfig.draftsLoaded = true;
                    userConfig.saveConfig(false);
                });
            }
        });
    }

    public int getDraftFolderId(long did) {
        return draftsFolderIds.get(did, 0);
    }

    public void setDraftFolderId(long did, int folderId) {
        draftsFolderIds.put(did, folderId);
    }

    public void clearDraftsFolderIds() {
        draftsFolderIds.clear();
    }

    public LongSparseArray<DraftMessage> getDrafts() {
        return drafts;
    }

    public DraftMessage getDraft(long did) {
        return drafts.get(did);
    }

    public Message getDraftMessage(long did) {
        return draftMessages.get(did);
    }

    public void saveDraft(long did, CharSequence message, ArrayList<MessageEntity> entities, Message replyToMessage, boolean noWebpage) {
        saveDraft(did, message, entities, replyToMessage, noWebpage, false);
    }

    public void saveDraft(long did, CharSequence message, ArrayList<MessageEntity> entities, Message replyToMessage, boolean noWebpage, boolean clean) {
        DraftMessage draftMessage;
        if (!TextUtils.isEmpty(message) || replyToMessage != null) {
            draftMessage = new TL_draftMessage();
        } else {
            draftMessage = new TL_draftMessageEmpty();
        }
        draftMessage.date = (int) (System.currentTimeMillis() / 1000);
        draftMessage.message = message == null ? "" : message.toString();
        draftMessage.no_webpage = noWebpage;
        if (replyToMessage != null) {
            draftMessage.reply_to_msg_id = replyToMessage.id;
            draftMessage.flags |= 1;
        }
        if (entities != null && !entities.isEmpty()) {
            draftMessage.entities = entities;
            draftMessage.flags |= 8;
        }

        DraftMessage currentDraft = drafts.get(did);
        if (!clean) {
            if (currentDraft != null && currentDraft.message.equals(draftMessage.message) && currentDraft.reply_to_msg_id == draftMessage.reply_to_msg_id && currentDraft.no_webpage == draftMessage.no_webpage ||
                    currentDraft == null && TextUtils.isEmpty(draftMessage.message) && draftMessage.reply_to_msg_id == 0) {
                return;
            }
        }

        saveDraft(did, draftMessage, replyToMessage, false);
        int lower_id = (int) did;
        if (lower_id != 0) {
            TL_messages_saveDraft req = new TL_messages_saveDraft();
            req.peer = getMessagesController().getInputPeer(lower_id);
            if (req.peer == null) {
                return;
            }
            req.message = draftMessage.message;
            req.no_webpage = draftMessage.no_webpage;
            req.reply_to_msg_id = draftMessage.reply_to_msg_id;
            req.entities = draftMessage.entities;
            req.flags = draftMessage.flags;
            getConnectionsManager().sendRequest(req, (response, error) -> {

            });
        }
        getMessagesController().sortDialogs(null);
        getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
    }

    public void saveDraft(final long did, DraftMessage draft, Message replyToMessage, boolean fromServer) {
        SharedPreferences.Editor editor = preferences.edit();
        final MessagesController messagesController = getMessagesController();
        if (draft == null || draft instanceof TL_draftMessageEmpty) {
            drafts.remove(did);
            draftMessages.remove(did);
            preferences.edit().remove("" + did).remove("r_" + did).commit();
            messagesController.removeDraftDialogIfNeed(did);
        } else {
            drafts.put(did, draft);
            messagesController.putDraftDialogIfNeed(did, draft);
            try {
                SerializedData serializedData = new SerializedData(draft.getObjectSize());
                draft.serializeToStream(serializedData);
                editor.putString("" + did, Utilities.bytesToHex(serializedData.toByteArray()));
                serializedData.cleanup();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        if (replyToMessage == null) {
            draftMessages.remove(did);
            editor.remove("r_" + did);
        } else {
            draftMessages.put(did, replyToMessage);
            SerializedData serializedData = new SerializedData(replyToMessage.getObjectSize());
            replyToMessage.serializeToStream(serializedData);
            editor.putString("r_" + did, Utilities.bytesToHex(serializedData.toByteArray()));
            serializedData.cleanup();
        }
        editor.commit();
        if (fromServer) {
            if (draft.reply_to_msg_id != 0 && replyToMessage == null) {
                int lower_id = (int) did;
                User user = null;
                Chat chat = null;
                if (lower_id > 0) {
                    user = getMessagesController().getUser(lower_id);
                } else {
                    chat = getMessagesController().getChat(-lower_id);
                }
                if (user != null || chat != null) {
                    long messageId = draft.reply_to_msg_id;
                    final int channelIdFinal;
                    if (ChatObject.isChannel(chat)) {
                        messageId |= ((long) chat.id) << 32;
                        channelIdFinal = chat.id;
                    } else {
                        channelIdFinal = 0;
                    }
                    final long messageIdFinal = messageId;

                    getMessagesStorage().getStorageQueue().postRunnable(() -> {
                        try {
                            Message message = null;
                            SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT data FROM messages WHERE mid = %d", messageIdFinal));
                            if (cursor.next()) {
                                NativeByteBuffer data = cursor.byteBufferValue(0);
                                if (data != null) {
                                    message = Message.TLdeserialize(data, data.readInt32(false), false);
                                    message.readAttachPath(data, getUserConfig().clientUserId);
                                    data.reuse();
                                }
                            }
                            cursor.dispose();
                            if (message == null) {
                                if (channelIdFinal != 0) {
                                    final TL_channels_getMessages req = new TL_channels_getMessages();
                                    req.channel = getMessagesController().getInputChannel(channelIdFinal);
                                    req.id.add((int) messageIdFinal);
                                    getConnectionsManager().sendRequest(req, (response, error) -> {
                                        if (error == null) {
                                            messages_Messages messagesRes = (messages_Messages) response;
                                            if (!messagesRes.messages.isEmpty()) {
                                                saveDraftReplyMessage(did, messagesRes.messages.get(0));
                                            }
                                        }
                                    });
                                } else {
                                    TL_messages_getMessages req = new TL_messages_getMessages();
                                    req.id.add((int) messageIdFinal);
                                    getConnectionsManager().sendRequest(req, (response, error) -> {
                                        if (error == null) {
                                            messages_Messages messagesRes = (messages_Messages) response;
                                            if (!messagesRes.messages.isEmpty()) {
                                                saveDraftReplyMessage(did, messagesRes.messages.get(0));
                                            }
                                        }
                                    });
                                }
                            } else {
                                saveDraftReplyMessage(did, message);
                            }
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    });
                }
            }
            getNotificationCenter().postNotificationName(NotificationCenter.newDraftReceived, did);
        }
    }

    private void saveDraftReplyMessage(final long did, final Message message) {
        if (message == null) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            DraftMessage draftMessage = drafts.get(did);
            if (draftMessage != null && draftMessage.reply_to_msg_id == message.id) {
                draftMessages.put(did, message);
                SerializedData serializedData = new SerializedData(message.getObjectSize());
                message.serializeToStream(serializedData);
                preferences.edit().putString("r_" + did, Utilities.bytesToHex(serializedData.toByteArray())).commit();
                getNotificationCenter().postNotificationName(NotificationCenter.newDraftReceived, did);
                serializedData.cleanup();
            }
        });
    }

    public void clearAllDrafts(boolean notify) {
        drafts.clear();
        draftMessages.clear();
        draftsFolderIds.clear();
        preferences.edit().clear().commit();
        if (notify) {
            getMessagesController().sortDialogs(null);
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        }
    }

    public void cleanDraft(long did, boolean replyOnly) {
        DraftMessage draftMessage = drafts.get(did);
        if (draftMessage == null) {
            return;
        }
        if (!replyOnly) {
            drafts.remove(did);
            draftMessages.remove(did);
            preferences.edit().remove("" + did).remove("r_" + did).commit();
            getMessagesController().sortDialogs(null);
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        } else if (draftMessage.reply_to_msg_id != 0) {
            draftMessage.reply_to_msg_id = 0;
            draftMessage.flags &= ~1;
            saveDraft(did, draftMessage.message, draftMessage.entities, null, draftMessage.no_webpage, true);
        }
    }

    public void beginTransaction() {
        inTransaction = true;
    }

    public void endTransaction() {
        inTransaction = false;
    }

    //endregion ---------------- DRAFT END ----------------


    //region ---------------- EMOJI START ----------------

    public static class KeywordResult {
        public String emoji;
        public String keyword;
    }

    public interface KeywordResultCallback {
        void run(ArrayList<KeywordResult> param, String alias);
    }

    private HashMap<String, Boolean> currentFetchingEmoji = new HashMap<>();

    public void fetchNewEmojiKeywords(String[] langCodes) {
        if (langCodes == null) {
            return;
        }
        for (int a = 0; a < langCodes.length; a++) {
            String langCode = langCodes[a];
            if (TextUtils.isEmpty(langCode)) {
                return;
            }
            if (currentFetchingEmoji.get(langCode) != null) {
                return;
            }
            currentFetchingEmoji.put(langCode, true);
            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                int version = -1;
                String alias = null;
                long date = 0;
                try {
                    SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT alias, version, date FROM emoji_keywords_info_v2 WHERE lang = ?", langCode);
                    if (cursor.next()) {
                        alias = cursor.stringValue(0);
                        version = cursor.intValue(1);
                        date = cursor.longValue(2);
                    }
                    cursor.dispose();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (!BuildVars.DEBUG_VERSION && Math.abs(System.currentTimeMillis() - date) < 60 * 60 * 1000) {
                    AndroidUtilities.runOnUIThread(() -> currentFetchingEmoji.remove(langCode));
                    return;
                }
                TLObject request;
                if (version == -1) {
                    TL_messages_getEmojiKeywords req = new TL_messages_getEmojiKeywords();
                    req.lang_code = langCode;
                    request = req;
                } else {
                    TL_messages_getEmojiKeywordsDifference req = new TL_messages_getEmojiKeywordsDifference();
                    req.lang_code = langCode;
                    req.from_version = version;
                    request = req;
                }
                String aliasFinal = alias;
                int versionFinal = version;
                getConnectionsManager().sendRequest(request, (response, error) -> {
                    if (response != null) {
                        TL_emojiKeywordsDifference res = (TL_emojiKeywordsDifference) response;
                        if (versionFinal != -1 && !res.lang_code.equals(aliasFinal)) {
                            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                                try {
                                    SQLitePreparedStatement deleteState = getMessagesStorage().getDatabase().executeFast("DELETE FROM emoji_keywords_info_v2 WHERE lang = ?");
                                    deleteState.bindString(1, langCode);
                                    deleteState.step();
                                    deleteState.dispose();

                                    AndroidUtilities.runOnUIThread(() -> {
                                        currentFetchingEmoji.remove(langCode);
                                        fetchNewEmojiKeywords(new String[]{langCode});
                                    });
                                } catch (Exception e) {
                                    FileLog.e(e);
                                }
                            });
                        } else {
                            putEmojiKeywords(langCode, res);
                        }
                    } else {
                        AndroidUtilities.runOnUIThread(() -> currentFetchingEmoji.remove(langCode));
                    }
                });
            });
        }
    }

    private void putEmojiKeywords(String lang, TL_emojiKeywordsDifference res) {
        if (res == null) {
            return;
        }
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                if (!res.keywords.isEmpty()) {
                    SQLitePreparedStatement insertState = getMessagesStorage().getDatabase().executeFast("REPLACE INTO emoji_keywords_v2 VALUES(?, ?, ?)");
                    SQLitePreparedStatement deleteState = getMessagesStorage().getDatabase().executeFast("DELETE FROM emoji_keywords_v2 WHERE lang = ? AND keyword = ? AND emoji = ?");
                    getMessagesStorage().getDatabase().beginTransaction();
                    for (int a = 0, N = res.keywords.size(); a < N; a++) {
                        EmojiKeyword keyword = res.keywords.get(a);
                        if (keyword instanceof TL_emojiKeyword) {
                            TL_emojiKeyword emojiKeyword = (TL_emojiKeyword) keyword;
                            String key = emojiKeyword.keyword.toLowerCase();
                            for (int b = 0, N2 = emojiKeyword.emoticons.size(); b < N2; b++) {
                                insertState.requery();
                                insertState.bindString(1, res.lang_code);
                                insertState.bindString(2, key);
                                insertState.bindString(3, emojiKeyword.emoticons.get(b));
                                insertState.step();
                            }
                        } else if (keyword instanceof TL_emojiKeywordDeleted) {
                            TL_emojiKeywordDeleted keywordDeleted = (TL_emojiKeywordDeleted) keyword;
                            String key = keywordDeleted.keyword.toLowerCase();
                            for (int b = 0, N2 = keywordDeleted.emoticons.size(); b < N2; b++) {
                                deleteState.requery();
                                deleteState.bindString(1, res.lang_code);
                                deleteState.bindString(2, key);
                                deleteState.bindString(3, keywordDeleted.emoticons.get(b));
                                deleteState.step();
                            }
                        }
                    }
                    getMessagesStorage().getDatabase().commitTransaction();
                    insertState.dispose();
                    deleteState.dispose();
                }

                SQLitePreparedStatement infoState = getMessagesStorage().getDatabase().executeFast("REPLACE INTO emoji_keywords_info_v2 VALUES(?, ?, ?, ?)");
                infoState.bindString(1, lang);
                infoState.bindString(2, res.lang_code);
                infoState.bindInteger(3, res.version);
                infoState.bindLong(4, System.currentTimeMillis());
                infoState.step();
                infoState.dispose();

                AndroidUtilities.runOnUIThread(() -> {
                    currentFetchingEmoji.remove(lang);
                    getNotificationCenter().postNotificationName(NotificationCenter.newEmojiSuggestionsAvailable, lang);
                });
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public void getEmojiSuggestions(String[] langCodes, String keyword, boolean fullMatch, KeywordResultCallback callback) {
        getEmojiSuggestions(langCodes, keyword, fullMatch, callback, null);
    }

    public void getEmojiSuggestions(String[] langCodes, String keyword, boolean fullMatch, KeywordResultCallback callback, CountDownLatch sync) {
        if (callback == null) {
            return;
        }
        if (TextUtils.isEmpty(keyword) || langCodes == null) {
            callback.run(new ArrayList<>(), null);
            return;
        }
        ArrayList<String> recentEmoji = new ArrayList<>(Emoji.recentEmoji);
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            ArrayList<KeywordResult> result = new ArrayList<>();
            HashMap<String, Boolean> resultMap = new HashMap<>();
            String alias = null;
            try {
                SQLiteCursor cursor;
                boolean hasAny = false;
                for (int a = 0; a < langCodes.length; a++) {
                    cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT alias FROM emoji_keywords_info_v2 WHERE lang = ?", langCodes[a]);
                    if (cursor.next()) {
                        alias = cursor.stringValue(0);
                    }
                    cursor.dispose();
                    if (alias != null) {
                        hasAny = true;
                    }
                }
                if (!hasAny) {
                    AndroidUtilities.runOnUIThread(() -> {
                        for (int a = 0; a < langCodes.length; a++) {
                            if (currentFetchingEmoji.get(langCodes[a]) != null) {
                                return;
                            }
                        }
                        callback.run(result, null);
                    });
                    return;
                }

                String key = keyword.toLowerCase();
                for (int a = 0; a < 2; a++) {
                    if (a == 1) {
                        String translitKey = LocaleController.getInstance().getTranslitString(key, false, false);
                        if (translitKey.equals(key)) {
                            continue;
                        }
                        key = translitKey;
                    }
                    String key2 = null;
                    StringBuilder nextKey = new StringBuilder(key);
                    int pos = nextKey.length();
                    while (pos > 0) {
                        pos--;
                        char value = nextKey.charAt(pos);
                        value++;
                        nextKey.setCharAt(pos, value);
                        if (value != 0) {
                            key2 = nextKey.toString();
                            break;
                        }
                    }

                    if (fullMatch) {
                        cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT emoji, keyword FROM emoji_keywords_v2 WHERE keyword = ?", key);
                    } else if (key2 != null) {
                        cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT emoji, keyword FROM emoji_keywords_v2 WHERE keyword >= ? AND keyword < ?", key, key2);
                    } else {
                        key += "%";
                        cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT emoji, keyword FROM emoji_keywords_v2 WHERE keyword LIKE ?", key);
                    }
                    while (cursor.next()) {
                        String value = cursor.stringValue(0).replace("\ufe0f", "");
                        if (resultMap.get(value) != null) {
                            continue;
                        }
                        resultMap.put(value, true);
                        KeywordResult keywordResult = new KeywordResult();
                        keywordResult.emoji = value;
                        keywordResult.keyword = cursor.stringValue(1);
                        result.add(keywordResult);
                    }
                    cursor.dispose();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
            Collections.sort(result, (o1, o2) -> {
                int idx1 = recentEmoji.indexOf(o1.emoji);
                if (idx1 < 0) {
                    idx1 = Integer.MAX_VALUE;
                }
                int idx2 = recentEmoji.indexOf(o2.emoji);
                if (idx2 < 0) {
                    idx2 = Integer.MAX_VALUE;
                }
                if (idx1 < idx2) {
                    return -1;
                } else if (idx1 > idx2) {
                    return 1;
                } else {
                    int len1 = o1.keyword.length();
                    int len2 = o2.keyword.length();

                    if (len1 < len2) {
                        return -1;
                    } else if (len1 > len2) {
                        return 1;
                    }
                    return 0;
                }
            });
            String aliasFinal = alias;
            if (sync != null) {
                callback.run(result, aliasFinal);
                sync.countDown();
            } else {
                AndroidUtilities.runOnUIThread(() -> callback.run(result, aliasFinal));
            }
        });
        if (sync != null) {
            try {
                sync.await();
            } catch (Throwable ignore) {

            }
        }
    }

    //endregion ---------------- EMOJI END ----------------

    public void increaseInlineRaiting(final int uid) {
        if (!getUserConfig().suggestContacts) {
            return;
        }
        int dt;
        if (getUserConfig().botRatingLoadTime != 0) {
            dt = Math.max(1, ((int) (System.currentTimeMillis() / 1000)) - getUserConfig().botRatingLoadTime);
        } else {
            dt = 60;
        }

        getNotificationCenter().postNotificationName(NotificationCenter.reloadInlineHints);
    }

}

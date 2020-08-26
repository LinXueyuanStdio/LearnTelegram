package com.demo.chat.controller;

import android.app.Activity;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.util.SparseArray;

import com.demo.chat.ApplicationLoader;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.FileLog;
import com.demo.chat.messager.NotificationCenter;
import com.demo.chat.messager.SerializedData;
import com.demo.chat.messager.Utilities;
import com.demo.chat.model.Message;
import com.demo.chat.model.MessageObject;
import com.demo.chat.model.User;
import com.demo.chat.model.small.Document;
import com.demo.chat.ui.Components.TextStyleSpan;
import com.demo.chat.ui.Components.URLSpanReplacement;

import java.util.ArrayList;
import java.util.Map;

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
                    TLRPC.DraftMessage draftMessage = TLRPC.DraftMessage.TLdeserialize(serializedData, serializedData.readInt32(true), true);
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
                TLRPC.InputPeer inputPeer = getMessagesController().getInputPeer((int) mergeDialogId);
                if (inputPeer == null) {
                    return;
                }
                final TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
                req.peer = inputPeer;
                lastMergeDialogId = mergeDialogId;
                req.limit = 1;
                req.q = query != null ? query : "";
                if (user != null) {
                    req.from_id = getMessagesController().getInputUser(user);
                    req.flags |= 1;
                }
                req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
                mergeReqId = getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                    if (lastMergeDialogId == mergeDialogId) {
                        mergeReqId = 0;
                        if (response != null) {
                            TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                            messagesSearchEndReached[1] = res.messages.isEmpty();
                            messagesSearchCount[1] = res instanceof TLRPC.TL_messages_messagesSlice ? res.count : res.messages.size();
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
        final TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
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
        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
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
                    TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                    for (int a = 0; a < res.messages.size(); a++) {
                        TLRPC.Message message = res.messages.get(a);
                        if (message instanceof TLRPC.TL_messageEmpty || message.action instanceof TLRPC.TL_messageActionHistoryClear) {
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
                        TLRPC.Message message = res.messages.get(a);
                        added = true;
                        MessageObject messageObject = new MessageObject(currentAccount, message, false);
                        searchResultMessages.add(messageObject);
                        searchResultMessagesMap[queryWithDialogFinal == dialogId ? 0 : 1].put(messageObject.getId(), messageObject);
                    }
                    messagesSearchEndReached[queryWithDialogFinal == dialogId ? 0 : 1] = res.messages.size() != 21;
                    messagesSearchCount[queryWithDialogFinal == dialogId ? 0 : 1] = res instanceof TLRPC.TL_messages_messagesSlice || res instanceof TLRPC.TL_messages_channelMessages ? res.count : res.messages.size();
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
}

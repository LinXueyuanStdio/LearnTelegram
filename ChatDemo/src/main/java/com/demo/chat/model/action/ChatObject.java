package com.demo.chat.model.action;

import com.demo.chat.controller.MessagesController;
import com.demo.chat.model.Chat;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class ChatObject {

    public static final int CHAT_TYPE_CHAT = 0;
    public static final int CHAT_TYPE_CHANNEL = 2;
    public static final int CHAT_TYPE_USER = 3;
    public static final int CHAT_TYPE_MEGAGROUP = 4;

    public static final int ACTION_PIN = 0;
    public static final int ACTION_CHANGE_INFO = 1;
    public static final int ACTION_BLOCK_USERS = 2;
    public static final int ACTION_INVITE = 3;
    public static final int ACTION_ADD_ADMINS = 4;
    public static final int ACTION_POST = 5;
    public static final int ACTION_SEND = 6;
    public static final int ACTION_SEND_MEDIA = 7;
    public static final int ACTION_SEND_STICKERS = 8;
    public static final int ACTION_EMBED_LINKS = 9;
    public static final int ACTION_SEND_POLLS = 10;
    public static final int ACTION_VIEW = 11;
    public static final int ACTION_EDIT_MESSAGES = 12;
    public static final int ACTION_DELETE_MESSAGES = 13;

    private static boolean isBannableAction(int action) {
        switch (action) {
            case ACTION_PIN:
            case ACTION_CHANGE_INFO:
            case ACTION_INVITE:
            case ACTION_SEND:
            case ACTION_SEND_MEDIA:
            case ACTION_SEND_STICKERS:
            case ACTION_EMBED_LINKS:
            case ACTION_SEND_POLLS:
            case ACTION_VIEW:
                return true;
        }
        return false;
    }

    private static boolean isAdminAction(int action) {
        switch (action) {
            case ACTION_PIN:
            case ACTION_CHANGE_INFO:
            case ACTION_INVITE:
            case ACTION_ADD_ADMINS:
            case ACTION_POST:
            case ACTION_EDIT_MESSAGES:
            case ACTION_DELETE_MESSAGES:
            case ACTION_BLOCK_USERS:
                return true;
        }
        return false;
    }

    private static boolean getBannedRight(TL_chatBannedRights rights, int action) {
        if (rights == null) {
            return false;
        }
        boolean value;
        switch (action) {
            case ACTION_PIN:
                return rights.pin_messages;
            case ACTION_CHANGE_INFO:
                return rights.change_info;
            case ACTION_INVITE:
                return rights.invite_users;
            case ACTION_SEND:
                return rights.send_messages;
            case ACTION_SEND_MEDIA:
                return rights.send_media;
            case ACTION_SEND_STICKERS:
                return rights.send_stickers;
            case ACTION_EMBED_LINKS:
                return rights.embed_links;
            case ACTION_SEND_POLLS:
                return rights.send_polls;
            case ACTION_VIEW:
                return rights.view_messages;
        }
        return false;
    }

    public static boolean isActionBannedByDefault(Chat chat, int action) {
        if (getBannedRight(chat.banned_rights, action)) {
            return false;
        }
        return getBannedRight(chat.default_banned_rights, action);
    }

    public static boolean isActionBanned(Chat chat, int action) {
        return chat != null && (getBannedRight(chat.banned_rights, action) || getBannedRight(chat.default_banned_rights, action));
    }

    public static boolean canUserDoAdminAction(Chat chat, int action) {
        if (chat == null) {
            return false;
        }
        if (chat.creator) {
            return true;
        }
        if (chat.admin_rights != null) {
            boolean value;
            switch (action) {
                case ACTION_PIN:
                    value = chat.admin_rights.pin_messages;
                    break;
                case ACTION_CHANGE_INFO:
                    value = chat.admin_rights.change_info;
                    break;
                case ACTION_INVITE:
                    value = chat.admin_rights.invite_users;
                    break;
                case ACTION_ADD_ADMINS:
                    value = chat.admin_rights.add_admins;
                    break;
                case ACTION_POST:
                    value = chat.admin_rights.post_messages;
                    break;
                case ACTION_EDIT_MESSAGES:
                    value = chat.admin_rights.edit_messages;
                    break;
                case ACTION_DELETE_MESSAGES:
                    value = chat.admin_rights.delete_messages;
                    break;
                case ACTION_BLOCK_USERS:
                    value = chat.admin_rights.ban_users;
                    break;
                default:
                    value = false;
                    break;
            }
            if (value) {
                return true;
            }
        }
        return false;
    }

    public static boolean canUserDoAction(Chat chat, int action) {
        if (chat == null) {
            return true;
        }
        if (canUserDoAdminAction(chat, action)) {
            return true;
        }
        if (getBannedRight(chat.banned_rights, action)) {
            return false;
        }
        if (isBannableAction(action)) {
            if (chat.admin_rights != null && !isAdminAction(action)) {
                return true;
            }
            if (chat.default_banned_rights == null && (
                    chat instanceof TL_chat_layer92 ||
                            chat instanceof TL_chat_old ||
                            chat instanceof TL_chat_old2 ||
                            chat instanceof TL_channel_layer92 ||
                            chat instanceof TL_channel_layer77 ||
                            chat instanceof TL_channel_layer72 ||
                            chat instanceof TL_channel_layer67 ||
                            chat instanceof TL_channel_layer48 ||
                            chat instanceof TL_channel_old)) {
                return true;
            }
            if (chat.default_banned_rights == null || getBannedRight(chat.default_banned_rights, action)) {
                return false;
            }
            return true;
        }
        return false;
    }

    public static boolean isLeftFromChat(Chat chat) {
        return chat == null || chat instanceof TL_chatEmpty || chat instanceof TL_chatForbidden || chat instanceof TL_channelForbidden || chat.left || chat.deactivated;
    }

    public static boolean isKickedFromChat(Chat chat) {
        return chat == null || chat instanceof TL_chatEmpty || chat instanceof TL_chatForbidden || chat instanceof TL_channelForbidden || chat.kicked || chat.deactivated || chat.banned_rights != null && chat.banned_rights.view_messages;
    }

    public static boolean isNotInChat(Chat chat) {
        return chat == null || chat instanceof TL_chatEmpty || chat instanceof TL_chatForbidden || chat instanceof TL_channelForbidden || chat.left || chat.kicked || chat.deactivated;
    }

    public static boolean isChannel(Chat chat) {
        return chat instanceof TL_channel || chat instanceof TL_channelForbidden;
    }

    public static boolean isMegagroup(Chat chat) {
        return (chat instanceof TL_channel || chat instanceof TL_channelForbidden) && chat.megagroup;
    }

    public static boolean hasAdminRights(Chat chat) {
        return chat != null && (chat.creator || chat.admin_rights != null && chat.admin_rights.flags != 0);
    }

    public static boolean canChangeChatInfo(Chat chat) {
        return canUserDoAction(chat, ACTION_CHANGE_INFO);
    }

    public static boolean canAddAdmins(Chat chat) {
        return canUserDoAction(chat, ACTION_ADD_ADMINS);
    }

    public static boolean canBlockUsers(Chat chat) {
        return canUserDoAction(chat, ACTION_BLOCK_USERS);
    }

    public static boolean canSendStickers(Chat chat) {
        return canUserDoAction(chat, ACTION_SEND_STICKERS);
    }

    public static boolean canSendEmbed(Chat chat) {
        return canUserDoAction(chat, ACTION_EMBED_LINKS);
    }

    public static boolean canSendMedia(Chat chat) {
        return canUserDoAction(chat, ACTION_SEND_MEDIA);
    }

    public static boolean canSendPolls(Chat chat) {
        return canUserDoAction(chat, ACTION_SEND_POLLS);
    }

    public static boolean canSendMessages(Chat chat) {
        return canUserDoAction(chat, ACTION_SEND);
    }

    public static boolean canPost(Chat chat) {
        return canUserDoAction(chat, ACTION_POST);
    }

    public static boolean canAddUsers(Chat chat) {
        return canUserDoAction(chat, ACTION_INVITE);
    }

    public static boolean canAddBotsToChat(Chat chat) {
        if (isChannel(chat)) {
            if (chat != null && chat.megagroup && (chat.admin_rights != null && (chat.admin_rights.post_messages || chat.admin_rights.add_admins) || chat.creator)) {
                return true;
            }
        } else {
            if (chat.migrated_to == null) {
                return true;
            }
        }
        return false;
    }

    public static boolean canPinMessages(Chat chat) {
        return canUserDoAction(chat, ACTION_PIN) || ChatObject.isChannel(chat) && !chat.megagroup && chat.admin_rights != null && chat.admin_rights.edit_messages;
    }

    public static boolean isChannel(int chatId, int currentAccount) {
        Chat chat = MessagesController.getInstance(currentAccount).getChat(chatId);
        return chat instanceof TL_channel || chat instanceof TL_channelForbidden;
    }

    public static boolean isCanWriteToChannel(int chatId, int currentAccount) {
        Chat chat = MessagesController.getInstance(currentAccount).getChat(chatId);
        return ChatObject.canSendMessages(chat) || chat != null && chat.megagroup;
    }

    public static boolean canWriteToChat(Chat chat) {
        return !isChannel(chat) || chat.creator || chat.admin_rights != null && chat.admin_rights.post_messages || !chat.broadcast;
    }

    public static String getBannedRightsString(TL_chatBannedRights bannedRights) {
        String currentBannedRights = "";
        currentBannedRights += bannedRights.view_messages ? 1 : 0;
        currentBannedRights += bannedRights.send_messages ? 1 : 0;
        currentBannedRights += bannedRights.send_media ? 1 : 0;
        currentBannedRights += bannedRights.send_stickers ? 1 : 0;
        currentBannedRights += bannedRights.send_gifs ? 1 : 0;
        currentBannedRights += bannedRights.send_games ? 1 : 0;
        currentBannedRights += bannedRights.send_inline ? 1 : 0;
        currentBannedRights += bannedRights.embed_links ? 1 : 0;
        currentBannedRights += bannedRights.send_polls ? 1 : 0;
        currentBannedRights += bannedRights.invite_users ? 1 : 0;
        currentBannedRights += bannedRights.change_info ? 1 : 0;
        currentBannedRights += bannedRights.pin_messages ? 1 : 0;
        currentBannedRights += bannedRights.until_date;
        return currentBannedRights;
    }

    public static Chat getChatByDialog(long did, int currentAccount) {
        int lower_id = (int) did;
        int high_id = (int) (did >> 32);
        if (lower_id < 0) {
            return MessagesController.getInstance(currentAccount).getChat(-lower_id);
        }
        return null;
    }
}

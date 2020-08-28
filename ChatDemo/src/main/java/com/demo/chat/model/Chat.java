package com.demo.chat.model;

import com.demo.chat.messager.AbstractSerializedData;
import com.demo.chat.model.bot.BotInfo;
import com.demo.chat.model.small.ChatPhoto;
import com.demo.chat.model.small.MessageMedia;
import com.demo.chat.model.sticker.StickerSet;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description 聊天模型
 *
 * 根据 type 区分是和人、机器人、群、订阅通道、加密聊天
 * @usage null
 */
public class Chat extends UserChat {
    public static int UserType = 0;
    public static int RobotType = 1;
    public static int GroupType = 2;
    public static int ChannelType = 3;

    public boolean isRobot() {
        return type == RobotType;
    }

    public boolean isUser() {
        return type == UserType;
    }

    public boolean isGroup() {
        return type == GroupType;
    }

    public boolean isChannel() {
        return type == ChannelType;
    }

    public boolean isForbidden() {
        return false;
    }

    public boolean isPublic() {
        return false;
    }

    public int type = 0;
    public int id = 0;
    public String title;
    public int date;
    public int flags;
    public boolean creator;
    public boolean kicked;
    public boolean deactivated;
    public boolean left;
    public boolean has_geo;
    public boolean slowmode_enabled;
    public ChatPhoto photo;
    public int participants_count;
    public int version;
    public boolean broadcast;
    public boolean megagroup;
    public long access_hash;
    public int until_date;
    public boolean moderator;
    public boolean verified;
    public boolean restricted;
    public boolean signatures;
    public String username;
    public boolean min;
    public boolean scam;
    public boolean has_link;
    public boolean explicit_content;

    public ChatParticipants participants;
    public MessageMedia.Photo chat_photo;
    public ArrayList<BotInfo> bot_info = new ArrayList<>();
    public boolean can_view_participants;
    public boolean can_set_username;
    public boolean has_scheduled;
    public String about;
    public int admins_count;
    public int read_inbox_max_id;
    public int read_outbox_max_id;
    public int unread_count;
    public int migrated_from_chat_id;
    public int migrated_from_max_id;
    public int pinned_msg_id;
    public int kicked_count;
    public int unread_important_count;
    public int folder_id;
    public boolean can_set_stickers;
    public boolean hidden_prehistory;
    public boolean can_view_stats;
    public boolean can_set_location;
    public int banned_count;
    public int online_count;
    public StickerSet stickerset;
    public int available_min_id;
    public int call_msg_id;
    public int linked_chat_id;
    public int slowmode_seconds;
    public int slowmode_next_send_date;
    public int stats_dc;
    public int pts;

    public TL_chatAdminRights admin_rights;
    public TL_chatBannedRights banned_rights;
    public TL_chatBannedRights default_banned_rights;

    public static class TL_chatAdminRights {
        public int flags;
        public boolean change_info;
        public boolean post_messages;
        public boolean edit_messages;
        public boolean delete_messages;
        public boolean ban_users;
        public boolean invite_users;
        public boolean pin_messages;
        public boolean add_admins;
    }

    public static class TL_chatBannedRights {
        public int flags;
        public boolean view_messages;
        public boolean send_messages;
        public boolean send_media;
        public boolean send_stickers;
        public boolean send_gifs;
        public boolean send_games;
        public boolean send_inline;
        public boolean embed_links;
        public boolean send_polls;
        public boolean change_info;
        public boolean invite_users;
        public boolean pin_messages;
        public int until_date;
    }

    public static class ChatParticipants {
        public int flags;
        public int chat_id;
        public ChatParticipant self_participant;
        public ArrayList<ChatParticipant> participants = new ArrayList<>();
        public int version;
        public int admin_id;
    }

    public static class ChatParticipant {
        public int user_id;
        public int inviter_id;
        public int date;
    }

    public void readParams(AbstractSerializedData stream, boolean exception) {

    }

    public void serializeToStream(AbstractSerializedData stream) {

    }

    public static Chat TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
        Chat result = new Chat();
        result.readParams(stream, exception);
        return result;
    }

}

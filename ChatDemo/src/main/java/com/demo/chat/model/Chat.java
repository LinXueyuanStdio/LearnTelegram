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

    public int type = UserType;
    public int id = 1;
    public String title = "这是会话";
    public int date;
    public int flags;
    public boolean creator = false;
    public boolean kicked = false;
    public boolean deactivated = false;
    public boolean left = false;
    public boolean has_geo = false;
    public boolean slowmode_enabled = false;
    public ChatPhoto photo = new ChatPhoto();
    public int participants_count = 1;
    public int version = 1;
    public boolean broadcast = true;
    public boolean megagroup = true;
    public long access_hash;
    public int until_date;
    public boolean moderator = true;
    public boolean verified = true;
    public boolean restricted = true;
    public boolean signatures = true;
    public String username = "名字";
    public boolean min = true;
    public boolean scam = true;
    public boolean has_link = true;
    public boolean explicit_content = true;

    public ChatParticipants participants;
    public MessageMedia.Photo chat_photo;
    public ArrayList<BotInfo> bot_info = new ArrayList<>();
    public boolean can_view_participants;
    public boolean can_set_username;
    public boolean has_scheduled;
    public String about;
    public int admins_count = 0;
    public int read_inbox_max_id = 0;
    public int read_outbox_max_id = 0;
    public int unread_count = 0;
    public int migrated_from_chat_id = 0;
    public int migrated_from_max_id = 0;
    public int pinned_msg_id = 0;
    public int kicked_count = 0;
    public int unread_important_count = 0;
    public int folder_id = 0;
    public boolean can_set_stickers = true;
    public boolean hidden_prehistory = false;
    public boolean can_view_stats = true;
    public boolean can_set_location = true;
    public int banned_count = 1000;
    public int online_count = 1000;
    public StickerSet stickerset = new StickerSet();
    public int available_min_id = 0;
    public int call_msg_id = 0;
    public int linked_chat_id = 0;
    public int slowmode_seconds = 1;
    public int slowmode_next_send_date = 1;
    public int stats_dc = 1;
    public int pts = 1;

    public TL_chatAdminRights admin_rights = new TL_chatAdminRights();
    public TL_chatBannedRights banned_rights = new TL_chatBannedRights();
    public TL_chatBannedRights default_banned_rights = new TL_chatBannedRights();

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

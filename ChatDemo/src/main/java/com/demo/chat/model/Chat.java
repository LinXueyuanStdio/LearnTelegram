package com.demo.chat.model;

import com.demo.chat.model.small.ChatPhoto;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description 聊天模型
 *
 * 根据 type 区分是和人、机器人、群、订阅通道、加密聊天
 * @usage null
 */
public class Chat {
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
}

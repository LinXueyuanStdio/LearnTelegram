package com.demo.chat.model;

import com.demo.chat.messager.AbstractSerializedData;
import com.demo.chat.model.small.FileLocation;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description 登录的用户
 * @usage null
 */
public class User {
    public int id;
    public String first_name;
    public String last_name;
    public String username;
    public long access_hash;
    public String phone;
    public int flags;
    public boolean self;
    public boolean contact;
    public boolean mutual_contact;
    public boolean deleted;
    public boolean bot;
    public boolean bot_chat_history;
    public boolean bot_nochats;
    public boolean verified;
    public boolean restricted;
    public boolean min;
    public boolean bot_inline_geo;
    public boolean support;
    public boolean scam;
    public int bot_info_version;
    public String bot_inline_placeholder;
    public String lang_code;
    public boolean inactive;
    public boolean explicit_content;

    public UserProfilePhoto photo;
    public UserStatus status;
//    public ArrayList<TL_restrictionReason> restriction_reason = new ArrayList<>();TODO 可能不需要这个字段

    public static class UserStatus {
        public int expires;
    }
    public static class UserProfilePhoto {
        public long photo_id;
        public FileLocation photo_small;
        public FileLocation photo_big;
        public int dc_id;
    }

    public static User TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
        User result = new User();
        result.readParams(stream, exception);
        return result;
    }

    public void readParams(AbstractSerializedData stream, boolean exception) {
        id = stream.readInt32(exception);
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(id);
    }
}

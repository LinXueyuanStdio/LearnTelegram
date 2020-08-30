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
public class User extends UserChat {
    public int id = 1;
    public String first_name = "first";
    public String last_name = "last";
    public String username = "username";
    public long access_hash;
    public String phone = "15768674243";
    public int flags;
    public boolean self = false;
    public boolean contact = false;
    public boolean mutual_contact = false;
    public boolean deleted = false;
    public boolean bot = true;
    public boolean bot_chat_history = false;
    public boolean bot_nochats = false;
    public boolean verified = false;
    public boolean restricted = false;
    public boolean min = false;
    public boolean bot_inline_geo = false;
    public boolean support = false;
    public boolean scam = false;
    public int bot_info_version = 1;
    public String bot_inline_placeholder;
    public String lang_code;
    public boolean inactive = false;
    public boolean explicit_content = false;

    public UserProfilePhoto photo = new UserProfilePhoto();
    public UserStatus status = new UserStatus();
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

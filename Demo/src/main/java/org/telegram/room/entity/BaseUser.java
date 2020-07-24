package org.telegram.room.entity;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/7/17
 * @description null
 * @usage null
 */
public abstract class BaseUser extends BaseObj{
    public int id;
    public String first_name;
    public String last_name;
    public String username;
    public long access_hash;
    public String phone;
    public UserProfilePhoto photo;
    public UserStatus status;
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
    public ArrayList<TL_restrictionReason> restriction_reason = new ArrayList<>();
}

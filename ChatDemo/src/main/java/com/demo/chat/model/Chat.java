package com.demo.chat.model;

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
}

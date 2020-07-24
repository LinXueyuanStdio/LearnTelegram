package org.telegram.room.entity;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/7/17
 * @description null
 * @usage null
 */
public abstract class BaseChat extends BaseObj{
    public int id;
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
    public ArrayList<TL_restrictionReason> restriction_reason = new ArrayList<>();
    public TL_channelAdminRights_layer92 admin_rights_layer92;
    public TL_channelBannedRights_layer92 banned_rights_layer92;
    public TL_chatAdminRights admin_rights;
    public TL_chatBannedRights banned_rights;
    public TL_chatBannedRights default_banned_rights;
    public InputChannel migrated_to;
}

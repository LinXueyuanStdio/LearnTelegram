package org.telegram.room.entity;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/7/17
 * @description null
 * @usage null
 */
public abstract class BaseDialog extends BaseObj {

    public int flags;
    public boolean pinned;
    public boolean unread_mark;
    public Peer peer;
    public int top_message;
    public int read_inbox_max_id;
    public int read_outbox_max_id;
    public int unread_count;
    public int unread_mentions_count;
    public PeerNotifySettings notify_settings;
    public int pts;
    public DraftMessage draft;
    public int folder_id;
    public int last_message_date; //custom
    public long id; //custom
    public int pinnedNum; //custom
}

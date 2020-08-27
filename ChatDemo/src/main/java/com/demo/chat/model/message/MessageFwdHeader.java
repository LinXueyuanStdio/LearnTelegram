package com.demo.chat.model.message;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class MessageFwdHeader {
    public int flags;
    public int from_id;
    public String from_name;
    public int date;
    public int channel_id;
    public int channel_post;
    public String post_author;
    public Peer saved_from_peer;
    public int saved_from_msg_id;
    public String psa_type;
}

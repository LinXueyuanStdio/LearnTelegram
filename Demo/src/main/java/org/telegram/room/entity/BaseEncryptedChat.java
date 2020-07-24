package org.telegram.room.entity;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/7/17
 * @description null
 * @usage null
 */
public abstract class BaseEncryptedChat extends BaseObj{
    public int id;
    public long access_hash;
    public int date;
    public int admin_id;
    public int participant_id;
    public byte[] g_a;
    public byte[] nonce;
    public byte[] g_a_or_b;
    public long key_fingerprint;
    public byte[] a_or_b; //custom
    public byte[] auth_key; //custom
    public int user_id; //custom
    public int ttl; //custom
    public int layer; //custom
    public int seq_in; //custom
    public int seq_out; //custom
    public int in_seq_no; //custom
    public int mtproto_seq; //custom
    public byte[] key_hash; //custom
    public short key_use_count_in; //custom
    public short key_use_count_out; //custom
    public long exchange_id; //custom
    public int key_create_date; //custom
    public long future_key_fingerprint; //custom
    public byte[] future_auth_key; //custom
}

package com.demo.chat.model.small;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/26
 * @description null
 * @usage null
 */
public class Document {
    public int flags;
    public long id;
    public long access_hash;
    public byte[] file_reference;
    public int user_id;
    public int date;
    public String file_name;
    public String mime_type;
    public int size;
    public int version;
    public int dc_id;
    public byte[] key;
    public byte[] iv;
    public ArrayList<PhotoSize> thumbs = new ArrayList<>();
    public ArrayList<TL_videoSize> video_thumbs = new ArrayList<>();
    public ArrayList<DocumentAttribute> attributes = new ArrayList<>();
}

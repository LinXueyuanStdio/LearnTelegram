package com.demo.chat.model.sticker;

import com.demo.chat.model.small.PhotoSize;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class StickerSet {
    public int flags;
    public boolean installed;
    public boolean archived;
    public boolean official;
    public boolean animated;
    public boolean masks;
    public long id;
    public long access_hash;
    public String title;
    public String short_name;
    public int count;
    public int hash;
    public int installed_date;
    public PhotoSize thumb;
    public int thumb_dc_id;
}

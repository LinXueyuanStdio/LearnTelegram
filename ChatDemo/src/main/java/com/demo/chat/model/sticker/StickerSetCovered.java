package com.demo.chat.model.sticker;

import com.demo.chat.messager.AbstractSerializedData;
import com.demo.chat.model.small.Document;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class StickerSetCovered extends Sticker{
    public StickerSet set;
    public ArrayList<Document> covers = new ArrayList<>();
    public Document cover;


    public static StickerSetCovered TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
        StickerSetCovered result = new StickerSetCovered();
        return result;
    }
}

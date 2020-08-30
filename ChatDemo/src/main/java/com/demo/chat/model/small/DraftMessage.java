package com.demo.chat.model.small;

import com.demo.chat.messager.AbstractSerializedData;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class DraftMessage {
    public int flags;
    public boolean no_webpage;
    public int reply_to_msg_id;
    public String message;
    public ArrayList<MessageEntity> entities = new ArrayList<>();
    public int date;
    public static DraftMessage TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
        DraftMessage result = new DraftMessage();
        return result;
    }

}

package com.demo.chat.model;

import com.demo.chat.messager.AbstractSerializedData;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description 登录的用户
 * @usage null
 */
public class User {
    public int id = 0;

    public static User TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
        User result = new User();
        result.readParams(stream, exception);
        return result;
    }

    public void readParams(AbstractSerializedData stream, boolean exception) {
        id = stream.readInt32(exception);
    }

    public void serializeToStream(AbstractSerializedData stream) {
        stream.writeInt32(id);
    }
}

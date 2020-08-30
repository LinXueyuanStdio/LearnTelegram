package com.demo.chat.model.bot;

import com.demo.chat.messager.AbstractSerializedData;
import com.demo.chat.messager.NativeByteBuffer;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description 机器人实体
 * @usage null
 */
public class BotInfo {
    public int user_id;
    public String description;
    public ArrayList<BotCommand> commands = new ArrayList<>();
    public int version;

    public static class BotCommand {
        public String command;
        public String description;
    }
    public static BotInfo TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
        BotInfo result = new BotInfo();
        return result;
    }

    public int networkType;

    public boolean disableFree = false;
    private static final ThreadLocal<NativeByteBuffer> sizeCalculator = new ThreadLocal<NativeByteBuffer>() {
        @Override
        protected NativeByteBuffer initialValue() {
            return new NativeByteBuffer(true);
        }
    };

    public void serializeToStream(AbstractSerializedData stream) {

    }
    public void freeResources() {

    }
    public int getObjectSize() {
        NativeByteBuffer byteBuffer = sizeCalculator.get();
        byteBuffer.rewind();
        serializeToStream(sizeCalculator.get());
        return byteBuffer.length();
    }
}

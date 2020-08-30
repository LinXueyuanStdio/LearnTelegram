package com.demo.chat.model.theme;

import com.demo.chat.messager.AbstractSerializedData;
import com.demo.chat.messager.NativeByteBuffer;
import com.demo.chat.model.small.Document;
import com.demo.chat.model.small.WallPaper;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class Skin {
    public int flags;
    public boolean creator;
    public boolean isDefault;
    public long id;
    public long access_hash;
    public String slug;
    public String title;
    public Document document;
    public SkinSettings settings;
    public int installs_count;

    public static class  SkinSettings{
        public int flags;
        public BaseTheme base_theme;
        public int accent_color;
        public int message_top_color;
        public int message_bottom_color;
        public WallPaper wallpaper;
    }

    public static abstract class BaseTheme{}

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

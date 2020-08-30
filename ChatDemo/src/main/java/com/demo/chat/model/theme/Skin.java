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

    public static class TL_baseThemeArctic extends BaseTheme {
        public static int constructor = 0x5b11125a;


        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_baseThemeNight extends BaseTheme {
        public static int constructor = 0xb7b31ea8;


        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_baseThemeClassic extends BaseTheme {
        public static int constructor = 0xc3a12462;


        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_baseThemeTinted extends BaseTheme {
        public static int constructor = 0x6d5f77ee;


        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_baseThemeDay extends BaseTheme {
        public static int constructor = 0xfbd81688;


        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }
    public int networkType;
    public static Skin TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
        Skin result = new Skin();
        return result;
    }
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

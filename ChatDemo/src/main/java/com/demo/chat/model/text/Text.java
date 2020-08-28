package com.demo.chat.model.text;

import com.demo.chat.messager.AbstractSerializedData;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class Text {

    public static class TL_textPhone extends RichText {
        public static int constructor = 0x1ccb966a;

        public RichText text;
        public String phone;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
            phone = stream.readString(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
            stream.writeString(phone);
        }
    }

    public static class TL_textSuperscript extends RichText {
        public static int constructor = 0xc7fb5e01;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
        }
    }

    public static class TL_textImage extends RichText {
        public static int constructor = 0x81ccf4f;

        public long document_id;
        public int w;
        public int h;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            document_id = stream.readInt64(exception);
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(document_id);
            stream.writeInt32(w);
            stream.writeInt32(h);
        }
    }

    public static class TL_textEmpty extends RichText {
        public static int constructor = 0xdc3d824f;


        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_textUrl extends RichText {
        public static int constructor = 0x3c2884c1;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
            url = stream.readString(exception);
            webpage_id = stream.readInt64(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
            stream.writeString(url);
            stream.writeInt64(webpage_id);
        }
    }

    public static class TL_textAnchor extends RichText {
        public static int constructor = 0x35553762;

        public RichText text;
        public String name;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
            name = stream.readString(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
            stream.writeString(name);
        }
    }

    public static class TL_textStrike extends RichText {
        public static int constructor = 0x9bf8bb95;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
        }
    }

    public static class TL_textMarked extends RichText {
        public static int constructor = 0x34b8621;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
        }
    }

    public static class TL_textFixed extends RichText {
        public static int constructor = 0x6c3f19b9;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
        }
    }

    public static class TL_textEmail extends RichText {
        public static int constructor = 0xde5a0dd6;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
            email = stream.readString(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
            stream.writeString(email);
        }
    }

    public static class TL_textPlain extends RichText {
        public static int constructor = 0x744694e0;

        public String text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = stream.readString(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(text);
        }
    }

    public static class TL_textConcat extends RichText {
        public static int constructor = 0x7e6260d7;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                RichText object = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                texts.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(0x1cb5c415);
            int count = texts.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                texts.get(a).serializeToStream(stream);
            }
        }
    }

    public static class TL_textBold extends RichText {
        public static int constructor = 0x6724abc4;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
        }
    }

    public static class TL_textItalic extends RichText {
        public static int constructor = 0xd912a59c;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
        }
    }

    public static class TL_textUnderline extends RichText {
        public static int constructor = 0xc12622c4;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
        }
    }

    public static class TL_textSubscript extends RichText {
        public static int constructor = 0xed6a8504;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
        }
    }

}

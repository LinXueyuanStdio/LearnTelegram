package com.demo.chat.model.text;

import com.demo.chat.messager.AbstractSerializedData;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class RichText {
    public String url;
    public long webpage_id;
    public String email;
    public ArrayList<RichText> texts = new ArrayList<>();
    public RichText parentRichText;

    public void readParams(AbstractSerializedData stream, boolean exception) {

    }
    public void serializeToStream(AbstractSerializedData stream) {

    }

    public static RichText TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
        RichText result = null;
        switch (constructor) {
            case 0x1ccb966a:
                result = new Text.TL_textPhone();
                break;
            case 0xc7fb5e01:
                result = new Text.TL_textSuperscript();
                break;
            case 0x81ccf4f:
                result = new Text.TL_textImage();
                break;
            case 0xc12622c4:
                result = new Text.TL_textUnderline();
                break;
            case 0xed6a8504:
                result = new Text.TL_textSubscript();
                break;
            case 0x3c2884c1:
                result = new Text.TL_textUrl();
                break;
            case 0x35553762:
                result = new Text.TL_textAnchor();
                break;
            case 0xdc3d824f:
                result = new Text.TL_textEmpty();
                break;
            case 0xde5a0dd6:
                result = new Text.TL_textEmail();
                break;
            case 0x744694e0:
                result = new Text.TL_textPlain();
                break;
            case 0x6724abc4:
                result = new Text.TL_textBold();
                break;
            case 0x9bf8bb95:
                result = new Text.TL_textStrike();
                break;
            case 0x7e6260d7:
                result = new Text.TL_textConcat();
                break;
            case 0xd912a59c:
                result = new Text.TL_textItalic();
                break;
            case 0x34b8621:
                result = new Text.TL_textMarked();
                break;
            case 0x6c3f19b9:
                result = new Text.TL_textFixed();
                break;
        }
        if (result == null && exception) {
            throw new RuntimeException(String.format("can't parse magic %x in RichText", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }

}

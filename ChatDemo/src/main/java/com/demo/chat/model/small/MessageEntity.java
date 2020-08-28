package com.demo.chat.model.small;

import com.demo.chat.messager.AbstractSerializedData;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/26
 * @description null
 * @usage null
 */
public class MessageEntity {
    public int offset;
    public int length;
    public String url;
    public String language;
    public int user_id;//TODO isMentionName

    public void setMentionName(boolean isMentionName) {}

    public void setCode(boolean isCode) {}

    public void setPre(boolean isPre) {}

    public void setBold(boolean isBold) {}

    public void setItalic(boolean isItalic) {}

    public void setStrike(boolean isStrike) {}

    public void setUnderline(boolean isUnderline) {}

    public void setTextUrl(boolean isTextUrl) {}

    public boolean isMentionName() {
        return false;
    }

    public boolean isCode() {
        return false;
    }

    public boolean isPre() {
        return false;
    }

    public boolean isBold() {
        return false;
    }

    public boolean isItalic() {
        return false;
    }

    public boolean isStrike() {
        return false;
    }

    public boolean isUnderline() {
        return false;
    }

    public boolean isTextUrl() {
        return false;
    }

    public boolean isUrl() {
        return false;
    }

    public boolean isEmail() {
        return false;
    }

    public boolean isBlockquote() {
        return false;
    }

    public boolean isMention() {
        return false;
    }

    public boolean isHashtag() {
        return false;
    }

    public boolean isBotCommand() {
        return false;
    }

    public boolean isPhone() {
        return false;
    }

    public boolean isBankCard() {
        return false;
    }

    public boolean isCashtag() {
        return false;
    }


    public static MessageEntity TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
        MessageEntity result = new MessageEntity();
        return result;
    }
}

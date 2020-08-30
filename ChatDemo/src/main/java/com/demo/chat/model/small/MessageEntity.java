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

    private int statusType = 0;

    public void setMentionName(boolean isMentionName) {statusType=1;}

    public void setCode(boolean isCode) {statusType=2;}

    public void setPre(boolean isPre) {statusType=3;}

    public void setBold(boolean isBold) {statusType=4;}

    public void setItalic(boolean isItalic) {statusType=5;}

    public void setStrike(boolean isStrike) {statusType=6;}

    public void setUnderline(boolean isUnderline) {statusType=7;}

    public void setTextUrl(boolean isTextUrl) {statusType=8;}
    public void setBlockquote(boolean isTextUrl) {statusType=11;}

    public boolean isMentionName() {
        return statusType==1;
    }

    public boolean isCode() {
        return statusType==2;
    }

    public boolean isPre() {
        return statusType==3;
    }

    public boolean isBold() {
        return statusType==4;
    }

    public boolean isItalic() {
        return statusType==5;
    }

    public boolean isStrike() {
        return statusType==6;
    }

    public boolean isUnderline() {
        return statusType==7;
    }

    public boolean isTextUrl() {
        return statusType==8;
    }

    public boolean isUrl() {
        return statusType==9;
    }

    public boolean isEmail() {
        return statusType==10;
    }

    public boolean isBlockquote() {
        return statusType==11;
    }

    public boolean isMention() {
        return statusType==12;
    }

    public boolean isHashtag() {
        return statusType==13;
    }

    public boolean isBotCommand() {
        return statusType==14;
    }

    public boolean isPhone() {
        return statusType==15;
    }

    public boolean isBankCard() {
        return statusType==16;
    }

    public boolean isCashtag() {
        return statusType==17;
    }


    public static MessageEntity TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
        MessageEntity result = new MessageEntity();
        return result;
    }
}

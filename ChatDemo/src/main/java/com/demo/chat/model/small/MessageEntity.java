package com.demo.chat.model.small;

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
}

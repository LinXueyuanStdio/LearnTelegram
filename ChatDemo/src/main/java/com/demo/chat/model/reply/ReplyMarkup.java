package com.demo.chat.model.reply;

import com.demo.chat.model.bot.KeyboardButton;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class ReplyMarkup {
    public ArrayList<KeyboardButtonRow> rows = new ArrayList<>();
    public int flags;
    public boolean selective;
    public boolean single_use;
    public boolean resize;

    public boolean isInlineMarkup() {return false;}

    public boolean isKeyboardHide() {return false;}

    public boolean isKeyboardForceReply() {return false;}

    public boolean isKeyboardMarkup() {return false;}

    public static class KeyboardButtonRow {
        public ArrayList<KeyboardButton> buttons = new ArrayList<>();
    }
}

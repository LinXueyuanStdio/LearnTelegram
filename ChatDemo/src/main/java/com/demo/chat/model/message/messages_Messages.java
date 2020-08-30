package com.demo.chat.model.message;

import com.demo.chat.model.Chat;
import com.demo.chat.model.Message;
import com.demo.chat.model.User;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/30
 * @description null
 * @usage null
 */
public class messages_Messages {
    public ArrayList<Message> messages = new ArrayList<>();
    public ArrayList<Chat> chats = new ArrayList<>();
    public ArrayList<User> users = new ArrayList<>();
    public int flags;
    public boolean inexact;
    public int pts;
    public int count;
    public int next_rate;
}

package com.demo.chat.model.message;

import com.demo.chat.model.bot.ReactionCount;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class MessageReactions {

    public int flags;
    public boolean min;
    public ArrayList<ReactionCount> results = new ArrayList<>();

}

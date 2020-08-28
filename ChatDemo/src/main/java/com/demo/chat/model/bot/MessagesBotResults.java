package com.demo.chat.model.bot;

import com.demo.chat.model.User;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class MessagesBotResults {
    public int flags;
    public boolean gallery;
    public long query_id;
    public String next_offset;
    public InlineBotSwitchPM switch_pm;
    public ArrayList<BotInlineResult> results = new ArrayList<>();
    public int cache_time;
    public ArrayList<User> users = new ArrayList<>();
}

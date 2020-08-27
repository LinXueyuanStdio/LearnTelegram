package com.demo.chat.model.bot;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description 机器人实体
 * @usage null
 */
public class BotInfo {
    public int user_id;
    public String description;
    public ArrayList<BotCommand> commands = new ArrayList<>();
    public int version;

    public static class BotCommand {
        public String command;
        public String description;
    }
}

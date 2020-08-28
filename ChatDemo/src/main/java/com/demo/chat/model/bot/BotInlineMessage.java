package com.demo.chat.model.bot;

import com.demo.chat.model.reply.ReplyMarkup;
import com.demo.chat.model.small.MessageEntity;
import com.demo.chat.model.small.MessageMedia;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class BotInlineMessage {
    public int flags;
    public MessageMedia.GeoPoint geo;
    public String title;
    public String address;
    public String provider;
    public String venue_id;
    public String venue_type;
    public ReplyMarkup reply_markup;
    public String message;
    public ArrayList<MessageEntity> entities = new ArrayList<>();
    public String phone_number;
    public String first_name;
    public String last_name;
    public String vcard;
    public boolean no_webpage;
    public int period;
}

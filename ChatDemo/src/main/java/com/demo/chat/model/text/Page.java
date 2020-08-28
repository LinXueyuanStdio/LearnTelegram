package com.demo.chat.model.text;

import com.demo.chat.model.small.Document;
import com.demo.chat.model.small.MessageMedia;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class Page {
    public int flags;
    public boolean part;
    public boolean rtl;
    public String url;
    public ArrayList<PageBlock> blocks = new ArrayList<>();
    public ArrayList<MessageMedia.Photo> photos = new ArrayList<>();
    public ArrayList<Document> documents = new ArrayList<>();
    public boolean v2;
    public int views;
}

package com.demo.chat.model.small;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class WebDocument {
    public String url;
    public long access_hash;
    public int size;
    public String mime_type;
    public ArrayList<Document.DocumentAttribute> attributes = new ArrayList<>();
}

package com.demo.chat.model.theme;

import com.demo.chat.model.small.Document;
import com.demo.chat.model.small.WallPaper;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class Skin {
    public int flags;
    public boolean creator;
    public boolean isDefault;
    public long id;
    public long access_hash;
    public String slug;
    public String title;
    public Document document;
    public SkinSettings settings;
    public int installs_count;

    public static class  SkinSettings{
        public int flags;
        public BaseTheme base_theme;
        public int accent_color;
        public int message_top_color;
        public int message_bottom_color;
        public WallPaper wallpaper;
    }

    public static abstract class BaseTheme{}
}

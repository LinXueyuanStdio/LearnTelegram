package com.demo.chat.model.small;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class WallPaper {

    public long id;
    public int flags;
    public boolean creator;
    public boolean isDefault;
    public boolean pattern;
    public boolean dark;
    public long access_hash;
    public String slug;
    public Document document;
    public WallPaperSettings settings;

    public static class WallPaperSettings {
        public int flags;
        public boolean blur;
        public boolean motion;
        public int background_color;
        public int second_background_color;
        public int intensity;
        public int rotation;
    }
}

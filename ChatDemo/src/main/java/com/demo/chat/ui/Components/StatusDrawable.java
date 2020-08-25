package com.demo.chat.ui.Components;

import android.graphics.drawable.Drawable;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public abstract class StatusDrawable extends Drawable {
    public abstract void start();
    public abstract void stop();
    public abstract void setIsChat(boolean value);
}

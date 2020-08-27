package com.demo.chat.messager;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class ExtendedBitmapDrawable extends BitmapDrawable {

    private boolean canInvert;
    private int orientation;

    public ExtendedBitmapDrawable(Bitmap bitmap, boolean invert, int orient) {
        super(bitmap);
        canInvert = invert;
        orientation = orient;
    }

    public boolean isCanInvert() {
        return canInvert;
    }

    public int getOrientation() {
        return orientation;
    }
}

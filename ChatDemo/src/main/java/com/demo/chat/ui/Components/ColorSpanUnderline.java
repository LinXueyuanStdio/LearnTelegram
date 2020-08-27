package com.demo.chat.ui.Components;

import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class ColorSpanUnderline extends ForegroundColorSpan {

    public ColorSpanUnderline(int color) {
        super(color);
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        ds.setUnderlineText(true);
    }
}

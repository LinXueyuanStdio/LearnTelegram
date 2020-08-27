package com.demo.chat.ui.Components;

import android.text.TextPaint;

import com.demo.chat.messager.AndroidUtilities;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class URLSpanNoUnderlineBold extends URLSpanNoUnderline {

    public URLSpanNoUnderlineBold(String url) {
        super(url != null ? url.replace('\u202E', ' ') : url);
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        ds.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        ds.setUnderlineText(false);
    }
}

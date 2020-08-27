package com.demo.chat.ui.Components;

import android.text.TextPaint;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class URLSpanUserMentionPhotoViewer extends URLSpanUserMention {

    public URLSpanUserMentionPhotoViewer(String url, boolean isOutOwner) {
        super(url, 2);
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        ds.setColor(0xffffffff);
        ds.setUnderlineText(false);
    }
}

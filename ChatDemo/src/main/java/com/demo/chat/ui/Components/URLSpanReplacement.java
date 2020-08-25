package com.demo.chat.ui.Components;

import android.net.Uri;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.View;

import com.demo.chat.messager.browser.Browser;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class URLSpanReplacement extends URLSpan {

    private TextStyleSpan.TextStyleRun style;

    public URLSpanReplacement(String url) {
        this(url, null);
    }

    public URLSpanReplacement(String url, TextStyleSpan.TextStyleRun run) {
        super(url != null ? url.replace('\u202E', ' ') : url);
        style = run;
    }

    public TextStyleSpan.TextStyleRun getTextStyleRun() {
        return style;
    }

    @Override
    public void onClick(View widget) {
        Uri uri = Uri.parse(getURL());
        Browser.openUrl(widget.getContext(), uri);
    }

    @Override
    public void updateDrawState(TextPaint p) {
        int color = p.getColor();
        super.updateDrawState(p);
        if (style != null) {
            style.applyStyle(p);
            p.setUnderlineText(p.linkColor == color);
        }
    }
}

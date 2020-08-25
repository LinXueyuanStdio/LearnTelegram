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
public class URLSpanNoUnderline extends URLSpan {

    private TextStyleSpan.TextStyleRun style;

    public URLSpanNoUnderline(String url) {
        this(url, null);
    }

    public URLSpanNoUnderline(String url, TextStyleSpan.TextStyleRun run) {
        super(url != null ? url.replace('\u202E', ' ') : url);
        style = run;
    }

    @Override
    public void onClick(View widget) {
        String url = getURL();
        if (url.startsWith("@")) {
            Uri uri = Uri.parse("https://t.me/" + url.substring(1));
            Browser.openUrl(widget.getContext(), uri);
        } else {
            Browser.openUrl(widget.getContext(), url);
        }
    }

    @Override
    public void updateDrawState(TextPaint p) {
        int l = p.linkColor;
        int c = p.getColor();
        super.updateDrawState(p);
        if (style != null) {
            style.applyStyle(p);
        }
        p.setUnderlineText(l == c);
    }
}

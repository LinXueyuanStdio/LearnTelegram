package com.demo.chat.ui.Components;

import android.net.Uri;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.View;

import com.demo.chat.messager.browser.Browser;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class URLSpanBrowser extends URLSpan {

    private TextStyleSpan.TextStyleRun style;

    public URLSpanBrowser(String url) {
        this(url, null);
    }

    public URLSpanBrowser(String url, TextStyleSpan.TextStyleRun run) {
        super(url != null ? url.replace('\u202E', ' ') : url);
        style = run;
    }

    public TextStyleSpan.TextStyleRun getStyle() {
        return style;
    }

    @Override
    public void onClick(View widget) {
        Uri uri = Uri.parse(getURL());
        Browser.openUrl(widget.getContext(), uri);
    }

    @Override
    public void updateDrawState(TextPaint p) {
        super.updateDrawState(p);
        if (style != null) {
            style.applyStyle(p);
        }
        p.setUnderlineText(true);
    }
}

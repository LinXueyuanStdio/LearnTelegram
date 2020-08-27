package com.demo.chat.ui.Components;

import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class TextPaintUrlSpan extends MetricAffectingSpan {

    private TextPaint textPaint;
    private int textSize;
    private int color;
    private String currentUrl;

    public TextPaintUrlSpan(TextPaint paint, String url) {
        textPaint = paint;
        currentUrl = url;
    }

    public String getUrl() {
        return currentUrl;
    }

    public TextPaint getTextPaint() {
        return textPaint;
    }

    @Override
    public void updateMeasureState(TextPaint p) {
        if (textPaint != null) {
            p.setColor(textPaint.getColor());
            p.setTypeface(textPaint.getTypeface());
            p.setFlags(textPaint.getFlags());
            p.setTextSize(textPaint.getTextSize());
            p.baselineShift = textPaint.baselineShift;
            p.bgColor = textPaint.bgColor;
        }
    }

    @Override
    public void updateDrawState(TextPaint p) {
        if (textPaint != null) {
            p.setColor(textPaint.getColor());
            p.setTypeface(textPaint.getTypeface());
            p.setFlags(textPaint.getFlags());
            p.setTextSize(textPaint.getTextSize());
            p.baselineShift = textPaint.baselineShift;
            p.bgColor = textPaint.bgColor;
        }
    }
}
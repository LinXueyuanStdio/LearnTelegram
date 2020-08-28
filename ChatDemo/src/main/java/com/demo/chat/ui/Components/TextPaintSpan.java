package com.demo.chat.ui.Components;

import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class TextPaintSpan extends MetricAffectingSpan {

    private TextPaint textPaint;

    public TextPaintSpan(TextPaint paint) {
        textPaint = paint;
    }

    @Override
    public void updateMeasureState(TextPaint p) {
        p.setColor(textPaint.getColor());
        p.setTypeface(textPaint.getTypeface());
        p.setFlags(textPaint.getFlags());
        p.setTextSize(textPaint.getTextSize());
        p.baselineShift = textPaint.baselineShift;
        p.bgColor = textPaint.bgColor;
    }

    @Override
    public void updateDrawState(TextPaint p) {
        p.setColor(textPaint.getColor());
        p.setTypeface(textPaint.getTypeface());
        p.setFlags(textPaint.getFlags());
        p.setTextSize(textPaint.getTextSize());
        p.baselineShift = textPaint.baselineShift;
        p.bgColor = textPaint.bgColor;
    }
}

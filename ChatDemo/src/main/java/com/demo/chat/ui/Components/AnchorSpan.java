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
public class AnchorSpan extends MetricAffectingSpan {

    private String name;

    public AnchorSpan(String n) {
        name = n.toLowerCase();
    }

    public String getName() {
        return name;
    }

    @Override
    public void updateMeasureState(TextPaint p) {

    }

    @Override
    public void updateDrawState(TextPaint tp) {

    }
}

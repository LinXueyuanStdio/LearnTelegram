package com.demo.chat.ui.Components;

import android.content.Context;
import android.text.Layout;
import android.widget.TextView;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class CorrectlyMeasuringTextView extends TextView {

    public CorrectlyMeasuringTextView(Context context) {
        super(context);
    }

    public void onMeasure(int wms, int hms) {
        super.onMeasure(wms, hms);
        try {
            Layout l = getLayout();
            if (l.getLineCount() <= 1) {
                return;
            }
            int maxw = 0;
            for (int i = l.getLineCount() - 1; i >= 0; --i) {
                maxw = Math.max(maxw, Math.round(l.getPaint().measureText(getText(), l.getLineStart(i), l.getLineEnd(i))));
            }
            super.onMeasure(Math.min(maxw + getPaddingLeft() + getPaddingRight(), getMeasuredWidth()) | MeasureSpec.EXACTLY, getMeasuredHeight() | MeasureSpec.EXACTLY);
        } catch (Exception ignore) {

        }
    }
}
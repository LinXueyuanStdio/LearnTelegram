package com.demo.chat.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.theme.Theme;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class DividerCell extends View {

    public DividerCell(Context context) {
        super(context);
        setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), getPaddingTop() + getPaddingBottom() + 1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawLine(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getPaddingTop(), Theme.dividerPaint);
    }
}

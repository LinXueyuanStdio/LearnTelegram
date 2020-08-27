package com.demo.chat.ui.Cells;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.ui.Components.LayoutHelper;
import com.demo.chat.ui.Components.RadialProgressView;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class LoadingCell extends FrameLayout {

    private RadialProgressView progressBar;
    private int height;

    public LoadingCell(Context context) {
        this(context, AndroidUtilities.dp(40), AndroidUtilities.dp(54));
    }

    public LoadingCell(Context context, int size, int h) {
        super(context);

        height = h;

        progressBar = new RadialProgressView(context);
        progressBar.setSize(size);
        addView(progressBar, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }
}

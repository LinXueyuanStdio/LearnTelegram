package com.demo.chat.ui.Cells;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.demo.chat.R;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.theme.Theme;
import com.demo.chat.ui.Components.CombinedDrawable;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class ShadowSectionCell extends View {

    private int size;

    public ShadowSectionCell(Context context) {
        this(context, 12);
    }

    public ShadowSectionCell(Context context, int s) {
        super(context);
        setBackgroundDrawable(Theme.getThemedDrawable(context, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
        size = s;
    }

    public ShadowSectionCell(Context context, int s, int backgroundColor) {
        super(context);
        Drawable shadowDrawable = Theme.getThemedDrawable(context, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow);
        Drawable background = new ColorDrawable(backgroundColor);
        CombinedDrawable combinedDrawable = new CombinedDrawable(background, shadowDrawable, 0, 0);
        combinedDrawable.setFullsize(true);
        setBackgroundDrawable(combinedDrawable);
        size = s;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(size), MeasureSpec.EXACTLY));
    }
}

package com.demo.chat.ui.Cells;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.demo.chat.controller.LocaleController;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.theme.Theme;
import com.demo.chat.theme.ThemeDescription;
import com.demo.chat.ui.Components.LayoutHelper;
import com.demo.chat.ui.Components.RecyclerListView;

import java.util.List;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class GraySectionCell extends FrameLayout {

    private TextView textView;
    private TextView rightTextView;

    public GraySectionCell(Context context) {
        super(context);

        setBackgroundColor(Theme.getColor(Theme.key_graySection));

        textView = new TextView(getContext());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setTextColor(Theme.getColor(Theme.key_graySectionText));
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 16, 0, 16, 0));

        rightTextView = new TextView(getContext());
        rightTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        rightTextView.setTextColor(Theme.getColor(Theme.key_graySectionText));
        rightTextView.setGravity((LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL);
        addView(rightTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.TOP, 16, 0, 16, 0));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(32), MeasureSpec.EXACTLY));
    }

    public void setText(String text) {
        textView.setText(text);
        rightTextView.setVisibility(GONE);
    }

    public void setText(String left, String right, View.OnClickListener onClickListener) {
        textView.setText(left);
        rightTextView.setText(right);
        rightTextView.setOnClickListener(onClickListener);
        rightTextView.setVisibility(VISIBLE);
    }

    public static void createThemeDescriptions(List<ThemeDescription> descriptions, RecyclerListView listView) {
        descriptions.add(new ThemeDescription(listView, 0, new Class[]{GraySectionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_graySectionText));
        descriptions.add(new ThemeDescription(listView, 0, new Class[]{GraySectionCell.class}, new String[]{"rightTextView"}, null, null, null, Theme.key_graySectionText));
        descriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{GraySectionCell.class}, null, null, null, Theme.key_graySection));
    }
}

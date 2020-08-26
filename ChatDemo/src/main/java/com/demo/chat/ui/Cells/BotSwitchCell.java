package com.demo.chat.ui.Cells;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.demo.chat.controller.LocaleController;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.theme.Theme;
import com.demo.chat.ui.Components.LayoutHelper;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/26
 * @description null
 * @usage null
 */
public class BotSwitchCell extends FrameLayout {

    private TextView textView;

    public BotSwitchCell(Context context) {
        super(context);

        textView = new TextView(context);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setTextColor(Theme.getColor(Theme.key_chat_botSwitchToInlineText));
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setMaxLines(1);
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT));
        addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT), 14, 0, 14, 0));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(36), MeasureSpec.EXACTLY));
    }

    public void setText(String text) {
        textView.setText(text);
    }

    public TextView getTextView() {
        return textView;
    }
}

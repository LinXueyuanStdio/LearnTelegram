package com.demo.chat.ui.Cells;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.demo.chat.R;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.theme.Theme;
import com.demo.chat.ui.Components.LayoutHelper;
import com.demo.chat.ui.Components.RadialProgressView;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/26
 * @description null
 * @usage null
 */
public class ChatLoadingCell extends FrameLayout {

    private FrameLayout frameLayout;
    private RadialProgressView progressBar;

    public ChatLoadingCell(Context context) {
        super(context);

        frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundResource(R.drawable.system_loader);
        frameLayout.getBackground().setColorFilter(Theme.colorFilter);
        addView(frameLayout, LayoutHelper.createFrame(36, 36, Gravity.CENTER));

        progressBar = new RadialProgressView(context);
        progressBar.setSize(AndroidUtilities.dp(28));
        progressBar.setProgressColor(Theme.getColor(Theme.key_chat_serviceText));
        frameLayout.addView(progressBar, LayoutHelper.createFrame(32, 32, Gravity.CENTER));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(44), MeasureSpec.EXACTLY));
    }

    public void setProgressVisible(boolean value) {
        frameLayout.setVisibility(value ? VISIBLE : INVISIBLE);
    }
}

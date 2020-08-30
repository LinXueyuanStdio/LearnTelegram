package com.demo.chat.ui.Cells;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.demo.chat.R;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.theme.Theme;
import com.demo.chat.ui.Components.LayoutHelper;
import com.demo.chat.ui.Components.SeekBarView;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/30
 * @description null
 * @usage null
 */
public class BrightnessControlCell extends FrameLayout {

    private ImageView leftImageView;
    private ImageView rightImageView;
    private SeekBarView seekBarView;

    public BrightnessControlCell(Context context) {
        super(context);

        leftImageView = new ImageView(context);
        leftImageView.setImageResource(R.drawable.brightness_low);
        addView(leftImageView, LayoutHelper.createFrame(24, 24, Gravity.LEFT | Gravity.TOP, 17, 12, 0, 0));

        seekBarView = new SeekBarView(context) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return super.onTouchEvent(event);
            }
        };
        seekBarView.setReportChanges(true);
        seekBarView.setDelegate(new SeekBarView.SeekBarViewDelegate() {
            @Override
            public void onSeekBarDrag(boolean stop, float progress) {
                didChangedValue(progress);
            }

            @Override
            public void onSeekBarPressed(boolean pressed) {

            }
        });
        addView(seekBarView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.TOP | Gravity.LEFT, 54, 5, 54, 0));

        rightImageView = new ImageView(context);
        rightImageView.setImageResource(R.drawable.brightness_high);
        addView(rightImageView, LayoutHelper.createFrame(24, 24, Gravity.RIGHT | Gravity.TOP, 0, 12, 17, 0));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        leftImageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.MULTIPLY));
        rightImageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.MULTIPLY));
    }

    protected void didChangedValue(float value) {

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(48), MeasureSpec.EXACTLY));
    }

    public void setProgress(float value) {
        seekBarView.setProgress(value);
    }
}

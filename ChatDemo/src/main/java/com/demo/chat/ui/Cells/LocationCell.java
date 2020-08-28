package com.demo.chat.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ShapeDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.demo.chat.controller.LocaleController;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.model.small.MessageMedia;
import com.demo.chat.theme.Theme;
import com.demo.chat.ui.Components.BackupImageView;
import com.demo.chat.ui.Components.LayoutHelper;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class LocationCell extends FrameLayout {

    private TextView nameTextView;
    private TextView addressTextView;
    private BackupImageView imageView;
    private ShapeDrawable circleDrawable;
    private boolean needDivider;
    private boolean wrapContent;

    public LocationCell(Context context, boolean wrap) {
        super(context);

        wrapContent = wrap;

        imageView = new BackupImageView(context);
        imageView.setBackground(circleDrawable = Theme.createCircleDrawable(AndroidUtilities.dp(42), 0xffffffff));
        imageView.setSize(AndroidUtilities.dp(30), AndroidUtilities.dp(30));
        addView(imageView, LayoutHelper.createFrame(42, 42, Gravity.TOP | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT), LocaleController.isRTL ? 0 : 15, 11, LocaleController.isRTL ? 15 : 0, 0));

        nameTextView = new TextView(context);
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        nameTextView.setMaxLines(1);
        nameTextView.setEllipsize(TextUtils.TruncateAt.END);
        nameTextView.setSingleLine(true);
        nameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        nameTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT), (LocaleController.isRTL ? 16 : 73), 10, (LocaleController.isRTL ? 73 : 16), 0));

        addressTextView = new TextView(context);
        addressTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        addressTextView.setMaxLines(1);
        addressTextView.setEllipsize(TextUtils.TruncateAt.END);
        addressTextView.setSingleLine(true);
        addressTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3));
        addressTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        addView(addressTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT), (LocaleController.isRTL ? 16 : 73), 35, (LocaleController.isRTL ? 73 : 16), 0));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (wrapContent) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
        } else {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
        }
    }

    public BackupImageView getImageView() {
        return imageView;
    }

    public void setLocation(MessageMedia location, String icon, int pos, boolean divider) {
        setLocation(location, icon, null, pos, divider);
    }

    public static int getColorForIndex(int index) {
        switch (index % 7) {
            case 0:
                return 0xffeb6060;
            case 1:
                return 0xfff2c04b;
            case 2:
                return 0xff459df5;
            case 3:
                return 0xff36c766;
            case 4:
                return 0xff8771fd;
            case 5:
                return 0xff43b9d7;
            case 6:
            default:
                return 0xffec638b;
        }
    }

    public void setLocation(MessageMedia location, String icon, String label, int pos, boolean divider) {
        needDivider = divider;
        circleDrawable.getPaint().setColor(getColorForIndex(pos));
        nameTextView.setText(location.title);
        if (label != null) {
            addressTextView.setText(label);
        } else {
            addressTextView.setText(location.address);
        }
        imageView.setImage(icon, null, null);
        setWillNotDraw(!divider);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(AndroidUtilities.dp(72), getHeight() - 1, getWidth(), getHeight() - 1, Theme.dividerPaint);
        }
    }
}

package com.demo.chat.ui.Cells;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.demo.chat.R;
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
public class PhotoAttachPermissionCell extends FrameLayout {

    private ImageView imageView;
    private ImageView imageView2;
    private TextView textView;
    private int itemSize;

    public PhotoAttachPermissionCell(Context context) {
        super(context);

        imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chat_attachPermissionImage), PorterDuff.Mode.MULTIPLY));
        addView(imageView, LayoutHelper.createFrame(44, 44, Gravity.CENTER, 5, 0, 0, 27));

        imageView2 = new ImageView(context);
        imageView2.setScaleType(ImageView.ScaleType.CENTER);
        imageView2.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chat_attachPermissionMark), PorterDuff.Mode.MULTIPLY));
        addView(imageView2, LayoutHelper.createFrame(44, 44, Gravity.CENTER, 5, 0, 0, 27));

        textView = new TextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_chat_attachPermissionText));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        textView.setGravity(Gravity.CENTER);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 13, 5, 0));

        itemSize = AndroidUtilities.dp(80);
    }

    public void setItemSize(int size) {
        itemSize = size;
    }

    public void setType(int type) {
        if (type == 0) {
            imageView.setImageResource(R.drawable.permissions_camera1);
            imageView2.setImageResource(R.drawable.permissions_camera2);
            textView.setText(LocaleController.getString("CameraPermissionText", R.string.CameraPermissionText));

            imageView.setLayoutParams(LayoutHelper.createFrame(44, 44, Gravity.CENTER, 5, 0, 0, 27));
            imageView2.setLayoutParams(LayoutHelper.createFrame(44, 44, Gravity.CENTER, 5, 0, 0, 27));
        } else {
            imageView.setImageResource(R.drawable.permissions_gallery1);
            imageView2.setImageResource(R.drawable.permissions_gallery2);
            textView.setText(LocaleController.getString("GalleryPermissionText", R.string.GalleryPermissionText));

            imageView.setLayoutParams(LayoutHelper.createFrame(44, 44, Gravity.CENTER, 0, 0, 2, 27));
            imageView2.setLayoutParams(LayoutHelper.createFrame(44, 44, Gravity.CENTER, 0, 0, 2, 27));
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(itemSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(itemSize + AndroidUtilities.dp(5), MeasureSpec.EXACTLY));
    }
}

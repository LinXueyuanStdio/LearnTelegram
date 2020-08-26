package com.demo.chat.ui.Cells;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.demo.chat.ApplicationLoader;
import com.demo.chat.R;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.theme.Theme;
import com.demo.chat.ui.Components.LayoutHelper;

import java.io.File;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/26
 * @description null
 * @usage null
 */
@SuppressLint("NewApi")
public class PhotoAttachCameraCell extends FrameLayout {

    private ImageView imageView;
    private ImageView backgroundView;
    private int itemSize;

    public PhotoAttachCameraCell(Context context) {
        super(context);

        backgroundView = new ImageView(context);
        backgroundView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        //backgroundView.setAdjustViewBounds(false);
        addView(backgroundView, LayoutHelper.createFrame(80, 80));

        imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setImageResource(R.drawable.instant_camera);
        addView(imageView, LayoutHelper.createFrame(80, 80));
        setFocusable(true);

        itemSize = AndroidUtilities.dp(0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(itemSize + AndroidUtilities.dp(5), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(itemSize + AndroidUtilities.dp(5), MeasureSpec.EXACTLY));
    }

    public void setItemSize(int size) {
        itemSize = size;

        FrameLayout.LayoutParams layoutParams = (LayoutParams) imageView.getLayoutParams();
        layoutParams.width = layoutParams.height = itemSize;

        layoutParams = (LayoutParams) backgroundView.getLayoutParams();
        layoutParams.width = layoutParams.height = itemSize;
    }

    public ImageView getImageView() {
        return imageView;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        imageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogCameraIcon), PorterDuff.Mode.MULTIPLY));
    }

    public void updateBitmap() {
        Bitmap bitmap = null;
        try {
            File file = new File(ApplicationLoader.getFilesDirFixed(), "cthumb.jpg");
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Throwable ignore) {

        }
        if (bitmap != null) {
            backgroundView.setImageBitmap(bitmap);
        } else {
            backgroundView.setImageResource(R.drawable.icplaceholder);
        }
    }
}

package com.demo.chat.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;

import com.demo.chat.R;
import com.demo.chat.controller.FileLoader;
import com.demo.chat.controller.LocaleController;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.ImageLocation;
import com.demo.chat.model.action.MessageObject;
import com.demo.chat.model.small.Document;
import com.demo.chat.model.small.PhotoSize;
import com.demo.chat.theme.Theme;
import com.demo.chat.ui.Components.BackupImageView;
import com.demo.chat.ui.Components.LayoutHelper;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/26
 * @description null
 * @usage null
 */
public class StickerCell extends FrameLayout {

    private BackupImageView imageView;
    private Document sticker;
    private Object parentObject;
    private long lastUpdateTime;
    private boolean scaled;
    private float scale;
    private long time = 0;
    private boolean clearsInputField;
    private static AccelerateInterpolator interpolator = new AccelerateInterpolator(0.5f);

    public StickerCell(Context context) {
        super(context);

        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        imageView.setLayerNum(1);
        addView(imageView, LayoutHelper.createFrame(66, 66, Gravity.CENTER_HORIZONTAL, 0, 5, 0, 0));
        setFocusable(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(76) + getPaddingLeft() + getPaddingRight(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(78), MeasureSpec.EXACTLY));
    }

    @Override
    public void setPressed(boolean pressed) {
        if (imageView.getImageReceiver().getPressed() != pressed) {
            imageView.getImageReceiver().setPressed(pressed ? 1 : 0);
            imageView.invalidate();
        }
        super.setPressed(pressed);
    }

    public void setClearsInputField(boolean value) {
        clearsInputField = value;
    }

    public boolean isClearsInputField() {
        return clearsInputField;
    }

    public void setSticker(Document document, Object parent, int side) {
        parentObject = parent;
        if (document != null) {
            PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
            if (MessageObject.canAutoplayAnimatedSticker(document)) {
                if (thumb != null) {
                    imageView.setImage(ImageLocation.getForDocument(document), "80_80", ImageLocation.getForDocument(thumb, document), null, 0, parentObject);
                } else {
                    imageView.setImage(ImageLocation.getForDocument(document), "80_80", null, null, parentObject);
                }
            } else {
                imageView.setImage(ImageLocation.getForDocument(thumb, document), null, "webp", null, parentObject);
            }
        }
        sticker = document;
        if (side == -1) {
            setBackgroundResource(R.drawable.stickers_back_left);
            setPadding(AndroidUtilities.dp(7), 0, 0, 0);
        } else if (side == 0) {
            setBackgroundResource(R.drawable.stickers_back_center);
            setPadding(0, 0, 0, 0);
        } else if (side == 1) {
            setBackgroundResource(R.drawable.stickers_back_right);
            setPadding(0, 0, AndroidUtilities.dp(7), 0);
        } else if (side == 2) {
            setBackgroundResource(R.drawable.stickers_back_all);
            setPadding(AndroidUtilities.dp(3), 0, AndroidUtilities.dp(3), 0);
        }
        Drawable background = getBackground();
        if (background != null) {
            background.setAlpha(230);
            background.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chat_stickersHintPanel), PorterDuff.Mode.MULTIPLY));
        }
    }

    public Document getSticker() {
        return sticker;
    }

    public Object getParentObject() {
        return parentObject;
    }

    public void setScaled(boolean value) {
        scaled = value;
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    public boolean showingBitmap() {
        return imageView.getImageReceiver().getBitmap() != null;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean result = super.drawChild(canvas, child, drawingTime);
        if (child == imageView && (scaled && scale != 0.8f || !scaled && scale != 1.0f)) {
            long newTime = System.currentTimeMillis();
            long dt = (newTime - lastUpdateTime);
            lastUpdateTime = newTime;
            if (scaled && scale != 0.8f) {
                scale -= dt / 400.0f;
                if (scale < 0.8f) {
                    scale = 0.8f;
                }
            } else {
                scale += dt / 400.0f;
                if (scale > 1.0f) {
                    scale = 1.0f;
                }
            }
            imageView.setScaleX(scale);
            imageView.setScaleY(scale);
            imageView.invalidate();
            invalidate();
        }
        return result;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){
        super.onInitializeAccessibilityNodeInfo(info);
        if (sticker == null)
            return;
        String emoji = null;
        for (int a = 0; a < sticker.attributes.size(); a++) {
            Document.DocumentAttribute attribute = sticker.attributes.get(a);
            if (attribute.isSticker()) {
                emoji = attribute.alt != null && attribute.alt.length() > 0 ? attribute.alt : null;
            }
        }
        if (emoji != null)
            info.setText(emoji + " " + LocaleController.getString("AttachSticker", R.string.AttachSticker));
        else
            info.setText(LocaleController.getString("AttachSticker", R.string.AttachSticker));
        info.setEnabled(true);
    }
}

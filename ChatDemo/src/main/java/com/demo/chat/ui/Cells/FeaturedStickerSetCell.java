package com.demo.chat.ui.Cells;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.demo.chat.R;
import com.demo.chat.controller.FileLoader;
import com.demo.chat.controller.LocaleController;
import com.demo.chat.controller.MediaDataController;
import com.demo.chat.controller.UserConfig;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.ImageLocation;
import com.demo.chat.model.MessageObject;
import com.demo.chat.model.small.Document;
import com.demo.chat.model.small.Media;
import com.demo.chat.model.small.PhotoSize;
import com.demo.chat.model.sticker.StickerSetCovered;
import com.demo.chat.theme.Theme;
import com.demo.chat.ui.Components.BackupImageView;
import com.demo.chat.ui.Components.LayoutHelper;
import com.demo.chat.ui.Components.ProgressButton;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class FeaturedStickerSetCell extends FrameLayout {

    private TextView textView;
    private TextView valueTextView;
    private BackupImageView imageView;
    private ProgressButton addButton;
    private ImageView checkImage;
    private boolean needDivider;
    private StickerSetCovered stickersSet;
    private AnimatorSet currentAnimation;
    private boolean wasLayout;

    private boolean isInstalled;

    private int currentAccount = UserConfig.selectedAccount;

    public FeaturedStickerSetCell(Context context) {
        super(context);

        textView = new TextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, LocaleController.isRTL ? 22 : 71, 10, LocaleController.isRTL ? 71 : 22, 0));

        valueTextView = new TextView(context);
        valueTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        valueTextView.setLines(1);
        valueTextView.setMaxLines(1);
        valueTextView.setSingleLine(true);
        valueTextView.setEllipsize(TextUtils.TruncateAt.END);
        valueTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        addView(valueTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, LocaleController.isRTL ? 100 : 71, 35, LocaleController.isRTL ? 71 : 100, 0));

        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        imageView.setLayerNum(1);
        addView(imageView, LayoutHelper.createFrame(48, 48, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 0 : 12, 8, LocaleController.isRTL ? 12 : 0, 0));

        addButton = new ProgressButton(context);
        addButton.setText(LocaleController.getString("Add", R.string.Add));
        addButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        addButton.setProgressColor(Theme.getColor(Theme.key_featuredStickers_buttonProgress));
        addButton.setBackgroundRoundRect(Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed));
        addView(addButton, LayoutHelper.createFrameRelatively(LayoutHelper.WRAP_CONTENT, 28, Gravity.TOP | Gravity.END, 0, 18, 14, 0));

        checkImage = new ImageView(context);
        checkImage.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_featuredStickers_addedIcon), PorterDuff.Mode.MULTIPLY));
        checkImage.setImageResource(R.drawable.sticker_added);
        addView(checkImage, LayoutHelper.createFrame(19, 14));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));

        measureChildWithMargins(textView, widthMeasureSpec, addButton.getMeasuredWidth(), heightMeasureSpec, 0);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int l = addButton.getLeft() + addButton.getMeasuredWidth() / 2 - checkImage.getMeasuredWidth() / 2;
        int t = addButton.getTop() + addButton.getMeasuredHeight() / 2 - checkImage.getMeasuredHeight() / 2;
        checkImage.layout(l, t, l + checkImage.getMeasuredWidth(), t + checkImage.getMeasuredHeight());
        wasLayout = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        wasLayout = false;
    }

    public void setStickersSet(StickerSetCovered set, boolean divider, boolean unread) {
        boolean sameSet = set == stickersSet && wasLayout;
        needDivider = divider;
        stickersSet = set;
        setWillNotDraw(!needDivider);

        textView.setText(stickersSet.set.title);
        if (unread) {
            Drawable drawable = new Drawable() {

                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

                @Override
                public void draw(Canvas canvas) {
                    paint.setColor(0xff44a8ea);
                    canvas.drawCircle(AndroidUtilities.dp(4), AndroidUtilities.dp(5), AndroidUtilities.dp(3), paint);
                }

                @Override
                public void setAlpha(int alpha) {

                }

                @Override
                public void setColorFilter(ColorFilter colorFilter) {

                }

                @Override
                public int getOpacity() {
                    return PixelFormat.TRANSPARENT;
                }

                @Override
                public int getIntrinsicWidth() {
                    return AndroidUtilities.dp(12);
                }

                @Override
                public int getIntrinsicHeight() {
                    return AndroidUtilities.dp(8);
                }
            };
            textView.setCompoundDrawablesWithIntrinsicBounds(LocaleController.isRTL ? null : drawable, null, LocaleController.isRTL ? drawable : null, null);
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        valueTextView.setText(LocaleController.formatPluralString("Stickers", set.set.count));

        Document sticker;
        if (set.cover != null) {
            sticker = set.cover;
        } else if (!set.covers.isEmpty()) {
            sticker = set.covers.get(0);
        } else {
            sticker = null;
        }
        if (sticker != null) {
            Media object;
            if (set.set.thumb instanceof PhotoSize) {
                object = set.set.thumb;
            } else {
                object = sticker;
            }
            ImageLocation imageLocation;

            if (object instanceof Document) {
                PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(sticker.thumbs, 90);
                imageLocation = ImageLocation.getForDocument(thumb, sticker);
            } else {
                PhotoSize thumb = (PhotoSize) object;
                imageLocation = ImageLocation.getForSticker(thumb, sticker);
            }

            if (object instanceof Document && MessageObject.isAnimatedStickerDocument(sticker, true)) {
                imageView.setImage(ImageLocation.getForDocument(sticker), "50_50", imageLocation, null, 0, set);
            } else if (imageLocation != null && imageLocation.imageType == FileLoader.IMAGE_TYPE_LOTTIE) {
                imageView.setImage(imageLocation, "50_50", "tgs", null, set);
            } else {
                imageView.setImage(imageLocation, "50_50", "webp", null, set);
            }
        } else {
            imageView.setImage(null, null, "webp", null, set);
        }

        if (sameSet) {
            boolean wasInstalled = isInstalled;
            if (isInstalled = MediaDataController.getInstance(currentAccount).isStickerPackInstalled(set.set.id)) {
                if (!wasInstalled) {
                    checkImage.setVisibility(VISIBLE);
                    addButton.setClickable(false);
                    if (currentAnimation != null) {
                        currentAnimation.cancel();
                    }
                    currentAnimation = new AnimatorSet();
                    currentAnimation.setDuration(200);
                    currentAnimation.playTogether(ObjectAnimator.ofFloat(addButton, "alpha", 1.0f, 0.0f),
                            ObjectAnimator.ofFloat(addButton, "scaleX", 1.0f, 0.01f),
                            ObjectAnimator.ofFloat(addButton, "scaleY", 1.0f, 0.01f),
                            ObjectAnimator.ofFloat(checkImage, "alpha", 0.0f, 1.0f),
                            ObjectAnimator.ofFloat(checkImage, "scaleX", 0.01f, 1.0f),
                            ObjectAnimator.ofFloat(checkImage, "scaleY", 0.01f, 1.0f));
                    currentAnimation.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            if (currentAnimation != null && currentAnimation.equals(animator)) {
                                addButton.setVisibility(INVISIBLE);
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {
                            if (currentAnimation != null && currentAnimation.equals(animator)) {
                                currentAnimation = null;
                            }
                        }
                    });
                    currentAnimation.start();
                }
            } else {
                if (wasInstalled) {
                    addButton.setVisibility(VISIBLE);
                    addButton.setClickable(true);
                    if (currentAnimation != null) {
                        currentAnimation.cancel();
                    }
                    currentAnimation = new AnimatorSet();
                    currentAnimation.setDuration(200);
                    currentAnimation.playTogether(ObjectAnimator.ofFloat(checkImage, "alpha", 1.0f, 0.0f),
                            ObjectAnimator.ofFloat(checkImage, "scaleX", 1.0f, 0.01f),
                            ObjectAnimator.ofFloat(checkImage, "scaleY", 1.0f, 0.01f),
                            ObjectAnimator.ofFloat(addButton, "alpha", 0.0f, 1.0f),
                            ObjectAnimator.ofFloat(addButton, "scaleX", 0.01f, 1.0f),
                            ObjectAnimator.ofFloat(addButton, "scaleY", 0.01f, 1.0f));
                    currentAnimation.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            if (currentAnimation != null && currentAnimation.equals(animator)) {
                                checkImage.setVisibility(INVISIBLE);
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {
                            if (currentAnimation != null && currentAnimation.equals(animator)) {
                                currentAnimation = null;
                            }
                        }
                    });
                    currentAnimation.start();
                }
            }
        } else {
            if (currentAnimation != null) {
                currentAnimation.cancel();
            }
            if (isInstalled = MediaDataController.getInstance(currentAccount).isStickerPackInstalled(set.set.id)) {
                addButton.setVisibility(INVISIBLE);
                addButton.setClickable(false);
                checkImage.setVisibility(VISIBLE);
                checkImage.setScaleX(1.0f);
                checkImage.setScaleY(1.0f);
                checkImage.setAlpha(1.0f);
            } else {
                addButton.setVisibility(VISIBLE);
                addButton.setClickable(true);
                checkImage.setVisibility(INVISIBLE);
                addButton.setScaleX(1.0f);
                addButton.setScaleY(1.0f);
                addButton.setAlpha(1.0f);
            }
        }
    }

    public StickerSetCovered getStickerSet() {
        return stickersSet;
    }

    public void setAddOnClickListener(View.OnClickListener onClickListener) {
        addButton.setOnClickListener(onClickListener);
    }

    public void setDrawProgress(boolean value, boolean animated) {
        addButton.setDrawProgress(value, animated);
    }

    public boolean isInstalled() {
        return isInstalled;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(0, getHeight() - 1, getWidth() - getPaddingRight(), getHeight() - 1, Theme.dividerPaint);
        }
    }
}

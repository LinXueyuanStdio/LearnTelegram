package com.demo.chat.ui.Components;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.demo.chat.controller.MediaController;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.ui.Components.Crop.CropRotationWheel;
import com.demo.chat.ui.Components.Crop.CropView;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class PhotoCropView extends FrameLayout {

    public interface PhotoCropViewDelegate {
        void onChange(boolean reset);
    }

    private PhotoCropViewDelegate delegate;
    private boolean showOnSetBitmap;

    private CropView cropView;
    private CropRotationWheel wheelView;

    public PhotoCropView(Context context) {
        super(context);

        cropView = new CropView(getContext());
        cropView.setListener(new CropView.CropViewListener() {
            @Override
            public void onChange(boolean reset) {
                if (delegate != null) {
                    delegate.onChange(reset);
                }
            }

            @Override
            public void onAspectLock(boolean enabled) {
                wheelView.setAspectLock(enabled);
            }
        });
        cropView.setBottomPadding(AndroidUtilities.dp(64));
        addView(cropView);

        wheelView = new CropRotationWheel(getContext());
        wheelView.setListener(new CropRotationWheel.RotationWheelListener() {
            @Override
            public void onStart() {
                cropView.onRotationBegan();
            }

            @Override
            public void onChange(float angle) {
                cropView.setRotation(angle);
                if (delegate != null) {
                    delegate.onChange(false);
                }
            }

            @Override
            public void onEnd(float angle) {
                cropView.onRotationEnded();
            }

            @Override
            public void aspectRatioPressed() {
                cropView.showAspectRatioDialog();
            }

            @Override
            public void rotate90Pressed() {
                rotate();
            }
        });
        addView(wheelView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER | Gravity.BOTTOM, 0, 0, 0, 0));
    }

    public void rotate() {
        if (wheelView != null) {
            wheelView.reset();
        }
        cropView.rotate90Degrees();
    }

    public void setBitmap(Bitmap bitmap, int rotation, boolean freeform, boolean update, PaintingOverlay paintingOverlay) {
        requestLayout();

        cropView.setBitmap(bitmap, rotation, freeform, update, paintingOverlay);

        if (showOnSetBitmap) {
            showOnSetBitmap = false;
            cropView.show();
        }

        wheelView.setFreeform(freeform);
        wheelView.reset();
        wheelView.setVisibility(freeform ? VISIBLE : INVISIBLE);
    }

    public boolean isReady() {
        return cropView.isReady();
    }

    public void reset() {
        wheelView.reset();
        cropView.reset();
    }

    public void onAppear() {
        cropView.willShow();
    }

    public void setAspectRatio(float ratio) {
        cropView.setAspectRatio(ratio);
    }

    public void hideBackView() {
        cropView.hideBackView();
    }

    public void showBackView() {
        cropView.showBackView();
    }

    public void setFreeform(boolean freeform) {
        cropView.setFreeform(freeform);
    }

    public void onAppeared() {
        if (cropView != null) {
            cropView.show();
        } else {
            showOnSetBitmap = true;
        }
    }

    public void onDisappear() {
        if (cropView != null) {
            cropView.hide();
        }
    }

    public float getRectX() {
        return cropView.getCropLeft() - AndroidUtilities.dp(14);
    }

    public float getRectY() {
        return cropView.getCropTop() - AndroidUtilities.dp(14) - (Build.VERSION.SDK_INT >= 21 ? AndroidUtilities.statusBarHeight : 0);
    }

    public float getRectSizeX() {
        return cropView.getCropWidth();
    }

    public float getRectSizeY() {
        return cropView.getCropHeight();
    }

    public Bitmap getBitmap(MediaController.MediaEditState editState) {
        return cropView.getResult(editState);
    }

    public void setDelegate(PhotoCropViewDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (cropView != null) {
            cropView.updateLayout();
        }
    }
}

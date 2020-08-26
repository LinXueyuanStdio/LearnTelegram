package com.demo.chat.ui.Components;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.WindowManager;

import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.ui.ActionBar.AdjustPanFrameLayout;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/26
 * @description null
 * @usage null
 */
public class SizeNotifierFrameLayoutPhoto extends AdjustPanFrameLayout {

    private android.graphics.Rect rect = new Rect();
    private int keyboardHeight;
    private SizeNotifierFrameLayoutPhotoDelegate delegate;
    private WindowManager windowManager;
    private boolean withoutWindow;
    private boolean useSmoothKeyboard;

    public interface SizeNotifierFrameLayoutPhotoDelegate {
        void onSizeChanged(int keyboardHeight, boolean isWidthGreater);
    }

    public SizeNotifierFrameLayoutPhoto(Context context, boolean smoothKeyboard) {
        super(context);
        useSmoothKeyboard = smoothKeyboard;
    }

    public void setDelegate(SizeNotifierFrameLayoutPhotoDelegate sizeNotifierFrameLayoutPhotoDelegate) {
        delegate = sizeNotifierFrameLayoutPhotoDelegate;
    }

    public void setWithoutWindow(boolean value) {
        withoutWindow = value;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        notifyHeightChanged();
    }

    public int getKeyboardHeight() {
        View rootView = getRootView();
        getWindowVisibleDisplayFrame(rect);
        if (withoutWindow) {
            int usableViewHeight = rootView.getHeight() - (rect.top != 0 ? AndroidUtilities.statusBarHeight : 0) - AndroidUtilities.getViewInset(rootView);
            return usableViewHeight - (rect.bottom - rect.top);
        } else {
            int usableViewHeight = rootView.getHeight() - AndroidUtilities.getViewInset(rootView);
            int top = rect.top;
            int size;
            if (useSmoothKeyboard) {
                size = Math.max(0, usableViewHeight - (rect.bottom - rect.top));
            } else {
                size = AndroidUtilities.displaySize.y - top - usableViewHeight;
            }
            if (size <= Math.max(AndroidUtilities.dp(10), AndroidUtilities.statusBarHeight)) {
                size = 0;
            }
            return size;
        }
    }

    public void notifyHeightChanged() {
        if (delegate != null) {
            keyboardHeight = getKeyboardHeight();
            final boolean isWidthGreater = AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y;
            post(() -> {
                if (delegate != null) {
                    delegate.onSizeChanged(keyboardHeight, isWidthGreater);
                }
            });
        }
    }
}

package com.demo.chat.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import com.demo.chat.R;
import com.demo.chat.controller.LocaleController;
import com.demo.chat.controller.UserConfig;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.model.Message;
import com.demo.chat.model.action.MessageObject;
import com.demo.chat.model.small.MessageEntity;
import com.demo.chat.model.small.MessageMedia;
import com.demo.chat.theme.Theme;
import com.demo.chat.ui.ActionBar.ActionBarLayout;
import com.demo.chat.ui.Components.BackgroundGradientDrawable;
import com.demo.chat.ui.Components.LayoutHelper;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/30
 * @description null
 * @usage null
 */
public class ThemePreviewMessagesCell extends LinearLayout {

    private final Runnable invalidateRunnable = this::invalidate;

    private BackgroundGradientDrawable.Disposable backgroundGradientDisposable;
    private BackgroundGradientDrawable.Disposable oldBackgroundGradientDisposable;

    private Drawable backgroundDrawable;
    private Drawable oldBackgroundDrawable;
    private ChatMessageCell[] cells = new ChatMessageCell[2];
    private Drawable shadowDrawable;
    private ActionBarLayout parentLayout;

    public ThemePreviewMessagesCell(Context context, ActionBarLayout layout, int type) {
        super(context);

        parentLayout = layout;

        setWillNotDraw(false);
        setOrientation(LinearLayout.VERTICAL);
        setPadding(0, AndroidUtilities.dp(11), 0, AndroidUtilities.dp(11));

        shadowDrawable = Theme.getThemedDrawable(context, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow);

        int date = (int) (System.currentTimeMillis() / 1000) - 60 * 60;
        Message message = new Message();
        if (type == 0) {
            message.message = LocaleController.getString("FontSizePreviewReply", R.string.FontSizePreviewReply);
        } else {
            message.message = LocaleController.getString("NewThemePreviewReply", R.string.NewThemePreviewReply);
        }
        message.date = date + 60;
        message.dialog_id = 1;
        message.flags = 259;
        message.from_id = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        message.id = 1;
        message.media = new MessageMedia();
        message.out = true;
        message.to_id = 0;
        MessageObject replyMessageObject = new MessageObject(UserConfig.selectedAccount, message, true);

        message = new Message();
        if (type == 0) {
            message.message = LocaleController.getString("FontSizePreviewLine2", R.string.FontSizePreviewLine2);
        } else {
            String text = LocaleController.getString("NewThemePreviewLine3", R.string.NewThemePreviewLine3);
            StringBuilder builder = new StringBuilder(text);
            int index1 = text.indexOf('*');
            int index2 = text.lastIndexOf('*');
            if (index1 != -1 && index2 != -1) {
                builder.replace(index2, index2 + 1, "");
                builder.replace(index1, index1 + 1, "");
                MessageEntity entityUrl = new MessageEntity();
                entityUrl.setTextUrl(true);
                entityUrl.offset = index1;
                entityUrl.length = index2 - index1 - 1;
                entityUrl.url = "https://www.baidu.com";
                message.entities.add(entityUrl);
            }
            message.message = builder.toString();
        }
        message.date = date + 960;
        message.dialog_id = 1;
        message.flags = 259;
        message.from_id = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        message.id = 1;
        message.media = new MessageMedia();
        message.out = true;
        message.to_id = 0;
        MessageObject message1 = new MessageObject(UserConfig.selectedAccount, message, true);
        message1.resetLayout();
        message1.eventId = 1;

        message = new Message();
        if (type == 0) {
            message.message = LocaleController.getString("FontSizePreviewLine1", R.string.FontSizePreviewLine1);
        } else {
            message.message = LocaleController.getString("NewThemePreviewLine1", R.string.NewThemePreviewLine1);
        }
        message.date = date + 60;
        message.dialog_id = 1;
        message.flags = 257 + 8;
        message.from_id = 1;
        message.id = 1;
        message.reply_to_msg_id = 5;
        message.media = new MessageMedia();
        message.out = true;
        message.to_id = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        MessageObject message2 = new MessageObject(UserConfig.selectedAccount, message, true);
        if (type == 0) {
            message2.customReplyName = LocaleController.getString("FontSizePreviewName", R.string.FontSizePreviewName);
        } else {
            message2.customReplyName = LocaleController.getString("NewThemePreviewName", R.string.NewThemePreviewName);
        }
        message2.eventId = 1;
        message2.resetLayout();
        message2.replyMessageObject = replyMessageObject;

        for (int a = 0; a < cells.length; a++) {
            cells[a] = new ChatMessageCell(context);
            cells[a].setDelegate(new ChatMessageCell.ChatMessageCellDelegate() {

            });
            cells[a].isChat = false;
            cells[a].setFullyDraw(true);
            cells[a].setMessageObject(a == 0 ? message2 : message1, null, false, false);
            addView(cells[a], LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
    }

    public ChatMessageCell[] getCells() {
        return cells;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        for (int a = 0; a < cells.length; a++) {
            cells[a].invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable newDrawable = Theme.getCachedWallpaperNonBlocking();
        if (newDrawable != backgroundDrawable && newDrawable != null) {
            if (Theme.isAnimatingColor()) {
                oldBackgroundDrawable = backgroundDrawable;
                oldBackgroundGradientDisposable = backgroundGradientDisposable;
            } else if (backgroundGradientDisposable != null) {
                backgroundGradientDisposable.dispose();
                backgroundGradientDisposable = null;
            }
            backgroundDrawable = newDrawable;
        }
        float themeAnimationValue = parentLayout.getThemeAnimationValue();
        for (int a = 0; a < 2; a++) {
            Drawable drawable = a == 0 ? oldBackgroundDrawable : backgroundDrawable;
            if (drawable == null) {
                continue;
            }
            if (a == 1 && oldBackgroundDrawable != null && parentLayout != null) {
                drawable.setAlpha((int) (255 * themeAnimationValue));
            } else {
                drawable.setAlpha(255);
            }
            if (drawable instanceof ColorDrawable || drawable instanceof GradientDrawable) {
                drawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
                if (drawable instanceof BackgroundGradientDrawable) {
                    final BackgroundGradientDrawable backgroundGradientDrawable = (BackgroundGradientDrawable) drawable;
                    backgroundGradientDisposable = backgroundGradientDrawable.drawExactBoundsSize(canvas, this);
                } else {
                    drawable.draw(canvas);
                }
            } else if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                if (bitmapDrawable.getTileModeX() == Shader.TileMode.REPEAT) {
                    canvas.save();
                    float scale = 2.0f / AndroidUtilities.density;
                    canvas.scale(scale, scale);
                    drawable.setBounds(0, 0, (int) Math.ceil(getMeasuredWidth() / scale), (int) Math.ceil(getMeasuredHeight() / scale));
                    drawable.draw(canvas);
                    canvas.restore();
                } else {
                    int viewHeight = getMeasuredHeight();
                    float scaleX = (float) getMeasuredWidth() / (float) drawable.getIntrinsicWidth();
                    float scaleY = (float) (viewHeight) / (float) drawable.getIntrinsicHeight();
                    float scale = scaleX < scaleY ? scaleY : scaleX;
                    int width = (int) Math.ceil(drawable.getIntrinsicWidth() * scale);
                    int height = (int) Math.ceil(drawable.getIntrinsicHeight() * scale);
                    int x = (getMeasuredWidth() - width) / 2;
                    int y = (viewHeight - height) / 2;
                    canvas.save();
                    canvas.clipRect(0, 0, width, getMeasuredHeight());
                    drawable.setBounds(x, y, x + width, y + height);
                    drawable.draw(canvas);
                    canvas.restore();
                }
            }
            if (a == 0 && oldBackgroundDrawable != null && themeAnimationValue >= 1.0f) {
                if (oldBackgroundGradientDisposable != null) {
                    oldBackgroundGradientDisposable.dispose();
                    oldBackgroundGradientDisposable = null;
                }
                oldBackgroundDrawable = null;
                invalidate();
            }
        }
        shadowDrawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
        shadowDrawable.draw(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (backgroundGradientDisposable != null) {
            backgroundGradientDisposable.dispose();
            backgroundGradientDisposable = null;
        }
        if (oldBackgroundGradientDisposable != null) {
            oldBackgroundGradientDisposable.dispose();
            oldBackgroundGradientDisposable = null;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
}
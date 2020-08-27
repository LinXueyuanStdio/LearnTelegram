package com.demo.chat.ui.Components;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.RectF;

import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.theme.Theme;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class RecordStatusDrawable extends StatusDrawable {

    private boolean isChat = false;
    private long lastUpdateTime = 0;
    private boolean started = false;
    private RectF rect = new RectF();
    private float progress;

    public void setIsChat(boolean value) {
        isChat = value;
    }

    private void update() {
        long newTime = System.currentTimeMillis();
        long dt = newTime - lastUpdateTime;
        lastUpdateTime = newTime;
        if (dt > 50) {
            dt = 50;
        }
        progress += dt / 800.0f;
        while (progress > 1.0f) {
            progress -= 1.0f;
        }
        invalidateSelf();
    }

    public void start() {
        lastUpdateTime = System.currentTimeMillis();
        started = true;
        invalidateSelf();
    }

    public void stop() {
        started = false;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.save();
        canvas.translate(0, getIntrinsicHeight() / 2 + AndroidUtilities.dp(isChat ? 1 : 2));
        for (int a = 0; a < 4; a++) {
            if (a == 0) {
                Theme.chat_statusRecordPaint.setAlpha((int) (255 * progress));
            } else if (a == 3) {
                Theme.chat_statusRecordPaint.setAlpha((int) (255 * (1.0f - progress)));
            } else {
                Theme.chat_statusRecordPaint.setAlpha(255);
            }
            float side = AndroidUtilities.dp(4) * a + AndroidUtilities.dp(4) * progress;
            rect.set(-side, -side, side, side);
            canvas.drawArc(rect, -15, 30, false, Theme.chat_statusRecordPaint);
        }
        canvas.restore();
        if (started) {
            update();
        }
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public int getIntrinsicWidth() {
        return AndroidUtilities.dp(18);
    }

    @Override
    public int getIntrinsicHeight() {
        return AndroidUtilities.dp(14);
    }
}


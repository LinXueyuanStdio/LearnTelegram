package com.demo.chat.ui.Components;

import android.graphics.Canvas;
import android.graphics.ColorFilter;

import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.theme.Theme;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class RoundStatusDrawable extends StatusDrawable {

    private boolean isChat = false;
    private long lastUpdateTime = 0;
    private boolean started = false;
    private float progress;
    private int progressDirection = 1;

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
        progress += progressDirection * dt / 400.0f;
        if (progressDirection > 0 && progress >= 1.0f) {
            progressDirection = -1;
            progress = 1;
        } else if (progressDirection < 0 && progress <= 0) {
            progressDirection = 1;
            progress = 0;
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
        Theme.chat_statusPaint.setAlpha(55 + (int) (200 * progress));
        canvas.drawCircle(AndroidUtilities.dp(6), AndroidUtilities.dp(isChat ? 8 : 9), AndroidUtilities.dp(4), Theme.chat_statusPaint);
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
        return AndroidUtilities.dp(12);
    }

    @Override
    public int getIntrinsicHeight() {
        return AndroidUtilities.dp(10);
    }
}


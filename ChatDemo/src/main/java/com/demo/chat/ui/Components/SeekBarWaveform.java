package com.demo.chat.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.model.MessageObject;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class SeekBarWaveform {

    private static Paint paintInner;
    private static Paint paintOuter;
    private int thumbX = 0;
    private int thumbDX = 0;
    private float startX;
    private boolean startDraging = false;
    private boolean pressed = false;
    private int width;
    private int height;
    private SeekBar.SeekBarDelegate delegate;
    private byte[] waveformBytes;
    private MessageObject messageObject;
    private View parentView;
    private boolean selected;

    private int innerColor;
    private int outerColor;
    private int selectedColor;

    private float waveScaling = 1f;

    public SeekBarWaveform(Context context) {
        if (paintInner == null) {
            paintInner = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintInner.setStyle(Paint.Style.STROKE);
            paintOuter.setStyle(Paint.Style.STROKE);
            paintInner.setStrokeWidth(AndroidUtilities.dpf2(2));
            paintOuter.setStrokeWidth(AndroidUtilities.dpf2(2));
            paintInner.setStrokeCap(Paint.Cap.ROUND);
            paintOuter.setStrokeCap(Paint.Cap.ROUND);

        }
    }

    public void setDelegate(SeekBar.SeekBarDelegate seekBarDelegate) {
        delegate = seekBarDelegate;
    }

    public void setColors(int inner, int outer, int selected) {
        innerColor = inner;
        outerColor = outer;
        selectedColor = selected;
    }

    public void setWaveform(byte[] waveform) {
        waveformBytes = waveform;
    }

    public void setSelected(boolean value) {
        selected = value;
    }

    public void setMessageObject(MessageObject object) {
        messageObject = object;
    }

    public void setParentView(View view) {
        parentView = view;
    }

    public boolean isStartDraging() {
        return startDraging;
    }

    public boolean onTouch(int action, float x, float y) {
        if (action == MotionEvent.ACTION_DOWN) {
            if (0 <= x && x <= width && y >= 0 && y <= height) {
                startX = x;
                pressed = true;
                thumbDX = (int) (x - thumbX);
                startDraging = false;
                return true;
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (pressed) {
                if (action == MotionEvent.ACTION_UP && delegate != null) {
                    delegate.onSeekBarDrag((float) thumbX / (float) width);
                }
                pressed = false;
                return true;
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (pressed) {
                if (startDraging) {
                    thumbX = (int) (x - thumbDX);
                    if (thumbX < 0) {
                        thumbX = 0;
                    } else if (thumbX > width) {
                        thumbX = width;
                    }
                }
                if (startX != -1 && Math.abs(x - startX) > AndroidUtilities.getPixelsInCM(0.2f, true)) {
                    if (parentView != null && parentView.getParent() != null) {
                        parentView.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    startDraging = true;
                    startX = -1;
                }
                return true;
            }
        }
        return false;
    }

    public void setProgress(float progress) {
        thumbX = (int) Math.ceil(width * progress);
        if (thumbX < 0) {
            thumbX = 0;
        } else if (thumbX > width) {
            thumbX = width;
        }
    }

    public boolean isDragging() {
        return pressed;
    }

    public void setSize(int w, int h) {
        width = w;
        height = h;
    }

    public void draw(Canvas canvas) {
        if (waveformBytes == null || width == 0) {
            return;
        }
        float totalBarsCount = width / AndroidUtilities.dp(3);
        if (totalBarsCount <= 0.1f) {
            return;
        }
        byte value;
        int samplesCount = (waveformBytes.length * 8 / 5);
        float samplesPerBar = samplesCount / totalBarsCount;
        float barCounter = 0;
        int nextBarNum = 0;

        paintInner.setColor(messageObject != null && !messageObject.isOutOwner() && messageObject.isContentUnread() && thumbX == 0 ? outerColor : (selected ? selectedColor : innerColor));
        paintOuter.setColor(outerColor);

        int y = (height - AndroidUtilities.dp(14)) / 2;
        int barNum = 0;
        int lastBarNum;
        int drawBarCount;

        for (int a = 0; a < samplesCount; a++) {
            if (a != nextBarNum) {
                continue;
            }
            drawBarCount = 0;
            lastBarNum = nextBarNum;
            while (lastBarNum == nextBarNum) {
                barCounter += samplesPerBar;
                nextBarNum = (int) barCounter;
                drawBarCount++;
            }

            int bitPointer = a * 5;
            int byteNum = bitPointer / 8;
            int byteBitOffset = bitPointer - byteNum * 8;
            int currentByteCount = 8 - byteBitOffset;
            int nextByteRest = 5 - currentByteCount;
            value = (byte) ((waveformBytes[byteNum] >> byteBitOffset) & ((2 << (Math.min(5, currentByteCount) - 1)) - 1));
            if (nextByteRest > 0 && byteNum + 1 < waveformBytes.length) {
                value <<= nextByteRest;
                value |= waveformBytes[byteNum + 1] & ((2 << (nextByteRest - 1)) - 1);
            }

            for (int b = 0; b < drawBarCount; b++) {
                float x = barNum * AndroidUtilities.dpf2(3);
                float h = AndroidUtilities.dpf2(Math.max(0, 7 * value / 31.0f));

                if (x < thumbX && x + AndroidUtilities.dp(2) < thumbX) {
                    drawLine(canvas,x, y, h, paintOuter);
                } else {
                    drawLine(canvas,x, y, h, paintInner);
                    if (x < thumbX) {
                        canvas.save();
                        canvas.clipRect(x - AndroidUtilities.dpf2(1), y, thumbX, y + AndroidUtilities.dp(14));
                        drawLine(canvas,x, y, h, paintOuter);
                        canvas.restore();
                    }
                }
                barNum++;
            }
        }
    }

    private void drawLine(Canvas canvas, float x, int y, float h, Paint paint) {
        h *= waveScaling;
        if (h == 0) {
            canvas.drawPoint(x + AndroidUtilities.dpf2(1), y + AndroidUtilities.dp(7), paint);
        } else {
            canvas.drawLine(x + AndroidUtilities.dpf2(1), y + AndroidUtilities.dp(7) - h, x + AndroidUtilities.dpf2(1), y + AndroidUtilities.dp(7) + h, paint);
        }
    }

    public void setWaveScaling(float waveScaling) {
        this.waveScaling = waveScaling;
    }
}

package com.demo.chat.ui.Components;

import android.content.Context;
import android.util.SparseIntArray;

import com.demo.chat.messager.AndroidUtilities;

import androidx.recyclerview.widget.GridLayoutManager;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class ExtendedGridLayoutManager extends GridLayoutManager {

    private final boolean lastRowFullWidth;

    private SparseIntArray itemSpans = new SparseIntArray();
    private SparseIntArray itemsToRow = new SparseIntArray();
    private int firstRowMax;
    private int rowsCount;
    private int calculatedWidth;

    public ExtendedGridLayoutManager(Context context, int spanCount) {
        this(context, spanCount, false);
    }

    public ExtendedGridLayoutManager(Context context, int spanCount, boolean lastRowFullWidth) {
        super(context, spanCount);
        this.lastRowFullWidth = lastRowFullWidth;
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }

    private void prepareLayout(float viewPortAvailableSize) {
        if (viewPortAvailableSize == 0) {
            viewPortAvailableSize = 100;
        }
        itemSpans.clear();
        itemsToRow.clear();
        rowsCount = 0;
        firstRowMax = 0;

        final int itemsCount = getFlowItemCount();
        if (itemsCount == 0) {
            return;
        }

        final int preferredRowSize = AndroidUtilities.dp(100);
        final int spanCount = getSpanCount();

        int spanLeft = spanCount;
        int currentItemsInRow = 0;
        int currentItemsSpanAmount = 0;
        for (int a = 0, N = itemsCount + (lastRowFullWidth ? 1 : 0); a < N; a++) {
            Size size = a < itemsCount ? sizeForItem(a) : null;
            int requiredSpan;
            boolean moveToNewRow;
            if (size == null) {
                moveToNewRow = currentItemsInRow != 0;
                requiredSpan = spanCount;
            } else {
                requiredSpan = Math.min(spanCount, (int) Math.floor(spanCount * (size.width / size.height * preferredRowSize / viewPortAvailableSize)));
                moveToNewRow = spanLeft<requiredSpan || requiredSpan > 33 && spanLeft < requiredSpan - 15;
            }
            if (moveToNewRow) {
                if (spanLeft != 0) {
                    int spanPerItem = spanLeft / currentItemsInRow;
                    for (int start = a - currentItemsInRow, b = start; b < start + currentItemsInRow; b++) {
                        if (b == start + currentItemsInRow - 1) {
                            itemSpans.put(b, itemSpans.get(b) + spanLeft);
                        } else {
                            itemSpans.put(b, itemSpans.get(b) + spanPerItem);
                        }
                        spanLeft -= spanPerItem;
                    }
                    itemsToRow.put(a - 1, rowsCount);
                }
                if (a == itemsCount) {
                    break;
                }
                rowsCount++;
                currentItemsSpanAmount = 0;
                currentItemsInRow = 0;
                spanLeft = spanCount;
            } else {
                if (spanLeft < requiredSpan) {
                    requiredSpan = spanLeft;
                }
            }
            if (rowsCount == 0) {
                firstRowMax = Math.max(firstRowMax, a);
            }
            if (a == itemsCount - 1 && !lastRowFullWidth) {
                itemsToRow.put(a, rowsCount);
            }
            currentItemsSpanAmount += requiredSpan;
            currentItemsInRow++;
            spanLeft -= requiredSpan;

            itemSpans.put(a, requiredSpan);
        }
        rowsCount++;
    }

    private Size sizeForItem(int i) {
        return fixSize(getSizeForItem(i));
    }

    protected Size fixSize(Size size) {
        if (size == null) {
            return null;
        }
        if (size.width == 0) {
            size.width = 100;
        }
        if (size.height == 0) {
            size.height = 100;
        }
        float aspect = size.width / size.height;
        if (aspect > 4.0f || aspect < 0.2f) {
            size.height = size.width = Math.max(size.width, size.height);
        }
        return size;
    }

    protected Size getSizeForItem(int i) {
        return new Size(100, 100);
    }

    private void checkLayout() {
        if (itemSpans.size() != getFlowItemCount() || calculatedWidth != getWidth()) {
            calculatedWidth = getWidth();
            prepareLayout(getWidth());
        }
    }

    public int getSpanSizeForItem(int i) {
        checkLayout();
        return itemSpans.get(i);
    }

    public int getRowsCount(int width) {
        if (rowsCount == 0) {
            prepareLayout(width);
        }
        return rowsCount;
    }

    public boolean isLastInRow(int i) {
        checkLayout();
        return itemsToRow.get(i, Integer.MAX_VALUE) != Integer.MAX_VALUE;
    }

    public boolean isFirstRow(int i) {
        checkLayout();
        return i <= firstRowMax;
    }

    protected int getFlowItemCount() {
        return getItemCount();
    }
}

package com.demo.chat.ui.Cells;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.demo.chat.R;
import com.demo.chat.controller.LocaleController;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.theme.Theme;
import com.demo.chat.ui.Components.LayoutHelper;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class StickerSetGroupInfoCell extends LinearLayout {

    private TextView addButton;
    private boolean isLast;

    public StickerSetGroupInfoCell(Context context) {
        super(context);
        setOrientation(VERTICAL);

        TextView infoTextView = new TextView(context);
        infoTextView.setTextColor(Theme.getColor(Theme.key_chat_emojiPanelTrendingDescription));
        infoTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        infoTextView.setText(LocaleController.getString("GroupStickersInfo", R.string.GroupStickersInfo));
        addView(infoTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 17, 4, 17, 0));

        addButton = new TextView(context);
        addButton.setPadding(AndroidUtilities.dp(17), 0, AndroidUtilities.dp(17), 0);
        addButton.setGravity(Gravity.CENTER);
        addButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        addButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        addButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        addButton.setBackgroundDrawable(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(4), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        addButton.setText(LocaleController.getString("ChooseStickerSet", R.string.ChooseStickerSet).toUpperCase());
        addView(addButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 28, Gravity.TOP | Gravity.LEFT, 17, 10, 14, 8));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), heightMeasureSpec);
        if (isLast) {
            View parent = (View) getParent();
            if (parent != null) {
                int height = parent.getMeasuredHeight() - parent.getPaddingBottom() - parent.getPaddingTop() - AndroidUtilities.dp(24);
                if (getMeasuredHeight() < height) {
                    setMeasuredDimension(getMeasuredWidth(), height);
                }
            }
        }
    }

    public void setAddOnClickListener(View.OnClickListener onClickListener) {
        addButton.setOnClickListener(onClickListener);
    }

    public void setIsLast(boolean last) {
        isLast = last;
        requestLayout();
    }
}

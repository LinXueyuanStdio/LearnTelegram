package com.demo.chat.ui.Adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.demo.chat.controller.MediaDataController;
import com.demo.chat.controller.UserConfig;
import com.demo.chat.model.action.MessageObject;
import com.demo.chat.ui.Cells.DialogCell;
import com.demo.chat.ui.Cells.LoadingCell;
import com.demo.chat.ui.Components.RecyclerListView;

import java.util.ArrayList;

import androidx.recyclerview.widget.RecyclerView;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class MessagesSearchAdapter extends RecyclerListView.SelectionAdapter {

    private Context mContext;
    private ArrayList<MessageObject> searchResultMessages = new ArrayList<>();

    private int currentAccount = UserConfig.selectedAccount;

    public MessagesSearchAdapter(Context context) {
        mContext = context;
    }

    @Override
    public void notifyDataSetChanged() {
        searchResultMessages = MediaDataController.getInstance(currentAccount).getFoundMessageObjects();
        super.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return searchResultMessages.size();
    }

    public Object getItem(int i) {
        if (i < 0 || i >= searchResultMessages.size()) {
            return null;
        }
        return searchResultMessages.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        return holder.getItemViewType() == 0;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        switch (viewType) {
            case 0:
                view = new DialogCell(mContext, false, true);
                break;
            case 1:
                view = new LoadingCell(mContext);
                break;
        }
        view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
        return new RecyclerListView.Holder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == 0) {
            DialogCell cell = (DialogCell) holder.itemView;
            cell.useSeparator = true;
            MessageObject messageObject = (MessageObject) getItem(position);
            cell.setDialog(messageObject.getDialogId(), messageObject, messageObject.messageOwner.date, true);
        }
    }

    @Override
    public int getItemViewType(int i) {
        if (i < searchResultMessages.size()) {
            return 0;
        }
        return 1;
    }
}


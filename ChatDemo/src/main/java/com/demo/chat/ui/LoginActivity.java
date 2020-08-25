package com.demo.chat.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.demo.chat.controller.UserConfig;
import com.demo.chat.ui.ActionBar.BaseFragment;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class LoginActivity extends BaseFragment {
    @Override
    public View createView(Context context) {
        LinearLayout con = new LinearLayout(context);

        TextView textView = new TextView(context);
        textView.setText("Login");

        Button button = new Button(context);
        button.setText("Chat");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putInt("user_id", UserConfig.getInstance(currentAccount).getClientUserId());
                presentFragment(new ChatActivity(args));
            }
        });

        con.addView(textView);
        con.addView(button);

        return con;
    }
}

package com.demo.chat.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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
                args.putInt("chat_id", 1);
                presentFragment(new ChatActivity(args));
            }
        });
        Button button2 = new Button(context);
        button2.setText("Theme");
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC));
            }
        });

        Button button3 = new Button(context);
        button3.setText("WallpapersList");
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presentFragment(new WallpapersListActivity(WallpapersListActivity.TYPE_ALL));
            }
        });

        con.addView(textView);
        con.addView(button);
        con.addView(button2);
        con.addView(button3);

        return con;
    }
}

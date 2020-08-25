package com.demo.chat.model;

import android.text.TextUtils;

import com.demo.chat.R;
import com.demo.chat.controller.LocaleController;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class UserObject {

    public static boolean isDeleted(User user) {
        return user == null || user instanceof TL_userDeleted_old2 || user instanceof TL_userEmpty || user.deleted;
    }

    public static boolean isContact(User user) {
        return user != null && (user instanceof TL_userContact_old2 || user.contact || user.mutual_contact);
    }

    public static boolean isUserSelf(User user) {
        return user != null && (user instanceof TL_userSelf_old3 || user.self);
    }

    public static String getUserName(User user) {
        if (user == null || isDeleted(user)) {
            return LocaleController.getString("HiddenName", R.string.HiddenName);
        }
        String name = ContactsController.formatName(user.first_name, user.last_name);
        return name.length() != 0 || TextUtils.isEmpty(user.phone) ? name : PhoneFormat.getInstance().format("+" + user.phone);
    }

    public static String getFirstName(User user) {
        return getFirstName(user, true);
    }

    public static String getFirstName(User user, boolean allowShort) {
        if (user == null || isDeleted(user)) {
            return "DELETED";
        }
        String name = user.first_name;
        if (TextUtils.isEmpty(name)) {
            name = user.last_name;
        } else if (!allowShort && name.length() <= 2) {
            return ContactsController.formatName(user.first_name, user.last_name);
        }
        return !TextUtils.isEmpty(name) ? name : LocaleController.getString("HiddenName", R.string.HiddenName);
    }
}

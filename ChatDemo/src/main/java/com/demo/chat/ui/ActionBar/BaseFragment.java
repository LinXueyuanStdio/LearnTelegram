package com.demo.chat.ui.ActionBar;

import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;

import com.demo.chat.ApplicationLoader;
import com.demo.chat.controller.AccountInstance;
import com.demo.chat.controller.ConnectionsManager;
import com.demo.chat.controller.FileLoader;
import com.demo.chat.controller.MediaController;
import com.demo.chat.controller.MediaDataController;
import com.demo.chat.controller.MessagesController;
import com.demo.chat.controller.MessagesStorage;
import com.demo.chat.controller.NotificationsController;
import com.demo.chat.controller.SendMessagesHelper;
import com.demo.chat.controller.UserConfig;
import com.demo.chat.messager.FileLog;
import com.demo.chat.messager.NotificationCenter;
import com.demo.chat.theme.Theme;
import com.demo.chat.theme.ThemeDescription;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 * 介绍：
 *     传说中拳打官方Activity脚踢官方Fragment的神之class
 *     因为它 extends nothing
 *     比 ChatActivity extends BaseFragment 更为蛋疼
 *     这个类，有毒
 * 职责：
 *     封装fragmentView
 *     规定了fragmentView的生命周期
 *     控制Dialog的显示
 *     支持BaseFragment的嵌套
 *     支持和官方Activity通信
 *
 * 生命周期：
 * <init>()
 * |
 * | presentFragment(this)
 * |
 *  ->  onFragmentCreate
 *      |
 *      onResume
 *      |
 *      onPause ---  finishFragment
 *      |
 *      onFragmentDestroy
 */
public class BaseFragment {

    private boolean isFinished;
    private boolean finishing;
    protected Dialog visibleDialog;
    protected int currentAccount = UserConfig.selectedAccount;

    protected View fragmentView;//子类的 View，将这个 View 添加到 parentLayout (ActionBarLayout)
    protected ActionBarLayout parentLayout;//ActionBarLayout 是所有 Fragment 的容器
    protected ActionBar actionBar;
    protected boolean inPreviewMode;
    /**
     * 类uid，本质是一个单调递增的计数器 [0,+∞)
     *
     * 每实例化一个BaseFragment类，下一个实例化类的uid自动+1
     */
    protected int classGuid;
    protected Bundle arguments;
    protected boolean hasOwnBackground = false;
    protected boolean isPaused = true;

    public BaseFragment() {
        classGuid = ConnectionsManager.generateClassGuid();
    }

    public BaseFragment(Bundle args) {
        arguments = args;
        classGuid = ConnectionsManager.generateClassGuid();
    }

    //region 工具方法，不要重写，只管调用
    //region getter
    public Activity getParentActivity() {
        if (parentLayout != null) {
            return parentLayout.parentActivity;
        }
        return null;
    }

    public ActionBarLayout getParentLayout() {
        return parentLayout;
    }

    public ActionBar getActionBar() {
        return actionBar;
    }

    public View getFragmentView() {
        return fragmentView;
    }

    public Bundle getArguments() {
        return arguments;
    }

    public int getCurrentAccount() {
        return currentAccount;
    }

    public int getClassGuid() {
        return classGuid;
    }
    //endregion

    //region setter
    public void setCurrentAccount(int account) {
        if (fragmentView != null) {
            throw new IllegalStateException("trying to set current account when fragment UI already created");
        }
        currentAccount = account;
    }

    /**
     * 绑定到容器
     * @param layout ActionBarLayout
     */
    protected void setParentLayout(ActionBarLayout layout) {
        if (parentLayout != layout) {
            parentLayout = layout;
            if (fragmentView != null) {
                ViewGroup parent = (ViewGroup) fragmentView.getParent();
                if (parent != null) {
                    try {
                        onRemoveFromParent();
                        parent.removeViewInLayout(fragmentView);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
                if (parentLayout != null && parentLayout.getContext() != fragmentView.getContext()) {
                    fragmentView = null;
                }
            }
            if (actionBar != null) {
                boolean differentParent = parentLayout != null && parentLayout.getContext() != actionBar.getContext();
                if (actionBar.shouldAddToContainer() || differentParent) {
                    ViewGroup parent = (ViewGroup) actionBar.getParent();
                    if (parent != null) {
                        try {
                            parent.removeViewInLayout(actionBar);
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }
                }
                if (differentParent) {
                    actionBar = null;
                }
            }
            if (parentLayout != null && actionBar == null) {
                actionBar = createActionBar(parentLayout.getContext());
                actionBar.parentFragment = this;
            }
        }
    }

    protected void setParentActivityTitle(CharSequence title) {
        Activity activity = getParentActivity();
        if (activity != null) {
            activity.setTitle(title);
        }
    }
    //endregion

    //region 子 Fragment 的控制
    public void movePreviewFragment(float dy) {
        parentLayout.movePreviewFragment(dy);
    }

    public void finishPreviewFragment() {
        parentLayout.finishPreviewFragment();
    }

    public BaseFragment getFragmentForAlert(int offset) {
        if (parentLayout == null || parentLayout.fragmentsStack.size() <= 1 + offset) {
            return this;
        }
        return parentLayout.fragmentsStack.get(parentLayout.fragmentsStack.size() - 2 - offset);
    }

    /**
     * 需要绑定到父Fragment的容器，才能往容器里添加自己的 ContentView
     * @param fragment 父Fragment
     */
    public void setParentFragment(BaseFragment fragment) {
        setParentLayout(fragment.parentLayout);
        fragmentView = createView(parentLayout.getContext());
    }

    /**
     * 显示一个预览模式的子Fragment
     * @param fragment 预览Fragment
     * @return 是否显示成功
     */
    public boolean presentFragmentAsPreview(BaseFragment fragment) {
        return parentLayout != null && parentLayout.presentFragmentAsPreview(fragment);
    }

    /**
     * 显示一个子Fragment，同时播放动画、入栈
     * 用法 presentFragment(new XXActivity());
     * @param fragment 子Fragment
     * @return 是否显示成功
     */
    public boolean presentFragment(BaseFragment fragment) {
        return parentLayout != null && parentLayout.presentFragment(fragment);
    }

    public boolean presentFragment(BaseFragment fragment, boolean removeLast) {
        return parentLayout != null && parentLayout.presentFragment(fragment, removeLast);
    }

    public boolean presentFragment(BaseFragment fragment, boolean removeLast, boolean forceWithoutAnimation) {
        return parentLayout != null && parentLayout.presentFragment(fragment, removeLast, forceWithoutAnimation, true, false);
    }
    //endregion

    /**
     * 给ActionBarLayout用的，不要重写
     */
    protected void clearViews() {
        if (fragmentView != null) {
            ViewGroup parent = (ViewGroup) fragmentView.getParent();
            if (parent != null) {
                try {
                    onRemoveFromParent();
                    parent.removeViewInLayout(fragmentView);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            fragmentView = null;
        }
        if (actionBar != null) {
            ViewGroup parent = (ViewGroup) actionBar.getParent();
            if (parent != null) {
                try {
                    parent.removeViewInLayout(actionBar);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            actionBar = null;
        }
        parentLayout = null;
    }
    //endregion

    //region 功能配置
    /**
     * 是否需要侧滑返回
     * @return true：将侧滑返回
     */
    public boolean isSwipeBackEnabled(MotionEvent event) {
        return true;
    }

    /**
     * 是否需要延迟播放打开动画
     * @return true：将延迟播放打开动画
     */
    public boolean needDelayOpenAnimation() {
        return false;
    }

    /**
     * 是否需要在显示前隐藏软键盘
     * @return true：将在显示前隐藏软键盘
     */
    protected boolean hideKeyboardOnShow() {
        return true;
    }
    //endregion

    /**
     * 需要重写这个方法提供View，以装载进ActionBarLayout
     * @param context 上下文
     * @return View
     */
    public View createView(Context context) {
        return null;
    }

    /**
     * 可以重写这个方法提供 ActionBar
     *
     * 注意：ActionBar是自定义的一个View，不是系统提供的 ActionBar
     *      这个 View 和 BaseFragment 的生命周期绑定在一起，可以优雅地控制其展示
     * @param context 上下文
     * @return ActionBar
     */
    protected ActionBar createActionBar(Context context) {
        ActionBar actionBar = new ActionBar(context);
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarActionModeDefaultSelector), true);
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false);
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarActionModeDefaultIcon), true);
        if (inPreviewMode) {
            actionBar.setOccupyStatusBar(false);
        }
        return actionBar;
    }

    /**
     * 预览模式（这个方法只有 ChatActivity 用到）
     * @param value true：处于预览模式，需要重写此方法设置预览模式的UI
     */
    protected void setInPreviewMode(boolean value) {
        inPreviewMode = value;
        if (actionBar != null) {
            if (inPreviewMode) {
                actionBar.setOccupyStatusBar(false);
            } else {
                actionBar.setOccupyStatusBar(Build.VERSION.SDK_INT >= 21);
            }
        }
    }

    //region 生命周期

    /**
     * fragmentLayout被从ActionBarLayout中移除
     */
    protected void onRemoveFromParent() {

    }

    /**
     * 完成、关闭、移除本Fragment
     */
    public void finishFragment() {
        finishFragment(true);
    }

    public void finishFragment(boolean animated) {
        if (isFinished || parentLayout == null) {
            return;
        }
        finishing = true;
        parentLayout.closeLastFragment(animated);
    }

    public void removeSelfFromStack() {
        if (isFinished || parentLayout == null) {
            return;
        }
        parentLayout.removeFragmentFromStack(this);
    }

    protected boolean isFinishing() {
        return finishing;
    }

    public boolean onFragmentCreate() {
        return true;
    }

    public void onFragmentDestroy() {
        getConnectionsManager().cancelRequestsForGuid(classGuid);
        getMessagesStorage().cancelTasksForGuid(classGuid);
        isFinished = true;
        if (actionBar != null) {
            actionBar.setEnabled(false);
        }
    }

    public void onResume() {
        isPaused = false;
    }

    public void onPause() {
        if (actionBar != null) {
            actionBar.onPause();
        }
        isPaused = true;
        try {
            if (visibleDialog != null && visibleDialog.isShowing() && dismissDialogOnPause(visibleDialog)) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
    //endregion

    public void onConfigurationChanged(android.content.res.Configuration newConfig) {

    }

    public void onLowMemory() {

    }

    public ArrayList<ThemeDescription> getThemeDescriptions() {
        return new ArrayList<>();
    }

    public boolean extendActionMode(Menu menu) {
        return false;
    }

    public boolean onBackPressed() {
        return true;
    }

    //region 向其他页面发起请求并获得返回值
    public void startActivityForResult(final Intent intent, final int requestCode) {
        if (parentLayout != null) {
            parentLayout.startActivityForResult(intent, requestCode);
        }
    }

    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {

    }

    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {

    }
    //endregion

    //region 管理参数
    public void saveSelfArgs(Bundle args) {

    }

    public void restoreSelfArgs(Bundle args) {

    }
    //endregion

    //region 侧滑返回
    public boolean canBeginSlide() {
        return true;
    }

    public void onBeginSlide() {
        try {
            if (visibleDialog != null && visibleDialog.isShowing()) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        if (actionBar != null) {
            actionBar.onPause();
        }
    }

    protected void onTransitionAnimationProgress(boolean isOpen, float progress) {

    }

    protected void onTransitionAnimationStart(boolean isOpen, boolean backward) {

    }

    protected void onTransitionAnimationEnd(boolean isOpen, boolean backward) {

    }

    protected void onBecomeFullyVisible() {
        //兼容辅助模式
        AccessibilityManager mgr = (AccessibilityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (mgr.isEnabled()) {
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                String title = actionBar.getTitle();
                if (!TextUtils.isEmpty(title)) {
                    setParentActivityTitle(title);
                }
            }
        }
    }

    protected void onBecomeFullyHidden() {

    }

    protected AnimatorSet onCustomTransitionAnimation(boolean isOpen, final Runnable callback) {
        return null;
    }
    //endregion

    //region 对话框管理 Dialog
    public Dialog showDialog(Dialog dialog) {
        return showDialog(dialog, false, null);
    }

    public Dialog showDialog(Dialog dialog, Dialog.OnDismissListener onDismissListener) {
        return showDialog(dialog, false, onDismissListener);
    }

    public Dialog showDialog(Dialog dialog, boolean allowInTransition, final Dialog.OnDismissListener onDismissListener) {
        if (dialog == null || parentLayout == null || parentLayout.animationInProgress || parentLayout.startedTracking || !allowInTransition && parentLayout.checkTransitionAnimation()) {
            return null;
        }
        try {
            if (visibleDialog != null) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        try {
            visibleDialog = dialog;
            visibleDialog.setCanceledOnTouchOutside(true);
            visibleDialog.setOnDismissListener(dialog1 -> {
                if (onDismissListener != null) {
                    onDismissListener.onDismiss(dialog1);
                }
                onDialogDismiss((Dialog) dialog1);
                if (dialog1 == visibleDialog) {
                    visibleDialog = null;
                }
            });
            visibleDialog.show();
            return visibleDialog;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    public void dismissCurrentDialog() {
        if (visibleDialog == null) {
            return;
        }
        try {
            visibleDialog.dismiss();
            visibleDialog = null;
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public boolean dismissDialogOnPause(Dialog dialog) {
        return true;
    }

    protected void onDialogDismiss(Dialog dialog) {

    }

    protected void onPanTranslationUpdate(int y) {

    }

    protected void onPanTransitionStart() {

    }

    protected void onPanTransitionEnd() {

    }

    public int getCurrentPanTranslationY() {
        return parentLayout != null ? parentLayout.getCurrentPanTranslationY() : 0;
    }

    public Dialog getVisibleDialog() {
        return visibleDialog;
    }

    public void setVisibleDialog(Dialog dialog) {
        visibleDialog = dialog;
    }
    //endregion

    //region 一些控制器。感觉直接使用单例来调用函数也是可以的，这里写成函数方便一点
    public AccountInstance getAccountInstance() {
        return AccountInstance.getInstance(currentAccount);
    }

    public MessagesController getMessagesController() {
        return getAccountInstance().getMessagesController();
    }
//
//    protected ContactsController getContactsController() {
//        return getAccountInstance().getContactsController();
//    }
//
    public MediaDataController getMediaDataController() {
        return getAccountInstance().getMediaDataController();
    }
//
//    public ConnectionsManager getConnectionsManager() {
//        return getAccountInstance().getConnectionsManager();
//    }
//
//    public LocationController getLocationController() {
//        return getAccountInstance().getLocationController();
//    }

    protected NotificationsController getNotificationsController() {
        return getAccountInstance().getNotificationsController();
    }

    public MessagesStorage getMessagesStorage() {
        return getAccountInstance().getMessagesStorage();
    }
//
    public SendMessagesHelper getSendMessagesHelper() {
        return getAccountInstance().getSendMessagesHelper();
    }

    public FileLoader getFileLoader() {
        return getAccountInstance().getFileLoader();
    }

//    protected SecretChatHelper getSecretChatHelper() {
//        return getAccountInstance().getSecretChatHelper();
//    }
//
//    protected DownloadController getDownloadController() {
//        return getAccountInstance().getDownloadController();
//    }

    public MediaController getMediaController() {
        return MediaController.getInstance();
    }

//    protected SharedPreferences getNotificationsSettings() {
//        return getAccountInstance().getNotificationsSettings();
//    }

    public NotificationCenter getNotificationCenter() {
        return getAccountInstance().getNotificationCenter();
    }

    public UserConfig getUserConfig() {
        return getAccountInstance().getUserConfig();
    }
    //endregion
}


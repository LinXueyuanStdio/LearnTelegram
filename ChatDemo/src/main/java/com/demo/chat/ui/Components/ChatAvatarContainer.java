package com.demo.chat.ui.Components;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.demo.chat.R;
import com.demo.chat.controller.ConnectionsManager;
import com.demo.chat.controller.LocaleController;
import com.demo.chat.controller.MediaDataController;
import com.demo.chat.controller.MessagesController;
import com.demo.chat.controller.UserConfig;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.FileLog;
import com.demo.chat.messager.ImageLocation;
import com.demo.chat.messager.NotificationCenter;
import com.demo.chat.model.Chat;
import com.demo.chat.model.User;
import com.demo.chat.model.UserObject;
import com.demo.chat.model.action.ChatObject;
import com.demo.chat.model.small.FileLocation;
import com.demo.chat.theme.Theme;
import com.demo.chat.ui.ActionBar.ActionBar;
import com.demo.chat.ui.ActionBar.SimpleTextView;
import com.demo.chat.ui.ChatActivity;
import com.demo.chat.ui.MediaActivity;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class ChatAvatarContainer extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

    private BackupImageView avatarImageView;
    private SimpleTextView titleTextView;
    private SimpleTextView subtitleTextView;
    private ImageView timeItem;
    private TimerDrawable timerDrawable;
    private ChatActivity parentFragment;
    private StatusDrawable[] statusDrawables = new StatusDrawable[5];
    private AvatarDrawable avatarDrawable = new AvatarDrawable();
    private int currentAccount = UserConfig.selectedAccount;
    private boolean occupyStatusBar = true;

    private boolean[] isOnline = new boolean[1];

    private int onlineCount = -1;
    private CharSequence lastSubtitle;
    private String lastSubtitleColorKey;

    private SharedMediaLayout.SharedMediaPreloader sharedMediaPreloader;

    public ChatAvatarContainer(Context context, ChatActivity chatActivity, boolean needTime) {
        super(context);
        parentFragment = chatActivity;

        if (parentFragment != null) {
            sharedMediaPreloader = new SharedMediaLayout.SharedMediaPreloader(chatActivity);
        }

        avatarImageView = new BackupImageView(context);
        avatarImageView.setRoundRadius(AndroidUtilities.dp(21));
        addView(avatarImageView);
        if (parentFragment != null && !parentFragment.isInScheduleMode()) {
            avatarImageView.setOnClickListener(v -> openProfile(true));
        }

        titleTextView = new SimpleTextView(context);
        titleTextView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultTitle));
        titleTextView.setTextSize(18);
        titleTextView.setGravity(Gravity.LEFT);
        titleTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleTextView.setLeftDrawableTopPadding(-AndroidUtilities.dp(1.3f));
        addView(titleTextView);

        subtitleTextView = new SimpleTextView(context);
        subtitleTextView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle));
        subtitleTextView.setTag(Theme.key_actionBarDefaultSubtitle);
        subtitleTextView.setTextSize(14);
        subtitleTextView.setGravity(Gravity.LEFT);
        addView(subtitleTextView);

        if (parentFragment != null && !parentFragment.isInScheduleMode()) {
            setOnClickListener(v -> openProfile(false));

            Chat chat = parentFragment.getCurrentChat();
            statusDrawables[0] = new TypingDotsDrawable();
            statusDrawables[1] = new RecordStatusDrawable();
            statusDrawables[2] = new SendingFileDrawable();
            statusDrawables[3] = new PlayingGameDrawable();
            statusDrawables[4] = new RoundStatusDrawable();
            for (int a = 0; a < statusDrawables.length; a++) {
                statusDrawables[a].setIsChat(chat != null);
            }
        }
    }

    private void openProfile(boolean byAvatar) {
        if (byAvatar && (AndroidUtilities.isTablet() || AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y || !avatarImageView.getImageReceiver().hasNotThumb())) {
            byAvatar = false;
        }
        User user = parentFragment.getCurrentUser();
        Chat chat = parentFragment.getCurrentChat();
        if (user != null) {
            Bundle args = new Bundle();
            if (UserObject.isUserSelf(user)) {
                args.putLong("dialog_id", parentFragment.getDialogId());
                int[] media = new int[MediaDataController.MEDIA_TYPES_COUNT];
                System.arraycopy(sharedMediaPreloader.getLastMediaCount(), 0, media, 0, media.length);
                MediaActivity fragment = new MediaActivity(args, media, sharedMediaPreloader.getSharedMediaData(), -1);
                fragment.setChatInfo(parentFragment.getCurrentChat());
                parentFragment.presentFragment(fragment);
            } else {
                args.putInt("user_id", user.id);
                args.putBoolean("reportSpam", parentFragment.hasReportSpam());
                if (timeItem != null) {
                    args.putLong("dialog_id", parentFragment.getDialogId());
                }
//                ProfileActivity fragment = new ProfileActivity(args, sharedMediaPreloader);
//                fragment.setUserInfo(parentFragment.getCurrentUserInfo());
//                fragment.setPlayProfileAnimation(byAvatar ? 2 : 1);
//                parentFragment.presentFragment(fragment);
            }
        } else if (chat != null) {
            Bundle args = new Bundle();
            args.putInt("chat_id", chat.id);
//            ProfileActivity fragment = new ProfileActivity(args, sharedMediaPreloader);
//            fragment.setChatInfo(parentFragment.getCurrentChatInfo());
//            fragment.setPlayProfileAnimation(byAvatar ? 2 : 1);
//            parentFragment.presentFragment(fragment);
        }
    }

    public void setOccupyStatusBar(boolean value) {
        occupyStatusBar = value;
    }

    public void setTitleColors(int title, int subtitle) {
        titleTextView.setTextColor(title);
        subtitleTextView.setTextColor(subtitle);
        subtitleTextView.setTag(subtitle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int availableWidth = width - AndroidUtilities.dp(54 + 16);
        avatarImageView.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(42), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(42), MeasureSpec.EXACTLY));
        titleTextView.measure(MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(24), MeasureSpec.AT_MOST));
        subtitleTextView.measure(MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.AT_MOST));
        if (timeItem != null) {
            timeItem.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(34), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(34), MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(width, MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int actionBarHeight = ActionBar.getCurrentActionBarHeight();
        int viewTop = (actionBarHeight - AndroidUtilities.dp(42)) / 2 + (Build.VERSION.SDK_INT >= 21 && occupyStatusBar ? AndroidUtilities.statusBarHeight : 0);
        avatarImageView.layout(AndroidUtilities.dp(8), viewTop, AndroidUtilities.dp(42 + 8), viewTop + AndroidUtilities.dp(42));
        if (subtitleTextView.getVisibility() == VISIBLE) {
            titleTextView.layout(AndroidUtilities.dp(8 + 54), viewTop + AndroidUtilities.dp(1.3f), AndroidUtilities.dp(8 + 54) + titleTextView.getMeasuredWidth(), viewTop + titleTextView.getTextHeight() + AndroidUtilities.dp(1.3f));
        } else {
            titleTextView.layout(AndroidUtilities.dp(8 + 54), viewTop + AndroidUtilities.dp(11), AndroidUtilities.dp(8 + 54) + titleTextView.getMeasuredWidth(), viewTop + titleTextView.getTextHeight() + AndroidUtilities.dp(11));
        }
        if (timeItem != null) {
            timeItem.layout(AndroidUtilities.dp(8 + 16), viewTop + AndroidUtilities.dp(15), AndroidUtilities.dp(8 + 16 + 34), viewTop + AndroidUtilities.dp(15 + 34));
        }
        subtitleTextView.layout(AndroidUtilities.dp(8 + 54), viewTop + AndroidUtilities.dp(24), AndroidUtilities.dp(8 + 54) + subtitleTextView.getMeasuredWidth(), viewTop + subtitleTextView.getTextHeight() + AndroidUtilities.dp(24));
    }

    public void showTimeItem() {
        if (timeItem == null) {
            return;
        }
        timeItem.setVisibility(VISIBLE);
    }

    public void hideTimeItem() {
        if (timeItem == null) {
            return;
        }
        timeItem.setVisibility(GONE);
    }

    public void setTime(int value) {
        if (timerDrawable == null) {
            return;
        }
        timerDrawable.setTime(value);
    }

    public void setTitleIcons(Drawable leftIcon, Drawable rightIcon) {
        titleTextView.setLeftDrawable(leftIcon);
        if (!(titleTextView.getRightDrawable() instanceof ScamDrawable)) {
            titleTextView.setRightDrawable(rightIcon);
        }
    }

    public void setTitle(CharSequence value) {
        setTitle(value, false);
    }

    public void setTitle(CharSequence value, boolean scam) {
        titleTextView.setText(value);
        if (scam) {
            if (!(titleTextView.getRightDrawable() instanceof ScamDrawable)) {
                ScamDrawable drawable = new ScamDrawable(11);
                drawable.setColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle));
                titleTextView.setRightDrawable(drawable);
            }
        } else if (titleTextView.getRightDrawable() instanceof ScamDrawable) {
            titleTextView.setRightDrawable(null);
        }
    }

    public void setSubtitle(CharSequence value) {
        if (lastSubtitle == null) {
            subtitleTextView.setText(value);
        } else {
            lastSubtitle = value;
        }
    }

    public ImageView getTimeItem() {
        return timeItem;
    }

    public SimpleTextView getTitleTextView() {
        return titleTextView;
    }

    public SimpleTextView getSubtitleTextView() {
        return subtitleTextView;
    }

    public void onDestroy() {
        if (sharedMediaPreloader != null) {
            sharedMediaPreloader.onDestroy(parentFragment);
        }
    }

    private void setTypingAnimation(boolean start) {
        if (start) {
            try {
                Integer type = MessagesController.getInstance(currentAccount).printingStringsTypes.get(parentFragment.getDialogId());
                subtitleTextView.setLeftDrawable(statusDrawables[type]);
                for (int a = 0; a < statusDrawables.length; a++) {
                    if (a == type) {
                        statusDrawables[a].start();
                    } else {
                        statusDrawables[a].stop();
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        } else {
            subtitleTextView.setLeftDrawable(null);
            for (int a = 0; a < statusDrawables.length; a++) {
                statusDrawables[a].stop();
            }
        }
    }

    public void updateSubtitle() {
        if (parentFragment == null) {
            return;
        }
        User user = parentFragment.getCurrentUser();
        if (UserObject.isUserSelf(user) || parentFragment.isInScheduleMode()) {
            if (subtitleTextView.getVisibility() != GONE) {
                subtitleTextView.setVisibility(GONE);
            }
            return;
        }
        Chat chat = parentFragment.getCurrentChat();
        CharSequence printString = MessagesController.getInstance(currentAccount).printingStrings.get(parentFragment.getDialogId());
        if (printString != null) {
            printString = TextUtils.replace(printString, new String[]{"..."}, new String[]{""});
        }
        CharSequence newSubtitle;
        boolean useOnlineColor = false;
        if (printString == null || printString.length() == 0 || ChatObject.isChannel(chat) && !chat.megagroup) {
            setTypingAnimation(false);
            if (chat != null) {
                Chat info = parentFragment.getCurrentChat();
                if (ChatObject.isChannel(chat)) {
                    if (info != null && info.participants_count != 0) {
                        if (chat.megagroup) {
                            if (onlineCount > 1) {
                                newSubtitle = String.format("%s, %s", LocaleController.formatPluralString("Members", info.participants_count), LocaleController.formatPluralString("OnlineCount", Math.min(onlineCount, info.participants_count)));
                            } else {
                                newSubtitle = LocaleController.formatPluralString("Members", info.participants_count);
                            }
                        } else {
                            int[] result = new int[1];
                            String shortNumber = LocaleController.formatShortNumber(info.participants_count, result);
                            if (chat.megagroup) {
                                newSubtitle = LocaleController.formatPluralString("Members", result[0]).replace(String.format("%d", result[0]), shortNumber);
                            } else {
                                newSubtitle = LocaleController.formatPluralString("Subscribers", result[0]).replace(String.format("%d", result[0]), shortNumber);
                            }
                        }
                    } else {
                        if (chat.megagroup) {
                            if (info == null) {
                                newSubtitle = LocaleController.getString("Loading", R.string.Loading).toLowerCase();
                            } else {
                                if (chat.has_geo) {
                                    newSubtitle = LocaleController.getString("MegaLocation", R.string.MegaLocation).toLowerCase();
                                } else if (!TextUtils.isEmpty(chat.username)) {
                                    newSubtitle = LocaleController.getString("MegaPublic", R.string.MegaPublic).toLowerCase();
                                } else {
                                    newSubtitle = LocaleController.getString("MegaPrivate", R.string.MegaPrivate).toLowerCase();
                                }
                            }
                        } else {
                            if (chat.isPublic()) {
                                newSubtitle = LocaleController.getString("ChannelPublic", R.string.ChannelPublic).toLowerCase();
                            } else {
                                newSubtitle = LocaleController.getString("ChannelPrivate", R.string.ChannelPrivate).toLowerCase();
                            }
                        }
                    }
                } else {
                    if (ChatObject.isKickedFromChat(chat)) {
                        newSubtitle = LocaleController.getString("YouWereKicked", R.string.YouWereKicked);
                    } else if (ChatObject.isLeftFromChat(chat)) {
                        newSubtitle = LocaleController.getString("YouLeft", R.string.YouLeft);
                    } else {
                        int count = chat.participants_count;
                        if (info != null && info.participants != null) {
                            count = info.participants.participants.size();
                        }
                        if (onlineCount > 1 && count != 0) {
                            newSubtitle = String.format("%s, %s", LocaleController.formatPluralString("Members", count), LocaleController.formatPluralString("OnlineCount", onlineCount));
                        } else {
                            newSubtitle = LocaleController.formatPluralString("Members", count);
                        }
                    }
                }
            } else if (user != null) {
                User newUser = MessagesController.getInstance(currentAccount).getUser(user.id);
                if (newUser != null) {
                    user = newUser;
                }
                String newStatus;
                if (user.id == UserConfig.getInstance(currentAccount).getClientUserId()) {
                    newStatus = LocaleController.getString("ChatYourSelf", R.string.ChatYourSelf);
                } else if (user.id == 333000 || user.id == 777000 || user.id == 42777) {
                    newStatus = LocaleController.getString("ServiceNotifications", R.string.ServiceNotifications);
                } else if (MessagesController.isSupportUser(user)) {
                    newStatus = LocaleController.getString("SupportStatus", R.string.SupportStatus);
                } else if (user.bot) {
                    newStatus = LocaleController.getString("Bot", R.string.Bot);
                } else {
                    isOnline[0] = false;
                    newStatus = LocaleController.formatUserStatus(currentAccount, user, isOnline);
                    useOnlineColor = isOnline[0];
                }
                newSubtitle = newStatus;
            } else {
                newSubtitle = "";
            }
        } else {
            newSubtitle = printString;
            useOnlineColor = true;
            setTypingAnimation(true);
        }
        lastSubtitleColorKey = useOnlineColor ? Theme.key_chat_status : Theme.key_actionBarDefaultSubtitle;
        if (lastSubtitle == null) {
            subtitleTextView.setText(newSubtitle);
            subtitleTextView.setTextColor(Theme.getColor(lastSubtitleColorKey));
            subtitleTextView.setTag(lastSubtitleColorKey);
        } else {
            lastSubtitle = newSubtitle;
        }
    }

    public void setChatAvatar(Chat chat) {
        avatarDrawable.setInfo(chat);
        if (avatarImageView != null) {
            avatarImageView.setImage(ImageLocation.getForChat(chat, false), "50_50", avatarDrawable, chat);
        }
    }

    public void setUserAvatar(User user) {
        FileLocation newPhoto = null;
        avatarDrawable.setInfo(user);
        if (UserObject.isUserSelf(user)) {
            avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_SAVED);
            avatarDrawable.setSmallSize(true);
            if (avatarImageView != null) {
                avatarImageView.setImage(null, null, avatarDrawable, user);
            }
        } else {
            avatarDrawable.setSmallSize(false);
            if (avatarImageView != null) {
                avatarImageView.setImage(ImageLocation.getForUser(user, false), "50_50", avatarDrawable, user);
            }
        }
    }

    public void checkAndUpdateAvatar() {
        if (parentFragment == null) {
            return;
        }
        User user = parentFragment.getCurrentUser();
        Chat chat = parentFragment.getCurrentChat();
        if (user != null) {
            avatarDrawable.setInfo(user);
            if (UserObject.isUserSelf(user)) {
                avatarDrawable.setSmallSize(true);
                avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_SAVED);
                if (avatarImageView != null) {
                    avatarImageView.setImage(null, null, avatarDrawable, user);
                }
            } else {
                avatarDrawable.setSmallSize(false);
                if (avatarImageView != null) {
                    avatarImageView.setImage(ImageLocation.getForUser(user, false), "50_50", avatarDrawable, user);
                }
            }
        } else if (chat != null) {
            avatarDrawable.setInfo(chat);
            if (avatarImageView != null) {
                avatarImageView.setImage(ImageLocation.getForChat(chat, false), "50_50", avatarDrawable, chat);
            }
        }
    }

    public void updateOnlineCount() {
        if (parentFragment == null) {
            return;
        }
        onlineCount = 0;
        Chat info = parentFragment.getCurrentChat();
        if (info == null) {
            return;
        }
        int currentTime = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
        if (info.isGroup() || info.isChannel() && info.participants_count <= 200 && info.participants != null) {
            for (int a = 0; a < info.participants.participants.size(); a++) {
                Chat.ChatParticipant participant = info.participants.participants.get(a);
                User user = MessagesController.getInstance(currentAccount).getUser(participant.user_id);
                if (user != null && user.status != null && (user.status.expires > currentTime || user.id == UserConfig.getInstance(currentAccount).getClientUserId()) && user.status.expires > 10000) {
                    onlineCount++;
                }
            }
        } else if (info.isChannel() && info.participants_count > 200) {
            onlineCount = info.online_count;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (parentFragment != null) {
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.didUpdateConnectionState);
            updateCurrentConnectionState();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (parentFragment != null) {
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.didUpdateConnectionState);
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didUpdateConnectionState) {

        }
    }

    private void updateCurrentConnectionState() {
        String title = null;
        if (lastSubtitle != null) {
            subtitleTextView.setText(lastSubtitle);
            lastSubtitle = null;
            if (lastSubtitleColorKey != null) {
                subtitleTextView.setTextColor(Theme.getColor(lastSubtitleColorKey));
                subtitleTextView.setTag(lastSubtitleColorKey);
            }
        }
    }
}

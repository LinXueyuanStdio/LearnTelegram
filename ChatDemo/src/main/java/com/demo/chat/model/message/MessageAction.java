package com.demo.chat.model.message;

import com.demo.chat.model.User;
import com.demo.chat.model.small.MessageMedia;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class MessageAction {
    public String title = "message action";
    public String address = "message action";
    public String message = "message action";
    public ArrayList<Integer> users = new ArrayList<>();
    public int channel_id = 1;
    public MessageMedia.Photo photo = new MessageMedia.Photo();
    public int chat_id = 1;
    public int user_id = 1;
    public User.UserProfilePhoto newUserPhoto;
    public int inviter_id;
    public int ttl;
    public int flags;
    public long call_id;
    public int duration;
    public String currency;
    public long total_amount;
    public long game_id;
    public int score;
    public boolean video;
//    public DecryptedMessageAction encryptedAction;
//    public TL_inputGroupCall call;
//    public PhoneCallDiscardReason reason;

    public boolean isChatEditPhoto(){return false;}
    public boolean isLoginUnknownLocation(){return false;}
    public boolean isSecureValuesSent(){return false;}
    public boolean isEmpty(){return false;}
    public boolean isChatCreate(){return false;}
    public boolean isChatDeleteUser(){return false;}
    public boolean isChatAddUser(){return false;}
    public boolean isPinMessage(){return false;}
    public boolean isScreenshotTaken(){return false;}
    public boolean isChatEditTitle(){return false;}
    public boolean isChatDeletePhoto(){return false;}
    public boolean isUserJoined(){return false;}
    public boolean isPhoneCall(){return false;}
    public boolean isContactSignUp(){return false;}
    public boolean isChatJoinedByLink(){return false;}
    public boolean isUserUpdatedPhoto(){return false;}
    public boolean isHistoryClear(){return false;}
}

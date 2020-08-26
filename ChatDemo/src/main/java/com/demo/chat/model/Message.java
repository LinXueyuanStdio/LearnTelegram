package com.demo.chat.model;

import android.text.TextUtils;

import com.demo.chat.messager.AbstractSerializedData;
import com.demo.chat.messager.Utilities;
import com.demo.chat.model.small.MessageEntity;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/26
 * @description null
 * @usage null
 */
public class Message {
    public int id;
    public int from_id;
    public Peer to_id;
    public int date;
    public MessageAction action;
    public int reply_to_msg_id;
    public long reply_to_random_id;
    public String message;
    public MessageMedia media;
    public int flags;
    public boolean mentioned;
    public boolean media_unread;
    public boolean out;
    public boolean unread;
    public ArrayList<MessageEntity> entities = new ArrayList<>();
    public String via_bot_name;
    public ReplyMarkup reply_markup;
    public int views;
    public int edit_date;
    public boolean silent;
    public boolean post;
    public boolean from_scheduled;
    public boolean legacy;
    public boolean edit_hide;
    public MessageFwdHeader fwd_from;
    public int via_bot_id;
    public String post_author;
    public long grouped_id;
    public TL_messageReactions reactions;
    public ArrayList<TL_restrictionReason> restriction_reason = new ArrayList<>();
    public int send_state = 0; //custom
    public int fwd_msg_id = 0; //custom
    public String attachPath = ""; //custom
    public HashMap<String, String> params; //custom
    public long random_id; //custom
    public int local_id = 0; //custom
    public long dialog_id; //custom
    public int ttl; //custom
    public int destroyTime; //custom
    public int layer; //custom
    public int seq_in; //custom
    public int seq_out; //custom
    public boolean with_my_score;
    public Message replyMessage; //custom
    public int reqId; //custom
    public int realId; //custom
    public int stickerVerified = 1; //custom


    public static Message TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
        Message result = null;
        switch (constructor) {
            case 0x1d86f70e:
                result = new TL_messageService_old2();
                break;
            case 0xa7ab1991:
                result = new TL_message_old3();
                break;
            case 0xc3060325:
                result = new TL_message_old4();
                break;
            case 0x555555fa:
                result = new TL_message_secret();
                break;
            case 0x555555f9:
                result = new TL_message_secret_layer72();
                break;
            case 0x90dddc11:
                result = new TL_message_layer72();
                break;
            case 0xc09be45f:
                result = new TL_message_layer68();
                break;
            case 0xc992e15c:
                result = new TL_message_layer47();
                break;
            case 0x5ba66c13:
                result = new TL_message_old7();
                break;
            case 0xc06b9607:
                result = new TL_messageService_layer48();
                break;
            case 0x83e5de54:
                result = new TL_messageEmpty();
                break;
            case 0x2bebfa86:
                result = new TL_message_old6();
                break;
            case 0x44f9b43d:
                result = new TL_message_layer104();
                break;
            case 0x1c9b1027:
                result = new TL_message_layer104_2();
                break;
            case 0xa367e716:
                result = new TL_messageForwarded_old2(); //custom
                break;
            case 0x5f46804:
                result = new TL_messageForwarded_old(); //custom
                break;
            case 0x567699b3:
                result = new TL_message_old2(); //custom
                break;
            case 0x9f8d60bb:
                result = new TL_messageService_old(); //custom
                break;
            case 0x22eb6aba:
                result = new TL_message_old(); //custom
                break;
            case 0x555555F8:
                result = new TL_message_secret_old(); //custom
                break;
            case 0x9789dac4:
                result = new TL_message_layer104_3();
                break;
            case 0x452c0e65:
                result = new TL_message();
                break;
            case 0x9e19a1f6:
                result = new TL_messageService();
                break;
            case 0xf07814c8:
                result = new TL_message_old5(); //custom
                break;
        }
        if (result == null && exception) {
            throw new RuntimeException(String.format("can't parse magic %x in Message", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }

    public void readAttachPath(AbstractSerializedData stream, int currentUserId) {
        boolean hasMedia = media != null && !(media instanceof TL_messageMediaEmpty) && !(media instanceof TL_messageMediaWebPage);
        boolean fixCaption = !TextUtils.isEmpty(message) &&
                (media instanceof TL_messageMediaPhoto_old ||
                        media instanceof TL_messageMediaPhoto_layer68 ||
                        media instanceof TL_messageMediaPhoto_layer74 ||
                        media instanceof TL_messageMediaDocument_old ||
                        media instanceof TL_messageMediaDocument_layer68 ||
                        media instanceof TL_messageMediaDocument_layer74)
                && message.startsWith("-1");
        if ((out || to_id != null && to_id.user_id != 0 && to_id.user_id == from_id && from_id == currentUserId) && (id < 0 || hasMedia || send_state == 3) || legacy) {
            if (hasMedia && fixCaption) {
                if (message.length() > 6 && message.charAt(2) == '_') {
                    params = new HashMap<>();
                    params.put("ve", message);
                }
                if (params != null || message.length() == 2) {
                    message = "";
                }
            }
            if (stream.remaining() > 0) {
                attachPath = stream.readString(false);
                if (attachPath != null) {
                    if ((id < 0 || send_state == 3 || legacy) && attachPath.startsWith("||")) {
                        String args[] = attachPath.split("\\|\\|");
                        if (args.length > 0) {
                            if (params == null) {
                                params = new HashMap<>();
                            }
                            for (int a = 1; a < args.length - 1; a++) {
                                String args2[] = args[a].split("\\|=\\|");
                                if (args2.length == 2) {
                                    params.put(args2[0], args2[1]);
                                }
                            }
                            attachPath = args[args.length - 1].trim();
                            if (legacy) {
                                layer = Utilities.parseInt(params.get("legacy_layer"));
                            }
                        }
                    } else {
                        attachPath = attachPath.trim();
                    }
                }
            }
        }
        if ((flags & MESSAGE_FLAG_FWD) != 0 && id < 0) {
            fwd_msg_id = stream.readInt32(false);
        }
    }

    protected void writeAttachPath(AbstractSerializedData stream) {
        if (this instanceof TL_message_secret || this instanceof TL_message_secret_layer72) {
            String path = attachPath != null ? attachPath : "";
            if (send_state == 1 && params != null && params.size() > 0) {
                for (HashMap.Entry<String, String> entry : params.entrySet()) {
                    path = entry.getKey() + "|=|" + entry.getValue() + "||" + path;
                }
                path = "||" + path;
            }
            stream.writeString(path);
        } else {
            String path = !TextUtils.isEmpty(attachPath) ? attachPath : " ";
            if (legacy) {
                if (params == null) {
                    params = new HashMap<>();
                }
                layer = LAYER;
                params.put("legacy_layer", "" + LAYER);
            }
            if ((id < 0 || send_state == 3 || legacy) && params != null && params.size() > 0) {
                for (HashMap.Entry<String, String> entry : params.entrySet()) {
                    path = entry.getKey() + "|=|" + entry.getValue() + "||" + path;
                }
                path = "||" + path;
            }
            stream.writeString(path);
            if ((flags & MESSAGE_FLAG_FWD) != 0 && id < 0) {
                stream.writeInt32(fwd_msg_id);
            }
        }
    }
}

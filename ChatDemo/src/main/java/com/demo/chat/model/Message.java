package com.demo.chat.model;

import android.text.TextUtils;

import com.demo.chat.messager.AbstractSerializedData;
import com.demo.chat.messager.Utilities;
import com.demo.chat.model.message.MessageAction;
import com.demo.chat.model.message.MessageFwdHeader;
import com.demo.chat.model.message.MessageReactions;
import com.demo.chat.model.reply.ReplyMarkup;
import com.demo.chat.model.small.MessageEntity;
import com.demo.chat.model.small.MessageMedia;

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
    //public static final int MESSAGE_FLAG_UNREAD             = 0x00000001;
    //public static final int MESSAGE_FLAG_OUT                = 0x00000002;
    public static final int MESSAGE_FLAG_FWD                = 0x00000004;
    public static final int MESSAGE_FLAG_REPLY              = 0x00000008;
    //public static final int MESSAGE_FLAG_MENTION            = 0x00000010;
    //public static final int MESSAGE_FLAG_CONTENT_UNREAD     = 0x00000020;
    public static final int MESSAGE_FLAG_HAS_MARKUP         = 0x00000040;
    public static final int MESSAGE_FLAG_HAS_ENTITIES       = 0x00000080;
    public static final int MESSAGE_FLAG_HAS_FROM_ID        = 0x00000100;
    public static final int MESSAGE_FLAG_HAS_MEDIA          = 0x00000200;
    public static final int MESSAGE_FLAG_HAS_VIEWS          = 0x00000400;
    public static final int MESSAGE_FLAG_HAS_BOT_ID         = 0x00000800;
    public static final int MESSAGE_FLAG_EDITED             = 0x00008000;
    public static final int MESSAGE_FLAG_MEGAGROUP          = 0x80000000;

    public int id;
    public int from_id;
    public int to_id;
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
    public MessageReactions reactions;
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
    public void readParams(AbstractSerializedData stream, boolean exception) {

    }

    public static Message TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
        Message result = new Message();
        if (result == null && exception) {
            throw new RuntimeException(String.format("can't parse magic %x in Message", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }

    public void readAttachPath(AbstractSerializedData stream, int currentUserId) {
        boolean hasMedia = media != null && !(media.isWebPage());
        boolean fixCaption = !TextUtils.isEmpty(message) && message.startsWith("-1");
        if ((out || to_id != 0 && to_id == from_id && from_id == currentUserId) && (id < 0 || hasMedia || send_state == 3) || legacy) {
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
        String path = !TextUtils.isEmpty(attachPath) ? attachPath : " ";
        if (legacy) {
            if (params == null) {
                params = new HashMap<>();
            }
            int LAYER = 1;
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

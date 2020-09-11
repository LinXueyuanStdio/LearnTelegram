package com.demo.chat.model;

import android.text.TextUtils;

import com.demo.chat.messager.AbstractSerializedData;
import com.demo.chat.messager.NativeByteBuffer;
import com.demo.chat.messager.Utilities;
import com.demo.chat.model.message.MessageAction;
import com.demo.chat.model.message.MessageFwdHeader;
import com.demo.chat.model.message.MessageReactions;
import com.demo.chat.model.reply.ReplyMarkup;
import com.demo.chat.model.small.MessageEntity;
import com.demo.chat.model.small.MessageMedia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

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

    public int id = 1;
    public int from_id = new Random().nextInt(50);
    public int to_id = new Random().nextInt(50);
    public int date = new Random().nextInt((int) (System.currentTimeMillis()/1000));
    public MessageAction action= null;
    public int reply_to_msg_id = 0;
    public long reply_to_random_id = 0;
    public String message = "几行代码轻松实现底部导航栏（Tab文字图片高度随意更改）；\n" +
            "中间可添加加号按钮，也可添加文字；（足够的属性满足你需要实现的加号样式）\n" +
            "如果还不能满足、中间可添加自定义View；\n" +
            "Tab中随意添加小红点提示、数字消息提示；\n" +
            "点击按钮可跳转界面、也可作为Tab切换Fragment；\n" +
            "2.0.+迁移AndroidX、支持ViewPager2;\n" +
            "剥离导航栏、不传Fragment则不会创建ViewPager、可自行实现ViewPager使用setupWithViewPager方法与之关联；\n" +
            "支持仅图片、仅文字的方式.；\n" +
            "支持字体单位修改、SP和DP切换；\n" +
            "支持红点消息大于99、则显示椭圆可自定义背景颜色及角度；\n" +
            "更多使用参考简书；";
    public MessageMedia media = new MessageMedia();
    public int flags;
    public boolean mentioned = false;
    public boolean media_unread = false;
    public boolean out = false;
    public boolean unread = false;
    public ArrayList<MessageEntity> entities = new ArrayList<>();
    public String via_bot_name = "";
    public ReplyMarkup reply_markup = new ReplyMarkup();
    public int views = 0;
    public int edit_date = 1;
    public boolean silent = false;
    public boolean post = false;
    public boolean from_scheduled = false;
    public boolean legacy = false;
    public boolean edit_hide = false;
    public MessageFwdHeader fwd_from = null;//new MessageFwdHeader();
    public int via_bot_id = 0;
    public String post_author = "author";
    public long grouped_id = 1;
    public MessageReactions reactions = null;//new MessageReactions();
    public int send_state = 0; //custom
    public int fwd_msg_id = 0; //custom
    public String attachPath = ""; //custom
    public HashMap<String, String> params = null;//new HashMap<>(); //custom
    public long random_id = 0; //custom
    public int local_id = 0; //custom
    public long dialog_id = 0; //custom
    public int ttl = 1; //custom
    public int destroyTime = 0; //custom
    public int layer = 1; //custom
    public int seq_in = 1; //custom
    public int seq_out = 1; //custom
    public boolean with_my_score = true;
    public Message replyMessage = null; //custom
    public int reqId = 1; //custom
    public int realId = 1; //custom
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

    public int networkType;

    public boolean disableFree = false;
    private static final ThreadLocal<NativeByteBuffer> sizeCalculator = new ThreadLocal<NativeByteBuffer>() {
        @Override
        protected NativeByteBuffer initialValue() {
            return new NativeByteBuffer(true);
        }
    };

    public void serializeToStream(AbstractSerializedData stream) {

    }
    public void freeResources() {

    }
    public int getObjectSize() {
        NativeByteBuffer byteBuffer = sizeCalculator.get();
        byteBuffer.rewind();
        serializeToStream(sizeCalculator.get());
        return byteBuffer.length();
    }
}

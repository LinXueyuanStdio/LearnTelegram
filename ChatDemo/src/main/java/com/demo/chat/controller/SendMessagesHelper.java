package com.demo.chat.controller;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;

import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.NotificationCenter;
import com.demo.chat.model.VideoEditedInfo;
import com.demo.chat.model.small.MessageEntity;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class SendMessagesHelper extends BaseController implements NotificationCenter.NotificationCenterDelegate  {

    private static volatile SendMessagesHelper[] Instance = new SendMessagesHelper[UserConfig.MAX_ACCOUNT_COUNT];
    public static SendMessagesHelper getInstance(int num) {
        SendMessagesHelper localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (SendMessagesHelper.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new SendMessagesHelper(num);
                }
            }
        }
        return localInstance;
    }

    public SendMessagesHelper(int instance) {
        super(instance);

        AndroidUtilities.runOnUIThread(() -> {
            getNotificationCenter().addObserver(SendMessagesHelper.this, NotificationCenter.FileDidUpload);
            getNotificationCenter().addObserver(SendMessagesHelper.this, NotificationCenter.FileDidFailUpload);
            getNotificationCenter().addObserver(SendMessagesHelper.this, NotificationCenter.filePreparingStarted);
            getNotificationCenter().addObserver(SendMessagesHelper.this, NotificationCenter.fileNewChunkAvailable);
            getNotificationCenter().addObserver(SendMessagesHelper.this, NotificationCenter.filePreparingFailed);
            getNotificationCenter().addObserver(SendMessagesHelper.this, NotificationCenter.httpFileDidFailedLoad);
            getNotificationCenter().addObserver(SendMessagesHelper.this, NotificationCenter.httpFileDidLoad);
            getNotificationCenter().addObserver(SendMessagesHelper.this, NotificationCenter.fileDidLoad);
            getNotificationCenter().addObserver(SendMessagesHelper.this, NotificationCenter.fileDidFailToLoad);
        });
    }

    public void checkUnsentMessages() {
//        getMessagesStorage().getUnsentMessages(1000);TODO
    }


    public static Bitmap createVideoThumbnail(String filePath, int kind) {
        float size;
        if (kind == MediaStore.Video.Thumbnails.FULL_SCREEN_KIND)  {
            size = 1920;
        } else if (kind == MediaStore.Video.Thumbnails.MICRO_KIND) {
            size = 96;
        } else {
            size = 512;
        }
        Bitmap bitmap = createVideoThumbnailAtTime(filePath, 0);
        if (bitmap != null) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            if (w > size || h > size) {
                float scale = Math.max(w, h) / size;
                w /= scale;
                h /= scale;
                bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
            }
        }
        return bitmap;
    }

    public static Bitmap createVideoThumbnailAtTime(String filePath, long time) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime(time, MediaMetadataRetriever.OPTION_NEXT_SYNC);
            if (bitmap == null) {
                bitmap = retriever.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST);
            }
        } catch (Exception ignore) {
            // Assume this is a corrupt video file.
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }
        return bitmap;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {

    }


    public static class SendingMediaInfo {
        public Uri uri;
        public String path;
        public String caption;
        public String thumbPath;
        public String paintPath;
        public int ttl;
        public ArrayList<MessageEntity> entities;
        public ArrayList<TLRPC.InputDocument> masks;
        public VideoEditedInfo videoEditedInfo;
        public MediaController.SearchImage searchImage;
        public TLRPC.BotInlineResult inlineResult;
        public HashMap<String, String> params;
        public boolean isVideo;
        public boolean canDeleteAfter;
    }
}

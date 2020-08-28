package com.demo.chat.ui.Components;

import android.annotation.SuppressLint;
import android.content.Context;

import com.demo.chat.R;
import com.demo.chat.controller.FileLoader;
import com.demo.chat.controller.LocaleController;
import com.demo.chat.messager.ImageLocation;
import com.demo.chat.model.action.MessageObject;
import com.demo.chat.model.small.Document;
import com.demo.chat.model.small.Media;
import com.demo.chat.model.small.PhotoSize;
import com.demo.chat.model.sticker.MessagesStickerSet;
import com.demo.chat.model.sticker.Sticker;
import com.demo.chat.model.sticker.StickerSet;
import com.demo.chat.model.sticker.StickerSetCovered;

import java.util.ArrayList;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
@SuppressLint("ViewConstructor")
public class StickerSetBulletinLayout extends Bulletin.TwoLineLayout {

    public static final int TYPE_REMOVED = 0;
    public static final int TYPE_ARCHIVED = 1;
    public static final int TYPE_ADDED = 2;

    @IntDef(value = {TYPE_REMOVED, TYPE_ARCHIVED, TYPE_ADDED})
    public @interface Type {}

    public StickerSetBulletinLayout(@NonNull Context context, Sticker setObject, @Type int type) {
        super(context);

        final Document sticker;
        final StickerSet stickerSet;

        if (setObject instanceof MessagesStickerSet) {
            final MessagesStickerSet obj = (MessagesStickerSet) setObject;
            stickerSet = obj.set;
            final ArrayList<Document> documents = obj.documents;
            if (documents != null && !documents.isEmpty()) {
                sticker = documents.get(0);
            } else {
                sticker = null;
            }
        } else if (setObject instanceof StickerSetCovered) {
            final StickerSetCovered obj = (StickerSetCovered) setObject;
            stickerSet = obj.set;
            if (obj.cover != null) {
                sticker = obj.cover;
            } else if (!obj.covers.isEmpty()) {
                sticker = obj.covers.get(0);
            } else {
                sticker = null;
            }
        } else {
            throw new IllegalArgumentException("Invalid type of the given setObject: " + setObject.getClass());
        }

        if (sticker != null) {
            Media object;
            if (stickerSet.thumb != null) {
                object = stickerSet.thumb;
            } else {
                object = sticker;
            }

            ImageLocation imageLocation;
            if (object instanceof Document) {
                PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(sticker.thumbs, 90);
                imageLocation = ImageLocation.getForDocument(thumb, sticker);
            } else {
                PhotoSize thumb = (PhotoSize) object;
                imageLocation = ImageLocation.getForSticker(thumb, sticker);
            }

            if (object instanceof Document && MessageObject.isAnimatedStickerDocument(sticker, true)) {
                imageView.setImage(ImageLocation.getForDocument(sticker), "50_50", imageLocation, null, 0, setObject);
            } else if (imageLocation != null && imageLocation.imageType == FileLoader.IMAGE_TYPE_LOTTIE) {
                imageView.setImage(imageLocation, "50_50", "tgs", null, setObject);
            } else {
                imageView.setImage(imageLocation, "50_50", "webp", null, setObject);
            }
        } else {
            imageView.setImage(null, null, "webp", null, setObject);
        }

        switch (type) {
            case TYPE_ADDED:
                if (stickerSet.masks) {
                    titleTextView.setText(LocaleController.getString("AddMasksInstalled", R.string.AddMasksInstalled));
                    subtitleTextView.setText(LocaleController.formatString("AddMasksInstalledInfo", R.string.AddMasksInstalledInfo, stickerSet.title));
                } else {
                    titleTextView.setText(LocaleController.getString("AddStickersInstalled", R.string.AddStickersInstalled));
                    subtitleTextView.setText(LocaleController.formatString("AddStickersInstalledInfo", R.string.AddStickersInstalledInfo, stickerSet.title));
                }
                break;
            case TYPE_REMOVED:
                if (stickerSet.masks) {
                    titleTextView.setText(LocaleController.getString("MasksRemoved", R.string.MasksRemoved));
                    subtitleTextView.setText(LocaleController.formatString("MasksRemovedInfo", R.string.MasksRemovedInfo, stickerSet.title));
                } else {
                    titleTextView.setText(LocaleController.getString("StickersRemoved", R.string.StickersRemoved));
                    subtitleTextView.setText(LocaleController.formatString("StickersRemovedInfo", R.string.StickersRemovedInfo, stickerSet.title));
                }
                break;
            case TYPE_ARCHIVED:
                if (stickerSet.masks) {
                    titleTextView.setText(LocaleController.getString("MasksArchived", R.string.MasksArchived));
                    subtitleTextView.setText(LocaleController.formatString("MasksArchivedInfo", R.string.MasksArchivedInfo, stickerSet.title));
                } else {
                    titleTextView.setText(LocaleController.getString("StickersArchived", R.string.StickersArchived));
                    subtitleTextView.setText(LocaleController.formatString("StickersArchivedInfo", R.string.StickersArchivedInfo, stickerSet.title));
                }
                break;
        }
    }
}

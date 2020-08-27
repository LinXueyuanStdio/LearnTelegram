package com.demo.chat.messager;

import com.demo.chat.controller.FileLoader;
import com.demo.chat.controller.MediaDataController;
import com.demo.chat.model.Chat;
import com.demo.chat.model.MessageObject;
import com.demo.chat.model.User;
import com.demo.chat.model.action.ChatObject;
import com.demo.chat.model.small.Document;
import com.demo.chat.model.small.FileLocation;
import com.demo.chat.model.small.Media;
import com.demo.chat.model.small.MessageMedia;
import com.demo.chat.model.small.PhotoSize;
import com.demo.chat.model.small.VideoSize;
import com.demo.chat.model.small.WebFile;
import com.demo.chat.model.sticker.InputStickerSet;
import com.demo.chat.theme.Theme;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class ImageLocation {

    public int dc_id;
    public byte[] file_reference;
    public byte[] key;
    public byte[] iv;
    public long access_hash;
    public TL_fileLocationToBeDeprecated location;

    public String path;

    public SecureDocument secureDocument;

    public Document document;

    public PhotoSize photoSize;
    public MessageMedia.Photo photo;
    public boolean photoPeerBig;
    public InputPeer photoPeer;
    public InputStickerSet stickerSet;
    public int imageType;

    public int currentSize;

    public long photoId;
    public long documentId;
    public String thumbSize;

    public WebFile webFile;

    public static ImageLocation getForPath(String path) {
        if (path == null) {
            return null;
        }
        ImageLocation imageLocation = new ImageLocation();
        imageLocation.path = path;
        return imageLocation;
    }

    public static ImageLocation getForSecureDocument(SecureDocument secureDocument) {
        if (secureDocument == null) {
            return null;
        }
        ImageLocation imageLocation = new ImageLocation();
        imageLocation.secureDocument = secureDocument;
        return imageLocation;
    }

    public static ImageLocation getForDocument(Document document) {
        if (document == null) {
            return null;
        }
        ImageLocation imageLocation = new ImageLocation();
        imageLocation.document = document;
        imageLocation.key = document.key;
        imageLocation.iv = document.iv;
        imageLocation.currentSize = document.size;
        return imageLocation;
    }

    public static ImageLocation getForWebFile(WebFile webFile) {
        if (webFile == null) {
            return null;
        }
        ImageLocation imageLocation = new ImageLocation();
        imageLocation.webFile = webFile;
        imageLocation.currentSize = webFile.size;
        return imageLocation;
    }

    public static ImageLocation getForObject(PhotoSize photoSize, Media object) {
        if (object instanceof MessageMedia.Photo) {
            return getForPhoto(photoSize, (MessageMedia.Photo) object);
        } else if (object instanceof Document) {
            return getForDocument(photoSize, (Document) object);
        }
        return null;
    }

    public static ImageLocation getForPhoto(PhotoSize photoSize, MessageMedia.Photo photo) {
        if (photoSize instanceof TL_photoStrippedSize) {
            ImageLocation imageLocation = new ImageLocation();
            imageLocation.photoSize = photoSize;
            return imageLocation;
        } else if (photoSize == null || photo == null) {
            return null;
        }
        int dc_id;
        if (photo.dc_id != 0) {
            dc_id = photo.dc_id;
        } else {
            dc_id = photoSize.location.dc_id;
        }
        return getForPhoto(photoSize.location, photoSize.size, photo, null, null, false, dc_id, null, photoSize.type);
    }

    public static ImageLocation getForUser(User user, boolean big) {
        if (user == null || user.access_hash == 0 || user.photo == null) {
            return null;
        }
        FileLocation fileLocation = big ? user.photo.photo_big : user.photo.photo_small;
        if (fileLocation == null) {
            return null;
        }
        TL_inputPeerUser inputPeer = new TL_inputPeerUser();
        inputPeer.user_id = user.id;
        inputPeer.access_hash = user.access_hash;
        int dc_id;
        if (user.photo.dc_id != 0) {
            dc_id = user.photo.dc_id;
        } else {
            dc_id = fileLocation.dc_id;
        }
        return getForPhoto(fileLocation, 0, null, null, inputPeer, big, dc_id, null, null);
    }

    public static ImageLocation getForChat(Chat chat, boolean big) {
        if (chat == null || chat.photo == null) {
            return null;
        }
        FileLocation fileLocation = big ? chat.photo.photo_big : chat.photo.photo_small;
        if (fileLocation == null) {
            return null;
        }
        InputPeer inputPeer;
        if (ChatObject.isChannel(chat)) {
            if (chat.access_hash == 0) {
                return null;
            }
            inputPeer = new TL_inputPeerChannel();
            inputPeer.channel_id = chat.id;
            inputPeer.access_hash = chat.access_hash;
        } else {
            inputPeer = new TL_inputPeerChat();
            inputPeer.chat_id = chat.id;
        }
        int dc_id;
        if (chat.photo.dc_id != 0) {
            dc_id = chat.photo.dc_id;
        } else {
            dc_id = fileLocation.dc_id;
        }
        return getForPhoto(fileLocation, 0, null, null, inputPeer, big, dc_id, null, null);
    }

    public static ImageLocation getForSticker(PhotoSize photoSize, Document sticker) {
        if (photoSize instanceof TL_photoStrippedSize) {
            ImageLocation imageLocation = new ImageLocation();
            imageLocation.photoSize = photoSize;
            return imageLocation;
        } else if (photoSize == null || sticker == null) {
            return null;
        }
        InputStickerSet stickerSet = MediaDataController.getInputStickerSet(sticker);
        if (stickerSet == null) {
            return null;
        }
        ImageLocation imageLocation = getForPhoto(photoSize.location, photoSize.size, null, null, null, false, sticker.dc_id, stickerSet, photoSize.type);
        if (MessageObject.isAnimatedStickerDocument(sticker, true)) {
            imageLocation.imageType = FileLoader.IMAGE_TYPE_LOTTIE;
        }
        return imageLocation;
    }

    public static ImageLocation getForDocument(VideoSize videoSize, Document document) {
        if (videoSize == null || document == null) {
            return null;
        }
        ImageLocation location = getForPhoto(videoSize.location, videoSize.size, null, document, null, false, document.dc_id, null, videoSize.type);
        location.imageType = FileLoader.IMAGE_TYPE_ANIMATION;
        return location;
    }

    public static ImageLocation getForDocument(PhotoSize photoSize, Document document) {
        if (photoSize instanceof TL_photoStrippedSize) {
            ImageLocation imageLocation = new ImageLocation();
            imageLocation.photoSize = photoSize;
            return imageLocation;
        } else if (photoSize == null || document == null) {
            return null;
        }
        return getForPhoto(photoSize.location, photoSize.size, null, document, null, false, document.dc_id, null, photoSize.type);
    }

    public static ImageLocation getForLocal(FileLocation location) {
        if (location == null) {
            return null;
        }
        ImageLocation imageLocation = new ImageLocation();
        imageLocation.location = new TL_fileLocationToBeDeprecated();
        imageLocation.location.local_id = location.local_id;
        imageLocation.location.volume_id = location.volume_id;
        imageLocation.location.secret = location.secret;
        imageLocation.location.dc_id = location.dc_id;
        return imageLocation;
    }

    private static ImageLocation getForPhoto(FileLocation location, int size, MessageMedia.Photo photo, Document document, InputPeer photoPeer, boolean photoPeerBig, int dc_id, InputStickerSet stickerSet, String thumbSize) {
        if (location == null || photo == null && photoPeer == null && stickerSet == null && document == null) {
            return null;
        }
        ImageLocation imageLocation = new ImageLocation();
        imageLocation.dc_id = dc_id;
        imageLocation.photo = photo;
        imageLocation.currentSize = size;
        imageLocation.photoPeer = photoPeer;
        imageLocation.photoPeerBig = photoPeerBig;
        imageLocation.stickerSet = stickerSet;
        if (location instanceof TL_fileLocationToBeDeprecated) {
            imageLocation.location = (TL_fileLocationToBeDeprecated) location;
            if (photo != null) {
                imageLocation.file_reference = photo.file_reference;
                imageLocation.access_hash = photo.access_hash;
                imageLocation.photoId = photo.id;
                imageLocation.thumbSize = thumbSize;
            } else if (document != null) {
                imageLocation.file_reference = document.file_reference;
                imageLocation.access_hash = document.access_hash;
                imageLocation.documentId = document.id;
                imageLocation.thumbSize = thumbSize;
            }
        } else {
            imageLocation.location = new TL_fileLocationToBeDeprecated();
            imageLocation.location.local_id = location.local_id;
            imageLocation.location.volume_id = location.volume_id;
            imageLocation.location.secret = location.secret;
            imageLocation.dc_id = location.dc_id;
            imageLocation.file_reference = location.file_reference;
            imageLocation.key = location.key;
            imageLocation.iv = location.iv;
            imageLocation.access_hash = location.secret;
        }
        return imageLocation;
    }

    public static String getStippedKey(Object parentObject, Object fullObject, Object strippedObject) {
        if (parentObject instanceof WebPage) {
            if (fullObject instanceof ImageLocation) {
                ImageLocation imageLocation = (ImageLocation) fullObject;
                if (imageLocation.document != null) {
                    fullObject = imageLocation.document;
                } else if (imageLocation.photoSize != null) {
                    fullObject = imageLocation.photoSize;
                } else if (imageLocation.photo != null) {
                    fullObject = imageLocation.photo;
                }
            }
            if (fullObject == null) {
                return "stripped" + FileRefController.getKeyForParentObject(parentObject) + "_" + strippedObject;
            } else if (fullObject instanceof Document) {
                Document document = (Document) fullObject;
                return "stripped" + FileRefController.getKeyForParentObject(parentObject) + "_" + document.id;
            } else if (fullObject instanceof Photo) {
                Photo photo = (Photo) fullObject;
                return "stripped" + FileRefController.getKeyForParentObject(parentObject) + "_" + photo.id;
            } else if (fullObject instanceof PhotoSize) {
                PhotoSize size = (PhotoSize) fullObject;
                if (size.location != null) {
                    return "stripped" + FileRefController.getKeyForParentObject(parentObject) + "_" + size.location.local_id + "_" + size.location.volume_id;
                } else {
                    return "stripped" + FileRefController.getKeyForParentObject(parentObject);
                }
            } else if (fullObject instanceof FileLocation) {
                FileLocation loc = (FileLocation) fullObject;
                return "stripped" + FileRefController.getKeyForParentObject(parentObject) + "_" + loc.local_id + "_" + loc.volume_id;
            }
        }
        return "stripped" + FileRefController.getKeyForParentObject(parentObject);
    }

    public String getKey(Object parentObject, Object fullObject, boolean url) {
        if (photoSize instanceof TL_photoStrippedSize) {
            if (photoSize.bytes.length > 0) {
                return getStippedKey(parentObject, fullObject, photoSize);
            }
        } else if (location != null) {
            return location.volume_id + "_" + location.local_id;
        } else if (webFile != null) {
            return Utilities.MD5(webFile.url);
        } else if (document != null) {
            if (!url && document instanceof DocumentObject.ThemeDocument) {
                DocumentObject.ThemeDocument themeDocument = (DocumentObject.ThemeDocument) document;
                return document.dc_id + "_" + document.id + "_" + Theme.getBaseThemeKey(themeDocument.themeSettings) + "_" + themeDocument.themeSettings.accent_color + "_" + themeDocument.themeSettings.message_top_color + "_" + themeDocument.themeSettings.message_bottom_color;
            } else if (document.id != 0 && document.dc_id != 0) {
                return document.dc_id + "_" + document.id;
            }
        } else if (path != null) {
            return Utilities.MD5(path);
        }
        return null;
    }

    public boolean isEncrypted() {
        return key != null;
    }

    public int getSize() {
        if (photoSize != null) {
            return photoSize.size;
        } else if (document != null) {
            return document.size;
        } else if (webFile != null) {
            return webFile.size;
        }
        return currentSize;
    }
}


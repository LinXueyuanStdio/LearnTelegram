package com.demo.chat.ui.Cells;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.util.Property;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;

import com.demo.chat.R;
import com.demo.chat.controller.DownloadController;
import com.demo.chat.controller.FileLoader;
import com.demo.chat.controller.LocaleController;
import com.demo.chat.controller.MediaController;
import com.demo.chat.controller.MessagesController;
import com.demo.chat.controller.UserConfig;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.Emoji;
import com.demo.chat.messager.FileLog;
import com.demo.chat.messager.ImageLoader;
import com.demo.chat.messager.ImageLocation;
import com.demo.chat.messager.Utilities;
import com.demo.chat.model.MessageObject;
import com.demo.chat.model.User;
import com.demo.chat.model.small.BotInlineResult;
import com.demo.chat.model.small.Document;
import com.demo.chat.model.small.MessageMedia;
import com.demo.chat.model.small.PhotoSize;
import com.demo.chat.receiver.ImageReceiver;
import com.demo.chat.theme.Theme;
import com.demo.chat.ui.Components.AnimationProperties;
import com.demo.chat.ui.Components.CheckBox2;
import com.demo.chat.ui.Components.LayoutHelper;
import com.demo.chat.ui.Components.LetterDrawable;
import com.demo.chat.ui.Components.MediaActionDrawable;
import com.demo.chat.ui.Components.RadialProgress2;
import com.demo.chat.ui.Viewer.PhotoViewer;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/26
 * @description null
 * @usage null
 */
public class ContextLinkCell extends FrameLayout implements DownloadController.FileDownloadProgressListener {

    private final static int DOCUMENT_ATTACH_TYPE_NONE = 0;
    private final static int DOCUMENT_ATTACH_TYPE_DOCUMENT = 1;
    private final static int DOCUMENT_ATTACH_TYPE_GIF = 2;
    private final static int DOCUMENT_ATTACH_TYPE_AUDIO = 3;
    private final static int DOCUMENT_ATTACH_TYPE_VIDEO = 4;
    private final static int DOCUMENT_ATTACH_TYPE_MUSIC = 5;
    private final static int DOCUMENT_ATTACH_TYPE_STICKER = 6;
    private final static int DOCUMENT_ATTACH_TYPE_PHOTO = 7;
    private final static int DOCUMENT_ATTACH_TYPE_GEO = 8;

    public interface ContextLinkCellDelegate {
        void didPressedImage(ContextLinkCell cell);
    }

    private ImageReceiver linkImageView;
    private boolean drawLinkImageView;
    private LetterDrawable letterDrawable;
    private int currentAccount = UserConfig.selectedAccount;
    private Object parentObject;

    private boolean needDivider;
    private boolean buttonPressed;
    private boolean needShadow;

    private boolean canPreviewGif;

    private boolean isForceGif;

    private int linkY;
    private StaticLayout linkLayout;

    private int titleY = AndroidUtilities.dp(7);
    private StaticLayout titleLayout;

    private int descriptionY = AndroidUtilities.dp(27);
    private StaticLayout descriptionLayout;

    private BotInlineResult inlineResult;
    private User inlineBot;
    private Document documentAttach;
    private int currentDate;
    private MessageMedia.Photo photoAttach;
    private PhotoSize currentPhotoObject;
    private int documentAttachType;
    private boolean mediaWebpage;
    private MessageObject currentMessageObject;

    private AnimatorSet animator;

    private Paint backgroundPaint;

    private int TAG;
    private int buttonState;
    private RadialProgress2 radialProgress;

    private long lastUpdateTime;
    private boolean scaled;
    private float scale;
    private static AccelerateInterpolator interpolator = new AccelerateInterpolator(0.5f);

    private boolean hideLoadProgress;

    private CheckBox2 checkBox;

    private ContextLinkCellDelegate delegate;

    public ContextLinkCell(Context context) {
        this(context, false);
    }

    public ContextLinkCell(Context context, boolean needsCheckBox) {
        super(context);

        linkImageView = new ImageReceiver(this);
        linkImageView.setLayerNum(1);
        linkImageView.setUseSharedAnimationQueue(true);
        letterDrawable = new LetterDrawable();
        radialProgress = new RadialProgress2(this);
        TAG = DownloadController.getInstance(currentAccount).generateObserverTag();
        setFocusable(true);

        if (needsCheckBox) {
            backgroundPaint = new Paint();
            backgroundPaint.setColor(Theme.getColor(Theme.key_sharedMedia_photoPlaceholder));

            checkBox = new CheckBox2(context, 21);
            checkBox.setVisibility(INVISIBLE);
            checkBox.setColor(null, Theme.key_sharedMedia_photoPlaceholder, Theme.key_checkboxCheck);
            checkBox.setDrawUnchecked(false);
            checkBox.setDrawBackgroundAsArc(1);
            addView(checkBox, LayoutHelper.createFrame(24, 24, Gravity.RIGHT | Gravity.TOP, 0, 1, 1, 0));
        }
        setWillNotDraw(false);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        drawLinkImageView = false;
        descriptionLayout = null;
        titleLayout = null;
        linkLayout = null;
        currentPhotoObject = null;
        linkY = AndroidUtilities.dp(27);

        if (inlineResult == null && documentAttach == null) {
            setMeasuredDimension(AndroidUtilities.dp(100), AndroidUtilities.dp(100));
            return;
        }

        int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxWidth = viewWidth - AndroidUtilities.dp(AndroidUtilities.leftBaseline) - AndroidUtilities.dp(8);

        PhotoSize currentPhotoObjectThumb = null;
        ArrayList<PhotoSize> photoThumbs = null;
        WebFile webFile = null;
        TLRPC.TL_webDocument webDocument = null;
        String urlLocation = null;

        if (documentAttach != null) {
            photoThumbs = new ArrayList<>(documentAttach.thumbs);
        } else if (inlineResult != null && inlineResult.photo != null) {
            photoThumbs = new ArrayList<>(inlineResult.photo.sizes);
        }

        if (!mediaWebpage && inlineResult != null) {
            if (inlineResult.title != null) {
                try {
                    int width = (int) Math.ceil(Theme.chat_contextResult_titleTextPaint.measureText(inlineResult.title));
                    CharSequence titleFinal = TextUtils.ellipsize(Emoji.replaceEmoji(inlineResult.title.replace('\n', ' '), Theme.chat_contextResult_titleTextPaint.getFontMetricsInt(), AndroidUtilities.dp(15), false), Theme.chat_contextResult_titleTextPaint, Math.min(width, maxWidth), TextUtils.TruncateAt.END);
                    titleLayout = new StaticLayout(titleFinal, Theme.chat_contextResult_titleTextPaint, maxWidth + AndroidUtilities.dp(4), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                } catch (Exception e) {
                    FileLog.e(e);
                }
                letterDrawable.setTitle(inlineResult.title);
            }

            if (inlineResult.description != null) {
                try {
                    descriptionLayout = ChatMessageCell.generateStaticLayout(Emoji.replaceEmoji(inlineResult.description, Theme.chat_contextResult_descriptionTextPaint.getFontMetricsInt(), AndroidUtilities.dp(13), false), Theme.chat_contextResult_descriptionTextPaint, maxWidth, maxWidth, 0, 3);
                    if (descriptionLayout.getLineCount() > 0) {
                        linkY = descriptionY + descriptionLayout.getLineBottom(descriptionLayout.getLineCount() - 1) + AndroidUtilities.dp(1);
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }

            if (inlineResult.url != null) {
                try {
                    int width = (int) Math.ceil(Theme.chat_contextResult_descriptionTextPaint.measureText(inlineResult.url));
                    CharSequence linkFinal = TextUtils.ellipsize(inlineResult.url.replace('\n', ' '), Theme.chat_contextResult_descriptionTextPaint, Math.min(width, maxWidth), TextUtils.TruncateAt.MIDDLE);
                    linkLayout = new StaticLayout(linkFinal, Theme.chat_contextResult_descriptionTextPaint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        }

        String ext = null;
        if (documentAttach != null) {
            if (isForceGif || MessageObject.isGifDocument(documentAttach)) {
                currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(documentAttach.thumbs, 90);
            } else if (MessageObject.isStickerDocument(documentAttach) || MessageObject.isAnimatedStickerDocument(documentAttach, true)) {
                currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(documentAttach.thumbs, 90);
                ext = "webp";
            } else {
                if (documentAttachType != DOCUMENT_ATTACH_TYPE_MUSIC && documentAttachType != DOCUMENT_ATTACH_TYPE_AUDIO) {
                    currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(documentAttach.thumbs, 90);
                }
            }
        } else if (inlineResult != null && inlineResult.photo != null) {
            currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(photoThumbs, AndroidUtilities.getPhotoSize(), true);
            currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(photoThumbs, 80);
            if (currentPhotoObjectThumb == currentPhotoObject) {
                currentPhotoObjectThumb = null;
            }
        }
        if (inlineResult != null) {
            if (inlineResult.content instanceof TLRPC.TL_webDocument) {
                if (inlineResult.type != null) {
                    if (inlineResult.type.startsWith("gif")) {
                        if (inlineResult.thumb instanceof TLRPC.TL_webDocument && "video/mp4".equals(inlineResult.thumb.mime_type)) {
                            webDocument = (TLRPC.TL_webDocument) inlineResult.thumb;
                        } else {
                            webDocument = (TLRPC.TL_webDocument) inlineResult.content;
                        }
                        documentAttachType = DOCUMENT_ATTACH_TYPE_GIF;
                    } else if (inlineResult.type.equals("photo")) {
                        if (inlineResult.thumb instanceof TLRPC.TL_webDocument) {
                            webDocument = (TLRPC.TL_webDocument) inlineResult.thumb;
                        } else {
                            webDocument = (TLRPC.TL_webDocument) inlineResult.content;
                        }
                    }
                }
            }
            if (webDocument == null && (inlineResult.thumb instanceof TLRPC.TL_webDocument)) {
                webDocument = (TLRPC.TL_webDocument) inlineResult.thumb;
            }
            if (webDocument == null && currentPhotoObject == null && currentPhotoObjectThumb == null) {
                if (inlineResult.send_message instanceof TLRPC.TL_botInlineMessageMediaVenue || inlineResult.send_message instanceof TLRPC.TL_botInlineMessageMediaGeo) {
                    double lat = inlineResult.send_message.geo.lat;
                    double lon = inlineResult.send_message.geo._long;
                    if (MessagesController.getInstance(currentAccount).mapProvider == 2) {
                        webFile = WebFile.createWithGeoPoint(inlineResult.send_message.geo, 72, 72, 15, Math.min(2, (int) Math.ceil(AndroidUtilities.density)));
                    } else {
                        urlLocation = AndroidUtilities.formapMapUrl(currentAccount, lat, lon, 72, 72, true, 15, -1);
                    }
                }
            }
            if (webDocument != null) {
                webFile = WebFile.createWithWebDocument(webDocument);
            }
        }

        int width;
        int w = 0;
        int h = 0;

        if (documentAttach != null) {
            for (int b = 0; b < documentAttach.attributes.size(); b++) {
                DocumentAttribute attribute = documentAttach.attributes.get(b);
                if (attribute instanceof TLRPC.TL_documentAttributeImageSize || attribute instanceof TLRPC.TL_documentAttributeVideo) {
                    w = attribute.w;
                    h = attribute.h;
                    break;
                }
            }
        }
        if (w == 0 || h == 0) {
            if (currentPhotoObject != null) {
                if (currentPhotoObjectThumb != null) {
                    currentPhotoObjectThumb.size = -1;
                }
                w = currentPhotoObject.w;
                h = currentPhotoObject.h;
            } else if (inlineResult != null) {
                int[] result = MessageObject.getInlineResultWidthAndHeight(inlineResult);
                w = result[0];
                h = result[1];
            }
        }
        if (w == 0 || h == 0) {
            w = h = AndroidUtilities.dp(80);
        }
        if (documentAttach != null || currentPhotoObject != null || webFile != null || urlLocation != null) {
            String currentPhotoFilter;
            String currentPhotoFilterThumb = "52_52_b";

            if (mediaWebpage) {
                width = (int) (w / (h / (float) AndroidUtilities.dp(80)));
                if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF) {
                    currentPhotoFilterThumb = currentPhotoFilter = String.format(Locale.US, "%d_%d_b", (int) (width / AndroidUtilities.density), 80);
                } else {
                    currentPhotoFilter = String.format(Locale.US, "%d_%d", (int) (width / AndroidUtilities.density), 80);
                    currentPhotoFilterThumb = currentPhotoFilter + "_b";
                }
            } else {
                currentPhotoFilter = "52_52";
            }
            linkImageView.setAspectFit(documentAttachType == DOCUMENT_ATTACH_TYPE_STICKER);

            if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF) {
                if (documentAttach != null) {
                    TLRPC.TL_videoSize thumb = MessageObject.getDocumentVideoThumb(documentAttach);
                    if (thumb != null) {
                        linkImageView.setImage(ImageLocation.getForDocument(thumb, documentAttach), null, ImageLocation.getForDocument(currentPhotoObject, documentAttach), currentPhotoFilter, -1, ext, parentObject, 1);
                    } else {
                        ImageLocation location = ImageLocation.getForDocument(documentAttach);
                        if (isForceGif) {
                            location.imageType = FileLoader.IMAGE_TYPE_ANIMATION;
                        }
                        linkImageView.setImage(location, null, ImageLocation.getForDocument(currentPhotoObject, documentAttach), currentPhotoFilter, documentAttach.size, ext, parentObject, 0);
                    }
                } else if (webFile != null) {
                    linkImageView.setImage(ImageLocation.getForWebFile(webFile), null, ImageLocation.getForPhoto(currentPhotoObject, photoAttach), currentPhotoFilter, -1, ext, parentObject, 1);
                } else {
                    linkImageView.setImage(ImageLocation.getForPath(urlLocation), null, ImageLocation.getForPhoto(currentPhotoObject, photoAttach), currentPhotoFilter, -1, ext, parentObject, 1);
                }
            } else {
                if (currentPhotoObject != null) {
                    if (MessageObject.canAutoplayAnimatedSticker(documentAttach)) {
                        linkImageView.setImage(ImageLocation.getForDocument(documentAttach), "80_80", ImageLocation.getForDocument(currentPhotoObject, documentAttach), currentPhotoFilterThumb, currentPhotoObject.size, null, parentObject, 0);
                    } else {
                        if (documentAttach != null) {
                            linkImageView.setImage(ImageLocation.getForDocument(currentPhotoObject, documentAttach), currentPhotoFilter, ImageLocation.getForPhoto(currentPhotoObjectThumb, photoAttach), currentPhotoFilterThumb, currentPhotoObject.size, ext, parentObject, 0);
                        } else {
                            linkImageView.setImage(ImageLocation.getForPhoto(currentPhotoObject, photoAttach), currentPhotoFilter, ImageLocation.getForPhoto(currentPhotoObjectThumb, photoAttach), currentPhotoFilterThumb, currentPhotoObject.size, ext, parentObject, 0);
                        }
                    }
                } else if (webFile != null) {
                    linkImageView.setImage(ImageLocation.getForWebFile(webFile), currentPhotoFilter, ImageLocation.getForPhoto(currentPhotoObjectThumb, photoAttach), currentPhotoFilterThumb, -1, ext, parentObject, 1);
                } else {
                    linkImageView.setImage(ImageLocation.getForPath(urlLocation), currentPhotoFilter, ImageLocation.getForPhoto(currentPhotoObjectThumb, photoAttach), currentPhotoFilterThumb, -1, ext, parentObject, 1);
                }
            }
            drawLinkImageView = true;
        }

        if (mediaWebpage) {
            width = viewWidth;
            int height = MeasureSpec.getSize(heightMeasureSpec);
            if (height == 0) {
                height = AndroidUtilities.dp(100);
            }
            setMeasuredDimension(width, height);
            int x = (width - AndroidUtilities.dp(24)) / 2;
            int y = (height - AndroidUtilities.dp(24)) / 2;
            radialProgress.setProgressRect(x, y, x + AndroidUtilities.dp(24), y + AndroidUtilities.dp(24));
            radialProgress.setCircleRadius(AndroidUtilities.dp(12));
            linkImageView.setImageCoords(0, 0, width, height);
        } else {
            int height = 0;
            if (titleLayout != null && titleLayout.getLineCount() != 0) {
                height += titleLayout.getLineBottom(titleLayout.getLineCount() - 1);
            }
            if (descriptionLayout != null && descriptionLayout.getLineCount() != 0) {
                height += descriptionLayout.getLineBottom(descriptionLayout.getLineCount() - 1);
            }
            if (linkLayout != null && linkLayout.getLineCount() > 0) {
                height += linkLayout.getLineBottom(linkLayout.getLineCount() - 1);
            }
            height = Math.max(AndroidUtilities.dp(52), height);
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Math.max(AndroidUtilities.dp(68), height + AndroidUtilities.dp(16)) + (needDivider ? 1 : 0));

            int maxPhotoWidth = AndroidUtilities.dp(52);
            int x = LocaleController.isRTL ? MeasureSpec.getSize(widthMeasureSpec) - AndroidUtilities.dp(8) - maxPhotoWidth : AndroidUtilities.dp(8);
            letterDrawable.setBounds(x, AndroidUtilities.dp(8), x + maxPhotoWidth, AndroidUtilities.dp(60));
            linkImageView.setImageCoords(x, AndroidUtilities.dp(8), maxPhotoWidth, maxPhotoWidth);
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
                radialProgress.setCircleRadius(AndroidUtilities.dp(24));
                radialProgress.setProgressRect(x + AndroidUtilities.dp(4), AndroidUtilities.dp(12), x + AndroidUtilities.dp(48), AndroidUtilities.dp(56));
            }
        }
        if (checkBox != null) {
            measureChildWithMargins(checkBox, widthMeasureSpec, 0, heightMeasureSpec, 0);
        }
    }

    private void setAttachType() {
        currentMessageObject = null;
        documentAttachType = DOCUMENT_ATTACH_TYPE_NONE;
        if (documentAttach != null) {
            if (MessageObject.isGifDocument(documentAttach)) {
                documentAttachType = DOCUMENT_ATTACH_TYPE_GIF;
            } else if (MessageObject.isStickerDocument(documentAttach) || MessageObject.isAnimatedStickerDocument(documentAttach, true)) {
                documentAttachType = DOCUMENT_ATTACH_TYPE_STICKER;
            } else if (MessageObject.isMusicDocument(documentAttach)) {
                documentAttachType = DOCUMENT_ATTACH_TYPE_MUSIC;
            } else if (MessageObject.isVoiceDocument(documentAttach)) {
                documentAttachType = DOCUMENT_ATTACH_TYPE_AUDIO;
            }
        } else if (inlineResult != null) {
            if (inlineResult.photo != null) {
                documentAttachType = DOCUMENT_ATTACH_TYPE_PHOTO;
            } else if (inlineResult.type.equals("audio")) {
                documentAttachType = DOCUMENT_ATTACH_TYPE_MUSIC;
            } else if (inlineResult.type.equals("voice")) {
                documentAttachType = DOCUMENT_ATTACH_TYPE_AUDIO;
            }
        }
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            TLRPC.TL_message message = new TLRPC.TL_message();
            message.out = true;
            message.id = -Utilities.random.nextInt();
            message.to_id = new TLRPC.TL_peerUser();
            message.to_id.user_id = message.from_id = UserConfig.getInstance(currentAccount).getClientUserId();
            message.date = (int) (System.currentTimeMillis() / 1000);
            message.message = "";
            message.media = new TLRPC.TL_messageMediaDocument();
            message.media.flags |= 3;
            message.media.document = new TLRPC.TL_document();
            message.media.document.file_reference = new byte[0];
            message.flags |= Message_FLAG_HAS_MEDIA | Message_FLAG_HAS_FROM_ID;

            if (documentAttach != null) {
                message.media.document = documentAttach;
                message.attachPath = "";
            } else {
                String ext = ImageLoader.getHttpUrlExtension(inlineResult.content.url, documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC ? "mp3" : "ogg");
                message.media.document.id = 0;
                message.media.document.access_hash = 0;
                message.media.document.date = message.date;
                message.media.document.mime_type = "audio/" + ext;
                message.media.document.size = 0;
                message.media.document.dc_id = 0;

                TLRPC.TL_documentAttributeAudio attributeAudio = new TLRPC.TL_documentAttributeAudio();
                attributeAudio.duration = MessageObject.getInlineResultDuration(inlineResult);
                attributeAudio.title = inlineResult.title != null ? inlineResult.title : "";
                attributeAudio.performer = inlineResult.description != null ? inlineResult.description : "";
                attributeAudio.flags |= 3;
                if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO) {
                    attributeAudio.voice = true;
                }
                message.media.document.attributes.add(attributeAudio);

                TLRPC.TL_documentAttributeFilename fileName = new TLRPC.TL_documentAttributeFilename();
                fileName.file_name = Utilities.MD5(inlineResult.content.url) + "." + ImageLoader.getHttpUrlExtension(inlineResult.content.url, documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC ? "mp3" : "ogg");
                message.media.document.attributes.add(fileName);

                message.attachPath = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), Utilities.MD5(inlineResult.content.url) + "." + ImageLoader.getHttpUrlExtension(inlineResult.content.url, documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC ? "mp3" : "ogg")).getAbsolutePath();
            }

            currentMessageObject = new MessageObject(currentAccount, message, false);
        }
    }

    public void setLink(BotInlineResult contextResult, User bot, boolean media, boolean divider, boolean shadow) {
        setLink(contextResult, bot, media, divider, shadow, false);
    }

    public void setLink(BotInlineResult contextResult, User bot, boolean media, boolean divider, boolean shadow, boolean forceGif) {
        needDivider = divider;
        needShadow = shadow;
        inlineBot = bot;
        parentObject = inlineResult = contextResult;
        if (inlineResult != null) {
            documentAttach = inlineResult.document;
            photoAttach = inlineResult.photo;
        } else {
            documentAttach = null;
            photoAttach = null;
        }
        mediaWebpage = media;
        isForceGif = forceGif;
        setAttachType();
        if (forceGif) {
            documentAttachType = DOCUMENT_ATTACH_TYPE_GIF;
        }
        requestLayout();
        updateButtonState(false, false);
    }

    public User getInlineBot() {
        return inlineBot;
    }

    public Object getParentObject() {
        return parentObject;
    }

    public void setGif(Document document, boolean divider) {
        setGif(document, "gif" + document, 0, divider);
    }

    public void setGif(Document document, Object parent, int date, boolean divider) {
        needDivider = divider;
        needShadow = false;
        currentDate = date;
        inlineResult = null;
        parentObject = parent;
        documentAttach = document;
        photoAttach = null;
        mediaWebpage = true;
        isForceGif = true;
        setAttachType();
        documentAttachType = DOCUMENT_ATTACH_TYPE_GIF;
        requestLayout();
        updateButtonState(false, false);
    }

    public boolean isSticker() {
        return documentAttachType == DOCUMENT_ATTACH_TYPE_STICKER;
    }

    public boolean isGif() {
        return documentAttachType == DOCUMENT_ATTACH_TYPE_GIF && canPreviewGif;
    }

    public boolean showingBitmap() {
        return linkImageView.getBitmap() != null;
    }

    public int getDate() {
        return currentDate;
    }

    public Document getDocument() {
        return documentAttach;
    }

    public TLRPC.BotInlineResult getBotInlineResult() {
        return inlineResult;
    }

    public ImageReceiver getPhotoImage() {
        return linkImageView;
    }

    public void setScaled(boolean value) {
        scaled = value;
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    public void setCanPreviewGif(boolean value) {
        canPreviewGif = value;
    }

    public boolean isCanPreviewGif() {
        return canPreviewGif;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (drawLinkImageView) {
            linkImageView.onDetachedFromWindow();
        }
        radialProgress.onDetachedFromWindow();
        DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (drawLinkImageView) {
            if (linkImageView.onAttachedToWindow()) {
                updateButtonState(false, false);
            }
        }
        radialProgress.onAttachedToWindow();
    }

    public MessageObject getMessageObject() {
        return currentMessageObject;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mediaWebpage || delegate == null || inlineResult == null) {
            return super.onTouchEvent(event);
        }
        int x = (int) event.getX();
        int y = (int) event.getY();

        boolean result = false;
        int side = AndroidUtilities.dp(48);
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            boolean area = letterDrawable.getBounds().contains(x, y);
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (area) {
                    buttonPressed = true;
                    radialProgress.setPressed(buttonPressed, false);
                    invalidate();
                    result = true;
                }
            } else if (buttonPressed) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    buttonPressed = false;
                    playSoundEffect(SoundEffectConstants.CLICK);
                    didPressedButton();
                    invalidate();
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    buttonPressed = false;
                    invalidate();
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (!area) {
                        buttonPressed = false;
                        invalidate();
                    }
                }
                radialProgress.setPressed(buttonPressed, false);
            }
        } else {
            if (inlineResult != null && inlineResult.content != null && !TextUtils.isEmpty(inlineResult.content.url)) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (letterDrawable.getBounds().contains(x, y)) {
                        buttonPressed = true;
                        result = true;
                    }
                } else {
                    if (buttonPressed) {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            buttonPressed = false;
                            playSoundEffect(SoundEffectConstants.CLICK);
                            delegate.didPressedImage(this);
                        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                            buttonPressed = false;
                        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                            if (!letterDrawable.getBounds().contains(x, y)) {
                                buttonPressed = false;
                            }
                        }
                    }
                }
            }
        }
        if (!result) {
            result = super.onTouchEvent(event);
        }

        return result;
    }

    private void didPressedButton() {
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            if (buttonState == 0) {
                if (MediaController.getInstance().playMessage(currentMessageObject)) {
                    buttonState = 1;
                    radialProgress.setIcon(getIconForCurrentState(), false, true);
                    invalidate();
                }
            } else if (buttonState == 1) {
                boolean result = MediaController.getInstance().pauseMessage(currentMessageObject);
                if (result) {
                    buttonState = 0;
                    radialProgress.setIcon(getIconForCurrentState(), false, true);
                    invalidate();
                }
            } else if (buttonState == 2) {
                radialProgress.setProgress(0, false);
                if (documentAttach != null) {
                    FileLoader.getInstance(currentAccount).loadFile(documentAttach, inlineResult, 1, 0);
                } else if (inlineResult.content instanceof TLRPC.TL_webDocument) {
                    FileLoader.getInstance(currentAccount).loadFile(WebFile.createWithWebDocument(inlineResult.content), 1, 1);
                }
                buttonState = 4;
                radialProgress.setIcon(getIconForCurrentState(), false, true);
                invalidate();
            } else if (buttonState == 4) {
                if (documentAttach != null) {
                    FileLoader.getInstance(currentAccount).cancelLoadFile(documentAttach);
                } else if (inlineResult.content instanceof TLRPC.TL_webDocument) {
                    FileLoader.getInstance(currentAccount).cancelLoadFile(WebFile.createWithWebDocument(inlineResult.content));
                }
                buttonState = 2;
                radialProgress.setIcon(getIconForCurrentState(), false, true);
                invalidate();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (checkBox != null) {
            if (checkBox.isChecked() || !linkImageView.hasBitmapImage() || linkImageView.getCurrentAlpha() != 1.0f || PhotoViewer.isShowingImage((MessageObject) parentObject)) {
                canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), backgroundPaint);
            }
        }
        if (titleLayout != null) {
            canvas.save();
            canvas.translate(AndroidUtilities.dp(LocaleController.isRTL ? 8 : AndroidUtilities.leftBaseline), titleY);
            titleLayout.draw(canvas);
            canvas.restore();
        }

        if (descriptionLayout != null) {
            Theme.chat_contextResult_descriptionTextPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            canvas.save();
            canvas.translate(AndroidUtilities.dp(LocaleController.isRTL ? 8 : AndroidUtilities.leftBaseline), descriptionY);
            descriptionLayout.draw(canvas);
            canvas.restore();
        }

        if (linkLayout != null) {
            Theme.chat_contextResult_descriptionTextPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
            canvas.save();
            canvas.translate(AndroidUtilities.dp(LocaleController.isRTL ? 8 : AndroidUtilities.leftBaseline), linkY);
            linkLayout.draw(canvas);
            canvas.restore();
        }

        if (!mediaWebpage) {
            if (drawLinkImageView && !PhotoViewer.isShowingImage(inlineResult)) {
                letterDrawable.setAlpha((int) (255 * (1.0f - linkImageView.getCurrentAlpha())));
            } else {
                letterDrawable.setAlpha(255);
            }
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
                radialProgress.setProgressColor(Theme.getColor(buttonPressed ? Theme.key_chat_inAudioSelectedProgress : Theme.key_chat_inAudioProgress));
                radialProgress.draw(canvas);
            } else if (inlineResult != null && inlineResult.type.equals("file")) {
                int w = Theme.chat_inlineResultFile.getIntrinsicWidth();
                int h = Theme.chat_inlineResultFile.getIntrinsicHeight();
                int x = (int) (linkImageView.getImageX() + (AndroidUtilities.dp(52) - w) / 2);
                int y = (int) (linkImageView.getImageY() + (AndroidUtilities.dp(52) - h) / 2);
                canvas.drawRect(linkImageView.getImageX(), linkImageView.getImageY(), linkImageView.getImageX() + AndroidUtilities.dp(52), linkImageView.getImageY() + AndroidUtilities.dp(52), LetterDrawable.paint);
                Theme.chat_inlineResultFile.setBounds(x, y, x + w, y + h);
                Theme.chat_inlineResultFile.draw(canvas);
            } else if (inlineResult != null && (inlineResult.type.equals("audio") || inlineResult.type.equals("voice"))) {
                int w = Theme.chat_inlineResultAudio.getIntrinsicWidth();
                int h = Theme.chat_inlineResultAudio.getIntrinsicHeight();
                int x = (int) (linkImageView.getImageX() + (AndroidUtilities.dp(52) - w) / 2);
                int y = (int) (linkImageView.getImageY() + (AndroidUtilities.dp(52) - h) / 2);
                canvas.drawRect(linkImageView.getImageX(), linkImageView.getImageY(), linkImageView.getImageX() + AndroidUtilities.dp(52), linkImageView.getImageY() + AndroidUtilities.dp(52), LetterDrawable.paint);
                Theme.chat_inlineResultAudio.setBounds(x, y, x + w, y + h);
                Theme.chat_inlineResultAudio.draw(canvas);
            } else if (inlineResult != null && (inlineResult.type.equals("venue") || inlineResult.type.equals("geo"))) {
                int w = Theme.chat_inlineResultLocation.getIntrinsicWidth();
                int h = Theme.chat_inlineResultLocation.getIntrinsicHeight();
                int x = (int) (linkImageView.getImageX() + (AndroidUtilities.dp(52) - w) / 2);
                int y = (int) (linkImageView.getImageY() + (AndroidUtilities.dp(52) - h) / 2);
                canvas.drawRect(linkImageView.getImageX(), linkImageView.getImageY(), linkImageView.getImageX() + AndroidUtilities.dp(52), linkImageView.getImageY() + AndroidUtilities.dp(52), LetterDrawable.paint);
                Theme.chat_inlineResultLocation.setBounds(x, y, x + w, y + h);
                Theme.chat_inlineResultLocation.draw(canvas);
            } else {
                letterDrawable.draw(canvas);
            }
        } else {
            if (inlineResult != null && (inlineResult.send_message instanceof TLRPC.TL_botInlineMessageMediaGeo || inlineResult.send_message instanceof TLRPC.TL_botInlineMessageMediaVenue)) {
                int w = Theme.chat_inlineResultLocation.getIntrinsicWidth();
                int h = Theme.chat_inlineResultLocation.getIntrinsicHeight();
                int x = (int) (linkImageView.getImageX() + (linkImageView.getImageWidth() - w) / 2);
                int y = (int) (linkImageView.getImageY() + (linkImageView.getImageHeight() - h) / 2);
                canvas.drawRect(linkImageView.getImageX(), linkImageView.getImageY(), linkImageView.getImageX() + linkImageView.getImageWidth(), linkImageView.getImageY() + linkImageView.getImageHeight(), LetterDrawable.paint);
                Theme.chat_inlineResultLocation.setBounds(x, y, x + w, y + h);
                Theme.chat_inlineResultLocation.draw(canvas);
            }
        }
        if (drawLinkImageView) {
            if (inlineResult != null) {
                linkImageView.setVisible(!PhotoViewer.isShowingImage(inlineResult), false);
            }
            canvas.save();
            if (scaled && scale != 0.8f || !scaled && scale != 1.0f) {
                long newTime = System.currentTimeMillis();
                long dt = (newTime - lastUpdateTime);
                lastUpdateTime = newTime;
                if (scaled && scale != 0.8f) {
                    scale -= dt / 400.0f;
                    if (scale < 0.8f) {
                        scale = 0.8f;
                    }
                } else {
                    scale += dt / 400.0f;
                    if (scale > 1.0f) {
                        scale = 1.0f;
                    }
                }
                invalidate();
            }
            canvas.scale(scale * imageScale, scale * imageScale, getMeasuredWidth() / 2, getMeasuredHeight() / 2);
            linkImageView.draw(canvas);
            canvas.restore();
        }
        if (mediaWebpage && (documentAttachType == DOCUMENT_ATTACH_TYPE_PHOTO || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF)) {
            radialProgress.draw(canvas);
        }

        if (needDivider && !mediaWebpage) {
            if (LocaleController.isRTL) {
                canvas.drawLine(0, getMeasuredHeight() - 1, getMeasuredWidth() - AndroidUtilities.dp(AndroidUtilities.leftBaseline), getMeasuredHeight() - 1, Theme.dividerPaint);
            } else {
                canvas.drawLine(AndroidUtilities.dp(AndroidUtilities.leftBaseline), getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, Theme.dividerPaint);
            }
        }
        if (needShadow) {
            Theme.chat_contextResult_shadowUnderSwitchDrawable.setBounds(0, 0, getMeasuredWidth(), AndroidUtilities.dp(3));
            Theme.chat_contextResult_shadowUnderSwitchDrawable.draw(canvas);
        }
    }

    private int getIconForCurrentState() {
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            radialProgress.setColors(Theme.key_chat_inLoader, Theme.key_chat_inLoaderSelected, Theme.key_chat_inMediaIcon, Theme.key_chat_inMediaIconSelected);
            if (buttonState == 1) {
                return MediaActionDrawable.ICON_PAUSE;
            } else if (buttonState == 2) {
                return MediaActionDrawable.ICON_DOWNLOAD;
            } else if (buttonState == 4) {
                return MediaActionDrawable.ICON_CANCEL;
            }
            return MediaActionDrawable.ICON_PLAY;
        }
        radialProgress.setColors(Theme.key_chat_mediaLoaderPhoto, Theme.key_chat_mediaLoaderPhotoSelected, Theme.key_chat_mediaLoaderPhotoIcon, Theme.key_chat_mediaLoaderPhotoIconSelected);
        return buttonState == 1 ? MediaActionDrawable.ICON_EMPTY : MediaActionDrawable.ICON_NONE;
    }

    public void updateButtonState(boolean ifSame, boolean animated) {
        String fileName = null;
        File cacheFile = null;
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC || documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO) {
            if (documentAttach != null) {
                fileName = FileLoader.getAttachFileName(documentAttach);
                cacheFile = FileLoader.getPathToAttach(documentAttach);
            } else if (inlineResult.content instanceof TLRPC.TL_webDocument) {
                fileName = Utilities.MD5(inlineResult.content.url) + "." + ImageLoader.getHttpUrlExtension(inlineResult.content.url, documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC ? "mp3" : "ogg");
                cacheFile = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), fileName);
            }
        } else if (mediaWebpage) {
            if (inlineResult != null) {
                if (inlineResult.document instanceof TLRPC.TL_document) {
                    fileName = FileLoader.getAttachFileName(inlineResult.document);
                    cacheFile = FileLoader.getPathToAttach(inlineResult.document);
                } else if (inlineResult.photo instanceof TLRPC.TL_photo) {
                    currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(inlineResult.photo.sizes, AndroidUtilities.getPhotoSize(), true);
                    fileName = FileLoader.getAttachFileName(currentPhotoObject);
                    cacheFile = FileLoader.getPathToAttach(currentPhotoObject);
                } else if (inlineResult.content instanceof TLRPC.TL_webDocument) {
                    fileName = Utilities.MD5(inlineResult.content.url) + "." + ImageLoader.getHttpUrlExtension(inlineResult.content.url, FileLoader.getMimeTypePart(inlineResult.content.mime_type));
                    cacheFile = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), fileName);
                    if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF && inlineResult.thumb instanceof TLRPC.TL_webDocument && "video/mp4".equals(inlineResult.thumb.mime_type)) {
                        fileName = null;
                    }
                } else if (inlineResult.thumb instanceof TLRPC.TL_webDocument) {
                    fileName = Utilities.MD5(inlineResult.thumb.url) + "." + ImageLoader.getHttpUrlExtension(inlineResult.thumb.url, FileLoader.getMimeTypePart(inlineResult.thumb.mime_type));
                    cacheFile = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), fileName);
                }
            } else if (documentAttach != null) {
                fileName = FileLoader.getAttachFileName(documentAttach);
                cacheFile = FileLoader.getPathToAttach(documentAttach);
            }

            if (documentAttach != null && documentAttachType == DOCUMENT_ATTACH_TYPE_GIF && MessageObject.getDocumentVideoThumb(documentAttach) != null) {
                fileName = null;
            }
        }

        if (TextUtils.isEmpty(fileName)) {
            buttonState = -1;
            radialProgress.setIcon(MediaActionDrawable.ICON_NONE, ifSame, false);
            return;
        }
        if (!cacheFile.exists()) {
            DownloadController.getInstance(currentAccount).addLoadingFileObserver(fileName, this);
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC || documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO) {
                boolean isLoading;
                if (documentAttach != null) {
                    isLoading = FileLoader.getInstance(currentAccount).isLoadingFile(fileName);
                } else {
                    isLoading = ImageLoader.getInstance().isLoadingHttpFile(fileName);
                }
                if (!isLoading) {
                    buttonState = 2;
                    radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
                } else {
                    buttonState = 4;
                    Float progress = ImageLoader.getInstance().getFileProgress(fileName);
                    if (progress != null) {
                        radialProgress.setProgress(progress, animated);
                    } else {
                        radialProgress.setProgress(0, animated);
                    }
                    radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
                }
            } else {
                buttonState = 1;
                Float progress = ImageLoader.getInstance().getFileProgress(fileName);
                float setProgress = progress != null ? progress : 0;
                radialProgress.setProgress(setProgress, false);
                radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
            }
            invalidate();
        } else {
            DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC || documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO) {
                boolean playing = MediaController.getInstance().isPlayingMessage(currentMessageObject);
                if (!playing || playing && MediaController.getInstance().isMessagePaused()) {
                    buttonState = 0;
                } else {
                    buttonState = 1;
                }
                radialProgress.setProgress(1, animated);
            } else {
                buttonState = -1;
            }
            radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
            invalidate();
        }
    }

    public void setDelegate(ContextLinkCellDelegate contextLinkCellDelegate) {
        delegate = contextLinkCellDelegate;
    }

    public BotInlineResult getResult() {
        return inlineResult;
    }

    @Override
    public void onFailedDownload(String fileName, boolean canceled) {
        updateButtonState(true, canceled);
    }

    @Override
    public void onSuccessDownload(String fileName) {
        radialProgress.setProgress(1, true);
        updateButtonState(false, true);
    }

    @Override
    public void onProgressDownload(String fileName, long downloadedSize, long totalSize) {
        radialProgress.setProgress(Math.min(1f, downloadedSize / (float) totalSize), true);
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            if (buttonState != 4) {
                updateButtonState(false, true);
            }
        } else {
            if (buttonState != 1) {
                updateButtonState(false, true);
            }
        }
    }

    @Override
    public void onProgressUpload(String fileName, long uploadedSize, long totalSize, boolean isEncrypted) {

    }

    @Override
    public int getObserverTag() {
        return TAG;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        StringBuilder sbuf = new StringBuilder();
        switch (documentAttachType) {
            case DOCUMENT_ATTACH_TYPE_DOCUMENT:
                sbuf.append(LocaleController.getString("AttachDocument", R.string.AttachDocument));
                break;
            case DOCUMENT_ATTACH_TYPE_GIF:
                sbuf.append(LocaleController.getString("AttachGif", R.string.AttachGif));
                break;
            case DOCUMENT_ATTACH_TYPE_AUDIO:
                sbuf.append(LocaleController.getString("AttachAudio", R.string.AttachAudio));
                break;
            case DOCUMENT_ATTACH_TYPE_VIDEO:
                sbuf.append(LocaleController.getString("AttachVideo", R.string.AttachVideo));
                break;
            case DOCUMENT_ATTACH_TYPE_MUSIC:
                sbuf.append(LocaleController.getString("AttachMusic", R.string.AttachMusic));
                if (descriptionLayout != null && titleLayout != null) {
                    sbuf.append(", ");
                    sbuf.append(LocaleController.formatString("AccDescrMusicInfo", R.string.AccDescrMusicInfo, descriptionLayout.getText(), titleLayout.getText()));
                }
                break;
            case DOCUMENT_ATTACH_TYPE_STICKER:
                sbuf.append(LocaleController.getString("AttachSticker", R.string.AttachSticker));
                break;
            case DOCUMENT_ATTACH_TYPE_PHOTO:
                sbuf.append(LocaleController.getString("AttachPhoto", R.string.AttachPhoto));
                break;
            case DOCUMENT_ATTACH_TYPE_GEO:
                sbuf.append(LocaleController.getString("AttachLocation", R.string.AttachLocation));
                break;
            default:
                if (titleLayout != null && !TextUtils.isEmpty(titleLayout.getText())) {
                    sbuf.append(titleLayout.getText());
                }
                if (descriptionLayout != null && !TextUtils.isEmpty(descriptionLayout.getText())) {
                    if (sbuf.length() > 0)
                        sbuf.append(", ");
                    sbuf.append(descriptionLayout.getText());
                }
                break;
        }
        info.setText(sbuf);
        if (checkBox != null && checkBox.isChecked()) {
            info.setCheckable(true);
            info.setChecked(true);
        }
    }

    private float imageScale = 1.0f;

    public final Property<ContextLinkCell, Float> IMAGE_SCALE = new AnimationProperties.FloatProperty<ContextLinkCell>("animationValue") {
        @Override
        public void setValue(ContextLinkCell object, float value) {
            imageScale = value;
            invalidate();
        }

        @Override
        public Float get(ContextLinkCell object) {
            return imageScale;
        }
    };

    public void setChecked(boolean checked, boolean animated) {
        if (checkBox == null) {
            return;
        }
        if (checkBox.getVisibility() != VISIBLE) {
            checkBox.setVisibility(VISIBLE);
        }
        checkBox.setChecked(checked, animated);
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        if (animated) {
            animator = new AnimatorSet();
            animator.playTogether(
                    ObjectAnimator.ofFloat(this, IMAGE_SCALE, checked ? 0.81f : 1.0f));
            animator.setDuration(200);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (animator != null && animator.equals(animation)) {
                        animator = null;
                        if (!checked) {
                            setBackgroundColor(0);
                        }
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    if (animator != null && animator.equals(animation)) {
                        animator = null;
                    }
                }
            });
            animator.start();
        } else {
            imageScale = checked ? 0.85f : 1.0f;
            invalidate();
        }
    }
}

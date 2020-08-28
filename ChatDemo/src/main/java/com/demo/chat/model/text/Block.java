package com.demo.chat.model.text;

import com.demo.chat.messager.AbstractSerializedData;
import com.demo.chat.model.Chat;
import com.demo.chat.model.small.MessageMedia;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class Block {
    public static abstract class PageListItem {

        public void readParams(AbstractSerializedData stream, boolean exception) {

        }
        public void serializeToStream(AbstractSerializedData stream) {

        }

        public static PageListItem TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            PageListItem result = null;
            switch (constructor) {
                case 0x25e073fc:
                    result = new TL_pageListItemBlocks();
                    break;
                case 0xb92fb6cd:
                    result = new TL_pageListItemText();
                    break;
            }
            if (result == null && exception) {
                throw new RuntimeException(String.format("can't parse magic %x in PageListItem", constructor));
            }
            if (result != null) {
                result.readParams(stream, exception);
            }
            return result;
        }
    }
    public static class TL_pageListItemBlocks extends PageListItem {
        public static int constructor = 0x25e073fc;

        public ArrayList<PageBlock> blocks = new ArrayList<>();

        public void readParams(AbstractSerializedData stream, boolean exception) {
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                PageBlock object = PageBlock.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                blocks.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(0x1cb5c415);
            int count = blocks.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                blocks.get(a).serializeToStream(stream);
            }
        }
    }

    public static class TL_pageListItemText extends PageListItem {
        public static int constructor = 0xb92fb6cd;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
        }
    }
    public static class TL_pageCaption {
        public static int constructor = 0x6f747657;

        public RichText text;
        public RichText credit;

        public static TL_pageCaption TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            if (TL_pageCaption.constructor != constructor) {
                if (exception) {
                    throw new RuntimeException(String.format("can't parse magic %x in TL_pageCaption", constructor));
                } else {
                    return null;
                }
            }
            TL_pageCaption result = new TL_pageCaption();
            result.readParams(stream, exception);
            return result;
        }

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
            credit = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
            credit.serializeToStream(stream);
        }
    }
    public static class TL_pageTableCell {
        public static int constructor = 0x34566b6a;

        public int flags;
        public boolean header;
        public boolean align_center;
        public boolean align_right;
        public boolean valign_middle;
        public boolean valign_bottom;
        public RichText text;
        public int colspan;
        public int rowspan;

        public static TL_pageTableCell TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            if (TL_pageTableCell.constructor != constructor) {
                if (exception) {
                    throw new RuntimeException(String.format("can't parse magic %x in TL_pageTableCell", constructor));
                } else {
                    return null;
                }
            }
            TL_pageTableCell result = new TL_pageTableCell();
            result.readParams(stream, exception);
            return result;
        }

        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            header = (flags & 1) != 0;
            align_center = (flags & 8) != 0;
            align_right = (flags & 16) != 0;
            valign_middle = (flags & 32) != 0;
            valign_bottom = (flags & 64) != 0;
            if ((flags & 128) != 0) {
                text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if ((flags & 2) != 0) {
                colspan = stream.readInt32(exception);
            }
            if ((flags & 4) != 0) {
                rowspan = stream.readInt32(exception);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            flags = header ? (flags | 1) : (flags &~ 1);
            flags = align_center ? (flags | 8) : (flags &~ 8);
            flags = align_right ? (flags | 16) : (flags &~ 16);
            flags = valign_middle ? (flags | 32) : (flags &~ 32);
            flags = valign_bottom ? (flags | 64) : (flags &~ 64);
            stream.writeInt32(flags);
            if ((flags & 128) != 0) {
                text.serializeToStream(stream);
            }
            if ((flags & 2) != 0) {
                stream.writeInt32(colspan);
            }
            if ((flags & 4) != 0) {
                stream.writeInt32(rowspan);
            }
        }
    }
    public static class TL_pageTableRow {
        public static int constructor = 0xe0c0c5e5;

        public ArrayList<TL_pageTableCell> cells = new ArrayList<>();

        public static TL_pageTableRow TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            if (TL_pageTableRow.constructor != constructor) {
                if (exception) {
                    throw new RuntimeException(String.format("can't parse magic %x in TL_pageTableRow", constructor));
                } else {
                    return null;
                }
            }
            TL_pageTableRow result = new TL_pageTableRow();
            result.readParams(stream, exception);
            return result;
        }

        public void readParams(AbstractSerializedData stream, boolean exception) {
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                TL_pageTableCell object = TL_pageTableCell.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                cells.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(0x1cb5c415);
            int count = cells.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                cells.get(a).serializeToStream(stream);
            }
        }
    }
    public static class TL_pageRelatedArticle  {
        public static int constructor = 0xb390dc08;

        public int flags;
        public String url;
        public long webpage_id;
        public String title;
        public String description;
        public long photo_id;
        public String author;
        public int published_date;

        public static TL_pageRelatedArticle TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            if (TL_pageRelatedArticle.constructor != constructor) {
                if (exception) {
                    throw new RuntimeException(String.format("can't parse magic %x in TL_pageRelatedArticle", constructor));
                } else {
                    return null;
                }
            }
            TL_pageRelatedArticle result = new TL_pageRelatedArticle();
            result.readParams(stream, exception);
            return result;
        }

        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            url = stream.readString(exception);
            webpage_id = stream.readInt64(exception);
            if ((flags & 1) != 0) {
                title = stream.readString(exception);
            }
            if ((flags & 2) != 0) {
                description = stream.readString(exception);
            }
            if ((flags & 4) != 0) {
                photo_id = stream.readInt64(exception);
            }
            if ((flags & 8) != 0) {
                author = stream.readString(exception);
            }
            if ((flags & 16) != 0) {
                published_date = stream.readInt32(exception);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeString(url);
            stream.writeInt64(webpage_id);
            if ((flags & 1) != 0) {
                stream.writeString(title);
            }
            if ((flags & 2) != 0) {
                stream.writeString(description);
            }
            if ((flags & 4) != 0) {
                stream.writeInt64(photo_id);
            }
            if ((flags & 8) != 0) {
                stream.writeString(author);
            }
            if ((flags & 16) != 0) {
                stream.writeInt32(published_date);
            }
        }
    }
    public static abstract class PageListOrderedItem {
        public void readParams(AbstractSerializedData stream, boolean exception) {

        }
        public void serializeToStream(AbstractSerializedData stream) {

        }

        public static PageListOrderedItem TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            PageListOrderedItem result = null;
            switch (constructor) {
                case 0x5e068047:
                    result = new TL_pageListOrderedItemText();
                    break;
                case 0x98dd8936:
                    result = new TL_pageListOrderedItemBlocks();
                    break;
            }
            if (result == null && exception) {
                throw new RuntimeException(String.format("can't parse magic %x in PageListOrderedItem", constructor));
            }
            if (result != null) {
                result.readParams(stream, exception);
            }
            return result;
        }
    }

    public static class TL_pageListOrderedItemText extends PageListOrderedItem {
        public static int constructor = 0x5e068047;

        public String num;
        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            num = stream.readString(exception);
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(num);
            text.serializeToStream(stream);
        }
    }

    public static class TL_pageListOrderedItemBlocks extends PageListOrderedItem {
        public static int constructor = 0x98dd8936;

        public String num;
        public ArrayList<PageBlock> blocks = new ArrayList<>();

        public void readParams(AbstractSerializedData stream, boolean exception) {
            num = stream.readString(exception);
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                PageBlock object = PageBlock.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                blocks.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(num);
            stream.writeInt32(0x1cb5c415);
            int count = blocks.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                blocks.get(a).serializeToStream(stream);
            }
        }
    }

    public static class TL_pageBlockOrderedList extends PageBlock {
        public static int constructor = 0x9a8ae1e1;

        public ArrayList<PageListOrderedItem> items = new ArrayList<>();

        public void readParams(AbstractSerializedData stream, boolean exception) {
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                PageListOrderedItem object = PageListOrderedItem.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                items.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(0x1cb5c415);
            int count = items.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                items.get(a).serializeToStream(stream);
            }
        }
    }

    public static class TL_pageBlockEmbedPost extends PageBlock {
        public static int constructor = 0xf259a80b;

        public String url;
        public long webpage_id;
        public long author_photo_id;
        public String author;
        public int date;
        public ArrayList<PageBlock> blocks = new ArrayList<>();
        public TL_pageCaption caption;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            url = stream.readString(exception);
            webpage_id = stream.readInt64(exception);
            author_photo_id = stream.readInt64(exception);
            author = stream.readString(exception);
            date = stream.readInt32(exception);
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                PageBlock object = PageBlock.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                blocks.add(object);
            }
            caption = TL_pageCaption.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(url);
            stream.writeInt64(webpage_id);
            stream.writeInt64(author_photo_id);
            stream.writeString(author);
            stream.writeInt32(date);
            stream.writeInt32(0x1cb5c415);
            int count = blocks.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                blocks.get(a).serializeToStream(stream);
            }
            caption.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockParagraph extends PageBlock {
        public static int constructor = 0x467a0766;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockKicker extends PageBlock {
        public static int constructor = 0x1e148390;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockFooter extends PageBlock {
        public static int constructor = 0x48870999;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockHeader extends PageBlock {
        public static int constructor = 0xbfd064ec;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockPreformatted extends PageBlock {
        public static int constructor = 0xc070d93e;

        public RichText text;
        public String language;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
            language = stream.readString(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
            stream.writeString(language);
        }
    }

    public static class TL_pageBlockRelatedArticles extends PageBlock {
        public static int constructor = 0x16115a96;

        public RichText title;
        public ArrayList<TL_pageRelatedArticle> articles = new ArrayList<>();

        public void readParams(AbstractSerializedData stream, boolean exception) {
            title = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                TL_pageRelatedArticle object = TL_pageRelatedArticle.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                articles.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            title.serializeToStream(stream);
            stream.writeInt32(0x1cb5c415);
            int count = articles.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                articles.get(a).serializeToStream(stream);
            }
        }
    }

    public static class TL_pageBlockSubheader extends PageBlock {
        public static int constructor = 0xf12bb6e1;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockSlideshow extends PageBlock {
        public static int constructor = 0x31f9590;

        public ArrayList<PageBlock> items = new ArrayList<>();
        public TL_pageCaption caption;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                PageBlock object = PageBlock.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                items.add(object);
            }
            caption = TL_pageCaption.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(0x1cb5c415);
            int count = items.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                items.get(a).serializeToStream(stream);
            }
            caption.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockAnchor extends PageBlock {
        public static int constructor = 0xce0d37b0;

        public String name;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            name = stream.readString(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(name);
        }
    }

    public static class TL_pageBlockMap extends PageBlock {
        public static int constructor = 0xa44f3ef6;

        public MessageMedia.GeoPoint geo;
        public int zoom;
        public int w;
        public int h;
        public TL_pageCaption caption;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            geo = MessageMedia.GeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
            zoom = stream.readInt32(exception);
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
            caption = TL_pageCaption.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            geo.serializeToStream(stream);
            stream.writeInt32(zoom);
            stream.writeInt32(w);
            stream.writeInt32(h);
            caption.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockDivider extends PageBlock {
        public static int constructor = 0xdb20b188;


        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_pageBlockPhoto extends PageBlock {
        public static int constructor = 0x1759c560;

        public int flags;
        public long photo_id;
        public TL_pageCaption caption;
        public String url;
        public long webpage_id;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            photo_id = stream.readInt64(exception);
            caption = TL_pageCaption.TLdeserialize(stream, stream.readInt32(exception), exception);
            if ((flags & 1) != 0) {
                url = stream.readString(exception);
            }
            if ((flags & 1) != 0) {
                webpage_id = stream.readInt64(exception);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt64(photo_id);
            caption.serializeToStream(stream);
            if ((flags & 1) != 0) {
                stream.writeString(url);
            }
            if ((flags & 1) != 0) {
                stream.writeInt64(webpage_id);
            }
        }
    }

    public static class TL_pageBlockList extends PageBlock {
        public static int constructor = 0xe4e88011;

        public boolean ordered;
        public ArrayList<PageListItem> items = new ArrayList<>();

        public void readParams(AbstractSerializedData stream, boolean exception) {
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                PageListItem object = PageListItem.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                items.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(0x1cb5c415);
            int count = items.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                items.get(a).serializeToStream(stream);
            }
        }
    }

    public static class TL_pageBlockUnsupported extends PageBlock {
        public static int constructor = 0x13567e8a;


        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
        }
    }

    public static class TL_pageBlockCollage extends PageBlock {
        public static int constructor = 0x65a0fa4d;

        public ArrayList<PageBlock> items = new ArrayList<>();
        public TL_pageCaption caption;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                PageBlock object = PageBlock.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                items.add(object);
            }
            caption = TL_pageCaption.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(0x1cb5c415);
            int count = items.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                items.get(a).serializeToStream(stream);
            }
            caption.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockEmbed extends PageBlock {
        public static int constructor = 0xa8718dc5;

        public int flags;
        public boolean full_width;
        public boolean allow_scrolling;
        public String url;
        public String html;
        public long poster_photo_id;
        public int w;
        public int h;
        public TL_pageCaption caption;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            full_width = (flags & 1) != 0;
            allow_scrolling = (flags & 8) != 0;
            if ((flags & 2) != 0) {
                url = stream.readString(exception);
            }
            if ((flags & 4) != 0) {
                html = stream.readString(exception);
            }
            if ((flags & 16) != 0) {
                poster_photo_id = stream.readInt64(exception);
            }
            if ((flags & 32) != 0) {
                w = stream.readInt32(exception);
            }
            if ((flags & 32) != 0) {
                h = stream.readInt32(exception);
            }
            caption = TL_pageCaption.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            flags = full_width ? (flags | 1) : (flags &~ 1);
            flags = allow_scrolling ? (flags | 8) : (flags &~ 8);
            stream.writeInt32(flags);
            if ((flags & 2) != 0) {
                stream.writeString(url);
            }
            if ((flags & 4) != 0) {
                stream.writeString(html);
            }
            if ((flags & 16) != 0) {
                stream.writeInt64(poster_photo_id);
            }
            if ((flags & 32) != 0) {
                stream.writeInt32(w);
            }
            if ((flags & 32) != 0) {
                stream.writeInt32(h);
            }
            caption.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockSubtitle extends PageBlock {
        public static int constructor = 0x8ffa9a1f;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockBlockquote extends PageBlock {
        public static int constructor = 0x263d7c26;

        public RichText text;
        public RichText caption;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
            caption = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
            caption.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockDetails extends PageBlock {
        public static int constructor = 0x76768bed;

        public int flags;
        public boolean open;
        public ArrayList<PageBlock> blocks = new ArrayList<>();
        public RichText title;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            open = (flags & 1) != 0;
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                PageBlock object = PageBlock.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                blocks.add(object);
            }
            title = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            flags = open ? (flags | 1) : (flags &~ 1);
            stream.writeInt32(flags);
            stream.writeInt32(0x1cb5c415);
            int count = blocks.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                blocks.get(a).serializeToStream(stream);
            }
            title.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockChannel extends PageBlock {
        public static int constructor = 0xef1751b5;

        public Chat channel;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            channel = Chat.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            channel.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockVideo extends PageBlock {
        public static int constructor = 0x7c8fe7b6;

        public int flags;
        public boolean autoplay;
        public boolean loop;
        public long video_id;
        public TL_pageCaption caption;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            autoplay = (flags & 1) != 0;
            loop = (flags & 2) != 0;
            video_id = stream.readInt64(exception);
            caption = TL_pageCaption.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            flags = autoplay ? (flags | 1) : (flags &~ 1);
            flags = loop ? (flags | 2) : (flags &~ 2);
            stream.writeInt32(flags);
            stream.writeInt64(video_id);
            caption.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockEmbed_layer60 extends TL_pageBlockEmbed {
        public static int constructor = 0xd935d8fb;

        public int flags;
        public boolean full_width;
        public boolean allow_scrolling;
        public String url;
        public String html;
        public int w;
        public int h;
        public RichText caption;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            full_width = (flags & 1) != 0;
            allow_scrolling = (flags & 8) != 0;
            if ((flags & 2) != 0) {
                url = stream.readString(exception);
            }
            if ((flags & 4) != 0) {
                html = stream.readString(exception);
            }
            w = stream.readInt32(exception);
            h = stream.readInt32(exception);
            caption = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            flags = full_width ? (flags | 1) : (flags &~ 1);
            flags = allow_scrolling ? (flags | 8) : (flags &~ 8);
            stream.writeInt32(flags);
            if ((flags & 2) != 0) {
                stream.writeString(url);
            }
            if ((flags & 4) != 0) {
                stream.writeString(html);
            }
            stream.writeInt32(w);
            stream.writeInt32(h);
            caption.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockPullquote extends PageBlock {
        public static int constructor = 0x4f4456d3;

        public RichText text;
        public RichText caption;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
            caption = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
            caption.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockAudio extends PageBlock {
        public static int constructor = 0x804361ea;

        public long audio_id;
        public TL_pageCaption caption;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            audio_id = stream.readInt64(exception);
            caption = TL_pageCaption.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(audio_id);
            caption.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockTable extends PageBlock {
        public static int constructor = 0xbf4dea82;

        public int flags;
        public boolean bordered;
        public boolean striped;
        public RichText title;
        public ArrayList<TL_pageTableRow> rows = new ArrayList<>();

        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            bordered = (flags & 1) != 0;
            striped = (flags & 2) != 0;
            title = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                TL_pageTableRow object = TL_pageTableRow.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                rows.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            flags = bordered ? (flags | 1) : (flags &~ 1);
            flags = striped ? (flags | 2) : (flags &~ 2);
            stream.writeInt32(flags);
            title.serializeToStream(stream);
            stream.writeInt32(0x1cb5c415);
            int count = rows.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                rows.get(a).serializeToStream(stream);
            }
        }
    }

    public static class TL_pageBlockTitle extends PageBlock {
        public static int constructor = 0x70abc3fd;

        public RichText text;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            text = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            text.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockCover extends PageBlock {
        public static int constructor = 0x39f23300;

        public PageBlock cover;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            cover = PageBlock.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            cover.serializeToStream(stream);
        }
    }

    public static class TL_pageBlockAuthorDate extends PageBlock {
        public static int constructor = 0xbaafe5e0;

        public RichText author;
        public int published_date;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            author = RichText.TLdeserialize(stream, stream.readInt32(exception), exception);
            published_date = stream.readInt32(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            author.serializeToStream(stream);
            stream.writeInt32(published_date);
        }
    }

}

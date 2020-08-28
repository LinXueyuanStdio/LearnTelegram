package com.demo.chat.model.small;

import com.demo.chat.messager.AbstractSerializedData;
import com.demo.chat.model.text.Page;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class MessageMedia {

    public byte[] bytes;
    public Audio audio_unused;
    public int flags;
    public boolean shipping_address_requested;
    public Photo photo;
    public GeoPoint geo;
    public String currency;
    public String description;
    public int receipt_msg_id;
    public long total_amount;
    public String start_param;
    public String title;
    public String address;
    public String provider;
    public String venue_id;
    public Video video_unused;
    public Document document;
    public String captionLegacy;
    public String phone_number;
    public String first_name;
    public String last_name;
    public String vcard;
    public int user_id;
    public WebPage webpage;
    public String venue_type;
    public boolean test;
    public int period;
    public int ttl_seconds;

    public static class Audio extends Media {
        public long id;
        public long access_hash;
        public int date;
        public int duration;
        public String mime_type;
        public int size;
        public int dc_id;
        public int user_id;
        public byte[] key;
        public byte[] iv;
    }

    public static class Photo extends Media {
        public int flags;
        public boolean has_stickers;
        public long id;
        public long access_hash;
        public byte[] file_reference;
        public int date;
        public ArrayList<PhotoSize> sizes = new ArrayList<>();
        public int dc_id;
        public int user_id;
        public GeoPoint geo;
        public String caption;
    }

    public static class GeoPoint extends Media {
        public double _long;
        public double lat;
        public long access_hash;


        public void readParams(AbstractSerializedData stream, boolean exception) {

        }
        public void serializeToStream(AbstractSerializedData stream) {

        }
        public static GeoPoint TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            GeoPoint result = null;
            switch (constructor) {
                case 0x296f104:
                    result = new GeoPoint();
                    break;
            }
            if (result == null && exception) {
                throw new RuntimeException(String.format("can't parse magic %x in GeoPoint", constructor));
            }
            if (result != null) {
                result.readParams(stream, exception);
            }
            return result;
        }

    }

    public static class Video extends Media {
        public long id;
        public long access_hash;
        public int user_id;
        public int date;
        public int duration;
        public int size;
        public PhotoSize thumb;
        public int dc_id;
        public int w;
        public int h;
        public String mime_type;
        public String caption;
        public byte[] key;
        public byte[] iv;
    }

    public static class WebPage extends Media {
        public int flags;
        public long id;
        public String url;
        public String display_url;
        public int hash;
        public String type;
        public String site_name;
        public String title;
        public String description;
        public Photo photo;
        public String embed_url;
        public String embed_type;
        public int embed_width;
        public int embed_height;
        public int duration;
        public String author;
        public Document document;
        public Page cached_page;
        public int date;
    }

    public MessageMedia setDocument(boolean isDocument){return this;}
    public MessageMedia setContact(boolean isContact){return this;}
    public MessageMedia setPhoto(boolean isPhoto){return this;}
    public MessageMedia setWebPage(boolean isWebPage){return this;}
    public MessageMedia setInvoice(boolean isInvoice){return this;}
    public boolean isDocument(){return false;}
    public boolean isPhoto(){return false;}
    public boolean isWebPage(){return false;}
    public boolean isContact(){return false;}
    public boolean isInvoice(){return false;}
    public boolean isDice(){return false;}
    public boolean isUnsupported(){return false;}
    public boolean isGeo(){return false;}
    public boolean isGeoLive(){return false;}
    public boolean isVenue(){return false;}
//    public boolean is(){return false;}


    public static MessageMedia TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
        MessageMedia result = new MessageMedia();
        return result;
    }

}

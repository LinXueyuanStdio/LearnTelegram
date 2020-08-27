package com.demo.chat.model.small;

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

    public static class Audio {
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

    public static class Photo {
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

    public static class GeoPoint {
        public double _long;
        public double lat;
        public long access_hash;
    }

    public static class Video {
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

    public static class WebPage {
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

    public static class Page{
        public int flags;
        public boolean part;
        public boolean rtl;
        public String url;
        public ArrayList<PageBlock> blocks = new ArrayList<>();
        public ArrayList<Photo> photos = new ArrayList<>();
        public ArrayList<Document> documents = new ArrayList<>();
        public boolean v2;
        public int views;
    }

    public static class PageBlock{
        public boolean first; //custom
        public boolean bottom; //custom
        public int level; //custom
        public int mid; //custom
        public int groupId; //custom
        public PhotoSize thumb; //custom
    }
}

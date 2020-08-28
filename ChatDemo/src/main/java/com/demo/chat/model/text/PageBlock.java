package com.demo.chat.model.text;

import com.demo.chat.messager.AbstractSerializedData;
import com.demo.chat.model.small.Media;
import com.demo.chat.model.small.PhotoSize;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class PageBlock {
    public boolean first; //custom
    public boolean bottom; //custom
    public int level; //custom
    public int mid; //custom
    public int groupId; //custom
    public PhotoSize thumb; //custom
    public Media thumbObject; //custom

    public void readParams(AbstractSerializedData stream, boolean exception) {

    }
    public void serializeToStream(AbstractSerializedData stream) {

    }

    public static PageBlock TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
        PageBlock result = null;
        switch (constructor) {
            case 0x9a8ae1e1:
                result = new Block.TL_pageBlockOrderedList();
                break;
            case 0xf259a80b:
                result = new Block.TL_pageBlockEmbedPost();
                break;
            case 0x467a0766:
                result = new Block.TL_pageBlockParagraph();
                break;
            case 0x1e148390:
                result = new Block.TL_pageBlockKicker();
                break;
            case 0x48870999:
                result = new Block.TL_pageBlockFooter();
                break;
            case 0xbfd064ec:
                result = new Block.TL_pageBlockHeader();
                break;
            case 0xc070d93e:
                result = new Block.TL_pageBlockPreformatted();
                break;
            case 0x16115a96:
                result = new Block.TL_pageBlockRelatedArticles();
                break;
            case 0xf12bb6e1:
                result = new Block.TL_pageBlockSubheader();
                break;
            case 0x31f9590:
                result = new Block.TL_pageBlockSlideshow();
                break;
            case 0xce0d37b0:
                result = new Block.TL_pageBlockAnchor();
                break;
            case 0xa44f3ef6:
                result = new Block.TL_pageBlockMap();
                break;
            case 0xdb20b188:
                result = new Block.TL_pageBlockDivider();
                break;
            case 0x1759c560:
                result = new Block.TL_pageBlockPhoto();
                break;
            case 0xe4e88011:
                result = new Block.TL_pageBlockList();
                break;
            case 0x13567e8a:
                result = new Block.TL_pageBlockUnsupported();
                break;
            case 0x65a0fa4d:
                result = new Block.TL_pageBlockCollage();
                break;
            case 0xa8718dc5:
                result = new Block.TL_pageBlockEmbed();
                break;
            case 0x8ffa9a1f:
                result = new Block.TL_pageBlockSubtitle();
                break;
            case 0x263d7c26:
                result = new Block.TL_pageBlockBlockquote();
                break;
            case 0x76768bed:
                result = new Block.TL_pageBlockDetails();
                break;
            case 0xef1751b5:
                result = new Block.TL_pageBlockChannel();
                break;
            case 0x7c8fe7b6:
                result = new Block.TL_pageBlockVideo();
                break;
            case 0xd935d8fb:
                result = new Block.TL_pageBlockEmbed_layer60();
                break;
            case 0x4f4456d3:
                result = new Block.TL_pageBlockPullquote();
                break;
            case 0x804361ea:
                result = new Block.TL_pageBlockAudio();
                break;
            case 0xbf4dea82:
                result = new Block.TL_pageBlockTable();
                break;
            case 0x70abc3fd:
                result = new Block.TL_pageBlockTitle();
                break;
            case 0x39f23300:
                result = new Block.TL_pageBlockCover();
                break;
            case 0xbaafe5e0:
                result = new Block.TL_pageBlockAuthorDate();
                break;
        }
        if (result == null && exception) {
            throw new RuntimeException(String.format("can't parse magic %x in PageBlock", constructor));
        }
        if (result != null) {
            result.readParams(stream, exception);
        }
        return result;
    }

}

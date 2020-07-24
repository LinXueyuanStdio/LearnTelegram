package org.telegram.room.entity;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/7/17
 * @description null
 * @usage null
 */
public class RoomFolder extends BaseObj{
    public int flags;
    public boolean autofill_new_broadcasts;
    public boolean autofill_public_groups;
    public boolean autofill_new_correspondents;
    public int id;
    public String title;
    public ChatPhoto photo;
}

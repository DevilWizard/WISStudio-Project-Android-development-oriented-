package com.wisstudio.devilwizard.photobrowserapp.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

/**
 * 储存已缓存图片相关信息的数据库
 *
 * @author WizardK
 * @date 2021-04-13
 */
public class PhotoDataBaseHelper extends SQLiteOpenHelper {


    /***
     * 储存图片信息的表名
     */
    public static final String TABLE_NAME = "Photo";
    /**
     * 创建名为Photo的表
     * 该表包含如下列：
     * id(自增键) | author | url | width | height | cachePath | stared(默认为0，0表示图片未被收藏，1表示已收藏)
     */
    public static final String CREATE_TABLE_PHOTO = "CREATE TABLE "+ TABLE_NAME +"("
            + "id INTERGE primary key autoincrement, "
            + "author TEXT, "
            + "url TEXT, "
            + "width INTERGE, "
            + "height INTERGE, "
            + "cachePath TEXT, "
            + "stared INTERGE DEFAULT 0)";

    /**
     * 初始化存储图片信息的表，若未创建则建表；若表已存在则不执行操作
     *
     * @param context
     * @param name 数据库的文件名（如“xxx.db”）
     * @param factory
     * @param version
     *
     */
    public PhotoDataBaseHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_PHOTO);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

}

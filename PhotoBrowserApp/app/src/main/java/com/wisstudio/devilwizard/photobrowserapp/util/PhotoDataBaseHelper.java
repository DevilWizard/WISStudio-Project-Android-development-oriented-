package com.wisstudio.devilwizard.photobrowserapp.util;

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


    public static final String TABLE_NAME = "Photo";
    /**
     * 创建名为Photo的表
     */
    public static final String CREATE_TABLE_PHOTO = "CREATE TABLE "+ TABLE_NAME +"("
            + "id integer primary key autoincrement, "
            + "author text, "
            + "url text, "
            + "cachePath text)";

    private Context mContext;

    public PhotoDataBaseHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_PHOTO);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}

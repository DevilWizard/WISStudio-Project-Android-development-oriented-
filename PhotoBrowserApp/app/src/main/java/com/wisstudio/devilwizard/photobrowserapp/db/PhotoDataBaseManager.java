package com.wisstudio.devilwizard.photobrowserapp.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.wisstudio.devilwizard.photobrowserapp.util.image.MyImage;
import com.wisstudio.devilwizard.photobrowserapp.util.logutil.MyLog;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于管理本地图片数据库
 *
 * @author WizardK
 * @date 2021-04-13
 */
public class PhotoDataBaseManager {

    /**
     * “已收藏”状态
     */
    public static final int STARRED_STATE = 1;
    /**
     * “未收藏”状态
     */
    public static final int UNSTARRED_STATE = 0;

    private static final String TAG = "DataBaseManager";
    private static final String AUTHOR_COLUMN = "author";
    private static final String URL_COLUMN = "url";
    private static final String WIDTH_COLUMN = "width";
    private static final String HEIGHT_COLUMN = "height";
    private static final String CACHEPATH_COLUMN = "cachePath";
    private static final String STARED_COLUMN = "stared";


    private final SQLiteDatabase db;
    private final String tableName;

    public PhotoDataBaseManager(SQLiteOpenHelper helper) {
        db = helper.getWritableDatabase();
        tableName = PhotoDataBaseHelper.TABLE_NAME;
    }

    /**
     * 往数据库中插入图片相关信息
     *
     * @param author 图片的创作者
     * @param url 图片下载的源网址
     * @param width 图片的宽
     * @param height 图片的高
     * @param cachePath 图片在本地缓存的路径
     *
     */
    public void addOnePhoto(String author, String url, int width, int height, String cachePath) {
        if (author == null || url == null || (width <= 0 || height <= 0) || cachePath == null) {
            throw new IllegalArgumentException("illegal argument detected! The author, url, cachePath must not be null, " +
                    "and width and height both must larger than 0");
        }
        if (!isPhotoExists(url)) {
            ContentValues values = new ContentValues();
            values.put(AUTHOR_COLUMN, author);
            values.put(URL_COLUMN, url);
            values.put(WIDTH_COLUMN, width);
            values.put(HEIGHT_COLUMN, height);
            values.put(CACHEPATH_COLUMN, cachePath);
            db.insert(tableName, null, values);
            values.clear();
        } else {
            MyLog.d(TAG, "addOnePhoto Failed: this photo is already in the database!");
        }

    }

    /**
     * 删除源网址为url的图片记录
     *
     * @param url 待删除图片记录的url
     */
    public void deleteOnePhoto(String url) {
        if (url == null) {
            throw new IllegalArgumentException("the url must not be null !");
        }
        if (isPhotoExists(url)) {
            db.delete(tableName, "url = ?", new String[]{url});
        } else {
            MyLog.d(TAG, "deleteOnePhoto Failed: this photo do not exists!");
        }
    }

    /**
     * 查询源网址为url的图片是否储存在数据库中
     *
     * @param url 图片的源网址
     * @return 若图片在数据库中，则返回true，否则false
     */
    public boolean isPhotoExists(String url) {
        if (url == null) {
            throw new IllegalArgumentException("the url must not be null !");
        }
        String selection = URL_COLUMN + " = ?";
        Cursor cursor = db.query(tableName, null, selection, new String[] {url},
                null, null, null);
        if (cursor.moveToFirst()) {
            return true;
        }
        cursor.close();
        return false;
    }

    /**
     * 返回图片的缓存路径
     *
     * @param url 图片的url
     * @return 返回图片的缓存路径
     *
     * @see com.wisstudio.devilwizard.photobrowserapp.cache.disk.FileCache#getFullCachePath(String)
     */
    public String getPhotoCachePath(String url) {
        if (url == null) {
            throw new IllegalArgumentException("the url must not be null !");
        }
        String selection = URL_COLUMN + " = ?";
        String cachePath = null;
        Cursor cursor = db.query(tableName, null, selection, new String[] {url},
                null, null, null);
        if (cursor.moveToFirst()) {
            cachePath = cursor.getString(cursor.getColumnIndex(CACHEPATH_COLUMN));
        }
        cursor.close();
        return cachePath;
    }

    /**
     * 将网址为url的图片设置为starredState的收藏状态，starredState共有两种状态:
     * {@link #STARRED_STATE}和{@link #UNSTARRED_STATE}
     *
     * @param url 待标记的图片
     * @param starredState 要设置的收藏状态
     */
    public void setStarredState(String url, int starredState) {
        if (url == null) {
            throw new IllegalArgumentException("the url must not be null !");
        }
        if (starredState != STARRED_STATE && starredState != UNSTARRED_STATE) {
            throw new IllegalArgumentException("the starredState must be 0 or 1 !");
        }
        String whereClause = URL_COLUMN + " = ?";
        ContentValues values = new ContentValues();
        switch (starredState) {
            case UNSTARRED_STATE:
                values.put(STARED_COLUMN, UNSTARRED_STATE);
                break;
            case STARRED_STATE:
                values.put(STARED_COLUMN, STARRED_STATE);
                break;
            default:
                break;
        }
        db.update(tableName, values, whereClause, new String[] {url});
        values.clear();
    }

    /**
     * 将数据库中储存的所有图片信息整合成MyImage的列表返回
     *
     * @return java.util.List<com.wisstudio.devilwizard.photobrowserapp.util.image.MyImage>
     *
     * @see MyImage
     */
    public List<MyImage> selectAllPhoto() {
        List<MyImage> cachedImages = new ArrayList<>();
        Cursor cursor = db.query(tableName, null, null, null, null, null, null);
        int authorColumn = cursor.getColumnIndex(AUTHOR_COLUMN);
        int urlColumn = cursor.getColumnIndex(URL_COLUMN);
        int widthColumn = cursor.getColumnIndex(WIDTH_COLUMN);
        int heightColumn = cursor.getColumnIndex(HEIGHT_COLUMN);
        String author = null;
        String url = null;
        int width = 0;
        int height = 0;
        if (cursor.moveToFirst()) {
            do {
                author = cursor.getString(authorColumn);
                url = cursor.getString(urlColumn);
                width = cursor.getInt(widthColumn);
                height = cursor.getInt(heightColumn);
                MyImage myImage = new MyImage(author, width, height, url);
                cachedImages.add(myImage);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return cachedImages;
    }

    /**
     * 关闭当前的数据库连接，此方法只可在完全不需要操作数据库的时候调用，例如app进程被关闭时
     */
    public void closeDataBase() {
        db.close();
    }

}

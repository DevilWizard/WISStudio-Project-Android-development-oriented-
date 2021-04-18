package com.wisstudio.devilwizard.photobrowserapp.util;

import androidx.annotation.NonNull;


/**
 * MyImage是描述每张图片对象的类，包含了图片的宽高、来源网址等属性，还有其对应的{@link android.graphics.Bitmap}属性
 *
 * @author wizardK
 * @version 1.0
 * @date 2021-03-31
 */
public class MyImage {

    /**
     * 图片的作者
     */
    private String author;
    /**
     * 图片的宽(以pixel为单位)
     */
    private int width;
    /**
     * 图片的高(以pixel为单位)
     */
    private int height;
    /**
     * 图片的源网址
     */
    private String download_url;

    public MyImage(String author, int width, int height, String download_url) {
        this.author = author;
        this.width = width;
        this.height = height;
        this.download_url = download_url;
    }

    public String getAuthor() {
        return author;
    }

    public String getUrl() {
        return download_url;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @NonNull
    @Override
    public String toString() {

        return "author: " + this.getAuthor() + "\n" +
               "url: " + this.getUrl() + "\n" ;
    }
}

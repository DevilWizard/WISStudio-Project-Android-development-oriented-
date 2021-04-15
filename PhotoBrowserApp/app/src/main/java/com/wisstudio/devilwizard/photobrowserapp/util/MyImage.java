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

    private String author;
    private String download_url;

    public MyImage(String author, String download_url) {
        this.author = author;
        this.download_url = download_url;
    }

    public String getAuthor() {
        return author;
    }

    public String getUrl() {
        return download_url;
    }


    @NonNull
    @Override
    public String toString() {

        return "author: " + this.getAuthor() + "\n" +
               "url: " + this.getUrl() + "\n" ;
    }
}

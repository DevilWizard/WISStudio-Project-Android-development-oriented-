package com.example.photobrowserapp;

import android.graphics.Bitmap;

public class MyImageList {

    private MyImage[] myImageList = new MyImage[3];

    public MyImageList(MyImage[] myImageList) {
        this.myImageList = myImageList;
    }

    public MyImage getImage(int imageID) {
        if (imageID > myImageList.length) {
            throw new IndexOutOfBoundsException();
        }
        return myImageList[imageID-1];
    }
}

class MyImage {
    private String imageName;
    private Bitmap imageBitmap;

    public MyImage(String imageName, Bitmap imageBitmap) {
        this.imageName = imageName;
        this.imageBitmap = imageBitmap;
    }

    public String getImageName() {
        return imageName;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }
}

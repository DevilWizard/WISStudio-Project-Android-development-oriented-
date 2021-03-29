package com.example.photobrowserapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private static final String TAG = "MainActivity";
    private List<MyImageList> myImageList = new ArrayList<>();
    private ImageView requestResultImg;
    private static final int ROW_IMAGES = 3;
    private static final String PHOTO_SOURCE_URL = "https://picsum.photos/200/300";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initImageList();
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.image_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        ImgAdapter imgAdapter = new ImgAdapter(myImageList);
        recyclerView.setAdapter(imgAdapter);
        Button sendRequest = (Button) findViewById(R.id.sendRequest);
        sendRequest.setOnClickListener(this);

    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.sendRequest) {

//            HttpURL.sendRequest("https://picsum.photos/200/300", new HttpCallBackListener<Bitmap>() {
//                @Override
//                public void onFinish(Bitmap bitmap) {
//                    //由于onFinish是在子线程中调用，因此若要更新UI，必须转回主线程
//                    runOnUiThread(() -> requestResultImg.setImageBitmap(bitmap));
//                }
//                @Override
//                public void onError(Exception e) {
//                    e.printStackTrace();
//                }
//            });

        }
    }

    private void initImageList() {
        MyImage[] myImage = new MyImage[ROW_IMAGES];
        myImage[0] = new MyImage("image1", BitmapFactory.decodeResource(this.getResources(), R.drawable.android_picture));
        myImage[1] = new MyImage("image2", BitmapFactory.decodeResource(this.getResources(), R.drawable.android_picture));
        myImage[2] = new MyImage("image3", BitmapFactory.decodeResource(this.getResources(), R.drawable.android_picture));
//        for (int i=1; i<=3; i++) {
//            HttpURL.sendRequest(PHOTO_SOURCE_URL, new Listener(i, myImage));
//        }
        for (MyImage image : myImage) {
            Log.d(TAG, "myImage: " + image.getImageName() + image.getImageBitmap());
        }
        MyImageList imageList = new MyImageList(myImage);
        MyImageList imageList1 = new MyImageList(myImage);
        myImageList.add(imageList);
        myImageList.add(imageList1);
    }

    private class Listener implements HttpCallBackListener<Bitmap> {

        private int imageNo;
        private MyImage[] myImages;

        public Listener(int imageNo, MyImage[] myImages) {
            this.imageNo = imageNo;
            this.myImages = myImages;
        }

        @Override
        public void onFinish(Bitmap response) {
            myImages[imageNo] = new MyImage("image" + imageNo, response);
        }

        @Override
        public void onError(Exception e) {
            e.printStackTrace();
        }
    }
}
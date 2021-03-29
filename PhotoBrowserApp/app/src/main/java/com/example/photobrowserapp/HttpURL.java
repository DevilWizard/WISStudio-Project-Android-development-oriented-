package com.example.photobrowserapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

public class HttpURL {


    public static void sendRequest(String photoUrl,
                                   HttpCallBackListener<Bitmap> listener) {

        new Thread( () -> {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(photoUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                InputStream in = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);

                if (listener != null)   listener.onFinish(bitmap);
            } catch (Exception e) {
                if (listener != null)   listener.onError(e);
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (connection != null) connection.disconnect();
            }
        }).start();

    }

}

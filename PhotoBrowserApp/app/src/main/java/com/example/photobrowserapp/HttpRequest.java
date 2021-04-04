package com.example.photobrowserapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * HttpRequest类封装了本app中所有与图片请求相关的操作，如{@link #getJson(String, HttpCallBackListener)}、
 * {@link #photo2Bitmap(String)}
 *
 * @author WizardK
 * @version 1.0
 * @date 2021-03-31
 */
public class HttpRequest {

    private static final String TAG = "HttpURL";


    /**
     * 通过读取图片的源网址将其转为Bitmap对象储存在MyImage对象内部
     *
     * @param photoUrl 待转换的图片的源网址
     *
     * @return android.graphics.Bitmap
     *
     * @exception
     */
    public static Bitmap photo2Bitmap(String photoUrl) throws ExecutionException, InterruptedException {

                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try {
                    URL url = new URL(photoUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);
                    InputStream in = connection.getInputStream();

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4;
                    Bitmap bitmap = BitmapFactory.decodeStream(in, null, options);
                    Log.d(TAG, "photo2Bitmap: " + Thread.currentThread().getId());
                    Log.d(TAG, "photo2Bitmap: " + bitmap);
                    return HttpRequest.compressImage(bitmap);

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
                return null;


    }

    /**
     * 压缩图片质量，加快加载速度
     *
     * @param bitmap 待压缩的图片的Bitmap对象
     *
     * @return android.graphics.Bitmap
     *
     * @exception
     */
    public static Bitmap compressImage(Bitmap bitmap){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos);
        int options = 100;
        //循环判断如果压缩后图片是否大于50kb,大于继续压缩
        while ( baos.toByteArray().length / 1024 > 50) {
            //清空baos
            baos.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
            options -= 10;//每次都减少10
        }
        //把压缩后的数据baos存放到ByteArrayInputStream中
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        //把ByteArrayInputStream数据生成图片
        Bitmap newBitmap = BitmapFactory.decodeStream(isBm, null, null);
        return newBitmap;
    }


    

    /**
     * 开启子线程来获取包含若干个图片信息的json文件
     *
     * @param jsonUrl 包含若干张图片的json的url地址
     *
     * @param listener 在将json转为List<MyImage>对象后被回调的监听器
     *
     * @return void
     *
     * @see HttpURLConnection
     *
     * @see InputStream
     *
     * @exception
     */
    public static void getJson(String jsonUrl, HttpCallBackListener<List<MyImage>> listener) {

        new Thread(() -> {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(jsonUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(2500);
                connection.setReadTimeout(2500);
                InputStream in = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                List<MyImage> imageList = parse2MyImageList(response.toString());
                Log.d(TAG, "getJson: json read finished");
                if (listener != null) {
                    listener.onFinish(imageList);
                }
            } catch (Exception e) {
                listener.onError(e);
            } finally {
                //关闭打开的流
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();

    }



    /**
     * 通过图片url储存其bitmap在MyImage对象中，具体通过线程池提交任务异步执行
     * @param  myImageList
     *         通过getJson函数{@link #getJson(String, HttpCallBackListener)}获取到的List<MyImage>对象 ，
     *        其bitamp属性待初始化
     *
     * @param  listener 当一个MyImage对象获取到其imageBitmap属性后马上被回调的监听器
     *
     * @return java.util.List<com.example.photobrowserapp.MyImage>
     *
     * @see ExecutorService
     *
     * @see Executors
     *
     * @exception
     */

    public List<MyImage> setBitmap(List<MyImage> myImageList, HttpCallBackListener<MyImage> listener) throws InterruptedException, ExecutionException {

        int images = myImageList.size();//the num of MyImage objects
        Log.d(TAG, "setBitmap: images" + images);
        ExecutorService executor = Executors.newCachedThreadPool();
        for (int i=0; i<images; i++) {
            executor.submit(new Task(myImageList.get(i), listener));
        }
        return myImageList;
    }


    /**
     * 将请求到的String类型的json转为MyImage的List对象
     *
     * @param jsonData
     *        json纯文本解析转成的String
     *
     * @return java.util.List<com.example.photobrowserapp.MyImage>
     *         返回jsonData转换后的List<MyImage>
     *
     * @exception
     */
    private static List<MyImage> parse2MyImageList(String jsonData) {
        if (jsonData == null)   throw new NullPointerException("The jsonData is null");
        Gson gson = new Gson();
        List<MyImage> imageList = gson.fromJson(jsonData, new TypeToken<List<MyImage>>(){}.getType());
        Log.d(TAG, "parse2MyImageList: imageList " + imageList.get(0));
        return imageList;
    }

}


/**
 * @<code>Task</code>类描述了在图片的Bitmap资源获取后执行的具体操作
 * @author WizardK
 * @version 1.0
 * @date 2021-03-31
 */
class Task implements Runnable {

    private MyImage myImage;
    private HttpCallBackListener<MyImage> listener;

    public Task(MyImage myImage, HttpCallBackListener<MyImage> listener) {
        this.myImage = myImage;
        this.listener = listener;
    }

    /**
     * 具体在线程池内异步执行的操作，即将图片加载进浏览视图中
     * @return void
     * @exception
     */
    @Override
    public void run() {
        try {
            Bitmap bitmap = HttpRequest.photo2Bitmap(myImage.getUrl());
            if (bitmap != null) {//考虑到若网络请求失败，图片的bitmap为null时界面显示留白的情况
                myImage.setImageBitmap(bitmap);
                listener.onFinish(myImage);//只要有图片请求成功就加载进界面，这样可以较为快速的展示图片
            }

        } catch (ExecutionException e) {
            listener.onError(e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
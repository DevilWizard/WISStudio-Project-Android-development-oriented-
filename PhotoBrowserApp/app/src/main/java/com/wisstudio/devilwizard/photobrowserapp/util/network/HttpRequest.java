package com.wisstudio.devilwizard.photobrowserapp.util.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wisstudio.devilwizard.photobrowserapp.db.PhotoDataBaseManager;
import com.wisstudio.devilwizard.photobrowserapp.ui.MainActivity;
import com.wisstudio.devilwizard.photobrowserapp.util.image.MyImage;
import com.wisstudio.devilwizard.photobrowserapp.util.image.load.ImageLoader;
import com.wisstudio.devilwizard.photobrowserapp.util.logutil.MyLog;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


/**
 * HttpRequest类封装了本app中所有与图片请求相关的操作，如{@link #getJson(String, HttpCallBackListener)}
 *
 * @author WizardK
 * @version 1.0
 * @date 2021-03-31
 */
public class HttpRequest {

    private static final String TAG = "HttpRequest";

    private static final PhotoDataBaseManager photoDataBaseManager = MainActivity.photoDBManager;

    /**
     * 开启子线程来获取包含若干个图片信息的json文件
     *
     * @param jsonUrl 包含若干张图片的json的url地址
     *
     * @param listener 在将json转为List<MyImage>对象后被回调的监听器
     *
     * @see HttpURLConnection
     * @see HttpCallBackListener
     * @see InputStream
     *
     * @exception
     */
    public static void getJson(String jsonUrl, HttpCallBackListener<List<MyImage>> listener) {

        new Thread(() -> {
            HttpURLConnection conn = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(jsonUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2500);
                conn.setReadTimeout(2500);
                InputStream in = conn.getInputStream();
                reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                List<MyImage> imageList = parse2MyImageList(response.toString());
                MyLog.d(TAG, "getJson: json read finished");
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
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();

    }

    /**
     * 加载图片的原始bitmap，即加载原图
     *
     * @param url 要保存的图片的下载地址
     *
     * @param listener 回调监听器
     *
     * @return 返回图片的原始bitmap(不经过压缩，相当于是原图)
     *
     * @exception
     */
    public static void loadBitmapFromWeb(String url, HttpCallBackListener<Bitmap> listener) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            InputStream is = null;
            try {
                Bitmap bitmap = null;
                URL imageUrl = new URL(url);
                conn = (HttpURLConnection) imageUrl.openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                is = conn.getInputStream();
                bitmap = BitmapFactory.decodeStream(is);
                listener.onFinish(bitmap);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                listener.onError(e);
            } catch (IOException e) {
                e.printStackTrace();
                listener.onError(e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    /**
     * 从网络获取图片，将其压缩并缓存在指定的文件中
     *
     * @param imageView 要加载图片的ImageView对象
     *
     * @param image 描述图片信息的MyImage对象
     *
     * @param file 缓存文件
     *
     * @return 返回缓存后的Bitmap对象
     */
    public static Bitmap loadBitmapFromWeb(ImageView imageView, MyImage image, File file) {
        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            Bitmap bitmap = null;
            URL imageUrl = new URL(image.getUrl());
            conn = (HttpURLConnection) imageUrl.openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            is = conn.getInputStream();
            bitmap = cacheToLocal(is, file, imageView, image);//将图片缓存至本地
            MyLog.d(TAG, "loadBitmapFromWeb: url: " + imageUrl);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if(is != null) {
                    is.close();
                }
                if(conn != null) {
                    conn.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 将图片压缩并以二进制形式缓存在本地，同时返回采样后的Bitmap，以及将图片信息插入数据库
     *
     * @param is 读取图片的输入流
     *
     * @param file 缓存目的路径的File对象
     *
     * @param imageView 要加载图片的ImageView对象
     *
     * @param image 描述图片信息的MyImage对象
     *
     * @return 返回采样后的Bitmap
     *
     * @exception  FileNotFoundException
     */
    private static Bitmap cacheToLocal(InputStream is, File file, ImageView imageView, MyImage image) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calcuSampleSize(image, imageView.getWidth(), imageView.getHeight());
        Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);//减少采样率，相当于是内存占用压缩
        byte[] bitmapBytes = qualityCompress(bitmap);//质量压缩后缓存在本地
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file);
            byteToFile(bitmapBytes, os);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (os !=null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        photoDataBaseManager.addOnePhoto(image.getAuthor(), image.getUrl(), image.getWidth(), image.getHeight(),
                ImageLoader.getInstance().getFileCache().getFullCachePath(image.getUrl()));//将图片信息插入数据库

        return bitmap;
    }

    /**
     * 将byte流储存在输出流中
     *
     * @param bytes 待储存的byte数组
     *
     * @param os 储存的输出流
     *
     * @exception
     */
    private static void byteToFile(byte[] bytes, OutputStream os){
        try {
            os.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 压缩图片质量，减少空间占用
     *
     * @param bitmap 待压缩的图片的Bitmap对象
     *
     * @return byte[]
     *
     * @exception
     */
    private static byte[] qualityCompress(Bitmap bitmap){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        int options = 100;
        //循环判断如果压缩后图片是否大于50kb,大于继续压缩
        while ( baos.toByteArray().length / 1024 > 50 && options > 0) {
            //清空baos
            baos.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
            options -= 10;//每次都减少10
        }
        return baos.toByteArray();
    }

    /**
     * 计算合适的采样比例
     *
     * @param image 待采样图片的MyImage对象
     * @param reqWidth 压缩后的宽度(以pixel为单位)
     * @param reqHeight 压缩后的高度(以pixel为单位)
     *
     * @return 返回合适大小的inSampleSize
     *
     */
    private static int calcuSampleSize(MyImage image, int reqWidth, int reqHeight) {
        //图片的原始宽高
        final int originalWidth = image.getWidth();
        final int originalHeight = image.getHeight();
        int sampleSize = 1;
        if (originalWidth > reqWidth || originalHeight > reqHeight) {
            int heightRatio = Math.round((float) originalHeight / (float) reqHeight);
            int widthRatio = Math.round((float) originalWidth / (float) reqWidth);
            // 选择宽和高中最小的比率作为sampleSize的值，这样可以保证最终图片的宽和高
            // 一定都会大于等于目标的宽和高。
            sampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        MyLog.d(TAG, "reqWidth: " + reqWidth + "reqHeight: " + reqHeight);
        MyLog.d(TAG, "calcuSampleSize: sampleSize" + sampleSize);
        return sampleSize;
    }

    /**
     * 将请求到的String类型的json转为MyImage的List对象
     *
     * @param jsonData
     *        json纯文本解析转成的String
     *
     * @return 返回jsonData转换后的List<MyImage>
     *
     * @see Gson#fromJson(String, Type)
     *
     * @exception
     */
    private static List<MyImage> parse2MyImageList(String jsonData) {
        if (jsonData == null)   throw new NullPointerException("The jsonData is null");
        Gson gson = new Gson();
        List<MyImage> imageList = gson.fromJson(jsonData, new TypeToken<List<MyImage>>(){}.getType());
        return imageList;
    }

}

package com.wisstudio.devilwizard.photobrowserapp.util.image.download;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import com.wisstudio.devilwizard.photobrowserapp.ui.MainActivity;
import com.wisstudio.devilwizard.photobrowserapp.util.MyApplication;
import com.wisstudio.devilwizard.photobrowserapp.util.image.load.ImageLoader;
import com.wisstudio.devilwizard.photobrowserapp.util.logutil.MyLog;
import com.wisstudio.devilwizard.photobrowserapp.util.network.HttpCallBackListener;
import com.wisstudio.devilwizard.photobrowserapp.util.network.HttpRequest;
import com.wisstudio.devilwizard.photobrowserapp.util.network.NetWorkState;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 将图片保存至本地相册的专用类
 *
 * @author WizardK
 * @date 2021-04-17
 */
public class ImageDownLoader {

    /**
     * 图片保存的默认目录名
     */
    public static final String SAVE_DIRECTORY_NAME = "photo_save";
    private static final String TAG = "ImageDownLoader";
    private final String savePath;

    /**
     * 初始化图片储存目录
     */
    public ImageDownLoader() {
        //安卓9以后getExternalStorageDirectory已被弃用，无法在其下创建文件目录
        savePath = MyApplication.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + File.separator + SAVE_DIRECTORY_NAME;
        File file = new File(savePath);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    /**
     * 保存图片到本地相册，有网络则请求下载，若无网络则提示保存失败
     *
     * @param url 要保存到相册的图片的下载网址
     *
     * @exception
     */
    public void saveImageToGallery(String url) throws FileNotFoundException {

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            ContentValues values = new ContentValues();
//            values.put(MediaStore.MediaColumns.DISPLAY_NAME, );
//        }

        if (NetWorkState.isNetworkConnected(MyApplication.getContext())) {
            MainActivity.getMainActivity().runOnUiThread(() -> Toast.makeText(MyApplication.getContext(),
                    "图片已保存", Toast.LENGTH_SHORT).show());
            HttpRequest.loadBitmapFromWeb(url, new HttpCallBackListener<Bitmap>() {
                @Override
                public void onFinish(Bitmap response) {
                    String fileName = ImageLoader.url2path(url) + ".jpg";
                    File file = new File(savePath, fileName);
                    MyLog.d(TAG, "onFinish: " + file.getAbsolutePath());
                    FileOutputStream fileOutputStream = null;
                    Bitmap bitmap = response;
                    try {
                        fileOutputStream = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                        MediaStore.Images.Media.insertImage(MyApplication.getContext().getContentResolver(),
                                file.getAbsolutePath(), fileName, null);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Uri uri = Uri.fromFile(file);
                    MyApplication.getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                    //不能在ui线程外toast
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            MainActivity.getMainActivity().runOnUiThread(() -> Toast.makeText(MyApplication.getContext(), "保存失败，当前无网络", Toast.LENGTH_LONG).show());
        }


    }
}

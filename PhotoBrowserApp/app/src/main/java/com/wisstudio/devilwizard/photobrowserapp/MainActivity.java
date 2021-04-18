package com.wisstudio.devilwizard.photobrowserapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.wisstudio.devilwizard.photobrowserapp.cache.disk.FileCache;
import com.wisstudio.devilwizard.photobrowserapp.cache.memory.MemoryCache;
import com.wisstudio.devilwizard.photobrowserapp.db.PhotoDataBaseHelper;
import com.wisstudio.devilwizard.photobrowserapp.db.PhotoDataBaseManager;
import com.wisstudio.devilwizard.photobrowserapp.download.ImageDownLoader;
import com.wisstudio.devilwizard.photobrowserapp.util.ImageLoader;
import com.wisstudio.devilwizard.photobrowserapp.util.MyAdapter;
import com.wisstudio.devilwizard.photobrowserapp.util.MyApplication;
import com.wisstudio.devilwizard.photobrowserapp.util.MyImage;
import com.wisstudio.devilwizard.photobrowserapp.util.NetWork.HttpCallBackListener;
import com.wisstudio.devilwizard.photobrowserapp.util.NetWork.HttpRequest;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements HttpCallBackListener<List<MyImage>> {

    public int page = 1;//图片url的页数，page不同请求回的图片也不同
    public static PhotoDataBaseManager photoDBManager;
    private static final int PHOTO_DB_VERESION = 1;
    private static final int STORAGE_REQUEST_CODE = 1;//权限请求码
    /**
     * 一次请求的默认图片张数
     */
    private static final int PAGE_PER_PHOTOS = 8;
    private static final String TAG = "MainActivity";
    private static final String PHOTO_JSON_HEAD = "https://picsum.photos/v2/list?page=";
    private static final String PHOTO_JSON_REAR = "&limit=8";//默认一次initImageList请求加载8张图片
    private static final String PHOTO_DB_NAME = "PhotoDataBase.db";
    private static MainActivity mainActivity;


    private final Object loadLock = new Object();//使page自增操作同步

    private List<MyImage> myImageList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private TextView firstTimeLoadingTips;
    private ProgressBar loadingBar;
    private MyAdapter myAdapter;
    private MemoryCache memoryCache;
    private FileCache fileCache;
    private ImageLoader imageLoader;
    private PhotoDataBaseHelper photoDataBaseHelper;
    private ImageDownLoader imageDownLoader;
    private int maxMemory;//应用占用的最大内存，以字节B为单位
    private int maxThread;//应用分配到的线程数

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainActivity = this;
        showPermissionDialog();
        maxMemory = (int) Runtime.getRuntime().maxMemory();
        maxThread = Runtime.getRuntime().availableProcessors();
        photoDataBaseHelper = new PhotoDataBaseHelper(this, PHOTO_DB_NAME, null, PHOTO_DB_VERESION);
        photoDBManager = new PhotoDataBaseManager(photoDataBaseHelper);
        imageDownLoader = new ImageDownLoader();
        firstTimeLoadingTips = (TextView) findViewById(R.id.firstTimeLoadingTips);
        loadingBar = (ProgressBar) findViewById(R.id.loadingBar);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);
        recyclerView = (RecyclerView) findViewById(R.id.image_recyclerview);
        initSwipeRefreshLayout(swipeRefreshLayout);
        initRecyclerView(recyclerView);//初始化recyclerview
        initImageList(getDifferentJson());//异步请求图片
        //Log.d(TAG, "onCreate: "+ "rootview token: "+ rootView.getWindowToken());
    }


    /**
     * 初始化下拉刷新的视图同时为其设置监听器
     *
     * @param swipeRefreshLayout
     */
    private void initSwipeRefreshLayout(SwipeRefreshLayout swipeRefreshLayout) {
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.design_default_color_primary));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshPage();
            }
        });
    }


    /**
     * 初始化主页面的recyclerView，并完成相关操作
     *
     * @param recyclerView
     *
     */
    private void initRecyclerView(RecyclerView recyclerView) {
        memoryCache = new MemoryCache(maxMemory / 8);
        fileCache = new FileCache(this);
        imageLoader = ImageLoader.getInstance(memoryCache, fileCache, 2*maxThread + 1);
        recyclerView = (RecyclerView) findViewById(R.id.image_recyclerview);
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(2,
                StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(staggeredGridLayoutManager);
        myAdapter = new MyAdapter(myImageList);
        recyclerView.setAdapter(myAdapter);
        ((DefaultItemAnimator)recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);//解决图片闪烁问题
        addListenerForRecyclerView(recyclerView);//初始化滑动监听器
    }

    /**
     * 用于请求包含图片的json网址，每调用一次其网址变化一次，防止加载重复图片
     *
     * @return java.lang.String
     *
     */
    private String getDifferentJson(){
        synchronized (loadLock) {//保证线程安全
            String jsonUrl = PHOTO_JSON_HEAD + page + PHOTO_JSON_REAR;
            page++; //防止加载重复图片
            return jsonUrl;
        }
    }

    private void refreshPage() {
        int randomPage = (int)(page + 1 + 99 * Math.random());//防止刷新到已加载的图片
        String randomJsonUrl = PHOTO_JSON_HEAD + randomPage + PHOTO_JSON_REAR;
        HttpRequest.getJson(randomJsonUrl, new HttpCallBackListener<List<MyImage>>() {
            @Override
            public void onFinish(List<MyImage> response) {
                for (MyImage image : response) {
                    myImageList.add(0, image);
                }
                MainActivity.getMainActivity().runOnUiThread(() -> {
                    myAdapter.notifyItemRangeChanged(0, PAGE_PER_PHOTOS);
                    recyclerView.smoothScrollToPosition(0);
                });
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 开启子线程异步请求图片json来往RecyclerView中加载图片资源
     *
     * @return void
     *
     */
    private void initImageList(String imgJsonUrl) {
        if (!ImageLoader.isNetworkConnected(this)) {    //无网络时直接读取缓存
            List<MyImage> cachedImages = photoDBManager.selectAllPhoto();
            for (MyImage myImage : cachedImages) {
                myImageList.add(myImage);
                myAdapter.notifyItemInserted(myImageList.size() - 1);
            }
            loadingBar.setVisibility(View.GONE);
            firstTimeLoadingTips.setVisibility(View.GONE);
            //读取本地缓存后add进适配器
        } else {
            HttpRequest.getJson(imgJsonUrl, this);
        }

    }

    /**
     * 为recyclerView设置自定义滑动监听器
     *
     * @param recyclerView 要设置滑动监听的RecyclerView
     */
    private void addListenerForRecyclerView(RecyclerView recyclerView) {

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private boolean isSlidingUpward = false;
            private int[] lastPositions = null;
            private int lastCompletelyVisibleItemPosition = 0;
            private StaggeredGridLayoutManager manager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
            private ImageLoader imageLoader = ImageLoader.getInstance();

            //当滑到底部时，开始加载下一页图片
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                //manager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_IDLE:
                        imageLoader.resume();
                        int visibleItemCount = manager.getChildCount();
                        int totalItemCount = manager.getItemCount();
                        manager.findLastCompletelyVisibleItemPositions(lastPositions);
                        lastCompletelyVisibleItemPosition = findMax(lastPositions) + 1;   //需要+1，因为position是从0开始算起的
                        Log.d(TAG, "onScrollStateChanged: " + "lastCompletelyVisibleItemPosition: " + lastCompletelyVisibleItemPosition);
                        Log.d(TAG, "onScrollStateChanged: " + "totalItemCount: " + totalItemCount);
                        if (isSlidingUpward && visibleItemCount > 0 && lastCompletelyVisibleItemPosition >= (totalItemCount-1)) {
                            if (ImageLoader.isNetworkConnected(MyApplication.getContext())) {//设备联网时才提供加载更多功能
                                myAdapter.setLoadState(MyAdapter.LOADING);
                                loadMoreImg();
                                recyclerView.smoothScrollToPosition(totalItemCount);
                            }
                        }
                        break;
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                    case RecyclerView.SCROLL_STATE_SETTLING:
                        imageLoader.pause();
                        break;
                    default:
                        break;
                }
                //滑到底部时显示“加载更多”视图同时加载图片
                //如何实现滑动时不加载，停下来才加载图片
            }

            //用于判断recyclerView是否在向上滑动，同时不断获取屏幕最底部的视图位置数组lastPositions
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //判断recyclerView是否在向上滑动，若只是手指向上滑动而整个视图没动，isSlidingUpward仍为false
                if (dy > 0) {
                    isSlidingUpward = true;
                } else {
                    isSlidingUpward = false;
                }
                //manager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
                if (lastPositions == null) {
                    lastPositions = new int[manager.getSpanCount()];
                }

            }

            //找到屏幕最底最右的视图位置
            private int findMax(int[] lastPositions) {
                int max = lastPositions[0];
                for (int value : lastPositions) {
                    if (value > max) {
                        max = value;
                    }
                }
                return max;
            }

            //当上拉到底时开始加载更多图片进页面
            private void loadMoreImg(){
                //性能较差需要优化
                initImageList(getDifferentJson());
            }
        });
    }

    /**
     * 弹出提示用户授权权限的对话框
     */
    private void showPermissionDialog() {//只应当在用户未授权相应权限时显示
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("储存权限访问申请")
                  .setMessage("为了顺利保存图片，请您在接下来的对话框中选择“允许储存访问”");
            dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            STORAGE_REQUEST_CODE);
                }
            });
            dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    forceKillApp();
                }
            });
            dialog.create().show();
        }
    }

    /**
     * 强制退出app
     */
    private void forceKillApp() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * 请求完图片的json后回调的onFinish函数
     *
     * @param response  响应回来的MyImage列表对象
     *
     * @return void
     *
     * @exception
     */
    @Override
    public void onFinish(List<MyImage> response) {
        for (MyImage myImage : response) {
            myImageList.add(myImage);
            Log.d(TAG, "onFinish: " + myImage);
        }
        MainActivity.getMainActivity().runOnUiThread(() -> {
            loadingBar.setVisibility(View.GONE);
            firstTimeLoadingTips.setVisibility(View.GONE);
            myAdapter.setLoadState(MyAdapter.LOAD_FINISHED);
            //myAdapter.notifyItemRangeChanged(0, myImageList.size());//尝试解决图片闪烁问题，暂时失败
            myAdapter.notifyItemInserted(myImageList.size() - 1);
        });

        Log.d(TAG, "onFinish: myImageList size" + myImageList.size());
//        try {
//            httpRequest.setBitmap(response, new HttpCallBackListener<MyImage>() {
//                @Override
//                public void onFinish(MyImage response) {
//                    runOnUiThread(() -> {
//                        myImageList.add(response);
//                        loadingBar.setVisibility(View.GONE);
//                        firstTimeLoadingTips.setVisibility(View.GONE);
//                        myAdapter.setLoadState(MyAdapter.LOAD_FINISHED);
//                        myAdapter.notifyItemInserted(myImageList.size() - 1);
//                    });
//                }
//                @Override
//                public void onError(Exception e) {
//                    e.printStackTrace();
//                }
//            });
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }
   }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case STORAGE_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "取消授权将导致图片无法下载", Toast.LENGTH_LONG);
                    forceKillApp();
                    //需要用户授权权限
                    finish();
                }
                break;
        }
    }

    public static MainActivity getMainActivity() {
        Log.d(TAG, "getMainActivity: mainactivity instance : " + mainActivity);
        return mainActivity;
    }

    public ImageDownLoader getImageDownLoader() {
        return imageDownLoader;
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        imageLoader.release();
        photoDBManager.closeDataBase();
    }
}
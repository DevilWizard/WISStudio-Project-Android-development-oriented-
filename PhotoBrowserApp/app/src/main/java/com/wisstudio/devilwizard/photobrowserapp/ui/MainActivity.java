package com.wisstudio.devilwizard.photobrowserapp.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.wisstudio.devilwizard.photobrowserapp.R;
import com.wisstudio.devilwizard.photobrowserapp.cache.disk.FileCache;
import com.wisstudio.devilwizard.photobrowserapp.cache.memory.MemoryCache;
import com.wisstudio.devilwizard.photobrowserapp.db.PhotoDataBaseHelper;
import com.wisstudio.devilwizard.photobrowserapp.db.PhotoDataBaseManager;
import com.wisstudio.devilwizard.photobrowserapp.util.MyApplication;
import com.wisstudio.devilwizard.photobrowserapp.util.image.MyImage;
import com.wisstudio.devilwizard.photobrowserapp.util.image.download.ImageDownLoader;
import com.wisstudio.devilwizard.photobrowserapp.util.image.load.ImageLoader;
import com.wisstudio.devilwizard.photobrowserapp.util.logutil.MyLog;
import com.wisstudio.devilwizard.photobrowserapp.util.network.HttpCallBackListener;
import com.wisstudio.devilwizard.photobrowserapp.util.network.HttpRequest;
import com.wisstudio.devilwizard.photobrowserapp.util.network.NetWorkState;

import java.util.ArrayList;
import java.util.List;

/**
 * 浏览页面的主活动
 */
public class MainActivity extends AppCompatActivity implements HttpCallBackListener<List<MyImage>> {


    public static PhotoDataBaseManager photoDBManager;
    private static MainActivity mainActivity;

    private static final String TAG = "MainActivity";
    private static final String PHOTO_JSON_HEAD = "https://picsum.photos/v2/list?page=";
    private static final String PHOTO_JSON_REAR = "&limit=8";//默认一次initImageList请求加载8张图片
    private static final String PHOTO_DB_NAME = "PhotoDataBase.db";

    /**
     * 储存图片信息的数据库版本
     */
    private static final int PHOTO_DB_VERSION = 1;

    /**
     * 储存权限请求码
     */
    private static final int STORAGE_REQUEST_CODE = 1;

    /**
     * 一次请求的默认图片张数
     */
    private static final int PAGE_PER_PHOTOS = 8;

    private ActionBar actionBar;
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

    private int page = 1;//图片url的页数，page不同请求回的图片也不同
    private final Object loadLock = new Object();//使page自增操作同步
    private final List<MyImage> myImageList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainActivity = this;
        showPermissionDialog();//第一次启动应用时进行弹窗请求相应权限
        maxMemory = (int) Runtime.getRuntime().maxMemory();
        maxThread = Runtime.getRuntime().availableProcessors();

        photoDataBaseHelper = new PhotoDataBaseHelper(this, PHOTO_DB_NAME, null, PHOTO_DB_VERSION);
        photoDBManager = new PhotoDataBaseManager(photoDataBaseHelper);
        imageDownLoader = new ImageDownLoader();
        memoryCache = new MemoryCache(maxMemory / 8);
        fileCache = new FileCache(this);
        imageLoader = ImageLoader.getInstance(memoryCache, fileCache, 2*maxThread + 1);

        firstTimeLoadingTips = findViewById(R.id.firstTimeLoadingTips);
        loadingBar = findViewById(R.id.loadingBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        recyclerView = findViewById(R.id.image_recyclerview);
        actionBar = getSupportActionBar();

        addDoubleClickToHome(actionBar);
        initSwipeRefreshLayout(swipeRefreshLayout);
        initRecyclerView(recyclerView);//初始化recyclerview
        initImageList(getDifferentJson());//异步请求图片
    }

    /**
     * 自定义顶部actionbar，同时增加双击回到顶部事件
     *
     * @param actionBar 要自定义视图和事件的actionbar
     */
    private void addDoubleClickToHome(ActionBar actionBar) {
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        ViewGroup actionBarRootView = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.my_actionbar_rootview, null);
        View myActionBarView = LayoutInflater.from(this).inflate(R.layout.my_actionbar_contentview, actionBarRootView, true);
        actionBar.setCustomView(myActionBarView);
        TextView actionBarTextView = myActionBarView.findViewById(R.id.actionBar_title);
        //添加双击回到顶部的事件
        actionBarTextView.setOnTouchListener(new View.OnTouchListener() {
            int count = 0;
            long firstClick = 0;
            long secondClick = 0;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(MotionEvent.ACTION_DOWN == event.getAction()){
                    count++;
                    if(count == 1){
                        firstClick = System.currentTimeMillis();

                    } else if (count == 2){
                        secondClick = System.currentTimeMillis();
                        if(secondClick - firstClick < 1000){
                            recyclerView.scrollToPosition(0);
                        }
                        count = 0;
                        firstClick = 0;
                        secondClick = 0;
                    }
                }
                return true;

            }

        });
    }

    /**
     * 初始化下拉刷新的视图同时为其设置监听器
     *
     * @param swipeRefreshLayout 要初始化的{@link SwipeRefreshLayout}
     */
    private void initSwipeRefreshLayout(SwipeRefreshLayout swipeRefreshLayout) {
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.design_default_color_primary));
        //设置下拉刷新事件
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
     */
    private void initRecyclerView(RecyclerView recyclerView) {
        recyclerView = findViewById(R.id.image_recyclerview);
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(2,
                StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(staggeredGridLayoutManager);
        myAdapter = new MyAdapter(myImageList);
        recyclerView.setAdapter(myAdapter);//初始化数组适配器
        addListenerForRecyclerView(recyclerView);//初始化滑动监听器
    }

    /**
     * 用于请求包含图片的json网址，每调用一次其网址变化一次，防止加载重复图片
     *
     * @return 返回包含 {@link #PAGE_PER_PHOTOS}张图片个数json的url
     *
     */
    private String getDifferentJson(){
        synchronized (loadLock) {//保证线程安全
            String jsonUrl = PHOTO_JSON_HEAD + page + PHOTO_JSON_REAR;
            page++; //防止加载重复图片
            return jsonUrl;
        }
    }

    /**
     * 下拉刷新的加载事件
     */
    private void refreshPage() {
        if (NetWorkState.isNetworkConnected(this)) {
            int randomPage = (int) (page + 1 + 99 * Math.random());//防止刷新到已加载的图片
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
        } else {
            Toast.makeText(this, "刷新失败，请检查网络是否连接", Toast.LENGTH_LONG).show();
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    /**
     * 开启子线程异步请求图片json来往RecyclerView中加载图片资源
     *
     * @param imgJsonUrl 要加载的图片集的Json的网址
     */
    private void initImageList(String imgJsonUrl) {
        if (!NetWorkState.isNetworkConnected(this)) {    //无网络时直接读取缓存
            List<MyImage> cachedImages = photoDBManager.selectAllPhoto();
            for (MyImage myImage : cachedImages) {
                myImageList.add(myImage);//读取本地缓存后add进适配器
                myAdapter.notifyItemInserted(myImageList.size() - 1);
            }
            loadingBar.setVisibility(View.GONE);
            firstTimeLoadingTips.setVisibility(View.GONE);
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
            private final StaggeredGridLayoutManager manager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
            private final ImageLoader imageLoader = ImageLoader.getInstance();

            //当滑到底部时，开始加载下一页图片
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_IDLE:
                        imageLoader.resume();
                        int visibleItemCount = manager.getChildCount();
                        int totalItemCount = manager.getItemCount();
                        manager.findLastCompletelyVisibleItemPositions(lastPositions);
                        lastCompletelyVisibleItemPosition = findMax(lastPositions) + 1;   //需要+1，因为position是从0开始算起的
                        MyLog.d(TAG, "onScrollStateChanged: " + "lastCompletelyVisibleItemPosition: " + lastCompletelyVisibleItemPosition);
                        MyLog.d(TAG, "onScrollStateChanged: " + "totalItemCount: " + totalItemCount);
                        if (isSlidingUpward && visibleItemCount > 0 && lastCompletelyVisibleItemPosition >= (totalItemCount-1)) {
                            if (NetWorkState.isNetworkConnected(MyApplication.getContext())) {//设备联网时才提供加载更多功能
                                myAdapter.setLoadState(MyAdapter.LOADING);
                                loadMoreImg();//滑到底部时显示“加载更多”视图同时加载图片
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

            }

            //用于判断recyclerView是否在向上滑动，同时不断获取屏幕最底部的视图位置数组lastPositions
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //判断recyclerView是否在向上滑动，若只是手指向上滑动而整个视图没动，isSlidingUpward仍为false
                isSlidingUpward = dy > 0;

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
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("储存权限访问申请")
                  .setMessage("为了顺利保存图片，请您在接下来的对话框中选择“允许储存访问”")
                  .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                          ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                  STORAGE_REQUEST_CODE);
                      }})
                  .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                          forceKillApp();
                      }});

            dialog.create().show();
        }
    }

    /**
     * 请求完图片的json后回调的onFinish函数（请求成功的情况）
     *
     * @param response 响应回来的MyImage列表对象
     *
     */
    @Override
    public void onFinish(List<MyImage> response) {
        for (MyImage myImage : response) {
            myImageList.add(myImage);
            MyLog.d(TAG, "onFinish: " + myImage);
        }
        MainActivity.getMainActivity().runOnUiThread(() -> {
            loadingBar.setVisibility(View.GONE);
            firstTimeLoadingTips.setVisibility(View.GONE);
            myAdapter.notifyItemRangeChanged(myImageList.size() - 1, PAGE_PER_PHOTOS);
            myAdapter.setLoadState(MyAdapter.LOAD_FINISHED);
        });
        MyLog.d(TAG, "onFinish: myImageList size" + myImageList.size());
   }

    /**
     * 请求图片的json后回调的onError函数（请求失败的情况）
     *
     * @param e 请求失败捕获的异常
     *
     */
    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }

    /**
     * 通过杀死进程强制退出app，此函数应慎用
     */
    private void forceKillApp() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

   //第一次安装应用时请求储存权限使用授权
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

    /**
     * 返回主页面的Activity实例
     * @return 返回主页面的Activity实例
     */
    public static MainActivity getMainActivity() {
        MyLog.d(TAG, "getMainActivity: mainactivity instance : " + mainActivity);
        return mainActivity;
    }

    public ImageDownLoader getImageDownLoader() {
        return imageDownLoader;
    }

    //当按返回键退出应用时不会将其销毁
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        imageLoader.release();
        photoDBManager.closeDataBase();
    }
}

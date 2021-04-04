package com.example.photobrowserapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements HttpCallBackListener<List<MyImage>>{


    public int page = 0;//图片url的页数，page不同请求回的图片也不同
    private static final String TAG = "MainActivity";
    private static final String PHOTO_JSON_HEAD = "https://picsum.photos/v2/list?page=";
    private static final String PHOTO_JSON_REAR = "&limit=8";
    private List<MyImage> myImageList = new ArrayList<>();
    private RecyclerView recyclerView;
    private TextView firstTimeLoadingTips;
    private ProgressBar loadingBar;
    private MyAdapter myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initImageList(getDifferentJson());//异步请求图片
        firstTimeLoadingTips = (TextView) findViewById(R.id.firstTimeLoadingTips);
        loadingBar = (ProgressBar) findViewById(R.id.loadingBar);
        recyclerView = (RecyclerView) findViewById(R.id.image_recyclerview);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            private boolean isSlidingUpward = false;
            private int[] lastPositions = null;
            private int lastCompletelyVisibleItemPosition = 0;

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                StaggeredGridLayoutManager manager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int visibleItemCount = manager.getChildCount();
                    Log.d(TAG, "onScrollStateChanged: visibleItemCount" + visibleItemCount);
                    int totalItemCount = manager.getItemCount();
                    Log.d(TAG, "onScrollStateChanged: totalItemCount" + totalItemCount);
                    Log.d(TAG, "onScrollStateChanged: lastVisibleItemPosition" + lastCompletelyVisibleItemPosition);
                    if (isSlidingUpward && visibleItemCount > 0 && lastCompletelyVisibleItemPosition >= (totalItemCount-1)) {
                        myAdapter.setLoadState(MyAdapter.LOADING);
                        Log.d(TAG, "onScrollStateChanged: 滑动到底了" );
                        loadMoreImg();

                    }

                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //判断recyclerView是否在向上滑动，若只是手指向上滑动而整个视图没动，isSlidingUpward仍为false
                //改为只要检测到手指向上滑动就为true比较合理，待修改
                if (dy > 0) {
                    isSlidingUpward = true;
                } else {
                    isSlidingUpward = false;
                }
                StaggeredGridLayoutManager manager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();

                if (lastPositions == null) {
                    lastPositions = new int[manager.getSpanCount()];
                }
                for (int position : lastPositions) {
                    Log.d(TAG, "onScrolled: " + position);
                }
                manager.findLastCompletelyVisibleItemPositions(lastPositions);
                lastCompletelyVisibleItemPosition = findMax(lastPositions) + 1;   //需要+1，因为position是从0开始算起的

            }

            private int findMax(int[] lastPositions) {
                int max = lastPositions[0];
                for (int value : lastPositions) {
                    if (value > max) {
                        max = value;
                    }
                }
                return max;
            }

            private void loadMoreImg(){
                initImageList(getDifferentJson());

            }

        });
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(2,
                StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(staggeredGridLayoutManager);
        myAdapter = new MyAdapter(myImageList);
        recyclerView.setAdapter(myAdapter);



    }

    /**
     * 用于请求包含图片的json网址，每请调用一次其网址变化一次，防止加载重复图片
     *
     * @return java.lang.String
     *
     * @exception
     */
    private String getDifferentJson(){
        String jsonUrl = PHOTO_JSON_HEAD + page + PHOTO_JSON_REAR;
        page++; //防止加载重复图片
        return jsonUrl;
    }

    /**
     * 在页面开始渲染时开启子线程异步请求图片json来往RecyclerView中添加图片资源
     *
     * @return void
     *
     * @exception
     */
    private void initImageList(String imgJsonUrl) {
        HttpRequest.getJson(imgJsonUrl, this);
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
        HttpRequest httpRequest = new HttpRequest();

        try {
            httpRequest.setBitmap(response, new HttpCallBackListener<MyImage>() {
                @Override
                public void onFinish(MyImage response) {
                    runOnUiThread(() -> {
                        myImageList.add(response);
                        loadingBar.setVisibility(View.GONE);
                        firstTimeLoadingTips.setVisibility(View.GONE);
                        myAdapter.setLoadState(MyAdapter.LOAD_FINISHED);
                        //myAdapter.notifyItemInserted(myImageList.size() - 1);
                        myAdapter.notifyDataSetChanged();
                    });
                }
                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }
}
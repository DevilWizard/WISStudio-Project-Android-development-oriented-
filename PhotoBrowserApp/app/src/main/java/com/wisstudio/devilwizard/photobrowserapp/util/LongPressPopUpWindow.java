package com.wisstudio.devilwizard.photobrowserapp.util;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

import com.wisstudio.devilwizard.photobrowserapp.R;

/**
 * 长按弹出窗口的自定义组件类
 * @author WizardK
 * @date 2021-04-17
 */
public class LongPressPopUpWindow extends PopupWindow {

    private static final String TAG = "LongPressPopUpWindow";
    public static final int IMAGEVIEW_POPUP_WINDOW = 1;
    private Context context;
    private View popUpView;
    /**
     * 弹出窗口的类型
     */
    private int popUpType;

    /**
     * 初始化要显示的窗口类型
     * @param popUpType 要显示的窗口类型
     */
    public LongPressPopUpWindow(Context context, int popUpType) {
        super(context);
        this.context = context;
        this.popUpType = popUpType;
        initPopUpView();

    }

    /**
     * 根据不同的{@link #popUpType}来初始化相应的PopUpView
     */
    private void initPopUpView() {

        switch (popUpType) {
            case IMAGEVIEW_POPUP_WINDOW:
                popUpView = LayoutInflater.from(context).inflate(R.layout.downloadview, null);
                popUpView.measure(0, 0);//必须先measure再调用getMeasured方法，若直接调用getWidth方法得到的始终是0
                break;
        }
        setContentView(popUpView);
        setHeight(popUpView.getMeasuredHeight());
        setWidth(popUpView.getMeasuredWidth());
        Log.d(TAG, "initPopUpView: " + "hieght: " + popUpView.getMeasuredHeight() + "width: " + popUpView.getMeasuredWidth());
        setOutsideTouchable(true);
    }

    public View getPopUpView() {
        return popUpView;
    }
}

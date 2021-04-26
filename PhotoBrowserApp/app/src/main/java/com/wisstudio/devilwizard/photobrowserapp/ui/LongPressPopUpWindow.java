package com.wisstudio.devilwizard.photobrowserapp.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

import com.wisstudio.devilwizard.photobrowserapp.R;
import com.wisstudio.devilwizard.photobrowserapp.util.logutil.MyLog;

/**
 * 长按弹出窗口的自定义组件类
 *
 * @author WizardK
 * @date 2021-04-17
 */
public class LongPressPopUpWindow extends PopupWindow {

    /**
     * 长按图片的弹出菜单类型
     */
    public static final int IMAGEVIEW_POPUP_WINDOW = 1;

    private static final String TAG = "LongPressPopUpWindow";

    private final Context context;
    private View popUpView;

    /**
     * 弹出窗口的类型
     */
    private final int popUpType;

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
     * 返回当前的弹窗实例
     * @return 返回弹出窗口的 {@link View}实例
     */
    public View getPopUpView() {
        return popUpView;
    }

    /**
     * 根据不同的{@link #popUpType}来初始化相应的PopUpView，有如下的弹窗类型：{@link #IMAGEVIEW_POPUP_WINDOW}
     */
    private void initPopUpView() {

        switch (popUpType) {
            case IMAGEVIEW_POPUP_WINDOW:
                popUpView = LayoutInflater.from(context).inflate(R.layout.long_press_image_pop_up_view, null);
                popUpView.measure(0, 0);//必须先measure再调用getMeasured方法，若直接调用getWidth方法得到的始终是0
                break;
        }

        setContentView(popUpView);
        setHeight(popUpView.getMeasuredHeight());
        setWidth(popUpView.getMeasuredWidth());
        MyLog.d(TAG, "initPopUpView: " + "hieght: " + popUpView.getMeasuredHeight() + "width: " + popUpView.getMeasuredWidth());
        setOutsideTouchable(true);//点击外部可使弹窗消失
        setFocusable(true);//防止在点击外部使菜单消失后同时误触其他地方
    }

}

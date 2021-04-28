package com.wisstudio.devilwizard.photobrowserapp.util.network;

/**
 * 网络请求结束的回调接口
 *
 * @author WizardK
 * @date 2021-04-17
 */
public interface HttpCallBackListener<T> {

    /**
     * 请求成功的回调操作
     * @param response 请求成功的响应
     */
    void onFinish(T response);

    /**
     * 请求失败的回调操作
     */
    void onFailed();

    /**
     *  请求过程若发生异常的回调操作
     * @param e 发生的具体异常
     */
    void onError(Exception e);
}

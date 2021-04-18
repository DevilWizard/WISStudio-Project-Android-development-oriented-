package com.wisstudio.devilwizard.photobrowserapp.util.NetWork;

/**
 * 网络请求结束的回调接口
 *
 * @author WizardK
 * @date 2021-04-17
 */
public interface HttpCallBackListener<T> {
    void onFinish(T response);
    void onError(Exception e);
}

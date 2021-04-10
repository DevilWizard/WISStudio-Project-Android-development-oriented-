package com.wisstudio.devilwizard.photobrowserapp.util;

public interface HttpCallBackListener<T> {
    void onFinish(T response);
    void onError(Exception e);
}

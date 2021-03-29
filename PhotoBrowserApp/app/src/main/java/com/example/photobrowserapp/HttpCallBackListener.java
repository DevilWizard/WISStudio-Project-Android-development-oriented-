package com.example.photobrowserapp;

public interface HttpCallBackListener<T> {
    void onFinish(T response);
    void onError(Exception e);
}

package com.example.bleconnect;

public interface OnPackageFoundCallback {

    void onPackageFoundSucceeded(byte[] reply);

    void onPackageFoundFailed(byte[] reply);
}

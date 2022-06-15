package com.example.bleconnect;

public interface OnCommandsCallback {
    void onGetFirmwareVersionSucceed(String firmwareVersion);

    void onCommandsFailed(int errorCode);
}

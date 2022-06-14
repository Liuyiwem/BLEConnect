package com.example.bleconnect;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Commands {

    private byte[] mCommandCode;
    private PackageCheck mPackageCheck;
    private String firmwareVersion;
    private OnCommandsCallback mOnCommandsCallback;
    private final static String TAG = Commands.class.getSimpleName();

    protected Commands(PackageCheck packageCheck) {
        this.mPackageCheck = packageCheck;
    }

    public void getFirmwareVersion() {

        mCommandCode = new byte[]{0x01, 0x00, 0x10, 0x01, 0x00, 0x71, 0x00};
        mPackageCheck.setOnPackageFoundCallback(new OnPackageFoundCallback() {
            @Override
            public void onPackageFoundSucceeded(byte[] reply) {
                if (mOnCommandsCallback != null) {
                    int parameterLength = reply[3];
                    try {
                        firmwareVersion = new String(reply, 4, parameterLength, "ASCII");
                        mOnCommandsCallback.onGetFirmwareVersionSucceed(firmwareVersion);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void setOnCommandsCallback(OnCommandsCallback onCommandsCallback) {
        this.mOnCommandsCallback = onCommandsCallback;
    }

    public byte[] getCommandCode() {
        Log.d(TAG, "getCommandCode: ");
        return mCommandCode;
    }

    public void remove(){
        mPackageCheck.removeData();
    }

}

package com.example.bleconnect;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PackageCheck {

    private static final String TAG = PackageCheck.class.getSimpleName();
    private static final byte HEADER_BIT = 0x01;
    private static final byte COMMAND_SUCCEED = 0x06;
    private byte[] buffer = new byte[9999];
    private int bufferLengthNow;
    private int packetLength;
    private byte[] reply;
    private OnPackageFoundCallback onPackageFoundCallback;
    private Lock mLock;
    private Condition mCondition;

    protected PackageCheck(Lock lock, Condition condition) {
        this.mLock = lock;
        this.mCondition = condition;
    }

    public void receiveData(BluetoothGattCharacteristic characteristic) {

        mLock.lock();
        byte[] rec;
        rec = characteristic.getValue();
        System.arraycopy(rec, 0, buffer, bufferLengthNow, rec.length);
        bufferLengthNow += rec.length;

        //Remove data
        if (buffer[0] != HEADER_BIT) {
            int newBufferLengthNow = 0;
            for (int i = 0; i < bufferLengthNow; i++) {
                if (buffer[i] == HEADER_BIT) {
                    for (int j = 0; j < bufferLengthNow; j++) {
                        buffer[j] = buffer[i + j];
                    }
                    newBufferLengthNow = i;
                    break;
                }
                newBufferLengthNow = i + 1;
            }
            bufferLengthNow -= newBufferLengthNow;
        }

        //Find Header
        while (buffer[0] == HEADER_BIT) {

            //Find Parameter Length
            if (bufferLengthNow >= 4) {
                int parameterLength = buffer[3];
                packetLength = parameterLength + 6;

                //Copy Package to reply
                if (bufferLengthNow >= packetLength) {
                    reply = new byte[packetLength];
                    System.arraycopy(buffer, 0, reply, 0, packetLength);

                    //Remove copied package from buffer
                    for (int i = 0; i < bufferLengthNow; i++) {
                        buffer[i] = buffer[i + packetLength];
                    }
                    bufferLengthNow -= packetLength;
                    parameterLength = buffer[3];
                    packetLength = parameterLength + 6;

                    //Receive Succeed
                    if (reply[2] == COMMAND_SUCCEED) {
                        int crc = (((reply[parameterLength + 4] & 0xFF) << 8) + (reply[parameterLength + 5] & 0xFF));
                        GNetPlus gNetPlus = new GNetPlus();
                        int crrCheck = gNetPlus.gNetPlusCRC16(reply, 1, parameterLength + 3);
                        if (crc == crrCheck) {
                            if (onPackageFoundCallback != null) {
                                onPackageFoundCallback.onPackageFoundSucceeded(reply);
                                mCondition.signal();
                            }
                        }
                    }
                    //Receive fail
                    if (reply[2] != COMMAND_SUCCEED) {
                        if (onPackageFoundCallback != null) {
                            onPackageFoundCallback.onPackageFoundFailed(reply);
                            mCondition.signal();
                        }
                    }
                }
            }
            //Package not complete
            if (bufferLengthNow < packetLength || bufferLengthNow < 4)
                break;
        }
        mLock.unlock();
    }

    public void setOnPackageFoundCallback(OnPackageFoundCallback onPackageFoundCallback) {
        this.onPackageFoundCallback = onPackageFoundCallback;
    }

    public void removeData() {
        bufferLengthNow = 0;
        buffer = new byte[9999];
    }
}

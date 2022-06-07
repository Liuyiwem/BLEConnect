package com.example.bleconnect;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PackageCheck {

    public final static Lock lock = new ReentrantLock();
    public byte[] buffer = new byte[9999];
    public int bufferLengthNow;
    private int packetLength;
    private byte[] reply;
    public final static Condition condition = lock.newCondition();
    public OnPackageFoundCallback onPackageFoundCallback;


    public void receiveData(BluetoothGattCharacteristic characteristic) {

        lock.lock();
        byte[] rec;
        rec = characteristic.getValue();
        System.arraycopy(rec, 0, buffer, bufferLengthNow, rec.length);
        bufferLengthNow += rec.length;

        //Remove data
        if (buffer[0] != 0x01) {
            int newBufferLengthNow = 0;
            for (int i = 0; i < bufferLengthNow; i++) {
                if (buffer[i] == 0x01) {
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
        while (buffer[0] == 0x01) {

            //Find Parameter Length
            if (bufferLengthNow >= 4) {
                int parameterLength = buffer[3];
                packetLength = parameterLength + 6;

                //Copy Package to reply
                if (bufferLengthNow >= packetLength) {
                    reply = new byte[packetLength];
                    System.arraycopy(buffer, 0, reply, 0, packetLength);

                    //Remove copied package of buffer
                    for (int i = 0; i < bufferLengthNow; i++) {
                        buffer[i] = buffer[i + packetLength];
                    }
                    bufferLengthNow -= packetLength;
                    packetLength = 0;

                    if (reply[2] == 0x06) {
                        int crc = (((reply[parameterLength + 4] & 0xFF) << 8) + (reply[parameterLength + 5] & 0xFF));
                        GNetPlus gNetPlus = new GNetPlus();
                        int crrCheck = gNetPlus.gNetPlusCRC16(reply, 1, parameterLength + 3);
                        if (crc == crrCheck) {
                            onPackageFoundCallback.onPackageFoundSucceeded(reply);
                            condition.signalAll();
                        }
                    }
                }
            }
            //Package not complete
            if (bufferLengthNow < packetLength || bufferLengthNow < 4)
                break;
        }
        lock.unlock();
    }

    public void setOnPackageFoundCallback(OnPackageFoundCallback onPackageFoundCallback) {
        this.onPackageFoundCallback = onPackageFoundCallback;
    }

    public void removeData(){
        bufferLengthNow = 0;
        buffer = new byte[9999];
    }
}

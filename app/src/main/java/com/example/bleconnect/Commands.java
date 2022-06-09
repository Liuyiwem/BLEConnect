package com.example.bleconnect;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Commands extends CommandThread {
    private Thread thread;
    ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue(1);
    private byte[] msg;


    protected Commands(BluetoothGattService bluetoothGattService, BluetoothGatt bluetoothGatt, Context context, Lock lock, Condition condition,PackageCheck packageCheck) {
        super(bluetoothGattService, bluetoothGatt, context, lock, condition,packageCheck);

    }

    public void getFirmwareVersion() {

        try {
            msg = new byte[]{0x01, 0x00, 0x10, 0x01, 0x00, 0x71, 0x00};
            queue.put(msg);
            thread = new Thread(new CommandThread.Runnable(queue));
            thread.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}

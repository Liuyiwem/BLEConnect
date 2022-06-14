package com.example.bleconnect;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class CommandThread {

    private final String CHARACTERISTIC_WRITE = "0000FFF4-000-1000-8000-00805F9B34FB";
    private BluetoothGattService mBluetoothGattService;
    private BluetoothGatt mBluetoothGatt;
    private byte[] mCommandCode;
    private OnTimeOutCallback onTimeOutCallback;
    private Context mContext;
    private Lock mLock;
    private Condition mCondition;
    private final static String TAG = CommandThread.class.getSimpleName();
    private PackageCheck mPackageCheck;
    private Thread thread;
    private Commands mCommands;

    protected CommandThread(BluetoothGattService bluetoothGattService, BluetoothGatt bluetoothGatt, Context context, Lock lock, Condition condition, Commands commands) {
        this.mBluetoothGattService = bluetoothGattService;
        this.mBluetoothGatt = bluetoothGatt;
        this.mContext = context;
        this.mCondition = condition;
        this.mLock = lock;
        this.mCommands = commands;
        thread = new Thread(new Runnable());
        thread.start();
        setCallback();
    }

    public class Runnable implements java.lang.Runnable {

        Timer timer = new Timer(true);

        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            try {
                synchronized (mCommands) {
                    mLock.lockInterruptibly();
                    mCommandCode = mCommands.getCommandCode();
                    timer.schedule(new TimeOutTask(Thread.currentThread()), 5000);
                    Log.d(TAG, "run: " + Thread.currentThread().getName());
                    BluetoothGattCharacteristic whiteCharacteristic = mBluetoothGattService.getCharacteristic(UUID.fromString(CHARACTERISTIC_WRITE));
                    whiteCharacteristic.setValue(mCommandCode);
                    mBluetoothGatt.writeCharacteristic(whiteCharacteristic);
                    mCondition.await();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mLock.unlock();
            }
        }
    }

    public class TimeOutTask extends TimerTask {

        Thread mThread;

        TimeOutTask(Thread thread) {
            this.mThread = thread;
        }

        @Override
        public void run() {
            if (onTimeOutCallback != null) {
                onTimeOutCallback.TimeOutCallbackFunction();
            }
            if (mThread != null && mThread.isAlive()) {
                mThread.interrupt();
                Looper.prepare();
                Toast.makeText(mContext, "Time Out", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }
    }

    public void setOnTimeOutCallback(OnTimeOutCallback onTimeOutCallback) {
        this.onTimeOutCallback = onTimeOutCallback;
    }

    public void setCallback() {

        setOnTimeOutCallback(new OnTimeOutCallback() {
            @Override
            public void TimeOutCallbackFunction() {
                mCommands.remove();
            }
        });
    }

}

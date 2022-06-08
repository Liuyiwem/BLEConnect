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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Command {

    private BluetoothGattService mBluetoothGattService;
    private BluetoothGatt mBluetoothGatt;
    private byte[] msg;
    private OnTimeOutCallback onTimeOutCallback;
    private Context mContext;
    private Lock mLock;
    private Condition mCondition;
    private final static String TAG = Command.class.getSimpleName();
    private PackageCheck mPackageCheck;

    protected Command(BluetoothGattService bluetoothGattService, BluetoothGatt bluetoothGatt, Context context, Lock lock, Condition condition) {
        this.mBluetoothGattService = bluetoothGattService;
        this.mBluetoothGatt = bluetoothGatt;
        this.mContext = context;
        this.mCondition = condition;
        this.mLock = lock;
        mPackageCheck = new PackageCheck();

    }

    public void command() {
        ExecutorService executorService =  Executors.newSingleThreadExecutor();
        setCallback();
        executorService.execute(new Runnable() {
            Timer timer = new Timer(true);

            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                try {
                    mLock.lockInterruptibly();
                    timer.schedule(new TimeOutTask(Thread.currentThread()), 5000);
                    Log.d(TAG, "run: "+Thread.currentThread().getName());
                    BluetoothGattCharacteristic whiteCharacteristic = mBluetoothGattService.getCharacteristic(UUID.fromString("0000FFF4-000-1000-8000-00805F9B34FB"));
                    msg = new byte[]{0x01, 0x00, 0x10, 0x01, 0x00, 0x71, 0x00};
                    whiteCharacteristic.setValue(msg);
                    mBluetoothGatt.writeCharacteristic(whiteCharacteristic);
                    mCondition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mLock.unlock();
                    executorService.shutdown();
                }
            }
        });

    }

    public class TimeOutTask extends TimerTask {

        Thread t;

        TimeOutTask(Thread t) {
            this.t = t;
        }

        @Override
        public void run() {

            onTimeOutCallback.TimeOutCallbackFunction();
            if (t != null && t.isAlive()) {
                t.interrupt();
                Looper.prepare();
                Toast.makeText(mContext, "Time Out", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }
    }

    public void setOnTimeOutCallback(OnTimeOutCallback onTimeOutCallback) {
        this.onTimeOutCallback = onTimeOutCallback;
    }

    private void setCallback() {

        setOnTimeOutCallback(new OnTimeOutCallback() {
            @Override
            public void TimeOutCallbackFunction() {
                mPackageCheck.removeData();
            }
        });
    }


}

package com.example.bleconnect;

import static android.Manifest.permission.*;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.TypedArrayUtils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectedDeviceActivity extends AppCompatActivity {
    private static final String TAG = ConnectedDeviceActivity.class.getSimpleName();
    public static final String SELECTED_DEVICE = "SELECTED_DEVICE";
    private ScannedData selectedDevice;
    private TextView mDeviceName, mMacAddress, mConnectStatus, mFirmwareVersion;
    private Button mBtnConnect;
    private Button mBtnGetFirmwareVersion;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mBluetoothGattService;
    private OnPackageFoundCallback onPackageFoundCallback;
    private OnTimeOutCallback onTimeOutCallback;
    private boolean isNotified = false;
    private boolean isConnected = false;
    private byte[] buffer = new byte[9999];
    private byte[] reply;
    private int bufferLengthNow = 0;
    private int packetLength;
    private String firmwareVersion;
    private byte[] msg;
    private static Lock lock = new ReentrantLock();
    private static Condition condition = lock.newCondition();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);
        selectedDevice = (ScannedData) getIntent().getSerializableExtra(SELECTED_DEVICE);
        initViews();
        initBLE();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setViews();
        bleConnected();

    }

    private void initViews() {
        mDeviceName = findViewById(R.id.tv_Connected_Device);
        mMacAddress = findViewById(R.id.tv_Mac_Address);
        mConnectStatus = findViewById(R.id.tv_Device_Status);
        mFirmwareVersion = findViewById(R.id.tv_Device_Firmware_Version);
        mBtnConnect = findViewById(R.id.btn_Connect);
        mBtnGetFirmwareVersion = findViewById(R.id.btn_Get_Firmware_Version);
        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isConnected) {
                    bleConnected();
                } else {
                    bleDisconnected();
                }

            }
        });
        mBtnGetFirmwareVersion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isNotified && isConnected) {
                    getFirmwareVersion();
                    setOnPackageFoundCallback(new OnPackageFoundCallback() {
                        @Override
                        public void onGetFirmwareVersionSucceeded(String firmwareVersion) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mFirmwareVersion.setText(firmwareVersion);
                                }
                            });
                        }
                    });

                }
            }
        });
    }

    private void setViews() {
        mDeviceName.setText(selectedDevice.getDeviceName());
        mMacAddress.setText(selectedDevice.getAddress());
    }

    private void initBLE() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @SuppressLint("MissingPermission")
    private void bleConnected() {
        final BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(selectedDevice.getAddress());
        if (bluetoothDevice != null) {
            mBluetoothGatt = bluetoothDevice.connectGatt(this, false, mBluetoothGattCallback);
        }
    }

    @SuppressLint("MissingPermission")
    private void bleDisconnected() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
    }

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "onConnectionStateChange: " + "Connected" + newState);
                isConnected = true;
                gatt.discoverServices();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBtnConnect.setText("Disconnect");
                        mConnectStatus.setText("Connected");
                    }
                });
                return;
            }
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "onConnectionStateChange: " + "Disconnected");
                isConnected = false;
                mBluetoothGatt.close();
                isNotified = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBtnConnect.setText("Connect");
                        mConnectStatus.setText("Disconnect");
                    }
                });
                return;
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite: " + status);
            if (status == gatt.GATT_SUCCESS) {
                isNotified = true;
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite: " + status);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered: " + status);
                mBluetoothGattService = gatt.getService(UUID.fromString("0000FFF0-000-1000-8000-00805F9B34FB"));
                BluetoothGattCharacteristic mBluetoothGattCharacteristic = mBluetoothGattService.getCharacteristic(UUID.fromString("0000FFF3-000-1000-8000-00805F9B34FB"));
                Log.d(TAG, "getCharacteristics: " + mBluetoothGattCharacteristic.getDescriptors().get(0).toString());
                mBluetoothGatt.setCharacteristicNotification(mBluetoothGattCharacteristic, true);
                BluetoothGattDescriptor mDescriptor = mBluetoothGattCharacteristic.getDescriptor(UUID.fromString("00002902-000-1000-8000-00805F9B34FB"));
                mDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(mDescriptor);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "onCharacteristicChanged: " + characteristic.getValue());
            receiveData(characteristic);
        }
    };

    public void receiveData(BluetoothGattCharacteristic characteristic) {

        setOnTimeOutCallback(new OnTimeOutCallback() {

            @Override
            public void TimeOutCallbackFunction(int bufferLengthNow) {
                ConnectedDeviceActivity.this.bufferLengthNow = bufferLengthNow;
                buffer = new byte[9999];
            }
        });

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
                        try {
                            int crc = (((reply[parameterLength + 4] & 0xFF) << 8) + (reply[parameterLength + 5] & 0xFF));
                            GNetPlus gNetPlus = new GNetPlus();
                            int crrCheck = gNetPlus.gNetPlusCRC16(reply, 1, parameterLength + 3);
                            if (crc == crrCheck) {
                                firmwareVersion = new String(reply, 4, parameterLength, "ASCII");
                                onPackageFoundCallback.onGetFirmwareVersionSucceeded(firmwareVersion);
                                condition.signalAll();
                            }
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
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

    @SuppressLint("MissingPermission")
    public void getFirmwareVersion() {
        new Thread(new Runnable() {
            Timer timer = new Timer(true);

            @Override
            public void run() {
                try {
                    lock.lockInterruptibly();
                    timer.schedule(new TimeOutTask(Thread.currentThread()), 5000);
                    BluetoothGattCharacteristic whiteCharacteristic = mBluetoothGattService.getCharacteristic(UUID.fromString("0000FFF4-000-1000-8000-00805F9B34FB"));
                    msg = new byte[]{0x01, 0x00, 0x10, 0x01, 0x00, 0x71, 0x00};
                    whiteCharacteristic.setValue(msg);
                    mBluetoothGatt.writeCharacteristic(whiteCharacteristic);
                    condition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        }).start();
    }

    public void setOnPackageFoundCallback(OnPackageFoundCallback onPackageFoundCallback) {
        this.onPackageFoundCallback = onPackageFoundCallback;
    }

    public void setOnTimeOutCallback(OnTimeOutCallback onTimeOutCallback) {
        this.onTimeOutCallback = onTimeOutCallback;
    }

    public class TimeOutTask extends TimerTask {

        Thread t;
        TimeOutTask(Thread t) {
            this.t = t;
        }

        @Override
        public void run() {
            int bufferLengthNow = 0;
            onTimeOutCallback.TimeOutCallbackFunction(bufferLengthNow);

            if (t != null && t.isAlive()) {
                t.interrupt();
                Looper.prepare();
                Toast.makeText(ConnectedDeviceActivity.this, "Time Out", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }

        }
    }
}

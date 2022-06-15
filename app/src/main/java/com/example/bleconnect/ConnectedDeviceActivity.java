package com.example.bleconnect;


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
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectedDeviceActivity extends AppCompatActivity {
    private static final String TAG = ConnectedDeviceActivity.class.getSimpleName();
    private static final String BLE_SERVICE = "0000FFF0-000-1000-8000-00805F9B34FB";
    private static final String BLE_NOTIFY_CHARACTERISTIC = "0000FFF3-000-1000-8000-00805F9B34FB";
    private static final String BLE_DESCRIPTOR = "00002902-000-1000-8000-00805F9B34FB";
    public static final String SELECTED_DEVICE = "SELECTED_DEVICE";
    private ScannedData selectedDevice;
    private TextView mDeviceName, mMacAddress, mConnectStatus, mFirmwareVersion;
    private Button mBtnConnect;
    private Button mBtnGetFirmwareVersion;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mBluetoothGattService;
    private boolean isNotified = false;
    private boolean isConnected = false;
    private PackageCheck mPackageCheck;
    private final static Lock mLock = new ReentrantLock();
    private final static Condition mCondition = mLock.newCondition();
    private Commands mCommands;
    private CommandThread mCommandThread;

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
        mPackageCheck = new PackageCheck(mLock, mCondition);
    }

    @Override
    protected void onStop() {
        super.onStop();
        bleDisconnected();
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

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered: " + status);
                setDescriptor(gatt);
                mCommands = new Commands(mPackageCheck);
                setCommandsCallbacks();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "onCharacteristicChanged: " + characteristic.getValue());
            mPackageCheck.receiveData(characteristic);
        }
    };

    @SuppressLint("MissingPermission")
    private void setDescriptor(@NonNull BluetoothGatt gatt) {
        mBluetoothGattService = gatt.getService(UUID.fromString(BLE_SERVICE));
        BluetoothGattCharacteristic mBluetoothGattCharacteristic = mBluetoothGattService.getCharacteristic(UUID.fromString(BLE_NOTIFY_CHARACTERISTIC));
        Log.d(TAG, "getCharacteristics: " + mBluetoothGattCharacteristic.getDescriptors().get(0).toString());
        mBluetoothGatt.setCharacteristicNotification(mBluetoothGattCharacteristic, true);
        BluetoothGattDescriptor mDescriptor = mBluetoothGattCharacteristic.getDescriptor(UUID.fromString(BLE_DESCRIPTOR));
        mDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(mDescriptor);
    }

    private void setCommandsCallbacks(){
        mCommands.setOnCommandsCallback(new OnCommandsCallback() {
            @Override
            public void onGetFirmwareVersionSucceed(String firmwareVersion) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mFirmwareVersion.setText(firmwareVersion);
                    }
                });
            }

            @Override
            public void onCommandsFailed(int errorCode) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mFirmwareVersion.setText(String.valueOf(errorCode));
                    }
                });
            }
        });
    }

    private void getFirmwareVersion(){
        mCommands.getFirmwareVersion();
        mCommandThread = new CommandThread(mBluetoothGattService, mBluetoothGatt, mLock, mCondition, mCommands);
        setCommandTimeoutCallback();
    }

    private void setCommandTimeoutCallback(){
        if (mCommandThread != null) {
            mCommandThread.setOnTimeOutCallback(new OnTimeOutCallback() {
                @Override
                public void TimeOutCallbackFunction() {
                    mPackageCheck.removeData();
                    Looper.prepare();
                    Toast.makeText(ConnectedDeviceActivity.this, "Time Out", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
            });
        }

    }

}

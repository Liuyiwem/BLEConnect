package com.example.bleconnect;

import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final int REQUEST_FINE_LOCATION_PERMISSION = 102;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_BLUETOOTH_SCAN = 3;
    private boolean isScanning = false;
    private Handler mHandler;
    private int SCAN_PERIOD = 10000;
    public ArrayList<ScannedData> device = new ArrayList<>();
    private ScannedDeviceAdapter mAdapter;
    private ScanCallback scanCallback;
    private Button mBtnScan;
    private BluetoothLeScanner mBluetoothScanner;
    private List<ScanFilter> scanFilters = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        initView();
        mHandler = new Handler();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setScanCallback();
        bleScan();
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onStop() {
        super.onStop();
        mBluetoothScanner.stopScan(scanCallback);
        mAdapter.clearDevice();
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int hasGone = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasGone != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_FINE_LOCATION_PERMISSION);
            }
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this, "Not support Bluetooth", Toast.LENGTH_SHORT).show();
                finish();
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.BLUETOOTH_SCAN},
                        REQUEST_BLUETOOTH_SCAN);
            }
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else finish();
    }

    private void bleScan() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBtnScan.setOnClickListener(new View.OnClickListener() {

            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                if (!isScanning) {
                    mHandler.postDelayed(new Runnable() {
                        @SuppressLint("MissingPermission")
                        @Override
                        public void run() {
                            isScanning = false;
                            mBluetoothScanner.stopScan(scanCallback);
                            mBtnScan.setText("start");
                        }
                    }, SCAN_PERIOD);
                    ScanSettings scanSettings = new ScanSettings.Builder()
                            .setScanMode(SCAN_MODE_LOW_LATENCY)
                            .build();
                    scanFilters.add(new ScanFilter.Builder()
                            .setServiceUuid(new ParcelUuid(UUID.fromString("0000FFF0-000-1000-8000-00805F9B34FB")))
                            .build());


                    isScanning = true;
                    mAdapter.clearDevice();
                    mBluetoothScanner.startScan(scanFilters,scanSettings,scanCallback);
                    mBtnScan.setText("stop");

                } else {
                    mBluetoothScanner.stopScan(scanCallback);
                    isScanning = false;
                    mBtnScan.setText("start");
                }
            }
        });
    }

    private void initView() {
        mBtnScan = findViewById(R.id.btn_scan);
        RecyclerView mRecyclerView = findViewById(R.id.recyclerView_Device);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new ScannedDeviceAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener(new ScannedDeviceAdapter.OnItemClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onItemClick(ScannedData selectedDevice) {
                Intent intent = new Intent(MainActivity.this, ConnectedDeviceActivity.class);
                intent.putExtra(ConnectedDeviceActivity.SELECTED_DEVICE, selectedDevice);
                startActivity(intent);
                mBluetoothScanner.stopScan(scanCallback);
                isScanning = false;
                mBtnScan.setText("start");
            }
        });
    }

    private void setScanCallback() {
        new Thread(() -> {
            scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    String Device_name = result.getScanRecord().getDeviceName();
                    if (Device_name == null) {
                        Device_name = "N/A";
                    }
                    Log.d(TAG, "onScanResult: "+Device_name);
                    device.add(new ScannedData(Device_name,
                            String.valueOf(result.getRssi()),
                            String.valueOf(result.getDevice())));
                    ArrayList newList = getSingle(device);
                    runOnUiThread(() -> {
                        mAdapter.addDevice(newList);
                    });
                }

            };
        }).start();
    }

    private ArrayList getSingle(ArrayList list) {
        ArrayList tempList = new ArrayList<>();
        try {
            Iterator it = list.iterator();
            while (it.hasNext()) {
                Object obj = it.next(); //Output the first item of list
                if (!tempList.contains(obj)) {  //Contain obj return true
                    tempList.add(obj);
                } else {
                    tempList.set(getIndex(tempList, obj), obj);
                }
            }
            return tempList;
        } catch (ConcurrentModificationException e) {
            return tempList;
        }
    }

    private int getIndex(ArrayList temp, Object obj) {
        for (int i = 0; i < temp.size(); i++) {
            if (temp.get(i).toString().contains(obj.toString())) {
                return i;
            }
        }
        return -1;
    }

}
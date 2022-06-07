package com.example.bleconnect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

public class ScannedData implements Serializable {
    private String deviceName;
    private String rssi;
    private String address;

    public ScannedData(String deviceName, String rssi, String address){
        this.deviceName = deviceName;
        this.rssi = rssi;
        this.address = address;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getRssi() {
        return rssi;
    }

    public String getAddress() {
        return address;
    }
    @Override
    public boolean equals(@Nullable Object obj) {
        ScannedData p = (ScannedData)obj;

        return this.address.equals(p.address);
    }

    @NonNull
    @Override
    public String toString() {
        return this.address;
    }
}

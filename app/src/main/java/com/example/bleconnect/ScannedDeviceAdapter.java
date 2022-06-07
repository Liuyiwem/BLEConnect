package com.example.bleconnect;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ScannedDeviceAdapter extends RecyclerView.Adapter<ScannedDeviceAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<ScannedData> mDevice;
    private OnItemClickListener listener;

    public ScannedDeviceAdapter(Context context) {
        mContext = context;
        mDevice = new ArrayList<>();
    }

    synchronized void addDevice(ArrayList<ScannedData> mDevice) {
        this.mDevice = mDevice;
        int i = this.mDevice.indexOf(mDevice);
        notifyItemChanged(i);
        notifyDataSetChanged();
    }

    public void clearDevice() {
        mDevice.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScannedDeviceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScannedDeviceAdapter.ViewHolder holder, int position) {
        holder.tvDeviceName.setText(mDevice.get(position).getDeviceName());
        holder.tvAddress.setText(mDevice.get(position).getAddress());
        holder.tvRssi.setText(mDevice.get(position).getRssi());
    }

    @Override
    public int getItemCount() {
        return mDevice.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDeviceName, tvAddress, tvRssi;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tv_Device_Name);
            tvAddress = itemView.findViewById(R.id.tv_Address);
            tvRssi = itemView.findViewById(R.id.tv_Rssi);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if (listener != null && position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(mDevice.get(getAdapterPosition()));
                    }
                }
            });
        }

    }

    public interface OnItemClickListener {
        void onItemClick(ScannedData selectedDevice);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}

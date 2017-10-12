package com.drivemode.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.example.eli.drivemodedemo.R;

import org.w3c.dom.Text;

import java.util.ArrayList;

/**
 * Created by liyuanqin on 17-10-9.
 */

public class MzBluetoothAdapter extends BaseAdapter {


    public static final String DTAG = "AAAAA";
    private static final String TAG = DTAG+"MzBluetoothAdapter";
    private ArrayList<BlueToothDetailItems.BlueToothItem> mAllDevices;
    private Context mContext;

    public MzBluetoothAdapter(ArrayList<BlueToothDetailItems.BlueToothItem> devices, Context context) {
        mAllDevices = devices;
        mContext = context;
        Log.v(TAG,"MzBluetoothAdapter 构造函数...size="+mAllDevices.size());
    }

    @Override
    public int getCount() {
        return mAllDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mAllDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.bluetooth_conn_listview_item_layout, null);
            holder = new ViewHolder();

            convertView.setTag(holder);

            holder.title = (TextView) convertView.findViewById(R.id.bluetooth_title);
            holder.icon = (ImageView) convertView.findViewById(R.id.bluetooth_icon);
            holder.selectIcon = (ImageView) convertView.findViewById(R.id.selected_map_icon);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.title.setTextColor(Color.BLACK);
        holder.title.setText(mAllDevices.get(position).blueToothTitle);
        holder.icon.setImageResource(mAllDevices.get(position).blueToothIconId);

        CachedBluetoothDevice device = (CachedBluetoothDevice) mAllDevices.get(position).tag;

        if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
            if (TextUtils.equals(device.getName(), "pro6test")) {
                Log.d(TAG,device.getName()+" 更新状态: 正在配对");
            }
            holder.title.setText(device.getName()+"   正在配对...");
        }
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) { //已配对!
            if (TextUtils.equals(device.getName(), "pro6test")) {
                Log.d(TAG,device.getName()+" 更新状态: 已配对");
            }
            holder.title.setText(device.getName()+" 　已配对!");
            if (device.getMaxConnectionState() == BluetoothProfile.STATE_CONNECTED) { //已连接!
                if (TextUtils.equals(device.getName(), "pro6test")) {
                    Log.v(TAG,device.getName()+" 更新状态: 已连接");
                }
                holder.title.setText(device.getName()+" 　已连接!");
                holder.selectIcon.setImageDrawable(mContext.getDrawable(R.drawable.tick));
            } else {
                if (TextUtils.equals(device.getName(), "pro6test")) {
                    Log.d(TAG,device.getName()+" 更新状态: 没有连接");
                }
                holder.selectIcon.setVisibility(View.GONE);
                //holder.title.setText(device.getName());
            }
        }
        if (device.getBondState() == BluetoothDevice.BOND_NONE) {

            if (TextUtils.equals(device.getName(), "pro6test")) {
                Log.d(TAG,device.getName()+" 更新状态: 没有绑定");
            }
            holder.title.setText(device.getName());
            if (device.getMaxConnectionState() == BluetoothProfile.STATE_CONNECTED) {
                holder.selectIcon.setVisibility(View.GONE);
                if (TextUtils.equals(device.getName(), "pro6test")) {
                    Log.d(TAG,device.getName()+" 更新状态: 断开连接");
                }
            }
        }

        return convertView;
    }

    public void updateDevices(ArrayList<BlueToothDetailItems.BlueToothItem> devices) {
        Log.v(TAG, "updateDevices......");
        mAllDevices = devices;
    }

    static class ViewHolder {
        ImageView icon;
        TextView title;
        ImageView selectIcon;
    }
}

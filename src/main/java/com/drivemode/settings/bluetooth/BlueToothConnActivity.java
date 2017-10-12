package com.drivemode.settings.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.example.eli.drivemodedemo.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class BlueToothConnActivity extends Activity {


    public static final String DEBUG_TAG = "@@@@";


    private final static String TAG = MzBluetoothAdapter.DTAG+ BlueToothConnActivity.class.getSimpleName();

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private ListView mShowBlueToothListView;


    private HandlerThread mHandlerThread;

    private Handler handler=null;

    private MzBluetoothAdapter mBluetoothListAdapter;

    ArrayList<BlueToothDetailItems.BlueToothItem> mAllDevices = new ArrayList<>();

    private BluetoothControllerImpl mBluetoothControllerImpl;

    private ImageView mScanBlueToothIV;


    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mEdit;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_conn_layout);


        Log.w("########","onCreate...............................");
        initBluetoothController();

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mEdit = mSharedPref.edit();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
        handler=new Handler();
        mShowBlueToothListView = findViewById(R.id.show_bluetooth_listview);
        mShowBlueToothListView.setOnItemClickListener(onBluetoothListItemClickListener);

        //进入这个页面的时候，开始扫描蓝牙
        mBluetoothControllerImpl.setBluetoothEnabled(true);
        mBluetoothControllerImpl.startScanning();

        mScanBlueToothIV = findViewById(R.id.bluetooth_icon);
        mScanBlueToothIV.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothControllerImpl.setBluetoothEnabled(true);
                mBluetoothControllerImpl.startScanning();
            }
        });

        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED));
        //registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));

    }

    public static final String KEY = "conn_bluetooth_name";

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG,"onReceive...action="+action);
            Toast.makeText(BlueToothConnActivity.this.getBaseContext(), "action="+action, Toast.LENGTH_SHORT).show();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.ERROR);
                if (bondState == BluetoothDevice.BOND_BONDED) {
                    Toast.makeText(BlueToothConnActivity.this.getBaseContext(), "匹配成功...", Toast.LENGTH_SHORT).show();
                    mBluetoothListAdapter.notifyDataSetChanged();
                } else if (bondState == BluetoothDevice.BOND_NONE) {
                    Toast.makeText(BlueToothConnActivity.this.getBaseContext(), "取消匹配成功...", Toast.LENGTH_SHORT).show();
                    mBluetoothListAdapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,BluetoothAdapter.STATE_CONNECTED);
                if (state == BluetoothAdapter.STATE_CONNECTED) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mEdit.putString(KEY,device.getName());
                    mEdit.apply();
                }
            }


            /*else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG,"device="+device.getName());

                mEdit.putString(KEY,device.getName());
                mEdit.apply();
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG,"device="+device.getName());

                //mEdit.putString(KEY,getString(R.string.settings_drivemode_no_devices));
                //mEdit.apply();
            }*/

            handler.post(runnableUi);
        }
    };

    private AdapterView.OnItemClickListener onBluetoothListItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            Toast.makeText(BlueToothConnActivity.this.getBaseContext(), "点击了"+position+"位置", Toast.LENGTH_SHORT).show();

            mAllDevices.clear();
            mAllDevices.addAll(updateItems());
            CachedBluetoothDevice device = (CachedBluetoothDevice) mAllDevices.get(position).tag;

            if (device != null && device.getBondState()
                    == BluetoothDevice.BOND_NONE) {
                mBluetoothControllerImpl.startPairing(device);
                Log.v(TAG, "onDetailItemClick bond_none device=" + device);
            }else if (device != null && device.getMaxConnectionState()
                    == BluetoothProfile.STATE_DISCONNECTED) { //蓝牙共享网络才用到
                mBluetoothControllerImpl.connect(device);
                Log.v(TAG, "onDetailItemClick state_disconnected device=" + device);
            }


            mBluetoothListAdapter.updateDevices(mAllDevices);
            mBluetoothListAdapter.notifyDataSetChanged();
            //handler.post(runnableUi);

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        //mBluetoothControllerImpl.setBluetoothEnabled(false);
        mAllDevices.clear();

        startService(new Intent(getBaseContext(), BlueToothStartDriveModeService.class));
    }

    boolean init = true;

    void initBluetoothController() {
        mAllDevices.clear();
        mHandlerThread = new HandlerThread(TAG, /*Process.THREAD_PRIORITY_BACKGROUND*/10);
        mHandlerThread.start();
        init = true;

        Log.w(TAG,"initBluetoothController 构造函数: looper="+mHandlerThread.getLooper());
        mBluetoothControllerImpl = new BluetoothControllerImpl(getBaseContext(), mHandlerThread.getLooper());

        mBluetoothControllerImpl.setOnBlueToothBtn(new BluetoothControllerImpl.BlueToothBtnOnClickListener() {
            @Override
            public void onBlueToothBtnClick(ArrayList<BlueToothDetailItems.BlueToothItem> allDevices) {
                //mAllDevices = allDevices;
                Log.w(TAG,"onBlueToothBtnClick...更新界面");
                handler.post(runnableUi);
            }

        });


    }

    // 构建Runnable对象，在runnable中更新界面
    Runnable   runnableUi=new  Runnable(){
        @Override
        public void run() {
            //更新界面
            mAllDevices.clear();
            mAllDevices.addAll(updateItems());
            if (init) {
                init = false;
                mBluetoothListAdapter = new MzBluetoothAdapter(mAllDevices, getBaseContext());
                mShowBlueToothListView.setAdapter(mBluetoothListAdapter);
            } else {
                mBluetoothListAdapter.updateDevices(mAllDevices);
                mBluetoothListAdapter.notifyDataSetChanged();
            }
        }

    };


    private ArrayList<BlueToothDetailItems.BlueToothItem> updateItems() {
        ArrayList<BlueToothDetailItems.BlueToothItem> items = new ArrayList<>();

        // get All Devices
        final Collection<CachedBluetoothDevice> devices = mBluetoothControllerImpl.getDevices();

        ArrayList<CachedBluetoothDevice> discoveredDevices = new ArrayList<>();
        ArrayList<CachedBluetoothDevice> bondedDevices = new ArrayList<>();

        if (devices != null) {
            //Log.i(TAG,"updateItems..get All Devices....size="+devices.size());
            for (CachedBluetoothDevice device : devices) {
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_NONE:
                        discoveredDevices.add(device);
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        bondedDevices.add(device);
                        break;
                    default:
                        Log.d(TAG, "BONDING and BOND_SUCCESS");
                        break;
                }
            }

            // sort only for bondedDevice
            bondedDevices.sort(mDeviceComparator);

            // create bonedDevice item
            for (CachedBluetoothDevice bondedDevice : bondedDevices) {
                items.add(initItem(bondedDevice));
            }

            // if (refreshAll) {
            // sort for discoverDevice
            //   discoveredDevices.sort(mDiscoveredComparator);

            // create discoveredDevice item
            for (CachedBluetoothDevice discoverDevice : discoveredDevices) {
                items.add(initItem(discoverDevice));
            }
            // }
        }
        return items;
    }


    private Comparator<CachedBluetoothDevice> mDeviceComparator = new Comparator<CachedBluetoothDevice>() {
        @Override
        public int compare(CachedBluetoothDevice o1, CachedBluetoothDevice o2) {
            if (o1 == null || o2 == null) {
                return o1 == o2 ? 0 : (o1 == null ? -1 : 1);
            }
            // Connected above not connected
            int comparison = (o2.isConnected() ? 1 : 0) - (o1.isConnected() ? 1 : 0);
            if (comparison != 0) return comparison;

            // connecting above not connnecting
            comparison = (o2.isBusy() ? 1 : 0) - (o1.isBusy() ? 1 : 0);
            if (comparison != 0) return comparison;

            // Fallback on name
            comparison = o1.getName().compareTo(o2.getName());
            return comparison == 0 ? 0 : (comparison > 0 ? 1 : -1);
        }
    };

    private BlueToothDetailItems.BlueToothItem initItem(CachedBluetoothDevice device) {
        //if (device.getBondState() == BluetoothDevice.BOND_NONE) continue;
        final BlueToothDetailItems.BlueToothItem item = new BlueToothDetailItems.BlueToothItem();
        item.blueToothIconId = R.drawable.ic_qs_bluetooth_on;
        item.blueToothTitle = device.getName();
        int state = device.getMaxConnectionState();
        if (state == BluetoothProfile.STATE_CONNECTED) {
            item.blueToothIconId = R.drawable.ic_qs_bluetooth_connected;
        } else if (state == BluetoothProfile.STATE_CONNECTING) {
            item.blueToothIconId = R.drawable.ic_qs_bluetooth_connecting;
        }
        //FLYME: flyme UI customization {@
        if (device instanceof CachedBluetoothDevice) {
            updateIcon(device, item);
        }

        item.tag = device;
        //@}
        return item;
    }


    private void updateIcon(CachedBluetoothDevice device, BlueToothDetailItems.BlueToothItem item) {
        if (device != null) {
            int icon = BluetoothIcon.getDeviceIcon(device);
            if (icon != 0) {
                item.blueToothIconId = icon;
            }
        }
    }


}

package com.drivemode.settings.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import com.drivemode.settings.DriveModeSettingsActivity;
import com.example.eli.drivemodedemo.R;

import static com.drivemode.settings.DriveModeSettingsActivity.MZ_DRIVE_MODE_BLUETOOTH_TRIGGER;

/**
 * Created by liyuanqin on 17-10-11.
 */

public class BlueToothStartDriveModeService extends Service {

    public static final String TAG = "BlueToothStartDriveModeService";

    private SharedPreferences mSharedPref;

    private String mConnBlueToothName;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(MzBluetoothAdapter.DTAG+TAG,"onCreate");

       // Toast.makeText(this,"BlueToothStartDriveModeService.onCreate",Toast.LENGTH_SHORT).show();

    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(MzBluetoothAdapter.DTAG+TAG,"onReceive action="+action);
            /*if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                mConnBlueToothName = mSharedPref.getString(BlueToothConnActivity.KEY,getString(R.string.settings_drivemode_no_devices));
                Log.i(MzBluetoothAdapter.DTAG+TAG,"onReceive mConnBlueToothName="+mConnBlueToothName);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                boolean isChecked = Settings.Secure.getInt(getContentResolver(),
                        MZ_DRIVE_MODE_BLUETOOTH_TRIGGER, 0) == 1;
                if (TextUtils.equals(mConnBlueToothName,device.getName()) && isChecked) {
                    sendBroadcast(new Intent(DriveModeSettingsActivity.MZ_ACTION_DRIVER_MODE_START));//收到连接成功广播，开启驾驶模式
                }

            } else */if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,BluetoothAdapter.STATE_CONNECTED);
                if (state == BluetoothAdapter.STATE_CONNECTED) {
                    mConnBlueToothName = mSharedPref.getString(BlueToothConnActivity.KEY,getString(R.string.settings_drivemode_no_devices));
                    Log.i(MzBluetoothAdapter.DTAG+TAG,"onReceive mConnBlueToothName="+mConnBlueToothName);
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    boolean isChecked = Settings.Secure.getInt(getContentResolver(),
                            MZ_DRIVE_MODE_BLUETOOTH_TRIGGER, 0) == 1;
                    if (TextUtils.equals(mConnBlueToothName,device.getName()) && isChecked) {
                        sendBroadcast(new Intent(DriveModeSettingsActivity.MZ_ACTION_DRIVER_MODE_START));//收到连接成功广播，开启驾驶模式
                    }
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(MzBluetoothAdapter.DTAG+TAG,"onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED));
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        Log.i(MzBluetoothAdapter.DTAG+TAG,"onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

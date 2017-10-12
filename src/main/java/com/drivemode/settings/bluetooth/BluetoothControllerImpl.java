/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.drivemode.settings.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

public class BluetoothControllerImpl implements BluetoothController, BluetoothCallback,
        CachedBluetoothDevice.Callback {
    private static final String TAG = /*BlueToothConnActivity.DEBUG_TAG+*/"BluetoothController";
    private static final boolean DEBUG = true;


    public static final String MEIZU_BLUETOOTH_CONNECT_AFTER_PAIR = "bluetooth_connect_after_pair";
    public static final String MEIZU_BLUETOOTH_HOST_START_PAIR = "bluetooth_host_start_pair";

    private final LocalBluetoothManager mLocalBluetoothManager;
    // Flyme {@
    //private final UserManager mUserManager;
    //private final int mCurrentUser;
    private final Context mContext;
    // @}

    private boolean mEnabled;
    private int mConnectionState = BluetoothAdapter.STATE_DISCONNECTED;
    private CachedBluetoothDevice mLastDevice;

    public static final int FILTER_TYPE_AUDIO = 1;

    private final H mHandler = new H();

    private BluetoothDeviceFilter.Filter mProfileFilter;

    public BluetoothControllerImpl(Context context, Looper bgLooper) {
        mLocalBluetoothManager = LocalBluetoothManager.getInstance(context, null);
        if (mLocalBluetoothManager != null) {
            mLocalBluetoothManager.getEventManager().setReceiverHandler(new Handler(bgLooper));
            mLocalBluetoothManager.getEventManager().registerCallback(this);
            onBluetoothStateChanged(
                    mLocalBluetoothManager.getBluetoothAdapter().getBluetoothState());
        }
        // Flyme {@
        //mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        //mCurrentUser = ActivityManager.getCurrentUser();
        mContext = context;
        mProfileFilter = BluetoothDeviceFilter.getFilter(FILTER_TYPE_AUDIO);
        Log.d(TAG,"BluetoothControllerImpl ");

        // @}

    }

    @Override
    public boolean canConfigBluetooth() {
        // Flyme {@
        return /*!mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_BLUETOOTH,
                UserHandle.of(mCurrentUser))*/true;
        // @}
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("BluetoothController state:");
        pw.print("  mLocalBluetoothManager="); pw.println(mLocalBluetoothManager);
        if (mLocalBluetoothManager == null) {
            return;
        }
        pw.print("  mEnabled="); pw.println(mEnabled);
        pw.print("  mConnectionState="); pw.println(stateToString(mConnectionState));
        pw.print("  mLastDevice="); pw.println(mLastDevice);
        pw.print("  mCallbacks.size="); pw.println(mHandler.mCallbacks.size());
        pw.println("  Bluetooth Devices:");
        for (CachedBluetoothDevice device :
                mLocalBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy()) {
            pw.println("    " + getDeviceString(device));
        }
    }

    private static String stateToString(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_CONNECTED:
                return "CONNECTED";
            case BluetoothAdapter.STATE_CONNECTING:
                return "CONNECTING";
            case BluetoothAdapter.STATE_DISCONNECTED:
                return "DISCONNECTED";
            case BluetoothAdapter.STATE_DISCONNECTING:
                return "DISCONNECTING";
        }
        return "UNKNOWN(" + state + ")";
    }

    private String getDeviceString(CachedBluetoothDevice device) {
        return device.getName() + " " + device.getBondState() + " " + device.isConnected();
    }

    @Override
    public void addStateChangedCallback(Callback cb) {
        mHandler.obtainMessage(H.MSG_ADD_CALLBACK, cb).sendToTarget();
        mHandler.sendEmptyMessage(H.MSG_STATE_CHANGED);
    }

    @Override
    public void removeStateChangedCallback(Callback cb) {
        mHandler.obtainMessage(H.MSG_REMOVE_CALLBACK, cb).sendToTarget();
    }

    @Override
    public boolean isBluetoothEnabled() {
        return mEnabled;
    }

    @Override
    public boolean isBluetoothConnected() {
        return mConnectionState == BluetoothAdapter.STATE_CONNECTED;
    }

    @Override
    public boolean isBluetoothConnecting() {
        return mConnectionState == BluetoothAdapter.STATE_CONNECTING;
    }

    @Override
    public void setBluetoothEnabled(boolean enabled) {
        if (mLocalBluetoothManager != null) {
            mLocalBluetoothManager.getBluetoothAdapter().setBluetoothEnabled(enabled);
        }
    }

    @Override
    public boolean isBluetoothSupported() {
        return mLocalBluetoothManager != null;
    }

    @Override
    public void connect(final CachedBluetoothDevice device) {
        if (mLocalBluetoothManager == null || device == null) return;
        device.connect(true);
    }

    @Override
    public void disconnect(CachedBluetoothDevice device) {
        if (mLocalBluetoothManager == null || device == null) return;
        device.disconnect();
    }

    @Override
    public String getLastDeviceName() {
        return mLastDevice != null ? mLastDevice.getName() : null;
    }

    @Override
    public Collection<CachedBluetoothDevice> getDevices() {
        return mLocalBluetoothManager != null
                ? mLocalBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy()
                : null;
    }

    @Override
    public Collection<BluetoothDevice> getBondedDevices(){
        return mLocalBluetoothManager != null
                ? mLocalBluetoothManager.getBluetoothAdapter().getBondedDevices()
                : null;
    }


    public interface BlueToothBtnOnClickListener {
        void onBlueToothBtnClick(ArrayList<BlueToothDetailItems.BlueToothItem> allDevices);
    }

    BlueToothBtnOnClickListener mBlueToothBtnOnClickListener;

    public void setOnBlueToothBtn(BlueToothBtnOnClickListener onClickListener) {
        mBlueToothBtnOnClickListener = onClickListener;
    }


    private void updateConnected() {
        // Make sure our connection state is up to date.
        int state = mLocalBluetoothManager.getBluetoothAdapter().getConnectionState();
        if (state != mConnectionState) {
            mConnectionState = state;
            mHandler.sendEmptyMessage(H.MSG_STATE_CHANGED);
        }
        if (mLastDevice != null && mLastDevice.isConnected()) {
            // Our current device is still valid.
            return;
        }

        mLastDevice = null;

      //  allDevices.clear();

      //  allDevices.addAll(updateItems());


        /*for (QSDetailItems.BlueToothItem item:  allDevices) {
            Log.w(TAG,"updateConnected..title="+item.blueToothTitle+" icon="+item.blueToothIconId);
        }*/


        Log.w(TAG,"updateConnected...getDevices.size="+getDevices().size());
        for (CachedBluetoothDevice device : getDevices()) {
            //Log.v(TAG,"updateConnected...device="+device.getName()+" address="+device.getDevice().getName()+" address="+device.getDevice().toString());
            if (device.isConnected()) {
                mLastDevice = device;
            }

            if (mProfileFilter != null && mProfileFilter.matches(device.getDevice())) {
                Log.d(TAG,"updateConnected..根据指定规则过滤蓝牙,device="+device.getName());
            }
        }
        if (mLastDevice == null && mConnectionState == BluetoothAdapter.STATE_CONNECTED) {
            // If somehow we think we are connected, but have no connected devices, we aren't
            // connected.
            mConnectionState = BluetoothAdapter.STATE_DISCONNECTED;
            mHandler.sendEmptyMessage(H.MSG_STATE_CHANGED);
        }

        Log.w(TAG,"updateConnected...mBlueToothBtnOnClickListener="+mBlueToothBtnOnClickListener);
        if( mBlueToothBtnOnClickListener != null)
        mBlueToothBtnOnClickListener.onBlueToothBtnClick(allDevices);

        /*if (allDevices != null && allDevices.size() > 1) {
            //mMainActivity.updateListView(allDevices);
        }*/
    }

    ArrayList<BlueToothDetailItems.BlueToothItem> allDevices = new ArrayList<>();


    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        // Flyme {@
        mBluetoothState = bluetoothState;
        //@}
        mEnabled = bluetoothState == BluetoothAdapter.STATE_ON;
        mHandler.sendEmptyMessage(H.MSG_STATE_CHANGED);
    }

    @Override
    public void onScanningStateChanged(boolean started) {
        // Flyme {@
        // AOSP Don't care, But flyme care.
        mHandler.obtainMessage(H.MSG_SCANNING_STATE_CHANGED, started ? 1 : 0, 0).sendToTarget();
        //@}
    }

    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
        cachedDevice.registerCallback(this);
        updateConnected();
        Log.d(TAG,"onDeviceAdded");
        mHandler.sendEmptyMessage(H.MSG_PAIRED_DEVICES_CHANGED);
    }

    @Override
    public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {
        updateConnected();
        Log.d(TAG,"onDeviceDeleted");
        // Flyme {@
        //mHandler.sendEmptyMessage(H.MSG_PAIRED_DEVICES_CHANGED);
        updateDevicesState();
        //@}
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        updateConnected();
        Log.d(TAG,"onDeviceBondStateChanged");
        // Flyme {@
        //mHandler.sendEmptyMessage(H.MSG_PAIRED_DEVICES_CHANGED);
        updateDevicesState();
        //@}
    }

    @Override
    public void onDeviceAttributesChanged() { //scan后主要这个函数带动更新
        updateConnected();
        Log.d(TAG,"onDeviceAttributesChanged");
        // Flyme {@
        //mHandler.sendEmptyMessage(H.MSG_PAIRED_DEVICES_CHANGED);
        updateDevicesState();
        //@}
    }

    @Override
    public void onConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
        mLastDevice = cachedDevice;
        updateConnected();
        Log.d(TAG,"onConnectionStateChanged");
        mConnectionState = state;
        mHandler.sendEmptyMessage(H.MSG_STATE_CHANGED);
    }

    private final class H extends Handler {
        private final ArrayList<BluetoothController.Callback> mCallbacks = new ArrayList<>();

        private static final int MSG_PAIRED_DEVICES_CHANGED = 1;
        private static final int MSG_STATE_CHANGED = 2;
        private static final int MSG_ADD_CALLBACK = 3;
        private static final int MSG_REMOVE_CALLBACK = 4;
        // Flyme {@
        private static final int MSG_FLYME_START = 99;
        private static final int MSG_START_SCANNING = MSG_FLYME_START + 1;
        private static final int MSG_SCANNING_STATE_CHANGED = MSG_FLYME_START + 2;
        //@}

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PAIRED_DEVICES_CHANGED:
                    firePairedDevicesChanged();
                    break;
                case MSG_STATE_CHANGED:
                    fireStateChange();
                    break;
                case MSG_ADD_CALLBACK:
                    mCallbacks.add((BluetoothController.Callback) msg.obj);
                    break;
                case MSG_REMOVE_CALLBACK:
                    mCallbacks.remove((BluetoothController.Callback) msg.obj);
                    break;
                // Flyme {@
                case MSG_START_SCANNING:
                    fireStartScanning();
                    break;
                case MSG_SCANNING_STATE_CHANGED:
                    fireScanningStateChanged(msg.arg1 != 0);
                    break;
                //@}
            }
        }

        private void firePairedDevicesChanged() {
            for (BluetoothController.Callback cb : mCallbacks) {
                cb.onBluetoothDevicesChanged();
            }
        }

        private void fireStateChange() {
            for (BluetoothController.Callback cb : mCallbacks) {
                fireStateChange(cb);
            }
        }

        private void fireStateChange(BluetoothController.Callback cb) {
            cb.onBluetoothStateChange(mEnabled);
        }

        // Flyme {@
        private void fireScanningStateChanged(boolean started) {
            for (BluetoothController.Callback cb : mCallbacks) {
                cb.onScanningStateChanged(started);
            }
        }
        //@}
    }


    /*********************************** Flyme extends begin ***********************************/
    @Override
    public boolean isBluetoothTurningOn() {
        return mBluetoothState == BluetoothAdapter.STATE_TURNING_ON;
    }

    @Override
    public boolean isBluetoothTurningOff() {
        return mBluetoothState == BluetoothAdapter.STATE_TURNING_OFF;
    }

    @Override
    public void startScanning() {
        if (mLocalBluetoothManager != null && isBluetoothEnabled()) {
            // Flyme: clear cached devices when start a new scanning {@
            mLocalBluetoothManager.getCachedDeviceManager().clearNonBondedDevices();
            // @}
            mLocalBluetoothManager.getBluetoothAdapter().startScanning(true);
           // mHandler.obtainMessage(H.MSG_SCANNING_STATE_CHANGED, 1, 0).sendToTarget();
        } else {
         //   mHandler.sendEmptyMessageDelayed(H.MSG_START_SCANNING, 50);
        }
    }

    @Override
    public void stopScanning() {
        if (mLocalBluetoothManager != null) {
            mLocalBluetoothManager.getBluetoothAdapter().stopScanning();
        }
    }

    @Override
    public void startPairing(CachedBluetoothDevice device) {
        if (mLocalBluetoothManager == null || device == null) return;
        if (!device.startPairing()) {
            Log.e(TAG, "pairing error");
        }
        Settings.Global.putInt(mContext.getContentResolver(),
                MEIZU_BLUETOOTH_CONNECT_AFTER_PAIR, 1);
        Settings.Global.putInt(mContext.getContentResolver(),
                MEIZU_BLUETOOTH_HOST_START_PAIR, 1);
    }

    private void fireStartScanning() {
        startScanning();
    }

    private void updateDevicesState() {
        if (mHandler.hasMessages(H.MSG_PAIRED_DEVICES_CHANGED)) {
            mHandler.removeMessages(H.MSG_PAIRED_DEVICES_CHANGED);
        }
        mHandler.sendEmptyMessageDelayed(H.MSG_PAIRED_DEVICES_CHANGED, NOTIFY_UPDATE_DEVICES_DURATION);
    }


    private static final int NOTIFY_UPDATE_DEVICES_DURATION = 350;
    private int mBluetoothState;

    @Override
    public boolean isBluetoothTetheringOn() {
        return mLastDevice != null && mLastDevice.isLocalNapRoleConnected();
    }
    /************************************ Flyme extends end ************************************/


}

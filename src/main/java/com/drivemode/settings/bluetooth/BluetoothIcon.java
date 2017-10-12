package com.drivemode.settings.bluetooth;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothProfile;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseBooleanArray;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.example.eli.drivemodedemo.R;

import java.lang.reflect.Method;

import static com.android.settingslib.bluetooth.HidProfile.getHidClassDrawable;

/**
 * Created by liyuanqin on 17-9-28.
 */

public class BluetoothIcon {

    private static String TAG = "BluetoothIcon";


    /** @hide */
    public static final int PROFILE_HEADSET = 0;
    /** @hide */
    public static final int PROFILE_A2DP = 1;

    /**
     * Input Device Profile
     * @hide
     */
    public static final int INPUT_DEVICE = 4;

    /**
     * PAN Profile
     * @hide
     */
    public static final int PAN = 5;

    private static final SparseBooleanArray mProfileSupport = new SparseBooleanArray();
    static {
        //init profile map
        mProfileSupport.put(BluetoothProfile.HEADSET, false);
        mProfileSupport.put(BluetoothProfile.A2DP, false);
        mProfileSupport.put(INPUT_DEVICE, false);
        mProfileSupport.put(PAN, false);
    }


    /**
     * @param profile
     */
    public static Boolean doesClassMatch(int profile) {
        try {
            Class clazz = Class.forName("android.bluetooth.BluetoothClass");
            Method m = clazz.getMethod("doesClassMatch", Integer.class);
            return (Boolean)m.invoke(clazz, profile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.w(TAG,"doesClassMatch..匹配不到类型..return false");
        return false;
    }


    public static int getDeviceIcon(CachedBluetoothDevice device) {
        // if (btClass != null) Log.v(TAG, "btClass: " + btClass.toString());
        BluetoothClass btClass = device.getBtClass();
        if (btClass != null) {
            switch (btClass.getMajorDeviceClass()) {
                case BluetoothClass.Device.Major.COMPUTER:
                    return R.drawable.ic_qs_bluetooth_mac;

                case BluetoothClass.Device.Major.PHONE:
                    return R.drawable.ic_qs_bluetooth_phone;

                case BluetoothClass.Device.Major.PERIPHERAL:
                    return getHidClassDrawable(btClass);

                case BluetoothClass.Device.Major.IMAGING:
                    return R.drawable.ic_qs_bluetooth_print;

                default:
                    // unrecognized device class; continue
            }
        } else {
            Log.w(TAG, "mBtClass is null");
        }

        if (btClass != null) {
            if (doesClassMatch(PROFILE_A2DP)) {
                return R.drawable.ic_qs_bluetooth_earphone;
            }
            if (doesClassMatch(PROFILE_HEADSET)) {
                return R.drawable.ic_qs_bluetooth_earphone;
            }
        }

        ParcelUuid[] uuids = device.getDevice().getUuids();
        if (uuids != null) {
            if (BluetoothUuid.containsAnyUuid(uuids, A2DPPROFILE_SINK_UUIDS)) {
                return R.drawable.ic_qs_bluetooth_earphone;
            }
            if (BluetoothUuid.containsAnyUuid(uuids, HEADSETPROFILE_UUIDS)) {
                return R.drawable.ic_qs_bluetooth_earphone;
            }
        }
        //for device has no cod, such as pair via NFC
        if (isProfileSupport(BluetoothProfile.HEADSET)) {
            return R.drawable.ic_qs_bluetooth_earphone;
        } else if (isProfileSupport(BluetoothProfile.A2DP)) {
            return R.drawable.ic_qs_bluetooth_earphone;
        } else if (isProfileSupport(INPUT_DEVICE)) {
            return R.drawable.ic_qs_bluetooth_xbox;
        } else if (isProfileSupport(PAN)) {
            return R.drawable.ic_qs_bluetooth_phone;
        }

        //no match to any class of icon
        return 0;
    }

    private static boolean isProfileSupport(int Profile) {
        if (mProfileSupport == null) {
            Log.d(TAG, "mProfileSupport is null, do not connect");
            return false;
        }
        try {
            switch (Profile) {
                case BluetoothProfile.HEADSET:
                    return mProfileSupport.get(BluetoothProfile.HEADSET);
                case BluetoothProfile.A2DP:
                    return mProfileSupport.get(BluetoothProfile.A2DP);
                case INPUT_DEVICE:
                    return mProfileSupport.get(INPUT_DEVICE);
                case PAN:
                    return mProfileSupport.get(PAN);
                default:
                    return false;
            }
        } catch (NullPointerException e) {
            Log.d(TAG, "fatal NullPointerException, isProfileSupport get null");
            e.printStackTrace();
        }
        return false;
    }

    static final ParcelUuid[] A2DPPROFILE_SINK_UUIDS = {
            BluetoothUuid.AudioSink,
            BluetoothUuid.AdvAudioDist,
    };

    static final ParcelUuid[] HEADSETPROFILE_UUIDS = {
            BluetoothUuid.HSP,
            BluetoothUuid.Handsfree,
    };
}

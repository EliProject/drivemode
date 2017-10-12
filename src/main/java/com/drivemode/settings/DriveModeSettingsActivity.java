package com.drivemode.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.drivemode.map.MapNotifyService;
import com.drivemode.music.MusicPlayService;
import com.drivemode.settings.bluetooth.BlueToothConnActivity;
import com.example.eli.drivemodedemo.R;


/**
 * Created by liyuanqin on 17-10-10.
 */

public class DriveModeSettingsActivity extends Activity {



    public static final String TAG = "DriveModeSettings_@@##";

    public static final String MZ_DRIVE_MODE_MUSIC_AUTOPLAY = "mz_drive_mode_music_autoplay";
    public static final String MZ_DRIVE_MODE_BLUETOOTH_TRIGGER = "mz_drive_mode_bluetooth_trigger";

    private Switch mAutoPlayMusicSwitch;
    private Switch mStartConnBlueTooth;
    private Button mDriveModeSwitchBtn;

    private ImageView mBlueToothDeviceNextIV;

    private TextView mTV;

    public static final String MZ_DRIVE_MODE = "mz_drive_mode";
    public static final Uri DRIVE_MODE_URI = Settings.Secure.getUriFor(MZ_DRIVE_MODE);

    private SharedPreferences mSharedPref;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        mAutoPlayMusicSwitch = findViewById(R.id.auto_play_music_switch);
        mStartConnBlueTooth = findViewById(R.id.conn_bluetooth_start_switch);
        mDriveModeSwitchBtn = findViewById(R.id.exit_drive_mode);

        mTV = findViewById(R.id.drivemode_bluetooth_devies_info);

        mBlueToothDeviceNextIV = findViewById(R.id.drivemode_bluetooth_devies_next);

        mAutoPlayMusicSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                autoPlayMusic(isChecked);
            }
        });


        mStartConnBlueTooth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                launchBtPicker(isChecked);
            }
        });

        mDriveModeSwitchBtn.setOnClickListener(mOnClickListener);
        mBlueToothDeviceNextIV.setOnClickListener(mOnClickListener);
    }


    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            switch (view.getId()) {
                case R.id.exit_drive_mode:
                    sendBroadcast(new Intent(MZ_ACTION_DRIVER_MODE_STOP));
                    break;
                case R.id.drivemode_bluetooth_devies_next:
                    startActivity(new Intent(getBaseContext(),BlueToothConnActivity.class));
                    break;
            }

        }
    };


    String mConnBlueToothName;

    @Override
    protected void onResume() {
        super.onResume();

        sendBroadcast(new Intent(MZ_ACTION_DRIVER_MODE_START));//进入这个页面时候，开启驾驶模式

        boolean on = Settings.Secure.getInt(getContentResolver(),
                MZ_DRIVE_MODE_MUSIC_AUTOPLAY, 0) == 1;
        mAutoPlayMusicSwitch.setChecked(on);

        boolean isChecked = Settings.Secure.getInt(getContentResolver(),
                MZ_DRIVE_MODE_BLUETOOTH_TRIGGER, 0) == 1;
        mStartConnBlueTooth.setChecked(isChecked);


        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        mConnBlueToothName = mSharedPref.getString(BlueToothConnActivity.KEY,getString(R.string.settings_drivemode_no_devices));

        mTV.setText(mConnBlueToothName);


        getContentResolver().registerContentObserver(DRIVE_MODE_URI, false, mObserver);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        getContentResolver().registerContentObserver(DRIVE_MODE_URI, false, mObserver);
    }

    private ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            Log.d(TAG,"onChange uri="+uri+" DRIVE_MODE_URI.equals(uri)="+DRIVE_MODE_URI.equals(uri));
            if (DRIVE_MODE_URI.equals(uri)) {
                boolean on = Settings.Secure.getInt(getContentResolver(),MZ_DRIVE_MODE, 0) == 1;
                Log.d(TAG,"onChange on="+on);
                if (on) {
                    //打开音乐通知
                    Intent musicIntent=new Intent(getApplicationContext(), MusicPlayService.class);
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(MusicPlayService.MUSIC_UPDATE_KEY,false);
                    musicIntent.putExtras(bundle);
                    startService(musicIntent);
                    //打开地图通知
                    Intent mapIntent=new Intent(getApplicationContext(), MapNotifyService.class);
                    Bundle mapBundle = new Bundle();
                    mapBundle.putBoolean(MapNotifyService.MAP_UPDATE_KEY,false);
                    mapBundle.putInt(MapNotifyService.MAP_INIT_SHOWTIME_KEY,0);
                    mapIntent.putExtras(mapBundle);
                    startService(mapIntent);

                } else {
                    //删除音乐通知
                    Intent musicIntent = new Intent(MusicPlayService.ACTION_CLICK);
                    musicIntent.putExtra(MusicPlayService.INTENT_BUTTONID_TAG, MusicPlayService.BUTTON_DELETE_NOFICY_ID);
                    sendBroadcast(musicIntent);

                    //删除地图通知
                    Intent mapIntent = new Intent(MapNotifyService.ACTION_CLICK);
                    mapIntent.putExtra(MapNotifyService.INTENT_BUTTONID_TAG, MapNotifyService.BUTTON_DELETE_NOFICY_ID);
                    sendBroadcast(mapIntent);
                }
            }
        }
    };

    public static final String MZ_ACTION_DRIVER_MODE_START = "meizu.intent.action.DRIVE_MODE_START";
    public static final String MZ_ACTION_DRIVER_MODE_STOP = "meizu.intent.action.DRIVE_MODE_STOP";

    private void autoPlayMusic(boolean isChecked) {
        Settings.Secure.putInt(getContentResolver(), MZ_DRIVE_MODE_MUSIC_AUTOPLAY,
                isChecked ? 1 : 0);
    }

    private void launchBtPicker(boolean isChecked) {
        Settings.Secure.putInt(getContentResolver(),
                                    MZ_DRIVE_MODE_BLUETOOTH_TRIGGER, isChecked ? 1 : 0);
    }

}

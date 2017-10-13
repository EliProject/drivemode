package com.example.eli.drivemodedemo;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.drivemode.WelComeDriveModeActivity;
import com.drivemode.map.MapActivity;
import com.drivemode.music.MediaActivity;
import com.drivemode.settings.bluetooth.BlueToothConnActivity;
import com.drivemode.settings.bluetooth.BlueToothStartDriveModeService;

public class MainActivity extends Activity {

    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mEdit;

    private static final boolean FIRST_TIME_VALUE = true;
    private static final String FIRST_TIME＿KEY = "first_time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button musicPlayerBtn = findViewById(R.id.enter_music_player);
        Button mapBtn = findViewById(R.id.enter_map);
        Button bluetoothBtn = findViewById(R.id.bluetooth);
        Button settingsBtn = findViewById(R.id.setttings);

        musicPlayerBtn.setOnClickListener(mOnClickListener);
        mapBtn.setOnClickListener(mOnClickListener);
        bluetoothBtn.setOnClickListener(mOnClickListener);
        settingsBtn.setOnClickListener(mOnClickListener);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mEdit = mSharedPref.edit();

        startService(new Intent(this, BlueToothStartDriveModeService.class));
    }


    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.enter_music_player:
                    Intent musicIntent = new Intent(getApplicationContext(), MediaActivity.class);
                    startActivity(musicIntent);
                    break;
                case R.id.enter_map:
                    Intent mapIntent = new Intent(getApplicationContext(), MapActivity.class);
                    startActivity(mapIntent);
                    break;
                case R.id.bluetooth:
                    Toast.makeText(MainActivity.this, "还没有上线，请等待！",Toast.LENGTH_SHORT).show();
                    break;
                case R.id.setttings:
                    boolean firstime = mSharedPref.getBoolean(FIRST_TIME＿KEY, FIRST_TIME_VALUE);
                    if (firstime) {
                        Intent welComeIntent = new Intent(getApplicationContext(), WelComeDriveModeActivity.class);
                        startActivity(welComeIntent);
                        mEdit.putBoolean(FIRST_TIME＿KEY, false);
                        mEdit.apply();
                    } else {
                        Intent settingsIntent = new Intent(getApplicationContext(), BlueToothConnActivity.class);
                        startActivity(settingsIntent);
                    }
                    break;
            }
        }
    };
}

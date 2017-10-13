package com.drivemode;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import com.drivemode.settings.bluetooth.BlueToothConnActivity;
import com.example.eli.drivemodedemo.R;

/**
 * Created by liyuanqin on 17-10-12.
 */

public class WelComeDriveModeActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_layout);


        Button nextStepBtn = findViewById(R.id.next_step);
        nextStepBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getBaseContext(), BlueToothConnActivity.class));
            }
        });
    }
}

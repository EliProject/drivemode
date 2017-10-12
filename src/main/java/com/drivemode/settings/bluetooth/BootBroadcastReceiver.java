package com.drivemode.settings.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by liyuanqin on 17-10-11.
 */

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Toast.makeText(context,"开机广播",Toast.LENGTH_SHORT).show();
        Intent service = new Intent(context,BlueToothStartDriveModeService.class);
        context.startService(service);
    }
}

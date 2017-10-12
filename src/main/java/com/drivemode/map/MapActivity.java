package com.drivemode.map;

/**
 * Created by liyuanqin on 17-9-29.
 */

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.drivemode.music.MusicPlayService;
import com.example.eli.drivemodedemo.R;


public class MapActivity extends Activity {

    private static final String TAG = "MapActivity_@@@@";


    private static final String MAP_SERVICE_ACTION = "flyme.drivemode.service.map";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_layout);

        Log.d(TAG,"onCreate..");
        Intent intent = new Intent();
        intent.setAction(MAP_SERVICE_ACTION);
        intent.setPackage(getPackageName());
        bindService(intent,conn, Context.BIND_AUTO_CREATE);


        Button btn = (Button) findViewById(R.id.exchange_maps);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMapNotifyService.getMapsList().size() <= 1) {
                    Toast.makeText(getBaseContext(), "只有一个地图!", Toast.LENGTH_SHORT).show();
                } else {
                    showPopupWindow(view);
                }
            }
        });


    }


    private MapNotifyService mMapNotifyService;
    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMapNotifyService = ((MapNotifyService.MapBinder) service).getMapService();
            Log.d(TAG,"onServiceConnected..mMapNotifyService="+mMapNotifyService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG,"onServiceDisconnected..mMapNotifyService="+mMapNotifyService);
            mMapNotifyService = null;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        sendNotification();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
        unbindService(conn);
        mMapNotifyService = null;
    }

    private void sendNotification() {
        Intent updateIntent=new Intent(getApplicationContext(), MapNotifyService.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean(MapNotifyService.MAP_UPDATE_KEY,false);
        bundle.putInt(MapNotifyService.MAP_INIT_SHOWTIME_KEY,0);
        updateIntent.putExtras(bundle);
        startService(updateIntent);
    }


    private MapsAdapter mGroupAdapter;


    private ListView mMapsListView;
    private PopupWindow mSelectMapsPopupWIndow;
    private void showPopupWindow(View parent) {
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.map_list, null);
        mMapsListView = view.findViewById(R.id.maps_listview);
        // 加载数据
        mGroupAdapter = new MapsAdapter(getApplicationContext(),
                mMapNotifyService.getMapsList(), MapNotifyService.sSelectedPopUpItem);
        mMapsListView.setAdapter(mGroupAdapter);
        // 创建一个PopuWidow对象
        int width = getResources().getDisplayMetrics().widthPixels;
        int height;
        if (mMapNotifyService.getMapsList().size() == 2) {
            height = 320;
        } else {
            height = 480;
        }
        mSelectMapsPopupWIndow = new PopupWindow(view, width, height);
        // 使其聚集
        mSelectMapsPopupWIndow.setFocusable(true);
        // 设置允许在外点击消失
        mSelectMapsPopupWIndow.setOutsideTouchable(true);

        // 这个是为了点击“返回Back”也能使其消失，并且并不会影响你的背景
        mSelectMapsPopupWIndow.setBackgroundDrawable(new BitmapDrawable());
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        // 显示的位置为:屏幕的宽度的一半-PopupWindow的高度的一半
        int xPos = windowManager.getDefaultDisplay().getWidth() / 2 -
                mSelectMapsPopupWIndow.getWidth() / 2;
        Log.i(TAG, "xPos:" + xPos);

        mSelectMapsPopupWIndow.showAsDropDown(parent, xPos, 0);
        mMapsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int position, long id) {
                MapNotifyService.sSelectedPopUpItem = position;
                Log.i(TAG, "onItemClick, sSelectedItem="+ MusicPlayService.sSelectedItem);
                Toast.makeText(getApplicationContext(),
                        "点击了第" + MapNotifyService.sSelectedPopUpItem + "个", Toast.LENGTH_SHORT).show();

                sendNotification();
                if (mSelectMapsPopupWIndow != null) {
                    mSelectMapsPopupWIndow.dismiss();
                }
            }
        });
    }


}


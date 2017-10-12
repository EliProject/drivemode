package com.drivemode.map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.drivemode.music.MusicPlayService;
import com.drivemode.settings.DriveModeSettingsActivity;
import com.example.eli.drivemodedemo.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liyuanqin on 17-10-8.
 */

public class MapNotifyService extends Service {


    private static final String TAG = "MapNotifyService_@@##";

    public final static String ACTION_CLICK = "com.notifications.intent.action.map";

    private ArrayList<Maps> mMapsList;
    private ArrayList mMapsPkgNameList = new ArrayList();

    class Maps {
        Drawable icon;
        String title;
    }
    /**
     * 通知栏按钮广播
     */
    public ButtonBroadcastReceiver bReceiver;

    /**
     * 进入地图
     */
    public final static int GO_MAP = 1;
    /**
     * 回家
     */
    public final static int GO_HOME = 2;
    /**
     * 去公司
     */
    public final static int GO_COMPAMY = 3;
    /**
     * 打开/关闭 电子狗
     */
    public final static int E_DOG = 4;
    /**
     * 切换地图
     */
    public final static int BUTTON_EXCHANGE_ID = 5;
    /**
     * 删除通知
     */
    public final static int BUTTON_DELETE_NOFICY_ID = 6;

    public final static String INTENT_BUTTONID_TAG = "ButtonId";

    private boolean mShowMaps = false;

    private NotificationManager mNotificationManager;

    /**
     * 列表中的第1个
     */
    private static final int LIST_ID_1 = 7;

    /**
     * 列表中的第2个
     */
    private static final int LIST_ID_2 = 8;

    /**
     * 列表中的第3个
     */
    private static final int LIST_ID_3 = 9;

    /**
     * 列表中的更多按钮
     */
    private static final int LIST_ID_MORE = 10;


    /**
     * 通知ID
     */
    private static final int MUSIC_NOTIFY_ID = 100;

    public static boolean HAS_MAP_NOTIFY = false;


    /**
     * 第几次显示播放器的列表
     */
    private int mShowTimes = 0;


    public final static String INDEX_OF = "IndexOf";


    public static int sSelectedPopUpItem;

    private static final String OLD_SYSTEM_MAP = "com.meizu.net.map";
    private static final String NEW_SYSTEM_MAP = "com.baidu.BaiduMap.meizu";

    /**
     * 广播监听按钮点击时间
     */
    public class ButtonBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            Log.i(TAG,"onReceive......action="+action);
            if (action.equals(ACTION_CLICK)) {
                //通过传递过来的ID判断按钮点击属性或者通过getResultCode()获得相应点击事件
                int buttonId = intent.getIntExtra(INTENT_BUTTONID_TAG, 0);
                Log.i(TAG,"buttonId="+buttonId);
                switch (buttonId) {
                    case GO_MAP:
                        Log.i(TAG,"进入地图");
                        break;
                    case GO_HOME:
                        Log.i(TAG,"回家");
                        break;
                    case GO_COMPAMY:
                        Log.i(TAG,"去公司");
                        break;
                    case E_DOG:
                        Log.i(TAG,"电子狗");
                        if (mMapsPkgNameList.contains(OLD_SYSTEM_MAP) ||
                                mMapsPkgNameList.contains(NEW_SYSTEM_MAP)) {
                            mOpenEDog = !mOpenEDog;
                        } else {
                            mOpenEDog = false;
                        }
                        sendNotification();
                        break;
                    case BUTTON_EXCHANGE_ID:
                        mShowTimes = 0;
                        mShowMaps = !mShowMaps;
                        Log.i(TAG,"切换地图");
                        sendNotification();
                        break;
                    case BUTTON_DELETE_NOFICY_ID:
                        mNotificationManager.cancel(MUSIC_NOTIFY_ID);
                        HAS_MAP_NOTIFY = false;
                        Log.e(TAG,"删除地图通知...音乐通知是否还在:"+ MusicPlayService.HAS_MUSIC_NOTIFY);
                        if (!MusicPlayService.HAS_MUSIC_NOTIFY) {
                            sendBroadcast(new Intent(DriveModeSettingsActivity.MZ_ACTION_DRIVER_MODE_STOP));
                        }
                        break;
                    case LIST_ID_MORE:
                        mShowTimes++;
                        Log.i(TAG,"BUTTON_NEXT_SHOW_ID...mShowTimes="+mShowTimes);
                        sendNotification();
                        break;
                    case LIST_ID_1:
                    case LIST_ID_2:
                    case LIST_ID_3:
                        sSelectedPopUpItem = intent.getIntExtra(INDEX_OF, 0);
                        mShowMaps = !mShowMaps;
                        Log.i(TAG,"BUTTON_THIRD_IN_LIST_ID...第"+mShowTimes+"列 第"+sSelectedPopUpItem+"个");
                        sendNotification();
                        mShowTimes = 0;
                    default:
                        break;
                }
            }
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"onCreate......");
        initButtonReceiver();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MapBinder();
    }

    public class MapBinder extends Binder {
        public MapNotifyService getMapService() {
            return MapNotifyService.this;
        }
    }

    public final static String MAP_UPDATE_KEY = "map_update_key";
    public final static String MAP_INIT_SHOWTIME_KEY = "map_init_show_time";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.v(TAG,"onStartCommand......");
        if (intent == null) return super.onStartCommand(intent, flags, startId);

        if (intent.hasExtra(MAP_UPDATE_KEY)) {
            Bundle bundle = intent.getExtras();
            boolean onItemClick = false;
            if (bundle != null) {
                onItemClick = bundle.getBoolean(MAP_UPDATE_KEY);
                if (!onItemClick) { //onResume
                    int initShowTime = 0;
                    if (intent.hasExtra(MAP_INIT_SHOWTIME_KEY)) {
                        Bundle bundle1 = intent.getExtras();
                        if (bundle1 != null) {
                            initShowTime = bundle1.getInt(MAP_INIT_SHOWTIME_KEY);
                        }
                    }
                    mShowTimes = initShowTime;
                    initOnResume();
                } else { //PopUpWindow ItemClick

                }

            }
            sendNotification();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void initOnResume() {
        initMapInfos();
        if (!mMapsPkgNameList.contains(OLD_SYSTEM_MAP) &&
                !mMapsPkgNameList.contains(NEW_SYSTEM_MAP)) {
            mOpenEDog = false;
        }
    }

    private void initMapInfos() {
        Log.i(TAG,"initMapInfos.....");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse("geo:39.940409,116.355257?q=西直门");
        intent.setData(uri);
        ResolveInfo resolveInfo;
        mMapsList = new ArrayList<Maps>();
        List<ResolveInfo> currentResolveList = getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY | PackageManager.GET_RESOLVED_FILTER);
        int currentResolveListSize = currentResolveList.size();
        Log.i(TAG,"currentResolveListSize="+currentResolveListSize);
        for (int i = 0; i < currentResolveListSize; i++) {
            resolveInfo = currentResolveList.get(i);
            Maps maps = new Maps();
            maps.title = resolveInfo.loadLabel(getPackageManager()).toString();
            maps.icon = resolveInfo.loadIcon(getPackageManager());
            mMapsList.add(maps);
            mMapsPkgNameList.add(resolveInfo.activityInfo.packageName);
            Log.i(TAG,"title="+maps.title+" pkg="+resolveInfo.activityInfo.packageName);
            if (TextUtils.equals("com.meizu.net.map", resolveInfo.activityInfo.packageName)) {
                sSelectedPopUpItem = i;
            }
        }
    }

    public ArrayList<Maps> getMapsList() {
        return mMapsList;
    }

    private boolean mOpenEDog = true;

    private void sendNotification() {
        Log.v(TAG,"sendNotification......");
        HAS_MAP_NOTIFY = true;

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification =new Notification();
        notification.icon=R.mipmap.ic_launcher;
        notification.when=System.currentTimeMillis();
        notification.flags= Notification.FLAG_AUTO_CANCEL;
        //跳转意图
        Intent intent = new Intent(ACTION_CLICK);
        //建立一个RemoteView的布局，并通过RemoteView加载这个布局
        RemoteViews remoteViews = new RemoteViews(getPackageName(),R.layout.map_notification_layout);

        //点击通知，打开指定的activity
        int requestCode = (int) SystemClock.uptimeMillis();
        Intent toActivityIntent = new Intent(getBaseContext(), MapActivity.class);
        toActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), requestCode, toActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.map_notify_ll, pendingIntent);

        /* 删除 按钮 */
        intent.putExtra(INTENT_BUTTONID_TAG, BUTTON_DELETE_NOFICY_ID);
        PendingIntent intent_delete = PendingIntent.getBroadcast(this, BUTTON_DELETE_NOFICY_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.delete_notify, intent_delete);

        /* 切换地图 */
        intent.putExtra(INTENT_BUTTONID_TAG, BUTTON_EXCHANGE_ID);
        PendingIntent intent_exchange_map = PendingIntent.getBroadcast(this, BUTTON_EXCHANGE_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setImageViewBitmap(R.id.notification_custom_map_icon, drawableToBitmap(mMapsList.get(sSelectedPopUpItem).icon));
        remoteViews.setOnClickPendingIntent(R.id.notification_custom_map_icon, intent_exchange_map);

        if (!mShowMaps) { //不是显示要选择的地图
            remoteViews.setViewVisibility(R.id.notification_map_controller_ll, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.notification_maps_icon_ll, View.GONE);
            remoteViews.setTextViewText(R.id.notification_hint_title, mMapsList.get(sSelectedPopUpItem).title);
            remoteViews.setTextViewText(R.id.notification_map_title, mMapsList.get(sSelectedPopUpItem).title);

            /* 进入地图 */
            intent.putExtra(INTENT_BUTTONID_TAG, GO_MAP);
            PendingIntent intent_go_map = PendingIntent.getBroadcast(this, GO_MAP, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.notification_map, intent_go_map);
            /* 回家 */
            intent.putExtra(INTENT_BUTTONID_TAG, GO_HOME);
            PendingIntent intent_home = PendingIntent.getBroadcast(this, GO_HOME, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.notification_home, intent_home);
            /* 去公司 */
            intent.putExtra(INTENT_BUTTONID_TAG, GO_COMPAMY);
            PendingIntent intent_company = PendingIntent.getBroadcast(this, GO_COMPAMY, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.notification_company, intent_company);
            /* 电子狗 */
            intent.putExtra(INTENT_BUTTONID_TAG, E_DOG);
            PendingIntent intent_e_dog = PendingIntent.getBroadcast(this, E_DOG, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.notification_e_dog, intent_e_dog);

            if (mOpenEDog) {
                remoteViews.setImageViewResource(R.id.notification_e_dog, R.drawable.notify_open_e_dog_icon);
            } else {
                remoteViews.setImageViewResource(R.id.notification_e_dog, R.drawable.notify_close_e_dog_icon);
            }

        } else {
            remoteViews.setViewVisibility(R.id.notification_map_controller_ll, View.GONE);
            remoteViews.setViewVisibility(R.id.notification_maps_icon_ll, View.VISIBLE);
            remoteViews.setTextViewText(R.id.notification_hint_title, getResources().getString(R.string.select_maps));


            int listSize = mMapsList.size() - mShowTimes * 3;
            Log.i("####","listSize="+listSize+" mShowTimes="+mShowTimes);
            if (listSize == 1) {
                remoteViews.setViewVisibility(R.id.notification_media_icon31, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon32, View.VISIBLE);
                remoteViews.setImageViewBitmap(R.id.notification_media_icon32, drawableToBitmap(mMapsList.get(mShowTimes * 3).icon));
                remoteViews.setViewVisibility(R.id.notification_media_icon33, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon21, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon22, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_more, View.GONE);

                intent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_1);
                intent.putExtra(INDEX_OF, mShowTimes * 3);
                PendingIntent intent_next1 = PendingIntent.getBroadcast(this, LIST_ID_1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon32, intent_next1);
            } else if (listSize == 2) {
                remoteViews.setViewVisibility(R.id.notification_media_icon32, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon31, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon33, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon21, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.notification_media_icon22, View.VISIBLE);
                remoteViews.setImageViewBitmap(R.id.notification_media_icon21, drawableToBitmap(mMapsList.get(mShowTimes * 3).icon));
                remoteViews.setImageViewBitmap(R.id.notification_media_icon22, drawableToBitmap(mMapsList.get(mShowTimes * 3 + 1).icon));
                remoteViews.setViewVisibility(R.id.notification_media_more, View.GONE);

                intent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_1);
                intent.putExtra(INDEX_OF, mShowTimes * 3);
                PendingIntent intent_next1 = PendingIntent.getBroadcast(this, LIST_ID_1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon21, intent_next1);

                intent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_2);
                intent.putExtra(INDEX_OF, mShowTimes * 3 + 1);
                PendingIntent intent_next2 = PendingIntent.getBroadcast(this, LIST_ID_2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon22, intent_next2);

            } else if (listSize == 3) {
                remoteViews.setViewVisibility(R.id.notification_media_icon31, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.notification_media_icon32, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.notification_media_icon33, View.VISIBLE);
                remoteViews.setImageViewBitmap(R.id.notification_media_icon31, drawableToBitmap(mMapsList.get(mShowTimes * 3).icon));
                remoteViews.setImageViewBitmap(R.id.notification_media_icon32, drawableToBitmap(mMapsList.get(mShowTimes * 3 + 1).icon));
                remoteViews.setImageViewBitmap(R.id.notification_media_icon33, drawableToBitmap(mMapsList.get(mShowTimes * 3 + 2).icon));
                remoteViews.setViewVisibility(R.id.notification_media_icon21, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon22, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_more, View.GONE);

                intent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_1);
                intent.putExtra(INDEX_OF, mShowTimes * 3);
                PendingIntent intent_next1 = PendingIntent.getBroadcast(this, LIST_ID_1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon31, intent_next1);

                intent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_2);
                intent.putExtra(INDEX_OF, mShowTimes * 3 + 1);
                PendingIntent intent_next2 = PendingIntent.getBroadcast(this, LIST_ID_2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon32, intent_next2);

                intent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_3);
                intent.putExtra(INDEX_OF, mShowTimes * 3 + 2);
                PendingIntent intent_next3 = PendingIntent.getBroadcast(this, LIST_ID_3, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon33, intent_next3);
            } else {
                remoteViews.setViewVisibility(R.id.notification_media_icon32, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.notification_media_icon31, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.notification_media_icon33, View.VISIBLE);
                remoteViews.setImageViewBitmap(R.id.notification_media_icon31, drawableToBitmap(mMapsList.get(mShowTimes * 3).icon));
                remoteViews.setImageViewBitmap(R.id.notification_media_icon32, drawableToBitmap(mMapsList.get(mShowTimes * 3 + 1).icon));
                remoteViews.setImageViewBitmap(R.id.notification_media_icon33, drawableToBitmap(mMapsList.get(mShowTimes * 3 + 2).icon));
                remoteViews.setViewVisibility(R.id.notification_media_icon21, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon22, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_more, View.VISIBLE);

                intent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_1);
                intent.putExtra(INDEX_OF, mShowTimes * 3);
                PendingIntent intent_next1 = PendingIntent.getBroadcast(this, LIST_ID_1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon31, intent_next1);

                intent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_2);
                intent.putExtra(INDEX_OF, mShowTimes * 3 + 1);
                PendingIntent intent_next2 = PendingIntent.getBroadcast(this, LIST_ID_2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon32, intent_next2);

                intent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_3);
                intent.putExtra(INDEX_OF, mShowTimes * 3 + 2);
                PendingIntent intent_next3 = PendingIntent.getBroadcast(this, LIST_ID_3, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon33, intent_next3);

                intent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_MORE);
                PendingIntent intent_next4 = PendingIntent.getBroadcast(this, LIST_ID_MORE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_more, intent_next4);
            }

        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContent(remoteViews)
                .setContentIntent(getDefalutIntent(Notification.FLAG_AUTO_CANCEL))
                .setWhen(System.currentTimeMillis())// 通知产生的时间，会在通知信息里显示
                .setTicker("正在播放")
                .setPriority(Notification.PRIORITY_MAX)// 设置该通知优先级
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher);
        Notification notify = builder.build();
        notify.flags = Notification.FLAG_ONGOING_EVENT; //之前是FLAG_AUTO_CANCEL，通知常驻在锁屏加上一个flag: show_in_keyguard,是flyme自己加的
        mNotificationManager.notify(MUSIC_NOTIFY_ID, notify);

    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                        : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        //canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;

    }

    /**
     * 带按钮的通知栏点击广播接收
     */
    public void initButtonReceiver() {
        bReceiver = new ButtonBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_CLICK);
        registerReceiver(bReceiver, intentFilter);
    }


    public PendingIntent getDefalutIntent(int flags){
        PendingIntent pendingIntent= PendingIntent.getActivity(this, 1, new Intent(), flags);
        return pendingIntent;
    }


}

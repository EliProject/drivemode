package com.drivemode.music;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
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
import android.widget.Toast;

import com.drivemode.map.MapNotifyService;
import com.drivemode.settings.DriveModeSettingsActivity;
import com.example.eli.drivemodedemo.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Eli on 2017/10/5.
 */

public class MusicPlayService extends Service
        implements MediaSessionManager.OnActiveSessionsChangedListener {


    private static final String TAG = "MusicPlayService_@@##";

    /**
     * 通知栏按钮点击事件对应的ACTION
     */
    public final static String ACTION_CLICK = "com.notifications.intent.action.customviewclick";
    public final static String INTENT_BUTTONID_TAG = "ButtonId";
    public final static String INDEX_OF = "IndexOf";

    /**
     * 上一首 按钮点击 ID
     */
    public final static int BUTTON_PREV_ID = 1;

    /**
     * 播放/暂停 按钮点击 ID
     */
    public final static int BUTTON_PALY_ID = 2;

    /**
     * 下一首 按钮点击 ID
     */
    public final static int BUTTON_NEXT_ID = 3;

    /**
     * 音乐播放器和播放按钮的显示切换
     */
    public final static int BUTTON_EXCHANGE_ID = 4;

    /**
     * 下一显示列表 按钮点击 ID
     */
    public final static int BUTTON_NEXT_SHOW_ID = 5;

    /**
     * 显示通知的列表中的第1个
     */
    public final static int LIST_ID_1 = 6;

    /**
     * 显示通知的列表中的第2个
     */
    public final static int LIST_ID_2 = 7;

    /**
     * 显示通知的列表中的第3个
     */
    public final static int LIST_ID_3 = 8;

    /**
     * 删除通知
     */
    public final static int BUTTON_DELETE_NOFICY_ID = 9;

    /**
     * 通知ID
     */
    private static final int MUSIC_NOTIFY_ID = 301;


    public static boolean HAS_MUSIC_NOTIFY = false;


    public final static String MUSIC_KEY = "music_key";
    public final static String MUSIC_UPDATE_KEY = "music_update_key";
    public final static String MUSIC_UPDATE_ITEM_KEY = "music_update_item";
    public final static String MUSIC_PLAY = "play";
    public final static String MUSIC_PAUSE = "pause";
    public final static String MUSIC_NEXT = "next";
    public final static String MUSIC_PRE = "pre";


    /**
     * 通知栏按钮广播
     */
    public NotifyBroadcastReceiver mNotifyReceiver;

    /**
     * 第几次显示播放器的列表
     */
    private int mShowTimes = 0;


    private MediaSessionManager mMediaSessionManager;


    private MediaController mMediaController = null;

    private NotificationManager mNotificationManager;
    
    private Context mContext;
    
    public static int sSelectedItem;

    public interface OnMediaInfoUpdateListener {
        void onMediaInfoUpdate(MediaMetadata metadata);
        void onMediaPlayBtnUpdate(int state);
        void onMusicPlayerIconUpdate(Drawable drawableId);
    }

    private OnMediaInfoUpdateListener mOnMediaInfoUpdateListener;


    /**
     * 是否显示音乐控制面板
     */
    private boolean mShowMediaController = true;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"onCreate");

        mContext = getApplicationContext();
        registerNotifyReceiver();
        mShowTimes = 0;
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        initMusicPlayerInfos();

    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"onStartCommand...mMediaController="+mMediaController);

        if (intent == null) return super.onStartCommand(intent, flags, startId);

        if (intent.hasExtra(MUSIC_KEY)) {//播放歌曲或者切换歌曲的时候
            String values = intent.getStringExtra(MUSIC_KEY);
            Log.i(TAG,"values="+values);
            if (mMediaController != null) {
                if (TextUtils.equals(values, MUSIC_PLAY)) {
                    play();
                } else if (TextUtils.equals(values, MUSIC_PAUSE)) {
                    pause();
                } else if (TextUtils.equals(values, MUSIC_PRE)) {
                    skipToPrevious();
                } else if (TextUtils.equals(values, MUSIC_NEXT)) {
                    skipToNext();
                }
            }
        } else if (intent.hasExtra(MUSIC_UPDATE_KEY)) {//onResume或者点击popUp时候
            Bundle bundle = intent.getExtras();
            boolean onItemClick = false;
            if (bundle != null) {
                onItemClick = bundle.getBoolean(MUSIC_UPDATE_KEY);
                if (onItemClick) {//onItemClick才有辅助，onResume时候addOnActiveSessionsChangedListener有初始化sSelectedItem
                    sSelectedItem = bundle.getInt(MUSIC_UPDATE_ITEM_KEY);
                }
                Log.i(TAG," sSelectedItem="+sSelectedItem+" values="+onItemClick);
            }
            addOnActiveSessionsChangedListener(onItemClick);
        } else if ("com.meizu.flyme.drivemode.music".equals(intent.getAction())) {
            String pkg = intent.getExtras().getString("pkg");
            for (int i = 0; i < mMusicPlayerPkgNameList.size(); i++) {
                if (pkg.equals(mMusicPlayerPkgNameList.get(i))) {
                    sSelectedItem = i;
                    addOnActiveSessionsChangedListener(true);
                    break;
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }


    //切换到上一首歌
    private void skipToPrevious() {//startService切换
        mMediaController.getTransportControls().skipToPrevious();
    }

    //切换到下一首歌
    private void skipToNext() { //startService切换
        mMediaController.getTransportControls().skipToNext();
    }
    //暂停
    public void pause() {
        mMediaController.getTransportControls().pause();
    }
    //播放
    private void play() {//startService切换
        mMediaController.getTransportControls().play();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy...");
        mMediaSessionManager.removeOnActiveSessionsChangedListener(this);
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaCallback);
            Log.d(TAG, "MediaController removed");
        }
    }

    public void setOnMediaInfoUpdateListener(OnMediaInfoUpdateListener listener) {
        mOnMediaInfoUpdateListener = listener;
    }

    /**
     * 带按钮的通知栏点击广播接收
     */
    public void registerNotifyReceiver() {
        mNotifyReceiver = new NotifyBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_CLICK);
        registerReceiver(mNotifyReceiver, intentFilter);
    }

    public boolean isPlay() {
        if (mMediaController == null || mMediaController.getPlaybackState() == null) return false;
        return mMediaController.getPlaybackState().getState() == PlaybackState.STATE_PLAYING;
    }

    private List<MediaController> removeDuplicate(List<MediaController> controllers) {
        List<MediaController> newList = new ArrayList<>();
        List<String> newPkgNameList = new ArrayList<>();
        int size = controllers.size();
        for (int i = 0; i < size; i++) {
            if (!newPkgNameList.contains(controllers.get(i).getPackageName())) {
                newPkgNameList.add(controllers.get(i).getPackageName());
                newList.add(controllers.get(i));
            }
        }
        return newList;
    }

    private ArrayList<String> mMusicPlayerPkgNameList = new ArrayList<>();
    private ArrayList<MusicPlayers> mMusicPlayersList;

    //酷狗和百度音乐没有走音乐框架，获取他们的信息需要通过通知栏获取，暂时屏蔽这两个播放器，后面再适配
    List<String> SPECIALLIST = Arrays.asList(
            MusicPlayerActions.MEIZU_MUSIC_PKG,
            MusicPlayerActions.XIAMI_MUSIC_PKG,
            //MusicPlayerActions.KUGOU_MUSIC_PKG,
            MusicPlayerActions.BAIDU_MUSIC_PKG,
            MusicPlayerActions.QQ_MUSIC_PKG
    );

    class MusicPlayers {
        Drawable icon;
        String title;
    }

    private void initMusicPlayerInfos() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse("file://test.mp3");
        intent.setData(uri);
        intent.setDataAndType(uri, "audio/mpeg");
        ResolveInfo resolveInfo;
        mMusicPlayersList = new ArrayList<MusicPlayers>();
        List<ResolveInfo> currentResolveList = getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY | PackageManager.GET_RESOLVED_FILTER);
        int currentResolveListSize = currentResolveList.size();
        for (int i = 0; i < currentResolveListSize; i++) {
            resolveInfo = currentResolveList.get(i);
            if (!SPECIALLIST.contains(resolveInfo.activityInfo.packageName)) {
                Log.i(TAG,resolveInfo.activityInfo.packageName+"不在指定名单里面");
                continue;
            }
            MusicPlayers musicPlayers = new MusicPlayers();
            musicPlayers.title = resolveInfo.loadLabel(getPackageManager()).toString();
            musicPlayers.icon = resolveInfo.loadIcon(getPackageManager());
            mMusicPlayersList.add(musicPlayers);
            mMusicPlayerPkgNameList.add(resolveInfo.activityInfo.packageName);
            Log.i(TAG, "mMusicPlayerPkgNameList=" + mMusicPlayerPkgNameList);
        }
    }


    public void addOnActiveSessionsChangedListener(boolean onItemClick) {
        try {
            mMediaSessionManager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
            ComponentName cn = new ComponentName(getApplicationContext(), MyNotificationListenerService.class);
            List<MediaController> controllers = removeDuplicate(mMediaSessionManager.getActiveSessions(cn));
            Log.v(TAG, "addOnActiveSessionsChangedListener...controllers=" + controllers);

            if (!onItemClick) {//onResume()
                sClickId = INIT_CLICK_ID;
                if (controllers != null && controllers.size() > 0 && mMusicPlayerPkgNameList.contains(controllers.get(0).getPackageName())) {
                    sSelectedItem = mMusicPlayerPkgNameList.indexOf(controllers.get(0).getPackageName());
                } else {
                    sSelectedItem = mMusicPlayerPkgNameList.indexOf(MusicPlayerActions.MEIZU_MUSIC_PKG);
                }
            }

            Log.i(TAG, "addOnActiveSessionsChangedListener, sSelectedItem="+sSelectedItem+" onItemClick="+onItemClick);

            if (onItemClick) {
                Log.v(TAG, "判断当前是否播放:"+isPlay());
                /*if (isPlay()) {
                    pause();//暂停原来的播放器
                    Log.v(TAG, "暂停原来的播放器");
                }*/
                //Log.v(TAG, "暂停原来的播放器后，判断当前是否播放:"+isPlay());
                Log.v(TAG, "选择新的音乐播放器:" + mMusicPlayerPkgNameList.get(sSelectedItem));
                MusicPlayerActions.getInstance().startServiceOrBroadcastWithActions(mMusicPlayerPkgNameList.get(sSelectedItem),
                        MusicPlayerActions.PLAY_OR_PAUSE, getApplicationContext());//激活新的播放器
                Log.v(TAG, "通过startServiceOrBroadcastWithActions触发激活新的音乐播放器,可能播放，可能暂停");
                if (isPlay()) {
                    mMediaController.getTransportControls().pause();
                    Log.v(TAG, "把新的播放器暂停");
                }

            }

            if (mOnMediaInfoUpdateListener != null) {
                mOnMediaInfoUpdateListener.onMusicPlayerIconUpdate(mMusicPlayersList.get(sSelectedItem).icon);
            }
            printLog(controllers);
            onActiveSessionsChanged(controllers);
            mMediaSessionManager.addOnActiveSessionsChangedListener(this, cn);
        } catch (SecurityException e) {
            Log.e(TAG, "No Notification Access e=" + e, new Exception());
            Toast.makeText(getBaseContext(),"No Notification Access", Toast.LENGTH_SHORT).show();
            new AlertDialog.Builder(getApplicationContext())
                    .setTitle(R.string.dialog_notification_access_title)
                    .setMessage(R.string.dialog_notification_access_message)
                    .setPositiveButton(R.string.dialog_notification_access_positive, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.dialog_notification_access_negative, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
    }

    private void printLog(List<MediaController> controllers) {
        for (int i = 0; i < controllers.size(); i++) {
            MediaController mediaController = controllers.get(i);
            Log.d(TAG, "111第" + i + "个　pkgName=" + mediaController.getPackageName()
                    + " PlaybackState=" + mediaController.getPlaybackState()
                    + " getQueueTitle=" + mediaController.getQueueTitle());
        }
    }

    /**
     * 广播监听按钮点击时间
     */
    private class NotifyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            Log.i(TAG, "onReceive......action="+action);
            if (action.equals(ACTION_CLICK)) {
                //通过传递过来的ID判断按钮点击属性或者通过getResultCode()获得相应点击事件
                int buttonId = intent.getIntExtra(INTENT_BUTTONID_TAG, 0);
                sClickId = INIT_CLICK_ID;
                Log.i(TAG, "buttonId=" + buttonId);
                switch (buttonId) {
                    case BUTTON_PREV_ID:
                        Log.d(TAG, "上一首");
                        mMediaController.getTransportControls().skipToPrevious();
                        Toast.makeText(getApplicationContext(), "上一首", Toast.LENGTH_SHORT).show();
                        break;
                    case BUTTON_PALY_ID:
                        String play_status = "";
                        if (!isPlay()) {
                            mMediaController.getTransportControls().play();
                            play_status = "开始播放";
                        } else {
                            mMediaController.getTransportControls().pause();
                            play_status = "已暂停";
                        }
                        Log.w(TAG, play_status);
                        Toast.makeText(getApplicationContext(), play_status, Toast.LENGTH_SHORT).show();
                        break;
                    case BUTTON_NEXT_ID:
                        Log.d(TAG, "下一首");
                        mMediaController.getTransportControls().skipToNext();
                        Toast.makeText(getApplicationContext(), "下一首", Toast.LENGTH_SHORT).show();
                        break;
                    case BUTTON_EXCHANGE_ID:
                        mShowTimes = 0;
                        mShowMediaController = !mShowMediaController;
                        showCustomNotification();
                        Log.i(TAG, "BUTTON_EXCHANGE_ID...");
                        break;
                    case BUTTON_NEXT_SHOW_ID:
                        mShowTimes++;
                        showCustomNotification();
                        Log.i(TAG, "BUTTON_NEXT_SHOW_ID...mShowTimes=" + mShowTimes);
                        break;
                    case LIST_ID_1:
                    case LIST_ID_2:
                    case LIST_ID_3:
                        sSelectedItem = intent.getIntExtra(INDEX_OF, 0);
                        addOnActiveSessionsChangedListener(true);
                        mShowMediaController = !mShowMediaController;
                        Log.i(TAG, "LIST_ID_3...第" + mShowTimes + "列 第" + sSelectedItem + "个");
                        mShowTimes = 0;
                        break;
                    case BUTTON_DELETE_NOFICY_ID:
                        mNotificationManager.cancel(MUSIC_NOTIFY_ID);
                        HAS_MUSIC_NOTIFY = false;
                        Log.e(TAG, "mNotificationManager.删除音乐的通知,　地图通知是否还在:"+ MapNotifyService.HAS_MAP_NOTIFY);
                        if (!MapNotifyService.HAS_MAP_NOTIFY) {
                            sendBroadcast(new Intent(DriveModeSettingsActivity.MZ_ACTION_DRIVER_MODE_STOP));
                        }
                        break;
                    default:
                        Intent i = new Intent(getApplicationContext(), MediaActivity.class);
                        startActivity(i);
                        showCustomNotification();
                        Log.i(TAG, "default.....");
                        break;
                }
            }
        }
    }


    /**
     * 显示通知，有需要显示或者更新通知的状态有: 切歌(onMetadataChanged),播放状态切换(onPlaybackStateChanged),点击暂停/播放按钮(BUTTON_PALY_ID),
     * 通知栏中在显示播放器列表中点击更多按钮(BUTTON_NEXT_SHOW_ID)或者点击切换按钮(BUTTON_EXCHANGE_ID)或者
     * 点击音乐播放器(LIST_ID_1,LIST_ID_2,LIST_ID_3)
     */
    private void showCustomNotification() {

        HAS_MUSIC_NOTIFY = true;
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.music_notification_custom_view);
        //API3.0 以上的时候显示按钮，否则消失
        Log.v(TAG, "showCustomNotification sSelectedItem=" + sSelectedItem);

        //点击的事件处理
        Intent buttonIntent = new Intent(ACTION_CLICK);

        //点击通知，打开指定的activity
        int requestCode = (int) SystemClock.uptimeMillis();
        Intent intent = new Intent(getBaseContext(), MediaActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notification_ll, pendingIntent);


        /* 切换播放器  */
        buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_EXCHANGE_ID);
        PendingIntent intent_exchange = PendingIntent.getBroadcast(this, BUTTON_EXCHANGE_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setImageViewBitmap(R.id.notification_music_player_custom_icon, drawableToBitmap(mMusicPlayersList.get(sSelectedItem).icon));
        remoteViews.setOnClickPendingIntent(R.id.notification_music_player_custom_icon, intent_exchange);

        /* 删除 按钮 */
        buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_DELETE_NOFICY_ID);
        PendingIntent intent_delete = PendingIntent.getBroadcast(this, BUTTON_DELETE_NOFICY_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.delete_notify, intent_delete);

        if (mShowMediaController) {
            remoteViews.setViewVisibility(R.id.notification_music_controller_icon, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.notification_music_player_icon_ll, View.GONE);

            remoteViews.setTextViewText(R.id.notification_music_title, mMediaTitle);
            /* 上一首按钮 */
            buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_PREV_ID);
            //这里加了广播，所及INTENT的必须用getBroadcast方法
            PendingIntent intent_prev = PendingIntent.getBroadcast(this, BUTTON_PREV_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.notification_media_prev, intent_prev);
            /* 播放/暂停  按钮 */
            buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_PALY_ID);
            PendingIntent intent_play = PendingIntent.getBroadcast(this, BUTTON_PALY_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.notification_media_play, intent_play);
            /* 下一首 按钮  */
            buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_NEXT_ID);
            PendingIntent intent_next = PendingIntent.getBroadcast(this, BUTTON_NEXT_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.notification_media_next, intent_next);
            Log.w(TAG, "showCustomNotification isPlay=" + isPlay() + " title=" + mMediaTitle);
            if (isPlay()) {
                remoteViews.setImageViewResource(R.id.notification_media_play, R.drawable.ic_pause_white_24dp);
            } else {
                remoteViews.setImageViewResource(R.id.notification_media_play, R.drawable.ic_play_arrow_white_24dp);
            }
        } else {
            remoteViews.setViewVisibility(R.id.notification_music_controller_icon, View.GONE);
            remoteViews.setViewVisibility(R.id.notification_music_player_icon_ll, View.VISIBLE);
            remoteViews.setTextViewText(R.id.notification_music_title, getResources().getString(R.string.select_music_apps));

            int listSize = mMusicPlayersList.size() - mShowTimes * 3;
            Log.i("####", "listSize=" + listSize + " mShowTimes=" + mShowTimes);
            if (listSize == 1) {
                remoteViews.setViewVisibility(R.id.notification_media_icon31, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon32, View.VISIBLE);
                remoteViews.setImageViewBitmap(R.id.notification_media_icon32, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3).icon));
                remoteViews.setViewVisibility(R.id.notification_media_icon33, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon21, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon22, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_more, View.GONE);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_1);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3);
                PendingIntent intent_next1 = PendingIntent.getBroadcast(this, LIST_ID_1, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon32, intent_next1);
            } else if (listSize == 2) {
                remoteViews.setViewVisibility(R.id.notification_media_icon32, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon31, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon33, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon21, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.notification_media_icon22, View.VISIBLE);
                remoteViews.setImageViewBitmap(R.id.notification_media_icon21, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3).icon));
                remoteViews.setImageViewBitmap(R.id.notification_media_icon22, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3 + 1).icon));
                remoteViews.setViewVisibility(R.id.notification_media_more, View.GONE);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_1);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3);
                PendingIntent intent_next1 = PendingIntent.getBroadcast(this, LIST_ID_1, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon21, intent_next1);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_2);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3 + 1);
                PendingIntent intent_next2 = PendingIntent.getBroadcast(this, LIST_ID_2, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon22, intent_next2);

            } else if (listSize == 3) {
                remoteViews.setViewVisibility(R.id.notification_media_icon31, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.notification_media_icon32, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.notification_media_icon33, View.VISIBLE);
                remoteViews.setImageViewBitmap(R.id.notification_media_icon31, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3).icon));
                remoteViews.setImageViewBitmap(R.id.notification_media_icon32, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3 + 1).icon));
                remoteViews.setImageViewBitmap(R.id.notification_media_icon33, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3 + 2).icon));
                remoteViews.setViewVisibility(R.id.notification_media_icon21, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon22, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_more, View.GONE);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_1);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3);
                PendingIntent intent_next1 = PendingIntent.getBroadcast(this, LIST_ID_1, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon31, intent_next1);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_2);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3 + 1);
                PendingIntent intent_next2 = PendingIntent.getBroadcast(this, LIST_ID_2, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon32, intent_next2);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_3);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3 + 2);
                PendingIntent intent_next3 = PendingIntent.getBroadcast(this, LIST_ID_3, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon33, intent_next3);
            } else {
                remoteViews.setViewVisibility(R.id.notification_media_icon32, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.notification_media_icon31, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.notification_media_icon33, View.VISIBLE);
                remoteViews.setImageViewBitmap(R.id.notification_media_icon31, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3).icon));
                remoteViews.setImageViewBitmap(R.id.notification_media_icon32, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3 + 1).icon));
                remoteViews.setImageViewBitmap(R.id.notification_media_icon33, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3 + 2).icon));
                remoteViews.setViewVisibility(R.id.notification_media_icon21, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon22, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_more, View.VISIBLE);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_1);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3);
                PendingIntent intent_next1 = PendingIntent.getBroadcast(this, LIST_ID_1, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon31, intent_next1);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_2);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3 + 1);
                PendingIntent intent_next2 = PendingIntent.getBroadcast(this, LIST_ID_2, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon32, intent_next2);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, LIST_ID_3);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3 + 2);
                PendingIntent intent_next3 = PendingIntent.getBroadcast(this, LIST_ID_3, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon33, intent_next3);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_NEXT_SHOW_ID);
                PendingIntent intent_next4 = PendingIntent.getBroadcast(this, BUTTON_NEXT_SHOW_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_more, intent_next4);
            }

        }
        mBuilder.setContent(remoteViews)
                .setContentIntent(getDefalutIntent(Notification.FLAG_AUTO_CANCEL))
                .setWhen(System.currentTimeMillis())// 通知产生的时间，会在通知信息里显示
                .setTicker("正在播放")
                .setPriority(Notification.PRIORITY_MAX)// 设置该通知优先级
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher);
        Notification notify = mBuilder.build();
        notify.flags = Notification.FLAG_ONGOING_EVENT; //之前是FLAG_AUTO_CANCEL,通知常驻在锁屏加上一个flag: show_in_keyguard,是flyme自己加的
        mNotificationManager.notify(MUSIC_NOTIFY_ID, notify);

    }


    public PendingIntent getDefalutIntent(int flags) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, new Intent(getBaseContext(), MediaActivity.class), flags);
        return pendingIntent;
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
     * Called when the list of active {@link MediaController MediaControllers} changes.
     *
     * @param controllers List of active MediaControllers
     */
    @Override
    public void onActiveSessionsChanged(List<MediaController> controllers) {
        Log.i(TAG, "onActiveSessionsChanged.. controllers.size=" + controllers.size() + " mMediaController是否为空: " + (mMediaController == null));
        if (controllers.size() > 0) {

            if (mMediaController != null) {
                //if (!controllers.get(0).getSessionToken().equals(mMediaController.getSessionToken())) {
                // Detach current controller

                MediaController mediaController = indexOfControllers(controllers, mMusicPlayerPkgNameList.get(sSelectedItem));
                if (mediaController== null) {
                    String s = "音乐播放器: "+mMusicPlayerPkgNameList.get(sSelectedItem)+"不在后台, 无法切换";
                    Log.w(TAG, s);
                    Toast.makeText(getBaseContext(),s, Toast.LENGTH_SHORT).show();
                    return;
                }

                mMediaController.unregisterCallback(mMediaCallback);
                Log.d(TAG, "MediaController removed");
                mMediaController = null;

                // Attach new controller
                mMediaController = mediaController;
                mMediaController.registerCallback(mMediaCallback);
                mMediaCallback.onMetadataChanged(mMediaController.getMetadata());
                mMediaCallback.onPlaybackStateChanged(mMediaController.getPlaybackState());
                Log.d(TAG, "MediaController set: " + mMediaController.getPackageName());

            } else {//应用启动，时候会跑这里
                // Attach new controller
                mMediaController = controllers.get(0);
                mMediaController.registerCallback(mMediaCallback);
                mMediaCallback.onMetadataChanged(mMediaController.getMetadata());
                mMediaCallback.onPlaybackStateChanged(mMediaController.getPlaybackState());
                Log.v(TAG, "应用启动..Attach new controller.MediaController set: " + mMediaController.getPackageName());
            }

        } else {
            if (mOnMediaInfoUpdateListener != null) {
                mOnMediaInfoUpdateListener.onMediaInfoUpdate(null);
            }
        }
    }

    private MediaController indexOfControllers(List<MediaController> controllers, String pkgName) {
        for (int i = 0;i < controllers.size();i++) {
            MediaController mediaController = controllers.get(i);
            if (TextUtils.equals(pkgName, mediaController.getPackageName())) {
                return mediaController;
            }
        }
        return null;
    }

    //避免回调多次不同的状态，导致 暂停/播放 的图标状态显示错误
    private boolean isPlayStateChange(int state) {
        /*if (sClickId == R.id.media_prev &&
                (state == PlaybackState.STATE_PAUSED || state ==PlaybackState.STATE_STOPPED)) {
            return false;
        }
        if (sClickId == R.id.media_next &&
                (state == PlaybackState.STATE_PAUSED || state ==PlaybackState.STATE_STOPPED)) {
            return false;
        }*/

        Log.w(TAG,"isPlayStateChange..sClickId="+sClickId);
        if (sClickId != INIT_CLICK_ID &&
                (state == PlaybackState.STATE_PAUSED || state ==PlaybackState.STATE_STOPPED)) {
            return false;
        }

        return true;
    }

    public static int sClickId;
    public static final int INIT_CLICK_ID = 0;
    public static final int CHANGEED_CLICK_ID = 0;

    private String mMediaTitle;

    /**
     * Callback for the MediaController.
     */
    private MediaController.Callback mMediaCallback = new MediaController.Callback() {

        @Override
        public void onAudioInfoChanged(MediaController.PlaybackInfo playbackInfo) {
            super.onAudioInfoChanged(playbackInfo);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            Log.d(TAG, "onMetadataChanged, 切歌,");

            if (metadata != null && metadata.getText(MediaMetadata.METADATA_KEY_TITLE) != null) {

                // Update media container
                Log.d(TAG, "onMetadataChanged,新的歌曲信息: title=" + metadata.getText(MediaMetadata.METADATA_KEY_TITLE) + " artist=" + metadata.getText(MediaMetadata.METADATA_KEY_ARTIST));

                mMediaTitle = metadata.getText(MediaMetadata.METADATA_KEY_TITLE).toString();
                if (mOnMediaInfoUpdateListener  != null) {
                    mOnMediaInfoUpdateListener.onMediaInfoUpdate(metadata);
                }
            } else {
                mMediaTitle = "";
                if (mOnMediaInfoUpdateListener  != null) {
                    mOnMediaInfoUpdateListener.onMediaInfoUpdate(null);
                }
                Log.d(TAG, "onMetadataChanged,这个播放器没有歌曲!...");
            }
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            super.onPlaybackStateChanged(state);

            Log.i(TAG, "onPlaybackStateChanged state=" + state);

            if (state != null) {
                Log.i(TAG, "onPlaybackStateChanged isPlayStateChange=" + isPlayStateChange(state.getState()));
                if (isPlayStateChange(state.getState())) {
                    // Update play/pause button
                    if (mOnMediaInfoUpdateListener != null) {
                        mOnMediaInfoUpdateListener.onMediaPlayBtnUpdate(state.getState());
                    }
                    showCustomNotification();
                }
            }
        }

        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            super.onQueueChanged(queue);
        }

        @Override
        public void onQueueTitleChanged(CharSequence title) {
            super.onQueueTitleChanged(title);
        }
    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MusicBinder();
    }

    public int getState() {//采用static方法
        return mMediaController.getPlaybackState().getState();
    }

    public String getControllerPkgName() {
        return mMediaController.getPackageName();
    }

    public boolean canNotShowPoPup() {//采用static方法
        return mMusicPlayersList.size() == 1;
    }

    public boolean hasTwoMusicPlayer() {//采用static方法
        return mMusicPlayersList.size() == 2;
    }

    public ArrayList<MusicPlayers> getMusicPlayersList() {//采用static变量
        return mMusicPlayersList;
    }

    public ArrayList<String> getMusicPlayerPkgNameList() {//采用static变量
        return mMusicPlayerPkgNameList;
    }

    public class MusicBinder extends Binder {
        public MusicPlayService getMusicPlayerService() {
            return MusicPlayService.this;
        }
    }
}

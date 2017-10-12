package com.drivemode.music;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by liyuanqin on 17-9-22.
 */

public class MusicPlayerActions {

    public static final String MEIZU_MUSIC_PKG = "com.meizu.media.music";

    public static final String KUGOU_MUSIC_PKG = "com.kugou.android";

    public static final String XIAMI_MUSIC_PKG = "fm.xiami.main";

    public static final String QQ_MUSIC_PKG = "com.tencent.qqmusic";

    public static final String BAIDU_MUSIC_PKG = "com.ting.mp3.android";


    private Context mContext;

    public static final String PLAY_OR_PAUSE = "play_or_pause";

    public static final String NEXT = "next";

    public static final String PREVIOUS = "previous";

    private static final MusicPlayerActions ourInstance = new MusicPlayerActions();

    public static MusicPlayerActions getInstance() {
        return ourInstance;
    }

    private MusicPlayerActions() {
    }


    /**
     *
     * 适配第三方音乐播放器接口
     * @param pkg 要执行任务的音乐播放器包名
     * @param action 音乐播放器执行的动作，目前支持有 {@link #PLAY_OR_PAUSE},{@link #NEXT},{@link #PREVIOUS}
     * @param context context
     *
     */
    public void startServiceOrBroadcastWithActions(String pkg, String action, Context context) {
        mContext = context;
        if (TextUtils.equals(pkg, MEIZU_MUSIC_PKG)) {
            meizuMusicActions(action);
        } else if (TextUtils.equals(pkg, KUGOU_MUSIC_PKG)) {
            kugouMusicActions(action);
        } else if (TextUtils.equals(pkg, QQ_MUSIC_PKG)) {
            qqMusicActions(action);
        } else if (TextUtils.equals(pkg, XIAMI_MUSIC_PKG)) {
            xiamiMusicActions(action);
        } else if (TextUtils.equals(pkg, BAIDU_MUSIC_PKG)) {
            baiduMusicActions(action);
        }
    }



    //魅族音乐　下一首歌的广播
    public final static int MEIZU_MUSIC_NEXT_ACTION = 1;
    //魅族音乐　暂停/播放　广播
    public final static int MEIZU_MUSIC_PAUSE_ACTION = 0;
    //魅族音乐　上一首歌的广播
    public final static int MEIZU_MUSIC_PREVIOUS_ACTION = 2;

    private void meizuMusicActions(String action) {
        Intent intent = new Intent("com.meizu.media.music.OPERATE");
        intent.putExtra("from", "Desktop");
        if (TextUtils.equals(action, PLAY_OR_PAUSE)) {
            intent.putExtra("action", MEIZU_MUSIC_PAUSE_ACTION);//暂停
            Log.i("@@@@","meizu MusicActions...暂停/播放");
        } else if (TextUtils.equals(action, NEXT)) {
            intent.putExtra("action", MEIZU_MUSIC_NEXT_ACTION);//下一首歌
        } else if (TextUtils.equals(action, PREVIOUS)) {
            intent.putExtra("action", MEIZU_MUSIC_PREVIOUS_ACTION);//上一首歌
        }
        mContext.sendBroadcast(intent);
    }


    //酷狗音乐　下一首歌的广播
    public final static String KUGOU_MUSIC_NEXT_ACTION = "com.kugou.android.music.musicservicecommand.next";
    //酷狗音乐　暂停广播
    public final static String KUGOU_MUSIC_PAUSE_ACTION = "com.kugou.android.music.musicservicecommand.pause";
    //酷狗音乐　暂停/播放　广播
    public final static String KUGOU_MUSIC_PAUSE_ACTION1 = "com.example.android.uamp.pause";
    //酷狗音乐　上一首歌的广播
    public final static String KUGOU_MUSIC_PREVIOUS_ACTION = "com.kugou.android.music.musicservicecommand.previous";
    //酷狗音乐　模式切换
    public final static String KUGOU_MUSIC_SWITCH_PLAYMODE_ACTION = "com.kugou.android.music.musicservicecommand.switch_playmode";


    private void kugouMusicActions(String action) {
        Intent intent = new Intent();
        if (TextUtils.equals(action, PLAY_OR_PAUSE)) {
            intent.setAction(KUGOU_MUSIC_PAUSE_ACTION);//暂停
            Log.i("@@@@","kugou MusicActions...暂停");
        } else if (TextUtils.equals(action, NEXT)) {
            intent.setAction(KUGOU_MUSIC_NEXT_ACTION);//下一首歌
        } else if (TextUtils.equals(action, PREVIOUS)) {
            intent.setAction(KUGOU_MUSIC_PREVIOUS_ACTION);//上一首歌
        }
        mContext.sendBroadcast(intent);
    }

    //qq音乐　下一首歌的广播
    public final static String QQ_MUSIC_NEXT_ACTION = "com.tencent.qqmusic.ACTION_SERVICE_NEXT_TASKBAR.QQMusicPhone";
    //qq音乐　暂停/播放　广播
    public final static String QQ_MUSIC_PAUSE_ACTION = "com.tencent.qqmusic.ACTION_SERVICE_TOGGLEPAUSE_TASKBAR.QQMusicPhone";
    //qq音乐　上一首歌的广播
    public final static String QQ_MUSIC_PREVIOUS_ACTION = "com.tencent.qqmusic.ACTION_SERVICE_PREVIOUS_TASKBAR.QQMusicPhone";
    //qq音乐　切换播放模式广播
    public final static String QQ_MUSIC_PLAY_MODE_ACTION = "com.tencent.qqmusic.ACTION_SERVICE_PLAY_MODE_WIDGET.QQMusicPhone";

    private void qqMusicActions(String action) {
        Intent intent = new Intent();
        if (TextUtils.equals(action, PLAY_OR_PAUSE)) {
            Log.i("@@@@","qqMusicActions...暂停/播放　QQ音乐");
            intent.setAction(QQ_MUSIC_PAUSE_ACTION);//暂停
        } else if (TextUtils.equals(action, NEXT)) {
            intent.setAction(QQ_MUSIC_NEXT_ACTION);//下一首歌
            Log.i("@@@@","qqMusicActions...下一首歌　QQ音乐");
        } else if (TextUtils.equals(action, PREVIOUS)) {
            intent.setAction(QQ_MUSIC_PREVIOUS_ACTION);//上一首歌
            Log.i("@@@@","qqMusicActions...上一首歌　QQ音乐");
        } else if (TextUtils.equals(action, PREVIOUS)) {
            intent.setAction(QQ_MUSIC_PLAY_MODE_ACTION);//模式切换
            Log.i("@@@@","qqMusicActions...切换播放模式　QQ音乐");
        }
        mContext.sendBroadcast(intent);
    }

    //虾米音乐服务启动方式
    public void startService() {
        Intent intent = new Intent();
        intent.setAction("fm.xiami.main.business.appwidget.APP_WIDGET_ACTION");
        intent.putExtra("fm.xiami.main.business.appwidget.APP_WIDGET_ACTION", "fm.xiami.main.business.appwidget.action.APP_WIDGET_ACTION_RESET");
        intent.setComponent(new ComponentName("fm.xiami.main","fm.xiami.main.service.MainService"));
        mContext.startService(intent);
    }


    //虾米音乐　下一首歌的广播
    public final static String XIAMI_MUSIC_NEXT_ACTION = "fm.xiami.main.business.notification.ACTION_NOTIFICATION_PLAYNEXT";
    //虾米音乐　暂停/播放　广播
    public final static String XIAMI_MUSIC_PAUSE_ACTION = "fm.xiami.main.business.notification.ACTION_NOTIFICATION_PLAYPAUSE";
    //虾米音乐　上一首歌的广播
    public final static String XIAMI_MUSIC_PREVIOUS_ACTION = "fm.xiami.main.business.notification.ACTION_NOTIFICATION_PLAYPREV";
    //虾米音乐　桌面歌词的广播
    public final static String XIAMI_MUSIC_DESKTOP_LYRIC_ACTION = "fm.xiami.main.business.notification.ACTION_NOTIFICATION_DESKTOP_LYRIC_TOGGLE";
    //虾米音乐　关闭通知的广播
    public final static String XIAMI_MUSIC_NOTIFICATION_CLOSE_ACTION = "fm.xiami.main.business.notification.ACTION_NOTIFICATION_CLOSE";

    private void xiamiMusicActions(String action) {
        Intent intent = new Intent();
        if (TextUtils.equals(action, PLAY_OR_PAUSE)) {
            intent.setAction(XIAMI_MUSIC_PAUSE_ACTION);//暂停
            Log.i("@@@@","xiami MusicActions...暂停/播放");
        } else if (TextUtils.equals(action, NEXT)) {
            intent.setAction(XIAMI_MUSIC_NEXT_ACTION);//下一首歌
        } else if (TextUtils.equals(action, PREVIOUS)) {
            intent.setAction(XIAMI_MUSIC_PREVIOUS_ACTION);//上一首歌
        }
        intent.setComponent(new ComponentName("fm.xiami.main","fm.xiami.main.service.PlayService"));
        mContext.startService(intent);
    }



    //百度音乐　下一首歌的广播
    public final static String BAIDU_MUSIC_NEXT_VALUE = "next_widget";
    //百度音乐　暂停/播放　广播
    public final static String BAIDU_MUSIC_PLAY_OR_PAUES_VALUE = "play_or_pause_widget";
    //百度音乐　上一首歌的广播
    public final static String BAIDU_MUSIC_PREVICOUS_VALUE = "previcous_widget";
    //百度音乐　隐藏或者显示桌面歌词的广播
    public final static String BAIDU_MUSIC_DESK_LYRIC_VALUE = "play_or_desk_lyric_widget";
    //百度音乐　模式切换广播
    public final static String BAIDU_MUSIC_MODE_VALUE = "mode_widget";

    public final static String BAIDU_MUSIC_PLAY_KEY = "command";


    private void baiduMusicActions(String action) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.ting.mp3.android","com.baidu.music.logic.service.MusicPlayService"));
        if (TextUtils.equals(action, PLAY_OR_PAUSE)) {
            intent.putExtra(BAIDU_MUSIC_PLAY_KEY,BAIDU_MUSIC_PLAY_OR_PAUES_VALUE);//暂停/播放
            Log.i("@@@@","baidu MusicActions...暂停/播放　");
        } else if (TextUtils.equals(action, NEXT)) {
            intent.putExtra(BAIDU_MUSIC_PLAY_KEY,BAIDU_MUSIC_NEXT_VALUE);//下一首歌
        } else if (TextUtils.equals(action, PREVIOUS)) {
            intent.putExtra(BAIDU_MUSIC_PLAY_KEY,BAIDU_MUSIC_PREVICOUS_VALUE);//上一首歌
        }

        mContext.startService(intent);
    }
}

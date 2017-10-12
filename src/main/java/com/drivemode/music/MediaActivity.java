package com.drivemode.music;

/**
 * Created by liyuanqin on 17-9-6.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;


import com.example.eli.drivemodedemo.R;

import java.util.ArrayList;
import java.util.List;


/**
 * Activity that shows media controls.
 */
public class MediaActivity extends Activity {

    private static final String TAG = "MediaActivity_@@@@";





    /**
     * "pref_key_show_media"
     */
    private boolean mPrefShowMedia = true;

    // Current track
    private TextView mMediaAlbum;
    private TextView mMediaArtist;
    private TextView mMediaTitle;

    // Next track
    private TextView mMediaUpNextArtist;
    private TextView mMediaUpNextTitle;

    // Controls
    private ImageView mMediaNext;
    private ImageView mMediaPlay;
    private ImageView mMediaPrev;
    private ImageView mMusicPlayerIcon;

   // private MediaController mMediaController = null;

    private SharedPreferences mSharedPref;

    private Button mSelectMusicePlayerBtn;

    private PopupWindow mSelectMusicPlayerPopupWIndow;
    private ListView mMusicPlayersListView;

    private MusicPlayService mMusicPlayService;


    private static final String PLAY_MUSIC_ACTION = "flyme.drivemode.service.playermusic";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()......");

        // Set layout
        setContentView(R.layout.musicplayer_layout);

        //getPerssion();
        Intent intent = new Intent();
        intent.setAction(PLAY_MUSIC_ACTION);
        intent.setPackage(getPackageName());
        this.bindService(intent,conn, Context.BIND_AUTO_CREATE);

        // Get views
        mMediaTitle = ((TextView) findViewById(R.id.media_title));
        mMediaArtist = ((TextView) findViewById(R.id.media_artist));
        mMediaAlbum = ((TextView) findViewById(R.id.media_album));
        mMediaPrev = ((ImageView) findViewById(R.id.media_prev));
        mMediaPlay = ((ImageView) findViewById(R.id.media_play));
        mMediaNext = ((ImageView) findViewById(R.id.media_next));
        mMusicPlayerIcon = ((ImageView) findViewById(R.id.music_player_icon));
        mMediaUpNextTitle = ((TextView) findViewById(R.id.media_up_next_title));
        mMediaUpNextArtist = ((TextView) findViewById(R.id.media_up_next_artist));
        mSelectMusicePlayerBtn = ((Button) findViewById(R.id.select_music_player));

        mMediaPrev.setOnClickListener(mMediaControlsListener);
        mMediaPlay.setOnClickListener(mMediaControlsListener);
        mMediaNext.setOnClickListener(mMediaControlsListener);
        mMusicPlayerIcon.setOnClickListener(mMediaControlsListener);
        mSelectMusicePlayerBtn.setOnClickListener(mMediaControlsListener);

        mMediaTitle.setSelected(true);


        //startService(intent);

        mMediaUpNextTitle.setVisibility(View.GONE);
        mMediaUpNextArtist.setVisibility(View.GONE);
        mMediaAlbum.setVisibility(View.GONE);
    }

    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMusicPlayService = ((MusicPlayService.MusicBinder) service).getMusicPlayerService();
            Log.i(TAG,"onServiceConnected..mMusicPlayService="+mMusicPlayService);
            mMusicPlayService.setOnMediaInfoUpdateListener(new MusicPlayService.OnMediaInfoUpdateListener() {
                @Override
                public void onMediaInfoUpdate(MediaMetadata metadata) {
                    if (metadata != null) {
                        mMediaTitle.setText(metadata.getText(MediaMetadata.METADATA_KEY_TITLE));
                        mMediaArtist.setText(metadata.getText(MediaMetadata.METADATA_KEY_ARTIST));
                        mMediaAlbum.setText(metadata.getText(MediaMetadata.METADATA_KEY_ALBUM));
                    } else {
                        mMediaTitle.setText(getString(R.string.no_music_hint));
                        mMediaArtist.setText("");
                        mMediaAlbum.setText("");
                    }
                }

                @Override
                public void onMediaPlayBtnUpdate(int state) {
                    if (mMediaPlay != null) {
                        switch (state) {
                            case PlaybackState.STATE_BUFFERING:
                            case PlaybackState.STATE_CONNECTING:
                                mMediaPlay.setVisibility(View.GONE);
                                break;
                            case PlaybackState.STATE_PLAYING:
                                mMediaPlay.setVisibility(View.VISIBLE);
                                mMediaPlay.setImageResource(R.drawable.ic_pause_white_24dp);
                                break;
                            default:
                                mMediaPlay.setVisibility(View.VISIBLE);
                                mMediaPlay.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                                break;
                        }
                    }
                }

                @Override
                public void onMusicPlayerIconUpdate(Drawable drawableId) {
                    mMusicPlayerIcon.setImageDrawable(drawableId);
                }
            });

            addOnActiveSessionsChangedListener();
            //mMusicPlayService.addOnActiveSessionsChangedListener(false);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMusicPlayService = null;
        }
    };

    /**
     * Detaches listeners.
     */
    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Attaches listeners and gets preferences.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume...mMusicPlayService="+mMusicPlayService);
        /*if (mMusicPlayService != null) {
            mMusicPlayService.addOnActiveSessionsChangedListener(false);
        }*/


        addOnActiveSessionsChangedListener();

        // Get preferences
        //mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
        mMusicPlayService.setOnMediaInfoUpdateListener(null);
        mMusicPlayService = null;
        Log.v(TAG, "onDestroy");
    }


    private void addOnActiveSessionsChangedListener() {
        Intent updateIntent=new Intent(MediaActivity.this.getApplicationContext(), MusicPlayService.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean(MusicPlayService.MUSIC_UPDATE_KEY,false);
        bundle.putInt(MusicPlayService.MUSIC_UPDATE_ITEM_KEY,11);
        updateIntent.putExtras(bundle);
        startService(updateIntent);
    }


    private void getPerssion() {
            try {
                MediaSessionManager mMediaSessionManager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
                ComponentName cn = new ComponentName(getApplicationContext(), MyNotificationListenerService.class);
                List<MediaController> controllers = mMediaSessionManager.getActiveSessions(cn);
            } catch (SecurityException e) {
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

                Log.e(TAG, "No Notification Access e=" + e, new Exception());
            }
    }

    /**
     * OnClickListener for the media controls.
     */
    private View.OnClickListener mMediaControlsListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (v.getId() == R.id.music_player_icon) {
                ArrayList<String> musicPlayerPkgNameList = mMusicPlayService.getMusicPlayerPkgNameList();
                Log.i(TAG, "点击了音乐播放器的icon,这个音乐播放器的componentName=" + musicPlayerPkgNameList.get(MusicPlayService.sSelectedItem));
                Intent intent = getPackageManager().getLaunchIntentForPackage(musicPlayerPkgNameList.get(MusicPlayService.sSelectedItem));
                startActivity(intent);
            } else {
                MusicPlayService.sClickId = v.getId();
                // Handle media controls
                switch (v.getId()) {
                    case R.id.media_prev:
                        Log.i(TAG, "上一首歌");
                        //mMusicPlayService.skipToPrevious();
                        Intent preIntent=new Intent(MediaActivity.this.getApplicationContext(), MusicPlayService.class);
                        preIntent.putExtra(MusicPlayService.MUSIC_KEY,MusicPlayService.MUSIC_PRE);
                        startService(preIntent);
                        break;
                    case R.id.media_play:
                        mMusicPlayService.sClickId = mMusicPlayService.INIT_CLICK_ID;
                        switch (mMusicPlayService.getState()) {
                            case PlaybackState.STATE_BUFFERING:
                            case PlaybackState.STATE_CONNECTING:
                                mMediaPlay.setVisibility(View.GONE);
                                break;
                            case PlaybackState.STATE_PLAYING:
                                //mMusicPlayService.pause();
                                Intent pauseIntent=new Intent(MediaActivity.this.getApplicationContext(), MusicPlayService.class);
                                pauseIntent.putExtra(MusicPlayService.MUSIC_KEY,MusicPlayService.MUSIC_PAUSE);
                                startService(pauseIntent);
                                Log.i(TAG, "onClick..点击了暂停音乐按钮");
                                break;
                            default:
                                //mMusicPlayService.play();
                                Intent playIntent=new Intent(MediaActivity.this.getApplicationContext(), MusicPlayService.class);
                                playIntent.putExtra(MusicPlayService.MUSIC_KEY,MusicPlayService.MUSIC_PLAY);
                                startService(playIntent);
                                Log.w(TAG, "onClick..点击了播放音乐按钮...当前因播放器包名=" + mMusicPlayService.getPackageName());
                                break;
                        }
                        break;
                    case R.id.media_next:
                        Log.i(TAG, "下一首歌");
                        //mMusicPlayService.skipToNext();
                        Intent skipToNextIntent=new Intent(MediaActivity.this.getApplicationContext(), MusicPlayService.class);
                        skipToNextIntent.putExtra(MusicPlayService.MUSIC_KEY,MusicPlayService.MUSIC_NEXT);
                        startService(skipToNextIntent);
                        break;
                    case R.id.select_music_player:
                        if (mMusicPlayService.canNotShowPoPup()) {
                            Toast.makeText(MediaActivity.this, "只有一个播放器!", Toast.LENGTH_SHORT).show();
                        } else {
                            showPopupWindow(v);
                        }
                        break;
                }
            }
        }
    };


    private MusicPlayerAdapter mGroupAdapter;
    private void showPopupWindow(View parent) {
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.music_player_list, null);
        mMusicPlayersListView = view.findViewById(R.id.music_players);
        // 加载数据
        mGroupAdapter = new MusicPlayerAdapter(this,
                mMusicPlayService.getMusicPlayersList(), MusicPlayService.sSelectedItem);
        mMusicPlayersListView.setAdapter(mGroupAdapter);
        // 创建一个PopuWidow对象
        int width = getResources().getDisplayMetrics().widthPixels;
        int height;
        if (mMusicPlayService.hasTwoMusicPlayer()) {
            height = 320;
        } else {
            height = 480;
        }
        mSelectMusicPlayerPopupWIndow = new PopupWindow(view, width, height);
        // 使其聚集
        mSelectMusicPlayerPopupWIndow.setFocusable(true);
        // 设置允许在外点击消失
        mSelectMusicPlayerPopupWIndow.setOutsideTouchable(true);

        // 这个是为了点击“返回Back”也能使其消失，并且并不会影响你的背景
        mSelectMusicPlayerPopupWIndow.setBackgroundDrawable(new BitmapDrawable());
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        // 显示的位置为:屏幕的宽度的一半-PopupWindow的高度的一半
        int xPos = windowManager.getDefaultDisplay().getWidth() / 2 -
                mSelectMusicPlayerPopupWIndow.getWidth() / 2;
        Log.i(TAG, "xPos:" + xPos);

        mSelectMusicPlayerPopupWIndow.showAsDropDown(parent, xPos, 0);
        mMusicPlayersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int position, long id) {

                MusicPlayService.sClickId = view.getId();
                MusicPlayService.sSelectedItem = position;
                if (mMusicPlayService.isPlay()) {
                    mMusicPlayService.pause();//暂停原来的播放器
                    Log.v(TAG, "暂停原来的播放器");
                }

                Log.i(TAG, "onItemClick, sSelectedItem="+MusicPlayService.sSelectedItem);
                //mMusicPlayService.addOnActiveSessionsChangedListener(true);

                Toast.makeText(MediaActivity.this,
                        "点击了第" + MusicPlayService.sSelectedItem + "个", Toast.LENGTH_SHORT).show();

                Intent updateIntent=new Intent(MediaActivity.this.getApplicationContext(), MusicPlayService.class);
                Bundle bundle = new Bundle();
                bundle.putBoolean(MusicPlayService.MUSIC_UPDATE_KEY,true);
                bundle.putInt(MusicPlayService.MUSIC_UPDATE_ITEM_KEY,position);
                updateIntent.putExtras(bundle);
                startService(updateIntent);


                if (mSelectMusicPlayerPopupWIndow != null) {
                    mSelectMusicPlayerPopupWIndow.dismiss();
                }
            }
        });
    }

}


package com.julie.spotifystreamer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;

/**
 * MediaPlayerService:
 * Extension of Service that handles playing the track
 * so that the track can continue to play in the background.
 * See https://developer.android.com/guide/topics/media/mediaplayer.html for details.
 */
public class MediaPlayerService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener{

    private static final String ACTION_PLAY = "com.julie.spotifystreamer.action.PLAY";
    private static final String LOG_TAG = MediaPlayerService.class.getSimpleName();
    private static final String WIFI_LOCK_TAG = "mediaPlayerWifiLock";
    public static final String ARG_SONG_NAME = "songName";
    public static final String ARG_URI = "uri";
    private static final int mNotificationId = 1;

    MediaPlayer mMediaPlayer = null;
    private String mUriString;
    private String mSongName;
    private WifiManager.WifiLock mWifiLock;

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mMediaPlayer == null) {
                    initMediaPlayer();
                }
                else if (!mMediaPlayer.isPlaying()) {
                    mMediaPlayer.start();
                }
                mMediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                break;
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(ACTION_PLAY)) {
            mUriString = intent.getStringExtra(ARG_URI);
            mSongName = intent.getStringExtra(ARG_SONG_NAME);

            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.e(LOG_TAG, "Audio focus request was denied.  Media player will not be started.");
                return -1;
            }

            initMediaPlayer();
            startNotification();
        }
        return 0;
    }

    //make the service a foreground service by creating a notification for the status bar.
    private void startNotification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), TrackPlayerActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle("Now playing:  ")
                .setSmallIcon(R.drawable.ic_queue_music_black_24dp)
                .setContentText(mSongName)
                .setContentIntent(pendingIntent);
        Notification notification = mBuilder.build();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(mNotificationId, notification);

        startForeground(mNotificationId, notification);
    }

    private void updateNotificationWithText(String msg) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    private void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        setListeners();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK_TAG);
        mWifiLock.acquire();
        Uri musicUri = Uri.parse(mUriString);
        try {
            mMediaPlayer.setDataSource(this, musicUri);
            mMediaPlayer.prepareAsync();
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Illegal argument to MediaPlayer setDataSource() :  " + e.getMessage());
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException on MediaPlayer setDataSource():  " + e.getMessage());
        }
    }

    public void onPause()
    {
        mMediaPlayer.pause();
        releaseWifiLock();
    }

    public void onStop()
    {
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = null;
        releaseWifiLock();
        stopForeground(true);
    }

    public void onResume()
    {
        mMediaPlayer.start();
    }

    private void releaseWifiLock()
    {
        mWifiLock.release();
        mWifiLock = null;
    }

    private void setListeners() {
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //https://stackoverflow.com/questions/17731527/how-to-implement-a-mediaplayer-restart-on-errors-in-android
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.e(LOG_TAG, "Error:  Server died.  Resetting MediaPlayer");
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.e(LOG_TAG, "Error:  Unknown.  Resetting MediaPlayer");
                break;
        }
        mp.reset();
        initMediaPlayer();
        return true;
    }
}

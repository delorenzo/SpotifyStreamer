package com.julie.spotifystreamer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
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

    //public constants
    public static final String ACTION_PLAY = "com.julie.spotifystreamer.action.PLAY";
    public static final String ACTION_STOP = "com.julie.spotifystreamer.action.STOP";
    public static final String ARG_TRACK_NAME = "songName";
    public static final String ARG_URI = "uri";

    //private constants
    private static final String LOG_TAG = MediaPlayerService.class.getSimpleName();
    private static final String WIFI_LOCK_TAG = "mediaPlayerWifiLock";
    private static final int mNotificationId = 1;

    //private internals
    private MediaPlayer mMediaPlayer = null;
    private String mUriString;
    private String mTrackName;
    private WifiManager.WifiLock mWifiLock;
    private final IBinder mBinder = new MediaPlayerBinder();
    private AudioManager mAudioManager;

    //handle audio focus changes by pausing/resuming/adjusting audio as is appropriate
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mMediaPlayer == null) {
                    initMediaPlayer();
                }
                onResume();
                mMediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                onStop();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                onPause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.setVolume(0.1f, 0.1f);
                }
                break;
        }
    }

    // The service is starting, due to a call to startService()
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(ACTION_PLAY)) {
            mUriString = intent.getStringExtra(ARG_URI);
            mTrackName = intent.getStringExtra(ARG_TRACK_NAME);

            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.e(LOG_TAG, "Audio focus request was denied.  Media player will not be started.");
                return -1;
            }

            initMediaPlayer();
            initNotification();
        }
        else if (intent.getAction().equals(ACTION_STOP)) {
            onStop();
        }
        else {
            Log.e(LOG_TAG, "Unrecognized intent action called for:  " + intent.getAction());
        }
        return 0;
    }

    //make the service a foreground service by creating a notification for the status bar.
    private void initNotification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), TrackPlayerActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle("Now playing:  ")
                .setSmallIcon(R.drawable.ic_queue_music_black_24dp)
                .setContentText(mTrackName)
                .setContentIntent(pendingIntent);
        Notification notification = mBuilder.build();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(mNotificationId, notification);

        startForeground(mNotificationId, notification);
    }

    //the media player has completed playback.  release resources
    @Override
    public void onCompletion(MediaPlayer mp) {
        mMediaPlayer.reset();
        mMediaPlayer.release();
        mMediaPlayer = null;
        stopSelf();
    }

    //initialize the media player
    private void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        setListeners();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        
        //acquire CPU lock to ensure playback is not interrupted
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        
        //acquire wifi lock to ensure playback is not interrupted
        acquireWifiLock();
        
        Uri musicUri = Uri.parse(mUriString);
        //setDataSoruce has the potential to throw IllegalArgumentException or IOException
        try {
            mMediaPlayer.setDataSource(this, musicUri);
            mMediaPlayer.prepareAsync();
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Illegal argument to MediaPlayer setDataSource() :  " + e.getMessage());
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException on MediaPlayer setDataSource():  " + e.getMessage());
        }
    }

    //pause - transient audio focus loss
    //note we need to release the wifi lock and not the cpu lock because the MediaPlayer will handle that for us.
    public void onPause()
    {
        mMediaPlayer.pause();
        releaseWifiLock();
    }

    //stop - indetermined length of audio focus loss.  release resources
    //note we need to release the wifi lock and not the cpu lock because the MediaPlayer will handle that for us.
    public void onStop()
    {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
        releaseWifiLock();
        mMediaPlayer.release();
        mMediaPlayer = null;
        stopForeground(true);
        stopSelf();
    }

    //resume - recover from from transient audio focus loss
    public void onResume()
    {
        acquireWifiLock();
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
    }

    //release the wifi lock
    private void releaseWifiLock() {
        mWifiLock.release();
        mWifiLock = null;
    }
    
    //acquire the wifi lock
    private void acquireWifiLock() {
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK_TAG);
        mWifiLock.acquire();
    }

    /* set listeners for the media player:
    on prepared is called when playback is ready,
    on completion is called when playback is complete,
    on error allows us to recover from the error state by resetting the player.
    */
    private void setListeners() {
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
    }

    //the media player is prepared to start.
    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    //the service is being destroyed.  release resources
    @Override
    public void onDestroy() {
        onStop();
    }

    // A client is binding to the service with bindService()
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
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

    //Class used for the client binder, allowing clients to call public methods.
    public class MediaPlayerBinder extends Binder {
        MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }
}

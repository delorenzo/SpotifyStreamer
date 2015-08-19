package com.julie.spotifystreamer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.julie.spotifystreamer.Receivers.RemoteControlReceiver;
import com.squareup.picasso.Picasso;

import java.io.IOException;

/**
 * MediaPlayerService:
 * Extension of Service that handles playing the track
 * so that the track can continue to play in the background.
 * See https://developer.android.com/guide/topics/media/mediaplayer.html
 * and
 * https://developer.android.com/reference/android/media/MediaPlayer.html
 * for details.
 */
public class MediaPlayerService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener{

    //public constants
    public static final String ACTION_PLAY = "com.julie.spotifystreamer.action.PLAY";
    public static final String ACTION_STOP = "com.julie.spotifystreamer.action.STOP";
    public static final String ACTION_CHANGE_TRACK = "com.julie.spotifystreamer.action.CHANGE_TRACK";
    public static final String ACTION_PREVIOUS = "com.julie.spotifystreamer.action.PREVIOUS";
    public static final String ACTION_NEXT = "com.julie.spotifystreamer.action.PREVIOUS";
    public static final String ACTION_PAUSE = "com.julie.spotifystreamer.action.PAUSE";
    public static final String ARG_TRACK_NAME = "songName";
    public static final String ARG_URI = "uri";
    public static final String ARG_ALBUM_ART = "albumArt";
    //receiver constants
    public static final String RESULT_RECEIVER = "resultReceiver";
    public static final int PREPARED = 22;
    public static final int COMPLETE = 33;

    //private constants
    private static final String LOG_TAG = MediaPlayerService.class.getSimpleName();
    private static final String WIFI_LOCK_TAG = "mediaPlayerWifiLock";
    private static final int mNotificationId = 1;

    //private internals
    private MediaPlayer mMediaPlayer = null;
    private String mUriString;
    private String mTrackName;
    private String mAlbumArtURL;
    private Bitmap mAlbumArt;
    private WifiManager.WifiLock mWifiLock;
    private final IBinder mBinder = new MediaPlayerBinder();
    private AudioManager mAudioManager;
    private NotificationCompat.Builder mNotificationBuilder;
    private Boolean isPrepared = false;
    private int duration = 0;
    private ResultReceiver resultReceiver;

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
        switch (intent.getAction()) {
            case ACTION_PLAY:
                mUriString = intent.getStringExtra(ARG_URI);
                mTrackName = intent.getStringExtra(ARG_TRACK_NAME);
                mAlbumArtURL = intent.getStringExtra(ARG_ALBUM_ART);

                mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
                if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    Log.e(LOG_TAG, "Audio focus request was denied.  Media player will not be started.");
                    return -1;
                }

                initMediaPlayer();
                initNotification();

                resultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);
                break;

            case ACTION_PAUSE:
                onPause();
                break;

            case ACTION_STOP:
                onStop();
                break;

            //to change the data source on a media player you must call reset
            //because setDataSource() called in any other state throws an IllegalStateException
            case ACTION_CHANGE_TRACK:
                //if the media player exists it needs to be reset.
                if (mMediaPlayer != null) {
                    mMediaPlayer.reset();
                }
                mUriString = intent.getStringExtra(ARG_URI);
                mTrackName = intent.getStringExtra(ARG_TRACK_NAME);
                initMediaPlayer();
                initNotification();
                break;

            default:
                Log.e(LOG_TAG, "Unrecognized intent action called for:  " + intent.getAction());
                break;
        }
        return 0;
    }

    //make the service a foreground service by creating a notification for the status bar.
    private void initNotification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), TrackPlayerActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent stopIntent = new Intent(getApplicationContext(), MediaPlayerService.class);
        stopIntent.setAction(MediaPlayerService.ACTION_STOP);
        PendingIntent deleteIntent = PendingIntent.getService(getApplicationContext(), 1, stopIntent, 0);

        Intent intent = new Intent(this, MediaPlayerService.class);
        intent.setAction(MediaPlayerService.ACTION_PAUSE);
        PendingIntent pausePendingIntent =
                PendingIntent.getActivity(
                        getApplicationContext(),
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mNotificationBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle(getString(R.string.now_playing))
                        .setSmallIcon(R.drawable.ic_queue_music_black_24dp)
                        .setContentText(mTrackName)
                        .setContentIntent(pendingIntent)
                        .setDeleteIntent(deleteIntent)
                        .addAction(R.drawable.ic_skip_previous_black_24dp, "Previous", generatePendingIntent(MediaPlayerService.ACTION_PREVIOUS))
                        .addAction(R.drawable.ic_pause_black_24dp, "Pause", generatePendingIntent(MediaPlayerService.ACTION_PAUSE))
                        .addAction(R.drawable.ic_skip_next_black_24dp, "Next", generatePendingIntent(MediaPlayerService.ACTION_NEXT))
                        .setVisibility(Notification.VISIBILITY_PUBLIC);
        Notification notification = mNotificationBuilder.build();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(mNotificationId, notification);

        startForeground(mNotificationId, notification);
    }

    private PendingIntent generatePendingIntent(String action) {
        Intent intent = new Intent(this, MediaPlayerService.class);
        intent.setAction(action);
        return PendingIntent.getActivity(
                        getApplicationContext(),
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
    }

    //update the notification with the appropriate track name
    private void updateNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationBuilder.setContentText(mTrackName);
        mNotificationManager.notify(mNotificationId, mNotificationBuilder.build());
    }


    //the media player has completed playback.  release resources
    @Override
    public void onCompletion(MediaPlayer mp) {
        resultReceiver.send(COMPLETE, new Bundle());
        mMediaPlayer.reset();
        mMediaPlayer.release();
        mMediaPlayer = null;
        stopSelf();
    }

    //initialize the media player
    private void initMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        }

        setListeners();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        
        //acquire CPU lock to ensure playback is not interrupted
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        
        //acquire wifi lock to ensure playback is not interrupted
        acquireWifiLock();
        
        setPlayerDataSource();
    }

    //parses the music URI from the URI String and attempts to set the data source.
    private void setPlayerDataSource() {
        Uri musicUri = Uri.parse(mUriString);
        //setDataSource has the potential to throw IllegalArgumentException or IOException
        try {
            mMediaPlayer.setDataSource(this, musicUri);
            mMediaPlayer.prepareAsync();
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Illegal argument to MediaPlayer setDataSource() :  " + e.getMessage());
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException on MediaPlayer setDataSource():  " + e.getMessage());
        } catch (IllegalStateException e) {
            Log.e(LOG_TAG, "Set data source called from invalid media player state:  " + e.getMessage());
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
        //sometimes onStop() is called with a media player that doesn't exist
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        isPrepared = false;
        releaseWifiLock();
        stopForeground(true);
        stopSelf();
    }

    //resume - recover from from transient audio focus loss
    public void onResume()
    {
        acquireWifiLock();
        if (mMediaPlayer == null) {
            initMediaPlayer();
        }
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
    }

    //release the wifi lock
    private void releaseWifiLock() {
        if (mWifiLock != null) {
            mWifiLock.release();
        }
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
        duration = mp.getDuration();
        isPrepared = true;
        resultReceiver.send(PREPARED, new Bundle());
    }

    //the service is being destroyed.  release resources
    @Override
    public void onDestroy() {
        onStop();
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(mNotificationId);
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

    //return current position of the media player
    public int getCurrentPosition()
    {
        return isPrepared ? mMediaPlayer.getCurrentPosition() : 0;
    }

    //return duration of the media player
    public int getDuration()
    {
        return duration;
    }

    public void MediaPlayerSeekTo(int progress) {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            mMediaPlayer.seekTo(progress);
            mMediaPlayer.start();
        }
    }

    public Boolean isPrepared() {
        return isPrepared;
    }

}

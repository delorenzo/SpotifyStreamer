package com.julie.spotifystreamer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

/**
 * handles playing the track so that the track can continue to play in the background.
 */
public class MediaPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener{
    private static final String ACTION_PLAY = "com.julie.spotifystreamer.action.PLAY";
    private static final String LOG_TAG = MediaPlayerService.class.getSimpleName();
    MediaPlayer mMediaPlayer = null;
    public static final String ARG_URI = "uri";

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(ACTION_PLAY)) {
            String musicUriString = intent.getStringExtra(ARG_URI);
            initMediaPlayer(musicUriString);
        }
        return 0;
    }

    private void initMediaPlayer(String musicUriString) {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        Uri musicUri = Uri.parse(musicUriString);
        try {
            mMediaPlayer.setDataSource(this, musicUri);
            mMediaPlayer.prepareAsync();
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Illegal argument to MediaPlayer setDataSource() :  " + e.getMessage());
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException on MediaPlayer setDataSource():  " + e.getMessage());
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(LOG_TAG, "MediaPlayer has moved to the Error state");
        return false;
    }
}

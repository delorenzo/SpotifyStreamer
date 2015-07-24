package com.julie.spotifystreamer;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;

//TrackPlayerActivity handles playing the selected track.
//see https://developer.android.com/guide/topics/media/mediaplayer.html
//and https://developer.android.com/reference/android/media/MediaPlayer.html
public class TrackPlayerActivity extends AppCompatActivity {
    private AudioManager am;
    private static final String LOG_TAG = TrackPlayerActivity.class.getSimpleName();
    private MediaPlayer mMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_player);
//        mMediaPlayer = new MediaPlayer();
//        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//        try {
////        mMediaPlayer.setDataSource(getApplicationContext(), myUri);
//        } catch (IllegalArgumentException e) {
//            Log.e(LOG_TAG, "Illegal argument exception:  " + myUri);
//            return;
//        } catch (IOException e) {
//            Log.e(LOG_TAG, "IO exception:  " + myUri);
//            return;
//        }
//        mMediaPlayer.prepare();
//        mMediaPlayer.start();
//        am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
//        if (am == null) {
//            Log.e(LOG_TAG, "Failed to get audio manager from audio service");
//        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //request audio focus for playback
//        int result = am.requestAudioFocus(afChangeListener,
//                AudioManager.STREAM_MUSIC,
//                AudioManager.AUDIOFOCUS_GAIN);
//        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
//        }
//        else {
//            Log.e(LOG_TAG, "Request for audio focus denied");
//        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_track_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
//        public void onAudioFocusChange(int focusChange) {
//            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
//                //lower volume
//        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
//            // Pause playback
//        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
//            // Resume playback
//        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
//            //am.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
//            am.abandonAudioFocus(afChangeListener);
//            // Stop playback
//        } else {
//            Log.e(LOG_TAG, "Unexpected focus change " + focusChange);
//        }
//         }
//    };
}

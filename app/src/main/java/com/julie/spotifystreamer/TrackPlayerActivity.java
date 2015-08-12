package com.julie.spotifystreamer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.julie.spotifystreamer.DataContent.TrackContent;

import java.util.ArrayList;

public class TrackPlayerActivity extends AppCompatActivity {

    public static final String ARG_TRACK = "track";
    public static final String ARG_TRACK_LIST = "trackList";
    public static final String ARG_POSITION = "position";
    public static final String PLAYER_FRAGMENT_TAG = "playerFragment";
    private static final String LOG_TAG = TrackPlayerActivity.class.getSimpleName();
    private boolean mBound = false;
    //the mediaplayerservice lives here so that its lifetime isn't linked to the lifetime
    //of the fragment.
    private MediaPlayerService mService;
    private TrackContent mTrackContent;
    private int progress = 0;
    private int mTrackListPosition = 0;
    private ArrayList<TrackContent> mTrackList;
    private Boolean isPlaying = false;
    private int mCurrentPos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_player);

        if (savedInstanceState == null) {
            //this should prevent the TrackContent class not being found when unmarshalling
            getIntent().getExtras().setClassLoader(TrackContent.class.getClassLoader());
            mTrackListPosition = getIntent().getIntExtra(ARG_POSITION, 0);
            mTrackList = getIntent().getParcelableArrayListExtra(ARG_TRACK_LIST);
            //mTrackContent = getIntent().getParcelableExtra(ARG_TRACK);
            mTrackContent = mTrackList.get(mTrackListPosition);

            startMusicPlayerService(MediaPlayerService.ACTION_PLAY);
            
            TrackPlayerFragment fragment = TrackPlayerFragment.newInstance(mTrackContent);

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.player_container, fragment, PLAYER_FRAGMENT_TAG)
                    .commit();
        }
    }

    //start the music player service given the current track content data
    private void startMusicPlayerService(String action) {
        //start music player service
        Intent playIntent = new Intent(this, MediaPlayerService.class);
        playIntent.putExtra(MediaPlayerService.ARG_URI, mTrackContent.getPreviewURL());
        playIntent.putExtra(MediaPlayerService.ARG_TRACK_NAME, mTrackContent.getTrackName());
        playIntent.setAction(action);
        startService(playIntent);

        //bind to service so the user can impact playback
        bindService(playIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        isPlaying = false;
        if (mBound) {
            mService.onStop();
            isPlaying = false;
            unbindService(mConnection);
            mBound = false;
        }
        super.onDestroy();
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

    //from https://developer.android.com/guide/components/bound-services.html
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // bind to the MediaPlayerService and get the instance of MediaPlayerService
            MediaPlayerService.MediaPlayerBinder binder = (MediaPlayerService.MediaPlayerBinder) service;
            mService = binder.getService();
            mBound = true;
            isPlaying = true;
            mCurrentPos = 0;

            thread.start();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.e(LOG_TAG, "Service disconnected unexpectedly");
            mBound = false;
            isPlaying = false;
        }
    };


    public void pausePlayer(View view) {
        isPlaying = false;
        if (mService != null) {
            mService.onPause();
        }
        TrackPlayerFragment fragment = (TrackPlayerFragment)getSupportFragmentManager().
                findFragmentByTag(PLAYER_FRAGMENT_TAG);
        if (fragment != null) {
            fragment.showResumeButton();
        }
    }

    public void resumePlayer(View view) {
        isPlaying = true;
        if (mService != null) {
            mService.onResume();
        }
        TrackPlayerFragment fragment = (TrackPlayerFragment)getSupportFragmentManager().
                findFragmentByTag(PLAYER_FRAGMENT_TAG);
        if (fragment != null) {
            fragment.showPauseButton();
        }
    }

    public void skipNextPlayer(View view) {
        isPlaying = false;
        mCurrentPos = 0;

        //increment track list position and update current track
        mTrackListPosition = mTrackListPosition + 1 % mTrackList.size();
        mTrackContent = mTrackList.get(mTrackListPosition);

        //update UI
        TrackPlayerFragment fragment = (TrackPlayerFragment)getSupportFragmentManager().
                findFragmentByTag(PLAYER_FRAGMENT_TAG);
        if (fragment != null) {
            fragment.updateDisplayedTrack(mTrackContent);
        }

        //restart playback
        startMusicPlayerService(MediaPlayerService.ACTION_CHANGE_TRACK);
        thread.start();
    }

    public void skipPreviousPlayer(View view) {
        isPlaying = false;
        mCurrentPos = 0;

        //decrement track list position and update current track
        mTrackListPosition = mTrackListPosition - 1 % mTrackList.size();
        mTrackContent = mTrackList.get(mTrackListPosition);

        //update UI
        TrackPlayerFragment fragment = (TrackPlayerFragment)getSupportFragmentManager().
                findFragmentByTag(PLAYER_FRAGMENT_TAG);
        if (fragment != null) {
            fragment.updateDisplayedTrack(mTrackContent);
        }

        //restart playback
        startMusicPlayerService(MediaPlayerService.ACTION_CHANGE_TRACK);
        thread.start();
    }

    //https://stackoverflow.com/questions/2967337/how-do-i-use-android-progressbar-in-determinate-mode
    //the thread runs in the activity and not the fragment because the fragment has the potential
    //to be destroyed on rotation.
    Thread thread = new Thread(new Runnable()
    {
        public void run()
        {

            //to get the player duration, the player has to be in an appropriate state, but
            //the activity doesn't have a way to wait for the on prepared callback, so poll until
            //the service gives a valid duration.
            TrackPlayerFragment fragment = (TrackPlayerFragment)getSupportFragmentManager().
                    findFragmentByTag(PLAYER_FRAGMENT_TAG);
            int duration = mService.getDuration();
            while (duration == 0) {
                try {
                    Thread.sleep(200);
                    duration = mService.getDuration();
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, "Progress bar thread interrupted:  " + e.getMessage());
                    return;
                }
            }

            //setup the progress bar
            fragment.setupProgressBar(duration);

            //update the progress bar as appropriate
            while (isPlaying && mCurrentPos < duration) {
                try {
                    Thread.sleep(500);
                    mCurrentPos = mService.getCurrentPosition();
                    fragment.updateProgress(mCurrentPos);
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, "Progress bar thread interrupted:  " + e.getMessage());
                    return;
                }
                if (mCurrentPos >= duration) {
                    isPlaying = false;
                    fragment.showResumeButton();
                }
            }
        }
    });
}

package com.julie.spotifystreamer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.julie.spotifystreamer.DataContent.TrackContent;

import java.util.ArrayList;


//TrackPlayerActivity attaches to the MediaPlayerService and manages the media player.
//see https://developer.android.com/guide/topics/media/mediaplayer.html
//and https://developer.android.com/reference/android/media/MediaPlayer.html

public class TrackPlayerActivity extends AppCompatActivity
        implements TrackPlayerFragment.OnSeekBarUserUpdateListener {

    public static final String ARG_TRACK = "track";
    public static final String ARG_TRACK_LIST = "trackList";
    public static final String ARG_POSITION = "position";
    public static final String TRACKPLAYER_TAG = "playerFragment";
    public static final String ARG_TABLET = "tablet";
    private static final String LOG_TAG = TrackPlayerActivity.class.getSimpleName();

    private boolean mBound = false;
    private boolean mTablet;
    //the mediaplayerservice lives here so that its lifetime isn't linked to the lifetime
    //of the fragment.
    private MediaPlayerService mService;
    private TrackContent mTrackContent;
    private int progress = 0;
    private int mTrackListPosition = 0;
    private ArrayList<TrackContent> mTrackList;
    private Boolean isPlaying = false;
    private int mCurrentPos = 0;
    private int mDuration = 0;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_player);

        if (savedInstanceState == null) {
            //this should prevent the TrackContent class not being found when unmarshalling
            getIntent().getExtras().setClassLoader(TrackContent.class.getClassLoader());
            mTrackListPosition = getIntent().getIntExtra(ARG_POSITION, 0);
            mTrackList = getIntent().getParcelableArrayListExtra(ARG_TRACK_LIST);
            mTrackContent = mTrackList.get(mTrackListPosition);
            mTablet = getIntent().getBooleanExtra(ARG_TABLET, false);
            
            TrackPlayerFragment fragment = TrackPlayerFragment.newInstance(mTrackContent);

            //Remove any previous instance of the track player fragment prior to initializing this one.
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment previousPlayer = getSupportFragmentManager().findFragmentByTag(TRACKPLAYER_TAG);
            if (previousPlayer != null) {
                ft.remove(previousPlayer);
            }

            //show the fragment as a dialog in two pane mode and embed it in the container otherwise
            if (mTablet) {
                fragment.show(getSupportFragmentManager(), TRACKPLAYER_TAG);
            }
            else {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.player_container, fragment, TRACKPLAYER_TAG)
                        .commit();
            }
            startMusicPlayerService(MediaPlayerService.ACTION_PLAY);
        }
        else {
            Intent playIntent = new Intent(this, MediaPlayerService.class);
            bindService(playIntent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    //start the music player service given the current track content data
    private void startMusicPlayerService(String action) {
        //start music player service
        Intent playIntent = new Intent(this, MediaPlayerService.class);
        playIntent.putExtra(MediaPlayerService.ARG_TRACK_LIST, mTrackList);
        playIntent.putExtra(MediaPlayerService.ARG_TRACK_LIST_POSITION, mTrackListPosition);
        //because the media player is being implemented as a service, we need some way
        //for the service to notify the activity of updates to its state.
        playIntent.putExtra(MediaPlayerService.RESULT_RECEIVER, mResultReceiver);
        playIntent.setAction(action);
        startService(playIntent);

        bindService(playIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onDestroy() {
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
            if (mService.isPrepared()) {
                new UpdateProgressTask().execute();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.e(LOG_TAG, "Service disconnected unexpectedly");
            mBound = false;
            mService = null;
            isPlaying = false;
        }
    };


    public void pausePlayer(View view) {
        if (mService != null) {
            mService.pause();
        }
        TrackPlayerFragment fragment = (TrackPlayerFragment)getSupportFragmentManager().
                findFragmentByTag(TRACKPLAYER_TAG);
        if (fragment != null) {
            fragment.showResumeButton();
        }
    }

    public void resumePlayer(View view) {
        if (mService != null) {
            mService.resume();
        }
        TrackPlayerFragment fragment = (TrackPlayerFragment)getSupportFragmentManager().
                findFragmentByTag(TRACKPLAYER_TAG);
        if (fragment != null) {
            fragment.showPauseButton();
        }
    }

    public void skipNextPlayer(View view) {
        isPlaying = false;
        mCurrentPos = 0;
        //if the track is paused and the user skips to next, we want the track to start
        //playing and the pause button to be displayed again.
        TrackPlayerFragment fragment = (TrackPlayerFragment)getSupportFragmentManager().
                findFragmentByTag(TRACKPLAYER_TAG);
        fragment.showPauseButton();
        startMusicPlayerService(MediaPlayerService.ACTION_NEXT);
    }

    public void skipPreviousPlayer(View view) {
        isPlaying = false;
        mCurrentPos = 0;
        //if the track is paused and the user skips to next, we want the track to start
        //playing and the pause button to be displayed again.
        TrackPlayerFragment fragment = (TrackPlayerFragment)getSupportFragmentManager().
                findFragmentByTag(TRACKPLAYER_TAG);
        fragment.showPauseButton();
        startMusicPlayerService(MediaPlayerService.ACTION_PREVIOUS);
    }

    ///asynchronous task that polls the media player for its progress
    //and notifies the fragment to update the UI.
    private class UpdateProgressTask extends AsyncTask<Integer, Integer, Integer> {
        protected Integer doInBackground(Integer ... args) {
            int currentPos = 0;
            mDuration = mService.getDuration();
            try {
                while (currentPos < mDuration && isPlaying && mBound) {
                    if (mBound && mService.isPrepared()) {
                        currentPos = mService.getCurrentPosition();
                        publishProgress(currentPos);
                    }
                    Thread.sleep(200);
                }
                isPlaying = false;
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Progress bar thread interrupted:  " + e.getMessage());
            }
            return 0;
        }

        protected void onProgressUpdate (Integer ... progress) {
            TrackPlayerFragment fragment = (TrackPlayerFragment)getSupportFragmentManager().
                    findFragmentByTag(TRACKPLAYER_TAG);
            if (fragment != null) {
                fragment.updateProgress(progress[0]);
            }
        }
    }

    /*
    ResultReceiver that handles messages from the Service about the Service's state.
     */
    final ResultReceiver mResultReceiver = new ResultReceiver(mHandler) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            TrackPlayerFragment fragment = (TrackPlayerFragment) getSupportFragmentManager().
                    findFragmentByTag(TRACKPLAYER_TAG);
            if (fragment == null) {
                return;
            }
            switch (resultCode) {
                case MediaPlayerService.PREPARED:
                    isPlaying = true;
                    mDuration = mService.getDuration();
                    fragment.setupProgressBar(mDuration, 0);
                    new UpdateProgressTask().execute();
                    break;
                case MediaPlayerService.COMPLETE:
                    isPlaying = false;
                    fragment.showResumeButton();
                    break;
                case MediaPlayerService.TRACK_CHANGE:
                    mTrackContent = mService.getCurrentTrack();
                    fragment.updateDisplayedTrack(mTrackContent);
                    break;
                default:
                    Log.e(LOG_TAG, "OnReceiveResult called with unknown result code:  " + resultCode);
                    break;
            }

        }
    };

    //the user has dragged the seek bar - attempt to update the music playback
    public void onSeekBarUserUpdate(int position) {
        if (mService != null && mService.isPrepared()) {
            mService.MediaPlayerSeekTo(position);
        }
    }
}

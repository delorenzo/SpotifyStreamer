package com.julie.spotifystreamer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.julie.spotifystreamer.datacontent.TrackContent;

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
    private static final String RECEIVER_TAG = "resultReceiver";

    private boolean mBound = false;
    //private boolean mTablet;
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
    //private ServiceReceiver mServiceReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MediaPlayerService.ACTION_PREPARED);
        intentFilter.addAction(MediaPlayerService.ACTION_COMPLETE);
        intentFilter.addAction(MediaPlayerService.ACTION_CHANGE_TRACK);
        bManager.registerReceiver(bReceiver, intentFilter);

        if (savedInstanceState == null) {
            if (getIntent().getExtras() != null) {
                //this should prevent the TrackContent class not being found when unmarshalling
                getIntent().getExtras().setClassLoader(TrackContent.class.getClassLoader());
                mTrackListPosition = getIntent().getIntExtra(ARG_POSITION, 0);
                mTrackList = getIntent().getParcelableArrayListExtra(ARG_TRACK_LIST);
                mTrackContent = mTrackList.get(mTrackListPosition);
                //mTablet = getIntent().getBooleanExtra(ARG_TABLET, false);
            }

            TrackPlayerFragment fragment = TrackPlayerFragment.newInstance(mTrackContent);

            //Remove any previous instance of the track player fragment prior to initializing this one.
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment previousPlayer = getSupportFragmentManager().findFragmentByTag(TRACKPLAYER_TAG);
            if (previousPlayer != null) {
                ft.remove(previousPlayer);
            }

            //show the fragment as a dialog in two pane mode and embed it in the container otherwise
            if (isLargeTablet(getApplication())) {
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
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
        else if (id == android.R.id.home) {
            //release resources
            unbindService(mConnection);
            //navigate to the home activity
            NavUtils.navigateUpFromSameTask(this);
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

    public void pausePlayer() {
        startMusicPlayerService(MediaPlayerService.ACTION_PAUSE);
    }

    public void resumePlayer() {
        new UpdateProgressTask().execute();
        startMusicPlayerService(MediaPlayerService.ACTION_RESUME);
    }

    public void skipNextPlayer() {
        isPlaying = false;
        mCurrentPos = 0;
        startMusicPlayerService(MediaPlayerService.ACTION_NEXT);
    }

    public void skipPreviousPlayer() {
        isPlaying = false;
        mCurrentPos = 0;
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

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        //Handle updates received from the media player service
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            TrackPlayerFragment fragment = (TrackPlayerFragment) getSupportFragmentManager().
                    findFragmentByTag(TRACKPLAYER_TAG);
            if (fragment == null) {
                return;
            }
            switch (action) {
                case MediaPlayerService.ACTION_PREPARED:
                    isPlaying = true;
                    mTrackContent = intent.getParcelableExtra(ARG_TRACK);
                    mDuration = mService.getDuration();
                    fragment.updateDisplayedTrack(mTrackContent);
                    fragment.setupProgressBar(mDuration, 0);
                    new UpdateProgressTask().execute();
                    break;
                case MediaPlayerService.ACTION_COMPLETE:
                    isPlaying = false;
                    fragment.showResumeButton();
                    break;
                case MediaPlayerService.ACTION_TRACK_CHANGE:
                    mTrackContent = intent.getParcelableExtra(ARG_TRACK);
                    fragment.updateDisplayedTrack(mTrackContent);
                    break;
                default:
                    Log.e(LOG_TAG, "HandleIntent called with unknown action : " + action);
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

    //the result receiver must be saved on orientation changes
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(TrackPlayerActivity.ARG_POSITION, mTrackListPosition);
    }

    //helper method to determine if the device is large enough to support the dialog fragment
    //used as a dialog
    private static boolean isLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}

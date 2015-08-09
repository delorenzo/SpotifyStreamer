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
import android.widget.ImageButton;

import com.julie.spotifystreamer.DataContent.TrackContent;

public class TrackPlayerActivity extends AppCompatActivity {

    public static final String ARG_TRACK = "track";
    private static final String LOG_TAG = TrackPlayerActivity.class.getSimpleName();
    private boolean mBound = false;
    //the mediaplayerservice lives here so that its lifetime isn't linked to the lifetime
    //of the fragment.
    private MediaPlayerService mService;
    private TrackContent mTrackContent;
    private ViewHolder viewHolder;
    private int progress = 0;

    private static class ViewHolder {
        ImageButton pauseButton;
        ImageButton resumeButton;
        public ViewHolder(View view) {
            pauseButton = (ImageButton)view.findViewById(R.id.button_pause);
            resumeButton = (ImageButton)view.findViewById(R.id.button_play);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_player);

        if (savedInstanceState == null) {
            //this should prevent the TrackContent class not being found when unmarshalling
            getIntent().getExtras().setClassLoader(TrackContent.class.getClassLoader());
            mTrackContent = getIntent().getParcelableExtra(ARG_TRACK);
            
            //start music player service
            Intent playIntent = new Intent(this, MediaPlayerService.class);
            playIntent.putExtra(MediaPlayerService.ARG_URI, mTrackContent.getPreviewURL());
            playIntent.putExtra(MediaPlayerService.ARG_TRACK_NAME, mTrackContent.getTrackName());
            playIntent.setAction(MediaPlayerService.ACTION_PLAY);
            startService(playIntent);

            //bind to service so the user can impact playback
            bindService(playIntent, mConnection, Context.BIND_AUTO_CREATE);
            
            TrackPlayerFragment fragment = TrackPlayerFragment.newInstance(mTrackContent);

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.player_container, fragment)
                    .commit();
        }
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
            mService.onStop();
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
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.e(LOG_TAG, "Service disconnected unexpectedly");
            mBound = false;
        }
    };


    public void pausePlayer(View view) {

        if (mService != null) {
            mService.onPause();
        }
        if (viewHolder == null) {
            viewHolder = new ViewHolder(view);
        }
        //swap the resume and pause buttons
        viewHolder.pauseButton.setVisibility(View.GONE);
        viewHolder.resumeButton.setVisibility(View.VISIBLE);
    }

    public void resumePlayer(View view) {
        if (mService != null) {
            mService.onResume();
        }
        if (viewHolder == null) {
            viewHolder = new ViewHolder(view);
        }
        //swap the resume and pause buttons
        viewHolder.resumeButton.setVisibility(View.GONE);
        viewHolder.pauseButton.setVisibility(View.VISIBLE);
    }

    public void skipNextPlayer(View view) {
        //TODO:  implement skip next
    }

    public void skipPreviousPlayer(View view) {
        //TODO: implement skip previous
    }

//    Runnable run  = new Runnable()
//    {
//        @Override
//        public void run()
//        {
//            updateProgressBar();
//        }
//    };
}

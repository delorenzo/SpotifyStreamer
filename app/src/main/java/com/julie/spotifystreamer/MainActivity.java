package com.julie.spotifystreamer;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.julie.spotifystreamer.DataContent.TrackContent;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;

public class MainActivity extends AppCompatActivity implements
        ArtistFragment.OnArtistSelectedListener, TrackFragment.OnTrackSelectedListener {
    private static SpotifyService mSpotifyService;
    private Boolean mTwoPane;
    private static final String ARTISTFRAGMENT_TAG = "AFTAG";
    private static final String TRACKFRAGMENT_TAG = "TFTAG";
    private static final String TRACKPLAYER_TAG = "TPTAG";
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private int mTrackListPosition = 0;
    private ArrayList<TrackContent> mTrackList;
    private TrackContent mTrackContent;
    private MediaPlayerService mService;
    private Boolean isPlaying = false;
    private int mCurrentPos = 0;
    private int mDuration = 0;
    private Handler mHandler = new Handler();
    private Boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SpotifyApi api = new SpotifyApi();
        mSpotifyService = api.getService();

        if (savedInstanceState != null) {
            return;
        }

        if (findViewById(R.id.container) != null) {
            mTwoPane = false;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ArtistFragment(), ARTISTFRAGMENT_TAG)
                    .commit();
        }
        else if (findViewById(R.id.artist_container) != null && findViewById(R.id.track_container) != null) {
            mTwoPane = true;
            TrackFragment trackFragment = new TrackFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.artist_container, new ArtistFragment(), ARTISTFRAGMENT_TAG)
                    .add(R.id.track_container, new TrackFragment(), TRACKFRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Fragment artistFragment = ArtistFragment.newInstance(query);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            //each new search discards the old search - it isn't added to the back stack.
            if (mTwoPane) {
                transaction.replace(R.id.artist_container, artistFragment, ARTISTFRAGMENT_TAG);
            }
            else {
                transaction.replace(R.id.container, artistFragment, ARTISTFRAGMENT_TAG);
            }
            transaction.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        //set up the search widget
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setQueryRefinementEnabled(true);
        //the search hint from searchable.xml doesn't show, so call setQueryHint here
        //see https://stackoverflow.com/questions/20082535/hint-in-search-widget-within-action-bar-is-not-showing-up
        searchView.setQueryHint(getString(R.string.search_hint));

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

    //for smaller devices, replace the artist fragment in the container with the track fragment.
    //for larger devices (sw-600dp) place the track fragment alongside the artist fragment.
    public void onArtistSelected(String spotifyId, String artistName) {
        Fragment trackFragment = TrackFragment.newInstance(spotifyId, artistName);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (mTwoPane) {
            transaction.replace(R.id.track_container, trackFragment, TRACKFRAGMENT_TAG);
        }
        else {
            transaction.replace(R.id.container, trackFragment, TRACKFRAGMENT_TAG);
        }
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void onTrackSelected(ArrayList<TrackContent> trackList, int position) {
        Intent intent = new Intent(this, TrackPlayerActivity.class);
        intent.putExtra(TrackPlayerActivity.ARG_POSITION, position);
        intent.putExtra(TrackPlayerActivity.ARG_TRACK_LIST, trackList);
        intent.putExtra(TrackPlayerActivity.ARG_TABLET, mTwoPane);
        startActivity(intent);

//        mTrackList = trackList;
//        mTrackListPosition = position;
//        mTrackContent = trackList.get(position);
//        startMusicPlayerService(MediaPlayerService.ACTION_PLAY);
//
//        //Remove any previous instance of the track player fragment prior to initializing this one.
//        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//        Fragment previousPlayer = getSupportFragmentManager().findFragmentByTag(TRACKPLAYER_TAG);
//        if (previousPlayer != null) {
//            ft.remove(previousPlayer);
//        }
//
//        TrackPlayerFragment fragment = TrackPlayerFragment.newInstance(trackList.get(position));
//
//        //show the fragment as a dialog in two pane mode and embed it in the container otherwise
//        if (mTwoPane) {
//            fragment.show(getSupportFragmentManager(), TRACKPLAYER_TAG);
//        }
//        else {
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.container, fragment, TRACKPLAYER_TAG)
//                    .commit();
//        }
    }

    public SpotifyService getSpotifyService()
    {
        return mSpotifyService;
    }

}

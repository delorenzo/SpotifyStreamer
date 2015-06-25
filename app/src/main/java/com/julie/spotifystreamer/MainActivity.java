package com.julie.spotifystreamer;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;

public class MainActivity extends AppCompatActivity implements ArtistFragment.OnArtistSelectedListener, TrackFragment.OnTrackSelectedListener {
    private static SpotifyService mSpotifyService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SpotifyApi api = new SpotifyApi();
        mSpotifyService = api.getService();
        if (findViewById(R.id.main_layout) != null) {
            if (savedInstanceState != null) {
                return;
            }
            ArtistFragment artistFragment = ArtistFragment.newInstance("");
            getSupportFragmentManager().beginTransaction().add(R.id.main_layout, artistFragment).commit();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (findViewById(R.id.main_layout) == null) {
            return;
        }

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Fragment artistFragment = ArtistFragment.newInstance(query);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (transaction.isEmpty()) {
                transaction.add(R.id.main_layout, artistFragment);
            }
            else {
                transaction.replace(R.id.main_layout, artistFragment);
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
        //for some reason the search hint from searchable.xml is not showing..
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

    public void onArtistSelected(String spotifyId, String artistName) {
        Fragment trackFragment = TrackFragment.newInstance(spotifyId, artistName);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_layout, trackFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void onTrackSelected(String spotifyId) {
        //TODO: play the track
    }

    public SpotifyService getSpotifyService()
    {
        return mSpotifyService;
    }
}

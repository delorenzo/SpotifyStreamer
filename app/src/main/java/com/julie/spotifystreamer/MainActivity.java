package com.julie.spotifystreamer;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
            ArtistFragment mArtistFragment = new ArtistFragment();
            mArtistFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(R.id.main_layout, mArtistFragment).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

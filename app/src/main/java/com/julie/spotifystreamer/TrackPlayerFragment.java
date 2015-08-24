package com.julie.spotifystreamer;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.support.v7.widget.ShareActionProvider;
import android.widget.TextView;

import com.julie.spotifystreamer.datacontent.TrackContent;
import com.squareup.picasso.Picasso;

//TrackPlayerFragment is a dialog fragment that displays the music player UI
//https://developer.android.com/reference/android/app/DialogFragment.html

public class TrackPlayerFragment extends DialogFragment {
    private static final String LOG_TAG = TrackPlayerFragment.class.getSimpleName();
    private static final String ARG_TRACK = "track";
    private static final String ARG_TRACK_DURATION = "trackDuration";
    private static final String ARG_SEEKBAR_POSITION = "seekBarPos";
    private static final String ARG_RESUME = "showPause";
    private TrackContent mTrackContent;
    private ImageButton mPauseButton;
    private ImageButton mResumeButton;
    private SeekBar mSeekBar;
    //this variable blocks the seek bar from advancing when the user is adjusting it
    private Boolean mDragging = false;
    private Boolean resumeVisible = false;
    OnSeekBarUserUpdateListener mCallBack;
    private int mDuration = 0;
    private int mPosition = 0;
    private ShareActionProvider mShareActionProvider;

    public TrackPlayerFragment() {
    }

    public static TrackPlayerFragment newInstance(TrackContent track) {
        TrackPlayerFragment fragment = new TrackPlayerFragment();
        Bundle args = new Bundle();
        args.setClassLoader(TrackContent.class.getClassLoader());
        args.putParcelable(ARG_TRACK, track);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallBack = (OnSeekBarUserUpdateListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSeekBarUserUpdateListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //obtain arguments
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            mTrackContent = getArguments().getParcelable(ARG_TRACK);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_track_player, container, false);

        if (mTrackContent == null) {
            Log.e(LOG_TAG, "No track content found.");
            return view;
        }

        mSeekBar = (SeekBar) view.findViewById(R.id.progress_bar);
        mPauseButton = (ImageButton) view.findViewById(R.id.button_pause);
        mResumeButton = (ImageButton) view.findViewById(R.id.button_play);

        //restore UI sensitive saved data
        if (savedInstanceState != null) {
            savedInstanceState.setClassLoader(TrackContent.class.getClassLoader());
            mTrackContent = savedInstanceState.getParcelable(ARG_TRACK);
            mPosition = savedInstanceState.getInt(ARG_SEEKBAR_POSITION);
            mDuration = savedInstanceState.getInt(ARG_TRACK_DURATION);
            resumeVisible = savedInstanceState.getBoolean(ARG_RESUME);
            setupProgressBar(mDuration, mPosition);
        }

        //the pause button is shown by default
        if (resumeVisible) {
            showResumeButton();
        }

        setupDisplayedTrack(view);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_track_player, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);

        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
        if (mTrackContent != null) {
            mShareActionProvider.setShareIntent(createShareSpotifyURLIntent());
        }
    }

    //set up the display from the current track
    private void setupDisplayedTrack(View view) {
        //set up the basic display content
        TextView artistTextView = (TextView)view.findViewById(R.id.artist_name);
        artistTextView.setText(mTrackContent.getArtistName());

        TextView albumTextView = (TextView)view.findViewById(R.id.album_name);
        albumTextView.setText(mTrackContent.getAlbumName());

        TextView trackTextView = (TextView)view.findViewById(R.id.track_name);
        trackTextView.setText(mTrackContent.getTrackName());

        ImageView trackImageView = (ImageView)view.findViewById(R.id.player_track_image);
        Picasso.with(getActivity())
                .load(mTrackContent.getThumbnailURL())
                .into(trackImageView);

        mSeekBar.setProgress(0);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //update the seekbar and notify the activity if the changes came from the user
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !mDragging) {
                    mSeekBar.setProgress(progress);
                    mCallBack.onSeekBarUserUpdate(progress);
                }
            }

            //the user has started dragging the seekbar
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mDragging = true;
            }

            //the user has stopped dragging the seekbar
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mDragging = false;
                mCallBack.onSeekBarUserUpdate(seekBar.getProgress());
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.setClassLoader(TrackContent.class.getClassLoader());
        outState.putParcelable(ARG_TRACK, mTrackContent);
        outState.putInt(ARG_SEEKBAR_POSITION, mPosition);
        outState.putInt(ARG_TRACK_DURATION, mDuration);
        outState.putBoolean(ARG_RESUME, resumeVisible);
    }


    @Override
    public void onStop() {
        super.onStop();
    }

    //public methods to be called by the activity

    //show the resume button instead of the pause button
    public void showResumeButton()
    {
        mPauseButton.setVisibility(View.GONE);
        resumeVisible = false;
        mResumeButton.setVisibility(View.VISIBLE);
    }

    //show the pause button instead of the resume button
    public void showPauseButton()
    {
        mPauseButton.setVisibility(View.VISIBLE);
        resumeVisible = true;
        mResumeButton.setVisibility(View.GONE);
    }

    //update the visible progress bar with the current position
    public void updateProgress(int pos)
    {
        if (!mDragging) {
            mSeekBar.setProgress(pos);
            //swap the pause and resume buttons if the end of playback is reached
            if (pos >= mSeekBar.getMax()) {
                showResumeButton();
            }
        }
    }

    //setup the progress bar with the duration and position
    public void setupProgressBar(int duration, int position)
    {
        mDuration = duration;
        mPosition = position;
        mSeekBar.setMax(duration);
        mSeekBar.setProgress(position);
    }

    //the track being played has changed - update the UI
    public void updateDisplayedTrack(TrackContent track) {
        mTrackContent = track;
        setupDisplayedTrack(this.getView());
    }

    interface OnSeekBarUserUpdateListener {
        void onSeekBarUserUpdate(int position);
    }

    private Intent createShareSpotifyURLIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mTrackContent.getPreviewURL());
        return shareIntent;
    }


}

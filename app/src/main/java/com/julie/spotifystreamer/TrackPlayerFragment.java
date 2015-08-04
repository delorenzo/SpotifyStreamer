package com.julie.spotifystreamer;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.julie.spotifystreamer.DataContent.TrackContent;
import com.squareup.picasso.Picasso;


//TrackPlayerFragment handles playing the selected track.
//see https://developer.android.com/guide/topics/media/mediaplayer.html
//and https://developer.android.com/reference/android/media/MediaPlayer.html

public class TrackPlayerFragment extends Fragment {
    private static final String LOG_TAG = TrackPlayerFragment.class.getSimpleName();
    private static final String ARG_TRACK = "track";
    private TrackContent mTrackContent;

    public TrackPlayerFragment() {
    }

    public static TrackPlayerFragment newInstance(TrackContent track) {
        TrackPlayerFragment fragment = new TrackPlayerFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_TRACK, track);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //obtain arguments
        super.onCreate(savedInstanceState);
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
                .resize(200,200)
                .centerCrop()
                .into(trackImageView);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_TRACK, mTrackContent);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mTrackContent = savedInstanceState.getParcelable(ARG_TRACK);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

}

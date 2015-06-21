package com.julie.spotifystreamer;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Hashtable;

import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnTrackSelectedListener}
 * interface.
 */
public class TrackFragment extends Fragment implements AbsListView.OnItemClickListener {

    private static final String ARG_SPOTIFY_ID = "spotifyId";
    private static final String LOG_TAG = TrackFragment.class.getSimpleName();
    private String mSpotifyId;
    private OnTrackSelectedListener mListener;
    private AbsListView mListView;
    private TrackArrayAdapter mAdapter;
    private SpotifyService mSpotifyService;
    //TODO: make this user modifiable via a settings preference screen
    private static final String COUNTRY_CODE = "US";

    public static TrackFragment newInstance(String spotifyId) {
        TrackFragment fragment = new TrackFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SPOTIFY_ID, spotifyId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TrackFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSpotifyId = getArguments().getString(ARG_SPOTIFY_ID);
        }
        mAdapter = new TrackArrayAdapter(getActivity(),
                android.R.layout.simple_list_item_1, new ArrayList<TrackContent>());
        mSpotifyService =  ((MainActivity)this.getActivity()).getSpotifyService();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        new RetrieveTrackTask().execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_track, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnTrackSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTrackSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            TrackContent track = mAdapter.getItem(position);
            mListener.onTrackSelected(track.getSpotifyId());
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnTrackSelectedListener {
        void onTrackSelected(String spotifyId);
    }

    private class RetrieveTrackTask extends AsyncTask<String, Void, ArrayList<TrackContent>> {
        @Override
        protected ArrayList<TrackContent> doInBackground(String... params) {
            Hashtable<String, Object> optionsMap = new Hashtable<>();
            optionsMap.put(SpotifyService.COUNTRY, COUNTRY_CODE);
            ArrayList<TrackContent> trackList = new ArrayList<>();
            Tracks tracks = mSpotifyService.getArtistTopTrack(mSpotifyId, optionsMap);
            if (tracks == null) {
                return null;
            }
            for (Track t: tracks.tracks) {
                String thumbnailURL = "";
                if (!t.album.images.isEmpty()) {
                    thumbnailURL = t.album.images.get(0).url;
                }
                trackList.add(new TrackContent(t.album.name, t.name, t.id, thumbnailURL));
            }
            return trackList;
        }

        @Override
        protected void onPostExecute(ArrayList<TrackContent> result) {
            if (result != null) {
                mAdapter.clear();
                mAdapter.addAll(result);
            }
        }
    }
}

package com.julie.spotifystreamer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link com.julie.spotifystreamer.ArtistFragment.OnArtistSelectedListener}
 * interface.
 */
public class ArtistFragment extends Fragment implements AbsListView.OnItemClickListener {

    private OnArtistSelectedListener mListener;
    private AbsListView mListView;
    private ArtistArrayAdapter mAdapter;
    private SpotifyService mSpotifyService;

    private String LOG_TAG = ArtistFragment.class.getSimpleName();


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArtistFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        new RetrieveArtistTask().execute("Coldplay");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist, container, false);

        // Set the adapter
        ArrayList<ArtistContent> artistList = new ArrayList<>();

        mAdapter = new ArtistArrayAdapter(getActivity(),
                android.R.layout.simple_list_item_1, artistList);
        mListView = (AbsListView) rootView.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        SpotifyApi api = new SpotifyApi();
        mSpotifyService = api.getService();

        return rootView;

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnArtistSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnArtistSelectedListener");
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
            //mListener.onFragmentInteraction(ArtistContent.getSpotifyId);
            ArtistContent artist = mAdapter.getItem(position);
            mListener.onArtistSelected(artist.getSpotifyId());
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
    public interface OnArtistSelectedListener {
        void onArtistSelected(String spotifyId);
    }

    private class RetrieveArtistTask extends AsyncTask<String, Void, ArrayList<ArtistContent>> {

        @Override
        protected ArrayList<ArtistContent> doInBackground(String... params) {
            if (params.length < 1) {
                Log.v(LOG_TAG, "doInBackground called with no params");
                return null;
            }
            String searchItem = params[0];
            ArtistsPager mArtistsPager = mSpotifyService.searchArtists(searchItem);
            ArrayList<ArtistContent> mArtistList = new ArrayList<>();
            List<Artist> resultList = mArtistsPager.artists.items;
            for (Artist a : resultList) {
                Bitmap thumbnailIcon = null;
                if (a.images.isEmpty()) {
                    //TODO:  set some placeholder image
                }
                else {
                    String url = a.images.get(0).url;
                    try {
                        InputStream in = new java.net.URL(url).openStream();
                        thumbnailIcon = BitmapFactory.decodeStream(in);
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Failed to import bitmap from " + url);
                    }
                }
                mArtistList.add(new ArtistContent(a.name, a.id, thumbnailIcon));
            }
            return mArtistList;
        }

        @Override
        protected void onPostExecute(ArrayList<ArtistContent> result) {
            if (result != null) {
                mAdapter.clear();
                mAdapter.addAll(result);
            }
        }
    }
}

package com.julie.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;

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
    private Toast mToast;
    private String mSearchQuery;

    private static final String ARG_SEARCH_QUERY = "searchQuery";
    private static final String LOG_TAG = ArtistFragment.class.getSimpleName();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArtistFragment() {
    }

    public static ArtistFragment newInstance(String searchQuery) {
        ArtistFragment fragment = new ArtistFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SEARCH_QUERY, searchQuery);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSearchQuery = getArguments().getString(ARG_SEARCH_QUERY);
        }
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        actionBar.setTitle(getString(R.string.app_name));
        mSpotifyService =  ((MainActivity)this.getActivity()).getSpotifyService();
        mAdapter = new ArtistArrayAdapter(getActivity(),
                android.R.layout.simple_list_item_1, new ArrayList<ArtistContent>());
    }

    @Override
    public void onStart() {
        super.onStart();
        setEmptyText(getString(R.string.empty_artist_list));
        if (mSearchQuery != null && !mSearchQuery.isEmpty()) {
            new RetrieveArtistTask(getActivity()).execute(mSearchQuery);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist, container, false);

        // Set the adapter
        mListView = (AbsListView) rootView.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

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
            mListener.onArtistSelected(artist.getSpotifyId(), artist.getName());
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
        void onArtistSelected(String spotifyId, String artistName);
    }

    private class RetrieveArtistTask extends AsyncTask<String, Void, ArrayList<ArtistContent>> {
        private Context mContext;
        public RetrieveArtistTask(Context context) {
            mContext = context;
        }
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
            if (resultList == null || resultList.isEmpty()) {
                return null;
            }
            for (Artist a : resultList) {
                String thumbnailURL = "";
                if (a.images.isEmpty()) {
                    //TODO:  set some placeholder image
                }
                else {
                    thumbnailURL = a.images.get(0).url;
                }
                mArtistList.add(new ArtistContent(a.name, a.id, thumbnailURL));
            }
            return mArtistList;
        }

        @Override
        protected void onPostExecute(ArrayList<ArtistContent> result) {
            if (result != null) {
                mAdapter.clear();
                mAdapter.addAll(result);
            }
            else {
                if (mToast != null) {
                    mToast.cancel();
                }
                mToast = Toast.makeText(mContext, "Artist not found.  Please refine your search.", Toast.LENGTH_SHORT);
            }
        }
    }
}

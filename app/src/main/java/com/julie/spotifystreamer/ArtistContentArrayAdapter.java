package com.julie.spotifystreamer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Custom ArrayAdapter that returns the view for the artist list item, including the thumbnail
 * image and the artist name.
 */
public class ArtistContentArrayAdapter extends ArrayAdapter {
    private List<ArtistContent> mArtistContentList;
    private LayoutInflater inflater;
    private Context mContext;
    ArtistContentArrayAdapter(Context context, int resource, List<ArtistContent> objects) {
        super(context, resource);
        mArtistContentList = objects;
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (inflater == null) {
            inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_artist, null);
        }

        ImageView thumbnailImage = (ImageView) convertView.findViewById(R.id.thumbnail_image);
        TextView artistName = (TextView) convertView.findViewById(R.id.artist_name);
        ArtistContent artist = mArtistContentList.get(position);

        thumbnailImage.setImageURI(artist.getThumbnailUri());
        artistName.setText(artist.getArtistName());
        return convertView;
    }
}

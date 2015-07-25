package com.julie.spotifystreamer;

import android.content.Context;
import android.media.Image;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;

/**
 * Custom ArrayAdapter that returns the view for the artist list item.
 */
public class ArtistArrayAdapter extends ArrayAdapter<ArtistContent> {

    //the ViewHolder pattern caches the view lookup to improve performance
    //see https://github.com/codepath/android_guides/wiki/Using-an-ArrayAdapter-with-ListView
    private static class ViewHolder {
        ImageView thumbnailImage;
        TextView artistName;
    }

    public ArtistArrayAdapter(Context context, int resource, ArrayList<ArtistContent> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ArtistContent artist = getItem(position);
        ViewHolder viewHolder;

        //If a view does not exist, inflate it and set up the ViewHolder.
        //Otherwise, load the ViewHolder from the existing view.
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater mInflater = LayoutInflater.from(getContext());
            convertView = mInflater.inflate(R.layout.list_item_artist, parent, false);
            viewHolder.thumbnailImage = (ImageView) convertView.findViewById(R.id.artist_thumbnail_image);
            viewHolder.artistName = (TextView) convertView.findViewById(R.id.artist_name);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        //Populate the data into the view
        if (artist.hasThumbnail()) {
            Picasso.with(getContext())
                    .load(artist.getThumbnailURL())
                    .resize(100, 100)
                    .centerCrop()
                    .into(viewHolder.thumbnailImage);
        }
        else {
            Picasso.with(getContext())
                    .load(R.mipmap.ic_launcher)
                    .into(viewHolder.thumbnailImage);
        }
        viewHolder.artistName.setText(artist.getName());

        return convertView;
    }
}

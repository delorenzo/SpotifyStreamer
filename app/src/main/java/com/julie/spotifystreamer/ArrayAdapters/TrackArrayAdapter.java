package com.julie.spotifystreamer.arrayadapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.julie.spotifystreamer.R;
import com.julie.spotifystreamer.datacontent.TrackContent;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Custom ArrayAdapter that returns the view for the track list item.
 */
public class TrackArrayAdapter extends ArrayAdapter<TrackContent> {
    //the ViewHolder pattern caches the view lookup to improve performance
    //see https://github.com/codepath/android_guides/wiki/Using-an-ArrayAdapter-with-ListView
    public static class ViewHolder {
        public final ImageView thumbnailImage;
        public final TextView trackName;
        public final TextView albumName;

        public ViewHolder(View view) {
            albumName = (TextView) view.findViewById(R.id.album_name);
            trackName = (TextView) view.findViewById(R.id.track_name);
            thumbnailImage = (ImageView) view.findViewById(R.id.album_thumbnail_image);
        }
    }

    public TrackArrayAdapter(Context context, int resource, ArrayList<TrackContent> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        TrackContent track = getItem(position);

        //If a view does not exist, inflate it and set up the ViewHolder.
        //Otherwise, load the ViewHolder from the existing view.
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_item_track, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        //populate the data into the view
        if (track.hasThumbnail()) {
            Picasso.with(getContext())
                    .load(track.getThumbnailURL())
                    .resize(100, 100)
                    .centerCrop()
                    .into(viewHolder.thumbnailImage);
        }
        else {
            Picasso.with(getContext())
                    .load(R.mipmap.placeholder_icon)
                    .into(viewHolder.thumbnailImage);
        }
        viewHolder.albumName.setText(track.getAlbumName());
        viewHolder.trackName.setText(track.getTrackName());

        return convertView;
    }
}

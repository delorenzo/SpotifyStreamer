package com.julie.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Helper class for storing track content.
 * Implements the Parcelable interface.
 * See http://www.developerphil.com/parcelable-vs-serializable/
 */
public class TrackContent implements Parcelable {
    private String albumName;
    private String trackName;
    private String spotifyId;
    private String thumbnailURL;

    public TrackContent (String album, String track, String id, String thumbnail) {
        albumName = album;
        trackName = track;
        spotifyId = id;
        thumbnailURL = thumbnail;
    }

    public String getAlbumName() {return albumName; }

    public String getSpotifyId()
    {
        return spotifyId;
    }

    public String getTrackName()
    {
        return trackName;
    }

    public String getThumbnailURL()
    {
        return thumbnailURL;
    }

    public Boolean hasThumbnail() { return !thumbnailURL.isEmpty(); }

    protected TrackContent(Parcel in) {
        albumName = in.readString();
        trackName = in.readString();
        spotifyId = in.readString();
        thumbnailURL = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(albumName);
        dest.writeString(trackName);
        dest.writeString(spotifyId);
        dest.writeString(thumbnailURL);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<TrackContent> CREATOR = new Parcelable.Creator<TrackContent>() {
        @Override
        public TrackContent createFromParcel(Parcel in) {
            return new TrackContent(in);
        }

        @Override
        public TrackContent[] newArray(int size) {
            return new TrackContent[size];
        }
    };
}
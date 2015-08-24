package com.julie.spotifystreamer.datacontent;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Helper class for storing Artist content.
 * Implements the parcelable interface.
 * See http://www.developerphil.com/parcelable-vs-serializable/
 */
public class ArtistContent implements Parcelable {
    private String artistName;
    private String spotifyId;
    private String thumbnailURL;

    protected ArtistContent(Parcel in) {
        artistName = in.readString();
        spotifyId = in.readString();
        thumbnailURL = in.readString();
    }

    public ArtistContent(String name, String id, String thumbnail)
    {
        artistName = name;
        spotifyId = id;
        thumbnailURL = thumbnail;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(artistName);
        dest.writeString(spotifyId);
        dest.writeString(thumbnailURL);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ArtistContent> CREATOR = new Parcelable.Creator<ArtistContent>() {
        @Override
        public ArtistContent createFromParcel(Parcel in) {
            return new ArtistContent(in);
        }

        @Override
        public ArtistContent[] newArray(int size) {
            return new ArtistContent[size];
        }
    };

    public String getSpotifyId()
    {
        return spotifyId;
    }

    public String getName()
    {
        return artistName;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public Boolean hasThumbnail() {
        return !thumbnailURL.isEmpty();
    }
}

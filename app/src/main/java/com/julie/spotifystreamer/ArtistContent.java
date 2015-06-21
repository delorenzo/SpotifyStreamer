package com.julie.spotifystreamer;

import android.graphics.Bitmap;
import android.net.Uri;

/**
 * Helper class for storing Artist content.
 */
public class ArtistContent {
    private String artistName;
    private String spotifyId;
    private String thumbnailURL;

    public ArtistContent(String name, String id, String thumbnail)
    {
        artistName = name;
        spotifyId = id;
        thumbnailURL = thumbnail;
    }

    public String getSpotifyId()
    {
        return spotifyId;
    }

    public String getName()
    {
        return artistName;
    }

    public String getThumbnailURL() {return thumbnailURL; }

    public Boolean hasThumbnail() { return !thumbnailURL.isEmpty(); }
}

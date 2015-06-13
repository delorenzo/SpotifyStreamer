package com.julie.spotifystreamer;

import android.net.Uri;

/**
 * Helper class for storing Artist content.
 */
public class ArtistContent {
    private String artistName;
    private int spotifyId;
    private Uri thumbnailUri;

    public ArtistContent(String name, int id, Uri thumbnail)
    {
        artistName = name;
        spotifyId = id;
        thumbnailUri = thumbnail;
    }

    public int getSpotifyId()
    {
        return spotifyId;
    }

    public String getArtistName()
    {
        return artistName;
    }

    public Uri getThumbnailUri()
    {
        return thumbnailUri;
    }
}

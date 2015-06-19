package com.julie.spotifystreamer;

import android.graphics.Bitmap;
import android.net.Uri;

/**
 * Helper class for storing Artist content.
 */
public class ArtistContent {
    private String artistName;
    private String spotifyId;
    private Bitmap thumbnailBitmap;

    public ArtistContent(String name, String id, Bitmap thumbnail)
    {
        artistName = name;
        spotifyId = id;
        thumbnailBitmap = thumbnail;
    }

    public String getSpotifyId()
    {
        return spotifyId;
    }

    public String getName()
    {
        return artistName;
    }

    public Bitmap getThumbnailBitmap()
    {
        return thumbnailBitmap;
    }
}

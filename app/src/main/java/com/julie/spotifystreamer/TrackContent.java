package com.julie.spotifystreamer;

import android.graphics.Bitmap;

/**
 * Helper class for storing track content.
 */
public class TrackContent {
    private String albumName;
    private String trackName;
    private String spotifyId;
    private Bitmap thumbnailBitmap;

    public TrackContent (String album, String track, String id, Bitmap thumbnail) {
        albumName = album;
        trackName = track;
        spotifyId = id;
        thumbnailBitmap = thumbnail;
    }

    String getAlbumName()
    {
        return albumName;
    }

    String getSpotifyId()
    {
        return spotifyId;
    }

    String getTrackName()
    {
        return trackName;
    }

    Bitmap getThumbnailBitmap()
    {
        return thumbnailBitmap;
    }
}

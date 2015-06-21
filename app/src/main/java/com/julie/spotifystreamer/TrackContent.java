package com.julie.spotifystreamer;

import android.graphics.Bitmap;

/**
 * Helper class for storing track content.
 */
public class TrackContent {
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
}

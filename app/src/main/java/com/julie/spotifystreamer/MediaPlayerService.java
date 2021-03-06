package com.julie.spotifystreamer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.julie.spotifystreamer.datacontent.TrackContent;
import com.julie.spotifystreamer.receivers.RemoteControlReceiver;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;

/**
 * MediaPlayerService:
 * Extension of Service that handles playing the track
 * so that the track can continue to play in the background.
 * See https://developer.android.com/guide/topics/media/mediaplayer.html
 * and
 * https://developer.android.com/reference/android/media/MediaPlayer.html
 * for details.
 */
public class MediaPlayerService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener {

    //actions
    public static final String ACTION_PLAY = "com.julie.spotifystreamer.action.PLAY";
    public static final String ACTION_STOP = "com.julie.spotifystreamer.action.STOP";
    public static final String ACTION_CHANGE_TRACK = "com.julie.spotifystreamer.action.CHANGE_TRACK";
    public static final String ACTION_PREVIOUS = "com.julie.spotifystreamer.action.PREVIOUS";
    public static final String ACTION_NEXT = "com.julie.spotifystreamer.action.NEXT";
    public static final String ACTION_PAUSE = "com.julie.spotifystreamer.action.PAUSE";
    public static final String ACTION_RESUME = "com.julie.spotifystreamer.action.RESUME";
    public static final String ACTION_PLAY_PAUSE = "com.julie.spotifystreamer.action.PLAY_PAUSE";
    public static final String ACTION_UPDATE_NOTIFICATION = "com.julie.spotifystreamer.action.UPDATE_NOTIFICATION";
    public static final String ACTION_PREPARED = "com.julie.spotifystreamer.action.PREPARED";
    public static final String ACTION_COMPLETE = "com.julie.spotifystreamer.action.COPMLETE";
    public static final String ACTION_TRACK_CHANGE = "com.julie.spotifystreamer.action.TRACK_CHANGE";

    //arguments
    public static final String ARG_TRACK_LIST = "trackList";
    public static final String ARG_TRACK_LIST_POSITION = "trackListPosition";

    //private constants
    private static final String LOG_TAG = MediaPlayerService.class.getSimpleName();
    private static final String WIFI_LOCK_TAG = "mediaPlayerWifiLock";
    private static final int mNotificationId = 1;
    private static final String MEDIASESSION_TAG = "mediaSession";
    private static final float MAX_VOLUME = 1.0f;
    private static final float MIN_VOLUME = 0.1f;
    private static final int mPendingIntentCode = 22;

    //private internals
    private MediaPlayer mMediaPlayer = null;
    private MediaSessionCompat mSession;
    private MediaControllerCompat.TransportControls mTransportController;
    private WifiManager.WifiLock mWifiLock;
    private final IBinder mBinder = new MediaPlayerBinder();
    private AudioManager mAudioManager;
    private NotificationCompat.Builder mNotificationBuilder;
    private Boolean isPrepared = false;
    private int duration = 0;
    private ArrayList<TrackContent> mTrackList;
    private TrackContent mCurrentTrack;
    private int mTrackListPosition;
    private Boolean userPaused = false;
    private Bitmap albumArt;

    //handle audio focus changes by pausing/resuming/adjusting audio as is appropriate
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mMediaPlayer == null) {
                    initMediaPlayer();
                }
                //only resume playback if it was a pause related to the loss of audio focus
                if (!userPaused) {
                    resume();
                }
                mMediaPlayer.setVolume(MAX_VOLUME, MAX_VOLUME);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                stop();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    mMediaPlayer.setVolume(MIN_VOLUME, MIN_VOLUME);
                }
                break;
        }
    }

    // The service is starting, due to a call to startService()
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }
        switch (intent.getAction()) {
            case ACTION_PLAY:
                userPaused = false;
                //if the activity is recovering from being stopped,
                //send out the prepared broadcast so it can update its UI.
                if (mMediaPlayer != null) {
                   resume();
                    Intent broadcastIntent = new Intent(ACTION_PREPARED);
                    broadcastIntent.putExtra(TrackPlayerActivity.ARG_TRACK, mCurrentTrack);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
                }
                //otherwise start the player normally
                else {
                    mTrackList = intent.getParcelableArrayListExtra(ARG_TRACK_LIST);
                    mTrackListPosition = intent.getIntExtra(ARG_TRACK_LIST_POSITION, 0);
                    mCurrentTrack = mTrackList.get(mTrackListPosition);
                    mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                            AudioManager.AUDIOFOCUS_GAIN);
                    if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        Log.e(LOG_TAG, "Audio focus request was denied.  Media player will not be started.");
                        return -1;
                    }
                    initMediaPlayer();
                    initNotification();
                    new LoadThumbnailTask().execute();
                }
                break;

            case ACTION_NEXT:
                skipToNext();
                break;

            case ACTION_PREVIOUS:
                skipToPrevious();
                break;

            case ACTION_PAUSE:
                userPaused = true;
                pause();
                break;

            case ACTION_RESUME:
                userPaused = false;
                resume();
                break;

            case ACTION_PLAY_PAUSE:
                if (mMediaPlayer != null) {
                    if (mMediaPlayer.isPlaying()) {
                        userPaused = true;
                        pause();
                    } else {
                        userPaused = false;
                        resume();
                    }
                }
                break;

            case ACTION_STOP:
                stop();
                break;

            //to change the data source on a media player you must call reset
            //because setDataSource() called in any other state throws an IllegalStateException
            case ACTION_CHANGE_TRACK:
                //if the media player exists it needs to be reset.
                changeTrack();
                break;

            case ACTION_UPDATE_NOTIFICATION:
                updateNotification();
                break;

            default:
                Log.e(LOG_TAG, "Unrecognized intent action called for:  " + intent.getAction());
                break;
        }
        return START_STICKY;
    }

    public void skipToNext() {
        if (mTrackList != null) {
            mTrackListPosition = (mTrackListPosition + 1) % mTrackList.size();
            mCurrentTrack = mTrackList.get(mTrackListPosition);
            changeTrack();
        }
    }

    public void skipToPrevious() {
        if (mTrackList != null) {
            mTrackListPosition --;
            if (mTrackListPosition < 0) {
                mTrackListPosition = mTrackList.size() -1;
            }
            mCurrentTrack = mTrackList.get(mTrackListPosition);
            changeTrack();
        }
    }

    public void changeTrack()
    {
        //tell the activity to update its fragment with the new track
        Intent intent = new Intent(ACTION_TRACK_CHANGE);
        intent.putExtra(TrackPlayerActivity.ARG_TRACK, mCurrentTrack);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        //to change the data source on a media player you must call reset
        //because setDataSource() called in any other state throws an IllegalStateException
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
        }
        initMediaPlayer();
        initNotification();
        new LoadThumbnailTask().execute();
    }

    private PendingIntent getNotificationContentIntent()
    {
        Intent contentIntent = new Intent(getApplicationContext(), TrackPlayerActivity.class);
        contentIntent.putExtra(TrackPlayerActivity.ARG_POSITION, mTrackListPosition);
        contentIntent.putExtra(TrackPlayerActivity.ARG_TRACK_LIST, mTrackList);
        return PendingIntent.getActivity(
                getApplicationContext(),
                0,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //make the service a foreground service by creating a notification for the status bar.
    private void initNotification() {
        //show lockscreen controls on pre-21 devices using MediaSessionCompat
        //see:
        //http://codeengine.org/media-session-compat-not-showing-lockscreen-controls-on-pre-lollipop/
        //https://developer.android.com/reference/android/support/v4/media/session/MediaSessionCompat.html
        //https://developer.android.com/reference/android/support/v7/app/NotificationCompat.MediaStyle.html
        //https://developer.android.com/training/managing-audio/volume-playback.html
        //https://developer.android.com/guide/topics/ui/notifiers/notifications.html#controllingMedia
        ComponentName mRemoteControlReceiver = new ComponentName(getPackageName(),
                RemoteControlReceiver.class.getName());
        //create the media button intent and set it to the remote control receiver
        final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mRemoteControlReceiver);

        //set up the media session
        mSession = new MediaSessionCompat(getApplicationContext(), MEDIASESSION_TAG,
                mRemoteControlReceiver, null);
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        PlaybackStateCompat mPlaybackState = new PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY |
                    PlaybackStateCompat.ACTION_PAUSE |
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(
                        mMediaPlayer.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                        mMediaPlayer.getCurrentPosition(),
                        MAX_VOLUME)
                .build();
        mSession.setPlaybackState(mPlaybackState);
        mSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mCurrentTrack.getArtistName())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, mCurrentTrack.getAlbumName())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mCurrentTrack.getTrackName())
                .build());
        mSession.setCallback(mMediaSessionCallback);
        mSession.setActive(true);

        //set visibility from shared preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean showOnLockScreen = prefs.getBoolean(getApplicationContext().getString(R.string.pref_show_notification_controls_key), true);
        int visibility = showOnLockScreen ? NotificationCompat.VISIBILITY_PUBLIC : NotificationCompat.VISIBILITY_PRIVATE;

        Intent deleteIntent = new Intent(getApplicationContext(), MediaPlayerService.class);
        deleteIntent.setAction(MediaPlayerService.ACTION_STOP);
        PendingIntent pendingDeleteIntent = PendingIntent.getService(
                getApplicationContext(),
                0,
                deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mNotificationBuilder =
                //for some reason this seems to need casting or the v4 notificationcompat is returned
                //v7 notificationcompat is required to use MediaStyle
                (android.support.v7.app.NotificationCompat.Builder)new NotificationCompat.Builder(getApplicationContext())
                        //set preferred visbility
                        .setVisibility(visibility)
                        //add media control buttons
                        .addAction(R.drawable.ic_skip_previous_black_24dp,
                                getString(R.string.msg_previous),
                                generatePendingIntent(MediaPlayerService.ACTION_PREVIOUS))
                        .addAction(R.drawable.ic_pause_black_24dp,
                                getString(R.string.msg_pause),
                                generatePendingIntent(MediaPlayerService.ACTION_PLAY_PAUSE))
                        .addAction(R.drawable.ic_skip_next_black_24dp, getString(R.string.msg_next),
                                generatePendingIntent(MediaPlayerService.ACTION_NEXT))
                        //set media style
                        .setStyle(new NotificationCompat.MediaStyle()
                                .setShowActionsInCompactView(0, 1, 2)
                                .setMediaSession(mSession.getSessionToken()))
                        .setContentTitle(getString(R.string.now_playing))
                        .setSmallIcon(R.drawable.ic_queue_music_black_24dp)
                        .setLargeIcon(albumArt)
                        .setContentText(mCurrentTrack.getTrackName())
                        //if the notificiation is dismissed, stop the service
                        .setDeleteIntent(pendingDeleteIntent)
                        //if the notification is clicked, restart the trackplayeractivity
                        .setContentIntent(getNotificationContentIntent());
        Notification notification = mNotificationBuilder.build();

        mTransportController = mSession.getController().getTransportControls();
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(mNotificationId, notification);

        startForeground(mNotificationId, notification);
    }

    private PendingIntent generatePendingIntent(String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        return PendingIntent.getBroadcast(
                getApplicationContext(),
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    private void updateNotification() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean showOnLockScreen = prefs.getBoolean(getApplicationContext()
                .getString(R.string.pref_show_notification_controls_key), true);
        int visibility = showOnLockScreen ? NotificationCompat.VISIBILITY_PUBLIC : NotificationCompat.VISIBILITY_PRIVATE;

        String playPauseString;
        int playPauseIcon;
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            playPauseIcon = R.drawable.ic_pause_black_24dp;
            playPauseString = getString(R.string.msg_pause);
        }
        else {
            playPauseIcon = R.drawable.ic_play_arrow_black_24dp;
            playPauseString = getString(R.string.msg_resume);
        }

        Intent deleteIntent = new Intent(getApplicationContext(), MediaPlayerService.class);
        deleteIntent.setAction(MediaPlayerService.ACTION_STOP);
        PendingIntent pendingDeleteIntent = PendingIntent.getService(
                getApplicationContext(),
                0,
                deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent contentIntent = new Intent(getApplicationContext(), TrackPlayerActivity.class);
        PendingIntent pendingContentIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mNotificationBuilder =
                //for some reason this seems to need casting or the v4 notificationcompat is returned
                //v7 notificationcompat is required to use MediaStyle
                (android.support.v7.app.NotificationCompat.Builder)new NotificationCompat.Builder(getApplicationContext())
                        //set preferred visbility
                        .setVisibility(visibility)
                        //add media control buttons
                        .addAction(R.drawable.ic_skip_previous_black_24dp,
                                getString(R.string.msg_previous),
                                generatePendingIntent(MediaPlayerService.ACTION_PREVIOUS))
                        .addAction(playPauseIcon, playPauseString,
                                generatePendingIntent(MediaPlayerService.ACTION_PLAY_PAUSE))
                        .addAction(R.drawable.ic_skip_next_black_24dp, getString(R.string.msg_next),
                                generatePendingIntent(MediaPlayerService.ACTION_NEXT))
                        //set media style
                        .setStyle(new NotificationCompat.MediaStyle()
                                .setShowActionsInCompactView(0, 1, 2)
                                .setMediaSession(mSession.getSessionToken()))
                        .setContentTitle(getString(R.string.now_playing))
                        .setSmallIcon(R.drawable.ic_queue_music_black_24dp)
                        .setLargeIcon(albumArt)
                        .setContentText(mCurrentTrack.getTrackName())
                        //if the notificiation is dismissed, stop the service
                        .setDeleteIntent(pendingDeleteIntent)
                        //if the notification is clicked, restart the trackplayeractivity
                        .setContentIntent(getNotificationContentIntent());
        Notification notification = mNotificationBuilder.build();

        mTransportController = mSession.getController().getTransportControls();
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(mNotificationId, notification);

        startForeground(mNotificationId, notification);
    }

    private void updateNotificationThumbnail(Bitmap image)
    {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationBuilder.setLargeIcon(image);
        mNotificationManager.notify(mNotificationId, mNotificationBuilder.build());
    }

    //the media player has completed playback.  release resources
    @Override
    public void onCompletion(MediaPlayer mp) {
        Intent intent = new Intent(ACTION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        mMediaPlayer.reset();
        mMediaPlayer.release();
        mMediaPlayer = null;
        stopSelf();
    }

    //initialize the media player
    private void initMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            setListeners();
        }
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        
        //acquire CPU lock to ensure playback is not interrupted
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        
        //acquire wifi lock to ensure playback is not interrupted
        acquireWifiLock();
        
        setPlayerDataSource();
    }
    //parses the music URI from the URI String and attempts to set the data source.
    private void setPlayerDataSource() {
        Uri musicUri = Uri.parse(mCurrentTrack.getPreviewURL());
        //setDataSource has the potential to throw IllegalArgumentException or IOException
        try {
            mMediaPlayer.setDataSource(this, musicUri);
            mMediaPlayer.prepareAsync();
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Illegal argument to MediaPlayer setDataSource() :  " + e.getMessage());
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException on MediaPlayer setDataSource():  " + e.getMessage());
        } catch (IllegalStateException e) {
            Log.e(LOG_TAG, "Set data source called from invalid media player state:  " + e.getMessage());
        }
    }

    //either pause or resume playback based on the current state of the media player
    public void playPause() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                pause();
            } else {
                resume();
            }
        }
    }

    //pause - transient audio focus loss
    //note we need to release the wifi lock and not the cpu lock because the MediaPlayer will handle that for us.
    public void pause()
    {
        mMediaPlayer.pause();
        releaseWifiLock();
        //unfortunately to update the lock screen button, you have to reinitialize the notification
        updateNotification();
    }

    //resume - recover from from transient audio focus loss
    public void resume()
    {
        acquireWifiLock();
        if (mMediaPlayer == null) {
            initMediaPlayer();
        }
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
        //unfortunately to update the lock screen button, you have to reinitialize the notification
        updateNotification();
    }

    //stop - indetermined length of audio focus loss.  release resources
    //note we need to release the wifi lock and not the cpu lock because the MediaPlayer will handle that for us.
    public void stop()
    {
        //sometimes onStop() is called with a media player that doesn't exist
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        isPrepared = false;
        //release wifi
        releaseWifiLock();
        //halt the notification and release the session
        stopForeground(true);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(mNotificationId);
        if (mSession != null) {
            mSession.release();
            mSession = null;
        }
        stopSelf();
    }

    private void releaseWifiLock() {
        if (mWifiLock != null) {
            mWifiLock.release();
        }
        mWifiLock = null;
    }

    private void acquireWifiLock() {
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK_TAG);
        mWifiLock.acquire();
    }

    /* set listeners for the media player:
    on prepared is called when playback is ready,
    on completion is called when playback is complete,
    on error allows us to recover from the error state by resetting the player.
    */
    private void setListeners() {
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
    }

    //the media player is prepared to start.
    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        duration = mp.getDuration();
        isPrepared = true;
        Intent intent = new Intent(ACTION_PREPARED);
        intent.putExtra(TrackPlayerActivity.ARG_TRACK, mCurrentTrack);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    //the service is being destroyed.  release resources
    @Override
    public void onDestroy() {
        stop();
    }

    // A client is binding to the service with bindService()
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //A client has unbound from the service
    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }


    //https://stackoverflow.com/questions/17731527/how-to-implement-a-mediaplayer-restart-on-errors-in-android
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.e(LOG_TAG, "Error:  Server died.  Resetting MediaPlayer");
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.e(LOG_TAG, "Error:  Unknown.  Resetting MediaPlayer");
                break;
        }
        mp.reset();
        initMediaPlayer();
        return true;
    }

    //Class used for the client binder, allowing clients to call public methods.
    public class MediaPlayerBinder extends Binder {
        MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    //return current position of the media player
    public int getCurrentPosition()
    {
        return isPrepared ? mMediaPlayer.getCurrentPosition() : 0;
    }

    //return duration of the media player
    public int getDuration()
    {
        return duration;
    }

    //seek the media player to the appropriate section of playback.
    public void MediaPlayerSeekTo(int progress) {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            mMediaPlayer.seekTo(progress);
            mMediaPlayer.start();
        }
    }

    //this call lets the attached activity verify the player is in the prepared state.
    public Boolean isPrepared() {
        return isPrepared;
    }

    //This task loads the bitmap for the notification image in the background, and updates
    //the notification once it has been loaded in.
    private class LoadThumbnailTask extends AsyncTask<Void, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground (Void... params) {
            try {
                return Picasso.with(getApplicationContext()).load(mCurrentTrack.getThumbnailURL()).get();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed to load thumbnail for track");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                albumArt = result;
                updateNotificationThumbnail(result);
            }
        }
    }

    //MediaSessionCompat callbacks for backwards compatible media style notification handling
    private final MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public boolean onMediaButtonEvent(Intent intent) {
            if (intent.getAction().equals(
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                onPause();
            } else if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        mTransportController.play();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        mTransportController.pause();
                        onPause();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        if (mMediaPlayer.isPlaying()) {
                            mTransportController.pause();
                        }
                        else {
                            mTransportController.play();
                        }
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        mTransportController.skipToNext();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        mTransportController.skipToPrevious();
                        break;
                    default:
                        Log.e(LOG_TAG, "Unhandled key event detected:  " + event.getKeyCode());
                        break;
                }
            }
            return super.onMediaButtonEvent(intent);
        }

        @Override
         public void onPlay() {
            super.onPlay();
            resume();
        }

        @Override
        public void onPause() {
            super.onPause();
            pause();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            skipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
        }

        @Override
        public void onStop() {
            super.onStop();
            stop();
        }
    };
}

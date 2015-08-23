package com.julie.spotifystreamer.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.julie.spotifystreamer.MediaPlayerService;

/**
 * Receiver that handles intents from the notification bar and notifies the service.
 */
public class MediaNotificationReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = MediaNotificationReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (MediaPlayerService.ACTION_PLAY.equals(action)) {
            context.startService(new Intent(MediaPlayerService.ACTION_PLAY));
        }
        else if (MediaPlayerService.ACTION_PAUSE.equals(action)) {
            context.startService(new Intent(MediaPlayerService.ACTION_PAUSE));
        }
        else if (MediaPlayerService.ACTION_PLAY_PAUSE.equals(action)) {
            context.startService((new Intent(MediaPlayerService.ACTION_PLAY_PAUSE)));
        }
        else if (MediaPlayerService.ACTION_NEXT.equals(action)) {
            context.startService(new Intent(MediaPlayerService.ACTION_NEXT));
        }
        else if (MediaPlayerService.ACTION_PREVIOUS.equals(action)) {
            context.startService(new Intent(MediaPlayerService.ACTION_PREVIOUS));
        }
        else {
           Log.e(LOG_TAG, "Unrecognized intent action :  " + action.toString());
        }
    }
}

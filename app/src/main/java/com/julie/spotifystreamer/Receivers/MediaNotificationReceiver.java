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
        Intent serviceIntent = new Intent(context, MediaPlayerService.class);
        if (MediaPlayerService.ACTION_PLAY.equals(action)) {
            serviceIntent.setAction(MediaPlayerService.ACTION_PLAY);
            context.startService(serviceIntent);
        }
        else if (MediaPlayerService.ACTION_PAUSE.equals(action)) {
            serviceIntent.setAction(MediaPlayerService.ACTION_PAUSE);
            context.startService(serviceIntent);
        }
        else if (MediaPlayerService.ACTION_PLAY_PAUSE.equals(action)) {
            serviceIntent.setAction(MediaPlayerService.ACTION_PLAY_PAUSE);
            context.startService(serviceIntent);
        }
        else if (MediaPlayerService.ACTION_NEXT.equals(action)) {
            serviceIntent.setAction(MediaPlayerService.ACTION_NEXT);
            context.startService(serviceIntent);
        }
        else if (MediaPlayerService.ACTION_PREVIOUS.equals(action)) {
            serviceIntent.setAction(MediaPlayerService.ACTION_PREVIOUS);
            context.startService(serviceIntent);
        }
        else {
           Log.e(LOG_TAG, "Unrecognized intent action :  " + action.toString());
        }
    }
}

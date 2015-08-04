package com.julie.spotifystreamer.Receivers;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.julie.spotifystreamer.MediaPlayerService;

/**
 *  Handles the ACTION_AUDIO_BECOMING_NOISY event by pausing music playback.
 */
public class MusicIntentReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(
                AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
            Intent pauseIntent = new Intent(context, MediaPlayerService.class);
            pauseIntent.setAction(MediaPlayerService.ACTION_STOP);
            context.startService(pauseIntent);
        }
    }
}

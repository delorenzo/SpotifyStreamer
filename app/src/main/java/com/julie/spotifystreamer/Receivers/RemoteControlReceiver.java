package com.julie.spotifystreamer.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;

import com.julie.spotifystreamer.MediaPlayerService;

/**
 * The RemoteControlReceiver handles hardware keypresses from remote media hardware
 * as well as lockscreen/notification button presses on pre lollipop devices.
 * See https://developer.android.com/training/managing-audio/volume-playback.html
 */
public class RemoteControlReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = RemoteControlReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    context.startService(new Intent(MediaPlayerService.ACTION_PLAY));
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    context.startService(new Intent(MediaPlayerService.ACTION_PAUSE));
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    context.startService(new Intent(MediaPlayerService.ACTION_PLAY_PAUSE));
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    context.startService(new Intent(MediaPlayerService.ACTION_NEXT));
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    context.startService(new Intent(MediaPlayerService.ACTION_PREVIOUS));
                    break;
                default:
                    Log.e(LOG_TAG, "Unhandled key event detected:  " + event.getKeyCode());
                    break;
            }
        }
    }
}

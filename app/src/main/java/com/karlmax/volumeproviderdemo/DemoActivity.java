package com.karlmax.volumeproviderdemo;

import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

public class DemoActivity extends AppCompatActivity {

    private static final String TAG = DemoActivity.class.getSimpleName();

    private static final int NOTIFICATION_ID = 1;
    private static final int DEFAULT_VOLUME = 50;

    private MediaSessionCompat session;
    private NotificationManagerCompat notificationManagerCompat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        showVolume(DEFAULT_VOLUME);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final Notification notification = createNotification(new DemoVolumeController());
        notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(NOTIFICATION_ID, notification);
    }

    @Override
    protected void onPause() {
        session.release();
        notificationManagerCompat.cancel(NOTIFICATION_ID);

        super.onPause();
    }

    private void showVolume(int volume) {
        final TextView volumeLabel = (TextView) findViewById(R.id.volume);
        if (volumeLabel != null) {
            volumeLabel.setText(getString(R.string.volumeLabelText, volume));
        }
    }

    private Notification createNotification(@NonNull final DemoVolumeController demoVolumeController) {
        Log.d(TAG, "createNotification()");

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (session != null) {
                session.release();
            }
            session = new MediaSessionCompat(this, "demoMediaSession");
            session.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 1, 1.0f)
                    .build());
            session.setPlaybackToRemote(createVolumeProvider(demoVolumeController));
            session.setActive(true);
        }

        return builder.build();
    }

    private VolumeProviderCompat createVolumeProvider(@NonNull final DemoVolumeController demoVolumeController) {
        // I don't use this callback directly, but i need to set it or my VolumeProvider will not work. (sounds
        // strange but i tried it several times)
        session.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(final Intent mediaButtonEvent) {
                Log.d(TAG, "onMediaButtonEvent() called with: " + "mediaButtonEvent = [" + mediaButtonEvent + "]");
                return super.onMediaButtonEvent(mediaButtonEvent);
            }
        });

        return new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE,
                100,
                demoVolumeController.getVolume()) {
            @Override
            public void onAdjustVolume(final int direction) {
                final int volume = demoVolumeController.setVolumeRelative(direction);
                showVolume(volume);

                Log.d(TAG, "onAdjustVolume() called with: " + "direction = [" + direction + "] - " +
                        "new volume=" + volume);

                // nasty hack to get sync with the volume overlay of android. setCurrentVolume does not work :(
                session.setPlaybackToRemote(createVolumeProvider(demoVolumeController));
            }
        };
    }

    private static class DemoVolumeController {

        private int volume = DEFAULT_VOLUME;

        public int getVolume() {
            return volume;
        }

        public int setVolumeRelative(int direction) {
            volume += direction;
            return volume;
        }
    }
}

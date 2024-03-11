package ca.chris_macleod.passiverecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RecordingService extends Service {
    // At 32KHz some audible frequencies will be lost. Unfortunately, 32KHz is the highest sample
    // rate supported that results in evenly divisible frame durations at the millisecond level.
    // If arbitrary sample rates ever gain support, up the rate to 40960Hz.
    static final int SAMPLE_RATE = 32000;
    static final int MAX_FRAMES = 562500; // TODO: Allow configuration
    static final int BIT_RATE = 96000;
    static final int CHANNEL_COUNT = 1;

    private static final String TAG = RecordingService.class.getSimpleName();

    private AACBuffer buffer;
    private AudioRecord recorder;
    private HandlerThread encodingThread;
    private MediaCodec codec;

    public RecordingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "service bound");
        return new RecordingServiceInterface(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        startForeground(1, makeNotification());

        try {
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        } catch (IOException exception) {
            Log.e(TAG, exception.toString());
            return;
        }

        buffer = new AACBuffer(MAX_FRAMES);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        recorder = new AudioRecord(MediaRecorder.AudioSource.UNPROCESSED, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 8192);
        recorder.startRecording();

        encodingThread = new HandlerThread("EncodingThread");
        encodingThread.start();
        Handler handler = new Handler(encodingThread.getLooper());

        EncoderCallback callback = new EncoderCallback(recorder, buffer);
        codec.setCallback(callback, handler);

        MediaFormat format = makeFormat();
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (MediaCodec.CodecException e) {
            Log.e(TAG, e.toString());
            return;
        }

        codec.start();

        Log.d(TAG, "service created");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "service unbound");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "service destroyed");
        encodingThread.quit();
        try {
            encodingThread.join();
        } catch (InterruptedException exception) {
            Log.e(TAG, exception.toString());
        }
        codec.stop();
        recorder.stop();
        codec.release();
        recorder.release();
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    int frameCount() {
        return buffer.frames;
    }

    int byteCount() {
        return buffer.byteCount;
    }

    private MediaFormat makeFormat() {
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, CHANNEL_COUNT);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AACFrame.samples * 2);
        return format;
    }

    private Notification makeNotification() {
        NotificationChannel chan = new NotificationChannel("recording_service",
                "Recording Service", NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        String appName = getResources().getString(R.string.app_name);
        Notification.Builder notificationBuilder = new Notification.Builder(this, chan.getId());
        notificationBuilder.setOngoing(true);
        notificationBuilder.setSmallIcon(R.drawable.ic_stat_name);
        notificationBuilder.setContentTitle(appName);
        notificationBuilder.setContentText(appName + " is running in the background");
        notificationBuilder.setContentIntent(pendingIntent);
        return notificationBuilder.build();
    }

    boolean save(long start, long end) {
        ContentResolver resolver = getApplicationContext().getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, LocalDateTime.now() + ".aac");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, MediaFormat.MIMETYPE_AUDIO_AAC);
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/Recordings"); // API 29 only, hence the deprecated method below
        Uri uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues);

        AACFrame[] span = buffer.range(start, end, TimeUnit.MILLISECONDS);

        try (OutputStream outFile = resolver.openOutputStream(Objects.requireNonNull(uri))) {
            for (AACFrame frame : span) {
                Objects.requireNonNull(outFile).write(frame.data);
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            return false;
        }
        return true;
    }
}

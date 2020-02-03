package ca.chris_macleod.passiverecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
        stopForeground(true);
        super.onDestroy();
    }

    int frameCount() {
        return buffer.frames;
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
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification.Builder notificationBuilder = new Notification.Builder(this, chan.getId());
        notificationBuilder.setOngoing(true);
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher_foreground);
        notificationBuilder.setContentTitle("Passive Recorder");
        notificationBuilder.setContentText(R.string.app_name + " is running in the background");
        notificationBuilder.setContentIntent(pendingIntent);
        return notificationBuilder.build();
    }

    void save(long start, long end, TimeUnit unit) {
        // TODO: Do I/O on a worker thread
        // ^This is only a pain because of the Toasts
        try {
            // TODO: Replace getExternalStorageDirectory(), it's deprecated
            String filename = String.format("%s%s%s", Environment.getExternalStorageDirectory(), File.separator, "passiverecordertest.aac");
            FileOutputStream outFile = new FileOutputStream(filename);

            AACFrame[] span = buffer.range(start, end, unit);

            for (AACFrame frame : span) {
                outFile.write(frame.data);
            }

            Toast.makeText(getApplicationContext(), "audio saved", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Log.e(TAG, e.toString());
            Toast.makeText(getApplicationContext(), "could not save", Toast.LENGTH_SHORT).show();
        }
    }
}
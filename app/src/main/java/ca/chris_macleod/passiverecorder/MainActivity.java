package ca.chris_macleod.passiverecorder;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSIONS_BASIC = 1;

    ServiceConnection connection;
    TextWatcher watcher;
    EditText startTime;
    EditText endTime;
    boolean refresh;
    Handler handler;
    Runnable refreshFunction;
    private RecordingServiceInterface service;
    private Button saveButton;
    private ToggleButton toggleButton;
    private TextView statistics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: Permissions are constantly required but can be retracted at any time. Mitigate.
        // Also, clean this mess up.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.FOREGROUND_SERVICE}, PERMISSIONS_BASIC);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_BASIC);
            }
        }

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, final IBinder binder) {
                service = (RecordingServiceInterface) binder;
                toggleButton.setChecked(true);
                startTime.setEnabled(true);
                endTime.setEnabled(true);
                refresh = true;
                handler.post(refreshFunction);
                Log.i(TAG, "service connected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.i(TAG, "service disconnected");
            }

            @Override
            public void onBindingDied(ComponentName name) {
                Log.d(TAG, "binding died");
            }
        };

        toggleButton = findViewById(R.id.toggleButton);
        saveButton = findViewById(R.id.saveButton);
        startTime = findViewById(R.id.startTime);
        endTime = findViewById(R.id.endTime);
        statistics = findViewById(R.id.statistics);

        watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkText();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        startTime.addTextChangedListener(watcher);
        endTime.addTextChangedListener(watcher);

        saveButton.setEnabled(false);
        startTime.setEnabled(false);
        endTime.setEnabled(false);

        handler = new Handler(getMainLooper());

        refreshFunction = new Runnable() {
            @Override
            public void run() {
                if (refresh) {
                    long total = service.frameCount() * AACFrame.milliseconds;
                    long seconds = (total / 1000) % 60;
                    long minutes = (total / (60000)) % 60;
                    long hours = (total / (3600000)) % 24;
                    String s = String.format("There are %d hours, %d minutes, and %d seconds of audio in memory, consuming %d bytes.", hours, minutes, seconds, service.byteCount());
                    statistics.setText(s);
                    checkText();
                    handler.postDelayed(this, 1000);
                }
            }
        };

        Intent intent = new Intent(this, RecordingService.class);
        bindService(intent, connection, 0);
    }

    private void checkText() {
        if (service != null) {
            TimeStamp startStamp = new TimeStamp(startTime.getText());
            TimeStamp endStamp = new TimeStamp(endTime.getText());
            if (startStamp.valid() && endStamp.valid()) {
                long bufferLength = service.frameCount() * AACFrame.milliseconds;
                if (endStamp.toMilliseconds() < startStamp.toMilliseconds() && startStamp.toMilliseconds() < bufferLength) {
                    saveButton.setEnabled(true);
                }
            } else {
                saveButton.setEnabled(false);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

    public void onSaveButtonClick(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "app does not have permission to write to storage", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText startText = findViewById(R.id.startTime);
        TimeStamp startStamp = new TimeStamp(startText.getText());
        final long start = startStamp.toMilliseconds();

        EditText endText = findViewById(R.id.endTime);
        TimeStamp endStamp = new TimeStamp(endText.getText());
        final long end = endStamp.toMilliseconds();

        // Look at this garbage.
        // TODO: De-Java this
        Thread thread = new Thread() {
            @Override
            public void run() {
                if (service.save(start, end, TimeUnit.MILLISECONDS)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "audio saved", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "could not save", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        };
        thread.start();
    }

    public void onToggleButtonClick(View view) {
        Intent intent = new Intent(this, RecordingService.class);
        if (service == null) {
            startForegroundService(intent);
            bindService(intent, connection, 0);
        } else {
            unbind();
        }
    }

    private void unbind() {
        refresh = false;
        statistics.setText(getResources().getString(R.string.statistics));

        Intent intent = new Intent(this, RecordingService.class);
        unbindService(connection);
        stopService(intent);
        service = null;
        toggleButton.setChecked(false);
        saveButton.setEnabled(false);
        startTime.setEnabled(false);
        endTime.setEnabled(false);
    }

    @Override
    protected void onPause() {
        refresh = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (service != null) {
            refresh = true;
            handler.post(refreshFunction);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // TODO: Do this with less magic
        if (requestCode == PERMISSIONS_BASIC && grantResults.length > 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) { // Record permission
                finishAffinity();
            }
            if (grantResults.length > 2 && grantResults[2] != PackageManager.PERMISSION_GRANTED) { // Service permission
                finishAffinity();
            }
        }
    }
}

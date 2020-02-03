package ca.chris_macleod.passiverecorder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    ServiceConnection connection;
    TextWatcher watcher;
    EditText startTime;
    EditText endTime;
    private RecordingServiceInterface service;
    private Button saveButton;
    private ToggleButton toggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Request permissions, and check for them every time they're used
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, final IBinder binder) {
                service = (RecordingServiceInterface) binder;
                toggleButton.setChecked(true);
                startTime.setEnabled(true);
                endTime.setEnabled(true);
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

        watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkText(s);
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

        Intent intent = new Intent(this, RecordingService.class);
        bindService(intent, connection, 0);
    }

    private void checkText(CharSequence s) {
        if (service != null) {
            TimeStamp stamp = new TimeStamp(s);
            if (stamp.valid()) {
                long bufferLength = service.frameCount() * AACFrame.milliseconds;
                if (stamp.toMilliseconds() < bufferLength) {
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
        EditText startText = findViewById(R.id.startTime);
        TimeStamp startStamp = new TimeStamp(startText.getText());
        final long start = startStamp.toMilliseconds();

        EditText endText = findViewById(R.id.endTime);
        TimeStamp endStamp = new TimeStamp(endText.getText());
        final long end = endStamp.toMilliseconds();

        service.save(start, end, TimeUnit.MILLISECONDS);
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
        Intent intent = new Intent(this, RecordingService.class);
        unbindService(connection);
        stopService(intent);
        service = null;
        toggleButton.setChecked(false);
        saveButton.setEnabled(false);
        startTime.setEnabled(false);
        endTime.setEnabled(false);
    }
}

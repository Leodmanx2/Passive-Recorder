package ca.chris_macleod.passiverecorder;

import android.os.Binder;

import java.util.concurrent.TimeUnit;

class RecordingServiceInterface extends Binder {
    final private RecordingService service;

    RecordingServiceInterface(RecordingService service) {
        this.service = service;
    }

    void save(long oldestOffset, long newestOffset, TimeUnit unit) {
        service.save(oldestOffset, newestOffset, unit);
    }

    int frameCount() {
        return service.frameCount();
    }
}

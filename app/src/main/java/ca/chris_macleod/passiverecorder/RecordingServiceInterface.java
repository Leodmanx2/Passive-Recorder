package ca.chris_macleod.passiverecorder;

import android.os.Binder;

import java.util.concurrent.TimeUnit;

class RecordingServiceInterface extends Binder {
    final private RecordingService service;

    RecordingServiceInterface(RecordingService service) {
        this.service = service;
    }

    boolean save(long oldestOffset, long newestOffset, TimeUnit unit) {
        return service.save(oldestOffset, newestOffset, unit);
    }

    int frameCount() {
        return service.frameCount();
    }

    int byteCount() {
        return service.byteCount();
    }
}

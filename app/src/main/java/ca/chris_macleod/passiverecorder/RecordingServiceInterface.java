package ca.chris_macleod.passiverecorder;

import android.os.Binder;

class RecordingServiceInterface extends Binder {
    final private RecordingService service;

    RecordingServiceInterface(RecordingService service) {
        this.service = service;
    }

    boolean save(long oldestOffset, long newestOffset) {
        return service.save(oldestOffset, newestOffset);
    }

    int frameCount() {
        return service.frameCount();
    }

    int byteCount() {
        return service.byteCount();
    }
}

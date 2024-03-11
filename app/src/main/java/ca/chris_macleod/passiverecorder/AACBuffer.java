package ca.chris_macleod.passiverecorder;

import android.os.Build;

import java.util.concurrent.TimeUnit;

class AACBuffer {
    private final AACFrame[] data;
    int frames;
    int byteCount;
    private int index; // Normalized with the module operator on use

    AACBuffer(int maxFrames) {
        data = new AACFrame[maxFrames];
        index = 0;
    }

    synchronized void insert(AACFrame frame) {
        final AACFrame existing = data[index % data.length];
        if (existing != null) {
            byteCount -= existing.data.length;
        }
        byteCount += frame.data.length;

        data[index % data.length] = frame;
        if (frames < data.length) ++frames;
        ++index;
    }

    synchronized AACFrame[] range(long oldestOffset, long newestOffset, TimeUnit unit) {
        int start = (int) (unit.toMillis(oldestOffset) / AACFrame.milliseconds);
        int end = (int) (unit.toMillis(newestOffset) / AACFrame.milliseconds);
        if (frames < start) throw new IndexOutOfBoundsException();
        AACFrame[] result = new AACFrame[start - end];
        int dst = 0;
        for (int src = index - start; src < index - end; ++src) {
            result[dst] = data[src % frames];
            ++dst;
        }
        return result;
    }

}

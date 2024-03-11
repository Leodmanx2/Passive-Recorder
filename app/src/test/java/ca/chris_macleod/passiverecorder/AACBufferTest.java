package ca.chris_macleod.passiverecorder;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

public class AACBufferTest {
    @org.junit.jupiter.api.Test
    public void insertConsistentWithRangeAccess_fullBuffer() {
        final int frameCount = 4;
        AACBuffer buffer = new AACBuffer(frameCount);
        AACFrame[] frames = new AACFrame[frameCount];
        for (int i = 0; i < frameCount; ++i) {
            byte[] data = {(byte) i};
            frames[i] = new AACFrame(data);
            buffer.insert(frames[i]);
        }
        long oldest = frameCount * AACFrame.milliseconds;
        long newest = 0;
        AACFrame[] retrieved = buffer.range(oldest, newest, TimeUnit.MILLISECONDS);
        Assert.assertArrayEquals(frames, retrieved);
    }

    @Test
    public void insertConsistentWithRangeAccess_partialBuffer() {
        final int frameCount = 4;
        AACBuffer buffer = new AACBuffer(6);
        AACFrame[] frames = new AACFrame[frameCount];
        for (int i = 0; i < frameCount; ++i) {
            byte[] data = {(byte) i};
            frames[i] = new AACFrame(data);
            buffer.insert(frames[i]);
        }
        long oldest = frameCount * AACFrame.milliseconds;
        long newest = 0;
        AACFrame[] retrieved = buffer.range(oldest, newest, TimeUnit.MILLISECONDS);
        Assert.assertArrayEquals(frames, retrieved);
    }

    @org.junit.jupiter.api.Test
    public void insertConsistentWithRangeAccess_cyclingBuffer() {
        final int frameCount = 4;
        AACBuffer buffer = new AACBuffer(frameCount);
        // Insert a couple samples that the buffer should clobber
        for (int i = 0; i < 2; ++i) {
            byte[] data = {(byte) i};
            buffer.insert(new AACFrame(data));
        }
        AACFrame[] frames = new AACFrame[frameCount];
        for (int i = 0; i < frameCount; ++i) {
            byte[] data = {(byte) i};
            frames[i] = new AACFrame(data);
            buffer.insert(frames[i]);
        }
        long oldest = frameCount * AACFrame.milliseconds;
        long newest = 0;
        AACFrame[] retrieved = buffer.range(oldest, newest, TimeUnit.MILLISECONDS);
        Assert.assertArrayEquals(frames, retrieved);
    }
}

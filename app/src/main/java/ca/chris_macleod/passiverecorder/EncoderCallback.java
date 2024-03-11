package ca.chris_macleod.passiverecorder;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

class EncoderCallback extends MediaCodec.Callback {
    private static final String TAG = RecordingService.class.getSimpleName();

    final private AudioRecord recorder;
    final private AACBuffer buffer;

    EncoderCallback(AudioRecord recorder, AACBuffer buffer) {
        this.recorder = recorder;
        this.buffer = buffer;
    }

    @Override
    public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
        ByteBuffer inputBuffer = codec.getInputBuffer(index);
        if (inputBuffer != null) {
            int bytesRead = recorder.read(inputBuffer, AACFrame.samples * 2); // 16bit samples
            codec.queueInputBuffer(index, 0, bytesRead, 0, 0);
        }
    }

    @Override
    public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
        ByteBuffer outputBuffer = codec.getOutputBuffer(index);
        if (outputBuffer != null && info.size > 0) {
            int packetSize = 7 + info.size;
            byte[] packet = new byte[packetSize];
            outputBuffer.get(packet, 7, info.size);

            // ADTS header
            packet[0] = (byte) 0b11111111;
            packet[1] = (byte) 0b11111001;
            packet[2] = (byte) 0b01010100;
            packet[3] = (byte) (0b01000000 | ((packetSize & 0b1100000000000) >> 11));
            packet[4] = (byte) ((packetSize & 0b1111111111000) >> 3);
            packet[5] = (byte) (((packetSize & 0b111) << 5) | 0b11111);
            packet[6] = (byte) 0b11111100;

            buffer.insert(new AACFrame(packet));
        } else {
            Log.d(TAG, "outputBuffer is null");
        }
        codec.releaseOutputBuffer(index, false);
    }

    @Override
    public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException exception) {
        Log.e(TAG, exception.toString());
    }

    @Override
    public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
        Log.w(TAG, String.format("output format changed to %s", format));
    }
}

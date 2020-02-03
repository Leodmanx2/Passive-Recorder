package ca.chris_macleod.passiverecorder;

class AACFrame {
    final static int samples = 1024;
    final static int milliseconds = 32;
    final byte[] data;

    AACFrame(byte[] buffer) {
        assert buffer != null;
        data = buffer;
    }
}

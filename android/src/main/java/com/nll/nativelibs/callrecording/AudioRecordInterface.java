package com.nll.nativelibs.callrecording;

import java.nio.ByteBuffer;

public interface AudioRecordInterface {
    void startRecording();

    int read(byte[] audioData, int offsetInBytes, int sizeInBytes);

    int read(short[] audioData, int offsetInShorts, int sizeInShorts);

    int read(ByteBuffer audioBuffer, int sizeInBytes);

    int getRecordingState();

    void stop() throws IllegalStateException;

    void release();

    int getState();
}

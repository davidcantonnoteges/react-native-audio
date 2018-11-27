package com.nll.sample;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import com.nll.nativelibs.callrecording.AudioRecordWrapper;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;



public class WavAudioRecorder{
    enum RecordingState {
        INITIALIZING, READY, RECORDING, ERROR, STOPPED, PAUSED
    }
    public interface ErrorListener {
        void onError(Exception e);
    }
    private static final int TIMER_INTERVAL = 120;
    private String tag = "WavAudioRecorder";
    private int mGain = 0;
    private AudioRecordWrapper aRecorder = null;
    private int mMaxAmplitude = 0;
    private String mPath = null;
    private RecordingState mState;
    private RandomAccessFile mRAFile;
    private short mChannels;
    private int mRate;
    private short mResolution;
    private int mBufferSize;
    private int mFramePeriod;
    private byte[] mBuffer;
    private int mPayloadSize;
    private int aSource;
    private int aFormat;
    private int sRate;
    private int mChannelConfig;
    private ErrorListener mErrorListener;
    private  Context context;

    public WavAudioRecorder(Context context, int audioSource, int sampleRate, int channelConfig, int audioFormat, ErrorListener listener) {
        Log.i("SAMPLE", "WavAudioRecorder: ");
        this.context = context;
        //needs to be set early, otherwise it causes null pointer
        setErorListener(listener);
        aSource = audioSource;
        aFormat = audioFormat;
        sRate = sampleRate;

        try {
            if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
                mResolution = 16;
            } else {
                mResolution = 8;
            }

            if (channelConfig == AudioFormat.CHANNEL_IN_MONO) {
                mChannels = 1;
            } else {
                mChannels = 2;
            }

            mRate = sampleRate;
            mFramePeriod = sampleRate * TIMER_INTERVAL / 1000;
            mBufferSize = mFramePeriod * 2 * mResolution * mChannels / 8;
            if (mBufferSize < AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)) {
                mBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                mFramePeriod = mBufferSize / (2 * mResolution * mChannels / 8);


            }

            Log.i("SAMPLE", "mBufferSize: " + mBufferSize);

            aRecorder = new AudioRecordWrapper(context, audioSource, sampleRate, channelConfig, audioFormat, mBufferSize);
            if (aRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.i("SAMPLE", "AudioRecordWrapper initialization failed: ");
                throw new Exception("AudioRecordWrapper initialization failed");
            }

            mMaxAmplitude = 0;
            mPath = null;
            mState = RecordingState.INITIALIZING;
            Log.i("SAMPLE", "AudioRecordWrapper initialized: ");


        } catch (Exception e) {
            Log.e("SAMPLE", "AudioRecordWrapper::Exception caught ", e);

            mState = RecordingState.ERROR;
            mErrorListener.onError(e);
        }
    }

    private static short getShortFromByte(byte argB1, byte argB2) {


        return (short) ((argB1 & 0xff) | (argB2 << 8));
    }

    private static byte[] getByteFromShort(short x) {

        byte[] a = new byte[2];
        a[0] = (byte) (x & 0xff);
        a[1] = (byte) ((x >> 8) & 0xff);
        return a;

    }

    public RecordingState getRecordingState() {
        return mState;
    }

    public void prepare() {
        try {
            Log.i("SAMPLE", "prepare: ");
            if (mState == RecordingState.INITIALIZING) {
                Log.i("SAMPLE", "prepare: INITIALIZING");
                if ((aRecorder.getState() == AudioRecord.STATE_INITIALIZED) && (mPath != null)) {
                    Log.i("SAMPLE", "prepare: Writing header");
                    mRAFile = new RandomAccessFile(mPath, "rw");
                    mRAFile.setLength(0);
                    mRAFile.writeBytes("RIFF");
                    mRAFile.writeInt(0);
                    mRAFile.writeBytes("WAVE");
                    mRAFile.writeBytes("fmt ");
                    mRAFile.writeInt(Integer.reverseBytes(16));
                    mRAFile.writeShort(Short.reverseBytes((short) 1));
                    mRAFile.writeShort(Short.reverseBytes(mChannels));
                    mRAFile.writeInt(Integer.reverseBytes(mRate));
                    mRAFile.writeInt(Integer.reverseBytes(mRate * (mResolution / 8) * mChannels));
                    mRAFile.writeShort(Short.reverseBytes((short) (mChannels * mResolution / 8)));
                    mRAFile.writeShort(Short.reverseBytes(mResolution));
                    mRAFile.writeBytes("data");
                    mRAFile.writeInt(0);
                    mBuffer = new byte[mFramePeriod * (mResolution / 8) * mChannels];
                    mState = RecordingState.READY;
                } else {
                    Log.i("SAMPLE", "prepare: Error");

                    mState = RecordingState.ERROR;
                }
            } else {
                Log.i("SAMPLE", "prepare: Error-Release");

                release();
                mState = RecordingState.ERROR;
            }
        } catch (Exception e) {
            Log.e("SAMPLE", "prepare: Exception caught.", e);
            mState = RecordingState.ERROR;
            mErrorListener.onError(e);
        }
    }

    public void release() {
        Log.i("SAMPLE", "release: ");
        if (mState == RecordingState.RECORDING || mState == RecordingState.PAUSED) {
            stop();
        } else {
            if ((mState == RecordingState.READY)) {
                try {
                    mRAFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    //already stopped, do not call on error creates loop
                    //mErrorListener.onError(e);
                }
                (new File(mPath)).delete();
            }
        }

        if (aRecorder != null) {
            aRecorder.release();
        }
    }

    public void start() {
        Log.i("SAMPLE", "start: ");
        if (mState == RecordingState.READY) {
            Log.i("SAMPLE", "start: READY");
            mPayloadSize = 0;
            aRecorder.startRecording();

            aRecorder.read(mBuffer, 0, mBuffer.length);
            mState = RecordingState.RECORDING;
            new Thread() {
                public void run() {
                    while (aRecorder != null && aRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        int status = read(aRecorder);
                        if (status < 0) {
                            break;
                        }
                    }
                }
            }.start();
        } else {
            Log.i("SAMPLE", "start: ERROR");

            mState = RecordingState.ERROR;
        }
    }

    public void resume() {
        mState = RecordingState.RECORDING;
    }

    public void pause() {
        mState = RecordingState.PAUSED;
    }

    public boolean isRecording() {
        return mState == RecordingState.RECORDING;
    }

    public void stop() {
       Log.i("SAMPLE", "stop: ");
        if (mState == RecordingState.RECORDING) {

            Log.i("SAMPLE", "stop:RECORDING ");
            aRecorder.stop();
            try {
                mRAFile.seek(4); // Write size to RIFF header
                mRAFile.writeInt(Integer.reverseBytes(36 + mPayloadSize));

                mRAFile.seek(40); // Write size to Subchunk2Size field
                mRAFile.writeInt(Integer.reverseBytes(mPayloadSize));

                mRAFile.close();
                mState = RecordingState.STOPPED;
            } catch (IOException e) {


                mState = RecordingState.ERROR;
                //already stopped, do not call on error creates loop
                //mErrorListener.onError(e);
            }
        } else {

            Log.i("SAMPLE", "stop: ERROR");
            mState = RecordingState.ERROR;
        }
    }

    private int read(AudioRecordWrapper recorder) {
        // public int read (byte[] audioData, int offsetInBytes, int
        // sizeInBytes)
        int numberOfBytes = recorder.read(mBuffer, 0, mBuffer.length);
        if (numberOfBytes == AudioRecord.ERROR_INVALID_OPERATION) {


            return -1;
        } else if (numberOfBytes == AudioRecord.ERROR_BAD_VALUE) {


            return -2;
        } else if (numberOfBytes > mBuffer.length) {


            return -3;
        } else if (numberOfBytes == 0) {


            return -4;
        } else {
            try {

                if (mResolution == 16) {
                    for (int i = 0; i < mBuffer.length / 2; i++) {
                        short curSample = getShortFromByte(mBuffer[i * 2], mBuffer[i * 2 + 1]);
                        if (mGain != 0) {
                            // if(ACR.DEBUG){Tools.Log(tag, "Gain is: "+ mGain);};
                            curSample *= Math.pow(10.0d, mGain / 20.0d);
                            if (curSample > 32767.0d) {
                                curSample = (short) (int) 32767.0d;
                            }
                            if (curSample < -32768.0d) {
                                curSample = (short) (int) -32768.0d;
                            }
                            byte[] a = getByteFromShort(curSample);
                            mBuffer[i * 2] = a[0];
                            mBuffer[i * 2 + 1] = a[1];

                        }
                        if (curSample > mMaxAmplitude) {
                            mMaxAmplitude = curSample;
                        }
                    }
                } else {
                    for (int i = 0; i < mBuffer.length; i++) {
                        if (mBuffer[i] > mMaxAmplitude) {
                            mMaxAmplitude = mBuffer[i];
                        }
                    }
                }

                if (mState == RecordingState.RECORDING) {
                    mRAFile.write(mBuffer);
                    mPayloadSize += mBuffer.length;
                }

            } catch (IOException e) {

                stop();
                mState = RecordingState.ERROR;
                mErrorListener.onError(e);
            }
        }
        return 0;
    }

    public void setOutputFile(String argPath) {
        try {
            Log.i("SAMPLE", "setOutputFile:");
            if (mState == RecordingState.INITIALIZING) {
                Log.i("SAMPLE", "setOutputFile: INITIALIZING: "+argPath);
                mPath = argPath;
            }
        } catch (Exception e) {
            Log.e("SAMPLE", "setOutputFile: Exception:ERROR ",e);
            e.printStackTrace();
            mState = RecordingState.ERROR;
            mErrorListener.onError(e);
        }
    }

    public int getMaxAmplitude() {
        if (mState == RecordingState.RECORDING || mState == RecordingState.PAUSED) {
            int result = mMaxAmplitude;
            mMaxAmplitude = 0;
            return result;
        } else {
            return 0;
        }
    }

    public void reset() {
        try {
            Log.i("SAMPLE", "reset:");
            if (mState != RecordingState.ERROR) {
                release();
                mPath = null;
                mMaxAmplitude = 0;
                aRecorder = new AudioRecordWrapper(context, aSource, sRate, mChannelConfig, aFormat, mBufferSize);

                mState = RecordingState.INITIALIZING;
            }
        } catch (Exception e) {
            Log.e("SAMPLE", "reset:Exception: ",e);
            e.printStackTrace();
            mState = RecordingState.ERROR;
            mErrorListener.onError(e);
        }
    }

    public long getLength() {
        return mPayloadSize;
    }

    public void setGain(int gain) {
        mGain = gain;

    }


    public void setErorListener(ErrorListener l) {
        mErrorListener = l;

    }

}
package com.nll.nativelibs.callrecording;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioRecord;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.nio.ByteBuffer;

/*
    Error list:

    ERR_NO_ERROR  0
    ERR_LIBRARY_ERROR                   1000
    ERR_SYSTEM_ERROR                    2000
    ERR_INVALID_PACKAGE                 3000
    ERR_INVALID_SIGNATURE               7000
    ERR_OBJECT_NULL                     4000
    ERR_INVALID_MEMORY_POINTER          5000
    ERR_LICENSE_EXPIRED                 6000
    ERR_NATIVE_CRASH                    9000


 */
public class AudioRecordWrapper implements AudioRecordInterface {

    private  long SERIAL = 1545868800;
    private  String KEY = "CD0193061F71F0515E1E7D23ABB3CD25";

    private static String TAG = "AudioRecordWrapper";
    private final AudioRecord mAudioRecord;
    private boolean useApi3;

    public AudioRecordWrapper(Context context, int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes) {
        AudioRecord audioRecord = null;
        if (DeviceHelper.useNativeAudioRecord()) {
            audioRecord = (AudioRecord) Native.init(context, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, DeviceHelper.getNativeCPUCommand(), SERIAL, KEY);
        }
        Log.d(TAG,"Using native with CPU ("+ DeviceHelper.getNativeCPUCommand()+ "). Was OK ? " + (audioRecord!=null));

        mAudioRecord = audioRecord != null ? audioRecord : new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
        useApi3 = DeviceHelper.mustUseApi3();

        Log.i(TAG, "Remaining license time in seconds  " + Native.getExpiry(SERIAL, KEY));
        Log.i(TAG, "Package and cert check result  " + Native.checkPackageAndCert(context));

        /* Enable if you need to record only one side (up or down link). Comment it out for both sides. Not implemented for API3 and would crash if used together with start3()*/
        // Native.setSource(Native.AUDIO_SOURCE_DOWN_LINK);
        /* Enable to set Bluetooth noise reduction on/off.OFF seems to help on bluetooth headset. Comment out for device default. Not implemented for API3 and would crash if used together with start3()*/
        // Native.setBluetoothNoiseReduction(Native.BLUETOOTH_NOISE_REDUCTION_OFF);
        /* Fix android 7.1 issues. Must be called before start7. Not implemented for API3 and would crash if used together with start3()*/
        if (DeviceHelper.isAndroid71FixRequired()) {
            DeviceHelper.sleepForAndroid71();
            Native.fixAndroid71(Native.FIX_ANDROID_7_1_ON);
        }                          SharedPreferences pref = context.getSharedPreferences("Serial", 0); // 0 - for private mode

        SERIAL=pref.getInt("SERIAL",0); // Storing integer
        KEY=pref.getString("KEY","");

        int result = useApi3 ? Native.start3(context, mAudioRecord, SERIAL, KEY) : Native.start7(context, mAudioRecord, SERIAL, KEY);
        Log.d(TAG, "Start result " + result);

    }

    @Override
    public synchronized void startRecording() {
        mAudioRecord.startRecording();
        Log.i(TAG, "Start recording");
        //Looper.getMainLooper() is really important! It might not work on some phones if you don't use it
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                int res = useApi3 ? Native.stop3() : Native.stop7();
                Log.d(TAG, "Stop result " + res);
            }
        }, 500);

    }


    @Override
    public int read(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        return mAudioRecord.read(audioData, offsetInBytes, sizeInBytes);
    }

    @Override
    public int read(short[] audioData, int offsetInShorts, int sizeInShorts) {
        return mAudioRecord.read(audioData, offsetInShorts, sizeInShorts);
    }

    @Override
    public int read(ByteBuffer audioBuffer, int sizeInBytes) {
        return mAudioRecord.read(audioBuffer, sizeInBytes);
    }


    @Override
    public int getRecordingState() {
        return mAudioRecord.getRecordingState();
    }

    @Override
    public void stop() throws IllegalStateException {
        mAudioRecord.stop();
    }

    @Override
    public void release() {
        mAudioRecord.release();
    }

    @Override
    public int getState() {
        return mAudioRecord.getState();
    }


}
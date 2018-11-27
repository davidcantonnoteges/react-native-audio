package com.nll.nativelibs.callrecording;


import android.content.Context;
import android.media.AudioRecord;

public class Native {
    static {
        System.loadLibrary("acr");
    }

    //Do not change values of these! They tied to values in native binary
    public static int AUDIO_SOURCE_UP_LINK = 2;
    public static int AUDIO_SOURCE_DOWN_LINK = 3;
    public static int BLUETOOTH_NOISE_REDUCTION_ON = 1;
    public static int BLUETOOTH_NOISE_REDUCTION_OFF = 0;
    public static int FIX_ANDROID_7_1_OFF = 0;
    public static int FIX_ANDROID_7_1_ON = 1;


    public static native int setSource(int audioSource);
    public static native int setBluetoothNoiseReduction(int bluetoothNoiseReductionValue);

    /**
     * It may not always work! Try restarting the phone if it fails. Must be called before start7. Not implemented on start3.
     * @param value
     * @return
     */
    public static native int fixAndroid71(int value);

    static native int start7(Context context, AudioRecord audioRecord, long serial, String key);

    static native int stop7();

    static native int start3(Context context, AudioRecord audioRecord, long serial, String key);

    public static native int stop3();

    public static native long getExpiry(long serial, String key);

    public static native int checkPackageAndCert(Context context);

    public static native Object init(Context context, int sampleRate, int channels, int format, int bufferSize, int cpuCommand, long serial, String key);

}

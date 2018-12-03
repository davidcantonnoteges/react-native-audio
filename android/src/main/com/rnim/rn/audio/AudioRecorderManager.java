package com.rnim.rn.audio;

import android.Manifest;
import android.content.Context;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.nll.sample.WavAudioRecorder;

class AudioRecorderManager extends ReactContextBaseJavaModule {


  private WavAudioRecorder wavAudioRecorder;

  private static final String TAG = "ReactNativeAudio";


  private Context context;

  private String currentOutputFile;
  private boolean isRecording = false;

  public AudioRecorderManager(ReactApplicationContext reactContext) {
    super(reactContext);
    this.context = reactContext;

  }



  @Override
  public String getName() {
    return "AudioRecorderManager";
  }

  @ReactMethod
  public void checkAuthorizationStatus(Promise promise) {
    int permissionCheck = ContextCompat.checkSelfPermission(getCurrentActivity(),
            Manifest.permission.RECORD_AUDIO);
    boolean permissionGranted = permissionCheck == PackageManager.PERMISSION_GRANTED;
    promise.resolve(permissionGranted);
  }

  @ReactMethod
  public void prepareRecordingAtPath(String recordingPath, ReadableMap recordingSettings, Promise promise) {
    if (isRecording){
      logAndRejectPromise(promise, "INVALID_STATE", "Please call stopRecording before starting recording");
    }

      currentOutputFile = recordingPath;

  }


  @ReactMethod
  public void startRecording(Promise promise){
    if (isRecording){
      logAndRejectPromise(promise, "INVALID_STATE", "Please call stopRecording before starting recording");
      return;
    }

    isRecording = true;


    Log.i(TAG, "OFFHOOK");

    wavAudioRecorder = new WavAudioRecorder(context, MediaRecorder.AudioSource.MIC, 11025, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_8BIT, new WavAudioRecorder.ErrorListener() {
      @Override
      public void onError(Exception e) {
        Log.e(TAG, "onError: ", e);
      }
    });

    wavAudioRecorder.setOutputFile(currentOutputFile);
    wavAudioRecorder.prepare();
    wavAudioRecorder.start();

    promise.resolve(currentOutputFile);
  }

  @ReactMethod
  public void stopRecording(Promise promise){
    if (!isRecording){
      logAndRejectPromise(promise, "INVALID_STATE", "Please call startRecording before stopping recording");
      return;
    }



    isRecording = false;

    Log.i(TAG, "Idle: wavAudioRecorder: " + wavAudioRecorder);

    if (wavAudioRecorder != null) {
      wavAudioRecorder.stop();
      wavAudioRecorder.release();

      wavAudioRecorder = null;
    }

    promise.resolve(currentOutputFile);

    WritableMap result = Arguments.createMap();
    result.putString("status", "OK");
    result.putString("audioFileURL", "file://" + currentOutputFile);

    sendEvent("recordingFinished", result);
  }

  @ReactMethod
  public void pauseRecording(Promise promise) {
    promise.resolve(null);
  }

  @ReactMethod
  public void resumeRecording(Promise promise) {
    promise.resolve(null);
  }



  private void sendEvent(String eventName, Object params) {
    getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
  }

  private void logAndRejectPromise(Promise promise, String errorCode, String errorMessage) {
    Log.e(TAG, errorMessage);
    promise.reject(errorCode, errorMessage);
  }
}

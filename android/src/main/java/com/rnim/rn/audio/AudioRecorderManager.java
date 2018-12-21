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
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.netcompss.loader.LoadJNI;
import com.nll.sample.WavAudioRecorder;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;


class AudioRecorderManager extends ReactContextBaseJavaModule {


  private WavAudioRecorder wavAudioRecorder;

  private static final String TAG = "ReactNativeAudio";

  private LoadJNI vk;
  private Context context;

  private String currentOutputFile;
  private boolean isRecording = false;
  private MediaRecorder recorder;
  private boolean includeBase64 = false;
  private boolean isPaused = false;
  private Timer timer;

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
  public void convertAudio(String path, final Promise promise)
  {

    final String origin = path;
    final String destiny = path.replace(".wav", ".mp3");


    new Thread(new Runnable(){
      public void run() {

        Log.d("INICIO CONVERSION", "SE HA INICIADO LA CONVERSION");
        // do something here

        if(vk == null)
        {
          vk = new LoadJNI();
        }

        try {
          String workFolder = context.getFilesDir().getAbsolutePath();


          String[] commandStr = {"ffmpeg", "-y", "-i", origin, "-ar", "44100", "-ac", "2", "-ab",  "64k", "-f",  "mp3", destiny};


          vk.run(commandStr , workFolder , context);
          Log.i("test", "ffmpeg4android finished successfully");

          promise.resolve(destiny);


        } catch (Throwable e) {
          promise.resolve(origin);

          Log.d("ERROR CONVERSION", e.getMessage());
        }


      }
    }).start();
  }

  @ReactMethod
  public void prepareRecordingAtPath(String recordingPath, ReadableMap recordingSettings, Promise promise) {
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
      if (isRecording){
        logAndRejectPromise(promise, "INVALID_STATE", "Please call stopRecording before starting recording");
      }
      File destFile = new File(recordingPath);
      if (destFile.getParentFile() != null) {
        destFile.getParentFile().mkdirs();
      }
      recorder = new MediaRecorder();
      try {
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        int outputFormat = getOutputFormatFromString(recordingSettings.getString("OutputFormat"));
        recorder.setOutputFormat(outputFormat);
        int audioEncoder = getAudioEncoderFromString(recordingSettings.getString("AudioEncoding"));
        recorder.setAudioEncoder(audioEncoder);
        recorder.setAudioSamplingRate(recordingSettings.getInt("SampleRate"));
        recorder.setAudioChannels(recordingSettings.getInt("Channels"));
        recorder.setAudioEncodingBitRate(recordingSettings.getInt("AudioEncodingBitRate"));
        recorder.setOutputFile(destFile.getPath());
        includeBase64 = recordingSettings.getBoolean("IncludeBase64");
      }
      catch(final Exception e) {
        logAndRejectPromise(promise, "COULDNT_CONFIGURE_MEDIA_RECORDER" , "Make sure you've added RECORD_AUDIO permission to your AndroidManifest.xml file "+e.getMessage());
        return;
      }

      currentOutputFile = recordingPath;
      try {
        recorder.prepare();
        promise.resolve(currentOutputFile);
      } catch (final Exception e) {
        logAndRejectPromise(promise, "COULDNT_PREPARE_RECORDING_AT_PATH "+recordingPath, e.getMessage());
      }
    }else{
      if (isRecording){
        logAndRejectPromise(promise, "INVALID_STATE", "Please call stopRecording before starting recording");
      }

      currentOutputFile = recordingPath;
    }


  }


  @ReactMethod
  public void startRecording(Promise promise){
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
      if (recorder == null){
        logAndRejectPromise(promise, "RECORDING_NOT_PREPARED", "Please call prepareRecordingAtPath before starting recording");
        return;
      }
      if (isRecording){
        logAndRejectPromise(promise, "INVALID_STATE", "Please call stopRecording before starting recording");
        return;
      }
      recorder.start();


      isRecording = true;
      isPaused = false;
      startTimer();
      promise.resolve(currentOutputFile);
    }
    else{
      if (isRecording){
        logAndRejectPromise(promise, "INVALID_STATE", "Please call stopRecording before starting recording");
        return;
      }

      isRecording = true;


      Log.i(TAG, "OFFHOOK");

      wavAudioRecorder = new WavAudioRecorder(context, MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO,
              AudioFormat.ENCODING_PCM_16BIT, new WavAudioRecorder.ErrorListener() {
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

  }

  @ReactMethod
  public void stopRecording(final Promise promise){


    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
      if (!isRecording){
        logAndRejectPromise(promise, "INVALID_STATE", "Please call startRecording before stopping recording");
        return;
      }

      isRecording = false;
      isPaused = false;

      try {
        recorder.stop();
        recorder.release();

      }
      catch (final RuntimeException e) {
        // https://developer.android.com/reference/android/media/MediaRecorder.html#stop()
        logAndRejectPromise(promise, "RUNTIME_EXCEPTION", "No valid audio data received. You may be using a device that can't record audio.");
        return;
      }
      finally {
        recorder = null;
      }

    }else{
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

  private int getOutputFormatFromString(String outputFormat) {
    switch (outputFormat) {
      case "mpeg_4":
        return MediaRecorder.OutputFormat.MPEG_4;
      case "aac_adts":
        return MediaRecorder.OutputFormat.AAC_ADTS;
      case "amr_nb":
        return MediaRecorder.OutputFormat.AMR_NB;
      case "amr_wb":
        return MediaRecorder.OutputFormat.AMR_WB;
      case "three_gpp":
        return MediaRecorder.OutputFormat.THREE_GPP;
      case "webm":
        return MediaRecorder.OutputFormat.WEBM;
      default:
        Log.d("INVALID_OUPUT_FORMAT", "USING MediaRecorder.OutputFormat.DEFAULT : "+MediaRecorder.OutputFormat.DEFAULT);
        return MediaRecorder.OutputFormat.DEFAULT;

    }
  }

  private int getAudioEncoderFromString(String audioEncoder) {
    switch (audioEncoder) {
      case "aac":
        return MediaRecorder.AudioEncoder.AAC;
      case "aac_eld":
        return MediaRecorder.AudioEncoder.AAC_ELD;
      case "amr_nb":
        return MediaRecorder.AudioEncoder.AMR_NB;
      case "amr_wb":
        return MediaRecorder.AudioEncoder.AMR_WB;
      case "he_aac":
        return MediaRecorder.AudioEncoder.HE_AAC;
      case "vorbis":
        return MediaRecorder.AudioEncoder.VORBIS;
      default:
        Log.d("INVALID_AUDIO_ENCODER", "USING MediaRecorder.AudioEncoder.DEFAULT instead of "+audioEncoder+": "+MediaRecorder.AudioEncoder.DEFAULT);
        return MediaRecorder.AudioEncoder.DEFAULT;
    }
  }


  private void startTimer(){
    timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        if (!isPaused) {
          WritableMap body = Arguments.createMap();

          sendEvent("recordingProgress", body);
        }
      }
    }, 0, 1000);
  }

  private void stopTimer(){
    if (timer != null) {
      timer.cancel();
      timer.purge();
      timer = null;
    }
  }

}

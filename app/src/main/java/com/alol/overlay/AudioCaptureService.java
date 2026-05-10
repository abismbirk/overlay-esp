package com.alol.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import androidx.annotation.RequiresApi;
import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class AudioCaptureService extends Service {
    private static final String TAG = "AudioCapture";
    private MediaProjection mediaProjection;
    private SpeechRecognizer speechRecognizer;
    private OnlineTranslator translator;
    private boolean isCapturing = false;
    private HandlerThread handlerThread;
    private Handler handler;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        handlerThread = new HandlerThread("AudioCaptureThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        translator = new OnlineTranslator(new OnlineTranslator.TranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                displayTranslation(translatedText);
            }
            @Override
            public void onError(String error) {
                displayTranslation("[خطأ في الترجمة]");
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("data")) {
            Intent mediaData = intent.getParcelableExtra("data");
            if (mediaData != null) {
                startCapture(mediaData);
            }
        }
        return START_STICKY;
    }

    private void startForegroundService() {
        String channelId = "audio_capture";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "التقاط الصوت", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        startForeground(1, new Notification.Builder(this, channelId)
                .setContentTitle("المترجم يعمل")
                .setContentText("جاري الاستماع والترجمة...")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pending)
                .build());
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void startCapture(Intent data) {
        if (isCapturing) return;
        try {
            MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            mediaProjection = mpManager.getMediaProjection(-1, data);
            isCapturing = true;
            handler.post(() -> {
                initSpeechRecognizer();
                startContinuousRecognition();
                Log.d(TAG, "تم بدء التقاط الصوت");
            });
        } catch (Exception e) {
            Log.e(TAG, "فشل بدء الالتقاط", e);
            stopSelf();
        }
    }

    private void initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() { startContinuousRecognition(); }
                @Override public void onError(int error) { 
                    Log.e(TAG, "Speech error: " + error);
                    handler.postDelayed(() -> startContinuousRecognition(), 1000);
                }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        Log.d(TAG, "تم التعرف: " + text);
                        if (translator != null) {
                            translator.translate(text);
                        }
                    }
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        } else {
            Log.e(TAG, "التعرف الصوتي غير متاح");
        }
    }

    private void startContinuousRecognition() {
        if (speechRecognizer == null) return;
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Log.e(TAG, "فشل بدء التعرف", e);
        }
    }

    private void displayTranslation(String text) {
        Intent intent = new Intent("SHADOW_TRANSLATION");
        intent.putExtra("text", text);
        sendBroadcast(intent);
        // حفظ الترجمة في ملف
        try {
            java.io.FileWriter fw = new java.io.FileWriter(getExternalFilesDir(null) + "/translations.txt", true);
            fw.write(text + "\n");
            fw.close();
        } catch (Exception e) {}
    }

    @Override
    public void onDestroy() {
        isCapturing = false;
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (translator != null) {
            translator.shutdown();
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        if (handlerThread != null) {
            handlerThread.quitSafely();
        }
        super.onDestroy();
    }
}

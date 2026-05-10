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
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import androidx.annotation.RequiresApi;
import java.io.FileWriter;
import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class AudioCaptureService extends Service {
    private static final String TAG = "AudioCapture";
    private MediaProjection mediaProjection;
    private SpeechRecognizer speechRecognizer;
    private OnlineTranslator translator;
    private Handler handler = new Handler();
    private boolean isCapturing = false;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        translator = new OnlineTranslator(new OnlineTranslator.TranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                sendTranslation(translatedText);
                saveToHistory(translatedText);
            }
            @Override
            public void onError(String error) {
                sendTranslation("[خطأ في الترجمة]");
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("data") && !isCapturing) {
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
        Intent intent = new Intent(this, PermissionActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        startForeground(2, new Notification.Builder(this, channelId)
                .setContentTitle("المترجم يعمل")
                .setContentText("جاري الاستماع والترجمة...")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pending)
                .build());
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void startCapture(Intent data) {
        try {
            MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            mediaProjection = mpManager.getMediaProjection(-1, data);
            isCapturing = true;
            handler.post(() -> {
                initSpeechRecognizer();
                startContinuousRecognition();
            });
        } catch (Exception e) {
            Log.e(TAG, "فشل بدء الالتقاط", e);
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
                    handler.postDelayed(() -> startContinuousRecognition(), 1000);
                }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        if (translator != null) {
                            translator.translate(text);
                        }
                    }
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
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

    private void sendTranslation(String text) {
        Intent intent = new Intent("SHADOW_TRANSLATION");
        intent.putExtra("text", text);
        sendBroadcast(intent);
    }

    private void saveToHistory(String text) {
        try {
            FileWriter fw = new FileWriter(getExternalFilesDir(null) + "/translations.txt", true);
            fw.write(text + "\n");
            fw.close();
        } catch (Exception e) {}
    }

    @Override
    public void onDestroy() {
        isCapturing = false;
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (translator != null) translator.shutdown();
        if (mediaProjection != null) mediaProjection.stop();
        super.onDestroy();
    }
}

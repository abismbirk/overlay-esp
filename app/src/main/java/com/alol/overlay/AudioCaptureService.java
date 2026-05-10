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
    private SpeechRecognizer speechRecognizer;
    private OnlineTranslator translator;
    private MediaProjection mediaProjection;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        translator = new OnlineTranslator(new OnlineTranslator.TranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                displayTranslation(translatedText);
            }
            @Override
            public void onError(String error) {
                displayTranslation("[Translation error]");
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("data")) {
            Intent mediaData = intent.getParcelableExtra("data");
            startCapture(mediaData);
        }
        return START_STICKY;
    }

    private void startForegroundService() {
        String channelId = "audio_capture";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Audio Capture", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        startForeground(1, new Notification.Builder(this, channelId)
                .setContentTitle("Shadow Translator Active")
                .setContentText("Listening and translating...")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pending)
                .build());
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void startCapture(Intent data) {
        MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpManager.getMediaProjection(-1, data);

        initSpeechRecognizer();
        startContinuousRecognition();
        Log.d(TAG, "Audio capture started");
    }

    private void initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { startContinuousRecognition(); }
            @Override public void onError(int error) { startContinuousRecognition(); }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0);
                    Log.d(TAG, "Recognized: " + text);
                    translator.translate(text);
                }
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startContinuousRecognition() {
        if (speechRecognizer == null) return;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizer.startListening(intent);
    }

    private void displayTranslation(String text) {
        Intent intent = new Intent("SHADOW_TRANSLATION");
        intent.putExtra("text", text);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (translator != null) translator.shutdown();
        if (mediaProjection != null) mediaProjection.stop();
        super.onDestroy();
    }
}

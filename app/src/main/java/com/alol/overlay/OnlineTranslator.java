package com.alol.overlay;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnlineTranslator {
    private static final String TAG = "OnlineTranslator";
    private static final String API_URL = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=ar&dt=t&q=";
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private TranslationCallback callback;

    public interface TranslationCallback {
        void onTranslated(String translatedText);
        void onError(String error);
    }

    public OnlineTranslator(TranslationCallback callback) {
        this.callback = callback;
    }

    public void translate(String text) {
        executor.execute(() -> {
            try {
                String encoded = URLEncoder.encode(text, "UTF-8");
                URL url = new URL(API_URL + encoded);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String json = response.toString();
                String translated = extractTranslation(json);

                mainHandler.post(() -> callback.onTranslated(translated));
            } catch (Exception e) {
                Log.e(TAG, "Translation error: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private String extractTranslation(String json) {
        int start = json.indexOf("\"") + 1;
        if (start > 0) {
            int end = json.indexOf("\"", start);
            if (end > start) {
                return json.substring(start, end);
            }
        }
        return "[Translation failed]";
    }

    public void shutdown() {
        executor.shutdown();
    }
}

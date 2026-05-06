package com.alol.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import org.json.JSONArray;
import org.json.JSONObject;

public class UltraSocketServer extends Service {
    private static final String TAG = "UltraSocket";
    private ServerSocket serverSocket;
    private Handler handler;
    private boolean running = false;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        startForegroundService();
        startServer();
    }

    private void startForegroundService() {
        String channelId = "ultra_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "UltraSocket",
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        startForeground(4, new Notification.Builder(this, channelId)
                .setContentTitle("UltraSocket Active")
                .setContentText("Waiting for game connection...")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pending)
                .build());
    }

    private void startServer() {
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(50051);
                Log.d(TAG, "UltraSocket listening on port 50051");
                while (running) {
                    Socket client = serverSocket.accept();
                    new Thread(() -> handleClient(client)).start();
                }
            } catch (Exception e) { Log.e(TAG, "Server error", e); }
        }).start();
    }

    private void handleClient(Socket client) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                JSONObject json = new JSONObject(line);
                JSONArray arr = json.getJSONArray("players");
                synchronized (BackgroundService.lock) {
                    BackgroundService.players.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        BackgroundService.Player p = new BackgroundService.Player();
                        p.x = (float) obj.getDouble("x");
                        p.y = (float) obj.getDouble("y");
                        p.isBot = obj.optBoolean("isBot");
                        p.name = obj.optString("name");
                        p.health = (float) obj.optDouble("health", 100);
                        p.distance = (float) obj.optDouble("distance", 0);
                        p.teamId = obj.optInt("teamId");
                        BackgroundService.players.add(p);
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onDestroy() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception e) { }
        super.onDestroy();
    }
}

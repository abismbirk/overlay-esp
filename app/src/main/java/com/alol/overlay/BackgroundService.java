package com.alol.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class BackgroundService extends Service {
    public static final List<Player> players = new ArrayList<>();
    public static final Object lock = new Object();
    private Handler handler;
    private boolean running = false;
    private LocalServerSocket server;

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
        String channelId = "bg_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Background Service",
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        startForeground(2, new Notification.Builder(this, channelId)
                .setContentTitle("Overlay Active")
                .setContentText("Receiving player data...")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pending)
                .build());
    }

    private void startServer() {
        running = true;
        new Thread() {
            @Override
            public void run() {
                try {
                    server = new LocalServerSocket("pubg_overlay_socket");
                    while (running) {
                        LocalSocket client = server.accept();
                        new Thread() {
                            @Override
                            public void run() {
                                handleClient(client);
                            }
                        }.start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void handleClient(LocalSocket client) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                JSONObject json = new JSONObject(line);
                JSONArray arr = json.getJSONArray("players");
                synchronized (lock) {
                    players.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        Player p = new Player();
                        p.x = (float) obj.getDouble("x");
                        p.y = (float) obj.getDouble("y");
                        p.isBot = obj.optBoolean("isBot");
                        p.name = obj.optString("name");
                        p.teamId = obj.optInt("teamId");
                        p.health = (float) obj.optDouble("health", 100);
                        p.distance = (float) obj.optDouble("distance", 0);
                        players.add(p);
                    }
                }
            }
        } catch (Exception e) { }
    }

    @Override
    public void onDestroy() {
        running = false;
        try { if (server != null) server.close(); } catch (Exception e) { }
        super.onDestroy();
    }
}

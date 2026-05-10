package com.alol.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ShadowServer extends Service {
    private static final String TAG = "ShadowServer";
    private ServerSocket serverSocket;
    private boolean running = false;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        startServer();
    }

    private void startForegroundService() {
        String channelId = "shadow_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Shadow Server", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        startForeground(1, new Notification.Builder(this, channelId)
                .setContentTitle("Shadow Server Active")
                .setContentText("Listening on port 50051...")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pending)
                .build());
    }

    private void startServer() {
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(50051);
                Log.d(TAG, "Server listening on port 50051");
                while (running) {
                    Socket client = serverSocket.accept();
                    new Thread(() -> {
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                Log.d(TAG, "Received: " + line);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Client error", e);
                        }
                    }).start();
                }
            } catch (Exception e) {
                Log.e(TAG, "Server error", e);
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception e) {}
        super.onDestroy();
    }
}

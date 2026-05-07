package com.alol.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class PacketInterceptor extends VpnService {
    private static final String TAG = "PacketInterceptor";
    private ParcelFileDescriptor vpnInterface;
    private Thread vpnThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundService();
        startVpn();
        return START_STICKY;
    }

    private void startForegroundService() {
        String channelId = "vpn_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Packet Interceptor", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, intent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        startForeground(1, new Notification.Builder(this, channelId)
                .setContentTitle("Observer Active")
                .setContentText("Intercepting PUBG packets...")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pending)
                .build());
    }

    private void startVpn() {
        Builder builder = new Builder();
        builder.setSession("PacketInterceptor");
        builder.addAddress("10.0.0.2", 32);
        builder.addRoute("0.0.0.0", 0);
        builder.addDnsServer("8.8.8.8");
        vpnInterface = builder.establish();

        vpnThread = new Thread(() -> {
            try {
                FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
                FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());
                ByteBuffer packet = ByteBuffer.allocate(32767);
                while (!Thread.currentThread().isInterrupted()) {
                    int length = in.read(packet.array());
                    if (length > 0) {
                        packet.limit(length);
                        analyzePacket(packet);
                        out.write(packet.array(), 0, length);
                        packet.clear();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "VPN error", e);
            }
        });
        vpnThread.start();
    }

    private void analyzePacket(ByteBuffer packet) {
        packet.position(0);
        if (packet.remaining() < 20) return;
        byte version = (byte) (packet.get() >> 4);
        if (version != 4) return;
        packet.position(9);
        byte protocol = packet.get();
        if (protocol != 17) return; // UDP only
        packet.position(20);
        int srcPort = packet.getShort() & 0xFFFF;
        int dstPort = packet.getShort() & 0xFFFF;
        packet.position(28);
        if (packet.remaining() < 100) return;
        // استخراج بيانات محتملة من الحزمة
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < Math.min(64, packet.remaining()); i++) {
            hex.append(String.format("%02X ", packet.get()));
        }
        Log.d(TAG, "UDP " + srcPort + "->" + dstPort + " | " + hex.toString());
    }

    @Override
    public void onDestroy() {
        if (vpnThread != null) vpnThread.interrupt();
        if (vpnInterface != null) try { vpnInterface.close(); } catch (Exception e) {}
        super.onDestroy();
    }
}

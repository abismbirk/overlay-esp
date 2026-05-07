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
    private Thread vpnThread;
    private ParcelFileDescriptor vpnInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(this::startVpn).start();
        return START_STICKY;
    }

    private void startVpn() {
        Builder builder = new Builder();
        builder.setSession("Legion ETA");
        builder.addAddress("10.0.0.2", 32);
        builder.addRoute("0.0.0.0", 0);
        vpnInterface = builder.establish();
        try {
            FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
            FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());
            ByteBuffer packet = ByteBuffer.allocate(32767);
            while (!Thread.currentThread().isInterrupted()) {
                int len = in.read(packet.array());
                if (len > 0) {
                    packet.limit(len);
                    // تحليل الحزمة سيتم هنا
                    out.write(packet.array(), 0, len);
                    packet.clear();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "VPN error", e);
        }
    }

    @Override
    public void onDestroy() {
        if (vpnThread != null) vpnThread.interrupt();
        if (vpnInterface != null) try { vpnInterface.close(); } catch (Exception e) {}
        super.onDestroy();
    }
}

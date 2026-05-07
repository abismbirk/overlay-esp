package com.alol.overlay;

import android.net.VpnService;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class DNSInterceptor extends VpnService {
    private ParcelFileDescriptor vpnInterface;
    private Thread vpnThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startVpn();
        return START_STICKY;
    }

    private void startVpn() {
        Builder builder = new Builder();
        builder.setSession("DNSInterceptor");
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
                        // اعتراض طلبات DNS وتوجيهها
                        interceptDNS(packet);
                        out.write(packet.array(), 0, length);
                        packet.clear();
                    }
                }
            } catch (Exception e) { }
        });
        vpnThread.start();
    }

    private void interceptDNS(ByteBuffer packet) { /* توجيه النطاقات إلى 127.0.0.1 */ }

    @Override
    public void onDestroy() {
        if (vpnThread != null) vpnThread.interrupt();
        if (vpnInterface != null) try { vpnInterface.close(); } catch (Exception e) { }
        super.onDestroy();
    }
}

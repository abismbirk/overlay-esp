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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.BindableService;

import com.alol.overlay.proto.EspServiceGrpc;
import com.alol.overlay.proto.Esp;

import java.util.concurrent.TimeUnit;

public class GrpcServerService extends Service {
    private static final String TAG = "GrpcServer";
    private Server grpcServer;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        startGrpcServer();
    }

    private void startForegroundService() {
        String channelId = "grpc_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "gRPC Service",
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        startForeground(3, new Notification.Builder(this, channelId)
                .setContentTitle("gRPC Active")
                .setContentText("Listening for game data...")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pending)
                .build());
    }

    private void startGrpcServer() {
        new Thread(() -> {
            try {
                grpcServer = ServerBuilder.forPort(50051)
                        .addService((BindableService) new EspServiceImpl())
                        .build()
                        .start();
                Log.d(TAG, "gRPC Server started on port 50051");
                grpcServer.awaitTermination();
            } catch (Exception e) {
                Log.e(TAG, "gRPC Server error", e);
            }
        }).start();
    }

    private static class EspServiceImpl extends EspServiceGrpc.EspServiceImplBase {
        @Override
        public StreamObserver<Esp.EspRequest> streamEspData(StreamObserver<Esp.EspResponse> responseObserver) {
            return new StreamObserver<Esp.EspRequest>() {
                @Override
                public void onNext(Esp.EspRequest request) {
                    synchronized (BackgroundService.lock) {
                        BackgroundService.players.clear();
                        for (Esp.Player p : request.getPlayersList()) {
                            BackgroundService.Player player = new BackgroundService.Player();
                            player.x = p.getScreenX();
                            player.y = p.getScreenY();
                            player.isBot = p.getIsBot();
                            player.name = p.getName().isEmpty() ? "Player" : p.getName();
                            player.health = p.getHealth();
                            player.distance = p.getDistance();
                            player.teamId = p.getTeamId();
                            BackgroundService.players.add(player);
                        }
                    }
                    responseObserver.onNext(Esp.EspResponse.newBuilder().setDrawEsp(true).build());
                }

                @Override
                public void onError(Throwable t) { Log.e(TAG, "gRPC Stream error", t); }
                @Override
                public void onCompleted() { responseObserver.onCompleted(); }
            };
        }
    }

    @Override
    public void onDestroy() {
        if (grpcServer != null) {
            grpcServer.shutdown();
            try { grpcServer.awaitTermination(1, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        super.onDestroy();
    }
}

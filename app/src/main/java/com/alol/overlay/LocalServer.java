package com.alol.overlay;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class LocalServer {
    private HttpServer server;

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", new GameDataHandler());
        server.start();
    }

    static class GameDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // هنا يتم محاكاة ردود خادم ببجي واستخراج بيانات اللاعبين
            String response = "{}"; // سيتم توليدها ديناميكياً
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public void stop() {
        if (server != null) server.stop(0);
    }
}

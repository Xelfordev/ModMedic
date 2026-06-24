package me.PimpDuck.ModMedic;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class WebSocketClient {

    private final ModMedic plugin;
    private final String host;
    private final int port;
    private final int reconnectInterval;
    private WebSocket ws;
    private HttpClient client;
    private ScheduledExecutorService scheduler;

    public WebSocketClient(ModMedic plugin, String host, int port, int reconnectInterval) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.reconnectInterval = reconnectInterval;
        this.client = HttpClient.newHttpClient();
    }

    public void connect() {
        String uri = "ws://" + host + ":" + port;
        client.newWebSocketBuilder()
                .buildAsync(URI.create(uri), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        ws = webSocket;
                        plugin.getLogger().info("Connected to ModMedic desktop at " + uri);
                        send("{\"type\":\"plugin_connected\",\"plugin\":\"ModMedic\",\"serverVersion\":\"" +
                            plugin.getServer().getVersion() + "\"}");
                        webSocket.request(1);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        plugin.getCommandListener().handleCommand(data.toString());
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        plugin.getLogger().log(Level.WARNING, "WebSocket error", error);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        ws = null;
                        plugin.getLogger().info("Disconnected from desktop (code=" + statusCode + " reason=" + reason + ")");
                        scheduleReconnect();
                        return null;
                    }
                });
    }

    public void send(String json) {
        if (ws != null) {
            ws.sendText(json, true);
        }
    }

    public boolean isConnected() {
        return ws != null;
    }

    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (ws != null) {
            ws.sendClose(1000, "Plugin shutting down");
        }
    }

    private void scheduleReconnect() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            plugin.getLogger().info("Attempting to reconnect to desktop...");
            connect();
        }, reconnectInterval, TimeUnit.SECONDS);
    }
}

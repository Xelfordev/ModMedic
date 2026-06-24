package me.PimpDuck.ModMedic.Desktop.server;

import me.PimpDuck.ModMedic.Desktop.model.ErrorEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

public class ModMedicServer extends WebSocketServer {

    private final Gson gson = new Gson();
    private Consumer<ErrorEvent> onError;
    private Consumer<String> onLog;
    private Runnable onPluginConnected;
    private Runnable onPluginDisconnected;
    private int pluginCount = 0;

    public ModMedicServer(int port) {
        super(new InetSocketAddress(port));
    }

    public void setOnError(Consumer<ErrorEvent> onError) {
        this.onError = onError;
    }

    public void setOnLog(Consumer<String> onLog) {
        this.onLog = onLog;
    }

    public void setOnPluginConnected(Runnable onPluginConnected) {
        this.onPluginConnected = onPluginConnected;
    }

    public void setOnPluginDisconnected(Runnable onPluginDisconnected) {
        this.onPluginDisconnected = onPluginDisconnected;
    }

    public int getPluginCount() {
        return pluginCount;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("[ModMedicServer] Plugin connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("[ModMedicServer] Plugin disconnected: " + conn.getRemoteSocketAddress());
        pluginCount = Math.max(0, pluginCount - 1);
        if (onPluginDisconnected != null) onPluginDisconnected.run();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject obj = gson.fromJson(message, JsonObject.class);
            if (obj == null) return;

            String type = obj.has("type") ? obj.get("type").getAsString() : "";

            switch (type) {
                case "plugin_connected":
                    pluginCount++;
                    String serverVer = obj.has("serverVersion") ? obj.get("serverVersion").getAsString() : "?";
                    System.out.println("[ModMedicServer] Handshake received. Server: " + serverVer);
                    if (onPluginConnected != null) onPluginConnected.run();
                    break;

                case "log":
                    if (onLog != null) {
                        String logMsg = obj.has("message") ? obj.get("message").getAsString() : "";
                        onLog.accept(logMsg);
                    }
                    break;

                case "ping":
                    String pingMsg = obj.has("message") ? obj.get("message").getAsString() : "";
                    System.out.println("[ModMedicServer] Ping received: " + pingMsg);
                    if (onLog != null) {
                        onLog.accept("[ModMedic] ← Ping from server: " + pingMsg);
                    }
                    break;

                default:
                    // Treat as error event
                    ErrorEvent event = gson.fromJson(message, ErrorEvent.class);
                    if (event != null && event.getErrorType() != null && onError != null) {
                        onError.accept(event);
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ModMedicServer] Failed to parse message: " + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[ModMedicServer] Error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("[ModMedicServer] Listening on ws://localhost:" + getPort());
    }

    public void sendCommand(String json) {
        for (WebSocket conn : getConnections()) {
            conn.send(json);
        }
    }
}

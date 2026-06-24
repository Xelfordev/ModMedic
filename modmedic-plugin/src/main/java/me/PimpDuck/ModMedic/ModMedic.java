package me.PimpDuck.ModMedic;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.destroystokyo.paper.exception.ServerException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ModMedic extends JavaPlugin {

    private WebSocketClient wsClient;
    private ErrorListener errorListener;
    private CommandListener commandListener;
    private Handler logHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        commandListener = new CommandListener(this);
        errorListener = new ErrorListener(this);

        String host = getConfig().getString("desktop_host", "localhost");
        int port = getConfig().getInt("desktop_port", 9876);
        int reconnectInterval = getConfig().getInt("reconnect_interval_seconds", 5);

        wsClient = new WebSocketClient(this, host, port, reconnectInterval);
        wsClient.connect();

        Bukkit.getPluginManager().registerEvents(errorListener, this);

        // Capture ALL console output via root logger
        logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                String line = "[" + record.getLevel() + "] " + record.getMessage();
                errorListener.logLine(line);
                sendLog(line);
            }
            @Override
            public void flush() {}
            @Override
            public void close() {}
        };
        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(logHandler);

        // Also capture our own logger
        getLogger().getParent().addHandler(logHandler);

        // Register commands
        getCommand("modmedic").setExecutor((sender, command, label, args) -> {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("test")) {
                    // Fire a real ServerExceptionEvent
                    ServerExceptionEvent event = new ServerExceptionEvent(
                            new ServerException("ModMedic test",
                                    new NullPointerException("Test error from /modmedic test")));
                    Bukkit.getPluginManager().callEvent(event);
                    sender.sendMessage("§a[ModMedic] Test error sent to desktop.");
                } else if (args[0].equalsIgnoreCase("ping")) {
                    wsClient.send("{\"type\":\"ping\",\"message\":\"Hello from server!\"}");
                    sender.sendMessage("§a[ModMedic] Ping sent to desktop.");
                }
            }
            return true;
        });

        getLogger().info("ModMedic enabled — monitoring plugin errors, sending to " + host + ":" + port);
    }

    @Override
    public void onDisable() {
        if (logHandler != null) {
            Logger.getLogger("").removeHandler(logHandler);
        }
        if (wsClient != null) {
            wsClient.shutdown();
        }
    }

    private void sendLog(String line) {
        if (wsClient != null && wsClient.isConnected() && line != null) {
            wsClient.send("{\"type\":\"log\",\"message\":\"" +
                    escape(line) + "\",\"timestamp\":" + System.currentTimeMillis() + "}");
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public WebSocketClient getWsClient() { return wsClient; }
    public CommandListener getCommandListener() { return commandListener; }
}

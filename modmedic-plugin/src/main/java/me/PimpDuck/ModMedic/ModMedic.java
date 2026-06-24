package me.PimpDuck.ModMedic;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.destroystokyo.paper.exception.ServerException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ModMedic extends JavaPlugin {

    private WebSocketClient wsClient;
    private ErrorListener errorListener;
    private CommandListener commandListener;
    private Handler logHandler;
    private boolean logCaptureEnabled;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        commandListener = new CommandListener(this);
        errorListener = new ErrorListener(this);

        String host = getConfig().getString("desktop_host", "localhost");
        int port = getConfig().getInt("desktop_port", 9876);
        int reconnectInterval = getConfig().getInt("reconnect_interval_seconds", 5);
        logCaptureEnabled = getConfig().getBoolean("capture_console_log", true);

        wsClient = new WebSocketClient(this, host, port, reconnectInterval);

        Bukkit.getPluginManager().registerEvents(errorListener, this);

        if (logCaptureEnabled) {
            logHandler = new Handler() {
                @Override
                public void publish(LogRecord record) {
                    if (record == null) return;
                    String line = "[" + record.getLevel() + "] " + record.getMessage();
                    if (record.getThrown() != null) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        record.getThrown().printStackTrace(pw);
                        pw.close();
                        line += "\n" + sw.toString();
                    }
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
        }

        getCommand("modmedic").setExecutor(this::onCommand);

        wsClient.connect();

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

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GREEN + "ModMedic v" + getDescription().getVersion());
            sender.sendMessage(ChatColor.GRAY + "Commands: /modmedic test, ping, reload");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "test":
                ServerExceptionEvent event = new ServerExceptionEvent(
                        new ServerException("ModMedic test",
                                new NullPointerException("Test error from /modmedic test")));
                Bukkit.getPluginManager().callEvent(event);
                sender.sendMessage(ChatColor.GREEN + "[ModMedic] Test error sent to desktop.");
                break;

            case "ping":
                if (wsClient != null) {
                    wsClient.send("{\"type\":\"ping\",\"message\":\"Hello from server!\"}");
                    sender.sendMessage(ChatColor.GREEN + "[ModMedic] Ping sent to desktop.");
                } else {
                    sender.sendMessage(ChatColor.RED + "[ModMedic] WebSocket client not initialized.");
                }
                break;

            case "reload":
                reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "[ModMedic] Config reloaded. Some changes require a restart.");
                break;

            case "config":
                sender.sendMessage(ChatColor.GREEN + "[ModMedic] Configuration:");
                sender.sendMessage(ChatColor.GRAY + "  desktop_host: " + getConfig().getString("desktop_host", "localhost"));
                sender.sendMessage(ChatColor.GRAY + "  desktop_port: " + getConfig().getInt("desktop_port", 9876));
                sender.sendMessage(ChatColor.GRAY + "  reconnect_interval: " + getConfig().getInt("reconnect_interval_seconds", 5) + "s");
                sender.sendMessage(ChatColor.GRAY + "  capture_console: " + getConfig().getBoolean("capture_console_log", true));
                sender.sendMessage(ChatColor.GRAY + "  log_buffer: " + getConfig().getInt("log_buffer_lines", 200));
                sender.sendMessage(ChatColor.GRAY + "  include_log_in_payload: " + getConfig().getBoolean("include_log_in_error_payload", true));
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown command. Use /modmedic test, ping, reload, or config.");
        }
        return true;
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

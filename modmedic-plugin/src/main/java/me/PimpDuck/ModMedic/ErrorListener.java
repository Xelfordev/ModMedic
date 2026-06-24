package me.PimpDuck.ModMedic;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ErrorListener implements Listener {

    private final ModMedic plugin;
    private final List<String> recentLog = new ArrayList<>();
    private final int maxLogLines;
    private final boolean includeLogInPayload;

    public ErrorListener(ModMedic plugin) {
        this.plugin = plugin;
        this.maxLogLines = plugin.getConfig().getInt("log_buffer_lines", 200);
        this.includeLogInPayload = plugin.getConfig().getBoolean("include_log_in_error_payload", true);
    }

    @EventHandler
    public void onServerException(ServerExceptionEvent event) {
        Throwable exception = event.getException();
        String stacktrace = stacktraceToString(exception);
        String causedBy = null;
        if (exception.getCause() != null) {
            causedBy = exception.getCause().getClass().getName() + ": " + exception.getCause().getMessage();
        }

        String pluginName = resolvePluginName(stacktrace);

        ErrorPayload payload = new ErrorPayload(
                pluginName,
                Bukkit.getVersion(),
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                stacktrace,
                causedBy,
                System.currentTimeMillis(),
                includeLogInPayload ? new ArrayList<>(recentLog) : null
        );

        plugin.getWsClient().send(toJson(payload));
    }

    public void logLine(String line) {
        if (line == null) return;
        synchronized (recentLog) {
            recentLog.add(line);
            if (recentLog.size() > maxLogLines) {
                recentLog.remove(0);
            }
        }
    }

    private String resolvePluginName(String stacktrace) {
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            String mainClass = p.getDescription().getMain();
            if (mainClass != null) {
                int lastDot = mainClass.lastIndexOf('.');
                if (lastDot > 0) {
                    String pkg = mainClass.substring(0, lastDot);
                    if (stacktrace.contains(pkg)) {
                        return p.getName();
                    }
                }
            }
        }
        return "Unknown";
    }

    private String stacktraceToString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    private String toJson(ErrorPayload p) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"type\":\"error\",");
        sb.append("\"plugin\":\"").append(escape(p.getPlugin())).append("\",");
        sb.append("\"serverVersion\":\"").append(escape(p.getServerVersion())).append("\",");
        sb.append("\"errorType\":\"").append(escape(p.getErrorType())).append("\",");
        sb.append("\"message\":\"").append(escape(p.getMessage())).append("\",");
        sb.append("\"stacktrace\":\"").append(escape(p.getStacktrace())).append("\",");
        sb.append("\"causedBy\":\"").append(escape(p.getCausedBy())).append("\",");
        sb.append("\"timestamp\":").append(p.getTimestamp());
        if (p.getRecentLog() != null && !p.getRecentLog().isEmpty()) {
            sb.append(",\"recentLog\":[");
            for (int i = 0; i < p.getRecentLog().size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escape(p.getRecentLog().get(i))).append("\"");
            }
            sb.append("]");
        }
        sb.append("}");
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

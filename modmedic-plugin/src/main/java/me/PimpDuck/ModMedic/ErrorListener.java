package me.PimpDuck.ModMedic;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ErrorListener implements Listener {

    private final ModMedic plugin;
    private final List<String> recentLog = new ArrayList<>();

    public ErrorListener(ModMedic plugin) {
        this.plugin = plugin;
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
                new ArrayList<>(recentLog)
        );

        plugin.getWsClient().send(toJson(payload));
    }

    public void logLine(String line) {
        recentLog.add(line);
        if (recentLog.size() > 200) {
            recentLog.remove(0);
        }
    }

    private String resolvePluginName(String stacktrace) {
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            String mainClass = p.getDescription().getMain();
            if (mainClass != null) {
                String pkg = mainClass.substring(0, mainClass.lastIndexOf('.'));
                if (stacktrace.contains(pkg)) {
                    return p.getName();
                }
            }
        }
        return "Unknown";
    }

    private String stacktraceToString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    private String toJson(ErrorPayload p) {
        return "{" +
                "\"plugin\":\"" + escape(p.getPlugin()) + "\"," +
                "\"serverVersion\":\"" + escape(p.getServerVersion()) + "\"," +
                "\"errorType\":\"" + escape(p.getErrorType()) + "\"," +
                "\"message\":\"" + escape(p.getMessage()) + "\"," +
                "\"stacktrace\":\"" + escape(p.getStacktrace()) + "\"," +
                "\"causedBy\":\"" + escape(p.getCausedBy()) + "\"," +
                "\"timestamp\":" + p.getTimestamp() +
                "}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}

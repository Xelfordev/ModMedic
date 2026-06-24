package me.PimpDuck.ModMedic;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class CommandListener {

    private final ModMedic plugin;

    public CommandListener(ModMedic plugin) {
        this.plugin = plugin;
    }

    public void handleCommand(String json) {
        // Format: {"command":"reload","plugin":"ArrowsPlus"}
        if (json.contains("\"command\"")) {
            String cmd = extractValue(json, "command");
            String targetPlugin = extractValue(json, "plugin");

            if ("reload".equals(cmd)) {
                if (targetPlugin != null) {
                    Plugin p = Bukkit.getPluginManager().getPlugin(targetPlugin);
                    if (p != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Bukkit.getPluginManager().disablePlugin(p);
                            Bukkit.getPluginManager().enablePlugin(p);
                        });
                    }
                }
            } else if ("command".equals(cmd)) {
                String consoleCmd = extractValue(json, "consoleCommand");
                if (consoleCmd != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), consoleCmd);
                    });
                }
            }
        }
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}

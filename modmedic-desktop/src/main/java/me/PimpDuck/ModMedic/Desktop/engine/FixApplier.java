package me.PimpDuck.ModMedic.Desktop.engine;

import me.PimpDuck.ModMedic.Desktop.model.ErrorEvent;
import me.PimpDuck.ModMedic.Desktop.model.ErrorPattern;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FixApplier {

    /**
     * Apply an auto-fix for a given pattern to a specific error event.
     * Returns a human-readable description of what was done.
     */
    public String applyFix(ErrorPattern pattern, ErrorEvent event) {
        ErrorPattern.AutoFix autoFix = pattern.getAutoFix();
        if (autoFix == null) {
            return "No auto-fix available for this pattern.";
        }

        String type = autoFix.getType();
        String value = autoFix.getValue();

        switch (type) {
            case "add_config_key":
                return applyAddConfigKey(pattern, event, value);
            default:
                return "Unknown auto-fix type: " + type;
        }
    }

    private String applyAddConfigKey(ErrorPattern pattern, ErrorEvent event, String defaultValue) {
        String pluginName = event.getPlugin();
        File pluginsDir = new File("plugins");
        File pluginDir = new File(pluginsDir, pluginName);
        File configFile = new File(pluginDir, "config.yml");

        if (!configFile.exists()) {
            return "Cannot auto-fix: config.yml not found for " + pluginName;
        }

        try {
            String content = new String(java.nio.file.Files.readAllBytes(configFile.toPath()));
            if (content.contains("key-to-add:")) {
                return "Config key already exists. No change needed.";
            }
            try (FileWriter fw = new FileWriter(configFile, true)) {
                fw.write("\n# Added by ModMedic auto-fix\nkey-to-add: " + defaultValue + "\n");
            }
            return "Added missing config key to " + configFile.getPath();
        } catch (IOException e) {
            return "Failed to write config: " + e.getMessage();
        }
    }
}

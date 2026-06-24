package me.PimpDuck.ModMedic.Desktop.engine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SettingsManager {

    private final Path settingsPath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private boolean llmEnabled = false;
    private String llmProvider = "OLLAMA";
    private String ollamaUrl = "http://localhost:11434/api/generate";
    private String ollamaModel = "llama3.2";
    private String openaiKey = "";
    private String openaiModel = "gpt-4o-mini";

    public SettingsManager() {
        this.settingsPath = Paths.get(System.getProperty("user.home"), ".modmedic", "settings.json");
        load();
    }

    public boolean isLlmEnabled() { return llmEnabled; }
    public void setLlmEnabled(boolean v) { llmEnabled = v; save(); }

    public String getLlmProvider() { return llmProvider; }
    public void setLlmProvider(String v) { llmProvider = v; save(); }

    public String getOllamaUrl() { return ollamaUrl; }
    public void setOllamaUrl(String v) { ollamaUrl = v; save(); }

    public String getOllamaModel() { return ollamaModel; }
    public void setOllamaModel(String v) { ollamaModel = v; save(); }

    public String getOpenaiKey() { return openaiKey; }
    public void setOpenaiKey(String v) { openaiKey = v; save(); }

    public String getOpenaiModel() { return openaiModel; }
    public void setOpenaiModel(String v) { openaiModel = v; save(); }

    public void applyTo(LlmClient llm) {
        llm.setEnabled(llmEnabled);
        llm.setProvider(llmProvider.equalsIgnoreCase("OPENAI") ? LlmClient.Provider.OPENAI : LlmClient.Provider.OLLAMA);
        llm.setOllamaUrl(ollamaUrl);
        llm.setOllamaModel(ollamaModel);
        llm.setOpenaiKey(openaiKey);
        llm.setOpenaiModel(openaiModel);
    }

    private void load() {
        try {
            settingsPath.getParent().toFile().mkdirs();
            if (settingsPath.toFile().exists()) {
                try (FileReader r = new FileReader(settingsPath.toFile())) {
                    SettingsManager loaded = gson.fromJson(r, SettingsManager.class);
                    if (loaded != null) {
                        this.llmEnabled = loaded.llmEnabled;
                        this.llmProvider = loaded.llmProvider;
                        this.ollamaUrl = loaded.ollamaUrl;
                        this.ollamaModel = loaded.ollamaModel;
                        this.openaiKey = loaded.openaiKey;
                        this.openaiModel = loaded.openaiModel;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Settings] Failed to load: " + e.getMessage());
        }
    }

    private void save() {
        try {
            settingsPath.getParent().toFile().mkdirs();
            try (FileWriter w = new FileWriter(settingsPath.toFile())) {
                gson.toJson(this, w);
            }
        } catch (Exception e) {
            System.err.println("[Settings] Failed to save: " + e.getMessage());
        }
    }
}

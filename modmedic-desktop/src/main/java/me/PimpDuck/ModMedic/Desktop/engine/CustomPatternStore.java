package me.PimpDuck.ModMedic.Desktop.engine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.PimpDuck.ModMedic.Desktop.model.ErrorPattern;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages user-created custom patterns stored in a local JSON file.
 * Merges with the bundled patterns at diagnosis time.
 */
public class CustomPatternStore {

    private final Path storagePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private List<ErrorPattern> customPatterns = new ArrayList<>();

    public CustomPatternStore() {
        this.storagePath = Paths.get(System.getProperty("user.home"), ".modmedic", "custom_patterns.json");
        load();
    }

    public CustomPatternStore(Path customPath) {
        this.storagePath = customPath;
        load();
    }

    public List<ErrorPattern> getCustomPatterns() {
        return customPatterns;
    }

    public void addPattern(ErrorPattern pattern) {
        customPatterns.add(pattern);
        save();
    }

    public void removePattern(String id) {
        customPatterns.removeIf(p -> p.getId().equals(id));
        save();
    }

    public void updatePattern(ErrorPattern pattern) {
        for (int i = 0; i < customPatterns.size(); i++) {
            if (customPatterns.get(i).getId().equals(pattern.getId())) {
                customPatterns.set(i, pattern);
                save();
                return;
            }
        }
        addPattern(pattern);
    }

    public List<ErrorPattern> getAllMerged(List<ErrorPattern> bundledPatterns) {
        List<ErrorPattern> merged = new ArrayList<>(bundledPatterns);
        merged.addAll(customPatterns);
        return merged;
    }

    private void load() {
        try {
            storagePath.getParent().toFile().mkdirs();
            if (storagePath.toFile().exists()) {
                Type listType = new TypeToken<List<ErrorPattern>>() {}.getType();
                try (FileReader reader = new FileReader(storagePath.toFile())) {
                    customPatterns = gson.fromJson(reader, listType);
                    if (customPatterns == null) customPatterns = new ArrayList<>();
                }
            }
        } catch (Exception e) {
            System.err.println("[CustomPatternStore] Failed to load: " + e.getMessage());
            customPatterns = new ArrayList<>();
        }
    }

    private void save() {
        try {
            storagePath.getParent().toFile().mkdirs();
            try (FileWriter writer = new FileWriter(storagePath.toFile())) {
                gson.toJson(customPatterns, writer);
            }
        } catch (IOException e) {
            System.err.println("[CustomPatternStore] Failed to save: " + e.getMessage());
        }
    }
}

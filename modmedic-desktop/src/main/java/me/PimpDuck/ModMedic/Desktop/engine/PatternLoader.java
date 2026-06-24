package me.PimpDuck.ModMedic.Desktop.engine;

import me.PimpDuck.ModMedic.Desktop.model.ErrorPattern;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PatternLoader {

    private final List<ErrorPattern> patterns = new ArrayList<>();

    public void load(InputStream inputStream) {
        Gson gson = new Gson();
        Type mapType = new TypeToken<java.util.Map<String, Object>>() {}.getType();
        java.util.Map<String, Object> root = gson.fromJson(new InputStreamReader(inputStream), mapType);

        Object raw = root.get("patterns");
        if (raw instanceof List) {
            String json = gson.toJson(raw);
            Type listType = new TypeToken<List<ErrorPattern>>() {}.getType();
            List<ErrorPattern> loaded = gson.fromJson(json, listType);
            patterns.addAll(loaded);
        }
    }

    public List<ErrorPattern> getPatterns() {
        return patterns;
    }
}

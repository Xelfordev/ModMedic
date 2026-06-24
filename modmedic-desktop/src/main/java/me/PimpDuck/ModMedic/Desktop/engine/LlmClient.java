package me.PimpDuck.ModMedic.Desktop.engine;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Connects to local (Ollama) or remote (OpenAI-compatible) LLM for fallback diagnosis.
 */
public class LlmClient {

    public enum Provider { OLLAMA, OPENAI }

    private Provider provider = Provider.OLLAMA;
    private String ollamaUrl = "http://localhost:11434/api/generate";
    private String openaiKey = "";
    private String openaiModel = "gpt-4o-mini";
    private String ollamaModel = "llama3.2";
    private boolean enabled = false;
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    private static final String SYSTEM_PROMPT = "You are a Minecraft Paper plugin error diagnosis expert. "
            + "Analyze the stacktrace and respond with ONLY a JSON object with two fields: "
            + "\"diagnosis\" (what caused the error in 1-2 sentences) and "
            + "\"suggested_fix\" (exact steps to fix it). "
            + "Be specific about which plugin, which method, and what needs to change. "
            + "If the error is a NullPointerException, identify which variable was null and why.";

    public void setProvider(Provider provider) { this.provider = provider; }
    public void setOllamaUrl(String url) { this.ollamaUrl = url; }
    public void setOpenaiKey(String key) { this.openaiKey = key; }
    public void setOpenaiModel(String model) { this.openaiModel = model; }
    public void setOllamaModel(String model) { this.ollamaModel = model; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }

    public Provider getProvider() { return provider; }
    public String getOllamaUrl() { return ollamaUrl; }
    public String getOpenaiKey() { return openaiKey; }
    public String getOpenaiModel() { return openaiModel; }
    public String getOllamaModel() { return ollamaModel; }

    public static class LlmResult {
        public String diagnosis;
        public String suggestedFix;
        public boolean success;
        public String error;
    }

    public LlmResult diagnose(String errorType, String message, String stacktrace) {
        if (!enabled) {
            LlmResult r = new LlmResult();
            r.success = false;
            r.error = "LLM diagnosis is disabled";
            return r;
        }

        String prompt = "Error type: " + errorType + "\n"
                + "Message: " + message + "\n"
                + "Stacktrace:\n" + truncate(stacktrace, 3000);

        try {
            switch (provider) {
                case OLLAMA:
                    return queryOllama(prompt);
                case OPENAI:
                    return queryOpenAI(prompt);
                default:
                    LlmResult r = new LlmResult();
                    r.success = false;
                    r.error = "Unknown provider";
                    return r;
            }
        } catch (Exception e) {
            LlmResult r = new LlmResult();
            r.success = false;
            r.error = e.getMessage();
            return r;
        }
    }

    private LlmResult queryOllama(String prompt) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", ollamaModel);
        body.addProperty("prompt", SYSTEM_PROMPT + "\n\n" + prompt);
        body.addProperty("stream", false);
        body.addProperty("format", "json");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        String rawResponse = json.has("response") ? json.get("response").getAsString() : "";

        return parseLlmResponse(rawResponse);
    }

    private LlmResult queryOpenAI(String prompt) throws Exception {
        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("role", "user");
        messageObj.addProperty("content", SYSTEM_PROMPT + "\n\n" + prompt);

        JsonObject body = new JsonObject();
        body.addProperty("model", openaiModel);
        body.add("messages", gson.toJsonTree(new JsonObject[]{messageObj}));
        body.add("response_format", gson.toJsonTree(new JsonObject()).getAsJsonObject());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openaiKey)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        String rawResponse = "";
        if (json.has("choices") && json.getAsJsonArray("choices").size() > 0) {
            rawResponse = json.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString();
        }

        return parseLlmResponse(rawResponse);
    }

    private LlmResult parseLlmResponse(String raw) {
        LlmResult result = new LlmResult();
        try {
            JsonObject json = gson.fromJson(raw, JsonObject.class);
            result.diagnosis = json.has("diagnosis") ? json.get("diagnosis").getAsString() : "";
            result.suggestedFix = json.has("suggested_fix") ? json.get("suggested_fix").getAsString() : "";
            result.success = !result.diagnosis.isEmpty();
            if (!result.success) {
                result.error = "LLM response didn't contain expected fields";
                result.diagnosis = raw; // fallback: show raw response
            }
        } catch (Exception e) {
            result.success = false;
            result.error = "Failed to parse LLM response";
            result.diagnosis = raw;
        }
        return result;
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }
}

package me.PimpDuck.ModMedic.Desktop.engine;

import me.PimpDuck.ModMedic.Desktop.model.ErrorEvent;
import me.PimpDuck.ModMedic.Desktop.model.ErrorPattern;

import java.util.List;

/**
 * Orchestrates the full diagnosis chain:
 * 1. Built-in pattern matching
 * 2. Custom user patterns
 * 3. LLM fallback (if enabled)
 */
public class FixSuggester {

    private final DiagnosisEngine diagnosisEngine;
    private final CustomPatternStore customPatternStore;
    private final LlmClient llmClient;

    public FixSuggester(DiagnosisEngine diagnosisEngine, CustomPatternStore customPatternStore, LlmClient llmClient) {
        this.diagnosisEngine = diagnosisEngine;
        this.customPatternStore = customPatternStore;
        this.llmClient = llmClient;
    }

    public static class SuggestionResult {
        public List<DiagnosisEngine.DiagnosisResult> patternResults;
        public LlmClient.LlmResult llmResult;
        public String source; // "patterns", "custom_patterns", "llm", "none"

        public boolean hasDiagnosis() {
            return (patternResults != null && !patternResults.isEmpty())
                    || (llmResult != null && llmResult.success);
        }
    }

    public SuggestionResult suggest(ErrorEvent event) {
        SuggestionResult result = new SuggestionResult();

        // Step 1 & 2: Built-in + custom patterns (merged)
        List<ErrorPattern> allPatterns = customPatternStore.getAllMerged(
                diagnosisEngine.getPatterns());
        DiagnosisEngine mergedEngine = new DiagnosisEngine(allPatterns);
        result.patternResults = mergedEngine.diagnose(event);

        if (!result.patternResults.isEmpty()) {
            // Check if a custom pattern matched
            String topId = result.patternResults.get(0).getPattern().getId();
            boolean isCustom = customPatternStore.getCustomPatterns().stream()
                    .anyMatch(p -> p.getId().equals(topId));
            result.source = isCustom ? "custom_patterns" : "patterns";
            return result;
        }

        // Step 3: LLM fallback
        if (llmClient.isEnabled()) {
            result.llmResult = llmClient.diagnose(
                    event.getErrorType(), event.getMessage(), event.getStacktrace());
            if (result.llmResult.success) {
                result.source = "llm";
                return result;
            }
        }

        result.source = "none";
        return result;
    }
}

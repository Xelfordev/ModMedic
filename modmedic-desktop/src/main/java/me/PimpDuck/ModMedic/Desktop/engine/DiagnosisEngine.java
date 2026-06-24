package me.PimpDuck.ModMedic.Desktop.engine;

import me.PimpDuck.ModMedic.Desktop.model.ErrorEvent;
import me.PimpDuck.ModMedic.Desktop.model.ErrorPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DiagnosisEngine {

    private final List<ErrorPattern> patterns;

    public DiagnosisEngine(List<ErrorPattern> patterns) {
        this.patterns = patterns;
    }

    public static class DiagnosisResult {
        private ErrorPattern pattern;
        private double confidence;
        private String matchedOn;
        private String causalChain;

        public DiagnosisResult(ErrorPattern pattern, double confidence, String matchedOn, String causalChain) {
            this.pattern = pattern;
            this.confidence = confidence;
            this.matchedOn = matchedOn;
            this.causalChain = causalChain;
        }

        public ErrorPattern getPattern() { return pattern; }
        public double getConfidence() { return confidence; }
        public String getMatchedOn() { return matchedOn; }
        public String getCausalChain() { return causalChain; }
    }

    public static class CausalChain {
        public List<CausalLink> links = new ArrayList<>();

        public static class CausalLink {
            public String exceptionType;
            public String message;
            public List<StackFrame> frames = new ArrayList<>();
        }

        public static class StackFrame {
            public String className;
            public String methodName;
            public String fileName;
            public int lineNumber;
        }

        public boolean isEmpty() { return links.isEmpty(); }
    }

    public List<DiagnosisResult> diagnose(ErrorEvent event) {
        String stacktrace = event.getStacktrace() != null ? event.getStacktrace() : "";
        String message = event.getMessage() != null ? event.getMessage() : "";
        String errorType = event.getErrorType() != null ? event.getErrorType() : "";

        // Parse causal chain
        CausalChain chain = parseCausalChain(stacktrace);

        List<DiagnosisResult> results = new ArrayList<>();

        for (ErrorPattern pattern : patterns) {
            double score = 0;
            int checks = 0;
            List<String> matches = new ArrayList<>();

            // 1. Check error type
            if (pattern.getMatch().getErrorType() != null) {
                checks++;
                String expected = pattern.getMatch().getErrorType().toLowerCase();
                if (errorType.equalsIgnoreCase(expected)) {
                    score++;
                    matches.add("error_type==" + expected);
                } else if (errorType.toLowerCase().contains(expected)) {
                    score += 0.5;
                    matches.add("error_type~=" + expected);
                }
                // Also check causal chain error types
                for (CausalChain.CausalLink link : chain.links) {
                    if (link.exceptionType != null && link.exceptionType.toLowerCase().contains(expected)) {
                        score += 0.6;
                        matches.add("caused_by==" + expected);
                        break;
                    }
                }
            }

            // 2. Check stacktrace content (broad match)
            if (pattern.getMatch().getStacktraceContains() != null) {
                checks++;
                String needle = pattern.getMatch().getStacktraceContains().toLowerCase();
                String fullStack = (stacktrace + "\n" + chain.links.stream()
                        .map(l -> l.exceptionType + ": " + l.message)
                        .collect(Collectors.joining("\n"))).toLowerCase();
                if (fullStack.contains(needle)) {
                    score++;
                    matches.add("stacktrace_contains==" + needle);
                }
            }

            // 3. Check message content
            if (pattern.getMatch().getMessageContains() != null) {
                checks++;
                String needle = pattern.getMatch().getMessageContains().toLowerCase();
                String fullMessage = (message + "\n" + chain.links.stream()
                        .map(l -> l.message)
                        .filter(m -> m != null)
                        .collect(Collectors.joining("\n"))).toLowerCase();
                if (fullMessage.contains(needle)) {
                    score++;
                    matches.add("message_contains==" + needle);
                }
            }

            // 4. Check specific method names in stack frames
            if (pattern.getMatch().getStacktraceContains() != null) {
                String needle = pattern.getMatch().getStacktraceContains().toLowerCase();
                for (CausalChain.CausalLink link : chain.links) {
                    for (CausalChain.StackFrame frame : link.frames) {
                        String frameStr = (frame.className + "." + frame.methodName).toLowerCase();
                        if (frameStr.contains(needle)) {
                            if (checks > 0) {
                                score += 0.3;
                            }
                            matches.add("frame_contains==" + frameStr);
                            break;
                        }
                    }
                }
            }

            if (checks > 0) {
                double confidence = Math.min(score / checks, 1.0);
                if (confidence >= 0.3) {
                    String causalChain = formatCausalChain(chain);
                    results.add(new DiagnosisResult(pattern, confidence,
                            matches.isEmpty() ? "general match" : String.join(", ", matches),
                            causalChain));
                }
            }
        }

        results.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
        return results;
    }

    public CausalChain parseCausalChain(String stacktrace) {
        CausalChain chain = new CausalChain();
        if (stacktrace == null || stacktrace.isEmpty()) return chain;

        String[] lines = stacktrace.split("\n");
        CausalChain.CausalLink currentLink = null;

        for (String line : lines) {
            String trimmed = line.trim();

            // Detect "Caused by:" or "Suppressed:" lines
            java.util.regex.Matcher causedByMatcher = Pattern.compile(
                    "^(Caused by|Suppressed):\\s*(\\S+)(:\\s*(.*))?$").matcher(trimmed);
            if (causedByMatcher.find()) {
                currentLink = new CausalChain.CausalLink();
                currentLink.exceptionType = causedByMatcher.group(2);
                currentLink.message = causedByMatcher.group(4);
                chain.links.add(currentLink);
                continue;
            }

            // Detect the first exception line (no leading whitespace / "at")
            if (currentLink == null && !trimmed.startsWith("at ") && !trimmed.startsWith("...")) {
                java.util.regex.Matcher exceptionMatcher = Pattern.compile(
                        "^(\\S+)(:\\s*(.*))?$").matcher(trimmed);
                if (exceptionMatcher.find() && !trimmed.contains(" ")) {
                    currentLink = new CausalChain.CausalLink();
                    currentLink.exceptionType = exceptionMatcher.group(1);
                    currentLink.message = exceptionMatcher.group(3);
                    chain.links.add(currentLink);
                    continue;
                }
                // Try to detect: "java.lang.NullPointerException: message" or just "java.lang.NullPointerException"
                java.util.regex.Matcher mainExMatcher = Pattern.compile(
                        "^([a-zA-Z_][\\w.]*(?:Exception|Error|Throwable))(?::\\s*(.*))?$").matcher(trimmed);
                if (mainExMatcher.find()) {
                    currentLink = new CausalChain.CausalLink();
                    currentLink.exceptionType = mainExMatcher.group(1);
                    currentLink.message = mainExMatcher.group(2);
                    chain.links.add(currentLink);
                    continue;
                }
            }

            // Parse stack frames "at com.example.Foo.bar(Foo.java:42)"
            if (trimmed.startsWith("at ")) {
                Matcher frameMatcher = Pattern.compile(
                        "at\\s+([\\w.]+)\\.([\\w<>\"]+)\\(([^:]+)(?::(\\d+))?\\)").matcher(trimmed);
                if (frameMatcher.find() && currentLink != null) {
                    CausalChain.StackFrame frame = new CausalChain.StackFrame();
                    frame.className = frameMatcher.group(1);
                    frame.methodName = frameMatcher.group(2);
                    frame.fileName = frameMatcher.group(3);
                    try {
                        frame.lineNumber = Integer.parseInt(frameMatcher.group(4));
                    } catch (Exception e) {
                        frame.lineNumber = -1;
                    }
                    currentLink.frames.add(frame);
                    continue;
                }
            }

            // Handle "at net.minecraft.server..." (mapped names) with simpler pattern
            if (trimmed.startsWith("at ")) {
                Matcher simpleMatcher = Pattern.compile("at\\s+([\\w.$]+)\\.([\\w<>$]+)\\(.*\\)").matcher(trimmed);
                if (simpleMatcher.find() && currentLink != null) {
                    CausalChain.StackFrame frame = new CausalChain.StackFrame();
                    frame.className = simpleMatcher.group(1);
                    frame.methodName = simpleMatcher.group(2);
                    frame.fileName = "?";
                    frame.lineNumber = -1;
                    currentLink.frames.add(frame);
                }
            }
        }

        return chain;
    }

    private String formatCausalChain(CausalChain chain) {
        if (chain == null || chain.links.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Error chain:\n");
        for (int i = 0; i < chain.links.size(); i++) {
            CausalChain.CausalLink link = chain.links.get(i);
            sb.append("  ").append(i == 0 ? "└─ " : "   └─ ");
            sb.append(link.exceptionType);
            if (link.message != null) {
                // Truncate long messages
                String msg = link.message.length() > 120 ? link.message.substring(0, 120) + "..." : link.message;
                sb.append(": ").append(msg);
            }
            sb.append("\n");
            // Show first plugin frame if available
            for (CausalChain.StackFrame frame : link.frames) {
                String pkg = frame.className.toLowerCase();
                if (!pkg.startsWith("net.minecraft") && !pkg.startsWith("org.bukkit")
                        && !pkg.startsWith("java.") && !pkg.startsWith("io.papermc")) {
                    sb.append("       at ").append(frame.className).append(".").append(frame.methodName);
                    if (frame.lineNumber > 0) {
                        sb.append(":").append(frame.lineNumber);
                    }
                    sb.append("\n");
                    break;
                }
            }
        }
        return sb.toString();
    }

    public List<ErrorPattern> getPatterns() {
        return patterns;
    }

    public DiagnosisEngine withAdditionalPatterns(List<ErrorPattern> additional) {
        List<ErrorPattern> merged = new ArrayList<>(patterns);
        merged.addAll(additional);
        return new DiagnosisEngine(merged);
    }
}

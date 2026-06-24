package me.PimpDuck.ModMedic.Desktop.model;

import java.util.Map;

public class ErrorPattern {

    public static class MatchCriteria {
        private String errorType;
        private String stacktraceContains;
        private String messageContains;

        public String getErrorType() { return errorType; }
        public void setErrorType(String errorType) { this.errorType = errorType; }
        public String getStacktraceContains() { return stacktraceContains; }
        public void setStacktraceContains(String stacktraceContains) { this.stacktraceContains = stacktraceContains; }
        public String getMessageContains() { return messageContains; }
        public void setMessageContains(String messageContains) { this.messageContains = messageContains; }
    }

    public static class AutoFix {
        private String type;
        private String value;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    private String id;
    private String name;
    private String severity;
    private MatchCriteria match;
    private String diagnosis;
    private String suggestedFix;
    private AutoFix autoFix;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public MatchCriteria getMatch() { return match; }
    public void setMatch(MatchCriteria match) { this.match = match; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getSuggestedFix() { return suggestedFix; }
    public void setSuggestedFix(String suggestedFix) { this.suggestedFix = suggestedFix; }
    public AutoFix getAutoFix() { return autoFix; }
    public void setAutoFix(AutoFix autoFix) { this.autoFix = autoFix; }
}

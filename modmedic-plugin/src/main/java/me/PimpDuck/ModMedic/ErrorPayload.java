package me.PimpDuck.ModMedic;

import java.util.List;

public class ErrorPayload {
    private String plugin;
    private String serverVersion;
    private String errorType;
    private String message;
    private String stacktrace;
    private String causedBy;
    private long timestamp;
    private List<String> recentLog;

    public ErrorPayload() {}

    public ErrorPayload(String plugin, String serverVersion, String errorType, String message,
                        String stacktrace, String causedBy, long timestamp, List<String> recentLog) {
        this.plugin = plugin;
        this.serverVersion = serverVersion;
        this.errorType = errorType;
        this.message = message;
        this.stacktrace = stacktrace;
        this.causedBy = causedBy;
        this.timestamp = timestamp;
        this.recentLog = recentLog;
    }

    public String getPlugin() { return plugin; }
    public void setPlugin(String plugin) { this.plugin = plugin; }
    public String getServerVersion() { return serverVersion; }
    public void setServerVersion(String serverVersion) { this.serverVersion = serverVersion; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStacktrace() { return stacktrace; }
    public void setStacktrace(String stacktrace) { this.stacktrace = stacktrace; }
    public String getCausedBy() { return causedBy; }
    public void setCausedBy(String causedBy) { this.causedBy = causedBy; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public List<String> getRecentLog() { return recentLog; }
    public void setRecentLog(List<String> recentLog) { this.recentLog = recentLog; }
}

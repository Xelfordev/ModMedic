# ModMedic — Architecture Plan

## Overview

Two-component system: a Paper plugin captures crashes in real-time, a desktop app receives, parses, diagnoses, and suggests fixes.

---

## Component 1: Paper Plugin (`ModMedic`)

### What it does
- Listens for every plugin error via Paper's `ServerExceptionEvent`
- Also catches exceptions from BukkitRunnable tasks
- Sends structured error data over WebSocket to the desktop app

### Data sent per error
```
{
  "plugin": "ArrowsPlus",
  "server_version": "1.21.11-Paper-69",
  "error_type": "NullPointerException",
  "message": "Cannot invoke \"org.bukkit.entity.Player.getName()\" because \"p\" is null",
  "stacktrace": "at me.PimpDuck.ArrowsPlus.BowEnchants$1.run(BowEnchants.java:142)...",
  "caused_by": "at com.example.SomePlugin.doSomething(SomePlugin.java:50)",
  "timestamp": 1719234567890,
  "recent_log": ["[08:44:37] [ArrowsPlus] is now enabled!", "..."]
}
```

### Network
- Embedded WebSocket **client** inside the plugin
- Connects to `ws://localhost:9876` (configurable)
- Auto-reconnects if desktop app is down
- Buffers up to 50 errors while disconnected, sends on reconnect

### Config (`config.yml`)
```yaml
desktop_host: localhost
desktop_port: 9876
reconnect_interval_seconds: 5
buffer_size: 50
```

---

## Component 2: Desktop App

### Tech Stack Options

| Option | Pros | Cons |
|--------|------|------|
| **JavaFX** | Same language as plugin, user knows Java | UIs look dated without heavy styling |
| **Electron** (HTML/CSS/JS) | Beautiful UI, huge ecosystem | Heavy bundle, need to learn web stack |
| **C# WPF/WinUI** | Best Windows-native UI | Windows-only, new language |
| **Tauri** (Rust + web UI) | Lightweight, beautiful UI | Need Rust + web stack |

**Recommendation:** [ask user]

### Core features

1. **Live Feed** — errors appear as they happen, color-coded by severity
2. **Detail Panel** — click any error to see full stacktrace, plugin info, server version
3. **Diagnosis Engine** — matches stacktrace against the pattern database
4. **Quick Fix Panel** — shows suggested fix with a button to apply it (e.g., "Create missing config key")
5. **Dashboard** — error frequency chart, top crashing plugins, errors over time
6. **Log Viewer** — recent log lines around the crash

---

## Component 3: Error Pattern Database

### Why not hardcoded?
- New errors discovered = just add a DB row, no recompile
- Can be shared/updated independently
- Community could contribute patterns
- Ships as a `patterns.json` file alongside the app

### Pattern structure
```json
{
  "patterns": [
    {
      "id": "cfg-missing-key",
      "name": "Missing configuration key",
      "severity": "low",
      "match": {
        "error_type": "NullPointerException",
        "stacktrace_contains": "getConfig().getString"
      },
      "diagnosis": "The plugin tried to read a config value that doesn't exist.",
      "suggested_fix": "Add the missing key to the plugin's config.yml, or use .getString(\"path\", \"default\") instead.",
      "auto_fix": {
        "type": "add_config_key",
        "value": "default_value"
      }
    },
    {
      "id": "deprecated-api",
      "name": "Deprecated Bukkit API usage",
      "severity": "medium",
      "match": {
        "error_type": "NoSuchMethodError",
        "message_contains": "getItemInHand"
      },
      "diagnosis": "The plugin uses a method removed in this server version.",
      "suggested_fix": "Replace getItemInHand() with getInventory().getItemInMainHand().",
      "auto_fix": null
    },
    {
      "id": "missing-dependency",
      "name": "Missing plugin dependency",
      "severity": "high",
      "match": {
        "stacktrace_contains": "ClassNotFoundException"
      },
      "diagnosis": "A required library or plugin is not installed.",
      "suggested_fix": "Install the required dependency plugin or add the library to the server's lib folder.",
      "auto_fix": null
    }
  ]
}
```

### Bootstrapping
- Start with ~50 manually curated patterns covering the most common Paper errors
- Sources: Paper docs, Spigot forums, Stack Overflow, user's own 9 years of fixing errors

---

## Communication Flow

```
Paper Server
  └─ ModMedic Plugin
       └─ catches ServerExceptionEvent
            └─ WebSocket Client ──→ ws://localhost:9876 ──→ Desktop App
                                                             ├─ Diagnosis Engine
                                                             │    └─ matches against patterns.json
                                                             ├─ Live Feed UI
                                                             ├─ Detail Panel
                                                             └─ Quick Fix actions
```

---

## Future Possibilities

- **LLM integration** — if pattern DB has no match, send stacktrace to an LLM for diagnosis
- **Community pattern sharing** — users can download additional pattern packs
- **Remote servers** — connect to multiple servers from one desktop app
- **Auto-fix scripts** — "Apply fix" button that edits plugin configs or suggests code changes
- **Plugin health score** — aggregate error data to score plugins by stability

---

## Questions to Decide

1. Desktop app language/framework?
2. Start with 50 patterns — how should we source them?
3. MVP scope: live feed + detail + diagnosis only, skip auto-fix for v1?
4. Should the desktop app be able to send commands back to the server (e.g., "reload plugin")?

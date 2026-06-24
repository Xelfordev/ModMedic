<div align="center">
  <img src="logo.svg" alt="ModMedic" width="128">
  <h1>ModMedic</h1>
  <p><strong>Real-time error diagnostics for Paper server plugins</strong></p>
  <p>
    <img src="https://img.shields.io/badge/Paper-1.21.1-2e7d32?style=flat-square">
    <img src="https://img.shields.io/badge/Java-21-f0920a?style=flat-square&logo=java">
    <img src="https://img.shields.io/badge/JavaFX-21-007396?style=flat-square">
    <img src="https://img.shields.io/badge/license-GPL--3.0-blue?style=flat-square">
    <img src="https://img.shields.io/github/v/release/Xelfordev/ModMedic?style=flat-square">
  </p>
  <p>
    <a href="#features">Features</a> |
    <a href="#architecture">Architecture</a> |
    <a href="#getting-started">Getting Started</a> |
    <a href="#building">Building</a> |
    <a href="#configuration">Configuration</a> |
    <a href="#license">License</a>
  </p>
</div>

---

ModMedic catches plugin errors on your Paper server in real time and sends them to a desktop companion app for instant diagnosis. No more digging through console logs — see the problem, the cause, and suggested fixes as they happen.

## Features

- **Live Error Feed** — Plugin crashes appear instantly in the desktop UI via WebSocket
- **Smart Diagnosis Engine** — 52 built-in patterns covering NPEs, NoSuchMethodError, ClassNotFoundException, YAML/config issues, version mismatches, and more
- **Causal Chain Parsing** — Follows `Caused by:` chains through the full stack trace
- **Custom Patterns** — Add your own diagnosis patterns through the desktop UI (persisted across restarts)
- **LLM Fallback** — Optional Ollama (local) or OpenAI integration for errors no pattern matches
- **Console Log Viewer** — All server output mirrored to the desktop app
- **Dashboard** — Stats dashboard showing error counts by plugin, pattern, and time
- **Self-Contained Bundle** — Desktop app ships with JavaFX bundled — no separate install needed
- **Zero Server Config** — Drop the plugin in `plugins/`, start desktop, that's it

## Architecture

```
┌──────────────────────┐     WebSocket      ┌──────────────────────────┐
│   Paper Server       │ ◄──────────────────►│   Desktop App (JavaFX)  │
│                      │     ws://:9876      │                          │
│  ┌────────────────┐  │                     │  ┌────────────────────┐  │
│  │ Plugin         │  │   live errors       │  │ DiagnosisEngine    │  │
│  │ ErrorListener  │──┤─────────────────────►│  │  · Built-in (52)  │  │
│  │ WebSocketClient│  │   + console lines   │  │  · Custom patterns │  │
│  │ CommandListener│  │                     │  │  · LLM fallback    │  │
│  └────────────────┘  │                     │  └────────────────────┘  │
│                      │   commands          │  ┌────────────────────┐  │
│                      │◄────────────────────┤──│ FixSuggester      │  │
│                      │   (reload, etc.)    │  └────────────────────┘  │
└──────────────────────┘                     └──────────────────────────┘
```

## Getting Started

### Prerequisites

- **Server:** Paper 1.21.1 (or compatible fork)
- **Desktop:** Windows 10+ (bundle includes JavaFX for Windows)
- **Java 21** (for building from source)

### Quick Start

1. **Download** the [latest release](https://github.com/Xelfordev/ModMedic/releases)
2. **Install the plugin** — copy `server/ModMedic-1.0.0.jar` to your server's `plugins/` folder
3. **Start the desktop app** — run `desktop/bin/ModMedicDesktop.bat`
4. **Start your server** — the plugin connects automatically

Errors will appear in the desktop app as they happen.

### Verifying the Connection

On the server, run:

```
/modmedic ping
```

If connected, you'll see: `ModMedic: WebSocket is connected.`

To test the full pipeline:

```
/modmedic test
```

This fires a real `ServerExceptionEvent` — you'll see the error appear in the desktop UI.

## Building from Source

### Plugin

```bash
cd modmedic-plugin
gradlew build
```

Output: `modmedic-plugin/build/libs/ModMedic-1.0.0.jar`

### Desktop App

```bash
cd modmedic-desktop
gradlew distZip
```

Output: `modmedic-desktop/build/distributions/ModMedicDesktop-1.0.0.zip`

### Full Bundle

Run `build-bundle.bat` from the project root — produces `ModMedic-Bundle/` with both the plugin and the ready-to-run desktop app.

## Configuration

### Plugin (`plugins/ModMedic/config.yml`)

```yaml
websocket:
  host: localhost
  port: 9876
```

No config needed for basic use — defaults work out of the box.

### Desktop App

Settings are managed through the UI (gear icon). Configurable:

| Setting | Description |
|---------|-------------|
| LLM Integration | Enable/disable LLM fallback |
| Provider | Ollama (local, free) or OpenAI |
| Model | e.g., `codellama`, `gpt-4o-mini` |
| API URL | Ollama: `http://localhost:11434`, OpenAI: `https://api.openai.com` |
| API Key | Required for OpenAI only |

Settings are saved to `~/.modmedic/settings.json`.

Custom patterns can be added via the Pattern Manager (book icon) and are saved to `~/.modmedic/custom_patterns.json`.

## Contributing

Contributions are welcome! See [ARCHITECTURE.md](ARCHITECTURE.md) for the project structure and design decisions.

- **Bug reports / feature requests** — open a [GitHub Issue](https://github.com/Xelfordev/ModMedic/issues)
- **Pull requests** — fork and submit

The diagnosis pattern library in `patterns/patterns.json` is a great place to contribute — add patterns for errors you've encountered.

## License

ModMedic is open source under the GNU General Public License v3.0. See [LICENSE](LICENSE).

---

<p align="center">
  Created by <a href="https://xelforo.lovestoblog.com">PimpDuck</a> — available on the <a href="https://spigotmc.org/">Spigot Resource</a> page.
</p>

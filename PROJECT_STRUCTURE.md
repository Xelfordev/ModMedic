# ModMedic — Project Structure

```
C:\Users\grove\Desktop\Modmedic Project\
├── ARCHITECTURE.md
├── modmedic-plugin\          # Paper plugin (Gradle project)
│   ├── build.gradle
│   ├── src\main\java\me\PimpDuck\ModMedic\
│   │   ├── ModMedic.java              # onEnable / onDisable
│   │   ├── ErrorListener.java          # ServerExceptionEvent handler
│   │   ├── WebSocketClient.java        # Connects to desktop app
│   │   ├── ErrorPayload.java           # Data model (serialized to JSON)
│   │   └── CommandListener.java        # Receives commands from desktop
│   └── src\main\resources\
│       ├── plugin.yml
│       └── config.yml
│
├── modmedic-desktop\         # JavaFX desktop app
│   ├── build.gradle
│   ├── src\main\java\me\PimpDuck\ModMedic\Desktop\
│   │   ├── ModMedicDesktop.java        # Main entry + JavaFX launch
│   │   ├── ui\
│   │   │   ├── MainController.java     # Main window controller
│   │   │   ├── LiveFeedPanel.java      # Real-time error list
│   │   │   ├── DetailPanel.java        # Stacktrace + diagnosis view
│   │   │   ├── DashboardPanel.java     # Charts / stats
│   │   │   └── LogViewerPanel.java     # Recent log lines
│   │   ├── engine\
│   │   │   ├── DiagnosisEngine.java    # Match errors → patterns
│   │   │   ├── PatternLoader.java      # Load patterns.json
│   │   │   └── FixApplier.java         # Execute auto-fixes
│   │   ├── server\
│   │   │   └── WebSocketServer.java    # Receives from plugin, sends commands
│   │   └── model\
│   │       ├── ErrorEvent.java
│   │       └── ErrorPattern.java
│   └── src\main\resources\
│       ├── patterns.json               # Ship with ~50 patterns
│       └── ui\*.fxml                   # JavaFX scene layouts
│
└── patterns\                 # Pattern development
    └── patterns.json          # Master pattern database
```

## Implementation Order

1. **Plugin**: WebSocket client + error capture → sends to desktop
2. **Desktop**: WebSocket server → receives errors → displays in live feed
3. **Pattern DB**: 50 curated patterns with diagnoses and auto-fix actions
4. **Desktop**: Diagnosis engine → matches errors → shows fix in UI
5. **Desktop**: Auto-fix execution (add config keys, etc.)
6. **Bidirectional**: Desktop → Plugin commands (reload, etc.)
7. **Polish**: Dashboard charts, logging, error buffering, dark theme

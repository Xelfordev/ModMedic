package me.PimpDuck.ModMedic.Desktop;

import javafx.application.Application;
import javafx.stage.Stage;
import me.PimpDuck.ModMedic.Desktop.engine.*;
import me.PimpDuck.ModMedic.Desktop.server.ModMedicServer;
import me.PimpDuck.ModMedic.Desktop.ui.MainController;

import java.io.InputStream;

public class ModMedicDesktop extends Application {

    private ModMedicServer server;
    private MainController controller;

    @Override
    public void start(Stage stage) {
        // Load patterns
        PatternLoader patternLoader = new PatternLoader();
        try (InputStream is = getClass().getResourceAsStream("/patterns.json")) {
            if (is != null) {
                patternLoader.load(is);
                System.out.println("[ModMedicDesktop] Loaded " + patternLoader.getPatterns().size() + " patterns");
            } else {
                System.out.println("[ModMedicDesktop] No patterns.json found — running without diagnosis");
            }
        } catch (Exception e) {
            System.err.println("[ModMedicDesktop] Failed to load patterns: " + e.getMessage());
        }

        // Initialize services
        SettingsManager settings = new SettingsManager();
        LlmClient llmClient = new LlmClient();
        settings.applyTo(llmClient);
        CustomPatternStore customPatternStore = new CustomPatternStore();

        // Start WebSocket server
        int port = 9876;
        server = new ModMedicServer(port);

        // Create UI
        controller = new MainController(server, patternLoader, customPatternStore, llmClient, settings);
        stage.setScene(controller.createScene(stage));
        stage.setOnCloseRequest(e -> {
            try {
                server.stop();
            } catch (Exception ignored) {}
        });

        // Wire up server to UI
        server.setOnError(event -> controller.onErrorReceived(event));
        server.setOnLog(line -> controller.onLogReceived(line));
        server.setOnPluginConnected(() -> controller.setPluginCount(server.getPluginCount()));
        server.setOnPluginDisconnected(() -> controller.setPluginCount(server.getPluginCount()));
        server.start();
        controller.setServerStatus(true);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

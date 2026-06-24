package me.PimpDuck.ModMedic.Desktop.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import me.PimpDuck.ModMedic.Desktop.engine.*;
import me.PimpDuck.ModMedic.Desktop.model.ErrorEvent;
import me.PimpDuck.ModMedic.Desktop.model.ErrorPattern;
import me.PimpDuck.ModMedic.Desktop.server.ModMedicServer;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainController {

    private final ModMedicServer server;
    private final FixSuggester fixSuggester;
    private final DiagnosisEngine diagnosisEngine;
    private final PatternLoader patternLoader;
    private final CustomPatternStore customPatternStore;
    private final LlmClient llmClient;
    private final SettingsManager settings;
    private final ObservableList<ErrorEvent> errorList = FXCollections.observableArrayList();
    private final ObservableList<String> logList = FXCollections.observableArrayList();

    private TableView<ErrorEvent> tableView;
    private TextArea detailArea;
    private TextArea diagnosisArea;
    private Label statusLabel;
    private Label countLabel;
    private ListView<String> logView;
    private Label pluginCountLabel;
    private int testCounter = 0;
    private TabPane tabPane;

    public MainController(ModMedicServer server, PatternLoader patternLoader,
                          CustomPatternStore customPatternStore, LlmClient llmClient,
                          SettingsManager settings) {
        this.server = server;
        this.patternLoader = patternLoader;
        this.customPatternStore = customPatternStore;
        this.llmClient = llmClient;
        this.settings = settings;
        this.diagnosisEngine = new DiagnosisEngine(patternLoader.getPatterns());
        this.fixSuggester = new FixSuggester(diagnosisEngine, customPatternStore, llmClient);
    }

    public Scene createScene(Stage stage) {
        stage.setTitle("ModMedic — Plugin Error Monitor");

        BorderPane root = new BorderPane();

        Label header = new Label("ModMedic");
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        header.setPadding(new Insets(15, 15, 10, 15));

        tabPane = new TabPane();
        Tab liveTab = new Tab("Errors");
        liveTab.setClosable(false);
        Tab consoleTab = new Tab("Console");
        consoleTab.setClosable(false);
        Tab dashboardTab = new Tab("Dashboard");
        dashboardTab.setClosable(false);

        // --- Errors Tab ---
        BorderPane livePane = new BorderPane();

        tableView = new TableView<>(errorList);
        tableView.setPrefWidth(500);

        TableColumn<ErrorEvent, String> timeCol = new TableColumn<>("Time");
        timeCol.setPrefWidth(140);
        timeCol.setCellValueFactory(data -> {
            long ts = data.getValue().getTimestamp();
            String formatted = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(ts));
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });

        TableColumn<ErrorEvent, String> pluginCol = new TableColumn<>("Plugin");
        pluginCol.setPrefWidth(120);
        pluginCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getPlugin()));

        TableColumn<ErrorEvent, String> typeCol = new TableColumn<>("Error");
        typeCol.setPrefWidth(200);
        typeCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getErrorType()));

        tableView.getColumns().addAll(timeCol, pluginCol, typeCol);
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) showDetail(selected);
        });

        VBox detailBox = new VBox(5);
        detailBox.setPadding(new Insets(10));
        detailBox.setPrefWidth(500);

        Label detailLabel = new Label("Stacktrace");
        detailLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        detailArea = new TextArea();
        detailArea.setEditable(false);
        detailArea.setFont(Font.font("Consolas", 12));
        detailArea.setPrefHeight(180);

        Label diagnosisLabel = new Label("Diagnosis");
        diagnosisLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        diagnosisArea = new TextArea();
        diagnosisArea.setEditable(false);
        diagnosisArea.setFont(Font.font("Consolas", 12));
        diagnosisArea.setPrefHeight(250);
        diagnosisArea.setStyle("-fx-control-inner-background: #1a1a2e; -fx-text-fill: #e0e0e0;");

        Button applyFixBtn = new Button("Apply Fix");
        applyFixBtn.setVisible(false);

        detailBox.getChildren().addAll(detailLabel, detailArea, diagnosisLabel, diagnosisArea, applyFixBtn);
        livePane.setLeft(tableView);
        livePane.setCenter(detailBox);
        liveTab.setContent(livePane);

        // --- Console Tab ---
        BorderPane consolePane = new BorderPane();
        logView = new ListView<>(logList);
        logView.setStyle("-fx-font-family: Consolas; -fx-font-size: 12;");
        Button clearLogBtn = new Button("Clear Console");
        clearLogBtn.setOnAction(e -> logList.clear());
        HBox consoleTop = new HBox(10, clearLogBtn);
        consoleTop.setPadding(new Insets(5));
        consolePane.setTop(consoleTop);
        consolePane.setCenter(logView);
        consoleTab.setContent(consolePane);

        // --- Dashboard Tab ---
        VBox dashboardBox = new VBox(10);
        dashboardBox.setPadding(new Insets(15));
        Label dashLabel = new Label("Error Statistics");
        dashLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        countLabel = new Label("Total errors captured: 0");
        countLabel.setFont(Font.font("Segoe UI", 14));
        pluginCountLabel = new Label("Connected plugins: 0");
        pluginCountLabel.setFont(Font.font("Segoe UI", 14));

        Label llmStatusLabel = new Label();
        llmStatusLabel.setFont(Font.font("Segoe UI", 14));
        updateLlmStatus(llmStatusLabel);

        Button settingsBtn = new Button("Settings");
        settingsBtn.setOnAction(e -> {
            SettingsDialog dialog = new SettingsDialog(settings, llmClient);
            dialog.showAndWait();
            updateLlmStatus(llmStatusLabel);
        });

        Button patternsBtn = new Button("Pattern Manager");
        patternsBtn.setOnAction(e -> {
            PatternManagerDialog dialog = new PatternManagerDialog(customPatternStore);
            dialog.showAndWait();
        });

        HBox dashButtons = new HBox(10, settingsBtn, patternsBtn);
        dashboardBox.getChildren().addAll(dashLabel, countLabel, pluginCountLabel, llmStatusLabel, dashButtons);
        dashboardTab.setContent(dashboardBox);

        tabPane.getTabs().addAll(liveTab, consoleTab, dashboardTab);

        // Bottom: status bar
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5, 15, 5, 15));
        statusBar.setStyle("-fx-background-color: #2d2d2d;");
        statusLabel = new Label("Server: Stopped");
        statusLabel.setTextFill(Color.LIGHTGRAY);

        Button testBtn = new Button("Test Event");
        testBtn.setOnAction(e -> generateTestEvent());
        Button clearBtn = new Button("Clear Errors");
        clearBtn.setOnAction(e -> {
            errorList.clear();
            detailArea.clear();
            diagnosisArea.clear();
            updateCount();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statusBar.getChildren().addAll(statusLabel, testBtn, clearBtn, spacer);

        root.setTop(header);
        root.setCenter(tabPane);
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/dark.css").toExternalForm());
        return scene;
    }

    public void onErrorReceived(ErrorEvent event) {
        Platform.runLater(() -> {
            errorList.add(0, event);
            updateCount();
        });
    }

    public void onLogReceived(String line) {
        Platform.runLater(() -> {
            logList.add(line);
            if (logList.size() > 1000) {
                logList.remove(0);
            }
        });
    }

    public void setServerStatus(boolean running) {
        Platform.runLater(() -> {
            if (running) {
                statusLabel.setText("Server: Running on ws://localhost:" + server.getPort());
                statusLabel.setTextFill(Color.LIGHTGREEN);
            } else {
                statusLabel.setText("Server: Stopped");
                statusLabel.setTextFill(Color.INDIANRED);
            }
        });
    }

    public void setPluginCount(int count) {
        Platform.runLater(() -> pluginCountLabel.setText("Connected plugins: " + count));
    }

    private void generateTestEvent() {
        testCounter++;
        ErrorEvent test = new ErrorEvent(
                "TestPlugin",
                "1.21.11-Paper",
                "NullPointerException",
                "Cannot invoke \"org.bukkit.entity.Player.getName()\" because \"player\" is null",
                "java.lang.NullPointerException: Cannot invoke \"org.bukkit.entity.Player.getName()\" because \"player\" is null\n" +
                "    at me.PimpDuck.TestPlugin.commands.SomeCommand.onCommand(SomeCommand.java:42)\n" +
                "    at org.bukkit.command.SimpleCommandMap.execute(SimpleCommandMap.java:150)\n" +
                "    at org.bukkit.craftbukkit.CraftServer.dispatchCommand(CraftServer.java:1018)\n" +
                "Caused by: java.lang.IllegalStateException: Player data not loaded yet\n" +
                "    at me.PimpDuck.TestPlugin.data.PlayerDataManager.load(PlayerDataManager.java:88)\n" +
                "    ... 3 more",
                null,
                System.currentTimeMillis(),
                null
        );
        onErrorReceived(test);
    }

    private void showDetail(ErrorEvent event) {
        String stack = event.getStacktrace();
        detailArea.setText(stack != null ? stack : event.getMessage());

        FixSuggester.SuggestionResult suggestion = fixSuggester.suggest(event);

        StringBuilder diag = new StringBuilder();

        if (suggestion.patternResults != null && !suggestion.patternResults.isEmpty()) {
            DiagnosisEngine.DiagnosisResult top = suggestion.patternResults.get(0);
            ErrorPattern pattern = top.getPattern();

            String sourceLabel = suggestion.source.equals("custom_patterns") ? "CUSTOM PATTERN" : "PATTERN MATCH";
            diag.append("=== ").append(sourceLabel).append(" (").append(Math.round(top.getConfidence() * 100)).append("%) ===\n");
            diag.append("Pattern: ").append(pattern.getName()).append("\n");
            diag.append("Severity: ").append(pattern.getSeverity().toUpperCase()).append("\n");
            diag.append("Matched on: ").append(top.getMatchedOn()).append("\n\n");

            if (top.getCausalChain() != null && !top.getCausalChain().isEmpty()) {
                diag.append(top.getCausalChain()).append("\n");
            }

            diag.append("--- Diagnosis ---\n").append(pattern.getDiagnosis()).append("\n\n");
            diag.append("--- Suggested Fix ---\n").append(pattern.getSuggestedFix());

            if (pattern.getAutoFix() != null) {
                diag.append("\n\n[Auto-fix available] ").append(pattern.getAutoFix().getType());
            }
        }

        if (suggestion.llmResult != null && suggestion.llmResult.success) {
            if (diag.length() > 0) diag.append("\n\n");
            diag.append("=== LLM DIAGNOSIS (").append(suggestion.source.toUpperCase()).append(") ===\n\n");
            if (suggestion.llmResult.diagnosis != null) {
                diag.append("--- Diagnosis ---\n").append(suggestion.llmResult.diagnosis).append("\n\n");
            }
            if (suggestion.llmResult.suggestedFix != null) {
                diag.append("--- Suggested Fix ---\n").append(suggestion.llmResult.suggestedFix);
            }
        }

        if (diag.isEmpty()) {
            diag.append("No matching pattern found.\n");
            diag.append("This error is not yet in the ModMedic database.\n\n");
            if (llmClient.isEnabled()) {
                diag.append("LLM diagnosis was attempted but returned no result.\n");
                diag.append("Check your LLM configuration in Settings.");
            } else {
                diag.append("Enable LLM diagnosis in Settings (Dashboard tab) for AI-powered fallback.\n");
                diag.append("Or add a custom pattern via Pattern Manager.");
            }
        }

        diagnosisArea.setText(diag.toString());
    }

    private void updateLlmStatus(Label label) {
        if (settings.isLlmEnabled()) {
            label.setText("LLM: Enabled (" + settings.getLlmProvider() + ")");
            label.setTextFill(Color.LIGHTGREEN);
        } else {
            label.setText("LLM: Disabled");
            label.setTextFill(Color.GRAY);
        }
    }

    private void updateCount() {
        countLabel.setText("Total errors captured: " + errorList.size());
    }
}

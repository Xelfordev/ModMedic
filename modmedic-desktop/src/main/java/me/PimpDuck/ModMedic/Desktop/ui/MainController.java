package me.PimpDuck.ModMedic.Desktop.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import me.PimpDuck.ModMedic.Desktop.engine.*;
import me.PimpDuck.ModMedic.Desktop.model.ErrorEvent;
import me.PimpDuck.ModMedic.Desktop.model.ErrorPattern;
import me.PimpDuck.ModMedic.Desktop.server.ModMedicServer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

public class MainController {

    private final ModMedicServer server;
    private final FixSuggester fixSuggester;
    private final DiagnosisEngine diagnosisEngine;
    private final PatternLoader patternLoader;
    private final CustomPatternStore customPatternStore;
    private final LlmClient llmClient;
    private final SettingsManager settings;
    private final ObservableList<ErrorEvent> errorList = FXCollections.observableArrayList();
    private final FilteredList<ErrorEvent> filteredErrors;
    private final ObservableList<String> logList = FXCollections.observableArrayList();

    private TableView<ErrorEvent> tableView;
    private TextArea detailArea;
    private TextArea diagnosisArea;
    private Label countLabel;
    private ListView<String> logView;
    private Label pluginCountLabel;
    private Label statusLabel;
    private Circle statusDot;
    private Label serverVerLabel;
    private int testCounter = 0;
    private boolean autoScroll = true;

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
        this.filteredErrors = new FilteredList<>(errorList, p -> true);
    }

    public Scene createScene(Stage stage) {
        BorderPane root = new BorderPane();

        // ---- Header ----
        HBox header = createHeader();
        root.setTop(header);

        // ---- Center: TabPane ----
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
                createErrorsTab(),
                createConsoleTab(),
                createDashboardTab()
        );
        root.setCenter(tabPane);

        // ---- Bottom: Status Bar ----
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/dark.css").toExternalForm());
        return scene;
    }

    private HBox createHeader() {
        HBox header = new HBox(12);
        header.setId("header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(0);
        Label title = new Label("ModMedic");
        title.setId("header-title");
        Label subtitle = new Label("Plugin Error Monitor");
        subtitle.setId("header-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusDot = new Circle();
        statusDot.getStyleClass().addAll("status-dot", "disconnected");
        statusLabel = new Label("Disconnected");
        statusLabel.getStyleClass().addAll("status-text", "disconnected");
        VBox statusBox = new VBox(0);
        statusBox.setAlignment(Pos.CENTER_RIGHT);
        Label statusTitle = new Label("Connection");
        statusTitle.getStyleClass().add("stat-label");
        HBox statusInner = new HBox(6, statusDot, statusLabel);
        statusInner.setAlignment(Pos.CENTER_LEFT);
        statusBox.getChildren().addAll(statusTitle, statusInner);

        VBox pluginBox = new VBox(0);
        pluginBox.setAlignment(Pos.CENTER_RIGHT);
        pluginCountLabel = new Label("0");
        pluginCountLabel.getStyleClass().add("stat-value");
        Label pluginTitle = new Label("Plugins");
        pluginTitle.getStyleClass().add("stat-label");
        pluginBox.getChildren().addAll(pluginTitle, pluginCountLabel);

        VBox srvBox = new VBox(0);
        srvBox.setAlignment(Pos.CENTER_RIGHT);
        serverVerLabel = new Label("—");
        serverVerLabel.getStyleClass().add("stat-value");
        Label srvTitle = new Label("Server");
        srvTitle.getStyleClass().add("stat-label");
        srvBox.getChildren().addAll(srvTitle, serverVerLabel);

        header.getChildren().addAll(titleBox, spacer, statusBox, pluginBox, srvBox);
        return header;
    }

    private Tab createErrorsTab() {
        BorderPane pane = new BorderPane();

        // Search bar
        TextField searchField = new TextField();
        searchField.getStyleClass().add("search-bar");
        searchField.setPromptText("Filter by plugin, error type, or message...");
        searchField.textProperty().addListener((obs, old, val) -> {
            String lower = val.toLowerCase().trim();
            filteredErrors.setPredicate(event -> {
                if (lower.isEmpty()) return true;
                return (event.getPlugin() != null && event.getPlugin().toLowerCase().contains(lower))
                    || (event.getErrorType() != null && event.getErrorType().toLowerCase().contains(lower))
                    || (event.getMessage() != null && event.getMessage().toLowerCase().contains(lower));
            });
        });

        HBox filterBar = new HBox(8, searchField);
        filterBar.setPadding(new Insets(8, 12, 4, 12));
        pane.setTop(filterBar);

        // SplitPane: table | detail+diagnosis
        SplitPane split = new SplitPane();
        split.setDividerPosition(0, 0.45);

        // Left: Error table
        VBox leftPane = new VBox(4);
        leftPane.setPadding(new Insets(4, 0, 4, 12));

        tableView = new TableView<>(filteredErrors);
        tableView.setTableMenuButtonVisible(false);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<ErrorEvent, String> timeCol = new TableColumn<>("Time");
        timeCol.setPrefWidth(130);
        timeCol.setCellValueFactory(data -> {
            long ts = data.getValue().getTimestamp();
            return new SimpleStringProperty(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(ts)));
        });

        TableColumn<ErrorEvent, String> pluginCol = new TableColumn<>("Plugin");
        pluginCol.setPrefWidth(130);
        pluginCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPlugin()));

        TableColumn<ErrorEvent, String> typeCol = new TableColumn<>("Error Type");
        typeCol.setPrefWidth(200);
        typeCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getErrorType()));

        tableView.getColumns().addAll(timeCol, pluginCol, typeCol);
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) showDetail(selected);
        });

        // Row factory for severity coloring
        tableView.setRowFactory(tv -> new TableRow<ErrorEvent>() {
            @Override
            protected void updateItem(ErrorEvent item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("severity-critical", "severity-high");
                if (item == null || empty) return;
                String type = item.getErrorType();
                if (type != null && type.toLowerCase().contains("nullpointer")) {
                    getStyleClass().add("severity-high");
                } else if (type != null && (type.toLowerCase().contains("exception")
                        || type.toLowerCase().contains("error"))) {
                    getStyleClass().add("severity-critical");
                }
            }
        });

        leftPane.getChildren().add(tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        // Right: Detail + Diagnosis
        VBox rightPane = new VBox(6);
        rightPane.setPadding(new Insets(4, 12, 4, 0));

        TabPane detailTabs = new TabPane();
        Tab stackTab = new Tab("Stacktrace");
        stackTab.setClosable(false);

        detailArea = new TextArea();
        detailArea.setEditable(false);
        detailArea.setPrefHeight(200);
        stackTab.setContent(detailArea);

        Tab diagTab = new Tab("Diagnosis");
        diagTab.setClosable(false);

        diagnosisArea = new TextArea();
        diagnosisArea.setEditable(false);
        diagnosisArea.setStyle("-fx-control-inner-background: #0d0e1a; -fx-text-fill: #bbddff;");

        // Copy button for diagnosis
        Button copyDiagBtn = new Button("Copy Diagnosis");
        copyDiagBtn.getStyleClass().addAll("button", "small", "secondary");
        copyDiagBtn.setOnAction(e -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            clipboard.setContent(new javafx.scene.input.ClipboardContent() {{
                putString(diagnosisArea.getText());
            }});
        });

        VBox diagBox = new VBox(4, diagnosisArea, copyDiagBtn);
        VBox.setVgrow(diagnosisArea, Priority.ALWAYS);
        diagTab.setContent(diagBox);

        detailTabs.getTabs().addAll(stackTab, diagTab);

        rightPane.getChildren().add(detailTabs);
        VBox.setVgrow(detailTabs, Priority.ALWAYS);

        split.getItems().addAll(leftPane, rightPane);
        pane.setCenter(split);

        Tab tab = new Tab("Errors");
        tab.setContent(pane);
        tab.setClosable(false);
        return tab;
    }

    private Tab createConsoleTab() {
        BorderPane pane = new BorderPane();

        // Toolbar
        HBox toolbar = new HBox(8);
        toolbar.setPadding(new Insets(8, 12, 4, 12));

        Button clearLogBtn = new Button("Clear");
        clearLogBtn.getStyleClass().addAll("button", "small", "secondary");
        clearLogBtn.setOnAction(e -> logList.clear());

        ToggleButton autoScrollBtn = new ToggleButton("Auto-scroll");
        autoScrollBtn.getStyleClass().addAll("button", "small", "secondary");
        autoScrollBtn.setSelected(true);
        autoScrollBtn.selectedProperty().addListener((obs, old, val) -> autoScroll = val);

        TextField logFilter = new TextField();
        logFilter.getStyleClass().add("search-bar");
        logFilter.setPromptText("Filter console...");
        ObservableList<String> filteredLog = FXCollections.observableArrayList();
        logFilter.textProperty().addListener((obs, old, val) -> {
            filteredLog.setAll(logList.filtered(l -> val.isEmpty() || l.toLowerCase().contains(val.toLowerCase())));
        });
        logList.addListener((javafx.collections.ListChangeListener<String>) c -> {
            filteredLog.setAll(logList.filtered(l -> logFilter.getText().isEmpty()
                    || l.toLowerCase().contains(logFilter.getText().toLowerCase())));
        });

        toolbar.getChildren().addAll(clearLogBtn, autoScrollBtn, logFilter);
        HBox.setHgrow(logFilter, Priority.ALWAYS);
        pane.setTop(toolbar);

        // Console view
        logView = new ListView<>(filteredLog);
        logView.setStyle("-fx-font-family: Consolas; -fx-font-size: 11px; -fx-background-color: #0d0e1a;");

        // Auto-scroll: keep track of last item and scroll to it
        filteredLog.addListener((javafx.collections.ListChangeListener<String>) c -> {
            if (autoScroll && !filteredLog.isEmpty()) {
                Platform.runLater(() -> logView.scrollTo(filteredLog.size() - 1));
            }
        });

        pane.setCenter(logView);

        Tab tab = new Tab("Console");
        tab.setContent(pane);
        tab.setClosable(false);
        return tab;
    }

    private Tab createDashboardTab() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(20));

        Label dashTitle = new Label("Dashboard");
        dashTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        // Stats cards row
        HBox cards = new HBox(16);

        VBox errorCard = createStatCard("Errors Captured", "0", "Total since app started", "#6c63ff");
        countLabel = (Label) errorCard.getProperties().get("value");

        VBox pluginCard = createStatCard("Plugins", "0", "Registered on server", "#4ade80");
        pluginCountLabel = (Label) pluginCard.getProperties().get("value");

        VBox patternCard = createStatCard("Patterns", String.valueOf(diagnosisEngine.getPatterns().size()), "Built-in error patterns", "#fbbf24");

        VBox llmCard = createStatCard("LLM", settings.isLlmEnabled() ? settings.getLlmProvider() : "Disabled", settings.isLlmEnabled() ? "AI fallback active" : "Enable in Settings", settings.isLlmEnabled() ? "#4ade80" : "#666");

        cards.getChildren().addAll(errorCard, pluginCard, patternCard, llmCard);

        // Actions
        Label actionsTitle = new Label("Actions");
        actionsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        HBox actions = new HBox(10);
        Button settingsBtn = new Button("⚙ Settings");
        settingsBtn.setOnAction(e -> {
            SettingsDialog dialog = new SettingsDialog(settings, llmClient);
            dialog.showAndWait();
            updateLlmCard(llmCard);
        });

        Button patternsBtn = new Button("▦ Pattern Manager");
        patternsBtn.setOnAction(e -> {
            PatternManagerDialog dialog = new PatternManagerDialog(customPatternStore);
            dialog.showAndWait();
        });

        Button testBtn = new Button("▶ Generate Test Error");
        testBtn.getStyleClass().add("button");
        testBtn.setOnAction(e -> generateTestEvent());

        actions.getChildren().addAll(settingsBtn, patternsBtn, testBtn);

        box.getChildren().addAll(dashTitle, cards, actionsTitle, actions);

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Tab tab = new Tab("Dashboard");
        tab.setContent(scroll);
        tab.setClosable(false);
        return tab;
    }

    private VBox createStatCard(String title, String value, String sub, String color) {
        VBox card = new VBox(4);
        card.getStyleClass().add("card");
        card.setPrefWidth(180);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("card-title");

        Label valueLbl = new Label(value);
        valueLbl.getStyleClass().add("card-value");
        valueLbl.setStyle("-fx-text-fill: " + color + ";");
        card.getProperties().put("value", valueLbl);

        Label subLbl = new Label(sub);
        subLbl.getStyleClass().add("card-sub");

        card.getChildren().addAll(titleLbl, valueLbl, subLbl);
        return card;
    }

    private HBox createStatusBar() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("status-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label errorsLabel = new Label("Errors: 0");
        errorsLabel.getStyleClass().add("stat-label");
        errorList.addListener((javafx.collections.ListChangeListener<ErrorEvent>) c ->
                errorsLabel.setText("Errors: " + errorList.size()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button clearBtn = new Button("Clear Errors");
        clearBtn.getStyleClass().addAll("button", "small", "secondary", "danger");
        clearBtn.setOnAction(e -> {
            errorList.clear();
            detailArea.clear();
            diagnosisArea.clear();
        });

        bar.getChildren().addAll(errorsLabel, spacer, clearBtn);
        return bar;
    }

    // ---- Public API (called from ModMedicDesktop) ----

    public void onErrorReceived(ErrorEvent event) {
        Platform.runLater(() -> {
            errorList.add(0, event);
            if (event.getServerVersion() != null && !event.getServerVersion().equals("?")) {
                serverVerLabel.setText(event.getServerVersion());
            }
            countLabel.setText(String.valueOf(errorList.size()));
        });
    }

    public void onLogReceived(String line) {
        Platform.runLater(() -> {
            logList.add(line);
            int maxLog = settings.getMaxLogLines();
            while (logList.size() > maxLog) {
                logList.remove(0);
            }
        });
    }

    public void setServerStatus(boolean running) {
        Platform.runLater(() -> {
            if (running) {
                statusLabel.setText("Connected");
                statusLabel.getStyleClass().setAll("status-text", "connected");
                statusDot.getStyleClass().setAll("status-dot", "connected");
            } else {
                statusLabel.setText("Disconnected");
                statusLabel.getStyleClass().setAll("status-text", "disconnected");
                statusDot.getStyleClass().setAll("status-dot", "disconnected");
            }
        });
    }

    public void setPluginCount(int count) {
        Platform.runLater(() -> pluginCountLabel.setText(String.valueOf(count)));
    }

    // ---- Internal ----

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

            String sourceLabel = switch (suggestion.source) {
                case "custom_patterns" -> "CUSTOM PATTERN";
                default -> "PATTERN MATCH";
            };
            int pct = (int) Math.round(top.getConfidence() * 100);
            String bar = "█".repeat(pct / 10) + "░".repeat(10 - pct / 10);

            diag.append("╔══ ").append(sourceLabel).append(" ═══════════════════════════\n");
            diag.append("║ Pattern:  ").append(pattern.getName()).append("\n");
            diag.append("║ Severity: ").append(pattern.getSeverity().toUpperCase()).append("\n");
            diag.append("║ Match:    ").append(bar).append(" ").append(pct).append("%\n");
            diag.append("║ Matched:  ").append(top.getMatchedOn()).append("\n");

            if (top.getCausalChain() != null && !top.getCausalChain().isEmpty()) {
                diag.append("║\n").append(top.getCausalChain().indent(2));
            }

            diag.append("\n── Diagnosis ──────────────────────\n");
            diag.append(pattern.getDiagnosis()).append("\n\n");
            diag.append("── Suggested Fix ──────────────────\n");
            diag.append(pattern.getSuggestedFix()).append("\n");

            if (pattern.getAutoFix() != null) {
                diag.append("\n[⚡ Auto-fix] ").append(pattern.getAutoFix().getType());
            }
        }

        if (suggestion.llmResult != null && suggestion.llmResult.success) {
            if (diag.length() > 0) diag.append("\n\n");
            diag.append("╔══ LLM DIAGNOSIS ════════════════════════\n");
            if (suggestion.llmResult.diagnosis != null) {
                diag.append("── Diagnosis ──────────────────────\n");
                diag.append(suggestion.llmResult.diagnosis).append("\n\n");
            }
            if (suggestion.llmResult.suggestedFix != null) {
                diag.append("── Suggested Fix ──────────────────\n");
                diag.append(suggestion.llmResult.suggestedFix).append("\n");
            }
        }

        if (diag.isEmpty()) {
            diag.append("No matching pattern found.\n");
            diag.append("This error is not yet in the ModMedic database.\n\n");
            if (llmClient.isEnabled()) {
                diag.append("LLM diagnosis was attempted but returned no result.\n");
                diag.append("Check your LLM configuration in Dashboard → Settings.");
            } else {
                diag.append("Enable LLM diagnosis in Dashboard → Settings ");
                diag.append("for AI-powered fallback analysis.\n");
                diag.append("Or add a custom pattern via Pattern Manager.");
            }
        }

        diagnosisArea.setText(diag.toString());
    }

    private void updateLlmCard(VBox card) {
        Label val = (Label) card.getProperties().get("value");
        Label sub = (Label) card.getChildren().get(2);
        if (settings.isLlmEnabled()) {
            val.setText(settings.getLlmProvider());
            val.setStyle("-fx-text-fill: #4ade80;");
            sub.setText("AI fallback active");
        } else {
            val.setText("Disabled");
            val.setStyle("-fx-text-fill: #666;");
            sub.setText("Enable in Settings");
        }
    }
}

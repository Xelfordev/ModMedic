package me.PimpDuck.ModMedic.Desktop.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import me.PimpDuck.ModMedic.Desktop.engine.LlmClient;
import me.PimpDuck.ModMedic.Desktop.engine.SettingsManager;

public class SettingsDialog {

    private final SettingsManager settings;
    private final LlmClient llm;

    public SettingsDialog(SettingsManager settings, LlmClient llm) {
        this.settings = settings;
        this.llm = llm;
    }

    public void showAndWait() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("ModMedic Settings");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label header = new Label("Settings");
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));

        // LLM toggle
        CheckBox enableLlm = new CheckBox("Enable LLM diagnosis fallback");
        enableLlm.setSelected(settings.isLlmEnabled());

        // Provider choice
        Label providerLabel = new Label("LLM Provider:");
        ToggleGroup providerGroup = new ToggleGroup();
        RadioButton ollamaRadio = new RadioButton("Ollama (local)");
        ollamaRadio.setToggleGroup(providerGroup);
        RadioButton openaiRadio = new RadioButton("OpenAI (API)");
        openaiRadio.setToggleGroup(providerGroup);
        if (settings.getLlmProvider().equalsIgnoreCase("OPENAI")) {
            openaiRadio.setSelected(true);
        } else {
            ollamaRadio.setSelected(true);
        }

        // Ollama settings
        Label ollamaSection = new Label("Ollama Settings");
        ollamaSection.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        TextField ollamaUrlField = new TextField(settings.getOllamaUrl());
        ollamaUrlField.setPromptText("http://localhost:11434/api/generate");
        Label ollamaUrlLabel = new Label("Ollama API URL:");

        TextField ollamaModelField = new TextField(settings.getOllamaModel());
        ollamaModelField.setPromptText("llama3.2");
        Label ollamaModelLabel = new Label("Model name:");

        // OpenAI settings
        Label openaiSection = new Label("OpenAI Settings");
        openaiSection.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        PasswordField openaiKeyField = new PasswordField();
        openaiKeyField.setText(settings.getOpenaiKey());
        openaiKeyField.setPromptText("sk-...");
        Label openaiKeyLabel = new Label("API Key:");

        TextField openaiModelField = new TextField(settings.getOpenaiModel());
        openaiModelField.setPromptText("gpt-4o-mini");
        Label openaiModelLabel = new Label("Model:");

        // Info text
        Label infoLabel = new Label("Note: Changes take effect immediately after saving.");
        infoLabel.setStyle("-fx-text-fill: #888;");

        // Buttons
        HBox buttons = new HBox(10);
        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");
        buttons.getChildren().addAll(saveBtn, cancelBtn);

        VBox ollamaBox = new VBox(5, ollamaUrlLabel, ollamaUrlField, ollamaModelLabel, ollamaModelField);
        ollamaBox.setPadding(new Insets(0, 0, 0, 20));

        VBox openaiBox = new VBox(5, openaiKeyLabel, openaiKeyField, openaiModelLabel, openaiModelField);
        openaiBox.setPadding(new Insets(0, 0, 0, 20));

        // Toggle visibility based on provider
        ollamaUrlField.visibleProperty().bind(ollamaRadio.selectedProperty());
        ollamaUrlLabel.visibleProperty().bind(ollamaRadio.selectedProperty());
        ollamaModelField.visibleProperty().bind(ollamaRadio.selectedProperty());
        ollamaModelLabel.visibleProperty().bind(ollamaRadio.selectedProperty());
        ollamaSection.visibleProperty().bind(ollamaRadio.selectedProperty());

        openaiKeyField.visibleProperty().bind(openaiRadio.selectedProperty());
        openaiKeyLabel.visibleProperty().bind(openaiRadio.selectedProperty());
        openaiModelField.visibleProperty().bind(openaiRadio.selectedProperty());
        openaiModelLabel.visibleProperty().bind(openaiRadio.selectedProperty());
        openaiSection.visibleProperty().bind(openaiRadio.selectedProperty());

        saveBtn.setOnAction(e -> {
            settings.setLlmEnabled(enableLlm.isSelected());
            settings.setLlmProvider(openaiRadio.isSelected() ? "OPENAI" : "OLLAMA");
            settings.setOllamaUrl(ollamaUrlField.getText());
            settings.setOllamaModel(ollamaModelField.getText());
            settings.setOpenaiKey(openaiKeyField.getText());
            settings.setOpenaiModel(openaiModelField.getText());
            settings.applyTo(llm);
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());

        root.getChildren().addAll(header, enableLlm, providerLabel, ollamaRadio, openaiRadio,
                ollamaSection, ollamaBox, openaiSection, openaiBox, infoLabel, buttons);

        Scene scene = new Scene(root, 500, 600);
        scene.getStylesheets().add(getClass().getResource("/dark.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
    }
}

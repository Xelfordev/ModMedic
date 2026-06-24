package me.PimpDuck.ModMedic.Desktop.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import me.PimpDuck.ModMedic.Desktop.engine.CustomPatternStore;
import me.PimpDuck.ModMedic.Desktop.model.ErrorPattern;

public class PatternManagerDialog {

    private final CustomPatternStore store;
    private final ObservableList<ErrorPattern> patternList = FXCollections.observableArrayList();

    public PatternManagerDialog(CustomPatternStore store) {
        this.store = store;
    }

    public void showAndWait() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("ModMedic — Custom Patterns");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        Label header = new Label("Custom Error Patterns");
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        root.setTop(header);

        VBox center = new VBox(10);
        center.setPadding(new Insets(10, 0, 10, 0));

        // Table of custom patterns
        TableView<ErrorPattern> table = new TableView<>();
        table.setItems(patternList);

        TableColumn<ErrorPattern, String> idCol = new TableColumn<>("ID");
        idCol.setPrefWidth(120);
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getId()));

        TableColumn<ErrorPattern, String> nameCol = new TableColumn<>("Name");
        nameCol.setPrefWidth(200);
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));

        TableColumn<ErrorPattern, String> severityCol = new TableColumn<>("Severity");
        severityCol.setPrefWidth(80);
        severityCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSeverity()));

        table.getColumns().addAll(idCol, nameCol, severityCol);

        // Buttons
        HBox btnBox = new HBox(10);
        Button addBtn = new Button("Add Pattern");
        Button removeBtn = new Button("Remove Selected");
        Button closeBtn = new Button("Close");

        removeBtn.setOnAction(e -> {
            ErrorPattern selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                store.removePattern(selected.getId());
                refreshList();
            }
        });

        addBtn.setOnAction(e -> {
            showPatternEditor(null, stage);
        });

        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                ErrorPattern selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showPatternEditor(selected, stage);
                }
            }
        });

        closeBtn.setOnAction(e -> stage.close());

        btnBox.getChildren().addAll(addBtn, removeBtn, closeBtn);
        center.getChildren().addAll(table, btnBox);
        root.setCenter(center);

        Scene scene = new Scene(root, 600, 500);
        scene.getStylesheets().add(getClass().getResource("/dark.css").toExternalForm());
        stage.setScene(scene);

        refreshList();
        stage.showAndWait();
    }

    private void refreshList() {
        patternList.setAll(store.getCustomPatterns());
    }

    private void showPatternEditor(ErrorPattern existing, Stage parent) {
        Stage editor = new Stage();
        editor.initModality(Modality.APPLICATION_MODAL);
        editor.initOwner(parent);
        editor.setTitle(existing == null ? "Add Pattern" : "Edit Pattern");

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        TextField idField = new TextField(existing != null ? existing.getId() : "");
        idField.setPromptText("my-pattern-id");
        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        nameField.setPromptText("Pattern name");
        ComboBox<String> severityBox = new ComboBox<>(
                FXCollections.observableArrayList("low", "medium", "high", "critical"));
        severityBox.setValue(existing != null ? existing.getSeverity() : "medium");

        TextField errorTypeField = new TextField(
                existing != null && existing.getMatch() != null ? existing.getMatch().getErrorType() : "");
        errorTypeField.setPromptText("e.g. NullPointerException");
        TextField stacktraceField = new TextField(
                existing != null && existing.getMatch() != null ? existing.getMatch().getStacktraceContains() : "");
        stacktraceField.setPromptText("e.g. getConfig().getString");
        TextField messageField = new TextField(
                existing != null && existing.getMatch() != null ? existing.getMatch().getMessageContains() : "");
        messageField.setPromptText("e.g. Cannot invoke");

        TextArea diagnosisArea = new TextArea(existing != null ? existing.getDiagnosis() : "");
        diagnosisArea.setPrefHeight(60);
        diagnosisArea.setPromptText("What caused the error?");
        TextArea fixArea = new TextArea(existing != null ? existing.getSuggestedFix() : "");
        fixArea.setPrefHeight(60);
        fixArea.setPromptText("How to fix it?");

        Label idLabel = new Label("ID (unique):");
        Label nameLabel = new Label("Name:");
        Label severityLabel = new Label("Severity:");
        Label matchLabel = new Label("Match criteria");
        matchLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        Label errorTypeLabel = new Label("Error type (optional):");
        Label stacktraceLabel = new Label("Stacktrace contains (optional):");
        Label messageLabel = new Label("Message contains (optional):");
        Label diagLabel = new Label("Diagnosis:");
        diagLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        Label fixLabel = new Label("Suggested fix:");
        fixLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        Button saveBtn = new Button("Save");
        saveBtn.setOnAction(e -> {
            ErrorPattern pattern = new ErrorPattern();

            ErrorPattern.MatchCriteria match = new ErrorPattern.MatchCriteria();
            match.setErrorType(errorTypeField.getText().isEmpty() ? null : errorTypeField.getText());
            match.setStacktraceContains(stacktraceField.getText().isEmpty() ? null : stacktraceField.getText());
            match.setMessageContains(messageField.getText().isEmpty() ? null : messageField.getText());

            pattern.setId(idField.getText().isEmpty() ? "custom-" + System.currentTimeMillis() : idField.getText());
            pattern.setName(nameField.getText());
            pattern.setSeverity(severityBox.getValue());
            pattern.setMatch(match);
            pattern.setDiagnosis(diagnosisArea.getText());
            pattern.setSuggestedFix(fixArea.getText());
            pattern.setAutoFix(null);

            store.addPattern(pattern);
            refreshList();
            editor.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> editor.close());

        HBox btnBox = new HBox(10, saveBtn, cancelBtn);

        root.getChildren().addAll(
                idLabel, idField, nameLabel, nameField, severityLabel, severityBox,
                matchLabel, errorTypeLabel, errorTypeField,
                stacktraceLabel, stacktraceField, messageLabel, messageField,
                diagLabel, diagnosisArea, fixLabel, fixArea, btnBox);

        Scene scene = new Scene(root, 500, 600);
        scene.getStylesheets().add(getClass().getResource("/dark.css").toExternalForm());
        editor.setScene(scene);
        editor.showAndWait();
    }
}

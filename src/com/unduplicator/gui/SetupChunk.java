package com.unduplicator.gui;

import com.unduplicator.FindDuplicatesTask;
import com.unduplicator.ResourcesProvider;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents gui entity that is responsible for task set up.
 * <p>Created by MontolioV on 30.08.17.
 */
public class SetupChunk extends AbstractGUIChunk {
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();

    private List<File> chosenFiles = new ArrayList<>();
    private String chosenAlgorithm;

    private Button startButton = new Button();
    private Button addDirectoryButton = new Button();
    private Button addFileButton = new Button();
    private Button clearDirsButton = new Button();

    private Label headerLabel = new Label();
    private Label algorithmLabel = new Label();

    public SetupChunk(Stage mainStage) {
        setSelfNode(makePane(mainStage));
        updateLocaleContent();
    }

    /**
     * Updates gui local dependent elements to current local settings.
     */
    @Override
    public void updateLocaleContent() {
        startButton.setText(resProvider.getStrFromGUIBundle("startButton"));
        addDirectoryButton.setText(resProvider.getStrFromGUIBundle("addDirectoryButton"));
        addFileButton.setText(resProvider.getStrFromGUIBundle("addFileButton"));
        clearDirsButton.setText(resProvider.getStrFromGUIBundle("clearDirsButton"));

        headerLabel.setText(resProvider.getStrFromGUIBundle("headerLabel"));
        algorithmLabel.setText(resProvider.getStrFromGUIBundle("algorithmLabel"));

    }

    /**
     * May change state depending on received state.
     *
     * @param newState
     * @return <code>true</code> if new state differs, otherwise <code>false</code>
     */
    @Override
    public boolean changeState(GuiStates newState) {
        if (super.changeState(newState)) {
            switch (newState) {

                case NO_RESULTS:
                    startButton.setDisable(false);
                    break;
                case RUNNING:
                    startButton.setDisable(true);
                    break;
                case HAS_RESULTS:
                    startButton.setDisable(false);
                    break;
            }
            return true;
        } else {
            return false;
        }
    }

    private BorderPane makePane(Stage mainStage) {
        //Display
        VBox innerBox = new VBox(headerLabel);
        innerBox.setAlignment(Pos.CENTER);
        Label showDirLabel = new Label("");
        showDirLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox displayPanel = new VBox(10,
                innerBox,
                new Separator(),
                showDirLabel);

        //Setting buttons
        ComboBox<String> algorithmCB = new ComboBox<>(FXCollections.observableArrayList(
                "MD5", "SHA-1", "SHA-256"));
        algorithmCB.setOnAction(event -> chosenAlgorithm = algorithmCB.getValue());
        algorithmCB.getSelectionModel().selectLast();

        addDirectoryButton.setOnAction(event -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            File dir = dirChooser.showDialog(mainStage);
            if (dir != null) {
                String tmp = showDirLabel.getText();
                showDirLabel.setText(tmp + dir.toString() + "\n");
                chosenFiles.add(dir);
            }
        });
        addFileButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(mainStage);
            if (file != null) {
                String tmp = showDirLabel.getText();
                showDirLabel.setText(tmp + file.toString() + "\n");
                chosenFiles.add(file);
            }
        });
        clearDirsButton.setOnAction(event -> {
            chosenFiles.clear();
            showDirLabel.setText("");
        });

        algorithmCB.setMaxWidth(Double.MAX_VALUE);
        addDirectoryButton.setMaxWidth(Double.MAX_VALUE);
        addFileButton.setMaxWidth(Double.MAX_VALUE);
        clearDirsButton.setMaxWidth(Double.MAX_VALUE);

        VBox buttonsPane = new VBox(10);
        buttonsPane.getChildren().addAll(
                algorithmLabel,
                algorithmCB,
                new Separator(),
                addDirectoryButton,
                addFileButton,
                clearDirsButton);
        HBox controlPanel = new HBox(10,
                buttonsPane,
                new Separator(Orientation.VERTICAL));
        controlPanel.setPadding(new Insets(0, 10, 20, 0));

        //Start button
        startButton.setDisable(true);
        startButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(displayPanel);
        borderPane.setLeft(controlPanel);
        borderPane.setBottom(startButton);
        borderPane.setPadding(new Insets(20));

        return borderPane;
    }

    protected void setStartButtonHandler(EventHandler<ActionEvent> eventHandler) {
        startButton.setOnAction(eventHandler);
        startButton.setDisable(false);
    }

    protected FindDuplicatesTask getTask () {
        return new FindDuplicatesTask(chosenFiles, chosenAlgorithm);
    }
}

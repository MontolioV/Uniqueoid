package com.unduplicator.gui;

import com.unduplicator.FindDuplicatesTask;
import com.unduplicator.ResourcesProvider;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents gui entity that is responsible for task set up.
 * <p>Created by MontolioV on 30.08.17.
 */
public class SetupChunk extends AbstractGUIChunk {
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();
    private Stage mainStage;

    private List<File> chosenFiles = new ArrayList<>();
    private String chosenAlgorithm = "SHA-256";

    private Button addDirectoryButton = new Button();
    private Button addFileButton = new Button();
    private Button clearDirsButton = new Button();
    private Button startButton = new Button();
    private Button addToResultsButton = new Button();

    private Label headerLabel = new Label();
    private Label algorithmLabel = new Label();

    public SetupChunk(Stage mainStage) {
        this.mainStage = mainStage;
        setSelfNode(makePane());
        updateLocaleContent();
    }

    /**
     * Updates gui local dependent elements to current local settings.
     */
    @Override
    public void updateLocaleContent() {
        startButton.setText(resProvider.getStrFromGUIBundle("startButton"));
        addToResultsButton.setText((resProvider.getStrFromGUIBundle("addToResultsButton")));
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
                    addToResultsButton.setDisable(true);
                    break;
                case RUNNING:
                    startButton.setDisable(true);
                    addToResultsButton.setDisable(true);
                    break;
                case HAS_RESULTS:
                    startButton.setDisable(false);
                    addToResultsButton.setDisable(false);
                    break;
            }
            return true;
        } else {
            return false;
        }
    }

    private Node makePane() {
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
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setPrefHeight(40);
        //AddToResults button
        addToResultsButton.setDisable(true);
        addToResultsButton.setMaxWidth(Double.MAX_VALUE);
        addToResultsButton.setPrefHeight(40);

        GridPane bottomGridPane = new GridPane();
        ColumnConstraints cCons0 = new ColumnConstraints();
        ColumnConstraints cCons1 = new ColumnConstraints();
        ColumnConstraints cCons2 = new ColumnConstraints();
        cCons0.setPercentWidth(3);
        cCons1.setPercentWidth(94);
        cCons2.setPercentWidth(3);
        bottomGridPane.getColumnConstraints().addAll(cCons0, cCons1, cCons2);
        bottomGridPane.add(startButton, 0, 0, 2, 1);
        bottomGridPane.add(addToResultsButton, 1, 1, 2, 1);
        bottomGridPane.setVgap(5);


        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(displayPanel);
        borderPane.setLeft(controlPanel);
        borderPane.setBottom(bottomGridPane);
        borderPane.setPadding(new Insets(20));

        addPictureOnBackground(displayPanel);
        return borderPane;
    }

    private void addPictureOnBackground(Region region) {
        String imgURI = "";
        try {
            URL picURL = getClass().getClassLoader().getResource("img/dudes.png");
            if (picURL != null) {
                imgURI = picURL.toURI().toString();
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        if (imgURI.equals("")) {
            imgURI = new File("img/dudes.png").toURI().toString();
        }
        region.setStyle("-fx-background-image: url(" + imgURI + "); " +
                "-fx-background-position: RIGHT BOTTOM; " +
                "-fx-background-repeat: NO-REPEAT; " +
                "-fx-background-size: CONTAIN");
    }

    protected void setStartButtonHandler(EventHandler<ActionEvent> eventHandler) {
        startButton.setOnAction(eventHandler);
        startButton.setDisable(false);
    }
    protected void setAddToResultsButtonHandler(EventHandler<ActionEvent> eventHandler) {
        addToResultsButton.setOnAction(eventHandler);
    }

    protected FindDuplicatesTask getStartTask() {
        return new FindDuplicatesTask(chosenFiles, chosenAlgorithm);
    }
    protected FindDuplicatesTask getAddToResultsTask(Map<String, Set<File>> previousResult) {
        return new FindDuplicatesTask(chosenFiles, chosenAlgorithm, previousResult);
    }
}

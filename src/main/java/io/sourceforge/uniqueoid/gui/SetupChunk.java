package io.sourceforge.uniqueoid.gui;

import io.sourceforge.uniqueoid.GlobalFiles;
import io.sourceforge.uniqueoid.ResourcesProvider;
import io.sourceforge.uniqueoid.logic.FindDuplicatesTask;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
    private ChunkManager chunkManager;

    private boolean canStart;

    private List<File> chosenFiles = new ArrayList<>();

    private Button addDirectoryButton = new Button();
    private Button addFileButton = new Button();
    private Button clearDirsButton = new Button();
    private Button startButton = new Button();
    private Button addToResultsButton = new Button();

    private Label headerLabel = new Label();
    private Label algorithmLabel = new Label();
    private Label showDirLabel = new Label("");

    public SetupChunk(Stage mainStage, ChunkManager chunkManager) {
        this.mainStage = mainStage;
        this.chunkManager = chunkManager;
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

    @Override
    public boolean changeState(GuiStates newState) {
        super.changeState(newState);

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

        if (!canStart) {
            startButton.setDisable(true);
            addToResultsButton.setDisable(true);
        }

        return true;
    }

    private Node makePane() {
        //Display
        VBox innerBox = new VBox(headerLabel);
        innerBox.setAlignment(Pos.CENTER);
        showDirLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox displayPanel = new VBox(10,
                innerBox,
                new Separator(),
                showDirLabel);

        //Setting buttons
        addDirectoryButton.setOnAction(event -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setInitialDirectory(GlobalFiles.getInstance().getLastVisitedDir());
            File dir = dirChooser.showDialog(mainStage);
            showSelectedFile(dir);
        });
        addFileButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(GlobalFiles.getInstance().getLastVisitedDir());
            File file = fileChooser.showOpenDialog(mainStage);
            showSelectedFile(file);
        });
        clearDirsButton.setOnAction(event -> {
            chosenFiles.clear();
            showDirLabel.setText("");
            canStart = false;
            refreshState();
        });

        addDirectoryButton.setMaxWidth(Double.MAX_VALUE);
        addFileButton.setMaxWidth(Double.MAX_VALUE);
        clearDirsButton.setMaxWidth(Double.MAX_VALUE);

        VBox buttonsPane = new VBox(10);
        buttonsPane.getChildren().addAll(
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
            URL picURL = getClass().getClassLoader().getResource("io/sourceforge/uniqueoid/img/dudes.png");
            if (picURL != null) {
                imgURI = picURL.toURI().toString();
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
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
        return new FindDuplicatesTask(chosenFiles, chunkManager.getFindTaskSettings());
    }
    protected FindDuplicatesTask getAddToResultsTask(Map<String, Set<File>> previousResult) {
        return new FindDuplicatesTask(chosenFiles, chunkManager.getFindTaskSettings(), previousResult);
    }

    private void showSelectedFile(File file) {
        if (file != null) {
            String tmp = showDirLabel.getText();
            showDirLabel.setText(tmp + file.toString() + "\n");
            chosenFiles.add(file);
            if (file.isDirectory()) {
                GlobalFiles.getInstance().setLastVisitedDir(file);
            } else {
                GlobalFiles.getInstance().setLastVisitedDir(file.getParentFile());
            }
            canStart = true;
            refreshState();
        }
    }
}

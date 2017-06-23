package com.unduplicator;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * <p>Created by MontolioV on 20.06.17.
 */
public class GUI extends Application {
    private List<File> targetDirs = new ArrayList<>();
    private HashMap<String,List<File>> processedFilesHM = new HashMap<>();
    private FindDuplicatesTask task;

    private Stage mainStage;
    private Scene setupScene;
    private Scene runtimeScene;
    private Scene resultScene;

    private ProgressBar progressBar = new ProgressBar();
    private Button startBut = new Button("Пуск");
    private Button stopBut = new Button("Отмена");
    private TextArea messages = new TextArea();
    private Label poolStatus = new Label();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        mainStage = primaryStage;

        makeSetupScene();
        makeRuntimeScene();
        makeResultScene();

        primaryStage.setScene(setupScene);
        primaryStage.show();
    }

    private void makeSetupScene() {
        //Display
        Label headerLabel = new Label("Выбранные файлы и директории:");
        Label showDirLabel = new Label("");

        VBox innerBox = new VBox(headerLabel);
        innerBox.setAlignment(Pos.CENTER);

        showDirLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        VBox centralP = new VBox(10,
                innerBox,
                new Separator(),
                showDirLabel);

        //Setting buttons
        Label algorithmLabel = new Label("Хеш функция");
        ComboBox<String> algorithmCB = new ComboBox<>(FXCollections.observableArrayList(
                "MD5", "SHA-1", "SHA-256"));
        algorithmCB.getSelectionModel().selectLast();

        Button addDirectory = new Button("Добавить папку");
        addDirectory.setOnAction(event -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            File dir = dirChooser.showDialog(mainStage);
            if (dir != null) {
                String tmp = showDirLabel.getText();
                showDirLabel.setText(tmp + dir.toString() + "\n");
                targetDirs.add(dir);
            }
        });
        Button addFile = new Button("Добавить файл");
        addFile.setOnAction(event ->{
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(mainStage);
            if (file != null) {
                String tmp = showDirLabel.getText();
                showDirLabel.setText(tmp + file.toString() + "\n");
                targetDirs.add(file);
            }
        });
        Button clearDirs = new Button("Очистить");
        clearDirs.setOnAction(event -> {
            targetDirs = new ArrayList<>();
            showDirLabel.setText("");
        });

        algorithmCB.setMaxWidth(Double.MAX_VALUE);
        addDirectory.setMaxWidth(Double.MAX_VALUE);
        addFile.setMaxWidth(Double.MAX_VALUE);
        clearDirs.setMaxWidth(Double.MAX_VALUE);

        VBox buttonsPane = new VBox(10);
        buttonsPane.getChildren().addAll(
                algorithmLabel,
                algorithmCB,
                new Separator(),
                addDirectory,
                addFile,
                clearDirs);
        HBox leftP = new HBox(10,
                buttonsPane,
                new Separator(Orientation.VERTICAL));
        leftP.setPadding(new Insets(0, 10, 20, 0));

        //Start button
        startBut.setOnAction(event -> {
            messages.clear();
            processedFilesHM = null;
            task = new FindDuplicatesTask(targetDirs, algorithmCB.getValue());
            progressBar.progressProperty().bind(task.progressProperty());
            poolStatus.textProperty().bind(task.titleProperty());
            task.messageProperty().addListener((observable, oldValue, newValue) -> {
                messages.appendText("\n" + newValue);
            });
            task.stateProperty().addListener((observable, oldValue, newValue) -> {
                switch (newValue) {
                    case READY:
                        swapMode(true);
                        break;
                    case SCHEDULED:
                        swapMode(true);
                        break;
                    case RUNNING:
                        swapMode(true);
                        messages.appendText("\nВыполняется...");
                        break;
                    case SUCCEEDED:
                        swapMode(false);
                        messages.appendText("\nВыполнено успешно");
/*
                        if (processedFilesHM != null) {
                            for (Map.Entry<String, List<File>> entry : processedFilesHM.entrySet()) {
                                if (entry.getValue().size() > 1) {
                                    StringJoiner sj = new StringJoiner("\n");
                                    sj.add(entry.getKey());
                                    entry.getValue().forEach(file -> sj.add(file.toString()));
                                    messages.appendText("\n\n");
                                    messages.appendText(sj.toString());
                                }
                            }
                        }
*/
                        break;
                    case CANCELLED:
                        swapMode(false);
                        messages.appendText("\nОтменено");
                        break;
                    case FAILED:
                        swapMode(false);
                        messages.appendText("\nВозникла ошибка");
                        break;
                }
            });
            Thread taskThread = new Thread(() -> {
                try {
                    task.run();
                    processedFilesHM = task.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            });
            taskThread.start();

            mainStage.setScene(runtimeScene);
        });
        startBut.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(centralP);
        borderPane.setLeft(leftP);
        borderPane.setBottom(startBut);
        borderPane.setPadding(new Insets(20));
        setupScene = new Scene(borderPane, 700, 500);
    }

    private void makeRuntimeScene() {
        progressBar.setMaxWidth(Double.MAX_VALUE);
        poolStatus.setMaxWidth(Double.MAX_VALUE);
        messages.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        messages.setEditable(false);
        messages.setWrapText(true);

        stopBut.setOnAction(event -> {
            if (task != null) {
                task.cancel();
            }
        });
        HBox stopP = new HBox(stopBut);
        stopP.setAlignment(Pos.CENTER);

        VBox bottomP = new VBox(10,
                poolStatus,
                progressBar,
                stopP);
        bottomP.setPadding(new Insets(20, 0, 0, 0));

        BorderPane runtimeP = new BorderPane();
        runtimeP.setCenter(messages);
        runtimeP.setBottom(bottomP);
        runtimeP.setPadding(new Insets(20));

        runtimeScene = new Scene(runtimeP);
    }

    private void makeResultScene() {

    }

    private void swapMode(boolean isRunning) {
        if (isRunning) {
            startBut.setDisable(true);
            stopBut.setDisable(false);
        } else {
            startBut.setDisable(false);
            stopBut.setDisable(true);
            task = null;
        }
    }
}

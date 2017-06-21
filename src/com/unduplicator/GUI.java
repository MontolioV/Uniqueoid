package com.unduplicator;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
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
    FindDuplicatesTask task;

    ProgressBar progressBar = new ProgressBar();
    Button startBut = new Button("Пуск");
    Button stopBut = new Button("Отмена");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        FlowPane mainPane = new FlowPane(Orientation.VERTICAL, 10, 10);
        mainPane.setAlignment(Pos.CENTER);

        Label showDirectories = new Label("Выбранные директории:");

        Button addDirectory = new Button("Добавить папку");
        addDirectory.setOnAction(event -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            File dir = dirChooser.showDialog(primaryStage);
            if (dir != null) {
                String tmp = showDirectories.getText();
                showDirectories.setText(tmp + "\n" + dir.toString());
                targetDirs.add(dir);
            }
        });
        Button addFile = new Button("Добавить файл");
        addFile.setOnAction(event ->{
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                String tmp = showDirectories.getText();
                showDirectories.setText(tmp + "\n" + file.toString());
                targetDirs.add(file);
            }
        });
        Button clearDirs = new Button("Очистить");
        clearDirs.setOnAction(event -> {
            targetDirs = new ArrayList<>();
            showDirectories.setText("Выбранные директории:");
        });

        FlowPane addButtonsPane = new FlowPane(5, 5);
        addButtonsPane.getChildren().addAll(addDirectory, addFile, clearDirs);

        ComboBox<String> algorithmCB = new ComboBox<>(FXCollections.observableArrayList(
                                               "MD5", "SHA-1", "SHA-256"));


        progressBar.setMaxWidth(500);
        progressBar.setVisible(false);

        Label messagesLabel = new Label("Сообщения:");
        TextArea messages = new TextArea();
        messages.setEditable(false);
        messages.setWrapText(true);


        startBut.setOnAction(event -> {
            messages.clear();
            processedFilesHM = null;
            task = new FindDuplicatesTask(targetDirs, algorithmCB.getValue());
            progressBar.progressProperty().bind(task.progressProperty());
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
        });

        stopBut.setOnAction(event -> {
            if (task != null) {
                task.cancel();
            }
        });

        primaryStage.setScene(new Scene(mainPane, 700, 500));

        mainPane.getChildren().addAll(
                showDirectories,
                addButtonsPane,
                algorithmCB,
                progressBar,
                startBut,
                stopBut,
                messagesLabel,
                messages);

        primaryStage.show();
    }

    private void swapMode(boolean isRunning) {
        if (isRunning) {
            progressBar.setVisible(true);
            startBut.setDisable(true);
            stopBut.setDisable(false);
        } else {
            progressBar.setVisible(false);
            startBut.setDisable(false);
            stopBut.setDisable(true);
            task = null;
        }
    }
}

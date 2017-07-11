package com.unduplicator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Created by MontolioV on 20.06.17.
 */
public class GUI extends Application {
    private HashMap<String, List<File>> processedFilesHM = new HashMap<>();
    private FindDuplicatesTask task;
    private Set<File> filesToDelete = new HashSet<>();

    private Stage mainStage;
    private Scene setupScene;
    private Scene runtimeScene;
    private Scene resultScene;

    private ProgressBar progressBar = new ProgressBar();
    private Button startBut = new Button("Пуск");
    private Button stopBut = new Button("Отмена");
    private TextArea messages = new TextArea();
    private Label poolStatus = new Label();
    private ListView<String> chSumListView = new ListView<>();

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
        final List<File> targetDirs = new ArrayList<>();

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
        addFile.setOnAction(event -> {
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
            targetDirs.clear();
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
            long startTime = System.currentTimeMillis();
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
                        long durationMS = System.currentTimeMillis() - startTime;
                        swapMode(false);
                        messages.appendText("\nВыполнено успешно за " + millisToTimeStr(durationMS));
                        if (processedFilesHM != null) {
                            messages.appendText("\nНайдено дублирующихся файлов: " + processedFilesHM.size());
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
                    ObservableList<String> obsListChSum = FXCollections.observableArrayList(processedFilesHM.keySet());
                    chSumListView.setItems(obsListChSum);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
            taskThread.start();

            switchScene(runtimeScene);
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

        Button toSetupBut = new Button("<-Настройки");
        Button toResultBut = new Button("Результат->");

        toSetupBut.setOnAction(event -> switchScene(setupScene));
        toResultBut.setOnAction(event -> switchScene(resultScene));
        stopBut.setOnAction(event -> {
            if (task != null) {
                task.cancel();
            }
        });

        toSetupBut.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        toResultBut.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        stopBut.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        TilePane stopP = new TilePane(Orientation.HORIZONTAL, 10, 0,
                toSetupBut,
                stopBut,
                toResultBut);
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
        TilePane previewPane = new TilePane();
        previewPane.setPrefColumns(3);
        previewPane.setVgap(3);
        previewPane.setHgap(3);
        previewPane.setPadding(new Insets(5));
        ScrollPane previewScrP = new ScrollPane(previewPane);
        previewScrP.setFitToWidth(true);


        chSumListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        chSumListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            private Task<Void> prevTask;
            final double WIDTH = 200;
            final double HEIGHT = 200;

            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                String chSum = newValue;
                List<File> selectedFiles = processedFilesHM.get(chSum);

                if (prevTask != null) {
                    prevTask.cancel();
                }
                previewPane.getChildren().clear();

                prevTask = new Task<Void>() {
                    ArrayList<Task<Void>> tasks = new ArrayList<>();
                    AtomicBoolean stop = new AtomicBoolean();

                    @Override
                    protected Void call() throws Exception {
                        Executor pool = Executors.newFixedThreadPool(2, r -> {
                            Thread daemonThr = new Thread(r);
                            daemonThr.setDaemon(true);
                            return daemonThr;
                        });

                        for (File selectedFile : selectedFiles) {
                            if (isCancelled()) {
                                return null;
                            }

                            ImageView imageView = new ImageView();
                            ProgressIndicator prIndicator = new ProgressIndicator();
                            StackPane stackPane = new StackPane(prIndicator);

                            Task<Void> imgTask = new Task<Void>() {
                                final long MAX_IMG_SIZE = 52_500_000;

                                @Override
                                protected Void call() throws Exception {
                                    Button previewButton = new Button(selectedFile.getName(), imageView);
                                    previewButton.setMaxSize(WIDTH, HEIGHT);
                                    previewButton.setContentDisplay(ContentDisplay.TOP);
                                    previewButton.setOnAction(event1 -> {
                                        filesToDelete.remove(selectedFile);
                                        processedFilesHM.get(chSum).stream().
                                                filter(file -> !file.equals(selectedFile)).
                                                forEach(filesToDelete::add);
                                    });
                                    if (selectedFile.length() < MAX_IMG_SIZE) {
                                        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(selectedFile))) {
                                            Image image = new Image(bis, WIDTH, HEIGHT, true, true);
                                            if (!isCancelled()) {
                                                Platform.runLater(() -> {
                                                    prIndicator.progressProperty().bind(image.progressProperty());
                                                    stackPane.getChildren().add(previewButton);
                                                });
                                            } else {
                                                image.cancel();
                                            }
                                            imageView.setImage(image);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    return null;
                                }
                            };

                            pool.execute(imgTask);
                            tasks.add(imgTask);

                            if (!isCancelled()) {
                                Platform.runLater(() -> previewPane.getChildren().add(stackPane));
                            }
                        }
                        return null;
                    }

                    @Override
                    public boolean cancel(boolean mayInterruptIfRunning) {
                        boolean result = super.cancel(mayInterruptIfRunning);
                        stop.set(true);
                        tasks.forEach(Task::cancel);
                        return result;
                    }
                };

                Thread thread = new Thread(prevTask);
                thread.setDaemon(true);
                thread.start();
            }
        });

        //Main pane with list and preview
        GridPane centerGrid = new GridPane();
        centerGrid.setVgap(10);
        centerGrid.setHgap(10);
        ColumnConstraints cCons0 = new ColumnConstraints();
        ColumnConstraints cCons1 = new ColumnConstraints();
        cCons0.setPercentWidth(30);
        cCons1.setPercentWidth(70);
        centerGrid.getColumnConstraints().addAll(cCons0, cCons1);
        RowConstraints rCons0 = new RowConstraints();
        rCons0.setPercentHeight(100);
        centerGrid.getRowConstraints().setAll(rCons0);
        centerGrid.setPadding(new Insets(0, 0, 10, 0));
        centerGrid.add(chSumListView, 0, 0);
        centerGrid.add(previewScrP, 1, 0);

        Button delete = new Button("Удалить");
        delete.setOnAction(event -> {
            StringJoiner sj = new StringJoiner("\n");
            filesToDelete.forEach(f -> sj.add(f.toString()));
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Подтвердите удаление");
            alert.setHeaderText("Удалить указанные файлы?");
            alert.setContentText(sj.toString());
            alert.showAndWait();
        });

        BorderPane resultPane = new BorderPane();
        resultPane.setCenter(centerGrid);
        resultPane.setBottom(delete);
        resultPane.setPadding(new Insets(10));

        resultScene = new Scene(resultPane);
    }

    private void swapMode(boolean isRunning) {
        if (isRunning) {
            startBut.setDisable(true);
            stopBut.setDisable(false);
            progressBar.setVisible(true);
        } else {
            startBut.setDisable(false);
            stopBut.setDisable(true);
            progressBar.setVisible(false);
            task = null;
        }
    }

    private String millisToTimeStr(long millis) {
        String result;

        long ms = millis % 1000;
        long s = (millis / 1000) % 60;
        long m = (millis / (1000 * 60)) % 60;
        long h = (millis / (1000 * 60 * 60)) % 60;

        if (millis >= 1000 * 60 * 60) {
            result = String.format("%d ч %2d м %2d с %3d мс", h, m, s, ms);
        } else if (millis >= 1000 * 60) {
            result = String.format("%2d м %2d с %3d мс", m, s, ms);
        } else if (millis >= 1000) {
            result = String.format("%2d с %3d мс", s, ms);
        } else {
            result = String.format("%3d мс", ms);
        }
        return result;
    }

    private void switchScene(Scene newScene) {
        double height = mainStage.getHeight();
        double width = mainStage.getWidth();

        mainStage.setScene(newScene);
        mainStage.setHeight(height);
        mainStage.setWidth(width);
    }


}

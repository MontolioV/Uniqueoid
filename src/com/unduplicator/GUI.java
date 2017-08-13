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
import javafx.scene.Node;
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
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * <p>Created by MontolioV on 20.06.17.
 */
public class GUI extends Application {
//    private Locale locale = new Locale("ru", "RU");
    private Locale locale = new Locale("en", "EN");
    private ResourceBundle guiBundle;
    private ResourceBundle messagesBundle;
    private ResourceBundle exceptionBundle;

    private HashMap<String, List<File>> processedFilesHM = new HashMap<>();
    private FindDuplicatesTask task;
    private Set<File> filesToDelete = new HashSet<>();

    private Stage mainStage;
    private Scene setupScene;
    private Scene runtimeScene;
    private Scene resultScene;

    private ProgressBar progressBar = new ProgressBar();
    private Button startBut;
    private Button stopBut;
    private TextArea messagesTA = new TextArea();
    private Label poolStatus = new Label();
    private ListView<String> duplChSumListView = new ListView<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        super.init();

        File settings = new File(System.getProperty("user.dir") + "/settings.ser");
        String language;
        String country;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(settings))) {
            language = (String) ois.readObject();
            country = (String) ois.readObject();
            locale = new Locale(language, country);
        }catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        guiBundle = ResourceBundle.getBundle("com.resources.GUI_Bundle", locale);
        messagesBundle = ResourceBundle.getBundle("com.resources.Messages_Bundle", locale);
        exceptionBundle = ResourceBundle.getBundle("com.resources.Exception_Bundle", locale);
    }

    @Override
    public void stop() throws Exception {
        super.stop();

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
        startBut = new Button(guiBundle.getString("startButton"));

        //Display
        Label headerLabel = new Label(guiBundle.getString("headerLabel"));
        Label showDirLabel = new Label("");

        VBox innerBox = new VBox(headerLabel);
        innerBox.setAlignment(Pos.CENTER);

        showDirLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        VBox centralP = new VBox(10,
                innerBox,
                new Separator(),
                showDirLabel);

        //Setting buttons
        Label algorithmLabel = new Label(guiBundle.getString("algorithmLabel"));
        ComboBox<String> algorithmCB = new ComboBox<>(FXCollections.observableArrayList(
                "MD5", "SHA-1", "SHA-256"));
        algorithmCB.getSelectionModel().selectLast();

        Button addDirectory = new Button(guiBundle.getString("addDirectoryButton"));
        addDirectory.setOnAction(event -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            File dir = dirChooser.showDialog(mainStage);
            if (dir != null) {
                String tmp = showDirLabel.getText();
                showDirLabel.setText(tmp + dir.toString() + "\n");
                targetDirs.add(dir);
            }
        });
        Button addFile = new Button(guiBundle.getString("addFileButton"));
        addFile.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(mainStage);
            if (file != null) {
                String tmp = showDirLabel.getText();
                showDirLabel.setText(tmp + file.toString() + "\n");
                targetDirs.add(file);
            }
        });
        Button clearDirs = new Button(guiBundle.getString("clearDirsButton"));
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
            messagesTA.clear();
            processedFilesHM = null;
            task = new FindDuplicatesTask(targetDirs, algorithmCB.getValue(), messagesBundle, exceptionBundle);
            progressBar.progressProperty().bind(task.progressProperty());
            poolStatus.textProperty().bind(task.titleProperty());
            task.messageProperty().addListener((observable, oldValue, newValue) -> {
                messagesTA.appendText("\n" + newValue);
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
                        messagesTA.appendText("\n" + messagesBundle.getString("taskProcessing"));
                        break;
                    case SUCCEEDED:
                        long durationMS = System.currentTimeMillis() - startTime;
                        swapMode(false);
                        messagesTA.appendText("\n" + messagesBundle.getString("completeSuccessfully") + millisToTimeStr(durationMS));
                        if (processedFilesHM != null) {
                            int duplCount = 0;
                            for (Map.Entry<String, List<File>> entry : processedFilesHM.entrySet()) {
                                if (entry.getValue().size() > 1) {
                                    duplCount += entry.getValue().size() - 1;
                                }
                            }
                            messagesTA.appendText("\n" + messagesBundle.getString("foundDuplicates") + duplCount);
                        }
                        break;
                    case CANCELLED:
                        swapMode(false);
                        messagesTA.appendText("\n" + messagesBundle.getString("canceled"));
                        break;
                    case FAILED:
                        swapMode(false);
                        messagesTA.appendText("\n" + messagesBundle.getString("error"));
                        break;
                }
            });
            Thread taskThread = new Thread(() -> {
                try {
                    task.run();
                    processedFilesHM = task.get();
                    updDuplListView();
                } catch (InterruptedException | ExecutionException e) {
                    showException(e);
                }
            });
            taskThread.setDaemon(true);
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
        stopBut = new Button(guiBundle.getString("cancelButton"));

        progressBar.setMaxWidth(Double.MAX_VALUE);
        poolStatus.setMaxWidth(Double.MAX_VALUE);
        messagesTA.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        messagesTA.setEditable(false);
        messagesTA.setWrapText(true);

        Button toSetupBut = new Button(guiBundle.getString("setupButton"));
        Button toResultBut = new Button(guiBundle.getString("resultButton"));

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
        runtimeP.setCenter(messagesTA);
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

        BiConsumer<File, String> setFilesToDeletion = (selectedFile, chSum) -> {
            filesToDelete.remove(selectedFile);
            processedFilesHM.get(chSum).stream().
                    filter(file -> !file.equals(selectedFile)).
                    forEach(filesToDelete::add);
        };
        ObservableList<File> addressValues = FXCollections.observableArrayList();
        ListView<File> addressListLView = new ListView<>(addressValues);
        addressListLView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        addressListLView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            //ListView and Preview buttons are connected by index
            int boundIndex = addressListLView.getSelectionModel().getSelectedIndex();
            previewPane.getChildren().stream().filter(Node::isDisabled).forEach(node -> node.setDisable(false));
            if (boundIndex > -1) {
                previewPane.getChildren().get(boundIndex).setDisable(true);
            }
            setFilesToDeletion.accept(newValue, duplChSumListView.getSelectionModel().getSelectedItem());
        });

        duplChSumListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        duplChSumListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            private Task<Void> prevTask;
            final double WIDTH = 200;
            final double HEIGHT = 200;

            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                String chSum = newValue;
                List<File> selectedFiles = processedFilesHM.get(chSum);
                addressValues.clear();

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
                                    //ListView and Preview buttons are connected by index
                                    int indexInLV = addressValues.size();

                                    Platform.runLater(() -> addressValues.add(selectedFile));

                                    Button previewButton = new Button(selectedFile.getName(), imageView);
                                    previewButton.setMaxSize(WIDTH, HEIGHT);
                                    previewButton.setContentDisplay(ContentDisplay.TOP);
                                    previewButton.setOnAction(event1 -> {
                                        Platform.runLater(() -> {
                                            addressListLView.getSelectionModel().select(indexInLV);
                                            addressListLView.scrollTo(indexInLV);
                                        });
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
                                            showException(e);
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
        cCons0.setPercentWidth(20);
        cCons1.setPercentWidth(80);
        centerGrid.getColumnConstraints().addAll(cCons0, cCons1);
        RowConstraints rCons0 = new RowConstraints();
        RowConstraints rCons1 = new RowConstraints();
        RowConstraints rCons2 = new RowConstraints();
        rCons0.setPercentHeight(0);
        rCons1.setPercentHeight(70);
        rCons2.setPercentHeight(30);
        centerGrid.getRowConstraints().setAll(rCons0, rCons1, rCons2);
        centerGrid.setPadding(new Insets(0, 0, 10, 0));

        centerGrid.add(new Label(guiBundle.getString("hashLabel")), 0, 0);
        centerGrid.add(duplChSumListView, 0, 1);
        centerGrid.add(new Label(guiBundle.getString("previewLabel")), 1, 0);
        centerGrid.add(previewScrP, 1, 1);
        centerGrid.add(addressListLView,0,2,2,1);


        ProgressBar pbDel = new ProgressBar();
        pbDel.setManaged(false);
        pbDel.setMaxWidth(Double.MAX_VALUE);

        Button toSetupBut = new Button(guiBundle.getString("setupButton"));
        Button toRuntimeBut = new Button(guiBundle.getString("runtimeButton"));
        toSetupBut.setOnAction(event -> switchScene(setupScene));
        toRuntimeBut.setOnAction(event -> switchScene(runtimeScene));
        Button deleteButton = new Button(guiBundle.getString("deleteButton"));
        deleteButton.setOnAction(event -> {
            Function<Collection<File>, TextArea> colToTAFunction = files -> {
                StringJoiner sj = new StringJoiner("\n");
                files.forEach(f -> sj.add(f.toString()));
                return new TextArea(sj.toString());
            };

            DeleteFilesTask delTask = new DeleteFilesTask(exceptionBundle, new ArrayList<>(filesToDelete));

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(guiBundle.getString("delConformAlertTitle"));
            alert.setHeaderText(guiBundle.getString("delConformAlertBodyPart1") +
                                filesToDelete.size() +
                                guiBundle.getString("delConformAlertBodyPart2"));
            alert.getDialogPane().setExpandableContent(colToTAFunction.apply(filesToDelete));

            resizeAlertManually(alert);

            alert.showAndWait()
                    .filter(response -> response == ButtonType.OK)
                    .ifPresent(type -> {
                        Alert reportAlert = new Alert(Alert.AlertType.INFORMATION);
                        pbDel.progressProperty().bind(delTask.progressProperty());
                        pbDel.setManaged(true);
                        delTask.run();
                        try {
                            List<File> notDeletedList = delTask.get();
                            if (!notDeletedList.isEmpty()) {
                                reportAlert.setHeaderText(guiBundle.getString("reportAlertHeaderFail"));
                                reportAlert.getDialogPane().setExpandableContent(
                                        colToTAFunction.apply(notDeletedList));
                            } else {
                                reportAlert.setHeaderText(guiBundle.getString("reportAlertHeaderSuccess"));
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            showException(e);
                        }

                        updProcFilesHM();
                        updDuplListView();
                        pbDel.setManaged(false);
                        reportAlert.showAndWait();
                    });
        });

        HBox buttonsBox = new HBox(5,
                toSetupBut,
                deleteButton,
                toRuntimeBut);
        buttonsBox.setAlignment(Pos.CENTER);

        VBox bottomBox = new VBox(10,
                pbDel,
                buttonsBox);

        BorderPane resultPane = new BorderPane();
        resultPane.setCenter(centerGrid);
        resultPane.setBottom(bottomBox);
        resultPane.setPadding(new Insets(20));

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
            result = String.format(messagesBundle.getString("h_m_s_ms"), h, m, s, ms);
        } else if (millis >= 1000 * 60) {
            result = String.format(messagesBundle.getString("m_s_ms"), m, s, ms);
        } else if (millis >= 1000) {
            result = String.format(messagesBundle.getString("s_ms"), s, ms);
        } else {
            result = String.format(messagesBundle.getString("ms"), ms);
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

    private void updProcFilesHM() {
        processedFilesHM.forEach((s, files) ->{
            ArrayList<File> updatedList = new ArrayList<>();
            for (File file : files) {
                if (file.exists()) {
                    updatedList.add(file);
                }
            }
            processedFilesHM.replace(s, updatedList);
        });
    }

    private void updDuplListView() {
        ObservableList<String> obsListChSum = FXCollections.observableArrayList();
        processedFilesHM.forEach((s, files) -> {
            if (files.size() > 1) {
                obsListChSum.add(s);
            }
        });
        duplChSumListView.setItems(obsListChSum);
    }

    private void showException(Exception ex) {
        ex.printStackTrace();

        StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));

        Platform.runLater(() -> {
            Alert exAlert = new Alert(Alert.AlertType.ERROR);
            exAlert.setHeaderText(guiBundle.getString("exceptionAlertHeader") + "\n" + ex.toString());
            exAlert.getDialogPane().setExpandableContent(new TextArea(stringWriter.toString()));
            resizeAlertManually(exAlert);

            exAlert.showAndWait();
        });
    }

    private void resizeAlertManually(Alert alert) {
        //Got a bug with stage resizing, must resize manually
        alert.getDialogPane().expandedProperty().addListener(observable -> {
            Platform.runLater(() -> {
                alert.getDialogPane().requestLayout();
                Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                stage.sizeToScene();
            });
        });
    }
}

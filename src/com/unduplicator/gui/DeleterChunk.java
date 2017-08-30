package com.unduplicator.gui;

import com.unduplicator.DeleteFilesTask;
import com.unduplicator.ResourcesProvider;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * <p>Created by MontolioV on 30.08.17.
 */
public class DeleterChunk extends AbstractGUIChunk {
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();
    private ChunkManager chunkManager;

    private Set<File> filesToDelete = new HashSet<>();
    private HashMap<File, Button> fileButtonHashMap;
    private ListView<String> checksumListView;
    private ObservableList<File> fileListViewValues;
    private ListView<File> fileListLView;

    private TilePane previewPane;
    private GridPane centerGrid;
    private VBox bottomBox;

    private Button toSetupButton = new Button();
    private Button toRuntimeButton = new Button();
    private Button deleteButton = new Button();

    private Label hashLabel = new Label();
    private Label previewLabel = new Label();

    public DeleterChunk(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
        setSelfNode(makePane());
        updateLocaleContent();
    }

    /**
     * Updates gui local dependent elements to current local settings.
     */
    @Override
    public void updateLocaleContent() {
        hashLabel.setText(resProvider.getStrFromGUIBundle("hashLabel"));
        previewLabel.setText(resProvider.getStrFromGUIBundle("previewLabel"));

        toSetupButton.setText(resProvider.getStrFromGUIBundle("setupButton"));
        toRuntimeButton.setText(resProvider.getStrFromGUIBundle("runtimeButton"));
        deleteButton.setText(resProvider.getStrFromGUIBundle("deleteButton"));

    }

    private BorderPane makePane() {
        makePreviewPane();
        makeFileListView();
        makeChecksumListView();
        makeCenterGrid();
        makeBottomBox();

        BorderPane resultPane = new BorderPane();
        resultPane.setCenter(centerGrid);
        resultPane.setBottom(bottomBox);
        resultPane.setPadding(new Insets(20));

        return resultPane;
    }

    private void makePreviewPane() {
        previewPane = new TilePane();
        previewPane.setPrefColumns(3);
        previewPane.setVgap(3);
        previewPane.setHgap(3);
        previewPane.setPadding(new Insets(5));
    }

    private void makeFileListView() {
        BiConsumer<File, String> setFilesToDeletion = (selectedFile, chSum) -> {
            filesToDelete.remove(selectedFile);
            chunkManager.getListCopy(chSum).stream().
                    filter(file -> !file.equals(selectedFile)).
                    forEach(filesToDelete::add);
        };
        fileListViewValues = FXCollections.observableArrayList();
        fileListLView = new ListView<>(fileListViewValues);
        fileListLView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        fileListLView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            String checksum = checksumListView.getSelectionModel().getSelectedItem();
            selectFileAndDisableButton(newValue);
            setFilesToDeletion.accept(newValue, checksum);
        });
    }

    private void makeChecksumListView() {
        updateChecksumListView();
        checksumListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        checksumListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            private Task<Void> prevTask;
            final double WIDTH = 200;
            final double HEIGHT = 200;

            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                String checksum = newValue;
                List<File> fileList = chunkManager.getListCopy(checksum);
                fileListViewValues.clear();

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

                        for (File file : fileList) {
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
                                    Platform.runLater(() -> fileListViewValues.add(file));

                                    Button previewButton = new Button(file.getName(), imageView);
                                    previewButton.setMaxSize(WIDTH, HEIGHT);
                                    previewButton.setContentDisplay(ContentDisplay.TOP);
                                    previewButton.setOnAction(event1 -> {
                                        selectFileAndDisableButton(file);
                                    });
                                    if (file.length() < MAX_IMG_SIZE) {
                                        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
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
                                            chunkManager.showException(e);
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
    }

    private void makeCenterGrid() {
        centerGrid = new GridPane();
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

        ScrollPane previewScrP = new ScrollPane(previewPane);
        previewScrP.setFitToWidth(true);


        centerGrid.add(hashLabel, 0, 0);
        centerGrid.add(checksumListView, 0, 1);
        centerGrid.add(previewLabel, 1, 0);
        centerGrid.add(previewScrP, 1, 1);
        centerGrid.add(fileListLView, 0, 2, 2, 1);
    }

    private void makeBottomBox() {
        ProgressBar progressBarDeletion = new ProgressBar();
        progressBarDeletion.setManaged(false);
        progressBarDeletion.setMaxWidth(Double.MAX_VALUE);

        toSetupButton.setOnAction(event -> chunkManager.showSetupNode());
        toRuntimeButton.setOnAction(event -> chunkManager.showRuntimeStatusNode());
        deleteButton.setOnAction(event -> {
            Function<Collection<File>, TextArea> colToTAFunction = files -> {
                StringJoiner sj = new StringJoiner("\n");
                files.forEach(f -> sj.add(f.toString()));
                return new TextArea(sj.toString());
            };

            DeleteFilesTask deletionTask = new DeleteFilesTask(new ArrayList<>(filesToDelete));

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(resProvider.getStrFromGUIBundle("delConformAlertTitle"));
            alert.setHeaderText(resProvider.getStrFromGUIBundle("delConformAlertBodyPart1") +
                    filesToDelete.size() +
                    resProvider.getStrFromGUIBundle("delConformAlertBodyPart2"));
            alert.getDialogPane().setExpandableContent(colToTAFunction.apply(filesToDelete));

            chunkManager.resizeAlertManually(alert);

            alert.showAndWait()
                    .filter(response -> response == ButtonType.OK)
                    .ifPresent(type -> {
                        Alert reportAlert = new Alert(Alert.AlertType.INFORMATION);
                        progressBarDeletion.progressProperty().bind(deletionTask.progressProperty());
                        progressBarDeletion.setManaged(true);
                        deletionTask.run();
                        try {
                            List<File> notDeletedList = deletionTask.get();
                            if (!notDeletedList.isEmpty()) {
                                reportAlert.setHeaderText(resProvider.getStrFromGUIBundle("reportAlertHeaderFail"));
                                reportAlert.getDialogPane().setExpandableContent(
                                        colToTAFunction.apply(notDeletedList));
                            } else {
                                reportAlert.setHeaderText(resProvider.getStrFromGUIBundle("reportAlertHeaderSuccess"));
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            chunkManager.showException(e);
                        }

                        updateChecksumListView();
                        progressBarDeletion.setManaged(false);
                        reportAlert.showAndWait();
                    });
        });

        HBox buttonsBox = new HBox(5,
                toSetupButton,
                deleteButton,
                toRuntimeButton);
        buttonsBox.setAlignment(Pos.CENTER);

        bottomBox = new VBox(10,
                progressBarDeletion,
                buttonsBox);
    }

    private void updateChecksumListView() {
        chunkManager.updateResults();
        checksumListView = new ListView<>(FXCollections.observableArrayList(
                                          chunkManager.getChecksumSetCopy()));
    }

    private void selectFileAndDisableButton(File file) {
        previewPane.getChildren().stream()
                .filter(Node::isDisabled)
                .forEach(node -> node.setDisable(false));
        fileButtonHashMap.get(file).setDisable(true);
        fileListLView.getSelectionModel().select(file);
    }
}

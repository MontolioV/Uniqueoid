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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * <p>Created by MontolioV on 30.08.17.
 */
public class DeleterChunk extends AbstractGUIChunk {
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();
    private ChunkManager chunkManager;

    private Set<File> filesToDelete = new HashSet<>();
    private Set<File> filesThatRemains = new HashSet<>();
    private HashMap<File, Button> fileButtonHashMap;
    private ListView<String> checksumListView = new ListView<>();
    private ListView<File> fileListLView;

    private TilePane previewPane;
    private GridPane centerGrid;
    private VBox bottomBox;

    private Button toSetupButton = new Button();
    private Button toRuntimeButton = new Button();
    private Button deleteButton = new Button();
    private Button chooserByParentButton = new Button();
    private Button chooserByRootButton = new Button();

    private Label hashLabel = new Label();
    private Label previewLabel = new Label();
    private Label massChooserLabel = new Label();

    private TextField massChooserTF = new TextField();

    private ProgressBar progressBar = new ProgressBar();

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
        massChooserLabel.setText(resProvider.getStrFromGUIBundle("massChooserLabel"));

        toSetupButton.setText(resProvider.getStrFromGUIBundle("setupNode"));
        toRuntimeButton.setText(resProvider.getStrFromGUIBundle("runtimeNode"));
        deleteButton.setText(resProvider.getStrFromGUIBundle("deleteButton"));
        chooserByParentButton.setText(resProvider.getStrFromGUIBundle("chooserByParentButton"));
        chooserByRootButton.setText(resProvider.getStrFromGUIBundle("chooserByRootButton"));
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
        fileListLView = new ListView<>(FXCollections.observableArrayList());
        fileListLView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        fileListLView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectFileAndDisableButton(newValue);
        });
    }

    private void makeChecksumListView() {
        updateDataForGUI();
        checksumListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        checksumListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            private Task<Void> prevTask;
            final double WIDTH = 200;
            final double HEIGHT = 200;

            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (newValue==null) return;

                String checksum = newValue;
                List<File> fileList = chunkManager.getListOfDuplicatesCopy(checksum);
                ObservableList<File> fileListViewValues = FXCollections.observableArrayList();
                fileListLView.setItems(fileListViewValues);
                fileButtonHashMap = new HashMap<>();

                if (prevTask != null) {
                    prevTask.cancel();
                }
                previewPane.getChildren().clear();

                ArrayList<Task<Void>> tasks = new ArrayList<>();
                prevTask = new Task<Void>() {
                    AtomicBoolean stop = new AtomicBoolean();
                    AtomicInteger progress = new AtomicInteger();

                    @Override
                    protected Void call() throws Exception {
                        Executor pool = Executors.newFixedThreadPool(4, r -> {
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
                            prIndicator.progressProperty().addListener((observable1, oldValue1, newValue1) -> {
                                if (newValue1.doubleValue() >= 1) {
                                    prIndicator.setVisible(false);
                                }
                            });
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

                                    fileButtonHashMap.put(file, previewButton);
                                    increaseProgress();
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

                    private void increaseProgress() {
                        updateProgress(progress.incrementAndGet(), fileList.size());
                    }
                };

                progressBar.setManaged(true);
                progressBar.progressProperty().bind(prevTask.progressProperty());

                Thread thread = new Thread(() -> {
                    try {
                        prevTask.run();
                        prevTask.get();
                        for (Task<Void> voidTask : tasks) {
                            voidTask.get();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        chunkManager.showException(e);
                    }
                    for (File file : fileList) {
                        if (filesThatRemains.contains(file)) {
                            Platform.runLater(() -> selectFileAndDisableButton(file));
                        }
                    }

                    Platform.runLater(() -> progressBar.setManaged(false));
                });
                thread.setDaemon(true);
                thread.start();
            }
        });
    }

    private VBox makeMassChooserPane() {
        VBox result;

        Function<BiPredicate<File, String>, String> massChooserFunc = fileBiPredicate -> {
            int saveCounter = 0;
            int delCounter = 0;
            String parentToFind = massChooserTF.getText();

            for (String checksum : chunkManager.getDuplicatesChecksumSet()) {
                List<File> duplicates = chunkManager.getListOfDuplicatesCopy(checksum);
                for (File duplicate : duplicates) {
                    if (fileBiPredicate.test(duplicate, parentToFind)) {
                        setToDelAllFilesExceptOne(checksum, duplicate);
                        delCounter += duplicates.size() - 1;
                        saveCounter++;
                        break;
                    }
                }
            }
            return saveCounter + "\n"
                    + resProvider.getStrFromMessagesBundle("chosenToDelete")
                    + delCounter;
        };
        BiPredicate<File, String> byParent = (file, parentToFind) -> file.getParent().equals(parentToFind);
        BiPredicate<File, String> byRoot = (file, rootToFind) -> file.getParent().startsWith(rootToFind);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        chooserByParentButton.setOnAction(event -> {
            String report = resProvider.getStrFromMessagesBundle("chosenByParent");
            report += massChooserFunc.apply(byParent);
            alert.setHeaderText(report);
            alert.showAndWait();
        });
        chooserByRootButton.setOnAction(event -> {
            String report = resProvider.getStrFromMessagesBundle("chosenByRoot");
            report += massChooserFunc.apply(byRoot);
            alert.setHeaderText(report);
            alert.showAndWait();
        });

        HBox.setHgrow(massChooserTF, Priority.ALWAYS);
        HBox textBox = new HBox(5, massChooserLabel, massChooserTF);
        textBox.setAlignment(Pos.CENTER);

        chooserByParentButton.setMaxWidth(Double.MAX_VALUE);
        chooserByRootButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(chooserByParentButton, Priority.ALWAYS);
        HBox.setHgrow(chooserByRootButton, Priority.ALWAYS);
        HBox buttonsBox = new HBox(5, chooserByParentButton, chooserByRootButton);

        result = new VBox(5, textBox, buttonsBox);
        return result;
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

        VBox textPart = new VBox(5, fileListLView, makeMassChooserPane());

        centerGrid.add(hashLabel, 0, 0);
        centerGrid.add(checksumListView, 0, 1);
        centerGrid.add(previewLabel, 1, 0);
        centerGrid.add(previewScrP, 1, 1);
        centerGrid.add(textPart, 0, 2, 2, 1);
    }

    private void makeBottomBox() {
        progressBar.visibleProperty().bindBidirectional(progressBar.managedProperty());
        progressBar.setManaged(false);
        progressBar.setMaxWidth(Double.MAX_VALUE);

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
                        progressBar.progressProperty().bind(deletionTask.progressProperty());
                        progressBar.setManaged(true);
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

                        updateDataForGUI();
                        progressBar.setManaged(false);
                        reportAlert.showAndWait();

                        previewPane.getChildren().clear();
                        fileListLView.getItems().clear();
                    });
        });

        HBox buttonsBox = new HBox(5,
                toSetupButton,
                deleteButton,
                toRuntimeButton);
        buttonsBox.setAlignment(Pos.CENTER);

        bottomBox = new VBox(10,
                progressBar,
                buttonsBox);
    }

    private void updateDataForGUI() {
        filesToDelete = new HashSet<>();
        filesThatRemains = new HashSet<>();
        chunkManager.updateResults();
        checksumListView.setItems(FXCollections.observableArrayList(
                                  chunkManager.getDuplicatesChecksumSet()));
    }

    private void selectFileAndDisableButton(File selectedFile) {
        if (selectedFile == null) return;
        massChooserTF.setText(selectedFile.getParent());
        Button linkedButton = fileButtonHashMap.get(selectedFile);
        if (linkedButton.isDisabled()) return;

        fileButtonHashMap.forEach((file, button) -> button.setDisable(false));
        linkedButton.setDisable(true);
        fileListLView.getSelectionModel().select(selectedFile);
        fileListLView.scrollTo(selectedFile);

        String checksum = checksumListView.getSelectionModel().getSelectedItem();
        setToDelAllFilesExceptOne(checksum, selectedFile);
    }

    private void setToDelAllFilesExceptOne (String fileChecksum, File fileToSave) {
        List<File> duplicatesList = chunkManager.getListOfDuplicatesCopy(fileChecksum);

        filesToDelete.remove(fileToSave);
        filesThatRemains.add(fileToSave);
        for (File file : duplicatesList) {
            if (!file.equals(fileToSave)) {
                filesToDelete.add(file);
                filesThatRemains.remove(file);
            }
        }
    }
}

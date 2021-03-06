package io.sourceforge.uniqueoid.gui;

import io.sourceforge.uniqueoid.ResourcesProvider;
import io.sourceforge.uniqueoid.logic.DeleteFilesTask;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <p>Created by MontolioV on 30.08.17.
 */
public class DeleterChunk extends AbstractGUIChunk {
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();
    private ChunkManager chunkManager;

    private AtomicLong previewTimeStamp = new AtomicLong();
    private AtomicLong searchEnterTimeStamp = new AtomicLong();

    private Executor deleterExecutor;
    private Task<Void> showDuplicatesTask;

    private Map<File, Button> fileButtonHashMap;
    private ListView<Text> checksumListView = new ListView<>();
    private ListView<File> fileListLView;
    private Set<Text> checksumTextSet;

    private TilePane previewPane;
    private GridPane centerGrid;
    private VBox bottomBox;
    private VBox progressControlBox = new VBox(3);

    private Button toSetupButton = new Button();
    private Button toRuntimeButton = new Button();
    private Button deleteButton = new Button();
    private Button chooserByParentButton = new Button();
    private Button chooserByRootButton = new Button();

    private Label hashLabel = new Label();
    private Label previewLabel = new Label();
    private Label massChooserLabel = new Label();
    private Label statisticsLabel = new Label();
    private Label massChooserTFBackgroundLabel = new Label();

    private TextField massChooserTF = new TextField();

    private String currentChecksum = null;

    public DeleterChunk(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
        makeThreadExecutor();
        setSelfNode(makePane());
        updateLocaleContent();
        bindToTemplateTF(chunkManager.getPropertiesToBindToTemplateTF());
    }

    /**
     * Updates gui local dependent elements to current local settings.
     */
    @Override
    public void updateLocaleContent() {
        updateStatisticsLabel();
        hashLabel.setText(resProvider.getStrFromGUIBundle("hashLabel"));
        previewLabel.setText(resProvider.getStrFromGUIBundle("previewLabel"));
        massChooserLabel.setText(resProvider.getStrFromGUIBundle("massChooserLabel"));
        massChooserTFBackgroundLabel.setText(resProvider.getStrFromGUIBundle("massChooserTFBackgroundLabel"));

        toSetupButton.setText(resProvider.getStrFromGUIBundle("setupNode"));
        toRuntimeButton.setText(resProvider.getStrFromGUIBundle("runtimeNode"));
        deleteButton.setText(resProvider.getStrFromGUIBundle("deleteButton"));
        chooserByParentButton.setText(resProvider.getStrFromGUIBundle("chooserByParentButton"));
        chooserByRootButton.setText(resProvider.getStrFromGUIBundle("chooserByRootButton"));
    }

    public void updateChunk() {
        String selectedChecksum = currentChecksum;

        updateChecksumListView();

        if (selectedChecksum != null) {
            for (Text text : checksumListView.getItems()) {
                if (text.getText().equals(selectedChecksum)) {
                    checksumListView.getSelectionModel().select(text);
                    return;
                }
            }
        }
        updateDuplicatesRepresentation(null);
    }

    private void updateStatisticsLabel() {
        int[] statistics = chunkManager.getStatistics(currentChecksum);
        String newStatistics = String.format(resProvider.getStrFromGUIBundle("statisticsLabel"),
                statistics[0],
                statistics[1],
                statistics[2],
                statistics[3]);
        statisticsLabel.setText(newStatistics);
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
            selectFile(newValue);
        });
        fileListLView.setMinHeight(50);
    }
    private void makeChecksumListView() {
        fullModelRefresh();
        checksumListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        checksumListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        currentChecksum = newValue.getText();
                    } else {
                        currentChecksum = null;
                    }
                    updateChecksumTextRepresentation();
                    updateDuplicatesRepresentation(currentChecksum);
                    updateStatisticsLabel();
                });
    }
    private VBox makeMassChooserPane() {
        VBox result;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        massChooserTFBackgroundLabel.maxWidthProperty().bind(massChooserTF.widthProperty());
        massChooserTFBackgroundLabel.visibleProperty().bind(massChooserTF.textProperty().isEmpty());
        massChooserTFBackgroundLabel.setStyle("-fx-opacity: 0.5");
        massChooserTFBackgroundLabel.setMouseTransparent(true);
        massChooserTFBackgroundLabel.setPadding(new Insets(0, 0, 0, 10));

        massChooserTF.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.contains(System.getProperty("file.separator"))) return;
            Task<Void> filterTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    long ownTimeStamp = System.currentTimeMillis();
                    searchEnterTimeStamp.set(ownTimeStamp);

                    Thread.sleep(500);
                    if (searchEnterTimeStamp.get() != ownTimeStamp) return null;

                    chunkManager.makeChecksumSetByFileName(newValue);
                    Platform.runLater(() -> updateChecksumListView());
                    return null;
                }
            };

            Supplier<Void> doNothingSupp = () -> null;

            runTaskSeparateThread(filterTask, doNothingSupp, resProvider.getStrFromGUIBundle("filterProgressLabel"));
        });

        chooserByParentButton.setOnAction(event -> {
            String pattern = massChooserTF.getText();
            int[] saveAndDel = chunkManager.massChooseByParent(pattern);
            String report = resProvider.getStrFromMessagesBundle("chosenByParent")
                    + saveAndDel[0] + "\n"
                    + resProvider.getStrFromMessagesBundle("chosenToDelete")
                    + saveAndDel[1];
            alert.setHeaderText(report);

            updateChecksumTextRepresentation();
            alert.showAndWait();
            updateStatisticsLabel();
        });
        chooserByRootButton.setOnAction(event -> {
            String pattern = massChooserTF.getText();
            int[] saveAndDel = chunkManager.massChooseByRoot(pattern);
            String report = resProvider.getStrFromMessagesBundle("chosenByRoot")
                    + saveAndDel[0] + "\n"
                    + resProvider.getStrFromMessagesBundle("chosenToDelete")
                    + saveAndDel[1];
            alert.setHeaderText(report);

            updateChecksumTextRepresentation();
            alert.showAndWait();
            updateStatisticsLabel();
        });

        bindToTemplateTF(chooserByParentButton.disableProperty());
        bindToTemplateTF(chooserByRootButton.disableProperty());

        StackPane massChooserTFStackPane = new StackPane(massChooserTF, massChooserTFBackgroundLabel);
        HBox.setHgrow(massChooserTFStackPane, Priority.ALWAYS);

        HBox textBox = new HBox(5, massChooserLabel, massChooserTFStackPane);
        textBox.setAlignment(Pos.CENTER);

        chooserByParentButton.setMaxWidth(Double.MAX_VALUE);
        chooserByRootButton.setMaxWidth(Double.MAX_VALUE);
        chooserByParentButton.setPrefWidth(100000);
        chooserByRootButton.setPrefWidth(100000);
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
        rCons1.setPercentHeight(60);
        rCons2.setPercentHeight(40);
        centerGrid.getRowConstraints().setAll(rCons0, rCons1, rCons2);
        centerGrid.setPadding(new Insets(0, 0, 10, 0));

        BorderPane labelsAbovePreview = new BorderPane(null, null, statisticsLabel, null, previewLabel);

        ScrollPane previewScrP = new ScrollPane(previewPane);
        previewScrP.setFitToWidth(true);

        VBox textPart = new VBox(5, fileListLView, makeMassChooserPane());

        centerGrid.add(hashLabel, 0, 0);
        centerGrid.add(checksumListView, 0, 1);
        centerGrid.add(labelsAbovePreview, 1, 0);
        centerGrid.add(previewScrP, 1, 1);
        centerGrid.add(textPart, 0, 2, 2, 1);
    }
    private void makeBottomBox() {
        toSetupButton.setOnAction(event -> chunkManager.showSetupNode());
        toRuntimeButton.setOnAction(event -> chunkManager.showRuntimeStatusNode());
        deleteButton.setOnAction(event -> {
            Set<File> filesToDelete = chunkManager.getFilesToDelete();

            Function<Collection<File>, TextArea> colToTAFunction = files -> {
                StringJoiner sj = new StringJoiner("\n");
                files.forEach(f -> sj.add(f.toString()));
                return new TextArea(sj.toString());
            };

            DeleteFilesTask deletionTask = new DeleteFilesTask(filesToDelete);

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(resProvider.getStrFromGUIBundle("delConformAlertTitle"));
            alert.setHeaderText(resProvider.getStrFromGUIBundle("delConformAlertBodyPart1") +
                    filesToDelete.size() +
                    " (" + freedSpace(filesToDelete) + ")" +
                    resProvider.getStrFromGUIBundle("delConformAlertBodyPart2"));
            alert.getDialogPane().setExpandableContent(colToTAFunction.apply(filesToDelete));

            chunkManager.resizeAlertManually(alert);

            alert.showAndWait()
                    .filter(response -> response == ButtonType.OK)
                    .ifPresent(type -> {
                        Supplier<Void> showAlert = () -> {
                            Platform.runLater(() -> {
                                Alert reportAlert = new Alert(Alert.AlertType.INFORMATION);

                                try {
                                    Set<File> notDeletedList = deletionTask.get();
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

                                chunkManager.updateResults();
                                chunkManager.resizeAlertManually(reportAlert);
                                reportAlert.showAndWait();
                            });
                            return null;
                        };

                        runTaskSeparateThread(deletionTask, showAlert, resProvider.getStrFromGUIBundle("deletionProgressLabel"));
                    });
        });

        toSetupButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        toRuntimeButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        deleteButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        TilePane buttonsBox = new TilePane(Orientation.HORIZONTAL, 10, 0,
                toSetupButton,
                deleteButton,
                toRuntimeButton);
        buttonsBox.setAlignment(Pos.CENTER);

        bottomBox = new VBox(10,
                progressControlBox,
                buttonsBox);
    }

    private void selectFile(File selectedFile) {
        if (selectedFile == null) return;

        showSelected(selectedFile);
        chunkManager.chooseOneAmongDuplicates(currentChecksum, selectedFile);
        updateStatisticsLabel();
        updateChecksumTextRepresentation();
    }
    private void showSelected(File selectedFile) {
        if (selectedFile == null) return;

        massChooserTF.setText(selectedFile.getParent());
        Button linkedButton = fileButtonHashMap.get(selectedFile);
        if (linkedButton == null || linkedButton.isDisabled()) return;

        fileButtonHashMap.forEach((file, button) -> button.setDisable(false));
        linkedButton.setDisable(true);
        fileListLView.getSelectionModel().select(selectedFile);
        fileListLView.scrollTo(selectedFile);
    }
    protected void unselectCurrent() {
        if (currentChecksum == null) return;

        chunkManager.removeSelectionsByChecksum(currentChecksum);
    }

    private void fullModelRefresh() {
        chunkManager.removeSelectionsAll();
        chunkManager.updateResults();
    }
    private void updateDuplicatesRepresentation(String checksum) {
        if (checksum == null || checksum.equals("")) {
            previewPane.getChildren().clear();
            fileListLView.getItems().clear();
            return;
        }

        long ownTimeStamp = System.currentTimeMillis();
        previewTimeStamp.set(ownTimeStamp);

        double width = 200;
        double height = 200;
        Set<File> duplicateFiles = chunkManager.getDuplicateFilesCopy(checksum);
        ObservableList<File> fileListViewValues = FXCollections.observableArrayList();
        Map<File, Button> personalFileButtonHashMap = Collections.synchronizedMap(new HashMap<>());
        previewPane.getChildren().clear();
        ArrayList<Task<Void>> tasks = new ArrayList<>();

        Task<Void> newTask = new Task<Void>() {
            AtomicInteger progress = new AtomicInteger();
            ExecutorService pool;

            @Override
            protected Void call() throws Exception {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    //Not really useful
//                    ex.printStackTrace();
                }
                if (isCancelled() || isOutdated()) return null;

                pool = Executors.newFixedThreadPool(4, r -> {
                    Thread daemonThr = new Thread(r);
                    daemonThr.setDaemon(true);
                    return daemonThr;
                });

                for (File file : duplicateFiles) {
                    if (isCancelled()) {
                        return null;
                    }

                    Task<Void> imgTask = new Task<Void>() {
                        final long MAX_IMG_SIZE = 52_500_000;

                        @Override
                        protected Void call() throws Exception {
                            if (isCancelled()) return null;

                            ImageView imageView = new ImageView();
                            Button previewButton = new Button(file.getName(), imageView);
                            previewButton.setMnemonicParsing(false);
                            previewButton.setMaxSize(width, height);
                            previewButton.setContentDisplay(ContentDisplay.TOP);
                            previewButton.setOnAction(event1 -> {
                                selectFile(file);
                            });
                            if (file.length() < MAX_IMG_SIZE && !isCancelled()) {
                                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                                    Image image = new Image(bis, width, height, true, true);
                                    imageView.setImage(image);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (isCancelled()) return null;

                            personalFileButtonHashMap.put(file, previewButton);
                            Platform.runLater(() -> {
                                fileListViewValues.add(file);
                                previewPane.getChildren().add(previewButton);
                            });

                            increaseProgress();
                            return null;
                        }
                    };

                    tasks.add(imgTask);
                    pool.execute(imgTask);
                }

                for (Task<Void> voidTask : tasks) {
                    voidTask.get();
                }

                Platform.runLater(() -> {
                    for (File file : duplicateFiles) {
                        if (isCancelled()) break;
                        if (chunkManager.isFileChosen(file)) {
                            showSelected(file);
                        }
                    }
                });
                pool.shutdown();
                return null;
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean result = super.cancel(mayInterruptIfRunning);
                tasks.forEach(Task::cancel);
                if (pool != null) pool.shutdown();
                return result;
            }

            private void increaseProgress() {
                updateProgress(progress.incrementAndGet(), duplicateFiles.size());
            }

            private boolean isOutdated() {
                return previewTimeStamp.get() != ownTimeStamp;
            }
        };

        Supplier<Void> idle = () -> null;

        synchronized (this) {
            if (showDuplicatesTask != null) showDuplicatesTask.cancel();
            showDuplicatesTask = newTask;
            fileListLView.setItems(fileListViewValues);
            fileButtonHashMap = personalFileButtonHashMap;
            runTaskSeparateThread(showDuplicatesTask, idle, resProvider.getStrFromGUIBundle("previewProgressLabel"));
        }
    }
    private void updateChecksumListView() {
        checksumTextSet = new HashSet<>();
        ObservableList<Text> obsList = FXCollections.observableArrayList();
        for (String checksum : chunkManager.getDuplicatesChecksumSet()) {
            Text textChecksum = new Text(checksum);
            checksumTextSet.add(textChecksum);
            obsList.add(textChecksum);
        }

        updateChecksumTextRepresentation();
        checksumListView.setItems(obsList);
        checksumListView.getSelectionModel().selectFirst();
    }
    protected void updateChecksumTextRepresentation() {
        for (Text text : checksumTextSet) {
            if (chunkManager.isChoiceMadeOnChecksum(text.getText())) {
                text.setStyle("-fx-font-weight: normal");
            } else {
                text.setStyle("-fx-font-weight: bolder");
            }
        }
    }

    protected void ignoreSelectedDuplicate() {
        if (fileListLView.getSelectionModel().getSelectedItem() == null || currentChecksum == null) {
            return;
        }

        File selectedDuplicate = fileListLView.getSelectionModel().getSelectedItem();
        chunkManager.ignoreDuplicate(currentChecksum, selectedDuplicate);
    }
    protected void ignoreDuplicatesByParent() {
        chunkManager.ignoreDuplicatesFromDirectory(massChooserTF.getText());
    }
    protected void ignoreDuplicatesByRoot() {
        chunkManager.ignoreDuplicatesFromRoot(massChooserTF.getText());
    }

    protected void removeFilter() {
        massChooserTF.setText("");
    }

    private void bindToTemplateTF(BooleanProperty booleanProperty) {
        booleanProperty.bind(massChooserTF.textProperty().isEmpty());
    }
    protected void bindToTemplateTF(BooleanProperty[] booleanProperties) {
        for (BooleanProperty booleanProperty : booleanProperties) {
            bindToTemplateTF(booleanProperty);
        }
    }

    private String freedSpace(Set<File> filesToDelete) {
        long bytes = filesToDelete.stream().mapToLong(File::length).sum();
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        if (bytes < Math.pow(2, 30)) {
            return decimalFormat.format(bytes / Math.pow(2, 20)) + "MB";
        } else {
            return decimalFormat.format(bytes / Math.pow(2, 30)) + "GB";
        }
    }

    private void makeThreadExecutor() {
        deleterExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread daemonThr = new Thread(runnable);
            daemonThr.setDaemon(true);
            return daemonThr;
        });
    }
    private void runTaskSeparateThread(Task task, Supplier<Void> postProcessing, String name) {
        Runnable control = () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!task.isDone()) {
                HBox hBox = new HBox(3);

                Platform.runLater(() -> {
                    Button cancelButton = new Button(resProvider.getStrFromGUIBundle("cancelButton"));
                    cancelButton.setOnAction(event -> task.cancel());

                    ProgressBar progressBar = new ProgressBar();
                    progressBar.setMaxWidth(Double.MAX_VALUE);
                    progressBar.progressProperty().bind(task.progressProperty());
                    progressBar.prefHeightProperty().bind(cancelButton.heightProperty());

                    StackPane pbWithNamePane = new StackPane(progressBar, new Label(name));
                    HBox.setHgrow(pbWithNamePane, Priority.ALWAYS);

                    hBox.getChildren().addAll(pbWithNamePane, cancelButton);
                    progressControlBox.getChildren().add(hBox);
                });

                try {
                    task.get();
                } catch (InterruptedException | ExecutionException e) {
                    chunkManager.showException(e);
                } finally {
                    Platform.runLater(() -> {
                        progressControlBox.getChildren().remove(hBox);
                    });
                    postProcessing.get();
                }
            } else {
                postProcessing.get();
            }
        };

        deleterExecutor.execute(control);
        deleterExecutor.execute(task);
    }
}

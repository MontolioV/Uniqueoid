package com.unduplicator.gui;

import com.unduplicator.GlobalFiles;
import com.unduplicator.ResourcesProvider;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * <p>Created by MontolioV on 30.08.17.
 */
public class RuntimeStatusChunk extends AbstractGUIChunk {
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();
    private ChunkManager chunkManager;

    private Task<Map<String, Set<File>>> task;

    private Button stopButton = new Button();
    private Button toSetupBut = new Button();
    private Button toResultBut = new Button();

    private Label poolStatusLabel = new Label();
    private TextArea messagesTA = new TextArea();
    private ProgressBar progressBar = new ProgressBar();

    public RuntimeStatusChunk(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
        setSelfNode(makePane());
        updateLocaleContent();
    }

    /**
     * Updates gui local dependent elements to current local settings.
     */
    @Override
    public void updateLocaleContent() {
        stopButton.setText(resProvider.getStrFromGUIBundle("cancelButton"));
        toSetupBut.setText(resProvider.getStrFromGUIBundle("setupNode"));
        toResultBut.setText(resProvider.getStrFromGUIBundle("deletionNode"));
    }

    /**
     * May change state depending on received state.
     * @return <code>true</code> if new state differs, otherwise <code>false</code>
     */
    @Override
    public boolean changeState(GuiStates newState) {
        if (super.changeState(newState)) {
            switch (newState) {

                case NO_RESULTS:
                    toSetupBut.setDisable(false);
                    toResultBut.setDisable(true);
                    stopButton.setDisable(true);
                    progressBar.setVisible(false);
                    break;
                case RUNNING:
                    toSetupBut.setDisable(true);
                    toResultBut.setDisable(true);
                    stopButton.setDisable(false);
                    progressBar.setVisible(true);
                    break;
                case HAS_RESULTS:
                    toSetupBut.setDisable(false);
                    toResultBut.setDisable(false);
                    stopButton.setDisable(true);
                    progressBar.setVisible(false);
                    break;
            }
            return true;
        }
        return false;
    }

    private BorderPane makePane() {
        progressBar.setMaxWidth(Double.MAX_VALUE);
        poolStatusLabel.setMaxWidth(Double.MAX_VALUE);
        messagesTA.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        messagesTA.setEditable(false);
        messagesTA.setWrapText(true);

        toSetupBut.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        toResultBut.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        stopButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        toSetupBut.setOnAction(event -> chunkManager.showSetupNode());
        toResultBut.setOnAction(event -> chunkManager.showDeletionNode());
        stopButton.setOnAction(event -> {
            if (task != null) {
                task.cancel();
            }
        });

        TilePane stopPane = new TilePane(Orientation.HORIZONTAL, 10, 0,
                toSetupBut,
                stopButton,
                toResultBut);
        stopPane.setAlignment(Pos.CENTER);

        VBox bottomP = new VBox(10,
                poolStatusLabel,
                progressBar,
                stopPane);
        bottomP.setPadding(new Insets(20, 0, 0, 0));

        BorderPane runtimePane = new BorderPane();
        runtimePane.setCenter(messagesTA);
        runtimePane.setBottom(bottomP);
        runtimePane.setPadding(new Insets(20));

        return runtimePane;
    }

    protected EventHandler<ActionEvent> getTaskButtonHandler(Supplier<Task<Map<String, Set<File>>>> taskSupplier) {
        EventHandler<ActionEvent> startButHandler = event -> {
            this.task = taskSupplier.get();

            long startTime = System.currentTimeMillis();
            messagesTA.clear();
            progressBar.progressProperty().bind(task.progressProperty());
            poolStatusLabel.textProperty().bind(task.titleProperty());
            task.messageProperty().addListener((observable, oldValue, newValue) -> {
                if (messagesTA.getParagraphs().size() > 100) {
                    messagesTA.setText(resProvider.getStrFromMessagesBundle("seeLog")
                                       + GlobalFiles.getInstance().getLogFile());
                }
                messagesTA.appendText("\n" + newValue);
            });
            task.stateProperty().addListener((observable, oldValue, newValue) -> {
                switch (newValue) {
                    case READY:
                        chunkManager.updateChunksStates(GuiStates.RUNNING);
                        break;
                    case SCHEDULED:
                        chunkManager.updateChunksStates(GuiStates.RUNNING);
                        break;
                    case RUNNING:
                        chunkManager.updateChunksStates(GuiStates.RUNNING);
                        chunkManager.cleanOldResults();
                        messagesTA.appendText("\n" + resProvider.getStrFromMessagesBundle("taskProcessing"));
                        break;
                    case SUCCEEDED:
                        chunkManager.updateChunksStates(GuiStates.HAS_RESULTS);
                        chunkManager.setResults(task.getValue());
                        chunkManager.makeDeleterChunk();

                        long durationMS = System.currentTimeMillis() - startTime;
                        messagesTA.appendText("\n" + resProvider.getStrFromMessagesBundle("completeSuccessfully")
                                              + millisToTimeStr(durationMS));
                        messagesTA.appendText("\n" + resProvider.getStrFromMessagesBundle("foundDuplicates")
                                              + chunkManager.getDuplicatesAmount());
                        break;
                    case CANCELLED:
                        chunkManager.updateChunksStates(GuiStates.NO_RESULTS);
                        messagesTA.appendText("\n" + resProvider.getStrFromMessagesBundle("canceled"));
                        break;
                    case FAILED:
                        chunkManager.updateChunksStates(GuiStates.NO_RESULTS);
                        messagesTA.appendText("\n" + resProvider.getStrFromMessagesBundle("error"));
                        break;
                }
            });
            Thread taskThread = new Thread(() -> {
                try {
                    task.run();
                    task.get();
                } catch (InterruptedException | ExecutionException e) {
                    chunkManager.showException(e);
                }
            });
            taskThread.setDaemon(true);
            taskThread.start();

            chunkManager.showRuntimeStatusNode();
        };
        return startButHandler;
    }

    private String millisToTimeStr(long millis) {
        String result;

        long ms = millis % 1000;
        long s = (millis / 1000) % 60;
        long m = (millis / (1000 * 60)) % 60;
        long h = (millis / (1000 * 60 * 60)) % 60;

        if (millis >= 1000 * 60 * 60) {
            result = String.format(resProvider.getStrFromMessagesBundle("h_m_s_ms"), h, m, s, ms);
        } else if (millis >= 1000 * 60) {
            result = String.format(resProvider.getStrFromMessagesBundle("m_s_ms"), m, s, ms);
        } else if (millis >= 1000) {
            result = String.format(resProvider.getStrFromMessagesBundle("s_ms"), s, ms);
        } else {
            result = String.format(resProvider.getStrFromMessagesBundle("ms"), ms);
        }
        return result;
    }

}

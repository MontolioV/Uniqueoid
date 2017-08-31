package com.unduplicator.gui;

import com.unduplicator.FindDuplicatesTask;
import javafx.scene.control.Alert;

import java.io.File;
import java.util.*;

/**
 * <p>Created by MontolioV on 30.08.17.
 */
public class ChunkManager {
    private final GUI GUI;
    private Results results;
    private Set<AbstractGUIChunk> chunks = new HashSet<>();
    private SetupChunk setupChunk;
    private RuntimeStatusChunk runtimeChunk;
    private DeleterChunk deleterChunk;


    public ChunkManager(GUI gui) {
        GUI = gui;
        makeChunks();
        showSetupNode();
    }

    //Make chunks
    private void makeChunks() {
        runtimeChunk = new RuntimeStatusChunk(this);
        setupChunk = new SetupChunk(GUI.getPrimaryStage());
        setupChunk.setStartButtonHandler(runtimeChunk.getStartButHandler());

        chunks.add(runtimeChunk);
        chunks.add(setupChunk);
    }
    protected void makeDeleterChunk() {
        deleterChunk = new DeleterChunk(this);
        chunks.add(deleterChunk);
    }

    //Show in GUI
    protected void showSetupNode() {
        GUI.setCenterNode(setupChunk.getAsNode());
    }
    protected void showRuntimeStatusNode() {
        GUI.setCenterNode(runtimeChunk.getAsNode());
    }
    protected void showResultsNode() {
        GUI.setCenterNode(deleterChunk.getAsNode());
    }
    protected void showException(Exception ex) {
        GUI.showException(ex);
    }
    protected void resizeAlertManually(Alert alert) {
        GUI.resizeAlertManually(alert);
    }

    //Update chunks
    protected void updateChunksLocales() {
        chunks.forEach(LocaleDependent::updateLocaleContent);
    }
    protected void updateChunksStates(GuiStates newState) {
        chunks.forEach(chunk -> chunk.changeState(newState));
    }

    protected FindDuplicatesTask getTask() {
        return setupChunk.getTask();
    }

    //Results object
    protected void setResults(HashMap<String, List<File>> processedFilesHM) {
        results = new Results(processedFilesHM);
    }
    /**
     * Remove files that already don't exist.
     */
    protected void updateResults() {
        results.removeMissingFiles();
    }
    protected int getDuplicatesAmount() {
        if (results == null) return 0;
        return results.countDuplicates();
    }
    protected List<File> getListCopy(String checksumKey) {
        return results.getFiles(checksumKey);
    }
    protected Set<String> getChecksumSetCopy() {
        return results.getCheckSums();
    }


    //Memory saving
    protected void cleanOldResults() {
        results = null;
        deleterChunk = null;
    }
}

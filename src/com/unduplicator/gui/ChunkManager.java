package com.unduplicator.gui;

import com.unduplicator.FindDuplicatesTask;
import javafx.scene.Parent;
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
    private MenuBarChunk menuBarChunk;
    private SetupChunk setupChunk;
    private RuntimeStatusChunk runtimeChunk;
    private DeleterChunk deleterChunk;


    public ChunkManager(GUI gui) {
        GUI = gui;
        makeChunks();
        showMenuBar();
        showSetupNode();
        updateChunksStates(GuiStates.NO_RESULTS);
    }

    //Make chunks
    private void makeChunks() {
        menuBarChunk = new MenuBarChunk(this, GUI.getPrimaryStage());
        runtimeChunk = new RuntimeStatusChunk(this);
        setupChunk = new SetupChunk(GUI.getPrimaryStage());

        setupChunk.setStartButtonHandler(runtimeChunk.getStartButHandler());

        chunks.add(menuBarChunk);
        chunks.add(runtimeChunk);
        chunks.add(setupChunk);
    }
    protected void makeDeleterChunk() {
        deleterChunk = new DeleterChunk(this);
        deleterChunk.updateChunk();
        chunks.add(deleterChunk);
    }

    //Show in GUI
    protected void showMenuBar() {
        GUI.setTopNode(menuBarChunk.getAsNode());
    }
    protected void showSetupNode() {
        GUI.setCenterNode(setupChunk.getAsNode());
    }
    protected void showRuntimeStatusNode() {
        GUI.setCenterNode(runtimeChunk.getAsNode());
    }
    protected void showDeletionNode() {
        GUI.setCenterNode(deleterChunk.getAsNode());
    }
    protected void showException(Exception ex) {
        ex.printStackTrace();
        GUI.showException(ex);
    }
    protected void resizeAlertManually(Alert alert) {
        GUI.resizeAlertManually(alert);
    }
    protected void showInNewStage(Parent parent) {
        GUI.showInNewStage(parent);
    }

    //Update all chunks
    protected void updateChunksLocales() {
        chunks.forEach(LocaleDependent::updateLocaleContent);
    }
    protected void updateChunksStates(GuiStates newState) {
        chunks.forEach(chunk -> chunk.changeState(newState));
    }

    //Results object
    protected void setResults(HashMap<String, List<File>> processedFilesHM) {
        results = new Results(this, processedFilesHM);
    }
    protected void saveResults(File file) {
        results.saveToFile(file);
    }
    protected void loadResults(File file) {
        results = new Results(this, file);
        updateChunksStates(GuiStates.HAS_RESULTS);

        DeleterChunk oldDeleterChunk = deleterChunk;
        makeDeleterChunk();

        if (oldDeleterChunk != null) {
            chunks.remove(oldDeleterChunk);
        }

        showDeletionNode();
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
    protected List<File> getListOfDuplicatesCopy(String checksumKey) {
        return results.getFilesListCopy(checksumKey);
    }
    protected Set<String> getDuplicatesChecksumSet() {
        return results.getDuplicateChecksumSet();
    }
    protected void chooseOneAmongDuplicates(String checksum, File fileThatRemains) {
        results.chooseOneAmongDuplicates(checksum, fileThatRemains);
    }
    protected int[] massChooseByParent(String patternToFind) {
        return results.massChooseByParent(patternToFind);
    }
    protected int[] massChooseByRoot(String patternToFind) {
        return results.massChooseByRoot(patternToFind);
    }
    protected void removeSelectionsByChecksum(String checksum) {
        results.unselectByChecksum(checksum);
    }
    protected void removeSelectionsAll() {
        results.unselectAll();
        if (deleterChunk != null) deleterChunk.updateChunk();
    }
    protected Set<File> getFilesThatRemains() {
        return results.getFilesThatRemains();
    }
    protected Set<File> getFilesToDelete() {
        return results.getFilesToDelete();
    }
    protected boolean isFileChosen(File file) {
        return results.isFileChosen(file);
    }
    protected boolean isChoiceMadeOnChecksum(String checksum) {
        return results.isChoiceMadeOnChecksum(checksum);
    }


    //Memory saving
    protected void cleanOldResults() {
        results = null;
        deleterChunk = null;
    }

    //Setup chunk features
    protected FindDuplicatesTask getTask() {
        return setupChunk.getTask();
    }

    //Deleter chunk features
    protected void removeSelectionsCurrent() {
        deleterChunk.unselectCurrent();
        deleterChunk.updateChunk();
    }
    protected void updateDeleterChunk() {
        if (deleterChunk != null) {
            deleterChunk.updateChunk();
        }
    }
    protected void updateChecksumRepresentation() {
        deleterChunk.updateChecksumTextRepresentation();
    }
}

package com.unduplicator.gui;

import java.io.*;
import java.util.*;

/**
 * <p>Created by MontolioV on 30.08.17.
 */
public class Results {
    private ChunkManager chunkManager;

    private Map<String, List<File>> processedFilesMap;
    private Set<String> duplicateChSumSet;

    public Results(ChunkManager chunkManager, Map<String, List<File>> processedFilesMap) {
        this.processedFilesMap = processedFilesMap;
        this.chunkManager = chunkManager;
        makeDuplicateSet();
    }

    public Results(ChunkManager chunkManager, File serializationFile) {
        this.chunkManager = chunkManager;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serializationFile))) {
            processedFilesMap = (Map<String, List<File>>) ois.readObject();
        } catch (Exception e) {
            chunkManager.showException(e);
        }
        makeDuplicateSet();
    }

    protected int countDuplicates() {
        int result = 0;
        for (Map.Entry<String, List<File>> entry : processedFilesMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                result += entry.getValue().size() - 1;
            }
        }
        return result;
    }

    protected void removeMissingFiles() {
        processedFilesMap.forEach((s, files) ->{
            ArrayList<File> updatedList = new ArrayList<>();
            for (File file : files) {
                if (file.exists()) {
                    updatedList.add(file);
                }
            }
            processedFilesMap.replace(s, updatedList);
        });
        makeDuplicateSet();
        chunkManager.updateDeleterChunk();
    }

    private void makeDuplicateSet() {
        duplicateChSumSet = new HashSet<>();
        processedFilesMap.entrySet().stream().filter(entry -> entry.getValue().size() > 1).forEach(entry -> duplicateChSumSet.add(entry.getKey()));
    }

    protected List<File> getFilesListCopy(String checksumKey) {
        for (File file : processedFilesMap.get(checksumKey)) {
            if (!file.exists()) {
                removeMissingFiles();
                break;
            }
        }
        return new ArrayList<>(processedFilesMap.get(checksumKey));
    }

    protected Set<String> getDuplicateChecksumSet() {
        return duplicateChSumSet;
    }

    protected void saveToFile(File file) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(processedFilesMap);
        } catch (IOException e) {
            chunkManager.showException(e);
        }
    }
}

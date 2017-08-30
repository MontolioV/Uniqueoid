package com.unduplicator.gui;

import javafx.collections.ObservableList;

import java.io.File;
import java.util.*;

/**
 * <p>Created by MontolioV on 30.08.17.
 */
public class Results {
    private Map<String, List<File>> processedFilesMap;

    public Results(Map<String, List<File>> processedFilesMap) {
        this.processedFilesMap = processedFilesMap;
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
    }

    protected List<File> getFiles(String checksumKey) {
        return new ArrayList<>(processedFilesMap.get(checksumKey));
    }

    protected Set<String> getCheckSums() {
        return new HashSet<>(processedFilesMap.keySet());
    }
}

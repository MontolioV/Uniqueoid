package com.unduplicator.gui;

import com.unduplicator.ResourcesProvider;
import javafx.concurrent.Task;

import java.io.*;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.function.BiPredicate;

/**
 * <p>Created by MontolioV on 30.08.17.
 */
public class Results {
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();
    private ChunkManager chunkManager;

    private Map<String, Set<File>> processedFilesMap;
    private Set<String> duplicateChSumSet;
    private Set<File> filesToDelete = new HashSet<>();
    private Map<File, String> filesThatRemains = new HashMap<>();
    private Set<String> duplicateChoiceMade = new HashSet<>();

    public Results(ChunkManager chunkManager, Map<String, Set<File>> processedFilesMap) {
        this.processedFilesMap = processedFilesMap;
        this.chunkManager = chunkManager;
        makeDuplicateSet();
    }

    public Results(ChunkManager chunkManager, File serializationFile) {
        this.chunkManager = chunkManager;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serializationFile))) {
            processedFilesMap = (Map<String, Set<File>>) ois.readObject();
        } catch (Exception e) {
            chunkManager.showException(e);
        }
        makeDuplicateSet();
    }

    protected int countDuplicates() {
        int result = 0;
        for (Map.Entry<String, Set<File>> entry : processedFilesMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                result += entry.getValue().size() - 1;
            }
        }
        return result;
    }

    protected void removeMissingFiles() {
        processedFilesMap.forEach((s, files) ->{
            HashSet<File> updatedSet = new HashSet<>();
            for (File file : files) {
                if (file.exists()) {
                    updatedSet.add(file);
                }
            }
            processedFilesMap.replace(s, updatedSet);
        });
        makeDuplicateSet();
        chunkManager.updateDeleterChunk();
    }

    private void makeDuplicateSet() {
        duplicateChSumSet = new HashSet<>();
        processedFilesMap.entrySet().stream().filter(entry -> entry.getValue().size() > 1).forEach(entry -> duplicateChSumSet.add(entry.getKey()));
    }

    protected Set<File> getDuplicateFilesCopy(String checksumKey) {
        for (File file : processedFilesMap.get(checksumKey)) {
            if (!file.exists()) {
                removeMissingFiles();
                break;
            }
        }
        return new HashSet<>(processedFilesMap.get(checksumKey));
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

    protected void chooseOneAmongDuplicates(String checksum, File fileThatRemains) {
        Set<File> duplList = processedFilesMap.get(checksum);
        for (File file : duplList) {
            if (!file.exists()) {
                removeMissingFiles();
                break;
            }
        }
        if (!duplList.contains(fileThatRemains)) {
            chunkManager.showException(new NoSuchFileException(fileThatRemains.toString()));
            return;
        }

        filesThatRemains.put(fileThatRemains, checksum);
        filesToDelete.remove(fileThatRemains);
        duplList.stream().filter(file -> !file.equals(fileThatRemains)).forEach(fileToDelete->{
            filesThatRemains.remove(fileToDelete);
            filesToDelete.add(fileToDelete);
        });
        duplicateChoiceMade.add(checksum);
        chunkManager.updateChecksumRepresentation();
    }

    protected int[] massChooseByParent(String patternToFind) {
        BiPredicate<File, String> byParent = (file, parentToFind) -> file.getParent().equals(parentToFind);
        return massChoose(byParent, patternToFind);
    }

    protected int[] massChooseByRoot(String patternToFind) {
        BiPredicate<File, String> byRoot = (file, rootToFind) -> file.getParent().startsWith(rootToFind);
        return massChoose(byRoot, patternToFind);
    }

    private int[] massChoose(BiPredicate<File, String> chooseCondition, String patternToFind) {
        int saveCounter = 0;
        int delCounter = 0;

        for (String checksum : duplicateChSumSet) {
            Set<File> duplicates = processedFilesMap.get(checksum);
            for (File duplicate : duplicates) {
                if (chooseCondition.test(duplicate, patternToFind)) {
                    chooseOneAmongDuplicates(checksum, duplicate);
                    delCounter += duplicates.size() - 1;
                    saveCounter++;
                    break;
                }
            }
        }
        int[] result = {saveCounter, delCounter};
        return result;
    }

    protected void unselectByChecksum(String checksum) {
        if (checksum == null || checksum.equals("")) return;

        Set<File> files = processedFilesMap.get(checksum);
        if (files != null) {
            files.forEach(file -> {
                filesToDelete.remove(file);
                filesThatRemains.remove(file);
            });
        }
        duplicateChoiceMade.remove(checksum);
    }

    protected void unselectAll() {
        filesToDelete = new HashSet<>();
        filesThatRemains = new HashMap<>();
        duplicateChoiceMade = new HashSet<>();
    }

    protected Set<File> getFilesThatRemains() {
        Set<File> result = new HashSet<>();
        Set<String> checksumsToUnselect = new HashSet<>();
        for (Map.Entry<File, String> entry : filesThatRemains.entrySet()) {
            if (entry.getKey().exists()) {
                result.add(entry.getKey());
            } else {
                checksumsToUnselect.add(entry.getValue());
            }
        }

        checksumsToUnselect.forEach(this::unselectByChecksum);
        if (checksumsToUnselect.size() > 0) removeMissingFiles();
        return result;
    }

    protected Set<File> getFilesToDelete() {
        Set<File> updatedSet = new HashSet<>();
        filesToDelete.stream().filter(File::exists).forEach(updatedSet::add);
        if (updatedSet.size() != filesToDelete.size()) {
            filesToDelete = new HashSet<>(updatedSet);
            removeMissingFiles();
        }
        return updatedSet;
    }

    protected boolean isFileChosen(File file) {
        return filesThatRemains.containsKey(file);
    }

    protected boolean isChoiceMadeOnChecksum(String checksum) {
        return duplicateChoiceMade.contains(checksum);
    }

    protected Task<Map<String, Set<File>>> addToPreviousResultTask() {
        return chunkManager.getAddToResultsTask(processedFilesMap);
    }
}

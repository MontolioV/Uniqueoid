package io.sourceforge.uniqueoid.gui;

import javafx.concurrent.Task;

import java.io.*;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;

/**
 * <p>Created by MontolioV on 30.08.17.
 */
public class Results {
    private ChunkManager chunkManager;

    private AtomicLong updateTimeStamp = new AtomicLong();

    private Map<String, Set<File>> processedFilesMap;
    private Set<String> duplicateChSumSet;
    private Set<File> filesToDelete = new HashSet<>();
    private Map<File, String> filesThatRemains = new HashMap<>();
    private Set<String> duplicateChoiceMade = new HashSet<>();

    private BiPredicate<File, String> byParentPredicate = (file, parentToFind) -> {
        if (parentToFind.equals("")) return false;
        return file.getParent().equals(parentToFind);
    };
    private BiPredicate<File, String> byRootPredicate = (file, rootToFind) -> {
        if (rootToFind.equals("")) return false;
        return file.getParent().startsWith(rootToFind);
    };

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
    protected void saveToFile(File file) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(processedFilesMap);
        } catch (IOException e) {
            chunkManager.showException(e);
        }
    }

    protected int countDuplicates() {
        int result = 0;
        for (Map.Entry<String, Set<File>> entry : processedFilesMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                result += entry.getValue().size();
            }
        }
        return result;
    }

    protected void update() {
        if ((System.currentTimeMillis() - updateTimeStamp.get()) < 500) {
            return;
        }

        processedFilesMap.forEach((s, files) ->{
            HashSet<File> updatedSet = null;
            for (File file : files) {
                if (!file.exists()) {
                    if (updatedSet == null) {
                        updatedSet = new HashSet<>(files);
                    }
                    updatedSet.remove(file);
                }
            }
            if (updatedSet != null) {
                processedFilesMap.replace(s, updatedSet);
            }
        });
        makeDuplicateSet();
        chunkManager.updateDeleterChunk();

        updateTimeStamp.set(System.currentTimeMillis());
    }
    private void makeDuplicateSet() {
        duplicateChSumSet = new HashSet<>();
        processedFilesMap.entrySet().stream().filter(entry -> entry.getValue().size() > 1).forEach(entry -> duplicateChSumSet.add(entry.getKey()));
    }
    private void validateFiles(String checksum) {
        for (File file : processedFilesMap.get(checksum)) {
            if (!file.exists()) {
                update();
                break;
            }
        }
    }

    protected Set<File> getDuplicateFilesCopy(String checksumKey) {
        validateFiles(checksumKey);
        return new HashSet<>(processedFilesMap.get(checksumKey));
    }
    protected Set<String> getDuplicateChecksumSet() {
        return duplicateChSumSet;
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
        if (checksumsToUnselect.size() > 0) update();
        return result;
    }
    protected Set<File> getFilesToDelete() {
        Set<File> updatedSet = new HashSet<>();
        filesToDelete.stream().filter(File::exists).forEach(updatedSet::add);
        if (updatedSet.size() != filesToDelete.size()) {
            filesToDelete = new HashSet<>(updatedSet);
            update();
        }
        return updatedSet;
    }

    protected void chooseOneAmongDuplicates(String checksum, File fileThatRemains) {
        validateFiles(checksum);

        Set<File> duplList = processedFilesMap.get(checksum);
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
    }
    protected int[] massChooseByParent(String patternToFind) {
        return massChoose(byParentPredicate, patternToFind);
    }
    protected int[] massChooseByRoot(String patternToFind) {
        return massChoose(byRootPredicate, patternToFind);
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

    protected boolean isFileChosen(File file) {
        return filesThatRemains.containsKey(file);
    }
    protected boolean isChoiceMadeOnChecksum(String checksum) {
        return duplicateChoiceMade.contains(checksum);
    }

    protected Task<Map<String, Set<File>>> addToPreviousResultTask() {
        return chunkManager.getAddToResultsTask(processedFilesMap);
    }
    protected void ignoreDuplicate(String checksum, File duplicate) {
        removeFromResults(checksum, duplicate);
        update();
    }
    protected void ignoreDuplicatesFromDirectory(String directoryString) {
        massIgnore(byParentPredicate, directoryString);
    }
    protected void ignoreDuplicatesFromRoot(String rootString) {
        massIgnore(byRootPredicate, rootString);
    }
    private void massIgnore(BiPredicate<File, String> ignoreCondition, String patternToFind) {
        HashMap<File, String> filesWithChecksumsToIgnore = new HashMap<>();

        for (Map.Entry<String, Set<File>> mainMapEntry : processedFilesMap.entrySet()) {
            for (File duplicate : mainMapEntry.getValue()) {
                if (ignoreCondition.test(duplicate, patternToFind)) {
                    filesWithChecksumsToIgnore.put(duplicate, mainMapEntry.getKey());
                }
            }
        }

        if (filesWithChecksumsToIgnore.size() == 0) return;
        for (Map.Entry<File, String> entry : filesWithChecksumsToIgnore.entrySet()) {
            removeFromResults(entry.getValue(), entry.getKey());
        }
        update();
    }
    private void removeFromResults(String checksum, File file) {
        if (file == null) return;

        filesToDelete.remove(file);
        if (filesThatRemains.containsKey(file)) {
            unselectByChecksum(checksum);
        }

        processedFilesMap.get(checksum).remove(file);   //it must be the last action
    }

    protected int[] getStatistics(String checksum) {
        int[] result = new int[4];
        result[0] = duplicateChSumSet.size();
        if (checksum == null || checksum.equals("")) {
            result[1] = 0;
        } else {
            result[1] = processedFilesMap.get(checksum).size();
        }
        result[2] = filesThatRemains.size();
        result[3] = filesToDelete.size();
        return result;
    }
}

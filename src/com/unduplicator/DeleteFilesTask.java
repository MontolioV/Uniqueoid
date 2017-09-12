package com.unduplicator;

import javafx.concurrent.Task;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.*;

/**
 * <p>Created by MontolioV on 12.07.17.
 */
public class DeleteFilesTask extends Task<List<File>> {
    private List<File> fileList;
    private Set<File> dirsToDelete = new HashSet<>();
    private List<File> notDeletedFileList = new ArrayList<>();
    private double progress = 0;
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();


    /**
     * Creates a new Task.
     */
    public DeleteFilesTask(List<File> fileList) {
        super();
        this.fileList = fileList;
    }

    @Override
    protected List<File> call() throws Exception {
        deleteFiles();
        markEmptyDirs();
        deleteEmptyDirs();
        return notDeletedFileList;
    }

    private void deleteFiles() {
        int counter = 0;

        for (File file : fileList) {
            if (file.isDirectory()) {
                cleanDir(file);
            } else {
                deleteFile(file);
            }
            progress += 0.5 * (++counter / fileList.size());
            updateProgress(progress, 1);
        }
    }

    private void cleanDir(File dir) {
        dirsToDelete.add(dir);
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                cleanDir(file);
            } else {
                deleteFile(file);
            }
        }
    }

    private void deleteFile(File file) {
        boolean deleted = file.delete();
        if (!deleted) {
            notDeletedFileList.add(file);
        }
    }

    private void markEmptyDirs() throws NoSuchFileException {
        int counter = 0;
        for (File file : fileList) {
            if (file.getParentFile() == null) {
                throw new NoSuchFileException(
                        resProvider.getStrFromExceptionBundle("hasNoParent") +
                                "\t" + file.toString());
            } else {
                addEmptyDirToList(file.getParentFile());
            }
            progress += 0.25 * (++counter / fileList.size());
            updateProgress(progress, 1);
        }
    }

    private boolean addEmptyDirToList(File dir) {
        if (!dir.isDirectory()) {
            return false;
        }
        if (dir.listFiles().length == 0) {
            dirsToDelete.add(dir);
            return true;
        } else {
            boolean canBeDeleted = true;
            for (File file : dir.listFiles()) {
                if (!addEmptyDirToList(file)) {
                    canBeDeleted = false;
                    break;
                }
            }
            if (canBeDeleted) {
                dirsToDelete.add(dir);
            }
            return canBeDeleted;
        }
    }

    private void deleteEmptyDirs() {
        //Sort dirs to avoid situation, when dir can't be deleted cause it contains another empty dir
        Comparator<File> pathDepthComp = Comparator.comparingInt(f -> f.toPath().toAbsolutePath().getNameCount());
        ArrayList<File> dirsToDeleteAL = new ArrayList<>(dirsToDelete);
        dirsToDeleteAL.sort(pathDepthComp.reversed());

        int counter = 0;
        for (File file : dirsToDeleteAL) {
            if (file.exists() && !file.delete()) {
                notDeletedFileList.add(file);
            }
            progress += 0.25 * (++counter / dirsToDeleteAL.size());
            updateProgress(progress, 1);
        }
    }
}

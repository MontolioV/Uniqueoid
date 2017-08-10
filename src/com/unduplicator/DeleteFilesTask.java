package com.unduplicator;

import javafx.concurrent.Task;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <p>Created by MontolioV on 12.07.17.
 */
public class DeleteFilesTask extends Task<List<File>> {
    private List<File> fileList;
    private ArrayList<File> dirsToDelete = new ArrayList<>();
    private List<File> notDeletedFileList = new ArrayList<>();

    /**
     * Creates a new Task.
     */
    public DeleteFilesTask(List<File> fileList) {
        super();
        this.fileList = fileList;
    }

    @Override
    protected List<File> call() throws Exception {
        int counter = 0;
        double progress = 0;
        //Delete files
        for (File file : fileList) {
            if (file.isDirectory()) {
                deleteDir(file);
            } else if (!file.delete()) {
                notDeletedFileList.add(file);
            }
            progress += 0.5 * (++counter / fileList.size());
            updateProgress(progress, 1);
        }

        //Delete empty dirs
        counter = 0;
        for (File file : fileList) {
            if (file.getParentFile() == null) {
                throw new NoSuchFileException("Hasn't got parent: " + file.toString());
            } else {
                deleteOnlyEmptyDir(file.getParentFile());
            }
            progress += 0.25 * (++counter / fileList.size());
            updateProgress(progress, 1);
        }

        //Sort dirs to avoid situation, when dir can't be deleted cause it contains another empty dir
        Comparator<File> pathDepthComp = Comparator.comparingInt(f -> f.toPath().toAbsolutePath().getNameCount());
        dirsToDelete.sort(pathDepthComp.reversed());

        counter = 0;
        for (File file : dirsToDelete) {
            if (file.exists() && !file.delete()) {
                notDeletedFileList.add(file);
            }
            progress += 0.25 * (++counter / dirsToDelete.size());
            updateProgress(progress, 1);
        }

        return notDeletedFileList;
    }

    private void deleteDir(File dir) {
        dirsToDelete.add(dir);
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                deleteDir(file);
            } else if (!file.delete()) {
                notDeletedFileList.add(file);
            }
        }
    }

    private boolean deleteOnlyEmptyDir(File dir) {
        if (dir.listFiles().length == 0) {
            dirsToDelete.add(dir);
            return true;
        } else {
            boolean canBeDeleted = true;
            for (File file : dir.listFiles()) {
                if (!file.isDirectory()) {
                    canBeDeleted = false;
                } else {
                    if (!deleteOnlyEmptyDir(file)) {
                        canBeDeleted = false;
                    }
                }
            }
            if (canBeDeleted) {
                dirsToDelete.add(dir);
            }
            return canBeDeleted;
        }
    }
}

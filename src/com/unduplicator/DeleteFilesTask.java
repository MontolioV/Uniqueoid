package com.unduplicator;

import javafx.concurrent.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <p>Created by MontolioV on 12.07.17.
 */
public class DeleteFilesTask extends Task<List<File>> {
    private List<File> fileList;

    /**
     * Creates a new Task.
     */
    public DeleteFilesTask(List<File> fileList) {
        super();
        this.fileList = fileList;
    }

    @Override
    protected List<File> call() throws Exception {
        List<File> notDeletedFileList = new ArrayList<>();
        ArrayList<File> dirsToDelete = new ArrayList<>();

        for (File file : fileList) {
            if (!file.delete()) {
                if (file.isDirectory()) {
                    dirsToDelete.add(file);
                } else {
                    notDeletedFileList.add(file);
                }
            }
        }

        Comparator<File> pathDepthComp = Comparator.comparingInt(f -> f.toPath().getNameCount());
        dirsToDelete.sort(pathDepthComp.reversed());



        return notDeletedFileList;
    }
}

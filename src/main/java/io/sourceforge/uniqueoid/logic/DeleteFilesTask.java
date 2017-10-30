package io.sourceforge.uniqueoid.logic;

import javafx.concurrent.Task;

import java.io.File;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * <p>Created by MontolioV on 12.07.17.
 */
public class DeleteFilesTask extends Task<Set<File>> {
    private Set<File> taskFiles;
    private Set<File> dirsToDelete = new HashSet<>();
    private Set<File> reportSet = new HashSet<>();

    /**
     * Creates a new Task.
     */
    public DeleteFilesTask(Set<File> fileList) {
        super();
        this.taskFiles = fileList;
    }

    @Override
    protected Set<File> call() throws Exception {
        riseStep(taskFiles);
        return reportSet;
    }

    private void riseStep(Set<File> files) {
        if (files == null || isCancelled()) return;

        files.forEach(this::extract);
        riseStep(deleteTheDeepest());
    }

    private void extract(File file) {
        if (file.exists()) {
            dirsToDelete.add(file);
        } else if (file.getParentFile() != null) {
            dirsToDelete.add(file.getParentFile());
        }
    }

    //Avoiding situation, when dir can't be deleted cause it contains another empty dir
    private Set<File> deleteTheDeepest() {
        if (dirsToDelete.isEmpty()) return null;

        Set<File> deleted = new HashSet<>();
        Set<File> notDeleted = new HashSet<>();

        ToIntFunction<File> getPathDepth = file -> file.toPath().toAbsolutePath().getNameCount();
        Comparator<File> pathDepthComp = Comparator.comparingInt(getPathDepth::applyAsInt);

        int maxDepth = getPathDepth.applyAsInt(dirsToDelete.stream().max(pathDepthComp).get());

        dirsToDelete.stream()
                .filter(f -> getPathDepth.applyAsInt(f) == maxDepth)
                .forEach(file -> {
                    if (file.delete()) {
                        deleted.add(file);
                    } else if (file.exists()) {
                        notDeleted.add(file);
                    }
                });
        dirsToDelete.removeAll(deleted);
        dirsToDelete.removeAll(notDeleted);
        notDeleted.stream().filter(taskFiles::contains).forEach(reportSet::add);
        updateProgress(1 / ((double) 1 + dirsToDelete.size()), 1);
        return deleted;
    }
}

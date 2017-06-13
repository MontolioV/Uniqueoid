package com.unduplicator;

import javafx.concurrent.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides task of finding duplicating files in given directories.
 * Organize such copies in hashmap by checksums.
 * <p>Created by MontolioV on 12.06.17.
 */
public class UnduplicatorTask extends Task<HashMap<String, List<File>>> {
    private final List<File> DIRECTORIES;
    private final String HASH_ALGORITHM;
    private AtomicInteger fileCounter = new AtomicInteger();
    private AtomicLong bitCounter = new AtomicLong();

    public UnduplicatorTask(List<File> directories, String hash_algorithm) {
        this.DIRECTORIES = directories;
        HASH_ALGORITHM = hash_algorithm;
    }

    /**
     * Invoked when the Task is executed. The call method actually performs the
     * background thread logic. Only the updateProgress, updateMessage, updateValue and
     * updateTitle methods of Task may be called from code within this method.
     * Any other interaction with the Task from the background thread will result
     * in runtime exceptions.
     *
     * @return Mapping of list of duplicate files (value) to file checksum (key).
     * @throws Exception an unhandled exception which occurred during the
     *                   background operation
     */
    @Override
    protected HashMap<String, List<File>> call() throws Exception {
        ConcurrentLinkedQueue<FileAndChecksum> queueProcessed = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> queueExMessages = new ConcurrentLinkedQueue<>();



        Thread fjThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ForkJoinPool fjPool = ForkJoinPool.commonPool();
                List<Callable<Void>> taskList = new ArrayList<>();
                DIRECTORIES.forEach(file -> taskList.add(new
                        new DirectoryHandler(file,
                                                                              HASH_ALGORITHM,
                                                                              queueProcessed,
                                                                              queueExMessages)));
                fjPool.invoke
            }
        });


        return;
    }
}

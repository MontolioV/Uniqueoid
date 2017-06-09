package com.unduplicator;

import java.io.*;
import java.util.ArrayList;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

/**
 * Directory Handler takes directory and puts checksums of all files recursively in storage.
 * <p>Created by MontolioV on 06.06.17.
 */
public class DirectoryHandler extends RecursiveAction {
    private final File directory;
    private final CheckSumMaker checkSumMaker;
    private final ConcurrentLinkedQueue<FileAndChecksum> queueFilesChSs;
    private final ConcurrentLinkedQueue<String> queueExMessages;
    private final String algorithm;

    public DirectoryHandler(File directory,
                            String hashAlgorithm,
                            ConcurrentLinkedQueue<FileAndChecksum> queueFilesChSs,
                            ConcurrentLinkedQueue<String> queueExMessages)
    {
        this.checkSumMaker = new CheckSumMaker(hashAlgorithm);
        this.directory = directory;
        this.queueFilesChSs = queueFilesChSs;
        this.algorithm = hashAlgorithm;
        this.queueExMessages = queueExMessages;
    }

    /**
     * The main computation performed by this task.
     */
    @Override
    protected void compute() {
        ArrayList<DirectoryHandler> tasks = new ArrayList<>();

        if (directory.listFiles() == null) {
            throw new IllegalArgumentException("Choose a folder, not a file.");
        }
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                DirectoryHandler task = new DirectoryHandler(file, algorithm, queueFilesChSs, queueExMessages);
                task.fork();
                tasks.add(task);
            } else {
                try {
                    FileAndChecksum pair = new FileAndChecksum(file, checkSumMaker.makeCheckSum(file));
                    queueFilesChSs.offer(pair);
                } catch (IOException e) {
                    StringJoiner sj = new StringJoiner("\n");
                    sj.add(e.toString());

                    Exception causeExc = (Exception) e.getCause();
                    while (causeExc != null) {
                        sj.add(causeExc.toString());
                        causeExc = (Exception) causeExc.getCause();
                    }

                    queueExMessages.offer(sj.toString());
                }
            }
        }

        tasks.forEach(ForkJoinTask::join);
    }
}

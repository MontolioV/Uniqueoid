package com.unduplicator.logic;

import com.unduplicator.ResourcesProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

/**
 * Directory Handler takes directory and puts checksums of all files recursively in storage.
 * ForkJoin divide and conquer strategy. Faster and more resource demanding.
 * <p>Created by MontolioV on 06.06.17.
 */
public class DirectoryHandler extends RecursiveAction {
    private static final List<DirectoryHandler> longTasks = Collections.synchronizedList(new ArrayList<>());

    private final File[] FILES_TO_HANDLE;
    private final CheckSumMaker CHECKSUM_MAKER;
    private final ConcurrentLinkedQueue<FileAndChecksum> QUEUE_FILE_AND_CHECKSUM;
    private final ConcurrentLinkedQueue<String> QUEUE_EX_MESSAGES;
    private final String HASH_ALGORITHM;
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();
    private ArrayList<ForkJoinTask<Void>> fastTasks = new ArrayList<>();
    private final long LARGE_FILE_SIZE = (long) (Math.pow(2, 20) * 10);    //10 MB

    public DirectoryHandler(File file,
                            String hashAlgorithm,
                            ConcurrentLinkedQueue<FileAndChecksum> queueProcessed,
                            ConcurrentLinkedQueue<String> queueExMessages) {
        this(new File[]{file}, hashAlgorithm, queueProcessed, queueExMessages);
    }
    public DirectoryHandler(File[] files,
                                 String hashAlgorithm,
                                 ConcurrentLinkedQueue<FileAndChecksum> queueProcessed,
                                 ConcurrentLinkedQueue<String> queueExMessages) {

        this.CHECKSUM_MAKER = new CheckSumMaker(hashAlgorithm);
        this.FILES_TO_HANDLE = files;
        this.QUEUE_FILE_AND_CHECKSUM = queueProcessed;
        this.HASH_ALGORITHM = hashAlgorithm;
        this.QUEUE_EX_MESSAGES = queueExMessages;
    }

    /**
     * The main computation performed by this task.
     */
    @Override
    protected void compute() {
        File[] remainder = splitLargeDirectory();
        analizeFiles(remainder);
        fastTasks.forEach(ForkJoinTask::join);
    }

    protected void analizeFiles(File[] files) {
        for (File file : files) {
            if (Files.isSymbolicLink(file.toPath())) {
                try {
                    QUEUE_EX_MESSAGES.offer(resProvider.getStrFromMessagesBundle("isLink")
                            + "\n" + resProvider.getStrFromMessagesBundle("link") + file.toString()
                            + "\n" + resProvider.getStrFromMessagesBundle("realPath") + file.toPath().toRealPath().toString());
                } catch (IOException e) {
                    exceptionToQueue(e);
                }
            } else if (file.isFile()) {
                if (file.length() < LARGE_FILE_SIZE) {
                    processFile(file);
                } else {
                    split(file);
                }
            } else if (file.isDirectory()) {
                if (file.listFiles() == null) {
                    QUEUE_EX_MESSAGES.offer(
                            resProvider.getStrFromMessagesBundle("cantGetFilesFromDir") +
                                    "\t" + file.toString());
                } else {
                    split(file.listFiles());
                }
            } else {
                QUEUE_EX_MESSAGES.offer(
                        resProvider.getStrFromMessagesBundle("notFileNotDir") +
                                "\t" + file.toString());
            }
        }
    }

    protected void split(File largeFile) {
        QUEUE_EX_MESSAGES.offer(resProvider.getStrFromMessagesBundle("bigFile") +
                "\t" + largeFile);
        DirectoryHandler longTask = new DirectoryHandlerSoloThread(largeFile,
                HASH_ALGORITHM,
                QUEUE_FILE_AND_CHECKSUM,
                QUEUE_EX_MESSAGES);
        longTasks.add(longTask);
        longTask.fork();
    }
    protected void split(File[] files) {
        DirectoryHandler fastTask = new DirectoryHandler(files,
                HASH_ALGORITHM,
                QUEUE_FILE_AND_CHECKSUM,
                QUEUE_EX_MESSAGES);
        fastTasks.add(fastTask);
        fastTask.fork();
    }

    protected void processFile(File file) {
        try {
            FileAndChecksum pair = new FileAndChecksum(file, CHECKSUM_MAKER.makeCheckSum(file));
            QUEUE_FILE_AND_CHECKSUM.offer(pair);
        } catch (IOException ex) {
            exceptionToQueue(ex);
        }
    }

    private void exceptionToQueue(Exception ex) {
        ex.printStackTrace();

        StringJoiner sj = new StringJoiner("\n");
        sj.add(ex.toString());

        Exception causeExc = (Exception) ex.getCause();
        while (causeExc != null) {
            sj.add(causeExc.toString());
            causeExc = (Exception) causeExc.getCause();
        }

        QUEUE_EX_MESSAGES.offer(sj.toString());
    }

    private File[] splitLargeDirectory() {
        int THRESHOLD = 100;
        if (FILES_TO_HANDLE.length <= THRESHOLD) {
            return FILES_TO_HANDLE;
        }

        int tasksNumber = FILES_TO_HANDLE.length / THRESHOLD;
        for (int i = 0; i < tasksNumber; i++) {
            File[] files = Arrays.copyOfRange(FILES_TO_HANDLE,
                    i * THRESHOLD, i * THRESHOLD + THRESHOLD);
            split(files);
        }
        return Arrays.copyOfRange(FILES_TO_HANDLE, THRESHOLD * tasksNumber, FILES_TO_HANDLE.length);
    }

    public static void joinLongTasks() {
        longTasks.forEach(DirectoryHandler::join);
    }
}

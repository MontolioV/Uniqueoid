package com.unduplicator.logic;

import com.unduplicator.GlobalFiles;
import com.unduplicator.ResourcesProvider;
import javafx.concurrent.Task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

/**
 * Provides task of finding duplicating files in given directories.
 * Groups files by their checksum so that copies are easy to find.
 * Divide and conquer strategy.
 * <p>Created by MontolioV on 12.06.17.
 */
public class FindDuplicatesTask extends Task<Map<String, Set<File>>> {
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();

    private final List<File> DIRECTORIES;
    private final String HASH_ALGORITHM;
    private Map<String, Set<File>> mapToReturn = new HashMap<>();
    private ConcurrentLinkedQueue<FileAndChecksum> queueProcessed = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<String> queueExMessages = new ConcurrentLinkedQueue<>();
    private ForkJoinPool fjPool = new ForkJoinPool();
    private Thread fjThread;
    private int filesCounter = 0;
    private int filesTotal = 0;
    private long byteCounter = 0;
    private long byteTotal = 0;
    private BufferedWriter logBW;

    public FindDuplicatesTask(List<File> directories, String hash_algorithm) {
        super();
        this.DIRECTORIES = directories;
        HASH_ALGORITHM = hash_algorithm;
    }
    public FindDuplicatesTask(List<File> directories, String hash_algorithm, Map<String, Set<File>> previousResult) {
        this(directories, hash_algorithm);
        mapToReturn = previousResult;
    }


    /**
     * Invoked when the Task is executed. The call method actually performs the
     * background thread logic. Only the updateProgress, updateMessage, updateValue and
     * updateTitle methods of Task may be called from code within this method.
     * Any other interaction with the Task from the background thread will result
     * in runtime exceptions.
     *
     * @return Mapping of list of files (value) to file checksum (key).
     *         If list.size() == 1 than there're no copies.
     * @throws Exception an unhandled exception which occurred during the
     *                   background operation
     */
    @Override
    protected Map<String, Set<File>> call() throws Exception {
        try {
            preparations();
            runTasksInNewThread();
            controlAndOutputResult();
            finishing();
        } finally {
            if (logBW != null) {
                logBW.close();
            }
        }
        return mapToReturn;
    }

    private void preparations() {
        try {
            logBW = new BufferedWriter(new FileWriter(GlobalFiles.getInstance().getLogFile()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateMessage(resProvider.getStrFromMessagesBundle("countFiles"));
        DIRECTORIES.forEach(dir -> filesTotal += countFiles(dir));
        updateMessage(resProvider.getStrFromMessagesBundle("totalFiles") + filesTotal);
    }

    public int countFiles(File dir) {
        if (dir == null || isCancelled() || Files.isSymbolicLink(dir.toPath())) return 0;

        if (dir.isDirectory()) {
            int result = 0;
            if (dir.listFiles() == null) {
                updateMessage(resProvider.getStrFromMessagesBundle("countFail") +
                        "\t" + dir.toString());
            } else {
                for (File file : dir.listFiles()) {
                    if (file.isDirectory()) {
                        result += countFiles(file);
                    } else {
                        result++;
                        byteTotal += file.length();
                    }
                }
            }
            return result;
        } else {
            return 1;
        }
    }

    private void runTasksInNewThread() {
        fjThread = new Thread(() -> {
            File[] files = new File[DIRECTORIES.size()];
            files = DIRECTORIES.toArray(files);
            DirectoryHandler directoryHandler = makeDirectoryHandler(files, HASH_ALGORITHM, queueProcessed, queueExMessages);
            fjPool.execute(directoryHandler);
            try {
                directoryHandler.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                updateMessage(e.toString() + "\n");
            }
            DirectoryHandler.joinRunningTasks();
        });

        fjThread.start();
    }
    protected DirectoryHandler makeDirectoryHandler(File[] files, String hashAlgorithm, ConcurrentLinkedQueue<FileAndChecksum> queueProcessed, ConcurrentLinkedQueue<String> queueExMessages) {
        return new DirectoryHandler(files, hashAlgorithm, queueProcessed, queueExMessages);
    }

    private void controlAndOutputResult() throws InterruptedException {
        StringJoiner sbMessages = new StringJoiner("\n");
        Formatter formatter = new Formatter();
        while (fjThread.isAlive() || !queueProcessed.isEmpty() || !queueExMessages.isEmpty()) {
            //Cancellation
            if (isCancelled()) {
                fjPool.shutdownNow();
                break;
            }

            if (queueProcessed.isEmpty() && queueExMessages.isEmpty()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    //No problem
                }
            }

            if (!queueProcessed.isEmpty()) {
                FileAndChecksum pair = queueProcessed.poll();
                //Here we find duplicated files
                Set<File> copies = mapToReturn.get(pair.getChecksum());
                if (copies == null) {
                    Set<File> newList = new HashSet<>();
                    newList.add(pair.getFile());
                    mapToReturn.put(pair.getChecksum(), newList);
                } else {
                    if (!copies.contains(pair.getFile())) {
                        copies.add(pair.getFile());
                    }
                }

                filesCounter++;
                byteCounter += pair.getFile().length();
            }

            if (!queueExMessages.isEmpty()) {
                sbMessages.add(queueExMessages.poll());
            } else if (sbMessages.length() > 0) {
                updateMessage(sbMessages.toString());
                sbMessages = new StringJoiner("\n");
            }

            formatter.format("Parallelism: %s, Size: %s, Active: %s, Running: %s, Steals: %s, Tasks: %s, Submissions: %s",
                    fjPool.getParallelism(),
                    fjPool.getPoolSize(),
                    fjPool.getActiveThreadCount(),
                    fjPool.getRunningThreadCount(),
                    fjPool.getStealCount(),
                    fjPool.getQueuedTaskCount(),
                    fjPool.getQueuedSubmissionCount());
            updateTitle(formatter.toString());
            formatter = new Formatter();
            updateProgress(byteCounter, byteTotal);
        }

        updateMessage(resProvider.getStrFromMessagesBundle("hashsumsCount") + filesCounter);
    }

    protected void finishing() throws InterruptedException {
        //Just to be sure
        fjThread.join();
    }

    /**
     * Writes message to log file as well.
     *
     * @param message the new message
     */
    @Override
    protected void updateMessage(String message) {
        super.updateMessage(message);
        if (logBW != null) {
            try {
                logBW.write(message);
                logBW.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

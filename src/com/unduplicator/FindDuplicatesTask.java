package com.unduplicator;

import javafx.concurrent.Task;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/**
 * Provides task of finding duplicating files in given directories.
 * Groups files by their checksum so that copies are easy to find.
 * <p>Created by MontolioV on 12.06.17.
 */
public class FindDuplicatesTask extends Task<Map<String, Set<File>>> {
    private final List<File> DIRECTORIES;
    private final String HASH_ALGORITHM;
    private Map<String, Set<File>> mapToReturn = new HashMap<>();
    private int filesCounter = 0;
    private int filesTotal = 0;
    private ConcurrentLinkedQueue<FileAndChecksum> queueProcessed = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<String> queueExMessages = new ConcurrentLinkedQueue<>();
    private ForkJoinPool fjPool = new ForkJoinPool();
    private Thread fjThread;
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();

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
        preparations();
        runTasksInNewThread();
        controlAndOutputResult();
        return mapToReturn;
    }

    private void preparations() {
        updateMessage(resProvider.getStrFromMessagesBundle("countFiles"));
        DIRECTORIES.forEach(dir -> filesTotal += countFiles(dir));
        updateMessage(resProvider.getStrFromMessagesBundle("totalFiles") + filesTotal);
    }

    public int countFiles(File dir) {
        if (dir == null || isCancelled()) return 0;

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
            ArrayList<DirectoryHandler> taskList = new ArrayList<>();
            DIRECTORIES.forEach(file -> taskList.add(new DirectoryHandler(file,
                    HASH_ALGORITHM,
                    queueProcessed,
                    queueExMessages)));
            taskList.forEach(fjPool::execute);
            taskList.forEach((directoryHandler) -> {
                try {
                    directoryHandler.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    updateMessage(e.toString() + "\n");
                }
            });
        });
        fjThread.start();
    }

    private void controlAndOutputResult() throws InterruptedException {
        while (fjThread.isAlive() || !queueProcessed.isEmpty() || !queueExMessages.isEmpty()) {
            //Cancellation
            if (isCancelled()) {
                fjPool.shutdownNow();
                break;
            }

            if (queueProcessed.isEmpty() && queueExMessages.isEmpty()) {
                try {
                    Thread.sleep(100);
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
            }

            if (!queueExMessages.isEmpty()) {
                updateMessage(queueExMessages.poll());
                filesCounter++;
            }

            updateTitle(fjPool.toString());
            updateProgress(filesCounter, filesTotal);
        }

        //Just to be sure
        fjThread.join();
    }
}

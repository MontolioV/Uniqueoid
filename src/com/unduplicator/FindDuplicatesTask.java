package com.unduplicator;

import javafx.concurrent.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

/**
 * Provides task of finding duplicating files in given directories.
 * Groups files by their checksum so that copies are easy to find.
 * <p>Created by MontolioV on 12.06.17.
 */
public class FindDuplicatesTask extends Task<HashMap<String, List<File>>> {
    private final List<File> DIRECTORIES;
    private final String HASH_ALGORITHM;
    private int filesCounter = 0;
    private int filesTotal = 0;

    public FindDuplicatesTask(List<File> directories, String hash_algorithm) {
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
     * @return Mapping of list of files (value) to file checksum (key).
     *         If list.size() == 1 than there're no copies.
     * @throws Exception an unhandled exception which occurred during the
     *                   background operation
     */
    @Override
    protected HashMap<String, List<File>> call() throws Exception {
        ConcurrentLinkedQueue<FileAndChecksum> queueProcessed = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> queueExMessages = new ConcurrentLinkedQueue<>();
        HashMap<String, List<File>> result = new HashMap<>();
        ForkJoinPool fjPool = new ForkJoinPool();

        updateMessage("Считаем файлы");
        DIRECTORIES.forEach(dir -> filesTotal += countFiles(dir));
        updateMessage("Всего " + filesTotal);

        Thread fjThread = new Thread(() -> {
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
                    //???
                    updateMessage(e.toString() + "\n");
                }
            });
        });
        fjThread.start();

        while (fjThread.isAlive() || !queueProcessed.isEmpty() || !queueExMessages.isEmpty()) {
            //Cancellation
            if (isCancelled()) {
                fjPool.shutdownNow();
                break;
            }

            if (queueProcessed.isEmpty() && queueExMessages.isEmpty()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    //No problem
                }
            }

            if (!queueProcessed.isEmpty()) {
                FileAndChecksum pair = queueProcessed.poll();
                //Here we find duplicated files
                List<File> copies = result.get(pair.getChecksum());
                if (copies == null) {
                    List<File> newList = new ArrayList<>();
                    newList.add(pair.getFile());
                    result.put(pair.getChecksum(), newList);
                } else {
                    copies.add(pair.getFile());
                }

                filesCounter++;
            }

            if (!queueExMessages.isEmpty()) {
                updateMessage(queueExMessages.poll());
                filesCounter++;
            }

            updateProgress(filesCounter, filesTotal);
        }

        //Just to be sure
        fjThread.join();
        return result;
    }

    public int countFiles(File dir) {
        int result = 0;
        if (dir == null || isCancelled()) {
            return 0;
        } else {
            if (dir.isDirectory()) {
                for (File file : dir.listFiles()) {
                    if (file.isDirectory()) {
                        result += countFiles(file);
                    } else {
                        result++;
                    }
                }
                return result;
            } else {
                return 1;
            }
        }
    }
}

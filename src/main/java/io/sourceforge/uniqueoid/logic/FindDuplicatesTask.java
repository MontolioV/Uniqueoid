package io.sourceforge.uniqueoid.logic;

import io.sourceforge.uniqueoid.GlobalFiles;
import io.sourceforge.uniqueoid.ResourcesProvider;
import javafx.concurrent.Task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    private final FindTaskSettings FIND_TASK_SETTINGS;
    private Map<String, Set<File>> mapToReturn = new HashMap<>();
    private ConcurrentLinkedQueue<FileAndChecksum> queueProcessed = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<String> queueExMessages = new ConcurrentLinkedQueue<>();
    private final Set<File> POSSIBLE_DUPLICATES = new HashSet<>();
    private ForkJoinPool fjPool;
    private Thread fjThread;
    private int filesCounter = 0;
    private int filesTotal = 0;
    private long byteCounter = 0;
    private long byteTotal = 0;
    private BufferedWriter logBW;

    public FindDuplicatesTask(List<File> directories, FindTaskSettings findTaskSettings) {
        super();
        this.DIRECTORIES = directories;
        this.FIND_TASK_SETTINGS = findTaskSettings;
        this.fjPool = new ForkJoinPool(findTaskSettings.getParallelism());
    }
    public FindDuplicatesTask(List<File> directories, FindTaskSettings findTaskSettings, Map<String, Set<File>> previousResult) {
        this(directories, findTaskSettings);
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
        updateMessage(resProvider.getStrFromMessagesBundle("totalFiles") + filesTotal + "\n" +
                      resProvider.getStrFromMessagesBundle("possibleDuplicates") + POSSIBLE_DUPLICATES.size());
    }


    public int countFiles(File dir) {
        HashSet<File> initialSet = new HashSet<>();
        if (!mapToReturn.isEmpty()) {
            mapToReturn.forEach((s, files) -> initialSet.addAll(files));
        }
        return countFiles(dir, new HashMap<>());
    }

    public int countFiles(File dir, Map<Long, File> uniqueSizeFileMap) {
        if (dir == null || isCancelled() || Files.isSymbolicLink(dir.toPath())) return 0;

        if (dir.isDirectory()) {
            int result = 0;
            if (dir.listFiles() == null) {
                queueExMessages.offer(resProvider.getStrFromMessagesBundle("countFail") +
                        "\t" + dir.toString());
            } else {
                for (File file : dir.listFiles()) {
                    if (file.isDirectory()) {
                        result += countFiles(file, uniqueSizeFileMap);
                    } else {
                        result++;
                        filePreCheck(file, uniqueSizeFileMap);
                    }
                }
            }
            return result;
        } else {
            return 1;
        }
    }

    private void filePreCheck(File file, Map<Long, File> uniqueSizeFileMap) {
        long fileSize = file.length();
        if (fileSize == 0) return;

        byteTotal += FIND_TASK_SETTINGS.isDisposable() ? 0 : fileSize;
        File previousFile = uniqueSizeFileMap.put(fileSize, file);
        if (previousFile != null) {
            boolean newDupe = POSSIBLE_DUPLICATES.add(previousFile);
            POSSIBLE_DUPLICATES.add(file);
            if (FIND_TASK_SETTINGS.isDisposable()) {
                if (newDupe) {
                    byteTotal += fileSize * 2;
                } else {
                    byteTotal += fileSize;
                }
            }
        }
    }

    private void runTasksInNewThread() {
        fjThread = new Thread(() -> {
            DirectoryHandler directoryHandler = makeDirectoryHandler();
            try {
                fjPool.execute(directoryHandler);
                directoryHandler.get();
                DirectoryHandler.joinRunningTasks();
            } catch (Exception e) {
                e.printStackTrace();
                queueExMessages.offer(e.toString() + "\n");
            }finally {
                DirectoryHandler.clearTasks();
            }
        });

        fjThread.start();
    }

    protected DirectoryHandler makeDirectoryHandler() {
        File[] files;
        DirectoryHandler result;

        if (FIND_TASK_SETTINGS.isDisposable()) {
            files = new File[POSSIBLE_DUPLICATES.size()];
            files = POSSIBLE_DUPLICATES.toArray(files);
        } else {
            files = new File[DIRECTORIES.size()];
            files = DIRECTORIES.toArray(files);
        }

        if (FIND_TASK_SETTINGS.isParallel()) {
            result = new DirectoryHandler(files, FIND_TASK_SETTINGS, queueProcessed, queueExMessages);
        } else {
            result = new DirectoryHandlerSoloThread(files, FIND_TASK_SETTINGS, queueProcessed, queueExMessages);
        }

        return result;
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

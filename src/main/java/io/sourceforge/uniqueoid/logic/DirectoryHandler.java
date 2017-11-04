package io.sourceforge.uniqueoid.logic;

import io.sourceforge.uniqueoid.ResourcesProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RecursiveAction;

/**
 * Directory Handler takes directory and puts checksums of all files recursively in storage.
 * ForkJoin divide and conquer strategy. Faster and more resource demanding.
 * <p>Created by MontolioV on 06.06.17.
 */
public class DirectoryHandler extends RecursiveAction {
    private static Set<DirectoryHandler> runningTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final File[] FILES_TO_HANDLE;
    private final CheckSumMaker CHECKSUM_MAKER;
    private final ConcurrentLinkedQueue<FileAndChecksum> QUEUE_FILE_AND_CHECKSUM;
    private final ConcurrentLinkedQueue<String> QUEUE_EX_MESSAGES;
    private final FindTaskSettings FIND_TASK_SETTINGS;
    private final long LARGE_FILE_SIZE;
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();

    public DirectoryHandler(File file,
                            FindTaskSettings findTaskSettings,
                            ConcurrentLinkedQueue<FileAndChecksum> queueProcessed,
                            ConcurrentLinkedQueue<String> queueExMessages) {
        this(new File[]{file}, findTaskSettings, queueProcessed, queueExMessages);
    }

    public DirectoryHandler(File[] files,
                            FindTaskSettings findTaskSettings,
                            ConcurrentLinkedQueue<FileAndChecksum> queueProcessed,
                            ConcurrentLinkedQueue<String> queueExMessages) {
        this.CHECKSUM_MAKER = new CheckSumMaker(findTaskSettings);
        this.FILES_TO_HANDLE = files;
        this.QUEUE_FILE_AND_CHECKSUM = queueProcessed;
        this.FIND_TASK_SETTINGS = findTaskSettings;
        this.QUEUE_EX_MESSAGES = queueExMessages;
        this.LARGE_FILE_SIZE = findTaskSettings.getBigFileSize();
    }

    /**
     * The main computation performed by this task.
     */
    @Override
    protected void compute() {
        File[] remainder = splitLargeDirectory();
        analizeFiles(remainder);
        runningTasks.remove(this);
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
        DirectoryHandler longTask = new DirectoryHandlerSoloThread(largeFile,
                FIND_TASK_SETTINGS,
                QUEUE_FILE_AND_CHECKSUM,
                QUEUE_EX_MESSAGES);
        forkAndRegistate(longTask);
    }
    protected void split(File[] files) {
        DirectoryHandler fastTask = new DirectoryHandler(files,
                FIND_TASK_SETTINGS,
                QUEUE_FILE_AND_CHECKSUM,
                QUEUE_EX_MESSAGES);
        forkAndRegistate(fastTask);
    }

    private void forkAndRegistate(DirectoryHandler directoryHandler) {
        runningTasks.add(directoryHandler);
        directoryHandler.fork();
    }

    protected void processFile(File file) {
        if (!FIND_TASK_SETTINGS.isSuitable(file)) return;

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

    public static void joinRunningTasks() throws Exception{
        try {
            while (!runningTasks.isEmpty()) {
                runningTasks.forEach(DirectoryHandler::join);
            }
        } finally {
            runningTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());
        }
    }
}

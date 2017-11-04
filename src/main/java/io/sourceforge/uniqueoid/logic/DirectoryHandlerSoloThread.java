package io.sourceforge.uniqueoid.logic;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Directory Handler takes directory and puts checksums of all files recursively in storage.
 * Solo thread strategy. No forking, often slower, but more stable.
 * <p>Created by MontolioV on 21.10.17.
 */
public class DirectoryHandlerSoloThread extends DirectoryHandler {

    public DirectoryHandlerSoloThread(File file, FindTaskSettings findTaskSettings, ConcurrentLinkedQueue<FileAndChecksum> queueProcessed, ConcurrentLinkedQueue<String> queueExMessages) {
        super(file, findTaskSettings, queueProcessed, queueExMessages);
    }

    public DirectoryHandlerSoloThread(File[] files, FindTaskSettings findTaskSettings, ConcurrentLinkedQueue<FileAndChecksum> queueProcessed, ConcurrentLinkedQueue<String> queueExMessages) {
        super(files, findTaskSettings, queueProcessed, queueExMessages);
    }

    @Override
    protected void split(File largeFile) {
        processFile(largeFile);
    }

    @Override
    protected void split(File[] files) {
        analizeFiles(files);
    }
}

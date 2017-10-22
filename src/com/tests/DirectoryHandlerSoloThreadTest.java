package com.tests;

import com.unduplicator.logic.DirectoryHandler;
import com.unduplicator.logic.DirectoryHandlerSoloThread;
import com.unduplicator.logic.FileAndChecksum;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DirectoryHandlerSoloThreadTest extends DirectoryHandlerTest {
    @Override
    protected DirectoryHandler makeDirectoryHandler(File file, String hashAlgorithm, ConcurrentLinkedQueue<FileAndChecksum> queueProcessed, ConcurrentLinkedQueue<String> queueExMessages) {
        return new DirectoryHandlerSoloThread(file, hashAlgorithm, queueProcessed, queueExMessages);
    }
}
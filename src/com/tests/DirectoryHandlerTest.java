package com.tests;

import com.unduplicator.DirectoryHandler;
import com.unduplicator.FileAndChecksum;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;

import static junit.framework.TestCase.assertEquals;


/**
 * <p>Created by MontolioV on 07.06.17.
 */
public class DirectoryHandlerTest {
    private ConcurrentLinkedQueue<FileAndChecksum> queueFilesChSs;
    private ConcurrentLinkedQueue<String> queueExMessages;

    @Before
    public void setUp() throws Exception {
        queueFilesChSs = new ConcurrentLinkedQueue<>();
        queueExMessages = new ConcurrentLinkedQueue<>();
    }

    @Test
    public void run_1Thread() throws Exception {
        DirectoryHandler dirHandler;
        dirHandler = new DirectoryHandler(new File("test_mat"), "SHA-256", queueFilesChSs, queueExMessages,
                                          ResourseBundles.MESSAGES_EN.getResourceBundle(), ResourseBundles.EXCEPTIONS_EN.getResourceBundle());

        ForkJoinPool fjPool = new ForkJoinPool();

        fjPool.invoke(dirHandler);

        assertEquals(7, queueFilesChSs.size() + queueExMessages.size());
    }
}
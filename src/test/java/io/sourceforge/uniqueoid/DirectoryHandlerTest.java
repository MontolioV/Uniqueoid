package io.sourceforge.uniqueoid;

import io.sourceforge.uniqueoid.logic.DirectoryHandler;
import io.sourceforge.uniqueoid.logic.FileAndChecksum;
import io.sourceforge.uniqueoid.logic.FindTaskSettings;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;

import static io.sourceforge.uniqueoid.TestMatEnum.ROOT_CONTROL;
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
        dirHandler = makeDirectoryHandler(new File(ROOT_CONTROL.getFile().getParent()), new FindTaskSettings(), queueFilesChSs, queueExMessages);
        ForkJoinPool fjPool = new ForkJoinPool();

        fjPool.invoke(dirHandler);
        DirectoryHandler.joinRunningTasks();

        assertEquals(7, queueFilesChSs.size() + queueExMessages.size());
    }

    protected DirectoryHandler makeDirectoryHandler(File file, FindTaskSettings findTaskSettings, ConcurrentLinkedQueue<FileAndChecksum> queueProcessed, ConcurrentLinkedQueue<String> queueExMessages) {
        return new DirectoryHandler(file, findTaskSettings, queueProcessed, queueExMessages);
    }
}
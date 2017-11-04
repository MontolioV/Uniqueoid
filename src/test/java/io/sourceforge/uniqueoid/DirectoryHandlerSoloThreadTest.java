package io.sourceforge.uniqueoid;

import io.sourceforge.uniqueoid.logic.DirectoryHandler;
import io.sourceforge.uniqueoid.logic.DirectoryHandlerSoloThread;
import io.sourceforge.uniqueoid.logic.FileAndChecksum;
import io.sourceforge.uniqueoid.logic.FindTaskSettings;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DirectoryHandlerSoloThreadTest extends DirectoryHandlerTest {
    @Override
    protected DirectoryHandler makeDirectoryHandler(File file, FindTaskSettings findTaskSettings, ConcurrentLinkedQueue<FileAndChecksum> queueProcessed, ConcurrentLinkedQueue<String> queueExMessages) {
        return new DirectoryHandlerSoloThread(file, findTaskSettings, queueProcessed, queueExMessages);
    }
}
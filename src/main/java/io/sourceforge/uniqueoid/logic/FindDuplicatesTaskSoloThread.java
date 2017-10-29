package io.sourceforge.uniqueoid.logic;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Provides task of finding duplicating files in given directories.
 * Groups files by their checksum so that copies are easy to find.
 * Solo thread execution strategy.
 * <p>Created by MontolioV on 22.10.17.
 */
public class FindDuplicatesTaskSoloThread extends FindDuplicatesTask{

    public FindDuplicatesTaskSoloThread(List<File> directories, String hash_algorithm) {
        super(directories, hash_algorithm);
    }
    public FindDuplicatesTaskSoloThread(List<File> directories, String hash_algorithm, Map<String, Set<File>> previousResult) {
        super(directories, hash_algorithm, previousResult);
    }

    @Override
    protected DirectoryHandler makeDirectoryHandler(File[] files, String hashAlgorithm, ConcurrentLinkedQueue<FileAndChecksum> queueProcessed, ConcurrentLinkedQueue<String> queueExMessages) {
        return new DirectoryHandlerSoloThread(files, hashAlgorithm, queueProcessed, queueExMessages);
    }
}

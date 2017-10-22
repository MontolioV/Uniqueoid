package com.tests;

import com.unduplicator.logic.FindDuplicatesTask;
import com.unduplicator.logic.FindDuplicatesTaskSoloThread;

import java.io.File;
import java.util.List;

public class FindDuplicatesTaskSoloThreadTest extends FindDuplicatesTaskTest {
    @Override
    protected FindDuplicatesTask makeFindDuplicatesTask(List<File> directories, String hash_algorithm) {
        return new FindDuplicatesTaskSoloThread(directories, hash_algorithm);
    }
}
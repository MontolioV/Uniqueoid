package io.sourceforge.uniqueoid;

import io.sourceforge.uniqueoid.logic.FindDuplicatesTask;
import io.sourceforge.uniqueoid.logic.FindDuplicatesTaskSoloThread;
import io.sourceforge.uniqueoid.logic.FindTaskSettings;

import java.io.File;
import java.util.List;

public class FindDuplicatesTaskSoloThreadTest extends FindDuplicatesTaskTest {
    @Override
    protected FindDuplicatesTask makeFindDuplicatesTask(List<File> directories, FindTaskSettings findTaskSettings) {
        return new FindDuplicatesTaskSoloThread(directories, findTaskSettings);
    }
}
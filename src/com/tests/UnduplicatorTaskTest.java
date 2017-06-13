package com.tests;

import com.unduplicator.UnduplicatorTask;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;
import static com.tests.TestMatEnum.*;

/**
 * <p>Created by MontolioV on 12.06.17.
 */
public class UnduplicatorTaskTest {

    @Test
    public void call() throws Exception {
        //Prepare control hashmap
        HashMap<String, List<File>> control = new HashMap<>();
        ArrayList<File> alTmp = new ArrayList<>();
        alTmp.add(ROOT_CONTROL.getFile());
        alTmp.add(INNER_COPY_GOOD.getFile());
        alTmp.add(INNER2_COPY_GOOD.getFile());
        alTmp.add(INNER2_INNER_COPY_GOOD.getFile());
        control.put(ROOT_CONTROL.getSha256CheckSum(), alTmp);

        alTmp = new ArrayList<>();
        alTmp.add(ROOT_COPY_BAD.getFile());
        alTmp.add(INNER2_COPY_BAD.getFile());
        alTmp.add(INNER2_INNER_COPY_BAD.getFile());
        control.put(ROOT_COPY_BAD.getSha256CheckSum(), alTmp);

        ArrayList<File> dirList = new ArrayList<>();
        dirList.add(new File("test_mat"));
        UnduplicatorTask undTask = new UnduplicatorTask(dirList, "SHA-256");

        Thread taskThread = new Thread(undTask);
        taskThread.start();

        HashMap<String, List<File>> result = undTask.get();

        assertEquals(control, result);
    }

}
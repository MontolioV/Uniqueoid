package com.tests;

import com.unduplicator.logic.FindDuplicatesTask;
import javafx.embed.swing.JFXPanel;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.*;
import java.util.concurrent.CancellationException;

import static com.tests.TestMatEnum.*;
import static org.junit.Assert.assertEquals;

/**
 * <p>Created by MontolioV on 12.06.17.
 */
public class FindDuplicatesTaskTest {
    private FindDuplicatesTask task;
    private ArrayList<File> dirList;
    private final String algorithm = "SHA-256";

    @BeforeClass
    public static void initToolkit() {
        //Needed to initialize JavaFX Toolkit
        new JFXPanel();
    }

    @Before
    public void setUp() throws Exception {
        dirList = new ArrayList<>();
        dirList.add(new File("test_mat"));
    }

    protected FindDuplicatesTask makeFindDuplicatesTask(List<File> directories, String hash_algorithm) {
        return new FindDuplicatesTask(directories, hash_algorithm);
    }

    @Test
    public void call() throws Exception {
        Map<String, Set<File>> result;
        task = makeFindDuplicatesTask(dirList, algorithm);

        //Prepare control hashmap
        HashMap<String, Set<File>> control = new HashMap<>();
        HashSet<File> alTmp = new HashSet<>();
        alTmp.add(ROOT_CONTROL.getFile());
        alTmp.add(INNER_COPY_GOOD.getFile());
        alTmp.add(INNER2_COPY_GOOD.getFile());
        alTmp.add(INNER2_INNER_COPY_GOOD.getFile());
        control.put(ROOT_CONTROL.getSha256CheckSum(), alTmp);

        alTmp = new HashSet<>();
        alTmp.add(ROOT_COPY_BAD.getFile());
        alTmp.add(INNER2_COPY_BAD.getFile());
        alTmp.add(INNER2_INNER_COPY_BAD.getFile());
        control.put(ROOT_COPY_BAD.getSha256CheckSum(), alTmp);

        task.run();
        result = task.get();

        assertEquals(control, result);
    }

    @Test(expected = CancellationException.class, timeout = 1000)
    public void cancel() throws Exception {
        //Task must be large
        for (int i = 0; i < 10_000; i++) {
            dirList.add(new File("test_mat"));
        }
        task = makeFindDuplicatesTask(dirList, algorithm);

        Thread trd = new Thread(() -> task.run());
        trd.start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        task.cancel();
        task.get();
    }

    @Test
    public void countFiles() throws Exception {
        int result = 0;
        for (File file : dirList) {
            result += makeFindDuplicatesTask(dirList, algorithm).countFiles(file);
        }

        assertEquals(7, result);
    }

}
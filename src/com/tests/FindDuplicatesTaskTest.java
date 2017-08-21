package com.tests;

import com.unduplicator.FindDuplicatesTask;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static com.tests.TestMatEnum.*;

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

    @Test
    public void call() throws Exception {
        HashMap<String, List<File>> result;
        task = new FindDuplicatesTask(dirList, algorithm);

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

        task.run();
        result = task.get();

        control.forEach((s, files) -> Collections.sort(files));
        result.forEach((s, files) -> Collections.sort(files));

        assertEquals(control, result);
    }

    @Test(expected = CancellationException.class, timeout = 1000)
    public void cancel() throws Exception {
        //Task must be large
        for (int i = 0; i < 10_000; i++) {
            dirList.add(new File("test_mat"));
        }
        task = new FindDuplicatesTask(dirList, algorithm);

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
            result += new FindDuplicatesTask(dirList, algorithm).countFiles(file);
        }

        assertEquals(7, result);
    }

}
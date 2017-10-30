package io.sourceforge.uniqueoid;

import io.sourceforge.uniqueoid.logic.DeleteFilesTask;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

import static io.sourceforge.uniqueoid.TestMatEnum.ROOT_CONTROL;
import static org.junit.Assert.*;

/**
 * <p>Created by MontolioV on 09.08.17.
 */
public class DeleteFilesTaskTest {
    @BeforeClass
    public static void initToolkit() {
        //Needed to initialize JavaFX Toolkit
        new JFXPanel();
    }

    @Test
    public void call() throws Exception {
        File rootDir = new File(ROOT_CONTROL.getFile().getParent());
        File mainDir = new File(rootDir, "newdir");
        File brch1 = new File(mainDir, "brch1/inner");
        File brch2 = new File(mainDir, "brch2/inner");

        brch1.mkdirs();
        brch2.mkdirs();
        assertTrue(brch1.exists());
        assertTrue(brch2.exists());
        assertTrue(mainDir.exists());
        assertTrue(rootDir.exists());

        Set<File> fileAL = new HashSet<>();
        fileAL.add(new File(mainDir, "1.txt"));
        fileAL.add(new File(brch1, "1.txt"));
        fileAL.add(new File(brch1, "2.txt"));
        fileAL.add(new File(brch2, "1.txt"));
        fileAL.add(new File(brch2, "2.txt"));

        for (File file : fileAL) {
            try (FileWriter fw = new FileWriter(file)) {
                fw.write("Hello world!");
            }
        }

        assertTrue(mainDir.listFiles().length > 0);
        assertTrue(brch1.listFiles().length > 0);

        DeleteFilesTask delTask = new DeleteFilesTask(fileAL);
        assertEquals(-1, delTask.getProgress(), 0.001);
        delTask.run();
        assertEquals(0, delTask.get().size());
        Platform.runLater(() -> assertEquals(1, delTask.getProgress(), 0.001));

        assertFalse(mainDir.exists());
        assertTrue(rootDir.exists());
        assertTrue(rootDir.getParentFile().exists());
    }

}
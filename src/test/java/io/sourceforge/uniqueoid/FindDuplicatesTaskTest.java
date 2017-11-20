package io.sourceforge.uniqueoid;

import io.sourceforge.uniqueoid.logic.FindDuplicatesTask;
import io.sourceforge.uniqueoid.logic.FindTaskSettings;
import javafx.embed.swing.JFXPanel;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import java.io.File;
import java.util.*;
import java.util.concurrent.CancellationException;

import static io.sourceforge.uniqueoid.TestMatEnum.*;
import static org.junit.Assert.assertEquals;

/**
 * <p>Created by MontolioV on 12.06.17.
 */
@RunWith(Parameterized.class)
public class FindDuplicatesTaskTest {
    @Parameter(0)
    public FindTaskSettings findTaskSettings;
    @Parameter(1)
    public Map<String, Set<File>> control;

    private ArrayList<File> dirList;

    @BeforeClass
    public static void initToolkit() {
        //Needed to initialize JavaFX Toolkit
        new JFXPanel();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        List<Object[]> result = new ArrayList<>();
        long mb = (long) Math.pow(10, 6);

        Object[] settings = new Object[]{
                new FindTaskSettings(),
                new FindTaskSettings(false, true, 2, 10 * mb, (int) (10 * mb), 10 * mb, mb),
                new FindTaskSettings(true, true, 2, 10 * mb, (int) (10 * mb), 10 * mb, 0L),
        };

        //Prepare control hashmap
        Object[] controlMaps = new Object[3];
        HashMap<String, Set<File>> control;
        HashSet<File> alTmp;

        //0
        control = new HashMap<>();
        alTmp = new HashSet<>();
        alTmp.add(ROOT_CONTROL.getFile());
        alTmp.add(INNER_COPY_GOOD.getFile());
        alTmp.add(INNER2_COPY_GOOD.getFile());
        alTmp.add(INNER2_INNER_COPY_GOOD.getFile());
        control.put(ROOT_CONTROL.getCheckSum(), alTmp);

        alTmp = new HashSet<>();
        alTmp.add(ROOT_COPY_BAD.getFile());
        alTmp.add(INNER2_COPY_BAD.getFile());
        alTmp.add(INNER2_INNER_COPY_BAD.getFile());
        control.put(ROOT_COPY_BAD.getCheckSum(), alTmp);

        controlMaps[0] = control;

        //1
        control = new HashMap<>();
        alTmp = new HashSet<>();
        alTmp.add(ROOT_COPY_BAD.getFile());
        alTmp.add(INNER2_COPY_BAD.getFile());
        alTmp.add(INNER2_INNER_COPY_BAD.getFile());
        control.put(ROOT_COPY_BAD.getCheckSum(), alTmp);

        controlMaps[1] = control;

        //2
        controlMaps[2] = controlMaps[0];

        for (int i = 0; i < 3; i++) {
            result.add(new Object[]{settings[i], controlMaps[i]});
        }
        return result;
    }

    @Before
    public void setUp() throws Exception {
        dirList = new ArrayList<>();
        dirList.add(new File(ROOT_CONTROL.getFile().getParent()));
    }

    protected FindDuplicatesTask makeFindDuplicatesTask(List<File> directories, FindTaskSettings findTaskSettings) {
        return new FindDuplicatesTask(directories, findTaskSettings);
    }

    @Test
    public void call() throws Exception {
        Map<String, Set<File>> result;
        FindDuplicatesTask task = makeFindDuplicatesTask(dirList, findTaskSettings);

        task.run();
        result = task.get();

        assertEquals(control, result);
    }

    @Test(expected = CancellationException.class, timeout = 5000)
    public void cancel() throws Exception {
        //Task must be large
        for (int i = 0; i < 10_000; i++) {
            dirList.add(new File(ROOT_CONTROL.getFile().getParent()));
        }
        FindDuplicatesTask task = makeFindDuplicatesTask(dirList, findTaskSettings);

        Thread trd = new Thread(() -> task.run());
        trd.start();

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        task.cancel();
        trd.join();
        task.get();
    }

    @Test
    public void countFiles() throws Exception {
        int result = 0;
        for (File file : dirList) {
            result += makeFindDuplicatesTask(dirList, findTaskSettings).countFiles(file);
        }

        assertEquals(7, result);
    }

}
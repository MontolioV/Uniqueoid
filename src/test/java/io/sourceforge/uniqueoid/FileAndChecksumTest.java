package io.sourceforge.uniqueoid;

import io.sourceforge.uniqueoid.logic.FileAndChecksum;
import org.junit.Before;
import org.junit.Test;

import static io.sourceforge.uniqueoid.TestMatEnum.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * <p>Created by MontolioV on 06.06.17.
 */
public class FileAndChecksumTest {
    private FileAndChecksum pairControl;
    private FileAndChecksum pairCopyOfControl;
    private FileAndChecksum pairFullDifferent;
    private FileAndChecksum pairPathDifferent;

    @Before
    public void setUp() throws Exception {
        pairControl = new FileAndChecksum(ROOT_CONTROL.getFile(), ROOT_CONTROL.getSha256CheckSum());
        pairCopyOfControl = new FileAndChecksum(ROOT_CONTROL.getFile(), ROOT_CONTROL.getSha256CheckSum());
        pairPathDifferent = new FileAndChecksum(INNER2_COPY_GOOD.getFile(), INNER2_COPY_GOOD.getSha256CheckSum());
        pairFullDifferent = new FileAndChecksum(INNER2_INNER_COPY_BAD.getFile(), INNER2_INNER_COPY_BAD.getSha256CheckSum());
    }

    @Test
    public void equals() throws Exception {
        assertTrue(pairControl.equals(pairCopyOfControl));
        assertFalse(pairControl.equals(pairPathDifferent));
        assertFalse(pairControl.equals(pairFullDifferent));
    }
}
package com.tests;

import com.unduplicator.CheckSumMaker;

import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Test for checksum
 * <p>Created by MontolioV on 30.05.17.
 */
public class CheckSumMakerTest {
    private CheckSumMaker checkSumMaker;
    private TestMatEnum fileControl;
    private TestMatEnum fileCopyGood;
    private TestMatEnum fileCopyBad;

    public CheckSumMakerTest() throws NoSuchAlgorithmException {
        checkSumMaker = new CheckSumMaker("SHA-256");
        fileControl = TestMatEnum.ROOT_CONTROL;
        fileCopyGood = TestMatEnum.INNER_COPY_GOOD;
        fileCopyBad = TestMatEnum.ROOT_COPY_BAD;
    }

    @org.junit.Test
    public void makeCheckSum() throws Exception {
        String chSControl = checkSumMaker.makeCheckSum(fileControl.getFile());
        String chSCopyGood = checkSumMaker.makeCheckSum(fileCopyGood.getFile());
        String chSCopyBad = checkSumMaker.makeCheckSum(fileCopyBad.getFile());

        assertEquals(chSControl, chSCopyGood);
        assertNotEquals(chSControl, chSCopyBad);
        assertNotEquals(chSCopyGood, chSCopyBad);

        assertEquals(chSControl, fileControl.getSha256CheckSum());
        assertEquals(chSCopyGood, fileCopyGood.getSha256CheckSum());
        assertEquals(chSCopyBad, fileCopyBad.getSha256CheckSum());
    }

}
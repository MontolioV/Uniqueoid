package io.sourceforge.uniqueoid.logic;

import java.io.File;
import java.io.Serializable;

/**
 * <p>Created by MontolioV on 31.10.17.
 */
public class FindTaskSettings implements Serializable{
    private String hashAlgorithm;
    private boolean isParallel;
    private long maxFileSize;
    private long minFileSize;

    public FindTaskSettings() {
        this.hashAlgorithm = "SHA-256";
        this.isParallel = true;
        this.maxFileSize = Long.MAX_VALUE;
        this.minFileSize = 0;
    }

    public FindTaskSettings(String hashAlgorithm, boolean isParallel, long maxFileSize, long minFileSize) {
        this.hashAlgorithm = hashAlgorithm;
        this.isParallel = isParallel;
        this.maxFileSize = maxFileSize;
        this.minFileSize = minFileSize;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public boolean isParallel() {
        return isParallel;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public long getMinFileSize() {
        return minFileSize;
    }

    public boolean testFile(File file) {
        long fileSize = file.length();
        if (fileSize < minFileSize || fileSize > maxFileSize) {
            return false;
        } else {
            return true;
        }
    }
}

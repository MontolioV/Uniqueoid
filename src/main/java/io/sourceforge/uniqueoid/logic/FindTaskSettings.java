package io.sourceforge.uniqueoid.logic;

import io.sourceforge.uniqueoid.gui.BytePower;

import java.io.File;
import java.io.Serializable;

/**
 * <p>Created by MontolioV on 31.10.17.
 */
public class FindTaskSettings implements Serializable{
    private boolean isDisposable;
    private boolean isParallel;
    private int parallelism;
    private long bigFileSize;
    private int maxBufferSize;
    private long maxFileSize;
    private long minFileSize;

    public FindTaskSettings() {
        this.isDisposable = false;
        this.isParallel = true;
        this.parallelism = Runtime.getRuntime().availableProcessors();
        this.bigFileSize = 10 * BytePower.MI_BYTES.getModifier();
        this.maxBufferSize = (int) (10 * BytePower.MI_BYTES.getModifier());
        this.maxFileSize = Long.MAX_VALUE;
        this.minFileSize = 0;
    }

    public FindTaskSettings(boolean isDisposable, boolean isParallel, int parallelism, long bigFileSize, int maxBufferSize, long maxFileSize, long minFileSize) {
        this.isDisposable = isDisposable;
        this.isParallel = isParallel;
        this.parallelism = parallelism;
        this.bigFileSize = bigFileSize;
        this.maxBufferSize = maxBufferSize;
        this.maxFileSize = maxFileSize;
        this.minFileSize = minFileSize;
    }

    public boolean isParallel() {
        return isParallel;
    }

    public int getParallelism() {
        return parallelism;
    }

    public long getBigFileSize() {
        return bigFileSize;
    }

    public int getMaxBufferSize() {
        return maxBufferSize;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public long getMinFileSize() {
        return minFileSize;
    }

    public boolean isSuitable(File file) {
        long fileSize = file.length();
        if (fileSize < minFileSize || fileSize > maxFileSize) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isDisposable() {
        return isDisposable;
    }
}

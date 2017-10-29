package io.sourceforge.uniqueoid.logic;

import java.io.File;

/**
 * Encapsulates File and Checksum in a single entity.
 * <p>Created by MontolioV on 06.06.17.
 */
public class FileAndChecksum {
    private File file;
    private String checksum;

    public FileAndChecksum(File file, String checksum) {
        this.file = file;
        this.checksum = checksum;
    }

    public File getFile() {
        return file;
    }

    public String getChecksum() {
        return checksum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileAndChecksum)) return false;

        FileAndChecksum that = (FileAndChecksum) o;

        if (getFile() != null ? !getFile().equals(that.getFile()) : that.getFile() != null) return false;
        return getChecksum() != null ? getChecksum().equals(that.getChecksum()) : that.getChecksum() == null;

    }

    @Override
    public int hashCode() {
        int result = getFile() != null ? getFile().hashCode() : 0;
        result = 31 * result + (getChecksum() != null ? getChecksum().hashCode() : 0);
        return result;
    }

    /**
     * Returns a string representation of the object.
     * @return path and checksum as a string.
     */
    @Override
    public String toString() {
        return file.toString() + "\t" + checksum;
    }
}

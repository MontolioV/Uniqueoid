package com.unduplicator;

import java.io.File;

/**
 * This singleton encapsulates core files and directories.
 *
 * <p>Created by MontolioV on 18.10.17.
 */
public class GlobalFiles {
    private static GlobalFiles ourInstance = new GlobalFiles();

    private File appDir;
    private File settingsDir;
    private File lastVisitedDir;
    private File localeFile;
    private File settingsFile;
    private File logFile;

    public static GlobalFiles getInstance() {
        return ourInstance;
    }

    private GlobalFiles() {
        appDir = new File(System.getProperty("user.home") + "/AppData/Unduplicator");
        if (!appDir.exists()) appDir.mkdirs();
        settingsDir = new File(appDir.toString() + "/settings");
        if (!settingsDir.exists()) settingsDir.mkdir();
        lastVisitedDir = makeRootFile();
        localeFile = new File(settingsDir, "/locale.ser");
        settingsFile = new File(settingsDir, "/settings.txt");
        logFile = new File(appDir, "/log.txt");
    }

    private File makeRootFile() {
        File result = new File(System.getProperty("user.home"));
        while (result.getParentFile() != null) {
            result = result.getParentFile();
        }
        return result;
    }

    public File getAppDir() {
        return appDir;
    }

    public File getSettingsDir() {
        return settingsDir;
    }

    public File getLastVisitedDir() {
        return lastVisitedDir;
    }

    public void setLastVisitedDir(File lastVisitedDir) {
        if (lastVisitedDir.exists()) {
            this.lastVisitedDir = lastVisitedDir;
        }
    }

    public void setLastVisitedDir(String lastVisitedDirStr) {
        setLastVisitedDir(new File(lastVisitedDirStr));
    }

    public File getLocaleFile() {
        return localeFile;
    }

    public File getSettingsFile() {
        return settingsFile;
    }

    public File getLogFile() {
        return logFile;
    }
}

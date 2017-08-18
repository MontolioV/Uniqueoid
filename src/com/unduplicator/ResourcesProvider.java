package com.unduplicator;

import java.util.ResourceBundle;
import java.util.concurrent.Semaphore;

/**
 * <p>Created by MontolioV on 18.08.17.
 */
public class ResourcesProvider {
    private static ResourcesProvider ourInstance = new ResourcesProvider();
    private ResourceBundle exceptionsBundle;
    private ResourceBundle guiBundle;
    private ResourceBundle messagesBundle;
    private Semaphore semaphore = new Semaphore(1);

    public static ResourcesProvider getInstance() {
        return ourInstance;
    }

    private ResourcesProvider() {
    }
}

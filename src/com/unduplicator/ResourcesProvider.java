package com.unduplicator;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>Created by MontolioV on 18.08.17.
 */
public class ResourcesProvider {
    private static ResourcesProvider ourInstance = new ResourcesProvider();
    private Locale currentLocal;
    private Set<Locale> supportedLocales;
    private Map<String, ResourceBundle> bundles = new HashMap<>();
    private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public static ResourcesProvider getInstance() {
        return ourInstance;
    }

    private ResourcesProvider() {
        currentLocal = loadLocale();
        setBundlesLocale(currentLocal);
        makeSupportedLocalesSet();
    }

    private void makeSupportedLocalesSet() {
        Set<Locale> result = new HashSet<>();
        result.add(new Locale("en", "US"));
        result.add(new Locale("ru", "RU"));
        result.add(new Locale("uk", "UA"));

        supportedLocales = Collections.unmodifiableSet(result);
    }

    protected Locale loadLocale() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(GlobalFiles.getInstance().getLocaleFile()))) {
            return (Locale) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new Locale("en", "US");
        }
    }

    protected boolean saveLocale() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(GlobalFiles.getInstance().getLocaleFile()))) {
            oos.writeObject(currentLocal);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setBundlesLocale(Locale locale) {
        currentLocal = locale;

        readWriteLock.writeLock().lock();

        ResourceBundle exceptionsBundle = ResourceBundle.getBundle("com.resources.Exception_Bundle", locale);
        ResourceBundle guiBundle = ResourceBundle.getBundle("com.resources.GUI_Bundle", locale);
        ResourceBundle messagesBundle = ResourceBundle.getBundle("com.resources.Messages_Bundle", locale);
        bundles.put("exceptions", exceptionsBundle);
        bundles.put("gui", guiBundle);
        bundles.put("messages", messagesBundle);
        saveLocale();

        readWriteLock.writeLock().unlock();
    }

    public String getStrFromBundle(String bundleName, String stringKey) {
        readWriteLock.readLock().lock();
        String result = bundles.get(bundleName).getString(stringKey);
        readWriteLock.readLock().unlock();

        return result;
    }

    public String getStrFromExceptionBundle(String stringKey) {
        return getStrFromBundle("exceptions", stringKey);
    }

    public String getStrFromGUIBundle(String stringKey) {
        return getStrFromBundle("gui", stringKey);
    }

    public String getStrFromMessagesBundle(String stringKey) {
        return getStrFromBundle("messages", stringKey);
    }

    public Set<Locale> getSupportedLocales() {
        return supportedLocales;
    }

    public Locale getCurrentLocal() {
        return currentLocal;
    }
}

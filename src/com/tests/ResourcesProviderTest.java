package com.tests;

import com.unduplicator.ResourcesProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * <p>Created by MontolioV on 21.08.17.
 */
public class ResourcesProviderTest {

    @Test
    public void getInstance() throws Exception {
        ResourcesProvider rp1 = ResourcesProvider.getInstance();
        ResourcesProvider rp2 = ResourcesProvider.getInstance();
        assertTrue(rp1.equals(rp2));
    }

    @Test
    public void setBundlesLocale() throws Exception {
        ResourcesProvider.getInstance().setBundlesLocale(new Locale("en", "US"));
        String defaultMessage = ResourcesProvider.getInstance().getStrFromBundle("messages", "taskProcessing");
        assertEquals("Processing...", defaultMessage);
        ResourcesProvider.getInstance().setBundlesLocale(new Locale("ru", "RU"));
        String newMessage = ResourcesProvider.getInstance().getStrFromBundle("messages", "taskProcessing");
        assertNotEquals(defaultMessage, newMessage);
        assertEquals("Выполняется...", newMessage);
    }

    @Test
    public void getStrFromBundle() throws Exception {
        ResourcesProvider rp = ResourcesProvider.getInstance();
        rp.setBundlesLocale(new Locale("ru", "RU"));
        assertEquals("Возникла ошибка", rp.getStrFromBundle("messages", "error"));
        assertEquals("Удалить", rp.getStrFromBundle("gui", "deleteButton"));
        assertEquals("Файл ничего не содержит.", rp.getStrFromBundle("exceptions", "fileIsEmpty"));

        concurrentAccess();
    }

    private void concurrentAccess() {
        ResourcesProvider rp = ResourcesProvider.getInstance();
        AtomicBoolean localeIsChanged = new AtomicBoolean(false);
        rp.setBundlesLocale(new Locale("ru", "RU"));
        Semaphore semaphore = new Semaphore(1);

        Thread writer = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    try {
                        semaphore.acquire();
                        if (localeIsChanged.get()) {
                            rp.setBundlesLocale(new Locale("ru", "RU"));
                            localeIsChanged.set(false);
                        } else {
                            rp.setBundlesLocale(new Locale("en", "US"));
                            localeIsChanged.set(true);
                        }
                        semaphore.release();
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        writer.start();

        for (int i = 0; i < 10_000; i++) {
            try {
                semaphore.acquire();
                if (localeIsChanged.get()) {
                    assertEquals("Error occurred", rp.getStrFromBundle("messages", "error"));
                } else {
                    assertEquals("Возникла ошибка", rp.getStrFromBundle("messages", "error"));
                }
                semaphore.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
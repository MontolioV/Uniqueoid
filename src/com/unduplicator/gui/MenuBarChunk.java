package com.unduplicator.gui;

import com.unduplicator.ResourcesProvider;
import javafx.application.Platform;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * <p>Created by MontolioV on 11.09.17.
 */
public class MenuBarChunk extends AbstractGUIChunk {
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();
    private ChunkManager chunkManager;

    private Menu fileMenu = new Menu();
    private MenuItem exitMI = new MenuItem();

    private Menu languageMenu = new Menu();
    private Map<Locale, MenuItem> languageMIMap = new HashMap<>();

    private Menu helpMenu = new Menu();


    public MenuBarChunk(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
        setSelfNode(makeMenuBar());
        updateLocaleContent();
    }

    /**
     * Updates gui local dependent elements to current local settings.
     */
    @Override
    public void updateLocaleContent() {
        fileMenu.setText(resProvider.getStrFromGUIBundle("fileMenu"));
        exitMI.setText(resProvider.getStrFromGUIBundle("exitMI"));

        languageMenu.setText(resProvider.getStrFromGUIBundle("languageMenu"));
        for (Locale locale : resProvider.getSupportedLocales()) {
            MenuItem tmpMI = languageMIMap.get(locale);
            tmpMI.setText(locale.getDisplayLanguage(resProvider.getCurrentLocal()));
        }

        helpMenu.setText(resProvider.getStrFromGUIBundle("helpMenu"));
    }

    private MenuBar makeMenuBar() {
        MenuBar result = new MenuBar();

        result.getMenus().add(makeFileMenu());
        result.getMenus().add(makeLanguageMenu());
        result.getMenus().add(makeHelpMenu());

        return result;
    }

    private Menu makeFileMenu() {
        exitMI.setOnAction(event -> Platform.exit());

        fileMenu.getItems().addAll(
                new SeparatorMenuItem(),
                exitMI);

        return fileMenu;
    }

    private Menu makeLanguageMenu() {
        for (Locale locale : resProvider.getSupportedLocales()) {
            MenuItem localeMI = new MenuItem();
            localeMI.setOnAction(event -> {
                resProvider.setBundlesLocale(locale);
                chunkManager.updateChunksLocales();
            });
            languageMIMap.put(locale, localeMI);
            languageMenu.getItems().add(localeMI);
        }
        return languageMenu;
    }

    private Menu makeHelpMenu() {

        return helpMenu;
    }



}

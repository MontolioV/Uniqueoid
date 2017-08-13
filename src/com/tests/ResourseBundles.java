package com.tests;

import java.util.Locale;
import java.util.ResourceBundle;

public enum ResourseBundles {
    MESSAGES_EN(ResourceBundle.getBundle("com.resources.Messages_Bundle",  new Locale("en", "EN"))),
    MESSAGES_RU(ResourceBundle.getBundle("com.resources.Messages_Bundle", new Locale("ru", "RU"))),
    GUI_EN(ResourceBundle.getBundle("com.resources.GUI_Bundle", new Locale("en", "EN"))),
    GUI_RU(ResourceBundle.getBundle("com.resources.GUI_Bundle", new Locale("ru", "RU"))),
    EXCEPTIONS_EN(ResourceBundle.getBundle("com.resources.Exception_Bundle", new Locale("en", "EN"))),
    EXCEPTIONS_RU(ResourceBundle.getBundle("com.resources.Exception_Bundle", new Locale("ru", "RU")));

    private ResourceBundle resourceBundle;

    ResourseBundles(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }
}

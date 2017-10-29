package io.sourceforge.uniqueoid;

import java.util.Locale;
import java.util.ResourceBundle;

public enum ResourseBundles {
    MESSAGES_EN(ResourceBundle.getBundle("io.sourceforge.uniqueoid.bundles.Messages_Bundle",  new Locale("en", "US"))),
    MESSAGES_RU(ResourceBundle.getBundle("io.sourceforge.uniqueoid.bundles.Messages_Bundle", new Locale("ru", "RU"))),
    GUI_EN(ResourceBundle.getBundle("io.sourceforge.uniqueoid.bundles.GUI_Bundle", new Locale("en", "US"))),
    GUI_RU(ResourceBundle.getBundle("io.sourceforge.uniqueoid.bundles.GUI_Bundle", new Locale("ru", "RU"))),
    EXCEPTIONS_EN(ResourceBundle.getBundle("io.sourceforge.uniqueoid.bundles.Exception_Bundle", new Locale("en", "US"))),
    EXCEPTIONS_RU(ResourceBundle.getBundle("io.sourceforge.uniqueoid.bundles.Exception_Bundle", new Locale("ru", "RU")));

    private ResourceBundle resourceBundle;

    ResourseBundles(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }
}

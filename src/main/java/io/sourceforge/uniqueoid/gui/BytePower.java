package io.sourceforge.uniqueoid.gui;

import io.sourceforge.uniqueoid.ResourcesProvider;

/**
 * <p>Created by MontolioV on 31.10.17.
 */
public enum BytePower {
    BYTES(1, 0),
    K_BYTES((long) Math.pow(10, 3), 1),
    M_BYTES((long) Math.pow(10, 6), 2),
    G_BYTES((long) Math.pow(10, 9), 3),
    T_BYTES((long) Math.pow(10, 12), 4),
    KI_BYTES((long) Math.pow(2, 10), 5),
    MI_BYTES((long) Math.pow(2, 20), 6),
    GI_BYTES((long) Math.pow(2, 30), 7),
    TI_BYTES((long) Math.pow(2, 40), 8),;

    private long modifier;
    private int resIndex;
    private String representation;

    BytePower(long modifier, int resIndex) {
        this.modifier = modifier;
        this.resIndex = resIndex;
        this.updateLocaleContent();
    }

    public void updateLocaleContent() {
        representation = ResourcesProvider.getInstance()
                .getStrFromMessagesBundle("bytePowers")
                .split(" ")[resIndex];
    }

    public long getModifier() {
        return modifier;
    }

    @Override
    public String toString() {
        return representation;
    }
}


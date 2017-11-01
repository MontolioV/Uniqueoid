package io.sourceforge.uniqueoid.gui;

import io.sourceforge.uniqueoid.ResourcesProvider;

/**
 * <p>Created by MontolioV on 31.10.17.
 */
public enum BytePower {
    BYTES(1, 0),
    K_BYTES((long) Math.pow(10, 3), 1),
    KI_BYTES((long) Math.pow(2, 10), 2),
    M_BYTES((long) Math.pow(10, 6), 3),
    MI_BYTES((long) Math.pow(2, 20), 4),
    G_BYTES((long) Math.pow(10, 9), 5),
    GI_BYTES((long) Math.pow(2, 30), 6),
    T_BYTES((long) Math.pow(10, 12), 7),
    TI_BYTES((long) Math.pow(2, 40), 8),
    P_BYTES((long) Math.pow(10, 15), 9),
    PI_BYTES((long) Math.pow(2, 50), 10),
    E_BYTES((long) Math.pow(10, 18), 11),
    EI_BYTES((long) Math.pow(2, 60), 12),;


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


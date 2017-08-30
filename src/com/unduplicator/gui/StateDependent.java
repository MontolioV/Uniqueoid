package com.unduplicator.gui;

/**
 * Can change state depending on program state.
 * <p>Created by MontolioV on 30.08.17.
 */
public interface StateDependent {

    /**
     * May change state depending on received state.
     * @return <code>true</code> if new state differs, otherwise <code>false</code>
     */
    boolean changeState(GuiStates newState);
}

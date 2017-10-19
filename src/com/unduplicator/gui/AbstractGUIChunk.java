package com.unduplicator.gui;

import javafx.scene.Node;

/**
 * <p>Created by MontolioV on 30.08.17.
 */
public abstract class AbstractGUIChunk implements NodeRepresentable, LocaleDependent, StateDependent {
    private Node selfNode;
    private GuiStates currentState;

    /**
     * @return self representation as <tt>Node</tt>
     */
    @Override
    public Node getAsNode() {
        return selfNode;
    }

    void setSelfNode(Node node) {
        selfNode = node;
    }

    /**
     * May change state depending on received state. Saves new state.
     *
     * @param newState
     * @return <code>true</code> if new state differs, otherwise <code>false</code>
     */
    @Override
    public boolean changeState(GuiStates newState) {
        if (!newState.equals(currentState)) {
            currentState = newState;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Call <code>changeState()</code> with the current state.
     */
    public void refreshState() {
        changeState(currentState);
    }
}

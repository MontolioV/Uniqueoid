package com.unduplicator.gui;

import javafx.scene.Node;

/**
 * Object must be able to encapsulated itself in <tt>Node</tt>.
 * <p>Created by MontolioV on 29.08.17.
 */
public interface NodeRepresentable {

    /**
     *@return self representation as <tt>Node</tt>
     */
    Node getAsNode();
}

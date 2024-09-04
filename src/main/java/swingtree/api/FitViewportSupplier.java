package swingtree.api;

import javax.swing.*;

@FunctionalInterface
public interface FitViewportSupplier {

    boolean get(JViewport viewport, JComponent component);

}

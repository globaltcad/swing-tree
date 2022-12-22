package swingtree.api;

import swingtree.SimpleDelegate;

import javax.swing.*;
import java.awt.event.ComponentEvent;

@FunctionalInterface
public interface UIVerifier<C extends JComponent> {

    boolean isValid(SimpleDelegate<C, ComponentEvent> delegate);

}

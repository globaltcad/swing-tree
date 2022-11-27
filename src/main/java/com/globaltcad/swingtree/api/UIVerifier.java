package com.globaltcad.swingtree.api;

import com.globaltcad.swingtree.SimpleDelegate;

import javax.swing.*;
import java.awt.event.ComponentEvent;

@FunctionalInterface
public interface UIVerifier<C extends JComponent> {

    boolean isValid(SimpleDelegate<C, ComponentEvent> delegate);

}

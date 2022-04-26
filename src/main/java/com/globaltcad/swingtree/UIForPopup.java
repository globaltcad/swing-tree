package com.globaltcad.swingtree;

import javax.swing.*;

/**
 *  A UI maker for {@link JPopupMenu} instances.
 */
public class UIForPopup extends UIForSwing<UIForPopup, JPopupMenu> {

    /**
     * Instances of the BasicBuilder as well as its sub types always wrap
     * a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    protected UIForPopup(JPopupMenu component) {
        super(component);
    }

    public UIForPopup isBorderPaintedIf(boolean borderPainted) {
        this.component.setBorderPainted(borderPainted);
        return this;
    }

    public UIForPopup add(JMenuItem item) { return this.add(UI.of(item)); }

    public UIForPopup add(JSeparator separator) { return this.add(UI.of(separator)); }

    public UIForPopup add(JPanel panel) { return this.add(UI.of(panel)); }
}


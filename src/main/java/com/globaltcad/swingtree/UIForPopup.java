package com.globaltcad.swingtree;

import javax.swing.*;

/**
 *  A swing tree builder for {@link JPopupMenu} instances.
 */
public class UIForPopup extends UIForSwing<UIForPopup, JPopupMenu>
{
    protected UIForPopup(JPopupMenu component) { super(component); }


    public UIForPopup isBorderPaintedIf(boolean borderPainted) {
        this.component.setBorderPainted(borderPainted);
        return this;
    }

    public UIForPopup add(JMenuItem item) { return this.add(UI.of(item)); }

    public UIForPopup add(JSeparator separator) { return this.add(UI.of(separator)); }

    public UIForPopup add(JPanel panel) { return this.add(UI.of(panel)); }
}


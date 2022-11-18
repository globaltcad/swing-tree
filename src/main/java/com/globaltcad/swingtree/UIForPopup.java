package com.globaltcad.swingtree;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JPopupMenu} instances.
 */
public class UIForPopup<P extends JPopupMenu> extends UIForAbstractSwing<UIForPopup<P>, P>
{
    protected UIForPopup(P component) { super(component); }


    public UIForPopup<P> isBorderPaintedIf(boolean borderPainted) {
        getComponent().setBorderPainted(borderPainted);
        return this;
    }

    public UIForPopup<P> add(JMenuItem item) { return this.add(UI.of(item)); }

    public UIForPopup<P> add(JSeparator separator) { return this.add(UI.of(separator)); }

    public UIForPopup<P> add(JPanel panel) { return this.add(UI.of(panel)); }
}


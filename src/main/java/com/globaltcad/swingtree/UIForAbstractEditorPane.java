package com.globaltcad.swingtree;

import javax.swing.*;

public class UIForAbstractEditorPane<I, C extends JEditorPane> extends UIForTextComponent<I, C>
{
    /**
     * {@link UIForAbstractSwing} types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    public UIForAbstractEditorPane(C component) { super(component); }


}

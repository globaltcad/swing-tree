package com.globaltcad.swingtree;

import javax.swing.*;

public class UIForAbstractEditorPane<I, C extends JEditorPane> extends UIForTextComponent<I, C>
{
    /**
     * Instances of the BasicBuilder as well as its sub types always wrap
     * a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    public UIForAbstractEditorPane(C component) { super(component); }


}

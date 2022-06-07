package com.globaltcad.swingtree;

import javax.swing.*;

/**
 *  A swing tree builder for {@link JEditorPane} instances.
 */
public class UIForEditorPane<P extends JEditorPane> extends UIForAbstractEditorPane<UIForEditorPane<P>, P>
{
    /**
     * {@link UIForAbstractSwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    public UIForEditorPane(P component) { super(component); }
}

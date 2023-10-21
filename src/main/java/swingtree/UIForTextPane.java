package swingtree;

import javax.swing.*;

/**
 *  A SwingTree builder node designed for configuring {@link UIForTextPane} instances.
 */
public class UIForTextPane<P extends JTextPane> extends UIForAnyEditorPane<UIForTextPane<P>, P>
{
    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    protected UIForTextPane( P component ) { super(component); }
}

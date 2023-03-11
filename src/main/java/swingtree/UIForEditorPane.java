package swingtree;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JEditorPane} instances.
 * 	<p>
 * 	<b>Take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class or other classes.</b>
 */
public class UIForEditorPane<P extends JEditorPane> extends UIForAbstractEditorPane<UIForEditorPane<P>, P>
{
    /**
     * {@link UIForAbstractSwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    public UIForEditorPane( P component ) { super(component); }
}

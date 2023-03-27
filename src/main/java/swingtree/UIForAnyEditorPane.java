package swingtree;

import javax.swing.*;

public class UIForAnyEditorPane<I, C extends JEditorPane> extends UIForAnyTextComponent<I, C>
{
    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    public UIForAnyEditorPane(C component) { super(component); }


}

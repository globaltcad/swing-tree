package com.globaltcad.swingtree;

import javax.swing.*;

public class UIForEditorPane extends UIForAbstractEditorPane<UIForEditorPane, JEditorPane>
{
    /**
     * {@link UIForAbstractSwing} types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    public UIForEditorPane(JEditorPane component) {
        super(component);
    }
}

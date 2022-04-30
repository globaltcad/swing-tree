package com.globaltcad.swingtree;

import javax.swing.*;

public class UIForTextPane extends UIForAbstractEditorPane<UIForTextPane, JTextPane>
{
    /**
     * Instances of the BasicBuilder as well as its sub types always wrap
     * a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    public UIForTextPane(JTextPane component) {
        super(component);
    }
}

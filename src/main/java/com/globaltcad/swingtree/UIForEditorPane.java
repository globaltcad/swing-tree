package com.globaltcad.swingtree;

import javax.swing.*;

public class UIForEditorPane extends UIForTextComponent<UIForEditorPane, JEditorPane>
{
    /**
     * Instances of the BasicBuilder as well as its sub types always wrap
     * a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    public UIForEditorPane(JEditorPane component) { super(component); }


}

package com.globaltcad.swingtree;

import javax.swing.*;

public class UIForScrollPane extends UIForAbstractSwing<UIForScrollPane, JScrollPane>
{
    /**
     * Instances of the BasicBuilder as well as its sub types always wrap
     * a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    public UIForScrollPane(JScrollPane component) { super(component); }

    @Override
    protected void _add(JComponent component, Object conf) {
        if ( conf != null )
            throw new IllegalArgumentException("Unknown constraint '"+conf+"'! (scroll pane does not support any constraint)");
        this.component.setViewportView(component);
    }

    public final UIForScrollPane withScrollBarPolicy(UI.Scroll scrollPolicy) {
        this.withVerticalScrollBarPolicy(scrollPolicy);
        this.withHorizontalScrollBarPolicy(scrollPolicy);
        return this;
    }

    public final UIForScrollPane withVerticalScrollBarPolicy(UI.Scroll scrollBarPolicy) {
        switch ( scrollBarPolicy )
        {
            case NEVER: this.component.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER); break;
            case ALWAYS: this.component.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS); break;
            case AS_NEEDED: this.component.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED); break;
        }
        return this;
    }

    public final UIForScrollPane withHorizontalScrollBarPolicy(UI.Scroll scrollBarPolicy) {
        switch ( scrollBarPolicy )
        {
            case NEVER: this.component.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); break;
            case ALWAYS: this.component.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS); break;
            case AS_NEEDED: this.component.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED); break;
        }
        return this;
    }

}

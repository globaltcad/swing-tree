package com.globaltcad.swingtree;

import javax.swing.*;

/**
 *  A swing tree builder for {@link JScrollPane} instances.
 */
public class UIForScrollPane<P extends JScrollPane> extends UIForAbstractSwing<UIForScrollPane<P>, P>
{
    /**
     * {@link UIForAbstractSwing} types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    public UIForScrollPane(P component) { super(component); }

    @Override
    protected void _add(JComponent component, Object conf) {
        if ( conf != null )
            throw new IllegalArgumentException("Unknown constraint '"+conf+"'! (scroll pane does not support any constraint)");
        this.component.setViewportView(component);
    }

    public final UIForScrollPane<P> withScrollBarPolicy(UI.Scroll scrollPolicy) {
        this.withVerticalScrollBarPolicy(scrollPolicy);
        this.withHorizontalScrollBarPolicy(scrollPolicy);
        return this;
    }

    public final UIForScrollPane<P> withVerticalScrollBarPolicy(UI.Scroll scrollBarPolicy) {
        switch ( scrollBarPolicy )
        {
            case NEVER: this.component.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER); break;
            case ALWAYS: this.component.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS); break;
            case AS_NEEDED: this.component.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED); break;
        }
        return this;
    }

    public final UIForScrollPane<P> withHorizontalScrollBarPolicy(UI.Scroll scrollBarPolicy) {
        switch ( scrollBarPolicy )
        {
            case NEVER: this.component.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); break;
            case ALWAYS: this.component.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS); break;
            case AS_NEEDED: this.component.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED); break;
        }
        return this;
    }

}

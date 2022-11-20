package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.mvvm.Val;
import com.globaltcad.swingtree.api.mvvm.Var;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JScrollPane} instances.
 */
public class UIForScrollPane<P extends JScrollPane> extends UIForAbstractSwing<UIForScrollPane<P>, P>
{
    /**
     * {@link UIForAbstractSwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    public UIForScrollPane(P component) { super(component); }

    @Override
    protected void _add( JComponent component, Object conf ) {
        if ( conf != null )
            throw new IllegalArgumentException("Unknown constraint '"+conf+"'! (scroll pane does not support any constraint)");
        getComponent().setViewportView(component);
    }

    public final UIForScrollPane<P> with( UI.ScrollBarPolicy scrollPolicy ) {
        this.withVertical(scrollPolicy);
        this.withHorizontal(scrollPolicy);
        return this;
    }

    public final UIForScrollPane<P> withVertical( UI.ScrollBarPolicy scrollBarPolicy ) {
        P pane = getComponent();
        switch ( scrollBarPolicy )
        {
            case NEVER: pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER); break;
            case ALWAYS: pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS); break;
            case AS_NEEDED: pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED); break;
        }
        return this;
    }

    public final UIForScrollPane<P> withVerticalScrollBarPolicy( Val<UI.ScrollBarPolicy> var ) {
        LogUtil.nullArgCheck(var, "var", Var.class);
        var.onShow(v-> _doUI(()->withVertical(v)));
        return this;
    }

    public final UIForScrollPane<P> withHorizontal( UI.ScrollBarPolicy scrollBarPolicy ) {
        P pane = getComponent();
        switch ( scrollBarPolicy )
        {
            case NEVER: pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); break;
            case ALWAYS: pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS); break;
            case AS_NEEDED: pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED); break;
        }
        return this;
    }

    public final UIForScrollPane<P> withHorizontalScrollBarPolicy( Val<UI.ScrollBarPolicy> var ) {
        LogUtil.nullArgCheck(var, "var", Var.class);
        var.onShow(v-> _doUI(()->withHorizontal(v)));
        return this;
    }

}

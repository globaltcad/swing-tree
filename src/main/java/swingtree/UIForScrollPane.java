package swingtree;

import sprouts.Val;

import javax.swing.*;

/**
 *  A SwingTree builder node designed for configuring {@link JScrollPane} instances.
 */
public class UIForScrollPane<P extends JScrollPane> extends UIForAnySwing<UIForScrollPane<P>, P>
{
    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    protected UIForScrollPane( P component ) { super(component); }

    @Override
    protected void _add( JComponent component, Object conf ) {
        if ( conf != null ) {
            // The user wants to add a component to the scroll pane with a specific constraint.
            // Swing does not support any constraints for scroll panes, but we are not Swing, we are SwingTree!
            component = UI.panel("fill, ins 0").add(conf.toString(), component).getComponent();
            //  ^ So we improve this situation by wrapping the component in a mig layout panel, supporting constraints.

            // Let's strip it of any visible properties, since it should serve merely as a container.
            component.setBorder(null);
            component.setOpaque(false);
            component.setBackground(null);
        }
        getComponent().setViewportView(component);
    }

    /**
     *  Use this to set the scroll bars policy for both horizontal and vertical scroll bars.
     *
     * @param scrollPolicy The scroll policy to use.
     * @return This builder node.
     */
    public final UIForScrollPane<P> withScrollBarPolicy( UI.Active scrollPolicy ) {
        this.withVerticalScrollBarPolicy(scrollPolicy);
        this.withHorizontalScrollBarPolicy(scrollPolicy);
        return this;
    }

    /**
     *  Use this to set the scroll bars policy for the vertical scroll bar.
     *
     * @param scrollBarPolicy The scroll policy to use.
     * @return This builder node.
     */
    public final UIForScrollPane<P> withVerticalScrollBarPolicy( UI.Active scrollBarPolicy ) {
        P pane = getComponent();
        switch ( scrollBarPolicy )
        {
            case NEVER: pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER); break;
            case ALWAYS: pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS); break;
            case AS_NEEDED: pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED); break;
        }
        return this;
    }

    /**
     *  Use this to dynamically set the scroll bars policy for the vertical scroll bar.
     *
     * @param scrollBarPolicy The scroll policy property to use.
     * @return This builder node.
     */
    public final UIForScrollPane<P> withVerticalScrollBarPolicy( Val<UI.Active> scrollBarPolicy ) {
        NullUtil.nullArgCheck(scrollBarPolicy, "scrollBarPolicy", Val.class);
        _onShow(scrollBarPolicy, v -> withVerticalScrollBarPolicy(v));
        return this;
    }

    /**
     *  Use this to set the scroll bars policy for the horizontal scroll bar.
     *
     * @param scrollBarPolicy The scroll policy to use.
     * @return This builder node.
     */
    public final UIForScrollPane<P> withHorizontalScrollBarPolicy(UI.Active scrollBarPolicy ) {
        P pane = getComponent();
        switch ( scrollBarPolicy )
        {
            case NEVER: pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); break;
            case ALWAYS: pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS); break;
            case AS_NEEDED: pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED); break;
        }
        return this;
    }

    /**
     *  Use this to dynamically set the scroll bars policy for the horizontal scroll bar.
     *
     * @param scrollBarPolicy The scroll policy property to use.
     * @return This builder node.
     */
    public final UIForScrollPane<P> withHorizontalScrollBarPolicy( Val<UI.Active> scrollBarPolicy ) {
        NullUtil.nullArgCheck(scrollBarPolicy, "scrollBarPolicy", Val.class);
        _onShow(scrollBarPolicy, v -> withHorizontalScrollBarPolicy(v) );
        return this;
    }

    /**
     *  Use this to set the vertical scroll increment.
     *
     * @param increment The scroll vertical increment to use.
     * @return This builder instance, to allow for method chaining.
     */
    public final UIForScrollPane<P> withVerticalScrollIncrement( int increment ) {
        getComponent().getVerticalScrollBar().setUnitIncrement(increment);
        return this;
    }

    /**
     *  Use this to set the horizontal scroll increment.
     *
     * @param increment The scroll horizontal increment to use.
     * @return This builder instance, to allow for method chaining.
     */
    public final UIForScrollPane<P> withHorizontalScrollIncrement( int increment ) {
        getComponent().getHorizontalScrollBar().setUnitIncrement(increment);
        return this;
    }

    /**
     * Use this to set the vertical and horizontal scroll increment.
     * @param increment The scroll increment to use.
     * @return This builder instance, to allow for method chaining.
     */
    public final UIForScrollPane<P> withScrollIncrement( int increment ) {
        return this.withVerticalScrollIncrement(increment)
                   .withHorizontalScrollIncrement(increment);
    }

    /**
     *  Use this to set the vertical scroll block increment.
     *
     * @param increment The scroll vertical block increment to use.
     * @return This builder instance, to allow for method chaining.
     */
    public final UIForScrollPane<P> withVerticalBlockScrollIncrement(int increment ) {
        getComponent().getVerticalScrollBar().setBlockIncrement(increment);
        return this;
    }

    /**
     *  Use this to set the horizontal scroll block increment.
     *  It is the amount to change the scrollbar's value by,
     *  given a block (usually "page") up/down request or when the user clicks
     *  above or below the scrollbar "knob" to change the value
     *  up or down by large amount.
     *
     * @param increment The scroll horizontal block increment to use.
     * @return This builder instance, to allow for method chaining.
     */
    public final UIForScrollPane<P> withHorizontalBlockScrollIncrement(int increment ) {
        getComponent().getHorizontalScrollBar().setBlockIncrement(increment);
        return this;
    }

    /**
     * Use this to set the vertical and horizontal scroll block increment.
     * The block increment is the amount to change the scrollbar's value by,
     * given a block (usually "page") up/down request or when the user clicks
     * above or below the scrollbar "knob" to change the value
     * up or down by large amount.
     *
     * @param increment The scroll block increment to use.
     * @return This builder instance, to allow for method chaining.
     */
    public final UIForScrollPane<P> withBlockScrollIncrement( int increment ) {
        return this.withVerticalBlockScrollIncrement(increment)
                   .withHorizontalBlockScrollIncrement(increment);
    }
}

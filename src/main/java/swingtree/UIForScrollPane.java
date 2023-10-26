package swingtree;

import sprouts.Val;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.util.Objects;

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
    protected void _doAddComponent( JComponent newComponent, Object conf, P thisComponent ) {
        if ( conf != null ) {
            // The user wants to add a component to the scroll pane with a specific constraint.
            // Swing does not support any constraints for scroll panes, but we are not Swing, we are SwingTree!
            newComponent = UI.panel("fill, ins 0").add(conf.toString(), newComponent).getComponent();
            //  ^ So we improve this situation by wrapping the component in a mig layout panel, supporting constraints.

            // Let's strip it of any visible properties, since it should serve merely as a container.
            newComponent.setBorder(null);
            newComponent.setOpaque(false);
            newComponent.setBackground(null);
        }
        thisComponent.setViewportView(newComponent);
    }

    /**
     *  Use this to set the scroll bars policy for both horizontal and vertical scroll bars.
     *
     * @param scrollPolicy The scroll policy to use.
     * @return This builder node.
     */
    public final UIForScrollPane<P> withScrollBarPolicy( UI.Active scrollPolicy ) {
        Objects.requireNonNull(scrollPolicy);
        return _with( thisComponent -> {
                    _setVerticalScrollBarPolicy(thisComponent, scrollPolicy);
                    _setHorizontalScrollBarPolicy(thisComponent, scrollPolicy);
               })
               ._this();
    }

    /**
     *  Use this to set the scroll bars policy for the vertical scroll bar.
     *
     * @param scrollBarPolicy The scroll policy to use.
     * @return This builder node.
     */
    public final UIForScrollPane<P> withVerticalScrollBarPolicy( UI.Active scrollBarPolicy ) {
        Objects.requireNonNull(scrollBarPolicy);
        return _with( thisComponent -> {
                    _setVerticalScrollBarPolicy(thisComponent, scrollBarPolicy);
                })
                ._this();
    }

    private void _setVerticalScrollBarPolicy( P thisComponent, UI.Active scrollBarPolicy ) {
        switch ( scrollBarPolicy ) {
            case NEVER:     thisComponent.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER); break;
            case ALWAYS:    thisComponent.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS); break;
            case AS_NEEDED: thisComponent.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED); break;
        }
    }

    /**
     *  Use this to dynamically set the scroll bars policy for the vertical scroll bar.
     *
     * @param scrollBarPolicy The scroll policy property to use.
     * @return This builder node.
     */
    public final UIForScrollPane<P> withVerticalScrollBarPolicy( Val<UI.Active> scrollBarPolicy ) {
        NullUtil.nullArgCheck(scrollBarPolicy, "scrollBarPolicy", Val.class);
        NullUtil.nullPropertyCheck(scrollBarPolicy, "scrollBarPolicy", "Null is not a valid scroll bar policy.");
        return _withOnShow( scrollBarPolicy, (thisComponent,v) -> {
                    _setVerticalScrollBarPolicy(thisComponent, v);
                })
                ._with( thisComponent -> {
                    _setVerticalScrollBarPolicy(thisComponent, scrollBarPolicy.get());
                })
                ._this();
    }

    /**
     *  Use this to set the scroll bars policy for the horizontal scroll bar.
     *
     * @param scrollBarPolicy The scroll policy to use.
     * @return This builder node.
     */
    public final UIForScrollPane<P> withHorizontalScrollBarPolicy(UI.Active scrollBarPolicy ) {
        Objects.requireNonNull(scrollBarPolicy);
        return _with( thisComponent -> {
                    _setHorizontalScrollBarPolicy(thisComponent, scrollBarPolicy);
                })
                ._this();
    }

    private void _setHorizontalScrollBarPolicy( P thisComponent, UI.Active scrollBarPolicy ) {
        switch ( scrollBarPolicy ) {
            case NEVER: thisComponent.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); break;
            case ALWAYS: thisComponent.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS); break;
            case AS_NEEDED: thisComponent.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED); break;
        }
    }

    /**
     *  Use this to dynamically set the scroll bars policy for the horizontal scroll bar.
     *
     * @param scrollBarPolicy The scroll policy property to use.
     * @return This builder node.
     */
    public final UIForScrollPane<P> withHorizontalScrollBarPolicy( Val<UI.Active> scrollBarPolicy ) {
        NullUtil.nullArgCheck(scrollBarPolicy, "scrollBarPolicy", Val.class);
        NullUtil.nullPropertyCheck(scrollBarPolicy, "scrollBarPolicy", "Null is not a valid scroll bar policy.");
        return _withOnShow( scrollBarPolicy, (thisComponent,v) -> {
                    _setHorizontalScrollBarPolicy(thisComponent, v);
                })
                ._with( thisComponent -> {
                    _setHorizontalScrollBarPolicy(thisComponent, scrollBarPolicy.get());
                })
                ._this();
    }

    /**
     *  Use this to set the vertical scroll increment.
     *
     * @param increment The scroll vertical increment to use.
     * @return This builder instance, to allow for method chaining.
     */
    public final UIForScrollPane<P> withVerticalScrollIncrement( int increment ) {
        return _with( thisComponent -> {
                    thisComponent.getVerticalScrollBar().setUnitIncrement(increment);
                })
                ._this();
    }

    /**
     *  Use this to set the horizontal scroll increment.
     *
     * @param increment The scroll horizontal increment to use.
     * @return This builder instance, to allow for method chaining.
     */
    public final UIForScrollPane<P> withHorizontalScrollIncrement( int increment ) {
        return _with( thisComponent -> {
                    thisComponent.getHorizontalScrollBar().setUnitIncrement(increment);
                })
                ._this();
    }

    /**
     * Use this to set the vertical and horizontal scroll increment.
     * @param increment The scroll increment to use.
     * @return This builder instance, to allow for method chaining.
     */
    public final UIForScrollPane<P> withScrollIncrement( int increment ) {
        return _with( thisComponent -> {
                    thisComponent.getVerticalScrollBar().setUnitIncrement(increment);
                    thisComponent.getHorizontalScrollBar().setUnitIncrement(increment);
                })
                ._this();
    }

    /**
     *  Use this to set the vertical scroll block increment.
     *
     * @param increment The scroll vertical block increment to use.
     * @return This builder instance, to allow for method chaining.
     */
    public final UIForScrollPane<P> withVerticalBlockScrollIncrement( int increment ) {
        return _with( thisComponent -> {
                    thisComponent.getVerticalScrollBar().setBlockIncrement(increment);
                })
                ._this();
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
    public final UIForScrollPane<P> withHorizontalBlockScrollIncrement( int increment ) {
        return _with( thisComponent -> {
                    thisComponent.getHorizontalScrollBar().setBlockIncrement(increment);
                })
                ._this();
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
        return _with( thisComponent -> {
                    thisComponent.getVerticalScrollBar().setBlockIncrement(increment);
                    thisComponent.getHorizontalScrollBar().setBlockIncrement(increment);
                })
                ._this();
    }
}

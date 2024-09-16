package swingtree;

import net.miginfocom.swing.MigLayout;
import org.jspecify.annotations.Nullable;
import sprouts.Val;
import swingtree.components.JBox;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Objects;

public abstract class UIForAnyScrollPane<I, P extends JScrollPane> extends UIForAnySwing<I, P>
{
    @Override
    protected void _addComponentTo(P thisComponent, JComponent addedComponent, @Nullable Object constraints) {
        if ( constraints != null ) {
            if ( addedComponent instanceof Scrollable ) {
                ThinScrollableDelegateBox thinDelegationBox = new ThinScrollableDelegateBox((Scrollable) addedComponent);
                thinDelegationBox.add(constraints.toString(), addedComponent);
            } else {
                // The user wants to add a component to the scroll pane with a specific constraint.
                // Swing does not support any constraints for scroll panes, but we are not Swing, we are SwingTree!
                ThinDelegationBox thinDelegationBox = new ThinDelegationBox(addedComponent);
                thinDelegationBox.add(constraints.toString(), addedComponent);
                addedComponent = thinDelegationBox;
                //  ^ So we improve this situation by wrapping the component in a mig layout panel, supporting constraints.

                // Let's strip it of any visible properties, since it should serve merely as a container.
                addedComponent.setBorder(null);
                addedComponent.setOpaque(false);
                addedComponent.setBackground(null);
            }
        }
        thisComponent.setViewportView(addedComponent);
    }

    /**
     *  Use this to set the scroll bars policy for both horizontal and vertical scroll bars.
     *
     * @param scrollPolicy The scroll policy to use.
     * @return This builder node.
     */
    public final I withScrollBarPolicy( UI.Active scrollPolicy ) {
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
    public final I withVerticalScrollBarPolicy( UI.Active scrollBarPolicy ) {
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
    public final I withVerticalScrollBarPolicy( Val<UI.Active> scrollBarPolicy ) {
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
    public final I withHorizontalScrollBarPolicy(UI.Active scrollBarPolicy ) {
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
    public final I withHorizontalScrollBarPolicy( Val<UI.Active> scrollBarPolicy ) {
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
    public final I withVerticalScrollIncrement( int increment ) {
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
    public final I withHorizontalScrollIncrement( int increment ) {
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
    public final I withScrollIncrement( int increment ) {
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
    public final I withVerticalBlockScrollIncrement( int increment ) {
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
    public final I withHorizontalBlockScrollIncrement( int increment ) {
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
    public final I withBlockScrollIncrement( int increment ) {
        return _with( thisComponent -> {
                    thisComponent.getVerticalScrollBar().setBlockIncrement(increment);
                    thisComponent.getHorizontalScrollBar().setBlockIncrement(increment);
                })
                ._this();
    }


    static class ThinDelegationBox extends JBox {

        protected final JComponent _child;

        ThinDelegationBox(JComponent child) {
            setLayout(new MigLayout("fill, ins 0, hidemode 2, gap 0"));
            _child = child;
        }

        @Override
        public void setSize(Dimension d) {
            super.setSize(d);
            _child.setSize(d);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension prefChildSize = _child.getPreferredSize();
            Dimension prefSelfSize  = super.getPreferredSize();
            if ( !Objects.equals(prefChildSize, prefSelfSize) ) {
                this.setPreferredSize(prefChildSize);
            }
            return prefChildSize;
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension minChildSize = _child.getMinimumSize();
            Dimension minSelfSize  = super.getMinimumSize();
            if ( !Objects.equals(minChildSize, minSelfSize) ) {
                this.setMinimumSize(minChildSize);
            }
            return minChildSize;
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension maxChildSize = _child.getMaximumSize();
            Dimension maxSelfSize  = super.getMaximumSize();
            if ( !Objects.equals(maxChildSize, maxSelfSize) ) {
                this.setMinimumSize(maxChildSize);
            }
            return maxChildSize;
        }
    }

    private static final class ThinScrollableDelegateBox extends ThinDelegationBox implements Scrollable {

        private final Scrollable _scrollable;


        ThinScrollableDelegateBox( Scrollable child ) {
            super((JComponent) child);
            _scrollable = child;
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return _scrollable.getPreferredScrollableViewportSize();
        }

        @Override
        public int getScrollableUnitIncrement( Rectangle visibleRect, int orientation, int direction ) {
            return _scrollable.getScrollableUnitIncrement(visibleRect, orientation, direction);
        }

        @Override
        public int getScrollableBlockIncrement( Rectangle visibleRect, int orientation, int direction ) {
            return _scrollable.getScrollableBlockIncrement(visibleRect, orientation, direction);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return _scrollable.getScrollableTracksViewportWidth();
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return _scrollable.getScrollableTracksViewportHeight();
        }
    }
}

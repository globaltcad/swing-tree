package swingtree;

import net.miginfocom.swing.MigLayout;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Val;
import swingtree.components.JBox;
import swingtree.layout.AddConstraint;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import java.awt.*;
import java.util.Objects;

/**
 *  Defines an abstract builder for
 *  constructing a scroll pane or any subclass of {@link JScrollPane}.
 *
 * @param <I> The concrete type of the builder instance, which is
 *           important as a return type for the builder methods.
 * @param <P> The type of the scroll pane or any subclass of {@link JScrollPane}.
 */
public abstract class UIForAnyScrollPane<I, P extends JScrollPane> extends UIForAnySwing<I, P>
{
    private static final Logger log = LoggerFactory.getLogger(UIForAnyScrollPane.class);

    @Override
    protected void _addComponentTo(P thisComponent, JComponent addedComponent, @Nullable AddConstraint constraints) {
        if ( constraints != null ) {
            if ( addedComponent instanceof Scrollable ) {
                ThinScrollableDelegateBox thinDelegationBox = new ThinScrollableDelegateBox((Scrollable) addedComponent);
                thinDelegationBox.add(addedComponent, constraints.toConstraintForLayoutManager());
            } else {
                // The user wants to add a component to the scroll pane with a specific constraint.
                // Swing does not support any constraints for scroll panes, but we are not Swing, we are SwingTree!
                ThinDelegationBox thinDelegationBox = new ThinDelegationBox(addedComponent);
                thinDelegationBox.add(addedComponent, constraints.toConstraintForLayoutManager());
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
     *  Use this to set the scroll bars policy for both horizontal and vertical scroll bars.<br>
     *  The scroll policy can be one of the following:
     *  <ul>
     *      <li>{@link swingtree.UI.Active#NEVER}: The scrolls bar will never be displayed.</li>
     *      <li>{@link swingtree.UI.Active#ALWAYS}: The scrolls bar will always be displayed.</li>
     *      <li>{@link swingtree.UI.Active#AS_NEEDED}:
     *          The two scroll bars will only be displayed when needed,
     *          i.e. when the content is too large to fit in the viewport
     *          and scrolling is required.
     *      </li>
     *  </ul>
     *
     * @param scrollPolicy The scroll policy to use.
     * @return The next builder instance, to allow for method chaining.
     * @throws NullPointerException If the argument is null.
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
     *  Use this to set the scroll bars policy for the vertical scroll bar,
     *  which controls when the vertical scroll bar should be displayed or not.<br>
     *  The scroll policy can be one of the following:
     *  <ul>
     *      <li>{@link swingtree.UI.Active#NEVER}: The vertical scroll bar will never be displayed.</li>
     *      <li>{@link swingtree.UI.Active#ALWAYS}: The vertical scroll bar will always be displayed.</li>
     *      <li>{@link swingtree.UI.Active#AS_NEEDED}:
     *          The vertical scroll bar will only be displayed when needed,
     *          i.e. when the content is too large to fit in the viewport
     *          and scrolling is required.
     *      </li>
     *  </ul>
     *
     * @param scrollBarPolicy The scroll policy to determine when the vertical scroll bar should be displayed.
     * @return This builder node, to allow for method chaining.
     * @throws NullPointerException If the argument is null.
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
     *  When the property changes, the scroll bar policy will be updated accordingly.
     *  <p>
     *  The scroll policy can be one of the following:
     *  <ul>
     *      <li>{@link swingtree.UI.Active#NEVER}: The vertical scroll bar will never be displayed.</li>
     *      <li>{@link swingtree.UI.Active#ALWAYS}: The vertical scroll bar will always be displayed.</li>
     *      <li>{@link swingtree.UI.Active#AS_NEEDED}:
     *          The vertical scroll bar will only be displayed when needed,
     *          i.e. when the content is too large to fit in the viewport
     *          and scrolling is required.
     *      </li>
     *  </ul>
     *
     * @param scrollBarPolicy The scroll policy property, whose value will determine when
     *                        the vertical scroll bar should be displayed.
     * @return This builder instance, to allow for method chaining.
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
     *  The scroll policy can be one of the following:
     *  <ul>
     *      <li>{@link swingtree.UI.Active#NEVER}: The horizontal scroll bar will never be displayed.</li>
     *      <li>{@link swingtree.UI.Active#ALWAYS}: The horizontal scroll bar will always be displayed.</li>
     *      <li>{@link swingtree.UI.Active#AS_NEEDED}:
     *          The horizontal scroll bar will only be displayed when needed,
     *          i.e. when the content is too large to fit in the viewport
     *          and scrolling is required.
     *      </li>
     *  </ul>
     *
     * @param scrollBarPolicy The scroll policy to use.
     * @return The next builder instance, to allow for method chaining.
     * @throws NullPointerException If the argument is null.
     */
    public final I withHorizontalScrollBarPolicy( UI.Active scrollBarPolicy ) {
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
     *  When the property changes, the scroll bar policy will be updated accordingly.
     *  <p>
     *  The scroll policy can be one of the following:
     *  <ul>
     *      <li>{@link swingtree.UI.Active#NEVER}: The horizontal scroll bar will never be displayed.</li>
     *      <li>{@link swingtree.UI.Active#ALWAYS}: The horizontal scroll bar will always be displayed.</li>
     *      <li>{@link swingtree.UI.Active#AS_NEEDED}:
     *          The horizontal scroll bar will only be displayed when needed,
     *          i.e. when the content is too large to fit in the viewport
     *          and scrolling is required.
     *      </li>
     *  </ul>
     *
     * @param scrollBarPolicy The scroll policy property, whose value will determine when
     *                        the horizontal scroll bar should be displayed.
     * @return The next builder instance, to allow for method chaining.
     * @throws NullPointerException If the argument is null.
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
     *  Use this to set the vertical scroll increment unit,
     *  which controls how far the content moves when you
     *  use the mouse wheel, scroll gesture on a touchpad or
     *  press the arrow buttons on the scrollbar.
     *  This can be thought of as the smallest step size for
     *  scrolling. Like for example, scrolling by one line of text
     *  at a time in a text area.
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
     *  Use this to set the horizontal scroll increment unit,
     *  which typically controls how far the content moves when you:
     *  <ul>
     *      <li>press the left and right arrow buttons on the scrollbar</li>
     *      <li>press the left and right arrow buttons on the keyboard</li>
     *      <li>use the mouse wheel or scroll gesture on a touchpad</li>
     *  </ul>
     *  <br>
     *  This can be thought of as the smallest step size for
     *  scrolling. Like for example, scrolling by one line of text
     *  at a time in a text area.
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
     * Use this to set the vertical and horizontal scroll increment,
     * which controls how far the content moves when you:
     *  <ul>
     *      <li>press the arrow buttons on the scrollbars</li>
     *      <li>press the arrow buttons on the keyboard</li>
     *      <li>use the mouse wheel or scroll gesture on a touchpad</li>
     *  </ul>
     *  <br>
     *  This can be thought of as the smallest step size for
     *  scrolling. Like for example, scrolling by one line of text
     *  at a time in a text area.
     *
     * @see #withVerticalScrollIncrement(int) if you only want to define the vertical increment.
     * @see #withHorizontalScrollIncrement(int) if you only want to define the horizontal increment.
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
     *  Use this to set the vertical scroll bar block increment,
     *  which typically controls how far the content moves when you:
     *  <ul>
     *      <li>press the page up or page down keys (not to be confused with the arrow keys)</li>
     *      <li>click on a scroll bar track (the empty area of the scrollbar, not the thumb or arrows)</li>
     *  </ul>
     *  It represents a larger jump, like moving an entire "page" or a
     *  significant chunk of content.
     *  <p>
     *  Note, that if the argument is equal to the value of Integer.MIN_VALUE,
     *  then most look and feel implementations will not provide scrolling
     *  to the right/down.
     *  <br><b>
     *  Please be aware that look and feel implementations
     *  that provide custom scrolling behavior may ignore
     *  the block increment value.
     *  </b>
     *
     * @param increment The scroll vertical block increment to use when scrolling by a "block".
     * @return This builder instance, to allow for method chaining.
     */
    public final I withVerticalBlockScrollIncrement( int increment ) {
        return _with( thisComponent -> {
                    thisComponent.getVerticalScrollBar().setBlockIncrement(increment);
                })
                ._this();
    }

    /**
     *  Use this to set the horizontal scroll bar block increment,
     *  which typically controls how far the content moves
     *  to the left or right when you:
     *  <ul>
     *      <li>press the page up or page down keys (not to be confused with the arrow keys)</li>
     *      <li>click on a scroll bar track (the empty area of the scrollbar, not the thumb or arrows)</li>
     *  </ul>
     *  <br><b>
     *  Please be aware that look and feel implementations
     *  that provide custom scrolling behavior may ignore
     *  the block increment value.
     *  </b>
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
     * Use this to set both the vertical and horizontal scroll block increment.
     * The block increment is the amount to change the scrollbar's value by,
     * given a block (usually "page") up/down request or when the user clicks
     * above or below the scrollbar "knob" to change the value
     * up or down by large amount.
     * <br><b>
     *  Please be aware that look and feel implementations
     *  that provide custom scrolling behavior may ignore
     *  the block increment value.
     * </b>
     *
     * @see #withVerticalBlockScrollIncrement(int) if you only want to define the vertical increment.
     * @see #withHorizontalBlockScrollIncrement(int) if you only want to define the horizontal increment.
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


    /**
     *  Delegate class for wrapping a component in a thin container
     *  which always has the same sizes as the wrapped component.
     */
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
            Dimension prefChildSize = null;
            try {
                LayoutManager layout = _child.getLayout();
                if ( layout != null)
                    prefChildSize = layout.preferredLayoutSize(_child);
            } catch (Exception e) {
                log.warn(SwingTree.get().logMarker(), "Failed to compute preferred size from the layout manager of the child component.", e);
            }
            if ( prefChildSize == null )
                prefChildSize = _child.getPreferredSize();
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

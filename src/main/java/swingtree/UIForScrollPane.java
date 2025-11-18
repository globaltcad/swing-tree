package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.api.Configurator;
import swingtree.components.JScrollPanels;
import swingtree.components.listener.NestedJScrollPanelScrollCorrection;
import swingtree.layout.AddConstraint;
import swingtree.layout.Bounds;
import swingtree.layout.Size;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JScrollPane} instances. <br>
 *  Use {@link UI#scrollPane()} or {@link UI#scrollPane(Configurator)} to create a new instance
 *  of this builder type.
 *
 * @param <P> The type of {@link JScrollPane} that this {@link UIForScrollPane} is configuring.
 */
public final class UIForScrollPane<P extends JScrollPane> extends UIForAnyScrollPane<UIForScrollPane<P>,P>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(UIForScrollPane.class);

    private final BuilderState<P> _state;
    private final @Nullable Configurator<ScrollableComponentDelegate> _configurator;

    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param state The {@link BuilderState} modelling how the underlying component is build.
     */
    UIForScrollPane( BuilderState<P> state ) {
        this(state, null);
    }

    /**
     * {@link UIForAnySwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param state The {@link BuilderState} modelling how the underlying component is build.
     * @param configurator A {@link Configurator} that can be used to configure how the content
     *                     of the {@link JScrollPane} relates to the {@link JScrollPane} itself.
     */
    UIForScrollPane( BuilderState<P> state, @Nullable Configurator<ScrollableComponentDelegate> configurator ) {
        Objects.requireNonNull(state);
        _state = state.withMutator(thisComponent -> {
           if ( !(thisComponent instanceof UI.ScrollPane) && !(thisComponent instanceof JScrollPanels) )
               thisComponent.addMouseWheelListener(new NestedJScrollPanelScrollCorrection(thisComponent));
        });
        _configurator = configurator;
    }

    @Override
    protected BuilderState<P> _state() {
        return _state;
    }
    
    @Override
    protected UIForScrollPane<P> _newBuilderWithState( BuilderState<P> newState ) {
        return new UIForScrollPane<>(newState, _configurator);
    }

    /**
     *  We override this method to wrap the added component in a {@link ScrollableBox} instance
     *  in case a {@link Configurator} for {@link ScrollableComponentDelegate} instances was provided.
     *  We then use the continuously supplied {@link ScrollableComponentDelegate} objects
     *  to satisfy the needs of the {@link Scrollable} implementation of the {@link ScrollableBox}. <br>
     *  <b>
     *      This way the user can take advantage of the Scrollable
     *      interface without the need for a custom implementation
     *  </b>
     *
     * @param thisComponent  The component which is wrapped by this builder.
     * @param addedComponent A component instance which ought to be added to the wrapped component type.
     * @param constraints    The layout constraint which ought to be used to add the component to the wrapped component type.
     */
    @Override
    protected void _addComponentTo( P thisComponent, JComponent addedComponent, @Nullable AddConstraint constraints ) {
        if ( _configurator != null ) {
            if ( addedComponent instanceof Scrollable ) {
                log.warn(
                    "Trying to add a 'Scrollable' component to a declarative scroll pane UI which is already " +
                    "configured with a SwingTree based Scrollable through the 'UI.scrollPane(Configurator)' method. " +
                    "The provided component of type '"+addedComponent.getClass().getName()+"' is most likely not intended to be used this way.",
                    new Throwable()
                );
            }
            ScrollableBox wrapper = new ScrollableBox(thisComponent, addedComponent, _configurator);
            if ( constraints != null ) {
                wrapper.add(addedComponent, constraints.toConstraintForLayoutManager());
            } else {
                wrapper.add(addedComponent, "grow");
                /*
                    If we do not use a Scrollable panel and add the component directly
                    it will be placed and sized to fill the viewport by default.
                    But when using a Scrollable wrapper, we have an indirection which causes the
                    content to NOT fill out the viewport.

                    This "grow" keyword ensures that MigLayout produces a layout
                    that mimics what is expected...
                */
            }
            super._addComponentTo(thisComponent, wrapper, null);
        }
        else
            super._addComponentTo(thisComponent, addedComponent, constraints);
    }

    /**
     *  A simple internal wrapper type for {@link JComponent} instances that are to be used as the content
     *  of a {@link JScrollPane} and that should have a special scroll behaviour defined
     *  by a {@link ScrollableComponentDelegate} instance whose configuration is
     *  delivered to the scroll pane through this class implementing the {@link Scrollable} interface.
     */
    private static class ScrollableBox extends ThinDelegationBox implements Scrollable
    {
        private final JScrollPane _parent;
        private final Configurator<ScrollableComponentDelegate> _configurator;

        ScrollableBox( JScrollPane parent, JComponent child, Configurator<ScrollableComponentDelegate> configurator ) {
            super(child);
            _parent       = parent;
            _configurator = configurator;
        }

        private ScrollableComponentDelegate _createNewScrollableConf() {
            ScrollableComponentDelegate delegate = ScrollableComponentDelegate.of(
                                                            _parent, _child,
                                                            Size.of(_child.getPreferredSize())
                                                       );
            try {
                delegate = _configurator.configure(delegate);
            } catch ( Exception e ) {
                log.error(SwingTree.get().logMarker(), "Error while configuring scrollable component.", e);
            }
            return delegate;
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            ScrollableComponentDelegate delegate = _createNewScrollableConf();
            try {
                return delegate.preferredSize().toDimension();
            } catch ( Exception e ) {
                log.error(SwingTree.get().logMarker(), "Error while calculating preferred size for scrollable component.", e);
                return new Dimension(0, 0);
            }
        }

        @Override
        public int getScrollableUnitIncrement( java.awt.@Nullable Rectangle visibleRect, int orientation, int direction ) {
            ScrollableComponentDelegate delegate = _createNewScrollableConf();
            try {
                Bounds bounds = Bounds.none();
                if ( visibleRect != null )
                    bounds = Bounds.of(visibleRect);
                UI.Align align = (orientation == SwingConstants.VERTICAL ? UI.Align.VERTICAL : UI.Align.HORIZONTAL);
                return delegate.unitIncrement(bounds, align, direction);
            } catch ( Exception e ) {
                log.error(SwingTree.get().logMarker(), "Error while calculating unit increment for scrollable component.", e);
                return 0;
            }
        }

        @Override
        public int getScrollableBlockIncrement( java.awt.@Nullable Rectangle visibleRect, int orientation, int direction ) {
            ScrollableComponentDelegate delegate = _createNewScrollableConf();
            try {
                Bounds bounds = Bounds.none();
                if ( visibleRect != null )
                    bounds = Bounds.of(visibleRect);
                UI.Align align = (orientation == SwingConstants.VERTICAL ? UI.Align.VERTICAL : UI.Align.HORIZONTAL);
                return delegate.blockIncrement(bounds, align, direction);
            } catch ( Exception e ) {
                log.error(SwingTree.get().logMarker(), "Error while calculating block increment for scrollable component.", e);
                return 0;
            }
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            try {
                ScrollableComponentDelegate delegate = _createNewScrollableConf();
                return delegate.fitWidth();
            } catch ( Exception e ) {
                log.error(SwingTree.get().logMarker(), "Error while calculating fit width for scrollable component.", e);
                return false;
            }
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            try {
                ScrollableComponentDelegate delegate = _createNewScrollableConf();
                return delegate.fitHeight();
            } catch ( Exception e ) {
                log.error(SwingTree.get().logMarker(), "Error while calculating fit height for scrollable component.", e);
                return false;
            }
        }
    }

}

package swingtree;

import net.miginfocom.swing.MigLayout;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.api.Configurator;
import swingtree.components.JScrollPanels;
import swingtree.components.listener.NestedJScrollPanelScrollCorrection;
import swingtree.layout.Bounds;
import swingtree.layout.Size;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JScrollPane} instances. <br>
 *  Use {@link UI#scrollPane()} or {@link UI#scrollPane(Configurator)} to create a new instance
 *  of this builder type.
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
    protected void _addComponentTo( P thisComponent, JComponent addedComponent, @Nullable Object constraints ) {
        if ( _configurator != null ) {
            ScrollableBox wrapper = new ScrollableBox(thisComponent, addedComponent, _configurator);
            if ( constraints != null ) {
                wrapper.add(addedComponent, constraints);
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
    private static class ScrollableBox extends UI.Box implements Scrollable
    {
        private final JScrollPane _parent;
        private final JComponent  _child;
        private final Configurator<ScrollableComponentDelegate> _configurator;

        ScrollableBox( JScrollPane parent, JComponent child, Configurator<ScrollableComponentDelegate> configurator ) {
            setLayout(new MigLayout("fill, ins 0, hidemode 2, gap 0"));
            _parent       = parent;
            _child        = child;
            _configurator = configurator;
        }

        private ScrollableComponentDelegate _createNewScrollableConf() {
            int averageBlockIncrement  = 10;
            int averageUnitIncrement   = 10;
            try {
                int verticalBlockIncrement   = _parent.getVerticalScrollBar().getBlockIncrement();
                int horizontalBlockIncrement = _parent.getHorizontalScrollBar().getBlockIncrement();
                averageBlockIncrement = (verticalBlockIncrement + horizontalBlockIncrement) / 2;
            } catch ( Exception e ) {
                log.error("Error while calculating average block increment for scrollable component.", e);
            }
            try {
                int verticalUnitIncrement   = _parent.getVerticalScrollBar().getUnitIncrement();
                int horizontalUnitIncrement = _parent.getHorizontalScrollBar().getUnitIncrement();
                averageUnitIncrement = (verticalUnitIncrement + horizontalUnitIncrement) / 2;
            } catch ( Exception e ) {
                log.error("Error while calculating average unit increment for scrollable component.", e);
            }
            ScrollableComponentDelegate delegate = ScrollableComponentDelegate.of(
                                                            _parent, _child,
                                                            Size.of(_child.getPreferredSize()),
                                                            averageUnitIncrement,
                                                            averageBlockIncrement
                                                       );
            try {
                delegate = _configurator.configure(delegate);
            } catch ( Exception e ) {
                log.error("Error while configuring scrollable component.", e);
            }
            return delegate;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension childSize = _child.getPreferredSize();
            Dimension selfSize = this.getSize();
            if ( !Objects.equals(childSize, selfSize) ) {
                this.setSize(childSize);
            }
            return childSize;
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension childSize = _child.getMinimumSize();
            Dimension selfSize = this.getSize();
            if ( !Objects.equals(childSize, selfSize) ) {
                this.setSize(childSize);
            }
            return childSize;
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension childSize = _child.getMaximumSize();
            Dimension selfSize = this.getSize();
            if ( !Objects.equals(childSize, selfSize) ) {
                this.setSize(childSize);
            }
            return childSize;
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            ScrollableComponentDelegate delegate = _createNewScrollableConf();
            try {
                return delegate.preferredSize().toDimension();
            } catch ( Exception e ) {
                log.error("Error while calculating preferred size for scrollable component.", e);
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
                log.error("Error while calculating unit increment for scrollable component.", e);
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
                log.error("Error while calculating block increment for scrollable component.", e);
                return 0;
            }
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            try {
                ScrollableComponentDelegate delegate = _createNewScrollableConf();
                return delegate.fitWidth();
            } catch ( Exception e ) {
                log.error("Error while calculating fit width for scrollable component.", e);
                return false;
            }
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            try {
                ScrollableComponentDelegate delegate = _createNewScrollableConf();
                return delegate.fitHeight();
            } catch ( Exception e ) {
                log.error("Error while calculating fit height for scrollable component.", e);
                return false;
            }
        }
    }

}

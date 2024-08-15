package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.api.Configurator;
import swingtree.components.JScrollPanels;
import swingtree.components.listener.NestedJScrollPanelScrollCorrection;
import swingtree.layout.Bounds;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.util.Objects;

/**
 *  A SwingTree builder node designed for configuring {@link JScrollPane} instances.
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
        return new UIForScrollPane<>(newState);
    }

    @Override
    protected void _addComponentTo( P thisComponent, JComponent addedComponent, @Nullable Object constraints ) {
        if ( _configurator != null ) {
            addedComponent = new ScrollableBox(addedComponent, _configurator);
        }
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
        private final JComponent _child;
        private final Configurator<ScrollableComponentDelegate> _configurator;

        ScrollableBox( JComponent child, Configurator<ScrollableComponentDelegate> configurator ) {
            _child    = child;
            _configurator = configurator;
        }

        private ScrollableComponentDelegate _getConf() {
            ScrollableComponentDelegate delegate = ScrollableComponentDelegate.none();
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
            ScrollableComponentDelegate delegate = _getConf();
            try {
                return delegate.preferredSize().toDimension();
            } catch ( Exception e ) {
                log.error("Error while calculating preferred size for scrollable component.", e);
                return new Dimension(0, 0);
            }
        }

        @Override
        public int getScrollableUnitIncrement( java.awt.Rectangle visibleRect, int orientation, int direction ) {
            ScrollableComponentDelegate delegate = _getConf();
            try {
                Bounds bounds = Bounds.of(visibleRect);
                UI.Align align = (orientation == SwingConstants.VERTICAL ? UI.Align.VERTICAL : UI.Align.HORIZONTAL);
                return delegate.unitIncrement(bounds, align, direction);
            } catch ( Exception e ) {
                log.error("Error while calculating unit increment for scrollable component.", e);
                return 0;
            }
        }

        @Override
        public int getScrollableBlockIncrement( java.awt.Rectangle visibleRect, int orientation, int direction ) {
            ScrollableComponentDelegate delegate = _getConf();
            try {
                Bounds bounds = Bounds.of(visibleRect);
                UI.Align align = (orientation == SwingConstants.VERTICAL ? UI.Align.VERTICAL : UI.Align.HORIZONTAL);
                return delegate.blockIncrement(bounds, align, direction);
            } catch ( Exception e ) {
                log.error("Error while calculating block increment for scrollable component.", e);
                return 0;
            }
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            ScrollableComponentDelegate delegate = _getConf();
            try {
                return delegate.fitWidth();
            } catch ( Exception e ) {
                log.error("Error while calculating fit width for scrollable component.", e);
                return false;
            }
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            ScrollableComponentDelegate delegate = _getConf();
            try {
                return delegate.fitHeight();
            } catch ( Exception e ) {
                log.error("Error while calculating fit height for scrollable component.", e);
                return false;
            }
        }
    }
}

package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.api.Configurator;
import swingtree.api.ScrollIncrementSupplier;
import swingtree.layout.Bounds;
import swingtree.layout.Size;

import javax.swing.*;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * This class is an immutable builder which defines the {@link javax.swing.Scrollable} behavior of a component
 * within a {@link JScrollPane}.
 * So it is in essence a config object that provides information to a scrolling container
 * like {@link JScrollPane}.
 * <p>
 * Instances of this class are exposed to the {@link swingtree.api.Configurator} lambda
 * of the {@link UI#scrollPane(Configurator)} factory method, where you can configure the scrollable behavior
 * according to your needs. <br>
 * This includes setting the preferred size, unit increment, block increment, and whether the component should
 * fit the width or height of the viewport. <br>
 * Here an example demonstrating how the API of this class
 * is typically used:
 * <pre>{@code
 * UI.panel()
 * .withBorderTitled("Scrollable Panel")
 * .add(
 *     UI.scrollPane(conf -> conf
 *         .prefSize(400, 300)
 *         .unitIncrement(20)
 *         .blockIncrement(50)
 *         .fitWidth(true)
 *         .fitHeight(false)
 *     )
 * )
 * }</pre>
 * <p>
 * In most cases, the supplied {@link Configurator} will be called
 * for every call to a method of the underlying {@link javax.swing.Scrollable}
 * component implementation, so the settings you provide can
 * also change dynamically based on the context captured by the lambda.<br>
 * <b>
 *     This however is NOT true in case of the scroll pane wrapping
 *     a {@link JTable} for which the configurator will be called once initially.
 * </b><br>
 * <i>(This is because a table expects to be tightly wrapped by the scroll pane in order to
 * function properly, and so we cannot install a delegation mechanism for you...)</i><br>
 * <p>
 * Also note that this configuration object exposes some additional context
 * information you may find useful when defining its properties like {@link #fitWidth(boolean)},
 * {@link #fitHeight(boolean)}, {@link #unitIncrement(int)}, and so on...<br>
 * Like for example the current {@link #view()}, which implements the {@link javax.swing.Scrollable}
 * and wraps your {@link #content} component. You can also access the {@link #viewport()} as
 * well as the overarching {@link #scrollPane()} overall!<br>
 * Again, the configurator you pass to {@link UI#scrollPane(Configurator)} will be
 * called eagerly (except for tables), so everything you define in there will be completely dynamic,
 * which means your scroll behaviour can dynamically react to the involved components.
 */
public final class ScrollableComponentDelegate
{
    private static final Logger log = LoggerFactory.getLogger(ScrollableComponentDelegate.class);

    /**
     *  Builds a delegate whose defaults are all deferred until they are actually read.
     *  <p>
     *  This matters because a {@link Scrollable} implementation backed by a
     *  {@link Configurator} has to build a fresh delegate for every single question the
     *  scroll pane asks it, and Swing asks several of those per layout pass. Almost every
     *  one of those questions concerns exactly one of the values below, and two of them
     *  (the preferred size and the two fitting flags) can only be answered by measuring a
     *  whole component tree. Computing all of them up front therefore meant measuring
     *  repeatedly to then throw the result away -- once because the caller only wanted a
     *  scroll increment, and once more for every default the configurator overrides
     *  anyway. Deferring them means each one is computed only if it survives configuration
     *  <i>and</i> is asked for, and then only once.
     *
     * @param scrollPane    The {@link JScrollPane} in which the content component is placed.
     * @param content       The user provided content component placed inside the scroll pane.
     * @param preferredSize Supplies the default preferred viewport size when it is first read.
     * @return A new {@link ScrollableComponentDelegate} with lazily computed defaults.
     */
    static ScrollableComponentDelegate of(
        JScrollPane scrollPane,
        JComponent content,
        Supplier<Size> preferredSize
    ) {
        Component view     = scrollPane.getViewport().getView();
        JViewport viewport = scrollPane.getViewport();

        Lazy<Boolean> fitWidth ;
        Lazy<Boolean> fitHeight;
        ScrollIncrementSupplier unitIncrementSupplier;
        ScrollIncrementSupplier blockIncrementSupplier;
        if ( content instanceof Scrollable) {
            Scrollable scrollable = (Scrollable)content;
            fitWidth  = Lazy.from(scrollable::getScrollableTracksViewportWidth);
            fitHeight = Lazy.from(scrollable::getScrollableTracksViewportHeight);
            unitIncrementSupplier = (a,b,c) -> {
                int orientation = b.isHorizontal() ? SwingConstants.HORIZONTAL : SwingConstants.VERTICAL;
                return scrollable.getScrollableUnitIncrement(a.toRectangle(),orientation,c);
            };
            blockIncrementSupplier = (a,b,c) -> {
                int orientation = b.isHorizontal() ? SwingConstants.HORIZONTAL : SwingConstants.VERTICAL;
                return scrollable.getScrollableBlockIncrement(a.toRectangle(),orientation,c);
            };
        } else {
            int averageBlockIncrement  = 10;
            int averageUnitIncrement   = 10;
            try {
                int verticalBlockIncrement   = scrollPane.getVerticalScrollBar().getBlockIncrement();
                int horizontalBlockIncrement = scrollPane.getHorizontalScrollBar().getBlockIncrement();
                averageBlockIncrement = (verticalBlockIncrement + horizontalBlockIncrement) / 2;
            } catch ( Exception e ) {
                log.error(SwingTree.get().logMarker(), "Error while calculating average block increment for scrollable component.", e);
            }
            try {
                int verticalUnitIncrement   = scrollPane.getVerticalScrollBar().getUnitIncrement();
                int horizontalUnitIncrement = scrollPane.getHorizontalScrollBar().getUnitIncrement();
                averageUnitIncrement = (verticalUnitIncrement + horizontalUnitIncrement) / 2;
            } catch ( Exception e ) {
                log.error(SwingTree.get().logMarker(), "Error while calculating average unit increment for scrollable component.", e);
            }
            // Note: a view's preferred size is not necessarily cached (a JComponent recomputes it on every call),
            // and depending on what sits behind it this single preferred size query can be expensive!
            Lazy<Dimension> viewPreferredSize = Lazy.from(view::getPreferredSize);
            fitWidth  = Lazy.from(() -> viewport.getWidth()  > viewPreferredSize.get().width );
            fitHeight = Lazy.from(() -> viewport.getHeight() > viewPreferredSize.get().height);
            int unitIncrement  = averageUnitIncrement;
            int blockIncrement = averageBlockIncrement;
            unitIncrementSupplier = (a,b,c) -> unitIncrement;
            blockIncrementSupplier = (a,b,c) -> blockIncrement;
        }
        return new ScrollableComponentDelegate(
                    scrollPane, content, view, Lazy.from(preferredSize),
                    unitIncrementSupplier, blockIncrementSupplier,
                    fitWidth, fitHeight
                );
    }

    /**
     *  An internal factory which constructs a {@link ScrollableComponentDelegate}
     *  from a set of explicit default values. This is useful for components
     *  (like the internal panel of {@link swingtree.components.JScrollPanels})
     *  which already implement {@link Scrollable} but want to expose a
     *  {@link swingtree.api.Configurator} based customization mechanism without
     *  re-entering their own {@link Scrollable} methods to compute the defaults.
     *
     * @param scrollPane     The {@link JScrollPane} in which the content component is placed.
     * @param content        The user provided content component placed inside the scroll pane.
     * @param preferredSize  The preferred viewport size to expose as the default.
     * @param unitIncrement  The default {@link ScrollIncrementSupplier} for unit increments.
     * @param blockIncrement The default {@link ScrollIncrementSupplier} for block increments.
     * @param fitWidth       Whether the viewport should force the content's width to match its own.
     * @param fitHeight      Whether the viewport should force the content's height to match its own.
     * @return A new {@link ScrollableComponentDelegate} populated with the supplied defaults.
     */
    public static ScrollableComponentDelegate of(
        JScrollPane             scrollPane,
        JComponent              content,
        Size                    preferredSize,
        ScrollIncrementSupplier unitIncrement,
        ScrollIncrementSupplier blockIncrement,
        boolean                 fitWidth,
        boolean                 fitHeight
    ) {
        Objects.requireNonNull(scrollPane);
        Objects.requireNonNull(content);
        Objects.requireNonNull(preferredSize);
        Objects.requireNonNull(unitIncrement);
        Objects.requireNonNull(blockIncrement);
        Component view = scrollPane.getViewport().getView();
        return new ScrollableComponentDelegate(
                    scrollPane, content, view, Lazy.of(preferredSize),
                    unitIncrement, blockIncrement, Lazy.of(fitWidth), Lazy.of(fitHeight)
                );
    }

    private final JScrollPane             _scrollPane;
    private final JComponent              _content;
    private final Component               _view;
    private final Lazy<Size>              _preferredSize;
    private final ScrollIncrementSupplier _unitIncrement;
    private final ScrollIncrementSupplier _blockIncrement;
    private final Lazy<Boolean>           _fitWidth;
    private final Lazy<Boolean>           _fitHeight;


    private ScrollableComponentDelegate(
        JScrollPane             scrollPane,
        JComponent              content,
        Component               view,
        Lazy<Size>              preferredSize,
        ScrollIncrementSupplier unitIncrement,
        ScrollIncrementSupplier blockIncrement,
        Lazy<Boolean>           fitWidth,
        Lazy<Boolean>           fitHeight
    ) {
        _scrollPane     = scrollPane;
        _content        = content;
        _view           = view;
        _preferredSize  = preferredSize;
        _unitIncrement  = unitIncrement;
        _blockIncrement = blockIncrement;
        _fitWidth       = fitWidth;
        _fitHeight      = fitHeight;
    }

    /**
     * Creates an updated scrollable config with the
     * preferred size of the viewport for a view component.
     * For example, the preferred size of a <code>JList</code> component
     * is the size required to accommodate all the cells in its list.
     * However, the value of <code>preferredScrollableViewportSize</code>
     * is the size required for <code>JList.getVisibleRowCount</code> rows.
     * A component without any properties that would affect the viewport
     * size should just return <code>getPreferredSize</code> here.
     *
     * @param width  The preferred width of a <code>JViewport</code> whose view a
     *               <code>Scrollable</code> configured by this config object.
     * @param height The preferred height of a <code>JViewport</code> whose view a
     *               <code>Scrollable</code> configured by this config object.
     * @return A new instance of {@link ScrollableComponentDelegate} with the updated preferred size.
     * @see JViewport#getPreferredSize
     */
    public ScrollableComponentDelegate prefSize( int width, int height ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _content, _view, Lazy.of(Size.of(width, height)), _unitIncrement, _blockIncrement, _fitWidth, _fitHeight
            );
    }

    /**
     * Configures the preferred <b>viewport size</b> which the view component may or may not
     * fill out depending on other configurations (see {@link #fitWidth(boolean)}, {@link #fitHeight(boolean)}).
     * Internally this translates to {@link Scrollable#getPreferredScrollableViewportSize()}.
     * For example, the preferred size of a <code>JList</code> component
     * is the size required to accommodate all the cells in its list.
     * However, the value of <code>preferredScrollableViewportSize</code>
     * is the size required for <code>JList.getVisibleRowCount</code> rows.
     * A component without any properties that would affect the viewport
     * size should just return <code>getPreferredSize</code> here.
     *
     * @param preferredSize The preferred size of the component.
     * @return A new instance of {@link ScrollableComponentDelegate} with the updated preferred size.
     * @see JViewport#getPreferredSize
     * @throws NullPointerException If the preferred size is null, use {@link Size#unknown()}
     *                              to indicate that the preferred size is unknown.
     */
    public ScrollableComponentDelegate prefSize( Size preferredSize ) {
        Objects.requireNonNull(preferredSize);
        if ( preferredSize.equals(Size.unknown()) )
            return this;
        return new ScrollableComponentDelegate(
                _scrollPane, _content, _view, Lazy.of(preferredSize), _unitIncrement, _blockIncrement, _fitWidth, _fitHeight
            );
    }

    /**
     * Creates an updated scrollable config with the specified unit increment.
     * The unit increment is the amount to scroll when the user requests a unit scroll.
     * For example, this could be the amount to scroll when the user presses the arrow keys.
     * Components that display logical rows or columns should compute
     * the scroll increment that will completely expose one new row
     * or column, depending on the value of orientation.  Ideally,
     * components should handle a partially exposed row or column by
     * returning the distance required to completely expose the item.
     * <p>
     * Scrolling containers, like JScrollPane, will use this increment value
     * each time the user requests a unit scroll.
     *
     * @param unitIncrement The unit increment value.
     * @return A new instance of {@link ScrollableComponentDelegate} with the updated unit increment.
     * @see JScrollBar#setUnitIncrement
     */
    public ScrollableComponentDelegate unitIncrement( int unitIncrement ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _content, _view, _preferredSize, (a, b, c)->unitIncrement, _blockIncrement, _fitWidth, _fitHeight
            );
    }

    /**
     * Creates an updated scrollable config with the specified unit increment supplier,
     * (see {@link ScrollIncrementSupplier}) which takes the visible rectangle,
     * orientation and direction as arguments and returns the unit increment for the given context. <br>
     * The unit increment is the amount to scroll when the user requests a unit scroll.
     * For example, this could be the amount to scroll when the user presses the arrow keys.
     * Components that display logical rows or columns should compute
     * the scroll increment that will completely expose one new row
     * or column, depending on the value of orientation.  Ideally,
     * components should handle a partially exposed row or column by
     * returning the distance required to completely expose the item.
     * <p>
     * Scrolling containers, like JScrollPane, will use this increment value
     * each time the user requests a unit scroll.
     *
     * @param unitIncrement A {@link ScrollIncrementSupplier} that returns the unit increment for the given context.
     * @return A new instance of {@link ScrollableComponentDelegate} with the updated unit increment supplier.
     * @see JScrollBar#setUnitIncrement
     */
    public ScrollableComponentDelegate unitIncrement( ScrollIncrementSupplier unitIncrement ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _content, _view, _preferredSize, unitIncrement, _blockIncrement, _fitWidth, _fitHeight
            );
    }

    /**
     * Creates an updated scrollable config with the specified block increment.
     * The block increment is the amount to scroll when the user requests a block scroll.
     * For example, this could be the amount to scroll when the user presses the page up or page down keys.
     * Components that display logical rows or columns should compute
     * the scroll increment that will completely expose one block
     * of rows or columns, depending on the value of orientation.
     * <p>
     * Scrolling containers, like JScrollPane, will use this increment value
     * each time the user requests a block scroll.
     *
     * @param blockIncrement The block increment value.
     * @return A new instance of {@link ScrollableComponentDelegate} with the updated block increment.
     * @see JScrollBar#setBlockIncrement
     */
    public ScrollableComponentDelegate blockIncrement( int blockIncrement ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _content, _view, _preferredSize, _unitIncrement, (a, b, c)->blockIncrement, _fitWidth, _fitHeight
            );
    }

    /**
     * Creates an updated scrollable config with the specified block increment supplier,
     * (see {@link ScrollIncrementSupplier}) which takes the visible rectangle,
     * orientation and direction as arguments and returns the block increment for the given context. <br>
     * The block increment is the amount to scroll when the user requests a block scroll.
     * For example, this could be the amount to scroll when the user presses the page up or page down keys.
     * Components that display logical rows or columns should compute
     * the scroll increment that will completely expose one block
     * of rows or columns, depending on the value of orientation.
     * <p>
     * Scrolling containers, like JScrollPane, will use this increment value
     * each time the user requests a block scroll.
     *
     * @param blockIncrement A {@link ScrollIncrementSupplier} that returns the block increment for the given context.
     * @return A new instance of {@link ScrollableComponentDelegate} with the updated block increment supplier.
     * @see JScrollBar#setBlockIncrement
     */
    public ScrollableComponentDelegate blockIncrement( ScrollIncrementSupplier blockIncrement ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _content, _view, _preferredSize, _unitIncrement, blockIncrement, _fitWidth, _fitHeight
            );
    }

    /**
     * Set this to true if a viewport should always force the width of this
     * <code>Scrollable</code> to match the width of the viewport.
     * For example a normal
     * text view that supported line wrapping would return true here, since it
     * would be undesirable for wrapped lines to disappear beyond the right
     * edge of the viewport.  Note that returning true for a Scrollable
     * whose ancestor is a JScrollPane effectively disables horizontal
     * scrolling.
     * <p>
     * Scrolling containers, like JViewport, will use this method each
     * time they are validated.
     *
     * @param fitWidth If true, the viewport will force the Scrollables width to match its own.
     * @return A new scroll config with the desired width fitting mode, which,
     *          if true, makes the viewport force the Scrollables width to match its own.
     */
    public ScrollableComponentDelegate fitWidth( boolean fitWidth ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _content, _view, _preferredSize, _unitIncrement, _blockIncrement, Lazy.of(fitWidth), _fitHeight
        );
    }

    /**
     * Set this to true if a viewport should always force the height of this
     * Scrollable to match the height of the viewport.  For example a
     * columnar text view that flowed text in left to right columns
     * could effectively disable vertical scrolling by returning
     * true here.
     * <p>
     * Scrolling containers, like JViewport, will use this method each
     * time they are validated.
     *
     * @param fitHeight If true, the viewport will force the Scrollables height to match its own.
     * @return A new scroll config with the desired width fitting mode, which,
     *          if true, makes a viewport force the Scrollables height to match its own.
     */
    public ScrollableComponentDelegate fitHeight( boolean fitHeight ) {
        return new ScrollableComponentDelegate(
                _scrollPane, _content, _view, _preferredSize, _unitIncrement, _blockIncrement, _fitWidth, Lazy.of(fitHeight)
        );
    }

    /**
     * Returns the scroll pane that contains the scrollable component
     * this configuration is for.
     *
     * @return The scroll pane that contains the scrollable component.
     */
    public JScrollPane scrollPane() {
        return _scrollPane;
    }

    /**
     * Returns the viewport of the scroll pane that contains the {@link javax.swing.Scrollable} component
     * this configuration is for.
     *
     * @return The viewport of the scroll pane that contains the scrollable component.
     */
    public JViewport viewport() {
        return _scrollPane.getViewport();
    }

    /**
     * Returns the user provided content component that is contained in the scroll pane
     * and which is wrapped by a view component implementing the {@link javax.swing.Scrollable}
     * interface configured by this {@link ScrollableComponentDelegate}.
     * <b>
     *     The content component is effectively the component supplied to
     *     the {@link UIForAnyScrollPane#add(Component[])} method.
     *     (Please note that a scroll pane can only ever hold a single content component)
     * </b>
     *
     * @return The content component that is contained within the scroll pane
     *         and wrapped by the {@link #view()} component.
     */
    public JComponent content() {
        return _content;
    }

    /**
     * Returns the view component implementing the {@link javax.swing.Scrollable} interface and which
     * is placed directly in the scroll panes {@link #viewport()} through {@link JViewport#setView(Component)}.<br>
     * This is the main UI component that is configured by this {@link ScrollableComponentDelegate}.
     *
     * @return The view component that is contained within the scroll pane.
     */
    public Component view() {
        return _view;
    }

    /**
     * Returns the preferred viewport size configured on this delegate.
     * This is the value reported to the scroll pane through
     * {@link Scrollable#getPreferredScrollableViewportSize()}.
     *
     * @return The configured preferred viewport size of the scrollable content.
     */
    public Size preferredSize() {
        return _preferredSize.get();
    }

    /**
     * Computes the unit increment value reported to the scroll pane through
     * {@link Scrollable#getScrollableUnitIncrement(java.awt.Rectangle, int, int)}.
     *
     * @param viewRectangle The view area visible within the viewport.
     * @param orientation Either {@link UI.Axis#VERTICAL} or {@link UI.Axis#HORIZONTAL}.
     * @param direction Less than zero to scroll up/left, greater than zero for down/right.
     * @return The configured unit increment for the given context.
     */
    public int unitIncrement(
        Bounds   viewRectangle,
        UI.Axis orientation,
        int      direction
    ) {
        return _unitIncrement.get(viewRectangle, orientation, direction);
    }

    /**
     * Computes the block increment value reported to the scroll pane through
     * {@link Scrollable#getScrollableBlockIncrement(java.awt.Rectangle, int, int)}.
     *
     * @param viewRectangle The view area visible within the viewport.
     * @param orientation Either {@link UI.Axis#VERTICAL} or {@link UI.Axis#HORIZONTAL}.
     * @param direction Less than zero to scroll up/left, greater than zero for down/right.
     * @return The configured block increment for the given context.
     */
    public int blockIncrement(
        Bounds   viewRectangle,
        UI.Axis orientation,
        int      direction
    ) {
        return _blockIncrement.get(viewRectangle, orientation, direction);
    }

    /**
     * Indicates whether the viewport should force the scrollable content's width
     * to match its own. This is the value reported to the scroll pane through
     * {@link Scrollable#getScrollableTracksViewportWidth()}.
     *
     * @return {@code true} if the viewport should force the width to match the viewport,
     *         {@code false} otherwise.
     */
    public boolean fitWidth() {
        return _fitWidth.get();
    }

    /**
     * Indicates whether the viewport should force the scrollable content's height
     * to match its own. This is the value reported to the scroll pane through
     * {@link Scrollable#getScrollableTracksViewportHeight()}.
     *
     * @return {@code true} if the viewport should force the height to match the viewport,
     *         {@code false} otherwise.
     */
    public boolean fitHeight() {
        return _fitHeight.get();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                "preferredSize="  + this.preferredSize() + ", " +
                "unitIncrement="  + _unitIncrement       + ", " +
                "blockIncrement=" + _blockIncrement      + ", " +
                "fitWidth="       + this.fitWidth()      + ", " +
                "fitHeight="      + this.fitHeight()     +
            "]";
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( !(obj instanceof ScrollableComponentDelegate) )
            return false;
        ScrollableComponentDelegate that = (ScrollableComponentDelegate) obj;
        return this.preferredSize().equals(that.preferredSize()) &&
               _unitIncrement.equals(that._unitIncrement) &&
               _blockIncrement.equals(that._blockIncrement) &&
               this.fitWidth()  == that.fitWidth() &&
               this.fitHeight() == that.fitHeight();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.preferredSize(), _unitIncrement, _blockIncrement, this.fitWidth(), this.fitHeight());
    }

    /**
     *  A value which is either known up front or else computed on demand, at most once.
     *  <p>
     *  The delegate hands the same instance to every copy of itself produced by its wither
     *  methods, which is what keeps a default from being computed twice when a configurator
     *  reads it and the scroll pane then reads it again. Note that the value a {@code Lazy}
     *  reports never changes; only the moment at which it is determined does. Like the
     *  Swing components it measures, it belongs to the event dispatch thread and is not
     *  safe to share across threads.
     *
     * @param <T> The type of the value held by this.
     */
    private static final class Lazy<T>
    {
        static <T> Lazy<T> of( T value ) {
            return new Lazy<>(null, Objects.requireNonNull(value));
        }

        static <T> Lazy<T> from( Supplier<T> supplier ) {
            return new Lazy<>(Objects.requireNonNull(supplier), null);
        }

        private @Nullable Supplier<T> _supplier;
        private @Nullable T           _value;

        private Lazy( @Nullable Supplier<T> supplier, @Nullable T value ) {
            _supplier = supplier;
            _value    = value;
        }

        T get() {
            T value = _value;
            if ( value == null ) {
                Supplier<T> supplier = Objects.requireNonNull(_supplier);
                value     = Objects.requireNonNull(supplier.get());
                _value    = value;
                _supplier = null; // Lets the supplier and everything it captured be collected.
            }
            return value;
        }
    }

}

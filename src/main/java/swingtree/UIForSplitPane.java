package swingtree;

import swingtree.api.mvvm.Val;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 *  A swing tree builder node for {@link JSplitPane} instances.
 */
public class UIForSplitPane<P extends JSplitPane> extends UIForAbstractSwing<UIForSplitPane<P>, P>
{
    /**
     * {@link UIForAbstractSwing} (sub)types always wrap
     * a single component for which they are responsible.
     *
     * @param component The {@link JComponent} type which will be wrapped by this builder node.
     */
    public UIForSplitPane( P component ) { super( component ); }

    /**
     * Sets the alignment of the split bar in the split pane.
     *
     * @param align The alignment of the split bar in the split pane.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if the provided alignment is null.
     */
    public final UIForSplitPane<P> with( UI.Align align ) {
        NullUtil.nullArgCheck( align, "split", UI.Align.class );
        getComponent().setOrientation( align.forSplitPane() );
        return this;
    }

    /**
     * Sets the alignment of the split bar in the split pane dynamically
     * based on the provided {@link Val} property which will be observed
     * by the split pane.
     *
     * @param align The alignment property of the split bar in the split pane.
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if the provided alignment is null or the property is allowed to wrap a null value.
     */
    public final UIForSplitPane<P> withAlignment( Val<UI.Align> align ) {
        NullUtil.nullArgCheck( align, "align", Val.class );
        NullUtil.nullPropertyCheck( align, "align", "Null is not a valid alignment." );
        _onShow( align, v -> with(align.orElseThrow()) );
        return this;
    }

    /**
     * Sets the location of the divider. This is passed off to the
     * look and feel implementation, and then listeners are notified. A value
     * less than 0 implies the divider should be reset to a value that
     * attempts to honor the preferred size of the left/top component.
     * After notifying the listeners, the last divider location is updated,
     * via <code>setLastDividerLocation</code>.
     *
     * @param location An int specifying a UI-specific value (typically a
     *        pixel count)
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSplitPane<P> withDividerAt( int location ) {
        getComponent().setDividerLocation(location);
        return this;
    }

    /**
     * Sets the location of the divider in the form of a property,
     * which can be dynamically update the divide.
     * This is passed off to the
     * look and feel implementation, and then listeners are notified. A value
     * less than 0 implies the divider should be reset to a value that
     * attempts to honor the preferred size of the left/top component.
     * After notifying the listeners, the last divider location is updated,
     * via <code>setLastDividerLocation</code>.
     *
     * @param location A property dynamically determining a UI-specific value (typically a
     *        pixel count)
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code location} is {@code null}.
     */
    public final UIForSplitPane<P> withDividerAt( Val<Integer> location ) {
        NullUtil.nullArgCheck( location, "location", Val.class );
        NullUtil.nullPropertyCheck( location, "location", "Null is not a valid divider location." );
        _onShow( location, this::withDividerAt );
        return this.withDividerAt( location.orElseThrow() );
    }

    /**
     * Sets the size of the divider.
     *
     * @param size An integer giving the size of the divider in pixels
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSplitPane<P> withDividerSize( int size ) {
        getComponent().setDividerSize(size);
        return this;
    }

    /**
     * Sets the size of the divider in the form of a property,
     * which can be dynamically update.
     *
     * @param size A property dynamically determining the size of the divider in pixels
     * @return This very instance, which enables builder-style method chaining.
     * @throws IllegalArgumentException if {@code size} is {@code null}.
     */
    public final UIForSplitPane<P> withDividerSize( Val<Integer> size ) {
        NullUtil.nullArgCheck( size, "size", Val.class );
        NullUtil.nullPropertyCheck( size, "size", "Null is not a valid divider size." );
        _onShow( size, this::withDividerSize );
        return this.withDividerSize( size.orElseThrow() );
    }

    private void _calculateDividerLocationFrom( double percentage ) {
        P p = getComponent();
        int loc = (int) (
                        p.getOrientation() == JSplitPane.HORIZONTAL_SPLIT
                            ? p.getWidth()  * percentage
                            : p.getHeight() * percentage
                    );
        p.setDividerLocation(loc);
    }

    /**
     * Sets the location of the divider based on a percentage value.
     * So if the split pane split is aligned horizontally, the divider
     * will be set to the percentage of the height of the split pane.
     * If the split pane is aligned vertically, the divider will be set
     * to the percentage of the width of the split pane.
     * This is ultimately passed off to the
     * look and feel implementation, and then listeners are notified. A value
     * less than 0 implies the divider should be reset to a value that
     * attempts to honor the preferred size of the left/top component.
     * After notifying the listeners, the last divider location is updated,
     * via <code>setLastDividerLocation</code>.
     *
     * @param percentage A double value between 0 and 1, representing the percentage of the split pane's
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSplitPane<P> withDivisionOf( double percentage ) {
        _calculateDividerLocationFrom(percentage);
        return this;
    }

    /**
     * Dynamically sets the location of the divider based on a percentage property.
     * So if the split pane split is aligned horizontally, the divider
     * will be set to the percentage of the height of the split pane.
     * If the split pane is aligned vertically, the divider will be set
     * to the percentage of the width of the split pane.
     * This is ultimately passed off to the
     * look and feel implementation, and then listeners are notified. A value
     * less than 0 implies the divider should be reset to a value that
     * attempts to honor the preferred size of the left/top component.
     * After notifying the listeners, the last divider location is updated,
     * via <code>setLastDividerLocation</code>.
     * <p>
     *     Note that the percentage is calculated based on the split pane's
     *     current size, so if the split pane is resized, the divider location
     *     will be recalculated.
     * <p>
     * @param percentage A property dynamically determining a double value between 0 and 1, representing the percentage of the split pane's
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSplitPane<P> withDivisionOf( Val<Double> percentage ) {
        NullUtil.nullArgCheck( percentage, "percentage", Val.class );
        NullUtil.nullPropertyCheck( percentage, "percentage", "Null is not a valid percentage." );
        _onShow( percentage, this::withDivisionOf );
        // Now we need to register a listener to the split pane's size, so that we can recalculate the divider location
        // when the split pane is resized:
        getComponent().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized( ComponentEvent e ) {
                _calculateDividerLocationFrom(percentage.orElseThrow());
            }
        });
        return this.withDivisionOf(percentage.orElseThrow());
    }

}

package com.globaltcad.swingtree;

import com.globaltcad.swingtree.api.UIAction;
import com.globaltcad.swingtree.api.mvvm.Val;

import javax.swing.*;

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
        return this;
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
        return this;
    }

}

package com.globaltcad.swingtree;

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
    public UIForSplitPane(P component) { super(component); }

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
    public final UIForSplitPane<P> withDividerAt(int location) {
        _component.setDividerLocation(location);
        return this;
    }

    /**
     * Sets the size of the divider.
     *
     * @param size An integer giving the size of the divider in pixels
     * @return This very instance, which enables builder-style method chaining.
     */
    public final UIForSplitPane<P> withDividerSize(int size) {
        _component.setDividerSize(size);
        return this;
    }

}

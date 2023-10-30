package swingtree;

import sprouts.Action;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Supplier;

/**
 *  A {@link JTabbedPane} delegate providing useful context information to various {@link sprouts.Action} listeners
 *  used by {@link UIForTabbedPane#onTabMouseClick(Action)}, {@link UIForTabbedPane#onTabMousePress(Action)} and
 *  {@link UIForTabbedPane#onTabMouseRelease(Action)}, for example.
 *  <br>
 *  Not only does this delegate provide access to the {@link JTabbedPane} component itself, but also to the
 *  {@link MouseEvent} that triggered the event and the index of the clicked tab.
 */
public final class TabDelegate extends ComponentMouseEventDelegate<JTabbedPane>
{
    private final int _clickedTab;

    public TabDelegate(
        JTabbedPane component,
        MouseEvent event,
        int clickedTab
    ) {
        super(component, event);
        _clickedTab = clickedTab;
    }

    /**
     * @return The index of the clicked tab.
     */
    public final int tabIndex() {
        return _clickedTab;
    }

    /**
     * @return The component of the clicked tab.
     */
    public final boolean tabIsSelected() {
        return tabIndex() == get().getSelectedIndex();
    }

    /**
     * @return The component of the clicked tab.
     */
    public final JComponent tabComponent() {
        Component found = get().getComponentAt(tabIndex());
        if ( found instanceof JComponent )
            return (JComponent) found;
        else
            throw new RuntimeException(
                    "The component at index " + tabIndex() + " is not a JComponent"
                );
    }

}

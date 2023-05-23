package swingtree;

import sprouts.Action;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 *  A {@link JTabbedPane} delegate providing useful context information to {@link sprouts.Action} listeners
 *  registered on the {@link UIForTabbedPane#onTabClick(Action)} method.
 *  <br>
 *  Not only does this delegate provide access to the {@link JTabbedPane} component itself, but also to the
 *  {@link MouseEvent} that triggered the event and the index of the clicked tab.
 */
public final class TabClickDelegate extends SimpleDelegate<JTabbedPane, MouseEvent>
{
    private final int _clickedTab;

    public TabClickDelegate(
        JTabbedPane component,
        MouseEvent event,
        Supplier<List<JComponent>> siblingSource,
        int clickedTab
    ) {
        super(component, event, siblingSource);
        _clickedTab = clickedTab;
    }

    /**
     * @return The index of the clicked tab.
     */
    public final int clickedTabIndex() {
        return _clickedTab;
    }

    /**
     * @return The component of the clicked tab.
     */
    public final boolean clickedTabIsSelected() {
        return clickedTabIndex() == get().getSelectedIndex();
    }

    /**
     * @return The optional component of the clicked tab.
     */
    public final Optional<JComponent> clickedTabComponent() {
        Component found = get().getComponentAt(clickedTabIndex());
        if ( found instanceof JComponent )
            return Optional.of((JComponent)found);
        else
            return Optional.empty();
    }

}

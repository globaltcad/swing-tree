package swingtree;

import java.awt.*;

/**
 *  A swing tree builder node for anything.
 *  Note: This does not support nesting.
 */
public class UIForAnything<T extends Component> extends AbstractBuilder<UIForAnything<T>, T>
{
    /**
     * Instances of the {@link UIForAnything} builder do not support the
     * "add" method as defined inside the {@link AbstractNestedBuilder}.
     *
     * @param component The component type which will be wrapped by this builder node.
     */
    protected UIForAnything(T component) { super(component); }
}

package swingtree;

import swingtree.components.JIcon;

/**
 * A {@link UIForAnySwing} subclass specifically designed for adding icons to your SwingTree.
 */
public class UIForIcon<I extends JIcon> extends UIForAnySwing<UIForIcon<I>, I>
{
    /**
     * Extensions of the {@link  UIForAnySwing} always wrap
     * a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    protected UIForIcon( I component ) {
        super(component);
    }
}

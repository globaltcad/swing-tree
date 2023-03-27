package swingtree;

import sprouts.Val;

import javax.swing.*;

/**
 *  A swing tree builder node for {@link JPopupMenu} instances.
 * 	<p>
 * 	<b>Take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class or other classes.</b>
 */
public class UIForPopup<P extends JPopupMenu> extends UIForAnySwing<UIForPopup<P>, P>
{
    protected UIForPopup( P component ) { super(component); }

    /**
     *  Determines if the border is painted or not.
     *
     * @param borderPainted True if the border is painted, false otherwise
     * @return This builder node, to qllow for method chaining.
     */
    public UIForPopup<P> borderIsPaintedIf( boolean borderPainted ) {
        getComponent().setBorderPainted(borderPainted);
        return this;
    }

    /**
     *  Determines if the border is painted or not
     *  based on the value of the given {@link Val}.
     *  If the value of the {@link Val} changes, the border will be painted or not.
     *
     * @param val A {@link Val} which will be used to determine if the border is painted or not.
     * @return This builder node, to qllow for method chaining.
     */
    public UIForPopup<P> borderIsPaintedIf( Val<Boolean> val ) {
        _onShow(val, v -> borderIsPaintedIf(v) );
        return borderIsPaintedIf( val.get() );
    }

    public UIForPopup<P> add(JMenuItem item) { return this.add(UI.of(item)); }

    public UIForPopup<P> add(JSeparator separator) { return this.add(UI.of(separator)); }

    public UIForPopup<P> add(JPanel panel) { return this.add(UI.of(panel)); }
}


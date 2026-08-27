package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPopupMenuUI;
import java.awt.Graphics;

/**
 *  The {@link JPopupMenu} UI delegate: the rounded, softly shadowed sheet the menu entries sit
 *  on, so that it visibly lifts off whatever lives underneath.
 */
public final class SwingTreePopupMenuUI
        extends    BasicPopupMenuUI
        implements SwingTreeStyledComponentUI<JPopupMenu>
{
    /** Called by Swing reflectively to make the delegate. */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreePopupMenuUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        SwingTreeLookAndFeel.installStyleOn(c);
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, g2 -> super.paint(g2, c));
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<JPopupMenu> style( ComponentStyleDelegate<JPopupMenu> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}

package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuUI;
import java.awt.Graphics;

/**
 *  The {@link JMenu} UI delegate, for a top level entry of a {@link javax.swing.JMenuBar}
 *  as well as for a submenu entry inside another popup.
 */
public final class SwingTreeMenuUI
        extends    BasicMenuUI
        implements SwingTreeStyledComponentUI<JMenu>
{
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeMenuUI(); }

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
    public ComponentStyleDelegate<JMenu> style( ComponentStyleDelegate<JMenu> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}

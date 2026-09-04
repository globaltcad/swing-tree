package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolBarUI;
import java.awt.Graphics;

/**
 *  The {@link JToolBar} UI delegate. An undocked tool bar is painted no differently, and the frame
 *  of the window it floats in belongs to whichever look and feel owns dialogs.
 */
public final class SwingTreeToolBarUI
        extends    BasicToolBarUI
        implements SwingTreeStyledComponentUI<JToolBar>
{
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeToolBarUI(); }

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
    public ComponentStyleDelegate<JToolBar> style( ComponentStyleDelegate<JToolBar> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}

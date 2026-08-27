package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolBarUI;
import java.awt.Graphics;

/**
 *  The {@link JToolBar} UI delegate: a flat strip carrying a row of commands. Floating mode -
 *  when the user undocks the bar - keeps the same painting; the floating window's own frame is
 *  drawn by whatever look and feel owns the dialog. The handle the bar is dragged by is painted
 *  by the configured symbol set, on the tool bar's content layer.
 */
public final class SwingTreeToolBarUI
        extends    BasicToolBarUI
        implements SwingTreeStyledComponentUI<JToolBar>
{
    /** Called by Swing reflectively to make the delegate. */
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

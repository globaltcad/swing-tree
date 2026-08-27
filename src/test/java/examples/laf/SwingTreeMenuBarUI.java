package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuBarUI;
import java.awt.Graphics;

/**
 *  The {@link JMenuBar} UI delegate: a flat horizontal strip separated from the window
 *  below it by a hairline. Its top-level entries are painted by {@link SwingTreeMenuUI}.
 */
public final class SwingTreeMenuBarUI
        extends    BasicMenuBarUI
        implements SwingTreeStyledComponentUI<JMenuBar>
{
    /** Called by Swing reflectively to make the delegate. */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeMenuBarUI(); }

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
    public ComponentStyleDelegate<JMenuBar> style( ComponentStyleDelegate<JMenuBar> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}

package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicViewportUI;
import java.awt.Graphics;

/**
 *  The {@link JViewport} UI delegate. A viewport carries no style group of its own, so
 *  {@link SwingTreeLookAndFeel.Surface#of(JComponent)} reads the one on the scroll pane around it.
 */
public final class SwingTreeViewportUI
        extends    BasicViewportUI
        implements SwingTreeStyledComponentUI<JViewport>
{
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeViewportUI(); }

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
    public ComponentStyleDelegate<JViewport> style( ComponentStyleDelegate<JViewport> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}

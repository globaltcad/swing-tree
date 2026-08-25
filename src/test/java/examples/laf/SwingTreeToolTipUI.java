package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;
import java.awt.Graphics;

/**
 *  The {@link JToolTip} UI delegate: a small rounded callout, distinct enough from the
 *  application's panels to read as a floating popover.
 */
public final class SwingTreeToolTipUI
        extends    BasicToolTipUI
        implements SwingTreeStyledComponentUI<JToolTip>
{
    /** Called by Swing reflectively to make the delegate. */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeToolTipUI(); }

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
    public ComponentStyleDelegate<JToolTip> style( ComponentStyleDelegate<JToolTip> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}

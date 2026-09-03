package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.AbstractButton;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Graphics;

/**
 *  The {@link AbstractButton} UI delegate, shared by {@link javax.swing.JButton} and
 *  {@link javax.swing.JToggleButton}.
 */
public final class SwingTreeButtonUI
        extends    BasicButtonUI
        implements SwingTreeStyledComponentUI<AbstractButton>
{
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeButtonUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        // Swing's own fill has to go only when a style rule paints one in its place, or it would
        // show through the rounded corners and the grain of that rule. A blank preset paints none.
        if ( SwingTreeLookAndFeel.styles(c.getClass()) ) {
            AbstractButton b = (AbstractButton) c;
            b.setContentAreaFilled(false);
            b.setBorderPainted(true);
            b.setRolloverEnabled(true);
            b.setFocusPainted(false);
        }
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
    public ComponentStyleDelegate<AbstractButton> style( ComponentStyleDelegate<AbstractButton> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}

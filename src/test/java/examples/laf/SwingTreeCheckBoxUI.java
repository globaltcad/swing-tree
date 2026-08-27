package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.AbstractButton;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicCheckBoxUI;
import java.awt.Graphics;

/**
 *  The {@link javax.swing.JCheckBox} UI delegate. Unlike a button, a check box reserves
 *  all of its visual identity for the glyph next to its label, which the configured
 *  symbol set draws through the {@code CheckBox.icon} default.
 */
public final class SwingTreeCheckBoxUI
        extends    BasicCheckBoxUI
        implements SwingTreeStyledComponentUI<AbstractButton>
{
    /** Called by Swing reflectively to make the delegate. */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeCheckBoxUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        // Suppressing Swing's own fill is right when a rule is going to paint one in its place -
        // it would otherwise double-paint underneath rounded corners and a noise overlay - and
        // wrong when nothing is, which is what a blank style preset means. The style engine is
        // installed either way: an application may still add a rule of its own.
        if ( SwingTreeLookAndFeel.styles(c.getClass()) ) {
            AbstractButton b = (AbstractButton) c;
            b.setContentAreaFilled(false);
            b.setRolloverEnabled(true);
            b.setFocusPainted(false);
        }
        SwingTreeLookAndFeel.installStyleOn(c);
    }

    @Override
    // Swing UI delegates only ever paint on the Event Dispatch Thread, so the inherited
    // synchronization is not needed here.
    @SuppressWarnings("UnsynchronizedOverridesSynchronized")
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

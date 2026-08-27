package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.AbstractButton;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRadioButtonUI;
import java.awt.Graphics;

/**
 *  The {@link javax.swing.JRadioButton} UI delegate. Mirrors {@link SwingTreeCheckBoxUI},
 *  but the glyph comes from the {@code RadioButton.icon} default.
 */
public final class SwingTreeRadioButtonUI
        extends    BasicRadioButtonUI
        implements SwingTreeStyledComponentUI<AbstractButton>
{
    /** Called by Swing reflectively to make the delegate. */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeRadioButtonUI(); }

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

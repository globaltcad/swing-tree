package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.AbstractButton;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 *  The {@link AbstractButton} UI delegate, shared by {@link javax.swing.JButton} and
 *  {@link javax.swing.JToggleButton}.
 */
public final class SwingTreeButtonUI
        extends    BasicButtonUI
        implements SwingTreeStyledComponentUI<AbstractButton>
{
    private final SwingTreeLookAndFeel.Theme _theme;

    SwingTreeButtonUI( SwingTreeLookAndFeel.Theme theme ) { _theme = theme; }

    public static ComponentUI createUI( JComponent c ) { return new SwingTreeButtonUI(SwingTreeLookAndFeel.installedTheme()); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        // Swing's own fill has to go only when a style rule paints one in its place, or it would
        // show through the rounded corners and the grain of that rule. A blank preset paints none.
        if ( _theme.styles(c.getClass()) ) {
            AbstractButton b = (AbstractButton) c;
            b.setContentAreaFilled(false);
            b.setBorderPainted(true);
            b.setRolloverEnabled(true);
            b.setFocusPainted(false);
        }
        _theme.installStyleOn(c);
    }

    /**
     *  Paints the style, then the label over it. The label is drawn outside the style engine's clip
     *  when this look and feel fills the button, because that clip follows the button's rounded
     *  corners and Java2D gives up subpixel text antialiasing under any clip that is not made of
     *  rectangles: every rounded button on the desktop would write its label in grey where the
     *  rest of the window writes in colour. With the content area left unfilled nothing the label
     *  painting draws reaches outside the button's body anyway.
     */
    @Override
    public void paint( Graphics g, JComponent c ) {
        if ( ((AbstractButton) c).isContentAreaFilled() )
            LafUtilities.paintStyled(g, c, g2 -> super.paint(g2, c));
        else
            LafUtilities.paintStyledUnderInheritedPainting(g, c, g2 -> super.paint(g2, c));
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    /** Basic embosses a disabled label out of a background this look and feel does not give a
     *  button, so the label is written in the ink the style rule chose instead. */
    @Override
    protected void paintText( Graphics g, AbstractButton b, Rectangle textRect, String text ) {
        if ( b.getModel().isEnabled() || !_theme.styles(b.getClass()) )
            super.paintText(g, b, textRect, text);
        else
            LafUtilities.paintDisabledText(g, b, textRect, text);
    }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public ComponentStyleDelegate<AbstractButton> style( ComponentStyleDelegate<AbstractButton> it ) throws Exception {
        return _theme.applyStyle(it);
    }
}

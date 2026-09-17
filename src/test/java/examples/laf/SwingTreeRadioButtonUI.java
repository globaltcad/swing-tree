package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.AbstractButton;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRadioButtonUI;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 *  The {@link javax.swing.JRadioButton} UI delegate. The dot itself is not painted here: it is the
 *  symbol set's glyph, installed as the {@code RadioButton.icon} default.
 */
public final class SwingTreeRadioButtonUI
        extends    BasicRadioButtonUI
        implements SwingTreeStyledComponentUI<AbstractButton>
{
    private final SwingTreeLookAndFeel.Theme _theme;

    SwingTreeRadioButtonUI( SwingTreeLookAndFeel.Theme theme ) { _theme = theme; }

    public static ComponentUI createUI( JComponent c ) { return new SwingTreeRadioButtonUI(SwingTreeLookAndFeel.installedTheme()); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        // A style rule shows the pointer, so the model has to track it. Installed rather than set,
        // so that the next look and feel can take it back and one the application set stands.
        LookAndFeel.installProperty(c, "rolloverEnabled", _theme.styles(c.getClass()));
        _theme.installStyleOn(c);
    }

    @Override
    // A UI delegate only ever paints on the event dispatch thread.
    @SuppressWarnings("UnsynchronizedOverridesSynchronized")
    public void paint( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, g2 -> super.paint(g2, c));
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

package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.AbstractButton;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRadioButtonUI;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link javax.swing.JRadioButton} UI delegate. Mirrors {@link SwingTreeCheckBoxUI},
 *  but the glyph comes from the {@code RadioButton.icon} default.
 *  <p>
 *  This class is an implementation detail of {@link SwingTreeLookAndFeel} and is only public
 *  because Swing instantiates a UI delegate reflectively through {@link javax.swing.UIDefaults}.
 *  Nothing about how it looks is decided here: the appearance comes from the configured style
 *  preset, and is reached through {@link SwingTreeLookAndFeel#applyStyle(ComponentStyleDelegate)}.
 */
public final class SwingTreeRadioButtonUI
        extends    BasicRadioButtonUI
        implements SwingTreeStyledComponentUI<AbstractButton>
{
    /** Called by Swing reflectively to obtain the UI delegate.
     *  @param c the component the delegate is created for
     *  @return a new delegate */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeRadioButtonUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        AbstractButton b = (AbstractButton) c;
        // The style engine paints the fill - Swing's own opaque fill would double-paint
        // underneath rounded corners and the noise overlay.
        b.setContentAreaFilled(false);
        b.setRolloverEnabled(true);
        b.setFocusPainted(false);
        ComponentExtension.from(c).gatherApplyAndInstallStyle(true);
    }

    @Override
    // Swing UI delegates only ever paint on the Event Dispatch Thread, so the inherited
    // synchronization is not needed here.
    @SuppressWarnings("UnsynchronizedOverridesSynchronized")
    public void paint( Graphics g, JComponent c ) {
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            LafPaint.applyAaHints((Graphics2D) g2);
            super.paint(g2, c);
        });
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

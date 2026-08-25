package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRadioButtonMenuItemUI;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JRadioButtonMenuItem} UI delegate: the same row as
 *  {@link SwingTreeMenuItemUI}, with the symbol set's radio glyph on the left to indicate
 *  membership in an exclusive group.
 *  <p>
 *  This class is an implementation detail of {@link SwingTreeLookAndFeel} and is only public
 *  because Swing instantiates a UI delegate reflectively through {@link javax.swing.UIDefaults}.
 *  Nothing about how it looks is decided here: the appearance comes from the configured style
 *  preset, and is reached through {@link SwingTreeLookAndFeel#applyStyle(ComponentStyleDelegate)}.
 */
public final class SwingTreeRadioButtonMenuItemUI
        extends    BasicRadioButtonMenuItemUI
        implements SwingTreeStyledComponentUI<JRadioButtonMenuItem>
{
    /** Called by Swing reflectively to obtain the UI delegate.
     *  @param c the component the delegate is created for
     *  @return a new delegate */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeRadioButtonMenuItemUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        SwingTreeLookAndFeel.installStyleOn(c);
    }

    @Override
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
    public ComponentStyleDelegate<JRadioButtonMenuItem> style( ComponentStyleDelegate<JRadioButtonMenuItem> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}

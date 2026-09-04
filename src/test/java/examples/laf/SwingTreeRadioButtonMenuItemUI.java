package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRadioButtonMenuItemUI;
import java.awt.Graphics;

/** The {@link JRadioButtonMenuItem} UI delegate. */
public final class SwingTreeRadioButtonMenuItemUI
        extends    BasicRadioButtonMenuItemUI
        implements SwingTreeStyledComponentUI<JRadioButtonMenuItem>
{
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeRadioButtonMenuItemUI(); }

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
    public ComponentStyleDelegate<JRadioButtonMenuItem> style( ComponentStyleDelegate<JRadioButtonMenuItem> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}

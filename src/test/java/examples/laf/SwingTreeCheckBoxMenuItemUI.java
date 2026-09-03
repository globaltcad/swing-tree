package examples.laf;

import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI;
import java.awt.Graphics;

/** The {@link JCheckBoxMenuItem} UI delegate. */
public final class SwingTreeCheckBoxMenuItemUI
        extends    BasicCheckBoxMenuItemUI
        implements SwingTreeStyledComponentUI<JCheckBoxMenuItem>
{
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeCheckBoxMenuItemUI(); }

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
    public ComponentStyleDelegate<JCheckBoxMenuItem> style( ComponentStyleDelegate<JCheckBoxMenuItem> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }
}

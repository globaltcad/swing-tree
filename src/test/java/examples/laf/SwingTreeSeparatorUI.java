package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSeparatorUI;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JSeparator} UI delegate: one hairline across the full length of the separator,
 *  horizontal or vertical depending on its orientation. The line is as thick as the configured
 *  symbol set asks for, scaled through {@link UI#scale(int)} so it stays crisp on HiDPI displays.
 */
public final class SwingTreeSeparatorUI
        extends    BasicSeparatorUI
        implements SwingTreeStyledComponentUI<JSeparator>
{
    /** Called by Swing reflectively to make the delegate. */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeSeparatorUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        SwingTreeLookAndFeel.installStyleOn(c);
    }

    @Override
    public void paint( Graphics g, JComponent c ) {
        LafUtilities.paintStyled(g, c, g2 -> {
            if ( SwingTreeLookAndFeel.drawsOwnChrome() )
                drawHairline(g2, (JSeparator) c);
            else
                super.paint(g2, c);
        });
    }

    @Override
    public void update( Graphics g, JComponent c ) { paint(g, c); }

    @Override
    public boolean canForwardPaintingToSwingTree() { return true; }

    @Override
    public Dimension getPreferredSize( JComponent c ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            return super.getPreferredSize(c);
        int thickness = thickness();
        return ((JSeparator) c).getOrientation() == SwingConstants.VERTICAL
                ? new Dimension(thickness, 0)
                : new Dimension(0, thickness);
    }

    @Override
    public ComponentStyleDelegate<JSeparator> style( ComponentStyleDelegate<JSeparator> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }

    private static int thickness() {
        return Math.max(1, UI.scale(SwingTreeLookAndFeel.symbols().separatorThickness()));
    }

    private static void drawHairline( Graphics2D g, JSeparator separator ) {
        int thickness = thickness();
        g.setColor(SwingTreeLookAndFeel.palette().borderSoft());
        if ( separator.getOrientation() == SwingConstants.VERTICAL )
            g.fillRect(0, 0, thickness, separator.getHeight());
        else
            g.fillRect(0, 0, separator.getWidth(), thickness);
    }
}

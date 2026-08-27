package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 *  The {@link JProgressBar} UI delegate. The trough is a style rule; the filled part is drawn on
 *  top by the configured symbol set. An indeterminate bar falls through to the inherited
 *  delegate's animated bouncing block.
 */
public final class SwingTreeProgressBarUI
        extends    BasicProgressBarUI
        implements SwingTreeStyledComponentUI<JProgressBar>
{
    /** Called by Swing reflectively to make the delegate. */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeProgressBarUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        SwingTreeLookAndFeel.installStyleOn(c);
    }

    @Override
    public Dimension getPreferredSize( JComponent c ) {
        Dimension d = super.getPreferredSize(c);
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            return d;
        int floor = UI.scale(SwingTreeLookAndFeel.symbols().progressBarThickness());
        if ( ((JProgressBar) c).getOrientation() == SwingConstants.HORIZONTAL )
            d.height = Math.max(d.height, floor);
        else
            d.width = Math.max(d.width, floor);
        return d;
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
    protected void paintDeterminate( Graphics g, JComponent c ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() ) { super.paintDeterminate(g, c); return; }
        JProgressBar bar   = (JProgressBar) c;
        double       ratio = fillRatio(bar);
        Graphics2D   g2    = (Graphics2D) g.create();
        try {
            SwingTreeLookAndFeel.symbols().paintProgressFill(
                    g2, SwingTreeLookAndFeel.palette(), bar.getWidth(), bar.getHeight(), ratio,
                    bar.getOrientation() == SwingConstants.HORIZONTAL, bar.isEnabled()
            );
        } finally {
            g2.dispose();
        }
        if ( bar.isStringPainted() )
            paintStringAtCentre((Graphics2D) g, bar, ratio);
    }

    @Override
    public ComponentStyleDelegate<JProgressBar> style( ComponentStyleDelegate<JProgressBar> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }

    private static double fillRatio( JProgressBar bar ) {
        int range = Math.max(1, bar.getMaximum() - bar.getMinimum());
        return Math.max(0, Math.min(1, (bar.getValue() - bar.getMinimum()) / (double) range));
    }

    /** Centres the bar's string, switching to the on-filled colour once the fill has reached it. */
    private static void paintStringAtCentre( Graphics2D g, JProgressBar bar, double ratio ) {
        String text = bar.getString();
        if ( text == null || text.isEmpty() )
            return;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(bar.getFont());
            SwingTreeLookAndFeel.Palette p = SwingTreeLookAndFeel.palette();
            Color colour = ratio > 0.45 ? p.onFilled() : p.text();
            g2.setColor(colour);
            FontMetrics metrics = g2.getFontMetrics();
            int x = (bar.getWidth()  - metrics.stringWidth(text)) / 2;
            int y = (bar.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(text, x, y);
        } finally {
            g2.dispose();
        }
    }
}

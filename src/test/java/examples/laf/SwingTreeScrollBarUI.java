package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 *  The {@link JScrollBar} UI delegate: a slim bar with no increment or decrement buttons. The
 *  thumb is drawn by the configured symbol set; the groove it slides along is the bar's own
 *  styled background - see {@link #paintTrack}.
 *  <p>
 *  The bar's thickness is deliberately <em>not</em> taken from the {@code ScrollBar.width}
 *  default: Swing's convention is that the value is in raw component pixels, which would be
 *  wrong on a HiDPI display. It is computed here instead, from the symbol set's developer-pixel
 *  constant, at every layout pass.
 */
public final class SwingTreeScrollBarUI
        extends    BasicScrollBarUI
        implements SwingTreeStyledComponentUI<JScrollBar>
{
    /** Called by Swing reflectively to make the delegate. */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeScrollBarUI(); }

    @Override
    public void installUI( JComponent c ) {
        super.installUI(c);
        SwingTreeLookAndFeel.installStyleOn(c);
    }

    @Override
    public Dimension getPreferredSize( JComponent c ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            return super.getPreferredSize(c);
        int t = UI.scale(SwingTreeLookAndFeel.symbols().scrollBarThickness());
        return scrollbar.getOrientation() == JScrollBar.VERTICAL
                ? new Dimension(t, t * 4)
                : new Dimension(t * 4, t);
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
    protected JButton createDecreaseButton( int orientation ) {
        return SwingTreeLookAndFeel.drawsOwnChrome() ? zeroButton() : super.createDecreaseButton(orientation);
    }

    @Override
    protected JButton createIncreaseButton( int orientation ) {
        return SwingTreeLookAndFeel.drawsOwnChrome() ? zeroButton() : super.createIncreaseButton(orientation);
    }

    /**
     *  The groove is the scroll bar's own styled background, which the style engine has already
     *  filled across the whole bar by the time the inherited delegate asks for a track. Filling it
     *  a second time here would push an antialiased round rectangle the height of the window
     *  through the rasterizer to arrive at exactly the colour that is already there - measured at
     *  2.5% of the event thread on the Linen showcase. A style rule is the place to say what the
     *  groove looks like.
     */
    @Override
    protected void paintTrack( Graphics g, JComponent c, Rectangle r ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            super.paintTrack(g, c, r);
    }

    @Override
    protected void paintThumb( Graphics g, JComponent c, Rectangle r ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() ) { super.paintThumb(g, c, r); return; }
        if ( r.width <= 0 || r.height <= 0 )
            return;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            SwingTreeLookAndFeel.symbols().paintScrollThumb(
                    g2, SwingTreeLookAndFeel.palette(), r, isThumbRollover() || isDragging
            );
        } finally {
            g2.dispose();
        }
    }

    @Override
    public ComponentStyleDelegate<JScrollBar> style( ComponentStyleDelegate<JScrollBar> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }

    /** A button that takes up no space and is never shown: Swing insists a scroll bar has two. */
    private static JButton zeroButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        button.setFocusable(false);
        button.setVisible(false);
        return button;
    }
}

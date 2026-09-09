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
import java.util.function.Supplier;

/**
 *  The {@link JScrollBar} UI delegate: a bar carrying a thumb the symbol set draws, and, when that
 *  set asks for them, a button at each end that scrolls a line at a time.
 *  <p>
 *  Its thickness is computed in {@link #getPreferredSize(JComponent)} rather than read from the
 *  {@code ScrollBar.width} default, because Swing reads that default as raw screen pixels and it
 *  would therefore stay the same width as the UI scale factor grows.
 */
public final class SwingTreeScrollBarUI
        extends    BasicScrollBarUI
        implements SwingTreeStyledComponentUI<JScrollBar>
{
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
        return endButton(orientation, false, () -> super.createDecreaseButton(orientation));
    }

    @Override
    protected JButton createIncreaseButton( int orientation ) {
        return endButton(orientation, true, () -> super.createIncreaseButton(orientation));
    }

    /** @return the button for one end of the bar: Swing's own when nothing here draws chrome, a
     *          stepper when the symbol set has them, and otherwise one taking up no space. */
    private JButton endButton( int orientation, boolean forward, Supplier<JButton> basic ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            return basic.get();
        if ( !SwingTreeLookAndFeel.symbols().scrollBarHasSteppers() )
            return zeroButton();
        boolean vertical = orientation == JScrollBar.VERTICAL;
        return new StepperButton(vertical ? ( forward ? LafUtilities.Direction.DOWN : LafUtilities.Direction.UP )
                                          : ( forward ? LafUtilities.Direction.RIGHT : LafUtilities.Direction.LEFT ));
    }

    /**
     *  The groove is the scroll bar's own background, which a style rule has already filled across
     *  the whole bar by the time this is called. Filling it again would run an antialiased round
     *  rectangle the height of the window through the rasterizer to arrive at the colour already
     *  there: 2.5% of the event thread on the Linen showcase.
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

    /** The button at one end of the bar, carrying the symbol set's stepper. */
    private static final class StepperButton extends ActuatorButton
    {
        private final LafUtilities.Direction _direction;

        StepperButton( LafUtilities.Direction direction ) { _direction = direction; }

        @Override public Dimension getPreferredSize() {
            int side = UI.scale(SwingTreeLookAndFeel.symbols().scrollBarThickness());
            return new Dimension(side, side);
        }

        @Override
        void paintActuator( Graphics2D g, Symbols symbols, SwingTreeLookAndFeel.Palette palette ) {
            symbols.paintScrollStepper(
                    g, palette, getWidth(), getHeight(), _direction,
                    isEnabled(), getModel().isRollover(), getModel().isPressed()
            );
        }
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

package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentStyleDelegate;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *  The {@link JSlider} UI delegate. The symbol set draws the groove, the filled part of it and
 *  the handle; this delegate places them and keeps room for the handle on both axes.
 */
public final class SwingTreeSliderUI
        extends    BasicSliderUI
        implements SwingTreeStyledComponentUI<JSlider>
{
    /** {@link BasicSliderUI} asks for a slider up front; {@link #installUI(JComponent)} supplies
     *  it instead, which is what the basic look and feel does too. */
    public SwingTreeSliderUI() { super(null); }

    public static ComponentUI createUI( JComponent c ) { return new SwingTreeSliderUI(); }

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
    public Dimension getPreferredHorizontalSize() {
        Dimension d = super.getPreferredHorizontalSize();
        d.height = Math.max(d.height, thickness());
        return d;
    }

    @Override
    public Dimension getPreferredVerticalSize() {
        Dimension d = super.getPreferredVerticalSize();
        d.width = Math.max(d.width, thickness());
        return d;
    }

    /**
     *  {@link BasicSliderUI#getPreferredSize(JComponent)} recomputes the extent across the slider
     *  from {@code trackRect + tickRect + labelRect} and drops the floor that
     *  {@link #getPreferredHorizontalSize()} and {@link #getPreferredVerticalSize()} put there,
     *  so the floor is applied a second time here.
     */
    @Override
    public Dimension getPreferredSize( JComponent c ) {
        Dimension d = super.getPreferredSize(c);
        int floor = thickness();
        if ( ((JSlider) c).getOrientation() == SwingConstants.HORIZONTAL )
            d.height = Math.max(d.height, floor);
        else
            d.width = Math.max(d.width, floor);
        return d;
    }

    @Override
    protected Dimension getThumbSize() {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            return super.getThumbSize();
        int side = UI.scale(SwingTreeLookAndFeel.symbols().sliderThumbDiameter());
        return new Dimension(side, side);
    }

    /** The handle shows focus by itself, so no focus rectangle is drawn around the slider -
     *  unless the symbol set draws no handle, and Swing's rectangle is the only sign there is. */
    @Override
    public void paintFocus( Graphics g ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            super.paintFocus(g);
    }

    @Override
    public void paintTrack( Graphics g ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() ) { super.paintTrack(g); return; }
        boolean    horizontal = slider.getOrientation() == SwingConstants.HORIZONTAL;
        int        centre     = horizontal
                                    ? thumbRect.x + thumbRect.width / 2
                                    : thumbRect.y + thumbRect.height / 2;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            SwingTreeLookAndFeel.symbols().paintSliderTrack(
                    g2, SwingTreeLookAndFeel.palette(), trackRect, centre,
                    horizontal, drawInverted(), slider.isEnabled()
            );
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void paintThumb( Graphics g ) {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() ) { super.paintThumb(g); return; }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            SwingTreeLookAndFeel.symbols().paintSliderThumb(
                    g2, SwingTreeLookAndFeel.palette(), thumbRect,
                    slider.isEnabled(), slider.isFocusOwner()
            );
        } finally {
            g2.dispose();
        }
    }

    @Override
    public ComponentStyleDelegate<JSlider> style( ComponentStyleDelegate<JSlider> it ) throws Exception {
        return SwingTreeLookAndFeel.applyStyle(it);
    }

    /** @return the smallest extent across the slider that still fits the handle, plus a margin. */
    private static int thickness() {
        if ( !SwingTreeLookAndFeel.drawsOwnChrome() )
            return 0;
        return UI.scale(SwingTreeLookAndFeel.symbols().sliderThumbDiameter() + 4);
    }
}

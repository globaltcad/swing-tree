package examples.laf;

import swingtree.UI;
import swingtree.api.laf.SwingTreeStyledComponentUI;
import swingtree.style.ComponentExtension;
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
 *  The {@link JSlider} UI delegate. The groove, the filled part of it and the handle are all
 *  drawn by the configured symbol set; this delegate only decides where they go and makes sure
 *  the slider is laid out with enough room for the handle on both axes.
 *  <p>
 *  This class is an implementation detail of {@link SwingTreeLookAndFeel} and is only public
 *  because Swing instantiates a UI delegate reflectively through {@link javax.swing.UIDefaults}.
 */
public final class SwingTreeSliderUI
        extends    BasicSliderUI
        implements SwingTreeStyledComponentUI<JSlider>
{
    /** Creates the delegate. {@link BasicSliderUI} wants a slider up front; it is installed
     *  through {@link #installUI(JComponent)} instead, exactly as the basic look and feel does. */
    public SwingTreeSliderUI() { super(null); }

    /** Called by Swing reflectively to obtain the UI delegate.
     *  @param c the component the delegate is created for
     *  @return a new delegate */
    public static ComponentUI createUI( JComponent c ) { return new SwingTreeSliderUI(); }

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
     *  {@link BasicSliderUI#getPreferredSize(JComponent)} recomputes the dimension perpendicular
     *  to the slider from {@code trackRect + tickRect + labelRect}, ignoring the floor set in
     *  {@link #getPreferredHorizontalSize()} and {@link #getPreferredVerticalSize()}. The floor
     *  is re-applied here so the slider always has room for the scaled handle on both axes.
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

    /** The handle itself indicates focus, so no separate focus rectangle is drawn - unless the
     *  symbol set has no handle of its own, in which case Swing's own indicator is the one there is. */
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

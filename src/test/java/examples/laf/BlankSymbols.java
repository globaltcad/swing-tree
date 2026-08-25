package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;

import swingtree.UI;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;

/**
 *  The symbol set with no opinions: it answers {@link #drawsItsOwnChrome()} with {@code false} and
 *  is never asked anything else.
 *  <p>
 *  Every call site checks that answer first and falls through to the painting and the sizing its
 *  inherited {@code Basic*UI} would do, and the look and feel installs none of the glyph icons. So
 *  a check box gets Swing's own check box, a scroll bar its own arrows and thumb, a tabbed pane its
 *  own tabs. Paired with {@link SwingTreeLookAndFeel.StylePreset#BLANK} the result is plain Swing
 *  with the SwingTree style engine wired into every component and nothing painted on top - the
 *  starting point for an application that wants to build its whole appearance itself.
 *  <p>
 *  Two glyphs are drawn anyway. A check box and a radio button are drawn through an icon rather
 *  than by their delegate, and {@code BasicIconFactory}'s versions of both are <b>empty stubs</b>,
 *  so falling through would leave a control that cannot be read - which is not what "no styling"
 *  means: a browser showing a page with no stylesheet still draws a real check box. Everything else
 *  here is unreachable, and returns nothing rather than throwing, so a lost guard leaves a control
 *  undecorated instead of taking the window down.
 */
final class BlankSymbols implements Symbols
{
    static final Symbols INSTANCE = new BlankSymbols();

    private BlankSymbols() {}

    @Override public boolean drawsItsOwnChrome() { return false; }

    /** The one metric a blank set still has to answer: the two glyphs below are drawn. */
    @Override public int checkGlyphSize()        { return 13; }
    @Override public int arrowGlyphSize()        { return 0; }
    @Override public int comboArrowButtonSize()  { return 0; }
    @Override public int spinnerButtonWidth()    { return 0; }
    @Override public int spinnerButtonHeight()   { return 0; }
    @Override public int sliderThumbDiameter()   { return 0; }
    @Override public int sliderTrackThickness()  { return 0; }
    @Override public int scrollBarThickness()    { return 0; }
    @Override public int splitDividerThickness() { return 0; }
    @Override public int progressBarThickness()  { return 0; }
    @Override public int separatorThickness()    { return 0; }
    @Override public int tableRowHeight()        { return 0; }
    @Override public int treeRowHeight()         { return 0; }
    @Override public int tabPaddingVertical()    { return 0; }
    @Override public int tabPaddingHorizontal()  { return 0; }
    @Override public int tabAreaGap()            { return 0; }

    @Override public void paintCheckGlyph(
        Graphics2D g, Palette p, int x, int y, int w, int h,
        boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
    ) {
        LafUtilities.antialiasShapes(g);
        g.setColor(enabled ? p.surfaceField() : p.surfaceDisabled());
        g.fillRect(x, y, w - 1, h - 1);
        g.setStroke(new BasicStroke(1f));
        g.setColor(enabled ? p.border() : p.borderSoft());
        g.drawRect(x, y, w - 1, h - 1);
        if ( !selected )
            return;
        g.setColor(enabled ? p.text() : p.textDisabled());
        g.setStroke(new BasicStroke(Math.max(1.5f, UI.scale(1.8f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(LafUtilities.tickShape(x, y, w, h));
    }

    @Override public void paintRadioGlyph(
        Graphics2D g, Palette p, int x, int y, int w, int h,
        boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
    ) {
        LafUtilities.antialiasShapes(g);
        g.setColor(enabled ? p.surfaceField() : p.surfaceDisabled());
        g.fill(new Ellipse2D.Float(x, y, w - 1, h - 1));
        g.setStroke(new BasicStroke(1f));
        g.setColor(enabled ? p.border() : p.borderSoft());
        g.draw(new Ellipse2D.Float(x, y, w - 1, h - 1));
        if ( !selected )
            return;
        float dot = UI.scale(4f);
        g.setColor(enabled ? p.text() : p.textDisabled());
        g.fill(new Ellipse2D.Float(x + dot, y + dot, w - 1 - 2 * dot, h - 1 - 2 * dot));
    }

    @Override public void paintDisclosure(
        Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
    ) {}

    @Override public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {}

    @Override public void paintComboArrow(
        Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
    ) {}

    @Override public void paintSpinnerArrow(
        Graphics2D g, Palette p, int w, int h, boolean up,
        boolean enabled, boolean rollover, boolean pressed
    ) {}

    @Override public void paintSliderTrack(
        Graphics2D g, Palette p, Rectangle track, int thumbCentre,
        boolean horizontal, boolean inverted, boolean enabled
    ) {}

    @Override public void paintSliderThumb( Graphics2D g, Palette p, Rectangle thumb, boolean enabled, boolean focused ) {}

    @Override public void paintScrollThumb( Graphics2D g, Palette p, Rectangle thumb, boolean active ) {}

    @Override public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {}

    @Override public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {}

    @Override public void paintProgressFill(
        Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
    ) {}

    @Override public void paintTabSurface(
        Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
    ) {}

    @Override public void paintTabAccent(
        Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
    ) {}
}

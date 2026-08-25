package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 *  Symbols for {@link SwingTreeLookAndFeel.StylePreset#POLYMORPHIC}: not a set of its own, but a
 *  choice between three of the others, remade on every call from the palette in force.
 *  <p>
 *  The preset it belongs to reads its whole idiom off the palette (see {@link Mood}), and the
 *  small geometry has to follow, or a theme that separates its surfaces with light would end up
 *  with flat glyphs drawn onto them. So the same question is asked here, and answered with
 *  whichever existing set was designed for that answer.
 *
 *  @see SwingTreeLookAndFeel.SymbolPreset#ADAPTIVE
 */
final class AdaptiveSymbols implements Symbols
{
    static final Symbols INSTANCE = new AdaptiveSymbols();

    private AdaptiveSymbols() {}

    /** @return the set designed for what the palette in force leaves a theme to work with. */
    private static Symbols chosen() {
        switch ( Mood.of(SwingTreeLookAndFeel.palette()) ) {
            case RELIEF:   return SoftSymbols.INSTANCE;
            case LUMINOUS: return GlassSymbols.INSTANCE;
            case SHEET:
            default:       return MaterialSymbols.INSTANCE;
        }
    }

    @Override public boolean drawsItsOwnChrome() { return chosen().drawsItsOwnChrome(); }
    @Override public int checkGlyphSize() { return chosen().checkGlyphSize(); }
    @Override public int arrowGlyphSize() { return chosen().arrowGlyphSize(); }
    @Override public int comboArrowButtonSize() { return chosen().comboArrowButtonSize(); }
    @Override public int spinnerButtonWidth() { return chosen().spinnerButtonWidth(); }
    @Override public int spinnerButtonHeight() { return chosen().spinnerButtonHeight(); }
    @Override public int sliderThumbDiameter() { return chosen().sliderThumbDiameter(); }
    @Override public int sliderTrackThickness() { return chosen().sliderTrackThickness(); }
    @Override public int scrollBarThickness() { return chosen().scrollBarThickness(); }
    @Override public int splitDividerThickness() { return chosen().splitDividerThickness(); }
    @Override public int progressBarThickness() { return chosen().progressBarThickness(); }
    @Override public int separatorThickness() { return chosen().separatorThickness(); }
    @Override public int tableRowHeight() { return chosen().tableRowHeight(); }
    @Override public int treeRowHeight() { return chosen().treeRowHeight(); }
    @Override public int tabPaddingVertical() { return chosen().tabPaddingVertical(); }
    @Override public int tabPaddingHorizontal() { return chosen().tabPaddingHorizontal(); }
    @Override public int tabAreaGap() { return chosen().tabAreaGap(); }
    @Override public void paintCheckGlyph( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected ) { chosen().paintCheckGlyph(g, p, x, y, w, h, enabled, focused, rollover, pressed, selected); }
    @Override public void paintRadioGlyph( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected ) { chosen().paintRadioGlyph(g, p, x, y, w, h, enabled, focused, rollover, pressed, selected); }
    @Override public void paintDisclosure( Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled ) { chosen().paintDisclosure(g, p, x, y, w, h, expanded, enabled); }
    @Override public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) { chosen().paintSubmenuArrow(g, p, x, y, w, h, enabled); }
    @Override public void paintComboArrow( Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed ) { chosen().paintComboArrow(g, p, w, h, enabled, rollover, pressed); }
    @Override public void paintSpinnerArrow( Graphics2D g, Palette p, int w, int h, boolean up, boolean enabled, boolean rollover, boolean pressed ) { chosen().paintSpinnerArrow(g, p, w, h, up, enabled, rollover, pressed); }
    @Override public void paintSliderTrack( Graphics2D g, Palette p, Rectangle track, int thumbCentre, boolean horizontal, boolean inverted, boolean enabled ) { chosen().paintSliderTrack(g, p, track, thumbCentre, horizontal, inverted, enabled); }
    @Override public void paintSliderThumb( Graphics2D g, Palette p, Rectangle thumb, boolean enabled, boolean focused ) { chosen().paintSliderThumb(g, p, thumb, enabled, focused); }
    @Override public void paintScrollThumb( Graphics2D g, Palette p, Rectangle thumb, boolean active ) { chosen().paintScrollThumb(g, p, thumb, active); }
    @Override public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) { chosen().paintSplitGrip(g, p, w, h, horizontalSplit, enabled); }
    @Override public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) { chosen().paintDragHandle(g, p, w, h, horizontal); }
    @Override public void paintProgressFill( Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled ) { chosen().paintProgressFill(g, p, w, h, ratio, horizontal, enabled); }
    @Override public void paintTabSurface( Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover ) { chosen().paintTabSurface(g, p, x, y, w, h, selected, rollover); }
    @Override public void paintTabAccent( Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled ) { chosen().paintTabAccent(g, p, x, y, w, h, tabPlacement, enabled); }
}

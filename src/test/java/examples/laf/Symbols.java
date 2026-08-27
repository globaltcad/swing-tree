package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 *  How a look and feel draws the small pieces of geometry that no style rule can express - a
 *  check mark, a radio dot, a drop-down arrow, a slider handle, a scroll bar thumb, the grip on a
 *  split-pane divider - and how much room they need. The two are one decision: a 16-pixel round
 *  slider handle wants a slider at least 20 pixels tall, so splitting them apart would let a
 *  symbol set be swapped into a layout it does not fit. Every metric is in <b>developer pixels</b>
 *  and is scaled by the caller through {@link swingtree.UI#scale(int)}.
 *  <p>
 *  Every painting method is handed a scratch {@link Graphics2D} it may configure freely, the
 *  {@link Palette} to take its colours from, the geometry in <b>component</b> pixels (already
 *  scaled), and the component state as plain flags. Nothing here reads a Swing component, so a
 *  symbol set is a pure function of its arguments and can be exercised without a GUI.
 *
 *  @see SwingTreeLookAndFeel.SymbolPreset
 */
interface Symbols
{
    /**
     *  Whether this set draws and sizes the chrome itself. Answering {@code false} - the blank set
     *  does - makes every delegate fall through to its inherited {@code Basic*UI}, so nothing else
     *  here is asked. The two exceptions are {@link #paintCheckGlyph} and {@link #paintRadioGlyph}:
     *  the basic look and feel's own versions of those are empty stubs, and a control nobody can
     *  read is not what "no chrome" is supposed to mean.
     */
    boolean drawsItsOwnChrome();

    // ── Metrics, in developer pixels ─────────────────────────────────────

    /** @return the side length of the check-box and radio glyphs. */
    int checkGlyphSize();

    /** @return the side length of a tree disclosure handle and a submenu arrow. */
    int arrowGlyphSize();

    /** @return the side length of a combo box's drop-down button. */
    int comboArrowButtonSize();

    /** @return the width of one of a spinner's two stepper buttons. */
    int spinnerButtonWidth();

    /** @return the height of one of a spinner's two stepper buttons. */
    int spinnerButtonHeight();

    /** @return the diameter of a slider's handle. */
    int sliderThumbDiameter();

    /** @return the thickness of a slider's track groove. */
    int sliderTrackThickness();

    /** @return the thickness of a scroll bar across its short axis. */
    int scrollBarThickness();

    /** @return the thickness of a split pane's divider. */
    int splitDividerThickness();

    /** @return the smallest thickness a progress bar is laid out at across its short axis. */
    int progressBarThickness();

    /** @return the thickness of the hairline a separator draws. */
    int separatorThickness();

    /** @return the height of one table row. */
    int tableRowHeight();

    /** @return the height of one tree row. */
    int treeRowHeight();

    /** @return the padding above and below a tab's label. */
    int tabPaddingVertical();

    /** @return the padding left and right of a tab's label. */
    int tabPaddingHorizontal();

    /** @return the gap between the edge of a tabbed pane and its strip of tabs. */
    int tabAreaGap();

    // ── Glyphs in front of a label ───────────────────────────────────────

    /** Draws the glyph in front of a check box or a check-box menu item. */
    void paintCheckGlyph(
        Graphics2D g, Palette p, int x, int y, int w, int h,
        boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
    );

    /** Draws the glyph in front of a radio button or a radio menu item. */
    void paintRadioGlyph(
        Graphics2D g, Palette p, int x, int y, int w, int h,
        boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
    );

    // ── Arrows ───────────────────────────────────────────────────────────

    /** Draws a tree node's disclosure handle. */
    void paintDisclosure(
        Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
    );

    /** Draws the arrow at the right edge of a menu entry that opens a submenu. */
    void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled );

    /** Draws the arrow on a combo box's drop-down button. */
    void paintComboArrow(
        Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
    );

    /** Draws the arrow on one of a spinner's two stepper buttons. */
    void paintSpinnerArrow(
        Graphics2D g, Palette p, int w, int h, boolean up,
        boolean enabled, boolean rollover, boolean pressed
    );

    // ── Chrome ───────────────────────────────────────────────────────────

    /** Draws a slider's groove and the part of it that lies on the filled side of the handle. */
    void paintSliderTrack(
        Graphics2D g, Palette p, Rectangle track, int thumbCentre,
        boolean horizontal, boolean inverted, boolean enabled
    );

    /** Draws a slider's handle. */
    void paintSliderThumb( Graphics2D g, Palette p, Rectangle thumb, boolean enabled, boolean focused );

    /**
     *  Draws a scroll bar's thumb. The groove it slides along is not a symbol: a flat fill with a
     *  radius is something a style rule already says, and the scroll bar's own styled background
     *  covers the whole bar, so a symbol set drawing it again would rasterize the same colour
     *  twice - see {@link SwingTreeScrollBarUI#paintTrack}.
     */
    void paintScrollThumb( Graphics2D g, Palette p, Rectangle thumb, boolean active );

    /** Draws the centre line and grip of a split pane's divider. */
    void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled );

    /** Draws the handle a floatable tool bar is dragged by. */
    void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal );

    /**
     *  Draws the filled part of a determinate progress bar. The trough underneath it is a
     *  style rule, not a symbol.
     */
    void paintProgressFill(
        Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
    );

    /** Draws what lies behind one tab's label. */
    void paintTabSurface(
        Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
    );

    /**
     *  Marks the selected tab on the edge nearest its page, so that tab and page read as one.
     *  {@code tabPlacement} is one of the {@link javax.swing.SwingConstants} edges.
     */
    void paintTabAccent(
        Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
    );
}

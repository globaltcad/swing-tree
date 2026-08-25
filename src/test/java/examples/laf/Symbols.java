package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 *  How a look and feel draws the small pieces of geometry that no style rule can express — a
 *  check mark, a radio dot, a drop-down arrow, a slider handle, a scrollbar thumb, the grip on
 *  a split-pane divider — and how thick the chrome those pieces live in is.
 *
 *  <h2>Why the metrics live here too</h2>
 *  A symbol and the space it needs are one decision: a 16-pixel round slider handle wants a
 *  slider at least 20 pixels tall, and a drop-down arrow drawn with a 4-pixel arm wants a
 *  button of about 20 to sit in. Splitting the two apart would let a symbol preset be swapped
 *  into a layout it does not fit. Every number returned here is therefore in <b>developer
 *  pixels</b>, and is scaled by the caller through {@link swingtree.UI#scale(int)}.
 *
 *  <h2>What a painting method receives</h2>
 *  Every painting method is handed a {@link Graphics2D} it may configure freely — the callers
 *  pass a scratch copy — the {@link Palette} to take its colours from, the geometry in
 *  <b>component</b> pixels (already scaled), and the component state it must react to as plain
 *  flags. Nothing here reads a Swing component, so a symbol set is a pure function of its
 *  arguments and can be exercised without a GUI.
 *
 *  @see SwingTreeLookAndFeel.SymbolPreset
 */
interface Symbols
{
    /**
     *  Whether this symbol set has chrome of its own at all.
     *  <p>
     *  A set answering {@code false} - the blank one - makes every delegate fall through to the
     *  painting and the sizing its inherited {@code Basic*UI} would do, and leaves the tree handles
     *  and the submenu arrow to Swing's own icons. Nothing else here is then called, which is why
     *  the blank set is allowed to answer most of it with nothing.
     *  <p>
     *  The two exceptions are {@link #paintCheckGlyph} and {@link #paintRadioGlyph}, which are
     *  asked of every set: the basic look and feel's own versions of those two are empty stubs, and
     *  a control nobody can read is not what "no chrome" is supposed to mean.
     *
     * @return {@code true} if this set draws and sizes the chrome itself
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

    /**
     *  Draws the glyph in front of a check box or a check-box menu item.
     *
     * @param g the graphics context to draw into
     * @param p the palette to take colours from
     * @param x the left edge of the glyph, in component pixels
     * @param y the top edge of the glyph, in component pixels
     * @param w the glyph width, in component pixels
     * @param h the glyph height, in component pixels
     * @param enabled  whether the control can be used
     * @param focused  whether the control owns the keyboard focus
     * @param rollover whether the pointer is over the control
     * @param pressed  whether the control is being pressed
     * @param selected whether the box is ticked
     */
    void paintCheckGlyph(
        Graphics2D g, Palette p, int x, int y, int w, int h,
        boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
    );

    /**
     *  Draws the glyph in front of a radio button or a radio menu item.
     *
     * @param g the graphics context to draw into
     * @param p the palette to take colours from
     * @param x the left edge of the glyph, in component pixels
     * @param y the top edge of the glyph, in component pixels
     * @param w the glyph width, in component pixels
     * @param h the glyph height, in component pixels
     * @param enabled  whether the control can be used
     * @param focused  whether the control owns the keyboard focus
     * @param rollover whether the pointer is over the control
     * @param pressed  whether the control is being pressed
     * @param selected whether this is the chosen member of its group
     */
    void paintRadioGlyph(
        Graphics2D g, Palette p, int x, int y, int w, int h,
        boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
    );

    // ── Arrows ───────────────────────────────────────────────────────────

    /**
     *  Draws a tree node's disclosure handle.
     *
     * @param g the graphics context to draw into
     * @param p the palette to take colours from
     * @param x the left edge of the glyph, in component pixels
     * @param y the top edge of the glyph, in component pixels
     * @param w the glyph width, in component pixels
     * @param h the glyph height, in component pixels
     * @param expanded whether the node's children are showing
     * @param enabled whether the tree can be used
     */
    void paintDisclosure(
        Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
    );

    /**
     *  Draws the arrow at the right edge of a menu entry that opens a submenu.
     *
     * @param g the graphics context to draw into
     * @param p the palette to take colours from
     * @param x the left edge of the glyph, in component pixels
     * @param y the top edge of the glyph, in component pixels
     * @param w the glyph width, in component pixels
     * @param h the glyph height, in component pixels
     * @param enabled whether the entry can be opened
     */
    void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled );

    /**
     *  Draws the arrow on a combo box's drop-down button.
     *
     * @param g the graphics context to draw into
     * @param p the palette to take colours from
     * @param w the button's width, in component pixels
     * @param h the button's height, in component pixels
     * @param enabled whether the combo box can be used
     * @param rollover whether the pointer is over the button
     * @param pressed whether the button is being pressed
     */
    void paintComboArrow(
        Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
    );

    /**
     *  Draws the arrow on one of a spinner's two stepper buttons.
     *
     * @param g the graphics context to draw into
     * @param p the palette to take colours from
     * @param w the button's width, in component pixels
     * @param h the button's height, in component pixels
     * @param up whether this is the button that raises the value
     * @param enabled whether the spinner can be used
     * @param rollover whether the pointer is over the button
     * @param pressed whether the button is being pressed
     */
    void paintSpinnerArrow(
        Graphics2D g, Palette p, int w, int h, boolean up,
        boolean enabled, boolean rollover, boolean pressed
    );

    // ── Chrome ───────────────────────────────────────────────────────────

    /**
     *  Draws a slider's groove and the part of it that lies on the filled side of the handle.
     *
     * @param g the graphics context to draw into
     * @param p the palette to take colours from
     * @param track the rectangle the groove runs through, in component pixels
     * @param thumbCentre the coordinate of the handle's centre along the slider's long axis
     * @param horizontal whether the slider runs left to right
     * @param inverted whether the value axis runs the other way
     * @param enabled whether the slider can be used
     */
    void paintSliderTrack(
        Graphics2D g, Palette p, Rectangle track, int thumbCentre,
        boolean horizontal, boolean inverted, boolean enabled
    );

    /**
     *  Draws a slider's handle.
     *
     * @param g the graphics context to draw into
     * @param p the palette to take colours from
     * @param thumb the handle's bounds, in component pixels
     * @param enabled whether the slider can be used
     * @param focused whether the slider owns the keyboard focus
     */
    void paintSliderThumb( Graphics2D g, Palette p, Rectangle thumb, boolean enabled, boolean focused );

    /**
     *  Draws a scroll bar's thumb. The groove it slides along is not a symbol: a flat fill with a
     *  radius is something a style rule already says, and the scroll bar's own styled background
     *  covers the whole bar, so a symbol set drawing it again would rasterize the same colour
     *  twice - see {@link SwingTreeScrollBarUI#paintTrack}.
     *
     * @param g the graphics context to draw into
     * @param p the palette to take colours from
     * @param thumb the thumb's bounds, in component pixels
     * @param active whether the pointer is over it or it is being dragged
     */
    void paintScrollThumb( Graphics2D g, Palette p, Rectangle thumb, boolean active );

    /**
     *  Draws the centre line and grip of a split pane's divider.
     *
     * @param g the graphics context to draw into
     * @param p the palette to take colours from
     * @param w the divider's width, in component pixels
     * @param h the divider's height, in component pixels
     * @param horizontalSplit whether the divider stands upright between two side-by-side panes
     * @param enabled whether the split pane can be used
     */
    void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled );

    /**
     *  Draws the handle a floatable tool bar is dragged by.
     *
     * @param g the graphics context to draw into
     * @param p the palette to take colours from
     * @param w the tool bar's width, in component pixels
     * @param h the tool bar's height, in component pixels
     * @param horizontal whether the tool bar runs left to right
     */
    void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal );

    /**
     *  Draws the filled part of a determinate progress bar. The trough underneath it is a
     *  style rule, not a symbol.
     *
     * @param g the graphics context to draw into
     * @param p the palette to take colours from
     * @param w the bar's width, in component pixels
     * @param h the bar's height, in component pixels
     * @param ratio how much of the bar is filled, from 0 to 1
     * @param horizontal whether the bar fills left to right
     * @param enabled whether the bar is enabled
     */
    void paintProgressFill(
        Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
    );

    /**
     *  Draws what lies behind one tab's label.
     *
     * @param g the graphics context to draw into
     * @param p the palette to take colours from
     * @param x the tab's left edge, in component pixels
     * @param y the tab's top edge, in component pixels
     * @param w the tab's width, in component pixels
     * @param h the tab's height, in component pixels
     * @param selected whether this is the tab whose page is showing
     * @param rollover whether the pointer is over it
     */
    void paintTabSurface(
        Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
    );

    /**
     *  Marks the selected tab on the edge nearest its page, so that tab and page read as one.
     *
     * @param g the graphics context to draw into
     * @param p the palette to take colours from
     * @param x the tab's left edge, in component pixels
     * @param y the tab's top edge, in component pixels
     * @param w the tab's width, in component pixels
     * @param h the tab's height, in component pixels
     * @param tabPlacement one of {@link javax.swing.SwingConstants#TOP}, {@code BOTTOM},
     *                     {@code LEFT} or {@code RIGHT}
     * @param enabled whether the tab can be selected
     */
    void paintTabAccent(
        Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
    );
}

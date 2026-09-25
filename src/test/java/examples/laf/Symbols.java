package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;
import swingtree.UI;
import swingtree.api.laf.OptimizedShapeRendering;

import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 *  How a look and feel draws the small pieces of geometry no style rule can express - a check mark,
 *  a radio dot, a drop-down arrow, a slider handle, a scroll bar thumb, the grip on a split-pane
 *  divider - and how much room they need. The shapes and the metrics are one decision, because a
 *  16-pixel round slider handle wants a slider at least 20 pixels tall. Every metric is in
 *  <b>developer pixels</b> and the caller scales it through {@link swingtree.UI#scale(int)}.
 *  <p>
 *  Every painting method is handed a scratch {@link Graphics2D} it may configure freely, the
 *  {@link Palette} to take its colours from, the geometry in <b>component</b> pixels, and the
 *  component's state as flags. Nothing here reads a Swing component, so a symbol set is a pure
 *  function of its arguments, can be exercised without a GUI, and can be memoised by
 *  {@link CachedSymbols}.
 *
 *  @see SwingTreeLookAndFeel.SymbolPreset
 */
interface Symbols
{
    /**
     *  Whether this set draws and sizes the chrome itself. An answer of {@code false}, which only
     *  {@link Blank} gives, makes every delegate fall through to the {@code Basic*UI} it extends and
     *  nothing else here is ever asked. {@link #paintCheckGlyph} and {@link #paintRadioGlyph} are the
     *  two exceptions, because {@code BasicIconFactory}'s versions of those are empty stubs.
     */
    boolean drawsItsOwnChrome();

    // ── Metrics, in developer pixels ─────────────────────────────────────
    // A thickness is measured across the control's short axis, and a size is the side length of a
    // square. The two glyph sizes cover more than their names say: the check size is the radio's
    // too, and the arrow size is a tree disclosure handle's as well as a submenu arrow's.

    int checkGlyphSize();

    int arrowGlyphSize();

    /**
     *  How large the icon a tree's disclosure handle is drawn in is, in developer pixels. The handle
     *  is laid out centred on the tree's indent, so a set that draws its wedge at the edge of a
     *  wider icon moves it along the row.
     *
     * @return the side of the square the handle is drawn in
     */
    default int disclosureGlyphSize() { return arrowGlyphSize(); }

    int comboArrowButtonSize();

    int spinnerButtonWidth();

    int spinnerButtonHeight();

    int sliderThumbDiameter();

    int sliderTrackThickness();

    int scrollBarThickness();

    int splitDividerThickness();

    /** @return the smallest thickness a progress bar may be laid out at. */
    int progressBarThickness();

    int separatorThickness();

    int tableRowHeight();

    int treeRowHeight();

    int tabPaddingVertical();

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

    /**
     *  Draws the same arrow on an entry that may be armed, which is to say lying on the selection
     *  band. A set whose arrow reads on the band as well as off it draws the one arrow for both.
     *
     * @param armed whether the entry is armed
     */
    default void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled, boolean armed ) {
        paintSubmenuArrow(g, p, x, y, w, h, enabled);
    }

    /**
     *  Draws the tick of a check box menu item or the mark of a radio button menu item. A set that
     *  marks menu entries the way it marks check boxes and radio buttons draws those glyphs.
     *
     * @param radio whether it marks a radio button menu item rather than a check box menu item
     * @param armed whether the entry lies on the selection band
     */
    default void paintMenuMark(
        Graphics2D g, Palette p, int x, int y, int w, int h, boolean radio,
        boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected, boolean armed
    ) {
        if ( radio )
            paintRadioGlyph(g, p, x, y, w, h, enabled, focused, rollover, pressed, selected);
        else
            paintCheckGlyph(g, p, x, y, w, h, enabled, focused, rollover, pressed, selected);
    }

    /** @return the side of the square a menu entry's tick or mark is drawn in, in developer pixels */
    default int menuMarkSize() { return checkGlyphSize(); }

    /**
     *  Draws the arrow on a combo box's drop-down button, filling it: for a set whose actuator
     *  {@linkplain #actuatorReachesBounds() reaches the control's edge}, the button is the end of
     *  the control, and the margin is already outside it.
     */
    void paintComboArrow(
        Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
    );

    /** Draws the arrow on one of a spinner's two stepper buttons. */
    void paintSpinnerArrow(
        Graphics2D g, Palette p, int w, int h, boolean up,
        boolean enabled, boolean rollover, boolean pressed
    );

    // ── Chrome ───────────────────────────────────────────────────────────

    /** Draws a slider's groove and the part of it lying on the filled side of the handle. */
    void paintSliderTrack(
        Graphics2D g, Palette p, Rectangle track, int thumbCentre,
        boolean horizontal, boolean inverted, boolean enabled
    );

    void paintSliderThumb(
        Graphics2D g, Palette p, Rectangle thumb, boolean enabled, boolean focused, boolean rollover
    );

    /**
     *  Draws a scroll bar's thumb. The groove it slides along is a style rule rather than a symbol,
     *  because the scroll bar's own styled background already covers the whole bar and a symbol
     *  drawing it again would rasterize the same colour twice.
     */
    void paintScrollThumb( Graphics2D g, Palette p, Rectangle thumb, boolean active );

    /** Draws the centre line and grip of a split pane's divider. */
    void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled );

    /**
     *  Draws the handle a floatable tool bar is dragged by, in the room the tool bar's border and
     *  padding leave before its first button. The context is already at the corner of that room,
     *  so {@code w} and {@code h} are its size and not the tool bar's.
     */
    void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal );

    /**
     *  Draws the filled part of a determinate progress bar. The trough underneath it is a style
     *  rule, not a symbol, and {@code w} and {@code h} are that trough: the box the bar's margin
     *  leaves, which the context is already at the corner of. A set that keeps the fill clear of
     *  the trough's edge measures its own padding in from there.
     */
    void paintProgressFill(
        Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
    );

    /**
     *  The ink a tab's label is written in. Most sets mute the tabs whose pages are hidden, so that
     *  the one whose page shows stands out by its label as well as by its surface.
     *
     * @param p the palette in force
     * @param selected whether this is the tab whose page shows
     * @param enabled whether the tab can be chosen
     * @return the ink for the label
     */
    default Color tabText( Palette p, boolean selected, boolean enabled ) {
        return !enabled ? p.textDisabled() : selected ? p.text() : p.textMuted();
    }

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

    /**
     *  How deep the strip between a tabbed pane's tabs and its page is, in developer pixels. A set
     *  that separates the two with a rule answers one.
     *  <p>
     *  This is one of the few members here carrying an answer of its own. Each of those describes a
     *  habit one set has rather than a decision every set has to take, so its answer is the habit's
     *  absence and a set says nothing at all unless it has that habit.
     *
     * @return how many developer pixels the edge occupies
     */
    default int tabEdgeThickness() { return 1; }

    /**
     *  Draws that strip, across the whole pane rather than under one tab.
     *
     * @param g the context to draw on
     * @param p the palette in force
     * @param edge the whole strip, in component pixels
     * @param selectedTab where the selected tab is, so that an edge may open under it and let the
     *                    tab run into the page; {@code null} when no tab is selected
     * @param tabPlacement which side of the page the tabs are on, as a
     *                     {@link javax.swing.SwingConstants} edge
     */
    default void paintTabEdge(
        Graphics2D g, Palette p, Rectangle edge, @Nullable Rectangle selectedTab, int tabPlacement
    ) {
        g.setColor(p.borderSoft());
        g.fillRect(edge.x, edge.y, edge.width, edge.height);
    }

    /**
     *  Whether a scroll bar has a button at each end that scrolls it a line at a time. A set
     *  answering {@code false}, which is all but one of them, gets a bar that is only its groove
     *  and its thumb.
     *
     * @return whether {@link #paintScrollStepper} should be asked for those two buttons
     */
    /**
     *  Whether the button of a combo box stands at the very end of the control and spans its whole
     *  height, over the combo box's own edge, instead of inside its insets. A set whose actuators
     *  carry an outline of their own, rather than sitting inside the control's outline, answers
     *  {@code true} and fills the button it is given.
     *  <p>
     *  The end of the <i>control</i> is not the edge of the component: a margin takes room outside
     *  it, and {@link SwingTreeComboBoxUI} lays the button out in the box the margin leaves, so
     *  that the actuator moves in with the rest of the control instead of staying at the edge.
     *
     * @return whether a combo box's button reaches the control's edge
     */
    default boolean actuatorReachesBounds() { return false; }

    default boolean scrollBarHasSteppers() { return false; }

    /**
     *  How long a scroll bar's stepper button is along the bar, in developer pixels. A set whose
     *  steppers are square answers the bar's thickness.
     *
     * @return the length of a stepper button
     */
    default int scrollStepperLength() { return scrollBarThickness(); }

    /**
     *  Draws one of them.
     *
     * @param g the context to draw on, whose origin is the button's own corner
     * @param p the palette in force
     * @param w how wide the button is, in component pixels
     * @param h how tall it is
     * @param direction which way it scrolls, which is also which way its arrow points
     * @param enabled whether the scroll bar can be worked
     * @param rollover whether the pointer is over this button
     * @param pressed whether it is being held down
     */
    default void paintScrollStepper(
        Graphics2D g, Palette p, int w, int h, LafUtilities.Direction direction,
        boolean enabled, boolean rollover, boolean pressed
    ) {}

    /**
     *  What the line between two column headings is drawn in. A set that rules its headings apart
     *  names a paint, which may shade along the line; one that draws the heading row as a single
     *  strip says nothing.
     *
     * @param p the palette in force
     * @param height how tall the heading row is, in component pixels, for a paint that shades along it
     * @return that paint, or {@code null} for a heading row with no lines in it
     */
    default @Nullable Paint tableHeaderDivider( Palette p, int height ) { return null; }

    /**
     *  What every second row of a table is tinted with, so that a wide row can be followed across
     *  it. A set that leaves a table as one unbroken sheet says nothing.
     *
     * @param p the palette in force
     * @return that colour, or {@code null} for a table with no stripes
     */
    default @Nullable Color tableRowStripe( Palette p ) { return null; }

    /**
     *  How wide the icon in front of a tree node is, in developer pixels. A set that puts nothing
     *  there answers zero, the look and feel installs no node icons, and a tree then indents its
     *  labels by the disclosure handle alone.
     *
     * @return the side of the square that icon occupies, or zero for no icon
     */
    default int treeNodeGlyphSize() { return 0; }

    /**
     *  Draws that icon.
     *
     * @param g the context to draw on
     * @param p the palette in force
     * @param x the left edge of the icon's box, in component pixels
     * @param y its top edge
     * @param w how wide the box is
     * @param h how tall it is
     * @param leaf whether the node can have no children
     * @param expanded whether a node that can have children is showing them
     * @param enabled whether the tree can be used
     */
    default void paintTreeNode(
        Graphics2D g, Palette p, int x, int y, int w, int h,
        boolean leaf, boolean expanded, boolean enabled
    ) {}

    // IMPLEMENTATIONS:

    /**
     *  The symbol set with no opinions: it answers {@link #drawsItsOwnChrome()} with {@code false},
     *  every call site checks that answer first, and the look and feel installs none of the glyph
     *  icons. So a check box gets Swing's own check box, a scroll bar its own arrows and thumb, a
     *  tabbed pane its own tabs. With {@link SwingTreeLookAndFeel.StylePreset#BLANK} the result is
     *  plain Swing with the style engine wired into every component and nothing painted on top.
     *  <p>
     *  The check and the radio glyph are drawn anyway, because they reach Swing through an icon and
     *  {@code BasicIconFactory}'s versions of both are empty stubs. Every other method here is
     *  unreachable, and draws nothing rather than throwing, so a guard someone forgets leaves a
     *  control undecorated instead of taking the window down.
     */
    final class Blank implements Symbols
    {
        static final Symbols INSTANCE = new Blank();

        private Blank() {}

        @Override public boolean drawsItsOwnChrome() { return false; }

        /** The one metric a blank set still answers, because it does draw the check and the radio
         *  glyph. */
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

        @Override public void paintSliderThumb( Graphics2D g, Palette p, Rectangle thumb, boolean enabled, boolean focused, boolean rollover ) {}

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

    /**
     *  Symbols for {@link SwingTreeLookAndFeel.SymbolPreset#LINEN}: thin strokes, round caps, no
     *  bevels and no gradients. Every stroke and radius comes from {@link UI#scale(float)} rather
     *  than from a bitmap, so the geometry stays crisp at any scale factor.
     */
    final class Linen implements Symbols
    {
        static final Symbols INSTANCE = new Linen();

        private Linen() {}

        @Override public boolean drawsItsOwnChrome() { return true; }

        @Override public int checkGlyphSize()        { return 14; }
        @Override public int arrowGlyphSize()        { return 12; }
        @Override public int comboArrowButtonSize()  { return 20; }
        @Override public int spinnerButtonWidth()    { return 18; }
        @Override public int spinnerButtonHeight()   { return 11; }
        @Override public int sliderThumbDiameter()   { return 16; }
        @Override public int sliderTrackThickness()  { return  4; }
        @Override public int scrollBarThickness()    { return 12; }
        @Override public int splitDividerThickness() { return  8; }
        @Override public int progressBarThickness()  { return 14; }
        @Override public int separatorThickness()    { return  1; }
        @Override public int tableRowHeight()        { return 24; }
        @Override public int treeRowHeight()         { return 22; }
        @Override public int tabPaddingVertical()    { return  6; }
        @Override public int tabPaddingHorizontal()  { return 14; }
        @Override public int tabAreaGap()            { return  4; }

        @Override
        public void paintCheckGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            antialias(g);
            int   arc = UI.scale(4);
            float pad = UI.scale(0.5f);
            g.setColor(glyphSurface(p, enabled, pressed, rollover, selected));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc));
            g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1f))));
            g.setColor(glyphBorder(p, enabled, focused));
            g.draw(new RoundRectangle2D.Float(x + pad, y + pad, w - 1 - pad, h - 1 - pad, arc, arc));
            if ( !selected )
                return;
            g.setColor(glyphMark(p, enabled));
            g.setStroke(new BasicStroke(Math.max(1.5f, UI.scale(1.8f)),
                                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D.Float check = new Path2D.Float();
            check.moveTo(x + w * 0.22f, y + h * 0.52f);
            check.lineTo(x + w * 0.43f, y + h * 0.73f);
            check.lineTo(x + w * 0.78f, y + h * 0.30f);
            g.draw(check);
        }

        @Override
        public void paintRadioGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            antialias(g);
            float pad = UI.scale(0.5f);
            g.setColor(glyphSurface(p, enabled, pressed, rollover, selected));
            g.fill(new Ellipse2D.Float(x, y, w - 1, h - 1));
            g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1f))));
            g.setColor(glyphBorder(p, enabled, focused));
            g.draw(new Ellipse2D.Float(x + pad, y + pad, w - 1 - pad, h - 1 - pad));
            if ( !selected )
                return;
            g.setColor(glyphMark(p, enabled));
            float dotPad = UI.scale(4f);
            g.fill(new Ellipse2D.Float(x + dotPad, y + dotPad,
                                       w - 1 - 2 * dotPad, h - 1 - 2 * dotPad));
        }

        @Override
        public void paintDisclosure(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
        ) {
            chevron(g, x, y, w, h, expanded ? 90 : 0, enabled ? p.textMuted() : p.textDisabled());
        }

        @Override
        public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
            chevron(g, x, y, w, h, 0, enabled ? p.textMuted() : p.textDisabled());
        }

        @Override
        public void paintComboArrow(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {
            antialias(g);
            g.setColor(arrowColour(p, enabled, rollover, pressed));
            float cx = w / 2f;
            float cy = h / 2f + UI.scale(1f);
            float a  = UI.scale(4f);
            Path2D.Float arrow = new Path2D.Float();
            arrow.moveTo(cx - a, cy - a * 0.5f);
            arrow.lineTo(cx,     cy + a * 0.5f);
            arrow.lineTo(cx + a, cy - a * 0.5f);
            g.setStroke(new BasicStroke(Math.max(1.4f, UI.scale(1.6f)),
                                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(arrow);
        }

        @Override
        public void paintSpinnerArrow(
            Graphics2D g, Palette p, int w, int h, boolean up,
            boolean enabled, boolean rollover, boolean pressed
        ) {
            antialias(g);
            g.setColor(arrowColour(p, enabled, rollover, pressed));
            float cx = w / 2f, cy = h / 2f;
            float a  = UI.scale(3f);
            Path2D.Float arrow = new Path2D.Float();
            if ( up ) {
                arrow.moveTo(cx - a, cy + a * 0.5f);
                arrow.lineTo(cx,     cy - a * 0.5f);
                arrow.lineTo(cx + a, cy + a * 0.5f);
            } else {
                arrow.moveTo(cx - a, cy - a * 0.5f);
                arrow.lineTo(cx,     cy + a * 0.5f);
                arrow.lineTo(cx + a, cy - a * 0.5f);
            }
            g.setStroke(new BasicStroke(Math.max(1.2f, UI.scale(1.4f)),
                                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(arrow);
        }

        @Override
        public void paintSliderTrack(
            Graphics2D g, Palette p, Rectangle track, int thumbCentre,
            boolean horizontal, boolean inverted, boolean enabled
        ) {
            antialias(g);
            int   t    = Math.max(2, UI.scale(sliderTrackThickness()));
            int   arc  = t;
            Color fill = enabled ? p.accent() : p.textDisabled();
            if ( horizontal ) {
                int y = track.y + (track.height - t) / 2;
                g.setColor(p.borderSoft());
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(track.x, y, track.width, t, arc, arc));
                if ( !inverted ) {
                    int filled = Math.max(0, thumbCentre - track.x);
                    if ( filled > 0 ) {
                        g.setColor(fill);
                        OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(track.x, y, filled, t, arc, arc));
                    }
                } else {
                    int filled = Math.max(0, track.x + track.width - thumbCentre);
                    if ( filled > 0 ) {
                        g.setColor(fill);
                        OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(thumbCentre, y, filled, t, arc, arc));
                    }
                }
            } else {
                int x = track.x + (track.width - t) / 2;
                g.setColor(p.borderSoft());
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, track.y, t, track.height, arc, arc));
                // A vertical slider defaults to "max at top", so the un-inverted fill covers the
                // area below the handle, down to the bottom of the track.
                if ( !inverted ) {
                    int filled = Math.max(0, track.y + track.height - thumbCentre);
                    if ( filled > 0 ) {
                        g.setColor(fill);
                        OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, thumbCentre, t, filled, arc, arc));
                    }
                } else {
                    int filled = Math.max(0, thumbCentre - track.y);
                    if ( filled > 0 ) {
                        g.setColor(fill);
                        OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, track.y, t, filled, arc, arc));
                    }
                }
            }
        }

        @Override
        public void paintSliderThumb( Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused, boolean rollover ) {
            antialias(g);
            float stroke = Math.max(1f, UI.scale(1f));
            float half   = stroke / 2f;
            Color face = enabled ? p.surfaceField() : p.surfaceDisabled();
            g.setColor(enabled && rollover ? LafUtilities.underPointer(p, face) : face);
            g.fill(new Ellipse2D.Float(r.x, r.y, r.width - 1, r.height - 1));
            g.setStroke(new BasicStroke(stroke));
            g.setColor(enabled ? ( focused ? p.accent() : p.border() ) : p.borderSoft());
            // Inset by half a stroke so the outline lands inside the bounds at any scale.
            g.draw(new Ellipse2D.Float(r.x + half, r.y + half,
                                       r.width - 1 - stroke, r.height - 1 - stroke));
            if ( !enabled )
                return;
            float dotPad = UI.scale(5f);
            g.setColor(p.accent());
            g.fill(new Ellipse2D.Float(r.x + dotPad, r.y + dotPad,
                                       r.width - 1 - 2 * dotPad, r.height - 1 - 2 * dotPad));
        }

        @Override
        public void paintScrollThumb( Graphics2D g, Palette p, Rectangle r, boolean active ) {
            antialias(g);
            int pad = UI.scale(2);
            int arc = UI.scale(8);
            g.setColor(active ? p.accent() : p.border());
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(r.x + pad, r.y + pad,
                                              r.width - 2 * pad, r.height - 2 * pad, arc, arc));
        }

        @Override
        public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
            antialias(g);
            g.setColor(p.borderSoft());
            if ( horizontalSplit )
                g.fillRect(w / 2, 0, Math.max(1, UI.scale(1)), h);
            else
                g.fillRect(0, h / 2, w, Math.max(1, UI.scale(1)));

            g.setColor(enabled ? p.border() : p.borderSoft());
            float r = UI.scale(1.5f);
            int   d = UI.scale(5);
            if ( horizontalSplit ) {
                float cx  = w / 2f;
                float cy0 = h / 2f - d;
                for ( int i = 0; i < 3; i++ )
                    g.fill(new Ellipse2D.Float(cx - r, cy0 + i * d - r, 2 * r, 2 * r));
            } else {
                float cy  = h / 2f;
                float cx0 = w / 2f - d;
                for ( int i = 0; i < 3; i++ )
                    g.fill(new Ellipse2D.Float(cx0 + i * d - r, cy - r, 2 * r, 2 * r));
            }
        }

        @Override
        public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {
            antialias(g);
            Color border = p.border();
            g.setColor(new Color(border.getRed(), border.getGreen(), border.getBlue(), 160));
            float r = UI.scale(1.4f);
            int   d = UI.scale(4);
            if ( horizontal ) {
                float cx  = UI.scale(4);
                float cy0 = h / 2f - d;
                for ( int i = 0; i < 3; i++ )
                    g.fill(new Ellipse2D.Float(cx - r, cy0 + i * d - r, 2 * r, 2 * r));
            } else {
                float cy  = UI.scale(4);
                float cx0 = w / 2f - d;
                for ( int i = 0; i < 3; i++ )
                    g.fill(new Ellipse2D.Float(cx0 + i * d - r, cy - r, 2 * r, 2 * r));
            }
        }

        @Override
        public void paintProgressFill(
            Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
        ) {
            if ( ratio <= 0 )
                return;
            antialias(g);
            int arc = UI.scale(6);
            int pad = UI.scale(2);
            g.setColor(enabled ? p.accent() : p.textDisabled());
            if ( horizontal ) {
                int fillW = Math.max(arc, (int) Math.round((w - 2 * pad) * ratio));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(pad, pad, fillW, h - 2 * pad, arc, arc));
            } else {
                int fillH = Math.max(arc, (int) Math.round((h - 2 * pad) * ratio));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(pad, h - pad - fillH, w - 2 * pad, fillH, arc, arc));
            }
        }

        @Override
        public void paintTabSurface(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
        ) {
            Color fill = selected ? p.surfaceField() : ( rollover ? p.surfaceHover() : null );
            if ( fill == null )
                return;
            antialias(g);
            g.setColor(fill);
            int arc = UI.scale(8);
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w, h, arc, arc));
        }

        @Override
        public void paintTabAccent(
            Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
        ) {
            antialias(g);
            int stripe = Math.max(2, UI.scale(2));
            int arc    = UI.scale(2);
            g.setColor(enabled ? p.accent() : p.textDisabled());
            switch ( tabPlacement ) {
                case SwingConstants.BOTTOM:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w, stripe, arc, arc));
                    break;
                case SwingConstants.LEFT:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x + w - stripe, y, stripe, h, arc, arc));
                    break;
                case SwingConstants.RIGHT:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, stripe, h, arc, arc));
                    break;
                case SwingConstants.TOP:
                default:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y + h - stripe, w, stripe, arc, arc));
                    break;
            }
        }

        // ── Internals ────────────────────────────────────────────────────────

        private static void antialias( Graphics2D g ) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        /** The chevron shared by tree handles and submenu arrows: symmetric, centred, rotated
         *  clockwise by {@code rotationDegrees} from its resting right-pointing shape. */
        private static void chevron( Graphics2D g, int x, int y, int w, int h, double rotationDegrees, Color colour ) {
            antialias(g);
            g.translate(x + w / 2.0, y + h / 2.0);
            g.rotate(Math.toRadians(rotationDegrees));
            g.setColor(colour);
            g.setStroke(new BasicStroke(Math.max(1.2f, UI.scale(1.3f)),
                                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float a = UI.scale(2.5f);
            Path2D.Float shape = new Path2D.Float();
            shape.moveTo(-a, -a);
            shape.lineTo( a,  0);
            shape.lineTo(-a,  a);
            g.draw(shape);
        }

        private static Color arrowColour( Palette p, boolean enabled, boolean rollover, boolean pressed ) {
            if ( !enabled )
                return p.textDisabled();
            return ( rollover || pressed ) ? p.accent() : p.textMuted();
        }

        private static Color glyphSurface( Palette p, boolean enabled, boolean pressed, boolean rollover, boolean selected ) {
            if ( !enabled ) return p.surfaceDisabled();
            if ( pressed )  return p.surfacePressed();
            if ( selected ) return p.accentSoft();
            if ( rollover ) return p.surfaceHover();
            return p.surfaceField();
        }

        private static Color glyphBorder( Palette p, boolean enabled, boolean focused ) {
            if ( !enabled ) return p.borderSoft();
            if ( focused )  return p.accent();
            return p.border();
        }

        private static Color glyphMark( Palette p, boolean enabled ) {
            return enabled ? p.accent() : p.textDisabled();
        }
    }

    /**
     *  Symbols for {@link SwingTreeLookAndFeel.StylePreset#SOFT_UI}: every glyph is the surface
     *  colour, told apart from the panel behind it only by a rim running from a near-white highlight
     *  at the top to a soft shadow at the bottom. Selecting something turns that rim around, so a
     *  ticked box reads as pressed into the clay. Arrows are embossed the same way, drawn once in the
     *  highlight colour a pixel down and right and again in their own colour on top.
     */
    final class Soft implements Symbols
    {
        static final Symbols INSTANCE = new Soft();

        private Soft() {}

        /**
         *  How far the light and the shadow move from the surface they fall on, in channel steps
         *  rather than as a fraction of the way to white. A fraction lifts a dark palette several
         *  times as far as a light one, which turns every thumb and rim into a glowing bar.
         */
        private static final int RIM_LIGHT    =  18;
        private static final int RIM_DARK     = -34;
        private static final int GROOVE_LIGHT =  16;
        private static final int GROOVE_DARK  = -26;
        private static final int EMBOSS_LIGHT =  19;
        private static final int EMBOSS_DARK  = -34;

        @Override public boolean drawsItsOwnChrome() { return true; }

        @Override public int checkGlyphSize()        { return 17; }
        @Override public int arrowGlyphSize()        { return 13; }
        @Override public int comboArrowButtonSize()  { return 22; }
        @Override public int spinnerButtonWidth()    { return 20; }
        @Override public int spinnerButtonHeight()   { return 12; }
        @Override public int sliderThumbDiameter()   { return 20; }
        @Override public int sliderTrackThickness()  { return  8; }
        @Override public int scrollBarThickness()    { return 14; }
        @Override public int splitDividerThickness() { return 10; }
        @Override public int progressBarThickness()  { return 16; }
        @Override public int separatorThickness()    { return  1; }
        @Override public int tableRowHeight()        { return 26; }
        @Override public int treeRowHeight()         { return 24; }
        @Override public int tabPaddingVertical()    { return  8; }
        @Override public int tabPaddingHorizontal()  { return 16; }
        @Override public int tabAreaGap()            { return  6; }

        // ── Glyphs ───────────────────────────────────────────────────────────

        @Override
        public void paintCheckGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            int arc = UI.scale(6);
            RoundRectangle2D.Float body = new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc);
            g.setColor(surface(p, enabled, rollover));
            g.fill(body);
            strokeRim(g, body, p, y, h, selected || pressed);
            if ( !selected )
                return;
            g.setColor(enabled ? p.accent() : p.textDisabled());
            g.setStroke(new BasicStroke(Math.max(1.6f, UI.scale(2f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(LafUtilities.tickShape(x, y, w, h));
        }

        @Override
        public void paintRadioGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            Ellipse2D.Float body = new Ellipse2D.Float(x, y, w - 1, h - 1);
            g.setColor(surface(p, enabled, rollover));
            g.fill(body);
            strokeRim(g, body, p, y, h, selected || pressed);
            if ( !selected )
                return;
            float dot = UI.scale(5f);
            g.setColor(enabled ? p.accent() : p.textDisabled());
            g.fill(new Ellipse2D.Float(x + dot, y + dot, w - 1 - 2 * dot, h - 1 - 2 * dot));
        }

        // ── Arrows ───────────────────────────────────────────────────────────

        @Override
        public void paintDisclosure(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
        ) {
            embossedArrow(g, p, x + w / 2f, y + h / 2f, UI.scale(3.2f),
                          expanded ? LafUtilities.Direction.DOWN : LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
            embossedArrow(g, p, x + w / 2f, y + h / 2f, UI.scale(3.2f), LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintComboArrow(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {
            embossedArrow(g, p, w / 2f, h / 2f, UI.scale(3.6f), LafUtilities.Direction.DOWN, enabled && !pressed);
        }

        @Override
        public void paintSpinnerArrow(
            Graphics2D g, Palette p, int w, int h, boolean up,
            boolean enabled, boolean rollover, boolean pressed
        ) {
            embossedArrow(g, p, w / 2f, h / 2f, UI.scale(2.8f),
                          up ? LafUtilities.Direction.UP : LafUtilities.Direction.DOWN, enabled && !pressed);
        }

        // ── Chrome ───────────────────────────────────────────────────────────

        @Override
        public void paintSliderTrack(
            Graphics2D g, Palette p, Rectangle track, int thumbCentre,
            boolean horizontal, boolean inverted, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int t   = Math.max(3, UI.scale(sliderTrackThickness()));
            int arc = t;
            Color fill = enabled ? p.accent() : p.textDisabled();
            if ( horizontal ) {
                int y = track.y + (track.height - t) / 2;
                g.setPaint(LafUtilities.verticalGradient(y, t, LafUtilities.shadeBySteps(p.background(), GROOVE_DARK), LafUtilities.shadeBySteps(p.background(), GROOVE_LIGHT)));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(track.x, y, track.width, t, arc, arc));
                int filled = inverted ? Math.max(0, track.x + track.width - thumbCentre)
                                      : Math.max(0, thumbCentre - track.x);
                if ( filled > 0 ) {
                    g.setColor(fill);
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(inverted ? thumbCentre : track.x, y, filled, t, arc, arc));
                }
            } else {
                int x = track.x + (track.width - t) / 2;
                g.setPaint(LafUtilities.verticalGradient(track.y, track.height,
                                       LafUtilities.shadeBySteps(p.background(), GROOVE_DARK), LafUtilities.shadeBySteps(p.background(), GROOVE_LIGHT)));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, track.y, t, track.height, arc, arc));
                int filled = inverted ? Math.max(0, thumbCentre - track.y)
                                      : Math.max(0, track.y + track.height - thumbCentre);
                if ( filled > 0 ) {
                    g.setColor(fill);
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, inverted ? track.y : thumbCentre, t, filled, arc, arc));
                }
            }
        }

        @Override
        public void paintSliderThumb( Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused, boolean rollover ) {
            LafUtilities.antialiasShapes(g);
            Ellipse2D.Float body = new Ellipse2D.Float(r.x, r.y, r.width - 1, r.height - 1);
            Color face = enabled ? p.surface() : p.surfaceDisabled();
            g.setColor(enabled && rollover ? LafUtilities.underPointer(p, face) : face);
            g.fill(body);
            strokeRim(g, body, p, r.y, r.height, false);
            if ( !enabled )
                return;
            float dot = UI.scale(6f);
            g.setColor(focused ? p.accent() : LafUtilities.shadeTowards(p.accent(), p.surface(), 0.35));
            g.fill(new Ellipse2D.Float(r.x + dot, r.y + dot, r.width - 1 - 2 * dot, r.height - 1 - 2 * dot));
        }

        @Override
        public void paintScrollThumb( Graphics2D g, Palette p, Rectangle r, boolean active ) {
            LafUtilities.antialiasShapes(g);
            int pad = UI.scale(3);
            int arc = UI.scale(10);
            RoundRectangle2D.Float body = new RoundRectangle2D.Float(
                    r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad, arc, arc);
            g.setColor(active ? LafUtilities.shadeTowards(p.surface(), p.accent(), 0.22) : p.surface());
            g.fill(body);
            strokeRim(g, body, p, r.y + pad, r.height - 2 * pad, false);
        }

        @Override
        public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
            LafUtilities.antialiasShapes(g);
            float radius = UI.scale(2f);
            int   step   = UI.scale(6);
            for ( int i = 0; i < 3; i++ ) {
                float cx = horizontalSplit ? w / 2f          : w / 2f - step + i * step;
                float cy = horizontalSplit ? h / 2f - step + i * step : h / 2f;
                embossedDot(g, p, cx, cy, radius, enabled);
            }
        }

        @Override
        public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {
            LafUtilities.antialiasShapes(g);
            float radius = UI.scale(1.8f);
            int   step   = UI.scale(5);
            for ( int i = 0; i < 3; i++ ) {
                float cx = horizontal ? UI.scale(6)                : w / 2f - step + i * step;
                float cy = horizontal ? h / 2f - step + i * step   : UI.scale(6);
                embossedDot(g, p, cx, cy, radius, true);
            }
        }

        @Override
        public void paintProgressFill(
            Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
        ) {
            if ( ratio <= 0 )
                return;
            LafUtilities.antialiasShapes(g);
            int   pad  = UI.scale(3);
            Color base = enabled ? p.accent() : p.textDisabled();
            if ( horizontal ) {
                int inner = h - 2 * pad;
                int fillW = Math.max(inner, (int) Math.round((w - 2 * pad) * ratio));
                g.setPaint(LafUtilities.verticalGradient(pad, inner, LafUtilities.shadeTowardsWhite(base, 0.22), LafUtilities.shadeTowardsBlack(base, 0.10)));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(pad, pad, fillW, inner, inner, inner));
            } else {
                int inner = w - 2 * pad;
                int fillH = Math.max(inner, (int) Math.round((h - 2 * pad) * ratio));
                g.setPaint(LafUtilities.verticalGradient(h - pad - fillH, fillH, LafUtilities.shadeTowardsWhite(base, 0.22), LafUtilities.shadeTowardsBlack(base, 0.10)));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(pad, h - pad - fillH, inner, fillH, inner, inner));
            }
        }

        @Override
        public void paintTabSurface(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
        ) {
            if ( !selected && !rollover )
                return;
            LafUtilities.antialiasShapes(g);
            int arc = UI.scale(14);
            RoundRectangle2D.Float body = new RoundRectangle2D.Float(x, y, w, h, arc, arc);
            g.setColor(selected ? p.surface() : p.surfaceHover());
            g.fill(body);
            if ( selected )
                strokeRim(g, body, p, y, h, false);
        }

        @Override
        public void paintTabAccent(
            Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int stripe = Math.max(2, UI.scale(3));
            int arc    = stripe;
            g.setColor(enabled ? p.accent() : p.textDisabled());
            switch ( tabPlacement ) {
                case SwingConstants.BOTTOM:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x + w * 0.25f, y, w * 0.5f, stripe, arc, arc)); break;
                case SwingConstants.LEFT:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x + w - stripe, y + h * 0.25f, stripe, h * 0.5f, arc, arc)); break;
                case SwingConstants.RIGHT:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y + h * 0.25f, stripe, h * 0.5f, arc, arc)); break;
                case SwingConstants.TOP:
                default:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x + w * 0.25f, y + h - stripe, w * 0.5f, stripe, arc, arc)); break;
            }
        }

        // ── Internals ────────────────────────────────────────────────────────

        private static Color surface( Palette p, boolean enabled, boolean rollover ) {
            if ( !enabled ) return p.surfaceDisabled();
            return rollover ? p.surfaceHover() : p.surface();
        }

        /** The lit rim that is the whole idiom: highlight at the top, shadow at the bottom - or the
         *  other way around, for something that is meant to look pressed in. */
        private static void strokeRim(
            Graphics2D g, Shape body, Palette p, float y, float h, boolean inverted
        ) {
            Color light = LafUtilities.shadeBySteps(p.background(), RIM_LIGHT);
            Color dark  = LafUtilities.shadeBySteps(p.background(), RIM_DARK);
            g.setStroke(new BasicStroke(Math.max(1.4f, UI.scale(1.6f))));
            g.setPaint(LafUtilities.verticalGradient(y, h, inverted ? dark : light, inverted ? light : dark));
            g.draw(body);
        }

        private static void embossedArrow(
            Graphics2D g, Palette p, float cx, float cy, float size, LafUtilities.Direction direction, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            float lift = UI.scale(1f);
            Path2D.Float shape = LafUtilities.arrowShape(cx, cy, size, size * 0.62f, direction);
            g.translate(lift, lift);
            g.setColor(LafUtilities.shadeBySteps(p.background(), EMBOSS_LIGHT));
            g.fill(shape);
            g.translate(-lift, -lift);
            g.setColor(enabled ? p.textMuted() : p.textDisabled());
            g.fill(shape);
        }

        private static void embossedDot( Graphics2D g, Palette p, float cx, float cy, float radius, boolean enabled ) {
            float lift = UI.scale(1f);
            g.setColor(LafUtilities.shadeBySteps(p.background(), EMBOSS_LIGHT));
            g.fill(new Ellipse2D.Float(cx - radius + lift, cy - radius + lift, 2 * radius, 2 * radius));
            g.setColor(enabled ? LafUtilities.shadeBySteps(p.background(), EMBOSS_DARK) : p.borderSoft());
            g.fill(new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius));
        }
    }

    /**
     *  Symbols for {@link SwingTreeLookAndFeel.StylePreset#FRUTIGER_AERO}: a glyph is a saturated fill
     *  under a highlight that breaks on a hard line just above the middle, wrapped in a crisp outline
     *  a few shades darker, with a white sheen along the top edge. Arrows are solid rather than
     *  stroked and carry a pale copy of themselves one pixel below.
     */
    final class Glossy implements Symbols
    {
        static final Symbols INSTANCE = new Glossy();

        private Glossy() {}

        @Override public boolean drawsItsOwnChrome() { return true; }

        @Override public int checkGlyphSize()        { return 15; }
        @Override public int arrowGlyphSize()        { return 12; }
        @Override public int comboArrowButtonSize()  { return 20; }
        @Override public int spinnerButtonWidth()    { return 18; }
        @Override public int spinnerButtonHeight()   { return 11; }
        @Override public int sliderThumbDiameter()   { return 18; }
        @Override public int sliderTrackThickness()  { return  7; }
        @Override public int scrollBarThickness()    { return 14; }
        @Override public int splitDividerThickness() { return  9; }
        @Override public int progressBarThickness()  { return 16; }
        @Override public int separatorThickness()    { return  1; }
        @Override public int tableRowHeight()        { return 24; }
        @Override public int treeRowHeight()         { return 22; }
        @Override public int tabPaddingVertical()    { return  6; }
        @Override public int tabPaddingHorizontal()  { return 16; }
        @Override public int tabAreaGap()            { return  4; }

        // ── Glyphs ───────────────────────────────────────────────────────────

        @Override
        public void paintCheckGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            int arc = UI.scale(4);
            Color base = glyphFill(p, enabled, rollover, pressed, selected);
            RoundRectangle2D.Float body = new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc);
            g.setPaint(LafUtilities.glossGradient(y, h - 1, base));
            g.fill(body);
            outline(g, body, p, enabled, focused, base);
            sheen(g, x + UI.scale(1.5f), y + UI.scale(1.5f), w - 1 - UI.scale(3f), (h - 1) * 0.38f, arc);
            if ( !selected )
                return;
            g.setColor(enabled ? p.onFilled() : p.textDisabled());
            g.setStroke(new BasicStroke(Math.max(1.5f, UI.scale(1.9f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(LafUtilities.tickShape(x, y, w, h));
        }

        @Override
        public void paintRadioGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            Color base = glyphFill(p, enabled, rollover, pressed, selected);
            Ellipse2D.Float body = new Ellipse2D.Float(x, y, w - 1, h - 1);
            g.setPaint(LafUtilities.glossGradient(y, h - 1, base));
            g.fill(body);
            outline(g, body, p, enabled, focused, base);
            g.setColor(new Color(255, 255, 255, 130));
            g.fill(new Ellipse2D.Float(x + UI.scale(2f), y + UI.scale(1.5f),
                                       (w - 1) - UI.scale(4f), (h - 1) * 0.42f));
            if ( !selected )
                return;
            float dot = UI.scale(4.5f);
            g.setColor(enabled ? p.onFilled() : p.textDisabled());
            g.fill(new Ellipse2D.Float(x + dot, y + dot, w - 1 - 2 * dot, h - 1 - 2 * dot));
        }

        // ── Arrows ───────────────────────────────────────────────────────────

        @Override
        public void paintDisclosure(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
        ) {
            droppedArrow(g, p, x + w / 2f, y + h / 2f, UI.scale(3.2f),
                         expanded ? LafUtilities.Direction.DOWN : LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
            droppedArrow(g, p, x + w / 2f, y + h / 2f, UI.scale(3.2f), LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintComboArrow(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {
            droppedArrow(g, p, w / 2f, h / 2f + UI.scale(0.5f), UI.scale(4f), LafUtilities.Direction.DOWN,
                         enabled && !pressed);
        }

        @Override
        public void paintSpinnerArrow(
            Graphics2D g, Palette p, int w, int h, boolean up,
            boolean enabled, boolean rollover, boolean pressed
        ) {
            droppedArrow(g, p, w / 2f, h / 2f, UI.scale(3f),
                         up ? LafUtilities.Direction.UP : LafUtilities.Direction.DOWN, enabled && !pressed);
        }

        // ── Chrome ───────────────────────────────────────────────────────────

        @Override
        public void paintSliderTrack(
            Graphics2D g, Palette p, Rectangle track, int thumbCentre,
            boolean horizontal, boolean inverted, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int   t     = Math.max(3, UI.scale(sliderTrackThickness()));
            int   arc   = t;
            Color fill  = enabled ? p.primary() : p.textDisabled();
            Color empty = p.surfaceDisabled();
            if ( horizontal ) {
                int y = track.y + (track.height - t) / 2;
                g.setPaint(LafUtilities.verticalGradient(y, t, LafUtilities.shadeTowardsBlack(empty, 0.16), LafUtilities.shadeTowardsWhite(empty, 0.30)));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(track.x, y, track.width, t, arc, arc));
                g.setColor(p.border());
                g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1f))));
                g.draw(new RoundRectangle2D.Float(track.x, y, track.width - 1, t - 1, arc, arc));
                int filled = inverted ? Math.max(0, track.x + track.width - thumbCentre)
                                      : Math.max(0, thumbCentre - track.x);
                if ( filled > 0 ) {
                    g.setPaint(LafUtilities.glossGradient(y, t, fill));
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(inverted ? thumbCentre : track.x, y, filled, t, arc, arc));
                }
            } else {
                int x = track.x + (track.width - t) / 2;
                g.setColor(LafUtilities.shadeTowardsBlack(empty, 0.08));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, track.y, t, track.height, arc, arc));
                g.setColor(p.border());
                g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1f))));
                g.draw(new RoundRectangle2D.Float(x, track.y, t - 1, track.height - 1, arc, arc));
                int filled = inverted ? Math.max(0, thumbCentre - track.y)
                                      : Math.max(0, track.y + track.height - thumbCentre);
                if ( filled > 0 ) {
                    int top = inverted ? track.y : thumbCentre;
                    g.setPaint(LafUtilities.glossGradient(top, filled, fill));
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, top, t, filled, arc, arc));
                }
            }
        }

        @Override
        public void paintSliderThumb( Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused, boolean rollover ) {
            LafUtilities.antialiasShapes(g);
            Color base = enabled ? ( focused ? LafUtilities.shadeTowardsWhite(p.accent(), 0.30) : p.surfaceField() ) : p.surfaceDisabled();
            if ( enabled && rollover )
                base = LafUtilities.underPointer(p, base);
            Ellipse2D.Float body = new Ellipse2D.Float(r.x, r.y, r.width - 1, r.height - 1);
            g.setPaint(LafUtilities.glossGradient(r.y, r.height - 1, base));
            g.fill(body);
            g.setColor(enabled ? ( focused ? p.accent() : p.border() ) : p.borderSoft());
            g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1.2f))));
            g.draw(body);
            g.setColor(new Color(255, 255, 255, 150));
            g.fill(new Ellipse2D.Float(r.x + UI.scale(3f), r.y + UI.scale(2f),
                                       (r.width - 1) - UI.scale(6f), (r.height - 1) * 0.38f));
        }

        @Override
        public void paintScrollThumb( Graphics2D g, Palette p, Rectangle r, boolean active ) {
            LafUtilities.antialiasShapes(g);
            int   pad  = UI.scale(2);
            int   arc  = UI.scale(8);
            Color base = active ? p.accent() : LafUtilities.shadeTowards(p.surface(), p.border(), 0.45);
            RoundRectangle2D.Float body = new RoundRectangle2D.Float(
                    r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad, arc, arc);
            g.setPaint(LafUtilities.glossGradient(r.y + pad, r.height - 2 * pad, base));
            g.fill(body);
            g.setColor(LafUtilities.shadeTowardsBlack(base, 0.25));
            g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1f))));
            g.draw(body);
        }

        @Override
        public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
            LafUtilities.antialiasShapes(g);
            float radius = UI.scale(1.8f);
            int   step   = UI.scale(5);
            for ( int i = 0; i < 3; i++ ) {
                float cx = horizontalSplit ? w / 2f                    : w / 2f - step + i * step;
                float cy = horizontalSplit ? h / 2f - step + i * step  : h / 2f;
                glassDot(g, p, cx, cy, radius, enabled);
            }
        }

        @Override
        public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {
            LafUtilities.antialiasShapes(g);
            float radius = UI.scale(1.6f);
            int   step   = UI.scale(4);
            for ( int i = 0; i < 3; i++ ) {
                float cx = horizontal ? UI.scale(5)                  : w / 2f - step + i * step;
                float cy = horizontal ? h / 2f - step + i * step     : UI.scale(5);
                glassDot(g, p, cx, cy, radius, true);
            }
        }

        @Override
        public void paintProgressFill(
            Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
        ) {
            if ( ratio <= 0 )
                return;
            LafUtilities.antialiasShapes(g);
            int   pad  = UI.scale(2);
            Color base = enabled ? p.primary() : p.textDisabled();
            if ( horizontal ) {
                int inner = h - 2 * pad;
                int fillW = Math.max(inner, (int) Math.round((w - 2 * pad) * ratio));
                g.setPaint(LafUtilities.glossGradient(pad, inner, base));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(pad, pad, fillW, inner, inner, inner));
            } else {
                int inner = w - 2 * pad;
                int fillH = Math.max(inner, (int) Math.round((h - 2 * pad) * ratio));
                g.setPaint(LafUtilities.glossGradient(h - pad - fillH, fillH, base));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(pad, h - pad - fillH, inner, fillH, inner, inner));
            }
        }

        @Override
        public void paintTabSurface(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
        ) {
            if ( !selected && !rollover )
                return;
            LafUtilities.antialiasShapes(g);
            int   arc  = UI.scale(8);
            Color base = selected ? p.surfaceField() : p.surfaceHover();
            RoundRectangle2D.Float body = new RoundRectangle2D.Float(x, y, w, h, arc, arc);
            g.setPaint(LafUtilities.glossGradient(y, h, base));
            g.fill(body);
            if ( !selected )
                return;
            g.setColor(p.border());
            g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1f))));
            g.draw(new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc));
        }

        @Override
        public void paintTabAccent(
            Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int stripe = Math.max(2, UI.scale(3));
            int arc    = UI.scale(2);
            g.setPaint(enabled ? LafUtilities.verticalGradient(y, h, LafUtilities.shadeTowardsWhite(p.accent(), 0.25), p.accent())
                               : p.textDisabled());
            switch ( tabPlacement ) {
                case SwingConstants.BOTTOM:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w, stripe, arc, arc)); break;
                case SwingConstants.LEFT:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x + w - stripe, y, stripe, h, arc, arc)); break;
                case SwingConstants.RIGHT:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, stripe, h, arc, arc)); break;
                case SwingConstants.TOP:
                default:
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y + h - stripe, w, stripe, arc, arc)); break;
            }
        }

        // ── Internals ────────────────────────────────────────────────────────

        private static Color glyphFill( Palette p, boolean enabled, boolean rollover, boolean pressed, boolean selected ) {
            if ( !enabled )  return p.surfaceDisabled();
            if ( selected )  return pressed ? p.primaryPressed() : rollover ? p.primaryHover() : p.primary();
            if ( pressed )   return p.surfacePressed();
            return rollover ? p.surfaceHover() : p.surfaceField();
        }

        private static void outline( Graphics2D g, Shape body, Palette p, boolean enabled, boolean focused, Color base ) {
            g.setColor(!enabled ? p.borderSoft() : focused ? p.accent() : LafUtilities.shadeTowardsBlack(base, 0.30));
            g.setStroke(new BasicStroke(Math.max(1f, UI.scale(1.1f))));
            g.draw(body);
        }

        /** The white sheen along the top edge, which is what makes a fill read as wet. */
        private static void sheen( Graphics2D g, float x, float y, float w, float h, int arc ) {
            if ( w <= 0 || h <= 0 )
                return;
            g.setColor(new Color(255, 255, 255, 120));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w, h, arc, arc));
        }

        private static void droppedArrow(
            Graphics2D g, Palette p, float cx, float cy, float size, LafUtilities.Direction direction, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            float lift = UI.scale(1f);
            g.setColor(new Color(255, 255, 255, 170));
            g.fill(LafUtilities.arrowShape(cx, cy + lift, size, size * 0.6f, direction));
            g.setColor(enabled ? LafUtilities.shadeTowardsBlack(p.accent(), 0.15) : p.textDisabled());
            g.fill(LafUtilities.arrowShape(cx, cy, size, size * 0.6f, direction));
        }

        private static void glassDot( Graphics2D g, Palette p, float cx, float cy, float radius, boolean enabled ) {
            g.setColor(new Color(255, 255, 255, 190));
            g.fill(new Ellipse2D.Float(cx - radius, cy - radius + UI.scale(1f), 2 * radius, 2 * radius));
            g.setColor(enabled ? p.border() : p.borderSoft());
            g.fill(new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius));
        }
    }

    /**
     *  Symbols for {@link SwingTreeLookAndFeel.StylePreset#MATERIAL}: bold, flat and geometric.
     *  Nothing is both outlined and filled. A control is either an outline in the muted text colour,
     *  meaning off, or a solid accent shape with the tick or the dot punched out of it in white,
     *  meaning on. Arrows are solid triangles and thumbs are plain accent shapes with no rim.
     *  <p>
     *  The metrics are the idiom's larger touch targets: taller rows, a fatter slider handle, and a
     *  scroll bar wide enough to grab.
     */
    final class Material implements Symbols
    {
        static final Symbols INSTANCE = new Material();

        private Material() {}

        @Override public boolean drawsItsOwnChrome() { return true; }

        @Override public int checkGlyphSize()        { return 18; }
        @Override public int arrowGlyphSize()        { return 14; }
        @Override public int comboArrowButtonSize()  { return 22; }
        @Override public int spinnerButtonWidth()    { return 20; }
        @Override public int spinnerButtonHeight()   { return 12; }
        @Override public int sliderThumbDiameter()   { return 16; }
        @Override public int sliderTrackThickness()  { return  4; }
        @Override public int scrollBarThickness()    { return 12; }
        @Override public int splitDividerThickness() { return  8; }
        @Override public int progressBarThickness()  { return  6; }
        @Override public int separatorThickness()    { return  1; }
        @Override public int tableRowHeight()        { return 32; }
        @Override public int treeRowHeight()         { return 28; }
        @Override public int tabPaddingVertical()    { return 10; }
        @Override public int tabPaddingHorizontal()  { return 18; }
        @Override public int tabAreaGap()            { return  2; }

        // ── Glyphs ───────────────────────────────────────────────────────────

        @Override
        public void paintCheckGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            int   arc    = UI.scale(3);
            float stroke = Math.max(1.6f, UI.scale(2f));
            Color mark   = enabled ? p.accent() : p.textDisabled();
            halo(g, p, x, y, w, h, enabled, focused, rollover, pressed);
            if ( selected ) {
                g.setColor(mark);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc));
                g.setColor(p.onFilled());
                g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(LafUtilities.tickShape(x, y, w, h));
            } else {
                g.setColor(enabled ? p.textMuted() : p.textDisabled());
                g.setStroke(new BasicStroke(stroke));
                float inset = stroke / 2f;
                g.draw(new RoundRectangle2D.Float(x + inset, y + inset, w - 1 - stroke, h - 1 - stroke, arc, arc));
            }
        }

        @Override
        public void paintRadioGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            float stroke = Math.max(1.6f, UI.scale(2f));
            float inset  = stroke / 2f;
            Color mark   = enabled ? p.accent() : p.textDisabled();
            halo(g, p, x, y, w, h, enabled, focused, rollover, pressed);
            g.setStroke(new BasicStroke(stroke));
            g.setColor(selected ? mark : ( enabled ? p.textMuted() : p.textDisabled() ));
            g.draw(new Ellipse2D.Float(x + inset, y + inset, w - 1 - stroke, h - 1 - stroke));
            if ( !selected )
                return;
            float dot = UI.scale(5f);
            g.setColor(mark);
            g.fill(new Ellipse2D.Float(x + dot, y + dot, w - 1 - 2 * dot, h - 1 - 2 * dot));
        }

        // ── Arrows ───────────────────────────────────────────────────────────

        @Override
        public void paintDisclosure(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
        ) {
            solidArrow(g, p, x + w / 2f, y + h / 2f, UI.scale(3.6f),
                       expanded ? LafUtilities.Direction.DOWN : LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
            solidArrow(g, p, x + w / 2f, y + h / 2f, UI.scale(3.4f), LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintComboArrow(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {
            solidArrow(g, p, w / 2f, h / 2f, UI.scale(4.2f), LafUtilities.Direction.DOWN, enabled);
        }

        @Override
        public void paintSpinnerArrow(
            Graphics2D g, Palette p, int w, int h, boolean up,
            boolean enabled, boolean rollover, boolean pressed
        ) {
            solidArrow(g, p, w / 2f, h / 2f, UI.scale(3.2f), up ? LafUtilities.Direction.UP : LafUtilities.Direction.DOWN, enabled);
        }

        // ── Chrome ───────────────────────────────────────────────────────────

        @Override
        public void paintSliderTrack(
            Graphics2D g, Palette p, Rectangle track, int thumbCentre,
            boolean horizontal, boolean inverted, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int   t     = Math.max(2, UI.scale(sliderTrackThickness()));
            int   arc   = t;
            Color fill  = enabled ? p.accent() : p.textDisabled();
            Color empty = enabled ? p.accentSoft() : p.surfaceDisabled();
            if ( horizontal ) {
                int y = track.y + (track.height - t) / 2;
                g.setColor(empty);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(track.x, y, track.width, t, arc, arc));
                int filled = inverted ? Math.max(0, track.x + track.width - thumbCentre)
                                      : Math.max(0, thumbCentre - track.x);
                if ( filled > 0 ) {
                    g.setColor(fill);
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(inverted ? thumbCentre : track.x, y, filled, t, arc, arc));
                }
            } else {
                int x = track.x + (track.width - t) / 2;
                g.setColor(empty);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, track.y, t, track.height, arc, arc));
                int filled = inverted ? Math.max(0, thumbCentre - track.y)
                                      : Math.max(0, track.y + track.height - thumbCentre);
                if ( filled > 0 ) {
                    g.setColor(fill);
                    OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, inverted ? track.y : thumbCentre, t, filled, arc, arc));
                }
            }
        }

        @Override
        public void paintSliderThumb( Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused, boolean rollover ) {
            LafUtilities.antialiasShapes(g);
            if ( enabled && ( focused || rollover ) ) {
                // The halo a Material handle grows under the pointer, and keeps while it has focus.
                g.setColor(LafUtilities.withOpacity(p.accent(), 46));
                float grow = UI.scale(5f);
                g.fill(new Ellipse2D.Float(r.x - grow, r.y - grow, r.width + 2 * grow, r.height + 2 * grow));
            }
            g.setColor(enabled ? p.accent() : p.textDisabled());
            g.fill(new Ellipse2D.Float(r.x, r.y, r.width - 1, r.height - 1));
        }

        @Override
        public void paintScrollThumb( Graphics2D g, Palette p, Rectangle r, boolean active ) {
            LafUtilities.antialiasShapes(g);
            int pad = UI.scale(3);
            int arc = Math.min(r.width, r.height) - 2 * pad;
            g.setColor(active ? p.accent() : LafUtilities.withOpacity(p.text(), 70));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad, arc, arc));
        }

        @Override
        public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
            LafUtilities.antialiasShapes(g);
            g.setColor(p.borderSoft());
            if ( horizontalSplit )
                g.fillRect(w / 2, 0, Math.max(1, UI.scale(1)), h);
            else
                g.fillRect(0, h / 2, w, Math.max(1, UI.scale(1)));

            g.setColor(enabled ? LafUtilities.withOpacity(p.text(), 90) : p.borderSoft());
            float radius = UI.scale(1.5f);
            int   step   = UI.scale(5);
            for ( int i = 0; i < 3; i++ ) {
                float cx = horizontalSplit ? w / 2f                   : w / 2f - step + i * step;
                float cy = horizontalSplit ? h / 2f - step + i * step : h / 2f;
                g.fill(new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius));
            }
        }

        @Override
        public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {
            LafUtilities.antialiasShapes(g);
            g.setColor(LafUtilities.withOpacity(p.text(), 70));
            float radius = UI.scale(1.4f);
            int   step   = UI.scale(4);
            for ( int i = 0; i < 3; i++ ) {
                float cx = horizontal ? UI.scale(5)                : w / 2f - step + i * step;
                float cy = horizontal ? h / 2f - step + i * step   : UI.scale(5);
                g.fill(new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius));
            }
        }

        @Override
        public void paintProgressFill(
            Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
        ) {
            if ( ratio <= 0 )
                return;
            LafUtilities.antialiasShapes(g);
            g.setColor(enabled ? p.accent() : p.textDisabled());
            if ( horizontal ) {
                int fillW = Math.max(h, (int) Math.round(w * ratio));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(0, 0, fillW, h, h, h));
            } else {
                int fillH = Math.max(w, (int) Math.round(h * ratio));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(0, h - fillH, w, fillH, w, w));
            }
        }

        @Override
        public void paintTabSurface(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
        ) {
            // A selected tab is said with the rule underneath it, not with a fill; only the pointer
            // gets a wash, which is the closest a static paint comes to the idiom's ripple.
            if ( !rollover )
                return;
            LafUtilities.antialiasShapes(g);
            g.setColor(LafUtilities.withOpacity(p.accent(), 26));
            g.fillRect(x, y, w, h);
        }

        @Override
        public void paintTabAccent(
            Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int stripe = Math.max(2, UI.scale(3));
            g.setColor(enabled ? p.accent() : p.textDisabled());
            switch ( tabPlacement ) {
                case SwingConstants.BOTTOM: g.fillRect(x, y, w, stripe);                  break;
                case SwingConstants.LEFT:   g.fillRect(x + w - stripe, y, stripe, h);     break;
                case SwingConstants.RIGHT:  g.fillRect(x, y, stripe, h);                  break;
                case SwingConstants.TOP:
                default:                    g.fillRect(x, y + h - stripe, w, stripe);     break;
            }
        }

        // ── Internals ────────────────────────────────────────────────────────

        /** The translucent disc a check box or radio grows under the pointer. */
        private static void halo(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed
        ) {
            if ( !enabled || !( focused || rollover || pressed ) )
                return;
            g.setColor(LafUtilities.withOpacity(p.accent(), pressed ? 56 : 30));
            float grow = UI.scale(5f);
            g.fill(new Ellipse2D.Float(x - grow, y - grow, w + 2 * grow, h + 2 * grow));
        }

        private static void solidArrow(
            Graphics2D g, Palette p, float cx, float cy, float size, LafUtilities.Direction direction, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            g.setColor(enabled ? p.textMuted() : p.textDisabled());
            g.fill(LafUtilities.arrowShape(cx, cy, size, size * 0.62f, direction));
        }
    }

    /**
     *  Symbols for {@link SwingTreeLookAndFeel.StylePreset#FLAT}: rectangles and solid triangles,
     *  with no radius, no rim, no halo and no shade anywhere. A control that is off is a two-pixel
     *  outline in the border colour, and one that is on is the same shape filled with the accent and
     *  its mark punched out in white. Only the radio button is left round.
     *
     *  @see SwingTreeLookAndFeel.SymbolPreset#FLAT
     */
    final class Flat implements Symbols
    {
        static final Symbols INSTANCE = new Flat();

        private Flat() {}

        @Override public boolean drawsItsOwnChrome() { return true; }

        @Override public int checkGlyphSize()        { return 16; }
        @Override public int arrowGlyphSize()        { return 12; }
        @Override public int comboArrowButtonSize()  { return 20; }
        @Override public int spinnerButtonWidth()    { return 18; }
        @Override public int spinnerButtonHeight()   { return 11; }
        @Override public int sliderThumbDiameter()   { return 14; }
        @Override public int sliderTrackThickness()  { return  4; }
        @Override public int scrollBarThickness()    { return 12; }
        @Override public int splitDividerThickness() { return  6; }
        @Override public int progressBarThickness()  { return  6; }
        @Override public int separatorThickness()    { return  1; }
        @Override public int tableRowHeight()        { return 26; }
        @Override public int treeRowHeight()         { return 22; }
        @Override public int tabPaddingVertical()    { return  8; }
        @Override public int tabPaddingHorizontal()  { return 16; }
        @Override public int tabAreaGap()            { return  2; }

        // ── Glyphs ───────────────────────────────────────────────────────────

        @Override
        public void paintCheckGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            int stroke = Math.max(1, UI.scale(2));
            if ( selected ) {
                g.setColor(mark(p, enabled, rollover));
                g.fillRect(x, y, w, h);
                g.setColor(p.onFilled());
                g.setStroke(new BasicStroke(Math.max(1.6f, UI.scale(2.2f)), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                g.draw(LafUtilities.tickShape(x, y, w, h));
            } else {
                g.setColor(outline(p, enabled, rollover));
                g.fillRect(x, y, w, stroke);
                g.fillRect(x, y + h - stroke, w, stroke);
                g.fillRect(x, y, stroke, h);
                g.fillRect(x + w - stroke, y, stroke, h);
            }
        }

        @Override
        public void paintRadioGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            float stroke = Math.max(1.6f, UI.scale(2f));
            float inset  = stroke / 2f;
            g.setStroke(new BasicStroke(stroke));
            g.setColor(selected ? mark(p, enabled, rollover) : outline(p, enabled, rollover));
            g.draw(new Ellipse2D.Float(x + inset, y + inset, w - 1 - stroke, h - 1 - stroke));
            if ( !selected )
                return;
            float dot = UI.scale(4f);
            g.setColor(mark(p, enabled, rollover));
            g.fill(new Ellipse2D.Float(x + dot, y + dot, w - 1 - 2 * dot, h - 1 - 2 * dot));
        }

        // ── Arrows ───────────────────────────────────────────────────────────

        @Override
        public void paintDisclosure(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
        ) {
            wedge(g, p, x + w / 2f, y + h / 2f, UI.scale(3.4f),
                  expanded ? LafUtilities.Direction.DOWN : LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
            wedge(g, p, x + w / 2f, y + h / 2f, UI.scale(3.2f), LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintComboArrow(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {
            wedge(g, p, w / 2f, h / 2f, UI.scale(4f), LafUtilities.Direction.DOWN, enabled);
        }

        @Override
        public void paintSpinnerArrow(
            Graphics2D g, Palette p, int w, int h, boolean up,
            boolean enabled, boolean rollover, boolean pressed
        ) {
            wedge(g, p, w / 2f, h / 2f, UI.scale(3f),
                  up ? LafUtilities.Direction.UP : LafUtilities.Direction.DOWN, enabled);
        }

        // ── Chrome ───────────────────────────────────────────────────────────

        @Override
        public void paintSliderTrack(
            Graphics2D g, Palette p, Rectangle track, int thumbCentre,
            boolean horizontal, boolean inverted, boolean enabled
        ) {
            int   t     = Math.max(2, UI.scale(sliderTrackThickness()));
            Color fill  = enabled ? p.accent() : p.textDisabled();
            Color empty = enabled ? p.accentSoft() : p.surfaceDisabled();
            if ( horizontal ) {
                int y = track.y + (track.height - t) / 2;
                g.setColor(empty);
                g.fillRect(track.x, y, track.width, t);
                int filled = inverted ? Math.max(0, track.x + track.width - thumbCentre)
                                      : Math.max(0, thumbCentre - track.x);
                g.setColor(fill);
                g.fillRect(inverted ? thumbCentre : track.x, y, filled, t);
            } else {
                int x = track.x + (track.width - t) / 2;
                g.setColor(empty);
                g.fillRect(x, track.y, t, track.height);
                int filled = inverted ? Math.max(0, thumbCentre - track.y)
                                      : Math.max(0, track.y + track.height - thumbCentre);
                g.setColor(fill);
                g.fillRect(x, inverted ? track.y : thumbCentre, t, filled);
            }
        }

        /** A bar rather than a knob, because a knob would need a rim to read as one. */
        @Override
        public void paintSliderThumb( Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused, boolean rollover ) {
            g.setColor(enabled ? ( focused || rollover ? p.accent() : p.text() ) : p.textDisabled());
            int narrow = Math.max(2, r.width / 3);
            g.fillRect(r.x + (r.width - narrow) / 2, r.y, narrow, r.height);
        }

        @Override
        public void paintScrollThumb( Graphics2D g, Palette p, Rectangle r, boolean active ) {
            int pad = UI.scale(3);
            g.setColor(active ? p.accent() : LafUtilities.withOpacity(p.text(), 90));
            g.fillRect(r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad);
        }

        @Override
        public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
            g.setColor(p.borderSoft());
            if ( horizontalSplit )
                g.fillRect(w / 2, 0, Math.max(1, UI.scale(1)), h);
            else
                g.fillRect(0, h / 2, w, Math.max(1, UI.scale(1)));
            dots(g, p, w, h, horizontalSplit, UI.scale(5));
        }

        @Override
        public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {
            int size = Math.max(2, UI.scale(2));
            g.setColor(LafUtilities.withOpacity(p.text(), 90));
            for ( int i = 0; i < 4; i++ ) {
                int step = i * UI.scale(4);
                if ( horizontal )
                    g.fillRect(UI.scale(4), h / 2 - UI.scale(6) + step, size, size);
                else
                    g.fillRect(w / 2 - UI.scale(6) + step, UI.scale(4), size, size);
            }
        }

        @Override
        public void paintProgressFill(
            Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
        ) {
            if ( ratio <= 0 )
                return;
            g.setColor(enabled ? p.accent() : p.textDisabled());
            if ( horizontal )
                g.fillRect(0, 0, (int) Math.round(w * ratio), h);
            else {
                int fillH = (int) Math.round(h * ratio);
                g.fillRect(0, h - fillH, w, fillH);
            }
        }

        @Override
        public void paintTabSurface(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
        ) {
            if ( selected )
                g.setColor(p.surface());
            else if ( rollover )
                g.setColor(p.accentSoft());
            else
                return;
            g.fillRect(x, y, w, h);
        }

        @Override
        public void paintTabAccent(
            Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
        ) {
            int stripe = Math.max(2, UI.scale(3));
            g.setColor(enabled ? p.accent() : p.textDisabled());
            switch ( tabPlacement ) {
                case SwingConstants.BOTTOM: g.fillRect(x, y, w, stripe);              break;
                case SwingConstants.LEFT:   g.fillRect(x + w - stripe, y, stripe, h); break;
                case SwingConstants.RIGHT:  g.fillRect(x, y, stripe, h);              break;
                case SwingConstants.TOP:
                default:                    g.fillRect(x, y + h - stripe, w, stripe); break;
            }
        }

        // ── Internals ────────────────────────────────────────────────────────

        private static Color mark( Palette p, boolean enabled, boolean rollover ) {
            if ( !enabled )
                return p.textDisabled();
            return rollover ? p.primaryHover() : p.accent();
        }

        private static Color outline( Palette p, boolean enabled, boolean rollover ) {
            if ( !enabled )
                return p.textDisabled();
            return rollover ? p.accent() : p.border();
        }

        private static void wedge(
            Graphics2D g, Palette p, float cx, float cy, float size, LafUtilities.Direction direction, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            g.setColor(enabled ? p.text() : p.textDisabled());
            g.fill(LafUtilities.arrowShape(cx, cy, size, size * 0.55f, direction));
        }

        private static void dots( Graphics2D g, Palette p, int w, int h, boolean vertical, int step ) {
            int size = Math.max(2, UI.scale(2));
            g.setColor(LafUtilities.withOpacity(p.text(), 90));
            for ( int i = 0; i < 3; i++ ) {
                int x = vertical ? w / 2 - size / 2                : w / 2 - step + i * step - size / 2;
                int y = vertical ? h / 2 - step + i * step - size / 2 : h / 2 - size / 2;
                g.fillRect(x, y, size, size);
            }
        }
    }

    /**
     *  Symbols for {@link SwingTreeLookAndFeel.StylePreset#SKEUOMORPHIC}: everything is either cut
     *  into the surface or screwed onto it. A mark cut in is drawn twice, dark on the line itself and
     *  light one pixel below it where the far wall of the groove catches the light; a mark standing
     *  on the surface is the same two copies the other way round. That, and a vertical gradient on
     *  anything wide enough to show one, is what every glyph here is made of.
     *
     *  @see SwingTreeLookAndFeel.SymbolPreset#CARVED
     */
    final class Carved implements Symbols
    {
        static final Symbols INSTANCE = new Carved();

        private Carved() {}

        /** How opaque the light that spills over the far lip of a groove is. */
        private static final int LIP = 150;
        /** How opaque the shadow inside a groove is. */
        private static final int GROOVE = 120;

        @Override public boolean drawsItsOwnChrome() { return true; }

        @Override public int checkGlyphSize()        { return 16; }
        @Override public int arrowGlyphSize()        { return 13; }
        @Override public int comboArrowButtonSize()  { return 20; }
        @Override public int spinnerButtonWidth()    { return 18; }
        @Override public int spinnerButtonHeight()   { return 11; }
        @Override public int sliderThumbDiameter()   { return 17; }
        @Override public int sliderTrackThickness()  { return  6; }
        @Override public int scrollBarThickness()    { return 14; }
        @Override public int splitDividerThickness() { return  8; }
        @Override public int progressBarThickness()  { return 12; }
        @Override public int separatorThickness()    { return  1; }
        @Override public int tableRowHeight()        { return 24; }
        @Override public int treeRowHeight()         { return 22; }
        @Override public int tabPaddingVertical()    { return  7; }
        @Override public int tabPaddingHorizontal()  { return 15; }
        @Override public int tabAreaGap()            { return  2; }

        // ── Glyphs ───────────────────────────────────────────────────────────

        @Override
        public void paintCheckGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            int   arc  = UI.scale(4);
            Color face = enabled ? ( selected ? p.accentSoft() : p.surfaceField() ) : p.surfaceDisabled();
            RoundRectangle2D.Float box = new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc);

            // The lip of light under the whole recess, drawn first so the recess sits on top of it.
            g.setColor(LafUtilities.withOpacity(Color.WHITE, LIP));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y + 1, w - 1, h - 1, arc, arc));

            g.setPaint(LafUtilities.verticalGradient(y, h, LafUtilities.shadeBySteps(face, -14), face));
            g.fill(box);
            g.setColor(enabled ? p.border() : p.textDisabled());
            g.setStroke(new BasicStroke(1f));
            g.draw(box);
            if ( !selected )
                return;
            engrave(g, enabled ? p.text() : p.textDisabled(), () -> {
                g.setStroke(new BasicStroke(Math.max(1.6f, UI.scale(2.1f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(LafUtilities.tickShape(x, y, w, h));
            });
        }

        @Override
        public void paintRadioGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            Color face = enabled ? p.surfaceField() : p.surfaceDisabled();
            g.setColor(LafUtilities.withOpacity(Color.WHITE, LIP));
            g.fill(new Ellipse2D.Float(x, y + 1, w - 1, h - 1));
            g.setPaint(LafUtilities.verticalGradient(y, h, LafUtilities.shadeBySteps(face, -14), face));
            g.fill(new Ellipse2D.Float(x, y, w - 1, h - 1));
            g.setColor(enabled ? p.border() : p.textDisabled());
            g.setStroke(new BasicStroke(1f));
            g.draw(new Ellipse2D.Float(x, y, w - 1, h - 1));
            if ( !selected )
                return;
            float dot = UI.scale(4f);
            Color bead = enabled ? p.accent() : p.textDisabled();
            g.setPaint(LafUtilities.glossGradient(y + dot, h - 2 * dot, bead));
            g.fill(new Ellipse2D.Float(x + dot, y + dot, w - 1 - 2 * dot, h - 1 - 2 * dot));
        }

        // ── Arrows ───────────────────────────────────────────────────────────

        @Override
        public void paintDisclosure(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
        ) {
            embossedArrow(g, p, x + w / 2f, y + h / 2f, UI.scale(3.4f),
                          expanded ? LafUtilities.Direction.DOWN : LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
            embossedArrow(g, p, x + w / 2f, y + h / 2f, UI.scale(3.2f), LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintComboArrow(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {
            embossedArrow(g, p, w / 2f, h / 2f + ( pressed ? 1 : 0 ), UI.scale(4f),
                          LafUtilities.Direction.DOWN, enabled);
        }

        @Override
        public void paintSpinnerArrow(
            Graphics2D g, Palette p, int w, int h, boolean up,
            boolean enabled, boolean rollover, boolean pressed
        ) {
            embossedArrow(g, p, w / 2f, h / 2f + ( pressed ? 1 : 0 ), UI.scale(3f),
                          up ? LafUtilities.Direction.UP : LafUtilities.Direction.DOWN, enabled);
        }

        // ── Chrome ───────────────────────────────────────────────────────────

        @Override
        public void paintSliderTrack(
            Graphics2D g, Palette p, Rectangle track, int thumbCentre,
            boolean horizontal, boolean inverted, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int   t    = Math.max(3, UI.scale(sliderTrackThickness()));
            int   arc  = t;
            Color fill = enabled ? p.accent() : p.textDisabled();
            if ( horizontal ) {
                int y = track.y + (track.height - t) / 2;
                groove(g, track.x, y, track.width, t, arc, p);
                int filled = inverted ? Math.max(0, track.x + track.width - thumbCentre)
                                      : Math.max(0, thumbCentre - track.x);
                if ( filled <= 0 )
                    return;
                g.setPaint(LafUtilities.glossGradient(y, t, fill));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(inverted ? thumbCentre : track.x, y, filled, t, arc, arc));
            } else {
                int x = track.x + (track.width - t) / 2;
                groove(g, x, track.y, t, track.height, arc, p);
                int filled = inverted ? Math.max(0, thumbCentre - track.y)
                                      : Math.max(0, track.y + track.height - thumbCentre);
                if ( filled <= 0 )
                    return;
                g.setColor(fill);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, inverted ? track.y : thumbCentre, t, filled, arc, arc));
            }
        }

        @Override
        public void paintSliderThumb( Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused, boolean rollover ) {
            LafUtilities.antialiasShapes(g);
            knob(g, p, r, enabled, focused, rollover);
        }

        @Override
        public void paintScrollThumb( Graphics2D g, Palette p, Rectangle r, boolean active ) {
            LafUtilities.antialiasShapes(g);
            int pad = UI.scale(2);
            Rectangle body = new Rectangle(r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad);
            int arc = Math.min(body.width, body.height);
            Color base = active ? p.accent() : p.surface();
            g.setColor(LafUtilities.withOpacity(Color.BLACK, 60));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(body.x, body.y + 1, body.width, body.height, arc, arc));
            g.setPaint(LafUtilities.glossGradient(body.y, body.height, base));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(body.x, body.y, body.width, body.height, arc, arc));
            g.setColor(p.border());
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Float(body.x, body.y, body.width - 1, body.height - 1, arc, arc));
        }

        @Override
        public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
            LafUtilities.antialiasShapes(g);
            int step = UI.scale(4);
            for ( int i = 0; i < 3; i++ ) {
                float cx = horizontalSplit ? w / 2f                   : w / 2f - step + i * step;
                float cy = horizontalSplit ? h / 2f - step + i * step : h / 2f;
                engravedDot(g, cx, cy, UI.scale(1.4f));
            }
        }

        @Override
        public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {
            LafUtilities.antialiasShapes(g);
            int step = UI.scale(4);
            for ( int i = 0; i < 3; i++ ) {
                float cx = horizontal ? UI.scale(5)                  : w / 2f - step + i * step;
                float cy = horizontal ? h / 2f - step + i * step     : UI.scale(5);
                engravedDot(g, cx, cy, UI.scale(1.4f));
            }
        }

        @Override
        public void paintProgressFill(
            Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
        ) {
            if ( ratio <= 0 )
                return;
            LafUtilities.antialiasShapes(g);
            Color base = enabled ? p.accent() : p.textDisabled();
            if ( horizontal ) {
                int fillW = Math.max(h, (int) Math.round(w * ratio));
                g.setPaint(LafUtilities.glossGradient(0, h, base));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(0, 0, fillW, h, h, h));
            } else {
                int fillH = Math.max(w, (int) Math.round(h * ratio));
                g.setColor(base);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(0, h - fillH, w, fillH, w, w));
            }
        }

        @Override
        public void paintTabSurface(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
        ) {
            LafUtilities.antialiasShapes(g);
            if ( !selected && !rollover )
                return;
            Color base = selected ? p.surface() : p.surfaceHover();
            int   arc  = UI.scale(5);
            g.setColor(LafUtilities.withOpacity(Color.BLACK, selected ? 60 : 30));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y + 1, w, h, arc, arc));
            g.setPaint(LafUtilities.verticalGradient(y, h, LafUtilities.shadeBySteps(base, 16), base));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w, h, arc, arc));
        }

        @Override
        public void paintTabAccent(
            Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
        ) {
            int stripe = Math.max(2, UI.scale(3));
            g.setColor(enabled ? p.accent() : p.textDisabled());
            switch ( tabPlacement ) {
                case SwingConstants.BOTTOM: g.fillRect(x, y, w, stripe);              break;
                case SwingConstants.LEFT:   g.fillRect(x + w - stripe, y, stripe, h); break;
                case SwingConstants.RIGHT:  g.fillRect(x, y, stripe, h);              break;
                case SwingConstants.TOP:
                default:                    g.fillRect(x, y + h - stripe, w, stripe); break;
            }
        }

        // ── Internals ────────────────────────────────────────────────────────

        /** A hole with a dark wall along the top and the light spilling over the far lip. */
        private static void groove( Graphics2D g, int x, int y, int w, int h, int arc, Palette p ) {
            g.setColor(LafUtilities.withOpacity(Color.WHITE, LIP));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y + 1, w, h, arc, arc));
            g.setPaint(LafUtilities.verticalGradient(y, h,
                            LafUtilities.withOpacity(Color.BLACK, GROOVE), p.surfaceDisabled()));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w, h, arc, arc));
        }

        /** A milled knob: a gloss down its face, a dark rim, and its own shadow underneath. */
        private static void knob(
            Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused, boolean rollover
        ) {
            Color base = enabled ? p.surface() : p.surfaceDisabled();
            if ( enabled && rollover )
                base = LafUtilities.underPointer(p, base);
            g.setColor(LafUtilities.withOpacity(Color.BLACK, 80));
            g.fill(new Ellipse2D.Float(r.x, r.y + 1.5f, r.width - 1, r.height - 1));
            g.setPaint(LafUtilities.glossGradient(r.y, r.height, base));
            g.fill(new Ellipse2D.Float(r.x, r.y, r.width - 1, r.height - 1));
            g.setStroke(new BasicStroke(1f));
            g.setColor(focused ? p.accent() : p.border());
            g.draw(new Ellipse2D.Float(r.x, r.y, r.width - 1, r.height - 1));
        }

        /** Draws a mark twice: light one pixel below the line, then the line itself. */
        private static void engrave( Graphics2D g, Color ink, Runnable mark ) {
            g.translate(0, 1);
            g.setColor(LafUtilities.withOpacity(Color.WHITE, LIP));
            mark.run();
            g.translate(0, -1);
            g.setColor(ink);
            mark.run();
        }

        private static void engravedDot( Graphics2D g, float cx, float cy, float radius ) {
            g.setColor(LafUtilities.withOpacity(Color.WHITE, LIP));
            g.fill(new Ellipse2D.Float(cx - radius, cy - radius + 1, 2 * radius, 2 * radius));
            g.setColor(LafUtilities.withOpacity(Color.BLACK, GROOVE));
            g.fill(new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius));
        }

        private static void embossedArrow(
            Graphics2D g, Palette p, float cx, float cy, float size, LafUtilities.Direction direction, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            g.setColor(LafUtilities.withOpacity(Color.WHITE, LIP));
            g.fill(LafUtilities.arrowShape(cx, cy + 1, size, size * 0.6f, direction));
            g.setColor(enabled ? p.text() : p.textDisabled());
            g.fill(LafUtilities.arrowShape(cx, cy, size, size * 0.6f, direction));
        }
    }

    /**
     *  Symbols for {@link SwingTreeLookAndFeel.StylePreset#GLASSMORPHIC}: small pieces of the same
     *  glass everything else is cut from. A shape that is off is a wash of white you can see the
     *  ground through, and one that is on is the accent at about three quarters with a brighter rim
     *  along its edge. The marks themselves - the tick, the dot, the arrows - are the one thing
     *  painted at full strength.
     *
     *  @see SwingTreeLookAndFeel.SymbolPreset#GLASS
     */
    final class Glass implements Symbols
    {
        static final Symbols INSTANCE = new Glass();

        private Glass() {}

        /** How much white an unlit piece of glass carries, out of 255. */
        private static final int PANE = 44;
        /** How opaque a lit piece is. */
        private static final int LIT  = 190;
        /** How bright the rim along a piece's edge is. */
        private static final int RIM  = 120;

        @Override public boolean drawsItsOwnChrome() { return true; }

        @Override public int checkGlyphSize()        { return 17; }
        @Override public int arrowGlyphSize()        { return 13; }
        @Override public int comboArrowButtonSize()  { return 22; }
        @Override public int spinnerButtonWidth()    { return 20; }
        @Override public int spinnerButtonHeight()   { return 12; }
        @Override public int sliderThumbDiameter()   { return 18; }
        @Override public int sliderTrackThickness()  { return  6; }
        @Override public int scrollBarThickness()    { return 13; }
        @Override public int splitDividerThickness() { return  8; }
        @Override public int progressBarThickness()  { return  8; }
        @Override public int separatorThickness()    { return  1; }
        @Override public int tableRowHeight()        { return 28; }
        @Override public int treeRowHeight()         { return 24; }
        @Override public int tabPaddingVertical()    { return  9; }
        @Override public int tabPaddingHorizontal()  { return 18; }
        @Override public int tabAreaGap()            { return  4; }

        // ── Glyphs ───────────────────────────────────────────────────────────

        @Override
        public void paintCheckGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            int   arc = UI.scale(5);
            Color face = face(p, enabled, selected, rollover);
            RoundRectangle2D.Float box = new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc);
            g.setColor(face);
            g.fill(box);
            rim(g, p, box, enabled, focused);
            if ( !selected )
                return;
            g.setColor(enabled ? p.onFilled() : p.textDisabled());
            g.setStroke(new BasicStroke(Math.max(1.6f, UI.scale(2f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(LafUtilities.tickShape(x, y, w, h));
        }

        @Override
        public void paintRadioGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            LafUtilities.antialiasShapes(g);
            Ellipse2D.Float disc = new Ellipse2D.Float(x, y, w - 1, h - 1);
            g.setColor(face(p, enabled, selected, rollover));
            g.fill(disc);
            rim(g, p, disc, enabled, focused);
            if ( !selected )
                return;
            float dot = UI.scale(4.5f);
            g.setColor(enabled ? p.onFilled() : p.textDisabled());
            g.fill(new Ellipse2D.Float(x + dot, y + dot, w - 1 - 2 * dot, h - 1 - 2 * dot));
        }

        // ── Arrows ───────────────────────────────────────────────────────────

        @Override
        public void paintDisclosure(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
        ) {
            mark(g, p, x + w / 2f, y + h / 2f, UI.scale(3.4f),
                 expanded ? LafUtilities.Direction.DOWN : LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
            mark(g, p, x + w / 2f, y + h / 2f, UI.scale(3.2f), LafUtilities.Direction.RIGHT, enabled);
        }

        @Override
        public void paintComboArrow(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {
            mark(g, p, w / 2f, h / 2f, UI.scale(4f), LafUtilities.Direction.DOWN, enabled);
        }

        @Override
        public void paintSpinnerArrow(
            Graphics2D g, Palette p, int w, int h, boolean up,
            boolean enabled, boolean rollover, boolean pressed
        ) {
            mark(g, p, w / 2f, h / 2f, UI.scale(3f),
                 up ? LafUtilities.Direction.UP : LafUtilities.Direction.DOWN, enabled);
        }

        // ── Chrome ───────────────────────────────────────────────────────────

        @Override
        public void paintSliderTrack(
            Graphics2D g, Palette p, Rectangle track, int thumbCentre,
            boolean horizontal, boolean inverted, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int   t     = Math.max(3, UI.scale(sliderTrackThickness()));
            int   arc   = t;
            Color fill  = LafUtilities.withOpacity(enabled ? p.accent() : p.textDisabled(), LIT);
            Color empty = LafUtilities.withOpacity(p.surface(), PANE);
            if ( horizontal ) {
                int y = track.y + (track.height - t) / 2;
                g.setColor(empty);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(track.x, y, track.width, t, arc, arc));
                int filled = inverted ? Math.max(0, track.x + track.width - thumbCentre)
                                      : Math.max(0, thumbCentre - track.x);
                if ( filled <= 0 )
                    return;
                g.setColor(fill);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(inverted ? thumbCentre : track.x, y, filled, t, arc, arc));
            } else {
                int x = track.x + (track.width - t) / 2;
                g.setColor(empty);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, track.y, t, track.height, arc, arc));
                int filled = inverted ? Math.max(0, thumbCentre - track.y)
                                      : Math.max(0, track.y + track.height - thumbCentre);
                if ( filled <= 0 )
                    return;
                g.setColor(fill);
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, inverted ? track.y : thumbCentre, t, filled, arc, arc));
            }
        }

        @Override
        public void paintSliderThumb( Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused, boolean rollover ) {
            LafUtilities.antialiasShapes(g);
            Ellipse2D.Float bead = new Ellipse2D.Float(r.x, r.y, r.width - 1, r.height - 1);
            g.setColor(LafUtilities.withOpacity(Color.BLACK, 70));
            g.fill(new Ellipse2D.Float(r.x, r.y + 2, r.width - 1, r.height - 1));
            g.setColor(LafUtilities.withOpacity(enabled ? p.surface() : p.surfaceDisabled(),
                                                enabled && rollover ? 190 : 150));
            g.fill(bead);
            rim(g, p, bead, enabled, focused);
        }

        @Override
        public void paintScrollThumb( Graphics2D g, Palette p, Rectangle r, boolean active ) {
            LafUtilities.antialiasShapes(g);
            int pad = UI.scale(3);
            int arc = Math.min(r.width, r.height) - 2 * pad;
            g.setColor(LafUtilities.withOpacity(active ? p.accent() : p.surface(), active ? LIT : 96));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad, arc, arc));
        }

        @Override
        public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
            LafUtilities.antialiasShapes(g);
            g.setColor(LafUtilities.withOpacity(p.border(), 60));
            if ( horizontalSplit )
                g.fillRect(w / 2, 0, Math.max(1, UI.scale(1)), h);
            else
                g.fillRect(0, h / 2, w, Math.max(1, UI.scale(1)));
            beads(g, p, w, h, horizontalSplit, UI.scale(5));
        }

        @Override
        public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {
            LafUtilities.antialiasShapes(g);
            beads(g, p, horizontal ? UI.scale(10) : w, horizontal ? h : UI.scale(10), horizontal, UI.scale(4));
        }

        @Override
        public void paintProgressFill(
            Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
        ) {
            if ( ratio <= 0 )
                return;
            LafUtilities.antialiasShapes(g);
            g.setColor(LafUtilities.withOpacity(enabled ? p.accent() : p.textDisabled(), LIT));
            if ( horizontal ) {
                int fillW = Math.max(h, (int) Math.round(w * ratio));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(0, 0, fillW, h, h, h));
            } else {
                int fillH = Math.max(w, (int) Math.round(h * ratio));
                OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(0, h - fillH, w, fillH, w, w));
            }
        }

        @Override
        public void paintTabSurface(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
        ) {
            if ( !selected && !rollover )
                return;
            LafUtilities.antialiasShapes(g);
            int arc = UI.scale(10);
            g.setColor(LafUtilities.withOpacity(p.surface(), selected ? 58 : 28));
            OptimizedShapeRendering.fill(g, new RoundRectangle2D.Float(x, y, w, h, arc, arc));
        }

        @Override
        public void paintTabAccent(
            Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            int stripe = Math.max(2, UI.scale(3));
            g.setColor(LafUtilities.withOpacity(enabled ? p.accent() : p.textDisabled(), LIT));
            switch ( tabPlacement ) {
                case SwingConstants.BOTTOM: g.fillRect(x, y, w, stripe);              break;
                case SwingConstants.LEFT:   g.fillRect(x + w - stripe, y, stripe, h); break;
                case SwingConstants.RIGHT:  g.fillRect(x, y, stripe, h);              break;
                case SwingConstants.TOP:
                default:                    g.fillRect(x, y + h - stripe, w, stripe); break;
            }
        }

        // ── Internals ────────────────────────────────────────────────────────

        private static Color face( Palette p, boolean enabled, boolean selected, boolean rollover ) {
            if ( !enabled )
                return LafUtilities.withOpacity(p.surfaceDisabled(), 40);
            if ( selected )
                return LafUtilities.withOpacity(p.accent(), LIT);
            return LafUtilities.withOpacity(p.surface(), rollover ? PANE + 30 : PANE);
        }

        /** The hairline of brighter glass along a piece's edge, and the accent when it has focus. */
        private static void rim( Graphics2D g, Palette p, Shape shape, boolean enabled, boolean focused ) {
            g.setStroke(new BasicStroke(focused ? Math.max(1.4f, UI.scale(2f)) : 1f));
            g.setColor(focused ? LafUtilities.withOpacity(p.accent(), 220)
                               : LafUtilities.withOpacity(p.border(), enabled ? RIM : 50));
            g.draw(shape);
        }

        private static void mark(
            Graphics2D g, Palette p, float cx, float cy, float size, LafUtilities.Direction direction, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            g.setColor(enabled ? p.text() : p.textDisabled());
            g.fill(LafUtilities.arrowShape(cx, cy, size, size * 0.62f, direction));
        }

        private static void beads( Graphics2D g, Palette p, int w, int h, boolean vertical, int step ) {
            float radius = UI.scale(1.5f);
            g.setColor(LafUtilities.withOpacity(p.surface(), 130));
            for ( int i = 0; i < 3; i++ ) {
                float cx = vertical ? w / 2f                   : w / 2f - step + i * step;
                float cy = vertical ? h / 2f - step + i * step : h / 2f;
                g.fill(new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius));
            }
        }
    }

    /**
     *  Symbols for {@link SwingTreeLookAndFeel.StylePreset#NIMBUS}: the shapes no style rule can
     *  express, redrawn from the painters the JDK generates for Nimbus. Each is drawn in the fixed
     *  square or strip Nimbus designs it in and scaled to the box it is handed, and each takes its
     *  colours from a {@link NimbusScheme}, so it follows a re-tinted {@code nimbusBase} the way the
     *  style rules do.
     *  <p>
     *  Where Nimbus draws a control partly with a style and partly with a glyph, the glyph reaches as
     *  far as Nimbus's painter does: a combo box's blue end covers the combo box's own edge, and a
     *  scroll bar's buttons curve down into its groove.
     *
     *  @see SwingTreeLookAndFeel.SymbolPreset#NIMBUS
     */
    final class Nimbus implements Symbols
    {
        private final NimbusScheme _scheme;

        /** @param scheme the Nimbus colours of the palette this set will be drawn with */
        Nimbus( NimbusScheme scheme ) { _scheme = scheme; }

        /** @return the Nimbus colours of {@code p}, which are this set's own unless it is handed a
         *          palette other than the one it was made for */
        private NimbusScheme schemeOf( Palette p ) { return _scheme.withPalette(p); }



        @Override public boolean drawsItsOwnChrome() { return true; }

        // The metrics the original lays out with, read out of its own UIDefaults.
        @Override public int checkGlyphSize()        { return 18; }
        @Override public int arrowGlyphSize()        { return 12; }
        // Nimbus's own number is 19, which is this end plus the two pixels of room its focus ring
        // and lip need on the right - room that is the control's margin here, and is already kept
        // outside the button by the box the button is laid out in.
        @Override public int comboArrowButtonSize()  { return 17; }
        @Override public int spinnerButtonWidth()    { return 20; }
        @Override public int spinnerButtonHeight()   { return 14; }
        @Override public int sliderThumbDiameter()   { return 17; }
        @Override public int sliderTrackThickness()  { return  5; }
        @Override public int scrollBarThickness()    { return 15; }
        @Override public int splitDividerThickness() { return 10; }
        @Override public int progressBarThickness()  { return 19; }
        @Override public int separatorThickness()    { return  1; }
        @Override public int tableRowHeight()        { return 16; }
        @Override public int treeRowHeight()         { return 20; }
        @Override public int tabPaddingVertical()    { return  2; }
        @Override public int tabPaddingHorizontal()  { return  9; }
        @Override public int tabAreaGap()            { return  2; }

        // ── Glyphs in front of a label ───────────────────────────────────────

        /**
         *  Nimbus's check box: a rounded box laid out in an eighteen pixel square and filled the
         *  way a button is, with a heavy black tick cut as one shape rather than stroked.
         */
        @Override
        public void paintCheckGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            NimbusScheme s = schemeOf(p);
            Glyph glyph = !enabled ? ( selected ? Glyph.CHECK_DISABLED_SELECTED : Glyph.CHECK_DISABLED )
                        : pressed  ? ( selected ? Glyph.CHECK_PRESSED_SELECTED  : Glyph.CHECK_PRESSED )
                        : rollover ? ( selected ? Glyph.CHECK_MOUSE_OVER_SELECTED : Glyph.CHECK_MOUSE_OVER )
                        :            ( selected ? Glyph.CHECK_SELECTED : Glyph.CHECK );
            inGlyphSquare(g, x, y, w, h);
            if ( enabled && focused )
                fill(g, new RoundRectangle2D.Float(0.6f, 0.6f, 16.8f, 16.8f, 8, 8), s.get(NimbusScheme.Key.FOCUS));
            else if ( glyph.lip != null )
                fill(g, new RoundRectangle2D.Float(2, 11, 14, 6, 5.2f, 5.2f), glyph.lip.in(s));
            g.setPaint(glyph.edge.paint(s, 5.5f, 2, 5.53f, 15.94f));
            g.fill(new RoundRectangle2D.Float(2, 2, 14, 14, 3.7f, 3.7f));
            g.setPaint(glyph.face.paint(s, 3, 14.97f));
            g.fill(new RoundRectangle2D.Float(3, 3, 12, 12, 3.8f, 3.8f));
            if ( glyph.mark != null )
                fill(g, TICK, glyph.mark.in(s));
        }

        /** A round button with a dark dot, the dot shaded like a bead rather than filled flat. */
        @Override
        public void paintRadioGlyph(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
        ) {
            NimbusScheme s = schemeOf(p);
            Glyph glyph = !enabled ? ( selected ? Glyph.RADIO_DISABLED_SELECTED : Glyph.RADIO_DISABLED )
                        : pressed  ? ( selected ? Glyph.RADIO_PRESSED_SELECTED  : Glyph.RADIO_PRESSED )
                        : rollover ? ( selected ? Glyph.RADIO_MOUSE_OVER_SELECTED : Glyph.RADIO_MOUSE_OVER )
                        :            ( selected ? Glyph.RADIO_SELECTED : Glyph.RADIO );
            inGlyphSquare(g, x, y, w, h);
            if ( enabled && focused )
                fill(g, new Ellipse2D.Float(0.6f, 0.6f, 16.8f, 16.8f), s.get(NimbusScheme.Key.FOCUS));
            else if ( glyph.lip != null )
                fill(g, new Ellipse2D.Float(2, 3, 14, 14), glyph.lip.in(s));
            g.setPaint(glyph.edge.paint(s, 8.97f, 1.94f, 9, 15.97f));
            g.fill(new Ellipse2D.Float(2, 2, 14, 14));
            g.setPaint(glyph.face.paint(s, 8.97f, 3.06f, 9.09f, 15));
            g.fill(new Ellipse2D.Float(3, 3, 12, 12));
            if ( glyph.mark != null ) {
                g.setPaint(glyph.mark.paint(s, 9.03f, 6, 8.97f, 12));
                g.fill(new Ellipse2D.Float(6, 6, 6, 6));
            }
        }

        /** The tick Nimbus cuts out of a check box, in the eighteen pixel square it is drawn in. */
        private static final Shape TICK = polygon(5.03f, 8.06f, 7.03f, 8.06f, 8.44f, 11.06f, 11.59f, 3.12f,
                                                  14.00f, 3.09f, 8.94f, 13.03f, 8.06f, 13.03f);

        /** Moves and scales a context so that the painting after it can be written in the eighteen
         *  pixel square Nimbus lays a check box and a radio button out in. */
        private static void inGlyphSquare( Graphics2D g, int x, int y, int w, int h ) {
            LafUtilities.antialiasShapes(g);
            g.translate(x, y);
            g.scale(w / 18.0, h / 18.0);
        }

        private static void fill( Graphics2D g, Shape shape, Color color ) {
            g.setColor(color);
            g.fill(shape);
        }

        private static Shape polygon( float... xy ) {
            Path2D.Float path = new Path2D.Float();
            path.moveTo(xy[0], xy[1]);
            for ( int i = 2; i < xy.length; i += 2 )
                path.lineTo(xy[i], xy[i + 1]);
            path.closePath();
            return path;
        }

        /**
         *  One state of a check box or a radio button as Nimbus fills it: the lip under the box, its
         *  edge, its face, and the mark on it. The mark is a single colour for a tick and a gradient
         *  for a radio dot, so a check box's {@code mark} holds a {@link NimbusScheme.Shade} and a
         *  radio button's a {@link NimbusScheme.Gradient}.
         */
        private static final class Glyph
        {
            final NimbusScheme.@Nullable Shade lip;
            final NimbusScheme.Gradient        edge;
            final NimbusScheme.Gradient        face;
            final @Nullable Mark               mark;

            private Glyph( NimbusScheme.@Nullable Shade lip, NimbusScheme.Gradient edge, NimbusScheme.Gradient face, @Nullable Mark mark ) {
                this.lip = lip; this.edge = edge; this.face = face; this.mark = mark;
            }

            /** A tick in one colour, or a dot in a gradient. */
            private static final class Mark
            {
                private final NimbusScheme.@Nullable Shade    _shade;
                private final NimbusScheme.@Nullable Gradient _gradient;

                Mark( NimbusScheme.@Nullable Shade shade, NimbusScheme.@Nullable Gradient gradient ) {
                    _shade = shade; _gradient = gradient;
                }

                Color in( NimbusScheme s ) { return java.util.Objects.requireNonNull(_shade).in(s); }

                Paint paint( NimbusScheme s, float x1, float y1, float x2, float y2 ) {
                    return java.util.Objects.requireNonNull(_gradient).paint(s, x1, y1, x2, y2);
                }
            }

            private static NimbusScheme.Shade bg( double h, double s, double b ) { return NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, h, s, b); }
            private static NimbusScheme.Shade base( double h, double s, double b ) { return NimbusScheme.shade(NimbusScheme.Key.BASE, h, s, b); }
            private static NimbusScheme.Gradient three( NimbusScheme.Shade top, NimbusScheme.Shade bottom ) {
                return NimbusScheme.gradient(new double[]{ 0, 0.5, 1 }, top, NimbusScheme.MID, bottom);
            }
            private static NimbusScheme.Gradient five( double[] fractions, NimbusScheme.Shade a, NimbusScheme.Shade b, NimbusScheme.Shade c ) {
                return NimbusScheme.gradient(fractions, a, NimbusScheme.MID, b, NimbusScheme.MID, c);
            }
            private static Mark tick( NimbusScheme.Shade shade ) { return new Mark(shade, null); }
            private static Mark dot( NimbusScheme.Shade top, NimbusScheme.Shade middle, NimbusScheme.Shade bottom ) {
                return new Mark(null, five(new double[]{ 0, 0.232, 0.464, 0.732, 1 }, top, middle, bottom));
            }

            private static final double[] BOX_FACE = { 0, 0.322, 0.645, 0.822, 1 };
            private static final NimbusScheme.Shade BOX_LIP   = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, 0, 0, -89);
            private static final NimbusScheme.Shade RADIO_LIP = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, 0, 0, -112);
            private static final NimbusScheme.Shade PRESSED_LIP = bg(0, -0.110526316, 0.25490195);
            private static final Mark BLACK_TICK = tick(base(-0.57865167, -0.6357143, -0.54901963));
            private static final Mark BEAD = dot(bg(-0.027777791, -0.07243107, -0.33333334), bg(-0.6111111, -0.110526316, -0.74509805), bg(-0.027777791, 0.07129187, -0.6156863));

            static final Glyph CHECK = new Glyph(BOX_LIP,
                    three(bg(0, -0.05356429, -0.12549019), bg(0, -0.015789472, -0.37254903)),
                    five(BOX_FACE, base(0.08801502, -0.63174605, 0.43921566), base(0.032459438, -0.5953556, 0.32549018), base(0.032459438, -0.59942394, 0.4235294)), null);
            static final Glyph CHECK_MOUSE_OVER = new Glyph(BOX_LIP,
                    three(bg(0, -0.020974077, -0.21960783), bg(0.01010108, 0.08947369, -0.5294118)),
                    five(BOX_FACE, base(0.08801502, -0.6317773, 0.4470588), base(0.032459438, -0.5985242, 0.39999998), base(0, -0.6357143, 0.45098037)), null);
            static final Glyph CHECK_PRESSED = new Glyph(BOX_LIP,
                    three(bg(0.055555582, 0.8894737, -0.7176471), bg(0, 0.0016232133, -0.3254902)),
                    five(BOX_FACE, base(0.027408898, -0.5847884, 0.2980392), base(0.029681683, -0.52701867, 0.17254901), base(0.029681683, -0.5376751, 0.25098038)), null);
            static final Glyph CHECK_DISABLED = new Glyph(null,
                    three(bg(0, -0.06766917, 0.07843137), bg(0, -0.06484103, 0.027450979)),
                    five(BOX_FACE, base(0.032459438, -0.60996324, 0.36470586), base(0.02551502, -0.5996783, 0.3215686), base(0.032459438, -0.59624064, 0.34509802)), null);
            static final Glyph CHECK_SELECTED = new Glyph(BOX_LIP,
                    three(base(0.00051498413, -0.34585923, -0.007843137), base(0.00051498413, -0.10238093, -0.25490198)),
                    five(BOX_FACE, base(0.004681647, -0.6197143, 0.43137252), base(0.00051498413, -0.44153953, 0.2588235), base(0.00051498413, -0.4602757, 0.34509802)), BLACK_TICK);
            static final Glyph CHECK_MOUSE_OVER_SELECTED = new Glyph(BOX_LIP,
                    three(base(0.0013483167, -0.1769987, -0.12156865), base(0.05468172, 0.3642857, -0.43137258)),
                    five(BOX_FACE, base(0.004681647, -0.6198413, 0.43921566), base(0.00051498413, -0.4555341, 0.3215686), base(0.00051498413, -0.47377098, 0.41960782)), BLACK_TICK);
            static final Glyph CHECK_PRESSED_SELECTED = new Glyph(PRESSED_LIP,
                    three(base(-0.57865167, -0.6357143, -0.54901963), base(-0.0000352859, 0.026785731, -0.23529413)),
                    NimbusScheme.gradient(new double[]{ 0, 0.058, 0.116, 0.38, 0.645, 0.822, 1 },
                            base(-0.00042033195, -0.38050595, 0.20392156), NimbusScheme.MID, base(-0.0021489263, -0.2891234, 0.14117646), NimbusScheme.MID,
                            base(-0.006362498, -0.016311288, -0.02352941), NimbusScheme.MID, base(0, -0.17930403, 0.21568626)), BLACK_TICK);
            static final Glyph CHECK_DISABLED_SELECTED = new Glyph(null,
                    three(bg(-0.01111114, -0.03771078, 0.062745094), bg(-0.02222222, -0.032806106, 0.011764705)),
                    five(BOX_FACE, base(0.021348298, -0.59223604, 0.35294116), base(0.021348298, -0.56722116, 0.3098039), base(0.021348298, -0.56875, 0.32941175)),
                    tick(base(0.027408898, -0.5735674, 0.14509803)));

            static final Glyph RADIO = new Glyph(RADIO_LIP,
                    three(bg(0, -0.053201474, -0.12941176), bg(0, 0.006356798, -0.44313726)),
                    NimbusScheme.gradient(new double[]{ 0.063, 0.25, 0.437, 0.48, 0.524, 0.705, 0.886 },
                            bg(0.055555582, -0.10654225, 0.23921567), NimbusScheme.MID, bg(0, -0.07016757, 0.12941176), NimbusScheme.MID,
                            bg(0, -0.07016757, 0.12941176), NimbusScheme.MID, bg(0, -0.07206477, 0.17254901)), null);
            static final Glyph RADIO_MOUSE_OVER = new Glyph(RADIO_LIP,
                    three(bg(-0.00505054, -0.027819552, -0.2235294), bg(0, 0.24241486, -0.6117647)),
                    NimbusScheme.gradient(new double[]{ 0.063, 0.216, 0.369, 0.548, 0.728, 0.775, 0.822, 0.911, 1 },
                            bg(-0.111111104, -0.10655806, 0.24313724), NimbusScheme.MID, bg(0, -0.07333623, 0.20392156), NimbusScheme.MID,
                            bg(0, -0.07333623, 0.20392156), NimbusScheme.MID, bg(0.08585858, -0.067389056, 0.25490195), NimbusScheme.MID,
                            bg(-0.111111104, -0.10628903, 0.18039215)), null);
            static final Glyph RADIO_PRESSED = new Glyph(PRESSED_LIP,
                    three(bg(0.055555582, 0.23947367, -0.6666667), bg(-0.0777778, -0.06815343, -0.28235295)),
                    NimbusScheme.gradient(new double[]{ 0.063, 0.208, 0.352, 0.45, 0.548, 0.748, 0.949 },
                            bg(0, -0.06866585, 0.09803921), NimbusScheme.MID, bg(-0.0027777553, -0.0018306673, -0.02352941), NimbusScheme.MID,
                            bg(-0.0027777553, -0.0018306673, -0.02352941), NimbusScheme.MID, bg(0.002924025, -0.02047892, 0.082352936)), null);
            static final Glyph RADIO_DISABLED = new Glyph(null,
                    three(bg(0, -0.06766917, 0.07843137), bg(0, -0.06413457, 0.015686274)),
                    NimbusScheme.gradient(new double[]{ 0.063, 0.216, 0.369, 0.548, 0.728, 0.775, 0.822, 0.911, 1 },
                            bg(0, -0.08466425, 0.16470587), NimbusScheme.MID, bg(0, -0.07016757, 0.12941176), NimbusScheme.MID,
                            bg(0, -0.07016757, 0.12941176), NimbusScheme.MID, bg(0, -0.070703305, 0.14117646), NimbusScheme.MID,
                            bg(0, -0.07052632, 0.1372549)), null);
            static final Glyph RADIO_SELECTED = new Glyph(RADIO_LIP,
                    three(base(0.00029569864, -0.36035198, -0.007843137), base(0.00029569864, 0.019458115, -0.32156867)),
                    NimbusScheme.gradient(new double[]{ 0.081, 0.101, 0.12, 0.289, 0.458, 0.616, 0.774, 0.83, 0.886 },
                            base(0.004681647, -0.6195853, 0.4235294), NimbusScheme.MID, base(0.004681647, -0.56704473, 0.36470586), NimbusScheme.MID,
                            base(0.00051498413, -0.43866998, 0.24705881), NimbusScheme.MID, base(0.00051498413, -0.43866998, 0.24705881), NimbusScheme.MID,
                            base(0.00051498413, -0.44879842, 0.29019606)), BEAD);
            static final Glyph RADIO_MOUSE_OVER_SELECTED = new Glyph(RADIO_LIP,
                    three(base(-0.0006374717, -0.20452163, -0.12156865), base(-0.57865167, -0.6357143, -0.5058824)),
                    NimbusScheme.gradient(new double[]{ 0.081, 0.101, 0.12, 0.202, 0.283, 0.492, 0.702, 0.756, 0.81, 0.848, 0.886 },
                            base(-0.011985004, -0.6157143, 0.43137252), NimbusScheme.MID, base(0.004681647, -0.56932425, 0.3960784), NimbusScheme.MID,
                            base(0.00051498413, -0.4555341, 0.3215686), NimbusScheme.MID, base(0.00051498413, -0.4555341, 0.3215686), NimbusScheme.MID,
                            base(0.00051498413, -0.46550155, 0.372549), NimbusScheme.MID, base(0.0024294257, -0.47271872, 0.34117645)), BEAD);
            static final Glyph RADIO_PRESSED_SELECTED = new Glyph(PRESSED_LIP,
                    three(base(-0.57865167, -0.6357143, -0.49803925), base(0.00029569864, 0.019458115, -0.32156867)),
                    NimbusScheme.gradient(new double[]{ 0.039, 0.078, 0.117, 0.288, 0.458, 0.562, 0.666, 0.776, 0.886 },
                            base(-0.0017285943, -0.4367347, 0.21960783), NimbusScheme.MID, base(-0.0010654926, -0.31349206, 0.15686274), NimbusScheme.MID,
                            base(0, 0, 0), NimbusScheme.MID, base(0, 0, 0), NimbusScheme.MID, base(0.000805676, -0.12380952, 0.109803915)),
                    dot(bg(-0.027777791, -0.080223285, -0.4862745), bg(-0.6111111, -0.110526316, -0.74509805), bg(-0.027777791, 0.07129187, -0.6156863)));
            static final Glyph RADIO_DISABLED_SELECTED = new Glyph(null,
                    three(base(0.010237217, -0.56289876, 0.2588235), base(0.016586483, -0.5620301, 0.19607842)),
                    five(new double[]{ 0.081, 0.27, 0.458, 0.672, 0.886 }, base(0.027408898, -0.5878882, 0.35294116), base(0.021348298, -0.56722116, 0.3098039), base(0.021348298, -0.567841, 0.31764704)),
                    dot(bg(-0.01111114, -0.058170296, 0.0039215684), bg(-0.013888836, -0.04195489, -0.058823526), bg(0.009259284, -0.0147816315, -0.007843137)));
        }

        // ── Arrows ───────────────────────────────────────────────────────────

        /**
         *  A small solid wedge in a grey taken from the blue-grey, pointing right at a closed node and
         *  down at an open one. Nimbus lays it out at the left of an eighteen pixel wide icon, which
         *  is what puts it where it is relative to the node's own icon.
         */
        @Override
        public void paintDisclosure(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
        ) {
            LafUtilities.antialiasShapes(g);
            float scale = w / (float) disclosureGlyphSize();
            g.translate(x, y + h / 2f - 3.5f * scale);
            g.scale(scale, scale);
            g.setColor(DISCLOSURE.in(schemeOf(p)));
            g.fill(expanded ? polygon(0, 0, 7, 0, 3.54f, 6.95f) : polygon(0, 0, 6.92f, 3.51f, 0, 7));
        }

        @Override public int disclosureGlyphSize() { return 18; }

        private static final NimbusScheme.Shade DISCLOSURE = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.6111111, -0.110526316, -0.34509805);

        @Override
        public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
            paintSubmenuArrow(g, p, x, y, w, h, enabled, false);
        }

        /** A solid wedge in the nine by ten pixel square Nimbus lays a menu's arrow out in, white on the selection band. */
        @Override
        public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled, boolean armed ) {
            NimbusScheme s = schemeOf(p);
            inMenuMarkSquare(g, x, y, w, h);
            fill(g, polygon(0, 1, 7.76f, 5.51f, 0, 10), ( !enabled ? MENU_MARK_DISABLED : armed ? MENU_MARK_ARMED : MENU_ARROW ).in(s));
        }

        /** A plain tick, or a diamond for a radio button menu item, and nothing at all for an entry that is off. */
        @Override
        public void paintMenuMark(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean radio,
            boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected, boolean armed
        ) {
            if ( !selected )
                return;
            NimbusScheme s = schemeOf(p);
            inMenuMarkSquare(g, x, y, w, h);
            Shape mark = radio ? polygon(0.01f, 5.49f, 4.55f, 1.01f, 9, 5.51f, 4.54f, 10)
                               : polygon(0, 5, 2.15f, 5, 3.56f, 7.39f, 6.96f, 0, 9, 0, 9, 1, 8.16f, 1.98f, 4, 10, 2.87f, 10);
            NimbusScheme.Shade ink = !enabled ? MENU_MARK_DISABLED : armed ? MENU_MARK_ARMED : radio ? MENU_ARROW : MENU_TICK;
            fill(g, mark, ink.in(s));
        }

        @Override public int menuMarkSize() { return 10; }

        /** Moves and scales a context into the nine by ten pixel box Nimbus draws a menu's marks in, centred in the square it is given. */
        private static void inMenuMarkSquare( Graphics2D g, int x, int y, int w, int h ) {
            LafUtilities.antialiasShapes(g);
            float scale = w / 10f;
            g.translate(x + scale / 2f, y);
            g.scale(scale, h / 10f);
        }

        private static final NimbusScheme.Shade MENU_ARROW         = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.055555582, -0.09663743, -0.4627451);
        private static final NimbusScheme.Shade MENU_TICK          = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.055555582, -0.096827686, -0.45882353);
        private static final NimbusScheme.Shade MENU_MARK_DISABLED = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.08983666, -0.17647058);
        private static final NimbusScheme.Shade MENU_MARK_ARMED    = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.110526316, 0.25490195);

        /**
         *  The blue end of a combo box: the default button's material, square on its left where it
         *  meets the value and rounded on its right, with a small dark wedge on it. The button it is
         *  drawn on reaches over the combo box's edge and margin, see {@link #actuatorReachesBounds()}, so
         *  its shape stops two pixels short of the button's right, top and bottom.
         */
        @Override
        public void paintComboArrow(
            Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
        ) {
            NimbusScheme s = schemeOf(p);
            NimbusMould mould = !enabled ? NimbusMould.COMBO_BOX_ACTUATOR_DISABLED
                              : pressed  ? NimbusMould.COMBO_BOX_ACTUATOR_PRESSED
                              : rollover ? NimbusMould.COMBO_BOX_ACTUATOR_MOUSE_OVER
                              :            NimbusMould.COMBO_BOX_ACTUATOR;
            float scale = UI.scale();
            float bw    = w / scale;
            float bh    = h / scale;
            LafUtilities.antialiasShapes(g);
            g.scale(scale, scale);
            g.setPaint(mould.edge().paint(s, 0, bh));
            g.fill(roundedOnTheRight(0, 0, bw, bh, 10, true, true));
            g.setPaint(mould.face().paint(s, 1, bh - 1));
            g.fill(roundedOnTheRight(0.25f, 1, bw - 1.25f, bh - 2, 8, true, true));
            float top = bh / 2f - 1.92f;
            Shape wedge = polygon(5, top, 12, top, 8.53f, top + 5);
            if ( !enabled )
                g.setColor(COMBO_WEDGE_DISABLED.in(s));
            else if ( pressed )
                g.setColor(WEDGE_ON_PRESSED.in(s));
            else
                g.setPaint(COMBO_WEDGE.paint(s, top, top + 5));
            g.fill(wedge);
        }

        /**
         *  One of a spinner's two buttons, stacked at its right: the upper one rounded at its top
         *  right, the lower at its bottom right, with a rule between them and the lip of the spinner
         *  under the lower one.
         */
        @Override
        public void paintSpinnerArrow(
            Graphics2D g, Palette p, int w, int h, boolean up,
            boolean enabled, boolean rollover, boolean pressed
        ) {
            NimbusScheme s = schemeOf(p);
            Stepper stepper = up ? ( !enabled ? Stepper.NEXT_DISABLED : pressed ? Stepper.NEXT_PRESSED : rollover ? Stepper.NEXT_MOUSE_OVER : Stepper.NEXT )
                                 : ( !enabled ? Stepper.PREVIOUS_DISABLED : pressed ? Stepper.PREVIOUS_PRESSED : rollover ? Stepper.PREVIOUS_MOUSE_OVER : Stepper.PREVIOUS );
            float scale = UI.scale();
            float bw    = w / scale;
            float bh    = h / scale;
            LafUtilities.antialiasShapes(g);
            g.scale(scale, scale);
            if ( up ) {
                g.setPaint(stepper.edge.paint(s, 2, bh));
                g.fill(roundedOnTheRight(0, 2, bw - 2, bh - 2, 10, true, false));
                g.setPaint(stepper.face.paint(s, 3, bh - 1));
                g.fill(roundedOnTheRight(1, 3, bw - 4, bh - 4, 8, true, false));
                if ( stepper.rule != null )
                    fill(g, new java.awt.geom.Rectangle2D.Float(1, bh - 1, bw - 4, 1), stepper.rule.in(s));
                float bottom = bh - 3.75f;
                g.setPaint(stepper.arrow.paint(s, bottom - 3.89f, bottom));
                g.fill(polygon(6, bottom, 8.45f, bottom - 3.89f, 11, bottom));
            } else {
                if ( stepper.lip != null )
                    fill(g, roundedOnTheRight(0, bh - 3, bw - 2, 2, 10, false, true), stepper.lip.in(s));
                g.setPaint(stepper.edge.paint(s, 0, bh - 2));
                g.fill(roundedOnTheRight(0, 0, bw - 2, bh - 2, 10, false, true));
                g.setPaint(stepper.face.paint(s, 0, bh - 3));
                g.fill(roundedOnTheRight(1, 0, bw - 4, bh - 3, 8, false, true));
                g.setPaint(stepper.arrow.paint(s, 3.75f, 7.73f));
                g.fill(polygon(6, 3.75f, 8.52f, 7.73f, 11, 3.75f));
            }
        }

        /**
         *  A combo box's button reaches over the combo box's edge and margin to its bounds, which is
         *  how Nimbus lays it out, so that the blue end of the control has an outline of its own
         *  rather than sitting inside the grey one.
         */
        @Override public boolean actuatorReachesBounds() { return true; }

        /** A rectangle rounded on its right-hand corners only, as the end of a control is. */
        private static Shape roundedOnTheRight( float x, float y, float w, float h, float arc, boolean top, boolean bottom ) {
            java.awt.geom.Area shape = new java.awt.geom.Area(new RoundRectangle2D.Float(x - arc, y, w + arc, h, arc, arc));
            shape.intersect(new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Float(x, y, w, h)));
            if ( !top )
                shape.add(new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Float(x, y, w, h / 2f)));
            if ( !bottom )
                shape.add(new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Float(x, y + h / 2f, w, h / 2f)));
            return shape;
        }

        private static final NimbusScheme.Gradient COMBO_WEDGE = NimbusScheme.gradient(new double[]{ 0, 0.5, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BASE, -0.57865167, -0.6357143, -0.37254906), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BASE, -0.57865167, -0.6357143, -0.5254902));
        private static final NimbusScheme.Shade COMBO_WEDGE_DISABLED = NimbusScheme.shade(NimbusScheme.Key.BASE, 0.027408898, -0.57391655, 0.1490196);
        private static final NimbusScheme.Shade WEDGE_ON_PRESSED     = NimbusScheme.shade(NimbusScheme.Key.BASE, 0, -0.6357143, 0.45098037);

        /** One state of one of a spinner's buttons, from SpinnerNextButtonPainter and SpinnerPreviousButtonPainter. */
        private enum Stepper
        {
            NEXT( null, three(base(0.00051498413, -0.34585923, -0.007843137), base(0.00051498413, -0.27207792, -0.11764708)),
                  NimbusScheme.gradient(new double[]{ 0, 0.365, 0.73, 0.865, 1 }, base(0.004681647, -0.6197143, 0.43137252), NimbusScheme.MID,
                                        base(-0.0012707114, -0.5078604, 0.3098039), NimbusScheme.MID, base(-0.0028941035, -0.4800539, 0.28235292)),
                  base(0.0023007393, -0.3622768, -0.04705882), UP_WEDGE ),
            NEXT_MOUSE_OVER( null, three(base(0.0013483167, -0.1769987, -0.12156865), base(0.0013483167, 0.039961398, -0.25882354)),
                  NimbusScheme.gradient(new double[]{ 0, 0.397, 0.794, 0.897, 1 }, base(0.004681647, -0.6198413, 0.43921566), NimbusScheme.MID,
                                        base(-0.0012707114, -0.51502466, 0.3607843), NimbusScheme.MID, base(0.0021564364, -0.49097747, 0.34509802)),
                  base(0.0000520349, -0.38743842, 0.019607842), UP_WEDGE ),
            NEXT_PRESSED( null, three(base(-0.57865167, -0.6357143, -0.54901963), base(0.08801502, 0.3642857, -0.454902)),
                  NimbusScheme.gradient(new double[]{ 0, 0.432, 0.864, 0.932, 1 }, base(-0.00042033195, -0.38050595, 0.20392156), NimbusScheme.MID,
                                        base(0.00029569864, -0.15470162, 0.07058823), NimbusScheme.MID, base(-0.00046235323, -0.09571427, 0.039215684)),
                  base(0.018363237, 0.18135887, -0.227451), flat(WEDGE_ON_PRESSED) ),
            NEXT_DISABLED( null, three(base(0.021348298, -0.56289876, 0.2588235), base(0.010237217, -0.5607143, 0.2352941)),
                  three(base(0.021348298, -0.59223604, 0.35294116), base(0.016586483, -0.5723659, 0.31764704)),
                  base(0.021348298, -0.56182265, 0.24705881), flat(base(0.021348298, -0.58106947, 0.16862744)) ),
            PREVIOUS( NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.0033834577, -0.30588236, -148),
                  three(base(0.00051498413, -0.2583558, -0.13333336), base(0.00051498413, -0.095173776, -0.25882354)),
                  sevenStops(base(0.004681647, -0.5383692, 0.33725488), base(-0.0017285943, -0.44453782, 0.25098038),
                             base(0.00051498413, -0.43866998, 0.24705881), base(0.00051498413, -0.4625541, 0.35686272)),
                  null, flat(base(-0.57865167, -0.6357143, -0.54901963)) ),
            PREVIOUS_MOUSE_OVER( NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.6111111, -0.110526316, -0.63529414, -179),
                  three(base(0.0013483167, 0.088923395, -0.2784314), base(0.059279382, 0.3642857, -0.43529415)),
                  sevenStops(base(0.0010585189, -0.541452, 0.4078431), base(0.00254488, -0.4608264, 0.32549018),
                             base(0.00051498413, -0.4555341, 0.3215686), base(0.00051498413, -0.4757143, 0.43137252)),
                  null, flat(base(-0.57865167, -0.6357143, -0.54901963)) ),
            PREVIOUS_PRESSED( NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.110526316, 0.25490195, -186),
                  three(base(0.061133325, 0.3642857, -0.427451), base(-0.0000352859, 0.018606722, -0.23137257)),
                  sevenStops(base(0.0008354783, -0.2578073, 0.12549019), base(0.00089377165, -0.01599598, 0.007843137),
                             base(0, -0.00895375, 0.007843137), base(0.00089377165, -0.13853917, 0.14509803)),
                  null, flat(WEDGE_ON_PRESSED) ),
            PREVIOUS_DISABLED( null,
                  three(base(0.015098333, -0.5557143, 0.2352941), base(0.010237217, -0.55799407, 0.20784312)),
                  NimbusScheme.gradient(new double[]{ 0, 0.057, 0.115, 0.557, 1 }, base(0.018570602, -0.5821429, 0.32941175), NimbusScheme.MID,
                                        base(0.021348298, -0.56722116, 0.3098039), NimbusScheme.MID, base(0.021348298, -0.567841, 0.31764704)),
                  null, flat(base(0.018570602, -0.56714284, 0.1372549)) );

            final NimbusScheme.@Nullable Shade lip;
            final NimbusScheme.Gradient        edge;
            final NimbusScheme.Gradient        face;
            final NimbusScheme.@Nullable Shade rule;
            final NimbusScheme.Gradient        arrow;

            Stepper( NimbusScheme.@Nullable Shade lip, NimbusScheme.Gradient edge, NimbusScheme.Gradient face,
                     NimbusScheme.@Nullable Shade rule, NimbusScheme.Gradient arrow ) {
                this.lip = lip; this.edge = edge; this.face = face; this.rule = rule; this.arrow = arrow;
            }

            private static NimbusScheme.Shade base( double h, double s, double b ) { return NimbusScheme.shade(NimbusScheme.Key.BASE, h, s, b); }

            private static NimbusScheme.Gradient three( NimbusScheme.Shade top, NimbusScheme.Shade bottom ) {
                return NimbusScheme.gradient(new double[]{ 0, 0.5, 1 }, top, NimbusScheme.MID, bottom);
            }

            private static NimbusScheme.Gradient sevenStops( NimbusScheme.Shade a, NimbusScheme.Shade b, NimbusScheme.Shade c, NimbusScheme.Shade d ) {
                return NimbusScheme.gradient(new double[]{ 0, 0.057, 0.115, 0.242, 0.369, 0.684, 1 },
                                             a, NimbusScheme.MID, b, NimbusScheme.MID, c, NimbusScheme.MID, d);
            }

            /** A gradient that is one colour, so that every arrow can be painted the same way. */
            private static NimbusScheme.Gradient flat( NimbusScheme.Shade colour ) {
                return NimbusScheme.gradient(new double[]{ 0, 1 }, colour, colour);
            }
        }

        private static final NimbusScheme.Gradient UP_WEDGE = NimbusScheme.gradient(new double[]{ 0, 0.5, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BASE, -0.57865167, -0.6357143, -0.043137252), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BASE, -0.57865167, -0.6357143, -0.24313727));

        // ── Chrome ───────────────────────────────────────────────────────────

        /**
         *  The groove a slider's knob runs along: five pixels deep, dark along its top, with a pale
         *  lip under it, the whole way across the slider and not only along the knob's travel. It carries no colour where the value is:
         *  the knob already says that.
         */
        @Override
        public void paintSliderTrack(
            Graphics2D g, Palette p, Rectangle track, int thumbCentre,
            boolean horizontal, boolean inverted, boolean enabled
        ) {
            NimbusScheme s = schemeOf(p);
            float scale  = UI.scale();
            // Nimbus runs the groove on past both ends of the knob's travel, to the slider's bounds.
            float reach  = sliderThumbDiameter() / 2f + 1.5f;
            float length = ( horizontal ? track.width : track.height ) / scale + 2 * reach;
            float across = ( horizontal ? track.height : track.width ) / scale;
            LafUtilities.antialiasShapes(g);
            g.translate(track.x, track.y);
            g.scale(scale, scale);
            if ( !horizontal )
                g.transform(new java.awt.geom.AffineTransform(0, 1, 1, 0, 0, 0));
            g.translate(-reach, 0);
            float top = across / 2f - 2.5f;
            fill(g, new RoundRectangle2D.Float(1, top + 3, length - 2, enabled ? 3 : 7, 8.7f, 8.7f), ( enabled ? TRACK_LIP : TRACK_LIP_DISABLED ).in(s));
            g.setPaint(( enabled ? TRACK_EDGE : TRACK_EDGE_DISABLED ).paint(s, top + 0.38f, top + 4.56f));
            g.fill(new RoundRectangle2D.Float(0, top, length, 5, 4.9f, 4.9f));
            g.setPaint(( enabled ? TRACK_FACE : TRACK_FACE_DISABLED ).paint(s, top + 1, top + 5));
            g.fill(new RoundRectangle2D.Float(1.44f, top + 1, length - 2.88f, 4, 4, 4));
        }

        /** A small round knob of the default button's blue, in the seventeen pixel square Nimbus lays it out in. */
        @Override
        public void paintSliderThumb(
            Graphics2D g, Palette p, Rectangle r, boolean enabled, boolean focused, boolean rollover
        ) {
            NimbusScheme s = schemeOf(p);
            Knob knob = !enabled ? Knob.DISABLED : rollover ? Knob.MOUSE_OVER : Knob.ENABLED;
            LafUtilities.antialiasShapes(g);
            g.translate(r.x, r.y);
            g.scale(r.width / 17.0, r.height / 17.0);
            if ( enabled && focused )
                fill(g, new Ellipse2D.Float(0.6f, 0.6f, 15.8f, 15.8f), s.get(NimbusScheme.Key.FOCUS));
            else if ( knob.lip != null )
                fill(g, new Ellipse2D.Float(2, 3, 13, 13), knob.lip.in(s));
            g.setPaint(knob.edge.paint(s, 8.63f, 2, 8.63f, 15.05f));
            g.fill(new Ellipse2D.Float(2, 2, 13, 13));
            g.setPaint(knob.face.paint(s, 8.5f, 3.02f, 8.5f, 14));
            g.fill(new Ellipse2D.Float(3, 3, 11, 11));
        }

        private static final NimbusScheme.Shade TRACK_LIP          = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.110526316, 0.25490195, -111);
        private static final NimbusScheme.Shade TRACK_LIP_DISABLED = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.110526316, 0.25490195, -245);
        private static final NimbusScheme.Gradient TRACK_EDGE = NimbusScheme.gradient(new double[]{ 0, 0.5, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.034093194, -0.12941176), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.01111114, -0.023821115, -0.06666666));
        private static final NimbusScheme.Gradient TRACK_FACE = NimbusScheme.gradient(new double[]{ 0, 0.138, 0.275, 0.491, 0.706 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.008547008, -0.03314536, -0.086274505), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.004273474, -0.040256046, -0.019607842), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.03626889, 0.04705882));
        private static final NimbusScheme.Gradient TRACK_EDGE_DISABLED = NimbusScheme.gradient(new double[]{ 0, 0.5, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.0055555105, -0.061265234, 0.05098039), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.01010108, -0.059835073, 0.10588235));
        private static final NimbusScheme.Gradient TRACK_FACE_DISABLED = NimbusScheme.gradient(new double[]{ 0, 0.138, 0.275, 0.638, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.01111114, -0.061982628, 0.062745094), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.00505054, -0.058639523, 0.086274505), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.01010108, -0.059835073, 0.10588235));

        /** One state of a slider's knob, from SliderThumbPainter. */
        private enum Knob
        {
            ENABLED( NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.003968239, 0.0014736876, -0.25490198, -156),
                     three(base(0.00051498413, -0.34585923, -0.007843137), base(-0.0017285943, -0.11571431, -0.25490198)),
                     NimbusScheme.gradient(new double[]{ 0, 0.213, 0.425, 0.561, 0.698, 0.849, 1 },
                             base(-0.023096085, -0.6238095, 0.43921566), NimbusScheme.MID, base(0.00051498413, -0.43866998, 0.24705881), NimbusScheme.MID,
                             base(0.00051498413, -0.43866998, 0.24705881), NimbusScheme.MID, base(0.00051498413, -0.45714286, 0.32941175)) ),
            MOUSE_OVER( NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.003968239, 0.0014736876, -0.25490198, -156),
                     three(base(-0.0038217902, -0.15532213, -0.14901963), base(-0.57865167, -0.6357143, -0.54509807)),
                     NimbusScheme.gradient(new double[]{ 0, 0.213, 0.425, 0.561, 0.698, 0.849, 1 },
                             base(0.004681647, -0.62780917, 0.44313723), NimbusScheme.MID, base(0.00029569864, -0.4653107, 0.32549018), NimbusScheme.MID,
                             base(0.00051498413, -0.4563421, 0.32549018), NimbusScheme.MID, base(-0.0017285943, -0.4732143, 0.39215684)) ),
            DISABLED( null,
                     three(base(0.021348298, -0.5625436, 0.25490195), base(0.015098333, -0.55105823, 0.19215685)),
                     NimbusScheme.gradient(new double[]{ 0, 0.213, 0.425, 0.713, 1 },
                             base(0.021348298, -0.5924243, 0.35686272), NimbusScheme.MID, base(0.021348298, -0.56722116, 0.3098039), NimbusScheme.MID,
                             base(0.021348298, -0.56844974, 0.32549018)) );

            final NimbusScheme.@Nullable Shade lip;
            final NimbusScheme.Gradient        edge;
            final NimbusScheme.Gradient        face;

            Knob( NimbusScheme.@Nullable Shade lip, NimbusScheme.Gradient edge, NimbusScheme.Gradient face ) {
                this.lip = lip; this.edge = edge; this.face = face;
            }

            private static NimbusScheme.Shade base( double h, double s, double b ) { return NimbusScheme.shade(NimbusScheme.Key.BASE, h, s, b); }

            private static NimbusScheme.Gradient three( NimbusScheme.Shade top, NimbusScheme.Shade bottom ) {
                return NimbusScheme.gradient(new double[]{ 0, 0.5, 1 }, top, NimbusScheme.MID, bottom);
            }
        }

        @Override public boolean scrollBarHasSteppers() { return true; }

        /** A rule shading from light at its ends to dark in its middle, so that it fades into the
         *  highlight along the top of the heading row and the shadow along its bottom. */
        @Override public @Nullable Paint tableHeaderDivider( Palette p, int height ) {
            return HEADER_DIVIDER.paint(schemeOf(p), 0, height - UI.scale(1));
        }

        private static final NimbusScheme.Gradient HEADER_DIVIDER = NimbusScheme.gradient(new double[]{ 0, 0.144, 0.437, 0.594, 0.752, 0.876, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.01111114, -0.08625447, 0.062745094), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.013888836, -0.028334536, -0.17254901), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.013888836, -0.029445238, -0.16470587), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.02020204, -0.053531498, 0.011764705));

        /** Nimbus's {@code Table.alternateRowColor}: the page, one step darker. */
        @Override public @Nullable Color tableRowStripe( Palette p ) {
            return NimbusScheme.derive(schemeOf(p).get(NimbusScheme.Key.LIGHT_BACKGROUND), 0, 0, -0.05098039f, 0);
        }

        @Override public int treeNodeGlyphSize() { return 16; }

        /**
         *  Nimbus's own tree icons, redrawn from its painters: a pale blue sheet of paper with its
         *  corner turned down for a leaf, and a blue folder for a node that holds others, its front
         *  dropped open for an expanded one.
         */
        @Override
        public void paintTreeNode(
            Graphics2D g, Palette p, int x, int y, int w, int h,
            boolean leaf, boolean expanded, boolean enabled
        ) {
            NimbusScheme s = schemeOf(p);
            LafUtilities.antialiasShapes(g);
            g.translate(x, y);
            g.scale(w / 16.0, h / 16.0);
            if ( leaf )
                paintPage(g, s);
            else
                paintFolder(g, s, expanded);
        }

        private static void paintPage( Graphics2D g, NimbusScheme s ) {
            fill(g, polygon(1, 0, 1, 16, 2, 16, 2, 1, 10.52f, 1, 14, 4.5f, 14, 16, 15, 16, 15, 4.44f, 10.72f, 0), PAGE_OUTLINE.in(s));
            fill(g, new java.awt.geom.Rectangle2D.Float(2, 15, 12, 1), PAGE_FOOT.in(s));
            g.setPaint(PAGE_CORNER.paint(s, 10.19f, 4.87f, 11.94f, 3.13f));
            g.fill(polygon(10, 1, 10, 5, 14, 5));
            g.setPaint(PAGE_SHEET.paint(s, 8, 1, 8, 15));
            g.fill(polygon(10, 1, 2, 1, 2, 15, 14, 15, 14, 5, 10, 5));
            fill(g, polygon(10, 1, 8.74f, 1, 8.78f, 6.22f, 14, 6.2f, 14, 5, 10, 5), PAGE_FOLD_SHADOW.in(s));
            fill(g, polygon(10, 2, 10, 1, 2, 1, 2, 15, 14, 15, 14, 5, 13, 5, 13, 14, 3, 14, 3, 2), PAGE_INNER_LIGHT.in(s));
        }

        private static void paintFolder( Graphics2D g, NimbusScheme s, boolean open ) {
            fill(g, polygon(0, 13, 0, 14, 1, 16, 14, 16, 15, 14, 15, 13), FOLDER_SHADOW.in(s));
            float lid = open ? 7 : 5;
            g.setPaint(FOLDER_FRONT.paint(s, 9, lid, 9, 14));
            g.fill(open ? polygon(3, 14, 3, 12, 4, 7, 15, 7, 15, 9, 14, 14)
                        : polygon(3, 14, 3.02f, 10.06f, 4, 5, 15, 5, 15, 7, 14, 14));
            g.setPaint(FOLDER_BACK.paint(s, 7, 1, 7, 14));
            g.fill(open ? polygon(1, 14, 2, 14, 2, 11, 4, 6, 13, 6, 13, 3, 8, 3, 7, 2, 7, 1, 3, 1, 3, 2, 2, 3, 1, 3)
                        : polygon(1, 14, 2, 14, 2.04f, 10.19f, 3.98f, 4, 13, 4, 13, 3, 8, 3, 7, 2, 7, 1, 3, 1, 3, 2, 2, 3, 1, 3));
            fill(g, new java.awt.geom.Rectangle2D.Float(1, 3, 1, 1), FOLDER_GLINT_1.in(s));
            fill(g, new java.awt.geom.Rectangle2D.Float(3, 1, 4, 1), FOLDER_GLINT_2.in(s));
            fill(g, new java.awt.geom.Rectangle2D.Float(8, 3, 5, 1), FOLDER_GLINT_3.in(s));
            g.setPaint(FOLDER_OUTLINE.paint(s, 8, 0, 8, 15));
            g.fill(open ? polygon(16, 6, 16, 7, 13, 7, 13, 3, 8, 3, 7, 2, 7, 1, 2.94f, 1.02f, 2.98f, 1.74f, 1.74f, 3, 1, 3, 1, 14, 14, 14, 14, 11,
                                  14, 10, 15.58f, 7.12f, 15.9f, 7.26f, 15, 10, 15, 11, 15, 14, 14, 15, 1, 15, 0, 14, 0, 3.26f, 3.19f, 0,
                                  7, 0, 8.56f, 2, 13, 2, 14, 3, 14, 6)
                        : polygon(16, 4, 16, 5, 13, 5, 13, 3, 8, 3, 7, 2, 7, 1, 2.94f, 1.02f, 2.98f, 1.74f, 1.74f, 3, 1, 3, 1, 14, 14, 14, 14, 7,
                                  14.87f, 5.96f, 15, 5, 16, 5, 15.46f, 6.13f, 15, 7, 15, 14, 14, 15, 1, 15, 0, 14, 0, 3.26f, 3.19f, 0,
                                  7, 0, 8.56f, 2, 13, 2, 14, 3, 14, 4));
            if ( open ) {
                g.setPaint(OPEN_FOLDER_RIM.paint(s, 7.5f, 6, 7.5f, 14));
                g.fill(polygon(13, 7, 13, 6, 3.7f, 6, 2, 11, 2, 14, 3, 14, 2.96f, 12.13f, 4, 7));
            } else {
                g.setPaint(CLOSED_FOLDER_RIM.paint(s, 7.5f, 4, 7.5f, 14));
                g.fill(polygon(13, 5, 13, 4, 3.74f, 4, 2.02f, 10.06f, 2, 14, 3, 14, 2.96f, 12.13f, 4.58f, 4.98f));
            }
        }

        private static final NimbusScheme.Shade PAGE_OUTLINE     = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.007936537, -0.065654516, -0.13333333);
        private static final NimbusScheme.Shade PAGE_FOOT        = NimbusScheme.constant(97, 98, 102, 255);
        private static final NimbusScheme.Gradient PAGE_CORNER   = NimbusScheme.gradient(new double[]{ 0, 0.5, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.032679737, -0.043332636, 0.24705881), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.110526316, 0.25490195));
        private static final NimbusScheme.Gradient PAGE_SHEET    = NimbusScheme.gradient(new double[]{ 0, 0.5, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.0077680945, -0.51781034, 0.3490196), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.013940871, -0.599277, 0.41960782));
        private static final NimbusScheme.Shade PAGE_FOLD_SHADOW = NimbusScheme.shade(NimbusScheme.Key.BASE, 0.004681647, -0.4198052, 0.14117646);
        private static final NimbusScheme.Shade PAGE_INNER_LIGHT = NimbusScheme.shade(NimbusScheme.Key.BASE, 0, -0.6357143, 0.45098037, -127);
        private static final NimbusScheme.Shade FOLDER_SHADOW    = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, 0, -0.21, -99);
        private static final NimbusScheme.Gradient FOLDER_FRONT  = NimbusScheme.gradient(new double[]{ 0.042, 0.103, 0.165, 0.246, 0.326, 0.663, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.00029569864, -0.45978838, 0.2980392), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.0015952587, -0.34848025, 0.18823528), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.0015952587, -0.30844158, 0.09803921), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.0015952587, -0.27329817, 0.035294116));
        private static final NimbusScheme.Gradient FOLDER_BACK   = NimbusScheme.gradient(new double[]{ 0, 0.5, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.0077680945, -0.51781034, 0.3490196), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.004681647, -0.6198413, 0.43921566));
        private static final NimbusScheme.Shade FOLDER_GLINT_1   = NimbusScheme.shade(NimbusScheme.Key.BASE, 0, -0.6357143, 0.45098037, -125);
        private static final NimbusScheme.Shade FOLDER_GLINT_2   = NimbusScheme.shade(NimbusScheme.Key.BASE, 0, -0.6357143, 0.45098037, -50);
        private static final NimbusScheme.Shade FOLDER_GLINT_3   = NimbusScheme.shade(NimbusScheme.Key.BASE, 0, -0.6357143, 0.45098037, -100);
        private static final NimbusScheme.Gradient FOLDER_OUTLINE = NimbusScheme.gradient(new double[]{ 0, 0.5, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.0012094378, -0.23571429, -0.0784314), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.00029569864, -0.115166366, -0.2627451));
        private static final NimbusScheme.Gradient CLOSED_FOLDER_RIM = NimbusScheme.gradient(new double[]{ 0, 0.127, 0.254, 0.627, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.0027436614, -0.335015, 0.011764705), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.0024294257, -0.3857143, 0.031372547), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.0018081069, -0.3595238, -0.13725492));
        private static final NimbusScheme.Gradient OPEN_FOLDER_RIM = NimbusScheme.gradient(new double[]{ 0, 0.5, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.004681647, -0.33496243, -0.027450979), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.0019934773, -0.361378, -0.10588238));

        /**
         *  One end of a scroll bar: a moulding twenty five pixels long whose inner end sweeps down
         *  into the groove in a curve, with a wedge pointing out of the bar. It is drawn the way
         *  Nimbus draws it, for the button at the left end of a horizontal bar, and turned for the
         *  other three.
         */
        @Override
        public void paintScrollStepper(
            Graphics2D g, Palette p, int w, int h, LafUtilities.Direction direction,
            boolean enabled, boolean rollover, boolean pressed
        ) {
            if ( !enabled )
                return;
            NimbusScheme s = schemeOf(p);
            ScrollEnd end = pressed ? ScrollEnd.PRESSED : rollover ? ScrollEnd.MOUSE_OVER : ScrollEnd.ENABLED;
            float scale = UI.scale();
            LafUtilities.antialiasShapes(g);
            g.scale(scale, scale);
            float length = scrollStepperLength();
            switch ( direction ) {
                case RIGHT: g.transform(new java.awt.geom.AffineTransform(-1, 0, 0, 1, length, 0)); break;
                case UP:    g.transform(new java.awt.geom.AffineTransform(0, 1, 1, 0, 0, 0)); break;
                case DOWN:  g.transform(new java.awt.geom.AffineTransform(0, -1, 1, 0, 0, length)); break;
                default: break;
            }
            g.setPaint(end.body.paint(s, 12.5f, 0, 12.5f, 15));
            g.fill(SCROLL_END_BODY);
            g.setPaint(end.shade.paint(s, 0, 8.01f, 1.11f, 8.01f));
            g.fill(SCROLL_END_SHADE);
            if ( end.arrow != null ) {
                g.setPaint(end.arrow.paint(s, 11.47f, 10.5f, 11.47f, 4.03f));
                g.fill(polygon(12, 4, 12, 11, 4.94f, 7.5f));
            } else
                fill(g, polygon(12, 4, 12, 11, 4.94f, 7.5f), SCROLL_END_ARROW_PRESSED.in(s));
            fill(g, SCROLL_END_GLINT, SCROLL_END_GLINT_COLOR.in(s));
        }

        @Override public int scrollStepperLength() { return 25; }

        /**
         *  The thumb: square along the side of the bar next to what it scrolls, and rounded off at
         *  both ends along the other side, in the default button's blue with a glint in each corner.
         */
        @Override
        public void paintScrollThumb( Graphics2D g, Palette p, Rectangle r, boolean active ) {
            NimbusScheme s = schemeOf(p);
            float   scale    = UI.scale();
            boolean vertical = r.height >= r.width;
            float   length   = ( vertical ? r.height : r.width ) / scale;
            LafUtilities.antialiasShapes(g);
            g.translate(r.x, r.y);
            g.scale(scale, scale);
            if ( vertical )
                g.transform(new java.awt.geom.AffineTransform(0, 1, 1, 0, 0, 0));
            NimbusScheme.Gradient[] thumb = active ? THUMB_MOUSE_OVER : THUMB;
            float l = length;
            Path2D.Float outer = new Path2D.Float();
            outer.moveTo(0, 0); outer.lineTo(0, 1); outer.curveTo(0, 7, 5, 15, 15, 15); outer.lineTo(l - 15, 15);
            outer.curveTo(l - 5, 15, l, 7, l, 1); outer.lineTo(l, 0); outer.closePath();
            g.setPaint(thumb[0].paint(s, 0, 15));
            g.fill(outer);
            Path2D.Float inner = new Path2D.Float();
            inner.moveTo(1, 0); inner.lineTo(1, 1); inner.curveTo(0.95f, 9.45f, 9.14f, 14, 15, 14); inner.lineTo(l - 15, 14);
            inner.curveTo(l - 9.09f, 14, l - 1.05f, 9.36f, l - 1, 1); inner.lineTo(l - 1, 0); inner.closePath();
            g.setPaint(thumb[1].paint(s, 0, 14));
            g.fill(inner);
            Path2D.Float glint = new Path2D.Float();
            glint.moveTo(6, 0); glint.lineTo(1, 0); glint.lineTo(2.41f, 7.64f); glint.curveTo(2.41f, 7.64f, 2.05f, 3.59f, 3, 2.05f);
            glint.curveTo(3.95f, 0.5f, 6, 0, 6, 0); glint.closePath();
            g.setPaint(THUMB_GLINT_LEFT.paint(s, 1.34f, -0.05f, 2.84f, 1.83f));
            g.fill(glint);
            Path2D.Float otherGlint = new Path2D.Float();
            otherGlint.moveTo(l - 0.95f, 0); otherGlint.lineTo(l - 5.95f, 0);
            otherGlint.curveTo(l - 5.95f, 0, l - 4, 0.77f, l - 3.32f, 2);
            otherGlint.curveTo(l - 2.64f, 3.23f, l - 2.41f, 7.59f, l - 2.41f, 7.59f); otherGlint.closePath();
            g.setPaint(THUMB_GLINT_RIGHT.paint(s, l - 1.25f, 0.27f, l - 2.98f, 2));
            g.fill(otherGlint);
        }

        private static final Shape SCROLL_END_BODY;
        private static final Shape SCROLL_END_GLINT;
        private static final Shape SCROLL_END_SHADE = polygon(0, 1.03f, 0.97f, 1.5f, 1.94f, 2.03f, 1.94f, 15, 0, 15);

        static {
            Path2D.Float body = new Path2D.Float();
            body.moveTo(0, 0); body.lineTo(17, 0); body.curveTo(17, 0, 16.29f, 1.97f, 17, 5);
            body.curveTo(17.71f, 8.03f, 18, 9.06f, 20, 11); body.curveTo(22, 12.94f, 25, 14, 25, 14);
            body.lineTo(25, 15); body.lineTo(0, 15); body.closePath();
            SCROLL_END_BODY = body;
            Path2D.Float glint = new Path2D.Float();
            glint.moveTo(16.44f, 2); glint.curveTo(17.18f, 2, 16.62f, 4.12f, 17.53f, 6.32f);
            glint.curveTo(18.44f, 8.53f, 18.06f, 9.47f, 20.41f, 11.32f); glint.curveTo(22.76f, 13.18f, 24.5f, 14.24f, 24.5f, 14.24f);
            glint.lineTo(24.35f, 14.82f); glint.curveTo(24.35f, 14.82f, 21.38f, 13.35f, 19.82f, 11.97f);
            glint.curveTo(18.26f, 10.59f, 17.76f, 9.29f, 16.97f, 7.29f); glint.curveTo(16.18f, 5.29f, 15.71f, 2, 16.44f, 2);
            glint.closePath();
            SCROLL_END_GLINT = glint;
        }

        private static final NimbusScheme.Shade SCROLL_END_GLINT_COLOR   = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.110526316, 0.25490195, -165);
        private static final NimbusScheme.Shade SCROLL_END_ARROW_PRESSED = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.6111111, -0.110526316, -0.74509805);

        /** One state of the end of a scroll bar, from ScrollBarButtonPainter. */
        private enum ScrollEnd
        {
            ENABLED( body(bg(-0.01111114, -0.07763158, -0.1490196), bg(-0.111111104, -0.10580933, 0.086274505), bg(-0.027777791, -0.102261856, 0.20392156),
                          bg(-0.039682567, -0.079276316, 0.13333333), bg(-0.027777791, -0.07382907, 0.109803915), bg(-0.039682567, -0.08241387, 0.23137254)),
                     NimbusScheme.gradient(new double[]{ 0, 0.5, 1 }, bg(-0.055555522, -0.08443936, -0.29411766, -136), NimbusScheme.MID, bg(-0.055555522, -0.09876161, 0.25490195, -178)),
                     arrow(bg(0.055555582, -0.08878718, -0.5647059), bg(-0.027777791, -0.080223285, -0.4862745), bg(-0.111111104, -0.09525914, -0.23137254)) ),
            MOUSE_OVER( body(bg(-0.04444444, -0.080223285, -0.09803921), bg(-0.6111111, -0.110526316, 0.10588235), bg(0, -0.110526316, 0.25490195),
                             bg(-0.039682567, -0.081719734, 0.20784312), bg(-0.027777791, -0.07677104, 0.18431371), bg(0, -0.110526316, 0.25490195)),
                     NimbusScheme.gradient(new double[]{ 0.195, 0.598, 1 }, bg(-0.04444444, -0.080223285, -0.09803921, -69), NimbusScheme.MID, bg(-0.055555522, -0.09876161, 0.25490195, -39)),
                     arrow(bg(0.055555582, -0.0951417, -0.49019608), bg(-0.027777791, -0.086996906, -0.4117647), bg(-0.111111104, -0.09719298, -0.15686274)) ),
            PRESSED( body(bg(-0.037037015, -0.043859646, -0.21568626), bg(-0.06349206, -0.07309316, -0.011764705), bg(-0.048611104, -0.07296763, 0.09019607),
                          bg(-0.03535354, -0.05497076, 0.031372547), bg(-0.034188032, -0.043168806, 0.011764705), bg(-0.03535354, -0.0600676, 0.109803915)),
                     NimbusScheme.gradient(new double[]{ 0, 0.5, 1 }, bg(-0.037037015, -0.043859646, -0.21568626, -44), NimbusScheme.MID, bg(-0.055555522, -0.09876161, 0.25490195, -178)),
                     null );

            final NimbusScheme.Gradient body;
            final NimbusScheme.Gradient shade;
            final NimbusScheme.@Nullable Gradient arrow;

            ScrollEnd( NimbusScheme.Gradient body, NimbusScheme.Gradient shade, NimbusScheme.@Nullable Gradient arrow ) {
                this.body = body; this.shade = shade; this.arrow = arrow;
            }

            private static NimbusScheme.Shade bg( double h, double s, double b ) { return NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, h, s, b); }
            private static NimbusScheme.Shade bg( double h, double s, double b, int a ) { return NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, h, s, b, a); }

            private static NimbusScheme.Gradient body( NimbusScheme.Shade a, NimbusScheme.Shade b, NimbusScheme.Shade c, NimbusScheme.Shade d, NimbusScheme.Shade e, NimbusScheme.Shade f ) {
                return NimbusScheme.gradient(new double[]{ 0, 0.033, 0.066, 0.09, 0.114, 0.231, 0.347, 0.494, 0.641, 0.784, 0.928 },
                                             a, NimbusScheme.MID, b, NimbusScheme.MID, c, NimbusScheme.MID, d, NimbusScheme.MID, e, NimbusScheme.MID, f);
            }

            private static NimbusScheme.Gradient arrow( NimbusScheme.Shade a, NimbusScheme.Shade b, NimbusScheme.Shade c ) {
                return NimbusScheme.gradient(new double[]{ 0, 0.296, 0.593, 0.793, 0.994 }, a, NimbusScheme.MID, b, NimbusScheme.MID, c);
            }
        }

        private static final NimbusScheme.Gradient[] THUMB = thumb(
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.00051498413, 0.18061227, -0.35686278), NimbusScheme.shade(NimbusScheme.Key.BASE, 0.00051498413, -0.21018237, -0.18039218),
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.000713408, -0.53277314, 0.25098038), NimbusScheme.shade(NimbusScheme.Key.BASE, -0.07865167, -0.6317617, 0.44313723),
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.00051498413, -0.44340658, 0.26666665), NimbusScheme.shade(NimbusScheme.Key.BASE, 0.00051498413, -0.4669379, 0.38039213),
                NimbusScheme.shade(NimbusScheme.Key.BASE, -0.07865167, -0.56512606, 0.45098037));
        private static final NimbusScheme.Gradient[] THUMB_MOUSE_OVER = thumb(
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.00051498413, 0.18061227, -0.35686278), NimbusScheme.shade(NimbusScheme.Key.BASE, 0.00051498413, -0.21018237, -0.18039218),
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.010237217, -0.5621849, 0.25098038), NimbusScheme.shade(NimbusScheme.Key.BASE, 0.08801502, -0.6317773, 0.4470588),
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.00051498413, -0.45950285, 0.34117645), NimbusScheme.shade(NimbusScheme.Key.BASE, -0.0017285943, -0.48277313, 0.45098037),
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0, -0.6357143, 0.45098037));
        private static final NimbusScheme.Gradient THUMB_GLINT_LEFT = NimbusScheme.gradient(new double[]{ 0, 0.5, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BASE, -0.0017285943, -0.362987, 0.011764705), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BASE, 0.0000520349, -0.41753247, 0.09803921, -222));
        private static final NimbusScheme.Gradient THUMB_GLINT_RIGHT = NimbusScheme.gradient(new double[]{ 0, 0.5, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BASE, -0.0017285943, -0.362987, 0.011764705), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BASE, -0.0017285943, -0.362987, 0.011764705, -255));

        /** The outline and the face of a scroll thumb, in that order. */
        private static NimbusScheme.Gradient[] thumb(
            NimbusScheme.Shade edgeTop, NimbusScheme.Shade edgeBottom,
            NimbusScheme.Shade a, NimbusScheme.Shade b, NimbusScheme.Shade c, NimbusScheme.Shade d, NimbusScheme.Shade e
        ) {
            return new NimbusScheme.Gradient[]{
                NimbusScheme.gradient(new double[]{ 0, 0.5, 1 }, edgeTop, NimbusScheme.MID, edgeBottom),
                NimbusScheme.gradient(new double[]{ 0.039, 0.051, 0.063, 0.196, 0.329, 0.49, 0.65, 0.825, 1 },
                                      a, NimbusScheme.MID, b, NimbusScheme.MID, c, NimbusScheme.MID, d, NimbusScheme.MID, e)
            };
        }

        /**
         *  A split pane's divider: a dark rule along both of its sides with a highlight just inside
         *  each, and a small rounded grip in its middle.
         */
        @Override
        public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
            NimbusScheme s = schemeOf(p);
            float scale = UI.scale();
            // Nimbus draws the divider of a split stacked top and bottom, and turns it for the other.
            float length = ( horizontalSplit ? h : w ) / scale;
            float across = ( horizontalSplit ? w : h ) / scale;
            LafUtilities.antialiasShapes(g);
            g.scale(scale, scale);
            if ( horizontalSplit )
                g.transform(new java.awt.geom.AffineTransform(0, 1, 1, 0, 0, 0));
            g.setPaint(DIVIDER.paint(s, 0, across));
            g.fill(new java.awt.geom.Rectangle2D.Float(0, 0, length, across));
            float middle = length / 2f;
            if ( horizontalSplit ) {
                g.setPaint(GRIP_EDGE_ACROSS.paint(s, middle - 9.5f, across / 2f - 2, middle + 9.5f, across / 2f));
                g.fill(new RoundRectangle2D.Float(middle - 9.5f, across / 2f - 2, 19, 4, 4, 4));
                g.setPaint(GRIP_FACE_ACROSS.paint(s, middle - 8.5f, 0, middle + 8, 0));
                g.fill(new java.awt.geom.Rectangle2D.Float(middle - 8.5f, across / 2f - 1, 16.5f, 2));
            } else {
                g.setPaint(GRIP_EDGE.paint(s, across / 2f - 2, across / 2f + 3));
                g.fill(new RoundRectangle2D.Float(middle - 9, across / 2f - 2, 18, 5, 3.7f, 3.7f));
                g.setPaint(GRIP_FACE.paint(s, across / 2f - 1, across / 2f + 2));
                g.fill(new RoundRectangle2D.Float(middle - 8, across / 2f - 1, 16, 3, 4, 4));
            }
        }

        private static final NimbusScheme.Gradient DIVIDER = NimbusScheme.gradient(new double[]{ 0.058, 0.081, 0.103, 0.116, 0.129, 0.434, 0.739, 0.779, 0.819, 0.858, 0.897 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.017358616, -0.11372548), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.055555582, -0.102396235, 0.21960783), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.07016757, 0.12941176), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.07016757, 0.12941176), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.055555582, -0.102396235, 0.21960783), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.017358616, -0.11372548));
        private static final NimbusScheme.Gradient GRIP_EDGE = NimbusScheme.gradient(new double[]{ 0.206, 0.5, 0.794 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.017358616, -0.11372548), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.110526316, 0.25490195));
        private static final NimbusScheme.Gradient GRIP_FACE = NimbusScheme.gradient(new double[]{ 0.09, 0.295, 0.5, 0.582, 0.665 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.048026316, 0.007843137), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.0055555105, -0.06970999, 0.21568626), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.06704806, 0.06666666));
        private static final NimbusScheme.Gradient GRIP_EDGE_ACROSS = NimbusScheme.gradient(new double[]{ 0, 0.421, 0.842, 0.895, 0.948 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.019617222, -0.09803921), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0.004273474, -0.03790062, -0.043137252), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.111111104, -0.106573746, 0.24705881));
        private static final NimbusScheme.Gradient GRIP_FACE_ACROSS = NimbusScheme.gradient(new double[]{ 0, 0.081, 0.161, 0.513, 0.865, 0.885, 0.906 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.049301825, 0.02352941), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.006944418, -0.07399663, 0.11372548), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.018518567, -0.06998578, 0.12549019), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.050526317, 0.039215684));

        /**
         *  The handle a floatable tool bar is dragged by: a strip ten pixels wide shading from white
         *  into the control colour, closed by a rule, with its two outer corners nicked.
         */
        @Override
        public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {
            NimbusScheme s = schemeOf(p);
            float scale = UI.scale();
            float along = ( horizontal ? h : w ) / scale;
            LafUtilities.antialiasShapes(g);
            g.scale(scale, scale);
            if ( !horizontal )
                g.transform(new java.awt.geom.AffineTransform(0, 1, 1, 0, 0, 0));
            g.setPaint(HANDLE.paint(s, 0, 0, 10, 0));
            g.fill(new java.awt.geom.Rectangle2D.Float(0, 0, 10, along));
            fill(g, new java.awt.geom.Rectangle2D.Float(10, 0, 1, along), HANDLE_RULE.in(s));
            fill(g, polygon(0, 0, 0, 2, 2, 0), HANDLE_CORNER.in(s));
            fill(g, polygon(0, along, 0, along - 2, 2, along), HANDLE_CORNER.in(s));
        }

        private static final NimbusScheme.Gradient HANDLE = NimbusScheme.gradient(new double[]{ 0, 0.5, 1 },
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, 0, -0.110526316, 0.25490195), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.006944418, -0.07399663, 0.11372548));
        private static final NimbusScheme.Shade HANDLE_RULE   = NimbusScheme.shade(NimbusScheme.Key.BORDER, 0, -0.029675633, 0.109803915);
        private static final NimbusScheme.Shade HANDLE_CORNER = NimbusScheme.shade(NimbusScheme.Key.BLUE_GREY, -0.008547008, -0.03494492, -0.07058823);

        /**
         *  The filled part of a progress bar: a bar of {@code nimbusOrange} under a hard sheen, laid
         *  over the trough's own edge, which is how Nimbus draws it. The soft glow around it lies in
         *  the bar's margin, where the look and feel's painting is clipped away, so the style rule
         *  paints that, see {@link #paintProgressGlow}.
         */
        @Override
        public void paintProgressFill(
            Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
        ) {
            if ( ratio <= 0 )
                return;
            NimbusScheme s = schemeOf(p);
            float scale  = UI.scale();
            float length = ( horizontal ? w : h ) / scale;
            float across = ( horizontal ? h : w ) / scale;
            float end    = Math.round(length * ratio);
            LafUtilities.antialiasShapes(g);
            g.scale(scale, scale);
            if ( !horizontal )
                g.transform(new java.awt.geom.AffineTransform(0, -1, 1, 0, 0, length));
            g.setPaint(( enabled ? FILL_EDGE : FILL_EDGE_DISABLED ).paint(s, 0, across));
            OptimizedShapeRendering.fill(g, new java.awt.geom.Rectangle2D.Float(0, 0, end, across));
            g.setPaint(( enabled ? FILL_FACE : FILL_FACE_DISABLED ).paint(s, 1, across - 1));
            OptimizedShapeRendering.fill(g, new java.awt.geom.Rectangle2D.Float(1, 1, end - 2, across - 2));
        }

        /**
         *  The glow around the filled part of a progress bar: a ring reaching out of the trough into the
         *  bar's margin, rounded at the start, open at the moving end until the bar is full.
         *
         * @param g the context, at the corner of the trough the glow runs along
         * @param s the Nimbus colours in force
         * @param w the width of that trough
         * @param h its height
         * @param ratio how much of it is filled
         * @param horizontal whether it fills from left to right rather than from bottom to top
         * @param enabled whether it is enabled
         */
        static void paintProgressGlow(
            Graphics2D g, NimbusScheme s, float w, float h, double ratio, boolean horizontal, boolean enabled
        ) {
            if ( ratio <= 0 )
                return;
            float   length   = horizontal ? w : h;
            float   across   = horizontal ? h : w;
            float   filled   = Math.round(length * ratio);
            boolean finished = ratio >= 1;
            LafUtilities.antialiasShapes(g);
            if ( !horizontal )
                g.transform(new java.awt.geom.AffineTransform(0, -1, 1, 0, 0, length));
            // The ring lies outside the trough, in the bar's margin, reaching this far past it:
            final float reach = 1.37f;
            java.awt.geom.Area glow = new java.awt.geom.Area(new RoundRectangle2D.Float(
                    -reach, -reach, finished ? filled + 2 * reach : filled + 5, across + 2 * reach, 4.7f, 4.7f));
            if ( !finished )
                glow.intersect(new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Float(-reach, -reach, filled + reach, across + 2 * reach)));
            glow.subtract(new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Float(0, 0, filled, across)));
            fill(g, glow, ( enabled ? FILL_GLOW : FILL_GLOW_DISABLED ).in(s));
        }

        private static final NimbusScheme.Shade FILL_GLOW          = NimbusScheme.shade(NimbusScheme.Key.ORANGE, 0, 0, 0, -156);
        private static final NimbusScheme.Shade FILL_GLOW_DISABLED = NimbusScheme.shade(NimbusScheme.Key.ORANGE, 0.024554357, -0.8873145, 0.10588235, -156);
        private static final NimbusScheme.Gradient FILL_EDGE = NimbusScheme.gradient(new double[]{ 0.039, 0.055, 0.071, 0.281, 0.49, 0.697, 0.903, 0.924, 0.945 },
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, -0.015796512, 0.02094239, -0.15294117), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, -0.004321605, 0.02094239, -0.0745098), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, -0.008021399, 0.02094239, -0.10196078), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, -0.011706904, -0.1790576, -0.02352941), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, -0.048691254, 0.02094239, -0.3019608));
        private static final NimbusScheme.Gradient FILL_FACE = NimbusScheme.gradient(new double[]{ 0.039, 0.061, 0.084, 0.273, 0.461, 0.49, 0.519, 0.718, 0.916, 0.924, 0.932 },
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, 0.003940329, -0.7375322, 0.17647058), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, 0.005506739, -0.46764207, 0.109803915), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, 0.0042127445, -0.18595415, 0.04705882), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, 0.0047626942, 0.02094239, 0.0039215684), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, 0.0047626942, -0.15147138, 0.1607843), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, 0.010665476, -0.27317524, 0.25098038));
        private static final NimbusScheme.Gradient FILL_EDGE_DISABLED = NimbusScheme.gradient(new double[]{ 0.039, 0.055, 0.071, 0.281, 0.49, 0.697, 0.903, 0.924, 0.945 },
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, -0.023593787, -0.7963165, 0.02352941), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, -0.010608241, -0.7760873, 0.043137252), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, -0.015402906, -0.7840576, 0.035294116), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, -0.017112307, -0.8091547, 0.058823526), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, -0.07044564, -0.844649, -0.019607842));
        private static final NimbusScheme.Gradient FILL_FACE_DISABLED = NimbusScheme.gradient(new double[]{ 0.039, 0.061, 0.084, 0.273, 0.461, 0.49, 0.519, 0.718, 0.916, 0.924, 0.932 },
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, -0.009704903, -0.9381485, 0.11372548), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, -0.00044563413, -0.86742973, 0.09411764), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, -0.00044563413, -0.79896283, 0.07843137), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, 0.0013274103, -0.7530961, 0.06666666), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, 0.0013274103, -0.7644457, 0.109803915), NimbusScheme.MID,
                NimbusScheme.shade(NimbusScheme.Key.ORANGE, 0.009244293, -0.78794646, 0.13333333));

        /**
         *  A tab: rounded at its top corners, outlined in a dark blue and filled pale, and filled with
         *  the default button's blue when it is the one whose page shows. The tab whose page shows has
         *  no outline along its bottom, so its face runs into {@link #paintTabEdge}'s band.
         */
        @Override
        public void paintTabSurface(
            Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
        ) {
            NimbusScheme s = schemeOf(p);
            TabLook look = selected ? ( rollover ? TabLook.MOUSE_OVER_SELECTED : TabLook.SELECTED )
                                    : ( rollover ? TabLook.MOUSE_OVER : TabLook.ENABLED );
            float scale = UI.scale();
            float tw = w / scale, th = h / scale;
            LafUtilities.antialiasShapes(g);
            g.translate(x, y);
            g.scale(scale, scale);
            Path2D.Float edge = new Path2D.Float();
            edge.moveTo(0, 5); edge.curveTo(0, 2, 2, 0, 5, 0); edge.lineTo(tw - 5, 0); edge.curveTo(tw - 2, 0, tw, 2, tw, 5);
            edge.lineTo(tw, th); edge.lineTo(0, th); edge.closePath();
            g.setPaint(look.edge.paint(s, 0, th));
            g.fill(edge);
            float bottom = selected ? th : th - 1;
            Path2D.Float face = new Path2D.Float();
            face.moveTo(1, bottom); face.lineTo(1, 6); face.curveTo(1, 2.44f, 2.56f, 1, 6, 1); face.lineTo(tw - 6, 1);
            face.curveTo(tw - 2.67f, 1, tw - 1, 2.72f, tw - 1, 6); face.lineTo(tw - 1, bottom); face.closePath();
            g.setPaint(look.face.paint(s, 1, bottom));
            g.fill(face);
        }

        /** Nothing: the selected tab is already the only one whose colour runs on into
         *  {@link #paintTabEdge}'s band, which is a stronger mark than a line and is the one
         *  Nimbus uses. */
        @Override
        public void paintTabAccent(
            Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
        ) {}

        @Override public int tabEdgeThickness() { return 5; }

        /**
         *  A dark rule, three pixels of pale blue shading darker away from the tabs, and a second
         *  dark rule. Along the tab whose page shows, the first rule is that tab's face instead, so
         *  the tab and its page read as one shape and every other tab stops at a line.
         */
        @Override
        public void paintTabEdge(
            Graphics2D g, Palette p, Rectangle edge, @Nullable Rectangle selectedTab, int tabPlacement
        ) {
            NimbusScheme s = schemeOf(p);
            boolean vertical  = tabPlacement == SwingConstants.LEFT || tabPlacement == SwingConstants.RIGHT;
            boolean fromStart = tabPlacement == SwingConstants.TOP || tabPlacement == SwingConstants.LEFT;
            int     depth     = vertical ? edge.width : edge.height;
            Color   rule      = TAB_EDGE_RULE.in(s);
            Color   upper     = TAB_EDGE_UPPER.in(s);
            Color   lower     = TAB_EDGE_LOWER.in(s);
            Color[] rows      = { rule, upper, NimbusScheme.midpoint(upper, lower), lower, rule };
            for ( int i = 0; i < rows.length; i++ ) {
                int from = depth * i / rows.length;
                int to   = depth * ( i + 1 ) / rows.length;
                g.setColor(rows[fromStart ? i : rows.length - 1 - i]);
                fillAcross(g, edge, vertical, from, to - from);
            }
            if ( selectedTab == null )
                return;
            int first = depth / rows.length;
            g.setColor(TabLook.SELECTED.face.colors(s)[TabLook.SELECTED.face.fractions().length - 1]);
            fillUnder(g, edge, selectedTab, vertical, fromStart ? 0 : depth - first, first);
        }

        /** Nimbus writes every tab's label in the ordinary ink, whichever tab is selected. */
        @Override
        public Color tabText( Palette p, boolean selected, boolean enabled ) {
            NimbusScheme s = schemeOf(p);
            return s.get(enabled ? NimbusScheme.Key.TEXT : NimbusScheme.Key.DISABLED_TEXT);
        }

        private static final NimbusScheme.Shade TAB_EDGE_RULE  = NimbusScheme.shade(NimbusScheme.Key.BASE, 0.08801502, 0.3642857, -0.4784314);
        private static final NimbusScheme.Shade TAB_EDGE_UPPER = NimbusScheme.shade(NimbusScheme.Key.BASE, 0.00051498413, -0.45471883, 0.31764704);
        private static final NimbusScheme.Shade TAB_EDGE_LOWER = NimbusScheme.shade(NimbusScheme.Key.BASE, 0.00051498413, -0.4633005, 0.3607843);

        /** One state of a tab, from TabbedPaneTabPainter. */
        private enum TabLook
        {
            ENABLED( three(base(0.032459438, -0.55535716, -0.109803945), base(0.08801502, 0.3642857, -0.4784314)),
                     NimbusScheme.gradient(new double[]{ 0, 0.1, 0.2, 0.6, 1 }, base(0.08801502, -0.63174605, 0.43921566), NimbusScheme.MID,
                                           base(0.05468172, -0.6145278, 0.37647057), NimbusScheme.MID, base(0.032459438, -0.5953556, 0.32549018)) ),
            MOUSE_OVER( three(base(0.032459438, -0.54616207, -0.02352941), base(0.08801502, 0.3642857, -0.4784314)),
                     NimbusScheme.gradient(new double[]{ 0, 0.1, 0.2, 0.6, 1 }, base(0.08801502, -0.6317773, 0.4470588), NimbusScheme.MID,
                                           base(0.021348298, -0.61547136, 0.41960782), NimbusScheme.MID, base(0.032459438, -0.5985242, 0.39999998)) ),
            SELECTED( three(base(0.00051498413, -0.08776909, -0.2627451), base(0.08801502, 0.3642857, -0.4784314)),
                     NimbusScheme.gradient(new double[]{ 0, 0.124, 0.248, 0.426, 0.603, 0.685, 0.768, 0.884, 1 },
                                           base(0.004681647, -0.6197143, 0.43137252), NimbusScheme.MID, base(0.000713408, -0.543609, 0.34509802), NimbusScheme.MID,
                                           base(-0.0020751357, -0.45610264, 0.2588235), NimbusScheme.MID, base(0.00051498413, -0.43866998, 0.24705881), NimbusScheme.MID,
                                           base(0.00051498413, -0.44879842, 0.29019606)) ),
            MOUSE_OVER_SELECTED( three(base(0.06332368, 0.3642857, -0.4431373), base(0.08801502, 0.3642857, -0.4784314)),
                     NimbusScheme.gradient(new double[]{ 0, 0.124, 0.248, 0.426, 0.603, 0.685, 0.768, 0.868, 0.968 },
                                           base(0.004681647, -0.6198413, 0.43921566), NimbusScheme.MID, base(-0.0022627711, -0.5335866, 0.372549), NimbusScheme.MID,
                                           base(-0.0017285943, -0.4608264, 0.32549018), NimbusScheme.MID, base(0.00051498413, -0.4555341, 0.3215686), NimbusScheme.MID,
                                           base(0.00051498413, -0.46404046, 0.36470586)) );

            final NimbusScheme.Gradient edge;
            final NimbusScheme.Gradient face;

            TabLook( NimbusScheme.Gradient edge, NimbusScheme.Gradient face ) { this.edge = edge; this.face = face; }

            private static NimbusScheme.Shade base( double h, double s, double b ) { return NimbusScheme.shade(NimbusScheme.Key.BASE, h, s, b); }

            private static NimbusScheme.Gradient three( NimbusScheme.Shade top, NimbusScheme.Shade bottom ) {
                return NimbusScheme.gradient(new double[]{ 0, 0.5, 1 }, top, NimbusScheme.MID, bottom);
            }
        }

        /** Fills a slice of the edge the whole way along it, {@code depth} pixels in from the edge's
         *  own origin and {@code thickness} pixels deep. */
        private static void fillAcross( Graphics2D g, Rectangle edge, boolean vertical, int depth, int thickness ) {
            if ( vertical )
                g.fillRect(edge.x + depth, edge.y, thickness, edge.height);
            else
                g.fillRect(edge.x, edge.y + depth, edge.width, thickness);
        }

        /** The same slice, but only where the selected tab lies over it. */
        private static void fillUnder(
            Graphics2D g, Rectangle edge, Rectangle tab, boolean vertical, int depth, int thickness
        ) {
            if ( vertical )
                g.fillRect(edge.x + depth, tab.y, thickness, tab.height);
            else
                g.fillRect(tab.x, edge.y + depth, tab.width, thickness);
        }
    }
}

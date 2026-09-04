package examples.laf;

import sprouts.Action;
import sprouts.From;
import sprouts.Subscriber;
import sprouts.ValDelegate;
import swingtree.UI;
import swingtree.api.Painter;
import swingtree.style.ComponentExtension;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.event.CaretListener;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Map;

/**
 *  The shared helpers of {@link SwingTreeLookAndFeel}: colour arithmetic for the style presets,
 *  drawing primitives for the symbol sets, and the painting and listener plumbing every UI
 *  delegate repeats.
 */
final class LafUtilities
{
    private LafUtilities() {}

    /** Which way {@link #arrowShape} points. */
    enum Direction { UP, DOWN, LEFT, RIGHT }


    // ── Colour ───────────────────────────────────────────────────────────

    /** Moves {@code base} the given fraction of the way towards white. */
    static Color shadeTowardsWhite( Color base, double amount ) { return shadeTowards(base, Color.WHITE, amount); }

    /** Moves {@code base} the given fraction of the way towards black. */
    static Color shadeTowardsBlack( Color base, double amount ) { return shadeTowards(base, Color.BLACK, amount); }

    /** Mixes two colours, opacity included. */
    static Color shadeTowards( Color from, Color to, double amount ) {
        double t = Math.max(0, Math.min(1, amount));
        return new Color(
            channel(from.getRed(),   to.getRed(),   t),
            channel(from.getGreen(), to.getGreen(), t),
            channel(from.getBlue(),  to.getBlue(),  t),
            channel(from.getAlpha(), to.getAlpha(), t)
        );
    }

    /**
     *  The same colour a fixed number of channel steps lighter (positive) or darker (negative).
     *  A <i>fraction</i> of the way to white moves a dark colour much further than a light one -
     *  the 0.4 that lifts a pale grey by thirteen steps lifts near-black by ninety - so a relief
     *  built out of fractions glares on a dark palette and vanishes on a light one.
     */
    static Color shadeBySteps( Color base, int steps ) {
        return new Color(
            Math.max(0, Math.min(255, base.getRed()   + steps)),
            Math.max(0, Math.min(255, base.getGreen() + steps)),
            Math.max(0, Math.min(255, base.getBlue()  + steps)),
            base.getAlpha()
        );
    }

    /**
     *  The same colour with its saturation and brightness moved by fixed amounts, both on the
     *  0-to-1 scale {@link Color#RGBtoHSB} reports and both clamped at the ends.
     *  <p>
     *  This is how {@link NimbusRelief} keeps its light the same light in every palette.
     *  {@link #shadeTowardsWhite} washes the hue out as it goes, so a stack of mixed shades drifts
     *  towards grey. Moving the brightness leaves the hue alone, so a red base yields light reds
     *  and a blue base light blues, and one set of offsets describes the same relief on either.
     *
     * @param base the colour to move
     * @param saturationOffset how much saturation to add, negative to remove it
     * @param brightnessOffset how much brightness to add, negative to remove it
     * @return the moved colour, at {@code base}'s hue and opacity
     */
    static Color shiftHsb( Color base, double saturationOffset, double brightnessOffset ) {
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        Color shifted = Color.getHSBColor(
                            hsb[0],
                            (float) clamp01(hsb[1] + saturationOffset),
                            (float) clamp01(hsb[2] + brightnessOffset)
                        );
        return base.getAlpha() == 255 ? shifted : withOpacity(shifted, base.getAlpha());
    }

    /**
     *  Washes a colour out towards the ground it will be seen against: the hue is kept, the
     *  saturation is scaled down, and the brightness is moved part of the way to the ground's.
     *  <p>
     *  {@link #shiftHsb} cannot do this. A fixed brightness offset means "lighter", which is right
     *  on a light theme and wrong on a dark one: the {@code +0.26} that turns a deep blue into a
     *  pale blue-grey on paper turns an already bright blue into white on near-black, and the label
     *  on top of it stops being readable. A fraction of the way to the ground means "less itself,
     *  more of the room it is in", which is the same instruction on both.
     *
     * @param colour the colour to wash out
     * @param ground the colour it will be seen against, which it is moved towards
     * @param saturationKept how much of the colour's own saturation survives, 0 to 1
     * @param towardsGround how far to move its brightness towards the ground's, 0 to 1; may be
     *                      slightly negative to move a shade the other way
     * @return the washed colour, at {@code colour}'s hue and opacity
     */
    static Color wash( Color colour, Color ground, double saturationKept, double towardsGround ) {
        float[] from = Color.RGBtoHSB(colour.getRed(), colour.getGreen(), colour.getBlue(), null);
        float[] to   = Color.RGBtoHSB(ground.getRed(), ground.getGreen(), ground.getBlue(), null);
        Color washed = Color.getHSBColor(
                            from[0],
                            (float) clamp01(from[1] * saturationKept),
                            (float) clamp01(from[2] + ( to[2] - from[2] ) * towardsGround)
                       );
        return colour.getAlpha() == 255 ? washed : withOpacity(washed, colour.getAlpha());
    }

    /**
     *  Picks whichever of two inks can be read on a given ground, by comparing luminance. A theme
     *  that derives its colours cannot know in advance whether a surface comes out light or dark,
     *  because that is the palette's business.
     *
     * @return whichever of {@code first} and {@code second} is further from {@code ground}
     */
    static Color readableOn( Color ground, Color first, Color second ) {
        double g = luminance(ground);
        return Math.abs(luminance(first) - g) >= Math.abs(luminance(second) - g) ? first : second;
    }

    private static double luminance( Color c ) {
        return ( 0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue() ) / 255.0;
    }

    private static double clamp01( double value ) { return Math.max(0, Math.min(1, value)); }

    /** The same colour at a different opacity, from 0 to 255. */
    static Color withOpacity( Color base, int alpha ) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static int channel( int from, int to, double t ) {
        return Math.max(0, Math.min(255, (int) Math.round(from + (to - from) * t)));
    }


    // ── Painting ─────────────────────────────────────────────────────────

    /**
     *  Runs a UI delegate's inherited painting inside the SwingTree style engine, which fills the
     *  surface first and hands back a graphics context to draw the chrome and the text on. Two
     *  things about that context have to be put right, and neither is the engine's business.
     *  <p>
     *  <b>The colour and the font.</b> Swing promises that a component's own colour and font are
     *  already on the graphics that reaches {@code paintComponent}, and the inherited painting
     *  relies on that: {@code BasicMenuItemUI.paintText} sets a colour only for a disabled or an
     *  armed row and draws every other row in whatever colour it was handed. A context from
     *  anywhere else therefore paints menu labels in the wrong colour, or in none at all.
     *  <p>
     *  <b>The opacity.</b> The engine works out whether the component is opaque while it paints,
     *  which is one repaint too late for the frame in which a component stops being opaque: Swing
     *  has already skipped repainting what is behind it, and nothing covers the pixels it used to
     *  fill. That is how a menu row filled only while armed leaves its highlight behind. Repainting
     *  the bounds it vacated costs one extra repaint on that one frame.
     */
    static void paintStyled( Graphics g, JComponent c, Painter inheritedPainting ) {
        boolean wasOpaque = c.isOpaque();
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            Object formerShapeAntialiasing = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            applyDesktopTextHints(g2);
            g2.setColor(c.getForeground());
            g2.setFont(c.getFont());
            try {
                inheritedPainting.paint(g2);
            } finally {
                if ( formerShapeAntialiasing != null )
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, formerShapeAntialiasing);
            }
        });
        Container parent = c.getParent();
        if ( wasOpaque && !c.isOpaque() && parent != null )
            parent.repaint(c.getX(), c.getY(), c.getWidth(), c.getHeight());
    }

    /** Turns on shape antialiasing, which every symbol wants and none of them want to repeat. */
    static void antialiasShapes( Graphics2D g ) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /**
     *  Puts the desktop's own text antialiasing settings on a context that is about to draw a
     *  component's text. {@code SwingUtilities2.drawString}, which every {@code BasicXxxUI} draws
     *  through, reads that setting from a client property {@code javax.swing} does not export and
     *  otherwise falls back to the hints on the context, which is the only handle a look and feel
     *  outside the JDK has on it.
     *  <p>
     *  Shape antialiasing is deliberately left alone. The painting that follows draws whole pixels
     *  - rules, grid lines, focus rectangles, a tabbed pane's content edge - and antialiasing a
     *  rectangle already sitting on pixel boundaries runs a coverage rasterization to arrive at
     *  the pixels a plain fill writes directly. Every curve and diagonal belongs to a
     *  {@link Symbols} glyph or to a surface the style engine paints, and both turn the hint on
     *  for themselves. Leaving it off here took a fifth off a repaint of the Linen preset and four
     *  fifths off one of {@link SwingTreeLookAndFeel.StylePreset#BLANK}, changing no pixel of
     *  either.
     */
    private static void applyDesktopTextHints( Graphics2D g ) {
        RenderingHints hints = textHints;
        if ( hints == null ) {
            hints = readDesktopTextHints();
            textHints = hints;
        }
        g.addRenderingHints(hints);
    }

    /**
     *  The hints last read from the desktop, or null until they are read again. Reading them is a
     *  lookup in the {@code UIManager} defaults and, when that misses, one in the toolkit's desktop
     *  properties, once per component per repaint: 1.3% of the event thread on the showcase. Both
     *  sources announce their own changes, and the static block that follows clears this field on
     *  either announcement.
     */
    private static volatile RenderingHints textHints = null;

    static {
        Toolkit.getDefaultToolkit()
               .addPropertyChangeListener("awt.font.desktophints", event -> textHints = null);
        UIManager.addPropertyChangeListener(event -> {
            if ( "lookAndFeel".equals(event.getPropertyName()) )
                textHints = null;
        });
    }

    private static RenderingHints readDesktopTextHints() {
        Object fromManager = UIManager.get("AwtFontDesktopHints");
        Map<?, ?> desktopHints = ( fromManager instanceof Map<?, ?> ) ? (Map<?, ?>) fromManager : null;
        if ( desktopHints == null ) {
            Object fromToolkit = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
            desktopHints = ( fromToolkit instanceof Map<?, ?> ) ? (Map<?, ?>) fromToolkit : null;
        }
        RenderingHints hints = new RenderingHints(null);
        if ( desktopHints != null && !desktopHints.isEmpty() )
            hints.add(new RenderingHints((Map<RenderingHints.Key, ?>) desktopHints));
        else {
            hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            hints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        }
        return hints;
    }

    /** A top-to-bottom two-stop paint, falling back to {@code top} for a shape with no height. */
    static Paint verticalGradient( float y, float height, Color top, Color bottom ) {
        if ( height <= 0.5f )
            return top;
        return new GradientPaint(0, y, top, 0, y + height, bottom);
    }

    /** A glass fill: bright at the top, a hard break just above the middle, then darkening again. */
    static Paint glossGradient( float y, float height, Color base ) {
        if ( height <= 1f )
            return base;
        return new LinearGradientPaint(
            new Point2D.Float(0, y),
            new Point2D.Float(0, y + height),
            new float[]{ 0f, 0.479f, 0.48f, 1f },
            new Color[]{
                shadeTowardsWhite(base, 0.45),
                shadeTowardsWhite(base, 0.16),
                base,
                shadeTowardsBlack(base, 0.14)
            }
        );
    }

    /** A solid arrow head as a closed path, centred on {@code (cx, cy)}. */
    static Path2D.Float arrowShape( float cx, float cy, float halfSpan, float halfDepth, Direction direction ) {
        Path2D.Float path = new Path2D.Float();
        switch ( direction ) {
            case UP:
                path.moveTo(cx - halfSpan, cy + halfDepth);
                path.lineTo(cx,            cy - halfDepth);
                path.lineTo(cx + halfSpan, cy + halfDepth);
                break;
            case DOWN:
                path.moveTo(cx - halfSpan, cy - halfDepth);
                path.lineTo(cx,            cy + halfDepth);
                path.lineTo(cx + halfSpan, cy - halfDepth);
                break;
            case LEFT:
                path.moveTo(cx + halfDepth, cy - halfSpan);
                path.lineTo(cx - halfDepth, cy);
                path.lineTo(cx + halfDepth, cy + halfSpan);
                break;
            case RIGHT:
            default:
                path.moveTo(cx - halfDepth, cy - halfSpan);
                path.lineTo(cx + halfDepth, cy);
                path.lineTo(cx - halfDepth, cy + halfSpan);
                break;
        }
        path.closePath();
        return path;
    }

    /** A tick as an open path inside the given box, so the caller decides how thick it is. */
    static Path2D.Float tickShape( float x, float y, float w, float h ) {
        Path2D.Float path = new Path2D.Float();
        path.moveTo(x + w * 0.22f, y + h * 0.52f);
        path.lineTo(x + w * 0.43f, y + h * 0.73f);
        path.lineTo(x + w * 0.78f, y + h * 0.30f);
        return path;
    }


    // ── Focus ────────────────────────────────────────────────────────────

    /**
     *  Whether a text component is the inside of something that already carries a surface - the page
     *  inside a scroll pane, or the editor inside a spinner or a combo box - so that a rule giving
     *  it a box of its own would draw one box inside another.
     */
    static boolean isInsideAnotherControl( JTextComponent text ) {
        return text.getParent() instanceof JViewport || isControlInternal(text);
    }

    /**
     *  Whether Swing put this component inside a control that already carries a surface. A spinner's
     *  editor is a {@link JPanel} the application never declared, holding a text field it never
     *  declared either, and an editable combo box has an editor of the same kind.
     */
    static boolean isControlInternal( JComponent inner ) {
        return SwingUtilities.getAncestorOfClass(JSpinner.class, inner) != null
            || SwingUtilities.getAncestorOfClass(JComboBox.class, inner) != null;
    }

    /** Whether a combo box should be drawn as focused. An editable one hands the focus to its
     *  editor, so the combo box itself never owns it. */
    static boolean hasFocus( JComboBox<?> combo ) {
        if ( combo.isFocusOwner() )
            return true;
        if ( combo.isEditable() && combo.getEditor() != null ) {
            Component editor = combo.getEditor().getEditorComponent();
            return editor != null && editor.isFocusOwner();
        }
        return false;
    }

    /** Whether a spinner should be drawn as focused. Its editor is a wrapper around the text field
     *  that takes the focus, so this walks one level in. */
    static boolean hasFocus( JSpinner spinner ) {
        Component editor = spinner.getEditor();
        if ( editor instanceof JSpinner.DefaultEditor )
            return ((JSpinner.DefaultEditor) editor).getTextField().isFocusOwner();
        return editor != null && editor.isFocusOwner();
    }

    /**
     *  Repaints {@code target} whenever {@code focusSource} gains or loses focus, so that a style
     *  depending on focus is re-gathered at once instead of at the next unrelated repaint. A style
     *  is only re-gathered while its component paints, and Swing does not repaint a text component
     *  on a focus change: only the caret repaints, and only its own narrow rectangle. Buttons and
     *  sliders need none of this, because their listeners repaint already.
     *  <p>
     *  The two parameters differ for a composite control, where the focus lands on a child of the
     *  component being styled.
     */
    static void repaintOnFocusChange( JComponent target, Component focusSource ) {
        if ( focusSource == null || target.getClientProperty(FOCUS_LISTENER) != null )
            return;
        FocusListener listener = new FocusListener() {
            @Override public void focusGained( FocusEvent e ) { repaintFocusTarget(target); }
            @Override public void focusLost( FocusEvent e )   { repaintFocusTarget(target); }
        };
        focusSource.addFocusListener(listener);
        target.putClientProperty(FOCUS_LISTENER, listener);
    }

    /** Undoes {@link #repaintOnFocusChange}, with the same {@code focusSource}. */
    static void uninstallFocusRepaint( JComponent target, Component focusSource ) {
        Object stored = target.getClientProperty(FOCUS_LISTENER);
        if ( stored instanceof FocusListener && focusSource != null )
            focusSource.removeFocusListener((FocusListener) stored);
        target.putClientProperty(FOCUS_LISTENER, null);
    }

    private static void repaintFocusTarget( JComponent target ) {
        Container parent = target.getParent();
        if ( !(parent instanceof JSpinner.NumberEditor) ) {
            target.repaint();
            return;
        }
        parent.repaint();
        Container spinner = parent.getParent();
        if ( spinner instanceof JSpinner )
            spinner.repaint();
    }

    /**
     *  Repaints {@code target} in full whenever its selection changes. {@code
     *  BasicTextUI.damageRange} repaints only the changed offsets, measured against insets the
     *  style engine installs while the component paints, which is too narrow for a new selection to
     *  appear inside. A caret that moves without selecting anything, which is what typing produces,
     *  keeps Swing's cheaper behaviour.
     */
    static void repaintOnSelectionChange( JTextComponent target ) {
        if ( target.getClientProperty(SELECTION_LISTENER) != null )
            return;
        int[] previous = { target.getCaret() == null ? 0 : target.getCaretPosition(),
                           target.getCaret() == null ? 0 : target.getCaret().getMark() };
        CaretListener listener = event -> {
            boolean had   = previous[0] != previous[1];
            boolean has   = event.getDot() != event.getMark();
            boolean moved = event.getDot() != previous[0] || event.getMark() != previous[1];
            previous[0] = event.getDot();
            previous[1] = event.getMark();
            if ( moved && ( had || has ) )
                target.repaint();
        };
        target.addCaretListener(listener);
        target.putClientProperty(SELECTION_LISTENER, listener);
    }

    /** Undoes {@link #repaintOnSelectionChange}. */
    static void uninstallSelectionRepaint( JTextComponent target ) {
        Object stored = target.getClientProperty(SELECTION_LISTENER);
        if ( stored instanceof CaretListener )
            target.removeCaretListener((CaretListener) stored);
        target.putClientProperty(SELECTION_LISTENER, null);
    }

    /**
     *  Runs {@code scaledMetrics} now, and again after every change of SwingTree's UI scale factor.
     *  <p>
     *  A length a delegate computes while it paints calls {@link UI#scale(int)} afresh each time
     *  and follows the factor by itself. A length the delegate stores on its component instead - a
     *  tree's row height, a split pane's divider thickness - is read back from the component, and
     *  keeps whatever the factor was during {@code installUI} until something writes it again.
     *  <p>
     *  The rewrite is deferred to the end of the event that changed the factor, because a row
     *  height may be a multiple of the component's font size and SwingTree rescales component fonts
     *  during that same event.
     *
     * @param target        the component the lengths are stored on
     * @param scaledMetrics writes those lengths, reading the scale factor as it is when it runs
     */
    static void rescaleOnUiScaleChange( JComponent target, Runnable scaledMetrics ) {
        scaledMetrics.run();
        if ( target.getClientProperty(UI_SCALE_ACTION) != null )
            return;
        Action<ValDelegate<Float>> action = ignored -> UI.runLater(scaledMetrics);
        ComponentExtension.from(target).localUiScaleFactor().onChange(From.ALL, action);
        target.putClientProperty(UI_SCALE_ACTION, action);
    }

    /** Undoes {@link #rescaleOnUiScaleChange}. */
    static void uninstallUiScaleRescale( JComponent target ) {
        Object stored = target.getClientProperty(UI_SCALE_ACTION);
        if ( stored instanceof Subscriber )
            ComponentExtension.from(target).localUiScaleFactor().unsubscribe((Subscriber) stored);
        target.putClientProperty(UI_SCALE_ACTION, null);
    }

    // Each key guards against a second installUI(..) stacking listeners, and is the handle the
    // matching uninstall method removes them by.
    private static final String FOCUS_LISTENER     = "swingtree.laf.focusRepaint.listener";
    private static final String SELECTION_LISTENER = "swingtree.laf.selectionRepaint.listener";
    private static final String UI_SCALE_ACTION    = "swingtree.laf.uiScaleRescale.action";
}

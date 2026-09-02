package examples.laf;

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

    /**
     *  Mixes two colours, opacity included. Presets that model light rather than pigment need
     *  shades a palette has no name for - "this surface, lit" - and computing them keeps a preset
     *  usable with any {@link SwingTreeLookAndFeel.PalettePreset}.
     */
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
     *  <p>
     *  A <i>fraction</i> of the way to white moves a dark colour ten times as far as a light one -
     *  the same 0.4 that lifts a pale grey by thirteen steps lifts near-black by ninety - so a
     *  relief built out of fractions glares on a dark palette and disappears on a light one. A
     *  fixed step behaves the way one light source falling on one material does.
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
     *  The same colour with its saturation and brightness moved by fixed amounts, both measured
     *  on the 0-to-1 scale {@link Color#RGBtoHSB} reports and both clamped at the ends.
     *  <p>
     *  This is how a theme built on one base colour stays one theme when that colour is replaced.
     *  Mixing towards white and black - {@link #shadeTowardsWhite} - washes the hue out as it
     *  goes, so a stack of mixed shades drifts towards grey and a saturated base loses what made
     *  it worth choosing. Moving the brightness leaves the hue untouched, so a red base yields
     *  light reds and a blue base light blues, and one set of offsets describes the same relief on
     *  either. That is also what makes the offsets transferable: they can be read off one colour
     *  scheme by measurement and replayed on another.
     *  <p>
     *  Clamping is the reason a bright base does not simply produce a brighter theme: a stop
     *  already near white cannot move, so the relief flattens where the colour runs out, which is
     *  what a light material does under a light.
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
     *  Washes a colour out towards the ground it is going to be seen against: the hue is kept, the
     *  saturation is scaled down, and the brightness is moved part of the way to the ground's.
     *  <p>
     *  This is what {@link #shiftHsb} cannot do. A fixed brightness offset says "lighter", which is
     *  right on a light theme and wrong on a dark one - the same {@code +0.26} that turns a deep
     *  blue into a pale blue-grey on a paper background turns an already-bright blue into white on
     *  a near-black one, and the label on top of it stops being readable. Moving a <em>fraction of
     *  the way to the ground</em> says "less itself, more of the room it is in", which is the same
     *  instruction on both.
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
     *  Picks whichever of two inks can actually be read on a given ground, by comparing relative
     *  luminance. A theme that derives its colours cannot know in advance whether a surface will
     *  come out light or dark - that is the palette's business - so anywhere the answer decides
     *  legibility it has to be measured rather than assumed.
     *
     * @param ground the colour the text will be drawn on
     * @param first the ink to use unless the second one is easier to read
     * @param second the alternative ink
     * @return whichever of the two is further from {@code ground} in luminance
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
     *  surface first and hands back a graphics context for the chrome and text on top. It also
     *  restores the two things about that context the engine has no reason to know matter.
     *  <p>
     *  <b>The colour and the font.</b> Swing guarantees that a component's own colour and font are
     *  already on the graphics reaching {@code paintComponent}, and inherited painting relies on
     *  that instead of setting a colour of its own: {@code BasicMenuItemUI.paintText} sets one only
     *  for a disabled or an armed row, and draws everything else in whatever colour it is handed.
     *  A context that came from somewhere else therefore renders menu labels invisible.
     *  <p>
     *  <b>The opacity.</b> The engine decides, while it paints, whether the component is opaque -
     *  one repaint too late for the frame in which a component stops being opaque. Swing has by
     *  then already skipped repainting what is behind it, and nothing covers the pixels it used to
     *  fill, which is how a menu row that is only filled while armed leaves its highlight behind.
     *  Repainting the vacated bounds in the parent costs one extra repaint, on that frame only.
     */
    static void paintStyled( Graphics g, JComponent c, Painter inheritedPainting ) {
        boolean wasOpaque = c.isOpaque();
        ComponentExtension.from(c).paintBackground(g, g2 -> {
            antialiasShapesAndText(g2);
            g2.setColor(c.getForeground());
            g2.setFont(c.getFont());
            inheritedPainting.paint(g2);
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
     *  Turns on shape antialiasing and the desktop's own text antialiasing.
     *  <p>
     *  Swing's {@code SwingUtilities2.drawString}, which every {@code BasicXxxUI} draws through,
     *  reads its text antialiasing from a client property {@code javax.swing} does not export,
     *  falling back to the rendering hints already on the context. Merging the toolkit's desktop
     *  hints in here is the only handle a third-party look and feel has on that, and it is also
     *  what makes the text match the rest of the desktop.
     */
    private static void antialiasShapesAndText( Graphics2D g ) {
        Map<?, ?> desktopHints = desktopFontHints();
        if ( desktopHints != null && !desktopHints.isEmpty() )
            g.addRenderingHints(desktopHints);
        else {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private static Map<?, ?> desktopFontHints() {
        Object fromManager = UIManager.get("AwtFontDesktopHints");
        if ( fromManager instanceof Map<?, ?> )
            return (Map<?, ?>) fromManager;
        Object fromToolkit = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        return ( fromToolkit instanceof Map<?, ?> ) ? (Map<?, ?>) fromToolkit : null;
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
     *  Whether a combo box should be drawn as focused. An editable one hands the focus to its
     *  editor, so asking the combo box itself returns {@code false} forever.
     */
    /**
     *  Whether a text component is the inside of a control that already carries a surface of its
     *  own - the page inside a scroll pane, or the editor inside a spinner - so that a rule
     *  giving it a box of its own would draw one box inside another.
     *
     * @param text the text component being styled
     * @return {@code true} if something around it has already been given a surface
     */
    static boolean isInsideAnotherControl( JTextComponent text ) {
        return text.getParent() instanceof JViewport || isControlInternal(text);
    }

    /**
     *  Whether Swing put this component inside a control which already carries a surface of its
     *  own. A spinner's editor is a {@link JPanel} the application never declared, holding a text
     *  field it never declared either, and an editable combo box has an editor of the same kind;
     *  all of them sit inside a box the control around them has already been given, so a rule
     *  filling one of them paints over that control.
     *
     * @param inner the component being styled
     * @return {@code true} if it is a piece of a control's own machinery
     */
    static boolean isControlInternal( JComponent inner ) {
        return SwingUtilities.getAncestorOfClass(JSpinner.class, inner) != null
            || SwingUtilities.getAncestorOfClass(JComboBox.class, inner) != null;
    }

    static boolean hasFocus( JComboBox<?> combo ) {
        if ( combo.isFocusOwner() )
            return true;
        if ( combo.isEditable() && combo.getEditor() != null ) {
            Component editor = combo.getEditor().getEditorComponent();
            return editor != null && editor.isFocusOwner();
        }
        return false;
    }

    /**
     *  Whether a spinner should be drawn as focused. Its editor is a wrapper around the text
     *  field that actually takes the focus, so this walks one level in.
     */
    static boolean hasFocus( JSpinner spinner ) {
        Component editor = spinner.getEditor();
        if ( editor instanceof JSpinner.DefaultEditor )
            return ((JSpinner.DefaultEditor) editor).getTextField().isFocusOwner();
        return editor != null && editor.isFocusOwner();
    }

    /**
     *  Repaints {@code target} whenever {@code focusSource} gains or loses focus, so a
     *  focus-dependent style is re-gathered right away rather than on the next unrelated repaint.
     *  <p>
     *  A style is only re-gathered while its component paints, and Swing does not repaint a text
     *  component on a focus change - only the caret repaints its own narrow rectangle. Buttons
     *  and sliders need none of this; their listeners already repaint. For a composite control
     *  the focus does not even live on the styled component, which is why the source and the
     *  target differ.
     *
     * @param target      the styled component to repaint
     * @param focusSource the component that owns the focus, often the target itself
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
     *  Repaints {@code target} in full whenever its selection changes.
     *  <p>
     *  {@code BasicTextUI.damageRange} repaints only the changed offsets, measured against the
     *  insets the style engine installs as part of the component's own paint - too narrow to be
     *  relied on for a new selection to appear. Bare caret moves, which is what typing produces,
     *  keep Swing's cheap behaviour.
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

    /** Guards against a second {@code installUI(..)} stacking listeners, and is the handle to remove them. */
    private static final String FOCUS_LISTENER     = "swingtree.laf.focusRepaint.listener";
    private static final String SELECTION_LISTENER = "swingtree.laf.selectionRepaint.listener";
}

package examples.selection;

import com.formdev.flatlaf.FlatLightLaf;
import sprouts.Var;
import swingtree.UI;
import swingtree.api.Painter;
import swingtree.components.JBox;
import swingtree.layout.Bounds;
import swingtree.layout.Position;
import swingtree.style.StyledString;
import swingtree.style.TextSelection;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.text.BreakIterator;
import java.util.Optional;

/**
 *  A styled text which your users can select in two ways, switched with the "Touch mode" checkbox.
 *  <p>
 *  In desktop mode, which is the default, the text behaves as {@code withTextSelection} makes it behave:
 *  your users drag the mouse to select, double click a word, shift-click to extend a selection, and press
 *  Ctrl+A and Ctrl+C.
 *  <p>
 *  In touch mode, a transparent overlay lies exactly on top of the text and receives every press of the mouse
 *  or the finger, so the desktop behaviour underneath never sees them. The overlay uses only the coordinate methods
 *  of {@link TextSelection}: it paints a round pin below {@link TextSelection#anchorBounds()} and below
 *  {@link TextSelection#caretBounds()}, moves the caret or the anchor with {@link TextSelection#withCaretAt(Position)}
 *  and {@link TextSelection#withAnchorAt(Position)} while a pin is dragged, places the caret where your users tap,
 *  and selects the word under the finger after a long press. Dragging across the text without a pin selects nothing,
 *  like on a phone. A caret without selected characters keeps its pin, so your users can drag it to start a selection.
 *  Every press in touch mode also gives the text the keyboard focus, so Ctrl+A and Ctrl+C keep working.
 *  The overlay has the same bounds as the text component, so the rectangles of the selection need no conversion.
 */
public class TextSelectionPinsExample extends UI.Panel
{
    private static final Color INK        = new Color( 38,  44,  60);
    private static final Color MUTED      = new Color( 96, 104, 124);
    private static final Color ACCENT     = new Color( 64,  98, 255);
    private static final Color WARM       = new Color(214,  92,  52);
    private static final Color PAPER      = new Color(252, 252, 255);
    private static final Color BACKDROP   = new Color(232, 236, 246);
    private static final Color HIGHLIGHT  = new Color( 64,  98, 255,  56);
    private static final Color OUTLINE    = new Color( 64,  98, 255, 150);

    private static final int PIN_RADIUS            = 9;
    private static final int PIN_GRAB_DISTANCE     = 16;
    private static final int LONG_PRESS_MILLIS     = 500;
    private static final int LONG_PRESS_TOLERANCE  = 6;
    private static final int STATUS_LENGTH         = 60;

    private static final String TITLE = "Select me, copy me";

    private static final StyledString[] CONTENT = {
        StyledString.of( f -> f.size(24).weight(2).color(INK), TITLE + "\n" ),
        StyledString.of( f -> f.size(16).color(MUTED), "This text is painted by the " ),
        StyledString.of( f -> f.size(16).color(ACCENT).weight(1.5), "style" ),
        StyledString.of( f -> f.size(16).color(MUTED), " of a plain component, not by a text field. Drag across it with the mouse, double click a word, or press " ),
        StyledString.of( f -> f.size(16).color(WARM).weight(1.5), "Ctrl+A" ),
        StyledString.of( f -> f.size(16).color(MUTED), " and " ),
        StyledString.of( f -> f.size(16).color(WARM).weight(1.5), "Ctrl+C" ),
        StyledString.of( f -> f.size(16).color(MUTED), ". Move the caret with the arrow keys, and hold " ),
        StyledString.of( f -> f.size(16).color(WARM).weight(1.5), "Shift" ),
        StyledString.of( f -> f.size(16).color(MUTED), " to select while it moves.\n" ),
        StyledString.of( f -> f.size(16).color(MUTED).posture(0.2f),
            "In touch mode, tap to place the caret, press and hold a word to select it, " +
            "and drag the round pins, which snap from one gap between two letters to the next." )
    };

    private static final String PAINTED_TEXT = _joined(CONTENT);

    private static final int STYLE_START = PAINTED_TEXT.indexOf("style");

    private enum Grab { NOTHING, CARET_PIN, ANCHOR_PIN, TEXT }

    private final Var<TextSelection> selection = Var.of(TextSelection.between(STYLE_START, STYLE_START + "style".length()));
    private final Var<Boolean>       touchMode = Var.of(false);
    private final Timer              longPress = new Timer(LONG_PRESS_MILLIS, e -> _selectWordAtPress());
    private final JBox               text      = _text();
    private Grab                     grab      = Grab.NOTHING;
    private Position                 pressedAt = Position.origin();
    private Position                 pinOffset = Position.origin();

    public TextSelectionPinsExample() {
        longPress.setRepeats(false);
        text.addPropertyChangeListener("preferredSize", e -> UI.runLater(this::_growWindowToFitText));
        Var<Boolean> selectable = selection.zoomTo(TextSelection::isEnabled, TextSelection::withEnabled);
        UI.of(this).withLayout("fill, wrap 1, insets 18", "[grow]", "[]12[grow]12[]6[]6[]")
        .withStyle( it -> it.backgroundColor(BACKDROP) )
        .add("growx",
            UI.label("Styled text selection")
            .withStyle( it -> it.fontSize(20).fontBold(true).foregroundColor(INK) )
        )
        .add("grow, w 240:660:, hmin 200",
            UI.panel("fill, insets 0").withStyle( it -> it.backgroundColor(BACKDROP) )
            .add("pos 0 0 100% 100%", _touchOverlay())
            .add("grow", text)
        )
        .add("growx, w 0:0:",
            UI.label(selection.viewAsString(TextSelectionPinsExample::_describe))
            .withStyle( it -> it.fontSize(13).foregroundColor(INK) )
        )
        .add("growx",
            UI.panel("insets 0", "[][]")
            .withStyle( it -> it.backgroundColor(BACKDROP) )
            .add(UI.checkBox("Touch mode", touchMode))
            .add(UI.checkBox("Selectable", selectable))
        )
        .add("growx",
            UI.panel("insets 0", "[][][]")
            .withStyle( it -> it.backgroundColor(BACKDROP) )
            .add(UI.button("Select the title").onClick( it -> selection.update( s -> TextSelection.between(0, TITLE.length()).withEnabled(s.isEnabled()) ) ))
            .add(UI.button("Select everything").onClick( it -> selection.update( s -> TextSelection.between(0, PAINTED_TEXT.length()).withEnabled(s.isEnabled()) ) ))
            .add(UI.button("Clear").onClick( it -> selection.update( s -> TextSelection.caretAt(s.caret()).withEnabled(s.isEnabled()) ) ))
        );
    }

    private JBox _text() {
        return
            UI.box("fill, insets 0", "[grow][]", "[]push")
            .withStyle( it -> it
                .margin(6)
                .padding(26)
                .borderRadius(18)
                .backgroundColor(PAPER)
                .shadowColor(new Color(40, 50, 90, 60))
                .shadowBlurRadius(14)
                .shadowSpreadRadius(-4)
                .text( t -> t
                    .placement(UI.Placement.TOP_LEFT)
                    .font( f -> f.family(Font.SANS_SERIF) )
                    .wrapLines(true)
                    .autoPreferredHeight(true)
                    .content(CONTENT)
                    .selection( s -> s
                        .color(HIGHLIGHT)
                        .mergeLines(true)
                        .border(1.5, OUTLINE)
                        .convexRadius(6)
                        .concaveRadius(4)
                        .showCaret(true)
                        .caretColor(ACCENT)
                    )
                )
            )
            .withTextSelection(selection)
            .add("cell 1 0, w 170!",
                UI.box("fill, insets 12", "[center]")
                .withStyle( it -> it
                    .margin(4, 0, 12, 16)
                    .borderRadius(14)
                    .backgroundColor(new Color(255, 243, 234))
                    .border(1, new Color(240, 196, 170))
                    .text( t -> t
                        .placement(UI.Placement.CENTER)
                        .wrapLines(true)
                        .autoPreferredHeight(true)
                        .content(
                            StyledString.of( f -> f.size(13).color(WARM).weight(2), "An obstacle\n" ),
                            StyledString.of( f -> f.size(12).color(MUTED), "The text flows around this card, wherever the window puts it." )
                        )
                    )
                )
            )
            .get(JBox.class);
    }

    private void _growWindowToFitText() {
        Window window = SwingUtilities.getWindowAncestor(this);
        invalidate();
        if ( window != null && window.getHeight() < window.getPreferredSize().height )
            window.setSize(window.getWidth(), window.getPreferredSize().height);
    }

    private JBox _touchOverlay() {
        return
            UI.box()
            .isVisibleIf(touchMode)
            .withRepaintOn(selection)
            .withStyle( it -> {
                TextSelection current = selection.get();
                return it.painter(UI.Layer.FOREGROUND, Painter.of(current, g -> _paintPins(g, current)));
            })
            .onMousePress( e -> _press(Position.of(e.getEvent().getX(), e.getEvent().getY())) )
            .onMouseDrag( e -> _drag(Position.of(e.getEvent().getX(), e.getEvent().getY())) )
            .onMouseRelease( e -> _release() )
            .get(JBox.class);
    }

    private void _press( Position at ) {
        TextSelection current = selection.get();
        text.requestFocusInWindow();
        pressedAt = at;
        if ( !current.isEnabled() )
            grab = Grab.NOTHING;
        else if ( _isOnPin(at, current.caretBounds()) )
            _grabPin(Grab.CARET_PIN, at, current.caretBounds());
        else if ( !current.isEmpty() && _isOnPin(at, current.anchorBounds()) )
            _grabPin(Grab.ANCHOR_PIN, at, current.anchorBounds());
        else {
            grab = Grab.TEXT;
            longPress.restart();
        }
    }

    private void _grabPin( Grab pin, Position at, Optional<Bounds> bounds ) {
        grab = pin;
        pinOffset = _middleOf(bounds).minus(at);
    }

    private void _drag( Position at ) {
        Position gap = at.plus(pinOffset);
        if ( grab == Grab.CARET_PIN )
            selection.update( s -> s.withCaretAt(gap) );
        else if ( grab == Grab.ANCHOR_PIN )
            selection.update( s -> s.withAnchorAt(gap) );
        else if ( grab == Grab.TEXT && _distance(at, pressedAt) > UI.scale(LONG_PRESS_TOLERANCE) ) {
            longPress.stop();
            grab = Grab.NOTHING;
        }
    }

    private void _release() {
        if ( grab == Grab.TEXT && longPress.isRunning() ) {
            longPress.stop();
            selection.update( s -> s.withAnchorAt(pressedAt).withCaretAt(pressedAt) );
        }
        grab = Grab.NOTHING;
    }

    private void _selectWordAtPress() {
        if ( grab != Grab.TEXT )
            return;
        grab = Grab.NOTHING;
        selection.update( s -> s.positionAt(pressedAt).map( position -> {
            BreakIterator words = BreakIterator.getWordInstance();
            words.setText(PAINTED_TEXT);
            int character = Math.max(0, Math.min(position, PAINTED_TEXT.length() - 1));
            int end   = words.following(character);
            int start = words.previous();
            return TextSelection.between(start, end).withEnabled(s.isEnabled());
        }).orElse(s) );
    }

    private static Position _middleOf( Optional<Bounds> bounds ) {
        return bounds.map( b -> Position.of(b.location().x() + b.size().width().orElse(0f) / 2, b.location().y() + b.size().height().orElse(0f) / 2) )
                     .orElse(Position.origin());
    }

    private static boolean _isOnPin( Position at, Optional<Bounds> pin ) {
        return pin.map( b -> _distance(at, _handleCentre(b)) <= UI.scale(PIN_GRAB_DISTANCE) ).orElse(false);
    }

    private static Position _handleCentre( Bounds caret ) {
        return Position.of(
                    caret.location().x() + caret.size().width().orElse(0f) / 2,
                    caret.location().y() + caret.size().height().orElse(0f) + UI.scale(PIN_RADIUS + 1)
                );
    }

    private static double _distance( Position a, Position b ) {
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }

    private static void _paintPins( Graphics2D g, TextSelection current ) {
        if ( !current.isEnabled() )
            return;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        current.caretBounds().ifPresent( b -> _paintPin(g, b) );
        if ( !current.isEmpty() )
            current.anchorBounds().ifPresent( b -> _paintPin(g, b) );
    }

    private static void _paintPin( Graphics2D g, Bounds caret ) {
        float scale  = UI.scale();
        Position handle = _handleCentre(caret);
        float x      = handle.x() / scale;
        float top    = caret.location().y() / scale;
        float centre = handle.y() / scale;
        g.setColor(ACCENT);
        g.setStroke(new BasicStroke(2f));
        g.draw(new Line2D.Float(x, top, x, centre));
        g.fill(new Ellipse2D.Float(x - PIN_RADIUS, centre - PIN_RADIUS, 2 * PIN_RADIUS, 2 * PIN_RADIUS));
        g.setColor(Color.WHITE);
        float dot = PIN_RADIUS * 0.4f;
        g.fill(new Ellipse2D.Float(x - dot, centre - dot, 2 * dot, 2 * dot));
    }

    private static String _describe( TextSelection s ) {
        if ( !s.isEnabled() )
            return "Selecting is switched off.";
        if ( s.isEmpty() )
            return "Caret at position " + s.caret() + ".";
        String shown = s.selectedText().replace('\n', '⏎');
        if ( shown.length() > STATUS_LENGTH )
            shown = shown.substring(0, STATUS_LENGTH - 1) + "…";
        return "Positions " + s.start() + " to " + s.end() + ":  “" + shown + "”";
    }

    private static String _joined( StyledString[] strings ) {
        StringBuilder joined = new StringBuilder();
        for ( StyledString string : strings )
            joined.append(string.string());
        return joined.toString();
    }

    public static void main( String[] args ) {
        FlatLightLaf.setup();
        UI.show("Styled Text Selection", f -> new TextSelectionPinsExample());
    }
}

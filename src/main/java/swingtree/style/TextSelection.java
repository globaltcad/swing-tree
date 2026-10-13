package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import swingtree.layout.Bounds;
import swingtree.layout.Position;

import java.util.Objects;
import java.util.Optional;

/**
 *  An immutable value describing which characters of a styled text are selected.
 *  Bind a property holding one to the styled text of a component through
 *  {@link swingtree.UIForAnySwing#withTextSelection(swingtree.UI.Layer, String, sprouts.Var)}:
 *  <pre>{@code
 *  Var<TextSelection> selection = Var.of(TextSelection.caretAt(0));
 *  UI.box()
 *  .withStyle( it -> it
 *      .text("body", t -> t.content("Hello world"))
 *  )
 *  .withTextSelection("body", selection);
 *  }</pre>
 *  A selection holds two positions in the text. A position is a gap between two characters:
 *  position 0 is the gap in front of the first character, position 1 is the gap behind
 *  the first character, and the position equal to the length of the text is the gap
 *  behind its last character. In "Hello world", {@code TextSelection.between(6, 11)}
 *  selects "world".
 *  <p>
 *  The <b>anchor</b> is the position where the selection began, for example where your user
 *  pressed the mouse button. The <b>caret</b> is the position which moves, for example where
 *  your user drags the mouse to. A click with the shift key held down keeps the anchor and
 *  moves the caret. Which of the two positions comes first in the text does not change which
 *  characters are selected: {@link #start()} is always the smaller and {@link #end()} always
 *  the larger of the two.
 *  <p>
 *  The two positions, {@link #isCaretAtLineEnd()} and {@link #isEnabled()} are the only parts of a
 *  selection which your code chooses. Everything else is read only, and SwingTree fills it in:
 *  after SwingTree has painted the text, it writes a selection into the property which holds the
 *  selected characters in
 *  {@link #selectedText()}, the pixel rectangles of the anchor and the caret in {@link #anchorBounds()}
 *  and {@link #caretBounds()}, and which converts a pixel coordinate into a position in
 *  {@link #positionAt(Position)}. SwingTree also moves a position which lies behind the end of the
 *  text to the end of the text, and sets {@link #isCaretAtLineEnd()} to {@code false} where the caret
 *  is not the end of a painted line. A selection which your code creates with {@link #caretAt(int)} or
 *  {@link #between(int, int)} has none of this until SwingTree has written it into the property.
 *  <p>
 *  Two selections are equal when their positions, their {@link #isCaretAtLineEnd()} and {@link #isEnabled()}
 *  flags and everything that SwingTree filled in are equal. So the property receives a new selection whenever the
 *  painted text moves, for example when the component gets a new size.
 */
@Immutable
public final class TextSelection
{
    private final int        _anchor;
    private final int        _caret;
    private final boolean    _caretAtLineEnd;
    private final boolean    _enabled;
    private final CaretTable _carets;

    private TextSelection( int anchor, int caret, boolean caretAtLineEnd, boolean enabled, CaretTable carets ) {
        if ( anchor < 0 || caret < 0 )
            throw new IllegalArgumentException(
                "A text position is never negative, but the anchor is " + anchor + " and the caret is " + caret + "."
            );
        _anchor         = anchor;
        _caret          = caret;
        _caretAtLineEnd = caretAtLineEnd;
        _enabled        = enabled;
        _carets         = Objects.requireNonNull(carets);
    }

    /**
     *  Creates an enabled selection of no characters, whose anchor and caret are both at the given position.
     *  Where a line wraps, the caret lies at the start of the next line, see {@link #isCaretAtLineEnd()}.
     *
     * @param position The gap between two characters at which both the anchor and the caret are,
     *                 counted from 0 for the gap in front of the first character.
     * @return A selection for which {@link #isEmpty()} is {@code true}.
     * @throws IllegalArgumentException If the position is negative.
     */
    public static TextSelection caretAt( int position ) {
        return new TextSelection(position, position, false, true, CaretTable.EMPTY);
    }

    /**
     *  Creates an enabled selection of the characters between the two given positions.
     *  The anchor may lie behind the caret, which is what a selection made by
     *  dragging the mouse backwards looks like. When the caret lies behind the anchor, so that the caret is
     *  the end of the selected characters, {@link #isCaretAtLineEnd()} is {@code true}, otherwise {@code false}.
     *
     * @param anchor The position where the selection began, which a shift-click keeps.
     * @param caret The position which moves, which a shift-click replaces.
     * @return A selection with the given anchor and caret.
     * @throws IllegalArgumentException If one of the positions is negative.
     */
    public static TextSelection between( int anchor, int caret ) {
        return new TextSelection(anchor, caret, anchor < caret, true, CaretTable.EMPTY);
    }

    /**
     *  Returns the position where the selection began, which a shift-click keeps.
     *
     * @return The anchor of this selection.
     */
    public int anchor() { return _anchor; }

    /**
     *  Returns the position which moves while your user drags the mouse, and which a shift-click replaces.
     *
     * @return The caret of this selection.
     */
    public int caret() { return _caret; }

    /**
     *  Returns the smaller of the anchor and the caret, which is the gap in front of the first selected character.
     *
     * @return The position where the selected characters start.
     */
    public int start() { return Math.min(_anchor, _caret); }

    /**
     *  Returns the larger of the anchor and the caret, which is the gap behind the last selected character.
     *
     * @return The position where the selected characters end.
     */
    public int end() { return Math.max(_anchor, _caret); }

    /**
     *  Tells whether no character is selected, because the anchor and the caret are the same position.
     *
     * @return {@code true} if the anchor and the caret are the same position.
     */
    public boolean isEmpty() { return _anchor == _caret; }

    /**
     *  Tells whether your users can select characters of the text. When this is {@code false},
     *  SwingTree paints no selection colour, no selection border and no caret, the mouse and
     *  the keyboard select and copy nothing, and the mouse cursor stays the default cursor.
     *  SwingTree still fills in {@link #selectedText()} and the rectangles of the anchor and the caret.
     *
     * @return {@code true} if your users can select characters, which is the default.
     */
    public boolean isEnabled() { return _enabled; }

    /**
     *  Returns a selection with the same positions which your users can or cannot change,
     *  see {@link #isEnabled()}.
     *
     * @param enabled {@code true} to let your users select characters, {@code false} to ignore the mouse and the keyboard.
     * @return A selection with the given flag.
     */
    public TextSelection withEnabled( boolean enabled ) {
        return enabled == _enabled ? this : new TextSelection(_anchor, _caret, _caretAtLineEnd, enabled, _carets);
    }

    /**
     *  Returns a selection with the same anchor and the given caret,
     *  which is what a click with the shift key held down makes of this selection.
     *  When the new caret lies behind the anchor, so that it is the end of the selected characters,
     *  {@link #isCaretAtLineEnd()} of the new selection is {@code true}, otherwise {@code false}.
     *  If the caret already is at the given position, this method returns this selection.
     *
     * @param caret The new position of the caret.
     * @return A selection from the anchor of this selection to the given caret.
     * @throws IllegalArgumentException If the caret is negative.
     */
    public TextSelection withCaret( int caret ) {
        return caret == _caret ? this : new TextSelection(_anchor, caret, _anchor < caret, _enabled, _carets);
    }

    /**
     *  Tells where the caret lies when its position is both the end of one painted line and the start of the
     *  next one. That happens where SwingTree wraps a long line, and on the two sides of an obstacle which the
     *  text flows around. With {@code true}, the caret lies at the end of the upper line, right next to the
     *  character in front of it. With {@code false}, it lies at the start of the next line, in front of the
     *  character behind it. At every other position there is only one place, and this flag changes nothing.
     *  Text editors know this as the affinity of the caret.
     *  <p>
     *  SwingTree sets the flag wherever your users put the caret: a click right of the end of a line, the End
     *  key and the arrow keys up and down landing on the end of a line put the caret at the end of that line,
     *  and a click in front of the first character of a line and the Home key put it at the start of the line.
     *  When SwingTree writes a selection into the property, it keeps this flag only where the caret really is
     *  the end of a painted line, and sets it to {@code false} everywhere else, so two carets in the same
     *  place are equal. A selection which your code creates holds the value which your code chose until
     *  SwingTree has written it into the property.
     *  {@link #caretBounds()} returns the place which this flag chooses:
     *  <pre>{@code
     *  // "HHHH HHHH" wraps behind the space, so position 5 ends the first line and starts the second:
     *  TextSelection atStartOfSecondLine = TextSelection.caretAt(5);
     *  TextSelection atEndOfFirstLine    = TextSelection.caretAt(5).withCaretAtLineEnd(true);
     *  }</pre>
     *
     * @return {@code true} if the caret lies at the end of the upper line where a position has two places.
     */
    public boolean isCaretAtLineEnd() { return _caretAtLineEnd; }

    /**
     *  Returns a selection with the same positions whose caret lies at the end of the upper line, or at the
     *  start of the next line, where its position is both, see {@link #isCaretAtLineEnd()}.
     *
     * @param atLineEnd {@code true} for the end of the upper line, {@code false} for the start of the next line.
     * @return A selection with the given flag.
     */
    public TextSelection withCaretAtLineEnd( boolean atLineEnd ) {
        return atLineEnd == _caretAtLineEnd ? this : new TextSelection(_anchor, _caret, atLineEnd, _enabled, _carets);
    }

    /**
     *  Returns the selected characters. SwingTree fills them in after it has painted the text,
     *  so this is an empty string for a selection which your code created and which SwingTree
     *  has not written into the property yet.
     *
     * @return The characters between {@link #start()} and {@link #end()}, up to the end of the text.
     */
    public String selectedText() {
        final String text = _carets.text();
        return text.substring(Math.min(start(), text.length()), Math.min(end(), text.length()));
    }

    /**
     *  Returns the pixel rectangle of the caret on the component, which is where SwingTree paints the
     *  caret when {@code showCaret(true)} is set in the style of the selection. The rectangle uses the
     *  same coordinates as the mouse events of the component: pixels, counted from the top left corner
     *  of the component, with the UI scale already applied. It reaches from the top to the bottom of the line of the caret,
     *  and its left edge is the gap between the two characters, whatever the shape of the caret is.
     *  For a {@link swingtree.UI.CaretShape#BAR}, which is the default, it is as wide as the {@code caretWidth} of the style
     *  of the selection, and at least one pixel. For a {@link swingtree.UI.CaretShape#BLOCK} and an
     *  {@link swingtree.UI.CaretShape#UNDERLINE}, it is as wide as the character behind the gap, and SwingTree
     *  paints an underline into the bottom of it, as thick as the {@code caretWidth}.
     *  <p>
     *  The position where a line wraps is both the end of that line and the start of the next line.
     *  There, {@link #isCaretAtLineEnd()} decides where the rectangle lies: at the end of the upper line
     *  when it is {@code true}, and in front of the character behind the caret when it is {@code false}.
     *  For example, when your users select up to the place where a line wraps, the rectangle of the caret
     *  lies at the right end of the upper line, next to the last selected character, and not in front of
     *  the first character of the next line. A caret made by {@link #caretAt(int)} at that place lies at
     *  the start of the next line.
     *  <p>
     *  It is empty for a selection which SwingTree has not written into the property yet, for a
     *  caret on a line which is not painted because the component is too small to show it, and for a
     *  caret in an empty paragraph between two line breaks in a row, which SwingTree paints as an empty
     *  line without a rectangle for the caret.
     *
     * @return The rectangle of the caret, or an empty {@link Optional}.
     */
    public Optional<Bounds> caretBounds() { return _carets.boundsOf(_caret, _caretAtLineEnd); }

    /**
     *  Returns the pixel rectangle of the anchor on the component, measured in the same way as
     *  {@link #caretBounds()}: when this selection is not empty and the anchor is its {@link #end()},
     *  which is the case for a selection made backwards, the rectangle lies at the end of the line which
     *  holds the last selected character, and otherwise in front of the character behind the anchor.
     *
     * @return The rectangle of the anchor, or an empty {@link Optional}.
     */
    public Optional<Bounds> anchorBounds() { return _carets.boundsOf(_anchor, !isEmpty() && _anchor == end()); }

    /**
     *  Returns the position nearest to the given pixel coordinate, by the rule which SwingTree
     *  uses for the mouse: the line nearest to the coordinate, and in that line the gap between
     *  two characters nearest to it. A coordinate left of the first or right of the last character
     *  of a line gives the first or the last position of that line.
     *  The coordinate uses the same coordinates as the mouse events of the component, see {@link #caretBounds()}.
     *
     * @param point A pixel coordinate on the component.
     * @return The position nearest to the coordinate, or an empty {@link Optional} for a selection
     *         which SwingTree has not written into the property yet.
     */
    public Optional<Integer> positionAt( Position point ) {
        final int position = _carets.positionAt(point.x(), point.y());
        return position < 0 ? Optional.empty() : Optional.of(position);
    }

    /**
     *  Returns a selection with the same anchor and the caret at the position nearest to the given
     *  pixel coordinate, see {@link #positionAt(Position)}. This is what dragging the mouse to the
     *  coordinate does to the position of the caret. {@link #isCaretAtLineEnd()} follows the rule of
     *  {@link #withCaret(int)}, so call {@link #withCaretAtLineEnd(boolean)} afterwards to choose the
     *  other place of a position where a line wraps.
     *
     * @param point A pixel coordinate on the component.
     * @return A selection with the moved caret, or this selection if SwingTree has not written it into the property yet.
     */
    public TextSelection withCaretAt( Position point ) {
        return positionAt(point).map(this::withCaret).orElse(this);
    }

    /**
     *  Returns a selection with the same caret and the anchor at the position nearest to the given
     *  pixel coordinate, see {@link #positionAt(Position)}.
     *
     * @param point A pixel coordinate on the component.
     * @return A selection with the moved anchor, or this selection if SwingTree has not written it into the property yet.
     */
    public TextSelection withAnchorAt( Position point ) {
        return positionAt(point)
                .map( anchor -> anchor == _anchor ? this : new TextSelection(anchor, _caret, _caretAtLineEnd, _enabled, _carets) )
                .orElse(this);
    }

    CaretTable carets() { return _carets; }

    @SuppressWarnings("ReferenceEquality")
    TextSelection resolvedWith( CaretTable carets ) {
        final int length = carets.text().length();
        final int anchor = Math.min(_anchor, length);
        final int     caret     = Math.min(_caret, length);
        final boolean atLineEnd = _caretAtLineEnd && carets.isLineEnd(caret);
        if ( anchor == _anchor && caret == _caret && atLineEnd == _caretAtLineEnd && carets == _carets )
            return this;
        return new TextSelection(anchor, caret, atLineEnd, _enabled, carets);
    }

    @Override
    public boolean equals( Object other ) {
        if ( this == other )
            return true;
        if ( !(other instanceof TextSelection) )
            return false;
        final TextSelection that = (TextSelection) other;
        return _anchor         == that._anchor         &&
               _caret          == that._caret          &&
               _caretAtLineEnd == that._caretAtLineEnd &&
               _enabled        == that._enabled        &&
               _carets.equals(that._carets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_anchor, _caret, _caretAtLineEnd, _enabled, _carets);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
                    "anchor=" + _anchor + ", " +
                    "caret=" + _caret + ", " +
                    "caretAtLineEnd=" + _caretAtLineEnd + ", " +
                    "enabled=" + _enabled + ", " +
                    "selectedText='" + selectedText() + "', " +
                    "anchorBounds=" + anchorBounds().map(Object::toString).orElse("?") + ", " +
                    "caretBounds=" + caretBounds().map(Object::toString).orElse("?") +
                "]";
    }
}

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
 *  The two positions and {@link #isEnabled()} are the only parts of a selection which your code
 *  chooses. Everything else is read only, and SwingTree fills it in: after SwingTree has painted
 *  the text, it writes a selection into the property which holds the selected characters in
 *  {@link #selectedText()}, the pixel rectangles of the anchor and the caret in {@link #anchorBounds()}
 *  and {@link #caretBounds()}, and which converts a pixel coordinate into a position in
 *  {@link #positionAt(Position)}. SwingTree also moves a position which lies behind the end of the
 *  text to the end of the text. A selection which your code creates with {@link #caretAt(int)} or
 *  {@link #between(int, int)} has none of this until SwingTree has written it into the property.
 *  <p>
 *  Two selections are equal when their positions, their {@link #isEnabled()} flag and everything
 *  that SwingTree filled in are equal. So the property receives a new selection whenever the
 *  painted text moves, for example when the component gets a new size.
 */
@Immutable
public final class TextSelection
{
    private final int        _anchor;
    private final int        _caret;
    private final boolean    _enabled;
    private final CaretTable _carets;

    private TextSelection( int anchor, int caret, boolean enabled, CaretTable carets ) {
        if ( anchor < 0 || caret < 0 )
            throw new IllegalArgumentException(
                "A text position is never negative, but the anchor is " + anchor + " and the caret is " + caret + "."
            );
        _anchor  = anchor;
        _caret   = caret;
        _enabled = enabled;
        _carets  = Objects.requireNonNull(carets);
    }

    /**
     *  Creates an enabled selection of no characters, whose anchor and caret are both at the given position.
     *
     * @param position The gap between two characters at which both the anchor and the caret are,
     *                 counted from 0 for the gap in front of the first character.
     * @return A selection for which {@link #isEmpty()} is {@code true}.
     * @throws IllegalArgumentException If the position is negative.
     */
    public static TextSelection caretAt( int position ) {
        return new TextSelection(position, position, true, CaretTable.EMPTY);
    }

    /**
     *  Creates an enabled selection of the characters between the two given positions.
     *  The anchor may lie behind the caret, which is what a selection made by
     *  dragging the mouse backwards looks like.
     *
     * @param anchor The position where the selection began, which a shift-click keeps.
     * @param caret The position which moves, which a shift-click replaces.
     * @return A selection with the given anchor and caret.
     * @throws IllegalArgumentException If one of the positions is negative.
     */
    public static TextSelection between( int anchor, int caret ) {
        return new TextSelection(anchor, caret, true, CaretTable.EMPTY);
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
        return enabled == _enabled ? this : new TextSelection(_anchor, _caret, enabled, _carets);
    }

    /**
     *  Returns a selection with the same anchor and the given caret,
     *  which is what a click with the shift key held down makes of this selection.
     *
     * @param caret The new position of the caret.
     * @return A selection from the anchor of this selection to the given caret.
     * @throws IllegalArgumentException If the caret is negative.
     */
    public TextSelection withCaret( int caret ) {
        return caret == _caret ? this : new TextSelection(_anchor, caret, _enabled, _carets);
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
     *  of the component, with the UI scale already applied. It reaches from the top to the bottom of the line of the caret.
     *  Its left edge is the gap between the two characters, and it is one pixel wide, or as wide
     *  as the UI scale factor when that is larger.
     *  <p>
     *  The position where a line wraps is both the end of that line and the start of the next line.
     *  When this selection is not empty and the caret is its {@link #end()}, the rectangle lies at the
     *  end of the line which holds the last selected character. In every other case, the rectangle lies
     *  in front of the character behind the caret. For example, when your users select up to the place
     *  where a line wraps, the rectangle of the caret lies at the right end of the upper line, next to
     *  the last selected character, and not in front of the first character of the next line. A caret
     *  without selected characters at that place lies at the start of the next line.
     *  <p>
     *  It is empty for a selection which SwingTree has not written into the property yet, and for a
     *  caret on a line which is not painted because the component is too small to show it.
     *
     * @return The rectangle of the caret, or an empty {@link Optional}.
     */
    public Optional<Bounds> caretBounds() { return _carets.boundsOf(_caret, !isEmpty() && _caret == end()); }

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
     *  coordinate does.
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
                .map( anchor -> anchor == _anchor ? this : new TextSelection(anchor, _caret, _enabled, _carets) )
                .orElse(this);
    }

    CaretTable carets() { return _carets; }

    @SuppressWarnings("ReferenceEquality")
    TextSelection resolvedWith( CaretTable carets ) {
        final int length = carets.text().length();
        final int anchor = Math.min(_anchor, length);
        final int caret  = Math.min(_caret, length);
        if ( anchor == _anchor && caret == _caret && carets == _carets )
            return this;
        return new TextSelection(anchor, caret, _enabled, carets);
    }

    @Override
    public boolean equals( Object other ) {
        if ( this == other )
            return true;
        if ( !(other instanceof TextSelection) )
            return false;
        final TextSelection that = (TextSelection) other;
        return _anchor  == that._anchor  &&
               _caret   == that._caret   &&
               _enabled == that._enabled &&
               _carets.equals(that._carets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_anchor, _caret, _enabled, _carets);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
                    "anchor=" + _anchor + ", " +
                    "caret=" + _caret + ", " +
                    "enabled=" + _enabled + ", " +
                    "selectedText='" + selectedText() + "', " +
                    "anchorBounds=" + anchorBounds().map(Object::toString).orElse("?") + ", " +
                    "caretBounds=" + caretBounds().map(Object::toString).orElse("?") +
                "]";
    }
}

package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.util.Objects;
import java.util.Optional;

/**
 *  Describes how the selected characters of a styled text look, inside the text style
 *  of a component:
 *  <pre>{@code
 *  UI.box()
 *  .withStyle( it -> it
 *      .text("body", t -> t
 *          .content("Hello world")
 *          .selection( s -> s.color(new Color(80, 140, 255, 90)).borderRadius(6) )
 *      )
 *  )
 *  .withTextSelection("body", selection);
 *  }</pre>
 *  The selection itself, which says which characters are selected, is not part of this
 *  configuration. It is held by the property which you pass to
 *  {@link swingtree.UIForAnySwing#withTextSelection(swingtree.UI.Layer, String, sprouts.Var)}.
 *  **So please note that a component without {@code withTextSelection} paints no selection at all.**
 *  <p>
 *  SwingTree paints the selection colour, and the border if there is one, behind the selected
 *  characters and then paints the characters on top of it, in their own colour.
 */
@Immutable
@SuppressWarnings("Immutable")
public final class TextSelectionConf
{
    private static final Color _DEFAULT_COLOR = new Color(51, 153, 255, 90);
    private static final TextSelectionConf _NONE = new TextSelectionConf(null, _DEFAULT_COLOR, false, 0f, 0f, 0f, null, false, null);

    static TextSelectionConf none() { return _NONE; }

    private final @Nullable TextSelection _range;
    private final Color           _color;
    private final boolean         _mergeLines;
    private final float           _convexRadius;
    private final float           _concaveRadius;
    private final float           _borderWidth;
    private final @Nullable Color _borderColor;
    private final boolean         _showCaret;
    private final @Nullable Color _caretColor;

    private TextSelectionConf(
        @Nullable TextSelection range,
        Color                   color,
        boolean                 mergeLines,
        float                   convexRadius,
        float                   concaveRadius,
        float                   borderWidth,
        @Nullable Color         borderColor,
        boolean                 showCaret,
        @Nullable Color         caretColor
    ) {
        _range        = range;
        _color        = Objects.requireNonNull(color);
        _mergeLines    = mergeLines;
        _convexRadius  = convexRadius;
        _concaveRadius = concaveRadius;
        _borderWidth  = borderWidth;
        _borderColor  = borderColor;
        _showCaret    = showCaret;
        _caretColor   = caretColor;
    }

    private static TextSelectionConf of(
        @Nullable TextSelection range,
        Color                   color,
        boolean                 mergeLines,
        float                   convexRadius,
        float                   concaveRadius,
        float                   borderWidth,
        @Nullable Color         borderColor,
        boolean                 showCaret,
        @Nullable Color         caretColor
    ) {
        final TextSelectionConf conf = new TextSelectionConf(range, color, mergeLines, convexRadius, concaveRadius, borderWidth, borderColor, showCaret, caretColor);
        return conf.equals(_NONE) ? _NONE : conf;
    }

    Color color() { return _color; }

    boolean mergeLines() { return _mergeLines; }

    float convexRadius() { return _convexRadius; }

    float concaveRadius() { return _concaveRadius; }

    float borderWidth() { return _borderWidth; }

    Optional<Color> borderColor() { return Optional.ofNullable(_borderColor); }

    boolean showCaret() { return _showCaret; }

    Optional<Color> caretColor() { return Optional.ofNullable(_caretColor); }

    @Nullable TextSelection rangeToPaint() {
        return _range == null || !_range.isEnabled() || ( _range.isEmpty() && !_showCaret ) ? null : _range;
    }

    TextSelectionConf range( TextSelection range ) {
        return of(range, _color, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, _borderColor, _showCaret, _caretColor);
    }

    /**
     *  Sets the colour which SwingTree paints behind the selected characters.
     *  A partly transparent colour lets the background of the component shine through.
     *  Without this method, the colour is a partly transparent light blue.
     *
     * @param color The colour behind the selected characters.
     * @return A new selection configuration with the given colour.
     */
    public TextSelectionConf color( Color color ) {
        Objects.requireNonNull(color);
        return of(_range, color, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, _borderColor, _showCaret, _caretColor);
    }

    /**
     *  Rounds every corner of the area which SwingTree paints behind the selected characters,
     *  with the same radius for the convex and the concave corners. It is the same as
     *  {@code convexRadius(radius).concaveRadius(radius)}, see {@link #convexRadius(double)}
     *  and {@link #concaveRadius(double)}. With a radius of 0, which is the default, the corners are square.
     *
     * @param radius The radius of the quarter circle of every corner, in pixels before the UI scale is applied.
     * @return A new selection configuration with the given corner radius.
     */
    public TextSelectionConf borderRadius( double radius ) {
        return convexRadius(radius).concaveRadius(radius);
    }

    /**
     *  Rounds the convex corners of the area which SwingTree paints behind the selected characters.
     *  A convex corner points out of the area, like each of the four corners of a rectangle.
     *  Without {@link #mergeLines(boolean)}, the area behind the selected characters of every line
     *  is a rectangle, so all of its corners are convex. With a border, the outer edge of the border
     *  has this radius at a convex corner, and the inner edge of the border has this radius minus the
     *  width of the border, or a square corner when the border is wider than the radius.
     *  With a radius of 0, which is the default, the convex corners are square.
     *
     * @param radius The radius of the quarter circle of every convex corner, in pixels before the UI scale is applied.
     * @return A new selection configuration with the given radius for convex corners.
     */
    public TextSelectionConf convexRadius( double radius ) {
        return of(_range, _color, _mergeLines, (float) Math.max(0, radius), _concaveRadius, _borderWidth, _borderColor, _showCaret, _caretColor);
    }

    /**
     *  Rounds the concave corners of the shape which {@link #mergeLines(boolean)} makes from the
     *  selected parts of several lines. A concave corner points into the shape, like the corner where
     *  the end of a short line meets the longer line below it. With a radius above 0, SwingTree fills
     *  that corner with a curve, so the outline bends from the edge of the short line into the edge of
     *  the longer line. With a border, the outer edge of the border has this radius
     *  at a concave corner, and the inner edge of the border has this radius plus the width of the
     *  border, so the border is equally wide all along the corner. Without {@code mergeLines(true)},
     *  every area is a rectangle without concave corners, and this radius changes nothing.
     *  With a radius of 0, which is the default, the concave corners are square.
     *
     * @param radius The radius of the quarter circle of every concave corner, in pixels before the UI scale is applied.
     * @return A new selection configuration with the given radius for concave corners.
     */
    public TextSelectionConf concaveRadius( double radius ) {
        return of(_range, _color, _mergeLines, _convexRadius, (float) Math.max(0, radius), _borderWidth, _borderColor, _showCaret, _caretColor);
    }

    /**
     *  Sets whether SwingTree paints the selected parts of several lines as one shape.
     *  Without this, which is the default, SwingTree paints a separate rectangle behind the selected
     *  characters of every line, each with its own rounded corners and its own border. With
     *  {@code mergeLines(true)}, the rectangles of two neighbouring lines are joined into one shape
     *  where they overlap horizontally, and the border runs only along the outline of that shape, never
     *  between two lines. Rectangles which do not touch, for example the rectangles on the two sides
     *  of an obstacle on the same line, are not joined to each other, and the shape never covers an obstacle.
     *  The corners of the shape are rounded with {@link #convexRadius(double)} and {@link #concaveRadius(double)}.
     *
     * @param merge {@code true} to paint the selected parts of neighbouring lines as one shape.
     * @return A new selection configuration which joins the lines or not.
     */
    public TextSelectionConf mergeLines( boolean merge ) {
        return of(_range, _color, merge, _convexRadius, _concaveRadius, _borderWidth, _borderColor, _showCaret, _caretColor);
    }

    /**
     *  Paints a border of the given width and colour along the inner edge of every area which
     *  SwingTree paints behind the selected characters of a line. The border takes its width from
     *  the area: the outer edge of the border is where the edge of the area would be without a border,
     *  so a selection with a border is exactly as large as a selection without one. The selection
     *  colour is painted only inside the border and never under it, so a partly transparent border
     *  colour shows the background of the component behind it. How the corners of the border are rounded
     *  is described in {@link #convexRadius(double)} and {@link #concaveRadius(double)}.
     *  A width of 0, which is the default, paints no border.
     *
     * @param width The width of the border, in pixels before the UI scale is applied.
     * @param color The colour of the border.
     * @return A new selection configuration with the given border.
     */
    public TextSelectionConf border( double width, Color color ) {
        return borderWidth(width).borderColor(color);
    }

    /**
     *  Sets the width of the border which SwingTree paints along the inner edge of the selection,
     *  see {@link #border(double, Color)}. The border is only painted when it also has a colour,
     *  which you set with {@link #borderColor(Color)}.
     *
     * @param width The width of the border, in pixels before the UI scale is applied.
     * @return A new selection configuration with the given border width.
     */
    public TextSelectionConf borderWidth( double width ) {
        return of(_range, _color, _mergeLines, _convexRadius, _concaveRadius, (float) Math.max(0, width), _borderColor, _showCaret, _caretColor);
    }

    /**
     *  Sets the colour of the border which SwingTree paints along the inner edge of the selection,
     *  see {@link #border(double, Color)}. The border is only painted when it also has a width
     *  larger than 0, which you set with {@link #borderWidth(double)}.
     *
     * @param color The colour of the border.
     * @return A new selection configuration with the given border colour.
     */
    public TextSelectionConf borderColor( Color color ) {
        Objects.requireNonNull(color);
        return of(_range, _color, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, color, _showCaret, _caretColor);
    }

    /**
     *  Decides whether SwingTree paints the caret when no character is selected.
     *  The caret is a thin vertical bar in the gap where a click with the shift key held down
     *  would continue the selection. It does not blink. By default, it is not painted,
     *  because most styled texts are labels and descriptions which nobody types into.
     *
     * @param show {@code true} to paint the caret when no character is selected.
     * @return A new selection configuration which paints the caret or not.
     */
    public TextSelectionConf showCaret( boolean show ) {
        return of(_range, _color, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, _borderColor, show, _caretColor);
    }

    /**
     *  Sets the colour of the caret, which SwingTree paints when {@link #showCaret(boolean)}
     *  is enabled and no character is selected. Without this method, the caret has the colour
     *  of the font of the text, or black if the font has no single colour.
     *
     * @param color The colour of the caret.
     * @return A new selection configuration with the given caret colour.
     */
    public TextSelectionConf caretColor( Color color ) {
        Objects.requireNonNull(color);
        return of(_range, _color, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, _borderColor, _showCaret, color);
    }

    TextSelectionConf _scale( double scale ) {
        if ( _convexRadius == 0f && _concaveRadius == 0f && _borderWidth == 0f )
            return this;
        return of(_range, _color, _mergeLines, (float) (_convexRadius * scale), (float) (_concaveRadius * scale), (float) (_borderWidth * scale), _borderColor, _showCaret, _caretColor);
    }

    @Override
    public boolean equals( Object other ) {
        if ( this == other )
            return true;
        if ( !(other instanceof TextSelectionConf) )
            return false;
        final TextSelectionConf that = (TextSelectionConf) other;
        return Objects.equals(_range, that._range) &&
               _color.equals(that._color) &&
               _mergeLines == that._mergeLines &&
               Float.floatToIntBits(_convexRadius)  == Float.floatToIntBits(that._convexRadius) &&
               Float.floatToIntBits(_concaveRadius) == Float.floatToIntBits(that._concaveRadius) &&
               Float.floatToIntBits(_borderWidth)  == Float.floatToIntBits(that._borderWidth) &&
               Objects.equals(_borderColor, that._borderColor) &&
               _showCaret == that._showCaret &&
               Objects.equals(_caretColor, that._caretColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_range, _color, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, _borderColor, _showCaret, _caretColor);
    }

    @Override
    public String toString() {
        if ( this.equals(_NONE) )
            return getClass().getSimpleName() + "[NONE]";
        return getClass().getSimpleName() + "[" +
                    "range=" + _range + ", " +
                    "color=" + _color + ", " +
                    "mergeLines=" + _mergeLines + ", " +
                    "convexRadius=" + _convexRadius + ", " +
                    "concaveRadius=" + _concaveRadius + ", " +
                    "borderWidth=" + _borderWidth + ", " +
                    "borderColor=" + _borderColor + ", " +
                    "showCaret=" + _showCaret + ", " +
                    "caretColor=" + _caretColor +
                "]";
    }
}

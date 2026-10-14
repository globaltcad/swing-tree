package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import org.jspecify.annotations.Nullable;
import swingtree.UI;

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
    private static final float _SATURATION_KEPT_WHILE_UNFOCUSED = 0.2f;
    private static final int   _NAVIGATE_WITH_KEYBOARD           = 1;
    private static final int   _SELECT_WORD_ON_DOUBLE_CLICK      = 1 << 1;
    private static final int   _SELECT_PARAGRAPH_ON_TRIPLE_CLICK = 1 << 2;
    private static final int   _SELECT_WORD_ON_LONG_PRESS        = 1 << 3;
    private static final int   _COPY_TO_CLIPBOARD                = 1 << 4;
    private static final int   _DEFAULT_BEHAVIOURS               = _NAVIGATE_WITH_KEYBOARD | _SELECT_WORD_ON_DOUBLE_CLICK | _SELECT_PARAGRAPH_ON_TRIPLE_CLICK | _COPY_TO_CLIPBOARD;
    private static final TextSelectionConf _NONE = new TextSelectionConf(null, _DEFAULT_COLOR, null, false, 0f, 0f, 0f, null, null, false, null, UI.CaretShape.BAR, 1f, true, _DEFAULT_BEHAVIOURS);

    static TextSelectionConf none() { return _NONE; }

    private final @Nullable TextSelection _range;
    private final Color           _color;
    private final @Nullable Color _unfocusedColor;
    private final boolean         _mergeLines;
    private final float           _convexRadius;
    private final float           _concaveRadius;
    private final float           _borderWidth;
    private final @Nullable Color _borderColor;
    private final @Nullable Color _unfocusedBorderColor;
    private final boolean         _showCaret;
    private final @Nullable Color _caretColor;
    private final UI.CaretShape   _caretShape;
    private final float           _caretWidth;
    private final boolean         _useUnfocusedLook;
    private final int             _behaviours;

    private TextSelectionConf(
        @Nullable TextSelection range,
        Color                   color,
        @Nullable Color         unfocusedColor,
        boolean                 mergeLines,
        float                   convexRadius,
        float                   concaveRadius,
        float                   borderWidth,
        @Nullable Color         borderColor,
        @Nullable Color         unfocusedBorderColor,
        boolean                 showCaret,
        @Nullable Color         caretColor,
        UI.CaretShape           caretShape,
        float                   caretWidth,
        boolean                 useUnfocusedLook,
        int                     behaviours
    ) {
        _range                = range;
        _color                = Objects.requireNonNull(color);
        _unfocusedColor       = unfocusedColor;
        _mergeLines           = mergeLines;
        _convexRadius         = convexRadius;
        _concaveRadius        = concaveRadius;
        _borderWidth          = borderWidth;
        _borderColor          = borderColor;
        _unfocusedBorderColor = unfocusedBorderColor;
        _showCaret            = showCaret;
        _caretColor           = caretColor;
        _caretShape           = Objects.requireNonNull(caretShape);
        _caretWidth           = caretWidth;
        _useUnfocusedLook     = useUnfocusedLook;
        _behaviours           = behaviours;
    }

    private static TextSelectionConf of(
        @Nullable TextSelection range,
        Color                   color,
        @Nullable Color         unfocusedColor,
        boolean                 mergeLines,
        float                   convexRadius,
        float                   concaveRadius,
        float                   borderWidth,
        @Nullable Color         borderColor,
        @Nullable Color         unfocusedBorderColor,
        boolean                 showCaret,
        @Nullable Color         caretColor,
        UI.CaretShape           caretShape,
        float                   caretWidth,
        boolean                 useUnfocusedLook,
        int                     behaviours
    ) {
        final TextSelectionConf conf = new TextSelectionConf(range, color, unfocusedColor, mergeLines, convexRadius, concaveRadius, borderWidth, borderColor, unfocusedBorderColor, showCaret, caretColor, caretShape, caretWidth, useUnfocusedLook, behaviours);
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

    UI.CaretShape caretShape() { return _caretShape; }

    float caretWidth() { return _caretWidth; }

    boolean navigateWithKeyboard() { return _does(_NAVIGATE_WITH_KEYBOARD); }

    boolean selectWordOnDoubleClick() { return _does(_SELECT_WORD_ON_DOUBLE_CLICK); }

    boolean selectParagraphOnTripleClick() { return _does(_SELECT_PARAGRAPH_ON_TRIPLE_CLICK); }

    boolean selectWordOnLongPress() { return _does(_SELECT_WORD_ON_LONG_PRESS); }

    boolean copyToClipboard() { return _does(_COPY_TO_CLIPBOARD); }

    private boolean _does( int behaviour ) { return ( _behaviours & behaviour ) != 0; }

    private TextSelectionConf _withBehaviour( int behaviour, boolean on ) {
        final int behaviours = on ? _behaviours | behaviour : _behaviours & ~behaviour;
        return of(_range, _color, _unfocusedColor, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, _borderColor, _unfocusedBorderColor, _showCaret, _caretColor, _caretShape, _caretWidth, _useUnfocusedLook, behaviours);
    }

    TextSelectionConf caretGeometry() {
        return of(null, _DEFAULT_COLOR, null, false, 0f, 0f, 0f, null, null, false, null, _caretShape, _caretWidth, true, _DEFAULT_BEHAVIOURS);
    }

    @Nullable TextSelection rangeToPaint() {
        return _range == null || !_range.isEnabled() || ( _range.isEmpty() && !_showCaret ) ? null : _range;
    }

    TextSelectionConf unfocused() {
        if ( !_useUnfocusedLook )
            return this;
        final Color           unfocusedColor       = _unfocusedColor != null ? _unfocusedColor : _greyed(_color);
        final @Nullable Color unfocusedBorderColor = _borderColor == null ? null : _unfocusedBorderColor != null ? _unfocusedBorderColor : _greyed(_borderColor);
        return of(_range, unfocusedColor, _unfocusedColor, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, unfocusedBorderColor, _unfocusedBorderColor, false, _caretColor, _caretShape, _caretWidth, _useUnfocusedLook, _behaviours);
    }

    private static Color _greyed( Color color ) {
        final float grey = 0.299f * color.getRed() + 0.587f * color.getGreen() + 0.114f * color.getBlue();
        return new Color(
                    _towardsGrey(color.getRed(),   grey),
                    _towardsGrey(color.getGreen(), grey),
                    _towardsGrey(color.getBlue(),  grey),
                    color.getAlpha()
                );
    }

    private static int _towardsGrey( int channel, float grey ) {
        return Math.round(grey + (channel - grey) * _SATURATION_KEPT_WHILE_UNFOCUSED);
    }

    TextSelectionConf range( TextSelection range ) {
        return of(range, _color, _unfocusedColor, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, _borderColor, _unfocusedBorderColor, _showCaret, _caretColor, _caretShape, _caretWidth, _useUnfocusedLook, _behaviours);
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
        return of(_range, color, _unfocusedColor, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, _borderColor, _unfocusedBorderColor, _showCaret, _caretColor, _caretShape, _caretWidth, _useUnfocusedLook, _behaviours);
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
        return of(_range, _color, _unfocusedColor, _mergeLines, (float) Math.max(0, radius), _concaveRadius, _borderWidth, _borderColor, _unfocusedBorderColor, _showCaret, _caretColor, _caretShape, _caretWidth, _useUnfocusedLook, _behaviours);
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
        return of(_range, _color, _unfocusedColor, _mergeLines, _convexRadius, (float) Math.max(0, radius), _borderWidth, _borderColor, _unfocusedBorderColor, _showCaret, _caretColor, _caretShape, _caretWidth, _useUnfocusedLook, _behaviours);
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
        return of(_range, _color, _unfocusedColor, merge, _convexRadius, _concaveRadius, _borderWidth, _borderColor, _unfocusedBorderColor, _showCaret, _caretColor, _caretShape, _caretWidth, _useUnfocusedLook, _behaviours);
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
        return of(_range, _color, _unfocusedColor, _mergeLines, _convexRadius, _concaveRadius, (float) Math.max(0, width), _borderColor, _unfocusedBorderColor, _showCaret, _caretColor, _caretShape, _caretWidth, _useUnfocusedLook, _behaviours);
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
        return of(_range, _color, _unfocusedColor, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, color, _unfocusedBorderColor, _showCaret, _caretColor, _caretShape, _caretWidth, _useUnfocusedLook, _behaviours);
    }

    /**
     *  Decides whether SwingTree paints the caret when no character is selected.
     *  The caret marks the gap where a click with the shift key held down would continue the selection.
     *  It is a thin vertical bar unless you choose another shape with {@link #caretShape(UI.CaretShape)}.
     *  It does not blink. By default, it is not painted,
     *  because most styled texts are labels and descriptions which nobody types into.
     *  From the moment the component loses the keyboard focus until it gets it back, SwingTree does not
     *  paint the caret either, unless you pass {@code false} to {@link #useUnfocusedLook(boolean)}.
     *
     * @param show {@code true} to paint the caret when no character is selected.
     * @return A new selection configuration which paints the caret or not.
     */
    public TextSelectionConf showCaret( boolean show ) {
        return of(_range, _color, _unfocusedColor, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, _borderColor, _unfocusedBorderColor, show, _caretColor, _caretShape, _caretWidth, _useUnfocusedLook, _behaviours);
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
        return of(_range, _color, _unfocusedColor, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, _borderColor, _unfocusedBorderColor, _showCaret, color, _caretShape, _caretWidth, _useUnfocusedLook, _behaviours);
    }

    /**
     *  Sets the shape of the caret, which SwingTree paints when {@link #showCaret(boolean)} is enabled
     *  and no character is selected. A {@link UI.CaretShape#BAR} is a vertical bar in the gap,
     *  a {@link UI.CaretShape#BLOCK} covers the character behind the gap, and a {@link UI.CaretShape#UNDERLINE}
     *  is a line under that character. Behind the last character of a line, where no character follows
     *  the gap, a block and an underline are half as wide as the line is tall.
     *  SwingTree paints the caret behind the characters, so give a block a colour in which the character
     *  on top of it stays readable, see {@link #caretColor(Color)}.
     *  Without this method, the caret is a {@link UI.CaretShape#BAR}.
     *
     * @param shape The shape of the caret.
     * @return A new selection configuration with the given caret shape.
     */
    public TextSelectionConf caretShape( UI.CaretShape shape ) {
        Objects.requireNonNull(shape);
        return of(_range, _color, _unfocusedColor, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, _borderColor, _unfocusedBorderColor, _showCaret, _caretColor, shape, _caretWidth, _useUnfocusedLook, _behaviours);
    }

    /**
     *  Sets how thick the caret is, which SwingTree paints when {@link #showCaret(boolean)} is enabled
     *  and no character is selected. It is the width of a {@link UI.CaretShape#BAR}, which grows to
     *  the right of the gap, and the height of an {@link UI.CaretShape#UNDERLINE}, which grows upwards
     *  from the bottom of the line. A {@link UI.CaretShape#BLOCK} is as wide as the character it covers,
     *  so this width does not change it. However thin you make it, SwingTree paints the caret at least
     *  one pixel thick on the screen. Without this method, the caret is 1 pixel thick.
     *
     * @param width The thickness of the caret, in pixels before the UI scale is applied.
     * @return A new selection configuration with the given caret width.
     */
    public TextSelectionConf caretWidth( double width ) {
        return of(_range, _color, _unfocusedColor, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, _borderColor, _unfocusedBorderColor, _showCaret, _caretColor, _caretShape, (float) Math.max(0, width), _useUnfocusedLook, _behaviours);
    }

    /**
     *  Sets the colour which SwingTree paints behind the selected characters from the moment the component
     *  loses the keyboard focus until it gets it back. Many programs, like the text fields of web browsers
     *  and of macOS, grey a selection when the keyboard focus goes elsewhere, so that your users can tell
     *  at a glance where the keys they type go to:
     *  <pre>{@code
     *  .selection( s -> s
     *      .color(new Color(80, 140, 255, 90))
     *      .unfocusedColor(new Color(160, 160, 160, 90))
     *  )
     *  }</pre>
     *  A component which never had the keyboard focus looks focused.
     *  Without this method, SwingTree takes the colour of {@link #color(Color)} and removes most of its
     *  saturation while it keeps its transparency and its brightness, so the selection turns grey but stays
     *  as easy to see as before. With {@link #useUnfocusedLook(boolean) useUnfocusedLook(false)},
     *  SwingTree never paints this colour.
     *
     * @param color The colour behind the selected characters after the component lost the keyboard focus.
     * @return A new selection configuration with the given colour for an unfocused component.
     */
    public TextSelectionConf unfocusedColor( Color color ) {
        Objects.requireNonNull(color);
        return of(_range, _color, color, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, _borderColor, _unfocusedBorderColor, _showCaret, _caretColor, _caretShape, _caretWidth, _useUnfocusedLook, _behaviours);
    }

    /**
     *  Sets the colour of the border of the selection from the moment the component loses the keyboard
     *  focus until it gets it back, see {@link #border(double, Color)} and {@link #unfocusedColor(Color)}. This colour only
     *  replaces the colour of a border which SwingTree paints while the component has the focus, so a
     *  selection without a border colour or without a border width gets no border from it.
     *  Without this method, SwingTree takes the colour of {@link #borderColor(Color)} and removes most of
     *  its saturation while it keeps its transparency and its brightness.
     *
     * @param color The colour of the border after the component lost the keyboard focus.
     * @return A new selection configuration with the given border colour for an unfocused component.
     */
    public TextSelectionConf unfocusedBorderColor( Color color ) {
        Objects.requireNonNull(color);
        return of(_range, _color, _unfocusedColor, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, _borderColor, color, _showCaret, _caretColor, _caretShape, _caretWidth, _useUnfocusedLook, _behaviours);
    }

    /**
     *  Decides whether the selection looks different from the moment the component loses the keyboard focus
     *  until it gets it back. A component which never had the keyboard focus always looks focused. By default, which is {@code true}, SwingTree paints the selection in the colours of
     *  {@link #unfocusedColor(Color)} and {@link #unfocusedBorderColor(Color)} and paints no caret from the
     *  moment the component loses the keyboard focus until it gets it back. With {@code false}, the keyboard
     *  focus changes nothing that your users can see: the selection keeps the colours of {@link #color(Color)}
     *  and {@link #borderColor(Color)}, and the caret of {@link #showCaret(boolean)} stays where it is.
     *  This suits an application whose look should not change with the keyboard focus, and a test which
     *  compares pictures of the component taken while the keyboard focus is somewhere else.
     *
     * @param use {@code true} to grey the selection and hide the caret after the component lost the keyboard focus.
     * @return A new selection configuration which changes its look with the keyboard focus or not.
     */
    public TextSelectionConf useUnfocusedLook( boolean use ) {
        return of(_range, _color, _unfocusedColor, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, _borderColor, _unfocusedBorderColor, _showCaret, _caretColor, _caretShape, _caretWidth, use, _behaviours);
    }

    /**
     *  Decides whether your users can move the caret with the keyboard, like in a text editor.
     *  By default, which is {@code true}, the keys of a component made selectable with
     *  {@link swingtree.UIForAnySwing#withTextSelection(String, sprouts.Var)} do this:
     *  <ul>
     *      <li>The left and the right arrow key move the caret by one character. When characters are selected,
     *          they put the caret at the start or the end of the selected characters instead.</li>
     *      <li>With Ctrl held down, or Alt on macOS, the left and the right arrow key move the caret to the
     *          start of the word in front of it or to the end of the word behind it.</li>
     *      <li>The up and the down arrow key move the caret to the line above or below, to the gap nearest to
     *          the place where the caret was. When your users press them several times in a row, the caret
     *          stays near the place where the first of these presses started, even across shorter lines.
     *          On the first line, the up arrow key moves the caret to the start of the text,
     *          and on the last line, the down arrow key moves it to the end of the text.</li>
     *      <li>Home and End move the caret to the start or the end of the painted line which holds it,
     *          and with Ctrl held down, or Alt on macOS, to the start or the end of the text. In an empty
     *          paragraph, between two line breaks in a row, the start and the end of the line are the same
     *          gap, so a caret there stays where it is.</li>
     *  </ul>
     *  With the shift key held down, each of these keys keeps the anchor where it is and moves only the caret,
     *  so your users select the characters which the caret passes over. The keys only reach the component
     *  while it has the keyboard focus, which it gets when your users click into the text.
     *  With {@code false}, SwingTree leaves these keys alone, for example because your code moves the caret
     *  with keys of its own. The keyboard shortcuts for selecting everything and for copying work either way.
     *  <pre>{@code
     *  .selection( s -> s.showCaret(true).navigateWithKeyboard(false) )
     *  }</pre>
     *
     * @param navigate {@code true} to let the arrow keys, Home and End move the caret.
     * @return A new selection configuration which moves the caret with the keyboard or not.
     */
    public TextSelectionConf navigateWithKeyboard( boolean navigate ) {
        return _withBehaviour(_NAVIGATE_WITH_KEYBOARD, navigate);
    }

    /**
     *  Decides whether a double click selects the word under the mouse. A word is what
     *  {@link java.text.BreakIterator#getWordInstance()} finds around the character under the mouse, which
     *  follows the word rules of many languages, so "don't" is one word. The arrow keys with Ctrl held down,
     *  see {@link #navigateWithKeyboard(boolean)}, jump over words by the same rules.
     *  By default, which is {@code true}, a double click selects the word. With {@code false}, the second
     *  click of a double click does what a single click does: it puts the caret into the gap nearest to the mouse.
     *
     * @param select {@code true} to select the word under the mouse on a double click.
     * @return A new selection configuration which selects a word on a double click or not.
     */
    public TextSelectionConf selectWordOnDoubleClick( boolean select ) {
        return _withBehaviour(_SELECT_WORD_ON_DOUBLE_CLICK, select);
    }

    /**
     *  Decides whether a triple click selects the paragraph under the mouse. A paragraph is everything between
     *  two line breaks of the text, the {@code '\n'} characters, however many painted lines it wraps over.
     *  The line breaks themselves are not selected, so a paragraph which your users copy does not end with an
     *  empty line.
     *  By default, which is {@code true}, a triple click selects the paragraph. With {@code false}, the third
     *  click of a triple click does what a single click does: it puts the caret into the gap nearest to the mouse.
     *  <pre>{@code
     *  .selection( s -> s.selectParagraphOnTripleClick(false) )
     *  }</pre>
     *
     * @param select {@code true} to select the paragraph under the mouse on a triple click.
     * @return A new selection configuration which selects a paragraph on a triple click or not.
     */
    public TextSelectionConf selectParagraphOnTripleClick( boolean select ) {
        return _withBehaviour(_SELECT_PARAGRAPH_ON_TRIPLE_CLICK, select);
    }

    /**
     *  Decides whether holding the left mouse button down on a word, without moving the mouse, selects
     *  that word, like a long press of a finger on the screen of a phone. After about half a second
     *  the word under the mouse is selected. A movement of up to 5 pixels before then is allowed, because a
     *  finger never holds perfectly still. Like every size of a style, these 5 pixels are multiplied by the
     *  UI scale, so they are 10 pixels on the screen at a UI scale of 2. Moving further, releasing the button earlier, or the component
     *  losing the keyboard focus cancels it, and then a press only puts the caret, as it does without a long press.
     *  By default, which is {@code false}, holding the button down does nothing more than a click, because
     *  on a desktop your users select a word with a double click.
     *  <pre>{@code
     *  .selection( s -> s.selectWordOnLongPress(true) )
     *  }</pre>
     *
     * @param select {@code true} to select the word under the mouse when the button is held down without moving.
     * @return A new selection configuration which selects a word on a long press or not.
     */
    public TextSelectionConf selectWordOnLongPress( boolean select ) {
        return _withBehaviour(_SELECT_WORD_ON_LONG_PRESS, select);
    }

    /**
     *  Decides whether your users can copy the selected characters. By default, which is {@code true},
     *  Ctrl+C, or Command+C on macOS, puts the selected characters onto the clipboard of the computer, and so
     *  does the {@link javax.swing.TransferHandler} of the component when your code calls
     *  {@link javax.swing.TransferHandler#exportToClipboard(javax.swing.JComponent, java.awt.datatransfer.Clipboard, int)}.
     *  With {@code false}, nothing reaches any clipboard, and whatever is on the clipboard stays there, for
     *  example for a text which your users may read and select but should type themselves.
     *  Your users can still select the characters, also everything at once with Ctrl+A or Command+A.
     *
     * @param copy {@code true} to let your users copy the selected characters.
     * @return A new selection configuration which lets your users copy or not.
     */
    public TextSelectionConf copyToClipboard( boolean copy ) {
        return _withBehaviour(_COPY_TO_CLIPBOARD, copy);
    }

    TextSelectionConf _scale( double scale ) {
        if ( _convexRadius == 0f && _concaveRadius == 0f && _borderWidth == 0f )
            return this;
        return of(_range, _color, _unfocusedColor, _mergeLines, (float) (_convexRadius * scale), (float) (_concaveRadius * scale), (float) (_borderWidth * scale), _borderColor, _unfocusedBorderColor, _showCaret, _caretColor, _caretShape, _caretWidth, _useUnfocusedLook, _behaviours);
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
               Objects.equals(_unfocusedColor, that._unfocusedColor) &&
               _mergeLines == that._mergeLines &&
               Float.floatToIntBits(_convexRadius)  == Float.floatToIntBits(that._convexRadius) &&
               Float.floatToIntBits(_concaveRadius) == Float.floatToIntBits(that._concaveRadius) &&
               Float.floatToIntBits(_borderWidth)  == Float.floatToIntBits(that._borderWidth) &&
               Objects.equals(_borderColor, that._borderColor) &&
               Objects.equals(_unfocusedBorderColor, that._unfocusedBorderColor) &&
               _showCaret == that._showCaret &&
               Objects.equals(_caretColor, that._caretColor) &&
               _caretShape == that._caretShape &&
               Float.floatToIntBits(_caretWidth) == Float.floatToIntBits(that._caretWidth) &&
               _useUnfocusedLook == that._useUnfocusedLook &&
               _behaviours == that._behaviours;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_range, _color, _unfocusedColor, _mergeLines, _convexRadius, _concaveRadius, _borderWidth, _borderColor, _unfocusedBorderColor, _showCaret, _caretColor, _caretShape, _caretWidth, _useUnfocusedLook, _behaviours);
    }

    @Override
    public String toString() {
        if ( this.equals(_NONE) )
            return getClass().getSimpleName() + "[NONE]";
        return getClass().getSimpleName() + "[" +
                    "range=" + _range + ", " +
                    "color=" + _color + ", " +
                    "unfocusedColor=" + _unfocusedColor + ", " +
                    "mergeLines=" + _mergeLines + ", " +
                    "convexRadius=" + _convexRadius + ", " +
                    "concaveRadius=" + _concaveRadius + ", " +
                    "borderWidth=" + _borderWidth + ", " +
                    "borderColor=" + _borderColor + ", " +
                    "unfocusedBorderColor=" + _unfocusedBorderColor + ", " +
                    "showCaret=" + _showCaret + ", " +
                    "caretColor=" + _caretColor + ", " +
                    "caretShape=" + _caretShape + ", " +
                    "caretWidth=" + _caretWidth + ", " +
                    "useUnfocusedLook=" + _useUnfocusedLook + ", " +
                    "navigateWithKeyboard=" + navigateWithKeyboard() + ", " +
                    "selectWordOnDoubleClick=" + selectWordOnDoubleClick() + ", " +
                    "selectParagraphOnTripleClick=" + selectParagraphOnTripleClick() + ", " +
                    "selectWordOnLongPress=" + selectWordOnLongPress() + ", " +
                    "copyToClipboard=" + copyToClipboard() +
                "]";
    }
}

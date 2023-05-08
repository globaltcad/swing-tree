package swingtree.style;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class Style
{
    private final PaddingStyle _padding;
    private final BorderStyle _border;
    private final BackgroundStyle _background;
    private final ShadowStyle _shadow;
    private final FontStyle _font;


    public Style(
        PaddingStyle padding,
        BorderStyle border,
        BackgroundStyle background,
        ShadowStyle shadow,
        FontStyle font
    ) {
        _padding = padding;
        _border = border;
        _background = background;
        _shadow = shadow;
        _font = font;
    }

    private Style _withPadding(PaddingStyle padding ) {
        return new Style(padding, _border, _background, _shadow, _font);
    }

    private Style _withBorder(BorderStyle border ) {
        return new Style(_padding, border, _background, _shadow, _font);
    }

    private Style _withBackground(BackgroundStyle background ) {
        return new Style(_padding, _border, background, _shadow, _font);
    }

    private Style _withShadow( ShadowStyle shadow ) {
        return new Style(_padding, _border, _background, shadow, _font);
    }

    private Style _withFont( FontStyle font ) {
        return new Style(_padding, _border, _background, _shadow, font);
    }

    /**
     *  Creates a new {@link Style} with the provided top, right, left and bottom pad distances.
     *  The padding does not affect the size or layout of the component, it
     *  simply determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #innerBackground(Color), {@link #shadow(int, int, int, int, Color)}).
     * <p>
     * @param top The top padding distance in pixels.
     * @param right The right padding distance in pixels.
     * @param bottom The bottom padding distance in pixels.
     * @param left The left padding distance in pixels.
     * @return A new {@link Style} with the provided padding distances.
     */
    public Style pad( int top, int right, int bottom, int left ) {
        return _withPadding(_padding.withTop(top).withLeft(left).withRight(right).withBottom(bottom));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for all sides of the component.
     *  The padding does not affect the size or layout of the component, it
     *  simply determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #innerBackground(Color), {@link #shadow(int, int, int, int, Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link Style} with the provided padding distance.
     */
    public Style pad( int padding ) {
        return _withPadding(this._padding.withTop(padding).withLeft(padding).withRight(padding).withBottom(padding));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the top side of the component.
     *  The padding does not affect the size or layout of the component, it
     *  simply determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #innerBackground(Color), {@link #shadow(int, int, int, int, Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link Style} with the provided padding distance.
     */
    public Style padTop(int padding ) {
        return _withPadding(this._padding.withTop(padding));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the right side of the component.
     *  The padding does not affect the size or layout of the component, it
     *  simply determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #innerBackground(Color), {@link #shadow(int, int, int, int, Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link Style} with the provided padding distance.
     */
    public Style padRight(int padding ) {
        return _withPadding(this._padding.withRight(padding));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the bottom side of the component.
     *  The padding does not affect the size or layout of the component, it
     *  simply determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #innerBackground(Color), {@link #shadow(int, int, int, int, Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link Style} with the provided padding distance.
     */
    public Style padBottom(int padding ) {
        return _withPadding(this._padding.withBottom(padding));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the left side of the component.
     *  The padding does not affect the size or layout of the component, it
     *  simply determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #innerBackground(Color), {@link #shadow(int, int, int, int, Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link Style} with the provided padding distance.
     */
    public Style padLeft(int padding ) {
        return _withPadding(this._padding.withLeft(padding));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the top and bottom sides of the component.
     *  The padding does not affect the size or layout of the component, it
     *  simply determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #innerBackground(Color), {@link #shadow(int, int, int, int, Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link Style} with the provided padding distance.
     */
    public Style padVertical(int padding ) {
        return _withPadding(this._padding.withTop(padding).withBottom(padding));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the left and right sides of the component.
     *  The padding does not affect the size or layout of the component, it
     *  simply determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #innerBackground(Color), {@link #shadow(int, int, int, int, Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link Style} with the provided padding distance.
     */
    public Style padHorizontal(int padding ) {
        return _withPadding(this._padding.withLeft(padding).withRight(padding));
    }

    /**
     *  Returns a new {@link Style} with the provided border width and border color.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param width The border width in pixels.
     * @param color The border color.
     * @return A new {@link Style} with the provided border width and border color.
     */
    public Style border( int width, Color color ) {
        return _withBorder(_border.withWidth(width).withColor(color));
    }

    /**
     *  Returns a new {@link Style} with the provided border width.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param width The border width in pixels.
     * @return A new {@link Style} with the provided border width.
     */
    public Style borderWidth( int width ) {
        return _withBorder(_border.withWidth(width));
    }

    /**
     *  Returns a new {@link Style} with the provided border color.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param color The border color.
     * @return A new {@link Style} with the provided border color.
     */
    public Style borderColor(Color color ) {
        return _withBorder(_border.withColor(color));
    }

    /**
     *  Returns a new {@link Style} with the provided border radius.
     *  This will override both the arc width and arc height of the border.
     *  The border will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param radius The border radius in pixels.
     * @return A new {@link Style} with the provided border radius.
     */
    public Style borderRadius( int radius ) {
        return this._withBorder(_border.withArcWidth(radius).withArcHeight(radius));
    }

    /**
     *  Returns a new {@link Style} with the provided border arc width and arc height.
     *  Note that the border will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param arcWidth The border arc width in pixels.
     * @param arcHeight The border arc height in pixels.
     * @return A new {@link Style} with the provided border arc width and arc height.
     */
    public Style borderRadius(int arcWidth, int arcHeight ) {
        return _withBorder(_border.withArcWidth(arcWidth).withArcHeight(arcHeight));
    }

    /**
     *  Returns a new {@link Style} with the provided inner Background color.
     *  The inner background will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param color The inner background color.
     * @return A new {@link Style} with the provided inner background color.
     */
    public Style innerBackground( Color color ) { return _withBackground(_background.withInnerColor(color)); }

    /**
     *  Returns a new {@link Style} with the provided background color.
     *  The background color covers the entire component area, including the padding spaces.
     *
     * @param color The background color.
     * @return A new {@link Style} with the provided background color.
     */
    public Style background( Color color ) { return _withBackground(_background.withColor(color)); }

    /**
     *  Returns a new {@link Style} with the provided background renderer, a {@link Consumer} that
     *  will be called using the {@link Graphics2D} instance used to render the component.
     *  You may use this to render a custom background for the component.
     * @param renderer The background renderer.
     * @return A new {@link Style} with the provided background renderer.
     */
    public Style backgroundRenderer( Consumer<Graphics2D> renderer ) { return _withBackground(_background.withBackgroundRenderer(renderer)); }

    /**
     *  Returns a new {@link Style} with the provided horizontal shadow offset.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call {@link #shadowSpreadRadius(int)},
     *  {@link #shadowBlurRadius(int)} and {@link #shadowColor(Color)}.
     *
     * @param offset The shadow offset in pixels.
     * @return A new {@link Style} with the provided horizontal shadow offset.
     */
    public Style shadowHorizontalOffset( int offset ) { return _withShadow(_shadow.withHorizontalOffset(offset)); }

    /**
     *  Returns a new {@link Style} with the provided vertical shadow offset.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call {@link #shadowSpreadRadius(int)},
     *  {@link #shadowBlurRadius(int)} and {@link #shadowColor(Color)}.
     *
     * @param offset The shadow offset in pixels.
     * @return A new {@link Style} with the provided vertical shadow offset.
     */
    public Style shadowVerticalOffset( int offset ) { return _withShadow(_shadow.withVerticalOffset(offset)); }

    /**
     *  Returns a new {@link Style} with the provided shadow offset.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call {@link #shadowSpreadRadius(int)},
     *  {@link #shadowBlurRadius(int)} and {@link #shadowColor(Color)}.
     *
     * @param horizontalOffset The horizontal shadow offset in pixels.
     * @param verticalOffset The vertical shadow offset in pixels.
     * @return A new {@link Style} with the provided shadow offset.
     */
    public Style shadowOffset(int horizontalOffset, int verticalOffset ) {
        return _withShadow(_shadow.withHorizontalOffset(horizontalOffset).withVerticalOffset(verticalOffset));
    }

    /**
     *  Returns a new {@link Style} with the provided shadow blur radius.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call
     *  {@link #shadowSpreadRadius(int)} and {@link #shadowColor(Color)}.
     *
     * @param radius The shadow blur radius in pixels.
     * @return A new {@link Style} with the provided shadow blur radius.
     */
    public Style shadowBlurRadius( int radius ) { return _withShadow(_shadow.withBlurRadius(radius)); }

    /**
     *  Returns a new {@link Style} with the provided shadow spread radius.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call
     *  {@link #shadowBlurRadius(int)} and {@link #shadowColor(Color)}.
     *
     * @param radius The shadow spread radius in pixels.
     * @return A new {@link Style} with the provided shadow spread radius.
     */
    public Style shadowSpreadRadius( int radius ) { return _withShadow(_shadow.withSpreadRadius(radius)); }

    /**
     *  Returns a new {@link Style} with the provided shadow color.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call
     *  {@link #shadowBlurRadius(int)} and {@link #shadowSpreadRadius(int)}.
     *
     * @param color The shadow color.
     * @return A new {@link Style} with the provided shadow color.
     */
    public Style shadowColor( Color color ) { return _withShadow(_shadow.withColor(color)); }

    public Style shadowInset( boolean b ) { return _withShadow(_shadow.withInset(b)); }

    /**
     *  Returns a new {@link Style} with the provided font name and size.
     *  Note that the font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param name The font name.
     * @param size The font size.
     * @return A new {@link Style} with the provided font name and size.
     */
    public Style font( String name, int size ) { return _withFont(_font.withName(name).withSize(size)); }

    /**
     *  Returns a new {@link Style} with the provided font name.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param name The font name.
     * @return A new {@link Style} with the provided font name.
     */
    public Style font( String name ) { return _withFont(_font.withName(name)); }

    /**
     *  Returns a new {@link Style} with the provided {@link Font}.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param font The {@link Font}.
     * @return A new {@link Style} with the provided {@link Font}.
     */
    public Style font( Font font ) { return _withFont(_font.withFont(font)); }

    /**
     *  Returns a new {@link Style} with the provided font size.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param size The font size.
     * @return A new {@link Style} with the provided font size.
     */
    public Style fontSize( int size ) { return _withFont(_font.withSize(size)); }

    /**
     *  Makes the font bold or not bold depending on the value of the {@code isBold} parameter.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param bold Whether the font should be bold or not.
     * @return A new {@link Style} with the provided font boldness.
     */
    public Style fontBold( boolean bold ) {
        return _withFont(_font.withStyle( bold ? Font.BOLD : Font.PLAIN ));
    }

    /**
     *  Makes the font italic or not italic depending on the value of the {@code italic} parameter.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param italic Whether the font should be italic or not.
     * @return A new {@link Style} with the provided font italicness.
     */
    public Style fontItalic( boolean italic ) {
        return _withFont(_font.withStyle( italic ? Font.ITALIC : Font.PLAIN ));
    }

    /**
     *  Makes the font underlined or not underlined depending on the value of the {@code underline} parameter.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param underline Whether the font should be underlined or not.
     * @return A new {@link Style} with the provided font underlinedness.
     */
    public Style fontUnderline( boolean underline ) {
        List<TextAttribute> attributes = new ArrayList<>(_font.attributes());
        if ( underline ) attributes.add(TextAttribute.UNDERLINE);
        else     attributes.remove(TextAttribute.UNDERLINE);
        return _withFont(_font.withAttributes(attributes));
    }

    /**
     *  Makes the font struck through or not struck through depending on the value of the {@code strikeThrough} parameter.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param strikeThrough Whether the font should be struck through or not.
     * @return A new {@link Style} with the provided font struck throughness.
     */
    public Style fontStrikeThrough( boolean strikeThrough ) {
        List<TextAttribute> attributes = new ArrayList<>(_font.attributes());
        if ( strikeThrough ) attributes.add(TextAttribute.STRIKETHROUGH);
        else     attributes.remove(TextAttribute.STRIKETHROUGH);
        return _withFont(_font.withAttributes(attributes));
    }

    /**
     *  Creates a new {@link Style} where the font color is set to the provided {@link Color}.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param color The {@link Color}.
     * @return A new {@link Style} with the provided font color.
     */
    public Style fontColor( Color color ) {
        return _withFont(_font.withColor(color));
    }

    /**
     *  Creates a new {@link Style} where the font background color is set to the provided {@link Color}.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param color The {@link Color}.
     * @return A new {@link Style} with the provided font background color.
     */
    public Style fontBackgroundColor( Color color ) {
        return _withFont(_font.withBackgroundColor(color));
    }

    /**
     *  Creates a new {@link Style} where the font selection color is set to the provided {@link Color}.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param color The {@link Color}.
     * @return A new {@link Style} with the provided font selection color.
     */
    public Style fontSelectionColor( Color color ) {
        return _withFont(_font.withSelectionColor(color));
    }

    public PaddingStyle padding() { return this._padding; }

    public BorderStyle border() { return _border; }

    public BackgroundStyle background() { return _background; }

    public ShadowStyle shadow() { return _shadow; }

    public FontStyle font() { return _font; }

}

package swingtree.style;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *  An immutable, wither based settings container for {@link javax.swing.JComponent} styling.
 *  The styling in SwingTree is functional, meaning that changing a property
 *  of a {@link Style} instance will return a new {@link Style} instance with the
 *  updated property.
 *  <p>
 *  Here an example of how a {@link Style} instance is applied to a {@link javax.swing.JPanel}:
 *  <pre>{@code
 *  panel(FILL)
 *  .withStyle( it ->
 *      it.style()
 *       .foundationColor(new Color(26,191,230))
 *       .backgroundColor(new Color(255,255,255))
 *       .padTop(30)
 *       .padLeft(35)
 *       .padRight(35)
 *       .padBottom(30)
 *       .borderRadius(25, 25)
 *       .borderWidth(3)
 *       .borderColor(new Color(0,102,255))
 *       .shadowColor(new Color(64,64,64))
 *       .shadowBlurRadius(6)
 *       .shadowSpreadRadius(5)
 *       .shadowInset(false)
 *  )
 *  }</pre>
 */
public final class Style
{
    private static final Style _NONE = new Style(
                                            LayoutStyle.none(),
                                            BorderStyle.none(),
                                            BackgroundStyle.none(),
                                            ForegroundStyle.none(),
                                            FontStyle.none(),
                                            Collections.singletonMap(StyleUtility.DEFAULT_KEY,ShadowStyle.none())
                                        );

    public static Style none() { return _NONE; }

    private final LayoutStyle     _layout;
    private final BorderStyle     _border;
    private final BackgroundStyle _background;
    private final ForegroundStyle _foreground;
    private final FontStyle       _font;
    private final Map<String, ShadowStyle> _shadows = new TreeMap<>();


    private Style(
        LayoutStyle layout,
        BorderStyle border,
        BackgroundStyle background,
        ForegroundStyle foreground,
        FontStyle font,
        Map<String, ShadowStyle> shadows
    ) {
        _layout = layout;
        _border = border;
        _background = background;
        _foreground = foreground;
        _font = font;
        _shadows.putAll(shadows);
    }

    private Style _withLayout( LayoutStyle layout ) { return new Style(layout, _border, _background, _foreground, _font, _shadows); }
    private Style _withBorder( BorderStyle border ) { return new Style(_layout, border, _background, _foreground, _font, _shadows); }
    private Style _withBackground( BackgroundStyle background ) { return new Style(_layout, _border, background, _foreground, _font, _shadows); }
    private Style _withForeground( ForegroundStyle foreground ) { return new Style(_layout, _border, _background, foreground, _font, _shadows); }
    private Style _withFont( FontStyle font ) { return new Style(_layout, _border, _background, _foreground, font, _shadows); }
    private Style _withShadow( Map<String, ShadowStyle> shadows ) { return new Style(_layout, _border, _background, _foreground, _font, shadows); }
    private Style _withShadow( Function<ShadowStyle, ShadowStyle> styler ) {
        // A new map is created where all the styler is applied to all the values:
        Map<String, ShadowStyle> styledShadows = new TreeMap<>();
        _shadows.forEach( (key, value) -> styledShadows.put(key, styler.apply(value)) );
        return _withShadow(styledShadows);
    }

    /**
     *  Creates a new {@link Style} with the provided top, right, left and bottom margin distances.
     *  It determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param top The top padding distance in pixels.
     * @param right The right padding distance in pixels.
     * @param bottom The bottom padding distance in pixels.
     * @param left The left padding distance in pixels.
     * @return A new {@link Style} with the provided padding distances.
     */
    public Style margin( int top, int right, int bottom, int left ) {
        return _withLayout(_layout.margin(_layout.margin().top(top).left(left).right(right).bottom(bottom)));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for all sides of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link Style} with the provided margin distance.
     */
    public Style margin( int margin ) {
        return _withLayout(_layout.margin(_layout.margin().top(margin).left(margin).right(margin).bottom(margin)));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the top side of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link Style} with the provided margin distance.
     */
    public Style marginTop( int margin ) {
        return _withLayout(_layout.margin(_layout.margin().top(margin)));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the right side of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link Style} with the provided margin distance.
     */
    public Style marginRight( int margin ) {
        return _withLayout(_layout.margin(_layout.margin().right(margin)));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the bottom side of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link Style} with the provided margin distance.
     */
    public Style marginBottom( int margin ) { return _withLayout(_layout.margin(_layout.margin().bottom(margin))); }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the left side of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link Style} with the provided margin distance.
     */
    public Style marginLeft( int margin ) { return _withLayout(_layout.margin(_layout.margin().left(margin))); }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the top and bottom sides of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link Style} with the provided margin distance.
     */
    public Style marginVertical( int margin ) {
        return _withLayout(_layout.margin(_layout.margin().top(margin).bottom(margin)));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the left and right sides of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link Style} with the provided margin distance.
     */
    public Style marginHorizontal( int margin ) {
        return _withLayout(_layout.margin(_layout.margin().left(margin).right(margin)));
    }

    /**
     *  Creates a new {@link Style} with the provided top, right, left and bottom pad distances.
     *  It determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param top The top padding distance in pixels.
     * @param right The right padding distance in pixels.
     * @param bottom The bottom padding distance in pixels.
     * @param left The left padding distance in pixels.
     * @return A new {@link Style} with the provided padding distances.
     */
    public Style pad( int top, int right, int bottom, int left ) {
        return _withLayout(_layout.padding(_layout.padding().top(top).left(left).right(right).bottom(bottom)));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for all sides of the component.
     *  It determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link Style} with the provided padding distance.
     */
    public Style pad( int padding ) {
        return _withLayout(_layout.padding(_layout.padding().top(padding).left(padding).right(padding).bottom(padding)));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the top side of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link Style} with the provided padding distance.
     */
    public Style padTop( int padding ) {
        return _withLayout(_layout.padding(_layout.padding().top(padding)));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the right side of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link Style} with the provided padding distance.
     */
    public Style padRight( int padding ) {
        return _withLayout(_layout.padding(_layout.padding().right(padding)));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the bottom side of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link Style} with the provided padding distance.
     */
    public Style padBottom(int padding ) {
        return _withLayout(_layout.padding(_layout.padding().bottom(padding)));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the left side of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link Style} with the provided padding distance.
     */
    public Style padLeft( int padding ) {
        return _withLayout(_layout.padding(_layout.padding().left(padding)));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the top and bottom sides of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link Style} with the provided padding distance.
     */
    public Style padVertical( int padding ) {
        return _withLayout(_layout.padding(_layout.padding().top(padding).bottom(padding)));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the left and right sides of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link Style} with the provided padding distance.
     */
    public Style padHorizontal( int padding ) {
        return _withLayout(_layout.padding(_layout.padding().left(padding).right(padding)));
    }

    /**
     *  Returns a new {@link Style} with the provided border width and border color.
     *  The border will be rendered with an inset space based on the margin defined by the {@link Style}.
     *
     * @param width The border width in pixels.
     * @param color The border color.
     * @return A new {@link Style} with the provided border width and border color.
     */
    public Style border( int width, Color color ) {
        return _withBorder(_border.width(width).color(color));
    }

    /**
     *  Returns a new {@link Style} with the provided border width and border color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param width The border width in pixels.
     * @param colorString The border color.
     * @return A new {@link Style} with the provided border width and border color.
     */
    public Style border( int width, String colorString ) {
        return _withBorder(_border.width(width).color(StyleUtility.toColor(colorString)));
    }

    /**
     *  Returns a new {@link Style} with the provided border width.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param width The border width in pixels.
     * @return A new {@link Style} with the provided border width.
     */
    public Style borderWidth( int width ) {
        return _withBorder(_border.width(width));
    }

    /**
     *  Returns a new {@link Style} with the provided border width for the specified edge.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param edge The edge to set the border width for.
     * @param width The border width in pixels.
     * @return A new {@link Style} with the provided border width for the specified edge.
     */
    public Style borderWidthAt( Edge edge, int width ) {
        return _withBorder(_border.widthAt(edge, width));
    }

    /**
     *  Returns a new {@link Style} with the provided top, right, bottom and left border widths.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *  <p>
     *  The border widths are specified in the following order: top, right, bottom, left.
     *  <p>
     *  Example:
     *  <pre>{@code
     *      UI.panel().withStyle( it -> it.style().borderWidths(1, 2, 3, 4) )
     *  }</pre>
     * @param top The top border width in pixels.
     * @param right The right border width in pixels.
     * @param bottom The bottom border width in pixels.
     * @param left The left border width in pixels.
     * @return A new {@link Style} with the provided top, right, bottom and left border widths.
     * @see #borderWidth(int)
     * @see #borderWidthAt(Edge, int)
     */
    public Style borderWidths( int top, int right, int bottom, int left ) {
        return _withBorder(_border.widths(Outline.of(top, right, bottom, left)));
    }

    /**
     *  Returns a new {@link Style} with the provided top/bottom and left/right border widths.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *  <p>
     *  Example:
     *  <pre>{@code
     *      UI.panel().withStyle( it -> it.style().borderWidths(1, 2) )
     *  }</pre>
     * @param topBottom The top and bottom border width in pixels.
     * @param leftRight The left and right border width in pixels.
     * @return A new {@link Style} with the provided top/bottom and left/right border widths.
     * @see #borderWidth(int)
     * @see #borderWidthAt(Edge, int)
     */
    public Style borderWidths( int topBottom, int leftRight ) {
        return _withBorder(_border.widths(Outline.of(topBottom, leftRight, topBottom, leftRight)));
    }

    /**
     *  Returns a new {@link Style} with the provided border color.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param color The border color.
     * @return A new {@link Style} with the provided border color.
     */
    public Style borderColor( Color color ) {
        return _withBorder(_border.color(color));
    }

    /**
     *  Returns a new {@link Style} with the provided border color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param colorString The border color.
     * @return A new {@link Style} with the provided border color.
     */
    public Style borderColor( String colorString ) {
        return _withBorder(_border.color(StyleUtility.toColor(colorString)));
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
        return this._withBorder(_border.arcWidth(radius).arcHeight(radius));
    }

    /**
     *  Returns a new {@link Style} with the provided border arc width and arc height.
     *  Note that the border will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param arcWidth The border arc width in pixels.
     * @param arcHeight The border arc height in pixels.
     * @return A new {@link Style} with the provided border arc width and arc height.
     */
    public Style borderRadius( int arcWidth, int arcHeight ) {
        return _withBorder(_border.arcWidth(arcWidth).arcHeight(arcHeight));
    }

    /**
     *  Returns a new {@link Style} with the provided border arc width and arc height for the specified corner.
     *  Note that the border will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param corner The corner to apply the border radius to.
     * @param arcWidth The border arc width in pixels.
     * @param arcHeight The border arc height in pixels.
     * @return A new {@link Style} with the provided border arc width and arc height for the specified corner.
     */
    public Style borderRadiusAt( Corner corner, int arcWidth, int arcHeight ) {
        return _withBorder(_border.arcWidthAt(corner, arcWidth).arcHeightAt(corner, arcHeight));
    }

    /**
     *  This method makes it possible to define border shades for the border of your UI components.
     *  This is useful when you want to do advanced border effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it ->
     *      it.style()
     *      .borderShade( shade -> shade.colors("#000000", "#000000").strategy(ShadeStrategy.TOP_TO_BOTTOM) )
     *    )
     * }</pre>
     *
     * @param styler A function that takes a {@link ShadeStyle} and returns a new {@link ShadeStyle}.
     * @return A new {@link Style} with a border shade defined by the provided styler lambda.
     */
    public Style borderShade( Function<ShadeStyle, ShadeStyle> styler ) {
        Objects.requireNonNull(styler);
        return _withBorder(border().shade(StyleUtility.DEFAULT_KEY, styler));
    }

    /**
     *  This method makes it possible to define multiple border shades through a unique name for said shading effect.
     *  This is useful when you want to do advanced border effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it ->
     *      it.style()
     *      .borderShade("dark shading", shade -> shade.colors("#000000", "#000000").strategy(ShadeStrategy.TOP_TO_BOTTOM)))
     *      .borderShade("light shading", shade -> shade.colors("#ffffff", "#ffffff").strategy(ShadeStrategy.TOP_TO_BOTTOM)))
     *    )
     * }</pre>
     * Note that the border shades will be rendered in alphabetical order based on the name of the shade.
     *
     * @param shadeName The name of the border shade.
     * @param styler A function that takes a {@link ShadeStyle} and returns a new {@link ShadeStyle}.
     * @return A new {@link Style} with a named border shade defined by the provided styler lambda.
     */
    public Style borderShade( String shadeName, Function<ShadeStyle, ShadeStyle> styler ) {
        Objects.requireNonNull(shadeName);
        Objects.requireNonNull(styler);
        return _withBorder(border().shade(shadeName, styler));
    }

    /**
     *  Returns a new {@link Style} with the provided inner Background color.
     *  The inner background will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param color The inner background color.
     * @return A new {@link Style} with the provided inner background color.
     */
    public Style backgroundColor( Color color ) { return _withBackground(_background.color(color)); }

    /**
     *  Returns a new {@link Style} with the provided inner Background color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The inner background will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param colorString The inner background color.
     * @return A new {@link Style} with the provided inner background color.
     */
    public Style backgroundColor( String colorString ) { return _withBackground(_background.color(StyleUtility.toColor(colorString))); }

    /**
     *  Returns a new {@link Style} with the provided background foundation color.
     *  The background color covers the entire component area, including the padding spaces.
     *
     * @param color The background color.
     * @return A new {@link Style} with the provided background color.
     */
    public Style foundationColor( Color color ) { return _withBackground(_background.foundationColor(color)); }

    /**
     *  Returns a new {@link Style} with the provided background foundation color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The background color covers the entire component area, including the padding spaces.
     *
     * @param colorString The background color.
     * @return A new {@link Style} with the provided background color.
     */
    public Style foundationColor( String colorString ) { return _withBackground(_background.foundationColor(StyleUtility.toColor(colorString))); }

    /**
     *  Returns a new {@link Style} with the provided background renderer, a {@link Painter} that
     *  will be called using the {@link Graphics2D} instance used to render the component.
     *  You may use this to render a custom background for the component.
     * @param renderer The background renderer.
     * @return A new {@link Style} with the provided background renderer.
     */
    public Style backgroundPainter( Painter renderer ) {
        return _withBackground(_background.painter(StyleUtility.DEFAULT_KEY, renderer));
    }

    /**
     *  Returns a new {@link Style} with the provided named background renderer, a {@link Painter} that
     *  will be called using the {@link Graphics2D} instance used to render the component.
     *  You may use this to render a custom background for the component.
     *  The name can be used to override {@link Painter} instances with that same name
     *  or use a unique name to ensure that you style is not overridden by another style.
     *
     * @param painterName The name of the painter.
     * @param renderer The background renderer.
     * @return A new {@link Style} with the provided background renderer.
     */
    public Style backgroundPainter( String painterName, Painter renderer ) {
        return _withBackground(_background.painter(painterName, renderer));
    }

    /**
     *  Returns a new {@link Style} with the provided foreground color.
     *
     * @param color The foreground color.
     * @return A new {@link Style} with the provided foreground color.
     */
    public Style foregroundColor( Color color ) { return _withForeground(_foreground.color(color)); }

    /**
     *  Returns a new {@link Style} with the provided foreground color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *
     * @param colorString The foreground color.
     * @return A new {@link Style} with the provided foreground color.
     */
    public Style foregroundColor( String colorString ) { return _withForeground(_foreground.color(StyleUtility.toColor(colorString))); }

    /**
     *  Returns a new {@link Style} with the provided foreground painter, a {@link Painter} that
     *  will be called using the {@link Graphics2D} instance used to render the component.
     *  You may use this to render a custom foreground for the component.
     * @param painter The foreground renderer.
     * @return A new {@link Style} with the provided foreground renderer.
     */
    public Style foregroundPainter( Painter painter ) { return _withForeground(_foreground.painter(StyleUtility.DEFAULT_KEY, painter)); }

    /**
     *  Returns a new {@link Style} with the provided named foreground painter, a {@link Painter} that
     *  will be called using the {@link Graphics2D} instance used to render the component.
     *  You may use this to render a custom foreground for the component.
     *  The name can be used to override {@link Painter} instances with that same name
     *  or use a unique name to ensure that you style is not overridden by another style.
     *
     * @param painterName The name of the painter.
     * @param painter The foreground renderer.
     * @return A new {@link Style} with the provided foreground renderer.
     */
    public Style foregroundPainter( String painterName, Painter painter ) { return _withForeground(_foreground.painter(painterName, painter)); }

    /**
     *  Returns a new {@link Style} with the provided horizontal shadow offset.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call {@link #shadowSpreadRadius(int)},
     *  {@link #shadowBlurRadius(int)} and {@link #shadowColor(Color)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Function)}.
     *
     * @param offset The shadow offset in pixels.
     * @return A new {@link Style} with the provided horizontal shadow offset.
     */
    public Style shadowHorizontalOffset( int offset ) { return _withShadow( shadow -> shadow.horizontalOffset(offset)); }

    /**
     *  Returns a new {@link Style} with the provided vertical shadow offset.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call {@link #shadowSpreadRadius(int)},
     *  {@link #shadowBlurRadius(int)} and {@link #shadowColor(Color)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Function)}.
     *
     * @param offset The shadow offset in pixels.
     * @return A new {@link Style} with the provided vertical shadow offset.
     */
    public Style shadowVerticalOffset( int offset ) { return _withShadow( shadow -> shadow.verticalOffset(offset)); }

    /**
     *  Returns a new {@link Style} with the provided shadow offset.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call {@link #shadowSpreadRadius(int)},
     *  {@link #shadowBlurRadius(int)} and {@link #shadowColor(Color)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Function)}.
     *
     * @param horizontalOffset The horizontal shadow offset in pixels.
     * @param verticalOffset The vertical shadow offset in pixels.
     * @return A new {@link Style} with the provided shadow offset.
     */
    public Style shadowOffset( int horizontalOffset, int verticalOffset ) {
        return _withShadow( shadow -> shadow.horizontalOffset(horizontalOffset).verticalOffset(verticalOffset));
    }

    /**
     *  Returns a new {@link Style} with the provided horizontal and vertical shadow offset.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call {@link #shadowSpreadRadius(int)},
     *  {@link #shadowBlurRadius(int)} and {@link #shadowColor(Color)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Function)}.
     *
     * @param horizontalAndVerticalOffset The horizontal and vertical shadow offset in pixels.
     * @return A new {@link Style} with the provided shadow offset.
     */
    public Style shadowOffset( int horizontalAndVerticalOffset ) {
        return _withShadow( shadow -> shadow.horizontalOffset(horizontalAndVerticalOffset).verticalOffset(horizontalAndVerticalOffset));
    }

    /**
     *  Returns a new {@link Style} with the provided shadow blur radius.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call
     *  {@link #shadowSpreadRadius(int)} and {@link #shadowColor(Color)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Function)}.
     *
     * @param radius The shadow blur radius in pixels.
     * @return A new {@link Style} with the provided shadow blur radius.
     */
    public Style shadowBlurRadius( int radius ) { return _withShadow( shadow -> shadow.blurRadius(radius)); }

    /**
     *  Returns a new {@link Style} with the provided shadow spread radius.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call
     *  {@link #shadowBlurRadius(int)} and {@link #shadowColor(Color)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Function)}.
     *
     * @param radius The shadow spread radius in pixels.
     * @return A new {@link Style} with the provided shadow spread radius.
     */
    public Style shadowSpreadRadius( int radius ) { return _withShadow( shadow -> shadow.spreadRadius(radius)); }

    /**
     *  Returns a new {@link Style} with the provided shadow color.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call
     *  {@link #shadowBlurRadius(int)} and {@link #shadowSpreadRadius(int)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Function)}.
     *
     * @param color The shadow color.
     * @return A new {@link Style} with the provided shadow color.
     */
    public Style shadowColor( Color color ) { return _withShadow( shadow -> shadow.color(color)); }

    /**
     *  Returns a new {@link Style} with the provided shadow color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call
     *  {@link #shadowBlurRadius(int)} and {@link #shadowSpreadRadius(int)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Function)}.
     *
     * @param colorString The shadow color.
     * @return A new {@link Style} with the provided shadow color.
     */
    public Style shadowColor( String colorString ) { return _withShadow( shadow -> shadow.color(StyleUtility.toColor(colorString))); }

    /**
     *  Use this to control whether the shadow should be rendered inwards or outwards. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Function)}. <br>
     *  (see {@link #shadow(String, Function)} for an example of how to use named shadows)
     *
     * @param inwards Whether the shadow should be rendered inwards or outwards.
     * @return A new {@link Style} with the provided shadow inset flag.
     */
    public Style shadowIsInset( boolean inwards ) { return _withShadow( shadow -> shadow.isInset(inwards)); }

    /**
     *  This method makes it possible to define multiple shadows for a single component
     *  through a unique name.
     *  This is useful when you want to do advanced shadow effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *      UI.panel()
     *      .withStyle( it ->
     *          it.style()
     *          .shadow("dark shading", shadow ->
     *              shadow.color("#000000")
     *              .horizontalOffset(5)
     *              .verticalOffset(5)
     *              .blurRadius(10)
     *              .spreadRadius(0)
     *          )
     *          .shadow("light shading", shadow ->
     *              shadow.color("#ffffff")
     *              .horizontalOffset(-5)
     *              .verticalOffset(-5)
     *              .blurRadius(10)
     *              .spreadRadius(0)
     *          )
     *  }</pre>
     *  Note that shadows will be rendered in alphabetical order based on their name.
     *
     * @param shadowName The name of the shadow.
     * @param styler A function that takes a {@link ShadowStyle} and returns a new {@link ShadowStyle}.
     * @return A new {@link Style} with a named shadow defined by the provided styler lambda.
     */
    public Style shadow( String shadowName, Function<ShadowStyle, ShadowStyle> styler ) {
        Objects.requireNonNull(shadowName);
        Objects.requireNonNull(styler);
        ShadowStyle shadow = Optional.ofNullable(_shadows.get(shadowName)).orElse(ShadowStyle.none());
        // We clone the shadow map:
        Map<String, ShadowStyle> newShadows = new HashMap<>(_shadows);
        newShadows.put(shadowName, styler.apply(shadow));
        return _withShadow(newShadows);
    }

    /**
     *  This method makes it possible to define multiple background shades for a single component
     *  through a unique name for said shading effect.
     *  This is useful when you want to do advanced background effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it ->
     *      it.style()
     *      .backgroundShade("dark shading", shade -> shade.colors("#000000", "#000000").strategy(ShadeStrategy.TOP_TO_BOTTOM)))
     *      .backgroundShade("light shading", shade -> shade.colors("#ffffff", "#ffffff").strategy(ShadeStrategy.TOP_TO_BOTTOM)))
     *    )
     * }</pre>
     * Note that the background shades will be rendered in alphabetical order based on the name of the shade.
     *
     * @param shadeName The name of the background shade.
     * @param styler A function that takes a {@link ShadeStyle} and returns a new {@link ShadeStyle}.
     * @return A new {@link Style} with a named background shade defined by the provided styler lambda.
     */
    public Style backgroundShade( String shadeName, Function<ShadeStyle, ShadeStyle> styler ) {
        Objects.requireNonNull(shadeName);
        Objects.requireNonNull(styler);
        return _withBackground(background().shade(shadeName, styler));
    }

    /**
     *  This method makes it possible to define a background shade for your components.
     *  This is useful when you want to do advanced background effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it ->
     *      it.style()
     *      .backgroundShade(shade -> shade.colors("#000000", "#000000").strategy(ShadeStrategy.TOP_TO_BOTTOM)))
     *    )
     * }</pre>
     *
     * @param styler A function that takes a {@link ShadeStyle} and returns a new {@link ShadeStyle}.
     * @return A new {@link Style} with a background shade defined by the provided styler lambda.
     */
    public Style backgroundShade( Function<ShadeStyle, ShadeStyle> styler ) {
        Objects.requireNonNull(styler);
        return _withBackground(background().shade(StyleUtility.DEFAULT_KEY, styler));
    }

    /**
     *  Returns a new {@link Style} with the provided font name and size.
     *  Note that the font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param name The font name.
     * @param size The font size.
     * @return A new {@link Style} with the provided font name and size.
     */
    public Style font( String name, int size ) { return _withFont(_font.name(name).size(size)); }

    /**
     *  Returns a new {@link Style} with the provided font name.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param name The font name.
     * @return A new {@link Style} with the provided font name.
     */
    public Style fontName( String name ) { return _withFont(_font.name(name)); }

    /**
     *  Returns a new {@link Style} with the provided {@link Font}.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param font The {@link Font}.
     * @return A new {@link Style} with the provided {@link Font}.
     */
    public Style font( Font font ) { return _withFont(_font.font(font)); }

    /**
     *  Returns a new {@link Style} with the provided font size.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param size The font size.
     * @return A new {@link Style} with the provided font size.
     */
    public Style fontSize( int size ) { return _withFont(_font.size(size)); }

    /**
     *  Makes the font bold or not bold depending on the value of the {@code isBold} parameter.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param bold Whether the font should be bold or not.
     * @return A new {@link Style} with the provided font boldness.
     */
    public Style fontBold( boolean bold ) {
        return _withFont(_font.style( bold ? Font.BOLD : Font.PLAIN ));
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
        return _withFont(_font.style( italic ? Font.ITALIC : Font.PLAIN ));
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
        return _withFont(_font.attributes(attributes));
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
        return _withFont(_font.attributes(attributes));
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
        return _withFont(_font.color(color));
    }

    /**
     *  Creates a new {@link Style} where the font color is set to a color parsed from the provided string.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param colorString The {@link Color} as a string.
     * @return A new {@link Style} with the provided font color.
     */
    public Style fontColor( String colorString ) {
        return _withFont(_font.color(StyleUtility.toColor(colorString)));
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
        return _withFont(_font.backgroundColor(color));
    }

    /**
     *  Creates a new {@link Style} where the font color is set to a color parsed from the provided string.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param colorString The {@link Color} as a string.
     * @return A new {@link Style} with the provided font color.
     */
    public Style fontBackgroundColor( String colorString ) {
        return _withFont(_font.backgroundColor(StyleUtility.toColor(colorString)));
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
        return _withFont(_font.selectionColor(color));
    }

    /**
     *  Creates a new {@link Style} where the font selection color is set to a color parsed from the provided string.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param colorString The {@link Color} as a string.
     * @return A new {@link Style} with the provided font selection color.
     */
    public Style fontSelectionColor( String colorString ) {
        return _withFont(_font.selectionColor(StyleUtility.toColor(colorString)));
    }

    /**
     *  Use this to define the weight of the default font of the component.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     * @param weight The weight of the font.
     * @return A new {@link Style} with the provided font weight.
     */
    public Style fontWeight( int weight ) { return _withFont(_font.weight(weight)); }

    /**
     *  Use this to define an array of {@link TextAttribute}s to be applied to the default font of the component.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     *  @param attributes The {@link TextAttribute}s to apply to the font.
     *  @return A new {@link Style} with the provided font attributes.
     */
    public Style fontAttributes( TextAttribute... attributes ) { return _withFont(_font.attributes(Objects.requireNonNull(attributes))); }


    public Outline padding() { return _layout.padding(); }

    public Outline margin() { return _layout.margin(); }

    public BorderStyle border() { return _border; }

    public BackgroundStyle background() { return _background; }

    public ForegroundStyle foreground() { return _foreground; }

    public ShadowStyle shadow() { return _shadows.get(StyleUtility.DEFAULT_KEY); }

    public ShadowStyle shadow(String shadowName) {
        Objects.requireNonNull(shadowName);
        return _shadows.get(shadowName);
    }

    /**
     * @return An unmodifiable list of all shadow styles sorted by their names in ascending alphabetical order.
     */
    public List<ShadowStyle> shadows() {
        return Collections.unmodifiableList(
                _shadows.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList())
            );
    }

    public boolean anyVisibleShadows() {
        return _shadows.values().stream().anyMatch(s -> s.color().isPresent() && s.color().get().getAlpha() > 0 );
    }

    public FontStyle font() { return _font; }

    @Override
    public int hashCode() {
        return Objects.hash(_layout, _border, _background, _foreground, _font, StyleUtility.mapHash(_shadows));
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == this ) return true;
        if ( obj == null ) return false;
        if ( !(obj instanceof Style) ) return false;
        Style other = (Style) obj;
        return Objects.equals(_layout,     other._layout    ) &&
               Objects.equals(_border,     other._border    ) &&
               Objects.equals(_background, other._background) &&
               Objects.equals(_foreground, other._foreground) &&
               Objects.equals(_font,       other._font      ) &&
                StyleUtility.mapEquals(_shadows,    other._shadows   );
    }

    @Override
    public String toString() {
        String shadowString;
        if ( _shadows.size() == 1 )
            shadowString = _shadows.get(StyleUtility.DEFAULT_KEY).toString();
        else
            shadowString = _shadows.entrySet()
                                    .stream()
                                    .map(e -> e.getKey() + ": " + e.getValue())
                                    .collect(Collectors.joining(", ", "shadows=[", "]"));

        return "Style[" +
                    _layout     + ", " +
                    _border     + ", " +
                    _background + ", " +
                    _foreground + ", " +
                    shadowString + ", " +
                    _font       +
                "]";
    }

}

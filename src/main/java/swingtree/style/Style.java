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
                                            Collections.singletonMap(ShadowStyle.DEFAULT_KEY,ShadowStyle.none())
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
        return _withLayout(_layout.withMargin(_layout.margin().withTop(top).withLeft(left).withRight(right).withBottom(bottom)));
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
        return _withLayout(_layout.withMargin(_layout.margin().withTop(margin).withLeft(margin).withRight(margin).withBottom(margin)));
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
        return _withLayout(_layout.withMargin(_layout.margin().withTop(margin)));
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
        return _withLayout(_layout.withMargin(_layout.margin().withRight(margin)));
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
    public Style marginBottom( int margin ) {
        return _withLayout(_layout.withMargin(_layout.margin().withBottom(margin)));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the left side of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link Style} with the provided margin distance.
     */
    public Style marginLeft( int margin ) {
        return _withLayout(_layout.withMargin(_layout.margin().withLeft(margin)));
    }

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
        return _withLayout(_layout.withMargin(_layout.margin().withTop(margin).withBottom(margin)));
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
        return _withLayout(_layout.withMargin(_layout.margin().withLeft(margin).withRight(margin)));
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
        return _withLayout(_layout.withPadding(_layout.padding().withTop(top).withLeft(left).withRight(right).withBottom(bottom)));
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
        return _withLayout(_layout.withPadding(_layout.padding().withTop(padding).withLeft(padding).withRight(padding).withBottom(padding)));
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
        return _withLayout(_layout.withPadding(_layout.padding().withTop(padding)));
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
        return _withLayout(_layout.withPadding(_layout.padding().withRight(padding)));
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
        return _withLayout(_layout.withPadding(_layout.padding().withBottom(padding)));
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
        return _withLayout(_layout.withPadding(_layout.padding().withLeft(padding)));
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
        return _withLayout(_layout.withPadding(_layout.padding().withTop(padding).withBottom(padding)));
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
        return _withLayout(_layout.withPadding(_layout.padding().withLeft(padding).withRight(padding)));
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
        return _withBorder(_border.withWidth(width).withColor(color));
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
        return _withBorder(_border.withWidth(width).withColor(_colorFrom(colorString)));
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
     *  Returns a new {@link Style} with the provided border width for the specified edge.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param edge The edge to set the border width for.
     * @param width The border width in pixels.
     * @return A new {@link Style} with the provided border width for the specified edge.
     */
    public Style borderWidthAt( Edge edge, int width ) {
        return _withBorder(_border.withWidthAt(edge, width));
    }

    /**
     *  Returns a new {@link Style} with the provided border color.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param color The border color.
     * @return A new {@link Style} with the provided border color.
     */
    public Style borderColor( Color color ) {
        return _withBorder(_border.withColor(color));
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
        return _withBorder(_border.withColor(_colorFrom(colorString)));
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
    public Style borderRadius( int arcWidth, int arcHeight ) {
        return _withBorder(_border.withArcWidth(arcWidth).withArcHeight(arcHeight));
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
        return _withBorder(_border.withArcWidthAt(corner, arcWidth).withArcHeightAt(corner, arcHeight));
    }

    /**
     *  Returns a new {@link Style} with the provided inner Background color.
     *  The inner background will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param color The inner background color.
     * @return A new {@link Style} with the provided inner background color.
     */
    public Style backgroundColor( Color color ) { return _withBackground(_background.withColor(color)); }

    /**
     *  Returns a new {@link Style} with the provided inner Background color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The inner background will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param colorString The inner background color.
     * @return A new {@link Style} with the provided inner background color.
     */
    public Style backgroundColor( String colorString ) { return _withBackground(_background.withColor(_colorFrom(colorString))); }

    /**
     *  Returns a new {@link Style} with the provided background foundation color.
     *  The background color covers the entire component area, including the padding spaces.
     *
     * @param color The background color.
     * @return A new {@link Style} with the provided background color.
     */
    public Style foundationColor( Color color ) { return _withBackground(_background.withFoundationColor(color)); }

    /**
     *  Returns a new {@link Style} with the provided background foundation color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The background color covers the entire component area, including the padding spaces.
     *
     * @param colorString The background color.
     * @return A new {@link Style} with the provided background color.
     */
    public Style foundationColor( String colorString ) { return _withBackground(_background.withFoundationColor(_colorFrom(colorString))); }

    /**
     *  Returns a new {@link Style} with the provided background renderer, a {@link Painter} that
     *  will be called using the {@link Graphics2D} instance used to render the component.
     *  You may use this to render a custom background for the component.
     * @param renderer The background renderer.
     * @return A new {@link Style} with the provided background renderer.
     */
    public Style backgroundPainter( Painter renderer ) { return _withBackground(_background.withPainter(renderer)); }

    /**
     *  Returns a new {@link Style} with the provided foreground color.
     *
     * @param color The foreground color.
     * @return A new {@link Style} with the provided foreground color.
     */
    public Style foregroundColor( Color color ) { return _withForeground(_foreground.withColor(color)); }

    /**
     *  Returns a new {@link Style} with the provided foreground color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *
     * @param colorString The foreground color.
     * @return A new {@link Style} with the provided foreground color.
     */
    public Style foregroundColor( String colorString ) { return _withForeground(_foreground.withColor(_colorFrom(colorString))); }

    /**
     *  Returns a new {@link Style} with the provided foreground painter, a {@link Painter} that
     *  will be called using the {@link Graphics2D} instance used to render the component.
     *  You may use this to render a custom foreground for the component.
     * @param painter The foreground renderer.
     * @return A new {@link Style} with the provided foreground renderer.
     */
    public Style foregroundPainter( Painter painter ) { return _withForeground(_foreground.withPainter(painter)); }

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
    public Style shadowHorizontalOffset( int offset ) { return _withShadow( shadow -> shadow.withHorizontalOffset(offset)); }

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
    public Style shadowVerticalOffset( int offset ) { return _withShadow( shadow -> shadow.withVerticalOffset(offset)); }

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
        return _withShadow( shadow -> shadow.withHorizontalOffset(horizontalOffset).withVerticalOffset(verticalOffset));
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
        return _withShadow( shadow -> shadow.withHorizontalOffset(horizontalAndVerticalOffset).withVerticalOffset(horizontalAndVerticalOffset));
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
    public Style shadowBlurRadius( int radius ) { return _withShadow( shadow -> shadow.withBlurRadius(radius)); }

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
    public Style shadowSpreadRadius( int radius ) { return _withShadow( shadow -> shadow.withSpreadRadius(radius)); }

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
    public Style shadowColor( Color color ) { return _withShadow( shadow -> shadow.withColor(color)); }

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
    public Style shadowColor( String colorString ) { return _withShadow( shadow -> shadow.withColor(_colorFrom(colorString))); }

    /**
     *  Use this to control whether the shadow should be rendered inwards or outwards. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Function)}. <br>
     *  (see {@link #shadow(String, Function)} for an example of how to use named shadows)
     *
     * @param inwards Whether the shadow should be rendered inwards or outwards.
     * @return A new {@link Style} with the provided shadow inset flag.
     */
    public Style shadowIsInset( boolean inwards ) { return _withShadow( shadow -> shadow.withIsInset(inwards)); }

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
    public Style fontName( String name ) { return _withFont(_font.withName(name)); }

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
     *  Creates a new {@link Style} where the font color is set to a color parsed from the provided string.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param colorString The {@link Color} as a string.
     * @return A new {@link Style} with the provided font color.
     */
    public Style fontColor( String colorString ) {
        return _withFont(_font.withColor(_colorFrom(colorString)));
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
     *  Creates a new {@link Style} where the font color is set to a color parsed from the provided string.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param colorString The {@link Color} as a string.
     * @return A new {@link Style} with the provided font color.
     */
    public Style fontBackgroundColor( String colorString ) {
        return _withFont(_font.withBackgroundColor(_colorFrom(colorString)));
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

    /**
     *  Creates a new {@link Style} where the font selection color is set to a color parsed from the provided string.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param colorString The {@link Color} as a string.
     * @return A new {@link Style} with the provided font selection color.
     */
    public Style fontSelectionColor( String colorString ) {
        return _withFont(_font.withSelectionColor(_colorFrom(colorString)));
    }

    public Outline padding() { return _layout.padding(); }

    public Outline margin() { return _layout.margin(); }

    public BorderStyle border() { return _border; }

    public BackgroundStyle background() { return _background; }

    public ForegroundStyle foreground() { return _foreground; }

    public ShadowStyle shadow() { return _shadows.get(ShadowStyle.DEFAULT_KEY); }

    public List<ShadowStyle> shadows() {
        return new ArrayList<>(_shadows.values());
    }

    public boolean anyVisibleShadows() {
        return _shadows.values().stream().anyMatch(s -> s.color().isPresent() && s.color().get().getAlpha() > 0 );
    }

    public FontStyle font() { return _font; }

    @Override
    public int hashCode() {
        return Objects.hash(_layout, _border, _background, _foreground, _font, _mapHash(_shadows));
    }

    private int _mapHash( Map<String, ShadowStyle> map ) {
        return map.entrySet().stream().mapToInt(e -> Objects.hash(e.getKey(), e.getValue())).sum();
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
                _shadowEquals(_shadows,    other._shadows   );
    }

    private boolean _shadowEquals( Map<String, ShadowStyle> map1, Map<String, ShadowStyle> map2 ) {
        if ( map1.size() != map2.size() ) return false;
        for ( Map.Entry<String, ShadowStyle> entry : map1.entrySet() ) {
            if ( !map2.containsKey(entry.getKey()) ) return false;
            if ( !Objects.equals(entry.getValue(), map2.get(entry.getKey())) ) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String shadowString;
        if ( _shadows.size() == 1 )
            shadowString = _shadows.get(ShadowStyle.DEFAULT_KEY).toString();
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

    /**
     *  Tries to parse the supplied string as a color value
     *  based on various formats.
     *
     * @param colorString The string to parse.
     * @return The parsed color.
     * @throws IllegalArgumentException If the string could not be parsed.
     */
    private Color _colorFrom( String colorString )
    {
        // First some cleanup
        colorString = colorString.trim();

        if ( colorString.startsWith("#") )
            return Color.decode(colorString);

        if ( colorString.startsWith("0x") )
            return Color.decode(colorString);

        if ( colorString.startsWith("rgb") ) {
            // We have an rgb() or rgba() color
            int start = colorString.indexOf('(');
            int end = colorString.indexOf(')');
            if ( start < 0 || end < 0 || end < start )
                throw new IllegalArgumentException("Invalid rgb() or rgba() color: " + colorString);

            String[] parts = colorString.substring(start + 1, end).split(",");
            if ( parts.length < 3 || parts.length > 4 )
                throw new IllegalArgumentException("Invalid rgb() or rgba() color: " + colorString);

            for ( int i = 0; i < parts.length; i++ )
                parts[i] = parts[i].trim();

            int[] values = new int[parts.length];

            for ( int i = 0; i < parts.length; i++ ) {
                String part = parts[i];
                if ( part.endsWith("%") ) {
                    part = part.substring(0, part.length() - 1);
                    values[i] = Integer.parseInt(part);
                    if ( values[i] < 0 || values[i] > 100 )
                        throw new IllegalArgumentException("Invalid rgb() or rgba() color: " + colorString);
                    values[i] = (int) Math.ceil(values[i] * 2.55);
                }
                else if ( part.matches("[0-9]+((\\.[0-9]+[fF]?)|[fF])") )
                    values[i] = (int) (Float.parseFloat(part) * 255);
                else
                    values[i] = Integer.parseInt(part);
            }
            int r = values[0];
            int g = values[1];
            int b = values[2];
            int a = values.length == 4 ? values[3] : 255;
            return new Color(r, g, b, a);
        }

        if ( colorString.startsWith("hsb") ) {
            // We have an hsb() or hsba() color
            int start = colorString.indexOf('(');
            int end = colorString.indexOf(')');
            if ( start < 0 || end < 0 || end < start )
                throw new IllegalArgumentException("Invalid hsb() or hsba() color: " + colorString);

            String[] parts = colorString.substring(start + 1, end).split(",");
            if ( parts.length < 3 || parts.length > 4 )
                throw new IllegalArgumentException("Invalid hsb() or hsba() color: " + colorString);

            for ( int i = 0; i < parts.length; i++ )
                parts[i] = parts[i].trim();

            float[] values = new float[parts.length];

            for ( int i = 0; i < parts.length; i++ ) {
                String part = parts[i];
                if ( part.endsWith("%") ) {
                    part = part.substring(0, part.length() - 1);
                    values[i] = Float.parseFloat(part);
                    if ( values[i] < 0 || values[i] > 100 )
                        throw new IllegalArgumentException(
                            "Invalid hsb() or hsba() string '" + colorString + "', value '" + part + "' out of range."
                        );
                    values[i] = values[i] / 100.0f;
                } else if ( part.endsWith("°") ) {
                    if ( i > 0 )
                        throw new IllegalArgumentException(
                            "Invalid hsb() or hsba() string '" + colorString + "', unexpected degree symbol in '" + part + "' (only allowed for hue)"
                        );

                    part = part.substring(0, part.length() - 1);
                    values[i] = Float.parseFloat(part);
                    if ( values[i] < 0 || values[i] > 360 )
                        throw new IllegalArgumentException(
                            "Invalid hsb() or hsba() string '" + colorString + "', hue value '" + part + "' out of range."
                        );
                    values[i] = values[i] / 360.0f;
                } else if ( part.matches("[0-9]+((\\.[0-9]+[fF]?)|[fF])") )
                    values[i] = Float.parseFloat(part);
                else
                    values[i] = Integer.parseInt(part);
            }

            float h = values[0];
            float s = values[1];
            float b = values[2];
            float a = values.length == 4 ? values[3] : 1.0f;
            Color c = Color.getHSBColor(h, s, b);
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(a * 255));
        }

        // Let's try a few common color names
        if ( colorString.equalsIgnoreCase("black")       ) return Color.BLACK;
        if ( colorString.equalsIgnoreCase("blue")        ) return Color.BLUE;
        if ( colorString.equalsIgnoreCase("cyan")        ) return Color.CYAN;
        if ( colorString.equalsIgnoreCase("darkGray")    ) return Color.DARK_GRAY;
        if ( colorString.equalsIgnoreCase("gray")        ) return Color.GRAY;
        if ( colorString.equalsIgnoreCase("green")       ) return Color.GREEN;
        if ( colorString.equalsIgnoreCase("lightGray")   ) return Color.LIGHT_GRAY;
        if ( colorString.equalsIgnoreCase("magenta")     ) return Color.MAGENTA;
        if ( colorString.equalsIgnoreCase("orange")      ) return Color.ORANGE;
        if ( colorString.equalsIgnoreCase("pink")        ) return Color.PINK;
        if ( colorString.equalsIgnoreCase("red")         ) return Color.RED;
        if ( colorString.equalsIgnoreCase("white")       ) return Color.WHITE;
        if ( colorString.equalsIgnoreCase("yellow")      ) return Color.YELLOW;
        if ( colorString.equalsIgnoreCase("transparent") ) return new Color(0, 0, 0, 0);

        // Let's try to find it as a system property
        try {
            return Color.getColor(colorString);
        } catch ( IllegalArgumentException e ) {
            // Ignore
        }

        throw new IllegalArgumentException("Could not parse or find color value: " + colorString);
    }
}

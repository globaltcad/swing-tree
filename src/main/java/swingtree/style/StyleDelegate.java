package swingtree.style;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.List;
import java.util.*;
import java.util.function.Function;

/**
 *  A {@link StyleDelegate} is a delegate for a {@link JComponent} and its {@link Style} configuration
 *  used to apply further specify the style of said {@link JComponent}.
 *  Instances of this will be exposed to you via the {@link swingtree.UIForAnySwing#withStyle(Styler)}
 *  method, where you can specify a lambda that takes a {@link StyleDelegate} and returns a
 *  transformed {@link Style} object, as well as inside of {@link StyleSheet} extensions
 *  where you can declare similar styling lambdas for {@link StyleTrait}s, which are
 *  styling rules... <br>
 *
 * @param <C> The type of {@link JComponent} this {@link StyleDelegate} is for.
 */
public final class StyleDelegate<C extends JComponent>
{
    private final C _component;
    private final Style _style;

    public StyleDelegate( C component, Style style ) {
        _component = component;
        _style     = style;
    }

    public C component() { return _component; }

    public Style style() { return _style; }
    
    StyleDelegate<C> _withStyle( Style style ) {
        return new StyleDelegate<>(_component, style);
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
     * @return A new {@link StyleDelegate} with the provided padding distances.
     */
    public StyleDelegate<C> margin( int top, int right, int bottom, int left ) {
        return _withStyle(_style._withLayout(_style.layout().margin(_style.layout().margin().top(top).left(left).right(right).bottom(bottom))));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for all sides of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link StyleDelegate} with the provided margin distance.
     */
    public StyleDelegate<C> margin( int margin ) {
        return _withStyle(_style._withLayout(_style.layout().margin(_style.layout().margin().top(margin).left(margin).right(margin).bottom(margin))));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the top side of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link StyleDelegate} with the provided margin distance.
     */
    public StyleDelegate<C> marginTop( int margin ) {
        return _withStyle(_style._withLayout(_style.layout().margin(_style.layout().margin().top(margin))));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the right side of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link StyleDelegate} with the provided margin distance.
     */
    public StyleDelegate<C> marginRight( int margin ) {
        return _withStyle(_style._withLayout(_style.layout().margin(_style.layout().margin().right(margin))));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the bottom side of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link StyleDelegate} with the provided margin distance.
     */
    public StyleDelegate<C> marginBottom( int margin ) {
        return _withStyle(_style._withLayout(_style.layout().margin(_style.layout().margin().bottom(margin))));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the left side of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link StyleDelegate} with the provided margin distance.
     */
    public StyleDelegate<C> marginLeft( int margin ) {
        return _withStyle(_style._withLayout(_style.layout().margin(_style.layout().margin().left(margin))));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the top and bottom sides of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link StyleDelegate} with the provided margin distance.
     */
    public StyleDelegate<C> marginVertical( int margin ) {
        return _withStyle(_style._withLayout(_style.layout().margin(_style.layout().margin().top(margin).bottom(margin))));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the left and right sides of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link StyleDelegate} with the provided margin distance.
     */
    public StyleDelegate<C> marginHorizontal( int margin ) {
        return _withStyle(_style._withLayout(_style.layout().margin(_style.layout().margin().left(margin).right(margin))));
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
     * @return A new {@link StyleDelegate} with the provided padding distances.
     */
    public StyleDelegate<C> padding( int top, int right, int bottom, int left ) {
        return _withStyle(_style._withLayout(_style.layout().padding(_style.layout().padding().top(top).left(left).right(right).bottom(bottom))));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for all sides of the component.
     *  It determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link StyleDelegate} with the provided padding distance.
     */
    public StyleDelegate<C> padding( int padding ) {
        return _withStyle(_style._withLayout(_style.layout().padding(_style.layout().padding().top(padding).left(padding).right(padding).bottom(padding))));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the top side of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link StyleDelegate} with the provided padding distance.
     */
    public StyleDelegate<C> paddingTop( int padding ) {
        return _withStyle(_style._withLayout(_style.layout().padding(_style.layout().padding().top(padding))));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the right side of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link StyleDelegate} with the provided padding distance.
     */
    public StyleDelegate<C> paddingRight( int padding ) {
        return _withStyle(_style._withLayout(_style.layout().padding(_style.layout().padding().right(padding))));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the bottom side of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link StyleDelegate} with the provided padding distance.
     */
    public StyleDelegate<C> paddingBottom( int padding ) {
        return _withStyle(_style._withLayout(_style.layout().padding(_style.layout().padding().bottom(padding))));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the left side of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link StyleDelegate} with the provided padding distance.
     */
    public StyleDelegate<C> paddingLeft( int padding ) {
        return _withStyle(_style._withLayout(_style.layout().padding(_style.layout().padding().left(padding))));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the top and bottom sides of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link StyleDelegate} with the provided padding distance.
     */
    public StyleDelegate<C> paddingVertical( int padding ) {
        return _withStyle(_style._withLayout(_style.layout().padding(_style.layout().padding().top(padding).bottom(padding))));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the left and right sides of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link StyleDelegate} with the provided padding distance.
     */
    public StyleDelegate<C> paddingHorizontal( int padding ) {
        return _withStyle(_style._withLayout(_style.layout().padding(_style.layout().padding().left(padding).right(padding))));
    }

    /**
     *  Returns a new {@link Style} with the provided border width and border color.
     *  The border will be rendered with an inset space based on the margin defined by the {@link Style}.
     *
     * @param width The border width in pixels.
     * @param color The border color.
     * @return A new {@link StyleDelegate} with the provided border width and border color.
     */
    public StyleDelegate<C> border( int width, Color color ) {
        return _withStyle(_style._withBorder(_style.border().width(width).color(color)));
    }

    /**
     *  Returns a new {@link Style} with the provided border widths and border color.
     *  The border will be rendered with an inset space based on the margin defined by the {@link Style}.
     *
     * @param top The border width in pixels for the top side of the component.
     * @param right The border width in pixels for the right side of the component.
     * @param bottom The border width in pixels for the bottom side of the component.
     * @param left The border width in pixels for the left side of the component.
     * @param color The border color.
     * @return A new {@link StyleDelegate} with the provided border widths and border color.
     */
    public StyleDelegate<C> border( int top, int right, int bottom, int left, Color color ) {
        return _withStyle(_style._withBorder(_style.border().widths(Outline.of(top, right, bottom, left)).color(color)));
    }

    /**
     *  Returns a new {@link Style} with the provided border width and border color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param width The border width in pixels.
     * @param colorString The border color.
     * @return A new {@link StyleDelegate} with the provided border width and border color.
     */
    public StyleDelegate<C> border( int width, String colorString ) {
        return _withStyle(_style._withBorder(_style.border().width(width).color(StyleUtility.toColor(colorString))));
    }

    /**
     *  Returns a new {@link Style} with the provided border width.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param width The border width in pixels.
     * @return A new {@link StyleDelegate} with the provided border width.
     */
    public StyleDelegate<C> borderWidth( int width ) {
        return _withStyle(_style._withBorder(_style.border().width(width)));
    }

    /**
     *  Returns a new {@link Style} with the provided border width for the specified edge.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param edge The edge to set the border width for.
     * @param width The border width in pixels.
     * @return A new {@link StyleDelegate} with the provided border width for the specified edge.
     */
    public StyleDelegate<C> borderWidthAt( Edge edge, int width ) {
        return _withStyle(_style._withBorder(_style.border().widthAt(edge, width)));
    }

    /**
     *  Returns a new {@link Style} with the provided top, right, bottom and left border widths.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *  <p>
     *  The border widths are specified in the following order: top, right, bottom, left.
     *  <p>
     *  Example:
     *  <pre>{@code
     *      UI.panel().withStyle( it -> it.borderWidths(1, 2, 3, 4) )
     *  }</pre>
     * @param top The top border width in pixels.
     * @param right The right border width in pixels.
     * @param bottom The bottom border width in pixels.
     * @param left The left border width in pixels.
     * @return A new {@link StyleDelegate} with the provided top, right, bottom and left border widths.
     * @see #borderWidth(int)
     * @see #borderWidthAt(Edge, int)
     */
    public StyleDelegate<C> borderWidths( int top, int right, int bottom, int left ) {
        return _withStyle(_style._withBorder(_style.border().widths(Outline.of(top, right, bottom, left))));
    }

    /**
     *  Returns a new {@link Style} with the provided top/bottom and left/right border widths.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *  <p>
     *  Example:
     *  <pre>{@code
     *      UI.panel().withStyle( it -> it.borderWidths(1, 2) )
     *  }</pre>
     * @param topBottom The top and bottom border width in pixels.
     * @param leftRight The left and right border width in pixels.
     * @return A new {@link StyleDelegate} with the provided top/bottom and left/right border widths.
     * @see #borderWidth(int)
     * @see #borderWidthAt(Edge, int)
     */
    public StyleDelegate<C> borderWidths( int topBottom, int leftRight ) {
        return _withStyle(_style._withBorder(_style.border().widths(Outline.of(topBottom, leftRight, topBottom, leftRight))));
    }

    /**
     *  Returns a new {@link Style} with the provided border color.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param color The border color.
     * @return A new {@link StyleDelegate} with the provided border color.
     */
    public StyleDelegate<C> borderColor( Color color ) {
        return _withStyle(_style._withBorder(_style.border().color(color)));
    }

    /**
     *  Returns a new {@link Style} with the provided border color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param colorString The border color.
     * @return A new {@link StyleDelegate} with the provided border color.
     */
    public StyleDelegate<C> borderColor( String colorString ) {
        return _withStyle(_style._withBorder(_style.border().color(StyleUtility.toColor(colorString))));
    }

    /**
     *  Returns a new {@link Style} with the provided border radius.
     *  This will override both the arc width and arc height of the border.
     *  The border will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param radius The border radius in pixels.
     * @return A new {@link StyleDelegate} with the provided border radius.
     */
    public StyleDelegate<C> borderRadius( int radius ) {
        return _withStyle(_style._withBorder(_style.border().arcWidth(radius).arcHeight(radius)));
    }

    /**
     *  Returns a new {@link Style} with the provided border arc width and arc height.
     *  Note that the border will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param arcWidth The border arc width in pixels.
     * @param arcHeight The border arc height in pixels.
     * @return A new {@link StyleDelegate} with the provided border arc width and arc height.
     */
    public StyleDelegate<C> borderRadius( int arcWidth, int arcHeight ) {
        return _withStyle(_style._withBorder(_style.border().arcWidth(arcWidth).arcHeight(arcHeight)));
    }

    /**
     *  Returns a new {@link Style} with the provided border arc width and arc height for the specified corner.
     *  Note that the border will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param corner The corner to apply the border radius to.
     * @param arcWidth The border arc width in pixels.
     * @param arcHeight The border arc height in pixels.
     * @return A new {@link StyleDelegate} with the provided border arc width and arc height for the specified corner.
     */
    public StyleDelegate<C> borderRadiusAt( Corner corner, int arcWidth, int arcHeight ) {
        return _withStyle(_style._withBorder(_style.border().arcWidthAt(corner, arcWidth).arcHeightAt(corner, arcHeight)));
    }

    /**
     *  This method makes it possible to define border shades for the border of your UI components.
     *  This is useful when you want to do advanced border effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it ->
     *      it.borderShade( shade -> shade.colors("#000000", "#000000").strategy(ShadeStrategy.TOP_TO_BOTTOM) )
     *    )
     * }</pre>
     *
     * @param styler A function that takes a {@link ShadeStyle} and returns a new {@link ShadeStyle}.
     * @return A new {@link StyleDelegate} with a border shade defined by the provided styler lambda.
     */
    public StyleDelegate<C> borderShade( Function<ShadeStyle, ShadeStyle> styler ) {
        Objects.requireNonNull(styler);
        return _withStyle(_style._withBorder(_style.border().shade(StyleUtility.DEFAULT_KEY, styler)));
    }

    /**
     *  This method makes it possible to define multiple border shades through a unique name for said shading effect.
     *  This is useful when you want to do advanced border effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it ->
     *      it
     *      .borderShade("dark shading", shade -> shade.colors("#000000", "#000000").strategy(ShadeStrategy.TOP_TO_BOTTOM)))
     *      .borderShade("light shading", shade -> shade.colors("#ffffff", "#ffffff").strategy(ShadeStrategy.TOP_TO_BOTTOM)))
     *    )
     * }</pre>
     * Note that the border shades will be rendered in alphabetical order based on the name of the shade.
     *
     * @param shadeName The name of the border shade.
     * @param styler A function that takes a {@link ShadeStyle} and returns a new {@link ShadeStyle}.
     * @return A new {@link StyleDelegate} with a named border shade defined by the provided styler lambda.
     */
    public StyleDelegate<C> borderShade( String shadeName, Function<ShadeStyle, ShadeStyle> styler ) {
        Objects.requireNonNull(shadeName);
        Objects.requireNonNull(styler);
        return _withStyle(_style._withBorder(_style.border().shade(shadeName, styler)));
    }

    /**
     *  Returns a new {@link Style} with the provided inner Background color.
     *  The inner background will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param color The inner background color.
     * @return A new {@link StyleDelegate} with the provided inner background color.
     */
    public StyleDelegate<C> backgroundColor( Color color ) {
        return _withStyle(_style._withBackground(_style.background().color(color)));
    }

    /**
     *  Returns a new {@link Style} with the provided inner Background color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The inner background will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param colorString The inner background color.
     * @return A new {@link StyleDelegate} with the provided inner background color.
     */
    public StyleDelegate<C> backgroundColor( String colorString ) {
        return _withStyle(_style._withBackground(_style.background().color(StyleUtility.toColor(colorString))));
    }

    /**
     *  Returns a new {@link Style} with the provided background foundation color.
     *  The background color covers the entire component area, including the padding spaces.
     *
     * @param color The background color.
     * @return A new {@link StyleDelegate} with the provided background color.
     */
    public StyleDelegate<C> foundationColor( Color color ) {
        return _withStyle(_style._withBackground(_style.background().foundationColor(color)));
    }

    /**
     *  Returns a new {@link Style} with the provided background foundation color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The background color covers the entire component area, including the padding spaces.
     *
     * @param colorString The background color.
     * @return A new {@link StyleDelegate} with the provided background color.
     */
    public StyleDelegate<C> foundationColor( String colorString ) {
        return _withStyle(_style._withBackground(_style.background().foundationColor(StyleUtility.toColor(colorString))));
    }

    /**
     *  Returns a new {@link Style} with the provided background renderer, a {@link Painter} that
     *  will be called using the {@link Graphics2D} instance used to render the component.
     *  You may use this to render a custom background for the component.
     * @param renderer The background renderer.
     * @return A new {@link StyleDelegate} with the provided background renderer.
     */
    public StyleDelegate<C> backgroundPainter( Painter renderer ) {
        return _withStyle(_style.painter(StyleUtility.DEFAULT_KEY, Layer.BACKGROUND, renderer));
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
     * @return A new {@link StyleDelegate} with the provided background renderer.
     */
    public StyleDelegate<C> backgroundPainter( String painterName, Painter renderer ) {
        return _withStyle(_style.painter(painterName, Layer.BACKGROUND, renderer));
    }

    /**
     *  Returns a new {@link Style} with the provided foreground color.
     *
     * @param color The foreground color.
     * @return A new {@link StyleDelegate} with the provided foreground color.
     */
    public StyleDelegate<C> foregroundColor( Color color ) {
        return _withStyle(_style._withForeground(_style.foreground().color(color)));
    }

    /**
     *  Returns a new {@link Style} with the provided foreground color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *
     * @param colorString The foreground color.
     * @return A new {@link StyleDelegate} with the provided foreground color.
     */
    public StyleDelegate<C> foregroundColor( String colorString ) {
        return _withStyle(_style._withForeground(_style.foreground().color(StyleUtility.toColor(colorString))));
    }

    /**
     *  Returns a new {@link Style} with the provided foreground painter, a {@link Painter} that
     *  will be called using the {@link Graphics2D} instance used to render the component.
     *  You may use this to render a custom foreground for the component.
     * @param painter The foreground renderer.
     * @return A new {@link StyleDelegate} with the provided foreground renderer.
     */
    public StyleDelegate<C> foregroundPainter( Painter painter ) {
        return _withStyle(_style.painter(StyleUtility.DEFAULT_KEY, Layer.FOREGROUND, painter));
    }

    /**
     *  Returns a new {@link Style} with the provided named foreground painter, a {@link Painter} that
     *  will be called using the {@link Graphics2D} instance used to render the component.
     *  You may use this to render a custom foreground for the component.
     *  The name can be used to override {@link Painter} instances with that same name
     *  or use a unique name to ensure that you style is not overridden by another style.
     *
     * @param painterName The name of the painter.
     * @param painter The foreground renderer.
     * @return A new {@link StyleDelegate} with the provided foreground renderer.
     */
    public StyleDelegate<C> foregroundPainter( String painterName, Painter painter ) {
        return _withStyle(_style.painter(painterName, Layer.FOREGROUND, painter));
    }

    /**
     *  Returns a new {@link Style} with the provided horizontal shadow offset.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call {@link #shadowSpreadRadius(int)},
     *  {@link #shadowBlurRadius(int)} and {@link #shadowColor(Color)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Function)}.
     *
     * @param offset The shadow offset in pixels.
     * @return A new {@link StyleDelegate} with the provided horizontal shadow offset.
     */
    public StyleDelegate<C> shadowHorizontalOffset( int offset ) {
        return _withStyle(_style._withShadow( shadow -> shadow.horizontalOffset(offset)));
    }

    /**
     *  Returns a new {@link Style} with the provided vertical shadow offset.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call {@link #shadowSpreadRadius(int)},
     *  {@link #shadowBlurRadius(int)} and {@link #shadowColor(Color)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Function)}.
     *
     * @param offset The shadow offset in pixels.
     * @return A new {@link StyleDelegate} with the provided vertical shadow offset.
     */
    public StyleDelegate<C> shadowVerticalOffset( int offset ) {
        return _withStyle(_style._withShadow( shadow -> shadow.verticalOffset(offset)));
    }

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
     * @return A new {@link StyleDelegate} with the provided shadow offset.
     */
    public StyleDelegate<C> shadowOffset( int horizontalOffset, int verticalOffset ) {
        return _withStyle(_style._withShadow( shadow -> shadow.horizontalOffset(horizontalOffset).verticalOffset(verticalOffset)));
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
     * @return A new {@link StyleDelegate} with the provided shadow offset.
     */
    public StyleDelegate<C> shadowOffset( int horizontalAndVerticalOffset ) {
        return _withStyle(_style._withShadow( shadow -> shadow.horizontalOffset(horizontalAndVerticalOffset).verticalOffset(horizontalAndVerticalOffset)));
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
     * @return A new {@link StyleDelegate} with the provided shadow blur radius.
     */
    public StyleDelegate<C> shadowBlurRadius( int radius ) {
        return _withStyle(_style._withShadow( shadow -> shadow.blurRadius(radius)));
    }

    /**
     *  Returns a new {@link Style} with the provided shadow spread radius.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call
     *  {@link #shadowBlurRadius(int)} and {@link #shadowColor(Color)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Function)}.
     *
     * @param radius The shadow spread radius in pixels.
     * @return A new {@link StyleDelegate} with the provided shadow spread radius.
     */
    public StyleDelegate<C> shadowSpreadRadius( int radius ) {
        return _withStyle(_style._withShadow( shadow -> shadow.spreadRadius(radius)));
    }

    /**
     *  Returns a new {@link Style} with the provided shadow color.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link Style}.
     *  Note that in order to see the shadow, you may also need to call
     *  {@link #shadowBlurRadius(int)} and {@link #shadowSpreadRadius(int)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Function)}.
     *
     * @param color The shadow color.
     * @return A new {@link StyleDelegate} with the provided shadow color.
     */
    public StyleDelegate<C> shadowColor( Color color ) {
        return _withStyle(_style._withShadow( shadow -> shadow.color(color)));
    }

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
     * @return A new {@link StyleDelegate} with the provided shadow color.
     */
    public StyleDelegate<C> shadowColor( String colorString ) {
        return _withStyle(_style._withShadow( shadow -> shadow.color(StyleUtility.toColor(colorString))));
    }

    /**
     *  Use this to control whether the shadow should be rendered inwards or outwards. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Function)}. <br>
     *  (see {@link #shadow(String, Function)} for an example of how to use named shadows)
     *
     * @param inwards Whether the shadow should be rendered inwards or outwards.
     * @return A new {@link StyleDelegate} with the provided shadow inset flag.
     */
    public StyleDelegate<C> shadowIsInset( boolean inwards ) {
        return _withStyle(_style._withShadow( shadow -> shadow.isInset(inwards)));
    }

    /**
     *  Use this to configure on which layer the shadow should be rendered. <br>
     *  The default layer is {@link Layer#CONTENT}. <br>
     * @param layer The layer on which the shadow should be rendered.
     * @return A new {@link StyleDelegate} with the provided shadow layer.
     */
    public StyleDelegate<C> shadowLayer( Layer layer ) {
        return _withStyle(_style._withShadow( shadow -> shadow.layer(layer)));
    }

    /**
     *  This method makes it possible to define multiple shadows for a single component
     *  through a unique name.
     *  This is useful when you want to do advanced shadow effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *      UI.panel()
     *      .withStyle( it ->
     *          it
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
     * @return A new {@link StyleDelegate} with a named shadow defined by the provided styler lambda.
     */
    public StyleDelegate<C> shadow( String shadowName, Function<ShadowStyle, ShadowStyle> styler ) {
        Objects.requireNonNull(shadowName);
        Objects.requireNonNull(styler);
        ShadowStyle shadow = Optional.ofNullable(_style.shadow(shadowName)).orElse(ShadowStyle.none());
        // We clone the shadow map:
        Map<String, ShadowStyle> newShadows = new HashMap<>(_style.shadowsMap());
        newShadows.put(shadowName, styler.apply(shadow));
        return _withStyle(_style._withShadow(newShadows));
    }

    /**
     *  This method makes it possible to define multiple background shades for a single component
     *  through a unique name for said shading effect.
     *  This is useful when you want to do advanced background effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it ->
     *      it
     *      .shade("dark shading", shade -> shade.colors("#000000", "#000000").strategy(ShadeStrategy.TOP_TO_BOTTOM)))
     *      .shade("light shading", shade -> shade.colors("#ffffff", "#ffffff").strategy(ShadeStrategy.TOP_TO_BOTTOM)))
     *    )
     * }</pre>
     * Note that the background shades will be rendered in alphabetical order based on the name of the shade.
     *
     * @param shadeName The name of the background shade.
     * @param styler A function that takes a {@link ShadeStyle} and returns a new {@link ShadeStyle}.
     * @return A new {@link StyleDelegate} with a named background shade defined by the provided styler lambda.
     */
    public StyleDelegate<C> shade( String shadeName, Function<ShadeStyle, ShadeStyle> styler ) {
        Objects.requireNonNull(shadeName);
        Objects.requireNonNull(styler);
        return _withStyle(_style.shade(shadeName, styler));
    }

    /**
     *  This method makes it possible to define a background shade for your components.
     *  This is useful when you want to do advanced background effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it ->
     *      it.backgroundShade(shade -> shade.colors("#000000", "#000000").strategy(ShadeStrategy.TOP_TO_BOTTOM)))
     *    )
     * }</pre>
     *
     * @param styler A function that takes a {@link ShadeStyle} and returns a new {@link ShadeStyle}.
     * @return A new {@link StyleDelegate} with a background shade defined by the provided styler lambda.
     */
    public StyleDelegate<C> shade(Function<ShadeStyle, ShadeStyle> styler ) {
        Objects.requireNonNull(styler);
        return _withStyle(_style.shade(StyleUtility.DEFAULT_KEY, styler));
    }

    /**
     *  Returns a new {@link Style} with the provided font name and size.
     *  Note that the font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param name The font name.
     * @param size The font size.
     * @return A new {@link StyleDelegate} with the provided font name and size.
     */
    public StyleDelegate<C> font( String name, int size ) {
        return _withStyle(_style._withFont(_style.font().name(name).size(size)));
    }

    /**
     *  Returns a new {@link Style} with the provided font name.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param name The font name.
     * @return A new {@link StyleDelegate} with the provided font name.
     */
    public StyleDelegate<C> fontName( String name ) {
        return _withStyle(_style._withFont(_style.font().name(name)));
    }

    /**
     *  Returns a new {@link Style} with the provided {@link Font}.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param font The {@link Font}.
     * @return A new {@link StyleDelegate} with the provided {@link Font}.
     */
    public StyleDelegate<C> font( Font font ) {
        return _withStyle(_style._withFont(_style.font().font(font)));
    }

    /**
     *  Returns a new {@link Style} with the provided font size.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param size The font size.
     * @return A new {@link StyleDelegate} with the provided font size.
     */
    public StyleDelegate<C> fontSize( int size ) {
        return _withStyle(_style._withFont(_style.font().size(size)));
    }

    /**
     *  Makes the font bold or not bold depending on the value of the {@code isBold} parameter.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param bold Whether the font should be bold or not.
     * @return A new {@link StyleDelegate} with the provided font boldness.
     */
    public StyleDelegate<C> fontBold( boolean bold ) {
        return _withStyle(_style._withFont(_style.font().style( bold ? Font.BOLD : Font.PLAIN )));
    }

    /**
     *  Makes the font italic or not italic depending on the value of the {@code italic} parameter.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param italic Whether the font should be italic or not.
     * @return A new {@link StyleDelegate} with the provided font italicness.
     */
    public StyleDelegate<C> fontItalic( boolean italic ) {
        return _withStyle(_style._withFont(_style.font().style( italic ? Font.ITALIC : Font.PLAIN )));
    }

    /**
     *  Makes the font underlined or not underlined depending on the value of the {@code underline} parameter.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param underline Whether the font should be underlined or not.
     * @return A new {@link StyleDelegate} with the provided font underlinedness.
     */
    public StyleDelegate<C> fontUnderline( boolean underline ) {
        List<TextAttribute> attributes = new ArrayList<>(_style.font().attributes());
        if ( underline ) attributes.add(TextAttribute.UNDERLINE);
        else     attributes.remove(TextAttribute.UNDERLINE);
        return _withStyle(_style._withFont(_style.font().attributes(attributes)));
    }

    /**
     *  Makes the font struck through or not struck through depending on the value of the {@code strikeThrough} parameter.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param strikeThrough Whether the font should be struck through or not.
     * @return A new {@link StyleDelegate} with the provided font struck throughness.
     */
    public StyleDelegate<C> fontStrikeThrough( boolean strikeThrough ) {
        List<TextAttribute> attributes = new ArrayList<>(_style.font().attributes());
        if ( strikeThrough ) attributes.add(TextAttribute.STRIKETHROUGH);
        else     attributes.remove(TextAttribute.STRIKETHROUGH);
        return _withStyle(_style._withFont(_style.font().attributes(attributes)));
    }

    /**
     *  Creates a new {@link Style} where the font color is set to the provided {@link Color}.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param color The {@link Color}.
     * @return A new {@link StyleDelegate} with the provided font color.
     */
    public StyleDelegate<C> fontColor( Color color ) {
        return _withStyle(_style._withFont(_style.font().color(color)));
    }

    /**
     *  Creates a new {@link Style} where the font color is set to a color parsed from the provided string.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param colorString The {@link Color} as a string.
     * @return A new {@link StyleDelegate} with the provided font color.
     */
    public StyleDelegate<C> fontColor( String colorString ) {
        return _withStyle(_style._withFont(_style.font().color(StyleUtility.toColor(colorString))));
    }

    /**
     *  Creates a new {@link Style} where the font background color is set to the provided {@link Color}.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param color The {@link Color}.
     * @return A new {@link StyleDelegate} with the provided font background color.
     */
    public StyleDelegate<C> fontBackgroundColor( Color color ) {
        return _withStyle(_style._withFont(_style.font().backgroundColor(color)));
    }

    /**
     *  Creates a new {@link Style} where the font color is set to a color parsed from the provided string.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param colorString The {@link Color} as a string.
     * @return A new {@link StyleDelegate} with the provided font color.
     */
    public StyleDelegate<C> fontBackgroundColor( String colorString ) {
        return _withStyle(_style._withFont(_style.font().backgroundColor(StyleUtility.toColor(colorString))));
    }

    /**
     *  Creates a new {@link Style} where the font selection color is set to the provided {@link Color}.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param color The {@link Color}.
     * @return A new {@link StyleDelegate} with the provided font selection color.
     */
    public StyleDelegate<C> fontSelectionColor( Color color ) {
        return _withStyle(_style._withFont(_style.font().selectionColor(color)));
    }

    /**
     *  Creates a new {@link Style} where the font selection color is set to a color parsed from the provided string.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param colorString The {@link Color} as a string.
     * @return A new {@link StyleDelegate} with the provided font selection color.
     */
    public StyleDelegate<C> fontSelectionColor( String colorString ) {
        return _withStyle(_style._withFont(_style.font().selectionColor(StyleUtility.toColor(colorString))));
    }

    /**
     *  Use this to define the weight of the default font of the component.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     * @param weight The weight of the font.
     * @return A new {@link StyleDelegate} with the provided font weight.
     */
    public StyleDelegate<C> fontWeight( int weight ) { 
        return _withStyle(_style._withFont(_style.font().weight(weight))); 
    }

    /**
     *  Use this to define an array of {@link TextAttribute}s to be applied to the default font of the component.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     *  @param attributes The {@link TextAttribute}s to apply to the font.
     *  @return A new {@link StyleDelegate} with the provided font attributes.
     */
    public StyleDelegate<C> fontAttributes( TextAttribute... attributes ) { 
        return _withStyle(_style._withFont(_style.font().attributes(Objects.requireNonNull(attributes)))); 
    }

    /**
     *  Defines the minimum {@link Dimension} of this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMinimumSize(Dimension)} on the underlying component. <br>
     * @param minSize The minimum {@link Dimension}.
     * @return A new {@link StyleDelegate} with the provided minimum {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public StyleDelegate<C> minSize( Dimension minSize ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withMaxWidth(minSize.width)._withMaxHeight(minSize.height)));
    }

    /**
     *  Defines the minimum {@link Dimension} for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMinimumSize(Dimension)} on the underlying component,
     *  which will be called when all the other styles are applied and rendered. <br>
     * @param width The minimum width.
     * @param height The minimum height.
     * @return A new {@link StyleDelegate} with the provided minimum {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public StyleDelegate<C> minSize( int width, int height ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withMaxWidth(width)._withMaxHeight(height)));
    }

    /**
     *  Defines the minimum width for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMinimumSize(Dimension)} on the underlying component,
     *  which will be called when all the other styles are applied and rendered. <br>
     * @param minWidth The minimum width.
     * @return A new {@link StyleDelegate} with the provided minimum width set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public StyleDelegate<C> minWidth( int minWidth ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withMaxWidth(minWidth)));
    }

    /**
     *  Defines the minimum height for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMinimumSize(Dimension)} on the underlying component,
     *  which will be called when all the other styles are applied and rendered. <br>
     * @param minHeight The minimum height.
     * @return A new {@link StyleDelegate} with the provided minimum height set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public StyleDelegate<C> minHeight( int minHeight ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withMaxHeight(minHeight)));
    }

    /**
     *  Defines the maximum {@link Dimension} for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     *  The passed {@link Dimension} will be applied when all the other styles are applied and rendered. <br>
     *
     * @param maxSize The maximum {@link Dimension}.
     * @return A new {@link StyleDelegate} with the provided maximum {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public StyleDelegate<C> maxSize( Dimension maxSize ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withMaxWidth(maxSize.width)._withMaxHeight(maxSize.height)));
    }

    /**
     *  Defines the maximum {@link Dimension} for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     *  The passed {@link Dimension} will be applied when all the other styles are applied and rendered. <br>
     *
     * @param width The maximum width.
     * @param height The maximum height.
     * @return A new {@link StyleDelegate} with the provided maximum {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public StyleDelegate<C> maxSize( int width, int height ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withMaxWidth(width)._withMaxHeight(height)));
    }

    /**
     *  Defines the maximum width for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     *  The passed width will be applied when all the other styles are applied and rendered. <br>
     *
     * @param maxWidth The maximum width.
     * @return A new {@link StyleDelegate} with the provided maximum width set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public StyleDelegate<C> maxWidth( int maxWidth ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withMaxWidth(maxWidth)));
    }

    /**
     *  Defines the maximum height for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     *  The passed height will be applied when all the other styles are applied and rendered. <br>
     *
     * @param maxHeight The maximum height.
     * @return A new {@link StyleDelegate} with the provided maximum height set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public StyleDelegate<C> maxHeight( int maxHeight ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withMaxHeight(maxHeight)));
    }

    /**
     *  Defines the preferred {@link Dimension} for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     *  The passed {@link Dimension} will be applied when all the other styles are applied and rendered. <br>
     *
     * @param preferredSize The preferred {@link Dimension}.
     * @return A new {@link StyleDelegate} with the provided preferred {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public StyleDelegate<C> prefSize( Dimension preferredSize ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withPreferredWidth(preferredSize.width)._withPreferredHeight(preferredSize.height)));
    }

    /**
     *  Defines the preferred {@link Dimension} for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     *  The passed {@link Dimension} will be applied when all the other styles are applied and rendered. <br>
     *
     * @param width The preferred width.
     * @param height The preferred height.
     * @return A new {@link StyleDelegate} with the provided preferred {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public StyleDelegate<C> prefSize( int width, int height ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withPreferredWidth(width)._withPreferredHeight(height)));
    }

    /**
     *  Defines the preferred width for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     *  The passed width will be applied when all the other styles are applied and rendered. <br>
     *
     * @param preferredWidth The preferred width.
     * @return A new {@link StyleDelegate} with the provided preferred width set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public StyleDelegate<C> prefWidth( int preferredWidth ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withPreferredWidth(preferredWidth)));
    }

    /**
     *  Defines the preferred height for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     *  The passed height will be applied when all the other styles are applied and rendered. <br>
     *
     * @param preferredHeight The preferred height.
     * @return A new {@link StyleDelegate} with the provided preferred height set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public StyleDelegate<C> prefHeight( int preferredHeight ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withPreferredHeight(preferredHeight)));
    }

    /**
     *  Defines the size of this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param size The width and height size {@link Dimension}.
     * @return A new {@link StyleDelegate} with the provided size (width and height) {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public StyleDelegate<C> size( Dimension size ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withWidth(size.width)._withHeight(size.height)));
    }

    /**
     *  Defines the size of this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param width The width.
     * @param height The height.
     * @return A new {@link StyleDelegate} with the provided size (width and height) {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public StyleDelegate<C> size( int width, int height ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withWidth(width)._withHeight(height)));
    }


    /**
     *  Defines the width of this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param width The width.
     * @return A new {@link StyleDelegate} with the provided width set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public StyleDelegate<C> width( int width ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withWidth(width)));
    }

    /**
     *  Defines the height of this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param height The height.
     * @return A new {@link StyleDelegate} with the provided height set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public StyleDelegate<C> height( int height ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withHeight(height)));
    }

}

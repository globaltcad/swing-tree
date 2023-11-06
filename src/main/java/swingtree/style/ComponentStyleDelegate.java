package swingtree.style;

import org.slf4j.Logger;
import swingtree.UI;
import swingtree.api.IconDeclaration;
import swingtree.api.Painter;
import swingtree.api.Peeker;
import swingtree.api.Styler;
import swingtree.layout.LayoutConstraint;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *  A {@link ComponentStyleDelegate} is a delegate for a {@link JComponent} and its {@link Style} configuration
 *  used to apply further specify the style of said {@link JComponent}.
 *  Instances of this will be exposed to you via the {@link swingtree.UIForAnySwing#withStyle(Styler)}
 *  method, where you can specify a lambda that takes a {@link ComponentStyleDelegate} and returns a
 *  transformed {@link Style} object, as well as inside of {@link StyleSheet} extensions
 *  where you can declare similar styling lambdas for {@link StyleTrait}s, which are
 *  styling rules... <br>
 *
 * @param <C> The type of {@link JComponent} this {@link ComponentStyleDelegate} is for.
 */
public final class ComponentStyleDelegate<C extends JComponent>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ComponentStyleDelegate.class);

    private final C _component;
    private final Style _style;


    public ComponentStyleDelegate( C component, Style style ) {
        _component = Objects.requireNonNull(component);
        _style     = Objects.requireNonNull(style);
    }

    ComponentStyleDelegate<C> _withStyle( Style style ) { return new ComponentStyleDelegate<>(_component, style); }

    /**
     *  Returns the {@link JComponent} this {@link ComponentStyleDelegate} is defining a {@link Style} for.
     *  This is useful if you want to make the styling of a component based on its state,
     *  like for example determining the background color of a {@link JCheckBox} based on
     *  whether it is selected or not...
     * <p>
     * @return The {@link JComponent} this {@link ComponentStyleDelegate} is for.
     */
    public C component() { return _component; }

    /**
     * @return An optional parent {@link Container} of the {@link JComponent} this {@link ComponentStyleDelegate} is for.
     */
    public Optional<Container> parent() { return Optional.ofNullable(_component.getParent()); }

    /**
     *  Use this to peek at the {@link JComponent} of this {@link ComponentStyleDelegate}
     *  to perform some style-related component specific actions on it
     *  which are otherwise not found in the {@link ComponentStyleDelegate} API.
     *
     * @param peeker A {@link Peeker} that takes the {@link JComponent} of this {@link ComponentStyleDelegate}
     * @return This {@link ComponentStyleDelegate} instance.
     */
    public ComponentStyleDelegate<C> peek( Peeker<C> peeker ) {
        try {
            peeker.accept(_component);
        } catch( Exception e ) {
            log.error("Peeker threw an exception: " + e.getMessage(), e);
            // We don't want to crash the application if the peeker throws an exception.
        }
        return this;
    }

    /**
     *   Allows you to apply styles based on a condition.
     *   So if the first argument, the condition, is true,
     *   then it causes the supplied {@link Styler} to
     *   update the style, if however the condition is false,
     *   then the styler will simply be ignored
     *   and the style will not be updated.
     *   <br>
     *   Here a simple usage example:
     *   <pre>{@code
     *       UI.panel().withStyle( it -> it
     *          .border(3, Color.BLACK)
     *          .borderRadius(24)
     *          .applyIf(it.component().isEnabled(), it2 -> it2
     *              .borderColor(Color.LIGHT_GRAY)
     *              .backgroundColor(Color.CYAN)
     *          )
     *          .margin(3)
     *          .padding(4)
     *       );
     *   }</pre>
     *   This is conceptually similar to {@link swingtree.UIForAnySwing#applyIf(boolean, Consumer)}
     *   with the difference that it is based on a {@link Styler} instead of a consumer,
     *   as the style API is based on immutable types whose updated results must be returned
     *   by the conditional scope.
     *
     * @param condition The condition determining if the provided {@code styler} should be executed.
     * @param styler A supplier for
     * @return This instance if the condition is false, or the supplied {@code styler} threw an exception,
     *         a new style delegate updated according to the {@code styler}.
     */
    public ComponentStyleDelegate<C> applyIf(
        boolean condition,
        Styler<C> styler
    ) {
        if ( !condition )
            return this;

        try {
            return styler.style(this);
        } catch( Exception e ) {
            log.error("Conditional styler threw an exception: " + e.getMessage(), e);
            // We don't want to crash the application if the conditional styler throws an exception.
        }
        return this;
    }

    /**
     *  Returns the {@link Style} this {@link ComponentStyleDelegate} is defining for the {@link JComponent}
     *  returned by {@link #component()}.
     * <p>
     * @return The {@link Style} this {@link ComponentStyleDelegate} is for.
     */
    Style style() { return _style; }

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
     * @return A new {@link ComponentStyleDelegate} with the provided padding distances.
     */
    public ComponentStyleDelegate<C> margin( int top, int right, int bottom, int left ) {
        return _withStyle(_style._withBorder(_style.border().margin(Outline.of(top, right, bottom, left))));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for all sides of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided margin distance.
     */
    public ComponentStyleDelegate<C> margin( int margin ) {
        return _withStyle(_style._withBorder(_style.border().margin(Outline.of(margin))));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the top side of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided margin distance.
     */
    public ComponentStyleDelegate<C> marginTop( int margin ) {
        return _withStyle(_style._withBorder(_style.border().margin(_style.border().margin().top(margin))));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the right side of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided margin distance.
     */
    public ComponentStyleDelegate<C> marginRight( int margin ) {
        return _withStyle(_style._withBorder(_style.border().margin(_style.border().margin().right(margin))));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the bottom side of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided margin distance.
     */
    public ComponentStyleDelegate<C> marginBottom( int margin ) {
        return _withStyle(_style._withBorder(_style.border().margin(_style.border().margin().bottom(margin))));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the left side of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided margin distance.
     */
    public ComponentStyleDelegate<C> marginLeft( int margin ) {
        return _withStyle(_style._withBorder(_style.border().margin(_style.border().margin().left(margin))));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the top and bottom sides of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided margin distance.
     */
    public ComponentStyleDelegate<C> marginVertical( int margin ) {
        return _withStyle(_style._withBorder(_style.border().margin(_style.border().margin().top(margin).bottom(margin))));
    }

    /**
     *  Creates a new {@link Style} with the provided margin distance for the left and right sides of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param margin The margin distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided margin distance.
     */
    public ComponentStyleDelegate<C> marginHorizontal( int margin ) {
        return _withStyle(_style._withBorder(_style.border().margin(_style.border().margin().left(margin).right(margin))));
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
     * @return A new {@link ComponentStyleDelegate} with the provided padding distances.
     */
    public ComponentStyleDelegate<C> padding( int top, int right, int bottom, int left ) {
        return _withStyle(_style._withBorder(_style.border().padding(Outline.of(top, right, bottom, left))));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for all sides of the component.
     *  It determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided padding distance.
     */
    public ComponentStyleDelegate<C> padding( int padding ) {
        return _withStyle(_style._withBorder(_style.border().padding(Outline.of(padding))));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the top side of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided padding distance.
     */
    public ComponentStyleDelegate<C> paddingTop( int padding ) {
        return _withStyle(_style._withBorder(_style.border().padding(_style.border().padding().top(padding))));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the right side of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided padding distance.
     */
    public ComponentStyleDelegate<C> paddingRight( int padding ) {
        return _withStyle(_style._withBorder(_style.border().padding(_style.border().padding().right(padding))));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the bottom side of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided padding distance.
     */
    public ComponentStyleDelegate<C> paddingBottom( int padding ) {
        return _withStyle(_style._withBorder(_style.border().padding(_style.border().padding().bottom(padding))));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the left side of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided padding distance.
     */
    public ComponentStyleDelegate<C> paddingLeft( int padding ) {
        return _withStyle(_style._withBorder(_style.border().padding(_style.border().padding().left(padding))));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the top and bottom sides of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided padding distance.
     */
    public ComponentStyleDelegate<C> paddingVertical( int padding ) {
        return _withStyle(_style._withBorder(_style.border().padding(_style.border().padding().top(padding).bottom(padding))));
    }

    /**
     *  Creates a new {@link Style} with the provided padding distance for the left and right sides of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(int)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * <p>
     * @param padding The padding distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided padding distance.
     */
    public ComponentStyleDelegate<C> paddingHorizontal( int padding ) {
        return _withStyle(_style._withBorder(_style.border().padding(_style.border().padding().left(padding).right(padding))));
    }

    /**
     *  Returns a new {@link Style} with the provided border width and border color.
     *  The border will be rendered with an inset space based on the margin defined by the {@link Style}.
     *
     * @param width The border width in pixels.
     * @param color The border color.
     * @return A new {@link ComponentStyleDelegate} with the provided border width and border color.
     */
    public ComponentStyleDelegate<C> border( int width, Color color ) {
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
     * @return A new {@link ComponentStyleDelegate} with the provided border widths and border color.
     */
    public ComponentStyleDelegate<C> border( int top, int right, int bottom, int left, Color color ) {
        return _withStyle(_style._withBorder(_style.border().widths(Outline.of(top, right, bottom, left)).color(color)));
    }

    /**
     *  Returns a new {@link Style} with the provided border width and border color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param width The border width in pixels.
     * @param colorString The border color.
     * @return A new {@link ComponentStyleDelegate} with the provided border width and border color.
     */
    public ComponentStyleDelegate<C> border( int width, String colorString ) {
        return _withStyle(_style._withBorder(_style.border().width(width).color(StyleUtility.toColor(colorString))));
    }

    /**
     *  Returns a new {@link Style} with the provided border width.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param width The border width in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided border width.
     */
    public ComponentStyleDelegate<C> borderWidth( int width ) {
        return _withStyle(_style._withBorder(_style.border().width(width)));
    }

    /**
     *  Returns a new {@link Style} with the provided border width for the specified edge.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param edge The edge to set the border width for.
     * @param width The border width in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided border width for the specified edge.
     */
    public ComponentStyleDelegate<C> borderWidthAt( UI.Edge edge, int width ) {
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
     * @return A new {@link ComponentStyleDelegate} with the provided top, right, bottom and left border widths.
     * @see #borderWidth(int)
     * @see #borderWidthAt(UI.Edge, int)
     */
    public ComponentStyleDelegate<C> borderWidths( int top, int right, int bottom, int left ) {
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
     * @return A new {@link ComponentStyleDelegate} with the provided top/bottom and left/right border widths.
     * @see #borderWidth(int)
     * @see #borderWidthAt(UI.Edge, int)
     */
    public ComponentStyleDelegate<C> borderWidths( int topBottom, int leftRight ) {
        return _withStyle(_style._withBorder(_style.border().widths(Outline.of(topBottom, leftRight, topBottom, leftRight))));
    }

    /**
     *  Returns a new {@link Style} with the provided border color.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param color The border color.
     * @return A new {@link ComponentStyleDelegate} with the provided border color.
     */
    public ComponentStyleDelegate<C> borderColor( Color color ) {
        return _withStyle(_style._withBorder(_style.border().color(color)));
    }

    /**
     *  Returns a new {@link Style} with the provided border color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The border will be rendered with an inset space based on the padding defined by the {@link Style}.
     *
     * @param colorString The border color.
     * @return A new {@link ComponentStyleDelegate} with the provided border color.
     */
    public ComponentStyleDelegate<C> borderColor( String colorString ) {
        return _withStyle(_style._withBorder(_style.border().color(StyleUtility.toColor(colorString))));
    }

    /**
     *  Returns a new {@link Style} with the provided border radius.
     *  This will override both the arc width and arc height of the border.
     *  The border will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param radius The border radius in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided border radius.
     */
    public ComponentStyleDelegate<C> borderRadius( int radius ) {
        return _withStyle(_style._withBorder(_style.border().arcWidth(radius).arcHeight(radius)));
    }

    /**
     *  Returns a new {@link Style} with the provided border arc width and arc height.
     *  Note that the border will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param arcWidth The border arc width in pixels.
     * @param arcHeight The border arc height in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided border arc width and arc height.
     */
    public ComponentStyleDelegate<C> borderRadius( int arcWidth, int arcHeight ) {
        return _withStyle(_style._withBorder(_style.border().arcWidth(arcWidth).arcHeight(arcHeight)));
    }

    /**
     *  Returns a new {@link Style} with the provided border arc width and arc height for the specified corner.
     *  Note that the border will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param corner The corner to apply the border radius to.
     * @param arcWidth The border arc width in pixels.
     * @param arcHeight The border arc height in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided border arc width and arc height for the specified corner.
     */
    public ComponentStyleDelegate<C> borderRadiusAt( UI.Corner corner, int arcWidth, int arcHeight ) {
        return _withStyle(_style._withBorder(_style.border().arcWidthAt(corner, arcWidth).arcHeightAt(corner, arcHeight)));
    }

    /**
     *  Returns a new {@link Style} with the provided border radius for the specified corner.
     *  This will override both the arc width and arc height of the border.
     *  Note that the border will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param corner The corner to apply the border radius to.
     * @param radius The border radius in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided border radius for the specified corner.
     */
    public ComponentStyleDelegate<C> borderRadiusAt( UI.Corner corner, int radius ) {
        return this.borderRadiusAt(corner, radius, radius);
    }

    /**
     *  This method makes it possible to define border shades for the border of your UI components.
     *  This is useful when you want to do advanced border effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it -> it
     *      .borderGradient( grad -> grad
     *        .colors("#000000", "#000000")
     *        .align(GradientAlignment.TOP_TO_BOTTOM)
     *      )
     *    )
     * }</pre>
     *
     * @param styler A function that takes a {@link GradientStyle} and returns a new {@link GradientStyle}.
     * @return A new {@link ComponentStyleDelegate} with a border shade defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> borderGradient( Function<GradientStyle, GradientStyle> styler ) {
        Objects.requireNonNull(styler);
        return _withStyle(_style._withBorder(_style.border().gradient(StyleUtility.DEFAULT_KEY, styler)));
    }

    /**
     *  This method makes it possible to define multiple border shades through a unique name for said shading effect.
     *  This is useful when you want to do advanced border effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it -> it
     *      .borderGradient("dark shading", grad -> grad
     *        .colors("#000000", "#000000")
     *        .transition(UI.Transition.TOP_TO_BOTTOM)
     *      )
     *      .borderGradient("light shading", grad -> grad
     *        .colors("#ffffff", "#ffffff")
     *        .transition(UI.Transition.TOP_TO_BOTTOM)
     *      )
     *    )
     * }</pre>
     * Note that the border shades will be rendered in alphabetical order based on the name of the shade.
     *
     * @param shadeName The name of the border shade.
     * @param styler A function that takes a {@link GradientStyle} and returns a new {@link GradientStyle}.
     * @return A new {@link ComponentStyleDelegate} with a named border shade defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> borderGradient( String shadeName, Function<GradientStyle, GradientStyle> styler ) {
        Objects.requireNonNull(shadeName);
        Objects.requireNonNull(styler);
        return _withStyle(_style._withBorder(_style.border().gradient(shadeName, styler)));
    }

    /**
     *  Returns a new {@link Style} with the provided {@link ImageIcon} as the icon
     *  for the current component (see {@link #component()}).
     *  Note that this will only produce a result for components that actually support icons.
     *  Like for example all the various {@link AbstractButton} subclasses, {@link JLabel}
     *  and {@link swingtree.components.JIcon}.
     *
     * @param icon The icon.
     * @return A new {@link ComponentStyleDelegate} with the provided icon.
     */
    public ComponentStyleDelegate<C> icon( ImageIcon icon ) {
        return _withStyle(_style._withBase(_style.base().icon(icon)));
    }

    /**
     *  Returns a new {@link Style} with the provided {@link ImageIcon} as the icon
     *  for the current component (see {@link #component()}) and the provided fit mode
     *  determining how the icon should be fitted to the component.
     *  Note that this will only work for components that support icons.
     *  Like for example all the various {@link AbstractButton} subclasses, {@link JLabel}
     *  and {@link swingtree.components.JIcon}.
     *
     * @param icon The icon in the form of an {@link ImageIcon}.
     * @param fit The fit mode for the icon (mostly intended for {@link SvgIcon}).
     * @return A new {@link ComponentStyleDelegate} with the provided icon.
     */
    public ComponentStyleDelegate<C> icon( ImageIcon icon, UI.FitComponent fit ) {
        return _withStyle(_style._withBase(_style.base().icon(icon).fit(fit)));
    }

    /**
     *  Returns a new {@link Style} with the provided {@link IconDeclaration} as the
     *  source for the icon of the current component (see {@link #component()}).
     *  Note that this will only have an effect for components that support icons.
     *  Like for example all the various {@link AbstractButton} subclasses, {@link JLabel}
     *  and {@link swingtree.components.JIcon}.
     *
     * @param icon The icon declaration, which will be resolved to an {@link ImageIcon}.
     * @return A new {@link ComponentStyleDelegate} with the provided icon.
     */
    public ComponentStyleDelegate<C> icon( IconDeclaration icon ) {
        return icon.find().map(this::icon).orElse(this);
    }

    /**
     *  Returns a new {@link Style} with the provided {@link IconDeclaration} as the
     *  source for the icon of the current component (see {@link #component()}) and the provided fit mode
     *  determining how the icon should be fitted to the component.
     *  Note that this will only have an effect for components that support icons.
     *  Like for example all the various {@link AbstractButton} subclasses, {@link JLabel}
     *  and {@link swingtree.components.JIcon}.
     *
     * @param icon The icon declaration, which will be resolved to an {@link ImageIcon}.
     * @param fit The fit mode for the icon (mostly intended for {@link SvgIcon}).
     * @return A new {@link ComponentStyleDelegate} with the provided icon.
     */
    public ComponentStyleDelegate<C> icon( IconDeclaration icon, UI.FitComponent fit ) {
        return icon.find().map(it -> icon(it, fit)).orElse(this);
    }

    /**
     *  Returns a new {@link Style} with the provided background foundation color.
     *  The background color covers the entire component area, including the padding spaces.
     *
     * @param color The background color.
     * @return A new {@link ComponentStyleDelegate} with the provided background color.
     */
    public ComponentStyleDelegate<C> foundationColor( Color color ) {
        return _withStyle(_style._withBase(_style.base().foundationColor(color)));
    }

    /**
     *  Returns a new {@link Style} with the provided background foundation color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The background color covers the entire component area, including the padding spaces.
     *
     * @param colorString The background color.
     * @return A new {@link ComponentStyleDelegate} with the provided background color.
     */
    public ComponentStyleDelegate<C> foundationColor( String colorString ) {
        return _withStyle(_style._withBase(_style.base().foundationColor(StyleUtility.toColor(colorString))));
    }

    /**
     *  Returns a new {@link Style} with the provided inner Background color.
     *  The inner background will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param color The inner background color.
     * @return A new {@link ComponentStyleDelegate} with the provided inner background color.
     */
    public ComponentStyleDelegate<C> backgroundColor( Color color ) {
        return _withStyle(_style._withBase(_style.base().backgroundColor(color)));
    }

    /**
     *  Returns a new {@link Style} with the provided inner Background color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The inner background will be rendered with an inset space based on the padding defined by this {@link Style}.
     *
     * @param colorString The inner background color.
     * @return A new {@link ComponentStyleDelegate} with the provided inner background color.
     */
    public ComponentStyleDelegate<C> backgroundColor( String colorString ) {
        return _withStyle(_style._withBase(_style.base().backgroundColor(StyleUtility.toColor(colorString))));
    }

    /**
     *  Returns a new {@link Style} with the provided custom {@link swingtree.api.Painter}, which
     *  will be called using the {@link Graphics2D} of the current component.
     *  You may use this to render a custom background for the component.
     * @param layer The layer on which the painter should do its work.
     * @param renderer The background renderer.
     * @return A new {@link ComponentStyleDelegate} with the provided background renderer.
     */
    public ComponentStyleDelegate<C> painter( UI.Layer layer, swingtree.api.Painter renderer ) {
        return _withStyle(_style.painter(StyleUtility.DEFAULT_KEY, layer, renderer));
    }

    /**
     *  Returns a new {@link Style} with the provided named {@link swingtree.api.Painter}, which
     *  will be called using the {@link Graphics2D} instance of the current component.
     *  You may use this to render custom styles for the component... <br>
     *  The name can be used to override {@link swingtree.api.Painter} instances with that same name
     *  or use a unique name to ensure that you style is not overridden by another style.
     *  This allows you to attach an arbitrary number of custom painters to a component.
     *
     * @param layer The layer on which the painter should do its work.
     * @param painterName The name of the painter.
     * @param renderer The background renderer.
     * @return A new {@link ComponentStyleDelegate} with the provided background renderer.
     */
    public ComponentStyleDelegate<C> painter( UI.Layer layer, String painterName, swingtree.api.Painter renderer ) {
        return _withStyle(_style.painter(painterName, layer, renderer));
    }

    /**
     *  Returns a new {@link Style} with the provided foreground color.
     *
     * @param color The foreground color.
     * @return A new {@link ComponentStyleDelegate} with the provided foreground color.
     */
    public ComponentStyleDelegate<C> foregroundColor( Color color ) {
        return _withStyle(_style._withBase(_style.base().foregroundColo(color)));
    }

    /**
     *  Returns a new {@link Style} with the provided foreground color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *
     * @param colorString The foreground color.
     * @return A new {@link ComponentStyleDelegate} with the provided foreground color.
     */
    public ComponentStyleDelegate<C> foregroundColor( String colorString ) {
        return _withStyle(_style._withBase(_style.base().foregroundColo(StyleUtility.toColor(colorString))));
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
     * @return A new {@link ComponentStyleDelegate} with the provided horizontal shadow offset.
     */
    public ComponentStyleDelegate<C> shadowHorizontalOffset( int offset ) {
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
     * @return A new {@link ComponentStyleDelegate} with the provided vertical shadow offset.
     */
    public ComponentStyleDelegate<C> shadowVerticalOffset( int offset ) {
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
     * @return A new {@link ComponentStyleDelegate} with the provided shadow offset.
     */
    public ComponentStyleDelegate<C> shadowOffset( int horizontalOffset, int verticalOffset ) {
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
     * @return A new {@link ComponentStyleDelegate} with the provided shadow offset.
     */
    public ComponentStyleDelegate<C> shadowOffset( int horizontalAndVerticalOffset ) {
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
     * @return A new {@link ComponentStyleDelegate} with the provided shadow blur radius.
     */
    public ComponentStyleDelegate<C> shadowBlurRadius( int radius ) {
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
     * @return A new {@link ComponentStyleDelegate} with the provided shadow spread radius.
     */
    public ComponentStyleDelegate<C> shadowSpreadRadius( int radius ) {
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
     * @return A new {@link ComponentStyleDelegate} with the provided shadow color.
     */
    public ComponentStyleDelegate<C> shadowColor( Color color ) {
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
     * @return A new {@link ComponentStyleDelegate} with the provided shadow color.
     */
    public ComponentStyleDelegate<C> shadowColor( String colorString ) {
        return _withStyle(_style._withShadow( shadow -> shadow.color(StyleUtility.toColor(colorString))));
    }

    /**
     *  Use this to control whether the shadow should be rendered inwards or outwards. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Function)}. <br>
     *  (see {@link #shadow(String, Function)} for an example of how to use named shadows)
     *
     * @param inwards Whether the shadow should be rendered inwards or outwards.
     * @return A new {@link ComponentStyleDelegate} with the provided shadow inset flag.
     */
    public ComponentStyleDelegate<C> shadowIsInset( boolean inwards ) {
        return _withStyle(_style._withShadow( shadow -> shadow.isInset(inwards)));
    }

    /**
     *  Use this to configure on which layer the shadow should be rendered. <br>
     *  The default layer is {@link UI.Layer#CONTENT}. <br>
     * @param layer The layer on which the shadow should be rendered.
     * @return A new {@link ComponentStyleDelegate} with the provided shadow layer.
     */
    public ComponentStyleDelegate<C> shadowLayer( UI.Layer layer ) {
        return _withStyle(_style._withShadow( shadow -> shadow.layer(layer)));
    }

    /**
     *  This method makes it possible to define multiple shadows for a single component
     *  through a unique name.
     *  This is useful when you want to do advanced shadow effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *      UI.panel()
     *      .withStyle( it -> it
     *          .shadow("dark shading", shadow -> shadow
     *              .color("#000000")
     *              .horizontalOffset(5)
     *              .verticalOffset(5)
     *              .blurRadius(10)
     *              .spreadRadius(0)
     *          )
     *          .shadow("light shading", shadow -> shadow
     *              .color("#ffffff")
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
     * @return A new {@link ComponentStyleDelegate} with a named shadow defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> shadow( String shadowName, Function<ShadowStyle, ShadowStyle> styler ) {
        Objects.requireNonNull(shadowName);
        Objects.requireNonNull(styler);
        ShadowStyle shadow = Optional.ofNullable(_style.shadow(shadowName)).orElse(ShadowStyle.none());
        // We clone the shadow map:
        NamedStyles<ShadowStyle> newShadows = _style.shadowsMap().withNamedStyle(shadowName, styler.apply(shadow));
        return _withStyle(_style._withShadow(newShadows));
    }

    /**
     *  This method makes it possible to define multiple background shades for a single component
     *  through a unique name for said shading effect.
     *  This is useful when you want to do advanced background effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it -> it
     *      .gradient("dark shading", grad -> grad
     *        .colors("#000000", "#000000")
     *        .transition(UI.Transition.TOP_TO_BOTTOM)
     *      )
     *      .gradient("light shading", grad -> grad
     *        .colors("#ffffff", "#ffffff")
     *        .transition(UI.Transition.TOP_TO_BOTTOM))
     *      )
     *    )
     * }</pre>
     * Note that the background shades will be rendered in alphabetical order based on the name of the shade.
     *
     * @param shadeName The name of the background shade.
     * @param styler A function that takes a {@link GradientStyle} and returns a new {@link GradientStyle}.
     * @return A new {@link ComponentStyleDelegate} with a named background shade defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> gradient( String shadeName, Function<GradientStyle, GradientStyle> styler ) {
        Objects.requireNonNull(shadeName);
        Objects.requireNonNull(styler);
        return _withStyle(_style.gradient(shadeName, styler));
    }

    /**
     *  This method makes it possible to define a background shade for your components.
     *  This is useful when you want to do advanced background effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it -> it
     *        .gradient( grad -> grad
     *            .colors("#000000", "#000000")
     *            .transition(UI.Transition.TOP_TO_BOTTOM)
     *        )
     *    )
     * }</pre>
     *
     * @param styler A function that takes a {@link GradientStyle} and returns a new {@link GradientStyle}.
     * @return A new {@link ComponentStyleDelegate} with a background shade defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> gradient( Function<GradientStyle, GradientStyle> styler ) {
        Objects.requireNonNull(styler);
        return _withStyle(_style.gradient(StyleUtility.DEFAULT_KEY, styler));
    }

    /**
     *  This method makes it possible to define multiple background styles for a single component
     *  through a unique name for said image.
     *  This is useful when you want to do advanced backgrounds
     *  displaying multiple images on top of each other. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it -> it
     *      .image("ground 1", image -> image
     *        .image(loadImageFrom("my/path/to/image1.png"))
     *      )
     *      .ground("ground 2", ground -> ground
     *        .color("blue")
     *      )
     *    )
     * }</pre>
     * Note that the background images will be rendered in alphabetical order based on the name of the image.
     *
     * @param imageName The name of the background image.
     * @param styler A function that takes a {@link ImageStyle} and returns a new {@link ImageStyle}.
     * @return A new {@link ComponentStyleDelegate} with a named background image defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> image( String imageName, Function<ImageStyle, ImageStyle> styler ) {
        Objects.requireNonNull(imageName);
        Objects.requireNonNull(styler);
        return _withStyle(_style.images(imageName, styler));
    }

    /**
     *  This method makes it possible to define a background image for your components.
     *  This is useful when you want to do advanced backgrounds
     *  displaying multiple images on top of each other. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it -> it
     *        .image( image -> image
     *            .image(loadImageFrom("my/path/to/image1.png"))
     *            .color("green")
     *        )
     *    )
     * }</pre>
     *
     * @param styler A function that takes a {@link ImageStyle} and returns a new {@link ImageStyle}.
     * @return A new {@link ComponentStyleDelegate} with a background image defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> image( Function<ImageStyle, ImageStyle> styler ) {
        Objects.requireNonNull(styler);
        return _withStyle(_style.images(StyleUtility.DEFAULT_KEY, styler));
    }

    /**
     *  Allow for the specification of client properties on the styled component.
     *  This is useful when you want to store arbitrary configuration data on the component,
     *  which is usually read and used by look and feel implementations to
     *  apply custom appearance and behavior to the component. <br>
     *  <br>
     *  If you want a particular property to be removed, you can pass and empty String {@code ""} as the value. <br>
     *  <b>A {@code null} reference is not allowed as a value and will throw a {@link NullPointerException}.</b>
     *
     * @param key The key of the property.
     * @param value The value of the property.
     * @return A new {@link ComponentStyleDelegate} with the provided client property.
     * @see JComponent#putClientProperty(Object, Object)
     * @see JComponent#getClientProperty(Object)
     * @throws NullPointerException If the value is {@code null}! (Use {@code ""} to remove a property)
     */
    public ComponentStyleDelegate<C> property( String key, String value ) {
        return _withStyle(_style.property(key, value));
    }

    /**
     *  Returns a new {@link Style} with the provided font name and size.
     *  Note that the font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param name The font name.
     * @param size The font size.
     * @return A new {@link ComponentStyleDelegate} with the provided font name and size.
     */
    public ComponentStyleDelegate<C> font( String name, int size ) {
        return _withStyle(_style._withFont(_style.font().family(name).size(size)));
    }

    /**
     *  Returns a new {@link Style} with the provided font family name.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param name The font name.
     * @return A new {@link ComponentStyleDelegate} with the provided font name.
     */
    public ComponentStyleDelegate<C> fontFamily( String name ) {
        return _withStyle(_style._withFont(_style.font().family(name)));
    }

    /**
     *  Returns a new {@link Style} with the provided {@link Font}.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param font The {@link Font}.
     * @return A new {@link ComponentStyleDelegate} with the provided {@link Font}.
     */
    public ComponentStyleDelegate<C> font( Font font ) {
        return _withStyle(_style._withFont(_style.font().font(font)));
    }

    /**
     *  Returns a new {@link Style} with the provided font size.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param size The font size.
     * @return A new {@link ComponentStyleDelegate} with the provided font size.
     */
    public ComponentStyleDelegate<C> fontSize( int size ) {
        return _withStyle(_style._withFont(_style.font().size(size)));
    }

    /**
     *  Makes the font bold or not bold depending on the value of the {@code isBold} parameter.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param bold Whether the font should be bold or not.
     * @return A new {@link ComponentStyleDelegate} with the provided font boldness.
     */
    public ComponentStyleDelegate<C> fontBold( boolean bold ) {
        return _withStyle(_style._withFont(_style.font().weight( bold ? 2 : 1 )));
    }

    /**
     *  Makes the font italic or not italic depending on the value of the {@code italic} parameter.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param italic Whether the font should be italic or not.
     * @return A new {@link ComponentStyleDelegate} with the provided font italicness.
     */
    public ComponentStyleDelegate<C> fontItalic( boolean italic ) {
        return _withStyle(_style._withFont(_style.font().posture( italic ? 0.2f : 0f )));
    }

    /**
     *  Makes the font underlined or not underlined depending on the value of the {@code underline} parameter.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param underline Whether the font should be underlined or not.
     * @return A new {@link ComponentStyleDelegate} with the provided font underlinedness.
     */
    public ComponentStyleDelegate<C> fontUnderline( boolean underline ) {
        return _withStyle(_style._withFont(_style.font().isUnderlined(underline)));
    }

    /**
     *  Makes the font struck through or not struck through depending on the value of the {@code strikeThrough} parameter.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param strikeThrough Whether the font should be struck through or not.
     * @return A new {@link ComponentStyleDelegate} with the provided font struck throughness.
     */
    public ComponentStyleDelegate<C> fontStrikeThrough( boolean strikeThrough ) {
        return _withStyle(_style._withFont(_style.font().isStrike(strikeThrough)));
    }

    /**
     *  Creates a new {@link Style} where the font color is set to the provided {@link Color}.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param color The {@link Color}.
     * @return A new {@link ComponentStyleDelegate} with the provided font color.
     */
    public ComponentStyleDelegate<C> fontColor( Color color ) {
        return _withStyle(_style._withFont(_style.font().color(color)));
    }

    /**
     *  Creates a new {@link Style} where the font color is set to a color parsed from the provided string.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param colorString The {@link Color} as a string.
     * @return A new {@link ComponentStyleDelegate} with the provided font color.
     */
    public ComponentStyleDelegate<C> fontColor( String colorString ) {
        return _withStyle(_style._withFont(_style.font().color(StyleUtility.toColor(colorString))));
    }

    /**
     *  Creates a new {@link Style} where the font background color is set to the provided {@link Color}.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param color The {@link Color}.
     * @return A new {@link ComponentStyleDelegate} with the provided font background color.
     */
    public ComponentStyleDelegate<C> fontBackgroundColor( Color color ) {
        return _withStyle(_style._withFont(_style.font().backgroundColor(color)));
    }

    /**
     *  Creates a new {@link Style} where the font color is set to a color parsed from the provided string.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param colorString The {@link Color} as a string.
     * @return A new {@link ComponentStyleDelegate} with the provided font color.
     */
    public ComponentStyleDelegate<C> fontBackgroundColor( String colorString ) {
        return _withStyle(_style._withFont(_style.font().backgroundColor(StyleUtility.toColor(colorString))));
    }

    /**
     *  Creates a new {@link Style} where the font selection color is set to the provided {@link Color}.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param color The {@link Color}.
     * @return A new {@link ComponentStyleDelegate} with the provided font selection color.
     */
    public ComponentStyleDelegate<C> fontSelectionColor( Color color ) {
        return _withStyle(_style._withFont(_style.font().selectionColor(color)));
    }

    /**
     *  Creates a new {@link Style} where the font selection color is set to a color parsed from the provided string.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     *
     * @param colorString The {@link Color} as a string.
     * @return A new {@link ComponentStyleDelegate} with the provided font selection color.
     */
    public ComponentStyleDelegate<C> fontSelectionColor( String colorString ) {
        return _withStyle(_style._withFont(_style.font().selectionColor(StyleUtility.toColor(colorString))));
    }

    /**
     * @param transform The {@link AffineTransform} to apply to the font.
     * @return A new {@link ComponentStyleDelegate} with the provided font transform.
     */
    public ComponentStyleDelegate<C> fontTransform( AffineTransform transform ) {
        return _withStyle(_style._withFont(_style.font().transform(transform)));
    }

    /**
     * @param paint The {@link Paint} to use for the foreground of the font, which translates to the
     *              {@link java.awt.font.TextAttribute#FOREGROUND} attribute.
     * @return A new {@link ComponentStyleDelegate} with the provided font paint.
     */
    public ComponentStyleDelegate<C> fontPaint( Paint paint ) {
        return _withStyle(_style._withFont(_style.font().paint(paint)));
    }

    /**
     * @param paint The {@link Paint} to use for the background of the font, which translates to the
     *              {@link java.awt.font.TextAttribute#BACKGROUND} attribute.
     * @return A new {@link ComponentStyleDelegate} with the provided font background paint.
     */
    public ComponentStyleDelegate<C> fontBackgroundPaint( Paint paint ) {
        return _withStyle(_style._withFont(_style.font().backgroundPaint(paint)));
    }


    /**
     *  Use this to define the weight of the default font of the component.
     *  The default value is 1.0 (see {@link java.awt.font.TextAttribute#WEIGHT_REGULAR}),
     *  whereas a bold font typically has a font weight
     *  of 2.0 (see {@link java.awt.font.TextAttribute#WEIGHT_BOLD}).
     *  <p>
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text.
     * @param weight The weight of the font.
     * @return A new {@link ComponentStyleDelegate} with the provided font weight.
     */
    public ComponentStyleDelegate<C> fontWeight( float weight ) {
        return _withStyle(_style._withFont(_style.font().weight(weight))); 
    }

    /**
     * @param spacing The spacing of the default font of the component, which translates to the
     *                {@link java.awt.font.TextAttribute#TRACKING} attribute.
     * @return A new {@link ComponentStyleDelegate} with the provided font spacing.
     */
    public ComponentStyleDelegate<C> fontSpacing( float spacing ) {
        return _withStyle(_style._withFont(_style.font().spacing(spacing)));
    }

    /**
     *  Use this to define the horizontal alignment of the default font of the component.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text. <br>
     *  Also note that not all text based components support text alignment.
     *  @param alignment The horizontal alignment of the font.
     *                   See {@link UI.HorizontalAlignment} for more information.
     *  @return A new {@link ComponentStyleDelegate} with the provided font alignment.
     */
    public ComponentStyleDelegate<C> fontAlignment( UI.HorizontalAlignment alignment ) {
        return _withStyle(_style._withFont(_style.font().horizontalAlignment(alignment)));
    }

    /**
     *  Use this to define the vertical alignment of the default font of the component.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text. <br>
     *  Also note that not all text based components support text alignment.
     *  @param alignment The vertical alignment of the font.
     *                   See {@link UI.VerticalAlignment} for more information.
     *  @return A new {@link ComponentStyleDelegate} with the provided font alignment.
     */
    public ComponentStyleDelegate<C> fontAlignment( UI.VerticalAlignment alignment ) {
        return _withStyle(_style._withFont(_style.font().verticalAlignment(alignment)));
    }

    /**
     *  Use this to define the horizontal and vertical alignment of the default font of the component.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text. <br>
     *  Also note that not all text based components support text alignment.
     *  @param alignment The horizontal and vertical alignment of the font.
     *                   See {@link UI.Alignment} for more information.
     *  @return A new {@link ComponentStyleDelegate} with the provided font alignment.
     */
    public ComponentStyleDelegate<C> fontAlignment( UI.Alignment alignment ) {
        return fontAlignment(alignment.getHorizontal()).fontAlignment(alignment.getVertical());
    }

    /**
     *  Defines the minimum {@link Dimension} of this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMinimumSize(Dimension)} on the underlying component. <br>
     * @param minSize The minimum {@link Dimension}.
     * @return A new {@link ComponentStyleDelegate} with the provided minimum {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public ComponentStyleDelegate<C> minSize( Dimension minSize ) {
        Objects.requireNonNull(minSize);
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withMinWidth(minSize.width)._withMinHeight(minSize.height)));
    }

    /**
     *  Defines the minimum {@link Dimension} for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMinimumSize(Dimension)} on the underlying component,
     *  which will be called when all the other styles are applied and rendered. <br>
     * @param width The minimum width.
     * @param height The minimum height.
     * @return A new {@link ComponentStyleDelegate} with the provided minimum {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public ComponentStyleDelegate<C> minSize( int width, int height ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withMinWidth(width)._withMinHeight(height)));
    }

    /**
     *  Defines the minimum width for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMinimumSize(Dimension)} on the underlying component,
     *  which will be called when all the other styles are applied and rendered. <br>
     * @param minWidth The minimum width.
     * @return A new {@link ComponentStyleDelegate} with the provided minimum width set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public ComponentStyleDelegate<C> minWidth( int minWidth ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withMinWidth(minWidth)));
    }

    /**
     *  Defines the minimum height for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMinimumSize(Dimension)} on the underlying component,
     *  which will be called when all the other styles are applied and rendered. <br>
     * @param minHeight The minimum height.
     * @return A new {@link ComponentStyleDelegate} with the provided minimum height set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public ComponentStyleDelegate<C> minHeight( int minHeight ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withMinHeight(minHeight)));
    }

    /**
     *  Defines the maximum {@link Dimension} for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     *  The passed {@link Dimension} will be applied when all the other styles are applied and rendered. <br>
     *
     * @param maxSize The maximum {@link Dimension}.
     * @return A new {@link ComponentStyleDelegate} with the provided maximum {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public ComponentStyleDelegate<C> maxSize( Dimension maxSize ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withMaxWidth(maxSize.width)._withMaxHeight(maxSize.height)));
    }

    /**
     *  Defines the maximum {@link Dimension} for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     *  The passed {@link Dimension} will be applied when all the other styles are applied and rendered. <br>
     *
     * @param width The maximum width.
     * @param height The maximum height.
     * @return A new {@link ComponentStyleDelegate} with the provided maximum {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public ComponentStyleDelegate<C> maxSize( int width, int height ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withMaxWidth(width)._withMaxHeight(height)));
    }

    /**
     *  Defines the maximum width for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     *  The passed width will be applied when all the other styles are applied and rendered. <br>
     *
     * @param maxWidth The maximum width.
     * @return A new {@link ComponentStyleDelegate} with the provided maximum width set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public ComponentStyleDelegate<C> maxWidth( int maxWidth ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withMaxWidth(maxWidth)));
    }

    /**
     *  Defines the maximum height for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     *  The passed height will be applied when all the other styles are applied and rendered. <br>
     *
     * @param maxHeight The maximum height.
     * @return A new {@link ComponentStyleDelegate} with the provided maximum height set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public ComponentStyleDelegate<C> maxHeight( int maxHeight ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withMaxHeight(maxHeight)));
    }

    /**
     *  Defines the preferred {@link Dimension} for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     *  The passed {@link Dimension} will be applied when all the other styles are applied and rendered. <br>
     *
     * @param preferredSize The preferred {@link Dimension}.
     * @return A new {@link ComponentStyleDelegate} with the provided preferred {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public ComponentStyleDelegate<C> prefSize( Dimension preferredSize ) {
        Objects.requireNonNull(preferredSize);
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withPreferredWidth(preferredSize.width)._withPreferredHeight(preferredSize.height)));
    }

    /**
     *  Defines the preferred {@link Dimension} for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     *  The passed {@link Dimension} will be applied when all the other styles are applied and rendered. <br>
     *
     * @param width The preferred width.
     * @param height The preferred height.
     * @return A new {@link ComponentStyleDelegate} with the provided preferred {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public ComponentStyleDelegate<C> prefSize( int width, int height ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withPreferredWidth(width)._withPreferredHeight(height)));
    }

    /**
     *  Defines the preferred width for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     *  The passed width will be applied when all the other styles are applied and rendered. <br>
     *
     * @param preferredWidth The preferred width.
     * @return A new {@link ComponentStyleDelegate} with the provided preferred width set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public ComponentStyleDelegate<C> prefWidth( int preferredWidth ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withPreferredWidth(preferredWidth)));
    }

    /**
     *  Defines the preferred height for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     *  The passed height will be applied when all the other styles are applied and rendered. <br>
     *
     * @param preferredHeight The preferred height.
     * @return A new {@link ComponentStyleDelegate} with the provided preferred height set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public ComponentStyleDelegate<C> prefHeight( int preferredHeight ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withPreferredHeight(preferredHeight)));
    }

    /**
     *  Defines the size of this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param size The width and height size {@link Dimension}.
     * @return A new {@link ComponentStyleDelegate} with the provided size (width and height) {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public ComponentStyleDelegate<C> size( Dimension size ) {
        Objects.requireNonNull(size);
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withWidth(size.width)._withHeight(size.height)));
    }

    /**
     *  Defines the size of this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param width The width.
     * @param height The height.
     * @return A new {@link ComponentStyleDelegate} with the provided size (width and height) {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public ComponentStyleDelegate<C> size( int width, int height ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withWidth(width)._withHeight(height)));
    }


    /**
     *  Defines the width of this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param width The width.
     * @return A new {@link ComponentStyleDelegate} with the provided width set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public ComponentStyleDelegate<C> width( int width ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withWidth(width)));
    }

    /**
     *  Defines the height of this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param height The height.
     * @return A new {@link ComponentStyleDelegate} with the provided height set to be later
     *          applied to the underlying component when the final {@link Style} is applied.
     */
    public ComponentStyleDelegate<C> height( int height ) {
        return _withStyle(_style._withDimensionality(_style.dimensionality()._withHeight(height)));
    }

    /**
     *  Defines the cursor type for this {@link JComponent} based on
     *  the predefined {@link UI.Cursor} values. <br>
     *  If you want to specify a custom cursor implementation,
     *  use {@link #cursor(Cursor)} instead. <br>
     *
     * @param cursor The {@link UI.Cursor} value.
     * @return A new {@link ComponentStyleDelegate} with the provided cursor type set to be later
     */
    public ComponentStyleDelegate<C> cursor( UI.Cursor cursor ) {
        Objects.requireNonNull(cursor);
        return this.cursor(cursor.toAWTCursor());
    }

    /**
     *  Defines the cursor type for this {@link JComponent} based on
     *  the provided {@link Cursor} value. <br>
     *  Use this method if you want to specify a custom cursor implementation,
     *  in case you merely want to pick one of the many predefined {@link UI.Cursor} values,
     *  use {@link #cursor(UI.Cursor)} instead. <br>
     *
     * @param cursor The {@link Cursor} value.
     * @return A new {@link ComponentStyleDelegate} with the provided cursor type set to be later
     */
    public ComponentStyleDelegate<C> cursor( Cursor cursor ) {
        Objects.requireNonNull(cursor);
        return _withStyle(_style._withBase(_style.base().cursor(cursor)));
    }

    /**
     * @param installer The {@link Layout} to use for installing the layout.
     * @return A new {@link ComponentStyleDelegate} with the provided {@link Layout} set to be later
     */
    public ComponentStyleDelegate<C> layout( Layout installer ) {
        return _withStyle(_style._withLayout(_style.layout().layout(installer)));
    }

    /**
     *  Defines the layout {@link net.miginfocom.swing.MigLayout} constraints for
     *  this {@link JComponent} in the form of a {@link String}. <br>
     *  This ultimately translates to {@link net.miginfocom.swing.MigLayout#setLayoutConstraints(Object)}
     *  on the underlying component. <br>
     *  <br>
     *  Note that if this property is specified, the style engine will automatically
     *  install a {@link net.miginfocom.swing.MigLayout} on the component if it does not already have one. <br>
     *
     * @param constraints The layout constraints as a {@link String}.
     * @return A new {@link ComponentStyleDelegate} with the provided layout constraints set to be later
     */
    public ComponentStyleDelegate<C> layout( String constraints ) {
        if ( _style.layout().layout() instanceof Layout.ForMigLayout ) {
            Layout.ForMigLayout migInstaller = (Layout.ForMigLayout) _style.layout().layout();
            migInstaller = migInstaller.withConstraint(constraints);
            return _withStyle(_style._withLayout(_style.layout().layout(migInstaller)));
        }
        return _withStyle(_style._withLayout(_style.layout().layout(Layout.mig(constraints, "", ""))));
    }

    /**
     *  Defines the {@link net.miginfocom.swing.MigLayout} based layout constraints
     *  and column layout constraints of this {@link JComponent} in the form of a {@link String}. <br>
     *  This ultimately translates to {@link net.miginfocom.swing.MigLayout#setLayoutConstraints(Object)}
     *  as well as {@link net.miginfocom.swing.MigLayout#setColumnConstraints(Object)}
     *  on the layout manager of the underlying component. <br>
     *  <br>
     *  Note that if this property is specified, the style engine will automatically
     *  install a {@link net.miginfocom.swing.MigLayout} on the component if it does not already have one. <br>
     *
     * @param constraints The layout constraints as a {@link String}.
     * @param columnConstraints The column constraints as a {@link String}.
     * @return A new {@link ComponentStyleDelegate} with the provided layout constraints set to be later
     */
    public ComponentStyleDelegate<C> layout( String constraints, String columnConstraints ) {
        if ( _style.layout().layout() instanceof Layout.ForMigLayout) {
            Layout.ForMigLayout migInstaller = (Layout.ForMigLayout) _style.layout().layout();
            migInstaller = migInstaller.withConstraint(constraints).withColumnConstraint(columnConstraints);
            return _withStyle(_style._withLayout(_style.layout().layout(migInstaller)));
        }
        return _withStyle(_style._withLayout(_style.layout().layout(Layout.mig(constraints, columnConstraints, ""))));
    }

    /**
     *  Defines the {@link net.miginfocom.swing.MigLayout} based layout constraints
     *  column layout constraints and row layout constraints of this {@link JComponent} in the form of a {@link String}. <br>
     *  This ultimately translates to {@link net.miginfocom.swing.MigLayout#setLayoutConstraints(Object)}
     *  as well as {@link net.miginfocom.swing.MigLayout#setColumnConstraints(Object)}
     *  and {@link net.miginfocom.swing.MigLayout#setRowConstraints(Object)}
     *  on the layout manager of the underlying component. <br>
     *  <br>
     *  Note that if this property is specified, the style engine will automatically
     *  install a {@link net.miginfocom.swing.MigLayout} on the component if it does not already have one. <br>
     *
     * @param constraints The layout constraints as a {@link String}.
     * @param columnConstraints The column constraints as a {@link String}.
     * @param rowConstraints The row constraints as a {@link String}.
     * @return A new {@link ComponentStyleDelegate} with the provided layout constraints set to be later
     */
    public ComponentStyleDelegate<C> layout( String constraints, String columnConstraints, String rowConstraints ) {
        return _withStyle(_style._withLayout(_style.layout().layout(Layout.mig(constraints, columnConstraints, rowConstraints))));
    }

    /**
     *  Defines the component constraints of this component with respect to the parent component
     *  and its layout manager, in the form of a {@link String}. <br>
     *  This ultimately translates to {@link net.miginfocom.swing.MigLayout#setComponentConstraints(Component, Object)}
     *  on the layout manager of the parent component. <br>
     *  <br>
     *  Note that if this property is specified, the style engine will automatically
     *  install a {@link net.miginfocom.swing.MigLayout} on the parent component if it does not already have one. <br>
     *
     * @param constraints The component constraints as a {@link String}.
     * @return A new {@link ComponentStyleDelegate} with the provided component constraints set to be later
     */
    public ComponentStyleDelegate<C> addConstraint( Object constraints ) {
        return _withStyle(_style._withLayout(_style.layout().constraint(constraints)));
    }

    /**
     *  Defines the layout {@link net.miginfocom.swing.MigLayout} constraints for
     *  this {@link JComponent} in the form of a {@link LayoutConstraint}
     *  (see {@link UI#FILL}, {@link UI#FILL_X}, {@link UI#FILL_Y}...). <br>
     *  This ultimately translates to {@link net.miginfocom.swing.MigLayout#setLayoutConstraints(Object)}
     *  on the underlying component. <br>
     *  <br>
     *  Note that if this property is specified, the style engine will automatically
     *  install a {@link net.miginfocom.swing.MigLayout} on the component if it does not already have one. <br>
     *
     * @param constraintAttr The layout constraints as a {@link LayoutConstraint}.
     * @return A new {@link ComponentStyleDelegate} with the provided layout constraints set to be later
     */
    public ComponentStyleDelegate<C> layout( LayoutConstraint constraintAttr ) {
        return layout(constraintAttr.toString());
    }

    /**
     *  Defines the alignment percentage alongside the X axis for a component (see {@link JComponent#setAlignmentX(float)}). <br>
     *  Note that the alignment may not have an effect on all components
     *  as it depends on the layout manager of the component. <br>
     *
     * @param percentage The alignment percentage in terms of a number between 0 and 1 alongside the X axis.
     * @return A new {@link ComponentStyleDelegate} with the provided alignment percentage alongside the X axis set to be later
     */
    public ComponentStyleDelegate<C> alignmentX( float percentage ) {
        return _withStyle(_style._withLayout(_style.layout().alignmentX(percentage)));
    }

    /**
     *  Defines the alignment percentage alongside the Y axis for a component (see {@link JComponent#setAlignmentY(float)}). <br>
     *  Note that the alignment may not have an effect on all components
     *  as it depends on the layout manager of the component. <br>
     *
     * @param percentage The alignment percentage in terms of a number between 0 and 1 alongside the Y axis.
     * @return A new {@link ComponentStyleDelegate} with the provided alignment percentage alongside the Y axis set to be later
     */
    public ComponentStyleDelegate<C> alignmentY( float percentage ) {
        return _withStyle(_style._withLayout(_style.layout().alignmentY(percentage)));
    }

    /**
     * @return The current UI scale factor, which is used to scale the UI
     *         for high resolution displays (high dots-per-inch, or DPI).
     */
    public float getScale() { return UI.scale(); }

    /**
     * @return The current UI scale factor, which is used to scale the UI
     *         for high resolution displays (high dots-per-inch, or DPI).
     */
    public float scale() { return UI.scale(); }

    /**
     *  Use this method inside custom {@link swingtree.api.Painter} implementations (see {@link #painter(UI.Layer, swingtree.api.Painter)})
     *  to scale an {@code int} value by the current UI scale factor to ensure
     *  that the UI is scaled properly for high resolution displays (high dots-per-inch, or DPI).
     *  @param value The {@code int} value to scale.
     *  @return The scaled {@code int} value.
     */
    public int scale( int value ) { return UI.scale(value); }

    /**
     *  Use this method inside custom {@link swingtree.api.Painter} implementations (see {@link #painter(UI.Layer, swingtree.api.Painter)})
     *  to scale a {@code float} value by the current UI scale factor to ensure
     *  that the UI is scaled properly for high resolution displays (high dots-per-inch, or DPI).
     *  @param value The {@code float} value to scale.
     *  @return The scaled {@code float} value.
     */
    public float scale( float value ) { return UI.scale(value); }

    /**
     *  Use this method inside custom {@link swingtree.api.Painter} implementations (see {@link #painter(UI.Layer, Painter)})
     *  to scale a {@code double} value by the current UI scale factor to ensure
     *  that the UI is scaled properly for high resolution displays (high dots-per-inch, or DPI).
     *  @param value The {@code double} value to scale.
     *  @return The scaled {@code double} value.
     */
    public double scale( double value ) { return UI.scale(value); }

}

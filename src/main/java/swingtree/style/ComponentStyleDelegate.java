package swingtree.style;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.UI;
import swingtree.api.*;
import swingtree.api.Painter;
import swingtree.layout.LayoutConstraint;
import swingtree.layout.Size;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 *  A {@link ComponentStyleDelegate} is a delegate for a {@link JComponent} and its {@link StyleConf} configuration
 *  used to apply further specify the style of said {@link JComponent}.
 *  Instances of this will be exposed to you via the {@link swingtree.UIForAnySwing#withStyle(Styler)}
 *  method, where you can specify a lambda that takes a {@link ComponentStyleDelegate} and returns a
 *  transformed {@link StyleConf} object, as well as inside of {@link StyleSheet} extensions
 *  where you can declare similar styling lambdas for {@link StyleTrait}s, which are
 *  styling rules... <br>
 *
 * @param <C> The type of {@link JComponent} this {@link ComponentStyleDelegate} is for.
 */
public final class ComponentStyleDelegate<C extends JComponent>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ComponentStyleDelegate.class);

    private final C _component;
    private final StyleConf _styleConf;


    ComponentStyleDelegate( C component, StyleConf styleConf) {
        _component = Objects.requireNonNull(component);
        _styleConf = Objects.requireNonNull(styleConf);
    }

    ComponentStyleDelegate<C> _withStyle( StyleConf styleConf) {
        return new ComponentStyleDelegate<>(_component, styleConf);
    }

    /**
     *  Returns the {@link JComponent} this {@link ComponentStyleDelegate} is defining a {@link StyleConf} for.
     *  This is useful if you want to make the styling of a component based on its state,
     *  like for example determining the background color of a {@link JCheckBox} based on
     *  whether it is selected or not...
     * 
     * @return The {@link JComponent} this {@link ComponentStyleDelegate} is for.
     */
    public C component() { return _component; }

    /**
     *  Exposes the parent {@link Container} of the {@link JComponent} delegated by this {@link ComponentStyleDelegate}
     *  through an {@link Optional} in case the parent is null.
     *  You may use this to make your styling dependent on the properties of the parent container.
     *
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
    public ComponentStyleDelegate<C> peek( Peeker<C> peeker )
    {
        Objects.requireNonNull(peeker);
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
        Objects.requireNonNull(styler);

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
     *  Returns the {@link StyleConf} this {@link ComponentStyleDelegate} is defining for the {@link JComponent}
     *  returned by {@link #component()}.
     * <p>
     * @return The {@link StyleConf} this {@link ComponentStyleDelegate} is for.
     */
    StyleConf style() { return _styleConf; }

    /**
     *  Creates a new {@link StyleConf} with the provided top, right, left and bottom margin distances.
     *  It determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(double)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * 
     * @param top The top padding distance in pixels.
     * @param right The right padding distance in pixels.
     * @param bottom The bottom padding distance in pixels.
     * @param left The left padding distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided padding distances.
     */
    public ComponentStyleDelegate<C> margin( double top, double right, double bottom, double left ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withMargin(Outline.of(top, right, bottom, left))));
    }

    /**
     *  Creates a new {@link StyleConf} with the provided margin distance for all sides of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(double)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     *
     * @param margin The margin distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided margin distance.
     */
    public ComponentStyleDelegate<C> margin( double margin ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withMargin(Outline.of((float) margin))));
    }

    /**
     *  Creates a new {@link StyleConf} with the provided margin distance for the top side of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(double)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * 
     * @param margin The margin distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided margin distance.
     */
    public ComponentStyleDelegate<C> marginTop( double margin ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withMargin(_styleConf.border().margin().withTop((float) margin))));
    }

    /**
     *  Creates a new {@link StyleConf} with the provided margin distance for the right side of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(double)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     *
     * @param margin The margin distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided margin distance.
     */
    public ComponentStyleDelegate<C> marginRight( double margin ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withMargin(_styleConf.border().margin().withRight((float) margin))));
    }

    /**
     *  Creates a new {@link StyleConf} with the provided margin distance for the bottom side of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(double)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     *
     * @param margin The margin distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided margin distance.
     */
    public ComponentStyleDelegate<C> marginBottom( double margin ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withMargin(_styleConf.border().margin().withBottom((float) margin))));
    }

    /**
     *  Creates a new {@link StyleConf} with the provided margin distance for the left side of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(double)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     *
     * @param margin The margin distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided margin distance.
     */
    public ComponentStyleDelegate<C> marginLeft( double margin ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withMargin(_styleConf.border().margin().withLeft((float) margin))));
    }

    /**
     *  Creates a new {@link StyleConf} with the provided margin distance for the top and bottom sides of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(double)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     *
     * @param margin The margin distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided margin distance.
     */
    public ComponentStyleDelegate<C> marginVertical( double margin ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withMargin(_styleConf.border().margin().withTop((float) margin).withBottom((float) margin))));
    }

    /**
     *  Creates a new {@link StyleConf} with the provided margin distance for the left and right sides of the component.
     *  The margin determines the amount of space between the component's outer bounds and the beginning
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(double)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     *
     * @param margin The margin distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided margin distance.
     */
    public ComponentStyleDelegate<C> marginHorizontal( double margin ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withMargin(_styleConf.border().margin().withLeft((float) margin).withRight((float) margin))));
    }

    /**
     *  Creates a new {@link StyleConf} with the provided top, right, left and bottom pad distances.
     *  It determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(double)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     *
     * @param top The top padding distance in pixels.
     * @param right The right padding distance in pixels.
     * @param bottom The bottom padding distance in pixels.
     * @param left The left padding distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided padding distances.
     */
    public ComponentStyleDelegate<C> padding( double top, double right, double bottom, double left ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withPadding(Outline.of(top, right, bottom, left))));
    }

    /**
     *  Creates a new {@link StyleConf} with the provided padding distance for all sides of the component.
     *  It determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(double)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     *
     * @param padding The padding distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided padding distance.
     */
    public ComponentStyleDelegate<C> padding( double padding ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withPadding(Outline.of((float) padding))));
    }

    /**
     *  Creates a new {@link StyleConf} with the provided padding distance for the top side of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(double)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     *
     * @param padding The padding distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided padding distance.
     */
    public ComponentStyleDelegate<C> paddingTop( double padding ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withPadding(_styleConf.border().padding().withTop((float) padding))));
    }

    /**
     *  Creates a new {@link StyleConf} with the provided padding distance for the right side of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  of the inner border, background region and shadow frame
     *  (see {@link #borderWidth(double)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     *
     * @param padding The padding distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided padding distance.
     */
    public ComponentStyleDelegate<C> paddingRight( double padding ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withPadding(_styleConf.border().padding().withRight((float) padding))));
    }

    /**
     *  Creates a new {@link StyleConf} with the provided padding distance for the bottom side of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(double)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     *
     * @param padding The padding distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided padding distance.
     */
    public ComponentStyleDelegate<C> paddingBottom( double padding ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withPadding(_styleConf.border().padding().withBottom((float) padding))));
    }

    /**
     *  Creates a new {@link StyleConf} with the provided padding distance for the left side of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(double)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     *
     * @param padding The padding distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided padding distance.
     */
    public ComponentStyleDelegate<C> paddingLeft( double padding ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withPadding(_styleConf.border().padding().withLeft((float) padding))));
    }

    /**
     *  Creates a new {@link StyleConf} with the provided padding distance for the top and bottom sides of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(double)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     *
     * @param padding The padding distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided padding distance.
     */
    public ComponentStyleDelegate<C> paddingVertical( double padding ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withPadding(_styleConf.border().padding().withTop((float) padding).withBottom((float) padding))));
    }

    /**
     *  Creates a new {@link StyleConf} with the provided padding distance for the left and right sides of the component.
     *  The padding determines the amount of space between the inner bounds (the inner border, background region and shadow frame)
     *  and the component's content.
     *  (see {@link #borderWidth(double)}, {@link #backgroundColor(Color)}, {@link #shadowColor(Color)}).
     * 
     * @param padding The padding distance in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided padding distance.
     */
    public ComponentStyleDelegate<C> paddingHorizontal( double padding ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withPadding(_styleConf.border().padding().withLeft((float) padding).withRight((float) padding))));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided border width and border color.
     *  The border will be rendered with an inset space based on the margin defined by the {@link StyleConf}.
     *
     * @param width The border width in pixels.
     * @param color The border color.
     * @return A new {@link ComponentStyleDelegate} with the provided border width and border color.
     */
    public ComponentStyleDelegate<C> border( double width, Color color ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withWidth(width).withColor(color)));
    }

    /**
     *   Returns a new {@link StyleConf} with the provided border width and border colors.
     *   The border will be rendered with an inset space based on the margin defined by the {@link StyleConf}.
     *   You may configure the border colors for each side of the component individually.
     * @param width The border width in pixels.
     * @param top The color for the top part of the border.
     * @param right The color for the right part of the border.
     * @param bottom The color for the bottom part of the border.
     * @param left The color for the left part of the border.
     * @return A new {@link ComponentStyleDelegate} with the provided border width and border colors.
     */
    public ComponentStyleDelegate<C> border( double width, Color top, Color right, Color bottom, Color left ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withWidth(width).withColors(top, right, bottom, left)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided border widths and border color.
     *  The border will be rendered with an inset space based on the margin defined by the {@link StyleConf}.
     *
     * @param top The border width in pixels for the top side of the component.
     * @param right The border width in pixels for the right side of the component.
     * @param bottom The border width in pixels for the bottom side of the component.
     * @param left The border width in pixels for the left side of the component.
     * @param color The border color.
     * @return A new {@link ComponentStyleDelegate} with the provided border widths and border color.
     */
    public ComponentStyleDelegate<C> border( double top, double right, double bottom, double left, Color color ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withWidths(Outline.of(top, right, bottom, left)).withColor(color)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided border width and border color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties
     *  (See {@link swingtree.UI#color(String)} for more information on the supported color formats).
     *  The border will be rendered with an inset space based on the padding defined by the {@link StyleConf}.
     *
     * @param width The border width in pixels.
     * @param colorString The border color.
     * @return A new {@link ComponentStyleDelegate} with the provided border width and border color.
     */
    public ComponentStyleDelegate<C> border( double width, String colorString ) {
        Objects.requireNonNull(colorString);
        Color newColor;
        try {
            newColor = UI.color(colorString);
        } catch ( Exception e ) {
            log.error("Failed to parse color string: '"+colorString+"'", e);
            return this;
        }
        return _withStyle(_styleConf._withBorder(_styleConf.border().withWidth(width).withColor(newColor)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided border width and border colors in the form of strings.
     *  The strings can be either hex color strings, color names or color constants from the system properties.
     *  The border will be rendered with an inset space based on the padding defined by the {@link StyleConf}.
     *
     * @param width The border width in pixels.
     * @param top The color for the top part of the border.
     * @param right The color for the right part of the border.
     * @param bottom The color for the bottom part of the border.
     * @param left The color for the left part of the border.
     * @return A new {@link ComponentStyleDelegate} with the provided border width and border colors.
     */
    public ComponentStyleDelegate<C> border( double width, String top, String right, String bottom, String left ) {
        Color topColor = UI.color(top);
        Color rightColor = UI.color(right);
        Color bottomColor = UI.color(bottom);
        Color leftColor = UI.color(left);
        return _withStyle(_styleConf._withBorder(_styleConf.border().withWidth(width).withColors(topColor, rightColor, bottomColor, leftColor)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided border width.
     *  <p>
     *  Note that in order for the border to be visible you also
     *  have to specify it's color, which you can do through
     *  {@link #borderColor(Color)} or {@link #borderColor(String)}.
     *  You may also specify different colors for each side of the border
     *  through {@link #borderColors(Color, Color, Color, Color)} or {@link #borderColors(String, String, String, String)}.
     *
     * @param width The border width in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided border width.
     */
    public ComponentStyleDelegate<C> borderWidth( double width ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withWidth(width)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided border colors,
     *  which are stored in the order of top, right, bottom, left.
     *  Note that a component border will be rendered with an inset
     *  space based on the padding defined by the {@link StyleConf}.
     *  <p>
     *  Instead of null, the {@link swingtree.UI.Color#UNDEFINED}
     *  constant is used to indicate that a border color is not set.
     *
     * @param top The color for the top part of the border.
     * @param right The color for the right part of the border.
     * @param bottom The color for the bottom part of the border.
     * @param left The color for the left part of the border.
     * @return A new {@link ComponentStyleDelegate} with the provided border colors.
     */
    public ComponentStyleDelegate<C> borderColors( Color top, Color right, Color bottom, Color left ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withColors(top, right, bottom, left)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided border colors in the form of strings.
     *  The strings can be either hex color strings, color names or color constants from the system properties
     *  (See {@link swingtree.UI#color(String)} for more information on the supported color formats).
     *  The border will be rendered with an inset space based on the padding defined by the {@link StyleConf}.
     *
     * @param top The color for the top part of the border.
     * @param right The color for the right part of the border.
     * @param bottom The color for the bottom part of the border.
     * @param left The color for the left part of the border.
     * @return A new {@link ComponentStyleDelegate} with the provided border colors.
     */
    public ComponentStyleDelegate<C> borderColors( String top, String right, String bottom, String left ) {
        Color topColor = UI.color(top);
        Color rightColor = UI.color(right);
        Color bottomColor = UI.color(bottom);
        Color leftColor = UI.color(left);
        return _withStyle(_styleConf._withBorder(_styleConf.border().withColors(topColor, rightColor, bottomColor, leftColor)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided border width for the specified edge.
     *  <p>
     *  Note that in order for the border to be visible you also
     *  have to specify it's color, which you can do through
     *  {@link #borderColor(Color)} or {@link #borderColor(String)}.
     *
     * @param edge The edge to set the border width for.
     * @param width The border width in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided border width for the specified edge.
     */
    public ComponentStyleDelegate<C> borderWidthAt( UI.Edge edge, double width ) {
        Objects.requireNonNull(edge);
        return _withStyle(_styleConf._withBorder(_styleConf.border().withWidthAt(edge, (float) width)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided width and color used to define
     *  the border for the specified edge of the component.
     *  The border will be rendered with an inset space based on the padding defined by the {@link StyleConf}.
     *
     * @param edge The edge to set the border width and color for, you may use {@link UI.Edge#EVERY}
     *             to set the same width and color for all edges.
     * @param width The border width in pixels.
     * @param color The border color.
     * @return A new {@link ComponentStyleDelegate} with the provided border width and color for the specified edge.
     */
    public ComponentStyleDelegate<C> borderAt( UI.Edge edge, double width, Color color ) {
        Objects.requireNonNull(edge);
        return _withStyle(_styleConf._withBorder(_styleConf.border().withWidthAt(edge, (float) width).withColorAt(edge, color)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided width and color string used to define
     *  the border for the specified edge of the component.
     *  The border will be rendered with an inset space based on the padding defined by the {@link StyleConf}.
     *  <p>
     *  The color is specified as a string, which can be either a hex color string, a color name or a color constant
     *  from the system properties (See {@link swingtree.UI#color(String)} for more information on the supported color formats).
     *
     * @param edge The edge to set the border width and color for, you may use {@link UI.Edge#EVERY}
     *             to set the same width and color for all edges.
     * @param width The border width in pixels.
     * @param colorString The border color.
     * @return A new {@link ComponentStyleDelegate} with the provided border width and color for the specified edge.
     */
    public ComponentStyleDelegate<C> borderAt( UI.Edge edge, double width, String colorString ) {
        Objects.requireNonNull(edge);
        Color newColor;
        try {
            newColor = UI.color(colorString);
        } catch ( Exception e ) {
            log.error("Failed to parse color string: '"+colorString+"'", e);
            return this;
        }
        return _withStyle(_styleConf._withBorder(_styleConf.border().withWidthAt(edge, (float) width).withColorAt(edge, newColor)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided top, right, bottom and left border widths.
     *  <p>
     *  The border widths are specified in the following order: top, right, bottom, left.
     *  <p>
     *  Example:
     *  <pre>{@code
     *      UI.panel().withStyle( it -> it.borderWidths(1, 2, 3, 4) )
     *  }</pre>
     *  <p>
     *  Note that in order for the border to be visible you also
     *  have to specify it's color, which you can do through
     *  {@link #borderColor(Color)} or {@link #borderColor(String)}.
     *
     * @param top The top border width in pixels.
     * @param right The right border width in pixels.
     * @param bottom The bottom border width in pixels.
     * @param left The left border width in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided top, right, bottom and left border widths.
     * @see #borderWidth(double)
     * @see #borderWidthAt(UI.Edge, double)
     */
    public ComponentStyleDelegate<C> borderWidths( double top, double right, double bottom, double left ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withWidths(Outline.of(top, right, bottom, left))));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided top/bottom and left/right border widths.
     *  <p>
     *  Example:
     *  <pre>{@code
     *      UI.panel().withStyle( it -> it.borderWidths(1, 2) )
     *  }</pre>
     *  <p>
     *  Note that in order for the border to be visible you also
     *  have to specify it's color, which you can do through
     *  {@link #borderColor(Color)} or {@link #borderColor(String)}.
     *
     * @param topBottom The top and bottom border width in pixels.
     * @param leftRight The left and right border width in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided top/bottom and left/right border widths.
     * @see #borderWidth(double)
     * @see #borderWidthAt(UI.Edge, double)
     */
    public ComponentStyleDelegate<C> borderWidths( double topBottom, double leftRight ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withWidths(Outline.of(topBottom, leftRight, topBottom, leftRight))));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided border color.
     *  The border will be rendered with an inset space based on the padding defined by the {@link StyleConf}.
     *
     * @param color The border color.
     * @return A new {@link ComponentStyleDelegate} with the provided border color.
     */
    public ComponentStyleDelegate<C> borderColor( Color color ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withColor(color)));
    }

    /**
     *  Returns an updated {@link StyleConf} with the provided border color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The border will be rendered with an inset space based on the padding defined by the {@link StyleConf}.
     *
     * @param colorString The border color.
     * @return A new {@link ComponentStyleDelegate} with the provided border color.
     */
    public ComponentStyleDelegate<C> borderColor( String colorString ) {
        Objects.requireNonNull(colorString);
        Color newColor;
        try {
            newColor = UI.color(colorString);
        } catch ( Exception e ) {
            log.error("Failed to parse color string: '{}'", colorString, e);
            return this;
        }
        return _withStyle(_styleConf._withBorder(_styleConf.border().withColor(newColor)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided border radius
     *  set for all 4 corners of the target component.
     *  This will override both the arc width and arc height of each corner.
     *  The border will be rendered with an inset space based on the padding defined by this {@link StyleConf}.
     *
     * @param radius The border radius in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided border radius.
     */
    public ComponentStyleDelegate<C> borderRadius( double radius ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withArcWidth(radius).withArcHeight(radius)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided border arc width and arc height
     *  set for all 4 corners of the target component.
     *  Note that the border will be rendered with an inset space based on the padding defined by this {@link StyleConf}.
     *
     * @param arcWidth The border arc width in pixels.
     * @param arcHeight The border arc height in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided border arc width and arc height.
     */
    public ComponentStyleDelegate<C> borderRadius( double arcWidth, double arcHeight ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withArcWidth(arcWidth).withArcHeight(arcHeight)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided border arc width and arc height for the specified corner.
     *  Note that the border will be rendered with an inset space based on the padding defined by this {@link StyleConf}.
     *
     * @param corner The corner to apply the border radius to.
     * @param arcWidth The border arc width in pixels.
     * @param arcHeight The border arc height in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided border arc width and arc height for the specified corner.
     */
    public ComponentStyleDelegate<C> borderRadiusAt( UI.Corner corner, double arcWidth, double arcHeight ) {
        return _withStyle(_styleConf._withBorder(_styleConf.border().withArcWidthAt(corner, arcWidth).withArcHeightAt(corner, arcHeight)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided border radius for the specified corner.
     *  This will override both the arc width and arc height of the border.
     *  Note that the border will be rendered with an inset space based on the padding defined by this {@link StyleConf}.
     *
     * @param corner The corner to apply the border radius to.
     * @param radius The border radius in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided border radius for the specified corner.
     */
    public ComponentStyleDelegate<C> borderRadiusAt( UI.Corner corner, double radius ) {
        return this.borderRadiusAt(corner, radius, radius);
    }

    /**
     *  Returns a new {@link StyleConf} with the provided {@link ImageIcon} as the icon
     *  for the current component (see {@link #component()}).
     *  Note that this will only produce a result for components that actually support icons.
     *  Like for example all the various {@link AbstractButton} subclasses, {@link JLabel}
     *  and {@link swingtree.components.JIcon}.
     *
     * @param icon The icon.
     * @return A new {@link ComponentStyleDelegate} with the provided icon.
     */
    public ComponentStyleDelegate<C> icon( ImageIcon icon ) {
        return _withStyle(_styleConf._withBase(_styleConf.base().icon(icon)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided {@link ImageIcon} as the icon
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
        return _withStyle(_styleConf._withBase(_styleConf.base().icon(icon).fit(fit)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided {@link IconDeclaration} as the
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
     *  Returns a new {@link StyleConf} with the provided {@link IconDeclaration} as the
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
     *  Returns a new {@link StyleConf} with the provided background foundation color.
     *  The foundation color covers the {@link UI.ComponentArea#EXTERIOR}, which
     *  starts at the outer bounds of the component and the beginning of the border.
     *  So the space spanned by the margins of the component including the additional
     *  exterior space exposed by the border radius.
     *
     * @param color The background color.
     * @return A new {@link ComponentStyleDelegate} with the provided background color.
     */
    public ComponentStyleDelegate<C> foundationColor( Color color ) {
        Objects.requireNonNull(color, "Use 'UI.Color.UNDEFINED' instead of 'null'.");
        return _withStyle(_styleConf._withBase(_styleConf.base().foundationColor(color)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided background foundation color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The foundation color covers the {@link UI.ComponentArea#EXTERIOR}, which
     *  starts at the outer bounds of the component and the beginning of the border.
     *  So the space spanned by the margins of the component including the additional
     *  exterior space exposed by the border radius.
     *
     * @param colorString The background color.
     * @return A new {@link ComponentStyleDelegate} with the provided background color.
     */
    public ComponentStyleDelegate<C> foundationColor( String colorString ) {
        Objects.requireNonNull(colorString);
        Color newColor;
        try {
            newColor = UI.color(colorString);
        } catch ( Exception e ) {
            log.error("Failed to parse color string: '"+colorString+"'", e);
            return this;
        }
        return _withStyle(_styleConf._withBase(_styleConf.base().foundationColor(newColor)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided background foundation color
     *  defined by the supplied red, green and blue color channels in
     *  the form of doubles expected to be in the range of 0.0 to 1.0.
     *  The foundation color covers the {@link UI.ComponentArea#EXTERIOR}, which
     *  starts at the outer bounds of the component and the beginning of the border.
     *  So the space spanned by the margins of the component including the additional
     *  exterior space exposed by the border radius.
     *
     * @param r The red component of the background color in the range of 0.0 to 1.0.
     * @param g The green component of the background color in the range of 0.0 to 1.0.
     * @param b The blue component of the background color in the range of 0.0 to 1.0.
     * @return A new {@link ComponentStyleDelegate} with the provided background color.
     */
    public ComponentStyleDelegate<C> foundationColor( double r, double g, double b ) {
        return foundationColor(new Color((float) r, (float) g, (float) b));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided background foundation color
     *  defined by the supplied red, green, blue and alpha color channels in
     *  the form of doubles expected to be in the range of 0.0 to 1.0.
     *  The foundation color covers the {@link UI.ComponentArea#EXTERIOR}, which
     *  starts at the outer bounds of the component and the beginning of the border.
     *  So the space spanned by the margins of the component including the additional
     *  exterior space exposed by the border radius.
     *
     * @param r The red component of the background color in the range of 0.0 to 1.0.
     * @param g The green component of the background color in the range of 0.0 to 1.0.
     * @param b The blue component of the background color in the range of 0.0 to 1.0.
     * @param a The alpha component of the background color in the range of 0.0 to 1.0.
     * @return A new {@link ComponentStyleDelegate} with the provided background color.
     */
    public ComponentStyleDelegate<C> foundationColor( double r, double g, double b, double a ) {
        return foundationColor(new Color((float) r, (float) g, (float) b, (float) a));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided inner Background color.
     *  The background color covers the {@link UI.ComponentArea#INTERIOR}, which, when going inwards,
     *  starts at the end of the component's border area ({@link UI.ComponentArea#BORDER}),
     *  (which is defined by {@link UI.ComponentBoundary#BORDER_TO_INTERIOR})
     *  and then completely fills the component's inner bounds ({@link UI.ComponentArea#INTERIOR}),
     *  including both the space spanned by the padding and the content area.
     *
     * @param color The inner background color.
     * @return A new {@link ComponentStyleDelegate} with the provided inner background color.
     */
    public ComponentStyleDelegate<C> backgroundColor( Color color ) {
        Objects.requireNonNull(color, "Use 'UI.Color.UNDEFINED' instead of 'null'.");
        return _withStyle(_styleConf._withBase(_styleConf.base().backgroundColor(color)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided inner Background color
     *  defined by the supplied red, green and blue color channels in
     *  the form of doubles expected to be in the range of 0.0 to 1.0.
     *  The background color covers the {@link UI.ComponentArea#INTERIOR}, which, when going inwards,
     *  starts at the end of the component's border area ({@link UI.ComponentArea#BORDER}),
     *  (which is defined by {@link UI.ComponentBoundary#BORDER_TO_INTERIOR})
     *  and then completely fills the component's inner bounds ({@link UI.ComponentArea#INTERIOR}),
     *  including both the space spanned by the padding and the content area.
     *
     * @param r The red component of the inner background color in the range of 0.0 to 1.0.
     * @param g The green component of the inner background color in the range of 0.0 to 1.0.
     * @param b The blue component of the inner background color in the range of 0.0 to 1.0.
     * @return A new {@link ComponentStyleDelegate} with the provided inner background color.
     */
    public ComponentStyleDelegate<C> backgroundColor( double r, double g, double b ) {
        return backgroundColor(new Color((float) r, (float) g, (float) b));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided inner Background color
     *  defined by the supplied red, green, blue and alpha color channels in
     *  the form of doubles expected to be in the range of 0.0 to 1.0.
     *  The background color covers the {@link UI.ComponentArea#INTERIOR}, which, when going inwards,
     *  starts at the end of the component's border area ({@link UI.ComponentArea#BORDER}),
     *  (which is defined by {@link UI.ComponentBoundary#BORDER_TO_INTERIOR})
     *  and then completely fills the component's inner bounds ({@link UI.ComponentArea#INTERIOR}),
     *  including both the space spanned by the padding and the content area.
     *
     * @param r The red component of the inner background color in the range of 0.0 to 1.0.
     * @param g The green component of the inner background color in the range of 0.0 to 1.0.
     * @param b The blue component of the inner background color in the range of 0.0 to 1.0.
     * @param a The alpha component of the inner background color in the range of 0.0 to 1.0.
     * @return A new {@link ComponentStyleDelegate} with the provided inner background color.
     */
    public ComponentStyleDelegate<C> backgroundColor( double r, double g, double b, double a ) {
        return backgroundColor(new Color((float) r, (float) g, (float) b, (float) a));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided inner Background color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  The background color covers the {@link UI.ComponentArea#INTERIOR}, which, when going inwards,
     *  starts at the end of the component's border area ({@link UI.ComponentArea#BORDER}),
     *  (which is defined by {@link UI.ComponentBoundary#BORDER_TO_INTERIOR})
     *  and then completely fills the component's inner bounds ({@link UI.ComponentArea#INTERIOR}),
     *  including both the space spanned by the padding and the content area.
     *
     * @param colorString The inner background color.
     * @return A new {@link ComponentStyleDelegate} with the provided inner background color.
     */
    public ComponentStyleDelegate<C> backgroundColor( String colorString ) {
        Objects.requireNonNull(colorString);
        Color newColor;
        try {
            newColor = UI.color(colorString);
        } catch ( Exception e ) {
            log.error("Failed to parse color string: '"+colorString+"'", e);
            return this;
        }
        return _withStyle(_styleConf._withBase(_styleConf.base().backgroundColor(newColor)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided custom {@link swingtree.api.Painter}, which
     *  will be called using the {@link Graphics2D} of the current component.
     *  You may use this to render a custom background for the component.
     *  <br><br>
     *  <b>
     *    Note that your custom painter will yield the best performance if they are value based in the sense that
     *    they have {@link Object#hashCode()} and {@link Object#equals(Object)} implementation which
     *    are based on the data that the painter uses to render the component.
     *    This is because it allows SwingTree to cache the rendering of the painters and avoid unnecessary repaints. <br>
     *    If you do not want to create a custom class just for painting but instead
     *    just want to pass an immutable cache key to a painter, then consider using the
     *    {@link Painter#of(Object, Painter)} factory method to create a painter that has the
     *    with {@link Object#hashCode()} and {@link Object#equals(Object)} implemented
     *    based on the provided cache key.
     *  </b>
     *
     * @param layer The layer on which the painter should do its work.
     *              It is an enum instance which
     *              gives the painter a particular rank in the painting order.
     *              So the {@link swingtree.UI.Layer#BACKGROUND} will be painted first,
     *              followed by the {@link swingtree.UI.Layer#CONTENT} and so on...
     *              <br>
     *              The following layers are available:
     *              <ul>
     *                  <li>{@link UI.Layer#BACKGROUND}</li>
     *                  <li>{@link UI.Layer#CONTENT}</li>
     *                  <li>{@link UI.Layer#BORDER}</li>
     *                  <li>{@link UI.Layer#FOREGROUND}</li>
     *              </ul>
     * @param painter A custom painter, which receives the {@link Graphics2D} instance of the current component.
     * @return A new {@link ComponentStyleDelegate} with the provided background renderer.
     */
    public ComponentStyleDelegate<C> painter( UI.Layer layer, swingtree.api.Painter painter ) {
        return _withStyle(_styleConf.painter(layer, UI.ComponentArea.INTERIOR, StyleUtil.DEFAULT_KEY, painter));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided custom {@link swingtree.api.Painter}, which
     *  will be called using the {@link Graphics2D} of the current component.
     *  You may use this to render a custom background for the component on the specified {@link swingtree.UI.Layer}
     *  and {@link swingtree.UI.ComponentArea}.
     *  <br><br>
     *  <b>
     *    Note that your custom painter will yield the best performance if they are value based in the sense that
     *    they have {@link Object#hashCode()} and {@link Object#equals(Object)} implementation which
     *    are based on the data that the painter uses to render the component.
     *    This is because it allows SwingTree to cache the rendering of the painters and avoid unnecessary repaints. <br>
     *    If you do not want to create a custom class just for painting but instead
     *    just want to pass an immutable cache key to a painter, then consider using the
     *    {@link Painter#of(Object, Painter)} factory method to create a painter that has the
     *    with {@link Object#hashCode()} and {@link Object#equals(Object)} implemented
     *    based on the provided cache key.
     *  </b>
     * @param layer The layer on which the painter should do its work.
     *              It is an enum instance which
     *              gives the painter a particular rank in the painting order.
     *              So the {@link swingtree.UI.Layer#BACKGROUND} will be painted first,
     *              followed by the {@link swingtree.UI.Layer#CONTENT} and so on...
     *              <br>
     *              The following layers are available:
     *              <ul>
     *                  <li>{@link UI.Layer#BACKGROUND}</li>
     *                  <li>{@link UI.Layer#CONTENT}</li>
     *                  <li>{@link UI.Layer#BORDER}</li>
     *                  <li>{@link UI.Layer#FOREGROUND}</li>
     *              </ul>
     * @param clipArea The area to which the painting should be confined. Paint operations outside of this area will be clipped away.
     *                 The following areas are available:
     *                 <ul>
     *                      <li>{@link UI.ComponentArea#ALL}</li>
     *                      <li>{@link UI.ComponentArea#EXTERIOR}</li>
     *                      <li>{@link UI.ComponentArea#BORDER}</li>
     *                      <li>{@link UI.ComponentArea#INTERIOR}</li>
     *                      <li>{@link UI.ComponentArea#BODY}</li>
     *                 </ul>
     * @param painter A custom painter, which receives the {@link Graphics2D} instance of the current component.
     * @return A new {@link ComponentStyleDelegate} with the provided background renderer.
     */
    public ComponentStyleDelegate<C> painter(
        UI.Layer              layer,
        UI.ComponentArea      clipArea,
        swingtree.api.Painter painter
    ) {
        return _withStyle(_styleConf.painter(layer, clipArea, StyleUtil.DEFAULT_KEY, painter));
    }


    /**
     *  Returns a new {@link StyleConf} with the provided named {@link swingtree.api.Painter}, which
     *  will be called using the {@link Graphics2D} instance of the current component.
     *  You may use this to render custom styles for the component... <br>
     *  The name can be used to override {@link swingtree.api.Painter} instances with that same name
     *  or use a unique name to ensure that you style is not overridden by another style.
     *  This allows you to attach an arbitrary number of custom painters to a component.
     *  <br><br>
     *  <b>
     *    Note that your custom painter will yield the best performance if they are value based in the sense that
     *    they have {@link Object#hashCode()} and {@link Object#equals(Object)} implementation which
     *    are based on the data that the painter uses to render the component.
     *    This is because it allows SwingTree to cache the rendering of the painters and avoid unnecessary repaints. <br>
     *    If you do not want to create a custom class just for painting but instead
     *    just want to pass an immutable cache key to a painter, then consider using the
     *    {@link Painter#of(Object, Painter)} factory method to create a painter that has the
     *    with {@link Object#hashCode()} and {@link Object#equals(Object)} implemented
     *    based on the provided cache key.
     *  </b>
     *
     * @param layer The layer on which the painter should do its work.
     *              It is an enum instance which
     *              gives the painter a particular rank in the painting order.
     *              So the {@link swingtree.UI.Layer#BACKGROUND} will be painted first,
     *              followed by the {@link swingtree.UI.Layer#CONTENT} and so on...
     *              <br>
     *              The following layers are available:
     *              <ul>
     *                  <li>{@link UI.Layer#BACKGROUND}</li>
     *                  <li>{@link UI.Layer#CONTENT}</li>
     *                  <li>{@link UI.Layer#BORDER}</li>
     *                  <li>{@link UI.Layer#FOREGROUND}</li>
     *              </ul>
     * @param painterName The name of the painter.
     * @param painter The custom painter lambda to which the {@link Graphics2D} instance of the current component will be passed.
     * @return A new {@link ComponentStyleDelegate} with the provided background renderer.
     */
    public ComponentStyleDelegate<C> painter( UI.Layer layer, String painterName, swingtree.api.Painter painter ) {
        return _withStyle(_styleConf.painter(layer, UI.ComponentArea.INTERIOR, painterName, painter));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided named {@link swingtree.api.Painter}, which
     *  will be called using the {@link Graphics2D} instance of the current component.
     *  You may use this to render custom styles for the component... <br>
     *  The name can be used to override {@link swingtree.api.Painter} instances with that same name
     *  or use a unique name to ensure that you style is not overridden by another style.
     *  This allows you to attach an arbitrary number of custom painters to a component.
     *  <br><br>
     *  <b>
     *    Note that your custom painter will yield the best performance if they are value based in the sense that
     *    they have {@link Object#hashCode()} and {@link Object#equals(Object)} implementation which
     *    are based on the data that the painter uses to render the component.
     *    This is because it allows SwingTree to cache the rendering of the painters and avoid unnecessary repaints. <br>
     *    If you do not want to create a custom class just for painting but instead
     *    just want to pass an immutable cache key to a painter, then consider using the
     *    {@link Painter#of(Object, Painter)} factory method to create a painter that has the
     *    with {@link Object#hashCode()} and {@link Object#equals(Object)} implemented
     *    based on the provided cache key.
     *  </b>
     *
     * @param layer The layer on which the painter should do its work.
     *              It is an enum instance which
     *              gives the painter a particular rank in the painting order.
     *              So the {@link swingtree.UI.Layer#BACKGROUND} will be painted first,
     *              followed by the {@link swingtree.UI.Layer#CONTENT} and so on...
     *              <br>
     *              The following layers are available:
     *              <ul>
     *                  <li>{@link UI.Layer#BACKGROUND}</li>
     *                  <li>{@link UI.Layer#CONTENT}</li>
     *                  <li>{@link UI.Layer#BORDER}</li>
     *                  <li>{@link UI.Layer#FOREGROUND}</li>
     *              </ul>
     * @param clipArea The area to which the painting should be confined. Paint operations outside of this area will be clipped away.
     *                 The following areas are available:
     *                 <ul>
     *                      <li>{@link UI.ComponentArea#ALL}</li>
     *                      <li>{@link UI.ComponentArea#EXTERIOR}</li>
     *                      <li>{@link UI.ComponentArea#BORDER}</li>
     *                      <li>{@link UI.ComponentArea#INTERIOR}</li>
     *                      <li>{@link UI.ComponentArea#BODY}</li>
     *                 </ul>
     * @param painterName The name of the painter.
     * @param painter The custom painter lambda to which the {@link Graphics2D} instance of the current component will be passed.
     * @return A new {@link ComponentStyleDelegate} with the provided background renderer.
     */
    public ComponentStyleDelegate<C> painter(
        UI.Layer              layer,
        UI.ComponentArea      clipArea,
        String                painterName,
        swingtree.api.Painter painter
    ) {
        return _withStyle(_styleConf.painter(layer, clipArea, painterName, painter));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided foreground color.
     *
     * @param color The foreground color.
     * @return A new {@link ComponentStyleDelegate} with the provided foreground color.
     */
    public ComponentStyleDelegate<C> foregroundColor( Color color ) {
        Objects.requireNonNull(color, "Use 'UI.Color.UNDEFINED' instead of 'null'.");
        return _withStyle(_styleConf._withBase(_styleConf.base().foregroundColor(color)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided foreground color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *
     * @param colorString The foreground color.
     * @return A new {@link ComponentStyleDelegate} with the provided foreground color.
     */
    public ComponentStyleDelegate<C> foregroundColor( String colorString ) {
        Objects.requireNonNull(colorString);
        Color newColor;
        try {
            newColor = UI.color(colorString);
        } catch ( Exception e ) {
            log.error("Failed to parse color string: '"+colorString+"'", e);
            return this;
        }
        return _withStyle(_styleConf._withBase(_styleConf.base().foregroundColor(newColor)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided horizontal shadow offset applied to all shadow configs.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link StyleConf}.
     *  Note that in order to see the shadow, you may also need to call {@link #shadowSpreadRadius(double)},
     *  {@link #shadowBlurRadius(double)} and {@link #shadowColor(Color)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Configurator)}.
     *
     * @param offset The shadow offset in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided horizontal shadow offset.
     */
    public ComponentStyleDelegate<C> shadowHorizontalOffset( double offset ) {
        return _withStyle(_styleConf._withShadow(shadow -> shadow.horizontalOffset((float) offset)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided vertical shadow offset applied to all shadow configs.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link StyleConf}.
     *  Note that in order to see the shadow, you may also need to call {@link #shadowSpreadRadius(double)},
     *  {@link #shadowBlurRadius(double)} and {@link #shadowColor(Color)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Configurator)}.
     *
     * @param offset The shadow offset in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided vertical shadow offset.
     */
    public ComponentStyleDelegate<C> shadowVerticalOffset( double offset ) {
        return _withStyle(_styleConf._withShadow(shadow -> shadow.verticalOffset((float) offset)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided shadow offset applied to all shadow configs.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link StyleConf}.
     *  Note that in order to see the shadow, you may also need to call {@link #shadowSpreadRadius(double)},
     *  {@link #shadowBlurRadius(double)} and {@link #shadowColor(Color)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Configurator)}.
     *
     * @param horizontalOffset The horizontal shadow offset in pixels.
     * @param verticalOffset The vertical shadow offset in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided shadow offset.
     */
    public ComponentStyleDelegate<C> shadowOffset( double horizontalOffset, double verticalOffset ) {
        return _withStyle(_styleConf._withShadow(shadow -> shadow.horizontalOffset((float) horizontalOffset).verticalOffset((float) verticalOffset)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided horizontal and vertical shadow offset.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link StyleConf}.
     *  Note that in order to see the shadow, you may also need to call {@link #shadowSpreadRadius(double)},
     *  {@link #shadowBlurRadius(double)} and {@link #shadowColor(Color)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Configurator)}.
     *
     * @param horizontalAndVerticalOffset The horizontal and vertical shadow offset in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided shadow offset.
     */
    public ComponentStyleDelegate<C> shadowOffset( double horizontalAndVerticalOffset ) {
        return _withStyle(_styleConf._withShadow(shadow -> shadow.horizontalOffset((float) horizontalAndVerticalOffset).verticalOffset((float) horizontalAndVerticalOffset)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided shadow blur radius applied to all shadow configs.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link StyleConf}.
     *  Note that in order to see the shadow, you may also need to call
     *  {@link #shadowSpreadRadius(double)} and {@link #shadowColor(Color)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Configurator)}.
     *
     * @param radius The shadow blur radius in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided shadow blur radius.
     */
    public ComponentStyleDelegate<C> shadowBlurRadius( double radius ) {
        return _withStyle(_styleConf._withShadow(shadow -> shadow.blurRadius((float) radius)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided shadow spread radius applied to all shadow configs.
     *  The shadow will be rendered with an inset space based on the padding defined by this {@link StyleConf}.
     *  Note that in order to see the shadow, you may also need to call
     *  {@link #shadowBlurRadius(double)} and {@link #shadowColor(Color)}. <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Configurator)}.
     *
     * @param radius The shadow spread radius in pixels.
     * @return A new {@link ComponentStyleDelegate} with the provided shadow spread radius.
     */
    public ComponentStyleDelegate<C> shadowSpreadRadius( double radius ) {
        return _withStyle(_styleConf._withShadow(shadow -> shadow.spreadRadius((float) radius)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided shadow color applied to the default shadow.
     *  Note that in order to see the shadow, you may also need to call
     *  {@link #shadowBlurRadius(double)} and {@link #shadowSpreadRadius(double)}. <br>
     *  The shadow will be rendered on the {@link UI.Layer#CONTENT} layer,
     *  if you want it to be rendered on a different layer, you
     *  may want to take a look at {@link #shadow(UI.Layer, String, Configurator)}. <br>
     *  <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Configurator)}
     *  (and which are also rendered on the {@link UI.Layer#CONTENT} layer).
     *
     * @param color The shadow color.
     * @return A new {@link ComponentStyleDelegate} with the provided shadow color.
     */
    public ComponentStyleDelegate<C> shadowColor( Color color ) {
        return _withStyle(_styleConf._withShadow(ShadowConf.DEFAULT_LAYER, shadow -> shadow.color(color)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided shadow color in the form of a string.
     *  The string can be either a hex color string, a color name or a color constant from the system properties.
     *  Note that in order to see the shadow, you may also need to call
     *  {@link #shadowBlurRadius(double)} and {@link #shadowSpreadRadius(double)}. <br>
     *  The shadow will be rendered on the {@link UI.Layer#CONTENT} layer,
     *  if you want it to be rendered on a different layer, you
     *  may want to take a look at {@link #shadow(UI.Layer, String, Configurator)}. <br>
     *  <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Configurator)}
     *  (and which are also rendered on the {@link UI.Layer#CONTENT} layer).
     *
     * @param colorString The shadow color.
     * @return A new {@link ComponentStyleDelegate} with the provided shadow color.
     */
    public ComponentStyleDelegate<C> shadowColor( String colorString ) {
        Objects.requireNonNull(colorString);
        Color newColor;
        try {
            newColor = UI.color(colorString);
        } catch ( Exception e ) {
            log.error("Failed to parse color string: '"+colorString+"'", e);
            return this;
        }
        return _withStyle(_styleConf._withShadow(ShadowConf.DEFAULT_LAYER, shadow -> shadow.color(newColor)));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided shadow color
     *  defined by the supplied red, green and blue color channels in
     *  the form of doubles expected to be in the range of 0.0 to 1.0.
     *  Note that in order to see the shadow, you may also need to call
     *  {@link #shadowBlurRadius(double)} and {@link #shadowSpreadRadius(double)}. <br>
     *  The shadow will be rendered on the {@link UI.Layer#CONTENT} layer,
     *  if you want it to be rendered on a different layer, you
     *  may want to take a look at {@link #shadow(UI.Layer, String, Configurator)}. <br>
     *  <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Configurator)}
     *  (and which are also rendered on the {@link UI.Layer#CONTENT} layer).
     *
     * @param r The red component of the shadow color in the range of 0.0 to 1.0.
     * @param g The green component of the shadow color in the range of 0.0 to 1.0.
     * @param b The blue component of the shadow color in the range of 0.0 to 1.0.
     * @return A new {@link ComponentStyleDelegate} with the provided shadow color.
     */
    public ComponentStyleDelegate<C> shadowColor( double r, double g, double b ) {
        return shadowColor(new Color((float) r, (float) g, (float) b));
    }

    /**
     *  Returns a new {@link StyleConf} with the provided shadow color
     *  defined by the supplied red, green, blue and alpha color channels in
     *  the form of doubles expected to be in the range of 0.0 to 1.0.
     *  Note that in order to see the shadow, you may also need to call
     *  {@link #shadowBlurRadius(double)} and {@link #shadowSpreadRadius(double)}. <br>
     *  The shadow will be rendered on the {@link UI.Layer#CONTENT} layer,
     *  if you want it to be rendered on a different layer, you
     *  may want to take a look at {@link #shadow(UI.Layer, String, Configurator)}. <br>
     *  <br>
     *  Note that this property will not only be applied to the default shadow, but also any
     *  other named shadow that you may have defined using {@link #shadow(String, Configurator)}
     *  (and which are also rendered on the {@link UI.Layer#CONTENT} layer).
     *
     * @param r The red component of the shadow color in the range of 0.0 to 1.0.
     * @param g The green component of the shadow color in the range of 0.0 to 1.0.
     * @param b The blue component of the shadow color in the range of 0.0 to 1.0.
     * @param a The alpha component of the shadow color in the range of 0.0 to 1.0.
     * @return A new {@link ComponentStyleDelegate} with the provided shadow color.
     */
    public ComponentStyleDelegate<C> shadowColor( double r, double g, double b, double a ) {
        return shadowColor(new Color((float) r, (float) g, (float) b, (float) a));
    }

    /**
     *  Use this to control whether your shadows should be rendered inwards or outwards. <br>
     *  Note that this property will be applied to all shadow effects of all layers, including
     *  the default shadow and named shadows defined using {@link #shadow(String, Configurator)}. <br>
     *  The default value is {@code false}.
     *
     * @param inwards Whether the shadow should be rendered inwards or outwards.
     * @return A new {@link ComponentStyleDelegate} with the provided shadow inset flag.
     */
    public ComponentStyleDelegate<C> shadowIsInset( boolean inwards ) {
        return _withStyle(_styleConf._withShadow(shadow -> shadow.isInset(inwards)));
    }

    /**
     *  This method makes it possible to define multiple shadows for a single component
     *  on the {@link UI.Layer#CONTENT} layer, by giving the shadow config a unique name.
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
     *  Note that the shadows will be rendered in
     *  alphabetical order based on their name (within a particular layer).
     *
     * @param shadowName The name of the shadow.
     * @param styler A function that takes a {@link ShadowConf} and returns a new {@link ShadowConf}.
     * @return A new {@link ComponentStyleDelegate} with a named shadow defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> shadow( String shadowName, Configurator<ShadowConf> styler ) {
        return shadow(ShadowConf.DEFAULT_LAYER, shadowName, styler);
    }

    /**
     *  This method makes it possible to define multiple shadows for a single component
     *  on a custom layer, by giving the shadow config a unique name.
     *  This is useful when you want to do advanced shadow effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *      UI.panel()
     *      .withStyle( it -> it
     *          .shadow(UI.Layer.CONTENT, "dark shading", shadow -> shadow
     *              .color("#000000")
     *              .horizontalOffset(5)
     *              .verticalOffset(5)
     *              .blurRadius(10)
     *              .spreadRadius(0)
     *          )
     *          .shadow(UI.Layer.CONTENT, "light shading", shadow -> shadow
     *              .color("#ffffff")
     *              .horizontalOffset(-5)
     *              .verticalOffset(-5)
     *              .blurRadius(10)
     *              .spreadRadius(0)
     *          )
     *  }</pre>
     *  Note that the shadows will be rendered in
     *  alphabetical order based on their name (within a particular layer).
     *
     * @param layer The layer of the shadow is an enum instance which
     *              gives the shadow effect a rank in the painting order.
     *              So the {@link swingtree.UI.Layer#BACKGROUND} will be painted first,
     *              followed by the {@link swingtree.UI.Layer#CONTENT} and so on...
     *              <br>
     *              The following layers are available:
     *              <ul>
     *                  <li>{@link UI.Layer#BACKGROUND}</li>
     *                  <li>{@link UI.Layer#CONTENT}</li>
     *                  <li>{@link UI.Layer#BORDER}</li>
     *                  <li>{@link UI.Layer#FOREGROUND}</li>
     *              </ul>
     * @param shadowName The name of the shadow.
     * @param styler A function that takes a {@link ShadowConf} and returns a new {@link ShadowConf}.
     * @return A new {@link ComponentStyleDelegate} with a named shadow defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> shadow(
        UI.Layer layer,
        String shadowName,
        Configurator<ShadowConf> styler
    ) {
        Objects.requireNonNull(shadowName);
        Objects.requireNonNull(styler);
        ShadowConf shadow = Optional.ofNullable(_styleConf.shadow(layer, shadowName)).orElse(ShadowConf.none());
        // We clone the shadow map:
        NamedConfigs<ShadowConf> newShadows = _styleConf.shadowsMap(layer).withNamedStyle(shadowName, styler.configure(shadow));
        return _withStyle(_styleConf._withShadow(layer, newShadows));
    }

    /**
     *  This method makes it possible to define multiple background gradient for a single component
     *  on the {@link UI.Layer#BACKGROUND} layer, by giving the gradient config a unique name.
     *  This is useful when you want to do advanced background effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it -> it
     *      .gradient("dark shading", conf -> conf
     *        .colors("#000000", "#000000")
     *        .transition(UI.Transition.TOP_TO_BOTTOM)
     *      )
     *      .gradient("light shading", conf -> conf
     *        .colors("#ffffff", "#ffffff")
     *        .transition(UI.Transition.TOP_TO_BOTTOM))
     *      )
     *    )
     * }</pre>
     * Note that the background shades will be rendered in alphabetical order based on the name of the shade.<br>
     * This method translates to {@link #gradient(UI.Layer, String, Configurator)} but with the
     * layer set to {@link UI.Layer#BACKGROUND}.
     *
     * @param shadeName The name of the background shade.
     * @param styler A function that takes a {@link GradientConf} and returns a new {@link GradientConf}.
     * @return A new {@link ComponentStyleDelegate} with a named background shade defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> gradient( String shadeName, Configurator<GradientConf> styler ) {
        return gradient(GradientConf.DEFAULT_LAYER, shadeName, styler);
    }

    /**
     *  This method makes it possible to define multiple background gradient for a single component
     *  on a particular layer, by giving the gradient config a unique name.
     *  This is useful when you want to do advanced background effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it -> it
     *      .gradient(UI.Layer.BACKGROUND, "dark shading", conf -> conf
     *        .colors("#000000", "#000000")
     *        .transition(UI.Transition.TOP_TO_BOTTOM)
     *      )
     *      .gradient(UI.Layer.BACKGROUND, "light shading", conf -> conf
     *        .colors("#ffffff", "#ffffff")
     *        .transition(UI.Transition.TOP_TO_BOTTOM))
     *      )
     *    )
     * }</pre>
     * Note that within a particular layer the gradients will be rendered in alphabetical order
     * based on the provided name.
     *
     * @param layer The layer on which the gradient should be rendered.
     * @param shadeName The name of the background shade.
     * @param styler A function that takes a {@link GradientConf} and returns a new {@link GradientConf}.
     * @return A new {@link ComponentStyleDelegate} with a named background shade defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> gradient(
        UI.Layer layer,
        String shadeName,
        Configurator<GradientConf> styler
    ) {
        Objects.requireNonNull(shadeName);
        Objects.requireNonNull(styler);
        return _withStyle(_styleConf.gradient(layer, shadeName, styler));
    }

    /**
     *  This method makes it possible to define a background shade for your components.
     *  This is useful when you want to do advanced background effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it -> it
     *        .gradient( conf -> conf
     *            .colors("#000000", "#000000")
     *            .transition(UI.Transition.TOP_TO_BOTTOM)
     *        )
     *    )
     * }</pre>
     * This method translates to {@link #gradient(UI.Layer, String, Configurator)} but with the
     * layer set to {@link UI.Layer#BACKGROUND} and the name being the "default" style name
     *
     * @param styler A function that takes a {@link GradientConf} and returns a new {@link GradientConf}.
     * @return A new {@link ComponentStyleDelegate} with a background shade defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> gradient( Configurator<GradientConf> styler ) {
        return gradient(GradientConf.DEFAULT_LAYER, StyleUtil.DEFAULT_KEY, styler);
    }

    /**
     *  This method makes it possible to define a gradient effect on a particular layer for your components.
     *  This is useful when you want to do advanced background effects, such as neumorphism a.k.a. soft UI. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it -> it
     *        .gradient(UI.Layer.BACKGROUND, conf -> conf
     *            .colors("#000000", "#000000")
     *            .transition(UI.Transition.TOP_TO_BOTTOM)
     *        )
     *    )
     * }</pre>
     * Note that this method translates to {@link #gradient(UI.Layer, String, Configurator)} but with the
     * name being the "default" style name.
     *
     * @param layer The layer on which the gradient should be rendered.
     * @param styler A function that takes a {@link GradientConf} and returns a new {@link GradientConf}.
     * @return A new {@link ComponentStyleDelegate} with a background shade defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> gradient( UI.Layer layer, Configurator<GradientConf> styler ) {
        Objects.requireNonNull(styler);
        return _withStyle(_styleConf.gradient(layer, StyleUtil.DEFAULT_KEY, styler));
    }

    /**
     *  This method makes it possible to define a background noise for your components.
     *  This is useful when you want to give component surfaces some naturally looking texture
     *  or special effects. <br>
     *  <br>
     *  Here is an example of how to use it:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it -> it
     *        .noise("my-noise" conf -> conf
     *            .scale(2, 3).rotation(45)
     *            .colors(Color.BLACK, Color.WHITE)
     *            .offset(64,85)
     *        )
     *    )
     * }</pre>
     * Note that this method translates to {@link #noise(UI.Layer, String, Configurator)} but with the
     * layer set to {@link UI.Layer#BACKGROUND}.
     *
     * @param noiseName The name of the noise which is used to create, identify and possibly override a noise with the same name.
     * @param styler A function that takes a {@link NoiseConf} and returns a new {@link NoiseConf}.
     * @return A new {@link ComponentStyleDelegate} with a background noise defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> noise( String noiseName, Configurator<NoiseConf> styler ) {
        Objects.requireNonNull(noiseName);
        Objects.requireNonNull(styler);
        return noise(NoiseConf.DEFAULT_LAYER, noiseName, styler);
    }

    /**
     *  This method makes it possible to define a background noise for your components.
     *  This is useful when you want to give component surfaces some naturally looking texture
     *  or special effects. <br>
     *  <br>
     *  Here is an example of how to use the method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it -> it
     *        .noise(UI.Layer.BACKGROUND, "my-noise" conf -> conf
     *            .scale(2, 3).rotation(45)
     *            .colors(Color.BLACK, Color.WHITE)
     *            .offset(64,85)
     *        )
     *    )
     * }</pre>
     *
     * @param layer The layer on which the noise should be rendered.
     * @param styler A function that takes a {@link NoiseConf} and returns a new {@link NoiseConf}.
     * @return A new {@link ComponentStyleDelegate} with a background noise defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> noise( UI.Layer layer, String noiseName, Configurator<NoiseConf> styler ) {
        Objects.requireNonNull(noiseName);
        Objects.requireNonNull(styler);
        return _withStyle(_styleConf.noise(layer, noiseName, styler));
    }

    /**
     *  This method makes it possible to define a background noise for your components.
     *  This is useful when you want to give component surfaces some naturally looking texture
     *  or special effects. <br>
     *  <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it -> it
     *        .noise( conf -> conf
     *            .scale(2, 3).rotation(45)
     *            .colors(Color.BLACK, Color.WHITE)
     *            .offset(64,85)
     *        )
     *    )
     * }</pre>
     * Note that this method translates to {@link #noise(UI.Layer, String, Configurator)} but with the
     * layer set to {@link UI.Layer#BACKGROUND} and the name being the "default" style name.
     *
     * @param styler A function that takes a {@link NoiseConf} and returns a new {@link NoiseConf}.
     * @return A new {@link ComponentStyleDelegate} with a background noise defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> noise( Configurator<NoiseConf> styler ) {
        Objects.requireNonNull(styler);
        return noise(NoiseConf.DEFAULT_LAYER, StyleUtil.DEFAULT_KEY, styler);
    }

    /**
     *  This method makes it possible to define multiple background styles for a single component
     *  rendered on the {@link UI.Layer#BACKGROUND} layer, by giving the background config a unique name.
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
     * @param styler A function that takes a {@link ImageConf} and returns a new {@link ImageConf}.
     * @return A new {@link ComponentStyleDelegate} with a named background image defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> image( String imageName, Configurator<ImageConf> styler ) {
        Objects.requireNonNull(imageName);
        Objects.requireNonNull(styler);
        return image(ImageConf.DEFAULT_LAYER, imageName, styler);
    }

    /**
     *  This method makes it possible to define multiple background styles for a single component
     *  rendered on a particular layer, by giving the background config a unique name.
     *  This is useful when you want to do advanced layer backgrounds
     *  displaying multiple images on top of each other. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it -> it
     *      .image(UI.Layer.BACKGROUND, "ground 1", image -> image
     *        .image(loadImageFrom("my/path/to/image1.png"))
     *      )
     *      .ground(UI.Layer.BACKGROUND, "ground 2", ground -> ground
     *        .color("blue")
     *      )
     *    )
     * }</pre>
     * Note that the background images will be rendered in alphabetical order based on the name of the image.
     *
     * @param layer The layer defines at which step in the rendering process the image should be rendered.
     *              The default layer is the background layer, which will be rendered first.
     *              Here a list of available layers:
     *              <ul>
     *                  <li>{@link swingtree.UI.Layer#BACKGROUND}</li>
     *                  <li>{@link swingtree.UI.Layer#CONTENT}</li>
     *                  <li>{@link swingtree.UI.Layer#BORDER}</li>
     *                  <li>{@link swingtree.UI.Layer#FOREGROUND}</li>
     *              </ul>
     * @param imageName The name of the background image.
     * @param styler A function that takes a {@link ImageConf} and returns a new {@link ImageConf}.
     * @return A new {@link ComponentStyleDelegate} with a named background image defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> image( UI.Layer layer, String imageName, Configurator<ImageConf> styler ) {
        Objects.requireNonNull(imageName);
        Objects.requireNonNull(styler);
        return _withStyle(_styleConf.images(layer, imageName, styler));
    }

    /**
     *  Allows for the rendering of a background image on your components.
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
     * Note that this method translates to {@link #image(UI.Layer, String, Configurator)} but with the
     * layer set to {@link UI.Layer#BACKGROUND} and the name being the "default" style name.
     *
     * @param styler A function that takes a {@link ImageConf} and returns a new {@link ImageConf}.
     * @return A new {@link ComponentStyleDelegate} with a background image defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> image( Configurator<ImageConf> styler ) {
        Objects.requireNonNull(styler);
        return image(ImageConf.DEFAULT_LAYER, StyleUtil.DEFAULT_KEY, styler);
    }

    /**
     *  Allows for the rendering of an image on a particular component layer.
     *  This is useful when you want to do advanced layer backgrounds
     *  displaying multiple images on top of each other. <br>
     *  Here is an example of how to use this method:
     *  <pre>{@code
     *    UI.panel()
     *    .withStyle( it -> it
     *        .image(UI.Layer.CONTENT, image -> image
     *            .image(loadImageFrom("my/path/to/image1.png"))
     *            .color("green")
     *        )
     *    )
     * }</pre>
     * Note that this method translates to {@link #image(UI.Layer, String, Configurator)} but with the
     * name being the "default" style name.
     *
     * @param layer The layer on which the image should be rendered.
     * @param styler A function that takes a {@link ImageConf} and returns a new {@link ImageConf}.
     * @return A new {@link ComponentStyleDelegate} with a background image defined by the provided styler lambda.
     */
    public ComponentStyleDelegate<C> image( UI.Layer layer, Configurator<ImageConf> styler ) {
        Objects.requireNonNull(layer);
        Objects.requireNonNull(styler);
        return _withStyle(_styleConf.images(layer, StyleUtil.DEFAULT_KEY, styler));
    }

    /**
     *  Returns an updated {@link StyleConf} with a named text style
     *  configurator for the default {@link TextConf} of the component. <br>
     *  The sub-style exposed by this method adds <b>support for text rendering to
     *  all components not just text components</b>. <br>
     *  If you only want to style the {@link JComponent#getFont()} property of the component,
     *  you can use {@link #componentFont(Configurator)} instead. <br>
     *  <br>
     *  The first parameter is the name of the text style, which allows
     *  you to define any number of text styles for a single component
     *  by using different names. <br>
     *  Two sub-styles with the same name will override each other. <br>
     *
     * @param styler A configurator function that takes a {@link TextConf} and returns an updated {@link TextConf}.
     *               The configurator function is called with the default text style of the component.
     * @return A new {@link ComponentStyleDelegate} with the provided text style.
     *         Each unique name creates an additional text style for the component.
     * @see #text(UI.Layer, String, Configurator)
     * @see #text(Configurator)
     * @throws NullPointerException If the provided styler or textName is {@code null}.
     */
    public ComponentStyleDelegate<C> text( String textName, Configurator<TextConf> styler ) {
        Objects.requireNonNull(textName);
        Objects.requireNonNull(styler);
        return text(TextConf.DEFAULT_LAYER, textName, styler);
    }

    /**
     *  Returns an updated {@link StyleConf} with the provided named text style
     *  configurator for the default {@link TextConf} of the component. <br>
     *  The sub-style exposed by this method adds <b>support for text rendering to
     *  all components not just text components</b>. <br>
     *  If you only want to style the {@link JComponent#getFont()} property of the component,
     *  you can use {@link #componentFont(Configurator)} instead. <br>
     *  <br>
     *  The first parameter is the name of the text style, which allows
     *  you to define any number of text styles for a single component
     *  by using different names. <br>
     *  Two sub-styles with the same name will override each other. <br>
     *
     * @param layer The layer on which the text should be rendered.
     * @param textName The name of the text style that you want to define.
     *                 Each unique name creates an additional text style for the component.
     * @param styler A configurator function that takes a {@link TextConf} and returns an updated {@link TextConf}.
     *               The configurator function is called with the default text style of the component.
     * @return A new {@link ComponentStyleDelegate} with the provided text style.
     * @see #text(String, Configurator)
     * @see #text(Configurator)
     * @throws NullPointerException If the provided styler or textName is {@code null}.
     */
    public ComponentStyleDelegate<C> text( UI.Layer layer, String textName, Configurator<TextConf> styler ) {
        Objects.requireNonNull(textName);
        Objects.requireNonNull(styler);
        return _withStyle(_styleConf.text(layer, textName, styler));
    }

    /**
     *  Returns an updated {@link StyleConf} with the provided text style
     *  configurator for the default {@link TextConf} of the component. <br>
     *  The sub-style exposed by this method adds <b>support for text rendering to
     *  all components not just text components</b>. <br>
     *  If you only want to style the {@link JComponent#getFont()} property of the component,
     *  you can use {@link #componentFont(Configurator)} instead. <br>
     *
     * @param styler A configurator function that takes a {@link TextConf} and returns an updated {@link TextConf}.
     *               The configurator function is called with the default text style of the component.
     * @return A new {@link ComponentStyleDelegate} with the provided text style.
     * @see #text(UI.Layer, String, Configurator)
     * @see #text(String, Configurator)
     * @throws NullPointerException If the provided styler is {@code null}.
     */
    public ComponentStyleDelegate<C> text( Configurator<TextConf> styler ) {
        Objects.requireNonNull(styler);
        return text(TextConf.DEFAULT_LAYER, StyleUtil.DEFAULT_KEY, styler);
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
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        return _withStyle(_styleConf.property(key, value));
    }

    public ComponentStyleDelegate<C> parentFilter( Configurator<FilterConf> filterStyler ) {
        Objects.requireNonNull(filterStyler);
        return _withStyle(_styleConf._withLayers(_styleConf.layers().filter(filterStyler)));
    }

    private ComponentStyleDelegate<C> _withFont( Configurator<FontConf> fontStyler ) {
        Objects.requireNonNull(fontStyler);
        StyleConf updatedStyle = _styleConf._withFont(fontStyler.configure(_styleConf.font()));
        // We also update the text style, if it exists:
        updatedStyle = updatedStyle.text( text -> text.font(fontStyler) );
        return _withStyle(updatedStyle);
    }

    /**
     *  Returns a new {@link StyleConf} with the provided font style applied to the
     *  font property of the component (see {@link JComponent#getFont()}). <br>
     *  If you want to style the text of the entire component, which includes both
     *  the component font property as well as the style engine based font render
     *  (see {@link #text(String, Configurator)}), you can simply
     *  call the regular font styling methods such as {@link #font(String, int)},
     *  {@link #font(Font)}, {@link #fontFamily(String)}, {@link #fontSize(int)},
     *  {@link #fontBold(boolean)}, {@link #fontItalic(boolean)}, {@link #fontUnderline(boolean)}...
     *
     * @param fontStyler A function that takes a {@link FontConf} and returns a new {@link FontConf}
     *                   that is exclusively applied to the font property of the component.
     * @return A new {@link ComponentStyleDelegate} with the provided font style
     *          applied to the font property of the component.
     */
    public final ComponentStyleDelegate<C> componentFont( Configurator<FontConf> fontStyler ) {
        Objects.requireNonNull(fontStyler);
        StyleConf updatedStyle = _styleConf._withFont(fontStyler.configure(_styleConf.font()));
        return _withStyle(updatedStyle);
    }

    /**
     *  Returns a new {@link StyleConf} with the provided font name and size.<br>
     *  Note that the font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}).<br>
     *  <p>
     *  Note that this font style will be applied to both the component font property
     *  and the style engine based text (see {@link #text(String, Configurator)}).
     *  If you only want to style the component font property, you can use
     *  {@link #componentFont(Configurator)}.
     *
     * @param name The font name.
     * @param size The font size.
     * @return A new {@link ComponentStyleDelegate} with the provided font name and size.
     */
    public ComponentStyleDelegate<C> font( String name, int size ) {
        Objects.requireNonNull(name);
        return _withFont( f -> f.family(name).size(size) );
    }

    /**
     *  Returns a new {@link StyleConf} with the provided font family name.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}).<br>
     *  <p>
     *  Note that this font style will be applied to both the component font property
     *  and the style engine based text (see {@link #text(String, Configurator)}).
     *  If you only want to style the component font property, you can use
     *  {@link #componentFont(Configurator)}.
     *
     * @param name The font name.
     * @return A new {@link ComponentStyleDelegate} with the provided font name.
     */
    public ComponentStyleDelegate<C> fontFamily( String name ) {
        Objects.requireNonNull(name);
        return _withFont( f -> f.family(name) );
    }

    /**
     *  Returns a new {@link StyleConf} with the provided {@link Font}.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}).<br>
     *  <p>
     *  Note that this font style will be applied to both the component font property
     *  and the style engine based text (see {@link #text(String, Configurator)}).
     *  If you only want to style the component font property, you can use
     *  {@link #componentFont(Configurator)}.
     *
     * @param font The {@link Font}.
     * @return A new {@link ComponentStyleDelegate} with the provided {@link Font}.
     * @throws NullPointerException If the font is {@code null}.
     *         Use {@link UI.Font#UNDEFINED} to remove the font style.
     */
    public ComponentStyleDelegate<C> font( Font font ) {
        Objects.requireNonNull(font, "The font cannot be null! Use UI.FONT_UNDEFINED to remove the font style.");
        return _withFont( f -> f.withPropertiesFromFont(font) );
    }

    /**
     *  Returns a new {@link StyleConf} with the provided font size.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}).<br>
     *  <p>
     *  Note that this font style will be applied to both the component font property
     *  and the style engine based text (see {@link #text(String, Configurator)}).
     *  If you only want to style the component font property, you can use
     *  {@link #componentFont(Configurator)}.
     *
     * @param size The font size.
     * @return A new {@link ComponentStyleDelegate} with the provided font size.
     */
    public ComponentStyleDelegate<C> fontSize( int size ) {
        return _withFont( f -> f.size(size) );
    }

    /**
     *  Makes the font bold or not bold depending on the value of the {@code isBold} parameter.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}).<br>
     *  <p>
     *  Note that this font style will be applied to both the component font property
     *  and the style engine based text (see {@link #text(String, Configurator)}).
     *  If you only want to style the component font property, you can use
     *  {@link #componentFont(Configurator)}.
     *
     * @param bold Whether the font should be bold or not.
     * @return A new {@link ComponentStyleDelegate} with the provided font boldness.
     */
    public ComponentStyleDelegate<C> fontBold( boolean bold ) {
        return _withFont( f -> f.weight( bold ? 2 : 1 ) );
    }

    /**
     *  Makes the font italic or not italic depending on the value of the {@code italic} parameter.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}).<br>
     *  <p>
     *  Note that this font style will be applied to both the component font property
     *  and the style engine based text (see {@link #text(String, Configurator)}).
     *  If you only want to style the component font property, you can use
     *  {@link #componentFont(Configurator)}.
     *
     * @param italic Whether the font should be italic or not.
     * @return A new {@link ComponentStyleDelegate} with the provided font italicness.
     */
    public ComponentStyleDelegate<C> fontItalic( boolean italic ) {
        return _withFont( f -> f.posture( italic ? 0.2f : 0f ) );
    }

    /**
     *  Determines if the font should be plain, bold, italic or bold and italic
     *  based on the provided {@link UI.FontStyle} parameter,
     *  which may be {@link UI.FontStyle#PLAIN}, {@link UI.FontStyle#BOLD},
     *  {@link UI.FontStyle#ITALIC} or {@link UI.FontStyle#BOLD_ITALIC}.<br>
     *  <b>
     *      Note that this will override any previous bold or italic settings.
     *  </b>
     * @param style The font style in form of a {@link UI.FontStyle}.
     * @return An updated {@link ComponentStyleDelegate} with the provided font style.
     */
    public ComponentStyleDelegate<C> fontStyle( UI.FontStyle style ) {
        return _withFont( f -> f.style(style) );
    }

    /**
     *  Makes the font underlined or not underlined depending on the value of the {@code underline} parameter.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}).<br>
     *  <p>
     *  Note that this font style will be applied to both the component font property
     *  and the style engine based text (see {@link #text(String, Configurator)}).
     *  If you only want to style the component font property, you can use
     *  {@link #componentFont(Configurator)}.
     *
     * @param underline Whether the font should be underlined or not.
     * @return A new {@link ComponentStyleDelegate} with the provided font underlinedness.
     */
    public ComponentStyleDelegate<C> fontUnderline( boolean underline ) {
        return _withFont( f -> f.underlined(underline) );
    }

    /**
     *  Makes the font struck through or not struck through depending on the value of the {@code strikeThrough} parameter.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}).<br>
     *  <p>
     *  Note that this font style will be applied to both the component font property
     *  and the style engine based text (see {@link #text(String, Configurator)}).
     *  If you only want to style the component font property, you can use
     *  {@link #componentFont(Configurator)}.
     *
     * @param strikeThrough Whether the font should be struck through or not.
     * @return A new {@link ComponentStyleDelegate} with the provided font struck throughness.
     */
    public ComponentStyleDelegate<C> fontStrikeThrough( boolean strikeThrough ) {
        return _withFont( f -> f.strikeThrough(strikeThrough) );
    }

    /**
     *  Creates a new {@link StyleConf} where the font color is set to the provided {@link Color}.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}).<br>
     *  <p>
     *  Note that this font style will be applied to both the component font property
     *  and the style engine based text (see {@link #text(String, Configurator)}).
     *  If you only want to style the component font property, you can use
     *  {@link #componentFont(Configurator)}.
     *
     * @param color The {@link Color}.
     * @return A new {@link ComponentStyleDelegate} with the provided font color.
     * @throws NullPointerException If the color is {@code null}.
     *         Use {@link UI.Color#UNDEFINED} to remove the font color style.
     */
    public ComponentStyleDelegate<C> fontColor( Color color ) {
        Objects.requireNonNull(color, "The color cannot be null! Use UI.Color.UNDEFINED to remove the font color style.");
        return _withFont( f -> f.color(color) );
    }

    /**
     *  Creates a new {@link StyleConf} where the font color is set to a color parsed from the provided string.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}).<br>
     *  <p>
     *  Note that this font style will be applied to both the component font property
     *  and the style engine based text (see {@link #text(String, Configurator)}).
     *  If you only want to style the component font property, you can use
     *  {@link #componentFont(Configurator)}.
     *
     * @param colorString The {@link Color} as a string.
     * @return A new {@link ComponentStyleDelegate} with the provided font color.
     */
    public ComponentStyleDelegate<C> fontColor( String colorString ) {
        Objects.requireNonNull(colorString, "The color string cannot be null! Use an empty string to remove the font color style.");
        return _withFont( f -> f.color(colorString) );
    }

    /**
     *  Creates a new {@link StyleConf} where the font background color is set to the provided {@link Color}.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}).<br>
     *  <p>
     *  Note that this font style will be applied to both the component font property
     *  and the style engine based text (see {@link #text(String, Configurator)}).
     *  If you only want to style the component font property, you can use
     *  {@link #componentFont(Configurator)}.
     *
     * @param color The {@link Color}.
     * @return A new {@link ComponentStyleDelegate} with the provided font background color.
     * @throws NullPointerException If the color is {@code null}.
     *          Use {@link UI.Color#UNDEFINED} to remove the font background color style.
     */
    public ComponentStyleDelegate<C> fontBackgroundColor( Color color ) {
        Objects.requireNonNull(color);
        return _withFont( f -> f.backgroundColor(color) );
    }

    /**
     *  Creates a new {@link StyleConf} where the font color is set to a color parsed from the provided string.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}).<br>
     *  <p>
     *  Note that this font style will be applied to both the component font property
     *  and the style engine based text (see {@link #text(String, Configurator)}).
     *  If you only want to style the component font property, you can use
     *  {@link #componentFont(Configurator)}.
     *
     * @param colorString The {@link Color} as a string.
     * @return A new {@link ComponentStyleDelegate} with the provided font color.
     */
    public ComponentStyleDelegate<C> fontBackgroundColor( String colorString ) {
        Objects.requireNonNull(colorString);
        return _withFont( f -> f.backgroundColor(colorString) );
    }

    /**
     *  Creates a new {@link StyleConf} where the font selection color is set to the provided {@link Color}.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}).<br>
     *  <p>
     *  Note that this font style will be applied to both the component font property
     *  and the style engine based text (see {@link #text(String, Configurator)}).
     *  If you only want to style the component font property, you can use
     *  {@link #componentFont(Configurator)}.
     *
     * @param color The {@link Color}.
     * @return A new {@link ComponentStyleDelegate} with the provided font selection color.
     * @throws NullPointerException If the color is {@code null}.
     *         Use {@link UI.Color#UNDEFINED} to remove the font selection color style.
     */
    public ComponentStyleDelegate<C> fontSelectionColor( Color color ) {
        Objects.requireNonNull(color);
        return _withFont( f -> f.selectionColor(color) );
    }

    /**
     *  Creates a new {@link StyleConf} where the font selection color is set to a color parsed from the provided string.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}).<br>
     *  <p>
     *  Note that this font style will be applied to both the component font property
     *  and the style engine based text (see {@link #text(String, Configurator)}).
     *  If you only want to style the component font property, you can use
     *  {@link #componentFont(Configurator)}.
     *
     * @param colorString The {@link Color} as a string.
     * @return A new {@link ComponentStyleDelegate} with the provided font selection color.
     */
    public ComponentStyleDelegate<C> fontSelectionColor( String colorString ) {
        return _withFont( f -> f.selectionColor(colorString) );
    }

    /**
     *  The {@link AffineTransform} property of a font defines how the font is
     *  rotated, scaled, skewed or translated. This method allows you to apply
     *  a custom {@link AffineTransform} to the fonts of the component.
     *
     * @param transform The {@link AffineTransform} to apply to the font.
     * @return A new {@link ComponentStyleDelegate} with the provided font transform.
     */
    public ComponentStyleDelegate<C> fontTransform( @Nullable AffineTransform transform ) {
        return _withStyle(_styleConf._withFont(_styleConf.font().transform(transform)));
    }

    /**
     *  Creates an updated style config with the provided font paint
     *  applied to all font configurations of the component.
     *
     * @param paint The {@link Paint} to use for the foreground of the font, which translates to the
     *              {@link java.awt.font.TextAttribute#FOREGROUND} attribute.
     * @return A new {@link ComponentStyleDelegate} with the provided font paint.
     */
    public ComponentStyleDelegate<C> fontPaint( @Nullable Paint paint ) {
        return _withFont( f-> f.paint(paint) );
    }

    /**
     *  Updates this style delegate with the supplied {@link Paint} object
     *  used for the background of the font, which translates to the
     *  {@link java.awt.font.TextAttribute#BACKGROUND} attribute.
     *  
     * @param paint The {@link Paint} to use for the background of the font, which translates to the
     *              {@link java.awt.font.TextAttribute#BACKGROUND} attribute.
     * @return A new {@link ComponentStyleDelegate} with the provided font background paint.
     */
    public ComponentStyleDelegate<C> fontBackgroundPaint( @Nullable Paint paint ) {
        return _withFont( f -> f.backgroundPaint(paint) );
    }


    /**
     *  Use this to define the weight of the default font of the component.
     *  The default value is 1.0 (see {@link java.awt.font.TextAttribute#WEIGHT_REGULAR}),
     *  whereas a bold font typically has a font weight
     *  of 2.0 (see {@link java.awt.font.TextAttribute#WEIGHT_BOLD}).
     *  <p>
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}).<br>
     *  <p>
     *  Note that this font style will be applied to both the component font property
     *  and the style engine based text (see {@link #text(String, Configurator)}).
     *  If you only want to style the component font property, you can use
     *  {@link #componentFont(Configurator)}.
     * @param weight The weight of the font.
     * @return A new {@link ComponentStyleDelegate} with the provided font weight.
     */
    public ComponentStyleDelegate<C> fontWeight( float weight ) {
        return _withFont( f -> f.weight(weight) );
    }

    /**
     *  The font spacing, which is also known as tracking, is the space between characters in a font.
     *  See {@link java.awt.font.TextAttribute#TRACKING} for more information.
     *
     * @param spacing The spacing of the default font of the component, which translates to the
     *                {@link java.awt.font.TextAttribute#TRACKING} attribute.
     * @return A new {@link ComponentStyleDelegate} with the provided font spacing.
     */
    public ComponentStyleDelegate<C> fontSpacing( float spacing ) {
        return _withFont( f -> f.spacing(spacing) );
    }

    /**
     *  Use this to define the horizontal alignment of the default font of the component.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}). <br>
     *  Also note that not all text based components support text alignment.
     *  @param alignment The horizontal alignment of the font.
     *                   See {@link UI.HorizontalAlignment} for more information.
     *  @return A new {@link ComponentStyleDelegate} with the provided font alignment.
     * @throws NullPointerException If the alignment is {@code null}.
     *         Use {@link UI.HorizontalAlignment#UNDEFINED} to remove the font alignment style.
     */
    public ComponentStyleDelegate<C> fontAlignment( UI.HorizontalAlignment alignment ) {
        Objects.requireNonNull(alignment);
        return _withFont( f -> f.horizontalAlignment(alignment) );
    }

    /**
     *  Use this to define the vertical alignment of the default font of the component.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}). <br>
     *  Also note that not all text based components support text alignment.
     *  @param alignment The vertical alignment of the font.
     *                   See {@link UI.VerticalAlignment} for more information.
     *  @return A new {@link ComponentStyleDelegate} with the provided font alignment.
     *  throws NullPointerException If the alignment is {@code null}.
     *       Use {@link UI.VerticalAlignment#UNDEFINED} to remove the font alignment style.
     */
    public ComponentStyleDelegate<C> fontAlignment( UI.VerticalAlignment alignment ) {
        Objects.requireNonNull(alignment);
        return _withFont( f -> f.verticalAlignment(alignment) );
    }

    /**
     *  Use this to define the horizontal and vertical alignment of the default font of the component.
     *  Note that font styles will only apply if the component that is being rendered
     *  also supports displaying text, or has a custom text style (see {@link TextConf}). <br>
     *  Also note that not all text based components support text alignment.
     *  @param alignment The horizontal and vertical alignment of the font.
     *                   See {@link UI.Alignment} for more information.
     *  @return A new {@link ComponentStyleDelegate} with the provided font alignment.
     *  throws NullPointerException If the alignment is {@code null}.
     *          Use {@link UI.Alignment#UNDEFINED} to remove the font alignment style.
     */
    public ComponentStyleDelegate<C> fontAlignment( UI.Alignment alignment ) {
        Objects.requireNonNull(alignment);
        return fontAlignment(alignment.getHorizontal()).fontAlignment(alignment.getVertical());
    }

    /**
     *  Defines the minimum {@link Dimension} for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMinimumSize(Dimension)} on the underlying component,
     *  which will be called when all the other styles are applied and rendered. <br>
     * @param width The minimum width.
     * @param height The minimum height.
     * @return A new {@link ComponentStyleDelegate} with the provided minimum {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link StyleConf} is applied.
     */
    public ComponentStyleDelegate<C> minSize( int width, int height ) {
        return _withStyle(_styleConf._withDimensionality(_styleConf.dimensionality()._withMinWidth(width)._withMinHeight(height)));
    }

    /**
     *  Defines the minimum size for this {@link JComponent} in the form of a {@link Size} object. <br>
     *  This ultimately translates to {@link JComponent#setMinimumSize(Dimension)} on the underlying component,
     *  which will be called when all the other styles are applied and rendered. <br>
     * @param size The minimum {@link Size}.
     * @return A new {@link ComponentStyleDelegate} with the provided minimum {@link Size} set to be later
     *          applied to the underlying component when the final {@link StyleConf} is applied.
     */
    public ComponentStyleDelegate<C> minSize( Size size ) {
        Objects.requireNonNull(size);
        return _withStyle(_styleConf._withDimensionality(_styleConf.dimensionality()._withMinSize(size)));
    }

    /**
     *  Defines the minimum width for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMinimumSize(Dimension)} on the underlying component,
     *  which will be called when all the other styles are applied and rendered. <br>
     * @param minWidth The minimum width.
     * @return A new {@link ComponentStyleDelegate} with the provided minimum width set to be later
     *          applied to the underlying component when the final {@link StyleConf} is applied.
     */
    public ComponentStyleDelegate<C> minWidth( int minWidth ) {
        return _withStyle(_styleConf._withDimensionality(_styleConf.dimensionality()._withMinWidth(minWidth)));
    }

    /**
     *  Defines the minimum height for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMinimumSize(Dimension)} on the underlying component,
     *  which will be called when all the other styles are applied and rendered. <br>
     * @param minHeight The minimum height.
     * @return A new {@link ComponentStyleDelegate} with the provided minimum height set to be later
     *          applied to the underlying component when the final {@link StyleConf} is applied.
     */
    public ComponentStyleDelegate<C> minHeight( int minHeight ) {
        return _withStyle(_styleConf._withDimensionality(_styleConf.dimensionality()._withMinHeight(minHeight)));
    }

    /**
     *  Defines the maximum {@link Dimension} for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     *  The passed {@link Dimension} will be applied when all the other styles are applied and rendered. <br>
     *
     * @param width The maximum width.
     * @param height The maximum height.
     * @return A new {@link ComponentStyleDelegate} with the provided maximum {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link StyleConf} is applied.
     */
    public ComponentStyleDelegate<C> maxSize( int width, int height ) {
        return _withStyle(_styleConf._withDimensionality(_styleConf.dimensionality()._withMaxWidth(width)._withMaxHeight(height)));
    }

    /**
     *  Defines the maximum {@link Size} for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     *  The passed {@link Size} will be applied when all the other styles are applied and rendered. <br>
     *
     * @param maxSize The maximum {@link Size}.
     * @return A new {@link ComponentStyleDelegate} with the provided maximum {@link Size} set to be later
     *          applied to the underlying component when the final {@link StyleConf} is applied.
     */
    public ComponentStyleDelegate<C> maxSize( Size maxSize ) {
        Objects.requireNonNull(maxSize);
        return _withStyle(_styleConf._withDimensionality(_styleConf.dimensionality()._withMaxSize(maxSize)));
    }

    /**
     *  Defines the maximum width for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     *  The passed width will be applied when all the other styles are applied and rendered. <br>
     *
     * @param maxWidth The maximum width.
     * @return A new {@link ComponentStyleDelegate} with the provided maximum width set to be later
     *          applied to the underlying component when the final {@link StyleConf} is applied.
     */
    public ComponentStyleDelegate<C> maxWidth( int maxWidth ) {
        return _withStyle(_styleConf._withDimensionality(_styleConf.dimensionality()._withMaxWidth(maxWidth)));
    }

    /**
     *  Defines the maximum height for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     *  The passed height will be applied when all the other styles are applied and rendered. <br>
     *
     * @param maxHeight The maximum height.
     * @return A new {@link ComponentStyleDelegate} with the provided maximum height set to be later
     *          applied to the underlying component when the final {@link StyleConf} is applied.
     */
    public ComponentStyleDelegate<C> maxHeight( int maxHeight ) {
        return _withStyle(_styleConf._withDimensionality(_styleConf.dimensionality()._withMaxHeight(maxHeight)));
    }

    /**
     *  Defines the preferred {@link Size} for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     *  The passed {@link Size} will be applied when all the other styles are applied and rendered. <br>
     *
     * @param preferredSize The preferred {@link Size}.
     * @return A new {@link ComponentStyleDelegate} with the provided preferred {@link Size} set to be later
     *          applied to the underlying component when the final {@link StyleConf} is applied.
     */
    public ComponentStyleDelegate<C> prefSize( Size preferredSize ) {
        Objects.requireNonNull(preferredSize);
        return _withStyle(_styleConf._withDimensionality(_styleConf.dimensionality()._withPreferredSize(preferredSize)));
    }

    /**
     *  Defines the preferred {@link Dimension} for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     *  The passed {@link Dimension} will be applied when all the other styles are applied and rendered. <br>
     *
     * @param width The preferred width.
     * @param height The preferred height.
     * @return A new {@link ComponentStyleDelegate} with the provided preferred {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link StyleConf} is applied.
     */
    public ComponentStyleDelegate<C> prefSize( int width, int height ) {
        return _withStyle(_styleConf._withDimensionality(_styleConf.dimensionality()._withPreferredWidth(width)._withPreferredHeight(height)));
    }

    /**
     *  Defines the preferred width for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     *  The passed width will be applied when all the other styles are applied and rendered. <br>
     *
     * @param preferredWidth The preferred width.
     * @return A new {@link ComponentStyleDelegate} with the provided preferred width set to be later
     *          applied to the underlying component when the final {@link StyleConf} is applied.
     */
    public ComponentStyleDelegate<C> prefWidth( int preferredWidth ) {
        return _withStyle(_styleConf._withDimensionality(_styleConf.dimensionality()._withPreferredWidth(preferredWidth)));
    }

    /**
     *  Defines the preferred height for this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     *  The passed height will be applied when all the other styles are applied and rendered. <br>
     *
     * @param preferredHeight The preferred height.
     * @return A new {@link ComponentStyleDelegate} with the provided preferred height set to be later
     *          applied to the underlying component when the final {@link StyleConf} is applied.
     */
    public ComponentStyleDelegate<C> prefHeight( int preferredHeight ) {
        return _withStyle(_styleConf._withDimensionality(_styleConf.dimensionality()._withPreferredHeight(preferredHeight)));
    }

    /**
     *  Defines the size of this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param size The width and height size {@link Dimension}.
     * @return A new {@link ComponentStyleDelegate} with the provided {@link Size} (width and height) set to be later
     *          applied to the underlying component when the final {@link StyleConf} is applied.
     */
    public ComponentStyleDelegate<C> size( Size size ) {
        Objects.requireNonNull(size);
        return _withStyle(_styleConf._withDimensionality(_styleConf.dimensionality()._withSize(size)));
    }

    /**
     *  Defines the size of this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param width The width.
     * @param height The height.
     * @return A new {@link ComponentStyleDelegate} with the provided size (width and height) {@link Dimension} set to be later
     *          applied to the underlying component when the final {@link StyleConf} is applied.
     */
    public ComponentStyleDelegate<C> size( int width, int height ) {
        return _withStyle(_styleConf._withDimensionality(_styleConf.dimensionality()._withWidth(width)._withHeight(height)));
    }


    /**
     *  Defines the width of this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param width The width.
     * @return A new {@link ComponentStyleDelegate} with the provided width set to be later
     *          applied to the underlying component when the final {@link StyleConf} is applied.
     */
    public ComponentStyleDelegate<C> width( int width ) {
        return _withStyle(_styleConf._withDimensionality(_styleConf.dimensionality()._withWidth(width)));
    }

    /**
     *  Defines the height of this {@link JComponent}. <br>
     *  This ultimately translates to {@link JComponent#setSize(Dimension)} on the underlying component. <br>
     * @param height The height.
     * @return A new {@link ComponentStyleDelegate} with the provided height set to be later
     *          applied to the underlying component when the final {@link StyleConf} is applied.
     */
    public ComponentStyleDelegate<C> height( int height ) {
        return _withStyle(_styleConf._withDimensionality(_styleConf.dimensionality()._withHeight(height)));
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
        return _withStyle(_styleConf._withBase(_styleConf.base().cursor(cursor)));
    }

    /**
     *  Determines how the component is oriented, typically with respect to
     *  the text direction and where content originates. <br>
     *  This translates to {@link JComponent#setComponentOrientation(ComponentOrientation)}
     *  on the underlying component. <br>
     *  <br>
     *  Note that although all components support this property, it may not always
     *  have a noticeable effect on the component. <br>
     *  How this property is interpreted depends heavily on the component, it's layout manager
     *  and the look and feel implementation. <br>
     * @param orientation The {@link UI.ComponentOrientation}, which maps 1:1 to the AWT {@link ComponentOrientation} constants.
     * @return A new {@link ComponentStyleDelegate} with the provided {@link UI.ComponentOrientation} set to be later
     *         applied to the underlying component when the final {@link StyleConf} is applied.
     */
    public ComponentStyleDelegate<C> orientation( UI.ComponentOrientation orientation ) {
        Objects.requireNonNull(orientation);
        return _withStyle(_styleConf._withBase(_styleConf.base().orientation(orientation)));
    }

    /**
     *  Use this to define the layout manager for this {@link JComponent}
     *  using a {@link Layout} object. <br>
     *  Checkout the factory methods in {@link Layout} for creating
     *  different types of layout managers like {@link Layout#flow()}, {@link Layout#mig(String)}
     *  or {@link Layout#grid(int, int)}. <br>
     *
     * @param installer The {@link Layout} to use for installing the layout.
     * @return A new {@link ComponentStyleDelegate} with the provided {@link Layout} set to be later
     */
    public ComponentStyleDelegate<C> layout( Layout installer ) {
        return _withStyle(_styleConf._withLayout(_styleConf.layout().layout(installer)));
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
        Objects.requireNonNull(constraints);
        if ( _styleConf.layout().layout() instanceof Layout.ForMigLayout ) {
            Layout.ForMigLayout migInstaller = (Layout.ForMigLayout) _styleConf.layout().layout();
            migInstaller = migInstaller.withConstraint(constraints);
            return _withStyle(_styleConf._withLayout(_styleConf.layout().layout(migInstaller)));
        }
        return _withStyle(_styleConf._withLayout(_styleConf.layout().layout(Layout.mig(constraints, "", ""))));
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
        Objects.requireNonNull(constraints);
        Objects.requireNonNull(columnConstraints);
        if ( _styleConf.layout().layout() instanceof Layout.ForMigLayout) {
            Layout.ForMigLayout migInstaller = (Layout.ForMigLayout) _styleConf.layout().layout();
            migInstaller = migInstaller.withConstraint(constraints).withColumnConstraint(columnConstraints);
            return _withStyle(_styleConf._withLayout(_styleConf.layout().layout(migInstaller)));
        }
        return _withStyle(_styleConf._withLayout(_styleConf.layout().layout(Layout.mig(constraints, columnConstraints, ""))));
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
        Objects.requireNonNull(constraints);
        Objects.requireNonNull(columnConstraints);
        Objects.requireNonNull(rowConstraints);
        return _withStyle(_styleConf._withLayout(_styleConf.layout().layout(Layout.mig(constraints, columnConstraints, rowConstraints))));
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
        return _withStyle(_styleConf._withLayout(_styleConf.layout().constraint(constraints)));
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
        Objects.requireNonNull(constraintAttr);
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
        return _withStyle(_styleConf._withLayout(_styleConf.layout().alignmentX(percentage)));
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
        return _withStyle(_styleConf._withLayout(_styleConf.layout().alignmentY(percentage)));
    }

    /**
     *  A convenient delegate method to {@link UI#scale()} which exposes the current UI scale factor
     *  that is used to scale the UI for high resolution displays (high dots-per-inch, or DPI).
     *  Use this scale factor when writing custom rendering code against the {@link Graphics2D} API.
     *
     * @return The current UI scale factor, which is used to scale the UI
     *         for high resolution displays (high dots-per-inch, or DPI).
     */
    public float getScale() { return UI.scale(); }

    /**
     *  A convenient delegate method to {@link UI#scale()} which exposes the current UI scale factor
     *  that is used to scale the UI for high resolution displays (high dots-per-inch, or DPI).
     *  Use this scale factor when writing custom rendering code against the {@link Graphics2D} API.
     *
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

    @Override
    public String toString() {
        try {
            return this.getClass().getSimpleName() + "[" +
                        "styleConf=" + _styleConf + ", " +
                        "component=" + _component + ", " +
                    "]";
        } catch ( Exception e ) {
            return this.getClass().getSimpleName() + "[toString() failed: " + e + "]";
        }
    }
}

package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.UI;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Color;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

/**
 *  An immutable config API for specifying a gradient style.
 *  as a sub-style of various other styles,
 *  like for example {@link BaseConf} or {@link BorderConf} accessed through the
 *  {@link ComponentStyleDelegate#gradient(String, Function)}
 *  method.
 *  The state of a gradient style is immutable and can only be updated by
 *  wither like methods that return a new instance of the gradient style
 *  with the specified property updated.
 *  <p>
 *  The following properties with their respective purpose are available:
 *  <br>
 *  <ul>
 *      <li><b>Transition</b>
 *          The transition defines the direction of the gradient.
 *          <br>
 *          The following transitions are available:
 *          <ul>
 *              <li>{@link UI.Span#TOP_LEFT_TO_BOTTOM_RIGHT}</li>
 *              <li>{@link UI.Span#BOTTOM_LEFT_TO_TOP_RIGHT}</li>
 *              <li>{@link UI.Span#TOP_RIGHT_TO_BOTTOM_LEFT}</li>
 *              <li>{@link UI.Span#BOTTOM_RIGHT_TO_TOP_LEFT}</li>
 *              <li>{@link UI.Span#TOP_TO_BOTTOM}</li>
 *              <li>{@link UI.Span#LEFT_TO_RIGHT}</li>
 *              <li>{@link UI.Span#BOTTOM_TO_TOP}</li>
 *              <li>{@link UI.Span#RIGHT_TO_LEFT}</li>
 *          </ul>
 *      </li>
 *      <li><b>Type</b>
 *          The type defines the shape of the gradient
 *          which can be either linear or radial. <br>
 *          So the following types are available:
 *          <ul>
 *              <li>{@link UI.GradientType#LINEAR}</li>
 *              <li>{@link UI.GradientType#RADIAL}</li>
 *              <li>{@link UI.GradientType#CONIC}</li>
 *          </ul>
 *      </li>
 *      <li><b>Colors</b>
 *          An array of colors that will be used
 *          as a basis for the gradient transition.
 *      </li>
 *      <li><b>Offset</b>
 *          The offset defines the start position of the gradient
 *          on the x and y axis.
 *          This property, together with the {@link #span(UI.Span)}
 *          property, defines the start position and direction of the gradient.
 *      </li>
 *      <li><b>Size</b>
 *          The size defines the size of the gradient
 *          in terms of the distance from the start position of the gradient
 *          to the end position of the gradient.
 *          <br>
 *          If no size is specified, the size of the gradient will be
 *          based on the size of the component that the gradient is applied to.
 *      </li>
 *      <li><b>Area</b>
 *          The component are to which the gradient is clipped to.
 *          Which means that the gradient will only be visible within the
 *          specified area of the component.
 *      </li>
 *      <li><b>Boundary</b>
 *          The boundaries of a component define the outlines between the different
 *          {@link swingtree.UI.ComponentArea}s.
 *          Setting a particular boundary causes the gradient to start at that boundary.
 *      </li>
 *      <li><b>Focus Offset</b>
 *          An offset property consisting of a {@code x} and {@code y} value
 *          which will be used together with the gradients position to calculate
 *          a focus point.
 *          This is only relevant for radial gradients!
 *      </li>
 *      <li><b>Rotation</b>
 *          The rotation of the gradient in degrees.
 *          This is typically only relevant for a linear gradient.
 *          However it is also applicable to a radial gradient with a focus offset,
 *          where the rotation will be applied to the focus offset.
 *      </li>
 *      <li><b>Fractions</b>
 *          An array of values between 0 and 1 that defines the relative position
 *          of each color in the gradient.
 *          <br>
 *          Note that the number of fractions must match the number of colors in the gradient.
 *          However, if the number of fractions is less than the number of colors, then the remaining
 *          colors will be determined based on linear interpolation.
 *          If the number of fractions is greater than the number of colors, then the remaining
 *          fractions will be ignored.
 *      </li>
 *      <li><b>Cycle</b>
 *          The cycle of the gradient which can be one of the following constants:
 *          <ul>
 *              <li>{@link UI.Cycle#NONE} -
 *                  The gradient is only rendered once, without repeating.
 *                  The last color is used to fill the remaining area.
 *                  This is the default cycle.
 *              </li>
 *              <li>{@link UI.Cycle#REFLECT} -
 *                  The gradient is rendered once and then reflected.,
 *                  which means that the gradient is rendered again in reverse order
 *                  starting from the last color and ending with the first color.
 *                  After that, the gradient is rendered again in the original order,
 *                  starting from the first color and ending with the last color and so on.
 *              </li>
 *              <li>{@link UI.Cycle#REPEAT} -
 *                  The gradient is rendered repeatedly, which means that it
 *                  is rendered again and again in the original order, starting from the first color
 *                  and ending with the last color.
 *              </li>
 *          </ul>
 *          Note that this property ultimately translates to the {@link java.awt.MultipleGradientPaint.CycleMethod}
 *          of the {@link java.awt.LinearGradientPaint} or {@link java.awt.RadialGradientPaint} that is used
 *          to render the gradient inside the SwingTree style engine.
 *      </li>
 *  </ul>
 *  <p>
 *  You can also use the {@link #none()} method to specify that no gradient should be used,
 *  as the instance returned by that method is a gradient without any colors, effectively
 *  making it a representation of the absence of a gradient style.
 */
@Immutable
@SuppressWarnings("Immutable")
public final class GradientConf implements Simplifiable<GradientConf>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GradientConf.class);
    static final UI.Layer DEFAULT_LAYER = UI.Layer.BACKGROUND;

    private static final GradientConf _NONE = new GradientConf(
                                                        UI.Span.TOP_TO_BOTTOM,
                                                        UI.GradientType.LINEAR,
                                                        new Color[0],
                                                        Offset.none(),
                                                        -1f,
                                                        UI.ComponentArea.BODY,
                                                        UI.ComponentBoundary.EXTERIOR_TO_BORDER,
                                                        Offset.none(),
                                                        0f,
                                                        new float[0],
                                                        UI.Cycle.NONE
                                                    );

    /**
     *  Use the returned instance as a representation of the absence of a gradient.
     *
     *  @return A gradient without any colors, effectively
     *          representing the absence of a gradient.
     */
    public static GradientConf none() { return _NONE; }
    
    static GradientConf of(
        UI.Span span,
        UI.GradientType      type,
        Color[]              colors,
        Offset               offset,
        float                size,
        UI.ComponentArea     area,
        UI.ComponentBoundary boundary,
        Offset               focus,
        float                rotation,
        float[]              fractions,
        UI.Cycle             cycle
    ) {
        // The rotation may be any number
        // which always has to be normalized to a value between -180 and 180
        rotation = ( (((rotation+180f) % 360f + 360f) % 360f) - 180f );

        GradientConf none = none();
        if ( span       == none._span &&
             type       == none._type       &&
             Arrays.equals(colors, none._colors) &&
             Objects.equals(offset, none._offset) &&
             size       == none._size &&
             area       == none._area       &&
             boundary   == none._boundary   &&
             Objects.equals(focus, none._focus) &&
             rotation   == none._rotation   &&
             Arrays.equals(fractions, none._fractions) &&
             cycle      == none._cycle
        )
            return none;

        return new GradientConf(span, type, colors, offset, size, area, boundary, focus, rotation, fractions, cycle);
    }


    private final UI.Span _span;
    private final UI.GradientType      _type;
    private final Color[]              _colors;
    private final Offset               _offset;
    private final float                _size;
    private final UI.ComponentArea     _area;
    private final UI.ComponentBoundary _boundary;
    private final Offset               _focus;
    private final float                _rotation;
    private final float[]              _fractions;
    private final UI.Cycle             _cycle;


    private GradientConf(
        UI.Span span,
        UI.GradientType      type,
        Color[]              colors,
        Offset               offset,
        float                size,
        UI.ComponentArea     area,
        UI.ComponentBoundary boundary,
        Offset               focus,
        float                rotation,
        float[]              fractions,
        UI.Cycle             cycle
    ) {
        _span = Objects.requireNonNull(span);
        _type       = Objects.requireNonNull(type);
        _colors     = Objects.requireNonNull(colors);
        _offset     = Objects.requireNonNull(offset);
        _size       = ( size < 0 ? -1 : size );
        _area       = Objects.requireNonNull(area);
        _boundary   = Objects.requireNonNull(boundary);
        _focus      = Objects.requireNonNull(focus);
        _rotation   = rotation;
        _fractions  = Objects.requireNonNull(fractions);
        _cycle      = Objects.requireNonNull(cycle);
    }

    UI.Span span() { return _span; }

    UI.GradientType type() { return _type; }

    Color[] colors() { return _colors; }

    Offset offset() { return _offset; }

    float size() { return _size; }

    UI.ComponentArea area() { return _area; }

    UI.ComponentBoundary boundary() { return _boundary; }

    Offset focus() { return _focus; }

    float rotation() { return _rotation; }

    float[] fractions() { return _fractions; }

    UI.Cycle cycle() { return _cycle; }

    boolean isOpaque() {
        if ( _colors.length == 0 )
            return false;

        boolean foundTransparentColor = false;
        for ( Color c : _colors ) {
            if ( c.getAlpha() < 255 ) {
                foundTransparentColor = true;
                break;
            }
        }
        return !foundTransparentColor;
    }

    GradientConf _scale( double scale ) {
        if ( _size > 0 && scale != 1 )
            return of(
                    _span,
                        _type,
                        _colors,
                        _offset.scale(scale),
                        (float) (_size * scale),
                        _area,
                        _boundary,
                        _focus.scale(scale),
                        _rotation,
                        _fractions,
                        _cycle
                    );
        else
            return this;
    }

    /**
     *  Define a list of colors which will, as part of the gradient, transition from one
     *  to the next in the order they are specified.
     *  <p>
     *  Note that you need to specify at least two colors for a gradient to be visible.
     *
     * @param colors The colors in the gradient.
     * @return A new gradient style with the specified colors.
     * @throws NullPointerException if any of the colors is {@code null}.
     */
    public GradientConf colors( Color... colors ) {
        Objects.requireNonNull(colors);
        for ( Color color : colors )
            Objects.requireNonNull(color, "Use UI.Color.UNDEFINED instead of null to represent the absence of a color.");
        return of(_span, _type, colors, _offset, _size, _area, _boundary, _focus, _rotation, _fractions, _cycle);
    }

    /**
     *  Define a list of {@link String} based colors which will, as part of the gradient, transition from one
     *  to the next in the order they are specified.
     *  <p>
     *  Note that you need to specify at least two colors for a gradient to be visible.
     *
     * @param colors The colors in the gradient in {@link String} format.
     * @return A new gradient style with the specified colors.
     * @throws NullPointerException if any of the colors is {@code null}.
     */
    public GradientConf colors( String... colors ) {
        Objects.requireNonNull(colors);
        try {
            Color[] actualColors = new Color[colors.length];
            for ( int i = 0; i < colors.length; i++ )
                actualColors[i] = UI.color(colors[i]);

            return of(_span, _type, actualColors, _offset, _size, _area, _boundary, _focus, _rotation, _fractions, _cycle);
        } catch ( Exception e ) {
            log.error("Failed to parse color strings: " + Arrays.toString(colors), e);
            return this; // We want to avoid side effects other than a wrong color
        }
    }

    /**
     *  Define from where and to where the gradient should transition to
     *  within the {@link UI.ComponentBoundary} of the component.
     *  <ul>
     *     <li>{@link UI.Span#TOP_LEFT_TO_BOTTOM_RIGHT}</li>
     *     <li>{@link UI.Span#BOTTOM_LEFT_TO_TOP_RIGHT}</li>
     *     <li>{@link UI.Span#TOP_RIGHT_TO_BOTTOM_LEFT}</li>
     *     <li>{@link UI.Span#BOTTOM_RIGHT_TO_TOP_LEFT}</li>
     *     <li>{@link UI.Span#TOP_TO_BOTTOM}</li>
     *     <li>{@link UI.Span#LEFT_TO_RIGHT}</li>
     *     <li>{@link UI.Span#BOTTOM_TO_TOP}</li>
     *     <li>{@link UI.Span#RIGHT_TO_LEFT}</li>
     *  </ul>
     *
     * @param span The span policy of the gradient, which defines the direction of the gradient.
     * @return A new gradient style with the specified alignment.
     * @throws NullPointerException if the alignment is {@code null}.
     */
    public GradientConf span( UI.Span span ) {
        Objects.requireNonNull(span);
        return of(span, _type, _colors, _offset, _size, _area, _boundary, _focus, _rotation, _fractions, _cycle);
    }

    /**
     *  Define the type of the gradient which is one of the following:
     *  <ul>
     *     <li>{@link UI.GradientType#LINEAR}</li>
     *     <li>{@link UI.GradientType#RADIAL}</li>
     *     <li>{@link UI.GradientType#CONIC}</li>
     *  </ul>
     *
     * @param type The type of the gradient.
     * @return A new gradient style with the specified type.
     * @throws NullPointerException if the type is {@code null}.
     */
    public GradientConf type( UI.GradientType type ) {
        Objects.requireNonNull(type);
        return of(_span, type, _colors, _offset, _size, _area, _boundary, _focus, _rotation, _fractions, _cycle);
    }

    /**
     *  Define the offset of the gradient which is the start position of the gradient
     *  on the x and y-axis. <br>
     *  Note that the offset is relative to the component that the gradient is applied to.
     *  <p>
     * @param x The gradient start offset on the x-axis.
     * @param y The gradient start offset on the y-axis.
     * @return A new gradient style with the specified offset.
     */
    public GradientConf offset( double x, double y ) {
        return of(_span, _type, _colors, Offset.of(x,y), _size, _area, _boundary, _focus, _rotation, _fractions, _cycle);
    }

    /**
     *  Define the size of the gradient which is the size of the gradient
     *  in terms of the distance from the start position of the gradient
     *  to the end position of the gradient.
     *  <p>
     *  Note that if no size is specified, the size of the gradient will be
     *  based on the size of the component that the gradient is applied to.
     *
     * @param size The gradient size.
     * @return A new gradient style with the specified size.
     */
    public GradientConf size( double size ) {
        return of(_span, _type, _colors, _offset, (float) size, _area, _boundary, _focus, _rotation, _fractions, _cycle);
    }

    /**
     *  Define the area of the component to which the gradient is clipped to.
     *  Which means that the gradient will only be visible within the
     *  specified area of the component.
     *
     * @param area The area of the component to which the gradient is clipped to.
     * @return A new gradient style with the specified area.
     */
    public GradientConf clipTo( UI.ComponentArea area ) {
        return of(_span, _type, _colors, _offset, _size, area, _boundary, _focus, _rotation, _fractions, _cycle);
    }

    /**
     *  Define the boundary at which the gradient should start in terms of its base position.
     *  So if the boundary is set to {@link UI.ComponentBoundary#EXTERIOR_TO_BORDER}
     *  then the gradient position will be determined by the margin of the component. <br>
     *  Here a complete list of the available boundaries:
     * <ul>
     *     <li>{@link UI.ComponentBoundary#OUTER_TO_EXTERIOR} -
     *     The outermost boundary of the entire component, including any margin that might be applied.
     *     Using this boundary will cause the gradient to be positioned somewhere at
     *     the outer most edge of the component.
     *     </li>
     *     <li>{@link UI.ComponentBoundary#EXTERIOR_TO_BORDER} -
     *     The boundary located after the margin but before the border.
     *     This tightly wraps the entire {@link UI.ComponentArea#BODY}.
     *     Using this boundary will cause the gradient to be positioned somewhere at
     *     the outer most edge of the component's body, which is between the margin and the border.
     *     </li>
     *     <li>{@link UI.ComponentBoundary#BORDER_TO_INTERIOR} -
     *     The boundary located after the border but before the padding.
     *     It represents the edge of the component's interior.
     *     Using this boundary will cause the gradient to be positioned somewhere at
     *     the outer most edge of the component's interior, which is between the border and the padding area.
     *     </li>
     *     <li>{@link UI.ComponentBoundary#INTERIOR_TO_CONTENT} -
     *     The boundary located after the padding.
     *     It represents the innermost boundary of the component, where the actual content of the component begins,
     *     like for example the contents of a {@link JPanel} or {@link JScrollPane}.
     *     Using this boundary will cause the gradient to be positioned somewhere after the padding area
     *     and before the content area, which is where all of the child components are located.
     *     </li>
     * </ul>
     *  <p>
     *  You can think of this property as a convenient way to define the base position of the gradient.
     *  So if you want to do the positioning yourself, then you may configure this property to
     *  {@link UI.ComponentBoundary#OUTER_TO_EXTERIOR}, which will cause the gradient to be positioned
     *  at the outermost edge of the component, and then use the {@link #offset(double, double)} method
     *  to define the exact position of the gradient.
     *  (You may also want to set the {@link #span(UI.Span)}
     *  property to {@link UI.Span#TOP_LEFT_TO_BOTTOM_RIGHT} to make sure that the gradient
     *  is positioned in the top left corner (origin position) of the component)
     *
     * @param boundary The boundary at which the gradient should start in terms of its offset.
     * @return A new gradient style with the specified boundary.
     */
    public GradientConf boundary( UI.ComponentBoundary boundary ) {
        return of(_span, _type, _colors, _offset, _size, _area, boundary, _focus, _rotation, _fractions, _cycle);
    }

    /**
     *  Define the focus offset of a radial gradient as a second position relative
     *  to the main position of the gradient (see {@link #offset(double, double)} and {@link #boundary(UI.ComponentBoundary)}
     *  which is used to define the direction of the gradient.
     *  <p>
     *  Note that this property is only relevant for radial gradients.
     *
     *  @param x The focus offset on the x-axis.
     *  @param y The focus offset on the y-axis.
     */
    public GradientConf focus( double x, double y ) {
        return of(_span, _type, _colors, _offset, _size, _area, _boundary, Offset.of(x,y), _rotation, _fractions, _cycle);
    }

    /**
     *  Define the rotation of the gradient in degrees.
     *
     *  @param rotation The rotation of the gradient in degrees.
     */
    public GradientConf rotation( float rotation ) {
        return of(_span, _type, _colors, _offset, _size, _area, _boundary, _focus, rotation, _fractions, _cycle);
    }

    /**
     *  Define the fractions of the gradient in the dorm of an array of values between 0 and 1
     *  that each the relative position of each color in the gradient transition.
     *  <p>
     *  Note that the number of fractions must match the number of colors in the gradient.
     *  If the number of fractions is less than the number of colors, then the remaining
     *  colors will be evenly distributed between the last two fractions.
     *
     *  @param fractions The fractions of the gradient.
     * @return An updated gradient configuration with the specified fractions.
     */
    public GradientConf fractions( double... fractions ) {
        float[] actualFractions = new float[fractions.length];
        for ( int i = 0; i < fractions.length; i++ )
            actualFractions[i] = (float) fractions[i];

        return of(_span, _type, _colors, _offset, _size, _area, _boundary, _focus, _rotation, actualFractions, _cycle);
    }

    /**
     *  Define the cycle of the gradient which is one of the following:
     *  <ul>
     *      <li>{@link UI.Cycle#NONE} -
     *          The gradient is only rendered once, without repeating.
     *          The last color is used to fill the remaining area.
     *          This is the default cycle.
     *      </li>
     *      <li>{@link UI.Cycle#REFLECT} -
     *          The gradient is rendered once and then reflected.,
     *          which means that the gradient is rendered again in reverse order
     *          starting from the last color and ending with the first color.
     *          After that, the gradient is rendered again in the original order,
     *          starting from the first color and ending with the last color and so on.
     *      </li>
     *      <li>{@link UI.Cycle#REPEAT} -
     *          The gradient is rendered repeatedly, which means that it
     *          is rendered again and again in the original order, starting from the first color
     *          and ending with the last color.
     *      </li>
     *  </ul>
     *  Note that this property ultimately translates to the {@link java.awt.MultipleGradientPaint.CycleMethod}
     *  of the {@link java.awt.LinearGradientPaint} or {@link java.awt.RadialGradientPaint} that is used
     *  to render the gradient inside the SwingTree style engine.
     *
     * @param cycle The cycle of the gradient.
     * @return A new gradient style with the specified cycle method.
     * @throws NullPointerException if the cycle is {@code null}.
     */
    public GradientConf cycle(UI.Cycle cycle ) {
        Objects.requireNonNull(cycle);
        return of(_span, _type, _colors, _offset, _size, _area, _boundary, _focus, _rotation, _fractions, cycle);
    }


    @Override
    public String toString() {
        if ( this.equals(_NONE) )
            return getClass().getSimpleName() + "[NONE]";
        return getClass().getSimpleName() + "[" +
                    "transition=" + _span + ", " +
                    "type="       + _type + ", " +
                    "colors="     + Arrays.toString(_colors) + ", " +
                    "offset="     + _offset + ", " +
                    "size="       + _size + ", " +
                    "area="       + _area + ", " +
                    "boundary="   + _boundary + ", " +
                    "focus="      + _focus + ", " +
                    "rotation="   + _rotation + ", " +
                    "fractions="  + Arrays.toString(_fractions) + ", " +
                    "cycle="      + _cycle +
                "]";
    }

    @Override
    public boolean equals( @Nullable Object o ) {
        if ( this == o ) return true;
        if ( !(o instanceof GradientConf) ) return false;
        GradientConf that = (GradientConf) o;
        return _span       == that._span &&
               _type       == that._type             &&
               Arrays.equals(_colors, that._colors)  &&
               Objects.equals(_offset, that._offset) &&
               _size       == that._size             &&
               _area       == that._area             &&
               _boundary   == that._boundary         &&
               Objects.equals(_focus, that._focus)   &&
               _rotation   == that._rotation         &&
               Arrays.equals(_fractions, that._fractions) &&
               _cycle      == that._cycle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                _span,
                _type,
                Arrays.hashCode(_colors),
                _offset,
                _size,
                _area,
                _boundary,
                _focus,
                _rotation,
                Arrays.hashCode(_fractions),
                _cycle
            );
    }

    @Override
    @SuppressWarnings("ReferenceEquality")
    public GradientConf simplified() {
        if ( this.equals(_NONE) )
            return _NONE;

        if ( _colors.length == 0 )
            return _NONE;

        if ( Arrays.stream(_colors).allMatch( color -> color.getAlpha() == 0 || color == UI.Color.UNDEFINED) )
            return _NONE;

        int numberOfRealColors = Arrays.stream(_colors).mapToInt( color -> color == UI.Color.UNDEFINED ? 0 : 1 ).sum();

        if ( numberOfRealColors == 0 )
            return _NONE;

        Offset focus = _focus;
        float  rotation = _rotation;

        if ( _type != UI.GradientType.RADIAL )
            focus = Offset.none();
        else
            if ( focus.equals(Offset.none()) )
                rotation = 0f; // If the focus is not set, then the rotation is irrelevant

        if ( numberOfRealColors != _colors.length ) {
            Color[] realColors = new Color[numberOfRealColors];
            int index = 0;
            for ( Color color : _colors )
                if ( color != UI.Color.UNDEFINED )
                    realColors[index++] = color;

            return of(_span, _type, realColors, _offset, _size, _area, _boundary, focus, rotation, _fractions, _cycle);
        }

        if ( !focus.equals(_focus) )
            return of(_span, _type, _colors, _offset, _size, _area, _boundary, focus, rotation, _fractions, _cycle);

        return this;
    }
}

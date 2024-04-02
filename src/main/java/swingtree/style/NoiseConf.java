package swingtree.style;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.UI;
import swingtree.api.NoiseFunction;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Color;
import java.util.Arrays;
import java.util.Objects;

/**
 *  A noise gradient configuration which is used to define a noise gradient style
 *  for a component based on a {@link NoiseFunction} which is a function
 *  that takes a coordinate and returns a value between 0 and 1. <br>
 *  The noise gradient is then defined by a list of colors which will transition from one
 *  to the next in the order they are specified. <br>
 *  The noise gradient can also be offset, scaled, rotated and clipped to a specific area of the component,
 *  and positioned at a specific boundary of the component.
 */
public final class NoiseConf implements Simplifiable<NoiseConf>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(NoiseConf.class);
    static final UI.Layer DEFAULT_LAYER = UI.Layer.BACKGROUND;

    private static final NoiseConf _NONE = new NoiseConf(
                                                        UI.NoiseType.STOCHASTIC,
                                                        new Color[0],
                                                        Offset.none(),
                                                        Offset.of(1f, 1f),
                                                        UI.ComponentArea.BODY,
                                                        UI.ComponentBoundary.EXTERIOR_TO_BORDER,
                                                        0f,
                                                        new float[0]
                                                    );

    /**
     *  Use the returned instance as a representation of the absence of a noise gradient.
     *
     *  @return A noise gradient without any colors, effectively
     *          representing the absence of a noise gradient.
     */
    public static NoiseConf none() { return _NONE; }

    static NoiseConf of(
        NoiseFunction        function,
        Color[]              colors,
        Offset               offset,
        Offset               scale,
        UI.ComponentArea     area,
        UI.ComponentBoundary boundary,
        float                rotation,
        float[]              fractions
    ) {
        // The rotation may be any number
        // which always has to be normalized to a value between -180 and 180
        rotation = ( (((rotation+180f) % 360f + 360f) % 360f) - 180f );

        NoiseConf none = none();
        if ( function   == none._function   &&
             colors     == none._colors     &&
             offset     == none._offset     &&
             scale      == none._scale      &&
             area       == none._area       &&
             boundary   == none._boundary   &&
             rotation   == none._rotation   &&
             Arrays.equals(fractions, none._fractions)
        )
            return none;

        return new NoiseConf(
            function,
            colors,
            offset,
            scale,
            area,
            boundary,
            rotation,
            fractions
        );
    }


    private final NoiseFunction        _function;
    private final Color[]              _colors;
    private final Offset               _offset;
    private final Offset               _scale;
    private final UI.ComponentArea     _area;
    private final UI.ComponentBoundary _boundary;
    private final float                _rotation;
    private final float[]              _fractions;


    private NoiseConf(
        NoiseFunction        function,
        Color[]              colors,
        Offset               offset,
        Offset               scale,
        UI.ComponentArea     area,
        UI.ComponentBoundary boundary,
        float                rotation,
        float[]              fractions
    ) {
        _function   = Objects.requireNonNull(function);
        _colors     = Objects.requireNonNull(colors);
        _offset     = Objects.requireNonNull(offset);
        _scale      = Objects.requireNonNull(scale);
        _area       = Objects.requireNonNull(area);
        _boundary   = Objects.requireNonNull(boundary);
        _rotation   = rotation;
        _fractions  = Objects.requireNonNull(fractions);
    }

    NoiseFunction function() { return _function; }
    
    Color[] colors() { return _colors; }

    Offset offset() { return _offset; }

    Offset scale() { return _scale; }

    UI.ComponentArea area() { return _area; }

    UI.ComponentBoundary boundary() { return _boundary; }

    float rotation() { return _rotation; }

    float[] fractions() { return _fractions; }


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

    NoiseConf _scale( double scale ) {
        if ( scale == 1 )
            return this;

        if ( this.equals(_NONE) )
            return _NONE;

        return of(
            _function,
            _colors,
            _offset,
            _scale.scale(scale),
            _area,
            _boundary,
            _rotation,
            _fractions
        );
    }

    /**
     *  Accepts the {@link NoiseFunction}, which takes a coordinate and returns a value
     *  between 0 and 1. <br>
     *  The noise function is used to define the noise gradient.
     *  <p>
     *  <b>Take a look at {@link UI.NoiseType} for a rich set of predefined noise functions.</b>
     *
     * @param function The noise function mapping the translated, scaled and rotated virtual space
     *                 to a gradient value of a pixel in the color space / view space of the screen.
     * @return A new noise gradient style with the specified noise function.
     */
    public NoiseConf function( NoiseFunction function ) {
        return of(function, _colors, _offset, _scale, _area, _boundary, _rotation, _fractions);
    }

    /**
     *  Define a list of colors which will, as part of the noise gradient, transition from one
     *  to the next in the order they are specified.
     *  <p>
     *  Note that you need to specify at least two colors for a noise gradient to be visible.
     *
     * @param colors The colors in the noise gradient.
     * @return A new noise gradient style with the specified colors.
     * @throws NullPointerException if any of the colors is {@code null}.
     */
    public NoiseConf colors( Color... colors ) {
        Objects.requireNonNull(colors);
        for ( Color color : colors )
            Objects.requireNonNull(color, "Use UI.Color.UNDEFINED instead of null to represent the absence of a color.");
        return of(_function, colors, _offset, _scale, _area, _boundary, _rotation, _fractions);
    }

    /**
     *  Define a list of {@link String} based colors which will, as part of the noise gradient, transition from one
     *  to the next in the order they are specified.
     *  <p>
     *  Note that you need to specify at least two colors for a noise gradient to be visible.
     *
     * @param colors The colors in the noise gradient in {@link String} format.
     * @return A new noise gradient style with the specified colors.
     * @throws NullPointerException if any of the colors is {@code null}.
     */
    public NoiseConf colors( String... colors ) {
        Objects.requireNonNull(colors);
        try {
            Color[] actualColors = new Color[colors.length];
            for ( int i = 0; i < colors.length; i++ )
                actualColors[i] = UI.color(colors[i]);

            return of(_function, actualColors, _offset, _scale, _area, _boundary, _rotation, _fractions);
        } catch ( Exception e ) {
            log.error("Failed to parse color strings: " + Arrays.toString(colors), e);
            return this; // We want to avoid side effects other than a wrong color
        }
    }

    /**
     *  Define the offset of the noise gradient which is the start position of the noise gradient
     *  on the x and y-axis. <br>
     *  Note that the offset is relative to the component that the noise gradient is applied to.
     *  <p>
     * @param x The noise gradient start offset on the x-axis.
     * @param y The noise gradient start offset on the y-axis.
     * @return A new noise gradient style with the specified offset.
     */
    public NoiseConf offset(double x, double y ) {
        return of(_function, _colors, Offset.of(x,y), _scale, _area, _boundary, _rotation, _fractions);
    }

    /**
     *  Define the scale of the noise gradient in terms of its size / granularity.
     *  It scales the input space of the noise function.
     *
     * @param scale The noise gradient size.
     * @return A new noise gradient style with the specified size.
     */
    public NoiseConf scale( double scale ) {
        return of(_function, _colors, _offset, Offset.of(scale, scale), _area, _boundary, _rotation, _fractions);
    }

    /**
     *  Define the x and y scale of the noise gradient in terms of its size / granularity.
     *  It scales the input space of the noise function.
     *
     * @param x The noise gradient size on the x-axis.
     * @param y The noise gradient size on the y-axis.
     * @return A new noise gradient style with the specified size.
     */
    public NoiseConf scale( double x, double y ) {
        return of(_function, _colors, _offset, Offset.of(x,y), _area, _boundary, _rotation, _fractions);
    }
    
    /**
     *  Define the area of the component to which the noise gradient is clipped to.
     *  Which means that the noise gradient will only be visible within the
     *  specified area of the component.
     *
     * @param area The area of the component to which the noise gradient is clipped to.
     * @return A new noise gradient style with the specified area.
     */
    public NoiseConf clipTo( UI.ComponentArea area ) {
        return of(_function, _colors, _offset, _scale, area, _boundary, _rotation, _fractions);
    }

    /**
     *  Define the boundary at which the noise gradient should start in terms of its base position.
     *  So if the boundary is set to {@link UI.ComponentBoundary#EXTERIOR_TO_BORDER}
     *  then the noise gradient position will be determined by the margin of the component. <br>
     *  Here a complete list of the available boundaries:
     * <ul>
     *     <li>{@link UI.ComponentBoundary#OUTER_TO_EXTERIOR} -
     *     The outermost boundary of the entire component, including any margin that might be applied.
     *     Using this boundary will cause the noise gradient to be positioned somewhere at
     *     the outer most edge of the component.
     *     </li>
     *     <li>{@link UI.ComponentBoundary#EXTERIOR_TO_BORDER} -
     *     The boundary located after the margin but before the border.
     *     This tightly wraps the entire {@link UI.ComponentArea#BODY}.
     *     Using this boundary will cause the noise gradient to be positioned somewhere at
     *     the outer most edge of the component's body, which is between the margin and the border.
     *     </li>
     *     <li>{@link UI.ComponentBoundary#BORDER_TO_INTERIOR} -
     *     The boundary located after the border but before the padding.
     *     It represents the edge of the component's interior.
     *     Using this boundary will cause the noise gradient to be positioned somewhere at
     *     the outer most edge of the component's interior, which is between the border and the padding area.
     *     </li>
     *     <li>{@link UI.ComponentBoundary#INTERIOR_TO_CONTENT} -
     *     The boundary located after the padding.
     *     It represents the innermost boundary of the component, where the actual content of the component begins,
     *     like for example the contents of a {@link JPanel} or {@link JScrollPane}.
     *     Using this boundary will cause the noise gradient to be positioned somewhere after the padding area
     *     and before the content area, which is where all of the child components are located.
     *     </li>
     * </ul>
     *  <p>
     *  You can think of this property as a convenient way to define the base position of the noise gradient.
     *  So if you want to do the positioning yourself, then you may configure this property to
     *  {@link UI.ComponentBoundary#OUTER_TO_EXTERIOR}, which will cause the noise gradient to be positioned
     *  at the outermost edge of the component, and then use the {@link #offset(double, double)} method
     *  to define the exact position of the noise gradient.
     *
     * @param boundary The boundary at which the noise gradient should start in terms of its offset.
     * @return A new noise gradient style with the specified boundary.
     */
    public NoiseConf boundary(UI.ComponentBoundary boundary ) {
        return of(_function, _colors, _offset, _scale, _area, boundary, _rotation, _fractions);
    }

    /**
     *  Define the rotation of the noise gradient in degrees.
     *  This will rotate the input space of the noise function.
     *
     *  @param rotation The rotation of the noise gradient in degrees.
     */
    public NoiseConf rotation( float rotation ) {
        return of(_function, _colors, _offset, _scale, _area, _boundary, rotation, _fractions);
    }

    /**
     *  Define the fractions of the noise gradient which is an array of values between 0 and 1
     *  that defines the relative position of each color in the noise gradient.
     *  <p>
     *  Note that the number of fractions must match the number of colors in the noise gradient.
     *  If the number of fractions is less than the number of colors, then the remaining
     *  colors will be evenly distributed between the last two fractions.
     *
     *  @param fractions The fractions of the noise gradient.
     */
    public NoiseConf fractions( double... fractions ) {
        float[] actualFractions = new float[fractions.length];
        for ( int i = 0; i < fractions.length; i++ )
            actualFractions[i] = (float) fractions[i];

        return of(_function, _colors, _offset, _scale, _area, _boundary, _rotation, actualFractions);
    }

    @Override
    public String toString() {
        if ( this == _NONE )
            return getClass().getSimpleName() + "[NONE]";
        return getClass().getSimpleName() + "[" +
                    "function="    + _function + ", " +
                    "colors="     + Arrays.toString(_colors) + ", " +
                    "offset="     + _offset + ", " +
                    "scale="      + _scale + ", " +
                    "area="       + _area + ", " +
                    "boundary="   + _boundary + ", " +
                    "rotation="   + _rotation + ", " +
                    "fractions="  + Arrays.toString(_fractions) +
                "]";
    }

    @Override
    public boolean equals( @Nullable Object o ) {
        if ( this == o ) return true;
        if ( !(o instanceof NoiseConf) ) return false;
        NoiseConf that = (NoiseConf) o;
        return Objects.equals(_function, that._function) &&
               Arrays.equals(_colors, that._colors)  &&
               Objects.equals(_offset, that._offset) &&
                Objects.equals(_scale, that._scale)   &&
               _area       == that._area             &&
               _boundary   == that._boundary         &&
               _rotation   == that._rotation         &&
               Arrays.equals(_fractions, that._fractions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                _function,
                Arrays.hashCode(_colors),
                _offset,
                _scale,
                _area,
                _boundary,
                _rotation,
                Arrays.hashCode(_fractions)
            );
    }

    @Override
    public NoiseConf simplified() {
        if ( this == _NONE )
            return _NONE;

        if ( _colors.length == 0 )
            return _NONE;

        if ( Arrays.stream(_colors).allMatch( color -> color.getAlpha() == 0 || StyleUtil.isUndefinedColor(color) ) )
            return _NONE;

        int numberOfRealColors = Arrays.stream(_colors).mapToInt( color -> StyleUtil.isUndefinedColor(color) ? 0 : 1 ).sum();

        if ( numberOfRealColors == 0 )
            return _NONE;

        if ( numberOfRealColors != _colors.length ) {
            Color[] realColors = new Color[numberOfRealColors];
            int index = 0;
            for ( Color color : _colors )
                if ( !StyleUtil.isUndefinedColor(color) )
                    realColors[index++] = color;

            return of( _function, realColors, _offset, _scale, _area, _boundary, _rotation, _fractions );
        }

        return this;
    }
}

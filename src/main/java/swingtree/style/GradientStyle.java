package swingtree.style;

import org.slf4j.Logger;
import swingtree.UI;
import swingtree.layout.Location;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

/**
 *  An immutable config API for specifying a gradient style.
 *  as a sub-style of various other styles,
 *  like for example {@link BaseStyle} or {@link BorderStyle} accessed through the
 *  {@link ComponentStyleDelegate#gradient(String, Function)}
 *  method.
 *  The state of a gradient style is immutable and can only be updated by
 *  wither like methods that return a new instance of the gradient style
 *  with the specified property updated.
 *  <p>
 *  The following properties with their respective purpose are available:
 *  <br>
 *  <ul>
 *      <li><h3>Transition</h3>
 *          The transition defines the direction of the gradient.
 *          <br>
 *          The following transitions are available:
 *          <ul>
 *              <li>{@link UI.Transition#TOP_LEFT_TO_BOTTOM_RIGHT}</li>
 *              <li>{@link UI.Transition#BOTTOM_LEFT_TO_TOP_RIGHT}</li>
 *              <li>{@link UI.Transition#TOP_RIGHT_TO_BOTTOM_LEFT}</li>
 *              <li>{@link UI.Transition#BOTTOM_RIGHT_TO_TOP_LEFT}</li>
 *              <li>{@link UI.Transition#TOP_TO_BOTTOM}</li>
 *              <li>{@link UI.Transition#LEFT_TO_RIGHT}</li>
 *              <li>{@link UI.Transition#BOTTOM_TO_TOP}</li>
 *              <li>{@link UI.Transition#RIGHT_TO_LEFT}</li>
 *          </ul>
 *      </li>
 *      <li><h3>Type</h3>
 *          The type defines the shape of the gradient
 *          which can be either linear or radial. <br>
 *          So the following types are available:
 *          <ul>
 *              <li>{@link UI.GradientType#LINEAR}</li>
 *              <li>{@link UI.GradientType#RADIAL}</li>
 *          </ul>
 *      </li>
 *      <li><h3>Colors</h3>
 *          An array of colors that will be used
 *          as a basis for the gradient transition.
 *      </li>
 *      <li><h3>Offset</h3>
 *          The offset defines the start position of the gradient
 *          on the x and y axis.
 *          This property, together with the {@link #transition(UI.Transition)}
 *          property, defines the start position and direction of the gradient.
 *      </li>
 *      <li><h3>Size</h3>
 *          The size defines the size of the gradient
 *          in terms of the distance from the start position of the gradient
 *          to the end position of the gradient.
 *          <br>
 *          If no size is specified, the size of the gradient will be
 *          based on the size of the component that the gradient is applied to.
 *      </li>
 *      <li><h3>Area</h3>
 *          The component are to which the gradient is clipped to.
 *          Which means that the gradient will only be visible within the
 *          specified area of the component.
 *      <br>
 *  </ul>
 *  <p>
 *  You can also use the {@link #none()} method to specify that no gradient should be used,
 *  as the instance returned by that method is a gradient without any colors, effectively
 *  making it a representation of the absence of a gradient style.
 */
public final class GradientStyle implements Simplifiable<GradientStyle>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GradientStyle.class);
    static final UI.Layer DEFAULT_LAYER = UI.Layer.BACKGROUND;

    private static final GradientStyle _NONE = new GradientStyle(
                                                        UI.Transition.TOP_TO_BOTTOM,
                                                        UI.GradientType.LINEAR,
                                                        new Color[0],
                                                        Offset.none(),
                                                        -1f,
                                                        UI.ComponentArea.BODY,
                                                        UI.ComponentBoundary.EXTERIOR_TO_BORDER
                                                    );

    /**
     *  Use the returned instance as a representation of the absence of a gradient.
     *
     *  @return A gradient without any colors, effectively
     *          representing the absence of a gradient.
     */
    public static GradientStyle none() { return _NONE; }


    private final UI.Transition        _transition;
    private final UI.GradientType      _type;
    private final Color[]              _colors;
    private final Offset               _offset;
    private final float                _size;
    private final UI.ComponentArea     _area;
    private final UI.ComponentBoundary _boundary;


    private GradientStyle(
        UI.Transition        transition,
        UI.GradientType      type,
        Color[]              colors,
        Offset               offset,
        float                size,
        UI.ComponentArea     area,
        UI.ComponentBoundary boundary
    ) {
        _transition = Objects.requireNonNull(transition);
        _type       = Objects.requireNonNull(type);
        _colors     = Objects.requireNonNull(colors);
        _offset     = Objects.requireNonNull(offset);
        _size       = ( size < 0 ? -1 : size );
        _area       = Objects.requireNonNull(area);
        _boundary   = Objects.requireNonNull(boundary);
    }

    UI.Transition transition() { return _transition; }

    UI.GradientType type() { return _type; }

    Color[] colors() { return _colors; }

    Offset offset() { return _offset; }

    float size() { return _size; }

    UI.ComponentArea area() { return _area; }

    UI.ComponentBoundary boundary() { return _boundary; }

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
    public GradientStyle colors( Color... colors ) {
        Objects.requireNonNull(colors);
        for ( Color color : colors )
            Objects.requireNonNull(color, "Use UI.COLOR_UNDEFINED instead of null to represent the absence of a color.");
        return new GradientStyle(_transition, _type, colors, _offset, _size, _area, _boundary);
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
    public GradientStyle colors( String... colors ) {
        Objects.requireNonNull(colors);
        try {
            Color[] actualColors = new Color[colors.length];
            for ( int i = 0; i < colors.length; i++ )
                actualColors[i] = UI.color(colors[i]);

            return new GradientStyle(_transition, _type, actualColors, _offset, _size, _area, _boundary);
        } catch ( Exception e ) {
            log.error("Failed to parse color strings: " + Arrays.toString(colors), e);
            return this; // We want to avoid side effects other than a wrong color
        }
    }

    /**
     *  Define the alignment of the gradient which is one of the following:
     *  <ul>
     *     <li>{@link UI.Transition#TOP_LEFT_TO_BOTTOM_RIGHT}</li>
     *     <li>{@link UI.Transition#BOTTOM_LEFT_TO_TOP_RIGHT}</li>
     *     <li>{@link UI.Transition#TOP_RIGHT_TO_BOTTOM_LEFT}</li>
     *     <li>{@link UI.Transition#BOTTOM_RIGHT_TO_TOP_LEFT}</li>
     *     <li>{@link UI.Transition#TOP_TO_BOTTOM}</li>
     *     <li>{@link UI.Transition#LEFT_TO_RIGHT}</li>
     *     <li>{@link UI.Transition#BOTTOM_TO_TOP}</li>
     *     <li>{@link UI.Transition#RIGHT_TO_LEFT}</li>
     *  </ul>
     *
     * @param transition The alignment of the gradient.
     * @return A new gradient style with the specified alignment.
     * @throws NullPointerException if the alignment is {@code null}.
     */
    public GradientStyle transition( UI.Transition transition ) {
        Objects.requireNonNull(transition);
        return new GradientStyle(transition, _type, _colors, _offset, _size, _area, _boundary);
    }

    /**
     *  Define the type of the gradient which is one of the following:
     *  <ul>
     *     <li>{@link UI.GradientType#LINEAR}</li>
     *     <li>{@link UI.GradientType#RADIAL}</li>
     *  </ul>
     *
     * @param type The type of the gradient.
     * @return A new gradient style with the specified type.
     * @throws NullPointerException if the type is {@code null}.
     */
    public GradientStyle type( UI.GradientType type ) {
        Objects.requireNonNull(type);
        return new GradientStyle(_transition, type, _colors, _offset, _size, _area, _boundary);
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
    public GradientStyle offset( double x, double y ) {
        return new GradientStyle(_transition, _type, _colors, Offset.of(x,y), _size, _area, _boundary);
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
    public GradientStyle size( double size ) {
        return new GradientStyle(_transition, _type, _colors, _offset, (float) size, _area, _boundary);
    }

    /**
     *  Define the area of the component to which the gradient is clipped to.
     *  Which means that the gradient will only be visible within the
     *  specified area of the component.
     *
     * @param area The area of the component to which the gradient is clipped to.
     * @return A new gradient style with the specified area.
     */
    public GradientStyle area( UI.ComponentArea area ) {
        return new GradientStyle(_transition, _type, _colors, _offset, _size, area, _boundary);
    }

    /**
     *  Define the boundary at which the gradient should start in terms of its offset.
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
     *
     *
     * @param boundary The boundary at which the gradient should start in terms of its offset.
     * @return A new gradient style with the specified boundary.
     */
    public GradientStyle boundary( UI.ComponentBoundary boundary ) {
        return new GradientStyle(_transition, _type, _colors, _offset, _size, _area, boundary);
    }

    @Override
    public String toString() {
        if ( this == _NONE )
            return getClass().getSimpleName() + "[NONE]";
        return getClass().getSimpleName() + "[" +
                    "transition=" + _transition + ", " +
                    "type="       + _type + ", " +
                    "colors="     + Arrays.toString(_colors) + ", " +
                    "offset="     + _offset + ", " +
                    "size="       + _size + ", " +
                    "area="       + _area +
                "]";
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !(o instanceof GradientStyle) ) return false;
        GradientStyle that = (GradientStyle) o;
        return _transition == that._transition       &&
               _type       == that._type             &&
               Arrays.equals(_colors, that._colors)  &&
               Objects.equals(_offset, that._offset) &&
               _size       == that._size             &&
               _area       == that._area             &&
               _boundary   == that._boundary;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_transition, _type, Arrays.hashCode(_colors), _offset, _size, _area, _boundary);
    }

    @Override
    public GradientStyle simplified() {
        if ( this == _NONE )
            return _NONE;

        if ( _colors.length == 0 )
            return _NONE;

        if ( Arrays.stream(_colors).allMatch( color -> color.getAlpha() == 0 || color == UI.COLOR_UNDEFINED) )
            return _NONE;

        int numberOfRealColors = Arrays.stream(_colors).mapToInt( color -> color == UI.COLOR_UNDEFINED ? 0 : 1 ).sum();

        if ( numberOfRealColors == 0 )
            return _NONE;

        if ( numberOfRealColors != _colors.length ) {
            Color[] realColors = new Color[numberOfRealColors];
            int index = 0;
            for ( Color color : _colors )
                if ( color != UI.COLOR_UNDEFINED)
                    realColors[index++] = color;

            return new GradientStyle(_transition, _type, realColors, _offset, _size, _area, _boundary);
        }

        return this;
    }
}

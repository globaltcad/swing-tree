package swingtree.style;

import java.awt.*;

/**
 *  A value object that represents the arc width and height of a rounded rectangle corner.
 */
final class Arc
{
    /**
     *  A factory method for creating an {@link Arc} from the provided width and height values.
     *
     * @param arcWidth The width of the arc.
     * @param arcHeight The height of the arc.
     * @return An {@link Arc} representing the 2 values.
     */
    static Arc of( int arcWidth, int arcHeight ) {
        return new Arc( arcWidth, arcHeight );
    }


    private final int _arcWidth;
    private final int _arcHeight;


    private Arc( int arcWidth, int arcHeight ) {
        _arcWidth  = arcWidth;
        _arcHeight = arcHeight;
    }

    /**
     * @return The width of the arc.
     */
    public int width() { return _arcWidth; }

    /**
     * @return The height of the arc.
     */
    public int height() { return _arcHeight; }

    /**
     *  Use this to scale the arc, which is usually needed for when you
     *  need to do custom painting logic which is also supposed to be
     *  high DPI screen aware, see methods like
     *  {@link swingtree.UI#scale(int)}, {@link swingtree.UI#scale(Rectangle)}...
     *
     * @param scale A scaling factor by which the values of this arc are multiplied.
     * @return A new scaled {@link Arc}.
     */
    Arc scale( double scale ) {
        return Arc.of(
                    (int) Math.round( _arcWidth  * scale ),
                    (int) Math.round( _arcHeight * scale )
                );
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + _arcWidth;
        hash = 31 * hash + _arcHeight;
        return hash;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;

        Arc rhs = (Arc) obj;
        return _arcWidth == rhs._arcWidth && _arcHeight == rhs._arcHeight;
    }
}

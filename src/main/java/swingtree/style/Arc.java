package swingtree.style;

import swingtree.UI;

import java.awt.*;

/**
 *  A value object that represents the arc width and height of a rounded rectangle corner
 *  as part of a component {@link StyleConf}.
 */
final class Arc
{
    private static final Arc _NONE = new Arc(-1, -1);

    /**
     * @return An {@link Arc} with width and height of -1.
     */
    static Arc none() { return _NONE; }

    /**
     *  A factory method for creating an {@link Arc} from the provided width and height values.
     *
     * @param arcWidth The width of the arc.
     * @param arcHeight The height of the arc.
     * @return An {@link Arc} representing the 2 values.
     */
    static Arc of( float arcWidth, float arcHeight ) {
        if ( arcWidth < 0 && arcHeight < 0 )
            return _NONE;
        return new Arc( arcWidth, arcHeight );
    }

    static Arc of( double arcWidth, double arcHeight ) {
        return Arc.of( (float) arcWidth, (float) arcHeight );
    }


    private final float _arcWidth;
    private final float _arcHeight;


    private Arc( float arcWidth, float arcHeight ) {
        _arcWidth  = Math.max(-1, arcWidth);
        _arcHeight = Math.max(-1, arcHeight);
    }

    /**
     * @return The width of the arc.
     */
    public float width() { return _arcWidth; }

    /**
     * @return The height of the arc.
     */
    public float height() { return _arcHeight; }

    /**
     *  Used to scale the arc, which is usually needed by the style engine
     *  to paint DPI scaled corners and also useful for when you
     *  need to do custom painting logic which is also supposed to be
     *  high DPI screen aware, see methods like
     *  {@link swingtree.UI#scale(int)}, {@link swingtree.UI#scale(Rectangle)}... <br>
     *  Also checkout {@link UI#scale()} to get the current scaling factor.
     *
     * @param scale A scaling factor by which the values of this arc are multiplied.
     * @return A new scaled {@link Arc}.
     */
    Arc scale( double scale ) {
        if ( this.equals(_NONE) )
            return _NONE;

        return Arc.of(
                (float) (_arcWidth  * scale),
                (float) (_arcHeight * scale)
            );
    }

    Arc simplified() {
        if ( this.equals(_NONE) )
            return _NONE;

        if ( _arcWidth <= 0 || _arcHeight <= 0 )
            return _NONE;

        return this;
    }

    @Override
    public String toString() {
        if ( this.equals(_NONE) )
            return "Arc[NONE]";
        return this.getClass().getSimpleName()+"[" +
                    "arcWidth="  + _toString(_arcWidth ) + ", "+
                    "arcHeight=" + _toString(_arcHeight) +
                "]";
    }

    static String _toString( float value ) {
        if ( value == -1 )
            return "?";
        else
            return String.valueOf(value).replace(".0", "");
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Float.floatToIntBits(_arcWidth);
        hash = 31 * hash + Float.floatToIntBits(_arcHeight);
        return hash;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;

        Arc other = (Arc) obj;
        return _arcWidth == other._arcWidth && _arcHeight == other._arcHeight;
    }
}

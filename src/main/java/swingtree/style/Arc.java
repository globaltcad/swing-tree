package swingtree.style;

/**
 *  A value object that represents the arc width and height of a rounded rectangle.
 */
final class Arc
{
    static Arc of( int arcWidth, int arcHeight ) {
        return new Arc( arcWidth, arcHeight );
    }


    private final int _arcWidth;
    private final int _arcHeight;

    private Arc( int arcWidth, int arcHeight ) {
        _arcWidth  = arcWidth;
        _arcHeight = arcHeight;
    }

    public int width() { return _arcWidth; }

    public int height() { return _arcHeight; }

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

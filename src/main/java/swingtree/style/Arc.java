package swingtree.style;

public class Arc
{
    private final int _arcWidth;
    private final int _arcHeight;

    public Arc( int arcWidth, int arcHeight ) {
        _arcWidth  = arcWidth;
        _arcHeight = arcHeight;
    }

    public int width() { return _arcWidth; }

    public int height() { return _arcHeight; }

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
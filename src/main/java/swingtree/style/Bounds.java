package swingtree.style;

/**
 *  An immutable value object that represents a position and size
 *  in the form of an x and y coordinate and a width and height.
 */
class Bounds
{
    private final static Bounds EMPTY = new Bounds(0, 0, 0, 0);

    public static Bounds none() {
        return EMPTY;
    }

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public static Bounds of( int x, int y, int width, int height ) {
        return new Bounds(x, y, width, height);
    }

    private Bounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    int x() { return x; }

    int y() { return y; }

    int width() { return width; }

    int height() { return height; }

    Bounds x( int x ) { return new Bounds(x, y, width, height); }

    Bounds y( int y ) { return new Bounds(x, y, width, height); }

    Bounds width( int width ) { return new Bounds(x, y, width, height); }

    Bounds height( int height ) { return new Bounds(x, y, width, height); }


    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "x="      + x      +", "+
                    "y="      + y      +", "+
                    "width="  + width  +", "+
                    "height=" + height +
                "]";
    }

    public boolean equals( int x, int y, int width, int height ) {
        return this.x      == x      &&
               this.y      == y      &&
               this.width  == width  &&
               this.height == height;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o == this ) return true;
        if ( o == null ) return false;
        if ( o.getClass() != this.getClass() ) return false;
        Bounds other = (Bounds) o;
        return x      == other.x      &&
               y      == other.y      &&
               width  == other.width  &&
               height == other.height;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + x;
        hash = 31 * hash + y;
        hash = 31 * hash + width;
        hash = 31 * hash + height;
        return hash;
    }
}

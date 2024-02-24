package swingtree.api;

import java.awt.Graphics2D;
import java.util.Objects;

/**
 *  A painter that paints a given data object using the given graphics context
 *  with {@link Object#hashCode()} and {@link Object#equals(Object)} implemented
 *  to delegate to the data object in order to allow for caching of
 *  what is painted.
 */
class CachablePainter implements Painter
{
    private final Object data;
    private final Painter painter;


    CachablePainter(Object data, Painter painter ) {
        this.data    = Objects.requireNonNull(data);
        this.painter = Objects.requireNonNull(painter);
    }

    @Override
    public void paint( Graphics2D g2d ) {
        painter.paint(g2d);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == this ) return true;
        if ( obj == null || obj.getClass() != getClass() ) return false;
        return data.equals(((CachablePainter)obj).data);
    }
}

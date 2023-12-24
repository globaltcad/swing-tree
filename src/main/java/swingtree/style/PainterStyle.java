package swingtree.style;

import swingtree.UI;
import swingtree.api.Painter;

import java.util.Objects;

/**
 *  An immutable, wither-like method chaining based config API for specifying a painter
 *  style for custom {@link Painter} implementations that are used to paint
 *  the inner area of a component on a specific layer.
 *  The following properties with their respective purpose are available:
 *  <br>
 *  <ol>
 *      <li><h3>Painter</h3>
 *          <p>
 *              The painter is a function that takes a {@link java.awt.Graphics2D} instance
 *              which is used to paint onto the inner area of a component.
 *          </p>
 *      </li>
 *  </ol>
 *  <p>
 *  Note that you can use the {@link #none()} method to specify that no painter should be used,
 *  as the instance returned by that method is a painter with a {@link Painter#none()} painter,
 *  effectively making it a representation of the absence of a painter.
 *  <p>
 */
final class PainterStyle
{
    static final UI.Layer DEFAULT_LAYER = UI.Layer.BACKGROUND;
    private static final PainterStyle _NONE = new PainterStyle(Painter.none());


    public static PainterStyle none() { return _NONE; }


    private final Painter _painter;


    private PainterStyle( Painter painter )
    {
        _painter = painter;
    }


    public Painter painter() { return _painter; }

    public PainterStyle painter(Painter painter) { return new PainterStyle(painter); }


    @Override
    public String toString() {
        if ( _painter == Painter.none() )
            return this.getClass().getSimpleName() + "[NONE]";
        else
            return this.getClass().getSimpleName() + "[" +
                    "painter=" + StyleUtility.toString(_painter) +
                ']';
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) return true;
        if ( !(o instanceof PainterStyle) ) return false;
        PainterStyle that = (PainterStyle) o;
        return _painter == that._painter;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_painter);
    }
}

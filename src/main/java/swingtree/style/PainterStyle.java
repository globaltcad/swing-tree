package swingtree.style;

import swingtree.UI;
import swingtree.api.Painter;

import java.util.Objects;

/**
 *  An immutable config API for specifying a painter
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
    private static final PainterStyle _NONE = new PainterStyle(Painter.none(), UI.ComponentArea.BODY);


    public static PainterStyle none() { return _NONE; }

    public static PainterStyle of( Painter painter, UI.ComponentArea area ) {
        if ( painter == Painter.none() )
            return none();
        else
            return new PainterStyle(painter, area);
    }


    private final Painter _painter;
    private final UI.ComponentArea _clipArea;


    private PainterStyle( Painter painter, UI.ComponentArea area )
    {
        _painter = Objects.requireNonNull(painter);
        _clipArea = Objects.requireNonNull(area);
    }


    public Painter painter() { return _painter; }

    public UI.ComponentArea clipArea() {
        return _clipArea;
    }

    @Override
    public String toString() {
        if ( _painter == Painter.none() )
            return this.getClass().getSimpleName() + "[NONE]";
        else
            return this.getClass().getSimpleName() + "[" +
                    "painter=" + StyleUtility.toString(_painter) + ", " +
                    "clipArea=" + _clipArea +
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

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
 *      <li><h3>Clip Area</h3>
 *          <p>
 *              The clip area specifies the area of the component that the painter
 *              should be clipped to, which means that the things painted
 *              will only be visible within the specified area.
 *          </p>
 *      </li>*
 *  </ol>
 *  <p>
 *  Note that you can use the {@link #none()} method to specify that no painter should be used,
 *  as the instance returned by that method is a painter with a {@link Painter#none()} painter,
 *  effectively making it a representation of the absence of a painter.
 *  <p>
 */
final class PainterConf
{
    private static final PainterConf _NONE = new PainterConf(Painter.none(), UI.ComponentArea.BODY);


    static PainterConf none() { return _NONE; }

    static PainterConf of( Painter painter, UI.ComponentArea area ) {
        if ( painter == Painter.none() )
            return none();
        else
            return new PainterConf(painter, area);
    }


    private final Painter _painter;
    private final UI.ComponentArea _clipArea;


    private PainterConf(Painter painter, UI.ComponentArea area )
    {
        _painter  = Objects.requireNonNull(painter);
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
                    "painter=" + StyleUtil.toString(_painter) + ", " +
                    "clipArea=" + _clipArea +
                ']';
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o )
            return true;
        if ( !(o instanceof PainterConf) )
            return false;

        PainterConf that = (PainterConf) o;

        return _painter.equals(that._painter) &&
               _clipArea == that._clipArea;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_painter, _clipArea);
    }
}

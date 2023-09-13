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
 *      <li><h3>Layer</h3>
 *          <p>
 *              In essence the layer is an enum instance which
 *              gives the painter a particular rank in the painting order.
 *              So the {@link swingtree.UI.Layer#BACKGROUND} will be painted first,
 *              followed by the {@link swingtree.UI.Layer#CONTENT} and so on...
 *              <br>
 *              The following layers are available:
 *          </p>
 *          <ul>
 *              <li>{@link UI.Layer#BACKGROUND}</li>
 *              <li>{@link UI.Layer#CONTENT}</li>
 *              <li>{@link UI.Layer#BORDER}</li>
 *              <li>{@link UI.Layer#FOREGROUND}</li>
 *          </ul>
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
    private static final PainterStyle _NONE = new PainterStyle(Painter.none(), UI.Layer.BACKGROUND);


    public static PainterStyle none() { return _NONE; }


    private final Painter _painter;
    private final UI.Layer _layer;


    private PainterStyle( Painter painter, UI.Layer layer )
    {
        _painter = painter;
        _layer   = layer;
    }


    public Painter painter() { return _painter; }

    public PainterStyle painter(Painter painter) { return new PainterStyle(painter, _layer); }

    public UI.Layer layer() { return _layer; }

    public PainterStyle layer( UI.Layer layer ) { return new PainterStyle(_painter, layer); }


    @Override
    public String toString() {
        return "PainterStyle[" +
                    "painter=" + StyleUtility.toString(_painter) + ", " +
                    "layer="   + _layer   +
                ']';
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) return true;
        if ( !(o instanceof PainterStyle) ) return false;
        PainterStyle that = (PainterStyle) o;
        return _painter == that._painter &&
               _layer == that._layer;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_painter, _layer);
    }
}

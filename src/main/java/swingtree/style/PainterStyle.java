package swingtree.style;

import swingtree.UI;
import swingtree.api.Painter;

import java.util.Objects;

public final class PainterStyle
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

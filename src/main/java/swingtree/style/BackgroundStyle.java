package swingtree.style;

import java.awt.*;
import java.util.Optional;
import java.util.function.Consumer;

public class BackgroundStyle
{
    private final Color _innerColor;
    private final Color _color;
    private final Consumer<Graphics2D> _renderer;

    public BackgroundStyle(
        Color innerColor,
        Color color,
        Consumer<Graphics2D> renderer
    ) {
        _innerColor = innerColor;
        _color      = color;
        _renderer   = renderer;
    }

    public Optional<Color> innerColor() { return Optional.ofNullable(_innerColor); }

    public Optional<Color> color() { return Optional.ofNullable(_color); }

    public Optional<Consumer<Graphics2D>> renderer() { return Optional.ofNullable(_renderer); }

    public BackgroundStyle withInnerColor( Color backgroundColor ) { return new BackgroundStyle(backgroundColor, _color, _renderer); }

    public BackgroundStyle withColor( Color outerBackgroundColor ) { return new BackgroundStyle(_innerColor, outerBackgroundColor, _renderer); }

    public BackgroundStyle withBackgroundRenderer(Consumer<Graphics2D> renderer) { return new BackgroundStyle(_innerColor, _color, renderer); }

    @Override
    public int hashCode() {
        return _innerColor.hashCode() + _color.hashCode() + _renderer.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        BackgroundStyle rhs = (BackgroundStyle) obj;
        return _innerColor.equals(rhs._innerColor) &&
               _color.equals(rhs._color) &&
               _renderer.equals(rhs._renderer);
    }

    @Override
    public String toString() {
        return "BackgroundStyle[" +
                    "innerColor=" + StyleUtility.toString(_innerColor) + ", " +
                    "color="      + StyleUtility.toString(_color) + ", " +
                    "renderer="   + _renderer +
                "]";
    }
}

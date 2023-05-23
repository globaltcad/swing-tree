package swingtree.style;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class BackgroundStyle
{
    private static final BackgroundStyle _NONE = new BackgroundStyle(null, null, null);

    public static BackgroundStyle none() { return _NONE; }

    private final Color _color;
    private final Color _foundationColor;
    private final Consumer<Graphics2D> _renderer;

    private BackgroundStyle(
        Color color,
        Color foundation,
        Consumer<Graphics2D> renderer
    ) {
        _color           = color;
        _foundationColor = foundation;
        _renderer        = renderer;
    }

    public Optional<Color> color() { return Optional.ofNullable(_color); }

    public Optional<Color> foundationColor() { return Optional.ofNullable(_foundationColor); }

    public Optional<Consumer<Graphics2D>> renderer() { return Optional.ofNullable(_renderer); }

    public BackgroundStyle withColor( Color color ) { return new BackgroundStyle(color, _foundationColor, _renderer); }

    public BackgroundStyle withFoundationColor( Color foundation ) { return new BackgroundStyle(_color, foundation, _renderer); }

    public BackgroundStyle withBackgroundRenderer(Consumer<Graphics2D> renderer) { return new BackgroundStyle(_color, _foundationColor, renderer); }

    @Override
    public int hashCode() { return Objects.hash(_color, _foundationColor, _renderer ); }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        BackgroundStyle rhs = (BackgroundStyle) obj;
        return Objects.equals(_color, rhs._color) &&
               Objects.equals(_foundationColor, rhs._foundationColor) &&
               Objects.equals(_renderer, rhs._renderer);
    }

    @Override
    public String toString() {
        return "BackgroundStyle[" +
                    "color="           + StyleUtility.toString(_color) + ", " +
                    "foundationColor=" + StyleUtility.toString(_foundationColor) + ", " +
                    "renderer="        + _renderer +
                "]";
    }
}

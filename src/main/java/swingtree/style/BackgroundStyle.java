package swingtree.style;

import java.awt.*;
import java.util.Optional;
import java.util.function.Consumer;

public class BackgroundStyle
{
    private final Color backgroundColor;
    private final Color outerBackgroundColor;
    private final Consumer<Graphics2D> renderer;

    public BackgroundStyle(
        Color backgroundColor,
        Color outerBackgroundColor,
        Consumer<Graphics2D> renderer
    ) {
        this.backgroundColor = backgroundColor;
        this.outerBackgroundColor = outerBackgroundColor;
        this.renderer =renderer;
    }

    public Optional<Color> color() { return Optional.ofNullable(backgroundColor); }

    public Optional<Color> outerColor() { return Optional.ofNullable(outerBackgroundColor); }

    public Optional<Consumer<Graphics2D>> renderer() { return Optional.ofNullable(renderer); }

    public BackgroundStyle withColor(Color backgroundColor) { return new BackgroundStyle(backgroundColor, outerBackgroundColor, renderer); }

    public BackgroundStyle withOuterColor(Color outerBackgroundColor) { return new BackgroundStyle(backgroundColor, outerBackgroundColor, renderer); }

    public BackgroundStyle withBackgroundRenderer(Consumer<Graphics2D> renderer) { return new BackgroundStyle(backgroundColor, outerBackgroundColor, renderer); }
}

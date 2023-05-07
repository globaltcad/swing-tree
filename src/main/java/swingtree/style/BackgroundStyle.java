package swingtree.style;

import java.awt.*;
import java.util.Optional;

public class BackgroundStyle
{
    private final Color backgroundColor;
    private final Color outerBackgroundColor;

    public BackgroundStyle(Color backgroundColor, Color outerBackgroundColor) {
        this.backgroundColor = backgroundColor;
        this.outerBackgroundColor = outerBackgroundColor;
    }

    public Optional<Color> color() { return Optional.ofNullable(backgroundColor); }

    public Optional<Color> outerColor() { return Optional.ofNullable(outerBackgroundColor); }

    public BackgroundStyle withColor(Color backgroundColor) { return new BackgroundStyle(backgroundColor, outerBackgroundColor); }

    public BackgroundStyle withOuterColor(Color outerBackgroundColor) { return new BackgroundStyle(backgroundColor, outerBackgroundColor); }
}

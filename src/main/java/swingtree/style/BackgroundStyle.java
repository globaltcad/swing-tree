package swingtree.style;

import java.awt.*;

public class BackgroundStyle
{
    private final Color backgroundColor;
    private final Color outerBackgroundColor;

    public BackgroundStyle(Color backgroundColor, Color outerBackgroundColor) {
        this.backgroundColor = backgroundColor;
        this.outerBackgroundColor = outerBackgroundColor;
    }

    public Color color() { return backgroundColor; }

    public Color outerColor() { return outerBackgroundColor; }

    public BackgroundStyle withColor(Color backgroundColor) { return new BackgroundStyle(backgroundColor, outerBackgroundColor); }

    public BackgroundStyle withOuterColor(Color outerBackgroundColor) { return new BackgroundStyle(backgroundColor, outerBackgroundColor); }
}

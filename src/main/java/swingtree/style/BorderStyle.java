package swingtree.style;

import java.awt.*;
import java.util.Optional;

public class BorderStyle {
    private final int borderArcWidth;
    private final int borderArcHeight;
    private final int borderThickness;
    private final Color borderColor;

    public BorderStyle(int borderArcWidth, int borderArcHeight, int borderThickness, Color borderColor) {
        this.borderArcWidth = borderArcWidth;
        this.borderArcHeight = borderArcHeight;
        this.borderThickness = borderThickness;
        this.borderColor = borderColor;
    }

    public int arcWidth() { return borderArcWidth; }

    public int arcHeight() { return borderArcHeight; }

    public int thickness() { return borderThickness; }

    public Optional<Color> color() { return Optional.ofNullable(borderColor); }

    public BorderStyle withArcWidth(int borderArcWidth) { return new BorderStyle(borderArcWidth, borderArcHeight, borderThickness, borderColor); }

    public BorderStyle withArcHeight(int borderArcHeight) { return new BorderStyle(borderArcWidth, borderArcHeight, borderThickness, borderColor); }

    public BorderStyle withWidth(int borderThickness) { return new BorderStyle(borderArcWidth, borderArcHeight, borderThickness, borderColor); }

    public BorderStyle withColor(Color borderColor) { return new BorderStyle(borderArcWidth, borderArcHeight, borderThickness, borderColor); }

}

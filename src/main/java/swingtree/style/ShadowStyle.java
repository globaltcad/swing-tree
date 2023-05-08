package swingtree.style;

import java.awt.*;
import java.util.Optional;

public class ShadowStyle
{
    private final int horizontalShadowOffset;
    private final int verticalShadowOffset;
    private final int shadowBlurRadius;
    private final int shadowSpreadRadius;
    private final Color shadowColor;
    private final boolean shadowInset;

    public ShadowStyle(
        int horizontalShadowOffset,
        int verticalShadowOffset,
        int shadowBlurRadius,
        int shadowSpreadRadius,
        Color shadowColor,
        boolean shadowInset
    ) {
        this.horizontalShadowOffset = horizontalShadowOffset;
        this.verticalShadowOffset = verticalShadowOffset;
        this.shadowBlurRadius = shadowBlurRadius;
        this.shadowSpreadRadius = shadowSpreadRadius;
        this.shadowColor = shadowColor;
        this.shadowInset = shadowInset;
    }

    public int horizontalOffset() { return horizontalShadowOffset; }

    public int verticalOffset() { return verticalShadowOffset; }

    public int blurRadius() { return shadowBlurRadius; }

    public int spreadRadius() { return shadowSpreadRadius; }

    public Optional<Color> color() { return Optional.ofNullable(shadowColor); }

    public boolean inset() { return shadowInset; }

    ShadowStyle withHorizontalOffset(int horizontalShadowOffset) { return new ShadowStyle(horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, shadowColor, shadowInset); }

    ShadowStyle withVerticalOffset(int verticalShadowOffset) { return new ShadowStyle(horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, shadowColor, shadowInset); }

    ShadowStyle withBlurRadius(int shadowBlurRadius) { return new ShadowStyle(horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, shadowColor, shadowInset); }

    ShadowStyle withSpreadRadius(int shadowSpreadRadius) { return new ShadowStyle(horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, shadowColor, shadowInset); }

    ShadowStyle withColor(Color shadowColor) { return new ShadowStyle(horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, shadowColor, shadowInset); }

    ShadowStyle withInset(boolean shadowInset) { return new ShadowStyle(horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, shadowColor, shadowInset); }

}

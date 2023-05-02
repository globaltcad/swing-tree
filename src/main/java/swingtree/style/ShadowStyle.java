package swingtree.style;

import java.awt.*;

public class ShadowStyle
{
    private final int horizontalShadowOffset;
    private final int verticalShadowOffset;
    private final int shadowBlurRadius;
    private final int shadowSpreadRadius;
    private final Color shadowColor;
    private final Color shadowBackgroundColor;
    private final boolean shadowInset;

    ShadowStyle(int horizontalShadowOffset, int verticalShadowOffset, int shadowBlurRadius, int shadowSpreadRadius, Color shadowColor, Color shadowBackgroundColor, boolean shadowInset) {
        this.horizontalShadowOffset = horizontalShadowOffset;
        this.verticalShadowOffset = verticalShadowOffset;
        this.shadowBlurRadius = shadowBlurRadius;
        this.shadowSpreadRadius = shadowSpreadRadius;
        this.shadowColor = shadowColor;
        this.shadowBackgroundColor = shadowBackgroundColor;
        this.shadowInset = shadowInset;
    }

    public int horizontalOffset() { return horizontalShadowOffset; }

    public int verticalOffset() { return verticalShadowOffset; }

    public int blurRadius() { return shadowBlurRadius; }

    public int spreadRadius() { return shadowSpreadRadius; }

    public Color color() { return shadowColor; }

    public Color backgroundColor() { return shadowBackgroundColor; }

    public boolean inset() { return shadowInset; }

    ShadowStyle withHorizontalOffset(int horizontalShadowOffset) { return new ShadowStyle(horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, shadowColor, shadowBackgroundColor, shadowInset); }

    ShadowStyle withVerticalOffset(int verticalShadowOffset) { return new ShadowStyle(horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, shadowColor, shadowBackgroundColor, shadowInset); }

    ShadowStyle withBlurRadius(int shadowBlurRadius) { return new ShadowStyle(horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, shadowColor, shadowBackgroundColor, shadowInset); }

    ShadowStyle withSpreadRadius(int shadowSpreadRadius) { return new ShadowStyle(horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, shadowColor, shadowBackgroundColor, shadowInset); }

    ShadowStyle withColor(Color shadowColor) { return new ShadowStyle(horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, shadowColor, shadowBackgroundColor, shadowInset); }

    ShadowStyle withBackgroundColor(Color shadowBackgroundColor) { return new ShadowStyle(horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, shadowColor, shadowBackgroundColor, shadowInset); }

    ShadowStyle withInset(boolean shadowInset) { return new ShadowStyle(horizontalShadowOffset, verticalShadowOffset, shadowBlurRadius, shadowSpreadRadius, shadowColor, shadowBackgroundColor, shadowInset); }

}

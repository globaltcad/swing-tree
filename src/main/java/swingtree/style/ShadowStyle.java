package swingtree.style;

import java.awt.*;
import java.util.Objects;
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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + horizontalShadowOffset;
        hash = 31 * hash + verticalShadowOffset;
        hash = 31 * hash + shadowBlurRadius;
        hash = 31 * hash + shadowSpreadRadius;
        hash = 31 * hash + Objects.hashCode(shadowColor);
        hash = 31 * hash + (shadowInset ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        ShadowStyle rhs = (ShadowStyle) obj;
        return horizontalShadowOffset == rhs.horizontalShadowOffset &&
               verticalShadowOffset   == rhs.verticalShadowOffset   &&
               shadowBlurRadius       == rhs.shadowBlurRadius       &&
               shadowSpreadRadius     == rhs.shadowSpreadRadius     &&
               Objects.equals(shadowColor, rhs.shadowColor)         &&
               shadowInset            == rhs.shadowInset;
    }

    @Override
    public String toString() {
        return "ShadowStyle[" +
            "horizontalOffset=" + horizontalShadowOffset + ", " +
            "verticalOffset="   + verticalShadowOffset + ", " +
            "blurRadius="       + shadowBlurRadius + ", " +
            "spreadRadius="     + shadowSpreadRadius + ", " +
            "color="            + StyleUtility.toString(shadowColor) + ", " +
            "shadowInset="            + shadowInset +
        "]";
    }

}

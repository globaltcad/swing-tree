package swingtree.style;

import java.awt.*;

public class StyleCollector 
{
    // Padding
    private PaddingStyle padding = new PaddingStyle(0,0,0,0);

    // Border
    private BorderStyle border = new BorderStyle(0,0,0, Color.BLACK);

    // Background
    private BackgroundStyle background = new BackgroundStyle(Color.WHITE, null);

    // Box Shadow
    private ShadowStyle shadow = new ShadowStyle(0,0,5,5, Color.BLACK, null, false);


    public StyleCollector() {}

    public StyleCollector pad( int top, int left, int right, int bottom ) {
        this.padding = new PaddingStyle(top, left, right, bottom);
        return this;
    }

    public StyleCollector pad( int padding ) {
        this.padding = new PaddingStyle(padding, padding, padding, padding);
        return this;
    }

    public StyleCollector padTop( int padding ) {
        this.padding = this.padding.withTop(padding);
        return this;
    }

    public StyleCollector padLeft( int padding ) {
        this.padding = this.padding.withLeft(padding);
        return this;
    }

    public StyleCollector padRight( int padding ) {
        this.padding = this.padding.withRight(padding);
        return this;
    }

    public StyleCollector padBottom( int padding ) {
        this.padding = this.padding.withBottom(padding);
        return this;
    }

    public StyleCollector border( int thickness, Color color ) {
        this.border = border.withThickness(thickness).withColor(color);
        return this;
    }

    public StyleCollector border( int thickness ) {
        this.border = border.withThickness(thickness);
        return this;
    }

    public StyleCollector border( Color color ) {
        this.border = border.withColor(color);
        return this;
    }

    public StyleCollector borderRadius(int radius ) {
        this.border = border.withArcWidth(radius).withArcHeight(radius);
        return this;
    }

    public StyleCollector borderRadius(int arcWidth, int arcHeight ) {
        this.border = border.withArcWidth(arcWidth).withArcHeight(arcHeight);
        return this;
    }

    public StyleCollector background( Color color ) {
        this.background = background.withColor(color);
        return this;
    }

    public StyleCollector outerBackground( Color color ) {
        this.background = background.withOuterColor(color);
        return this;
    }

    public StyleCollector shadowHorizontalOffset( int offset ) {
        this.shadow = shadow.withHorizontalOffset(offset);
        return this;
    }

    public StyleCollector shadowVerticalOffset(int offset ) {
        this.shadow = shadow.withVerticalOffset(offset);
        return this;
    }

    public StyleCollector shadowOffset(int horizontalOffset, int verticalOffset ) {
        this.shadow = shadow.withHorizontalOffset(horizontalOffset).withVerticalOffset(verticalOffset);
        return this;
    }

    public StyleCollector shadowBlurRadius(int radius) {
        this.shadow = shadow.withBlurRadius(radius);
        return this;
    }

    public StyleCollector shadowSpreadRadius(int radius ) {
        this.shadow = shadow.withSpreadRadius(radius);
        return this;
    }

    public StyleCollector shadowColor(Color color ) {
        this.shadow = shadow.withColor(color);
        return this;
    }

    public StyleCollector shadowBackgroundColor(Color color ) {
        this.shadow = shadow.withBackgroundColor(color);
        return this;
    }

    public StyleCollector shadowInset(boolean b) {
        this.shadow = shadow.withInset(b);
        return this;
    }

    public PaddingStyle padding() { return this.padding; }

    public BorderStyle border() { return border; }

    public BackgroundStyle background() { return background; }

    public ShadowStyle shadow() { return shadow; }

}

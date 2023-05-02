package swingtree.style;

import java.awt.*;

public class StyleCollector 
{
    private final PaddingStyle padding;
    private final BorderStyle border;
    private final BackgroundStyle background;
    private final ShadowStyle shadow;


    public StyleCollector(
        PaddingStyle padding,
        BorderStyle border,
        BackgroundStyle background,
        ShadowStyle shadow
    ) {
        this.padding = padding;
        this.border = border;
        this.background = background;
        this.shadow = shadow;
    }

    private StyleCollector _withPadding( PaddingStyle padding ) {
        return new StyleCollector(padding, border, background, shadow);
    }

    private StyleCollector _withBorder( BorderStyle border ) {
        return new StyleCollector(padding, border, background, shadow);
    }

    private StyleCollector _withBackground( BackgroundStyle background ) {
        return new StyleCollector(padding, border, background, shadow);
    }

    private StyleCollector _withShadow( ShadowStyle shadow ) {
        return new StyleCollector(padding, border, background, shadow);
    }

    public StyleCollector pad( int top, int left, int right, int bottom ) {
        return this._withPadding(padding.withTop(top).withLeft(left).withRight(right).withBottom(bottom));
    }

    public StyleCollector pad( int padding ) {
        return this._withPadding(this.padding.withTop(padding).withLeft(padding).withRight(padding).withBottom(padding));
    }

    public StyleCollector padTop( int padding ) {
        return this._withPadding(this.padding.withTop(padding));
    }

    public StyleCollector padLeft( int padding ) {
        return this._withPadding(this.padding.withLeft(padding));
    }

    public StyleCollector padRight( int padding ) {
        return this._withPadding(this.padding.withRight(padding));
    }

    public StyleCollector padBottom( int padding ) {
        return this._withPadding(this.padding.withBottom(padding));
    }

    public StyleCollector border( int thickness, Color color ) {
        return this._withBorder(border.withThickness(thickness).withColor(color));
    }

    public StyleCollector border( int thickness ) {
        return this._withBorder(border.withThickness(thickness));
    }

    public StyleCollector border( Color color ) {
        return this._withBorder(border.withColor(color));
    }

    public StyleCollector borderRadius(int radius ) {
        return this._withBorder(border.withArcWidth(radius).withArcHeight(radius));
    }

    public StyleCollector borderRadius(int arcWidth, int arcHeight ) {
        return this._withBorder(border.withArcWidth(arcWidth).withArcHeight(arcHeight));
    }

    public StyleCollector background( Color color ) {
        return this._withBackground(background.withColor(color));
    }

    public StyleCollector outerBackground( Color color ) {
        return this._withBackground(background.withOuterColor(color));
    }

    public StyleCollector shadowHorizontalOffset( int offset ) {
        return this._withShadow(shadow.withHorizontalOffset(offset));
    }

    public StyleCollector shadowVerticalOffset(int offset ) {
        return this._withShadow(shadow.withVerticalOffset(offset));
    }

    public StyleCollector shadowOffset(int horizontalOffset, int verticalOffset ) {
        return this._withShadow(shadow.withHorizontalOffset(horizontalOffset).withVerticalOffset(verticalOffset));
    }

    public StyleCollector shadowBlurRadius(int radius) {
        return this._withShadow(shadow.withBlurRadius(radius));
    }

    public StyleCollector shadowSpreadRadius(int radius ) {
        return this._withShadow(shadow.withSpreadRadius(radius));
    }

    public StyleCollector shadowColor(Color color ) {
        return this._withShadow(shadow.withColor(color));
    }

    public StyleCollector shadowBackgroundColor(Color color ) {
        return this._withShadow(shadow.withBackgroundColor(color));
    }

    public StyleCollector shadowInset(boolean b) {
        return this._withShadow(shadow.withInset(b));
    }

    public PaddingStyle padding() { return this.padding; }

    public BorderStyle border() { return border; }

    public BackgroundStyle background() { return background; }

    public ShadowStyle shadow() { return shadow; }

}

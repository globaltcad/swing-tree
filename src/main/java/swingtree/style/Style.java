package swingtree.style;

import java.awt.*;

public final class Style
{
    private final PaddingStyle padding;
    private final BorderStyle border;
    private final BackgroundStyle background;
    private final ShadowStyle shadow;


    public Style(
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

    private Style _withPadding(PaddingStyle padding ) {
        return new Style(padding, border, background, shadow);
    }

    private Style _withBorder(BorderStyle border ) {
        return new Style(padding, border, background, shadow);
    }

    private Style _withBackground(BackgroundStyle background ) {
        return new Style(padding, border, background, shadow);
    }

    private Style _withShadow(ShadowStyle shadow ) {
        return new Style(padding, border, background, shadow);
    }

    public Style pad( int top, int right, int bottom, int left ) {
        return this._withPadding(padding.withTop(top).withLeft(left).withRight(right).withBottom(bottom));
    }

    public Style pad(int padding ) {
        return this._withPadding(this.padding.withTop(padding).withLeft(padding).withRight(padding).withBottom(padding));
    }

    public Style padTop(int padding ) {
        return this._withPadding(this.padding.withTop(padding));
    }

    public Style padLeft(int padding ) {
        return this._withPadding(this.padding.withLeft(padding));
    }

    public Style padRight(int padding ) {
        return this._withPadding(this.padding.withRight(padding));
    }

    public Style padBottom(int padding ) {
        return this._withPadding(this.padding.withBottom(padding));
    }

    public Style border(int thickness, Color color ) {
        return this._withBorder(border.withThickness(thickness).withColor(color));
    }

    public Style border(int thickness ) {
        return this._withBorder(border.withThickness(thickness));
    }

    public Style border(Color color ) {
        return this._withBorder(border.withColor(color));
    }

    public Style borderRadius(int radius ) {
        return this._withBorder(border.withArcWidth(radius).withArcHeight(radius));
    }

    public Style borderRadius(int arcWidth, int arcHeight ) {
        return this._withBorder(border.withArcWidth(arcWidth).withArcHeight(arcHeight));
    }

    public Style background(Color color ) {
        return this._withBackground(background.withColor(color));
    }

    public Style outerBackground(Color color ) {
        return this._withBackground(background.withOuterColor(color));
    }

    public Style shadowHorizontalOffset(int offset ) {
        return this._withShadow(shadow.withHorizontalOffset(offset));
    }

    public Style shadowVerticalOffset(int offset ) {
        return this._withShadow(shadow.withVerticalOffset(offset));
    }

    public Style shadowOffset(int horizontalOffset, int verticalOffset ) {
        return this._withShadow(shadow.withHorizontalOffset(horizontalOffset).withVerticalOffset(verticalOffset));
    }

    public Style shadowBlurRadius(int radius) {
        return this._withShadow(shadow.withBlurRadius(radius));
    }

    public Style shadowSpreadRadius(int radius ) {
        return this._withShadow(shadow.withSpreadRadius(radius));
    }

    public Style shadowColor(Color color ) {
        return this._withShadow(shadow.withColor(color));
    }

    public Style shadowBackgroundColor(Color color ) {
        return this._withShadow(shadow.withBackgroundColor(color));
    }

    public Style shadowInset(boolean b) {
        return this._withShadow(shadow.withInset(b));
    }

    public PaddingStyle padding() { return this.padding; }

    public BorderStyle border() { return border; }

    public BackgroundStyle background() { return background; }

    public ShadowStyle shadow() { return shadow; }

}

package swingtree.style;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class Style
{
    private final PaddingStyle padding;
    private final BorderStyle border;
    private final BackgroundStyle _background;
    private final ShadowStyle shadow;
    private final FontStyle font;


    public Style(
        PaddingStyle padding,
        BorderStyle border,
        BackgroundStyle background,
        ShadowStyle shadow,
        FontStyle font
    ) {
        this.padding = padding;
        this.border = border;
        this._background = background;
        this.shadow = shadow;
        this.font = font;
    }

    private Style _withPadding(PaddingStyle padding ) {
        return new Style(padding, border, _background, shadow, font);
    }

    private Style _withBorder(BorderStyle border ) {
        return new Style(padding, border, _background, shadow, font);
    }

    private Style _withBackground(BackgroundStyle background ) {
        return new Style(padding, border, background, shadow, font);
    }

    private Style _withShadow( ShadowStyle shadow ) {
        return new Style(padding, border, _background, shadow, font);
    }

    private Style _withFont( FontStyle font ) {
        return new Style(padding, border, _background, shadow, font);
    }

    public Style pad( int top, int right, int bottom, int left ) {
        return _withPadding(padding.withTop(top).withLeft(left).withRight(right).withBottom(bottom));
    }

    public Style pad( int padding ) {
        return _withPadding(this.padding.withTop(padding).withLeft(padding).withRight(padding).withBottom(padding));
    }

    public Style padTop(int padding ) {
        return _withPadding(this.padding.withTop(padding));
    }

    public Style padLeft(int padding ) {
        return _withPadding(this.padding.withLeft(padding));
    }

    public Style padRight(int padding ) {
        return _withPadding(this.padding.withRight(padding));
    }

    public Style padBottom(int padding ) {
        return _withPadding(this.padding.withBottom(padding));
    }

    public Style border(int thickness, Color color ) {
        return _withBorder(border.withThickness(thickness).withColor(color));
    }

    public Style border(int thickness ) {
        return _withBorder(border.withThickness(thickness));
    }

    public Style border(Color color ) {
        return _withBorder(border.withColor(color));
    }

    public Style borderRadius(int radius ) {
        return this._withBorder(border.withArcWidth(radius).withArcHeight(radius));
    }

    public Style borderRadius(int arcWidth, int arcHeight ) {
        return _withBorder(border.withArcWidth(arcWidth).withArcHeight(arcHeight));
    }

    public Style innerBackground( Color color ) {
        return _withBackground(_background.withColor(color));
    }

    public Style background( Color color ) {
        return _withBackground(_background.withOuterColor(color));
    }

    public Style background( Consumer<Graphics2D> renderer ) {
        return _withBackground(_background.withBackgroundRenderer(renderer));
    }

    public Style shadowHorizontalOffset(int offset ) {
        return _withShadow(shadow.withHorizontalOffset(offset));
    }

    public Style shadowVerticalOffset(int offset ) {
        return _withShadow(shadow.withVerticalOffset(offset));
    }

    public Style shadowOffset(int horizontalOffset, int verticalOffset ) {
        return _withShadow(shadow.withHorizontalOffset(horizontalOffset).withVerticalOffset(verticalOffset));
    }

    public Style shadowBlurRadius( int radius ) {
        return _withShadow(shadow.withBlurRadius(radius));
    }

    public Style shadowSpreadRadius( int radius ) {
        return _withShadow(shadow.withSpreadRadius(radius));
    }

    public Style shadowColor( Color color ) {
        return _withShadow(shadow.withColor(color));
    }

    public Style shadowBackgroundColor( Color color ) {
        return _withShadow(shadow.withBackgroundColor());
    }

    public Style shadowInset( boolean b ) {
        return _withShadow(shadow.withInset(b));
    }

    public Style font( String name, int size ) {
        return _withFont(font.withName(name).withSize(size));
    }

    public Style font( String name ) {
        return _withFont(font.withName(name));
    }

    public Style fontSize( int size ) {
        return _withFont(font.withSize(size));
    }

    public Style fontBold( boolean b ) {
        return _withFont(font.withStyle( b ? Font.BOLD : Font.PLAIN ));
    }

    public Style fontItalic( boolean b ) {
        return _withFont(font.withStyle( b ? Font.ITALIC : Font.PLAIN ));
    }

    public Style fontUnderline( boolean b ) {
        List<TextAttribute> attributes = new ArrayList<>(font.attributes());
        if ( b ) attributes.add(TextAttribute.UNDERLINE);
        else     attributes.remove(TextAttribute.UNDERLINE);
        return _withFont(font.withAttributes(attributes));
    }

    public Style fontStrikeThrough( boolean b ) {
        List<TextAttribute> attributes = new ArrayList<>(font.attributes());
        if ( b ) attributes.add(TextAttribute.STRIKETHROUGH);
        else     attributes.remove(TextAttribute.STRIKETHROUGH);
        return _withFont(font.withAttributes(attributes));
    }

    public Style fontColor( Color color ) {
        return _withFont(font.withColor(color));
    }

    public Style fontBackgroundColor( Color color ) {
        return _withFont(font.withBackgroundColor(color));
    }

    public PaddingStyle padding() { return this.padding; }

    public BorderStyle border() { return border; }

    public BackgroundStyle background() { return _background; }

    public ShadowStyle shadow() { return shadow; }

    public FontStyle font() { return font; }

}

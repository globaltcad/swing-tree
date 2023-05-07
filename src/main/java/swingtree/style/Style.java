package swingtree.style;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class Style
{
    private final PaddingStyle _padding;
    private final BorderStyle _border;
    private final BackgroundStyle _background;
    private final ShadowStyle _shadow;
    private final FontStyle _font;


    public Style(
        PaddingStyle padding,
        BorderStyle border,
        BackgroundStyle background,
        ShadowStyle shadow,
        FontStyle font
    ) {
        _padding = padding;
        _border = border;
        _background = background;
        _shadow = shadow;
        _font = font;
    }

    private Style _withPadding(PaddingStyle padding ) {
        return new Style(padding, _border, _background, _shadow, _font);
    }

    private Style _withBorder(BorderStyle border ) {
        return new Style(_padding, border, _background, _shadow, _font);
    }

    private Style _withBackground(BackgroundStyle background ) {
        return new Style(_padding, _border, background, _shadow, _font);
    }

    private Style _withShadow( ShadowStyle shadow ) {
        return new Style(_padding, _border, _background, shadow, _font);
    }

    private Style _withFont( FontStyle font ) {
        return new Style(_padding, _border, _background, _shadow, font);
    }

    public Style pad( int top, int right, int bottom, int left ) {
        return _withPadding(_padding.withTop(top).withLeft(left).withRight(right).withBottom(bottom));
    }

    public Style pad( int padding ) {
        return _withPadding(this._padding.withTop(padding).withLeft(padding).withRight(padding).withBottom(padding));
    }

    public Style padTop(int padding ) {
        return _withPadding(this._padding.withTop(padding));
    }

    public Style padLeft(int padding ) {
        return _withPadding(this._padding.withLeft(padding));
    }

    public Style padRight(int padding ) {
        return _withPadding(this._padding.withRight(padding));
    }

    public Style padBottom(int padding ) {
        return _withPadding(this._padding.withBottom(padding));
    }

    public Style border(int thickness, Color color ) {
        return _withBorder(_border.withThickness(thickness).withColor(color));
    }

    public Style border(int thickness ) {
        return _withBorder(_border.withThickness(thickness));
    }

    public Style border(Color color ) {
        return _withBorder(_border.withColor(color));
    }

    public Style borderRadius(int radius ) {
        return this._withBorder(_border.withArcWidth(radius).withArcHeight(radius));
    }

    public Style borderRadius(int arcWidth, int arcHeight ) {
        return _withBorder(_border.withArcWidth(arcWidth).withArcHeight(arcHeight));
    }

    public Style innerBackground( Color color ) {
        return _withBackground(_background.withInnerColor(color));
    }

    public Style background( Color color ) {
        return _withBackground(_background.withColor(color));
    }

    public Style background( Consumer<Graphics2D> renderer ) {
        return _withBackground(_background.withBackgroundRenderer(renderer));
    }

    public Style shadowHorizontalOffset(int offset ) {
        return _withShadow(_shadow.withHorizontalOffset(offset));
    }

    public Style shadowVerticalOffset(int offset ) {
        return _withShadow(_shadow.withVerticalOffset(offset));
    }

    public Style shadowOffset(int horizontalOffset, int verticalOffset ) {
        return _withShadow(_shadow.withHorizontalOffset(horizontalOffset).withVerticalOffset(verticalOffset));
    }

    public Style shadowBlurRadius( int radius ) {
        return _withShadow(_shadow.withBlurRadius(radius));
    }

    public Style shadowSpreadRadius( int radius ) {
        return _withShadow(_shadow.withSpreadRadius(radius));
    }

    public Style shadowColor( Color color ) {
        return _withShadow(_shadow.withColor(color));
    }

    public Style shadowBackgroundColor( Color color ) {
        return _withShadow(_shadow.withBackgroundColor());
    }

    public Style shadowInset( boolean b ) {
        return _withShadow(_shadow.withInset(b));
    }

    public Style font( String name, int size ) {
        return _withFont(_font.withName(name).withSize(size));
    }

    public Style font( String name ) {
        return _withFont(_font.withName(name));
    }

    public Style font( Font font ) {
        return _withFont(_font.withFont(font));
    }

    public Style fontSize( int size ) {
        return _withFont(_font.withSize(size));
    }

    public Style fontBold( boolean b ) {
        return _withFont(_font.withStyle( b ? Font.BOLD : Font.PLAIN ));
    }

    public Style fontItalic( boolean b ) {
        return _withFont(_font.withStyle( b ? Font.ITALIC : Font.PLAIN ));
    }

    public Style fontUnderline( boolean b ) {
        List<TextAttribute> attributes = new ArrayList<>(_font.attributes());
        if ( b ) attributes.add(TextAttribute.UNDERLINE);
        else     attributes.remove(TextAttribute.UNDERLINE);
        return _withFont(_font.withAttributes(attributes));
    }

    public Style fontStrikeThrough( boolean b ) {
        List<TextAttribute> attributes = new ArrayList<>(_font.attributes());
        if ( b ) attributes.add(TextAttribute.STRIKETHROUGH);
        else     attributes.remove(TextAttribute.STRIKETHROUGH);
        return _withFont(_font.withAttributes(attributes));
    }

    public Style fontColor( Color color ) {
        return _withFont(_font.withColor(color));
    }

    public Style fontBackgroundColor( Color color ) {
        return _withFont(_font.withBackgroundColor(color));
    }

    public PaddingStyle padding() { return this._padding; }

    public BorderStyle border() { return _border; }

    public BackgroundStyle background() { return _background; }

    public ShadowStyle shadow() { return _shadow; }

    public FontStyle font() { return _font; }

}

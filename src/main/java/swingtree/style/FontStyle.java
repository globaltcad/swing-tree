package swingtree.style;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.util.*;
import java.util.List;

/**
 *  An immutable, wither-like cloner method based settings class for font styles
 *  that is part of the full {@link Style} configuration object.
 */
public final class FontStyle
{
    private static final FontStyle _NONE = new FontStyle("", 0, 0, 0, Collections.emptyList(), null, null, null);

    public static FontStyle none() { return _NONE; }

    private final String _name;
    private final int _size;
    private final int _style;
    private final int _weight;
    private final List<TextAttribute> _attributes;
    private final Color _color;
    private final Color _backgroundColor;
    private final Color _selectionColor;

    FontStyle(
        String name,
        int fontSize,
        int fontStyle,
        int fontWeight,
        List<TextAttribute> attributes,
        Color color,
        Color backgroundColor,
        Color selectionColor
    ) {
        _name = Objects.requireNonNull(name);
        _size = fontSize;
        _style = fontStyle;
        _weight = fontWeight;
        _attributes = Collections.unmodifiableList(Objects.requireNonNull(attributes));
        _color = color;
        _backgroundColor = backgroundColor;
        _selectionColor = selectionColor;
    }

    public String name() { return _name; }

    public int size() { return _size; }

    public int style() { return _style; }

    public int weight() { return _weight; }
    
    public List<TextAttribute> attributes() { return _attributes; }

    public Optional<Color> color() { return Optional.ofNullable(_color); }

    public Optional<Color> backgroundColor() { return Optional.ofNullable(_backgroundColor); }

    public Optional<Color> selectionColor() { return Optional.ofNullable(_selectionColor); }

    FontStyle name( String fontFamily ) { return new FontStyle(fontFamily, _size, _style, _weight, _attributes, _color, _backgroundColor, _selectionColor); }

    FontStyle size( int fontSize ) { return new FontStyle(_name, fontSize, _style, _weight, _attributes, _color, _backgroundColor, _selectionColor); }

    FontStyle style( int fontStyle ) { return new FontStyle(_name, _size, fontStyle, _weight, _attributes, _color, _backgroundColor, _selectionColor); }

    FontStyle weight( int fontWeight ) { return new FontStyle(_name, _size, _style, fontWeight, _attributes, _color, _backgroundColor, _selectionColor); }

    FontStyle attributes( TextAttribute... attributes ) {
        Objects.requireNonNull(attributes);
        return new FontStyle(_name, _size, _style, _weight, Arrays.asList(attributes), _color, _backgroundColor, _selectionColor);
    }

    FontStyle attributes( List<TextAttribute> attributes ) {
        Objects.requireNonNull(attributes);
        return new FontStyle(_name, _size, _style, _weight, attributes, _color, _backgroundColor, _selectionColor);
    }

    FontStyle color( Color color ) { return new FontStyle(_name, _size, _style, _weight, _attributes, color, _backgroundColor, _selectionColor); }

    FontStyle backgroundColor( Color backgroundColor ) { return new FontStyle(_name, _size, _style, _weight, _attributes, _color, backgroundColor, _selectionColor); }

    FontStyle selectionColor( Color selectionColor ) { return new FontStyle(_name, _size, _style, _weight, _attributes, _color, _backgroundColor, selectionColor); }

    FontStyle font( Font font ) {
        Objects.requireNonNull(font);
        return new FontStyle(
                    font.getFamily(),
                    font.getSize(),
                    font.getStyle(),
                    font.isBold() ? Font.BOLD : font.isItalic() ? Font.ITALIC : Font.PLAIN,
                    Collections.emptyList(),
                    _color,
                    _backgroundColor,
                    _selectionColor
                );
    }

    public Optional<Font> createDerivedFrom( Font existingFont )
    {
        if ( this.equals(_NONE) )
            return Optional.empty();

        boolean isChange = false;

        if ( existingFont == null )
            existingFont = new JLabel().getFont();

        Map<TextAttribute, Object> currentAttributes = (Map<TextAttribute, Object>) existingFont.getAttributes();
        Map<TextAttribute, Object> attributes = new HashMap<>();

        if ( _size > 0 ) {
            isChange = isChange || !Integer.valueOf(_size).equals(currentAttributes.get(TextAttribute.SIZE));
            attributes.put(TextAttribute.SIZE, _size);
        }
        if ( _style > 0 ) {
            isChange = isChange || !Integer.valueOf(_style).equals(currentAttributes.get(TextAttribute.POSTURE));
            attributes.put(TextAttribute.POSTURE, _style);
        }
        if ( _weight > 0 ) {
            isChange = isChange || !Integer.valueOf(_weight).equals(currentAttributes.get(TextAttribute.WEIGHT));
            attributes.put(TextAttribute.WEIGHT, _weight);
        }
        if ( !_name.isEmpty() ) {
            isChange = isChange || !Objects.equals(_name, currentAttributes.get(TextAttribute.FAMILY));
            attributes.put(TextAttribute.FAMILY, _name);
        }
        if ( _color != null ) {
            isChange = isChange || !Objects.equals(_color, currentAttributes.get(TextAttribute.FOREGROUND));
            attributes.put(TextAttribute.FOREGROUND, _color);
        }
        if ( _backgroundColor != null ) {
            isChange = isChange || !Objects.equals(_backgroundColor, currentAttributes.get(TextAttribute.BACKGROUND));
            attributes.put(TextAttribute.BACKGROUND, _backgroundColor);
        }

        if ( _attributes != null ) {
            _attributes.forEach( attr -> attributes.put(attr, attr));
        }

        if ( isChange )
            return Optional.of(existingFont.deriveFont(attributes));
        else
            return Optional.empty();
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(_name);
        hash = 97 * hash + _size;
        hash = 97 * hash + _style;
        hash = 97 * hash + _weight;
        hash = 97 * hash + Objects.hashCode(_attributes);
        hash = 97 * hash + Objects.hashCode(_color);
        hash = 97 * hash + Objects.hashCode(_backgroundColor);
        hash = 97 * hash + Objects.hashCode(_selectionColor);
        return hash;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        final FontStyle other = (FontStyle)obj;
        if ( !Objects.equals(_name, other._name) )
            return false;
        if ( _size != other._size )
            return false;
        if ( _style != other._style )
            return false;
        if ( _weight != other._weight )
            return false;
        if ( !Objects.equals(_attributes, other._attributes) )
            return false;
        if ( !Objects.equals(_color, other._color) )
            return false;
        if ( !Objects.equals(_backgroundColor, other._backgroundColor) )
            return false;
        if ( !Objects.equals(_selectionColor, other._selectionColor) )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "FontStyle[" +
                    "name="            + _name + ", " +
                    "size="            + _size + ", " +
                    "style="           + _style + ", " +
                    "weight="          + _weight + ", " +
                    "attributes=["     + _attributes.stream().map(AttributedCharacterIterator.Attribute::toString).reduce( (a, b) -> a + "," + b ).orElse("") + "], " +
                    "color="           + StyleUtility.toString(_color) + ", " +
                    "backgroundColor=" + StyleUtility.toString(_backgroundColor) + ", " +
                    "selectionColor="  + StyleUtility.toString(_selectionColor) +
                "]";
    }
}

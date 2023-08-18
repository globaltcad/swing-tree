package swingtree.style;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;

/**
 *  An immutable, wither-like cloner method based settings class for basic component style
 *  properties like background color, foundation color, foreground color and the cursor.
 *  Instances of this are part of the full {@link Style} configuration that is used to
 *  style a component in all kinds of ways.
 */
public final class BaseStyle
{
    private static final BaseStyle _NONE = new BaseStyle(
                                                     null,
                                                     null,
                                                     null,
                                                     null
                                                 );

    public static BaseStyle none() { return _NONE; }

    private final Color _backgroundColor;
    private final Color _foundationColor;
    private final Color _foregroundColor;
    private final Cursor _cursor;



    private BaseStyle(
        Color color,
        Color foundation,
        Color foregroundColor,
        Cursor cursor
    ) {
        _backgroundColor = color;
        _foundationColor = foundation;
        _foregroundColor = foregroundColor;
        _cursor          = cursor;
    }

    public Optional<Color> backgroundColor() { return Optional.ofNullable(_backgroundColor); }

    public Optional<Color> foundationColor() { return Optional.ofNullable(_foundationColor); }

    public Optional<Color> foregroundColo() { return Optional.ofNullable(_foregroundColor); }

    public Optional<Cursor> cursor() { return Optional.ofNullable(_cursor); }

    BaseStyle backgroundColor( Color color ) { return new BaseStyle(color, _foundationColor, _foregroundColor, _cursor); }

    BaseStyle foundationColor( Color foundation ) { return new BaseStyle(_backgroundColor, foundation, _foregroundColor, _cursor); }

    BaseStyle foregroundColo( Color color ) { return new BaseStyle(_backgroundColor, _foundationColor, color, _cursor); }

    BaseStyle cursor( Cursor cursor ) { return new BaseStyle(_backgroundColor, _foundationColor, _foregroundColor, cursor); }

    @Override
    public int hashCode() { return Objects.hash(_backgroundColor, _foundationColor, _foregroundColor, _cursor); }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        BaseStyle rhs = (BaseStyle) obj;
        return Objects.equals(_backgroundColor, rhs._backgroundColor) &&
               Objects.equals(_foundationColor, rhs._foundationColor) &&
               Objects.equals(_foregroundColor, rhs._foregroundColor) &&
               Objects.equals(_cursor,          rhs._cursor);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "backgroundColor=" + StyleUtility.toString(_backgroundColor) + ", " +
                    "foundationColor=" + StyleUtility.toString(_foundationColor) + ", " +
                    "foregroundColor=" + StyleUtility.toString(_foregroundColor) + ", " +
                    "cursor="          + ( _cursor     == null ? "?" : _cursor.toString()     ) +
                "]";
    }
}

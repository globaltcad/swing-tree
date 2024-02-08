package swingtree.style;

import swingtree.UI;

import javax.swing.ImageIcon;
import java.awt.*;
import java.util.Objects;
import java.util.Optional;

/**
 *  An immutable, wither-like copy method based config object for basic component style
 *  properties like background color, foundation color, foreground color and the cursor.
 *  Instances of this are part of the full {@link StyleConf} configuration that is used to
 *  style a component in all kinds of ways.
 */
final class BaseConf
{
    private static final BaseConf _NONE = new BaseConf(
                                                    null,
                                                    UI.FitComponent.NO,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    UI.ComponentOrientation.UNKNOWN
                                                 );

    /**
     *  Returns the default {@link BaseConf} which represents the absence of a base style.
     *
     * @return The default {@link BaseConf}, containing no style properties.
     */
    public static BaseConf none() { return _NONE; }


    private final ImageIcon               _icon;
    private final UI.FitComponent         _fit;
    private final Color                   _foundationColor;
    private final Color                   _backgroundColor;
    private final Color                   _foregroundColor;
    private final Cursor                  _cursor;
    private final UI.ComponentOrientation _orientation;


    private BaseConf(
        ImageIcon               icon,
        UI.FitComponent         fit,
        Color                   foundation,
        Color                   background,
        Color                   foregroundColor,
        Cursor                  cursor,
        UI.ComponentOrientation orientation
    ) {
        _icon            = icon;
        _fit             = Objects.requireNonNull(fit);
        _foundationColor = foundation;
        _backgroundColor = background;
        _foregroundColor = foregroundColor;
        _cursor          = cursor;
        _orientation     = orientation;
    }

    public Optional<ImageIcon> icon() { return Optional.ofNullable(_icon); }

    public UI.FitComponent fit() { return _fit; }

    public Optional<Color> foundationColor() { return Optional.ofNullable(_foundationColor); }

    public Optional<Color> backgroundColor() { return Optional.ofNullable(_backgroundColor); }

    public Optional<Color> foregroundColor() { return Optional.ofNullable(_foregroundColor); }

    public Optional<Cursor> cursor() { return Optional.ofNullable(_cursor); }

    public UI.ComponentOrientation orientation() { return _orientation; }

    boolean hasAnyColors() {
        return _backgroundColor != null || _foundationColor != null || _foregroundColor != null;
    }

    BaseConf icon(ImageIcon icon ) {
        return new BaseConf(icon, _fit, _foundationColor, _backgroundColor, _foregroundColor, _cursor, _orientation);
    }

    BaseConf fit(UI.FitComponent fit ) {
        return new BaseConf(_icon, fit, _foundationColor, _backgroundColor, _foregroundColor, _cursor, _orientation);
    }

    BaseConf foundationColor(Color foundation ) {
        return new BaseConf(_icon, _fit, foundation, _backgroundColor, _foregroundColor, _cursor, _orientation);
    }

    BaseConf backgroundColor(Color color ) {
        return new BaseConf(_icon, _fit, _foundationColor, color, _foregroundColor, _cursor, _orientation);
    }

    BaseConf foregroundColor(Color color ) {
        return new BaseConf(_icon, _fit, _foundationColor, _backgroundColor, color, _cursor, _orientation);
    }

    BaseConf cursor(Cursor cursor ) {
        return new BaseConf(_icon, _fit, _foundationColor, _backgroundColor, _foregroundColor, cursor, _orientation);
    }

    BaseConf orientation(UI.ComponentOrientation orientation ) {
        return new BaseConf(_icon, _fit, _foundationColor, _backgroundColor, _foregroundColor, _cursor, orientation);
    }

    BaseConf simplified() {
        Color simplifiedFoundation = _foundationColor == UI.COLOR_UNDEFINED ? null : _foundationColor;

        if ( simplifiedFoundation == _foundationColor )
            return this;

        return new BaseConf(_icon, _fit, simplifiedFoundation, _backgroundColor, _foregroundColor, _cursor, _orientation);
    }

    @Override
    public int hashCode() { return Objects.hash(_icon, _fit, _backgroundColor, _foundationColor, _foregroundColor, _cursor, _orientation); }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        BaseConf rhs = (BaseConf) obj;

        boolean sameBackground = Objects.equals(_backgroundColor, rhs._backgroundColor);
        if ( _backgroundColor == UI.COLOR_UNDEFINED || rhs._backgroundColor == UI.COLOR_UNDEFINED )
            sameBackground = _backgroundColor == rhs._backgroundColor;

        boolean sameFoundation = Objects.equals(_foundationColor, rhs._foundationColor);
        if ( _foundationColor == UI.COLOR_UNDEFINED || rhs._foundationColor == UI.COLOR_UNDEFINED )
            sameFoundation = _foundationColor == rhs._foundationColor;

        boolean sameForeground = Objects.equals(_foregroundColor, rhs._foregroundColor);
        if ( _foregroundColor == UI.COLOR_UNDEFINED || rhs._foregroundColor == UI.COLOR_UNDEFINED )
            sameForeground = _foregroundColor == rhs._foregroundColor;

        return Objects.equals(_icon,            rhs._icon           ) &&
               Objects.equals(_fit,             rhs._fit            ) &&
               sameBackground                                         &&
               sameFoundation                                         &&
               sameForeground                                         &&
               Objects.equals(_cursor,          rhs._cursor         ) &&
               Objects.equals(_orientation,     rhs._orientation    );
    }

    @Override
    public String toString() {
        if ( this == _NONE )
            return this.getClass().getSimpleName() + "[NONE]";
        return this.getClass().getSimpleName() + "[" +
                    "icon="            + ( _icon       == null ? "?" : _icon.toString()  ) + ", " +
                    "fitComponent="    + _fit                                              + ", " +
                    "backgroundColor=" + StyleUtility.toString(_backgroundColor)           + ", " +
                    "foundationColor=" + StyleUtility.toString(_foundationColor)           + ", " +
                    "foregroundColor=" + StyleUtility.toString(_foregroundColor)           + ", " +
                    "cursor="          + ( _cursor == null ? "?" : _cursor.toString() )    + ", " +
                    "orientation="     + _orientation                                      +
                "]";
    }
}

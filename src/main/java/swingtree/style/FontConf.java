package swingtree.style;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.function.Function;

/**
 *  An immutable, wither-like method based config API for font styles
 *  that is part of the full {@link StyleConf} configuration object.
 *  <p>
 *  The following properties with their respective purpose are available:
 *  <br>
 *  <ol>
 *      <li><h3>Name</h3>
 *          <p>
 *              The name of the font, which is essentially the font family.
 *              This will ultimately translate to {@link Font#getFamily()}.<br>
 *              You may specify the font family name through the {@link #family(String)} method.
 *          </p>
 *      </li>
 *      <li><h3>Size</h3>
 *          <p>
 *              The size of the font in points,
 *              which will ultimately translate to {@link Font#getSize()}.
 *              Use the {@link #size(int)} method to specify the size of the font.
 *          </p>
 *      </li>
 *      <li><h3>Posture</h3>
 *          <p>
 *              The posture of the font, which is a value between 0 and 1.
 *              <br>
 *              A value of 0 means that the font is not italic,
 *              while a value of 1 means that the font is "fully" italic.
 *              <br>
 *              You can use the {@link #posture(float)} method to specify the posture of the font.
 *          </p>
 *      </li>
 *      <li><h3>Weight</h3>
 *          <p>
 *              The weight of the font (boldness, see {@link Font#BOLD}),
 *              which is a value between 0 and 2.
 *          </p>
 *          <p>
 *              The weight of the font can be specified using the {@link #weight(float)} method.
 *          </p>
 *      </li>
 *      <li><h3>Spacing (Tracking)</h3>
 *          <p>
 *              This property controls the tracking which is a floating point number
 *              with the default value of
 *              {@code 0}, meaning no additional tracking is added to the font.
 *             
 *              <p>Useful constant values are the predefined {@link TextAttribute#TRACKING_TIGHT} and {@link
 *              TextAttribute#TRACKING_LOOSE} values, which represent values of {@code -0.04} and {@code 0.04},
 *             
 *              <p>The tracking value is multiplied by the font point size and
 *              passed through the font transform to determine an additional
 *              amount to add to the advance of each glyph cluster.  Positive
 *              tracking values will inhibit formation of optional ligatures.
 *              Tracking values are typically between {@code -0.1} and
 *              {@code 0.3}; values outside this range are generally not
 *              desirable.
 *          </p>
 *          <p>
 *              You can use the {@link #spacing(float)} method to specify the tracking of the font.
 *          </p>
 *      </li>
 *      <li><h3>Color</h3>
 *          <p>
 *              The color of the font, which translates to the text property
 *              {@link TextAttribute#FOREGROUND}.
 *          </p>
 *          <p>
 *              You can use the {@link #color(Color)} or {@link #color(String)} methods to specify the color of the font.
 *          </p>
 *      </li>
 *      <li><h3>Background Color</h3>
 *          <p>
 *              The background color of the font
 *              which translates to the text property {@link TextAttribute#BACKGROUND}.
 *          </p>
 *      </li>
 *      <li><h3>Selection Color</h3>
 *          <p>
 *              The selection color of the font, which translates to
 *              {@link javax.swing.text.JTextComponent#setSelectionColor(Color)}.
 *              <br>
 *              Note that this property is only relevant for text components,
 *              most components do not support text selection.
 *          </p>
 *      </li>
 *      <li><h3>Underlined</h3>
 *          <p>
 *              Whether or not the font is underlined.
 *              This will ultimately translate to {@link TextAttribute#UNDERLINE}.
 *          </p>
 *      </li>
 *      <li><h3>Strike</h3>
 *          <p>
 *              Whether or not the font is strike through.
 *              This will ultimately translate to {@link TextAttribute#STRIKETHROUGH}.
 *          </p>
 *      </li>
 *      <li><h3>Transform</h3>
 *          <p>
 *              The transform of the font, which is an {@link AffineTransform} instance.
 *          </p>
 *      </li>
 *      <li><h3>Paint</h3>
 *          <p>
 *              The paint of the font, which is a {@link Paint} instance.
 *              Note that specifying a custom paint will override the effects of the color property
 *              as the color property is in essence merely a convenience property for a
 *              paint painting across the entire font area homogeneously using the specified color.
 *          </p>
 *      </li>
 *      <li><h3>Background Paint</h3>
 *          <p>
 *              The background paint of the font, which is a {@link Paint} instance
 *              that is used to paint the background of the font.
 *          </p>
 *      </li>
 *      <li><h3>Horizontal Alignment</h3>
 *          <p>
 *              The horizontal alignment of the font.
 *              <br>
 *              Note that this property is not relevant for all components,
 *              It will usually only be relevant for {@link JLabel}, {@link AbstractButton} and {@link JTextField}
 *              types or maybe some custom components.
 *              Not all components support horizontal alignment.
 *          </p>
 *      </li>
 *      <li><h3>Vertical Alignment</h3>
 *          <p>
 *              The vertical alignment of the font.
 *              <br>
 *              Note that this property is not relevant for all components,
 *              It will usually only be relevant for {@link JLabel}, {@link AbstractButton} and {@link JTextField}
 *              types or maybe some custom components.
 *              Not all components support vertical alignment.
 *          </p>
 *      </li>
 *  </ol>
 *  <p>
 *  You can use the {@link #none()} method to specify that no font should be used,
 *  as the instance returned by that method is a font style with an empty string as its name,
 *  and other properties set to their default values,
 *  effectively making it a representation of the absence of a font style.
 *  <p>
 *  Also note that this class is immutable, which means that wither-like methods
 *  will always return new instances of this class, leaving the original instance untouched.
 *  <br>
 *  This means that you can not modify a font style instance directly, but you can
 *  easily create a modified copy of it by calling one of the wither-like methods.
 *  <p>
 */
public final class FontConf
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FontConf.class);

    private static final FontConf _NONE = new FontConf(
                                                        "",    // Font name (family)
                                                        0,     // size
                                                        0,     // posture
                                                        0,     // weight
                                                        0,     // spacing
                                                        null,  // color
                                                        null,  // background color
                                                        null,  // selection color
                                                        null,  // is underlined
                                                        null,  // is strike through
                                                        null,  // transform
                                                        null,  // paint
                                                        null,
                                                        UI.HorizontalAlignment.UNDEFINED,  // horizontal alignment
                                                        UI.VerticalAlignment.UNDEFINED   // vertical alignment
                                                    );

    public static FontConf none() { return _NONE; }

    static FontConf of(
        String                     name,
        int                        fontSize,
        float                      posture,
        float                      weight,
        float                      spacing,
        @Nullable Color            color,
        @Nullable Color            backgroundColor,
        @Nullable Color            selectionColor,
        @Nullable Boolean          isUnderline,
        @Nullable Boolean          isStrike,
        @Nullable AffineTransform  transform,
        @Nullable Paint            paint,
        @Nullable Paint            backgroundPaint,
        UI.HorizontalAlignment     horizontalAlignment,
        UI.VerticalAlignment       verticalAlignment
    ) {
        if (
            name.isEmpty() &&
            fontSize == 0 &&
            posture == 0 &&
            weight == 0 &&
            spacing == 0 &&
            color == null &&
            backgroundColor == null &&
            selectionColor == null &&
            isUnderline == null &&
            isStrike == null &&
            transform == null &&
            paint == null &&
            backgroundPaint == null &&
            horizontalAlignment == _NONE._horizontalAlignment &&
            verticalAlignment == _NONE._verticalAlignment
        )
            return _NONE;
        else
            return new FontConf(
                    name,
                    fontSize,
                    posture,
                    weight,
                    spacing,
                    color,
                    backgroundColor,
                    selectionColor,
                    isUnderline,
                    isStrike,
                    transform,
                    paint,
                    backgroundPaint,
                    horizontalAlignment,
                    verticalAlignment
                );
    }

    private final String                    _familyName;
    private final int                       _size;
    private final float                     _posture;
    private final float                     _weight;
    private final float                     _spacing;
    private final @Nullable Color           _color;
    private final @Nullable Color           _backgroundColor;
    private final @Nullable Color           _selectionColor; // Only relevant for text components with selection support.
    private final @Nullable Boolean         _isUnderlined;
    private final @Nullable Boolean         _isStrike;
    private final @Nullable Paint           _paint;
    private final @Nullable Paint           _backgroundPaint;
    private final @Nullable AffineTransform _transform;
    private final UI.HorizontalAlignment    _horizontalAlignment;
    private final UI.VerticalAlignment      _verticalAlignment;


    private FontConf(
        String                    name,
        int                       fontSize,
        float                     posture,
        float                     weight,
        float                     spacing,
        @Nullable Color           color,
        @Nullable Color           backgroundColor,
        @Nullable Color           selectionColor,
        @Nullable Boolean         isUnderline,
        @Nullable Boolean         isStrike,
        @Nullable AffineTransform transform,
        @Nullable Paint           paint,
        @Nullable Paint           backgroundPaint,
        UI.HorizontalAlignment    horizontalAlignment,
        UI.VerticalAlignment      verticalAlignment
    ) {
        _familyName = Objects.requireNonNull(name);
        _size    = fontSize;
        _posture = posture;
        _weight  = weight;
        _spacing = spacing;
        _color   = color;
        _backgroundColor = backgroundColor;
        _selectionColor  = selectionColor;
        _isUnderlined    = isUnderline;
        _isStrike        = isStrike;
        _transform       = transform;
        _paint           = paint;
        _backgroundPaint = backgroundPaint;
        _horizontalAlignment = horizontalAlignment;
        _verticalAlignment   = verticalAlignment;
    }

    String family() { return _familyName; }

    int size() { return _size; }

    float posture() { return _posture; }

    float weight() { return _weight; }

    float spacing() { return _spacing; }

    Optional<Color> color() { return Optional.ofNullable(_color); }

    Optional<Color> backgroundColor() { return Optional.ofNullable(_backgroundColor); }

    Optional<Color> selectionColor() { return Optional.ofNullable(_selectionColor); }

    boolean isUnderlined() { return _isUnderlined != null ? _isUnderlined : false; }

    Optional<AffineTransform> transform() { return Optional.ofNullable(_transform); }

    Optional<Paint> paint() { return Optional.ofNullable(_paint); }

    Optional<Paint> backgroundPaint() { return Optional.ofNullable(_backgroundPaint); }

    UI.HorizontalAlignment horizontalAlignment() { return _horizontalAlignment; }

    UI.VerticalAlignment verticalAlignment() { return _verticalAlignment; }

    /**
     * Returns an updated font config with the specified font family name.
     *
     * @param fontFamily The font family name to use for the {@link Font#getFamily()} property.
     * @return A new font style with the specified font family name.
     */
    public FontConf family( String fontFamily ) {
        return FontConf.of(fontFamily, _size, _posture, _weight, _spacing, _color, _backgroundColor, _selectionColor, _isUnderlined, _isStrike,  _transform, _paint, _backgroundPaint, _horizontalAlignment, _verticalAlignment);
    }

    /**
     * Returns an updated font config with the specified font size,
     * which will translate to a {@link Font} instance with the specified size 
     * (see {@link Font#getSize()}).
     *
     * @param fontSize The font size to use for the {@link Font#getSize()} property.
     * @return A new font style with the specified font size.
     */
    public FontConf size( int fontSize ) {
        return FontConf.of(_familyName, fontSize, _posture, _weight, _spacing, _color, _backgroundColor, _selectionColor, _isUnderlined, _isStrike,  _transform, _paint, _backgroundPaint, _horizontalAlignment, _verticalAlignment);
    }

    /**
     * Returns an updated font config with the specified posture, defining the tilt of the font.
     * A {@link Font} with a higher posture value will be more italic.
     * (see {@link Font#isItalic()}).
     *
     * @param posture The posture to use for the {@link Font#isItalic()} property.
     * @return A new font style with the specified posture.
     */
    public FontConf posture( float posture ) {
        return FontConf.of(_familyName, _size, posture, _weight, _spacing, _color, _backgroundColor, _selectionColor, _isUnderlined, _isStrike,  _transform, _paint, _backgroundPaint, _horizontalAlignment, _verticalAlignment);
    }

    /**
     * Returns an updated font config with the specified weight, defining the boldness of the font.
     * A {@link Font} with a higher weight value will be bolder.
     * (see {@link Font#isBold()}).
     *
     * @param fontWeight The weight to use for the {@link Font#isBold()} property.
     * @return A new font style with the specified weight.
     */
    public FontConf weight( float fontWeight ) {
        return FontConf.of(_familyName, _size, _posture, fontWeight, _spacing, _color, _backgroundColor, _selectionColor, _isUnderlined, _isStrike,  _transform, _paint, _backgroundPaint, _horizontalAlignment, _verticalAlignment);
    }

    /**
     * Returns an updated font config with the specified spacing, defining the tracking of the font.
     * The tracking value is multiplied by the font point size and
     * passed through the font transform to determine an additional
     * amount to add to the advance of each glyph cluster.  Positive
     * tracking values will inhibit formation of optional ligatures.
     * Tracking values are typically between {@code -0.1} and
     * {@code 0.3}; values outside this range are generally not
     * desirable.
     *
     * @param spacing The spacing to use for the {@link TextAttribute#TRACKING} property.
     * @return A new font style with the specified spacing.
     */
    public FontConf spacing( float spacing ) {
        return FontConf.of(_familyName, _size, _posture, _weight, spacing, _color, _backgroundColor, _selectionColor, _isUnderlined, _isStrike,  _transform, _paint, _backgroundPaint, _horizontalAlignment, _verticalAlignment);
    }

    /**
     * Returns an updated font config with the specified color,
     * which will be used for the {@link TextAttribute#FOREGROUND} property
     * of the resulting {@link Font} instance.
     *
     * @param color The color to use for the {@link TextAttribute#FOREGROUND} property.
     * @return A new font style with the specified color.
     */
    public FontConf color( Color color ) {
        Objects.requireNonNull(color);
        if ( color == UI.Colour.UNDEFINED)
            color = null;
        if ( color == _color )
            return this;

        return FontConf.of(_familyName, _size, _posture, _weight, _spacing, color, _backgroundColor, _selectionColor, _isUnderlined, _isStrike,  _transform, _paint, _backgroundPaint, _horizontalAlignment, _verticalAlignment);
    }

    /**
     * Returns an updated font config with the specified color string used to define the font color.
     * The color will be used for the {@link TextAttribute#FOREGROUND} property
     * of the resulting {@link Font} instance.
     *
     * @param colorString The color string to use for the {@link TextAttribute#FOREGROUND} property.
     * @return A new font style with the specified color.
     */
    public FontConf color( String colorString ) {
        Objects.requireNonNull(colorString);
        Color newColor;
        try {
            if ( colorString.isEmpty() )
                newColor = UI.Colour.UNDEFINED;
            else
                newColor = UI.color(colorString);
        } catch ( Exception e ) {
            log.error("Failed to parse color string: '"+colorString+"'", e);
            return this;
        }
        return color(newColor);
    }

    /**
     * Returns an updated font config with the specified background color.
     * The color value will be used for the {@link TextAttribute#BACKGROUND} property
     * of the resulting {@link Font} instance.
     *
     * @param backgroundColor The background color to use for the {@link TextAttribute#BACKGROUND} property.
     * @return A new font style with the specified background color.
     */
    public FontConf backgroundColor( Color backgroundColor ) {
        Objects.requireNonNull(backgroundColor);
        if ( backgroundColor == UI.Colour.UNDEFINED)
            backgroundColor = null;
        if ( backgroundColor == _backgroundColor )
            return this;
        return FontConf.of(_familyName, _size, _posture, _weight, _spacing, _color, backgroundColor, _selectionColor, _isUnderlined, _isStrike,  _transform, _paint, _backgroundPaint, _horizontalAlignment, _verticalAlignment);
    }

    /**
     * Returns an updated font config with the specified background color string used to define the background color.
     * The background color will be used for the {@link TextAttribute#BACKGROUND} property
     * of the resulting {@link Font} instance.
     *
     * @param colorString The color string to use for the {@link TextAttribute#BACKGROUND} property.
     * @return A new font style with the specified background color.
     */
    public FontConf backgroundColor( String colorString ) {
        Objects.requireNonNull(colorString);
        Color newColor;
        try {
            if ( colorString.isEmpty() )
                newColor = UI.Colour.UNDEFINED;
            else
                newColor = UI.color(colorString);
        } catch ( Exception e ) {
            log.error("Failed to parse color string: '{}'", colorString, e);
            return this;
        }
        return backgroundColor(newColor);
    }

    /**
     * Returns an updated font config with the specified selection color.
     * The selection color will be used for the selection color of the font.
     * Note that not all components support text selection, so this property may not
     * have an effect on all components.
     *
     * @param selectionColor The selection color to use for the selection color of the font.
     * @return A new font style with the specified selection color.
     */
    public FontConf selectionColor( Color selectionColor ) {
        Objects.requireNonNull(selectionColor);
        if ( selectionColor == UI.Colour.UNDEFINED)
            selectionColor = null;
        if ( selectionColor == _selectionColor )
            return this;
        return FontConf.of(_familyName, _size, _posture, _weight, _spacing, _color, _backgroundColor, selectionColor, _isUnderlined, _isStrike, _transform, _paint, _backgroundPaint, _horizontalAlignment, _verticalAlignment);
    }

    /**
     * Returns an updated font config with the specified selection color string used to define the selection color.
     * The selection color will be used for the selection color of the font.
     * Note that not all components support text selection, so this property may not
     * have an effect on all components.
     *
     * @param colorString The color string to use for the selection color of the font.
     * @return A new font style with the specified selection color.
     */
    public FontConf selectionColor( String colorString ) {
        Objects.requireNonNull(colorString);
        Color newColor;
        try {
            if ( colorString.isEmpty() )
                newColor = UI.Colour.UNDEFINED;
            else
                newColor = UI.color(colorString);
        } catch ( Exception e ) {
            log.error("Failed to parse color string: '"+colorString+"'", e);
            return this;
        }
        return selectionColor(newColor);
    }

    /**
     * Returns an updated font config with the specified underlined property.
     * This boolean will translate to the {@link TextAttribute#UNDERLINE} property
     * of the resulting {@link Font} instance.
     *
     * @param underlined Whether the font should be underlined.
     * @return A new font style with the specified underlined property.
     */
    public FontConf underlined( boolean underlined ) {
        return FontConf.of(_familyName, _size, _posture, _weight, _spacing, _color, _backgroundColor, _selectionColor, underlined, _isStrike, _transform, _paint, _backgroundPaint, _horizontalAlignment, _verticalAlignment);
    }

    /**
     * Returns an updated font config with the specified strike through property.
     * This boolean will translate to the {@link TextAttribute#STRIKETHROUGH} property
     * of the resulting {@link Font} instance.
     *
     * @param strike Whether the font should be strike through.
     * @return A new font style with the specified strike through property.
     */
    public FontConf strikeThrough( boolean strike ) {
        return FontConf.of(_familyName, _size, _posture, _weight, _spacing, _color, _backgroundColor, _selectionColor, _isUnderlined, strike, _transform, _paint, _backgroundPaint, _horizontalAlignment, _verticalAlignment);
    }

    /**
     * Returns an updated font config with the specified transform.
     * This transform will be used for the {@link TextAttribute#TRANSFORM} property
     * of the resulting {@link Font} instance.
     *
     * @param transform The transform to use for the {@link TextAttribute#TRANSFORM} property.
     * @return A new font style with the specified transform.
     */
    public FontConf transform( @Nullable AffineTransform transform ) {
        return FontConf.of(_familyName, _size, _posture, _weight, _spacing, _color, _backgroundColor, _selectionColor, _isUnderlined, _isStrike, transform, _paint, _backgroundPaint, _horizontalAlignment, _verticalAlignment);
    }

    /**
     * Returns an updated font config with the specified paint.
     * This paint will be used for the {@link TextAttribute#FOREGROUND} property
     * of the resulting {@link Font} instance.
     * Note that specifying a custom paint will override the effects of the color property
     * as the color property is in essence merely a convenience property for a
     * paint painting across the entire font area homogeneously using the specified color.
     *
     * @param paint The paint to use for the {@link TextAttribute#FOREGROUND} property.
     * @return A new font style with the specified paint.
     */
    public FontConf paint( @Nullable Paint paint ) {
        return FontConf.of(_familyName, _size, _posture, _weight, _spacing, _color, _backgroundColor, _selectionColor, _isUnderlined, _isStrike,  _transform, paint, _backgroundPaint, _horizontalAlignment, _verticalAlignment);
    }

    /**
     * Returns an updated font config with the specified background paint.
     * This paint will be used for the {@link TextAttribute#BACKGROUND} property
     * of the resulting {@link Font} instance.
     *
     * @param backgroundPaint The background paint to use for the {@link TextAttribute#BACKGROUND} property.
     * @return A new font style with the specified background paint.
     */
    public FontConf backgroundPaint( @Nullable Paint backgroundPaint ) {
        return FontConf.of(_familyName, _size, _posture, _weight, _spacing, _color, _backgroundColor, _selectionColor, _isUnderlined, _isStrike,  _transform, _paint, backgroundPaint, _horizontalAlignment, _verticalAlignment);
    }

    /**
     * Returns an updated font config with the specified horizontal alignment.
     * This property is not relevant for all components,
     * It will usually only be relevant for {@link JLabel}, {@link AbstractButton} and {@link JTextField}
     * types or maybe some custom components.
     * Not all components support horizontal alignment.
     * This will also not have an effect for font configs which
     * are part of the {@link TextConf}.
     * <b>
     *     This method is deliberately package-private as it is not relevant for the
     *     {@link ComponentStyleDelegate#componentFont(Function)} or
     *     {@link TextConf#font(Function)} methods. <br>
     *     <br>
     *     It only makes sense to specify this property 
     *     through the {@link ComponentStyleDelegate#fontAlignment(UI.HorizontalAlignment)}
     *     method!!
     * </b>
     *
     * @param horizontalAlignment The horizontal alignment to use for the font.
     * @return A new font style with the specified horizontal alignment.
     */
    FontConf horizontalAlignment( UI.HorizontalAlignment horizontalAlignment ) {
        return FontConf.of(_familyName, _size, _posture, _weight, _spacing, _color, _backgroundColor, _selectionColor, _isUnderlined, _isStrike,  _transform, _paint, _backgroundPaint, horizontalAlignment, _verticalAlignment);
    }

    /**
     * Returns an updated font config with the specified vertical alignment.
     * This property is not relevant for all components,
     * It will usually only be relevant for {@link JLabel}, {@link AbstractButton} and {@link JTextField}
     * types or maybe some custom components.
     * Not all components support vertical alignment.
     * This will also not have an effect for font configs which
     * are part of the {@link TextConf}.
     * <b>
     *     This method is deliberately package-private as it is not relevant for the
     *     {@link ComponentStyleDelegate#componentFont(Function)} or
     *     {@link TextConf#font(Function)} methods. <br>
     *     <br>
     *     It only makes sense to specify this property 
     *     through the {@link ComponentStyleDelegate#fontAlignment(UI.VerticalAlignment)}
     *     method!!
     * </b>
     *
     * @param verticalAlignment The vertical alignment to use for the font.
     * @return A new font style with the specified vertical alignment.
     */
    FontConf verticalAlignment( UI.VerticalAlignment verticalAlignment ) {
        return FontConf.of(_familyName, _size, _posture, _weight, _spacing, _color, _backgroundColor, _selectionColor, _isUnderlined, _isStrike,  _transform, _paint, _backgroundPaint, _horizontalAlignment, verticalAlignment);
    }

    FontConf withPropertiesFromFont( Font font )
    {
        if ( font == UI.FONT_UNDEFINED )
            return this;

        Map<TextAttribute, ?> attributeMap = font.getAttributes();

        String family = font.getFamily();

        int size = font.getSize();

        float posture = font.isItalic() ? 0.2f : 0f;
        try {
            if (attributeMap.containsKey(TextAttribute.POSTURE))
                posture = ((Number) attributeMap.get(TextAttribute.POSTURE)).floatValue();
        } catch (Exception e) {
            log.debug("Failed to fetch TextAttribute.POSTURE in font attributes '" + attributeMap + "' of font '" + font + "'", e);
        }

        float weight = font.isBold() ? 2f : 0f;
        try {
            if (attributeMap.containsKey(TextAttribute.WEIGHT))
                weight = ((Number) attributeMap.get(TextAttribute.WEIGHT)).floatValue();
        } catch (Exception e) {
            log.debug("Failed to fetch TextAttribute.WEIGHT in font attributes '" + attributeMap + "' of font '" + font + "'", e);
        }

        float spacing = _spacing;
        try {
            if (attributeMap.containsKey(TextAttribute.TRACKING))
                spacing = ((Number) attributeMap.get(TextAttribute.TRACKING)).floatValue();
        } catch (Exception e) {
            log.debug("Failed to fetch TextAttribute.TRACKING in font attributes '" + attributeMap + "' of font '" + font + "'", e);
        }

        Color color = _color;
        try {
            if (attributeMap.containsKey(TextAttribute.FOREGROUND))
                color = (Color) attributeMap.get(TextAttribute.FOREGROUND);
        } catch (Exception e) {
            log.debug("Failed to fetch TextAttribute.FOREGROUND in font attributes '" + attributeMap + "' of font '" + font + "'", e);
        }

        Color backgroundColor = _backgroundColor;
        try {
            if (attributeMap.containsKey(TextAttribute.BACKGROUND))
                backgroundColor = (Color) attributeMap.get(TextAttribute.BACKGROUND);
        } catch (Exception e) {
            log.debug("Failed to fetch TextAttribute.BACKGROUND in font attributes '" + attributeMap + "' of font '" + font + "'", e);
        }

        Color selectionColor = _selectionColor;
        // The selection color is not a text attribute, but a component property, like for text areas.

        boolean isUnderline = ( _isUnderlined != null ? _isUnderlined : false );
        try {
            if (attributeMap.containsKey(TextAttribute.UNDERLINE))
                isUnderline = Objects.equals(attributeMap.get(TextAttribute.UNDERLINE), TextAttribute.UNDERLINE_ON);
        } catch (Exception e) {
            log.debug("Failed to fetch TextAttribute.UNDERLINE in font attributes '" + attributeMap + "' of font '" + font + "'", e);
        }

        boolean isStriked   = ( _isStrike != null ? _isStrike : false );
        try {
            if (attributeMap.containsKey(TextAttribute.STRIKETHROUGH))
                isStriked   = Objects.equals(attributeMap.get(TextAttribute.STRIKETHROUGH), TextAttribute.STRIKETHROUGH_ON);
        } catch (Exception e) {
            log.debug("Failed to fetch TextAttribute.STRIKETHROUGH in font attributes '" + attributeMap + "' of font '" + font + "'", e);
        }

        AffineTransform transform = _transform;
        try {
            if (attributeMap.containsKey(TextAttribute.TRANSFORM))
                transform = (AffineTransform) attributeMap.get(TextAttribute.TRANSFORM);
        } catch (Exception e) {
            log.debug("Failed to fetch TextAttribute.TRANSFORM in font attributes '" + attributeMap + "' of font '" + font + "'", e);
        }

        Paint paint = _paint;
        try {
            if (attributeMap.containsKey(TextAttribute.FOREGROUND))
                paint = (Paint) attributeMap.get(TextAttribute.FOREGROUND);
        } catch (Exception e) {
            log.warn("Failed to extract font attributes from font: " + font, e);
        }

        Paint backgroundPaint = _backgroundPaint;
        try {
            if (attributeMap.containsKey(TextAttribute.BACKGROUND))
                backgroundPaint = (Paint) attributeMap.get(TextAttribute.BACKGROUND);
        } catch (Exception e) {
            log.warn("Failed to extract font attributes from font: " + font, e);
        }

        Objects.requireNonNull(font);
        return FontConf.of(
                    family,
                    size,
                    posture,
                    weight,
                    spacing,
                    ( paint           == null ? color           : null ),
                    ( backgroundPaint == null ? backgroundColor : null ),
                    selectionColor,
                    isUnderline,
                    isStriked,
                    transform,
                    paint,
                    backgroundPaint,
                    _horizontalAlignment,
                    _verticalAlignment
                );
    }

    Optional<Font> createDerivedFrom( Font existingFont )
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
        if ( _posture > 0 ) {
            isChange = isChange || !Float.valueOf(_posture).equals(currentAttributes.get(TextAttribute.POSTURE));
            attributes.put(TextAttribute.POSTURE, _posture);
        }
        if ( _weight > 0 ) {
            isChange = isChange || !Float.valueOf(_weight).equals(currentAttributes.get(TextAttribute.WEIGHT));
            attributes.put(TextAttribute.WEIGHT, _weight);
        }
        if ( _spacing != 0 ) {
            isChange = isChange || !Float.valueOf(_spacing).equals(currentAttributes.get(TextAttribute.TRACKING));
            attributes.put(TextAttribute.TRACKING, _spacing);
        }
        if ( _isUnderlined != null ) {
            isChange = isChange || !Objects.equals(_isUnderlined, currentAttributes.get(TextAttribute.UNDERLINE));
            attributes.put(TextAttribute.UNDERLINE, _isUnderlined);
        }
        if ( _isStrike != null ) {
            isChange = isChange || !Objects.equals(_isStrike, currentAttributes.get(TextAttribute.STRIKETHROUGH));
            attributes.put(TextAttribute.STRIKETHROUGH, _isStrike);
        }
        if ( _transform != null ) {
            isChange = isChange || !Objects.equals(_transform, currentAttributes.get(TextAttribute.TRANSFORM));
            attributes.put(TextAttribute.TRANSFORM, _transform);
        }
        if ( !_familyName.isEmpty() ) {
            isChange = isChange || !Objects.equals(_familyName, currentAttributes.get(TextAttribute.FAMILY));
            attributes.put(TextAttribute.FAMILY, _familyName);
        }
        if ( _color != null ) {
            isChange = isChange || !Objects.equals(_color, currentAttributes.get(TextAttribute.FOREGROUND));
            attributes.put(TextAttribute.FOREGROUND, _color);
        }
        if ( _backgroundColor != null ) {
            isChange = isChange || !Objects.equals(_backgroundColor, currentAttributes.get(TextAttribute.BACKGROUND));
            attributes.put(TextAttribute.BACKGROUND, _backgroundColor);
        }
        if ( _paint != null ) {
            isChange = isChange || !Objects.equals(_paint, currentAttributes.get(TextAttribute.FOREGROUND));
            attributes.put(TextAttribute.FOREGROUND, _paint);
        }
        if ( _backgroundPaint != null ) {
            isChange = isChange || !Objects.equals(_backgroundPaint, currentAttributes.get(TextAttribute.BACKGROUND));
            attributes.put(TextAttribute.BACKGROUND, _backgroundPaint);
        }
        if ( isChange )
            return Optional.of(existingFont.deriveFont(attributes));
        else
            return Optional.empty();
    }

    FontConf _scale(double scale ) {
        if ( scale == 1.0 )
            return this;
        else if ( this == _NONE )
            return this;
        else
            return FontConf.of(
                    _familyName,
                    (int) Math.round(_size * scale),
                    _posture,
                    _weight,
                    _spacing,
                    _color,
                    _backgroundColor,
                    _selectionColor, 
                    _isUnderlined,
                    _isStrike,
                    _transform,
                    _paint,
                    _backgroundPaint,
                    _horizontalAlignment,
                    _verticalAlignment
                );
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(_familyName);
        hash = 97 * hash + _size;
        hash = 97 * hash + Float.hashCode(_posture);
        hash = 97 * hash + Float.hashCode(_weight);
        hash = 97 * hash + Float.hashCode(_spacing);
        hash = 97 * hash + Objects.hashCode(_color);
        hash = 97 * hash + Objects.hashCode(_backgroundColor);
        hash = 97 * hash + Objects.hashCode(_selectionColor);
        hash = 97 * hash + Objects.hashCode(_isUnderlined);
        hash = 97 * hash + Objects.hashCode(_transform);
        hash = 97 * hash + Objects.hashCode(_paint);
        hash = 97 * hash + Objects.hashCode(_backgroundPaint);
        hash = 97 * hash + Objects.hashCode(_horizontalAlignment);
        hash = 97 * hash + Objects.hashCode(_verticalAlignment);
        return hash;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        final FontConf other = (FontConf)obj;
        if ( !Objects.equals(_familyName, other._familyName) )
            return false;
        if ( _size != other._size )
            return false;
        if ( _posture != other._posture)
            return false;
        if ( _weight != other._weight )
            return false;
        if ( _spacing != other._spacing )
            return false;
        if ( !Objects.equals(_color, other._color) )
            return false;
        if ( !Objects.equals(_backgroundColor, other._backgroundColor) )
            return false;
        if ( !Objects.equals(_selectionColor, other._selectionColor) )
            return false;
        if ( !Objects.equals(_isUnderlined, other._isUnderlined) )
            return false;
        if ( !Objects.equals(_transform, other._transform) )
            return false;
        if ( !Objects.equals(_paint, other._paint) )
            return false;
        if ( !Objects.equals(_backgroundPaint, other._backgroundPaint) )
            return false;
        if ( _horizontalAlignment != other._horizontalAlignment )
            return false;
        if ( _verticalAlignment != other._verticalAlignment )
            return false;

        return true;
    }

    @Override
    public String toString()
    {
        if ( this == _NONE )
            return this.getClass().getSimpleName() + "[NONE]";
        String underline       = ( _isUnderlined        == null ? "?" : String.valueOf(_isUnderlined)   );
        String strike          = ( _isStrike            == null ? "?" : String.valueOf(_isStrike)       );
        String transform       = ( _transform           == null ? "?" : _transform.toString()           );
        String paint           = ( _paint               == null ? "?" : _paint.toString()               );
        String backgroundPaint = ( _backgroundPaint     == null ? "?" : _backgroundPaint.toString()     );
        String horizontalAlign = ( _horizontalAlignment == UI.HorizontalAlignment.UNDEFINED ? "?" : _horizontalAlignment.toString() );
        String verticalAlign   = ( _verticalAlignment   == UI.VerticalAlignment.UNDEFINED ? "?" : _verticalAlignment.toString()   );
        return this.getClass().getSimpleName() + "[" +
                    "family="              + _familyName + ", " +
                    "size="                + _size                                   + ", " +
                    "posture="             + _posture                                + ", " +
                    "weight="              + _weight                                 + ", " +
                    "spacing="             + _spacing                                + ", " +
                    "underlined="          + underline                               + ", " +
                    "strikeThrough="       + strike                                  + ", " +
                    "color="               + StyleUtil.toString(_color)           + ", " +
                    "backgroundColor="     + StyleUtil.toString(_backgroundColor) + ", " +
                    "selectionColor="      + StyleUtil.toString(_selectionColor)  + ", " +
                    "transform="           + transform                               + ", " +
                    "paint="               + paint                                   + ", " +
                    "backgroundPaint="     + backgroundPaint                         + ", " +
                    "horizontalAlignment=" + horizontalAlign                         + ", " +
                    "verticalAlignment="   + verticalAlign                           +
                "]";
    }
}

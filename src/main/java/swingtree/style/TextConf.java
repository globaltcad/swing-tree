package swingtree.style;

import swingtree.UI;
import swingtree.api.Styler;

import java.awt.Font;
import java.util.Objects;
import java.util.function.Function;

/**
 *  An immutable configuration type which holds custom
 *  text as well as placement and font properties used for
 *  rendering text onto a Swing component. <br>
 *  This objects is exposed inside the {@link ComponentStyleDelegate#text(Function)}
 *  as a way to configure custom text properties of a component
 *  as part of the style API exposed by {@link swingtree.UIForAnySwing#withStyle(Styler)}.
 *  <p>
 *  Here a simple usage example which
 *  demonstrates how to render text onto the top edge of a
 *  {@link javax.swing.JPanel}:
 *  <pre>{@code
 *      UI.panel()
 *      .withStyle(conf -> conf
 *          .prefSize(180, 100)
 *          .background(Color.CYAN)
 *          .text(textConf -> textConf
 *              .content("Hello World!")
 *              .placement(UI.Placement.TOP)
 *              .font( fontConf -> fontConf.color(Color.DARK_GRAY).size(20) )
 *          )
 *      )
 *  }</pre>
 *  In this small example you can see the usage of {@link TextConf#content(String)},
 *  {@link TextConf#placement(UI.Placement)} and {@link TextConf#font(Function)}.
 *  But there are much more properties available to configure the text rendering
 *  as part of the style API. <br>
 *  <p>
 *  Here a full list of all available properties with their respective
 *  meaning and default values:
 *  <ul>
 *      <li><b>Content</b>
 *          You can set this property through {@link TextConf#content(String)}.
 *          This is the actual text content that should be rendered onto the component.
 *          It's default value is an empty string, in which case this
 *          configuration object will not have any effect.
 *      </li>
 *      <li><b>Font</b>
 *          The {@link FontConf} object is its own rich configuration object
 *          which holds all font properties like size, style, color, etc.
 *          You can configure it through {@link TextConf#font(Function)}.<br>
 *          The default font configuration is {@link FontConf#none()}.
 *      </li>
 *      <li><b>Clip Area</b>
 *          The clip area is an enum the area of the component where the text should be
 *          rendered. So the text will only be visible within this area.<br>
 *          You can configure it through {@link TextConf#clipTo(UI.ComponentArea)}.<br>
 *          The default clip area is {@link UI.ComponentArea#INTERIOR}.
 *      </li>
 *      <li><b>Placement Boundary</b>
 *          The placement boundary is an enum which defines the boundary of the component
 *          onto which the text placement should be bound to.
 *          You can configure it through {@link TextConf#placementBoundary(UI.ComponentBoundary)}.<br>
 *          The default placement boundary is {@link UI.ComponentBoundary#INTERIOR_TO_CONTENT},
 *          which honours the padding of the component.
 *          If you want to ignore the padding and place the text directly after the border
 *          of the component, you can set it to {@link UI.ComponentBoundary#BORDER_TO_INTERIOR}.
 *      </li>
 *          <li><b>Placement</b>
 *          The placement is an enum which defines where the text should be placed
 *          according to the {@link TextConf#placementBoundary(UI.ComponentBoundary)}.
 *          You can configure it through {@link TextConf#placement(UI.Placement)}.<br>
 *          The default placement is {@link UI.Placement#CENTER}.
 *      </li>
 *      <li><b>Offset</b>
 *          The offset holds the x and y placement offset of the text.
 *          You can configure it through {@link TextConf#offset(Offset)} or {@link TextConf#offset(int, int)}.
 *          <br>
 *          The default offset is {@link Offset#none()} (0, 0).
 *          This property is great for making fine adjustments to the text placement. <br>
 *          However, for a more robust alignment of the text, it is recommended to use the
 *          {@link TextConf#placement(UI.Placement)} and {@link TextConf#placementBoundary(UI.ComponentBoundary)}
 *          properties as a first choice.
 *      </li>
 *  </ul>
 *  Use {@link TextConf#none()} to access the <i>null object</i> of the {@link TextConf} type.
 *  It is a convenient way to represent a <i>no-op</i> configuration object which will not have any effect
 *  when applied to a component.
 */
public final class TextConf implements Simplifiable<TextConf>
{
    public static UI.Layer DEFAULT_LAYER = UI.Layer.CONTENT;
    private static final TextConf _NONE = new TextConf(
                                                "",
                                                FontConf.none(),
                                                UI.ComponentArea.INTERIOR,
                                                UI.ComponentBoundary.INTERIOR_TO_CONTENT,
                                                UI.Placement.CENTER,
                                                Offset.none()
                                            );

    static final TextConf none() {
        return _NONE;
    }

    private final String               _content;
    private final FontConf             _fontConf;
    private final UI.ComponentArea     _clipArea;
    private final UI.ComponentBoundary _placementBoundary;
    private final UI.Placement         _placement;
    private final Offset               _offset;

    private TextConf(
        String               content,
        FontConf             fontConf,
        UI.ComponentArea     clipArea,
        UI.ComponentBoundary placementBoundary,
        UI.Placement         placement,
        Offset               offset
    )
    {
        _content            = Objects.requireNonNull(content);
        _fontConf           = Objects.requireNonNull(fontConf);
        _clipArea           = Objects.requireNonNull(clipArea);
        _placementBoundary  = Objects.requireNonNull(placementBoundary);
        _placement          = Objects.requireNonNull(placement);
        _offset             = Objects.requireNonNull(offset);
    }

    private static TextConf of(
        String               content,
        FontConf             fontConf,
        UI.ComponentArea     clipArea,
        UI.ComponentBoundary placementBoundary,
        UI.Placement         placement,
        Offset               offset
    )
    {
        if (
            content.isEmpty() &&
            fontConf.equals(_NONE._fontConf) &&
            clipArea.equals(_NONE._clipArea) &&
            placementBoundary.equals(_NONE._placementBoundary) &&
            placement.equals(_NONE._placement) &&
            offset.equals(_NONE._offset)
        ) {
            return _NONE;
        }
        return new TextConf(content, fontConf, clipArea, placementBoundary, placement, offset);
    }

    String content() {
        return _content;
    }

    FontConf fontConf() {
        return _fontConf;
    }

    UI.ComponentArea clipArea() {
        return _clipArea;
    }

    UI.ComponentBoundary placementBoundary() {
        return _placementBoundary;
    }

    UI.Placement placement() {
        return _placement;
    }

    Offset offset() {
        return _offset;
    }

    /**
     * Returns a new {@link TextConf} object with the given text content.
     * @param textString The text content to be rendered onto the component.
     * @return A new {@link TextConf} object with the given text content.
     */
    public TextConf content( String textString ) {
        return of(textString, _fontConf, _clipArea, _placementBoundary, _placement, _offset);
    }

    private TextConf _fontConf(FontConf fontConf) {
        return of(_content, fontConf, _clipArea, _placementBoundary, _placement, _offset);
    }

    /**
     * Returns a new {@link TextConf} object with the given font configuration
     * defined by a configurator function which takes a {@link FontConf} object
     * and returns an updated {@link FontConf} object with the desired font properties.
     *
     * @param fontConfFunction A function which takes the current font configuration
     *                         and returns a new font configuration with the desired properties.
     * @return A new {@link TextConf} object with the given font configuration.
     */
    public TextConf font( Function<FontConf, FontConf> fontConfFunction ) {
        return _fontConf(fontConfFunction.apply(_fontConf));
    }

    /**
     * Returns a new {@link TextConf} object with the given font.
     * @param font The font to be used for rendering the text onto the component.
     * @return A new {@link TextConf} object with the given font.
     */
    public TextConf font( Font font ) {
        return _fontConf(_fontConf.withPropertiesFromFont(font));
    }

    /**
     * Returns a new {@link TextConf} object with the given clip area
     * defined by a {@link UI.ComponentArea} enum.
     * Text positioned outside the clip area will not be visible.
     * @param clipArea The clip area where the text should be rendered onto the component.
     * @return A new {@link TextConf} object with the given clip area.
     */
    public TextConf clipTo( UI.ComponentArea clipArea ) {
        return of(_content, _fontConf, clipArea, _placementBoundary, _placement, _offset);
    }

    /**
     * Returns a new {@link TextConf} object with the given placement boundary
     * defined by a {@link UI.ComponentBoundary} enum.
     * The placement boundary defines the boundary of the component onto which
     * the text placement should be bound to.
     * <p>
     * The following placement boundaries are available:
     * <ul>
     *     <li>{@link UI.ComponentBoundary#OUTER_TO_EXTERIOR} -
     *     The outermost boundary of the entire component, including any margin that might be applied.
     *     </li>
     *     <li>{@link UI.ComponentBoundary#EXTERIOR_TO_BORDER} -
     *     The boundary located after the margin but before the border.
     *     This tightly wraps the entire {@link UI.ComponentArea#BODY}.
     *     </li>
     *     <li>{@link UI.ComponentBoundary#BORDER_TO_INTERIOR} -
     *     The boundary located after the border but before the padding.
     *     It represents the edge of the component's interior.
     *     </li>
     *     <li>{@link UI.ComponentBoundary#INTERIOR_TO_CONTENT} -
     *     The boundary located after the padding.
     *     It represents the innermost boundary of the component, where the actual content of the component begins,
     *     typically the sub-components of the component.
     *     </li>
     * </ul>
     *
     * @param placementBoundary The placement boundary of the component.
     * @return A new {@link TextConf} object with the given placement boundary.
     */
    public TextConf placementBoundary(UI.ComponentBoundary placementBoundary) {
        return of(_content, _fontConf, _clipArea, placementBoundary, _placement, _offset);
    }

    /**
     * Returns an updated {@link TextConf} object with the given placement,
     * defined by a {@link UI.Placement} enum.
     * The placement defines where the text should be placed according to the
     * {@link TextConf#placementBoundary(UI.ComponentBoundary)}.
     * <p>
     * The following placements are available:
     * <ul>
     *     <li>{@link UI.Placement#TOP} - Placed centered at the top edge of the component.</li>
     *     <li>{@link UI.Placement#BOTTOM} - Placed centered at the bottom edge of the component.</li>
     *     <li>{@link UI.Placement#LEFT} - At the left center edge of the component.</li>
     *     <li>{@link UI.Placement#RIGHT} - The right center edge of the component.</li>
     *     <li>{@link UI.Placement#CENTER} - Placed on the center of the edges defined by the {@link UI.ComponentBoundary}.</li>
     *     <li>{@link UI.Placement#TOP_LEFT} - Placed at the top left corner of the component.</li>
     *     <li>{@link UI.Placement#TOP_RIGHT} - Placed at the top right corner of the component.</li>
     *     <li>{@link UI.Placement#BOTTOM_LEFT} - Placed at the bottom left corner of the component.</li>
     *     <li>{@link UI.Placement#BOTTOM_RIGHT} - Placed at the bottom right corner of the component.</li>
     * </ul>
     *
     * @param placement The placement of the text, defined by a {@link UI.Placement} enum.
     * @return An updated {@link TextConf} object with the desired placement.
     */
    public TextConf placement(UI.Placement placement) {
        return of(_content, _fontConf, _clipArea, _placementBoundary, placement, _offset);
    }

    /**
     * Returns a new {@link TextConf} object with the given offset.
     * The offset holds the x and y placement offset of the text.
     * This property is great for making fine adjustments to the text placement.
     * However, for a more robust alignment of the text, it is recommended to use the
     * {@link TextConf#placement(UI.Placement)} and {@link TextConf#placementBoundary(UI.ComponentBoundary)}
     * properties as a first choice.
     * @param offset The offset of the text, defined by an {@link Offset} object.
     *               You may create an {@link Offset} object with {@link Offset#of(int, int)}.
     * @return An updated {@link TextConf} object with the given offset.
     */
    TextConf offset(Offset offset) {
        return of(_content, _fontConf, _clipArea, _placementBoundary, _placement, offset);
    }

    /**
     * Returns a {@link TextConf} object updated with an x and y placement offset.
     * The offset holds the x and y placement offset of the text.
     * This property is great for making fine adjustments to the text placement.
     * However, for a more robust alignment of the text, it is recommended to use the
     * {@link TextConf#placement(UI.Placement)} and {@link TextConf#placementBoundary(UI.ComponentBoundary)}
     * properties as a first choice.
     * @param x The x placement offset of the text.
     * @param y The y placement offset of the text.
     * @return An updated {@link TextConf} object with the given offset.
     */
    public TextConf offset(int x, int y) {
        return offset(Offset.of(x, y));
    }

    @Override
    public TextConf simplified() {
        if ( _content.isEmpty() )
            return _NONE;
        return this;
    }

    TextConf _scale(double scale) {
        return of(
            _content,
            _fontConf._scale(scale),
            _clipArea,
            _placementBoundary,
            _placement,
            _offset.scale(scale)
        );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;

        if ( !(obj instanceof TextConf) )
            return false;

        TextConf other = (TextConf) obj;
        return
            _content.equals(other._content) &&
            _fontConf.equals(other._fontConf) &&
            _clipArea.equals(other._clipArea) &&
            _placementBoundary.equals(other._placementBoundary) &&
            _placement.equals(other._placement) &&
            _offset.equals(other._offset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_content, _fontConf, _clipArea, _placementBoundary, _placement, _offset);
    }

    @Override
    public String toString() {
        if ( this.equals(_NONE) )
            return this.getClass().getSimpleName() + "[NONE]";
        return this.getClass().getSimpleName() + "[" +
            "content=" + _content + ", " +
            "fontConf=" + _fontConf + ", " +
            "clipArea=" + _clipArea + ", " +
            "placementBoundary=" + _placementBoundary + ", " +
            "placement=" + _placement + ", " +
            "offset=" + _offset +
        "]";
    }

}

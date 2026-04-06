package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Tuple;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.api.Configurator;
import swingtree.api.Styler;

import java.awt.Font;
import java.awt.Shape;
import java.util.Collection;
import java.util.Objects;

/**
 *  An immutable configuration type which holds custom
 *  text as well as placement and font properties used for
 *  rendering text onto a Swing component. <br>
 *  This objects is exposed inside the {@link ComponentStyleDelegate#text(Configurator)}
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
 *  {@link TextConf#placement(UI.Placement)} and {@link TextConf#font(Configurator)}.
 *  But there are much more properties available to configure the text rendering
 *  as part of the style API. <br>
 *  <p>
 *  Here a full list of all available properties with their respective
 *  meaning and default values:
 *  <ul>
 *      <li><b>Content</b>
 *          You can set this property through {@link TextConf#content(String)}.
 *          This is the actual text content that should be rendered onto the component.
 *          For richer text display with multiple styles for different parts of the text,
 *          you can also pass a sequence of {@link StyledString}s to define the text content.
 *          Each {@link StyledString} can have its own font properties which will be resolved
 *          using the {@link FontConf} of this {@link TextConf} to be derived from.<br>
 *          See {@link TextConf#content(StyledString...)} and {@link TextConf#content(Tuple)} for more details
 *          on how to configure the content with multiple styled strings.<br>
 *          The default content is an empty {@link Tuple} of {@link StyledString}s
 *          in which case the text configuration is effectively disabled!
 *      </li>
 *      <li><b>Font</b>
 *          The {@link FontConf} object is its own rich configuration object
 *          which holds all font properties like size, style, color, etc.
 *          You can configure it through {@link TextConf#font(Configurator)}.<br>
 *          The default font configuration is {@link FontConf#none()}.
 *      </li>
 *      <li><b>Clip Area</b>
 *          The clip area is an enum the area of the component where the text should be
 *          rendered. So the text will only be visible within this area.<br>
 *          You can configure it through {@link TextConf#clipTo(UI.ComponentArea)}.<br>
 *          The default clip area is {@link UI.ComponentArea#INTERIOR}.
 *      </li>
 *      <li><b>Placement Boundary</b>
 *          The placement boundary refers to one of many rectangular bounding boxes that capture
 *          <b>the transitional bounding lines between different {@link UI.ComponentArea}s in the
 *          box model (margin|border|padding) of a styled component.</b><br>
 *          You can configure it through {@link TextConf#placementBoundary(UI.ComponentBoundary)}.<br>
 *          The default placement boundary is {@link UI.ComponentBoundary#INTERIOR_TO_CONTENT},
 *          which honors the padding of the component.
 *          If you want to ignore the padding and place the text directly after the border
 *          of the component, you can set it to {@link UI.ComponentBoundary#BORDER_TO_INTERIOR}.
 *      </li>
 *          <li><b>Placement</b>
 *          The placement is an enum which defines where the text should be placed
 *          according to the {@link TextConf#placementBoundary(UI.ComponentBoundary)}.
 *          You can configure it through {@link TextConf#placement(UI.Placement)}.<br>
 *          The default placement is {@link UI.Placement#UNDEFINED}. At render time this is
 *          first resolved using the horizontal and vertical alignment from the {@code FontConf};
 *          only when those alignments are also {@link UI.Placement#UNDEFINED} does it behave
 *          like {@link swingtree.UI.Placement#CENTER}.
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
 *      <li><b>Wrap Lines</b>
 *          This is a boolean property, and it defines whether the text should be wrapped into multiple lines
 *          if the text content exceeds the width of the available space inside the component. <br>
 *          You can configure it through {@link TextConf#wrapLines(boolean)}.<br>
 *          The default value is {@code true}, which means that the text will wrap into multiple
 *          lines if it exceeds the width of the available space inside the component. <br>
 *          If set to {@code false}, the text will be rendered in a single line
 *          and may overflow the component if the text content is too long.
 *      </li>
 *      <li><b>Auto Preferred Height</b>
 *          Is a boolean property which configures whether the preferred height of the styled component should be computed
 *          from the text you render onto it through this {@link TextConf} and then set as the preferred height of the component. <br>
 *          This will effectively turn the preferred height of the component to a function of the component's
 *          width and the displayed text.<br>
 *          <b>It will also take full ownership of the preferred height of the component,
 *          which means that a preferred height specified elsewhere in the style configuration
 *          of the component will be ignored.</b><br>
 *          You can configure it through {@link TextConf#autoPreferredHeight(boolean)}.<br>
 *      </li>
 *      <li><b>Obstacles</b>
 *          A set of {@link Shape}s (in component coordinates) that the text must skip over and flow
 *          around so that it is never rendered on top of.<br>
 *          You can configure it through {@link TextConf#obstacles(Shape...)} or
 *          {@link TextConf#obstacles(Tuple)}.<br>
 *          The default value is an empty {@link Tuple}, meaning no obstacles are applied,
 *          <b>however</b>, if a particular component with a text configuration has child components,
 *          then the bounding boxes of those child components will automatically be registered as obstacles.
 *      </li>
 *      <li><b>Obstacles From Children Enabled</b>
 *          A boolean property that controls whether child components of the styled component
 *          are automatically registered as text-layout obstacles.<br>
 *          When {@code true} (the default), every child contributes an obstacle shape so that
 *          the text flows around the children rather than rendering on top of them.<br>
 *          When {@code false}, child components are completely ignored during obstacle collection
 *          regardless of the {@link TextConf#obstaclesFromChildrenAs(UI.ComponentArea)} setting.<br>
 *          You can configure it through {@link TextConf#obstaclesFromChildrenEnabled(boolean)}.
 *      </li>
 *      <li><b>Obstacles From Children As</b>
 *          A {@link UI.ComponentArea} property that selects <em>which portion</em> of each child
 *          component is used as its obstacle shape when automatic child-obstacle registration is
 *          active (i.e. {@link TextConf#obstaclesFromChildrenEnabled(boolean)} is {@code true}).<br>
 *          {@link UI.ComponentArea#ALL} (the default) uses the full bounding rectangle of the child,
 *          which is equivalent to {@code Component.getBounds()}.  Other values such as
 *          {@link UI.ComponentArea#BODY} or {@link UI.ComponentArea#INTERIOR} use the corresponding
 *          styled area of the child, falling back to the full bounds for children without a SwingTree style.<br>
 *          You can configure it through {@link TextConf#obstaclesFromChildrenAs(UI.ComponentArea)}.<br>
 *          If you want to disable automatic child-derived obstacles entirely, you can do so by calling
 *          {@link TextConf#obstaclesFromChildrenEnabled(boolean) obstaclesFromChildrenEnabled(false)}.
 *      </li>
 *  </ul>
 *  Use {@link TextConf#none()} to access the <i>null object</i> of the {@link TextConf} type.
 *  It is a convenient way to represent a <i>no-op</i> configuration object which will not have any effect
 *  when applied to a component.
 */
@Immutable
@SuppressWarnings("Immutable")
public final class TextConf implements Simplifiable<TextConf>
{
    private static final Logger log = LoggerFactory.getLogger(TextConf.class);
    public static UI.Layer DEFAULT_LAYER = UI.Layer.CONTENT;
    private static final TextConf _NONE = new TextConf(
                                                Tuple.of(StyledString.class),
                                                FontConf.none(),
                                                UI.ComponentArea.INTERIOR,
                                                UI.ComponentBoundary.INTERIOR_TO_CONTENT,
                                                UI.Placement.UNDEFINED,
                                                Offset.none(),
                                                true,
                                                false,
                                                Tuple.of(Shape.class),
                                                UI.ComponentArea.ALL,
                                                true
                                            );

    static final TextConf none() {
        return _NONE;
    }

    private final Tuple<StyledString>  _content;
    private final FontConf             _fontConf;
    private final UI.ComponentArea     _clipArea;
    private final UI.ComponentBoundary _placementBoundary;
    private final UI.Placement         _placement;
    private final Offset               _offset;
    private final boolean              _wrapLines;
    private final boolean              _autoPreferredHeight;
    private final Tuple<Shape>         _obstacles;
    private final UI.ComponentArea     _obstaclesFromChildrenAs;
    private final boolean              _obstaclesFromChildrenEnabled;

    private TextConf(
        Tuple<StyledString>  content,
        FontConf             fontConf,
        UI.ComponentArea     clipArea,
        UI.ComponentBoundary placementBoundary,
        UI.Placement         placement,
        Offset               offset,
        boolean              wrapLines,
        boolean              autoPreferredHeight,
        Tuple<Shape>         obstacles,
        UI.ComponentArea     obstaclesFromChildrenAs,
        boolean              obstaclesFromChildrenEnabled
    )
    {
        _content                      = Objects.requireNonNull(content);
        _fontConf                     = Objects.requireNonNull(fontConf);
        _clipArea                     = Objects.requireNonNull(clipArea);
        _placementBoundary            = Objects.requireNonNull(placementBoundary);
        _placement                    = Objects.requireNonNull(placement);
        _offset                       = Objects.requireNonNull(offset);
        _wrapLines                    = wrapLines;
        _autoPreferredHeight          = autoPreferredHeight;
        _obstacles                    = Objects.requireNonNull(obstacles);
        _obstaclesFromChildrenAs      = Objects.requireNonNull(obstaclesFromChildrenAs);
        _obstaclesFromChildrenEnabled = obstaclesFromChildrenEnabled;
    }

    private static TextConf of(
        Tuple<StyledString>  content,
        FontConf             fontConf,
        UI.ComponentArea     clipArea,
        UI.ComponentBoundary placementBoundary,
        UI.Placement         placement,
        Offset               offset,
        boolean              wrapLines,
        boolean              autoPreferredHeight,
        Tuple<Shape>         obstacles,
        UI.ComponentArea     obstaclesFromChildrenAs,
        boolean              obstaclesFromChildrenEnabled
    ) {
        if (
            content.isEmpty() &&
            fontConf.equals(_NONE._fontConf) &&
            clipArea.equals(_NONE._clipArea) &&
            placementBoundary.equals(_NONE._placementBoundary) &&
            placement.equals(_NONE._placement) &&
            offset.equals(_NONE._offset) &&
            wrapLines == _NONE._wrapLines &&
            autoPreferredHeight == _NONE._autoPreferredHeight &&
            obstacles.isEmpty() &&
            obstaclesFromChildrenAs == _NONE._obstaclesFromChildrenAs &&
            obstaclesFromChildrenEnabled == _NONE._obstaclesFromChildrenEnabled
        ) {
            return _NONE;
        }
        return new TextConf(content, fontConf, clipArea, placementBoundary, placement, offset, wrapLines, autoPreferredHeight, obstacles, obstaclesFromChildrenAs, obstaclesFromChildrenEnabled);
    }

    Tuple<StyledString> content() {
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

    boolean wrapLines() {
        return _wrapLines;
    }

    boolean autoPreferredHeight() {
        return _autoPreferredHeight;
    }

    Tuple<Shape> obstacles() {
        return _obstacles;
    }

    /**
     * Returns which area of each child component is used when automatically deriving
     * text-layout obstacles from the parent component's children.
     * <p>
     * This getter is the companion to {@link #obstaclesFromChildrenAs(UI.ComponentArea)}.
     * The returned value is only consulted when {@link #obstaclesFromChildrenEnabled()}
     * returns {@code true}.
     *
     * @return The {@link UI.ComponentArea} that describes which portion of each child
     *         is registered as a text obstacle; defaults to {@link UI.ComponentArea#ALL}.
     */
    UI.ComponentArea obstaclesFromChildrenAs() {
        return _obstaclesFromChildrenAs;
    }

    /**
     * Returns a new {@link TextConf} object with the given text content.
     * @param textString The text content to be rendered onto the component.
     * @return A new {@link TextConf} object with the given text content.
     */
    public TextConf content( String textString ) {
        Objects.requireNonNull(textString);
        if ( textString.isEmpty() )
            return content(Tuple.of(StyledString.class));
        return of(Tuple.of(StyledString.of(textString)), _fontConf, _clipArea, _placementBoundary, _placement, _offset, _wrapLines, _autoPreferredHeight, _obstacles, _obstaclesFromChildrenAs, _obstaclesFromChildrenEnabled);
    }

    /**
     * Configures this {@link TextConf} object to render the supplied sequence
     * of {@link StyledString}s as the text content onto the component.<br>
     * Each {@link StyledString} in the sequence can have its own font properties.
     * This allows you to render visually rich and expressive text.
     * @param styledStrings A vararg sequence of {@link StyledString}s which should be
     *                      rendered as the text content onto the component.
     * @return A new {@link TextConf} object with the given text content.
     * @throws NullPointerException if the supplied styledStrings is null.
     */
    public TextConf content( StyledString... styledStrings ) {
        Objects.requireNonNull(styledStrings);
        return of(Tuple.of(StyledString.class, styledStrings), _fontConf, _clipArea, _placementBoundary, _placement, _offset, _wrapLines, _autoPreferredHeight, _obstacles, _obstaclesFromChildrenAs, _obstaclesFromChildrenEnabled);
    }

    /**
     * Configures this {@link TextConf} object to render the supplied {@link Tuple}
     * of {@link StyledString}s as the text content onto the component.
     * Each {@link StyledString} in the sequence can have its own font properties.
     * This allows you to render visually rich and expressive text.
     * @param styledStrings A {@link Tuple} of {@link StyledString}s which should be
     *                      rendered as the text content onto the component.
     * @return A new {@link TextConf} object with the given text content.
     * @throws NullPointerException if the supplied styledStrings is null.
     */
    public TextConf content( Tuple<StyledString> styledStrings ) {
        if ( _content.equals(styledStrings) ) {
            return this;
        }
        return of(styledStrings, _fontConf, _clipArea, _placementBoundary, _placement, _offset, _wrapLines, _autoPreferredHeight, _obstacles, _obstaclesFromChildrenAs, _obstaclesFromChildrenEnabled);
    }

    /**
     * Configures this {@link TextConf} object to render the supplied collection
     * of {@link StyledString}s as the text content onto the component.<br>
     * Each {@link StyledString} in the sequence can have its own font properties.
     * This allows you to render visually rich and expressive text.
     * @param styledStrings A collection of {@link StyledString}s which should be
     *                      rendered as the text content onto the component.
     * @return A new {@link TextConf} object with the given text content.
     * @throws NullPointerException if the supplied styledStrings is null.
     */
    public TextConf content( Collection<StyledString> styledStrings ) {
        Objects.requireNonNull(styledStrings);
        return content(Tuple.of(StyledString.class, styledStrings));
    }

    private TextConf _fontConf(FontConf fontConf) {
        return of(_content, fontConf, _clipArea, _placementBoundary, _placement, _offset, _wrapLines, _autoPreferredHeight, _obstacles, _obstaclesFromChildrenAs, _obstaclesFromChildrenEnabled);
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
    public TextConf font( Configurator<FontConf> fontConfFunction ) {
        try {
            return _fontConf(fontConfFunction.configure(_fontConf));
        } catch ( Exception e ) {
            log.error(SwingTree.get().logMarker(), "Error configuring font style.", e);
            return this;
        }
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
        return of(_content, _fontConf, clipArea, _placementBoundary, _placement, _offset, _wrapLines, _autoPreferredHeight, _obstacles, _obstaclesFromChildrenAs, _obstaclesFromChildrenEnabled);
    }

    /**
     * Allows you to narrow down the rectangular placement area of the text in the box
     * model of the underlying component using a {@link UI.ComponentBoundary} enum constant.
     * The component boundaries can be thought of as rectangular bounding boxes that capture
     * the transitional edges between different {@link UI.ComponentArea}s.<br>
     * This property ensures that the text is placed inside the transitional bounding box.
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
        return of(_content, _fontConf, _clipArea, placementBoundary, _placement, _offset, _wrapLines, _autoPreferredHeight, _obstacles, _obstaclesFromChildrenAs, _obstaclesFromChildrenEnabled);
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
        return of(_content, _fontConf, _clipArea, _placementBoundary, placement, _offset, _wrapLines, _autoPreferredHeight, _obstacles, _obstaclesFromChildrenAs, _obstaclesFromChildrenEnabled);
    }

    /**
     * Returns a new {@link TextConf} object with the given offset.
     * The offset holds the x and y placement offset of the text.
     * This property is great for making fine adjustments to the text placement.
     * However, for a more robust alignment of the text, it is recommended to use the
     * {@link TextConf#placement(UI.Placement)} and {@link TextConf#placementBoundary(UI.ComponentBoundary)}
     * properties as a first choice.
     * @param offset The offset of the text, defined by an {@link Offset} object.
     *               You may create an {@link Offset} object with {@link Offset#of(float, float)}.
     * @return An updated {@link TextConf} object with the given offset.
     */
    TextConf offset(Offset offset) {
        return of(_content, _fontConf, _clipArea, _placementBoundary, _placement, offset, _wrapLines, _autoPreferredHeight, _obstacles, _obstaclesFromChildrenAs, _obstaclesFromChildrenEnabled);
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

    /**
     * Configures whether the text should be wrapped into multiple lines if the text
     * content exceeds the width of the available space inside the component.
     * The default value is {@code true}, which means that the text will wrap into multiple
     * lines if it exceeds the width of the available space inside the component.
     * @param wrapLines A boolean value which defines whether the text should be wrapped into multiple lines.
     * @return An updated {@link TextConf} object with the given wrap lines property.
     */
    public TextConf wrapLines(boolean wrapLines) {
        return of(_content, _fontConf, _clipArea, _placementBoundary, _placement, _offset, wrapLines, _autoPreferredHeight, _obstacles, _obstaclesFromChildrenAs, _obstaclesFromChildrenEnabled);
    }

    /**
     * Configures whether the preferred height of the styled component should be computed
     * from the text you render onto it through this {@link TextConf} and then set as the preferred height of the component. <br>
     * This will effectively turn the preferred height of the component to a function of the component's
     * width and the displayed text.<br>
     * <b>It will also take full ownership of the preferred height of the component,
     * which means that a preferred height specified elsewhere in the style configuration
     * of the component will be ignored.</b><br>
     *
     * @param autoPreferredHeight If true, then the style engine will compute and set a preferred height
     *                            for the styled component which is based on the text layout produced by this text configuration.
     * @return A new text configuration with the desired auto height behavior.
     */
    public TextConf autoPreferredHeight(boolean autoPreferredHeight) {
        return of(_content, _fontConf, _clipArea, _placementBoundary, _placement, _offset, _wrapLines, autoPreferredHeight, _obstacles, _obstaclesFromChildrenAs, _obstaclesFromChildrenEnabled);
    }

    /**
     * Returns an updated {@link TextConf} with the given shapes registered as obstacles
     * for the text layout engine. The text will wrap around and skip over each obstacle and
     * can never be rendered on top of it. Obstacle shapes are specified in component coordinates.
     * <p>
     * Curved shapes such as circles or ellipses are supported as well.
     *
     * @param obstacles One or more {@link Shape}s in component coordinates for text to skip over.
     * @return An updated {@link TextConf} with the given obstacles.
     */
    public TextConf obstacles( Shape... obstacles ) {
        Objects.requireNonNull(obstacles, "obstacles");
        for ( int i = 0; i < obstacles.length; i++ )
            Objects.requireNonNull(obstacles[i], "obstacles[" + i + "]");
        return of(_content, _fontConf, _clipArea, _placementBoundary, _placement, _offset, _wrapLines, _autoPreferredHeight,
                  Tuple.of(Shape.class, obstacles), _obstaclesFromChildrenAs, _obstaclesFromChildrenEnabled);
    }

    /**
     * Returns an updated {@link TextConf} with the given {@link Tuple} of shapes registered
     * as obstacles for the text layout engine. The text will wrap around and skip over each obstacle and
     * can never be rendered on top of it. Obstacle shapes are specified in component coordinates.
     * <p>
     * Curved shapes such as circles or ellipses are supported as well.
     *
     * @param obstacles A {@link Tuple} of {@link Shape}s in component coordinates for text to skip over.
     * @return An updated {@link TextConf} with the given obstacles.
     */
    public TextConf obstacles( Tuple<Shape> obstacles ) {
        Objects.requireNonNull(obstacles);
        return of(_content, _fontConf, _clipArea, _placementBoundary, _placement, _offset, _wrapLines, _autoPreferredHeight, obstacles, _obstaclesFromChildrenAs, _obstaclesFromChildrenEnabled);
    }

    /**
     * Configures which area of each child component should be used when automatically
     * deriving text-layout obstacles from the parent component's children.
     * <p>
     * When a styled component has child components, then the style engine automatically collects
     * the bounding shapes of the children and registers them as obstacles for the text layout,
     * so that the text flows around the children rather than rendering on top of them.
     * <p>
     * The area value controls <em>which portion</em> of each child is used as the obstacle:
     * <ul>
     *     <li>{@link UI.ComponentArea#ALL} — the full bounding rectangle of the child
     *         (the default — identical to the previous automatic behaviour).</li>
     *     <li>{@link UI.ComponentArea#BODY} — the child's body area (excluding its margin).</li>
     *     <li>{@link UI.ComponentArea#INTERIOR} — the child's interior (excluding margin and border).</li>
     *     <li>{@link UI.ComponentArea#BORDER} — only the child's border band.</li>
     *     <li>{@link UI.ComponentArea#EXTERIOR} — only the child's margin area.</li>
     * </ul>
     * For child components that do not carry a SwingTree style (i.e. plain Swing components
     * without margin or border styling), all non-{@code UNDEFINED} values fall back to the
     * full bounding rectangle, equivalent to {@code ALL}.
     *
     * @param area The component area of each child that should be treated as an obstacle.
     * @return An updated {@link TextConf} with the given child-obstacle area setting.
     */
    public TextConf obstaclesFromChildrenAs( UI.ComponentArea area ) {
        Objects.requireNonNull(area);
        return of(_content, _fontConf, _clipArea, _placementBoundary, _placement, _offset, _wrapLines, _autoPreferredHeight, _obstacles, area, _obstaclesFromChildrenEnabled);
    }

    /**
     * Returns whether the style engine should automatically register the bounding shapes of
     * child components as text-layout obstacles for this text configuration.
     * <p>
     * When {@code true} (the default), every child of the parent component contributes an
     * obstacle shape determined by {@link #obstaclesFromChildrenAs(UI.ComponentArea)}, so that
     * the text flows around the children rather than rendering on top of them.
     * <p>
     * When {@code false}, child components are completely ignored during obstacle collection —
     * the text layout behaves as if no children exist, regardless of the
     * {@link #obstaclesFromChildrenAs(UI.ComponentArea)} setting.
     *
     * @return {@code true} if child-derived obstacles are enabled; {@code false} if they are suppressed.
     */
    boolean obstaclesFromChildrenEnabled() {
        return _obstaclesFromChildrenEnabled;
    }

    /**
     * Controls whether the style engine should automatically register child components
     * of the parent as text-layout obstacles for this text configuration.
     * <p>
     * When set to {@code true} (the default), the style engine collects the bounding shape
     * of every child component and adds it to the text obstacles, so that the rendered text
     * flows around the children rather than overlapping them.  The exact portion of each
     * child that is treated as an obstacle can be refined with
     * {@link #obstaclesFromChildrenAs(UI.ComponentArea)}.
     * <p>
     * When set to {@code false}, child components are completely ignored during obstacle
     * collection.  This is useful when you deliberately want text and child components
     * to share the same visual area — for example when a child is a transparent overlay or
     * a decorative element that should not interrupt the text flow.
     *
     * @param enabled {@code true} to enable automatic child-obstacle registration (the default),
     *                {@code false} to disable it entirely.
     * @return An updated {@link TextConf} with the given child-obstacle-enabled setting.
     */
    public TextConf obstaclesFromChildrenEnabled( boolean enabled ) {
        return of(_content, _fontConf, _clipArea, _placementBoundary, _placement, _offset, _wrapLines, _autoPreferredHeight, _obstacles, _obstaclesFromChildrenAs, enabled);
    }

    @Override
    public TextConf simplified() {
        // Note: When "autoPreferredHeight" is enabled we must not simplify entirely to _NONE,
        //       because this configuration still participates in preferred height computation
        //       for the component, even when the content is empty (the resulting height may be 0).
        if ( _content.isEmpty() && !_autoPreferredHeight )
            return _NONE;
        Tuple<StyledString> simplifiedContent = _content.removeIf( it -> it.string().isEmpty() )
                                                        .map( it -> it.resolveUsing(_fontConf))
                                                        .removeIf( it -> it
                                                                .fontConf()
                                                                .map( fontConf -> fontConf.size() == 0 )
                                                                .orElse( false )
                                                            );
        if ( simplifiedContent.isEmpty() && !_autoPreferredHeight )
             return _NONE;
        return content(simplifiedContent);
    }

    @Override
    public boolean isNone() {
        return this.equals(_NONE);
    }

    TextConf _scale(double scale) {
        return of(
            _content.map( it -> it.mapStyle( s -> s._scale(scale) ) ),
            _fontConf._scale(scale),
            _clipArea,
            _placementBoundary,
            _placement,
            _offset.scale(scale),
            _wrapLines,
            _autoPreferredHeight,
            _obstacles, // obstacle shapes are in component coordinates; scaling is the caller's responsibility
            _obstaclesFromChildrenAs,
            _obstaclesFromChildrenEnabled
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
            _offset.equals(other._offset) &&
            _wrapLines == other._wrapLines &&
            _autoPreferredHeight == other._autoPreferredHeight &&
            _obstacles.equals(other._obstacles) &&
            _obstaclesFromChildrenAs == other._obstaclesFromChildrenAs &&
            _obstaclesFromChildrenEnabled == other._obstaclesFromChildrenEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_content, _fontConf, _clipArea, _placementBoundary, _placement, _offset, _wrapLines, _autoPreferredHeight, _obstacles, _obstaclesFromChildrenAs, _obstaclesFromChildrenEnabled);
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
            "offset=" + _offset + ", " +
            "wrapLines=" + _wrapLines + ", " +
            "autoPreferredHeight=" + _autoPreferredHeight + ", " +
            "obstacles=" + _obstacles + ", " +
            "obstaclesFromChildrenAs=" + _obstaclesFromChildrenAs + ", " +
            "obstaclesFromChildrenEnabled=" + _obstaclesFromChildrenEnabled +
        "]";
    }

}

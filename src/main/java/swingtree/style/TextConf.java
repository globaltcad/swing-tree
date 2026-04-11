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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
 *          like {@link swingtree.UI.Placement#CENTER}.<br>
 *          <b>Important: Note that {@link #obstacles(Shape...)} are only compatible with {@code TOP_LEFT},
 *          {@code TOP} and {@code TOP_RIGHT}! Any other placement turns off obstacle avoidance...</b>
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
 *          then the bounding boxes of those child components will automatically be registered as obstacles.<br>
 *          <p>
 *          <b>Important — placement compatibility:</b> obstacle avoidance is only active when
 *          {@link #placement(UI.Placement)} is one of
 *          {@link UI.Placement#TOP_LEFT}, {@link UI.Placement#TOP}, or {@link UI.Placement#TOP_RIGHT}.
 *          Any other placement silently disables obstacle avoidance and renders the text as if no
 *          obstacles had been registered.  The reason for this restriction is architectural:
 *          <ul>
 *              <li>The algorithm flows text <em>downward</em>, tracking the y-coordinate of each
 *                  successive line to query which horizontal intervals are free at that level.
 *                  This maps cleanly only onto a layout that starts from the top and grows downward.
 *              </li>
 *              <li>A <b>bottom-anchored</b> placement would require running the entire algorithm
 *                  in reverse (growing upward), roughly doubling the implementation complexity.
 *              </li>
 *              <li>A <b>vertically centred</b> placement creates a circular dependency: the height
 *                  of the text block depends on the obstacles, but where the lines land vertically
 *                  depends on the height of the text block.  Resolving this correctly would require
 *                  iterative convergence akin to a fluid-dynamics solver — far beyond the scope of
 *                  a UI layout engine.
 *              </li>
 *          </ul>
 *      </li>
 *      <li><b>Obstacles From Children Enabled</b>
 *          A boolean property that controls whether child components of the styled component
 *          are automatically registered as text-layout obstacles.<br>
 *          When {@code true} (the default), every child contributes an obstacle shape so that
 *          the text flows around the children rather than rendering on top of them.<br>
 *          When {@code false}, child components are completely ignored during obstacle collection
 *          regardless of the {@link TextConf#obstaclesFromChildren(UI.ComponentBoundary)} setting.<br>
 *          You can configure it through {@link TextConf#obstaclesFromChildrenEnabled(boolean)}.
 *      </li>
 *      <li><b>Obstacles From Children</b>
 *          A {@link UI.ComponentBoundary} property that selects <em>which boundary layer</em> of each
 *          child component is used as its obstacle shape when automatic child-obstacle registration is
 *          active (i.e. {@link TextConf#obstaclesFromChildrenEnabled(boolean)} is {@code true}).<br>
 *          Think of the boundaries as an onion peeled inward:
 *          {@link UI.ComponentBoundary#OUTER_TO_EXTERIOR} (the default) uses the full bounding
 *          rectangle of the child including any margin, while inner boundaries such as
 *          {@link UI.ComponentBoundary#EXTERIOR_TO_BORDER} or
 *          {@link UI.ComponentBoundary#BORDER_TO_INTERIOR} shrink the obstacle progressively inward,
 *          letting text flow into the child's margin or border areas respectively.
 *          Children without a SwingTree style fall back to the full bounding rectangle for any value.<br>
 *          You can configure it through {@link TextConf#obstaclesFromChildren(UI.ComponentBoundary)}.<br>
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
    @SuppressWarnings("unchecked")
    private static final Tuple<Pooled<Paragraph>> _EMPTY_CONTENT = (Tuple<Pooled<Paragraph>>) (Tuple<?>) Tuple.of(Pooled.class);

    private static final TextConf _NONE = new TextConf(
                                                _EMPTY_CONTENT,
                                                FontConf.none(),
                                                UI.ComponentArea.INTERIOR,
                                                UI.ComponentBoundary.INTERIOR_TO_CONTENT,
                                                UI.Placement.UNDEFINED,
                                                Offset.none(),
                                                true,
                                                false,
                                                Tuple.of(Shape.class),
                                                UI.ComponentBoundary.OUTER_TO_EXTERIOR,
                                                true
                                            );

    static final TextConf none() {
        return _NONE;
    }

    private final Tuple<Pooled<Paragraph>>  _content;
    private final FontConf                  _fontConf;
    private final UI.ComponentArea          _clipArea;
    private final UI.ComponentBoundary      _placementBoundary;
    private final UI.Placement              _placement;
    private final Offset                    _offset;
    private final boolean                   _wrapLines;
    private final boolean                   _autoPreferredHeight;
    private final Tuple<Shape>              _obstacles;
    private final UI.ComponentBoundary      _obstaclesFromChildrenAs;
    private final boolean                   _obstaclesFromChildrenEnabled;

    private TextConf(
        Tuple<Pooled<Paragraph>>  content,
        FontConf                  fontConf,
        UI.ComponentArea          clipArea,
        UI.ComponentBoundary      placementBoundary,
        UI.Placement              placement,
        Offset                    offset,
        boolean                   wrapLines,
        boolean                   autoPreferredHeight,
        Tuple<Shape>              obstacles,
        UI.ComponentBoundary      obstaclesFromChildrenAs,
        boolean                   obstaclesFromChildrenEnabled
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
        Tuple<Pooled<Paragraph>> content,
        FontConf             fontConf,
        UI.ComponentArea     clipArea,
        UI.ComponentBoundary placementBoundary,
        UI.Placement         placement,
        Offset               offset,
        boolean              wrapLines,
        boolean              autoPreferredHeight,
        Tuple<Shape>         obstacles,
        UI.ComponentBoundary obstaclesFromChildrenAs,
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

    /**
     * Returns the interned paragraphs that make up the text content of this configuration.
     * <p>
     * Before {@link #simplified()} is called this tuple contains at most one un-split,
     * un-interned wrapper paragraph.  After {@link #simplified()} the tuple holds the
     * fully split and interned paragraphs that the layout engine uses as cache keys.
     *
     * @return The tuple of {@link Pooled} {@link Paragraph} objects for this text configuration.
     */
    Tuple<Pooled<Paragraph>> content() {
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
     * Returns which boundary layer of each child component is used when automatically deriving
     * text-layout obstacles from the parent component's children.
     * <p>
     * This getter is the companion to {@link #obstaclesFromChildren(UI.ComponentBoundary)}.
     * The returned value is only consulted when {@link #obstaclesFromChildrenEnabled()}
     * returns {@code true}.
     *
     * @return The {@link UI.ComponentBoundary} that describes how far inward into each child
     *         its obstacle extends; defaults to {@link UI.ComponentBoundary#OUTER_TO_EXTERIOR}.
     */
    UI.ComponentBoundary obstaclesFromChildren() {
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
            return _withRawContent(Tuple.of(StyledString.class));
        return _withRawContent(Tuple.of(StyledString.of(textString)));
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
        return _withRawContent(Tuple.of(StyledString.class, styledStrings));
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
        Objects.requireNonNull(styledStrings);
        if ( _contentEqualTo(styledStrings) )
            return this;
        return _withRawContent(styledStrings);
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

    /**
     * Wraps a raw {@link Tuple} of {@link StyledString}s into a single un-interned
     * {@link Pooled}{@literal <}{@link Paragraph}{@literal >} and returns a new
     * {@link TextConf} with that as its content.  Splitting into multiple paragraphs
     * is deferred until {@link #simplified()} is called.
     */
    private TextConf _withRawContent( Tuple<StyledString> strings ) {
        if ( strings.isEmpty() ) {
            return of(_EMPTY_CONTENT, _fontConf, _clipArea, _placementBoundary, _placement,
                      _offset, _wrapLines, _autoPreferredHeight, _obstacles,
                      _obstaclesFromChildrenAs, _obstaclesFromChildrenEnabled);
        }
        final Pooled<Paragraph> pooled = new Pooled<>(Paragraph.of(strings));
        @SuppressWarnings("unchecked")
        final Tuple<Pooled<Paragraph>> wrapped = (Tuple<Pooled<Paragraph>>) (Tuple<?>) Tuple.of(Pooled.class, pooled);
        return of(wrapped, _fontConf, _clipArea, _placementBoundary, _placement,
                  _offset, _wrapLines, _autoPreferredHeight, _obstacles,
                  _obstaclesFromChildrenAs, _obstaclesFromChildrenEnabled);
    }

    /**
     * Returns {@code true} when the pre-simplified content of this instance is a single
     * (non-blank) paragraph whose {@code styledStrings} tuple equals {@code strings}.
     * Used to short-circuit no-op calls to {@link #content(Tuple)}.
     */
    private boolean _contentEqualTo( Tuple<StyledString> strings ) {
        if ( _content.size() == 1 && !_content.get(0).get().isBlankLine )
            return _content.get(0).get().styledStrings.equals(strings);
        return false;
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
     * </ul><br>
     * <b>Also note that not all placements are compatible with the {@link #obstacles(Shape...)} avoidance feature.
     * Only {@code TOP_LEFT}, {@code TOP} and {@code TOP_RIGHT} allow for text to avoid obstacles...</b>
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
     * Curved shapes such as circles or ellipses are supported as well.<br>
     * <b>Important — placement compatibility:</b> obstacle avoidance is only active when
     * {@link #placement(UI.Placement)} is one of
     * {@link UI.Placement#TOP_LEFT}, {@link UI.Placement#TOP}, or {@link UI.Placement#TOP_RIGHT}.
     * Any other placement silently disables obstacle avoidance.
     * See the {@link TextConf} class-level documentation for a detailed explanation of why
     * bottom-anchored and vertically-centred placements cannot support obstacle avoidance.
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
     * Curved shapes such as circles or ellipses are supported as well.<br>
     * <b>Important — placement compatibility:</b> obstacle avoidance is only active when
     * {@link #placement(UI.Placement)} is one of
     * {@link UI.Placement#TOP_LEFT}, {@link UI.Placement#TOP}, or {@link UI.Placement#TOP_RIGHT}.
     * Any other placement silently disables obstacle avoidance.
     * See the {@link TextConf} class-level documentation for a detailed explanation of why
     * bottom-anchored and vertically-centred placements cannot support obstacle avoidance.
     *
     * @param obstacles A {@link Tuple} of {@link Shape}s in component coordinates for text to skip over.
     * @return An updated {@link TextConf} with the given obstacles.
     */
    public TextConf obstacles( Tuple<Shape> obstacles ) {
        Objects.requireNonNull(obstacles);
        return of(_content, _fontConf, _clipArea, _placementBoundary, _placement, _offset, _wrapLines, _autoPreferredHeight, obstacles, _obstaclesFromChildrenAs, _obstaclesFromChildrenEnabled);
    }

    /**
     * Configures which boundary layer of each child component should be used when automatically
     * deriving text-layout obstacles from the parent component's children.
     * <p>
     * When a styled component has child components, the style engine automatically collects
     * a shape for each child and registers it as an obstacle for the text layout, so that the
     * text flows around the children rather than rendering on top of them.
     * <p>
     * Think of the boundaries as an onion peeled inward — each boundary defines how far inside
     * a child the obstacle extends.  The available values are:
     * <ul>
     *     <li>{@link UI.ComponentBoundary#OUTER_TO_EXTERIOR} — the full bounding rectangle of
     *         the child including any margin (the default).</li>
     *     <li>{@link UI.ComponentBoundary#EXTERIOR_TO_BORDER} — shrinks the obstacle inward to
     *         exclude the child's margin, so text may flow into the margin area.</li>
     *     <li>{@link UI.ComponentBoundary#BORDER_TO_INTERIOR} — shrinks further to exclude both
     *         margin and border, so text may flow through margin and border areas.</li>
     *     <li>{@link UI.ComponentBoundary#INTERIOR_TO_CONTENT} — shrinks to the content area,
     *         letting text flow through margin, border, and padding of the child.</li>
     * </ul>
     * Child components that do not carry a SwingTree style (i.e. plain Swing components without
     * margin or border styling) always fall back to the full bounding rectangle.
     *
     * @param boundary The component boundary of each child that should be treated as an obstacle.
     * @return An updated {@link TextConf} with the given child-obstacle boundary setting.
     */
    public TextConf obstaclesFromChildren( UI.ComponentBoundary boundary ) {
        Objects.requireNonNull(boundary);
        return of(_content, _fontConf, _clipArea, _placementBoundary, _placement, _offset, _wrapLines, _autoPreferredHeight, _obstacles, boundary, _obstaclesFromChildrenEnabled);
    }

    /**
     * Returns whether the style engine should automatically register the bounding shapes of
     * child components as text-layout obstacles for this text configuration.
     * <p>
     * When {@code true} (the default), every child of the parent component contributes an
     * obstacle shape determined by {@link #obstaclesFromChildren(UI.ComponentBoundary)}, so that
     * the text flows around the children rather than rendering on top of them.
     * <p>
     * When {@code false}, child components are completely ignored during obstacle collection —
     * the text layout behaves as if no children exist, regardless of the
     * {@link #obstaclesFromChildren(UI.ComponentBoundary)} setting.
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
     * {@link #obstaclesFromChildren(UI.ComponentBoundary)}.
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

        // Collect all StyledStrings from the (pre-simplified) content.
        // Before simplified() is called, _content holds at most one wrapper paragraph.
        // After a second call, it holds the already-split interned paragraphs, so we flatten.
        final Tuple<StyledString> rawStrings;
        if ( _content.size() == 1 && !_content.get(0).get().isBlankLine ) {
            rawStrings = _content.get(0).get().styledStrings;
        } else {
            // We should never really get here! -> simplification should only happen on un-simplified / un-split content.
            final List<StyledString> flat = new ArrayList<>();
            for ( Pooled<Paragraph> p : _content ) {
                if ( !p.get().isBlankLine ) {
                    for ( StyledString s : p.get().styledStrings )
                        flat.add(s);
                }
            }
            rawStrings = Tuple.of(StyledString.class, flat);
        }

        // Existing simplification: remove empties, resolve font, remove zero-size segments.
        final Tuple<StyledString> simplified = rawStrings
                .removeIf( it -> it.string().isEmpty() )
                .map( it -> it.resolveUsing(_fontConf) )
                .removeIf( it -> it.fontConf()
                                   .map( fc -> fc.size() == 0 )
                                   .orElse(false) );

        if ( simplified.isEmpty() && !_autoPreferredHeight )
            return _NONE;

        // Split by '\n' into interned paragraphs, then store as the new _content.
        final Tuple<Pooled<Paragraph>> splitContent = _splitAndIntern(simplified);
        final Tuple<Pooled<Paragraph>> newContent = splitContent.isEmpty() ? _EMPTY_CONTENT : splitContent;

        if ( newContent.isEmpty() && !_autoPreferredHeight )
            return _NONE;

        return of(newContent, _fontConf, _clipArea, _placementBoundary, _placement,
                  _offset, _wrapLines, _autoPreferredHeight, _obstacles,
                  _obstaclesFromChildrenAs, _obstaclesFromChildrenEnabled);
    }

    /**
     * Splits a flat sequence of {@link StyledString}s at {@code '\n'} boundaries into
     * interned {@link Pooled}{@literal <}{@link Paragraph}{@literal >} objects.
     * <p>
     * Each contiguous run of text between newlines becomes one {@link Paragraph}.
     * A newline that produces an empty segment (blank line) becomes a
     * {@link Paragraph#blankLine()} entry.  The resulting paragraphs are interned
     * via {@link Pooled#intern()} so identical paragraphs share the same reference and
     * can be used as identity-stable cache keys in the layout engine.
     */
    private static Tuple<Pooled<Paragraph>> _splitAndIntern( Tuple<StyledString> content ) {
        final List<Pooled<Paragraph>> result = new ArrayList<>();
        List<StyledString> current = null;
        for ( StyledString s : content ) {
            final String[] parts = s.string().split("\n", -1);
            if ( parts.length <= 1 ) {
                if ( current == null ) current = new ArrayList<>();
                current.add(s);
            } else {
                for ( int i = 0; i < parts.length; i++ ) {
                    if ( current == null ) current = new ArrayList<>();
                    if ( !parts[i].isEmpty() ) current.add(s.withString(parts[i]));
                    if ( i < parts.length - 1 ) {
                        result.add(_internedParagraphFrom(current));
                        current = null;
                    }
                }
            }
        }
        if ( current != null )
            result.add(_internedParagraphFrom(current));
        @SuppressWarnings("unchecked")
        final Tuple<Pooled<Paragraph>> tuple = ((Tuple<Pooled<Paragraph>>) (Tuple<?>) Tuple.of(Pooled.class)).addAll(result);
        return tuple;
    }

    private static Pooled<Paragraph> _internedParagraphFrom( List<StyledString> strings ) {
        final int len = strings.stream().mapToInt(s -> s.string().length()).sum();
        final Paragraph p = len <= 0
            ? Paragraph.blankLine()
            : Paragraph.of(Tuple.of(StyledString.class, strings));
        return new Pooled<>(p).intern();
    }

    @Override
    public boolean isNone() {
        return this.equals(_NONE);
    }

    TextConf _scale( double scale ) {
        // Scale each StyledString inside every paragraph, wrapping them back into un-interned
        // Pooled<Paragraph> objects.  Interning happens later in simplified().
        final List<Pooled<Paragraph>> scaledParagraphs = new ArrayList<>(_content.size());
        for ( Pooled<Paragraph> pooled : _content ) {
            final Paragraph original = pooled.get();
            if ( original.isBlankLine ) {
                scaledParagraphs.add(new Pooled<>(Paragraph.blankLine()));
            } else {
                final Tuple<StyledString> scaledStrings = original.styledStrings.map( s -> s.mapStyle( st -> st._scale(scale) ) );
                scaledParagraphs.add(new Pooled<>(Paragraph.of(scaledStrings)));
            }
        }
        @SuppressWarnings("unchecked")
        final Tuple<Pooled<Paragraph>> scaledContent = ((Tuple<Pooled<Paragraph>>) (Tuple<?>) Tuple.of(Pooled.class)).addAll(scaledParagraphs);
        return of(
            scaledContent,
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
        Tuple<StyledString> flatContent = _content.stream().flatMap(p->p.get().styledStrings.stream()).collect(Tuple.collectorOf(StyledString.class));
        return this.getClass().getSimpleName() + "[" +
            "content=" + flatContent + ", " +
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

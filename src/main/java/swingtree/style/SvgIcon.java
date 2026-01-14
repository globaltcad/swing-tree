package swingtree.style;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.DomProcessor;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.layout.Bounds;
import swingtree.layout.Size;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 *   A specialized {@link ImageIcon} subclass that allows you to use SVG based icon images in your GUI.
 *   This in essence just a wrapper around the <a href="https://github.com/weisJ/jsvg">JSVG library</a>,
 *   which renders SVG images using the Java2D graphics API.
 *   <p>
 *   You may use this like a regular {@link ImageIcon}, but have to keep in mind that SVG documents
 *   do not really have a fixed size, meaning that the {@link #getIconWidth()} and {@link #getIconHeight()}
 *   on a freshly loaded {@link SvgIcon} will return -1 which causes the icon to be rendered according to the
 *   width and height of the component it is rendered into (see {@link #paintIcon(Component, java.awt.Graphics, int, int)}).
 *   <p>
 *   If you want to render the icon with a fixed size, you can use the {@link #withIconWidth(int)} and
 *   {@link #withIconHeight(int)} or {@link #withIconSize(int, int)} methods to create a new {@link SvgIcon}
 *   with the given width and height.
 *   <p>
 *   An {@link SvgIcon} with an undefined width or height will also be using the {@link UI.FitComponent}
 *   and {@link UI.Placement} policies to determine how the icon should be placed and sized within a component.
 *   Use the {@link #withFitComponent(UI.FitComponent)} and {@link #withPreferredPlacement(UI.Placement)}
 *   methods to create a new {@link SvgIcon} with the given policies and use the {@link #getFitComponent()}
 *   and {@link #getPreferredPlacement()} methods to retrieve the current policies
 *   (Not that these will not have any effect if the width and height are both defined).
 *   <p>
 *   <b>Also note that the direct use of this class and it's API is discouraged in favour of simply
 *   calling the {@link UI#findIcon(String)} or {@link UI#findSvgIcon(String)} methods, which
 *   will automatically load and cache all the icons for you.</b>
 */
public final class SvgIcon extends ImageIcon
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SvgIcon.class);
    private static final UI.FitComponent DEFAULT_FIT_COMPONENT = UI.FitComponent.UNDEFINED;
    private static final UI.Placement    DEFAULT_PLACEMENT     = UI.Placement.UNDEFINED;
    private static final int             NO_SIZE               = -1;
    private static final Insets          ZERO_INSETS           = new Insets(0,0,0,0);


    private final RawSVG                _core;
    private final Size                  _size;
    private final Unit                  _widthUnit;
    private final Unit                  _heightUnit;
    private final UI.FitComponent       _fitComponent;
    private final UI.Placement          _preferredPlacement;

    private @Nullable BufferedImage _cache = null;

    /**
     *  Tries to create an {@link SvgIcon} from a resource path
     *  defined by the supplied string.
     *
     * @param path The path to the SVG document.
     */
    public static SvgIcon at( String path ) {
        RawSVG args = _loadSvgDocument(SvgIcon.class.getResource(path), Size.unknown());
        return new SvgIcon(args, DEFAULT_FIT_COMPONENT, DEFAULT_PLACEMENT);
    }

    /**
     *  Creates an {@link SvgIcon} from a resource path and a custom size.
     * @param path The path to the SVG document.
     * @param size The size of the icon in the form of a {@link Size}.
     */
    public static SvgIcon at( String path, Size size ) {
        RawSVG args = _loadSvgDocument(SvgIcon.class.getResource(path), size);
        return new SvgIcon(args, DEFAULT_FIT_COMPONENT, DEFAULT_PLACEMENT);
    }

    /**
     *  Creates an {@link SvgIcon} from the supplied URL pointing to an SVG document.
     *  If parsing the SVG document fails, an empty icon will be created, which does not render anything.
     *  The resulting icon will have an unknown size, placement and fit policy,
     *  meaning that it will be rendered according to the size of the component
     *  it is rendered into (see {@link #paintIcon(Component, Graphics, int, int)}).
     * @param svgUrl The URL to the SVG document.
     */
    public static SvgIcon at( URL svgUrl ) {
        RawSVG args = _loadSvgDocument(svgUrl, Size.unknown());
        return new SvgIcon(args, DEFAULT_FIT_COMPONENT, DEFAULT_PLACEMENT);
    }

    /**
     * @param svgUrl The URL to the SVG document.
     * @param size The size of the icon in the form of a {@link Size}.
     */
    public static SvgIcon at( URL svgUrl, Size size ) {
        RawSVG args = _loadSvgDocument(svgUrl, size);
        return new SvgIcon(args, DEFAULT_FIT_COMPONENT, DEFAULT_PLACEMENT);
    }

    /**
     *  Allows you to directly create an {@link SvgIcon} from
     *  a string containing an SVG text.
     * @param svgString A string containing SVG text.
     * @return A new {@link SvgIcon} created from a {@link String}
     *         containing an SVG document.
     */
    public static SvgIcon of( String svgString ) {
        return of( svgString, Size.unknown() );
    }

    /**
     *  Allows you to directly create an {@link SvgIcon} from
     *  a string containing an SVG text, with a custom width
     *  and height defined by the supplied {@link Size}.
     * @param svgString A string containing SVG text.
     * @param size A {@link Size} object containing the desired SVG width and height.
     * @return A new {@link SvgIcon} created from a {@link String}
     *         containing an SVG document and a custom width and height...
     */
    public static SvgIcon of( String svgString, Size size ) {
        InputStream inputStream = new ByteArrayInputStream(svgString.getBytes(StandardCharsets.UTF_8));
        RawSVG args = _loadSvgDocument(inputStream, size);
        return new SvgIcon(args, DEFAULT_FIT_COMPONENT, DEFAULT_PLACEMENT);
    }

    /**
     *  Reads the SVG document from the supplied input stream and creates
     *  an {@link SvgIcon} from it. If parsing the SVG document fails,
     *  an empty icon will be created, which does not render anything.
     *  The resulting icon will have an unknown size, placement and fit policy,
     *  meaning that it will be rendered according to the size of the component
     *  it is rendered into (see {@link #paintIcon(Component, Graphics, int, int)}).
     *  If you want to customize the size, layout and placement of the icon, consider creating
     *  derived versions using the {@link #withIconWidth(int)}, {@link #withIconHeight(int)},
     *  {@link #withIconSize(int, int)}, {@link #withFitComponent(UI.FitComponent)} and
     *  {@link #withPreferredPlacement(UI.Placement)} methods.
     * @param stream The input stream supplying the text data of the SVG document.
     */
    public static SvgIcon of( InputStream stream ) {
        RawSVG args = _loadSvgDocument(stream, Size.unknown());
        return new SvgIcon(args, DEFAULT_FIT_COMPONENT, DEFAULT_PLACEMENT);
    }

    /**
     *  Reads the SVG document from the supplied input stream and creates
     *  an {@link SvgIcon} from it with a custom width and height defined
     *  by the supplied {@link Size}.<br>
     *  If parsing the SVG document fails, an empty icon will be created, which does not render anything.
     *  The resulting icon will have the given size, but the default placement
     *  and fit policy, meaning that it will be rendered according to the size of the component
     *  it is rendered into (see {@link #paintIcon(Component, Graphics, int, int)}).<br>
     *  If you want to customize the layout and placement of the icon, consider creating
     *  derived versions using the {@link #withFitComponent(UI.FitComponent)} and
     *  {@link #withPreferredPlacement(UI.Placement)} methods.
     * @param stream The input stream supplying the text data of the SVG document.
     * @param size The size of the icon in the form of a {@link Size}.
     */
    public static SvgIcon of( InputStream stream, Size size ) {
        RawSVG args = _loadSvgDocument(stream, size);
        return new SvgIcon(args, DEFAULT_FIT_COMPONENT, DEFAULT_PLACEMENT);
    }

    /**
     *  Allows you to create an {@link SvgIcon} from an already loaded {@link SVGDocument}.
     *  The icon will be created with an unknown size, meaning that it will be rendered
     *  according to the size of the component it is rendered into (see {@link #paintIcon(Component, Graphics, int, int)}).
     *  If you want to customize the size, layout and placement of the icon, consider creating
     *  derived versions using the {@link #withIconWidth(int)}, {@link #withIconHeight(int)},
     *  {@link #withIconSize(int, int)}, {@link #withFitComponent(UI.FitComponent)} and
     *  {@link #withPreferredPlacement(UI.Placement)} methods.
     * @param svgDocument The already loaded SVG document, which will be used to render the icon.
     */
    public static SvgIcon of( SVGDocument svgDocument ) {
        RawSVG args = new RawSVG(svgDocument, Size.unknown(), Unit.UNKNOWN, Unit.UNKNOWN);
        return new SvgIcon(args, DEFAULT_FIT_COMPONENT, DEFAULT_PLACEMENT);
    }

    /**
     * Allows you to create an {@link SvgIcon} from an already loaded {@link SVGDocument}
     * with a custom width and height defined by the supplied {@link Size}.<br>
     * If you want to customize the layout and placement of the icon, consider creating
     * derived versions using the {@link #withFitComponent(UI.FitComponent)} and
     * {@link #withPreferredPlacement(UI.Placement)} methods.
     * @param svgDocument The already loaded SVG document, which will be used to render the icon.
     * @param size The size of the icon in the form of a {@link Size}.
     */
    public static SvgIcon of( SVGDocument svgDocument, Size size ) {
        RawSVG args = new RawSVG(svgDocument, size, Unit.UNKNOWN, Unit.UNKNOWN);
        return new SvgIcon(args, DEFAULT_FIT_COMPONENT, DEFAULT_PLACEMENT);
    }

    private static RawSVG _loadSvgDocument(@Nullable URL svgUrl, Size size ) {
        if ( svgUrl == null )
            return new RawSVG(null, size, Unit.UNKNOWN, Unit.UNKNOWN);
        return _loadSvgDocument( processor -> {
            SVGLoader loader = new SVGLoader();
            return loader.load(svgUrl, LoaderContext.builder().preProcessor(processor).build());
        }, size);
    }

    private static RawSVG _loadSvgDocument(InputStream stream, Size size ) {
        return _loadSvgDocument( processor -> {
            SVGLoader loader = new SVGLoader();
            return loader.load(stream, null, LoaderContext.builder().preProcessor(processor).build());
        }, size);
    }

    private static RawSVG _loadSvgDocument(Function<DomProcessor, @Nullable SVGDocument> loader, Size size) {
        SVGDocument tempSVGDocument = null;
        Unit widthUnit  = Unit.UNKNOWN;
        Unit heightUnit = Unit.UNKNOWN;
        try {
            AtomicReference<String> widthUnitString = new AtomicReference<>("");
            AtomicReference<String> heightUnitString = new AtomicReference<>("");
            tempSVGDocument = loader.apply(dom->{
                widthUnitString.set(_parseUnitFrom(dom.attribute("width", "")));
                heightUnitString.set(_parseUnitFrom(dom.attribute("height", "")));
            });
            widthUnit = _unitOf(Objects.requireNonNull(widthUnitString.get()));
            heightUnit = _unitOf(Objects.requireNonNull(heightUnitString.get()));
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "Failed to load SVG document! ", e);
        }
        if ( tempSVGDocument != null ) {
            if ( widthUnit != Unit.UNKNOWN && !size.hasPositiveWidth() ) {
                size = size.withWidth(tempSVGDocument.size().width);
            }
            if ( heightUnit != Unit.UNKNOWN && !size.hasPositiveHeight() ) {
                size = size.withHeight(tempSVGDocument.size().height);
            }
        }
        return new RawSVG(tempSVGDocument, size, widthUnit, heightUnit);
    }

    private static Unit _unitOf(String unitString) {
        if ( unitString.isEmpty() )
            return Unit.UNKNOWN;
        if ( unitString.equals("px") )
            return Unit.PX;
        if ( unitString.equals("%") )
            return Unit.PERCENTAGE;

        return Unit.UNKNOWN;
    }

    private static String _parseUnitFrom( String numberWithUnit ) {
        numberWithUnit =  numberWithUnit.trim();// sanitize
        if ( numberWithUnit.isEmpty() ) {
            return "";
        }
        // This regex matches the leading number (including decimals) and removes it
        String unit = numberWithUnit.replaceAll("^[-+]?\\d*\\.?\\d+", "");
        if ( unit.isEmpty() ) {
            return "px"; // Default to pixels if no unit is specified
        }
        return unit.trim(); // There may be a space between the number and the unit!
    }

    private SvgIcon(
        RawSVG args,
        UI.FitComponent  fitComponent,
        UI.Placement     preferredPlacement
    ) {
        this(args, args.size, args.widthUnit, args.heightUnit, fitComponent, preferredPlacement);
    }

    private SvgIcon(
        RawSVG core,
        Size                  size,
        Unit                  widthUnit,
        Unit                  heightUnit,
        UI.FitComponent       fitComponent,
        UI.Placement          preferredPlacement
    ) {
        super();
        _core               = Objects.requireNonNull(core);
        _size               = Objects.requireNonNull(size);
        _widthUnit          = Objects.requireNonNull(widthUnit);
        _heightUnit         = Objects.requireNonNull(heightUnit);
        _fitComponent       = Objects.requireNonNull(fitComponent);
        _preferredPlacement = Objects.requireNonNull(preferredPlacement);
    }

    private Size _size() {
        if ( _widthUnit != Unit.PERCENTAGE && _heightUnit != Unit.PERCENTAGE )
            return _size;
        return Size.of(
                _widthUnit == Unit.PERCENTAGE ? -1 : _size.width().orElse(-1f),
                _heightUnit == Unit.PERCENTAGE ? -1 : _size.height().orElse(-1f)
            );
    }

    /**
     * Returns the unit of the width of the icon or an empty {@link String} if the encountered unit is unknown.
     * If the underlying document reports a valid width without a unit postfix, then it will be interpreted as "px"!<br>
     * <b>
     *     Please note that this method does not return the exact same unit found in the underlying
     *     document, since not all units are supported by the SwingTree icon...
     * </b>
     * @return The unit of the width of the SVG icon in the form of a string similar
     *          to how it is present in the underlying document.
     */
    public String widthUnitString() {
        return _widthUnit.toPublicString();
    }

    /**
     * Returns the unit of the height of the icon or an empty {@link String} if the encountered unit is unknown.
     * If the underlying document reports a valid height without a unit postfix, then it will be interpreted as "px"!<br>
     * <b>
     *     Please note that this method does not return the exact same unit found in the underlying
     *     document, since not all units are supported by the SwingTree icon...
     * </b>
     * @return The unit of the height of the SVG icon in the form of a string similar
     *          to how it is present in the underlying document.
     */
    public String heightUnitString() {
        return _heightUnit.toPublicString();
    }

    /**
     *  Exposes the width of the icon <b>in component pixel space</b>,
     *  or -1 if the icon should be rendered according to the width of
     *  a given component or the width of the SVG document itself.
     *  (...or other policies such as {@link swingtree.UI.FitComponent} and {@link swingtree.UI.Placement}).<br>
     *  <b>
     *      Note that the returned width is dynamically scaled according to
     *      the current {@link swingtree.UI#scale()} value.
     *      This is to ensure that the icon is rendered at the correct size
     *      according to the current DPI settings.
     *      If you want the unscaled width, use {@link #getBaseWidth()}.
     *  </b>
     * @return The width of the icon <b>in component pixel space</b>, or -1 if the icon should be rendered according
     *         to the width of a given component or the width of the SVG document itself.
     * @see #getBaseWidth() to access the icon width in <b>developer pixel space</b>.
     */
    @Override
    public int getIconWidth() {
        Size adjustedSize = _sizeWithAspectRatioCorrection(_size());
        return adjustedSize.width().map(UI::scale).map(Math::round).orElse(NO_SIZE);
    }

    /**
     *  Creates an updated {@link SvgIcon} with a new width measured in "developer pixel",
     *  which effectively translates to what is being reported by {@link #getBaseWidth()},
     *  and indirectly also {@link #getIconWidth()} (which is scaled to component pixel space).
     *  Since the supplied width is developer pixel based, <b>the existing width-unit in
     *  this instance will be overridden to being pixel based in the returned instance</b>
     * <p>
     * This method is useful when you want to adjust the width of
     * an SVG without affecting the (pixel based) height!<br>
     * For example, calling {@code icon.withIconWidth(100)} on an icon
     * with dimensions 20pxx20px will result in an icon that is 100px×20px pixels in size.<br>
     * <b>
     *     So be aware that using this method may also change
     *     the aspect ratio of the icon, possibly causing
     *     it to be rendered distorted...
     * </b>
     * </p><br>
     * <p>
     *     However, in case of the height of this icon being percentage based, then the new
     *     icon will have its height overridden to {@code -1} and the unit of the height will
     *     also always be pixel based (see {@link #widthUnitString()} and {@link #heightUnitString()}).
     * </p>
     *
     * @param newWidth The width of the icon, or -1 if the icon should be rendered according
     *              to the width of a given component or the width of the SVG document itself.
     * @return A new {@link SvgIcon} with the given width.
     *        If the width is -1, the icon will be rendered according to the width of a given component
     *        or the width of the SVG document itself.
     * @see #withIconHeight(int) similar to this method but for setting the height instead of width...
     * @see #withIconSize(int, int) for setting both dimensions independently.
     * @see #getIconWidth() to get the current icon width in <b>component pixel space.</b>
     * @see #getBaseWidth() to get the current icon width in <b>developer pixel space.</b>
     */
    public SvgIcon withIconWidth( int newWidth ) {
        int width = _size.width().map(Math::round).orElse(NO_SIZE);
        if ( newWidth == width && _widthUnit != Unit.PERCENTAGE )
            return this;
        return new SvgIcon(
                _core,
                _heightUnit == Unit.PERCENTAGE ? Size.of(newWidth, -1) : _size.withWidth(newWidth),
                Unit.PX,
                Unit.PX,
                _fitComponent,
                _preferredPlacement
            );
    }

    /**
     *  Exposes the height of the icon <b>in component pixel space</b>, or -1 if the icon should be rendered according
     *  to the height of a given component or the height of the SVG document itself.
     *  (...or other policies such as {@link swingtree.UI.FitComponent} and {@link swingtree.UI.Placement}).<br>
     *  <b>
     *      Note that the returned height is dynamically scaled according to
     *      the current {@link swingtree.UI#scale()} value.
     *      This is to ensure that the icon is rendered at the correct size
     *      according to the current DPI settings.
     *      If you want the unscaled height, use {@link #getBaseHeight()}.
     *  </b>
     *
     * @return The height of the icon <b>in component pixel space</b>, or -1 if the icon to
     *          indicate an undefined height, which implies that the icon ought to be
     *          rendered according to the height of a given component or other properties...
     * @see #getBaseHeight() to access the icon height in <b>developer pixel space</b>.
     */
    @Override
    public int getIconHeight() {
        Size adjustedSize = _sizeWithAspectRatioCorrection(_size());
        return adjustedSize.height().map(UI::scale).map(Math::round).orElse(NO_SIZE);
    }

    /**
     *  Exposes the width of the icon <b>in developer pixels</b>, which is the width
     *  that was set when the icon was created or updated using the
     *  {@link #withIconWidth(int)} method.<br>
     *  <b>
     *      Note that this width is not scaled to component pixel space using
     *      the current {@link swingtree.UI#scale()} factor.
     *      If you want a scaled width, that is more suitable for rendering the icon,
     *      use the {@link #getIconWidth()} method.
     *  </b>
     *
     * @return The width of the icon without scaling.
     * @see #getIconWidth() to access the icon width in <b>component pixel space</b>.
     */
    public int getBaseWidth() {
        return _size().width().map(Math::round).orElse(NO_SIZE);
    }

    /**
     *  Exposes the height of the icon <b>in developer pixels</b>, which is
     *  also the height that was set when the icon was created or updated using the
     *  {@link #withIconHeight(int)} method.<br>
     *  <b>
     *      Note that this height is not scaled to component pixel space using
     *      the current {@link swingtree.UI#scale()} factor.
     *      If you want a scaled height, that is more suitable for rendering the icon,
     *      use the {@link #getIconHeight()} method.
     *  </b>
     *
     * @return The height of the icon without scaling.
     * @see #getIconHeight() to access the icon height in <b>component pixel space</b>.
     */
    public int getBaseHeight() {
        return _size().height().map(Math::round).orElse(NO_SIZE);
    }

    /**
     *  Creates a new {@link SvgIcon} with the specified height measured in "developer pixels".
     *  The supplied height is developer pixel based, and <b>the existing height-unit in
     *  this instance will be overridden to being pixel based in the returned instance</b>.
     *  <p>
     *  This method is useful when you want to adjust the height of an SVG without
     *  affecting the (pixel based) width!<br>
     *  For example, calling {@code icon.withIconHeight(50)} on an icon with
     *  dimensions 20px×20px will result in an icon that is 20px×50px in size.<br>
     *  <b>
     *      Be aware that using this method may also change the aspect ratio
     *      of the icon, potentially causing it to be rendered distorted.
     *  </b>
     *  </p><br>
     *  <p>
     *      If the width of this icon is percentage based, then the new icon will have
     *      its width overridden to {@code -1} and the unit of the width will also always
     *      be pixel based (see {@link #widthUnitString()} and {@link #heightUnitString()}).
     *      This ensures that percentage-based dimensions are not mixed with explicit
     *      pixel dimensions in a single icon instance.
     *  </p>
     *
     * @param newHeight The height of the icon in developer pixels, or -1 if the icon
     *                  should be rendered according to the height of a given component
     *                  or the height of the SVG document itself.
     * @return A new {@link SvgIcon} with the given height.
     *         If the height is -1, the icon will be rendered according to the height
     *         of a given component or the height of the SVG document itself.
     * @see #withIconWidth(int) similar to this method but for setting the width instead of height...
     * @see #withIconSize(int, int) for setting both dimensions independently.
     * @see #getIconHeight() to get the current icon height in <b>component pixel space.</b>
     * @see #getBaseHeight() to get the current icon height in <b>developer pixel space.</b>
     */
    public SvgIcon withIconHeight( int newHeight ) {
        int currentHeight = _size.height().map(Math::round).orElse(NO_SIZE);
        if ( newHeight == currentHeight && _heightUnit != Unit.PERCENTAGE )
            return this;
        return new SvgIcon(
                _core,
                _widthUnit == Unit.PERCENTAGE ? Size.of(-1, newHeight) : _size.withHeight(newHeight),
                Unit.PX,
                Unit.PX,
                _fitComponent,
                _preferredPlacement
            );
    }

    /**
     *  Creates a new {@link SvgIcon} with the specified width and height measured in "developer pixels".
     *  Both supplied dimensions are developer pixel based, and <b>the existing width and height units
     *  of this instance will be overridden to being pixel based in the returned instance</b>.
     *  <p>
     *  This method allows you to explicitly set both dimensions of the icon, giving you
     *  complete control over its rendered size. However, be mindful that this may distort
     *  the icon if the new dimensions don't maintain the original aspect ratio.
     *  </p><br>
     *  <p>
     *      <b>Important:</b> This method implicitly converts any percentage-based
     *      dimensions to developer pixel-based dimensions. The conversion preserves
     *      the percentage value's intent by resolving it to -1 (unknown) when a negative
     *      value is provided. This ensures that percentage-based sizing can be cleared
     *      when needed.
     *  </p>
     *  <p>
     *      For example, if you have an icon with width="50%" and height="100%",
     *      calling {@code withIconSize(100, -1)} will result in an icon with
     *      width=100px and height=-1 (undefined), allowing the height to be
     *      determined by other factors like the component size or aspect ratio.
     *  </p>
     *
     * @param newWidth The width of the icon in developer pixels, or -1 to indicate
     *                 an undefined width that should be determined by other factors.
     * @param newHeight The height of the icon in developer pixels, or -1 to indicate
     *                  an undefined height that should be determined by other factors.
     * @return A new {@link SvgIcon} with the given width and height.
     *         Negative values indicate that the dimension should be determined
     *         by the component, SVG document, or other layout policies.
     * @see #withIconWidth(int) for setting only the width.
     * @see #withIconHeight(int) for setting only the height.
     * @see #withIconSizeFromWidth(int) for setting width while maintaining aspect ratio.
     * @see #withIconSizeFromHeight(int) for setting height while maintaining aspect ratio.
     */
    public SvgIcon withIconSize( int newWidth, int newHeight ) {
        newWidth  = newWidth  < 0 ? NO_SIZE : newWidth;
        newHeight = newHeight < 0 ? NO_SIZE : newHeight;
        int width  = _size.width().map(Math::round).orElse(NO_SIZE);
        int height = _size.height().map(Math::round).orElse(NO_SIZE);
        if ( newWidth == width && newHeight == height && _widthUnit != Unit.PERCENTAGE && _heightUnit != Unit.PERCENTAGE )
            return this;
        return new SvgIcon(
                _core,
                Size.of(
                    newWidth < 0 &&  _widthUnit == Unit.PERCENTAGE ?  -1 : newWidth,
                    newHeight < 0 && _heightUnit == Unit.PERCENTAGE ? -1 : newHeight
                ),
                Unit.PX,
                Unit.PX,
                _fitComponent,
                _preferredPlacement
            );
    }

    /**
     * Creates a new {@link SvgIcon} with the specified width and height measured in "developer pixels".
     * This method accepts a {@link Size} object containing the desired dimensions.
     * Both supplied dimensions are developer pixel based, and <b>the existing width and height units
     * of this instance will be overridden to being pixel based in the returned instance</b>.
     * <p>
     * This method allows you to explicitly set both dimensions of the icon using a {@link Size} object,
     * giving you complete control over its rendered size. However, be mindful that this may distort
     * the icon if the new dimensions don't maintain the original aspect ratio.
     * </p><br>
     * <p>
     * <b>Important:</b> This method implicitly converts any percentage-based
     * dimensions in the current icon to pixel-based dimensions in the returned icon.
     * The conversion preserves the percentage value's intent by resolving it to -1 (unknown)
     * when a negative value is provided in the {@link Size} object. This ensures that
     * percentage-based sizing can be cleared when needed.
     * </p>
     * <p>
     * For example, if you have an icon with width="50%" and height="100%",
     * calling {@code withIconSize(Size.of(100, -1))} will result in an icon with
     * width=100px and height=-1 (undefined), allowing the height to be
     * determined by other factors like the component size or aspect ratio.
     * </p>
     * <p>
     * The {@link Size} object can contain optional dimensions (using {@link Optional} semantics),
     * allowing you to specify only width, only height, both, or neither. When a dimension
     * is not present in the {@link Size} object, the corresponding dimension in the
     * returned icon will be set to -1 (unknown).
     * </p>
     *
     * @param size A {@link Size} object containing the desired width and/or height for the icon.
     *             Use negative values or empty optionals to indicate that a dimension should
     *             remain undefined and be determined by other factors.
     * @return A new {@link SvgIcon} with the given width and height from the {@link Size} object.
     *         Negative values indicate that the dimension should be determined by the component,
     *         SVG document, or other layout policies.
     * @see #withIconSize(int, int) for the integer-based version of this method.
     * @see #withIconWidth(int) for setting only the width.
     * @see #withIconHeight(int) for setting only the height.
     * @see #withIconSizeFromWidth(int) for setting width while maintaining aspect ratio.
     * @see #withIconSizeFromHeight(int) for setting height while maintaining aspect ratio.
     */
    public SvgIcon withIconSize( Size size ) {
        return withIconSize(
                    size.width().map(Math::round).orElse(NO_SIZE),
                    size.height().map(Math::round).orElse(NO_SIZE)
                );
    }

    /**
     * Creates a new {@link SvgIcon} with the specified width, calculating the height
     * automatically to maintain the aspect ratio of the original SVG document.
     * <p>
     * This method is particularly useful when you want to scale an SVG icon proportionally
     * while controlling only one dimension. Unlike {@link #withIconWidth(int)}, which can
     * distort the icon by changing only the width independently, this method preserves
     * the original aspect ratio by calculating the appropriate height.
     * </p>
     * <b>Important behaviors and edge cases:</b>
     * <ul>
     *   <li>If {@code newWidth} is negative, returns an icon with both dimensions set to -1
     *       (unknown), effectively resetting to undefined size.</li>
     *   <li>If the current icon has percentage-based dimensions (e.g., width="50%"),
     *       they are first resolved to pixel values using the SVG document's view box
     *       as reference (via {@link #withPercentageSizeResolvedAsPixels()}) before
     *       calculating the aspect ratio.</li>
     *   <li>If the SVG document cannot be loaded or has no view box, the original icon
     *       is returned unchanged.</li>
     *   <li>The resulting icon will always have pixel-based units (PX) for both dimensions,
     *       even if the original used percentages.</li>
     *   <li>If the calculated height would be fractional, it's rounded up to the nearest
     *       integer to prevent clipping.</li>
     *   <li>When called on an icon with fixed pixel dimensions, this method simply
     *       recalculates the height based on the existing aspect ratio.</li>
     * </ul>
     * <p>
     * This method is particularly useful in layout scenarios where you need to
     * constrain an icon by width (e.g., fitting within a fixed-width toolbar)
     * while maintaining its visual proportions.
     * </p>
     *
     * @param newWidth The desired width in developer pixels. Use -1 to reset to unknown size.
     * @return A new {@link SvgIcon} with the specified width and a height calculated
     *         to maintain the original aspect ratio. Returns the original icon if
     *         the SVG document cannot provide aspect ratio information.
     * @see #withIconSizeFromHeight(int) for the counterpart that sets height and calculates width.
     * @see #withIconWidth(int) to set width independently (may distort aspect ratio).
     * @see #withIconSize(int, int) to set both dimensions independently.
     * @see #withPercentageSizeResolvedAsPixels() for understanding how percentage
     *      dimensions are handled before aspect ratio calculation.
     */
    public SvgIcon withIconSizeFromWidth( int newWidth ) {
        if ( newWidth < 0 )
            return this.withIconSize(NO_SIZE, NO_SIZE);

        SvgIcon icon = this.withPercentageSizeResolvedAsPixels();
        Size adjustedSize = icon._sizeWithAspectRatioCorrection(Size.unknown().withWidth(newWidth));
        return icon.withIconSize(adjustedSize);
    }

    /**
     * Creates a new {@link SvgIcon} with the specified height, calculating the width
     * automatically to maintain the aspect ratio of the original SVG document.
     * <p>
     * This method is the counterpart to {@link #withIconSizeFromWidth(int)} and is
     * useful when you need to control the icon's height while preserving its
     * proportional width. Like its width-based counterpart, this method prevents
     * distortion by maintaining the original aspect ratio.
     * </p>
     * <b>Important behaviors and edge cases:</b>
     * <ul>
     *   <li>If {@code newHeight} is negative, returns an icon with both dimensions set to -1
     *       (unknown), effectively resetting to undefined size.</li>
     *   <li>If the current icon has percentage-based dimensions (e.g., height="75%"),
     *       they are first resolved to pixel values using the SVG document's view box
     *       as reference (via {@link #withPercentageSizeResolvedAsPixels()}) before
     *       calculating the aspect ratio.</li>
     *   <li>If the SVG document cannot be loaded or has no view box, the original icon
     *       is returned unchanged.</li>
     *   <li>The resulting icon will always have pixel-based units (PX) for both dimensions,
     *       even if the original used percentages.</li>
     *   <li>If the calculated width would be fractional, it's rounded up to the nearest
     *       integer to prevent clipping.</li>
     *   <li>When called on an icon with fixed pixel dimensions, this method simply
     *       recalculates the width based on the existing aspect ratio.</li>
     * </ul>
     * <p>
     * This method is particularly useful in layout scenarios where you need to
     * constrain an icon by height (e.g., fitting within a fixed-height toolbar)
     * while maintaining its visual proportions.
     * </p>
     *
     * @param newHeight The desired height in developer pixels. Use -1 to reset to unknown size.
     * @return A new {@link SvgIcon} with the specified height and a width calculated
     *         to maintain the original aspect ratio. Returns the original icon if
     *         the SVG document cannot provide aspect ratio information.
     * @see #withIconSizeFromWidth(int) for the counterpart that sets width and calculates height.
     * @see #withIconHeight(int) to set height independently (may distort aspect ratio).
     * @see #withIconSize(int, int) to set both dimensions independently.
     * @see #withPercentageSizeResolvedAsPixels() for understanding how percentage
     *      dimensions are handled before aspect ratio calculation.
     */
    public SvgIcon withIconSizeFromHeight( int newHeight ) {
        if ( newHeight < 0 )
            return this.withIconSize(NO_SIZE, NO_SIZE);

        SvgIcon icon = this.withPercentageSizeResolvedAsPixels();
        Size adjustedSize = icon._sizeWithAspectRatioCorrection(Size.unknown().withHeight(newHeight));
        return icon.withIconSize(adjustedSize);
    }

    /**
     *  Creates a new {@link SvgIcon} where percentage based dimensions
     *  are converted to developer pixel based dimensions using the SVG document's
     *  view box as reference dimensions.
     *  This method is specifically designed for SVG documents with percentage
     *  based dimensions. Icons which do not have at least one percentage based dimension
     *  are returned as is.<br>
     *  <p>
     *  Consider the following example:
     *  <pre>{@code
     *      <svg width="100%" height="50%" viewBox="0 0 24 24">
     *          ...
     *      </svg>
     *  }</pre>
     *  An {@link SvgIcon} with this header will resolve to having
     *  the following dimensions:
     *  <ul>
     *      <li>{@link #getIconWidth()} == 24</li>
     *      <li>{@link #getIconHeight()} == 12</li>
     *  </ul>
     *  <b>Note that if this {@link SvgIcon} is "empty", meaning that
     *  it's {@link #getSvgDocument()} does not return an {@link SVGDocument}
     *  instance, then this method will also return this instance.</b>
     *
     * @return A new {@link SvgIcon} with its percentage based dimensions
     *         converted to being pixel based, <b>using the view box of the
     *         SVG document as reference frame.</b>
     */
    public SvgIcon withPercentageSizeResolvedAsPixels() {
        if ( _core.svgDocument == null || _widthUnit != Unit.PERCENTAGE && _heightUnit != Unit.PERCENTAGE ) {
            return this;
        }
        return new SvgIcon(
                _core,
                _percentageResolvedSize(),
                _widthUnit == Unit.PERCENTAGE ? Unit.PX : _widthUnit,
                _heightUnit == Unit.PERCENTAGE ? Unit.PX : _heightUnit,
                _fitComponent,
                _preferredPlacement
            );
    }

    private Size _percentageResolvedSize() {
        if ( _core.svgDocument == null || _widthUnit != Unit.PERCENTAGE && _heightUnit != Unit.PERCENTAGE ) {
            return _size;
        }
        if ( _widthUnit == Unit.PERCENTAGE && _heightUnit != Unit.PERCENTAGE ) {
            // We resolve the width from the height!
            return _sizeWithAspectRatioCorrection(_size());
        }
        if ( _widthUnit != Unit.PERCENTAGE && _heightUnit == Unit.PERCENTAGE ) {
            // We resolve the width from the height!
            return _sizeWithAspectRatioCorrection(_size());
        }
        float boxWidth = _core.svgDocument.viewBox().width;
        float boxHeight = _core.svgDocument.viewBox().height;
        return Size.of(
                   _size.width().map( w -> w >= 0 && _widthUnit == Unit.PERCENTAGE ? boxWidth * w/100f : w ).orElse(-1f),
                   _size.height().map( h -> h >= 0 && _heightUnit == Unit.PERCENTAGE ? boxHeight * h/100f : h ).orElse(-1f)
                );
    }

    /**
     *  The underlying SVG document contains a size object, which
     *  is the width and height of the root SVG element inside the document.
     *
     * @return The size of the SVG document in the form of a {@link Size}.
     */
    public Size getSvgSize() {
        if ( _core.svgDocument == null )
            return Size.unknown();
        FloatSize svgSize = _core.svgDocument.size();
        return Size.of(svgSize.width, svgSize.height);
    }

    /**
     *  Allows you to access the underlying {@link SVGDocument} that is used to render the icon.
     *
     * @return The underlying {@link SVGDocument} that is used to render the icon.
     */
    public @Nullable SVGDocument getSvgDocument() {
        return _core.svgDocument;
    }

    /**
     *  Allows you to access the {@link UI.FitComponent} policy, which
     *  determines if and how the icon should be fitted
     *  onto a component when rendered through the
     *  {@link #paintIcon(Component, Graphics, int, int)} method.<br>
     *  The following fit modes are available:
     *  <ul>
     *      <li>{@link UI.FitComponent#NO} -
     *      The image will not be scaled to fit the inner component area.
     *      </li>
     *      <li>{@link UI.FitComponent#WIDTH} -
     *      The image will be scaled to fit the inner component width.
     *      </li>
     *      <li>{@link UI.FitComponent#HEIGHT} -
     *      The image will be scaled to fit the inner component height.
     *      </li>
     *      <li>{@link UI.FitComponent#WIDTH_AND_HEIGHT} -
     *      The image will be scaled to fit both the component width and height.
     *      </li>
     *      <li>{@link UI.FitComponent#MAX_DIM} -
     *      The image will be scaled to fit the larger of the two dimensions of the inner component area.
     *      </li>
     *      <li>{@link UI.FitComponent#MIN_DIM} -
     *      The image will be scaled to fit the smaller of the two dimensions of the inner component area.
     *      </li>
     *      <li>{@link UI.FitComponent#UNDEFINED} -
     *      How the image will be scaled to fit the component is unclear.
     *      Another property may override this, but typically the behavior
     *      is similar to {@link UI.FitComponent#NO}.
     *      </li>
     *  </ul>
     *  See {@link #withFitComponent(UI.FitComponent)} if you want to create a new {@link SvgIcon}
     *  with an updated fit policy.
     *
     * @return The {@link UI.FitComponent} that determines if and how the icon should be fitted into a
     *         any given component (see {@link #paintIcon(Component, Graphics, int, int)} ).
     */
    public UI.FitComponent getFitComponent() { return _fitComponent; }

    /**
     *  There are different kinds of strategies to fit an SVG icon onto the component.
     *  These strategies are identified using the {@link UI.FitComponent} enum
     *  which defines the following fit modes:
     *  <ul>
     *      <li>{@link UI.FitComponent#NO} -
     *          The image will not be scaled to fit the inner component area.
     *      </li>
     *      <li>{@link UI.FitComponent#WIDTH} -
     *          The image will be scaled to fit the inner component width.
     *      </li>
     *      <li>{@link UI.FitComponent#HEIGHT} -
     *          The image will be scaled to fit the inner component height.
     *      </li>
     *      <li>{@link UI.FitComponent#WIDTH_AND_HEIGHT} -
     *          The image will be scaled to fit both the component width and height.
     *      </li>
     *      <li>{@link UI.FitComponent#MAX_DIM} -
     *          The image will be scaled to fit the larger
     *          of the two dimension of the inner component area.
     *      </li>
     *      <li>{@link UI.FitComponent#MIN_DIM} -
     *          The image will be scaled to fit the smaller
     *          of the two dimension of the inner component area.
     *      </li>
     *  </ul>
     * @param fit The {@link UI.FitComponent} that determines if and how the icon should be fitted into a
     *            any given component (see {@link #paintIcon(Component, Graphics, int, int)} ).
     * @return A new {@link SvgIcon} with the given {@link UI.FitComponent} policy.
     */
    public SvgIcon withFitComponent( UI.FitComponent fit ) {
        Objects.requireNonNull(fit);
        if ( fit == _fitComponent )
            return this;
        return new SvgIcon(_core, _size, _widthUnit, _heightUnit, fit, _preferredPlacement);
    }

    /**
     *  The preferred placement policy determines where the icon
     *  should be placed within a component when rendered through the
     *  {@link #paintIcon(Component, Graphics, int, int)} method.
     *
     * @return The {@link UI.Placement} that determines where the icon should be placed within a component
     *         (see {@link #paintIcon(Component, Graphics, int, int)}).
     */
    public UI.Placement getPreferredPlacement() { return _preferredPlacement; }

    /**
     *  Allows you to get an updated {@link SvgIcon} with the given {@link UI.Placement} policy
     *  which determines where the icon should be placed within a component
     *  when rendered through the {@link #paintIcon(Component, Graphics, int, int)} method.
     *
     * @param placement The {@link UI.Placement} that determines where the icon should be placed within a component
     *                  (see {@link #paintIcon(Component, Graphics, int, int)}).
     * @return A new {@link SvgIcon} with the given {@link UI.Placement} policy.
     */
    public SvgIcon withPreferredPlacement( UI.Placement placement ) {
        Objects.requireNonNull(placement);
        if ( placement == _preferredPlacement )
            return this;
        return new SvgIcon(_core, _size, _widthUnit, _heightUnit, _fitComponent, placement);
    }

    /**
     *  Creates a new {@link Image} from the SVG document.
     * @return A new {@link Image} where the SVG document has been rendered into.
     */
    @Override
    public Image getImage() {

        if ( _cache != null )
            return _cache;

        int width  = getIconWidth();
        int height = getIconHeight();

        if ( _core.svgDocument != null ) {
            if (width < 0)
                width = (int) UI.scale(_core.svgDocument.size().width);
            if (height < 0)
                height = (int) UI.scale(_core.svgDocument.size().height);
        }

        // We create a new buffered image, render into it, and then return it.
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        if ( _core.svgDocument != null )
            _paintIcon(
                    null,
                    image.createGraphics(),
                    Bounds.of(0, 0, width, height),
                    Offset.none(),
                    UI.Placement.CENTER,
                    UI.FitComponent.WIDTH_AND_HEIGHT,
                    Outline.none()
                );

        return image;
    }

    /**
     *  We don't support this. An SVG document is not really an image, it's a vector graphic.
     *  Extending {@link ImageIcon} is just for compatibility reasons...
     */
    @Override
    public void setImage( Image image ) {
        // We don't support this.
    }

    /**
     * @param c The component to render the icon into.
     * @param g the graphics context
     * @param x the X coordinate of the icon's top-left corner
     * @param y the Y coordinate of the icon's top-left corner
     */
    @Override
    public synchronized void paintIcon( java.awt.@Nullable Component c, java.awt.Graphics g, int x, int y )
    {
        if ( _core.svgDocument == null )
            return;

        final int scaledWidth = getIconWidth();
        final int scaledHeight = getIconHeight();

        UI.Placement preferredPlacement = _preferredPlacement;
        UI.FitComponent fitComponent = _fitComponent;

        // If this SVG has no special layout requirements, we render it exactly like expected in Swing:
        boolean weNeedToRenderLikeTheInvokerWantsTo = preferredPlacement == UI.Placement.UNDEFINED &&
                                                      fitComponent == UI.FitComponent.UNDEFINED;

        if ( fitComponent == UI.FitComponent.UNDEFINED )
            fitComponent = UI.FitComponent.MIN_DIM; // best default!
        if ( preferredPlacement == UI.Placement.UNDEFINED && c instanceof JComponent )
            preferredPlacement = ComponentExtension.from((JComponent) c).preferredIconPlacement();

        Insets insets = ZERO_INSETS;

        if ( c != null ) {
            /*
                If the component exists we want to account for its (border) insets.
                This is to avoid the icon colliding with the component border
                and also to make sure the icon is centered properly.
            */
            insets = Optional.ofNullable( c instanceof JComponent ? ((JComponent)c).getBorder() : null )
                                .map( b -> {
                                    try {
                                        return _determineInsetsForBorder(b, c);
                                    } catch (Exception e) {
                                        return ZERO_INSETS;
                                    }
                                })
                                .orElse(ZERO_INSETS);

            if ( scaledWidth < 0 || !weNeedToRenderLikeTheInvokerWantsTo )
                x = insets.left;

            if ( scaledHeight < 0 || !weNeedToRenderLikeTheInvokerWantsTo )
                y = insets.top;
        }

        int width  = Math.max( scaledWidth,  c == null ? NO_SIZE : c.getWidth()  );
        int height = Math.max( scaledHeight, c == null ? NO_SIZE : c.getHeight() );

        width  = scaledWidth  >= 0 && weNeedToRenderLikeTheInvokerWantsTo ? scaledWidth  : width  - insets.right  - insets.left;
        height = scaledHeight >= 0 && weNeedToRenderLikeTheInvokerWantsTo ? scaledHeight : height - insets.bottom - insets.top ;

        if ( width  <= 0 ) {
            int smaller = (int) Math.floor( width / 2.0 );
            int larger  = (int) Math.ceil(  width / 2.0 );
            x += smaller;
            width = ( larger - smaller );
        }
        if ( height <= 0 ) {
            int smaller = (int) Math.floor( height / 2.0 );
            int larger  = (int) Math.ceil(  height / 2.0 );
            y += smaller;
            height = ( larger - smaller );
        }

        if ( scaledWidth > 0 && scaledHeight > 0 && preferredPlacement == UI.Placement.UNDEFINED ) {
            if ( _cache != null && _cache.getWidth() == width && _cache.getHeight() == height )
                g.drawImage(_cache, x, y, width, height, null);
            else {
                _cache = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                paintIcon(c, _cache.getGraphics(), Bounds.of(0, 0, width, height), Offset.none(), Outline.none() );
                g.drawImage(_cache, x, y, width, height, null);
            }
        }
        else
            _paintIcon( c, g, Bounds.of(x, y, width, height), Offset.of(0, 0), preferredPlacement, fitComponent, Outline.none() );
    }

    @SuppressWarnings("DoNotCall")
    private Insets _determineInsetsForBorder( Border b, Component c )
    {
        return LibraryInternalCrossPackageStyleUtil._onlyBorderInsetsOf(b, c)
                                                    .logProblemsAsError()
                                                    .orElse(ZERO_INSETS);
    }

    void paintIcon(
        final @Nullable Component c,
        final Graphics g,
        final Bounds bounds,
        final Offset offset,
        final Outline padding
    ) {
        Size size = _size();
        UI.FitComponent fitComponent = _fitComponent;
        if ( fitComponent == UI.FitComponent.UNDEFINED && !size.width().isPresent() && !size.height().isPresent() )
            fitComponent = UI.FitComponent.MIN_DIM; // best default!
        _paintIcon( c, g, bounds, offset, _preferredPlacement, fitComponent, padding);
    }

    private Size _computeBaseSizeFrom(int areaWidth, int areaHeight) {
        if ( _core.svgDocument == null )
            return Size.unknown();
        final int iconWidth  = getIconWidth();
        final int iconHeight = getIconHeight();
        final FloatSize svgSize = _core.svgDocument.size();

        float finalWidth  = ( iconWidth  > 0 || areaWidth  < 0 ? iconWidth  : -1 );
        float finalHeight = ( iconHeight > 0 || areaHeight < 0 ? iconHeight : -1 );
        boolean hasPercentageScaling = false;
        if ( finalWidth <= 0 || finalHeight <= 0 ) {
            finalWidth = iconWidth;
            if ( iconWidth < 0 ) {
                if ( _widthUnit == Unit.PERCENTAGE && _size.width().isPresent() ) {
                    finalWidth = areaWidth * _size.width().get() / 100f;
                    hasPercentageScaling = true;
                } else {
                    finalWidth = svgSize.width;
                }
            }
            finalHeight = iconHeight;
            if ( iconHeight < 0 ) {
                if ( _heightUnit == Unit.PERCENTAGE && _size.height().isPresent() ) {
                    finalHeight = areaHeight * _size.height().get() / 100f;
                    hasPercentageScaling = true;
                } else {
                    finalHeight = svgSize.height;
                }
            }
            if ( !hasPercentageScaling ) {
                float scale;
                if (areaWidth < areaHeight) { // <- Tall area
                    if (finalWidth > finalHeight) {
                        scale = areaWidth / finalWidth;
                    } else {
                        scale = areaHeight / finalHeight;
                    }
                } else { // < - Wide area
                    if (finalWidth < finalHeight) {
                        scale = areaWidth / finalWidth;
                    } else {
                        scale = areaHeight / finalHeight;
                    }
                }
                finalWidth = finalWidth * scale;
                finalHeight = finalHeight * scale;
            }
        }
        return Size.of(finalWidth, finalHeight);
    }

    private void _paintIcon(
        final @Nullable Component c,
        final Graphics g,
        final Bounds bounds,
        final Offset offset,
        final UI.Placement preferredPlacement,
        final UI.FitComponent fitComponent,
        final Outline padding
    ) {
        final int areaX = Math.round(bounds.location().x() + offset.x());
        final int areaY = Math.round(bounds.location().y() + offset.y());
        final int areaWidth  = bounds.size().width().map(Math::round).orElse(0);
        final int areaHeight = bounds.size().height().map(Math::round).orElse(0);
                
        if ( _core.svgDocument == null )
            return;

        final Size iconSize = _computeBaseSizeFrom(areaWidth, areaHeight);
        final int iconWidth = iconSize.width().map(Math::round).orElse(0);
        final int iconHeight = iconSize.height().map(Math::round).orElse(0);

        int x = areaX;
        int y = areaY;
        int width  = ( areaWidth  < 0 ? iconWidth  : areaWidth  );
        int height = ( areaHeight < 0 ? iconHeight : areaHeight );

        Graphics2D g2d = (Graphics2D) g.create();

        float scaleX = 1f;
        float scaleY = 1f;

        if ( fitComponent == UI.FitComponent.MIN_DIM || fitComponent == UI.FitComponent.MAX_DIM ) {
            if ( fitComponent == UI.FitComponent.MIN_DIM ) {
                 if (areaWidth < areaHeight) {
                    scaleX = (float) width / iconWidth;
                    scaleY = scaleX;
                 }
                if (areaHeight < areaWidth) {
                    scaleY = (float) height / iconHeight;
                    scaleX = scaleY;
                }
            } else {
                if (areaWidth > areaHeight) {
                    scaleX = (float) width / iconWidth;
                    scaleY = scaleX;
                }
                if (areaHeight > areaWidth) {
                    scaleY = (float) height / iconHeight;
                    scaleX = scaleY;
                }
            }
        }

        if ( fitComponent == UI.FitComponent.WIDTH || fitComponent == UI.FitComponent.WIDTH_AND_HEIGHT ) {
            scaleX = (float) width / iconWidth;
        }

        if ( fitComponent == UI.FitComponent.HEIGHT || fitComponent == UI.FitComponent.WIDTH_AND_HEIGHT ) {
            scaleY = (float) height / iconHeight;
        }

        boolean sizeIsUnknown = false;
        if ( getIconWidth() < 0 && getIconHeight() < 0 && preferredPlacement == UI.Placement.UNDEFINED && fitComponent == UI.FitComponent.UNDEFINED ) {
            sizeIsUnknown = true;
        }
        ViewBox viewBox = new ViewBox(x, y, !sizeIsUnknown ? iconWidth : areaWidth, !sizeIsUnknown ? iconHeight : areaHeight);

        if ( fitComponent == UI.FitComponent.NO || fitComponent == UI.FitComponent.UNDEFINED ) {
            final FloatSize svgSize = _core.svgDocument.size();
            float newWidth   = iconWidth  >= 0 ? iconWidth  : svgSize.width;
            float newHeight  = iconHeight >= 0 ? iconHeight : svgSize.height;
            final FloatSize viewBoxSize = _core.svgDocument.viewBox().size();
            newWidth   = newWidth  >= 0 ? newWidth  : viewBoxSize.width;
            newHeight  = newHeight >= 0 ? newHeight : viewBoxSize.height;
            viewBox = new ViewBox( x, y, newWidth, newHeight );
        }

        // Let's check if the view box exists:
        if ( viewBox.width <= 0 || viewBox.height <= 0 )
            return;

        // Also let's check if the view box has valid values:
        if ( Float.isNaN(viewBox.x) || Float.isNaN(viewBox.y) || Float.isNaN(viewBox.width) || Float.isNaN(viewBox.height) )
            return;

        // We correct if the component area is smaller than the view box:
        width += (int) Math.max(0, ( viewBox.x + viewBox.width ) - ( x + width ) );
        width += (int) Math.max(0, x - viewBox.x );
        height += (int) Math.max(0, ( viewBox.y + viewBox.height ) - ( y + height ) );
        height += (int) Math.max(0, y - viewBox.y );
        x = (int) Math.min(x, viewBox.x);
        y = (int) Math.min(y, viewBox.y);

        {
            viewBox = new ViewBox(viewBox.x, viewBox.y, viewBox.width*scaleX, viewBox.height*scaleY);
            FloatSize svgSize = _core.svgDocument.viewBox().size();
            float svgRefWidth = ((svgSize.width) / (svgSize.height));
            float svgRefHeight = ((svgSize.height) / (svgSize.width));
            float imgRefWidth = (viewBox.width / viewBox.height);
            float imgRefHeight = (viewBox.height / viewBox.width);

            scaleX = Math.max(1f, imgRefWidth / svgRefWidth);
            scaleY = Math.max(1f, imgRefHeight / svgRefHeight);
            viewBox = new ViewBox(viewBox.x / scaleX, viewBox.y / scaleY, viewBox.width / scaleX, viewBox.height / scaleY);
        }
        /*
            Before we do the actual rendering we first check if there
            is a preferred placement that is not the center.
            If that is the case we move the view box accordingly.
        */
        final float scaledAreaX = x / scaleX;
        final float scaledAreaY = y / scaleY;
        final float scaledWidth = width / scaleX;
        final float scaledHeight = height / scaleY;
        final float shiftHalfX = ((scaledWidth - viewBox.width) / 2f);
        final float shiftHalfY = ((scaledHeight - viewBox.height) / 2f);
        switch ( preferredPlacement ) {
            case TOP_LEFT:
                viewBox = new ViewBox( scaledAreaX, scaledAreaY, viewBox.width, viewBox.height );
                break;
            case TOP_RIGHT:
                viewBox = new ViewBox( scaledAreaX + scaledWidth - viewBox.width, scaledAreaY, viewBox.width, viewBox.height );
                break;
            case BOTTOM_LEFT:
                viewBox = new ViewBox( scaledAreaX, scaledAreaY + scaledHeight - viewBox.height, viewBox.width, viewBox.height );
                break;
            case BOTTOM_RIGHT:
                viewBox = new ViewBox( scaledAreaX + scaledWidth - viewBox.width, scaledAreaY + scaledHeight - viewBox.height, viewBox.width, viewBox.height );
                break;
            case TOP:
                viewBox = new ViewBox( scaledAreaX + shiftHalfX, scaledAreaY, viewBox.width, viewBox.height );
                break;
            case BOTTOM:
                viewBox = new ViewBox( scaledAreaX + shiftHalfX, scaledAreaY + scaledHeight - viewBox.height , viewBox.width, viewBox.height );
                break;
            case LEFT:
                viewBox = new ViewBox( scaledAreaX, scaledAreaY + shiftHalfY, viewBox.width, viewBox.height );
                break;
            case RIGHT:
                viewBox = new ViewBox( scaledAreaX + scaledWidth - viewBox.width, scaledAreaY + shiftHalfY, viewBox.width, viewBox.height );
                break;
            case CENTER:
            case UNDEFINED:
                viewBox = new ViewBox( scaledAreaX + shiftHalfX, scaledAreaY + shiftHalfY, viewBox.width, viewBox.height );
                break;
            default:
                log.warn(SwingTree.get().logMarker(), "Unknown preferred placement: {}", preferredPlacement);
        }

        // Finally, the padding:
        if ( !Outline.none().equals(padding) ) {
            viewBox = new ViewBox(
                    viewBox.x + padding.left().orElse(0f),
                    viewBox.y + padding.top().orElse(0f),
                    viewBox.width - (padding.left().orElse(0f) + padding.right().orElse(0f)),
                    viewBox.height - (padding.top().orElse(0f) + padding.bottom().orElse(0f))
            );
        }

        // Now onto the actual rendering:

        boolean doAntiAliasing  = StyleEngine.IS_ANTIALIASING_ENABLED();
        boolean wasAntiAliasing = g2d.getRenderingHint( java.awt.RenderingHints.KEY_ANTIALIASING ) == java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
        if ( doAntiAliasing && !wasAntiAliasing )
            g2d.setRenderingHint( java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON );

        boolean needsScaling = ( scaleX != 1 || scaleY != 1 );
        AffineTransform oldTransform = g2d.getTransform();

        if ( needsScaling ) {
            AffineTransform newTransform = new AffineTransform(oldTransform);
            newTransform.scale(scaleX, scaleY);
            g2d.setTransform(newTransform);
        }

        try {
            // We also have to scale x and y, this is because the SVGDocument does not
            // account for the scale of the transform with respect to the view box!
            _core.svgDocument.render(c, g2d, viewBox);
        } catch (Exception e) {
            log.warn(SwingTree.get().logMarker(), "Failed to render SVG document.", e);
        }

        if ( needsScaling )
            g2d.setTransform(oldTransform); // back to the previous scaling!

        if ( doAntiAliasing && !wasAntiAliasing )
            g2d.setRenderingHint( java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_OFF );
    }

    @Override
    public int hashCode() {
        return Objects.hash(_core.svgDocument, _widthUnit, _heightUnit, _size, _fitComponent, _preferredPlacement);
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        SvgIcon rhs = (SvgIcon) obj;
        return Objects.equals(_size,               rhs._size)        &&
               Objects.equals(_core.svgDocument,   rhs._core.svgDocument)  &&
               Objects.equals(_widthUnit,          rhs._heightUnit)  &&
               Objects.equals(_heightUnit,         rhs._widthUnit)  &&
               Objects.equals(_fitComponent,       rhs._fitComponent) &&
               Objects.equals(_preferredPlacement, rhs._preferredPlacement);
    }

    @Override
    public String toString() {
        int width  = _size.width().map(Math::round).orElse(NO_SIZE);
        int height = _size.height().map(Math::round).orElse(NO_SIZE);
        String typeName           = getClass().getSimpleName();
        String widthAsStr         = _dimeToString(width , _widthUnit);
        String heightAsStr        = _dimeToString(height, _heightUnit);
        String fitComponent       = _fitComponent.toString();
        String preferredPlacement = _preferredPlacement.toString();
        String svgDocument        = Optional.ofNullable(_core.svgDocument)
                                            .map(it -> {
                                                String docClass = it.getClass().getSimpleName();
                                                FloatSize size = it.size();
                                                return docClass + "[width=" + size.width + ", height=" + size.height + "]";
                                            })
                                            .orElse("?");
        return typeName + "[" +
                    "width=" + widthAsStr + ", " +
                    "height=" + heightAsStr + ", " +
                    "fitComponent=" + fitComponent + ", " +
                    "preferredPlacement=" + preferredPlacement + ", " +
                    "doc=" + svgDocument +
                "]";
    }

    private static String _dimeToString(int dim, Unit unit) {
        return dim < 0 ? "?" : (dim +unit.toPublicString());
    }

    private Size _sizeWithAspectRatioCorrection( Size size ) {
        if ( _core.svgDocument == null )
            return size;
        if ( size.hasPositiveWidth() && size.hasPositiveHeight() )
            return size;
        if ( size.equals(Size.unknown()) )
            return size;

        /*
            The client code has only specified one of the dimensions
            and the other dimension is unknown.

            This means they want the icon to have a somewhat fixed size,
            but they want the other dimension to be determined by the
            aspect ratio of the SVG document.

            So we try to calculate the missing dimension here:
        */
        return _aspectRatio().map( aspectRatio -> {
                    // Now the two cases:
                    if ( size.hasPositiveWidth() ) {
                        // The width is known, calculate the height:
                        double width = size.width().map(Number::doubleValue).orElse(1d);
                        return size.withHeight((int) Math.ceil(width / aspectRatio));
                    } else {
                        // The height is known, calculate the width:
                        double height = size.height().map(Number::doubleValue).orElse(1d);
                        return size.withWidth((int) Math.ceil(height * aspectRatio));
                    }
                })
                .orElse(size);
    }

    private Optional<Double> _aspectRatio() {
        if ( _core.svgDocument == null )
            return Optional.empty();
        if ( _widthUnit == Unit.PERCENTAGE && _heightUnit != Unit.PERCENTAGE )
            return Optional.empty(); // The ration cannot be known ahead of time!
        if ( _widthUnit != Unit.PERCENTAGE && _heightUnit == Unit.PERCENTAGE )
            return Optional.empty(); // The ration cannot be known ahead of time!

        double aspectRatio1 = 0;
        double aspectRatio2 = 0;

        ViewBox viewBox = _core.svgDocument.viewBox();
        if ( viewBox.width > 0 && viewBox.height > 0 )
            aspectRatio1 = viewBox.width / viewBox.height;

        if ( _widthUnit == Unit.PERCENTAGE && _heightUnit == Unit.PERCENTAGE ) {
            Size svgSize = _percentageResolvedSize();
            if (svgSize.width().isPresent() && svgSize.height().isPresent())
                aspectRatio2 = svgSize.width().get() / svgSize.height().get();
        } else {
            if ( _core.widthUnit != Unit.PERCENTAGE && _core.heightUnit != Unit.PERCENTAGE ) {
                FloatSize svgSize = _core.svgDocument.size();
                if (svgSize.width > 0 && svgSize.height > 0)
                    aspectRatio2 = svgSize.width / svgSize.height;
            } else {
                // Percentages do not represent the innate ratio! So let's use the viewbox ratio:
                aspectRatio2 = aspectRatio1;
                // In this case, the view box is a better source for the ratio.
            }
        }

        // We prefer the "svgSize" aspect ratio over the "viewBox" aspect ratio:
        double aspectRatio = aspectRatio2 > 0 ? aspectRatio2 : aspectRatio1;

        if ( aspectRatio == 0 )
            return Optional.empty();
        else
            return Optional.of(aspectRatio);
    }

    private enum Unit {
        PX, PERCENTAGE, UNKNOWN;

        String toPublicString() {
            String unitAsStr = "";
            if ( this == Unit.PX )
                unitAsStr = "px";
            if ( this == Unit.PERCENTAGE )
                unitAsStr = "%";
            return unitAsStr;
        }
    }

    private static final class RawSVG { // This should be a record
        final @Nullable SVGDocument svgDocument;
        final Size                  size;
        final Unit                  widthUnit;
        final Unit                  heightUnit;
        private RawSVG(@Nullable SVGDocument svgDocument, Size size, Unit widthUnit, Unit heightUnit) {
            this.svgDocument = svgDocument;
            this.size        = size;
            this.widthUnit   = widthUnit;
            this.heightUnit  = heightUnit;
        }
    }
}

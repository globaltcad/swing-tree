package swingtree.style;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.SVGLoader;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.UI;
import swingtree.layout.Size;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

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
 *   will automatically load and cache all of the icons for you.</b>
 */
public final class SvgIcon extends ImageIcon
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SvgIcon.class);
    private static final UI.FitComponent DEFAULT_FIT_COMPONENT = UI.FitComponent.MIN_DIM;
    private static final UI.Placement    DEFAULT_PLACEMENT     = UI.Placement.UNDEFINED;
    private static final int             NO_SIZE               = -1;
    private static final Insets          ZERO_INSETS           = new Insets(0,0,0,0);


    private final @Nullable SVGDocument _svgDocument;

    private final int _width;
    private final int _height;

    private final UI.FitComponent _fitComponent;
    private final UI.Placement    _preferredPlacement;

    private @Nullable BufferedImage _cache = null;


    private SvgIcon(
        @Nullable SVGDocument svgDocument, // nullable
        int                   width,
        int                   height,
        UI.FitComponent       fitComponent,
        UI.Placement          preferredPlacement
    ) {
        super();
        _svgDocument        = svgDocument;
        _width              = width;
        _height             = height;
        _fitComponent       = Objects.requireNonNull(fitComponent);
        _preferredPlacement = Objects.requireNonNull(preferredPlacement);
    }

    /**
     * @param path The path to the SVG document.
     */
    public SvgIcon( String path ) {
        this(_loadSvgDocument(SvgIcon.class.getResource(path)), NO_SIZE, NO_SIZE, DEFAULT_FIT_COMPONENT, DEFAULT_PLACEMENT);
    }

    /**
     * @param svgUrl The URL to the SVG document.
     */
    public SvgIcon( URL svgUrl ) {
        this(_loadSvgDocument(svgUrl), NO_SIZE, NO_SIZE, DEFAULT_FIT_COMPONENT, DEFAULT_PLACEMENT);
    }

    /**
     * @param stream The input stream supplying the text data of the SVG document.
     */
    public SvgIcon( InputStream stream ) {
        this(_loadSvgDocument(stream), NO_SIZE, NO_SIZE, DEFAULT_FIT_COMPONENT, DEFAULT_PLACEMENT);
    }

    /**
     * @param svgDocument The already loaded SVG document, which will be used to render the icon.
     */
    public SvgIcon( SVGDocument svgDocument ) {
        this(svgDocument, NO_SIZE, NO_SIZE, DEFAULT_FIT_COMPONENT, DEFAULT_PLACEMENT);
    }

    /**
     * @param path The path to the SVG document.
     * @param size The size of the icon in the form of a {@link Dimension}.
     */
    public SvgIcon( String path, Dimension size ) {
        this(_loadSvgDocument(SvgIcon.class.getResource(path)), size.width, size.height, DEFAULT_FIT_COMPONENT, DEFAULT_PLACEMENT);
    }

    /**
     * @param svgUrl The URL to the SVG document.
     * @param size The size of the icon in the form of a {@link Dimension}.
     */
    public SvgIcon( URL svgUrl, Dimension size ) {
        this(_loadSvgDocument(svgUrl), size.width, size.height, DEFAULT_FIT_COMPONENT, DEFAULT_PLACEMENT);
    }

    /**
     * @param stream The input stream supplying the text data of the SVG document.
     * @param size The size of the icon in the form of a {@link Dimension}.
     */
    public SvgIcon( InputStream stream, Dimension size ) {
        this(_loadSvgDocument(stream), size.width, size.height, DEFAULT_FIT_COMPONENT, DEFAULT_PLACEMENT);
    }

    /**
     * @param svgDocument The already loaded SVG document, which will be used to render the icon.
     * @param size The size of the icon in the form of a {@link Dimension}.
     */
    public SvgIcon( SVGDocument svgDocument, Dimension size ) {
        this(svgDocument, size.width, size.height, DEFAULT_FIT_COMPONENT, DEFAULT_PLACEMENT);
    }


    private static @Nullable SVGDocument _loadSvgDocument( URL svgUrl ) {
        SVGDocument tempSVGDocument = null;
        try {
            SVGLoader loader = new SVGLoader();
            tempSVGDocument = loader.load(svgUrl);
        } catch (Exception e) {
            log.error("Failed to load SVG document from URL: " + svgUrl, e);
        }
        return tempSVGDocument;
    }

    private static @Nullable SVGDocument _loadSvgDocument( InputStream stream ) {
        SVGDocument tempSVGDocument = null;
        try {
            SVGLoader loader = new SVGLoader();
            tempSVGDocument = loader.load(stream);
        } catch (Exception e) {
            log.error("Failed to load SVG document from stream: " + stream, e);
        }
        return tempSVGDocument;
    }

    /**
     *  Exposes the width of the icon, or -1 if the icon should be rendered according
     *  to the width of a given component or the width of the SVG document itself.
     *  (...or other policies such as {@link swingtree.UI.FitComponent} and {@link swingtree.UI.Placement}).<br>
     *  <b>
     *      Note that the returned width is dynamically scaled according to
     *      the current {@link swingtree.UI#scale()} value.
     *      This is to ensure that the icon is rendered at the correct size
     *      according to the current DPI settings.
     *      If you want the unscaled width, use {@link #getBaseWidth()}.
     *  </b>
     * @return The width of the icon, or -1 if the icon should be rendered according
     *         to the width of a given component or the width of the SVG document itself.
     */
    @Override
    public int getIconWidth() {
        return UI.scale(_width);
    }

    /**
     *  Creates an updated {@link SvgIcon} with the given width returned by {@link #getIconWidth()}.
     *
     * @param width The width of the icon, or -1 if the icon should be rendered according
     *              to the width of a given component or the width of the SVG document itself.
     * @return A new {@link SvgIcon} with the given width.
     *        If the width is -1, the icon will be rendered according to the width of a given component
     *        or the width of the SVG document itself.
     */
    public SvgIcon withIconWidth( int width ) {
        if ( width == _width )
            return this;
        return new SvgIcon(_svgDocument, width, _height, _fitComponent, _preferredPlacement);
    }

    /**
     *  Exposes the height of the icon, or -1 if the icon should be rendered according
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
     * @return A new {@link SvgIcon} with the given width and height.
     *        If the width or height is -1, the icon will be rendered according to the width or height of a given component
     *        or the width or height of the SVG document itself.
     */
    @Override
    public int getIconHeight() {
        return UI.scale(_height);
    }

    /**
     *  Exposes the fixed width defined for the icon, which is the width
     *  that was set when the icon was created or updated using the
     *  {@link #withIconWidth(int)} method.<br>
     *  <b>
     *      Note that this width is not scaled according to the current {@link swingtree.UI#scale()} value.
     *      If you want a scaled width, that is more suitable for rendering the icon,
     *      use the {@link #getIconWidth()} method.
     *  </b>
     *
     * @return The width of the icon without scaling.
     */
    public int getBaseWidth() {
        return _width;
    }

    /**
     *  Exposes the fixed height defined for the icon, which is the height
     *  that was set when the icon was created or updated using the
     *  {@link #withIconHeight(int)} method.<br>
     *  <b>
     *      Note that this height is not scaled according to the current {@link swingtree.UI#scale()} value.
     *      If you want a scaled height, that is more suitable for rendering the icon,
     *      use the {@link #getIconHeight()} method.
     *  </b>
     *
     * @return The height of the icon without scaling.
     */
    public int getBaseHeight() {
        return _height;
    }

    /**
     *  Creates an updated {@link SvgIcon} with the supplied integer used
     *  as the icon height, which you can retrieve using {@link #getIconHeight()}.
     *  If the height is -1, the icon will be rendered according to the height of a given component
     *  or the height of the SVG document itself.
     *  (...or other policies such as {@link swingtree.UI.FitComponent} and {@link swingtree.UI.Placement}).
     *
     * @param height The height of the icon, or -1 if the icon should be rendered according
     *               to the height of a given component or the height of the SVG document itself.
     * @return A new {@link SvgIcon} with the given height.
     *        If the height is -1, the icon will be rendered according to the height of a given component
     *        or the height of the SVG document itself.
     */
    public SvgIcon withIconHeight( int height ) {
        if ( height == _height )
            return this;
        return new SvgIcon(_svgDocument, _width, height, _fitComponent, _preferredPlacement);
    }

    /**
     *  Creates an updated {@link SvgIcon} with the given width and height.
     *  Dimensions smaller than 0 are considered "undefined".
     *  When the icon is being rendered then these will be determined according to the
     *  aspect ratio of the SVG document, the {@link swingtree.UI.FitComponent} / {@link swingtree.UI.Placement}
     *  policies or the size of the component the SVG is rendered into.
     *
     * @param width The width of the icon, or -1 if the icon should be rendered according
     *              to the width of a given component or the width of the SVG document itself.
     * @param height The height of the icon, or -1 if the icon should be rendered according
     *               to the height of a given component or the height of the SVG document itself.
     * @return A new {@link SvgIcon} with the given width and height.
     *        If the width or height is -1, the icon will be rendered according to the width or height of a given component
     *        or the width or height of the SVG document itself.
     */
    public SvgIcon withIconSize( int width, int height ) {
        width  = width  < 0 ? NO_SIZE : width;
        height = height < 0 ? NO_SIZE : height;
        if ( width == _width && height == _height )
            return this;
        return new SvgIcon(_svgDocument, width, height, _fitComponent, _preferredPlacement);
    }

    /**
     *  Allows you to create an updated {@link SvgIcon} with the given size
     *  in the form of a {@link Size} object containing the width and height.
     *  If the width or height is -1, the icon will be rendered according to the
     *  width or height of a given component, the width or height of the SVG document
     *  and the {@link swingtree.UI.FitComponent} / {@link swingtree.UI.Placement} policies.
     *
     * @param size The size of the icon in the form of a {@link Size}.
     * @return A new {@link SvgIcon} with the given width and height.
     */
    public SvgIcon withIconSize( Size size ) {
        return withIconSize(
                    size.width().map(Number::intValue).orElse(NO_SIZE),
                    size.height().map(Number::intValue).orElse(NO_SIZE)
                );
    }

    /**
     *  Determines the size of the icon (both width and height) using the provided width
     *  and the aspect ratio of the SVG document.
     *  If the width is -1, the icon will lose its fixed width and will
     *  be rendered according to the width of a given component.
     *  <p>
     *  For example, if the SVG document has an aspect ratio of 2:1, and the width is 200,
     *  then the height will be 100.
     *  <p>
     *  Also see {@link #withIconSizeFromHeight(int)}.
     *
     * @param width The width of the icon, or -1 if the icon should be rendered according
     *              to the width of a given component or the width of the SVG document itself.
     * @return A new {@link SvgIcon} with the given width and a logical height that is
     *         determined by the aspect ratio of the SVG document.
     */
    public SvgIcon withIconSizeFromWidth( int width ) {
        if ( width < 0 )
            return this.withIconSize(NO_SIZE, NO_SIZE);

        double ratio = 1d;

        if ( _svgDocument != null )
            ratio = (double) _svgDocument.size().height / (double) _svgDocument.size().width;

        int logicalHeight = (int) Math.ceil( width * ratio );

        if ( width == _width && logicalHeight == _height )
            return this;

        return new SvgIcon(_svgDocument, width, logicalHeight, _fitComponent, _preferredPlacement);
    }

    /**
     *  Determines the size of the icon (both width and height) using the provided height
     *  and the aspect ratio of the SVG document.
     *  If the height is -1, the icon will lose its fixed height and will
     *  be rendered according to the height of a given component.
     *  <p>
     *  For example, if the SVG document has an aspect ratio of 2:1, and the height is 100,
     *  then the width will be 200.
     *  <p>
     *  Also see {@link #withIconSizeFromWidth(int)}.
     *
     * @param height The height of the icon, or -1 if the icon should be rendered according
     *               to the height of a given component or the height of the SVG document itself.
     * @return A new {@link SvgIcon} with the given height and a logical width that is
     *         determined by the aspect ratio of the SVG document.
     */
    public SvgIcon withIconSizeFromHeight( int height ) {
        if ( height < 0 )
            return this.withIconSize(NO_SIZE, NO_SIZE);

        double ratio = 1d;

        if ( _svgDocument != null )
            ratio = (double) _svgDocument.size().width / (double) _svgDocument.size().height;

        int logicalWidth = (int) Math.ceil( height * ratio );

        if ( logicalWidth == _width && height == _height )
            return this;

        return new SvgIcon(_svgDocument, logicalWidth, height, _fitComponent, _preferredPlacement);
    }

    /**
     *  The underlying SVG document contains a size object, which
     *  is the width and height of the root SVG element inside the document.
     *
     * @return The size of the SVG document in the form of a {@link FloatSize},
     *         a subclass of {@link java.awt.geom.Dimension2D}.
     */
    public FloatSize getSvgSize() {
        if ( _svgDocument == null )
            return new FloatSize(0, 0);
        return _svgDocument.size();
    }

    /**
     *  Allows you to access the underlying {@link SVGDocument} that is used to render the icon.
     *
     * @return The underlying {@link SVGDocument} that is used to render the icon.
     */
    public @Nullable SVGDocument getSvgDocument() {
        return _svgDocument;
    }

    /**
     *  Allows you to access the {@link UI.FitComponent} policy, which
     *  determines if and how the icon should be fitted
     *  onto a component when rendered through the
     *  {@link #paintIcon(Component, java.awt.Graphics, int, int, int, int)} method.<br>
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
     *  </ul>
     *  See {@link #withFitComponent(UI.FitComponent)} if you want to create a new {@link SvgIcon}
     *  with an updated fit policy.
     *
     * @return The {@link UI.FitComponent} that determines if and how the icon should be fitted into a
     *         any given component (see {@link #paintIcon(Component, java.awt.Graphics, int, int, int, int)}).
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
     *            any given component (see {@link #paintIcon(Component, java.awt.Graphics, int, int, int, int)}).
     * @return A new {@link SvgIcon} with the given {@link UI.FitComponent} policy.
     */
    public SvgIcon withFitComponent( UI.FitComponent fit ) {
        Objects.requireNonNull(fit);
        if ( fit == _fitComponent )
            return this;
        return new SvgIcon(_svgDocument, _width, _height, fit, _preferredPlacement);
    }

    /**
     *  The preferred placement policy determines where the icon
     *  should be placed within a component when rendered through the
     *  {@link #paintIcon(Component, java.awt.Graphics, int, int, int, int)} method.
     *
     * @return The {@link UI.Placement} that determines where the icon should be placed within a component
     *         (see {@link #paintIcon(Component, java.awt.Graphics, int, int, int, int)}).
     */
    public UI.Placement getPreferredPlacement() { return _preferredPlacement; }

    /**
     *  Allows you to get an updated {@link SvgIcon} with the given {@link UI.Placement} policy
     *  which determines where the icon should be placed within a component
     *  when rendered through the {@link #paintIcon(Component, java.awt.Graphics, int, int, int, int)} method.
     *
     * @param placement The {@link UI.Placement} that determines where the icon should be placed within a component
     *                  (see {@link #paintIcon(Component, java.awt.Graphics, int, int, int, int)}).
     * @return A new {@link SvgIcon} with the given {@link UI.Placement} policy.
     */
    public SvgIcon withPreferredPlacement( UI.Placement placement ) {
        Objects.requireNonNull(placement);
        if ( placement == _preferredPlacement )
            return this;
        return new SvgIcon(_svgDocument, _width, _height, _fitComponent, placement);
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

        if ( _svgDocument != null ) {
            if (width < 0)
                width = (int) UI.scale(_svgDocument.size().width);
            if (height < 0)
                height = (int) UI.scale(_svgDocument.size().height);
        }

        // We create a new buffered image, render into it, and then return it.
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        if ( _svgDocument != null )
            _svgDocument.render(
                    null,
                    image.createGraphics(),
                    new ViewBox(0, 0, width, height)
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
    public synchronized void paintIcon( java.awt.Component c, java.awt.Graphics g, int x, int y )
    {
        if ( _svgDocument == null )
            return;

        int scaledWidth  = getIconWidth();
        int scaledHeight = getIconHeight();

        UI.Placement preferredPlacement = _preferredPlacement;

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

            if ( scaledWidth < 0 )
                x = insets.left;

            if ( scaledHeight < 0 )
                y = insets.top;
        }

        int width  = Math.max( scaledWidth,  c == null ? NO_SIZE : c.getWidth()  );
        int height = Math.max( scaledHeight, c == null ? NO_SIZE : c.getHeight() );

        width  = scaledWidth  >= 0 ? scaledWidth  : width  - insets.right  - insets.left;
        height = scaledHeight >= 0 ? scaledHeight : height - insets.bottom - insets.top ;

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

        if ( scaledWidth > 0 && scaledHeight > 0 ) {
            if ( _cache != null && _cache.getWidth() == width && _cache.getHeight() == height )
                g.drawImage(_cache, x, y, width, height, null);
            else {
                _cache = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                paintIcon(c, _cache.getGraphics(), 0, 0, width, height);
                g.drawImage(_cache, x, y, width, height, null);
            }
        }
        else
            _paintIcon( c, g, x, y, width, height, preferredPlacement );
    }

    private Insets _determineInsetsForBorder( Border b, Component c )
    {
        if ( b == null )
            return ZERO_INSETS;

        if ( b instanceof StyleAndAnimationBorder )
            return ((StyleAndAnimationBorder<?>)b).getFullPaddingInsets();

        // Compound border
        if ( b instanceof javax.swing.border.CompoundBorder ) {
            javax.swing.border.CompoundBorder cb = (javax.swing.border.CompoundBorder) b;
            return cb.getOutsideBorder().getBorderInsets(c);
        }

        try {
            return b.getBorderInsets(c);
        } catch (Exception e) {
            // Ignore
        }
        return ZERO_INSETS;
    }

    void paintIcon(
            final @Nullable Component c,
            final java.awt.Graphics g,
            int x,
            int y,
            int width,
            int height
    ) {
        _paintIcon( c, g, x, y, width, height, _preferredPlacement );
    }

    private void _paintIcon(
        final @Nullable Component c,
        final java.awt.Graphics g,
        int x,
        int y,
        int width,
        int height,
        UI.Placement preferredPlacement
    ) {
        if ( _svgDocument == null )
            return;

        int scaledWidth  = getIconWidth();
        int scaledHeight = getIconHeight();

        width  = ( width  < 0 ? scaledWidth  : width  );
        height = ( height < 0 ? scaledHeight : height );

        Graphics2D g2d = (Graphics2D) g.create();

        FloatSize svgSize = _svgDocument.size();
        float svgRefWidth  = ( svgSize.width  > svgSize.height ? 1f : svgSize.width  / svgSize.height );
        float svgRefHeight = ( svgSize.height > svgSize.width  ? 1f : svgSize.height / svgSize.width  );
        float imgRefWidth  = (     width      >=   height      ? 1f : (float) width  /   height       );
        float imgRefHeight = (     height     >=   width       ? 1f : (float) height /   width        );

        float scaleX = imgRefWidth  / svgRefWidth;
        float scaleY = imgRefHeight / svgRefHeight;

        if ( _fitComponent == UI.FitComponent.MIN_DIM || _fitComponent == UI.FitComponent.MAX_DIM ) {
            if ( width < height )
                scaleX = 1f;
            if ( height < width )
                scaleY = 1f;
        }

        if ( _fitComponent == UI.FitComponent.WIDTH )
            scaleX = 1f;

        if ( _fitComponent == UI.FitComponent.HEIGHT )
            scaleY = 1f;

        ViewBox viewBox = new ViewBox(x, y, width, height);
        float boxX      = viewBox.x  / scaleX;
        float boxY      = viewBox.y  / scaleY;
        float boxWidth  = viewBox.width  / scaleX;
        float boxHeight = viewBox.height / scaleY;
        if ( _fitComponent == UI.FitComponent.MAX_DIM ) {
            // We now want to make sure that the
            if ( boxWidth < boxHeight ) {
                // We find the scale factor of the heights between the two rectangles:
                float scaleHeight = ( viewBox.height / svgSize.height );
                // We now want to scale the view box so that both have the same heights:
                float newWidth  = svgSize.width  * scaleHeight;
                float newHeight = svgSize.height * scaleHeight;
                float newX = viewBox.x + (viewBox.width - newWidth) / 2f;
                float newY = viewBox.y;
                viewBox = new ViewBox(newX, newY, newWidth, newHeight);
            } else {
                // We find the scale factor of the widths between the two rectangles:
                float scaleWidth = ( viewBox.width / svgSize.width );
                // We now want to scale the view box so that both have the same widths:
                float newWidth  = svgSize.width  * scaleWidth;
                float newHeight = svgSize.height * scaleWidth;
                float newX = viewBox.x;
                float newY = viewBox.y + (viewBox.height - newHeight) / 2f;
                viewBox = new ViewBox(newX, newY, newWidth, newHeight);
            }
        }
        else
            viewBox = new ViewBox(boxX, boxY, boxWidth, boxHeight);

        if ( _fitComponent == UI.FitComponent.NO ) {
            width   = scaledWidth  >= 0 ? scaledWidth  : (int) svgSize.width;
            height  = scaledHeight >= 0 ? scaledHeight : (int) svgSize.height;
            viewBox = new ViewBox( x, y, width, height );
        }

        // Let's check if the view box exists:
        if ( viewBox.width <= 0 || viewBox.height <= 0 )
            return;

        // Also let's check if the view box has valid values:
        if ( Float.isNaN(viewBox.x) || Float.isNaN(viewBox.y) || Float.isNaN(viewBox.width) || Float.isNaN(viewBox.height) )
            return;

        // Let's make sure the view box has the correct dimension ratio:
        float viewBoxRatio = _svgDocument.size().width / _svgDocument.size().height;
        float boxRatio     =      viewBox.width        /      viewBox.height;
        if ( boxRatio > viewBoxRatio ) {
            // The view box is too wide, we need to make it narrower:
            float newWidth = viewBox.height * viewBoxRatio;
            viewBox = new ViewBox( viewBox.x + (viewBox.width - newWidth) / 2f, viewBox.y, newWidth, viewBox.height );
        }
        if ( boxRatio < viewBoxRatio ) {
            // The view box is too tall, we need to make it shorter:
            float newHeight = viewBox.width / viewBoxRatio;
            viewBox = new ViewBox( viewBox.x, viewBox.y + (viewBox.height - newHeight) / 2f, viewBox.width, newHeight );
        }

        /*
            Before we do the actual rendering we first check if there
            is a preferred placement that is not the center.
            If that is the case we move the view box accordingly.
         */
        if ( preferredPlacement != UI.Placement.UNDEFINED && preferredPlacement != UI.Placement.CENTER ) {
            // First we correct if the component area is smaller than the view box:
            width += (int) Math.max(0, ( viewBox.x + viewBox.width ) - ( x + width ) );
            width += (int) Math.max(0, x - viewBox.x );
            x = (int) Math.min(x, viewBox.x);
            height += (int) Math.max(0, ( viewBox.y + viewBox.height ) - ( y + height ) );
            height += (int) Math.max(0, y - viewBox.y );
            y = (int) Math.min(y, viewBox.y);

            switch ( preferredPlacement ) {
                case TOP_LEFT:
                    viewBox = new ViewBox( x, y, viewBox.width, viewBox.height );
                    break;
                case TOP_RIGHT:
                    viewBox = new ViewBox( x + width - viewBox.width, y, viewBox.width, viewBox.height );
                    break;
                case BOTTOM_LEFT:
                    viewBox = new ViewBox( x, y + height - viewBox.height, viewBox.width, viewBox.height );
                    break;
                case BOTTOM_RIGHT:
                    viewBox = new ViewBox( x + width - viewBox.width, y + height - viewBox.height, viewBox.width, viewBox.height );
                    break;
                case TOP:
                    viewBox = new ViewBox( x + (width - viewBox.width) / 2f, y, viewBox.width, viewBox.height );
                    break;
                case BOTTOM:
                    viewBox = new ViewBox( x + (width - viewBox.width) / 2f, y + height - viewBox.height, viewBox.width, viewBox.height );
                    break;
                case LEFT:
                    viewBox = new ViewBox( x, y + (height - viewBox.height) / 2f, viewBox.width, viewBox.height );
                    break;
                case RIGHT:
                    viewBox = new ViewBox( x + width - viewBox.width, y + (height - viewBox.height) / 2f, viewBox.width, viewBox.height );
                    break;
                default:
                    log.warn("Unknown preferred placement: " + preferredPlacement);
            }
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
            _svgDocument.render((JComponent) c, g2d, viewBox);
        } catch (Exception e) {
            log.warn("Failed to render SVG document.", e);
        }

        if ( needsScaling )
            g2d.setTransform(oldTransform); // back to the previous scaling!

        if ( doAntiAliasing && !wasAntiAliasing )
            g2d.setRenderingHint( java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_OFF );
    }

    @Override
    public int hashCode() {
        return Objects.hash(_svgDocument, _width, _height, _fitComponent, _preferredPlacement);
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        SvgIcon rhs = (SvgIcon) obj;
        return _width  == rhs._width  &&
               _height == rhs._height &&
               Objects.equals(_svgDocument,        rhs._svgDocument)  &&
               Objects.equals(_fitComponent,       rhs._fitComponent) &&
               Objects.equals(_preferredPlacement, rhs._preferredPlacement);
    }

    @Override
    public String toString() {
        String typeName           = getClass().getSimpleName();
        String width              = _width  < 0 ? "?" : String.valueOf(_width);
        String height             = _height < 0 ? "?" : String.valueOf(_height);
        String fitComponent       = _fitComponent.toString();
        String preferredPlacement = _preferredPlacement.toString();
        String svgDocument        = Optional.ofNullable(_svgDocument)
                                            .map(it -> {
                                                String docClass = it.getClass().getSimpleName();
                                                FloatSize size = it.size();
                                                return docClass + "[width=" + size.width + ", height=" + size.height + "]";
                                            })
                                            .orElse("?");
        return typeName + "[" +
                    "width=" + width + ", " +
                    "height=" + height + ", " +
                    "fitComponent=" + fitComponent + ", " +
                    "preferredPlacement=" + preferredPlacement + ", " +
                    "doc=" + svgDocument +
                "]";
    }
}

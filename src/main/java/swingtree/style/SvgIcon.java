package swingtree.style;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.SVGLoader;
import org.slf4j.Logger;
import swingtree.UI;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

/**
 *   A specialized {@link ImageIcon} subclass that allows you to use SVG based icon images in your UI.
 *   This in essence just a wrapper around the JSVG library, which renders SVG images into Java2D graphics API.
 */
public final class SvgIcon extends ImageIcon
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SvgIcon.class);

    private final SVGDocument _svgDocument;

    private final int _width;
    private final int _height;

    private final UI.FitComponent _fitComponent;


    private SvgIcon( SVGDocument svgDocument, int width, int height, UI.FitComponent fitComponent ) {
        super();
        _svgDocument  = svgDocument;
        _width        = width;
        _height       = height;
        _fitComponent = fitComponent;
    }

    public SvgIcon( SvgIcon icon ) {
        this(icon._svgDocument, icon._width, icon._height, icon._fitComponent);
    }

    public SvgIcon( URL svgUrl, int width, int height, UI.FitComponent fitComponent ) {
        this(_loadSvgDocument(svgUrl), width, height, fitComponent);
    }

    public SvgIcon( URL svgUrl, int width, int height ) {
        this(svgUrl, width, height, UI.FitComponent.MIN_DIM);
    }

    public SvgIcon( String path, int width, int height ) {
        this(SvgIcon.class.getResource(path), width, height);
    }

    public SvgIcon( String path ) { this(path, -1, -1); }

    public SvgIcon( URL svgUrl ) { this(svgUrl, -1, -1); }


    private static SVGDocument _loadSvgDocument( URL svgUrl ) {
        SVGDocument tempSVGDocument = null;
        try {
            SVGLoader loader = new SVGLoader();
            tempSVGDocument = loader.load(svgUrl);
        } catch (Exception e) {
            log.error("Failed to load SVG document from URL: " + svgUrl, e);
        }
        return tempSVGDocument;
    }


    /**
     * @return The width of the icon, or -1 if the icon should be rendered according
     *         to the width of a given component or the width of the SVG document itself.
     */
    @Override
    public int getIconWidth() { return _width; }

    /**
     * @return A new {@link SvgIcon} with the given width.
     *        If the width is -1, the icon will be rendered according to the width of a given component
     *        or the width of the SVG document itself.
     */
    public SvgIcon withIconWidth( int width ) {
        if ( width == _width )
            return this;
        return new SvgIcon(_svgDocument, width, _height, _fitComponent);
    }

    /**
     * @return A new {@link SvgIcon} with the given width and height.
     *        If the width or height is -1, the icon will be rendered according to the width or height of a given component
     *        or the width or height of the SVG document itself.
     */
    @Override
    public int getIconHeight() { return _height; }

    /**
     * @return A new {@link SvgIcon} with the given height.
     *        If the height is -1, the icon will be rendered according to the height of a given component
     *        or the height of the SVG document itself.
     */
    public SvgIcon withIconHeight(int height ) {
        if ( height == _height )
            return this;
        return new SvgIcon(_svgDocument, _width, height, _fitComponent);
    }

    /**
     * @return A new {@link SvgIcon} with the given width and height.
     *        If the width or height is -1, the icon will be rendered according to the width or height of a given component
     *        or the width or height of the SVG document itself.
     */
    public SvgIcon withIconSize( int width, int height ) {
        if ( width == _width && height == _height )
            return this;
        return new SvgIcon(_svgDocument, width, height, _fitComponent);
    }

    /**
     * @return The {@link UI.FitComponent} that determines if and how the icon should be fitted into a
     *         any given component (see {@link #paintIcon(Component, java.awt.Graphics, int, int, int, int)}).
     */
    public UI.FitComponent getFitComponent() { return _fitComponent; }

    /**
     * @return A new {@link SvgIcon} with the given {@link UI.FitComponent} policy.
     */
    public SvgIcon withFitComponent( UI.FitComponent fit ) {
        Objects.requireNonNull(fit);
        if ( fit == _fitComponent )
            return this;
        return new SvgIcon(_svgDocument, _width, _height, fit);
    }

    /**
     *  Creates a new {@link Image} from the SVG document.
     * @return A new {@link Image} where the SVG document has been rendered into.
     */
    @Override
    public Image getImage() {
        // We create a new buffered image, render into it, and then return it.
        BufferedImage image = new BufferedImage(getIconWidth(), getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        if ( _svgDocument != null )
            _svgDocument.render(
                    null,
                    image.createGraphics(),
                    new ViewBox(0, 0, getIconWidth(), getIconHeight())
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
    public void paintIcon( java.awt.Component c, java.awt.Graphics g, int x, int y )
    {
        if ( _svgDocument == null )
            return;

        Insets insets = Optional.ofNullable( c instanceof JComponent ? ((JComponent)c).getBorder() : null )
                                .map( b -> {
                                    try {
                                        return _determineInsetsForBorder(b, c);
                                    } catch (Exception e) {
                                        return new Insets(0,0,0,0);
                                    }
                                })
                                .orElse(new Insets(0,0,0,0));
        x = insets.left;
        y = insets.top;

        int width  = c.getWidth();
        int height = c.getHeight();

        width  = width  - insets.right  - insets.left;
        height = height - insets.bottom - insets.top;

        paintIcon( c, g, x, y, width, height );
    }

    private Insets _determineInsetsForBorder( Border b, Component c )
    {
        if ( b == null )
            return new Insets(0,0,0,0);

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
        return new Insets(0,0,0,0);
    }

    public void paintIcon(
        final java.awt.Component c,
        final java.awt.Graphics g,
        final int x,
        final int y,
        int width,
        int height
    ) {
        if ( _svgDocument == null )
            return;

        width  = ( width  < 0 ? getIconWidth()  : width  );
        height = ( height < 0 ? getIconHeight() : height );

        Graphics2D g2d = (Graphics2D) g.create();

        FloatSize svgSize = _svgDocument.size();
        float svgRefWidth  = ( svgSize.width  > svgSize.height ? 1f : svgSize.width  / svgSize.height );
        float svgRefHeight = ( svgSize.height > svgSize.width  ? 1f : svgSize.height / svgSize.width  );
        float imgRefWidth  = (     width      >=   height      ? 1f :  (float) width /   height       );
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
            width = _width >= 0 ? _width : (int) svgSize.width;
            height = _height >= 0 ? _height : (int) svgSize.height;
            viewBox = new ViewBox(x, y, width, height);
        }

        // Let's check if the view box exists:
        if ( viewBox.width <= 0 || viewBox.height <= 0 )
            return;

        // Also let's check if the view box has valid values:
        if ( Float.isNaN(viewBox.x) || Float.isNaN(viewBox.y) || Float.isNaN(viewBox.width) || Float.isNaN(viewBox.height) )
            return;

        // Now onto the actual rendering:

        boolean doAntiAliasing  = StylePainter.DO_ANTIALIASING();
        boolean wasAntiAliasing = g2d.getRenderingHint( java.awt.RenderingHints.KEY_ANTIALIASING ) == java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
        if ( doAntiAliasing && !wasAntiAliasing )
            g2d.setRenderingHint( java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON );

        AffineTransform oldTransform = g2d.getTransform();
        AffineTransform newTransform = new AffineTransform(oldTransform);
        newTransform.scale(scaleX, scaleY);

        g2d.setTransform(newTransform);

        try {
            // We also have to scale x and y, this is because the SVGDocument does not
            // account for the scale of the transform with respect to the view box!
            _svgDocument.render((JComponent) c, g2d, viewBox);
        } catch (Exception e) {
            log.warn("Failed to render SVG document: " + _svgDocument, e);
        }

        g2d.setTransform(oldTransform);

        if ( doAntiAliasing && !wasAntiAliasing )
            g2d.setRenderingHint( java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_OFF );
    }

    @Override
    public int hashCode() { return Objects.hash(_svgDocument, _width, _height, _fitComponent); }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj.getClass() != getClass() ) return false;
        SvgIcon rhs = (SvgIcon) obj;
        return Objects.equals(_svgDocument,  rhs._svgDocument) &&
               Objects.equals(_width,        rhs._width) &&
               Objects.equals(_height,       rhs._height) &&
               Objects.equals(_fitComponent, rhs._fitComponent);
    }
}

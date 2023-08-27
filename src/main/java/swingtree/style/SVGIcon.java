package swingtree.style;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.SVGLoader;
import swingtree.UI;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Optional;

/**
 *   A specialized {@link ImageIcon} subclass that allows you to use SVG based icon images in your UI.
 *   This in essence just a wrapper around the JSVG library, which renders SVG images into Java2D graphics API.
 */
public class SVGIcon extends ImageIcon
{
    /**
     *  This enum is used to specify how the SVG image should be scaled to fit the
     *  dimensions of the component that it is being rendered into
     *  using the {@link SVGIcon#paintIcon(java.awt.Component, java.awt.Graphics, int, int, int, int)} method.
     */
    public enum FitComponent {
        WIDTH,
        HEIGHT,
        WIDTH_AND_HEIGHT,
        MAX_DIM,
        MIN_DIM
    }

    private final SVGDocument svgDocument;

    private int _width;
    private int _height;

    private FitComponent _fitComponent;


    public SVGIcon( URL svgUrl, int width, int height, FitComponent fitComponent ) {
        super();
        _width = width;
        _height = height;
        _fitComponent = fitComponent;

        SVGDocument tempSVGDocument = null;
        try {
            SVGLoader loader = new SVGLoader();
            tempSVGDocument = loader.load(svgUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.svgDocument = tempSVGDocument;
    }

    public SVGIcon( URL svgUrl, int width, int height ) {
        this(svgUrl, width, height, FitComponent.MIN_DIM);
    }

    public SVGIcon( String path, int width, int height ) {
        this(SVGIcon.class.getResource(path), width, height);
    }

    public SVGIcon( String path ) { this(path, -1, -1); }

    public SVGIcon( URL svgUrl ) { this(svgUrl, -1, -1); }


    @Override
    public int getIconWidth() { return _width; }

    public void setIconWidth( int width ) { _width = width; }

    @Override
    public int getIconHeight() { return _height; }

    public void setIconHeight( int height ) { _height = height; }

    public void setFitComponent( FitComponent fit ) { _fitComponent = fit; }

    public FitComponent getFitComponent() { return _fitComponent; }

    @Override
    public Image getImage() {
        // We create a new buffered image, render into it, and then return it.
        BufferedImage image = new BufferedImage(getIconWidth(), getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        if ( svgDocument != null )
            svgDocument.render(
                    null,
                    image.createGraphics(),
                    new ViewBox(0, 0, getIconWidth(), getIconHeight())
                );

        return image;
    }

    @Override
    public void setImage( Image image ) {
        // We don't support this.
    }

    @Override
    public void paintIcon( java.awt.Component c, java.awt.Graphics g, int x, int y )
    {
        if ( svgDocument == null )
            return;

        Insets insets = Optional.ofNullable( c instanceof JComponent ? ((JComponent)c).getBorder() : null )
                                .map( b -> b.getBorderInsets(c) )
                                .orElse(new Insets(0,0,0,0));
        x = insets.left;
        y = insets.top;

        int width  = _width;
        int height = _height;

        width  = width  < 0 ? c.getWidth()  : width;
        height = height < 0 ? c.getHeight() : height;

        width  = width  - insets.right  - insets.left;
        height = height - insets.bottom - insets.top;

        paintIcon( c, g, x, y, width, height );
    }

    public void paintIcon( java.awt.Component c, java.awt.Graphics g, int x, int y, int width, int height )
    {
        Graphics2D g2d = (Graphics2D) g.create();
        boolean doAntiAliasing  = UI.scale() < 1.5;
        boolean wasAntiAliasing = g2d.getRenderingHint( java.awt.RenderingHints.KEY_ANTIALIASING ) == java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
        if (doAntiAliasing && !wasAntiAliasing) {
            g2d.setRenderingHint( java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON );
        }

        FloatSize svgSize = svgDocument.size();
        float svgRefWidth  = ( svgSize.width  > svgSize.height ? 1f : svgSize.width  / svgSize.height );
        float svgRefHeight = ( svgSize.height > svgSize.width  ? 1f : svgSize.height / svgSize.width  );
        float imgRefWidth  = (     width      >    height      ? 1f :  (float) width /   height       );
        float imgRefHeight = (     height     >    width       ? 1f : (float) height /   width        );

        float scaleX = imgRefWidth  / svgRefWidth;
        float scaleY = imgRefHeight / svgRefHeight;

        if ( _fitComponent == FitComponent.MIN_DIM || _fitComponent == FitComponent.MAX_DIM ) {
            if ( width < height )
                scaleX = 1f;
            if ( height < width )
                scaleY = 1f;
        }

        if ( _fitComponent == FitComponent.WIDTH )
            scaleX = 1f;

        if ( _fitComponent == FitComponent.HEIGHT )
            scaleY = 1f;

        ViewBox viewBox = new ViewBox(x, y, width, height);
        float boxX      = viewBox.x  / scaleX;
        float boxY      = viewBox.y  / scaleY;
        float boxWidth  = viewBox.width  / scaleX;
        float boxHeight = viewBox.height / scaleY;
        if ( _fitComponent == FitComponent.MAX_DIM ) {
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

        AffineTransform oldTransform = g2d.getTransform();
        AffineTransform newTransform = new AffineTransform(oldTransform);
        newTransform.scale(scaleX, scaleY);

        g2d.setTransform(newTransform);

        // We also have to scale x and y, this is because the SVGDocument does not
        // account for the scale of the transform with respect to the view box!
        svgDocument.render( (JComponent) c, g2d, viewBox );

        g2d.setTransform(oldTransform);

        if (doAntiAliasing && !wasAntiAliasing) {
            g2d.setRenderingHint( java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_OFF );
        }
    }

}

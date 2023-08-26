package swingtree.style;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.parser.SVGLoader;
import swingtree.UI;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 *
 */
public class SVGIcon extends ImageIcon
{
    private final SVGDocument svgDocument;

    private int width = -1;
    private int height = -1;

    public SVGIcon( URL svgUrl, int width, int height ) {
        super();
        this.width = width;
        this.height = height;
        SVGDocument tempSVGDocument = null;
        try {
            SVGLoader loader = new SVGLoader();
            tempSVGDocument = loader.load(svgUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.svgDocument = tempSVGDocument;
    }

    public SVGIcon( String path, int width, int height ) {
        this(SVGIcon.class.getResource(path), width, height);
    }

    public SVGIcon( String path ) {
        this(path, -1, -1);
    }

    public SVGIcon( URL svgUrl ) {
        this(svgUrl, -1, -1);
    }

    @Override
    public void paintIcon( java.awt.Component c, java.awt.Graphics g, int x, int y )
    {
        if (svgDocument == null)
            return;

        int width = getIconWidth();
        int height = getIconHeight();
        width = width < 0 ? c.getWidth() : width;
        height = height < 0 ? c.getHeight() : height;
        Graphics2D g2d = (Graphics2D) g.create();
        boolean doAntiAliasing = UI.scale() < 1.5;
        boolean wasAntiAliasing = g2d.getRenderingHint( java.awt.RenderingHints.KEY_ANTIALIASING ) == java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
        if (doAntiAliasing && !wasAntiAliasing) {
            g2d.setRenderingHint( java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON );
        }
        svgDocument.render(
                (JComponent) c,
                g2d,
                new ViewBox(0, 0, width, height)
            );
        if (doAntiAliasing && !wasAntiAliasing) {
            g2d.setRenderingHint( java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_OFF );
        }
    }

    @Override
    public int getIconWidth() { return width; }

    @Override
    public int getIconHeight() { return height; }

    @Override
    public Image getImage() {
        // We create a new buffered image we can render into and return it.
        BufferedImage image = new BufferedImage(getIconWidth(), getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        if (svgDocument != null)
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

}

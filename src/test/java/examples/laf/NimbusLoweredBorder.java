package examples.laf;

import javax.swing.border.Border;
import javax.swing.plaf.UIResource;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

/**
 *  The border Nimbus puts inside a {@link javax.swing.border.TitledBorder}: a rounded dip pressed
 *  into whatever it surrounds, shaded along its inside by the light from above. It is not a style
 *  but a {@link Border}, because a titled border is something an application sets on a panel, and
 *  {@code UIManager}'s {@code TitledBorder.border} is how a look and feel is asked for one.
 *  <p>
 *  Nimbus renders the dip once into a thirty pixel image with a blurred inner shadow and stretches
 *  it. This draws the same shadow as a stack of rings instead, darkest at the rim, each ring the
 *  rounded outline moved one pixel further in and one pixel down, which is where the light puts the
 *  shadow. The darkness of each ring was read off Nimbus's own rendering.
 */
final class NimbusLoweredBorder implements Border, UIResource
{
    /** How strongly the shadow colour is laid over the ground, ring by ring from the rim inwards. */
    private static final float[] SHADE = { 0.593f, 0.398f, 0.221f, 0.106f, 0.035f, 0.018f, 0.009f };

    private static final float ARC = 13;

    @Override
    public void paintBorder( Component c, Graphics g, int x, int y, int width, int height ) {
        Color ground = c.getBackground();
        Color shadow = new Color(( int ) ( ground.getRed() / 2.1f ), ( int ) ( ground.getGreen() / 2.1f ), ( int ) ( ground.getBlue() / 2.1f ));
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float left = x + 2, top = y, w = width - 4, h = height - 4;
            Area dip = new Area(new RoundRectangle2D.Float(left, top, w, h, ARC, ARC));
            Area outside = new Area(dip);
            for ( int ring = 0; ring < SHADE.length; ring++ ) {
                Area inside = new Area(new RoundRectangle2D.Float(
                        left + ring, top + ring + 1, w - 2 * ring, h - 2 * ring, Math.max(0, ARC - 2 * ring), Math.max(0, ARC - 2 * ring)));
                inside.intersect(dip);
                Area band = new Area(outside);
                band.subtract(inside);
                g2.setColor(new Color(shadow.getRed(), shadow.getGreen(), shadow.getBlue(), Math.round(255 * SHADE[ring])));
                g2.fill(band);
                outside = inside;
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public Insets getBorderInsets( Component c ) {
        return new Insets(10, 10, 10, 10);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}

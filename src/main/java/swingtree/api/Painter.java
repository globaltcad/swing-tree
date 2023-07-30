package swingtree.api;


import java.awt.*;

@FunctionalInterface
public interface Painter
{
    static Painter none() { return Constants.PAINTER_NONE; }

    /**
     * Paints a custom style on a component using the given graphics context.
     * @param g2d the graphics context to use for painting.
     */
    void paint( Graphics2D g2d );

    /**
     * Returns a new painter that paints this painter's style and then the given painter's style.
     * @param after the painter to paint after this painter.
     * @return a new painter that paints this painter's style and then the given painter's style.
     */
    default Painter andThen(Painter after) {
        return g2d -> {
                    try {
                        paint(g2d);
                    } catch ( Exception e ) {
                        e.printStackTrace();
                        // Exceptions inside a painter should not be fatal.
                    }
                    try {
                        after.paint(g2d);
                    } catch ( Exception e ) {
                        e.printStackTrace();
                        // Exceptions inside a painter should not cripple the rest of the painting.
                    }
                };
    }
}

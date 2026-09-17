package examples.laf;

import swingtree.UI;
import swingtree.api.Painter;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.Objects;

/**
 *  The focus ring or the lip of a Nimbus control: whichever of the two it wears in the two pixels
 *  around its edge, painted for {@link Styles.Nimbus} by the style engine's background layer.
 *  <p>
 *  Nimbus fills the whole ring first and the control's edge over it, so the ring shows only where the
 *  edge does not cover it. The style engine paints a shadow after its gradients instead, and lets it
 *  reach one pixel into the body so that no seam shows, which here would paint the ring over the
 *  outline of the control. So a ring is its outer shape with the control's body cut out of it. Both
 *  shapes are written as distances in from the component's bounds, the way Nimbus's painters place
 *  them, so one class serves a rounded button and a square text field.
 *  <p>
 *  It is a value rather than a lambda, because the style engine compares the painters of two styles
 *  to decide whether anything changed.
 */
final class NimbusRing implements Painter
{
    private final JComponent _component;
    private final Color      _color;
    /** left, top, right, bottom and arc of the outer shape, then the same for the body cut out of it */
    private final float[]    _geometry;

    private NimbusRing( JComponent component, Color color, float[] geometry ) {
        _component = component;
        _color     = color;
        _geometry  = geometry;
    }

    /**
     *  Nimbus's focus ring around a raised control: from six tenths of a pixel inside the bounds,
     *  two arc pixels rounder than the edge it surrounds.
     *
     * @param component the control
     * @param color the focus colour
     * @param edgeArc how round the control's edge is
     * @return the ring
     */
    static NimbusRing aroundEdge( JComponent component, Color color, float edgeArc ) {
        return new NimbusRing(component, color, new float[]{ 0.6f, 0.6f, 0.6f, 0.6f, edgeArc + 2, 2, 2, 2, 2, edgeArc });
    }

    /**
     *  The lip under a raised control: its edge, one pixel lower and three arc pixels rounder, which
     *  leaves a single row showing below it.
     *
     * @param component the control
     * @param color the colour of the lip, usually translucent
     * @param edgeArc how round the control's edge is
     * @return the lip
     */
    static NimbusRing underEdge( JComponent component, Color color, float edgeArc ) {
        return new NimbusRing(component, color, new float[]{ 2, 3, 2, 1, edgeArc + 3, 2, 2, 2, 2, edgeArc });
    }

    /**
     *  Any ring at all, for a control whose box is not inset by two pixels on every side - the text
     *  field inside a spinner, which runs right up to the spinner's buttons.
     *
     * @param component the control
     * @param color the colour to fill with
     * @param outer the left, top, right and bottom distance in from the bounds of the outer shape, and its arc
     * @param body the same for the body cut out of it
     * @return the ring
     */
    static NimbusRing of( JComponent component, Color color, float[] outer, float[] body ) {
        float[] geometry = new float[10];
        System.arraycopy(outer, 0, geometry, 0, 5);
        System.arraycopy(body, 0, geometry, 5, 5);
        return new NimbusRing(component, color, geometry);
    }

    @Override
    public void paint( Graphics2D g ) {
        if ( _color.getAlpha() == 0 )
            return;
        float scale = UI.scale();
        float w     = _component.getWidth()  / scale;
        float h     = _component.getHeight() / scale;
        // Filled even-odd, the two shapes leave what lies in one and not the other. Where the outer
        // shape is the body moved down, that is a sliver below the body and another above it inside
        // the body, and the one above is cut off by keeping to the lower half.
        Path2D.Float ring = new Path2D.Float(Path2D.WIND_EVEN_ODD);
        ring.append(shape(w, h, 0), false);
        ring.append(shape(w, h, 5), false);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            if ( _geometry[1] > _geometry[6] )
                g2.clip(new Rectangle2D.Float(0, h / 2f, w, h / 2f));
            LafUtilities.antialiasShapes(g2);
            g2.setColor(_color);
            g2.fill(ring);
        } finally {
            g2.dispose();
        }
    }

    /** @param at where in the geometry the shape's numbers start: 0 for the outer shape, 5 for the body */
    private RoundRectangle2D.Float shape( float w, float h, int at ) {
        float[] q = _geometry;
        return new RoundRectangle2D.Float(q[at], q[at + 1], w - q[at] - q[at + 2], h - q[at + 1] - q[at + 3], q[at + 4], q[at + 4]);
    }

    @Override
    public boolean equals( Object other ) {
        if ( this == other ) return true;
        if ( !(other instanceof NimbusRing) ) return false;
        NimbusRing that = (NimbusRing) other;
        return _component == that._component && _color.equals(that._color) && Arrays.equals(_geometry, that._geometry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(System.identityHashCode(_component), _color, Arrays.hashCode(_geometry));
    }

    @Override
    public String toString() {
        return "NimbusRing[color=" + _color + ", geometry=" + Arrays.toString(_geometry) + "]";
    }
}

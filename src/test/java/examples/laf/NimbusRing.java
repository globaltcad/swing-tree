package examples.laf;

import swingtree.api.Painter;
import swingtree.layout.Bounds;
import swingtree.style.ComponentStyleDelegate;

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
 *  outline of the control. So a ring is its outer shape with the control's body cut out of it.
 *  <p>
 *  Both shapes are placed against the control's box - the box the style engine leaves inside the
 *  margin, which is what the gradients fill - rather than against the component's bounds: the shape
 *  cut out is that box, and the outer shape is that box grown by a distance per side. So a margin
 *  an application adds carries the ring along with the rest of the control instead of leaving it
 *  behind at the component's edge, and one class serves a rounded button and a square text field,
 *  because a distance per side and two arcs are all that differ between them.
 *  <p>
 *  A ring is a value and not a lambda over its component: {@link LafUtilities#marginBoxOf} reads
 *  the box once while the style is built and the four numbers are kept, so two rings that are
 *  {@link #equals(Object)} paint the same pixels whatever component they were built for. That is
 *  what the engine needs to compare the painters of two styles, and what lets it cache the layer
 *  this is painted on rather than repaint it per frame (see {@link #canBeCached()}).
 */
final class NimbusRing implements Painter
{
    private final Color   _color;
    /** The control's box, in "developer pixel" from the component's top left corner. */
    private final Bounds  _box;
    /** How far the outer shape reaches out past that box at the left, top, right and bottom. */
    private final float[] _outsets;
    private final float   _outerArc;
    private final float   _boxArc;

    private NimbusRing( Color color, Bounds box, float[] outsets, float outerArc, float boxArc ) {
        _color    = color;
        _box      = box;
        _outsets  = outsets;
        _outerArc = outerArc;
        _boxArc   = boxArc;
    }

    /**
     *  Nimbus's focus ring around a raised control: one and two fifths of a pixel out from the
     *  control's edge on every side, two arc pixels rounder than that edge.
     *
     * @param it the style being built, which is asked for the control's box
     * @param color the focus colour
     * @param edgeArc how round the control's edge is
     * @return the ring
     */
    static NimbusRing aroundEdge( ComponentStyleDelegate<?> it, Color color, float edgeArc ) {
        return of(it, color, new float[]{ 1.4f, 1.4f, 1.4f, 1.4f }, edgeArc + 2, edgeArc);
    }

    /**
     *  The lip under a raised control: its edge, one pixel lower and three arc pixels rounder, which
     *  leaves a single row showing below it.
     *
     * @param it the style being built, which is asked for the control's box
     * @param color the colour of the lip, usually translucent
     * @param edgeArc how round the control's edge is
     * @return the lip
     */
    static NimbusRing underEdge( ComponentStyleDelegate<?> it, Color color, float edgeArc ) {
        return of(it, color, new float[]{ 0, -1, 0, 1 }, edgeArc + 3, edgeArc);
    }

    /**
     *  Any ring at all, for one that does not stand the same distance out from the control on every
     *  side - the text field inside a spinner, which runs right up to the spinner's buttons.
     *
     * @param it the style being built, which is asked for the control's box
     * @param color the colour to fill with
     * @param outsets how far the outer shape reaches out past the control's box at the left, top,
     *                right and bottom; a negative distance starts it inside the box
     * @param outerArc how round the outer shape is
     * @param boxArc how round the control's box is, which is cut out of it
     * @return the ring
     */
    static NimbusRing of( ComponentStyleDelegate<?> it, Color color, float[] outsets, float outerArc, float boxArc ) {
        return new NimbusRing(color, LafUtilities.marginBoxOf(it), outsets.clone(), outerArc, boxArc);
    }

    @Override
    public void paint( Graphics2D g ) {
        if ( _color.getAlpha() == 0 )
            return;
        RoundRectangle2D.Float outer = _outerShape();
        // Filled even-odd, the two shapes leave what lies in one and not the other. Where the outer
        // shape is the box moved down, that is a sliver below the box and another above it inside
        // the box, and the one above is cut off by keeping to the lower half.
        Path2D.Float ring = new Path2D.Float(Path2D.WIND_EVEN_ODD);
        ring.append(outer, false);
        ring.append(_boxShape(), false);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            if ( _outsets[1] < 0 )
                g2.clip(_lowerHalfOf(outer));
            LafUtilities.antialiasShapes(g2);
            g2.setColor(_color);
            g2.fill(ring);
        } finally {
            g2.dispose();
        }
    }

    /**
     *  The box a ring stands around is one of its own numbers rather than something it reads off a
     *  component while painting, so two rings that are equal paint the same pixels and the engine
     *  may keep the layer they are painted on as an image instead of running them every frame.
     */
    @Override
    public boolean canBeCached() {
        return true;
    }

    private RoundRectangle2D.Float _outerShape() {
        return new RoundRectangle2D.Float(
                    _box.location().x() - _outsets[0],
                    _box.location().y() - _outsets[1],
                    _box.size().widthOrElse(0f)  + _outsets[0] + _outsets[2],
                    _box.size().heightOrElse(0f) + _outsets[1] + _outsets[3],
                    _outerArc, _outerArc
                );
    }

    private RoundRectangle2D.Float _boxShape() {
        return new RoundRectangle2D.Float(
                    _box.location().x(),            _box.location().y(),
                    _box.size().widthOrElse(0f),    _box.size().heightOrElse(0f),
                    _boxArc, _boxArc
                );
    }

    /**
     *  Everything from halfway down the control's box to below the given outer shape. The cut is
     *  the horizontal one alone: the rectangle is a pixel wider and a pixel deeper than the shape
     *  on the three sides it keeps, so that it never trims the antialiased edge of what it keeps.
     */
    private Rectangle2D.Float _lowerHalfOf( RoundRectangle2D.Float outer ) {
        float middle = _box.location().y() + _box.size().heightOrElse(0f) / 2f;
        return new Rectangle2D.Float(outer.x - 1, middle, outer.width + 2, outer.y + outer.height + 1 - middle);
    }

    @Override
    public boolean equals( Object other ) {
        if ( this == other ) return true;
        if ( !(other instanceof NimbusRing) ) return false;
        NimbusRing that = (NimbusRing) other;
        return _color.equals(that._color)
            && _box.equals(that._box)
            && Arrays.equals(_outsets, that._outsets)
            && _outerArc == that._outerArc
            && _boxArc   == that._boxArc;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_color, _box, Arrays.hashCode(_outsets), _outerArc, _boxArc);
    }

    @Override
    public String toString() {
        return "NimbusRing[color=" + _color + ", box=" + _box + ", outsets=" + Arrays.toString(_outsets) +
               ", outerArc=" + _outerArc + ", boxArc=" + _boxArc + "]";
    }
}

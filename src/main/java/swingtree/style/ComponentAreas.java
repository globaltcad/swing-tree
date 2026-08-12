package swingtree.style;

import sprouts.Pair;
import swingtree.UI;
import swingtree.layout.Size;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

/**
 *  A wrapper object for transient reference based caching of the various areas of a component
 *  based on the immutable {@link BoxModelConf} object as cache key.
 *  This is used to avoid recalculating the areas of a component over and over again
 *  if they don't change, which can be a little expensive, especially if the component
 *  has round corners. Note that this may also be used by different components in case
 *  of them having equal box model configurations...
 */
final class ComponentAreas
{
    private static final Map<Pooled<BoxModelConf>, ComponentAreas> _CACHE = new WeakHashMap<>();

    private final BoxModelConf     _boxModel;
    private final LazyRef<Shape>   _borderArea;
    private final LazyRef<Shape>   _interiorArea;
    private final LazyRef<Shape>   _exteriorArea;
    private final LazyRef<Shape>   _bodyArea;
    private final LazyRef<Shape>   _contentArea;
    private final LazyRef<Area[]>  _borderEdgeAreas;
    private final LazyRef<Area[]>  _clippedBorderEdgeAreas;

    static ComponentAreas of( Pooled<BoxModelConf> state ) {
        return _CACHE.computeIfAbsent(state, conf -> new ComponentAreas(state.get()));
    }


    private ComponentAreas(BoxModelConf conf) {
        _boxModel        = conf;
        _bodyArea        = new LazyRef<>(_boxModel, ComponentAreas::_produceBodyArea);
        _interiorArea    = new LazyRef<>(_boxModel, ComponentAreas::_produceInteriorArea);
        _borderArea      = new LazyRef<>(Pair.of(_interiorArea, _bodyArea), s->ComponentAreas._produceBorderArea(s.first(), s.second()));
        _exteriorArea    = new LazyRef<>(Pair.of(_boxModel, _bodyArea), s->ComponentAreas._produceExteriorArea(s.first(),s.second()));
        _contentArea     = new LazyRef<>(Pair.of(_boxModel, _interiorArea), s->ComponentAreas._produceContentArea(s.first(), s.second()));
        _borderEdgeAreas = new LazyRef<>(_boxModel, ComponentAreas::calculateEdgeBorderAreas);
        _clippedBorderEdgeAreas = new LazyRef<>(Pair.of(_borderArea, _borderEdgeAreas), s->ComponentAreas._produceClippedBorderEdgeAreas(s.first(), s.second()));
    }

    public Shape get( UI.ComponentArea areaType ) {
        BoxModelConf boxModel = _boxModel;
        switch ( areaType ) {
            case BODY:
                return _bodyArea.get(); // all - exterior == interior + border
            case INTERIOR:
                return _interiorArea.get(); // all - exterior - border == content - border
            case BORDER:
                return _borderArea.get(); // all - exterior - interior
            case EXTERIOR:
                return _exteriorArea.get(); // all - border - interior
            case ALL:
            default:
                return new Rectangle(
                        0, 0,
                        boxModel.size().width().map(Float::intValue).orElse(0),
                        boxModel.size().height().map(Float::intValue).orElse(0)
                    );
        }
    }

    /**
     * Returns the intersection between the interior area and the rectangular content area.
     * @return The intersection between the interior area and the rectangular content area.
     *         The rectangular content area is essentially all insets from the box model applied to the component's bounds,
     *         without taking into account any border radius.
     */
    public Shape getContentArea() {
        return _contentArea.get();
    }

    private static Area _produceContentArea( BoxModelConf boxModel, LazyRef<Shape> interiorArea ) {
        Outline insets = boxModel.insetsFor(UI.ComponentBoundary.INTERIOR_TO_CONTENT);
        Size size = boxModel.size();
        Area contentArea = new Area(new Rectangle2D.Float(
                insets.left().orElse(0f),
                insets.top().orElse(0f),
                size.widthOrElse(0f) - insets.left().orElse(0f) - insets.right().orElse(0f),
                size.heightOrElse(0f) - insets.top().orElse(0f) - insets.bottom().orElse(0f)
        ));
        contentArea.intersect(new Area(interiorArea.get()));
        return contentArea;
    }

    public Area[] getEdgeAreas() {
        return _borderEdgeAreas.get();
    }

    /**
     *  Returns the four border-edge areas already clipped to the border area, i.e. the actual shapes
     *  filled for a per-edge (non-homogeneous) border, in the order top, right, bottom, left. These
     *  are the intersection of the {@link UI.ComponentArea#BORDER} area with each of the
     *  {@link #getEdgeAreas() edge regions}. Because both inputs are a pure function of the immutable
     *  {@link BoxModelConf}, the (expensive) {@link Area#intersect(Area) intersections} are computed
     *  once and then reused on every subsequent paint of any component with an equal box model,
     *  instead of being recomputed on every repaint.
     *
     * @return An array of 4 {@link Area} objects: the border area clipped to the top, right, bottom
     *         and left edge regions respectively. The array and its areas are shared/cached — callers
     *         must only read from them (e.g. {@link Graphics2D#fill(Shape)}), never mutate them.
     */
    public Area[] getClippedEdgeAreas() {
        return _clippedBorderEdgeAreas.get();
    }

    private static Area[] _produceClippedBorderEdgeAreas( LazyRef<Shape> borderAreaRef, LazyRef<Area[]> edgeAreasRef ) {
        Shape  borderArea = borderAreaRef.get();
        Area[] edgeAreas  = edgeAreasRef.get();
        Area[] clipped    = new Area[4];
        for ( int i = 0; i < 4; i++ ) {
            Area edgeBorder = new Area(borderArea);
            edgeBorder.intersect(edgeAreas[i]);
            clipped[i] = edgeBorder;
        }
        return clipped;
    }

    public boolean areaExists(UI.ComponentArea area) {
        switch ( area ) {
            case BODY:
                return _bodyArea.exists();
            case INTERIOR:
                return _interiorArea.exists();
            case BORDER:
                return _borderArea.exists();
            case EXTERIOR:
                return _exteriorArea.exists();
            case ALL:
            default:
                return true;
        }
    }

    static Shape calculateComponentBodyArea(BoxModelConf state, float insTop, float insLeft, float insBottom, float insRight ) {
        return _calculateComponentBodyArea(
                    state,
                    insTop,
                    insLeft,
                    insBottom,
                    insRight
                );
    }

    private static Area _produceBorderArea(LazyRef<Shape> interiorArea, LazyRef<Shape> bodyArea) {
        Area componentArea = new Area(interiorArea.get());
        Area borderArea = new Area(bodyArea.get());
        borderArea.subtract(componentArea);
        return borderArea;
    }

    private static Shape _produceInteriorArea(BoxModelConf currentState) {
        Outline widths = currentState.widths();
        float leftBorderWidth   = widths.left().orElse(0f);
        float topBorderWidth    = widths.top().orElse(0f);
        float rightBorderWidth  = widths.right().orElse(0f);
        float bottomBorderWidth = widths.bottom().orElse(0f);
        return calculateComponentBodyArea(
                   currentState,
                   topBorderWidth,
                   leftBorderWidth,
                   bottomBorderWidth,
                   rightBorderWidth
               );
    }

    private static Area _produceExteriorArea(BoxModelConf currentState, LazyRef<Shape> bodyArea) {
        Size size = currentState.size();
        float width  = size.widthOrElse(0f);
        float height = size.heightOrElse(0f);
        Area exteriorComponentArea = new Area(new Rectangle2D.Float(0, 0, width, height));
        exteriorComponentArea.subtract(new Area(bodyArea.get()));
        return exteriorComponentArea;
    }

    private static Shape _produceBodyArea(BoxModelConf currentState) {
        return calculateComponentBodyArea(currentState, 0, 0, 0, 0);
    }

    private static Shape _calculateComponentBodyArea(
        final BoxModelConf border,
        float insTop,
        float insLeft,
        float insBottom,
        float insRight
    ) {
        final Outline margin  = border.margin();
        final Size    size    = border.size();
        final Outline outline = border.baseOutline();

        if ( BoxModelConf.none().equals(border) ) {
            Outline insets = outline.plus(margin).plus(Outline.of(insTop, insLeft, insBottom, insRight));
            // If there is no style, we just return the component's bounds:
            float left   = insets.left().orElse(0f);
            float top    = insets.top().orElse(0f);
            float right  = insets.right().orElse(0f);
            float bottom = insets.bottom().orElse(0f);
            float width  = size.widthOrElse(0f) - left - right;
            float height = size.heightOrElse(0f) - top - bottom;
            return _rectangularShapeFrom(left, top, width, height);
        }

        insTop    += outline.top().orElse(0f);
        insLeft   += outline.left().orElse(0f);
        insBottom += outline.bottom().orElse(0f);
        insRight  += outline.right().orElse(0f);

        // The background box is calculated from the margins and border radius:
        float left   = Math.max(margin.left().orElse(0f), 0)   + insLeft  ;
        float top    = Math.max(margin.top().orElse(0f), 0)    + insTop   ;
        float right  = Math.max(margin.right().orElse(0f), 0)  + insRight ;
        float bottom = Math.max(margin.bottom().orElse(0f), 0) + insBottom;
        float width  = size.widthOrElse(0f);
        float height = size.heightOrElse(0f);

        boolean insAllTheSame = insTop == insLeft && insLeft == insBottom && insBottom == insRight;

        if ( border.allCornersShareTheSameArc() && insAllTheSame ) {
            float arcWidth  = border.topLeftArc().map( a -> Math.max(0,a.width() ) ).orElse(0f);
            float arcHeight = border.topLeftArc().map( a -> Math.max(0,a.height()) ).orElse(0f);
            arcWidth  = Math.max(0, arcWidth  - insTop * 2f);
            arcHeight = Math.max(0, arcHeight - insTop * 2f);
            if ( arcWidth == 0 || arcHeight == 0 )
                return _rectangularShapeFrom(left, top, width - left - right, height - top - bottom);

            // We can return a simple round rectangle:
            return new RoundRectangle2D.Float(
                                left, top,
                                width - left - right, height - top - bottom,
                                arcWidth, arcHeight
                            );
        } else {
            Arc topLeftArc     = border.topLeftArc().orElse(null);
            Arc topRightArc    = border.topRightArc().orElse(null);
            Arc bottomRightArc = border.bottomRightArc().orElse(null);
            Arc bottomLeftArc  = border.bottomLeftArc().orElse(null);
            Area area = new Area();

            float topLeftRoundnessAdjustment     = Math.min(insLeft,   insTop  ) * 2f;
            float topRightRoundnessAdjustment    = Math.min(insTop,    insRight) * 2f;
            float bottomRightRoundnessAdjustment = Math.min(insBottom, insRight) * 2f;
            float bottomLeftRoundnessAdjustment  = Math.min(insBottom, insLeft ) * 2f;

            float arcWidthTL  = Math.max(0, topLeftArc     == null ? 0 : topLeftArc.width()      - topLeftRoundnessAdjustment);
            float arcHeightTL = Math.max(0, topLeftArc     == null ? 0 : topLeftArc.height()     - topLeftRoundnessAdjustment);
            float arcWidthTR  = Math.max(0, topRightArc    == null ? 0 : topRightArc.width()     - topRightRoundnessAdjustment);
            float arcHeightTR = Math.max(0, topRightArc    == null ? 0 : topRightArc.height()    - topRightRoundnessAdjustment);
            float arcWidthBR  = Math.max(0, bottomRightArc == null ? 0 : bottomRightArc.width()  - bottomRightRoundnessAdjustment);
            float arcHeightBR = Math.max(0, bottomRightArc == null ? 0 : bottomRightArc.height() - bottomRightRoundnessAdjustment);
            float arcWidthBL  = Math.max(0, bottomLeftArc  == null ? 0 : bottomLeftArc.width()   - bottomLeftRoundnessAdjustment);
            float arcHeightBL = Math.max(0, bottomLeftArc  == null ? 0 : bottomLeftArc.height()  - bottomLeftRoundnessAdjustment);

            // Top left:
            if ( topLeftArc != null ) {
                area.add(new Area(new Arc2D.Float(
                        left, top,
                        arcWidthTL, arcHeightTL,
                        90, 90, Arc2D.PIE
                )));
            }
            // Top right:
            if ( topRightArc != null ) {
                area.add(new Area(new Arc2D.Float(
                        width - right - topRightArc.width() + topRightRoundnessAdjustment,
                        top,
                        arcWidthTR, arcHeightTR,
                        0, 90, Arc2D.PIE
                )));
            }
            // Bottom right:
            if ( bottomRightArc != null ) {
                area.add(new Area(new Arc2D.Float(
                        width  - right  - bottomRightArc.width()  + bottomRightRoundnessAdjustment,
                        height - bottom - bottomRightArc.height() + bottomRightRoundnessAdjustment,
                        arcWidthBR, arcHeightBR,
                        270, 90, Arc2D.PIE
                )));
            }
            // Bottom left:
            if ( bottomLeftArc != null ) {
                area.add(new Area(new Arc2D.Float(
                        left,
                        height - bottom - bottomLeftArc.height() + bottomLeftRoundnessAdjustment,
                        arcWidthBL, arcHeightBL,
                        180, 90, Arc2D.PIE
                )));
            }
            /*
                Now we are going to have to fill four rectangles for each side of the partially rounded background box
                and then a single rectangle for the center.
                The four outer rectangles are calculated from the arcs and the margins.
             */
            float topDistance    = 0;
            float rightDistance  = 0;
            float bottomDistance = 0;
            float leftDistance   = 0;
            // top:
            if ( topLeftArc != null || topRightArc != null ) {
                float arcWidthLeft   = (arcWidthTL  / 2f);
                float arcHeightLeft  = (arcHeightTL / 2f);
                float arcWidthRight  = (arcWidthTR  / 2f);
                float arcHeightRight = (arcHeightTR / 2f);
                topDistance = Math.max(arcHeightLeft, arcHeightRight);// This is where the center rectangle will start!
                float innerLeft   = left + arcWidthLeft;
                float innerRight  = width - right - arcWidthRight;
                float edgeRectangleHeight = topDistance;
                area.add(new Area(new Rectangle2D.Float(
                        innerLeft, top, innerRight - innerLeft, edgeRectangleHeight
                    )));
            }
            // right:
            if ( topRightArc != null || bottomRightArc != null ) {
                float arcWidthTop    = (arcWidthTR  / 2f);
                float arcHeightTop   = (arcHeightTR / 2f);
                float arcWidthBottom = (arcWidthBR  / 2f);
                float arcHeightBottom= (arcHeightBR / 2f);
                rightDistance = Math.max(arcWidthTop, arcWidthBottom);// This is where the center rectangle will start!
                float innerTop    = top + arcHeightTop;
                float innerBottom = height - bottom - arcHeightBottom;
                float edgeRectangleWidth = rightDistance;
                area.add(new Area(new Rectangle2D.Float(
                        width - right - edgeRectangleWidth, innerTop, edgeRectangleWidth, innerBottom - innerTop
                    )));
            }
            // bottom:
            if ( bottomRightArc != null || bottomLeftArc != null ) {
                float arcWidthRight  = (arcWidthBR  / 2f);
                float arcHeightRight = (arcHeightBR / 2f);
                float arcWidthLeft   = (arcWidthBL  / 2f);
                float arcHeightLeft  = (arcHeightBL / 2f);
                bottomDistance = Math.max(arcHeightRight, arcHeightLeft);// This is where the center rectangle will start!
                float innerLeft   = left + arcWidthLeft;
                float innerRight  = width - right - arcWidthRight;
                float edgeRectangleHeight = bottomDistance;
                area.add(new Area(new Rectangle2D.Float(
                        innerLeft, height - bottom - edgeRectangleHeight, innerRight - innerLeft, edgeRectangleHeight
                    )));
            }
            // left:
            if ( bottomLeftArc != null || topLeftArc != null ) {
                float arcWidthBottom = (arcWidthBL  / 2f);
                float arcHeightBottom= (arcHeightBL / 2f);
                float arcWidthTop    = (arcWidthTL  / 2f);
                float arcHeightTop   = (arcHeightTL / 2f);
                leftDistance = Math.max(arcWidthBottom, arcWidthTop);// This is where the center rectangle will start!
                float innerTop    = top + arcHeightTop;
                float innerBottom = height - bottom - arcHeightBottom;
                float edgeRectangleWidth = leftDistance;
                area.add(new Area(new Rectangle2D.Float(
                        left, innerTop, edgeRectangleWidth, innerBottom - innerTop
                    )));
            }
            // Now we add the center:
            area.add(new Area(
                        new Rectangle2D.Float(
                            left + leftDistance, top + topDistance,
                            width - left - leftDistance - right - rightDistance,
                            height - top - topDistance - bottom - bottomDistance
                        )
                    ));
            return area;
        }
    }

    private static Shape _rectangularShapeFrom(
        float left  ,
        float top   ,
        float width ,
        float height
    ) {
        boolean isRound = Math.abs(left   % 1f) == 0f &&
                          Math.abs(top    % 1f) == 0f &&
                          Math.abs(width  % 1f) == 0f &&
                          Math.abs(height % 1f) == 0f;

        if ( isRound )
            return new Rectangle((int) left, (int) top, (int) width, (int) height);
        else
            return new Rectangle2D.Float(left, top, width, height);
    }


    /**
     *  Calculates the border-edge regions of the component's box model in the form of an array
     *  of 4 {@link Area} objects, one per edge, in the order top, right, bottom and left.
     *  Together they tile the margin box, so filling them with four different colors paints a
     *  border whose color changes along a miter at each corner.
     *  <p>
     *  A point belongs to the edge which is closest to it <i>relative to that edge's own border
     *  width</i>. This places every corner seam on the straight line running from the outer
     *  corner of the margin box through the matching inner corner, which is where a mitered
     *  corner joint belongs. For edges of equal width that line is the corner's diagonal, so a
     *  box as wide as it is tall is divided into four triangles, whereas a box wider than it is
     *  tall gets triangles at the left and right and trapeziums at the top and bottom.
     *  <p>
     *  Each such comparison of two scaled distances is cross multiplied into an affine
     *  inequality, which stays well defined when a border width is zero and makes a region the
     *  intersection of the margin box with three half planes.
     *  <p>
     *  The corner seams are therefore a function of the border widths alone and never of the
     *  size of the component, which is what makes a corner look the same at every aspect ratio.
     *  Only the two seams between opposite edges move with the size, and those lie within the
     *  {@link UI.ComponentArea#INTERIOR}, which is not part of any border.
     *  <p>
     *  An edge of zero width is given an empty region and its neighbours reach across it.
     *
     * @param boxModel The box model of the component
     * @return An array of 4 {@link Area} objects representing the border-edge areas
     */
    private static Area[] calculateEdgeBorderAreas( BoxModelConf boxModel ) {
        final Size    size   = boxModel.size();
        final Outline margin = boxModel.margin();
        final Outline widths = boxModel.widths();

        final double boxLeft   = Math.max(margin.left().orElse(0f), 0f);
        final double boxTop    = Math.max(margin.top().orElse(0f),  0f);
        final double boxWidth  = size.widthOrElse(0f)  - boxLeft - Math.max(margin.right().orElse(0f),  0f);
        final double boxHeight = size.heightOrElse(0f) - boxTop  - Math.max(margin.bottom().orElse(0f), 0f);

        final Area[] edgeAreas = new Area[4];
        if ( boxWidth <= 0 || boxHeight <= 0 ) {
            for ( int edge = 0; edge < 4; edge++ )
                edgeAreas[edge] = new Area();
            return edgeAreas;
        }

        final double[] edgeWidth = {
                            Math.max(widths.top().orElse(0f),    0f),
                            Math.max(widths.right().orElse(0f),  0f),
                            Math.max(widths.bottom().orElse(0f), 0f),
                            Math.max(widths.left().orElse(0f),   0f)
                        };
        final double[][] distanceToEdge = { // As affine {x, y, constant} coefficients:
                            {  0,  1, 0         }, // top
                            { -1,  0, boxWidth  }, // right
                            {  0, -1, boxHeight }, // bottom
                            {  1,  0, 0         }  // left
                        };

        for ( int edge = 0; edge < 4; edge++ ) {
            double[] region = { 0, 0, boxWidth, 0, boxWidth, boxHeight, 0, boxHeight };
            for ( int other = 0; other < 4 && region.length >= 6; other++ ) {
                if ( other == edge )
                    continue;
                region = _clippedToHalfPlane(region,
                            distanceToEdge[edge][0] * edgeWidth[other] - distanceToEdge[other][0] * edgeWidth[edge],
                            distanceToEdge[edge][1] * edgeWidth[other] - distanceToEdge[other][1] * edgeWidth[edge],
                            distanceToEdge[edge][2] * edgeWidth[other] - distanceToEdge[other][2] * edgeWidth[edge]
                        );
            }
            edgeAreas[edge] = _areaOf(region, boxLeft, boxTop);
        }
        return edgeAreas;
    }

    /**
     *  Clips a convex polygon, given as alternating x and y coordinates, against the half plane
     *  {@code a * x + b * y + c <= 0}, and returns the remaining polygon in the same form.
     *  A polygon of fewer than three points is empty.
     */
    private static double[] _clippedToHalfPlane( final double[] polygon, final double a, final double b, final double c ) {
        final double[] clipped = new double[polygon.length + 2];
        int size = 0;
        for ( int point = 0; point < polygon.length; point += 2 ) {
            final int next = ( point + 2 ) % polygon.length;
            final double x = polygon[point], y = polygon[point+1];
            final double nextX = polygon[next], nextY = polygon[next+1];
            final double side     = a * x     + b * y     + c;
            final double nextSide = a * nextX + b * nextY + c;
            if ( side <= 0 ) {
                clipped[size++] = x;
                clipped[size++] = y;
            }
            if ( ( side < 0 && nextSide > 0 ) || ( side > 0 && nextSide < 0 ) ) {
                final double crossing = side / ( side - nextSide );
                clipped[size++] = x + crossing * ( nextX - x );
                clipped[size++] = y + crossing * ( nextY - y );
            }
        }
        return Arrays.copyOf(clipped, size);
    }

    private static Area _areaOf( final double[] polygon, final double offsetX, final double offsetY ) {
        if ( polygon.length < 6 )
            return new Area();
        final Path2D.Double path = new Path2D.Double();
        path.moveTo(offsetX + polygon[0], offsetY + polygon[1]);
        for ( int point = 2; point < polygon.length; point += 2 )
            path.lineTo(offsetX + polygon[point], offsetY + polygon[point+1]);
        path.closePath();
        return new Area(path);
    }

}

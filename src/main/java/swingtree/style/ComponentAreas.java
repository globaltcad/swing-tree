package swingtree.style;

import swingtree.UI;
import swingtree.layout.Size;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
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

    private final BoxModelConf    _sourceState;
    private final LazyRef<Area>   _borderArea;
    private final LazyRef<Area>   _interiorArea;
    private final LazyRef<Area>   _exteriorArea;
    private final LazyRef<Area>   _bodyArea;
    private final LazyRef<Area[]> _borderEdgeAreas;

    static ComponentAreas of( Pooled<BoxModelConf> state ) {
        return _CACHE.computeIfAbsent(state, conf -> new ComponentAreas(state.get()));
    }


    private ComponentAreas(BoxModelConf conf) {
        _sourceState = conf;
        _borderArea      = new LazyRef<>(_sourceState, s->ComponentAreas._produceBorderArea(this));
        _interiorArea    = new LazyRef<>(_sourceState, ComponentAreas::_produceInteriorArea);
        _exteriorArea    = new LazyRef<>(_sourceState, s->ComponentAreas._produceExteriorArea(s,this));
        _bodyArea        = new LazyRef<>(_sourceState, ComponentAreas::_produceBodyArea);
        _borderEdgeAreas = new LazyRef<>(_sourceState, ComponentAreas::calculateEdgeBorderAreas);
    }

    public Shape get( UI.ComponentArea areaType ) {
        BoxModelConf boxModel = _sourceState;
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

    public Area[] getEdgeAreas() {
        return _borderEdgeAreas.get();
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

    static Area calculateComponentBodyArea(BoxModelConf state, float insTop, float insLeft, float insBottom, float insRight )
    {
        return _calculateComponentBodyArea(
                    state,
                    insTop,
                    insLeft,
                    insBottom,
                    insRight
                );
    }

    private static Area _produceBorderArea(ComponentAreas currentAreas) {
        Area componentArea = currentAreas._interiorArea.get();
        Area borderArea = new Area(currentAreas._bodyArea.get());
        borderArea.subtract(componentArea);
        return borderArea;
    }

    private static Area _produceInteriorArea(BoxModelConf currentState) {
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

    private static Area _produceExteriorArea(BoxModelConf currentState, ComponentAreas currentAreas) {
        Size size = currentState.size();
        float width  = size.width().orElse(0f);
        float height = size.height().orElse(0f);
        Area exteriorComponentArea = new Area(new Rectangle2D.Float(0, 0, width, height));
        exteriorComponentArea.subtract(currentAreas._bodyArea.get());
        return exteriorComponentArea;
    }

    private static Area _produceBodyArea(BoxModelConf currentState) {
        return calculateComponentBodyArea(currentState, 0, 0, 0, 0);
    }

    private static Area _calculateComponentBodyArea(
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
            return new Area(new Rectangle2D.Float(
                            insets.left().orElse(0f),
                            insets.top().orElse(0f),
                            size.width().orElse(0f) - insets.left().orElse(0f) - insets.right().orElse(0f),
                            size.height().orElse(0f) - insets.top().orElse(0f) - insets.bottom().orElse(0f)
                        ));
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
        float width  = size.width().orElse(0f);
        float height = size.height().orElse(0f);

        boolean insAllTheSame = insTop == insLeft && insLeft == insBottom && insBottom == insRight;

        if ( border.allCornersShareTheSameArc() && insAllTheSame ) {
            float arcWidth  = border.topLeftArc().map( a -> Math.max(0,a.width() ) ).orElse(0f);
            float arcHeight = border.topLeftArc().map( a -> Math.max(0,a.height()) ).orElse(0f);
            arcWidth  = Math.max(0, arcWidth  - insTop * 2f);
            arcHeight = Math.max(0, arcHeight - insTop * 2f);
            if ( arcWidth == 0 || arcHeight == 0 )
                return new Area(new Rectangle2D.Float(left, top, width - left - right, height - top - bottom));

            // We can return a simple round rectangle:
            return new Area(new RoundRectangle2D.Float(
                                left, top,
                                width - left - right, height - top - bottom,
                                arcWidth, arcHeight
                            ));
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


    /**
     *  Calculates the border-edge areas of the components box model in the form of
     *  an array of 4 {@link Area} objects, each representing the area of a single edge.
     *  So the top, right, bottom and left edge areas are returned in that order.
     *  <p>
     *  Each area is essentially just a polygon which consists of 5 points,
     *  two of which are the margin based border corners and the other three
     *  are the inner border width based corners as well as a center point.
     *
     * @param boxModel The box model of the component
     * @return An array of 4 {@link Area} objects representing the border-edge areas
     */
    private static Area[] calculateEdgeBorderAreas( BoxModelConf boxModel) {
        final Size    size   = boxModel.size();
        final Outline margin = boxModel.margin();
        final Outline widths = boxModel.widths();
        final float   width  = size.width().orElse(0f);
        final float   height = size.height().orElse(0f);

        final float topLeftX     = margin.left().orElse(0f);
        final float topLeftY     = margin.top().orElse(0f);
        final float topRightX    = width - margin.right().orElse(0f);
        final float topRightY    = topLeftY;
        final float bottomLeftX  = topLeftX;
        final float bottomLeftY  = height - margin.bottom().orElse(0f);
        final float bottomRightX = topRightX;
        final float bottomRightY = bottomLeftY;

        final float innerTopLeftX     = topLeftX + widths.left().orElse(0f);
        final float innerTopLeftY     = topLeftY + widths.top().orElse(0f);
        final float innerTopRightX    = topRightX - widths.right().orElse(0f);
        final float innerTopRightY    = innerTopLeftY;
        final float innerBottomLeftX  = bottomLeftX + widths.left().orElse(0f);
        final float innerBottomLeftY  = bottomLeftY - widths.bottom().orElse(0f);
        final float innerBottomRightX = bottomRightX - widths.right().orElse(0f);
        final float innerBottomRightY = innerBottomLeftY;

        final float innerCenterX = (innerTopLeftX + innerTopRightX) / 2f;
        final float innerCenterY = (innerTopLeftY + innerBottomLeftY) / 2f;

        Area[] edgeAreas = new Area[4];
        { // TOP:
            edgeAreas[0] = new Area(new Polygon(
                new int[] {(int)innerCenterX, (int)innerTopLeftX, (int)topLeftX, (int)topRightX, (int)innerTopRightX},
                new int[] {(int)innerCenterY, (int)innerTopLeftY, (int)topLeftY, (int)topRightY, (int)innerTopRightY},
                5
            ));
        }
        { // RIGHT:
            edgeAreas[1] = new Area(new Polygon(
                new int[] {(int)innerCenterX, (int)innerTopRightX, (int)topRightX, (int)bottomRightX, (int)innerBottomRightX},
                new int[] {(int)innerCenterY, (int)innerTopRightY, (int)topRightY, (int)bottomRightY, (int)innerBottomRightY},
                5
            ));
        }
        { // BOTTOM:
            edgeAreas[2] = new Area(new Polygon(
                new int[] {(int)innerCenterX, (int)innerBottomRightX, (int)bottomRightX, (int)bottomLeftX, (int)innerBottomLeftX},
                new int[] {(int)innerCenterY, (int)innerBottomRightY, (int)bottomRightY, (int)bottomLeftY, (int)innerBottomLeftY},
                5
            ));
        }
        { // LEFT:
            edgeAreas[3] = new Area(new Polygon(
                new int[] {(int)innerCenterX, (int)innerBottomLeftX, (int)bottomLeftX, (int)topLeftX, (int)innerTopLeftX},
                new int[] {(int)innerCenterY, (int)innerBottomLeftY, (int)bottomLeftY, (int)topLeftY, (int)innerTopLeftY},
                5
            ));
        }
        return edgeAreas;
    }

}

package swingtree.style;

import java.awt.Rectangle;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

final class AreasCache
{

    // Cached Area objects representing the component areas:

    // == _exteriorComponentArea - _interiorComponentArea
    private final Cached<Area> _borderArea = new Cached<Area>() {

        @Override
        protected Area produce(StyleRenderState currentState) {
            Area componentArea = _mainComponentArea.getFor(currentState);
            Area borderArea = new Area(_interiorComponentArea.getFor(currentState));
            borderArea.subtract(componentArea);
            return borderArea;
        }

        @Override
        public boolean leadsToSameValue(StyleRenderState oldState, StyleRenderState newState) {
            if ( !_mainComponentArea.leadsToSameValue(oldState, newState) )
                return false;

            if ( !_interiorComponentArea.leadsToSameValue(oldState, newState) )
                return false;

            return true;
        }
    };

    // == _borderArea + _interiorComponentArea
    private final Cached<Area> _mainComponentArea = new Cached<Area>() {

        @Override
        protected Area produce(StyleRenderState currentState) {
            Outline widths = currentState.style().border().widths();
            int leftBorderWidth   = widths.left().orElse(0);
            int topBorderWidth    = widths.top().orElse(0);
            int rightBorderWidth  = widths.right().orElse(0);
            int bottomBorderWidth = widths.bottom().orElse(0);
            return calculateBaseArea(
                       currentState,
                       topBorderWidth,
                       leftBorderWidth,
                       bottomBorderWidth,
                       rightBorderWidth
                   );
        }

        @Override
        public boolean leadsToSameValue(StyleRenderState oldState, StyleRenderState newState) {
            Outline oldWidths = oldState.style().border().widths();
            Outline newWidths = newState.style().border().widths();
            boolean sameWidths = oldWidths.equals(newWidths);
            return sameWidths && _testWouldLeadToSameBaseArea(oldState, newState);
        }
    };

    // == full component bounds - _mainComponentArea
    private final Cached<Area> _exteriorComponentArea = new Cached<Area>() {
        @Override
        protected Area produce(StyleRenderState currentState) {
            Area main = _mainComponentArea.getFor(currentState);
            Bounds bounds = currentState.currentBounds();
            Area exteriorComponentArea = new Area(new Rectangle(bounds.x(), bounds.y(), bounds.width(), bounds.height()));
            exteriorComponentArea.subtract(main);
            return exteriorComponentArea;
        }

        @Override
        public boolean leadsToSameValue(StyleRenderState oldState, StyleRenderState newState) {
            boolean mainIsSame = _mainComponentArea.leadsToSameValue(oldState, newState);
            if ( !mainIsSame )
                return false;
            
            Bounds oldBounds = oldState.currentBounds();
            Bounds newBounds = newState.currentBounds();

            return oldBounds.equals(newBounds);
        }
    };

    private final Cached<Area> _interiorComponentArea = new Cached<Area>() {
        @Override
        protected Area produce(StyleRenderState currentState) {
            return calculateBaseArea(currentState, 0, 0, 0, 0);
        }
        @Override
        public boolean leadsToSameValue(StyleRenderState oldState, StyleRenderState newState) {
            return _testWouldLeadToSameBaseArea(oldState, newState);
        }
    };


    public AreasCache() {}


    public Cached<Area> borderArea() { return _borderArea; }

    public Cached<Area> exteriorComponentArea() { return _exteriorComponentArea; }

    public Cached<Area> interiorComponentArea() { return _interiorComponentArea; }

    public void validate( StyleRenderState oldState, StyleRenderState newState )
    {
        if ( oldState.equals(newState) )
            return;

        _borderArea.validate(oldState, newState);
        _mainComponentArea.validate(oldState, newState);
        _exteriorComponentArea.validate(oldState, newState);
        _interiorComponentArea.validate(oldState, newState);
    }

    static Area calculateBaseArea(StyleRenderState state, int insTop, int insLeft, int insBottom, int insRight )
    {
        return _calculateBaseArea(
                    state.baseOutline(),
                    state.style().margin(),
                    state.style().border(),
                    state.currentBounds(),
                    state.style(),
                    insTop,
                    insLeft,
                    insBottom,
                    insRight
                );
    }

    private static Area _calculateBaseArea(
            Outline outline,
            Outline margin,
            BorderStyle border,
            Bounds currentBounds,
            Style style,
            int insTop,
            int insLeft,
            int insBottom,
            int insRight
    ) {
        if ( style.equals(Style.none()) ) {
            // If there is no style, we just return the component's bounds:
            return new Area(new Rectangle(0, 0, currentBounds.width(), currentBounds.height()));
        }

        insTop    += outline.top().orElse(0);
        insLeft   += outline.left().orElse(0);
        insBottom += outline.bottom().orElse(0);
        insRight  += outline.right().orElse(0);

        // The background box is calculated from the margins and border radius:
        int left   = Math.max(margin.left().orElse(0), 0)   + insLeft  ;
        int top    = Math.max(margin.top().orElse(0), 0)    + insTop   ;
        int right  = Math.max(margin.right().orElse(0), 0)  + insRight ;
        int bottom = Math.max(margin.bottom().orElse(0), 0) + insBottom;
        int width  = currentBounds.width();
        int height = currentBounds.height();

        boolean insAllTheSame = insTop == insLeft && insLeft == insBottom && insBottom == insRight;

        if ( border.allCornersShareTheSameArc() && insAllTheSame ) {
            int arcWidth  = border.topLeftArc().map( a -> Math.max(0,a.width() ) ).orElse(0);
            int arcHeight = border.topLeftArc().map( a -> Math.max(0,a.height()) ).orElse(0);
            arcWidth  = Math.max(0, arcWidth  - insTop);
            arcHeight = Math.max(0, arcHeight - insTop);
            if ( arcWidth == 0 || arcHeight == 0 )
                return new Area(new Rectangle(left, top, width - left - right, height - top - bottom));

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

            int topLeftRoundnessAdjustment     = Math.min(insLeft,   insTop  );
            int topRightRoundnessAdjustment    = Math.min(insTop,    insRight);
            int bottomRightRoundnessAdjustment = Math.min(insBottom, insRight);
            int bottomLeftRoundnessAdjustment  = Math.min(insBottom, insLeft );

            int arcWidthTL  = Math.max(0, topLeftArc     == null ? 0 : topLeftArc.width()      - topLeftRoundnessAdjustment);
            int arcHeightTL = Math.max(0, topLeftArc     == null ? 0 : topLeftArc.height()     - topLeftRoundnessAdjustment);
            int arcWidthTR  = Math.max(0, topRightArc    == null ? 0 : topRightArc.width()     - topRightRoundnessAdjustment);
            int arcHeightTR = Math.max(0, topRightArc    == null ? 0 : topRightArc.height()    - topRightRoundnessAdjustment);
            int arcWidthBR  = Math.max(0, bottomRightArc == null ? 0 : bottomRightArc.width()  - bottomRightRoundnessAdjustment);
            int arcHeightBR = Math.max(0, bottomRightArc == null ? 0 : bottomRightArc.height() - bottomRightRoundnessAdjustment);
            int arcWidthBL  = Math.max(0, bottomLeftArc  == null ? 0 : bottomLeftArc.width()   - bottomLeftRoundnessAdjustment);
            int arcHeightBL = Math.max(0, bottomLeftArc  == null ? 0 : bottomLeftArc.height()  - bottomLeftRoundnessAdjustment);

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
            int topDistance    = 0;
            int rightDistance  = 0;
            int bottomDistance = 0;
            int leftDistance   = 0;
            // top:
            if ( topLeftArc != null || topRightArc != null ) {
                int arcWidthLeft   = (int) Math.floor(arcWidthTL  / 2.0);
                int arcHeightLeft  = (int) Math.floor(arcHeightTL / 2.0);
                int arcWidthRight  = (int) Math.floor(arcWidthTR  / 2.0);
                int arcHeightRight = (int) Math.floor(arcHeightTR / 2.0);
                topDistance = Math.max(arcHeightLeft, arcHeightRight);// This is where the center rectangle will start!
                int innerLeft   = left + arcWidthLeft;
                int innerRight  = width - right - arcWidthRight;
                int edgeRectangleHeight = topDistance;
                area.add(new Area(new Rectangle2D.Float(
                        innerLeft, top, innerRight - innerLeft, edgeRectangleHeight
                    )));
            }
            // right:
            if ( topRightArc != null || bottomRightArc != null ) {
                int arcWidthTop    = (int) Math.floor(arcWidthTR  / 2.0);
                int arcHeightTop   = (int) Math.floor(arcHeightTR / 2.0);
                int arcWidthBottom = (int) Math.floor(arcWidthBR  / 2.0);
                int arcHeightBottom= (int) Math.floor(arcHeightBR / 2.0);
                rightDistance = Math.max(arcWidthTop, arcWidthBottom);// This is where the center rectangle will start!
                int innerTop    = top + arcHeightTop;
                int innerBottom = height - bottom - arcHeightBottom;
                int edgeRectangleWidth = rightDistance;
                area.add(new Area(new Rectangle2D.Float(
                        width - right - edgeRectangleWidth, innerTop, edgeRectangleWidth, innerBottom - innerTop
                    )));
            }
            // bottom:
            if ( bottomRightArc != null || bottomLeftArc != null ) {
                int arcWidthRight  = (int) Math.floor(arcWidthBR  / 2.0);
                int arcHeightRight = (int) Math.floor(arcHeightBR / 2.0);
                int arcWidthLeft   = (int) Math.floor(arcWidthBL  / 2.0);
                int arcHeightLeft  = (int) Math.floor(arcHeightBL / 2.0);
                bottomDistance = Math.max(arcHeightRight, arcHeightLeft);// This is where the center rectangle will start!
                int innerLeft   = left + arcWidthLeft;
                int innerRight  = width - right - arcWidthRight;
                int edgeRectangleHeight = bottomDistance;
                area.add(new Area(new Rectangle2D.Float(
                        innerLeft, height - bottom - edgeRectangleHeight, innerRight - innerLeft, edgeRectangleHeight
                    )));
            }
            // left:
            if ( bottomLeftArc != null || topLeftArc != null ) {
                int arcWidthBottom = (int) Math.floor(arcWidthBL  / 2.0);
                int arcHeightBottom= (int) Math.floor(arcHeightBL / 2.0);
                int arcWidthTop    = (int) Math.floor(arcWidthTL  / 2.0);
                int arcHeightTop   = (int) Math.floor(arcHeightTL / 2.0);
                leftDistance = Math.max(arcWidthBottom, arcWidthTop);// This is where the center rectangle will start!
                int innerTop    = top + arcHeightTop;
                int innerBottom = height - bottom - arcHeightBottom;
                int edgeRectangleWidth = leftDistance;
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
     *  For caching purposes we need to know if two states would lead to the same base area or not.
     *  So we check the various properties of the states that are used to calculate the base area
     *  and if they are all the same, we return true.
     */
    private static boolean _testWouldLeadToSameBaseArea( StyleRenderState state1, StyleRenderState state2 ) {
        if ( state1 == state2 ) return true;
        if ( state1 == null || state2 == null ) return false;
        boolean sameStyle   = state1.style().equals(state2.style());
        boolean sameBounds  = state1.currentBounds().equals(state2.currentBounds());
        if ( !sameStyle || !sameBounds ) return false;
        boolean sameOutline = state1.baseOutline().equals(state2.baseOutline());
        if ( !sameOutline ) return false;
        boolean sameMargin  = state1.style().margin().equals(state2.style().margin());
        if ( !sameMargin ) return false;
        boolean sameBorder  = state1.style().border().equals(state2.style().border());
        if ( !sameBorder ) return false;
        return true;
    }


}

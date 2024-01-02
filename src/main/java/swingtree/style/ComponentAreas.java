package swingtree.style;

import swingtree.layout.Bounds;
import swingtree.layout.Size;

import java.awt.Rectangle;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;

/**
 *  A wrapper object for transient reference based caching of the various areas of a component.
 *  This is used to avoid recalculating the areas of a component over and over again.
 */
final class ComponentAreas
{
    private final Cached<Area> _borderArea;
    private final Cached<Area> _interiorArea;
    private final Cached<Area> _exteriorArea;
    private final Cached<Area> _bodyArea;


    public ComponentAreas() {
        this(
            new Cached<>(new CacheProducerAndValidator<Area>(){

                @Override
                public Area produce(ComponentConf currentState, ComponentAreas currentAreas) {
                    Area componentArea = currentAreas._interiorArea.getFor(currentState, currentAreas);
                    Area borderArea = new Area(currentAreas._bodyArea.getFor(currentState, currentAreas));
                    borderArea.subtract(componentArea);
                    return borderArea;
                }

                @Override
                public boolean leadsToSameValue(ComponentConf oldState, ComponentConf newState, ComponentAreas currentAreas) {
                    if ( !currentAreas._interiorArea.leadsToSameValue(oldState, newState, currentAreas) )
                        return false;

                    if ( !currentAreas._bodyArea.leadsToSameValue(oldState, newState, currentAreas) )
                        return false;

                    return true;
                }
            }) ,
            new Cached<>(new CacheProducerAndValidator<Area>(){
        
                @Override
                public Area produce(ComponentConf currentState, ComponentAreas currentAreas) {
                    Outline widths = currentState.style().border().widths();
                    float leftBorderWidth   = widths.left().orElse(0f);
                    float topBorderWidth    = widths.top().orElse(0f);
                    float rightBorderWidth  = widths.right().orElse(0f);
                    float bottomBorderWidth = widths.bottom().orElse(0f);
                    return calculateBaseArea(
                               currentState,
                               topBorderWidth,
                               leftBorderWidth,
                               bottomBorderWidth,
                               rightBorderWidth
                           );
                }
        
                @Override
                public boolean leadsToSameValue(ComponentConf oldState, ComponentConf newState, ComponentAreas currentAreas) {
                    Outline oldWidths = oldState.style().border().widths();
                    Outline newWidths = newState.style().border().widths();
                    boolean sameWidths = oldWidths.equals(newWidths);
                    return sameWidths && _testWouldLeadToSameBaseArea(oldState, newState);
                }
            }),
            new Cached<>(new CacheProducerAndValidator<Area>(){
                @Override
                public Area produce(ComponentConf currentState, ComponentAreas currentAreas) {
                    Bounds bounds = currentState.currentBounds();
                    Area exteriorComponentArea = new Area(bounds.toRectangle());
                    exteriorComponentArea.subtract(currentAreas._bodyArea.getFor(currentState, currentAreas));
                    return exteriorComponentArea;
                }
        
                @Override
                public boolean leadsToSameValue(ComponentConf oldState, ComponentConf newState, ComponentAreas currentAreas) {
                    boolean mainIsSame = currentAreas._bodyArea.leadsToSameValue(oldState, newState, currentAreas);
                    if ( !mainIsSame )
                        return false;
                    
                    Bounds oldBounds = oldState.currentBounds();
                    Bounds newBounds = newState.currentBounds();
        
                    return oldBounds.equals(newBounds);
                }
            }),
            new Cached<>(new CacheProducerAndValidator<Area>(){
                @Override
                public Area produce(ComponentConf currentState, ComponentAreas currentAreas) {
                    return calculateBaseArea(currentState, 0, 0, 0, 0);
                }
                @Override
                public boolean leadsToSameValue(ComponentConf oldState, ComponentConf newState, ComponentAreas currentAreas) {
                    return _testWouldLeadToSameBaseArea(oldState, newState);
                }
            })
        );
    }
    
    public ComponentAreas(
        Cached<Area> borderArea, 
        Cached<Area> interiorComponentArea, 
        Cached<Area> exteriorComponentArea, 
        Cached<Area> mainComponentArea
    ) {
        _borderArea   = Objects.requireNonNull(borderArea);
        _interiorArea = Objects.requireNonNull(interiorComponentArea);
        _exteriorArea = Objects.requireNonNull(exteriorComponentArea);
        _bodyArea     = Objects.requireNonNull(mainComponentArea);
    }


    public Cached<Area> borderArea() { return _borderArea; }

    public Cached<Area> exteriorArea() { return _exteriorArea; }

    public Cached<Area> interiorArea() { return _interiorArea; }

    public Cached<Area> bodyArea() { return _bodyArea; }


    public ComponentAreas validate(ComponentConf oldConf, ComponentConf newConf )
    {
        if ( oldConf.equals(newConf) )
            return this;

        Cached<Area> newBorderArea = _borderArea.validate(oldConf, newConf, this);
        Cached<Area> newInterior   = _interiorArea.validate(oldConf, newConf, this);
        Cached<Area> newExterior   = _exteriorArea.validate(oldConf, newConf, this);
        Cached<Area> newBody       = _bodyArea.validate(oldConf, newConf, this);
        
        if (
            newBorderArea != _borderArea ||
            newInterior   != _interiorArea ||
            newExterior   != _exteriorArea ||
            newBody       != _bodyArea
        ) {
            return new ComponentAreas(newBorderArea, newInterior, newExterior, newBody);
        }
        return this;
    }

    static Area calculateBaseArea( ComponentConf state, float insTop, float insLeft, float insBottom, float insRight )
    {
        return _calculateBaseArea(
                    state.baseOutline(),
                    state.style().margin(),
                    state.style().border(),
                    state.currentBounds().size(),
                    state.style(),
                    insTop,
                    insLeft,
                    insBottom,
                    insRight
                );
    }

    private static Area _calculateBaseArea(
            Outline     outline,
            Outline     margin,
            BorderStyle border,
            Size        size,
            Style       style,
            float       insTop,
            float       insLeft,
            float       insBottom,
            float       insRight
    ) {
        if ( style.equals(Style.none()) ) {
            // If there is no style, we just return the component's bounds:
            return new Area(new Rectangle2D.Float(
                            insLeft, insTop,
                            size.width().orElse(0f) - insLeft - insRight,
                            size.height().orElse(0f) - insTop - insBottom
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
            float arcWidth  = border.topLeftArc().map( a -> Math.max(0,a.width() ) ).orElse(0);
            float arcHeight = border.topLeftArc().map( a -> Math.max(0,a.height()) ).orElse(0);
            arcWidth  = Math.max(0, arcWidth  - insTop);
            arcHeight = Math.max(0, arcHeight - insTop);
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

            float topLeftRoundnessAdjustment     = Math.min(insLeft,   insTop  );
            float topRightRoundnessAdjustment    = Math.min(insTop,    insRight);
            float bottomRightRoundnessAdjustment = Math.min(insBottom, insRight);
            float bottomLeftRoundnessAdjustment  = Math.min(insBottom, insLeft );

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
     *  For caching purposes we need to know if two states would lead to the same base area or not.
     *  So we check the various properties of the states that are used to calculate the base area
     *  and if they are all the same, we return true.
     */
    private static boolean _testWouldLeadToSameBaseArea(ComponentConf state1, ComponentConf state2 ) {
        if ( state1 == state2 )
            return true;
        if ( state1 == null || state2 == null )
            return false;
        Outline     outline1 = state1.baseOutline();
        Outline     outline2 = state2.baseOutline();
        boolean sameOutline = outline1.equals(outline2);
        if ( !sameOutline )
            return false;
        Outline     margin1  = state1.style().margin();
        Outline     margin2  = state2.style().margin();
        boolean sameMargin  = margin1.equals(margin2);
        if ( !sameMargin )
            return false;
        BorderStyle border1  = state1.style().border();
        BorderStyle border2  = state2.style().border();
        boolean sameBorder  = border1.equals(border2);
        if ( !sameBorder )
            return false;
        Size size1 = state1.currentBounds().size();
        Size size2 = state2.currentBounds().size();
        boolean sameSize  = size1.equals(size2);
        if ( !sameSize )
            return false;
        return true;
    }


}

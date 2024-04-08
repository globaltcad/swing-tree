package swingtree.style;

import org.jspecify.annotations.Nullable;
import swingtree.UI;
import swingtree.layout.Size;

import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 *  A wrapper object for transient reference based caching of the various areas of a component.
 *  This is used to avoid recalculating the areas of a component over and over again.
 */
final class ComponentAreas
{
    private static final Map<BoxModelConf, ComponentAreas> _CACHE = new WeakHashMap<>();

    private final LazyRef<Area> _borderArea;
    private final LazyRef<Area> _interiorArea;
    private final LazyRef<Area> _exteriorArea;
    private final LazyRef<Area> _bodyArea;
    private final WeakReference<BoxModelConf> _key;


    static ComponentAreas of( BoxModelConf state ) {
        return _CACHE.computeIfAbsent(state, conf -> new ComponentAreas(state));
    }

    static BoxModelConf intern(BoxModelConf state ) {
        ComponentAreas areas = _CACHE.get(state);
        if ( areas != null ) {
            BoxModelConf key = areas._key.get();
            if ( key != null )
                return key;
        }
        _CACHE.put(state, new ComponentAreas(state));
        return state;
    }

    private ComponentAreas(BoxModelConf conf) {
        this(
            conf,
            new LazyRef<>(new CacheProducerAndValidator<Area>(){
                @Override
                public Area produce(BoxModelConf currentState, ComponentAreas currentAreas) {
                    Area componentArea = currentAreas._interiorArea.getFor(currentState, currentAreas);
                    Area borderArea = new Area(currentAreas._bodyArea.getFor(currentState, currentAreas));
                    borderArea.subtract(componentArea);
                    return borderArea;
                }
            }),
            new LazyRef<>(new CacheProducerAndValidator<Area>(){
        
                @Override
                public Area produce(BoxModelConf currentState, ComponentAreas currentAreas) {
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
            }),
            new LazyRef<>(new CacheProducerAndValidator<Area>(){
                @Override
                public Area produce(BoxModelConf currentState, ComponentAreas currentAreas) {
                    Size size = currentState.size();
                    float width  = size.width().orElse(0f);
                    float height = size.height().orElse(0f);
                    Area exteriorComponentArea = new Area(new Rectangle2D.Float(0, 0, width, height));
                    exteriorComponentArea.subtract(currentAreas._bodyArea.getFor(currentState, currentAreas));
                    return exteriorComponentArea;
                }
            }),
            new LazyRef<>(new CacheProducerAndValidator<Area>(){
                @Override
                public Area produce(BoxModelConf currentState, ComponentAreas currentAreas) {
                    return calculateComponentBodyArea(currentState, 0, 0, 0, 0);
                }
            })
        );
    }
    
    public ComponentAreas(
        BoxModelConf conf,
        LazyRef<Area> borderArea,
        LazyRef<Area> interiorComponentArea,
        LazyRef<Area> exteriorComponentArea,
        LazyRef<Area> componentBodyArea
    ) {
        _key = new WeakReference<>(conf);
        _borderArea   = Objects.requireNonNull(borderArea);
        _interiorArea = Objects.requireNonNull(interiorComponentArea);
        _exteriorArea = Objects.requireNonNull(exteriorComponentArea);
        _bodyArea     = Objects.requireNonNull(componentBodyArea);
    }


    public @Nullable Area get( UI.ComponentArea areaType ) {
        BoxModelConf boxModel = Optional.ofNullable(_key.get()).orElse(BoxModelConf.none());
        switch ( areaType ) {
            case ALL:
                return null; // No clipping
            case BODY:
                return bodyArea().getFor(boxModel, this); // all - exterior == interior + border
            case INTERIOR:
                return interiorArea().getFor(boxModel, this); // all - exterior - border == content - border
            case BORDER:
                return borderArea().getFor(boxModel, this); // all - exterior - interior
            case EXTERIOR:
                return exteriorArea().getFor(boxModel, this); // all - border - interior
            default:
                return null;
        }
    }

    public LazyRef<Area> borderArea() { return _borderArea; }

    public LazyRef<Area> exteriorArea() { return _exteriorArea; }

    public LazyRef<Area> interiorArea() { return _interiorArea; }

    public LazyRef<Area> bodyArea() { return _bodyArea; }

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

        if ( BorderConf.none().equals(border) ) {
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
}

package swingtree.style;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.api.Painter;
import swingtree.layout.Bounds;
import swingtree.layout.Size;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.*;
import java.util.List;
import java.util.function.Function;

/**
 *  A stateless un-instantiable utility class that renders the style of a component
 *  using the immutable {@link LayerRenderConf} object containing the essential state
 *  needed for rendering, like for example the current {@link Bounds} and {@link StyleConf}
 *  of a particular component.
 */
final class StyleRenderer
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(StyleRenderer.class);
    private static final Map<NoiseConf, Map<Point2D,NoiseGradientPaint>> _NOISE_PAINT_CACHE = new WeakHashMap<>();

    private StyleRenderer() {} // Un-instantiable!


    public static void renderStyleOn(
        UI.Layer layer,
        LayerRenderConf conf,
        Graphics2D g2d
    ) {
        // First we render things unique to certain layers:

        // Background stuff:
        conf.baseColors().foundationColor().ifPresent(outerColor -> {
            if ( outerColor.getAlpha() > 0 ) { // Avoid rendering a fully transparent color!
                g2d.setColor(outerColor);
                g2d.fill(conf.areas().get(UI.ComponentArea.EXTERIOR));
            }
        });
        conf.baseColors().backgroundColor().ifPresent(color -> {
            if ( color.getAlpha() > 0 ) { // Avoid rendering a fully transparent color!
                g2d.setColor(color);
                g2d.fill(conf.areas().get(UI.ComponentArea.BODY));
            }
        });

        // Border stuff:
        _drawBorder( conf, conf.baseColors().borderColor(), g2d);

        // Now onto things every layer has in common:

        // Every layer has 4 things:
        // 1. A grounding serving as a base background, which is a filled color and/or an image:
        for ( ImageConf imageConf : conf.layer().images().sortedByNames() )
            if ( !imageConf.equals(ImageConf.none()) )
                _renderImage( conf, imageConf, conf.boxModel().size(), g2d);

        // 2. Gradients, which are best used to give a component a nice surface lighting effect.
        // They may transition vertically, horizontally or diagonally over various different colors:
        for ( GradientConf gradient : conf.layer().gradients().sortedByNames() )
            if ( gradient.colors().length > 0 ) {
                _renderGradient( gradient, conf, g2d );
            }

        // 3. Noise, which is a simple way to add a bit of texture to a component:
        for ( NoiseConf noise : conf.layer().noises().sortedByNames() )
            if ( noise.colors().length > 0 ) {
                _renderNoise( noise, conf, g2d );
            }

        // 4. Shadows, which are simple gradient based drop shadows that can go inwards or outwards
        for ( ShadowConf shadow : conf.layer().shadows().sortedByNames() )
            _renderShadows(conf, shadow, g2d);

        // 5. Custom text, which can be rendered in any font and color:
        for ( TextConf text : conf.layer().texts().sortedByNames() )
            _renderText( text, conf, g2d );

        // 6. Painters, which are provided by the user and can be anything
        _executeUserPainters(layer, conf, g2d);

        // And that's it! We have rendered a style layer!
    }

    private static void _drawBorder( LayerRenderConf conf, BorderColorsConf colors, Graphics2D g2d )
    {
        if ( colors.equals(BorderColorsConf.none()) )
            return;

        if ( !Outline.none().equals(conf.boxModel().widths()) ) {
            try {
                Shape borderArea = conf.areas().get(UI.ComponentArea.BORDER);
                Objects.requireNonNull(borderArea);
                if ( colors.isHomogeneous() ) {
                    g2d.setColor(colors.bottom().orElse(UI.Color.BLACK));
                    g2d.fill(borderArea);
                } else {
                    Area[] borderEdgeRegions = conf.areas().getEdgeAreas();
                    // We created clipped border areas:
                    Area topBorderArea = new Area(borderArea);
                    topBorderArea.intersect(borderEdgeRegions[0]);
                    Area rightBorderArea = new Area(borderArea);
                    rightBorderArea.intersect(borderEdgeRegions[1]);
                    Area bottomBorderArea = new Area(borderArea);
                    bottomBorderArea.intersect(borderEdgeRegions[2]);
                    Area leftBorderArea = new Area(borderArea);
                    leftBorderArea.intersect(borderEdgeRegions[3]);
                    // Now we can draw the borders:
                    g2d.setColor(colors.top().orElse(UI.Color.BLACK));
                    g2d.fill(topBorderArea);
                    g2d.setColor(colors.right().orElse(UI.Color.BLACK));
                    g2d.fill(rightBorderArea);
                    g2d.setColor(colors.bottom().orElse(UI.Color.BLACK));
                    g2d.fill(bottomBorderArea);
                    g2d.setColor(colors.left().orElse(UI.Color.BLACK));
                    g2d.fill(leftBorderArea);
                }
            } catch ( Exception e ) {
                log.warn(
                    "An exception occurred while drawing the border of border style '" + conf.boxModel() + "' ",
                    e
                );
                /*
                    If exceptions happen in user provided painters, we don't want to
                    mess up the rendering of the rest of the component, so we catch them here!
                */
            }
        }
    }

    private static void _renderShadows(
        LayerRenderConf conf,
        ShadowConf    shadow,
        Graphics2D    g2d
    ) {
        if ( !shadow.color().isPresent() )
            return;

        Color shadowColor = shadow.color().orElse(Color.BLACK);
        Size  size        = conf.boxModel().size();

        // First let's check if we need to render any shadows at all
        // Is the shadow color transparent?
        if ( shadowColor.getAlpha() == 0 )
            return;

        // The background box is calculated from the margins and border radius:
        final float leftBorderWidth   = conf.boxModel().widths().left().orElse(0f);
        final float topBorderWidth    = conf.boxModel().widths().top().orElse(0f);
        final float rightBorderWidth  = conf.boxModel().widths().right().orElse(0f);
        final float bottomBorderWidth = conf.boxModel().widths().bottom().orElse(0f);
        final float left   = Math.max(conf.boxModel().margin().left().orElse(0f),   0) + ( shadow.isInset() ? leftBorderWidth   : 0 );
        final float top    = Math.max(conf.boxModel().margin().top().orElse(0f),    0) + ( shadow.isInset() ? topBorderWidth    : 0 );
        final float right  = Math.max(conf.boxModel().margin().right().orElse(0f),  0) + ( shadow.isInset() ? rightBorderWidth  : 0 );
        final float bottom = Math.max(conf.boxModel().margin().bottom().orElse(0f), 0) + ( shadow.isInset() ? bottomBorderWidth : 0 );
        final float topLeftRadius     = Math.max(conf.boxModel().topLeftRadius(), 0);
        final float topRightRadius    = Math.max(conf.boxModel().topRightRadius(), 0);
        final float bottomRightRadius = Math.max(conf.boxModel().bottomRightRadius(), 0);
        final float bottomLeftRadius  = Math.max(conf.boxModel().bottomLeftRadius(), 0);

        final float width     = size.width().orElse(0f);
        final float height    = size.height().orElse(0f);

        // Calculate the shadow box bounds based on the padding and border thickness
        final float x = left + shadow.horizontalOffset();
        final float y = top  + shadow.verticalOffset();
        final float w = width  - left - right;
        final float h = height - top  - bottom;

        final float blurRadius   = Math.max(shadow.blurRadius(), 0);
        final float spreadRadius = !shadow.isOutset() ? shadow.spreadRadius() : -shadow.spreadRadius();

        Rectangle2D.Float outerShadowRect = new Rectangle2D.Float(
                                        x - blurRadius + spreadRadius,
                                        y - blurRadius + spreadRadius,
                                        w + blurRadius * 2 - spreadRadius * 2,
                                        h + blurRadius * 2 - spreadRadius * 2
                                    );

        Function<Integer, Integer> offsetFunction = (radius) -> (int)((radius * 2) / ( shadow.isInset() ? 4.5 : 3.79) );

        final int averageCornerRadius = ((int) ( topLeftRadius + topRightRadius + bottomRightRadius + bottomLeftRadius )) / 4;
        final int averageBorderWidth  = (int) (( leftBorderWidth + topBorderWidth + rightBorderWidth +  bottomBorderWidth ) / 4);
        final int shadowCornerRadius  = (int) Math.max( 0, averageCornerRadius + (shadow.isOutset() ? -spreadRadius-blurRadius*2 : -Math.max(averageBorderWidth,spreadRadius)) );
        final int gradientStartOffset = 1 + offsetFunction.apply(shadowCornerRadius);

        Rectangle2D.Float innerShadowRect = new Rectangle2D.Float(
                                        x + blurRadius + gradientStartOffset + spreadRadius,
                                        y + blurRadius + gradientStartOffset + spreadRadius,
                                        w - blurRadius * 2 - gradientStartOffset * 2 - spreadRadius * 2,
                                        h - blurRadius * 2 - gradientStartOffset * 2 - spreadRadius * 2
                                    );

        final Area baseArea;

        if ( shadow.isOutset() ) {
            int artifactAdjustment = 1;
            baseArea = ComponentAreas.calculateComponentBodyArea(conf.boxModel(), artifactAdjustment, artifactAdjustment, artifactAdjustment, artifactAdjustment);
        }
        else
            baseArea = new Area(conf.areas().get(UI.ComponentArea.BODY));

        // Apply the clipping to avoid overlapping the shadow and the box
        Area shadowArea = new Area(outerShadowRect);

        if ( shadow.isOutset() )
            shadowArea.subtract(baseArea);
        else
            shadowArea.intersect(baseArea);

        // Draw the corner shadows
        _renderCornerShadow(shadow, UI.Corner.TOP_LEFT,     shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
        _renderCornerShadow(shadow, UI.Corner.TOP_RIGHT,    shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
        _renderCornerShadow(shadow, UI.Corner.BOTTOM_LEFT,  shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
        _renderCornerShadow(shadow, UI.Corner.BOTTOM_RIGHT, shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);

        // Draw the edge shadows
        _renderEdgeShadow(shadow, UI.Edge.TOP,    shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
        _renderEdgeShadow(shadow, UI.Edge.RIGHT,  shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
        _renderEdgeShadow(shadow, UI.Edge.BOTTOM, shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
        _renderEdgeShadow(shadow, UI.Edge.LEFT,   shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);

        Area outerMostArea = new Area(outerShadowRect);
        // If the base rectangle and the outer shadow box are not equal, then we need to fill the area of the base rectangle that is not covered by the outer shadow box!
        _renderShadowBody(shadow, baseArea, innerShadowRect, outerMostArea, g2d);

    }

    private static void _renderShadowBody(
        ShadowConf shadowConf,
        Area              baseArea,
        Rectangle2D.Float innerShadowRect,
        Area              outerShadowBox,
        Graphics2D        g2d
    ) {
        Graphics2D g2d2 = (Graphics2D) g2d.create();
        g2d2.setColor(shadowConf.color().orElse(Color.BLACK));
        if ( !shadowConf.isOutset() ) {
            baseArea.subtract(outerShadowBox);
            g2d2.fill(baseArea);
        } else {
            Area innerShadowArea = new Area(innerShadowRect);
            innerShadowArea.subtract(baseArea);
            g2d2.fill(innerShadowArea);
        }
        g2d2.dispose();
    }

    private static void _renderCornerShadow(
        ShadowConf shadowConf,
        UI.Corner         corner,
        Area              areaWhereShadowIsAllowed,
        Rectangle2D.Float innerShadowRect,
        Rectangle2D.Float outerShadowRect,
        int               gradientStartOffset,
        Graphics2D        g2d
    ) {
        // We define a clipping box so that corners don't overlap
        float clipBoxWidth   = outerShadowRect.width / 2f;
        float clipBoxHeight  = outerShadowRect.height / 2f;
        float clipBoxCenterX = outerShadowRect.x + clipBoxWidth;
        float clipBoxCenterY = outerShadowRect.y + clipBoxHeight;
        Rectangle2D.Float cornerClipBox; // outer box!

        // The defining the corner shadow bound (where it starts and ends
        Rectangle2D.Float cornerBox;
        float cx;
        float cy;
        float cr; // depending on the corner, this is either the corner box width or height
        switch (corner) {
            case TOP_LEFT:
                cornerBox = new Rectangle2D.Float(
                                    outerShadowRect.x, outerShadowRect.y,
                                    innerShadowRect.x - outerShadowRect.x,
                                    innerShadowRect.y - outerShadowRect.y
                                );
                cornerClipBox = new Rectangle2D.Float(
                                    clipBoxCenterX - clipBoxWidth, clipBoxCenterY - clipBoxHeight,
                                    clipBoxWidth, clipBoxHeight
                                );

                cx = cornerBox.x + cornerBox.width;
                cy = cornerBox.y + cornerBox.height;
                cr = cornerBox.width;
                break;
            case TOP_RIGHT:
                cornerBox = new Rectangle2D.Float(
                                innerShadowRect.x + innerShadowRect.width, outerShadowRect.y,
                                outerShadowRect.x + outerShadowRect.width - innerShadowRect.x - innerShadowRect.width,
                                innerShadowRect.y - outerShadowRect.y
                            );
                cornerClipBox = new Rectangle2D.Float(
                                    clipBoxCenterX, clipBoxCenterY - clipBoxHeight,
                                    clipBoxWidth, clipBoxHeight
                                );

                cx = cornerBox.x;
                cy = cornerBox.y + cornerBox.height;
                cr = cornerBox.width;
                break;
            case BOTTOM_LEFT:
                cornerBox = new Rectangle2D.Float(
                                outerShadowRect.x,
                                innerShadowRect.y + innerShadowRect.height,
                                innerShadowRect.x - outerShadowRect.x,
                                outerShadowRect.y + outerShadowRect.height - innerShadowRect.y - innerShadowRect.height
                            );
                cornerClipBox = new Rectangle2D.Float(
                                    clipBoxCenterX - clipBoxWidth, clipBoxCenterY,
                                    clipBoxWidth, clipBoxHeight
                                );

                cx = cornerBox.x + cornerBox.width;
                cy = cornerBox.y;
                cr = cornerBox.width;
                break;
            case BOTTOM_RIGHT:
                cornerBox = new Rectangle2D.Float(
                            innerShadowRect.x + innerShadowRect.width, innerShadowRect.y + innerShadowRect.height,
                            outerShadowRect.x + outerShadowRect.width - innerShadowRect.x - innerShadowRect.width,
                            outerShadowRect.y + outerShadowRect.height - innerShadowRect.y - innerShadowRect.height
                            );
                cornerClipBox = new Rectangle2D.Float(
                                    clipBoxCenterX, clipBoxCenterY,
                                    clipBoxWidth, clipBoxHeight
                                );

                cx = cornerBox.x;
                cy = cornerBox.y;
                cr = cornerBox.width;
                break;
            default:
                throw new IllegalArgumentException("Invalid corner: " + corner);
        }

        if (cr <= 0) return;

        Color innerColor;
        Color outerColor;
        Color shadowBackgroundColor = _transparentShadowBackground(shadowConf);
        if ( shadowConf.isOutset() ) {
            innerColor = shadowConf.color().orElse(Color.BLACK);
            outerColor = shadowBackgroundColor;
        } else {
            innerColor = shadowBackgroundColor;
            outerColor = shadowConf.color().orElse(Color.BLACK);
        }
        float gradientStart = (float) gradientStartOffset / cr;

        // The first thing we can do is to clip the corner box to the area where the shadow is allowed
        Area cornerArea = new Area(cornerBox);
        cornerArea.intersect(areaWhereShadowIsAllowed);

        // In the simplest case we don't need to do any gradient painting:
        if ( gradientStart == 1f || gradientStart == 0f ) {
            // Simple, we just draw a circle and clip it
            Area circle = new Area(new Ellipse2D.Float(cx - cr, cy - cr, cr * 2, cr * 2));
            if ( shadowConf.isInset() ) {
                g2d.setColor(outerColor);
                cornerArea.subtract(circle);
            } else {
                g2d.setColor(innerColor);
                cornerArea.intersect(circle);
            }
            g2d.fill(cornerArea);
            return;
        }

        RadialGradientPaint cornerPaint;
        if ( gradientStart > 1f || gradientStart < 0f )
            cornerPaint = new RadialGradientPaint(
                             cx, cy, cr,
                             new float[] {0f, 1f},
                             new Color[] {innerColor, outerColor}
                         );
        else
            cornerPaint = new RadialGradientPaint(
                             cx, cy, cr,
                             new float[] {0f, gradientStart, 1f},
                             new Color[] {innerColor, innerColor, outerColor}
                         );

        // We need to clip the corner paint to the corner box
        cornerArea.intersect(new Area(cornerClipBox));

        Graphics2D cornerG2d = (Graphics2D) g2d.create();
        cornerG2d.setPaint(cornerPaint);
        cornerG2d.fill(cornerArea);
        cornerG2d.dispose();
    }

    private static void _renderEdgeShadow(
        ShadowConf        shadowConf,
        UI.Edge           edge,
        Area              contentArea,
        Rectangle2D.Float innerShadowRect,
        Rectangle2D.Float outerShadowRect,
        int               gradientStartOffset,
        Graphics2D        g2d
    ) {
        // We define a boundary center point and a clipping box so that edges don't overlap
        float clipBoundaryX = outerShadowRect.x + outerShadowRect.width / 2f;
        float clipBoundaryY = outerShadowRect.y + outerShadowRect.height / 2f;
        Rectangle2D.Float edgeClipBox = null;

        Rectangle2D.Float edgeBox;
        float gradEndX;
        float gradEndY;
        float gradStartX;
        float gradStartY;
        switch (edge) {
            case TOP:
                edgeBox = new Rectangle2D.Float(
                                innerShadowRect.x, outerShadowRect.y,
                                innerShadowRect.width, innerShadowRect.y - outerShadowRect.y
                            );

                if ( (edgeBox.y + edgeBox.height) > clipBoundaryY )
                    edgeClipBox = new Rectangle2D.Float(
                            edgeBox.x, edgeBox.y,
                            edgeBox.width, clipBoundaryY - edgeBox.y
                    );

                gradEndX = edgeBox.x;
                gradEndY = edgeBox.y;
                gradStartX = edgeBox.x;
                gradStartY = edgeBox.y + edgeBox.height;
                break;
            case RIGHT:
                edgeBox = new Rectangle2D.Float(
                                innerShadowRect.x + innerShadowRect.width, innerShadowRect.y,
                                outerShadowRect.x + outerShadowRect.width - innerShadowRect.x - innerShadowRect.width,
                                innerShadowRect.height
                            );
                if ( edgeBox.x < clipBoundaryX )
                    edgeClipBox = new Rectangle2D.Float(
                                        clipBoundaryX, edgeBox.y,
                                        edgeBox.x + edgeBox.width - clipBoundaryX, edgeBox.height
                                    );
                gradEndX = edgeBox.x + edgeBox.width;
                gradEndY = edgeBox.y;
                gradStartX = edgeBox.x;
                gradStartY = edgeBox.y;
                break;
            case BOTTOM:
                edgeBox = new Rectangle2D.Float(
                        innerShadowRect.x, innerShadowRect.y + innerShadowRect.height,
                        innerShadowRect.width, outerShadowRect.y + outerShadowRect.height - innerShadowRect.y - innerShadowRect.height
                    );
                if ( edgeBox.y < clipBoundaryY )
                    edgeClipBox = new Rectangle2D.Float(
                            edgeBox.x,
                            clipBoundaryY,
                            edgeBox.width,
                            edgeBox.y + edgeBox.height - clipBoundaryY
                    );

                gradEndX = edgeBox.x;
                gradEndY = edgeBox.y + edgeBox.height;
                gradStartX = edgeBox.x;
                gradStartY = edgeBox.y;
                break;
            case LEFT:
                edgeBox = new Rectangle2D.Float(
                            outerShadowRect.x,
                            innerShadowRect.y,
                            innerShadowRect.x - outerShadowRect.x,
                            innerShadowRect.height
                            );
                if ( (edgeBox.x + edgeBox.width) > clipBoundaryX )
                    edgeClipBox = new Rectangle2D.Float(
                            edgeBox.x,
                            edgeBox.y,
                            clipBoundaryX - edgeBox.x,
                            edgeBox.height
                    );
                gradEndX = edgeBox.x;
                gradEndY = edgeBox.y;
                gradStartX = edgeBox.x + edgeBox.width;
                gradStartY = edgeBox.y;
                break;
            default:
                throw new IllegalArgumentException("Invalid edge: " + edge);
        }

        if ( gradStartX == gradEndX && gradStartY == gradEndY ) return;

        Color innerColor;
        Color outerColor;
        // Same as shadow color but without alpha:
        Color shadowBackgroundColor = _transparentShadowBackground(shadowConf);
        if (shadowConf.isOutset()) {
            innerColor = shadowConf.color().orElse(Color.BLACK);
            outerColor = shadowBackgroundColor;
        } else {
            innerColor = shadowBackgroundColor;
            outerColor = shadowConf.color().orElse(Color.BLACK);
        }
        LinearGradientPaint edgePaint;
        // distance between start and end of gradient
        float dist = (float) Math.sqrt(
                                    (gradEndX - gradStartX) * (gradEndX - gradStartX) +
                                    (gradEndY - gradStartY) * (gradEndY - gradStartY)
                                );
        float gradientStart = (float) gradientStartOffset / dist;
        if ( gradientStart > 1f || gradientStart < 0f )
            edgePaint = new LinearGradientPaint(
                               gradStartX, gradStartY,
                               gradEndX, gradEndY,
                               new float[] {0f, 1f},
                               new Color[] {innerColor, outerColor}
                           );
        else {
            if ( gradientStart == 1f || gradientStart == 0f ) {
                // The gradient does not really exist, so we can just fill the whole area and then return
                Area edgeArea = new Area(edgeBox);
                g2d.setColor(innerColor);
                if ( shadowConf.isOutset() )
                    edgeArea.intersect(contentArea);
                g2d.fill(edgeArea);
                return;
            }
            edgePaint = new LinearGradientPaint(
                             gradStartX, gradStartY,
                             gradEndX, gradEndY,
                             new float[] {0f, gradientStart, 1f},
                             new Color[] {innerColor, innerColor, outerColor}
                         );
        }

        // We need to clip the edge paint to the edge box
        Area edgeArea = new Area(edgeBox);
        edgeArea.intersect(contentArea);
        if ( edgeClipBox != null )
            edgeArea.intersect(new Area(edgeClipBox));

        Graphics2D edgeG2d = (Graphics2D) g2d.create();
        edgeG2d.setPaint(edgePaint);
        edgeG2d.fill(edgeArea);
        edgeG2d.dispose();
    }

    private static Color _transparentShadowBackground(ShadowConf shadow) {
        return shadow.color()
                    .map(c -> new Color(c.getRed(), c.getGreen(), c.getBlue(), 0))
                    .orElse(new Color(0.5f, 0.5f, 0.5f, 0f));
    }

    private static Outline _insetsFrom(UI.ComponentBoundary boundary, BoxModelConf boxModel) {
        Outline insets = Outline.none();
        switch ( boundary ) {
            case OUTER_TO_EXTERIOR:
                insets = Outline.none(); break;
            case EXTERIOR_TO_BORDER:
                insets = boxModel.margin(); break;
            case BORDER_TO_INTERIOR:
                insets = boxModel.margin().plus(boxModel.widths()); break;
            case INTERIOR_TO_CONTENT:
                insets = boxModel.margin().plus(boxModel.widths()).plus(boxModel.padding()); break;
            case CENTER_TO_CONTENT:
                insets = boxModel.margin().plus(boxModel.widths()).plus(boxModel.padding());
                float deltaWidth = boxModel.size().width().orElse(0f) - boxModel.margin().left().orElse(0f) - boxModel.margin().right().orElse(0f);
                float deltaHeight = boxModel.size().height().orElse(0f) - boxModel.margin().top().orElse(0f) - boxModel.margin().bottom().orElse(0f);
                float halfWidth = deltaWidth / 2f;
                float halfHeight = deltaHeight / 2f;
                insets = insets.plus(Outline.of(halfHeight, halfWidth, halfHeight, halfWidth));
            break;
        }
        return insets;
    }

    private static void _renderGradient(
        final GradientConf    gradient,
        final LayerRenderConf conf,
        final Graphics2D g2d
    ) {
        if ( gradient.colors().length == 1 ) {
            g2d.setColor(gradient.colors()[0]);
            g2d.fill(conf.areas().get(gradient.area()));
        }
        else {
            Paint paint = _createGradientPaint(conf.boxModel(), gradient);
            if ( paint != null ) {
                Shape areaToFill = conf.areas().get(gradient.area());
                g2d.setPaint(paint);
                g2d.fill(areaToFill);
            }
        }
    }

    static @Nullable Paint _createGradientPaint(
        BoxModelConf boxModel,
        GradientConf gradient
    ) {
        final Size dimensions = boxModel.size();
        Outline insets;
        if ( gradient.boundary() == UI.ComponentBoundary.CENTER_TO_CONTENT ) {
            Outline contentIns = _insetsFrom(UI.ComponentBoundary.INTERIOR_TO_CONTENT, boxModel);
            float verticalInset = dimensions.height().orElse(0f) / 2f;
            float horizontalInset = dimensions.width().orElse(0f) / 2f;
            insets = Outline.of(verticalInset, horizontalInset);
            switch ( gradient.span() ) {
                case TOP_TO_BOTTOM:
                    insets = insets.withBottom(contentIns.bottom().orElse(0f));
                    break;
                case BOTTOM_TO_TOP:
                    insets = insets.withTop(contentIns.top().orElse(0f));
                    break;
                case LEFT_TO_RIGHT:
                    insets = insets.withRight(contentIns.right().orElse(0f));
                    break;
                case RIGHT_TO_LEFT:
                    insets = insets.withLeft(contentIns.left().orElse(0f));
                    break;
                case TOP_LEFT_TO_BOTTOM_RIGHT:
                    insets = insets.withBottom(contentIns.bottom().orElse(0f))
                                    .withRight(contentIns.right().orElse(0f));
                    break;
                case BOTTOM_RIGHT_TO_TOP_LEFT:
                    insets = insets.withTop(contentIns.top().orElse(0f))
                                    .withLeft(contentIns.left().orElse(0f));
                    break;
                case TOP_RIGHT_TO_BOTTOM_LEFT:
                    insets = insets.withBottom(contentIns.bottom().orElse(0f))
                                    .withLeft(contentIns.left().orElse(0f));
                    break;
                case BOTTOM_LEFT_TO_TOP_RIGHT:
                    insets = insets.withTop(contentIns.top().orElse(0f))
                                    .withRight(contentIns.right().orElse(0f));
                    break;
                default:
                    break;
            }
        } else {
            insets = _insetsFrom(gradient.boundary(), boxModel);
        }

        final float width  = dimensions.width().orElse(0f)  - ( insets.right().orElse(0f)  + insets.left().orElse(0f) );
        final float height = dimensions.height().orElse(0f) - ( insets.bottom().orElse(0f) + insets.top().orElse(0f) );
        final float realX  = insets.left().orElse(0f) + gradient.offset().x();
        final float realY  = insets.top().orElse(0f)  + gradient.offset().y();

        Point2D.Float corner1;
        Point2D.Float corner2;

        final UI.Span type = gradient.span();
        if ( type.isOneOf(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT) ) {
            corner1 = new Point2D.Float(realX, realY);
            corner2 = new Point2D.Float(realX + width, realY + height);
        } else if ( type.isOneOf(UI.Span.BOTTOM_LEFT_TO_TOP_RIGHT) ) {
            corner1 = new Point2D.Float(realX, realY + height);
            corner2 = new Point2D.Float(realX + width, realY);
        } else if ( type.isOneOf(UI.Span.TOP_RIGHT_TO_BOTTOM_LEFT) ) {
            corner1 = new Point2D.Float(realX + width, realY);
            corner2 = new Point2D.Float(realX, realY + height);
        } else if ( type.isOneOf(UI.Span.BOTTOM_RIGHT_TO_TOP_LEFT) ) {
            corner1 = new Point2D.Float(realX + width, realY + height);
            corner2 = new Point2D.Float(realX, realY);
        } else if ( type == UI.Span.TOP_TO_BOTTOM ) {
            corner1 = new Point2D.Float(realX, realY);
            corner2 = new Point2D.Float(realX, realY + height);
        } else if ( type == UI.Span.LEFT_TO_RIGHT ) {
            corner1 = new Point2D.Float(realX, realY);
            corner2 = new Point2D.Float(realX + width, realY);
        } else if ( type == UI.Span.BOTTOM_TO_TOP ) {
            corner1 = new Point2D.Float(realX, realY + height);
            corner2 = new Point2D.Float(realX, realY);
        } else if ( type == UI.Span.RIGHT_TO_LEFT ) {
            corner1 = new Point2D.Float(realX + width, realY);
            corner2 = new Point2D.Float(realX, realY);
        }
        else {
            log.warn(SwingTree.get().logMarker(), "Unknown gradient type: " + type, new Throwable());
            return null;
        }

        if ( gradient.type() == UI.GradientType.CONIC )
            return _createConicGradientPaint(corner1, corner2, gradient);
        else if ( gradient.type() == UI.GradientType.RADIAL )
            return _createRadialGradientPaint(corner1, corner2, gradient);
        else if ( gradient.span().isDiagonal() )
            return _createDiagonalGradientPaint(corner1, corner2, gradient);
        else
            return _createVerticalOrHorizontalGradientPaint(corner1, corner2, gradient);

    }

    private static Paint _createConicGradientPaint(
        Point2D.Float  corner1,
        Point2D.Float  corner2,
        GradientConf   gradient
    ) {
        final Color[] colors    = gradient.colors();
        final float[] fractions = _fractionsFrom(gradient);
        float rotation = gradient.rotation() + _rotationBetween(corner1, corner2);

        // we normalize the rotation to be between -180 and 180
        rotation = ((((rotation+180f) % 360f + 360f) % 360f)-180f);

        // Now we convert the fractions to rotations:
        for ( int i = 0; i < fractions.length; i++ )
            fractions[i] = (fractions[i] * 360f);// (((((fractions[i] * 360f)+180f) % 360f + 360f) % 360f)-180f);

        return new ConicalGradientPaint(
                        true,
                        corner1,
                        rotation,
                        fractions,
                        colors
                    );
    }

    private static void _renderNoise(
        final NoiseConf       noise,
        final LayerRenderConf conf,
        final Graphics2D g2d
    ) {
        Paint noisePaint = _createNoisePaint(conf.boxModel(), noise);
        Shape areaToFill = conf.areas().get(noise.area());
        g2d.setPaint(noisePaint);
        g2d.fill(areaToFill);
    }

    static Paint _createNoisePaint(
        final BoxModelConf   boxModel,
        final NoiseConf      noise
    ) {
        if ( noise.colors().length == 1 ) {
            return noise.colors()[0];
        } else {
            Size dimensions = boxModel.size();
            Outline insets = Outline.none();
            switch ( noise.boundary() ) {
                case OUTER_TO_EXTERIOR:
                    insets = Outline.none(); break;
                case EXTERIOR_TO_BORDER:
                    insets = boxModel.margin(); break;
                case BORDER_TO_INTERIOR:
                    insets = boxModel.margin().plus(boxModel.widths()); break;
                case INTERIOR_TO_CONTENT:
                    insets = boxModel.margin().plus(boxModel.widths()).plus(boxModel.padding()); break;
                case CENTER_TO_CONTENT:
                    float verticalInset   = dimensions.height().orElse(0f) / 2f;
                    float horizontalInset = dimensions.width().orElse(0f) / 2f;
                    insets = Outline.of(verticalInset, horizontalInset);
            }

            Point2D.Float corner1 = new Point2D.Float(
                                        insets.left().orElse(0f) + noise.offset().x(),
                                        insets.top().orElse(0f) + noise.offset().y()
                                    );

            return _createNoisePaint(corner1, noise);
        }
    }

    private static Paint _createNoisePaint(
        final Point2D.Float  center,
        final NoiseConf      noise
    ) {
        Map<Point2D, NoiseGradientPaint> cachedPaints = _NOISE_PAINT_CACHE.get(noise);
        if ( cachedPaints == null ) {
            cachedPaints = new HashMap<>();
        }
        _NOISE_PAINT_CACHE.put(noise, cachedPaints); // Ensure the cache has a reference to a used noise conf...
        NoiseGradientPaint paint = cachedPaints.get(center);
        if ( paint != null ) {
            return paint;
        }

        final Color[] colors    = noise.colors();
        final float[] fractions = _fractionsFrom(colors, noise.fractions());
        float rotation = noise.rotation();
        Scale scale = noise.scale();
        float scaleX = scale.x();
        float scaleY = scale.y();

        paint = new NoiseGradientPaint(
                        center,
                        scaleX,
                        scaleY,
                        rotation,
                        fractions,
                        colors,
                        noise.function()
                    );
        cachedPaints.put(center, paint);
        return paint;
    }


    /**
     *  Renders a shade from the top left corner to the bottom right corner.
     *
     * @param corner1 The first corner of the shade.
     * @param corner2 The second corner of the shade.
     * @param gradient The shade to render.
     */
    private static Paint _createDiagonalGradientPaint(
        Point2D.Float        corner1,
        Point2D.Float        corner2,
        final GradientConf   gradient
    ) {
        {
            final float cx = ( corner1.x + corner2.x ) / 2;
            final float cy = ( corner1.y + corner2.y ) / 2;
            final float nx = ( corner2.x - corner1.x );
            final float ny = ( corner1.y - corner2.y );
            /*
                The above variables form 2 lines:
                    1. The line with direction n going through corner1.
                    2. The line with direction n going through corner2.
            */

            // project the center (cx,cy) onto the lines:
            corner1 = projectPointOntoLine(corner1, new Point2D.Float(nx, ny), new Point2D.Float(cx, cy));
            corner2 = projectPointOntoLine(corner2, new Point2D.Float(nx, ny), new Point2D.Float(cx, cy));
        }

        final UI.Cycle cycle  = gradient.cycle();
        final Color[]  colors = gradient.colors();

        final float size   = gradient.size();

        final float corner1X = corner1.x;
        final float corner1Y = corner1.y;
        float corner2X = corner2.x;
        float corner2Y = corner2.y;

        float[] fractions = _fractionsFrom(gradient);

        if ( size >= 0 ) {
            float vectorX = corner2X - corner1X;
            float vectorY = corner2Y - corner1Y;
            float vectorLength2 = (float) Math.sqrt(vectorX * vectorX + vectorY * vectorY);
            vectorX = (vectorX / vectorLength2);
            vectorY = (vectorY / vectorLength2);
            corner2X = corner1X + vectorX * size;
            corner2Y = corner1Y + vectorY * size;
        }

        if ( gradient.rotation() % 360f != 0 ) {
            Point2D.Float p1 = new Point2D.Float(corner1X, corner1Y);
            Point2D.Float p2 = new Point2D.Float(corner2X, corner2Y);
            p2 = _rotatePoint(p1, p2, gradient.rotation());
            corner2X = p2.x;
            corner2Y = p2.y;
        }

        if ( colors.length == 2 && gradient.fractions().length == 0 && cycle == UI.Cycle.NONE )
            return new GradientPaint(
                            corner1X, corner1Y, colors[0],
                            corner2X, corner2Y, colors[1]
                        );
        else
            return new LinearGradientPaint(
                            corner1X, corner1Y,
                            corner2X, corner2Y,
                            fractions, colors,
                            _cycleMethodFrom(cycle)
                        );
    }

    private static Paint _createVerticalOrHorizontalGradientPaint(
        Point2D.Float  corner1,
        Point2D.Float  corner2,
        GradientConf   gradient
    ) {
        final UI.Cycle      cycle      = gradient.cycle();
        final Color[]       colors     = gradient.colors();

        final float size   = gradient.size();

        final float corner1X = corner1.x;
        final float corner1Y = corner1.y;
        float corner2X = corner2.x;
        float corner2Y = corner2.y;

        if ( gradient.type() == UI.GradientType.LINEAR ) {
            if ( size >= 0 ) {
                float vectorX = corner2X - corner1X;
                float vectorY = corner2Y - corner1Y;
                float vectorLength = (float) Math.sqrt(vectorX * vectorX + vectorY * vectorY);
                vectorX = (vectorX / vectorLength);
                vectorY = (vectorY / vectorLength);
                corner2X = corner1X + vectorX * size;
                corner2Y = corner1Y + vectorY * size;
            }
        }

        if (
            colors.length == 2 &&
            gradient.fractions().length == 0 &&
            cycle == UI.Cycle.NONE
        ) {
            return new GradientPaint(
                        corner1X, corner1Y, colors[0],
                        corner2X, corner2Y, colors[1]
                    );
        } else {
            float[] fractions = _fractionsFrom(gradient);

            if ( gradient.rotation() % 360f != 0 ) {
                Point2D.Float p1 = new Point2D.Float(corner1X, corner1Y);
                Point2D.Float p2 = new Point2D.Float(corner2X, corner2Y);
                p2 = _rotatePoint(p1, p2, gradient.rotation());
                corner2X = p2.x;
                corner2Y = p2.y;
            }

            return new LinearGradientPaint(
                        corner1X, corner1Y,
                        corner2X, corner2Y,
                        fractions, colors,
                        _cycleMethodFrom(cycle)
                    );

        }
    }

    private static Point2D.Float projectPointOntoLine(Point2D.Float A, Point2D.Float n, Point2D.Float C) {
        Point2D.Float B = new Point2D.Float(A.x + n.x, A.y + n.y);
        float t = ((C.x - A.x) * (B.x - A.x) + (C.y - A.y) * (B.y - A.y)) / ((B.x - A.x) * (B.x - A.x) + (B.y - A.y) * (B.y - A.y));
        return new Point2D.Float(A.x + t * (B.x - A.x), A.y + t * (B.y - A.y));
    }

    private static Paint _createRadialGradientPaint(
        Point2D.Float  corner1,
        Point2D.Float  corner2,
        GradientConf   gradient
    ) {
        final UI.Cycle cycle  = gradient.cycle();
        final Color[]  colors = gradient.colors();

        final float size   = gradient.size();

        final float corner1X = corner1.x;
        final float corner1Y = corner1.y;
        float corner2X = corner2.x;
        float corner2Y = corner2.y;


        float[] fractions = _fractionsFrom(gradient);

        float radius;

        if ( size < 0 )
            radius = (float) Math.sqrt(
                                 (corner2X - corner1X) * (corner2X - corner1X) +
                                 (corner2Y - corner1Y) * (corner2Y - corner1Y)
                             );
        else
            radius = size;

        if ( gradient.focus().equals(Offset.none()) ) {
            if ( colors.length == 2 )
                return new RadialGradientPaint(
                        new Point2D.Float(corner1X, corner1Y),
                        radius,
                        fractions,
                        colors,
                        _cycleMethodFrom(cycle)
                    );
            else
                return new RadialGradientPaint(
                        new Point2D.Float(corner1X, corner1Y),
                        radius,
                        fractions,
                        colors,
                        _cycleMethodFrom(cycle)
                     );
        } else {
            float focusX = corner1X + gradient.focus().x();
            float focusY = corner1Y + gradient.focus().y();

            if ( gradient.rotation() % 360f != 0 ) {
                Point2D.Float p1 = new Point2D.Float(corner1X, corner1Y);
                Point2D.Float p2 = new Point2D.Float(focusX, focusY);
                p2 = _rotatePoint(p1, p2, gradient.rotation());
                focusX = p2.x;
                focusY = p2.y;
            }

            return new RadialGradientPaint(
                    new Point2D.Float(corner1X, corner1Y),
                    radius,
                    new Point2D.Float(focusX, focusY),
                    fractions,
                    colors,
                    _cycleMethodFrom(cycle)
                );
        }
    }

    private static MultipleGradientPaint.CycleMethod _cycleMethodFrom(UI.Cycle cycle) {
        switch (cycle) {
            case NONE:     return MultipleGradientPaint.CycleMethod.NO_CYCLE;
            case REPEAT:   return MultipleGradientPaint.CycleMethod.REPEAT;
            case REFLECT:  return MultipleGradientPaint.CycleMethod.REFLECT;
            default:
                log.warn(SwingTree.get().logMarker(), "Unknown cycle method: " + cycle, new Throwable());
                return MultipleGradientPaint.CycleMethod.NO_CYCLE;
        }
    }

    private static float[] _fractionsFrom(GradientConf style ) {
        Color[] colors   = style.colors();
        float[] fractions = style.fractions();
        return _fractionsFrom(colors, fractions);
    }

    private static float[] _fractionsFrom(
        Color[] colors,
        float[] fractions
    ) {
        if ( fractions.length == colors.length )
            return fractions;
        else if ( fractions.length > colors.length ) {
            float[] newFractions = new float[colors.length];
            System.arraycopy(fractions, 0, newFractions, 0, colors.length);
            return newFractions;
        } else {
            if ( fractions.length == 0 ) {
                fractions = new float[colors.length];
                for ( int i = 0; i < colors.length; i++ )
                    fractions[i] = (float) i / (float) (colors.length - 1);
                return fractions;
            } else {
                float[] newFractions = new float[colors.length];
                System.arraycopy(fractions, 0, newFractions, 0, fractions.length);
                /*
                    Now simply complete th missing fractions by linear interpolation
                    between the last fraction and 1f
                */
                float lastFraction = fractions[fractions.length - 1];
                float step = (1f - lastFraction) / (colors.length - fractions.length);
                for ( int i = fractions.length; i < colors.length; i++ )
                    newFractions[i] = lastFraction + step * (i - fractions.length + 1);
                return newFractions;
            }
        }
    }

    /**
     *  Takes two points {@code p1} and {@code p2} as well as
     *  a {@code rotation} float representing degrees and returns
     *  the point {@code p2} rotated around {@code p1} by {@code rotation} degrees.
     */
    private static Point2D.Float _rotatePoint(
        final Point2D.Float p1,
        final Point2D.Float p2,
        final float rotation
    ) {
        if ( rotation == 0f )
            return p2;
        else if ( rotation % 360f == 0f )
            return p2;

        final double angle = Math.toRadians(rotation);
        final double sin   = Math.sin(angle);
        final double cos   = Math.cos(angle);

        final double x = p2.x - p1.x;
        final double y = p2.y - p1.y;

        final double newX = x * cos - y * sin;
        final double newY = x * sin + y * cos;

        return new Point2D.Float((float) (p1.x + newX), (float) (p1.y + newY));
    }

    /**
     *  Takes 2 points and calculates the rotation
     *  of point 2 around point 1.
     *
     * @param p1 The first point which serves as the center of rotation.
     * @param p2 The second point which is rotated around point 1.
     * @return The rotation in degrees.
     */
    private static final float _rotationBetween(
        final Point2D.Float p1,
        final Point2D.Float p2
    ){
        final double x = p2.x - p1.x;
        final double y = p2.y - p1.y;
        return (float) Math.toDegrees(Math.atan2(y, x));
    }

    private static void _renderImage(
        final LayerRenderConf conf,
        final ImageConf style,
        final Size        componentSize,
        final Graphics2D  g2d
    ) {
        if ( style.primer().isPresent() ) {
            g2d.setColor(style.primer().get());
            g2d.fill(conf.areas().get(style.clipArea()));
        }

        style.image().ifPresent( imageIcon -> {
            final UI.FitComponent fit          = style.fitMode();
            final UI.Placement placement       = style.placement();
            final Outline      padding         = style.padding();
            final Outline      insets          = conf.boxModel().margin().plus(conf.boxModel().widths());
            final int          componentWidth  = componentSize.width().orElse(0f).intValue() - (insets.left().orElse(0f).intValue() + insets.right().orElse(0f).intValue());
            final int          componentHeight = componentSize.height().orElse(0f).intValue() - (insets.top().orElse(0f).intValue()  + insets.bottom().orElse(0f).intValue());
            final int          iconBaseWidth   = imageIcon.getIconWidth();
            final int          iconBaseHeight  = imageIcon.getIconHeight();
            final boolean repeat  = style.repeat();
            final float   opacity = style.opacity();

            final Shape oldClip = g2d.getClip();

            Shape newClip = conf.areas().get(style.clipArea());
            // We merge the new clip with the old one:
            if ( oldClip != null )
                newClip = StyleUtil.intersect( newClip, oldClip );

            g2d.setClip(newClip);

            if ( imageIcon instanceof SvgIcon ) {
                SvgIcon svgIcon = (SvgIcon) imageIcon;
                if ( style.width().isPresent() )
                    svgIcon = svgIcon.withIconWidth(UI.unscale(style.width().get()));
                if ( style.height().isPresent() )
                    svgIcon = svgIcon.withIconHeight(UI.unscale(style.height().get()));
                imageIcon = svgIcon;
            }
            if ( !repeat && imageIcon instanceof SvgIcon ) {
                SvgIcon svgIcon = ((SvgIcon) imageIcon);
                int areaX = insets.left().orElse(0f).intValue();
                int areaY = insets.top().orElse(0f).intValue();
                UI.Placement localPlacement = placement == UI.Placement.UNDEFINED ? svgIcon.getPreferredPlacement() : placement;
                localPlacement = localPlacement == UI.Placement.UNDEFINED ? UI.Placement.CENTER : localPlacement;
                UI.FitComponent localFit = fit == UI.FitComponent.UNDEFINED ? svgIcon.getFitComponent() : fit;
                svgIcon.withFitComponent(localFit)
                        .withPreferredPlacement(localPlacement)
                        .paintIcon(null, g2d, Bounds.of(areaX, areaY, componentWidth, componentHeight), style.offset());
            } else {
                int imgWidth  = style.width().orElse(iconBaseWidth);
                int imgHeight = style.height().orElse(iconBaseHeight);

                if ( fit != UI.FitComponent.NO && fit != UI.FitComponent.UNDEFINED ) {
                    if ( fit == UI.FitComponent.WIDTH_AND_HEIGHT ) {
                        imgWidth  = style.width().orElse(componentWidth);
                        imgHeight = style.height().orElse(componentHeight);
                    }
                    if (
                            fit == UI.FitComponent.WIDTH ||
                                    (fit == UI.FitComponent.MAX_DIM && componentWidth > componentHeight)  ||
                                    (fit == UI.FitComponent.MIN_DIM && componentWidth < componentHeight )
                    ) {
                        imgWidth = style.width().orElse(componentWidth);
                        double aspectRatio = (double) iconBaseHeight / (double) iconBaseWidth;
                        // We preserve the aspect ratio:
                        imgHeight = (int) (imgWidth * aspectRatio);
                    } if (
                            fit == UI.FitComponent.HEIGHT ||
                                    (fit == UI.FitComponent.MAX_DIM && componentWidth < componentHeight) ||
                                    (fit == UI.FitComponent.MIN_DIM && componentWidth > componentHeight )
                    ) {
                        imgHeight = style.height().orElse(componentHeight);
                        double aspectRatio = (double) iconBaseWidth / (double) iconBaseHeight;
                        // We preserve the aspect ratio:
                        imgWidth = (int) (imgHeight * aspectRatio);
                    }
                    imgWidth  = imgWidth  >= 0 ? imgWidth  : componentWidth;
                    imgHeight = imgHeight >= 0 ? imgHeight : componentHeight;
                }
                int x = style.horizontalOffset();
                int y = style.verticalOffset();
                // We apply the insets:
                x += insets.left().orElse(0f).intValue();
                y += insets.top().orElse(0f).intValue();

                switch ( placement ) {
                    case TOP:
                        x += (componentWidth - imgWidth) / 2;
                        break;
                    case LEFT:
                        y += (componentHeight - imgHeight) / 2;
                        break;
                    case BOTTOM:
                        x += (componentWidth - imgWidth) / 2;
                        y += componentHeight - imgHeight;
                        break;
                    case RIGHT:
                        x += componentWidth - imgWidth;
                        y += (componentHeight - imgHeight) / 2;
                        break;
                    case TOP_LEFT: break;
                    case TOP_RIGHT:
                        x += componentWidth - imgWidth;
                        break;
                    case BOTTOM_LEFT:
                        y += componentHeight - imgHeight;
                        break;
                    case BOTTOM_RIGHT:
                        x += componentWidth - imgWidth;
                        y += componentHeight - imgHeight;
                        break;
                    case CENTER:
                    case UNDEFINED:
                        x += (componentWidth - imgWidth) / 2;
                        y += (componentHeight - imgHeight) / 2;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown placement: " + placement);
                }
                x += padding.left().orElse(0f).intValue();
                y += padding.top().orElse(0f).intValue();
                imgWidth  -= (padding.left().orElse(0f).intValue() + padding.right().orElse(0f).intValue());
                imgHeight -= (padding.top().orElse(0f).intValue()  + padding.bottom().orElse(0f).intValue());

                Image image;
                if ( imageIcon instanceof SvgIcon) {
                    SvgIcon svgIcon = (SvgIcon) imageIcon;
                    svgIcon = svgIcon.withIconWidth(imgWidth);
                    svgIcon = svgIcon.withIconHeight(imgHeight);
                    image   = svgIcon.getImage(); // This will render the SVGIcon with the new size
                }
                else
                    image = imageIcon.getImage();

                Composite oldComposite = g2d.getComposite();

                try {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
                    if (repeat) {
                        Paint oldPaint = g2d.getPaint();
                        try {
                            g2d.setPaint(new TexturePaint((BufferedImage) image, new Rectangle(x, y, imgWidth, imgHeight)));
                            g2d.fill(conf.areas().get(UI.ComponentArea.BODY));
                        } finally {
                            g2d.setPaint(oldPaint);
                        }
                    }
                    else
                        g2d.drawImage(image, x, y, imgWidth, imgHeight, null);

                } finally {
                    g2d.setComposite(oldComposite);
                }
            }
            g2d.setClip(oldClip);
        });
    }

    private static void _renderText(
        final TextConf        text,
        final LayerRenderConf conf,
        final Graphics2D      g2d
    ) {
        if ( text.content().isEmpty() )
            return;

        final BoxModelConf boxModel = conf.boxModel();

        final Font initialFont = g2d.getFont();
        final Shape oldClip = g2d.getClip();

        final String               textToRender      = text.content();
        final UI.ComponentArea     clipArea          = text.clipArea();
        final UI.ComponentBoundary placementBoundary = text.placementBoundary();
        final UI.Placement         placement         = text.placement();
        final Offset               offset            = text.offset();

        Font font = Optional.ofNullable(initialFont).orElse(new Font(Font.DIALOG, Font.PLAIN, UI.scale(12)));
        font = text.fontConf().createDerivedFrom(font, boxModel).orElse(font);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics(font);
        Rectangle2D rect = fm.getStringBounds(textToRender, g2d);

        Outline insets = _insetsFrom(placementBoundary, boxModel);
        float x = insets.left().orElse(0f); // Top left is always the starting point
        float y = insets.top().orElse(0f) ;
        {
            float leftX = insets.left().orElse(0f);
            float topY = insets.top().orElse(0f);
            float localWidth = boxModel.size().width().orElse(0f) - (leftX + insets.right().orElse(0f));
            float localHeight = boxModel.size().height().orElse(0f) - (topY + insets.bottom().orElse(0f));
            float rightX = leftX + localWidth;
            float bottomY = topY + localHeight;

            switch (placement) {
                case CENTER:
                    float centerX = leftX + localWidth / 2f;
                    float centerY = topY + localHeight / 2f;
                    x = centerX - (float) rect.getWidth() / 2f;
                    y = centerY - (float) rect.getHeight() / 2f;
                break;
                case TOP:
                    x = leftX + (localWidth - (float) rect.getWidth()) / 2f;
                    y = topY;
                break;
                case LEFT:
                    x = leftX;
                    y = topY + (localHeight - (float) rect.getHeight()) / 2f;
                break;
                case BOTTOM:
                    x = leftX + (localWidth - (float) rect.getWidth()) / 2f;
                    y = bottomY - (float) rect.getHeight();
                break;
                case RIGHT:
                    x = rightX - (float) rect.getWidth();
                    y = topY + (localHeight - (float) rect.getHeight()) / 2f;
                break;
                case TOP_LEFT:
                    x = leftX;
                    y = topY;
                break;
                case TOP_RIGHT:
                    x = rightX - (float) rect.getWidth();
                    y = topY;
                break;
                case BOTTOM_LEFT:
                    x = leftX;
                    y = bottomY - (float) rect.getHeight();
                break;
                case BOTTOM_RIGHT:
                    x = rightX - (float) rect.getWidth();
                    y = bottomY - (float) rect.getHeight();
                break;
                case UNDEFINED:
                break;
            }
        }

        try {
            x += (offset.x() - (float) rect.getX());
            y += (offset.y() - (float) rect.getY());
            g2d.setClip(conf.areas().get(clipArea));
            // Render the text:
            g2d.drawString(textToRender, x, y);
        } finally {
            g2d.setFont(initialFont);
            g2d.setClip(oldClip);
        }
    }

    private static void _executeUserPainters( UI.Layer layer, LayerRenderConf conf, Graphics2D g2d ) {
        List<PainterConf> painters = conf.layer().painters().sortedByNames();

        if ( painters.isEmpty() )
            return;

        // We remember the current clip so that we can reset it later:
        final Shape currentClip = g2d.getClip();

        UI.ComponentArea allowedArea = null;
        Shape localClip = null;

        for ( PainterConf painterConf : painters ) {
            Painter backgroundPainter = painterConf.painter();

            if ( backgroundPainter == Painter.none() )
                continue;

            // We remember if antialiasing was enabled before we render:
            boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;
            // We remember the current transform and clip so that we can reset them after each painter:
            AffineTransform currentTransform = new AffineTransform(g2d.getTransform());

            if ( allowedArea == null || allowedArea != painterConf.clipArea() ) {
                allowedArea = painterConf.clipArea();
                localClip = conf.areas().get(allowedArea);
                localClip = StyleUtil.intersect(localClip, currentClip);
            }
            g2d.setClip(localClip);
            float uiScale = UI.scale();
            if ( uiScale != 1f )
                g2d.scale(uiScale, uiScale);

            try {
                backgroundPainter.paint(g2d);
            } catch (Exception e) {
                log.warn(
                    "An exception occurred while executing painter '" + backgroundPainter + "' " +
                    "on layer '" + layer + "' for style '" + conf + "' ",
                    e
                );
                /*
                    If exceptions happen in user provided painters, we don't want to
                    mess up the rendering of the rest of the component, so we catch them here!

                    We log as warning because exceptions during rendering are not considered
                    as harmful as elsewhere!

                    Hi there! If you are reading this, you are probably a developer using the SwingTree
                    library, thank you for using it! Good luck finding out what went wrong! :)
                */
            } finally {
                // We do not know what the painter did to the graphics transform, so we reset it:
                g2d.setTransform(currentTransform);

                // Reset antialiasing to its previous state:
                g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );
            }
        }
        // We are done with the painters, so we can reset the clip:
        g2d.setClip(currentClip);
    }

    static void renderParentFilter(
        FilterConf    filterConf,
        BufferedImage parentRendering,
        Graphics2D    g2d,
        int offsetX,
        int offsetY,
        BoxModelConf boxModelConf
    ) {
        final Size       size   = boxModelConf.size();
        final float      width  = size.width().orElse(0f);
        final float      height = size.height().orElse(0f);
        final Offset     center = filterConf.offset();
        final Scale      scale  = filterConf.scale();
        final KernelConf kernel = filterConf.kernel();
        final float      blur   = filterConf.blur();

        BufferedImage filtered = parentRendering;

        if ( !center.equals(Offset.none()) || !scale.equals(Scale.none()) ) {
            if ( scale.equals(Scale.none()) ) {
                offsetX += (int) center.x();
                offsetY += (int) center.y();
            } else {
                AffineTransform at = new AffineTransform();
                float vx = center.x() + offsetX + width / 2f;
                float vy = center.y() + offsetY + height / 2f;
                at.translate(vx, vy);
                at.scale(scale.x(), scale.y());
                at.translate(-vx, -vy);
                AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
                filtered = scaleOp.filter(filtered, null);
            }
        }

        if ( !kernel.equals(KernelConf.none()) ) {
            Kernel awtKernel = kernel.toAwtKernel();
            ConvolveOp convolve = new ConvolveOp(awtKernel, ConvolveOp.EDGE_NO_OP, null);
            filtered = convolve.filter(filtered, null);
        }

        if ( blur > 0 ) {
            Kernel blurKernelHorizontal = _makeKernel(blur, false);
            ConvolveOp blurOp = new ConvolveOp(blurKernelHorizontal, ConvolveOp.EDGE_NO_OP, null);
            BufferedImage blurred = blurOp.filter(filtered, null);
            Kernel blurKernelVertical = _makeKernel(blur, true);
            blurOp = new ConvolveOp(blurKernelVertical, ConvolveOp.EDGE_NO_OP, null);
            filtered = blurOp.filter(blurred, filtered);
        }

        Shape oldClip = g2d.getClip();
        try {
            ComponentAreas areas = boxModelConf.areas();
            Shape newClip = areas.get(filterConf.area());
            g2d.setClip(newClip);
            g2d.drawImage(filtered, -offsetX, -offsetY, null);
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "Failed to successfully render filtered parent buffer!", e);
        } finally {
            g2d.setClip(oldClip);
        }
    }

    private static Kernel _makeKernel( float radius, boolean transpose ) {
        final int maxRadius = (int)Math.ceil(radius);
        final int rows = maxRadius * 2 + 1;
        final float[] matrix = new float[rows];
        final float sigma = radius / 3;
        final float sigma22 = 2*sigma*sigma;
        final float sigmaPi2 = (float) ( 2 * Math.PI * sigma );
        final float sqrtSigmaPi2 = (float)Math.sqrt(sigmaPi2);
        final float radius2 = radius*radius;

        float total = 0;
        int   index = 0;

        for (int row = -maxRadius; row <= maxRadius; row++) {
            float distance = row*row;
            if (distance > radius2)
                matrix[index] = 0;
            else
                matrix[index] = (float)Math.exp(-distance/sigma22) / sqrtSigmaPi2;
            total += matrix[index];
            index++;
        }
        for ( int i = 0; i < rows; i++ )
            matrix[i] /= total;

        return new Kernel( transpose ? 1 : rows, transpose ? rows : 1, matrix );
    }
}

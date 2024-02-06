package swingtree.style;

import com.github.weisj.jsvg.geometry.size.FloatSize;
import org.slf4j.Logger;
import swingtree.UI;
import swingtree.api.Painter;
import swingtree.layout.Bounds;
import swingtree.layout.Size;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
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

    private StyleRenderer() {} // Un-instantiable!


    public static void renderStyleOn( UI.Layer layer, LayerRenderConf conf, Graphics2D g2d )
    {
        // First up, we render things unique to certain layers:

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
        conf.baseColors().borderColor().ifPresent(color -> {
            _drawBorder( conf, color, g2d);
        });

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
                if ( gradient.colors().length == 1 ) {
                    g2d.setColor(gradient.colors()[0]);
                    g2d.fill(conf.areas().get(UI.ComponentArea.BODY));
                }
                else {
                    Outline insets = Outline.none();
                    switch ( gradient.boundary() ) {
                        case OUTER_TO_EXTERIOR:
                            insets = Outline.none(); break;
                        case EXTERIOR_TO_BORDER:
                            insets = conf.boxModel().margin(); break;
                        case BORDER_TO_INTERIOR:
                            insets = conf.boxModel().margin().plus(conf.boxModel().widths()); break;
                        case INTERIOR_TO_CONTENT:
                            insets = conf.boxModel().margin().plus(conf.boxModel().widths()).plus(conf.boxModel().padding()); break;
                        case CENTER_TO_CONTENT:
                            float verticalInset = conf.boxModel().size().height().orElse(0f) / 2f;
                            float horizontalInset = conf.boxModel().size().width().orElse(0f) / 2f;
                            insets = Outline.of(verticalInset, horizontalInset);
                            break;
                    }

                    final Size dimensions = conf.boxModel().size();

                    final float width  = dimensions.width().orElse(0f)  - ( insets.right().orElse(0f)  + insets.left().orElse(0f) );
                    final float height = dimensions.height().orElse(0f) - ( insets.bottom().orElse(0f) + insets.top().orElse(0f) );
                    final float realX  = insets.left().orElse(0f) + gradient.offset().x();
                    final float realY  = insets.top().orElse(0f)  + gradient.offset().y();
                    Bounds bounds = Bounds.of(realX, realY, width, height);

                    if ( gradient.transition().isDiagonal() )
                        _renderDiagonalGradient(g2d, bounds, gradient, conf.areas().get(gradient.area()));
                    else
                        _renderVerticalOrHorizontalGradient(g2d, bounds, gradient, conf.areas().get(gradient.area()));
                }
            }

        // 3. Shadows, which are simple gradient based drop shadows that can go inwards or outwards
        for ( ShadowConf shadow : conf.layer().shadows().sortedByNames() )
            _renderShadows(conf, shadow, g2d);

        // 4. Painters, which are provided by the user and can be anything
        List<PainterConf> painters = conf.layer().painters().sortedByNames();

        if ( !painters.isEmpty() )
        {
            for ( PainterConf painterConf : painters )
            {
                Painter backgroundPainter = painterConf.painter();

                if ( backgroundPainter == Painter.none() )
                    break;

                conf.paintClippedTo( painterConf.clipArea(), g2d, () -> {
                    // We remember the current transform and clip so that we can reset them after each painter:
                    AffineTransform currentTransform = new AffineTransform(g2d.getTransform());
                    Shape           currentClip      = g2d.getClip();

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
                        // We do not know what the painter did to the graphics object, so we reset it:
                        g2d.setTransform(currentTransform);
                        g2d.setClip(currentClip);
                    }
                });
            }
        }
        // And that's it! We have rendered a style layer!
    }

    private static void _drawBorder(LayerRenderConf conf, Color color, Graphics2D g2d )
    {
        if ( !Outline.none().equals(conf.boxModel().widths()) ) {
            try {
                Area borderArea = conf.areas().get(UI.ComponentArea.BORDER);
                g2d.setColor(color);
                g2d.fill(borderArea);
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
            baseArea = ComponentAreas.calculateBaseArea(conf.boxModel(), artifactAdjustment, artifactAdjustment, artifactAdjustment, artifactAdjustment);
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
        ShadowConf shadowConf,
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

    /**
     *  Renders a shade from the top left corner to the bottom right corner.
     *
     * @param g2d The graphics object to render to.
     * @param bounds The margin of the component.
     * @param gradient The shade to render.
     */
    private static void _renderDiagonalGradient(
        Graphics2D    g2d,
        Bounds        bounds,
        GradientConf  gradient,
        Area          specificArea
    ) {
        UI.Transition  type   = gradient.transition();
        final UI.Cycle cycle  = gradient.cycle();
        final Color[]  colors = gradient.colors();

        final float width  = bounds.size().width().orElse(0f);
        final float height = bounds.size().height().orElse(0f);
        final float realX  = bounds.location().x();
        final float realY  = bounds.location().y();
        final float size   = gradient.size();

        float corner1X;
        float corner1Y;
        float corner2X;
        float corner2Y;
        float diagonalCorner1X;
        float diagonalCorner1Y;
        float diagonalCorner2X;
        float diagonalCorner2Y;

        if ( type.isOneOf(UI.Transition.TOP_LEFT_TO_BOTTOM_RIGHT, UI.Transition.BOTTOM_RIGHT_TO_TOP_LEFT) ) {
            corner1X = realX;
            corner1Y = realY;
            corner2X = realX + width;
            corner2Y = realY + height;
            diagonalCorner1X = realX;
            diagonalCorner1Y = realY + height;
            diagonalCorner2X = realX + width;
            diagonalCorner2Y = realY;
            if ( type == UI.Transition.BOTTOM_RIGHT_TO_TOP_LEFT ) {
                // We simplify flip the corners:
                float tempX = corner1X;
                float tempY = corner1Y;
                corner1X = corner2X;
                corner1Y = corner2Y;
                corner2X = tempX;
                corner2Y = tempY;
                tempX = diagonalCorner1X;
                tempY = diagonalCorner1Y;
                diagonalCorner1X = diagonalCorner2X;
                diagonalCorner1Y = diagonalCorner2Y;
                diagonalCorner2X = tempX;
                diagonalCorner2Y = tempY;
            }
        } else if ( type.isOneOf(UI.Transition.BOTTOM_LEFT_TO_TOP_RIGHT, UI.Transition.TOP_RIGHT_TO_BOTTOM_LEFT) ) {
            corner1X = realX + width;
            corner1Y = realY;
            corner2X = realX;
            corner2Y = realY + height;
            diagonalCorner1X = realX + width;
            diagonalCorner1Y = realY + height;
            diagonalCorner2X = realX;
            diagonalCorner2Y = realY;
            if ( type == UI.Transition.TOP_RIGHT_TO_BOTTOM_LEFT ) {
                // We simplify flip the corners:
                float tempX = corner1X;
                float tempY = corner1Y;
                corner1X = corner2X;
                corner1Y = corner2Y;
                corner2X = tempX;
                corner2Y = tempY;
                tempX = diagonalCorner1X;
                tempY = diagonalCorner1Y;
                diagonalCorner1X = diagonalCorner2X;
                diagonalCorner1Y = diagonalCorner2Y;
                diagonalCorner2X = tempX;
                diagonalCorner2Y = tempY;
            }
        }
        else {
            log.warn("Invalid transition type: " + type, new Throwable());
            return;
        }

        float diagonalCenterX = (diagonalCorner1X + diagonalCorner2X) / 2;
        float diagonalCenterY = (diagonalCorner1Y + diagonalCorner2Y) / 2;

        float[] fractions = _fractionsFrom(gradient);

        if ( gradient.type() == UI.GradientType.RADIAL ) {
            float radius;

            if ( size < 0 )
                radius = (float) Math.sqrt(
                                     (diagonalCenterX - corner1X) * (diagonalCenterX - corner1X) +
                                     (diagonalCenterY - corner1Y) * (diagonalCenterY - corner1Y)
                                 );
            else
                radius = size;

            if ( gradient.focus().equals(Offset.none()) ) {
                if ( colors.length == 2 )
                    g2d.setPaint(new RadialGradientPaint(
                            new Point2D.Float(corner1X, corner1Y),
                            radius,
                            fractions,
                            colors,
                            _cycleMethodFrom(cycle)
                        ));
                else
                    g2d.setPaint(new RadialGradientPaint(
                            new Point2D.Float(corner1X, corner1Y),
                            radius,
                            fractions,
                            colors,
                            _cycleMethodFrom(cycle)
                        ));
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

                g2d.setPaint(new RadialGradientPaint(
                        new Point2D.Float(corner1X, corner1Y),
                        radius,
                        new Point2D.Float(focusX, focusY),
                        fractions,
                        colors,
                        _cycleMethodFrom(cycle)
                    ));
            }
        } else if ( gradient.type() == UI.GradientType.LINEAR ) {
            float vector1X = diagonalCorner1X - diagonalCenterX;
            float vector1Y = diagonalCorner1Y - diagonalCenterY;
            float vector2X = diagonalCorner2X - diagonalCenterX;
            float vector2Y = diagonalCorner2Y - diagonalCenterY;

            float vectorLength = (float) Math.sqrt(vector1X * vector1X + vector1Y * vector1Y);
            vector1X = (vector1X / vectorLength);
            vector1Y = (vector1Y / vectorLength);

            vectorLength = (float) Math.sqrt(vector2X * vector2X + vector2Y * vector2Y);
            vector2X = (vector2X / vectorLength);
            vector2Y = (vector2Y / vectorLength);

            float nVector1X = -vector1Y;
            float nVector1Y =  vector1X;
            float nVector2X = -vector2Y;
            float nVector2Y =  vector2X;

            float distance1 = (corner1X - diagonalCenterX) * nVector1X + (corner1Y - diagonalCenterY) * nVector1Y;
            float distance2 = (corner2X - diagonalCenterX) * nVector2X + (corner2Y - diagonalCenterY) * nVector2Y;

            float gradientStartX = (diagonalCenterX + nVector1X * distance1);
            float gradientStartY = (diagonalCenterY + nVector1Y * distance1);
            float gradientEndX   = (diagonalCenterX + nVector2X * distance2);
            float gradientEndY   = (diagonalCenterY + nVector2Y * distance2);

            if ( size >= 0 ) {
                float vectorX = gradientEndX - gradientStartX;
                float vectorY = gradientEndY - gradientStartY;
                float vectorLength2 = (float) Math.sqrt(vectorX * vectorX + vectorY * vectorY);
                vectorX = (vectorX / vectorLength2);
                vectorY = (vectorY / vectorLength2);
                gradientEndX = gradientStartX + vectorX * size;
                gradientEndY = gradientStartY + vectorY * size;
            }

            if ( gradient.rotation() % 360f != 0 ) {
                Point2D.Float p1 = new Point2D.Float(gradientStartX, gradientStartY);
                Point2D.Float p2 = new Point2D.Float(gradientEndX, gradientEndY);
                p2 = _rotatePoint(p1, p2, gradient.rotation());
                gradientEndX = p2.x;
                gradientEndY = p2.y;
            }

            if ( colors.length == 2 && gradient.fractions().length == 0 && cycle == UI.Cycle.NONE )
                g2d.setPaint(new GradientPaint(
                                gradientStartX, gradientStartY, colors[0],
                                gradientEndX, gradientEndY, colors[1]
                            ));
            else
                g2d.setPaint(new LinearGradientPaint(
                                gradientStartX, gradientStartY,
                                gradientEndX, gradientEndY,
                                fractions, colors,
                                _cycleMethodFrom(cycle)
                            ));
        }
        g2d.fill(specificArea);
    }

    private static void _renderVerticalOrHorizontalGradient(
        Graphics2D    g2d,
        Bounds        bounds,
        GradientConf gradient,
        Area          specificArea
    ) {
        final UI.Transition type       = gradient.transition();
        final UI.Cycle      cycle      = gradient.cycle();
        final Color[]       colors     = gradient.colors();

        final float width  = bounds.size().width().orElse(0f);
        final float height = bounds.size().height().orElse(0f);
        final float realX  = bounds.location().x();
        final float realY  = bounds.location().y();
        final float size   = gradient.size();

        float corner1X;
        float corner1Y;
        float corner2X;
        float corner2Y;

        if ( type == UI.Transition.TOP_TO_BOTTOM ) {
            corner1X = realX;
            corner1Y = realY;
            corner2X = realX;
            corner2Y = realY + height;
        } else if ( type == UI.Transition.LEFT_TO_RIGHT ) {
            corner1X = realX;
            corner1Y = realY;
            corner2X = realX + width;
            corner2Y = realY;
        } else if ( type == UI.Transition.BOTTOM_TO_TOP ) {
            corner1X = realX;
            corner1Y = realY + height;
            corner2X = realX;
            corner2Y = realY;
        } else if ( type == UI.Transition.RIGHT_TO_LEFT ) {
            corner1X = realX + width;
            corner1Y = realY;
            corner2X = realX;
            corner2Y = realY;
        }
        else {
            log.warn("Unknown gradient type: " + type, new Throwable());
            return;
        }

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

        float radius = size;

        if ( gradient.type() == UI.GradientType.RADIAL ) {
            if ( size < 0 )
                radius = (float) Math.sqrt(
                                        (corner2X - corner1X) * (corner2X - corner1X) +
                                        (corner2Y - corner1Y) * (corner2Y - corner1Y)
                                    );
        }

        if (
            colors.length == 2 &&
            gradient.fractions().length == 0 &&
            gradient.type() == UI.GradientType.LINEAR &&
            cycle == UI.Cycle.NONE
        ) {
            g2d.setPaint(
                    new GradientPaint(
                        corner1X, corner1Y, colors[0],
                        corner2X, corner2Y, colors[1]
                    )
                );
        } else if ( colors.length == 2 && gradient.fractions().length == 0 && gradient.type() == UI.GradientType.RADIAL ) {
            if ( gradient.focus().equals(Offset.none()) ) {
                g2d.setPaint(new RadialGradientPaint(
                            new Point2D.Float(corner1X, corner1Y),
                            radius,
                            new float[] {0f, 1f},
                            colors,
                            _cycleMethodFrom(cycle)
                        ));
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

                g2d.setPaint(new RadialGradientPaint(
                            new Point2D.Float(corner1X, corner1Y),
                            radius,
                            new Point2D.Float(focusX, focusY),
                            new float[] {0f, 1f},
                            colors,
                            _cycleMethodFrom(cycle)
                        ));
            }
        } else {
            float[] fractions = _fractionsFrom(gradient);

            if ( gradient.rotation() % 360f != 0 ) {
                Point2D.Float p1 = new Point2D.Float(corner1X, corner1Y);
                Point2D.Float p2 = new Point2D.Float(corner2X, corner2Y);
                p2 = _rotatePoint(p1, p2, gradient.rotation());
                corner2X = p2.x;
                corner2Y = p2.y;
            }

            if ( gradient.type() == UI.GradientType.LINEAR ) {
                g2d.setPaint(
                    new LinearGradientPaint(
                            corner1X, corner1Y,
                            corner2X, corner2Y,
                            fractions, colors,
                            _cycleMethodFrom(cycle)
                        )
                );
            } else if ( gradient.type() == UI.GradientType.RADIAL ) {
                if ( gradient.focus().equals(Offset.none()) ) {
                    g2d.setPaint(new RadialGradientPaint(
                                new Point2D.Float(corner1X, corner1Y),
                                radius,
                                fractions,
                                colors,
                                _cycleMethodFrom(cycle)
                            ));
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

                    g2d.setPaint(new RadialGradientPaint(
                                new Point2D.Float(corner1X, corner1Y),
                                radius,
                                new Point2D.Float(focusX, focusY),
                                fractions,
                                colors,
                                _cycleMethodFrom(cycle)
                            ));
                }
            }
            else {
                log.warn("Unknown gradient type: " + gradient.type(), new Throwable());
                return;
            }
        }
        g2d.fill(specificArea);
    }

    private static MultipleGradientPaint.CycleMethod _cycleMethodFrom(UI.Cycle cycle) {
        switch (cycle) {
            case NONE:     return MultipleGradientPaint.CycleMethod.NO_CYCLE;
            case REPEAT:   return MultipleGradientPaint.CycleMethod.REPEAT;
            case REFLECT:  return MultipleGradientPaint.CycleMethod.REFLECT;
            default:
                log.warn("Unknown cycle method: " + cycle, new Throwable());
                return MultipleGradientPaint.CycleMethod.NO_CYCLE;
        }
    }

    private static float[] _fractionsFrom(GradientConf style ) {
        Color[] colors = style.colors();
        float[] fractions = style.fractions();

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
        final float         rotation
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

    private static void _renderImage(
        LayerRenderConf conf,
        ImageConf style,
        Size        componentSize,
        Graphics2D  g2d
    ) {
        if ( style.primer().isPresent() ) {
            g2d.setColor(style.primer().get());
            g2d.fill(conf.areas().get(style.clipArea()));
        }

        style.image().ifPresent( imageIcon -> {
            final UI.FitComponent fit          = style.fitMode();
            final UI.Placement placement       = style.placement();
            final Outline      padding         = style.padding();
            final int          componentWidth  = componentSize.width().orElse(0f).intValue();
            final int          componentHeight = componentSize.height().orElse(0f).intValue();

            int imgWidth  = style.width().orElse(imageIcon.getIconWidth());
            int imgHeight = style.height().orElse(imageIcon.getIconHeight());

            if ( fit != UI.FitComponent.NO ) {
                if ( imageIcon instanceof SvgIcon) {
                    // The SvgIcon does the fitting...
                    imgWidth  = style.width().orElse(componentWidth);
                    imgHeight = style.height().orElse(componentHeight);
                } else {
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
                        double aspectRatio = (double) imageIcon.getIconHeight() / (double) imageIcon.getIconWidth();
                        // We preserve the aspect ratio:
                        imgHeight = (int) (imgWidth * aspectRatio);
                    } if (
                        fit == UI.FitComponent.HEIGHT ||
                        (fit == UI.FitComponent.MAX_DIM && componentWidth < componentHeight) ||
                        (fit == UI.FitComponent.MIN_DIM && componentWidth > componentHeight )
                    ) {
                        imgHeight = style.height().orElse(componentHeight);
                        double aspectRatio = (double) imageIcon.getIconWidth() / (double) imageIcon.getIconHeight();
                        // We preserve the aspect ratio:
                        imgWidth = (int) (imgHeight * aspectRatio);
                    }
                }
                imgWidth  = imgWidth  >= 0 ? imgWidth  : componentWidth;
                imgHeight = imgHeight >= 0 ? imgHeight : componentHeight;
            }
            if ( imageIcon instanceof SvgIcon ) {
                SvgIcon   svgIcon = (SvgIcon) imageIcon;
                FloatSize size    = svgIcon.getSvgSize();
                imgWidth  = imgWidth  >= 0 ? imgWidth  : (int) size.width;
                imgHeight = imgHeight >= 0 ? imgHeight : (int) size.height;
            }
            int x = style.horizontalOffset();
            int y = style.verticalOffset();

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
            // We apply the padding:
            x += padding.left().orElse(0f).intValue();
            y += padding.top().orElse(0f).intValue();
            imgWidth  -= padding.left().orElse(0f).intValue() + padding.right().orElse(0f).intValue();
            imgHeight -= padding.top().orElse(0f).intValue()  + padding.bottom().orElse(0f).intValue();
            if ( imageIcon instanceof SvgIcon ) {
                SvgIcon svgIcon = (SvgIcon) imageIcon;
                if ( imgWidth > -1 && svgIcon.getIconWidth() < 0 )
                    svgIcon = svgIcon.withIconWidth(imgWidth);
                if ( imgHeight > -1 && svgIcon.getIconHeight() < 0 )
                    svgIcon = svgIcon.withIconHeight(imgHeight);
                imageIcon = svgIcon;
            }

            final boolean repeat  = style.repeat();
            final float   opacity = style.opacity();

            final Shape oldClip = g2d.getClip();

            Shape newClip = conf.areas().get(style.clipArea());
            // We merge the new clip with the old one:
            if ( newClip != null && oldClip != null )
                newClip = StyleUtility.intersect( newClip, oldClip );

            g2d.setClip(newClip);

            if ( !repeat && imageIcon instanceof SvgIcon ) {
                SvgIcon svgIcon = ((SvgIcon) imageIcon).withFitComponent(fit);
                svgIcon.withPreferredPlacement(UI.Placement.CENTER)
                        .paintIcon(null, g2d, x, y, imgWidth, imgHeight);
            }
            else
            {
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

}

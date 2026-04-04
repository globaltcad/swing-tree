package swingtree.style;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Pair;
import sprouts.Tuple;
import swingtree.SwingTree;
import swingtree.UI;
import swingtree.api.Painter;
import swingtree.layout.Bounds;
import swingtree.layout.Size;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.*;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.BreakIterator;
import java.util.*;
import java.util.List;

/**
 *  A stateless un-instantiable utility class that renders the style of a component
 *  using the immutable {@link LayerRenderConf} object containing the essential state
 *  needed for rendering, like for example the current {@link Bounds} and {@link StyleConf}
 *  of a particular component.
 */
final class StyleRenderer
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(StyleRenderer.class);
    private static final Map<Pooled<NoiseConf>, Map<Point2D,NoiseGradientPaint>> _NOISE_PAINT_CACHE = new WeakHashMap<>();

    private StyleRenderer() {} // Un-instantiable!

    /**
     *  Holds a single laid-out text line together with the obstacle-free horizontal region
     *  it was fitted into.  Both {@code regionX} and {@code regionWidth} are in component
     *  coordinates and are used by the renderer to position the line, including alignment
     *  (LEFT / CENTER / RIGHT) within the available region.
     *  <p>
     *  When obstacles split a visual line into more than one free interval, every interval
     *  beyond the first is captured as an extra {@link Segment} in {@code extraSegments}.
     *  All segments share the same baseline (y is not advanced between them).
     *  <p>
     *  A {@code null} {@code layout} represents a blank / empty line — the region fields
     *  still carry the full text-bounds width so that the blank contributes the correct height.
     */
    static final class LayoutLine {

        /** A secondary text fragment placed in one of the additional free intervals on this line. */
        static final class Segment {
            final TextLayout layout;
            final float regionX;
            final float regionWidth;
            Segment(TextLayout layout, float regionX, float regionWidth) {
                this.layout      = layout;
                this.regionX     = regionX;
                this.regionWidth = regionWidth;
            }
        }

        final @Nullable TextLayout  layout;
        final float                 regionX;
        final float                 regionWidth;
        /** Additional same-baseline fragments produced when obstacles split the line. */
        final List<Segment>         extraSegments;

        LayoutLine(@Nullable TextLayout layout, float regionX, float regionWidth) {
            this(layout, regionX, regionWidth, Collections.emptyList());
        }

        LayoutLine(@Nullable TextLayout layout, float regionX, float regionWidth, List<Segment> extraSegments) {
            this.layout        = layout;
            this.regionX       = regionX;
            this.regionWidth   = regionWidth;
            this.extraSegments = extraSegments;
        }
    }

    public static void renderStyleOn(
        final UI.Layer layer,
        final LayerRenderConf conf,
        final Graphics2D g2d
    ) {
        // 1. Foundation + Background fill (not every layer has this):
        _drawBackgroundFill(conf, g2d);
        // 2. Border (not every layer has this):
        _drawBorder( conf, conf.baseColors().borderColor(), g2d);

        // Now on to things every layer has:

        // 3. A grounding serving as a base background, which is a filled color and/or an image:
        for ( ImageConf imageConf : conf.layer().images().sortedByNames() )
            if ( !imageConf.equals(ImageConf.none()) )
                _renderImage( conf, imageConf, conf.boxModel().size(), g2d);

        // 4. Gradients, which are best used to give a component a nice surface lighting effect.
        // They may transition vertically, horizontally or diagonally over various different colors:
        for ( GradientConf gradient : conf.layer().gradients().sortedByNames() )
            if ( gradient.colors().length > 0 ) {
                _renderGradient( gradient, conf, g2d );
            }

        // 5. Noise, which is a simple way to add a bit of texture to a component:
        for ( Pooled<NoiseConf> noise : conf.layer().noises().sortedByNames() )
            if ( noise.get().colors().length > 0 ) {
                _renderNoise( noise, conf, g2d );
            }

        // 6. Shadows, which are simple gradient based drop shadows that can go inwards or outwards
        for ( ShadowConf shadow : conf.layer().shadows().sortedByNames() )
            _renderShadows(conf, shadow, g2d);

        // 7. Custom text, which can be rendered in any font and color:
        for ( TextConf text : conf.layer().texts().sortedByNames() )
            _renderText( text, conf, g2d );

        // 8. Painters, which are provided by the user and can be anything
        _executeUserPainters(layer, conf, g2d);

        // And that's it! We have rendered a style layer!
    }

    private static void _drawBackgroundFill(
        final LayerRenderConf conf,
        final Graphics2D g2d
    ) {
        final Color foundationColor = conf.baseColors().foundationColor().map( c -> c.getAlpha() == 0 ? null : c ).orElse(UI.Color.UNDEFINED);
        final Color backgroundColor = conf.baseColors().backgroundColor().map( c -> c.getAlpha() == 0 ? null : c ).orElse(UI.Color.UNDEFINED);
        final boolean borderIsOpaque = conf.boxModel().widths().equals(Outline.none()) || conf.baseColors().borderColor().isFullyOpaque();
        final boolean bodyIsOpaque = backgroundColor.getAlpha() == 255;
        if ( bodyIsOpaque && borderIsOpaque ) {
            g2d.setColor(foundationColor);
            g2d.fill(conf.areas().get(UI.ComponentArea.ALL)); // Filling everything is a bit cheaper than UI.ComponentArea.EXTERIOR!
            g2d.setColor(backgroundColor);
            g2d.fill(conf.areas().get(UI.ComponentArea.BODY));
        } else {
            if ( foundationColor.getAlpha() > 0 ) { // Avoid rendering a fully transparent color!
                g2d.setColor(foundationColor);
                g2d.fill(conf.areas().get(UI.ComponentArea.EXTERIOR));
            }
            if ( backgroundColor.getAlpha() > 0 ) { // Avoid rendering a fully transparent color!
                g2d.setColor(backgroundColor);
                g2d.fill(conf.areas().get(UI.ComponentArea.BODY));
            }
        }
    }

    private static void _drawBorder(
        final LayerRenderConf conf,
        final BorderColorsConf colors,
        final Graphics2D g2d
    ) {
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
                log.warn(SwingTree.get().logMarker(),
                        "An exception occurred while drawing the border of border style '{}' ",
                        conf.boxModel(), e
                    );
                /*
                    If exceptions happen in user provided painters, we don't want to
                    mess up the rendering of the rest of the component, so we catch them here!
                */
            }
        }
    }

    private static void _renderShadows(
        final LayerRenderConf conf,
        final ShadowConf    shadow,
        final Graphics2D    g2d
    ) {
        final Color shadowColor = shadow.color().orElse(null);
        if ( shadowColor == null )
            return;
        final Size  size        = conf.boxModel().size();

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

        final int averageCornerRadius = ((int) ( topLeftRadius + topRightRadius + bottomRightRadius + bottomLeftRadius )) / 4;
        final int averageBorderWidth  = (int) (( leftBorderWidth + topBorderWidth + rightBorderWidth +  bottomBorderWidth ) / 4);
        final int shadowCornerRadius  = (int) Math.max( 0, averageCornerRadius + (shadow.isOutset() ? -spreadRadius-blurRadius*2 : -Math.max(averageBorderWidth,spreadRadius)) );
        final int gradientStartOffset = 1 + (int)((shadowCornerRadius * 2) / ( shadow.isInset() ? 4.5 : 3.79) );

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
        final Area shadowArea = new Area(outerShadowRect);

        if ( shadow.isOutset() )
            shadowArea.subtract(baseArea);
        else
            shadowArea.intersect(baseArea);

        // Compute the transparent shadow background color once so that sub-methods don't repeat the allocation
        final Color transparentShadowBg = _transparentShadowBackground(shadow);

        // Draw the corner shadows
        _renderCornerShadow(shadow, UI.Corner.TOP_LEFT,     shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, transparentShadowBg, g2d);
        _renderCornerShadow(shadow, UI.Corner.TOP_RIGHT,    shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, transparentShadowBg, g2d);
        _renderCornerShadow(shadow, UI.Corner.BOTTOM_LEFT,  shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, transparentShadowBg, g2d);
        _renderCornerShadow(shadow, UI.Corner.BOTTOM_RIGHT, shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, transparentShadowBg, g2d);

        // Draw the edge shadows
        _renderEdgeShadow(shadow, UI.Edge.TOP,    shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, transparentShadowBg, g2d);
        _renderEdgeShadow(shadow, UI.Edge.RIGHT,  shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, transparentShadowBg, g2d);
        _renderEdgeShadow(shadow, UI.Edge.BOTTOM, shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, transparentShadowBg, g2d);
        _renderEdgeShadow(shadow, UI.Edge.LEFT,   shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, transparentShadowBg, g2d);

        final Area outerMostArea = new Area(outerShadowRect);
        // If the base rectangle and the outer shadow box are not equal, then we need to fill the area of the base rectangle that is not covered by the outer shadow box!
        _renderShadowBody(shadow, baseArea, innerShadowRect, outerMostArea, g2d);

    }

    private static void _renderShadowBody(
        final ShadowConf shadowConf,
        final Area              baseArea,
        final Rectangle2D.Float innerShadowRect,
        final Area              outerShadowBox,
        final Graphics2D        g2d
    ) {
        final Graphics2D g2d2 = (Graphics2D) g2d.create();
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
        final ShadowConf        shadowConf,
        final UI.Corner         corner,
        final Area              areaWhereShadowIsAllowed,
        final Rectangle2D.Float innerShadowRect,
        final Rectangle2D.Float outerShadowRect,
        final int               gradientStartOffset,
        final Color             shadowBackgroundColor,
        final Graphics2D        g2d
    ) {
        // We define a clipping box so that corners don't overlap
        final float clipBoxWidth   = outerShadowRect.width / 2f;
        final float clipBoxHeight  = outerShadowRect.height / 2f;
        final float clipBoxCenterX = outerShadowRect.x + clipBoxWidth;
        final float clipBoxCenterY = outerShadowRect.y + clipBoxHeight;
        final Rectangle2D.Float cornerClipBox; // outer box!

        // The defining the corner shadow bound (where it starts and ends
        final Rectangle2D.Float cornerBox;
        final float cx;
        final float cy;
        final float cr; // depending on the corner, this is either the corner box width or height
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

        final Color innerColor;
        final Color outerColor;
        if ( shadowConf.isOutset() ) {
            innerColor = shadowConf.color().orElse(Color.BLACK);
            outerColor = shadowBackgroundColor;
        } else {
            innerColor = shadowBackgroundColor;
            outerColor = shadowConf.color().orElse(Color.BLACK);
        }
        final float gradientStart = (float) gradientStartOffset / cr;

        // The first thing we can do is to clip the corner box to the area where the shadow is allowed
        final Area cornerArea = new Area(cornerBox);
        cornerArea.intersect(areaWhereShadowIsAllowed);

        // In the simplest case we don't need to do any gradient painting:
        if ( gradientStart == 1f || gradientStart == 0f ) {
            // Simple, we just draw a circle and clip it
            final Area circle = new Area(new Ellipse2D.Float(cx - cr, cy - cr, cr * 2, cr * 2));
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

        final RadialGradientPaint cornerPaint;
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

        final Graphics2D cornerG2d = (Graphics2D) g2d.create();
        cornerG2d.setPaint(cornerPaint);
        cornerG2d.fill(cornerArea);
        cornerG2d.dispose();
    }

    private static void _renderEdgeShadow(
        final ShadowConf        shadowConf,
        final UI.Edge           edge,
        final Area              contentArea,
        final Rectangle2D.Float innerShadowRect,
        final Rectangle2D.Float outerShadowRect,
        final int               gradientStartOffset,
        final Color             shadowBackgroundColor,
        final Graphics2D        g2d
    ) {
        // We define a boundary center point and a clipping box so that edges don't overlap
        final float clipBoundaryX = outerShadowRect.x + outerShadowRect.width / 2f;
        final float clipBoundaryY = outerShadowRect.y + outerShadowRect.height / 2f;
        Rectangle2D.Float edgeClipBox = null;

        final Rectangle2D.Float edgeBox;
        final float gradEndX;
        final float gradEndY;
        final float gradStartX;
        final float gradStartY;
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

        final Color innerColor;
        final Color outerColor;
        // Same as shadow color but without alpha (pre-computed by the caller):
        if (shadowConf.isOutset()) {
            innerColor = shadowConf.color().orElse(Color.BLACK);
            outerColor = shadowBackgroundColor;
        } else {
            innerColor = shadowBackgroundColor;
            outerColor = shadowConf.color().orElse(Color.BLACK);
        }
        LinearGradientPaint edgePaint;
        // distance between start and end of gradient
        final float dist = (float) Math.sqrt(
                                    (gradEndX - gradStartX) * (gradEndX - gradStartX) +
                                    (gradEndY - gradStartY) * (gradEndY - gradStartY)
                                );
        final float gradientStart = (float) gradientStartOffset / dist;
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
        final Area edgeArea = new Area(edgeBox);
        edgeArea.intersect(contentArea);
        if ( edgeClipBox != null )
            edgeArea.intersect(new Area(edgeClipBox));

        final Graphics2D edgeG2d = (Graphics2D) g2d.create();
        edgeG2d.setPaint(edgePaint);
        edgeG2d.fill(edgeArea);
        edgeG2d.dispose();
    }

    private static Color _transparentShadowBackground(final ShadowConf shadow) {
        return shadow.color()
                    .map(c -> new Color(c.getRed(), c.getGreen(), c.getBlue(), 0))
                    .orElse(new Color(0.5f, 0.5f, 0.5f, 0f));
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
            final Paint paint = _createGradientPaint(conf.boxModel(), gradient);
            if ( paint != null ) {
                Shape areaToFill = conf.areas().get(gradient.area());
                g2d.setPaint(paint);
                g2d.fill(areaToFill);
            }
        }
    }

    static @Nullable Paint _createGradientPaint(
        final BoxModelConf boxModel,
        final GradientConf gradient
    ) {
        final Size dimensions = boxModel.size();
        Outline insets;
        if ( gradient.boundary() == UI.ComponentBoundary.CENTER_TO_CONTENT ) {
            final Outline contentIns = boxModel.insetsFor(UI.ComponentBoundary.INTERIOR_TO_CONTENT);
            final float verticalInset = dimensions.height().orElse(0f) / 2f;
            final float horizontalInset = dimensions.width().orElse(0f) / 2f;
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
            insets = boxModel.insetsFor(gradient.boundary());
        }

        final float insLeft   = insets.left().orElse(0f);
        final float insTop    = insets.top().orElse(0f);
        final float insRight  = insets.right().orElse(0f);
        final float insBottom = insets.bottom().orElse(0f);
        final float width  = dimensions.width().orElse(0f)  - ( insRight  + insLeft );
        final float height = dimensions.height().orElse(0f) - ( insBottom + insTop );
        final float realX  = insLeft + gradient.offset().x();
        final float realY  = insTop  + gradient.offset().y();

        final Point2D.Float corner1;
        final Point2D.Float corner2;
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
            log.warn(SwingTree.get().logMarker(),
                    "Unknown gradient type: {}",
                    type, new Throwable("Stack trace for debugging purposes.")
                );
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
        final Point2D.Float  corner1,
        final Point2D.Float  corner2,
        final GradientConf   gradient
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
        final Pooled<NoiseConf> noise,
        final LayerRenderConf conf,
        final Graphics2D g2d
    ) {
        final Paint noisePaint = _createNoisePaint(conf.boxModel(), noise);
        final Shape areaToFill = conf.areas().get(noise.get().area());
        g2d.setPaint(noisePaint);
        g2d.fill(areaToFill);
    }

    static Paint _createNoisePaint(
        final BoxModelConf   boxModel,
        final Pooled<NoiseConf> noise
    ) {
        if ( noise.get().colors().length == 1 ) {
            return noise.get().colors()[0];
        } else {
            Outline insets = boxModel.insetsFor(noise.get().boundary());
            Point2D.Float corner1 = new Point2D.Float(
                                        insets.left().orElse(0f) + noise.get().offset().x(),
                                        insets.top().orElse(0f) + noise.get().offset().y()
                                    );

            return _createNoisePaint(corner1, noise);
        }
    }

    private static Paint _createNoisePaint(
        final Point2D.Float  center,
        final Pooled<NoiseConf> noise
    ) {
        Map<Point2D, NoiseGradientPaint> cachedPaints = _NOISE_PAINT_CACHE.computeIfAbsent(noise, k -> new HashMap<>());
        NoiseGradientPaint paint = cachedPaints.get(center);
        if ( paint != null ) {
            return paint;
        }

        final Color[] colors    = noise.get().colors();
        final float[] fractions = _fractionsFrom(colors, noise.get().fractions());
        final float rotation = noise.get().rotation();
        final Scale scale = noise.get().scale();
        final float scaleX = scale.x();
        final float scaleY = scale.y();

        paint = new NoiseGradientPaint(
                        center,
                        scaleX,
                        scaleY,
                        rotation,
                        fractions,
                        colors,
                        noise.get().function()
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
            Point2D.Float p = _rotatePoint(corner1X, corner1Y, corner2X, corner2Y, gradient.rotation());
            corner2X = p.x;
            corner2Y = p.y;
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
                Point2D.Float p = _rotatePoint(corner1X, corner1Y, corner2X, corner2Y, gradient.rotation());
                corner2X = p.x;
                corner2Y = p.y;
            }

            return new LinearGradientPaint(
                        corner1X, corner1Y,
                        corner2X, corner2Y,
                        fractions, colors,
                        _cycleMethodFrom(cycle)
                    );

        }
    }

    private static Point2D.Float projectPointOntoLine(
        final Point2D.Float A,
        final Point2D.Float n,
        final Point2D.Float C
    ) {
        Point2D.Float B = new Point2D.Float(A.x + n.x, A.y + n.y);
        float t = ((C.x - A.x) * (B.x - A.x) + (C.y - A.y) * (B.y - A.y)) / ((B.x - A.x) * (B.x - A.x) + (B.y - A.y) * (B.y - A.y));
        return new Point2D.Float(A.x + t * (B.x - A.x), A.y + t * (B.y - A.y));
    }

    private static Paint _createRadialGradientPaint(
        final Point2D.Float  corner1,
        final Point2D.Float  corner2,
        final GradientConf   gradient
    ) {
        final UI.Cycle cycle  = gradient.cycle();
        final Color[]  colors = gradient.colors();

        final float size   = gradient.size();

        final float corner1X = corner1.x;
        final float corner1Y = corner1.y;
        float corner2X = corner2.x;
        float corner2Y = corner2.y;


        final float[] fractions = _fractionsFrom(gradient);

        final float radius;

        if ( size < 0 )
            radius = (float) Math.sqrt(
                                 (corner2X - corner1X) * (corner2X - corner1X) +
                                 (corner2Y - corner1Y) * (corner2Y - corner1Y)
                             );
        else
            radius = size;

        if ( gradient.focus().equals(Offset.none()) ) {
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
                Point2D.Float p = _rotatePoint(corner1X, corner1Y, focusX, focusY, gradient.rotation());
                focusX = p.x;
                focusY = p.y;
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
                log.warn(SwingTree.get().logMarker(),
                        "Unknown cycle method: {}",
                        cycle, new Throwable("Stack trace for debugging purposes.")
                    );
                return MultipleGradientPaint.CycleMethod.NO_CYCLE;
        }
    }

    private static float[] _fractionsFrom( final GradientConf style ) {
        final Color[] colors   = style.colors();
        final float[] fractions = style.fractions();
        return _fractionsFrom(colors, fractions);
    }

    private static float[] _fractionsFrom(
        final Color[] colors,
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
                    Now simply complete the missing fractions by linear interpolation
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
        final float p1X, final float p1Y,
        final float p2X, final float p2Y,
        final float rotation
    ) {
        if ( rotation == 0f )
            return new Point2D.Float(p2X, p2Y);
        else if ( rotation % 360f == 0f )
            return new Point2D.Float(p2X, p2Y);

        final double angle = Math.toRadians(rotation);
        final double sin   = Math.sin(angle);
        final double cos   = Math.cos(angle);

        final double x = p2X - p1X;
        final double y = p2Y - p1Y;

        final double newX = x * cos - y * sin;
        final double newY = x * sin + y * cos;

        return new Point2D.Float((float) (p1X + newX), (float) (p1Y + newY));
    }

    /**
     *  Takes 2 points and calculates the rotation
     *  of point 2 around point 1.
     *
     * @param p1 The first point which serves as the center of rotation.
     * @param p2 The second point which is rotated around point 1.
     * @return The rotation in degrees.
     */
    private static float _rotationBetween(
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
            final UI.FitComponent      fit               = style.fitMode();
            final UI.Placement         placement         = style.placement();
            final UI.ComponentBoundary placementBoundary = style.placementBoundary();
            final Outline              insets            = conf.boxModel().insetsFor(placementBoundary);
            final Outline      padding         = style.padding();
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
                svgIcon.withOpacity(opacity)
                        .withFitComponent(localFit)
                        .withPreferredPlacement(localPlacement)
                        .paintIcon(null, g2d, Bounds.of(areaX, areaY, componentWidth, componentHeight), style.offset(), padding);
            } else {
                int imgWidth  = style.width().orElse(iconBaseWidth);
                int imgHeight = style.height().orElse(iconBaseHeight);

                if ( fit != UI.FitComponent.NO && fit != UI.FitComponent.UNDEFINED ) {
                    if ( fit == UI.FitComponent.WIDTH_AND_HEIGHT ) {
                        imgWidth  = style.width().orElse(componentWidth);
                        imgHeight = style.height().orElse(componentHeight);
                    }
                    if ( fit == UI.FitComponent.WIDTH ) {
                        imgWidth  = style.width().orElse(componentWidth);
                    }
                    if ( fit == UI.FitComponent.HEIGHT ) {
                        imgHeight = style.height().orElse(componentHeight);
                    }
                    if (
                        (fit == UI.FitComponent.MAX_DIM && componentWidth > componentHeight)  ||
                        (fit == UI.FitComponent.MIN_DIM && componentWidth < componentHeight )
                    ) {
                        imgWidth = style.width().orElse(componentWidth);
                        double aspectRatio = (double) iconBaseHeight / (double) iconBaseWidth;
                        // We preserve the aspect ratio:
                        imgHeight = (int) (imgWidth * aspectRatio);
                    }
                    if (
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
                int x = style.horizontalOffset() + insets.left().orElse(0f).intValue();
                int y = style.verticalOffset() + insets.top().orElse(0f).intValue();
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

        final Tuple<StyledString>  textToRender      = text.content();
        final UI.ComponentArea     clipArea          = text.clipArea();
        final UI.Placement         placement         = findDesiredPlacementFrom(text);
        final boolean              wrapLines         = text.wrapLines();
        // Computing the area available for text rendering after applying the offset and insets:
        final Bounds textBounds = _computeTextBounds(text, boxModel);
        try {
            Font font = Optional.ofNullable(initialFont).orElse(new Font(Font.DIALOG, Font.PLAIN, UI.scale(12)));
            font = text.fontConf().createDerivedFrom(font, boxModel).orElse(font);
            g2d.setFont(font);
            // Phase 1 - 2: Build TextLayouts for each line and calculate the total height of the text block
            final FontRenderContext frc = g2d.getFontRenderContext();
            final float boundsWidth = textBounds.size().width().orElse(0f);
            final float boundsX     = textBounds.location().x();
            final float boundsY     = textBounds.location().y();
            final Pair<Float, List<LayoutLine>> layoutResult = _buildTextLayoutsAndPreferredHeight(font, frc, textToRender, boundsWidth, boundsX, boundsY, wrapLines, conf.boxModel(), text.obstacles());
            final List<LayoutLine> lines    = layoutResult.second();
            final float            totalHeight = layoutResult.first();
            // Phase 3 - 5: Rendering
            Shape newClip = conf.areas().get(clipArea);
            // We merge the new clip with the old one:
            if ( oldClip != null )
                newClip = StyleUtil.intersect( newClip, oldClip );
            g2d.setClip(newClip);
            _renderTextInternal(g2d, font, textBounds, placement, lines, totalHeight);
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "Unexpected error while rendering text: '{}'\n", textToRender, e);
        } finally {
            g2d.setFont(initialFont);
            g2d.setClip(oldClip);
        }
    }

    static Bounds _computeTextBounds(final TextConf text, final BoxModelConf boxModel) {
        final UI.ComponentBoundary placementBoundary = text.placementBoundary();
        final Offset               offset            = text.offset();
        final Outline              insets            = boxModel.insetsFor(placementBoundary);
        // Computing the area available for text rendering after applying the offset and insets:
        final float insLeft   = insets.left().orElse(0f);
        final float insTop    = insets.top().orElse(0f);
        final float leftX = offset.x() + insLeft;
        final float topY  = offset.y() + insTop;
        final float localWidth  = Math.max(0, boxModel.size().width().orElse(0f)  - (insLeft + insets.right().orElse(0f)));
        final float localHeight = Math.max(0, boxModel.size().height().orElse(0f) - (insTop  + insets.bottom().orElse(0f)));
        return Bounds.of(leftX, topY, localWidth, localHeight);
    }

    private static UI.Placement findDesiredPlacementFrom(TextConf text) {
        UI.Placement chosenPlacement = text.placement();
        if ( chosenPlacement == UI.Placement.UNDEFINED ) {
            // We determine the placement of the text from the font configuration if not explicitly set:
            UI.HorizontalAlignment horizontalAlignment = text.fontConf().horizontalAlignment();
            UI.VerticalAlignment verticalAlignment = text.fontConf().verticalAlignment();
            chosenPlacement = placementOf(horizontalAlignment, verticalAlignment);
        }
        return chosenPlacement;
    }

    static UI.Placement placementOf(
        UI.HorizontalAlignment horizontalAlignment, 
        UI.VerticalAlignment verticalAlignment
    ) {
        UI.Placement currentPlacement = UI.Placement.UNDEFINED;
        switch (horizontalAlignment) {
            case LEFT: currentPlacement = UI.Placement.LEFT;break;
            case CENTER: currentPlacement = UI.Placement.CENTER;break;
            case RIGHT: currentPlacement = UI.Placement.RIGHT;break;
            case LEADING: currentPlacement = UI.Placement.LEFT;break; // leading means: "align with the reading direction of the text". In most cases, this is equivalent to LEFT, but it can be different for right-to-left languages. For simplicity, we treat it as LEFT here.
            case TRAILING: currentPlacement = UI.Placement.RIGHT;break;// trailing means: "align with the opposite of the reading direction of the text". In most cases, this is equivalent to RIGHT, but it can be different for right-to-left languages. For simplicity, we treat it as RIGHT here.
            default: break;
        }
        switch (verticalAlignment) {
            case TOP:
                switch (currentPlacement) {
                    case LEFT: return UI.Placement.TOP_LEFT;
                    case CENTER: return UI.Placement.TOP;
                    case RIGHT: return UI.Placement.TOP_RIGHT;
                    default: return UI.Placement.TOP;
                }
            case CENTER:
                switch (currentPlacement) {
                    case LEFT: return UI.Placement.LEFT;
                    case CENTER: return UI.Placement.CENTER;
                    case RIGHT: return UI.Placement.RIGHT;
                    default: return UI.Placement.CENTER;
                }
            case BOTTOM:
                switch (currentPlacement) {
                    case LEFT: return UI.Placement.BOTTOM_LEFT;
                    case CENTER: return UI.Placement.BOTTOM;
                    case RIGHT: return UI.Placement.BOTTOM_RIGHT;
                    default: return UI.Placement.BOTTOM;
                }
            default:
                return currentPlacement;
        }
    }
    
    private static void _renderTextInternal(
        final Graphics2D       g2d,
        final Font             font,
        final Bounds           textBounds,
        final UI.Placement     placement,
        final List<LayoutLine> lines,
        final float            totalHeight
    ) {
        final float boundsY      = textBounds.location().y();
        final float boundsHeight = textBounds.size().height().orElse(0f);
        /*
            ------------------------------------------------
            Phase 3 : Determine visible slice (overflow policy)
            ------------------------------------------------
         */
        final List<LayoutLine> visible = new ArrayList<>();
        float accumulated = 0;
        if (
            placement == UI.Placement.TOP ||
            placement == UI.Placement.TOP_LEFT ||
            placement == UI.Placement.TOP_RIGHT
        ) {
            for ( LayoutLine line : lines ) {
                float h = line.layout == null
                            ? font.getSize2D()
                            : line.layout.getAscent() + line.layout.getDescent() + line.layout.getLeading();
                if ( Math.floor(accumulated + h) > boundsHeight )
                    break;
                visible.add(line);
                accumulated += h;
            }
        } else if (
            placement == UI.Placement.BOTTOM ||
            placement == UI.Placement.BOTTOM_LEFT ||
            placement == UI.Placement.BOTTOM_RIGHT
        ) {
            final ListIterator<LayoutLine> it = lines.listIterator(lines.size());
            while ( it.hasPrevious() ) {
                LayoutLine line = it.previous();
                float h = line.layout == null
                        ? font.getSize2D()
                        : line.layout.getAscent() + line.layout.getDescent() + line.layout.getLeading();
                if ( Math.floor(accumulated + h) > boundsHeight )
                    break;
                visible.add(0, line);
                accumulated += h;
            }
        } else {
            /*
                CENTER / LEFT / RIGHT — overflow both directions
             */
            final float centerHeight = Math.min(totalHeight, boundsHeight);
            final float targetTop    = (totalHeight - centerHeight) / 2f;
            float cursor = 0;
            for ( LayoutLine line : lines ) {
                float h = line.layout == null
                        ? font.getSize2D()
                        : line.layout.getAscent() + line.layout.getDescent() + line.layout.getLeading();
                if ( cursor + h < targetTop ) {
                    cursor += h;
                    continue;
                }
                if ( Math.floor(accumulated + h) > boundsHeight )
                    break;
                visible.add(line);
                accumulated += h;
                cursor += h;
            }
        }

        /*
            ------------------------------------------------
            Phase 4 : Vertical anchor
            ------------------------------------------------
         */
        final float visibleHeight = accumulated;
        float y;
        if (
            placement == UI.Placement.TOP ||
            placement == UI.Placement.TOP_LEFT ||
            placement == UI.Placement.TOP_RIGHT
        ) {
            y = boundsY;
        } else if (
            placement == UI.Placement.BOTTOM ||
            placement == UI.Placement.BOTTOM_LEFT ||
            placement == UI.Placement.BOTTOM_RIGHT
        ) {
            y = boundsY + boundsHeight - visibleHeight;
        } else {
            y = boundsY + (boundsHeight - visibleHeight) / 2f;
        }

        /*
            ------------------------------------------------
            Phase 5 : Render lines
            ------------------------------------------------
         */
        for ( LayoutLine line : visible ) {
            if ( line.layout == null ) {
                y += font.getSize2D();
                continue;
            }
            y += line.layout.getAscent();

            // Draw all fragments (primary + obstacle-split extras) at the same baseline.
            // x positioning is relative to each fragment's own obstacle-free region.
            _drawLineFragment(g2d, placement, line.layout, line.regionX, line.regionWidth, y);
            for ( LayoutLine.Segment seg : line.extraSegments )
                _drawLineFragment(g2d, placement, seg.layout, seg.regionX, seg.regionWidth, y);

            y += line.layout.getDescent() + line.layout.getLeading();
        }
    }

    /** Draws one text fragment aligned within its obstacle-free region at the given baseline y. */
    private static void _drawLineFragment(
        final Graphics2D   g2d,
        final UI.Placement placement,
        final TextLayout   layout,
        final float        regionX,
        final float        regionWidth,
        final float        baselineY
    ) {
        final float advance = layout.getAdvance();
        final float x;
        switch (placement) {
            case LEFT:
            case TOP_LEFT:
            case BOTTOM_LEFT:
                x = regionX;
                break;
            case RIGHT:
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                x = regionX + regionWidth - advance;
                break;
            default:// UNDEFINED / CENTER / TOP / BOTTOM
                x = regionX + (regionWidth - advance) / 2f;
        }
        layout.draw(g2d, x, baselineY);
    }

    /**
     *  Builds the list of {@link TextLayout} objects for the given styled text and measures
     *  the total rendered height of all lines (Phase 1 + Phase 2 of the text rendering pipeline).
     *  When {@code obstacles} is non-empty, each line layout will be built with awareness
     *  of the obstacle shapes and may contain multiple TextLayout segments to skip over obstacles.
     *  <b>But hitting an obstacle does not just lead to a simple line break, within a line,
     *  the text will try to skip over the obstacle shape until it can continue.</b>
     *
     * @param font         The base font to use for unstyled segments.
     * @param frc          The {@link FontRenderContext} used by the measurer.
     * @param text         The styled text to lay out.
     * @param boundsWidth  The available width for the text. This property is only relevant when line wrapping is active.
     * @param boundsX,     The x-offset of the text in the component space. It is important for intersecting obstacles!
     * @param boundsY,     The y-offset of the text in the component space. It is important for intersecting obstacles!
     * @param wrapLines    Whether long lines should be wrapped at word boundaries.
     * @param boxModelConf The box-model configuration forwarded to per-segment font derivation.
     * @param obstacles    Shapes (in component coordinates) the text must not be rendered on top of.
     *                     The text will skip over these shapes and continue rendering on the other side,
     *                     if possible or break the line if no more horizontal space is left.
     *                     Pass an empty {@link Tuple} when no obstacles are present.
     * @return A {@link Pair} whose {@link Pair#first()} is the total pixel height of all lines
     *         and whose {@link Pair#second()} is the ordered list of {@link LayoutLine} entries,
     *         each carrying its layout and the obstacle-free horizontal region it was placed into.
     */
    static Pair<Float, List<LayoutLine>> _buildTextLayoutsAndPreferredHeight(
        final Font                font,
        final FontRenderContext   frc,
        final Tuple<StyledString> text,
        final float               boundsWidth,
        final float               boundsX,
        final float               boundsY,
        final boolean             wrapLines,
        final BoxModelConf        boxModelConf,
        final Tuple<Shape>        obstacles
    ) {
        final List<LayoutLine> lines = new ArrayList<>();
        /*
            ------------------------------------------------
            Phase 1 : Build layouts using LineBreakMeasurer
            ------------------------------------------------
        */
        // currentY tracks the top of the next line in component coordinates (TOP-placement
        // assumption). Used solely for obstacle intersection — a good approximation for all
        // placement modes because obstacles are typically large enough that the small vertical
        // offset introduced by CENTER/BOTTOM placement does not change which obstacle a line hits.
        float currentY = boundsY;
        final float estLineHeight = font.getSize2D();

        final List<@Nullable AttributedString> paragraphs = _splitStyledTextIntoParagraphs(text, font, boxModelConf);
        for ( @Nullable AttributedString attrStr : paragraphs ) {
            if ( attrStr == null ) {
                lines.add(new LayoutLine(null, boundsX, boundsWidth)); // blank line
                currentY += estLineHeight;
                continue;
            }
            final AttributedCharacterIterator it = attrStr.getIterator();

            if ( wrapLines && boundsWidth >= 0 ) {// Word wrapping using LineBreakMeasurer
                final LineBreakMeasurer measurer = new LineBreakMeasurer(it, BreakIterator.getLineInstance(), frc);
                final int end = it.getEndIndex();
                while ( measurer.getPosition() < end ) {
                    final List<float[]> intervals = _freeIntervalsAt(currentY, estLineHeight, boundsX, boundsWidth, obstacles);

                    TextLayout           firstLayout = null;
                    float                firstX      = boundsX;
                    float                firstW      = boundsWidth;
                    List<LayoutLine.Segment> extras  = Collections.emptyList();

                    if ( intervals.isEmpty() ) {
                        // All space is blocked — fall back to full width so the measurer advances
                        firstLayout = measurer.nextLayout(boundsWidth);
                    } else {
                        for ( float[] iv : intervals ) {
                            if ( measurer.getPosition() >= end ) break;
                            final float x = iv[0], w = iv[1];
                            final TextLayout layout = measurer.nextLayout(w);
                            if ( firstLayout == null ) {
                                firstLayout = layout; firstX = x; firstW = w;
                            } else {
                                if ( extras.isEmpty() ) extras = new ArrayList<>();
                                extras.add(new LayoutLine.Segment(layout, x, w));
                            }
                        }
                    }

                    if ( firstLayout != null ) {
                        lines.add(new LayoutLine(firstLayout, firstX, firstW, extras));
                        currentY += firstLayout.getAscent() + firstLayout.getDescent() + firstLayout.getLeading();
                    } else {
                        currentY += estLineHeight; // all intervals had zero width — skip band
                    }
                }
            } else {// No wrapping — render full line even if wider than bounds
                final TextLayout layout = new TextLayout(it, frc);
                lines.add(new LayoutLine(layout, boundsX, boundsWidth));
                currentY += layout.getAscent() + layout.getDescent() + layout.getLeading();
            }
        }

        /*
            remove trailing blank-line marker
         */
        if ( !lines.isEmpty() && lines.get(lines.size() - 1).layout == null )
            lines.remove(lines.size() - 1);

        /*
            ------------------------------------------------
            Phase 2 : Measure total text height
            ------------------------------------------------
         */
        float totalHeight = 0;
        for ( LayoutLine line : lines ) {
            if ( line.layout == null )
                totalHeight += font.getSize2D();
            else
                totalHeight += line.layout.getAscent() + line.layout.getDescent() + line.layout.getLeading();
        }

        return Pair.of(totalHeight, lines);
    }

    /**
     *  Returns all contiguous obstacle-free horizontal intervals within
     *  {@code [boundsX, boundsX+boundsWidth]} for a line whose vertical band spans
     *  {@code [y, y+lineHeight]} in component coordinates, sorted left to right.
     *  <p>
     *  Each obstacle's contribution is derived from its <em>exact geometry</em> via
     *  {@link Area} intersection (not just the bounding box), so non-rectangular shapes
     *  such as circles or ellipses are handled correctly — only the actual chord at the
     *  current y-level is subtracted, not the full bounding-box width.
     *  <p>
     *  Returning all intervals (rather than just the widest one) allows the caller to
     *  fill text into every free gap on a line, so text appears on both sides of an obstacle.
     *  If all horizontal space is consumed, an empty list is returned.
     *
     * @param y            Top of the line in component coordinates.
     * @param lineHeight   Estimated height of one line (typically {@code font.getSize2D()}).
     * @param boundsX      Left edge of the text bounds in component coordinates.
     * @param boundsWidth  Full width of the text bounds.
     * @param obstacles    Shapes to avoid, in component coordinates.
     * @return List of {@code float[]{xStart, width}} entries for every obstacle-free interval,
     *         ordered left to right.  Each width is guaranteed to be {@code > 0}.
     */
    private static List<float[]> _freeIntervalsAt(
        final float        y,
        final float        lineHeight,
        final float        boundsX,
        final float        boundsWidth,
        final Tuple<Shape> obstacles
    ) {
        if ( obstacles.isEmpty() )
            return Collections.singletonList(new float[]{ boundsX, boundsWidth });

        // Internal representation: {start, end} pairs (converted to {start, width} on exit)
        List<float[]> free = new ArrayList<>();
        free.add(new float[]{ boundsX, boundsX + boundsWidth });

        final Area lineStrip = new Area(new Rectangle2D.Float(boundsX, y, boundsWidth, lineHeight));

        for ( Shape obstacle : obstacles ) {
            // Fast pre-check with the bounding box before the more expensive Area intersection
            final Rectangle2D ob = obstacle.getBounds2D();
            if ( ob.getMaxY() <= y || ob.getMinY() >= y + lineHeight )
                continue;

            // Intersect the exact obstacle geometry with the line strip
            final Area intersection = new Area(obstacle);
            intersection.intersect(lineStrip);
            if ( intersection.isEmpty() )
                continue;

            final Rectangle2D ib     = intersection.getBounds2D();
            final float       oLeft  = (float) ib.getMinX();
            final float       oRight = (float) ib.getMaxX();

            // Subtract [oLeft, oRight] from every free interval (1-D interval difference)
            final List<float[]> remaining = new ArrayList<>(free.size() + 1);
            for ( float[] iv : free ) {
                final float a = iv[0], b = iv[1];
                if ( oRight <= a || oLeft >= b ) {
                    remaining.add(iv);                                                               // no overlap
                } else {
                    if ( oLeft  > a ) remaining.add(new float[]{ a,                   Math.min(b, oLeft)  }); // left fragment
                    if ( oRight < b ) remaining.add(new float[]{ Math.max(a, oRight), b                   }); // right fragment
                }
            }
            free = remaining;
        }

        if ( free.isEmpty() )
            return Collections.emptyList();

        // Convert {start, end} → {start, width}, filtering out zero-width gaps
        final List<float[]> result = new ArrayList<>(free.size());
        for ( float[] iv : free ) {
            final float w = iv[1] - iv[0];
            if ( w > 0f ) result.add(new float[]{ iv[0], w });
        }
        return result;
    }

    private static List<@Nullable AttributedString> _splitStyledTextIntoParagraphs(
        final Tuple<StyledString> text,
        final Font                font,
        final BoxModelConf        boxModelConf
    ) {
        List<@Nullable AttributedString> paragraphs = new ArrayList<>();
        List<StyledString> currentParagraph = null;
        for ( StyledString styledString : text ) {
            String[] parts = styledString.string().split("\n", -1);
            if ( parts.length <= 1 ) {
                if ( currentParagraph == null )
                    currentParagraph = new ArrayList<>();
                currentParagraph.add(styledString);
            } else {
                for ( int i = 0; i < parts.length; i++ ) {
                    String part = parts[i];
                    if ( currentParagraph == null )
                        currentParagraph = new ArrayList<>();
                    if ( !part.isEmpty() )
                        currentParagraph.add(styledString.withString(part));
                    // if it is not the last part, we start a new line/paragraph:
                    if ( i < parts.length - 1 ) {
                        paragraphs.add(_paragraphToAttributedString(currentParagraph, font, boxModelConf));
                        currentParagraph = null;
                    }
                }
            }
        }
        if ( currentParagraph != null )
            paragraphs.add(_paragraphToAttributedString(currentParagraph, font, boxModelConf));
        return paragraphs;
    }

    private static @Nullable AttributedString _paragraphToAttributedString(
        final List<StyledString> paragraph,
        final Font               font,
        final BoxModelConf       boxModelConf
    ) {
        int length = paragraph.stream().mapToInt(s -> s.string().length()).sum();
        if ( length <= 0 )
            return null;
        final StringBuilder sb = new StringBuilder();
        for ( StyledString s : paragraph )
            sb.append(s.string());
        final AttributedString attrStr = new AttributedString(sb.toString());
        int index = 0;
        for ( StyledString styledString : paragraph ) {
            int styledStringLength = styledString.string().length();
            if ( styledStringLength <= 0 )
                continue; // Skip zero-length segments to avoid AttributedString IllegalArgumentException for empty ranges
            int endIndex = index + styledStringLength;
            if ( styledString.fontConf().isPresent() ) {
                java.awt.Font localFont = styledString.fontConf().get().createDerivedFrom(font, boxModelConf).orElse(font);
                attrStr.addAttribute(TextAttribute.FONT, localFont, index, endIndex);
                attrStr.addAttributes(localFont.getAttributes(), index, endIndex);
            } else {
                attrStr.addAttribute(TextAttribute.FONT, font, index, endIndex);
                attrStr.addAttributes(font.getAttributes(), index, endIndex);
            }
            index += styledStringLength;
        }
        return attrStr;
    }

    private static void _executeUserPainters(
        final UI.Layer layer,
        final LayerRenderConf conf,
        final Graphics2D g2d
    ) {
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
                log.warn(SwingTree.get().logMarker(),
                        "An exception occurred while executing painter '{}' on layer '{}' for style '{}' ",
                        backgroundPainter, layer, conf, e
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
        final FilterConf    filterConf,
        final BufferedImage parentRendering,
        final Graphics2D    g2d,
        int offsetX,
        int offsetY,
        final Pooled<BoxModelConf> boxModelConf
    ) {
        final Size       size   = boxModelConf.get().size();
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
            ComponentAreas areas = ComponentAreas.of(boxModelConf);
            Shape newClip = areas.get(filterConf.area());
            g2d.setClip(newClip);
            g2d.drawImage(filtered, -offsetX, -offsetY, null);
        } catch (Exception e) {
            log.error(SwingTree.get().logMarker(), "Failed to successfully render filtered parent buffer!", e);
        } finally {
            g2d.setClip(oldClip);
        }
    }

    private static Kernel _makeKernel( final float radius, final boolean transpose ) {
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
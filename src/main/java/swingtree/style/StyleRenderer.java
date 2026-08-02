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
import java.awt.font.TextLayout;
import java.awt.geom.*;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
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
    private static final Map<Pooled<NoiseConf>, NoisePaintCache> _NOISE_PAINT_CACHE = new WeakHashMap<>();
    /**
     *  Caches the geometry-independent blended gradient color stops of a shadow, keyed by the
     *  shadow's interned {@link ShadowConf#renderCacheKey()}. The keys are kept alive by the
     *  {@link ShadowConf} instances held in the (style-cached) {@link StyleConfLayer}s, so this
     *  map is self-cleaning: when a style is dropped its shadow cache entry becomes weakly
     *  reachable and is collected. Mirrors {@link #_NOISE_PAINT_CACHE}.
     */
    private static final Map<Pooled<ShadowConf>, ShadowGradientCache> _SHADOW_GRADIENT_CACHE = new WeakHashMap<>();

    /** Live number of cached noise paints (for monitoring/tests). */
    static int noisePaintCacheSize() { return _NOISE_PAINT_CACHE.size(); }

    /** Live number of cached shadow gradients (for monitoring/tests). */
    static int shadowGradientCacheSize() { return _SHADOW_GRADIENT_CACHE.size(); }

    /** Drops the globally cached noise paints/tiles and shadow gradient stops. Called when the
     *  library cache configuration changes (see {@link ComponentExtension#updateAllCachesFromLibraryConfig()})
     *  so memory shrinks immediately; both maps repopulate lazily under the new budget. */
    static void clearGlobalRenderCaches() {
        _NOISE_PAINT_CACHE.clear();
        _SHADOW_GRADIENT_CACHE.clear();
    }

    /**
     *  A shadow's gradient transition happens across the normalized region
     *  {@code [gradientStart, 1]}. When that region is narrower than this, it cannot hold the
     *  gradient's stops as distinct {@code float} values (the fine falloff curves use up to 65
     *  stops), which would make {@link MultipleGradientPaint} throw, and it is invisible anyway,
     *  so we render such a degenerate shadow as a solid fill instead. See
     *  {@link #_isDegenerateShadowGradient(float)}.
     */
    private static final float SHADOW_GRADIENT_MIN_SPAN = 1e-3f;

    private StyleRenderer() {} // Un-instantiable!

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

    /**
     *  Fills a shape, switching antialiasing off first when the shape provably has
     *  no partially covered edge pixels for it to smooth.
     *  <p>
     *  This matters a great deal. With antialiasing on, {@code Graphics2D.fill(..)}
     *  routes even an axis aligned rectangle through the AA shape pipe, which
     *  rasterizes a per-pixel coverage mask for the whole shape before compositing
     *  anything. For a style layer covering a maximized window that is millions of
     *  mask pixels per fill, computed on every single frame of a live resize - and
     *  every one of them comes out fully opaque, because a rectangle aligned to the
     *  pixel grid has no soft edge to begin with.
     *  <p>
     *  The precondition is checked, not assumed. A {@link Rectangle} is integer valued
     *  by its very type, so the only thing left that could put an edge between two
     *  pixels is the transform, and that is verified here to be free of shear and to
     *  map all four corners onto whole device pixels. Anything else - a fractional
     *  {@link java.awt.geom.Rectangle2D}, a rounded shape, a rotated or oddly scaled
     *  transform - keeps antialiasing and is filled exactly as before.
     *  <p>
     *  Note that the check deliberately says nothing about where the {@code Rectangle}
     *  came from. {@link ComponentAreas} produces one both by verifying that a shape's
     *  bounds are whole and by truncating them, and either way the two antialiasing
     *  states fill the very same integer rectangle identically.
     */
    private static void _fillShape( final Graphics2D g2d, final Shape shape ) {
        if ( shape instanceof Rectangle && RenderingHints.VALUE_ANTIALIAS_ON.equals(g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING)) ) {
            if ( _mapsOntoWholeDevicePixels(g2d.getTransform(), (Rectangle) shape) ) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                try {
                    g2d.fill(shape);
                } finally {
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                }
                return;
            }
        }
        g2d.fill(shape);
    }

    /**
     *  Whether the given transform turns the corners of the given integer rectangle
     *  into whole device pixels, which requires it to be free of rotation and shear
     *  and to scale the corners onto integers.
     */
    private static boolean _mapsOntoWholeDevicePixels( final AffineTransform transform, final Rectangle rectangle ) {
        if ( transform.getShearX() != 0 || transform.getShearY() != 0 )
            return false;
        return _isWhole(transform.getScaleX() * rectangle.x                    + transform.getTranslateX())
            && _isWhole(transform.getScaleY() * rectangle.y                    + transform.getTranslateY())
            && _isWhole(transform.getScaleX() * (rectangle.x + rectangle.width ) + transform.getTranslateX())
            && _isWhole(transform.getScaleY() * (rectangle.y + rectangle.height) + transform.getTranslateY());
    }

    private static boolean _isWhole( final double value ) {
        return Math.abs(value - Math.rint(value)) < 1e-6;
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
            Shape fullArea = conf.areas().get(UI.ComponentArea.ALL);
            Shape bodyArea = conf.areas().get(UI.ComponentArea.BODY);
            if ( !StyleUtil.shapesAreEqual(fullArea, bodyArea) ) {
                g2d.setColor(foundationColor);
                _fillShape(g2d, fullArea); // Filling everything is a bit cheaper than UI.ComponentArea.EXTERIOR!
            }
            g2d.setColor(backgroundColor);
            _fillShape(g2d, bodyArea);
        } else {
            if ( foundationColor.getAlpha() > 0 ) { // Avoid rendering a fully transparent color!
                g2d.setColor(foundationColor);
                _fillShape(g2d, conf.areas().get(UI.ComponentArea.EXTERIOR));
            }
            if ( backgroundColor.getAlpha() > 0 ) { // Avoid rendering a fully transparent color!
                g2d.setColor(backgroundColor);
                _fillShape(g2d, conf.areas().get(UI.ComponentArea.BODY));
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
                    _fillShape(g2d, borderArea);
                } else {
                    // The border area clipped to each edge region. These intersections are a pure
                    // function of the (immutable) box model, so they are computed once and cached in
                    // ComponentAreas rather than recomputed on every repaint (Area.intersect is a hot,
                    // expensive operation for this per-edge border path).
                    Area[] edgeBorders = conf.areas().getClippedEdgeAreas();
                    g2d.setColor(colors.top().orElse(UI.Color.BLACK));
                    g2d.fill(edgeBorders[0]);
                    g2d.setColor(colors.right().orElse(UI.Color.BLACK));
                    g2d.fill(edgeBorders[1]);
                    g2d.setColor(colors.bottom().orElse(UI.Color.BLACK));
                    g2d.fill(edgeBorders[2]);
                    g2d.setColor(colors.left().orElse(UI.Color.BLACK));
                    g2d.fill(edgeBorders[3]);
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
        final Size size = conf.boxModel().size();

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
        final float width     = size.widthOrElse(0f);
        final float height    = size.heightOrElse(0f);

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

        final int gradientStartOffset = shadowGradientStartOffset(conf.boxModel(), shadow);

        Rectangle2D.Float innerShadowRect = new Rectangle2D.Float(
                                        x + blurRadius + gradientStartOffset + spreadRadius,
                                        y + blurRadius + gradientStartOffset + spreadRadius,
                                        w - blurRadius * 2 - gradientStartOffset * 2 - spreadRadius * 2,
                                        h - blurRadius * 2 - gradientStartOffset * 2 - spreadRadius * 2
                                    );

        final Area baseArea;

        if ( shadow.isOutset() ) {
            int artifactAdjustment = 1;
            baseArea = new Area(ComponentAreas.calculateComponentBodyArea(conf.boxModel(), artifactAdjustment, artifactAdjustment, artifactAdjustment, artifactAdjustment));
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

    /**
     *  Calculates the distance from the outer bounds of a shadow towards its center,
     *  after which the shadow gradients have fully faded into the solid shadow color.
     *  Beyond this offset (plus blur and spread) the shadow is a uniform fill.
     *  Note that this is a pure function of size independent style properties, namely
     *  the corner radii and border widths of the box model as well as the blur/spread
     *  radii of the shadow configuration.
     */
    static int shadowGradientStartOffset( final BoxModelConf boxModel, final ShadowConf shadow )
    {
        final float blurRadius   = Math.max(shadow.blurRadius(), 0);
        final float spreadRadius = !shadow.isOutset() ? shadow.spreadRadius() : -shadow.spreadRadius();
        final float leftBorderWidth   = boxModel.widths().left().orElse(0f);
        final float topBorderWidth    = boxModel.widths().top().orElse(0f);
        final float rightBorderWidth  = boxModel.widths().right().orElse(0f);
        final float bottomBorderWidth = boxModel.widths().bottom().orElse(0f);
        final float topLeftRadius     = Math.max(boxModel.topLeftRadius(), 0);
        final float topRightRadius    = Math.max(boxModel.topRightRadius(), 0);
        final float bottomRightRadius = Math.max(boxModel.bottomRightRadius(), 0);
        final float bottomLeftRadius  = Math.max(boxModel.bottomLeftRadius(), 0);
        final int averageCornerRadius = ((int) ( topLeftRadius + topRightRadius + bottomRightRadius + bottomLeftRadius )) / 4;
        final int averageBorderWidth  = (int) (( leftBorderWidth + topBorderWidth + rightBorderWidth +  bottomBorderWidth ) / 4);
        final int shadowCornerRadius  = (int) Math.max( 0, averageCornerRadius + (shadow.isOutset() ? -spreadRadius-blurRadius*2 : -Math.max(averageBorderWidth,spreadRadius)) );
        return 1 + (int)((shadowCornerRadius * 2) / ( shadow.isInset() ? 4.5 : 3.79) );
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
        if ( _isDegenerateShadowGradient(gradientStart) ) {
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

        final float effectiveStart = ( gradientStart > 1f || gradientStart < 0f ) ? 0f : gradientStart;
        final GradientStops stops = _shadowGradientStops(shadowConf, effectiveStart);
        final RadialGradientPaint cornerPaint = new RadialGradientPaint(
                             cx, cy, cr,
                             stops.fractions,
                             stops.colors
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

        // The inner (solid) color of an edge; for an inset edge it is the transparent background
        // (the shadow color without alpha, pre-computed by the caller). The full transition colors
        // are handled by the cached gradient stops below:
        final Color innerColor = shadowConf.isOutset()
                                    ? shadowConf.color().orElse(Color.BLACK)
                                    : shadowBackgroundColor;
        final LinearGradientPaint edgePaint;
        // distance between start and end of gradient
        final float dist = (float) Math.sqrt(
                                    (gradEndX - gradStartX) * (gradEndX - gradStartX) +
                                    (gradEndY - gradStartY) * (gradEndY - gradStartY)
                                );
        final float gradientStart = (float) gradientStartOffset / dist;
        if ( _isDegenerateShadowGradient(gradientStart) ) {
            // The gradient does not really exist, so we can just fill the whole area and then return
            Area edgeArea = new Area(edgeBox);
            g2d.setColor(innerColor);
            if ( shadowConf.isOutset() )
                edgeArea.intersect(contentArea);
            g2d.fill(edgeArea);
            return;
        }
        final float effectiveStart = ( gradientStart > 1f || gradientStart < 0f ) ? 0f : gradientStart;
        final GradientStops stops = _shadowGradientStops(shadowConf, effectiveStart);
        edgePaint = new LinearGradientPaint(
                         gradStartX, gradStartY,
                         gradEndX, gradEndY,
                         stops.fractions,
                         stops.colors
                     );

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

    /**
     *  A small immutable holder for the {@code fractions} and {@code colors} arrays
     *  that make up a {@link MultipleGradientPaint}, returned by {@link #_shadowGradientStops}.
     */
    private static final class GradientStops {
        final float[] fractions;
        final Color[] colors;
        GradientStops(float[] fractions, Color[] colors) {
            this.fractions = fractions;
            this.colors    = colors;
        }
    }

    /**
     *  Decides whether a shadow's gradient transition region {@code [gradientStart, 1]} is
     *  degenerate and should be rendered as a solid fill rather than as a gradient.
     *  <p>
     *  This is the case when {@code gradientStart} is exactly {@code 0} (no transition at all)
     *  or sits within {@link #SHADOW_GRADIENT_MIN_SPAN} below {@code 1}, where the transition
     *  region is too narrow to hold the gradient's stops as distinct {@code float} values
     *  (which would make {@link MultipleGradientPaint} throw an {@link IllegalArgumentException})
     *  and would be invisible anyway. A {@code gradientStart > 1} is deliberately NOT treated as
     *  degenerate here: it means the flat core is larger than the available space and is handled
     *  by the callers by falling back to a full, core-less gradient ({@code effectiveStart == 0}).
     */
    private static boolean _isDegenerateShadowGradient( final float gradientStart ) {
        return gradientStart == 0f || ( gradientStart <= 1f && gradientStart > 1f - SHADOW_GRADIENT_MIN_SPAN );
    }

    /**
     *  Returns the {@link GradientStops} for a shadow gradient whose flat "core" spans
     *  {@code [0, gradientStart]} and whose transition spans {@code [gradientStart, 1]}, going
     *  via the {@link ShadowGradientCache} keyed by {@code shadow}'s geometry-independent
     *  {@link ShadowConf#renderCacheKey()}. The cache holds the blended transition colors (a pure
     *  function of the shadow color, inset/outset direction and type) so they are computed once
     *  and reused across all four corners, all four edges and across repaints, instead of
     *  re-blending up to ~65 {@link Color} stops eight times per shadow per frame.
     */
    private static GradientStops _shadowGradientStops(
        final ShadowConf shadow,
        final float      gradientStart
    ) {
        final ShadowGradientCache cache = _SHADOW_GRADIENT_CACHE.computeIfAbsent(shadow.renderCacheKey(), ShadowGradientCache::new);
        return cache.stopsFor(gradientStart);
    }

    /**
     *  Caches the geometry-independent parts of a shadow's gradient color stops for one
     *  {@link ShadowConf} (normalized to its color, inset/outset direction and {@link UI.ShadowType}).
     *  <p>
     *  The blended transition colors do not depend on the gradient's geometry at all, so they are
     *  blended once (lazily) and reused. Only the {@code fractions} positions depend on the
     *  {@code gradientStart}, which is cheap float arithmetic; the fully assembled
     *  {@link GradientStops} are additionally cached per {@code gradientStart} (bounded, LRU) so
     *  that the symmetric corners/edges sharing a radius within a frame do not even re-allocate
     *  the stop arrays.
     */
    private static final class ShadowGradientCache {

        /** Absolute ceiling on retained per-{@code gradientStart} stop arrays. The live cap
         *  (see {@link #maxCachedStops()}) only drops below this on a constrained byte budget. */
        private static final int MAX_CACHED_STOPS = 16;

        /** Upper bound on retained per-{@code gradientStart} stop arrays, derived from this
         *  cache's slice of the shared {@link CacheBudget} byte budget and clamped to
         *  {@link #MAX_CACHED_STOPS}. {@code 0} disables stop caching. */
        private static int maxCachedStops() {
            return Math.min(MAX_CACHED_STOPS, CacheBudget.maxEntriesFor(CacheBudget.Kind.SHADOW_GRADIENT));
        }

        private final ShadowConf      _conf; // normalized: color + isOutset + type only
        private @Nullable Color       _innerColor;
        private Color    @Nullable [] _transitionColors; // blended curve for sampling indices 1..n
        private final Map<Float,GradientStops> _stopsByStart =
                new LinkedHashMap<Float,GradientStops>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry( Map.Entry<Float,GradientStops> eldest ) {
                        return size() > maxCachedStops();
                    }
                };

        ShadowGradientCache( Pooled<ShadowConf> key ) {
            _conf = key.get();
        }

        private void _ensureColors() {
            if ( _transitionColors != null )
                return;
            final boolean isOutset      = _conf.isOutset();
            final Color   shadowColor   = _conf.color().orElse(Color.BLACK);
            final Color   transparentBg = _transparentShadowBackground(_conf);
            // The solid edge is the inner color for an outset shadow and the outer color for an inset one:
            final Color innerColor = isOutset ? shadowColor   : transparentBg;
            final Color outerColor = isOutset ? transparentBg : shadowColor;
            // A gradient needs at least two stops; a well behaved ShadowFractionsSupplier always
            // supplies >= 2, but as it is a public interface we defensively fall back to the flat falloff:
            final Tuple<Float> falloff = _conf.type().getFractions();
            final Tuple<Float> curve   = falloff.size() >= 2 ? falloff : ShadowFractions.flat();
            final int n = curve.size() - 1; // number of sampling intervals
            final Color[] transition = new Color[n];
            for ( int i = 1; i <= n; i++ ) {
                // The falloff fractions describe the shadow intensity (1 = solid, 0 = transparent)
                // as a function of the distance from the solid edge, oriented per inset/outset:
                final float blend = isOutset ? (1f - curve.get(i)) : curve.get(n - i);
                transition[i - 1] = _blend(innerColor, outerColor, blend);
            }
            _innerColor       = innerColor;
            _transitionColors = transition;
        }

        GradientStops stopsFor( final float gradientStart ) {
            final GradientStops cached = _stopsByStart.get(gradientStart);
            if ( cached != null )
                return cached;
            _ensureColors();
            final Color   innerColor  = Objects.requireNonNull(_innerColor);
            final Color[] transition  = Objects.requireNonNull(_transitionColors);
            final int     n           = transition.length;
            final boolean hasFlatCore = gradientStart > 0f;
            final int     lead        = hasFlatCore ? 2 : 1; // leading stops fixed at innerColor
            final float[] fractions   = new float[lead + n];
            final Color[] colors      = new Color[lead + n];
            fractions[0] = 0f;
            colors[0]    = innerColor;
            if ( hasFlatCore ) {
                fractions[1] = gradientStart;
                colors[1]    = innerColor;
            }
            for ( int i = 1; i <= n; i++ ) {
                final float p   = (float) i / n; // progress across the transition region, in (0, 1]
                final int   idx = lead - 1 + i;
                fractions[idx] = gradientStart + p * (1f - gradientStart);
                colors[idx]    = transition[i - 1];
            }
            fractions[lead - 1 + n] = 1f; // guard against float rounding on the last fraction
            final GradientStops stops = new GradientStops(fractions, colors);
            if ( maxCachedStops() > 0 )
                _stopsByStart.put(gradientStart, stops);
            return stops;
        }
    }

    /**
     *  Linearly interpolates between two colors (including their alpha channel)
     *  by the supplied {@code factor} in {@code [0, 1]}, where {@code 0} yields {@code a}
     *  and {@code 1} yields {@code b}.
     */
    private static Color _blend( final Color a, final Color b, float factor ) {
        factor = factor < 0f ? 0f : ( factor > 1f ? 1f : factor );
        final int r     = Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * factor);
        final int g     = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * factor);
        final int blue  = Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * factor);
        final int alpha = Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * factor);
        return new Color(r, g, blue, alpha);
    }

    private static void _renderGradient(
        final GradientConf    gradient,
        final LayerRenderConf conf,
        final Graphics2D g2d
    ) {
        if ( gradient.colors().length == 1 ) {
            g2d.setColor(gradient.colors()[0]);
            _fillShape(g2d, conf.areas().get(gradient.area()));
        }
        else {
            final Paint paint = _createGradientPaint(conf.boxModel(), gradient);
            if ( paint != null ) {
                Shape areaToFill = conf.areas().get(gradient.area());
                g2d.setPaint(paint);
                _fillShape(g2d, areaToFill);
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
            final float verticalInset = dimensions.heightOrElse(0f) / 2f;
            final float horizontalInset = dimensions.widthOrElse(0f) / 2f;
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
        final float width  = dimensions.widthOrElse(0f)  - ( insRight  + insLeft );
        final float height = dimensions.heightOrElse(0f) - ( insBottom + insTop );
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
        final Shape areaToFill = conf.areas().get(noise.get().area());
        final Pooled<NoiseConf> withoutOffset = noise.get().withoutOffsetForRenderCacheAccess();
        final NoisePaintCache noiseRenderer = _NOISE_PAINT_CACHE.computeIfAbsent(withoutOffset, k -> new NoisePaintCache());
        noiseRenderer.renderNoise(conf.boxModel(), noise, areaToFill, g2d);
    }

    /**
     *  Returns a {@link Paint} for the supplied noise configuration. Used where a plain
     *  {@link Paint} object is required (e.g. font painting) and the large-tile blitting
     *  strategy of {@link #_renderNoise} is not applicable.
     */
    static Paint _createNoisePaint(
        final BoxModelConf      boxModel,
        final Pooled<NoiseConf> noise
    ) {
        final Pooled<NoiseConf> withoutOffset = noise.get().withoutOffsetForRenderCacheAccess();
        final NoisePaintCache noiseRenderer = _NOISE_PAINT_CACHE.computeIfAbsent(withoutOffset, k -> new NoisePaintCache());
        return noiseRenderer.getNoisePaint(boxModel, noise);
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
            _fillShape(g2d, conf.areas().get(style.clipArea()));
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
                            _fillShape(g2d, conf.areas().get(UI.ComponentArea.BODY));
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

        final Tuple<Pooled<Paragraph>> textToRender  = text.content();
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
            final float boundsWidth = textBounds.size().widthOrElse(0f);
            final float boundsX     = textBounds.location().x();
            final float boundsY     = textBounds.location().y();
            final Pair<Float, List<TextLayoutEngine.LayoutLine>> layoutResult = TextLayoutEngine._buildTextLayoutsAndPreferredHeight(font, frc, textToRender, boundsWidth, boundsX, boundsY, wrapLines, conf.boxModel(), text.obstacles(), placement);
            final List<TextLayoutEngine.LayoutLine> lines    = layoutResult.second();
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
        final float localWidth  = Math.max(0, boxModel.size().widthOrElse(0f)  - (insLeft + insets.right().orElse(0f)));
        final float localHeight = Math.max(0, boxModel.size().heightOrElse(0f) - (insTop  + insets.bottom().orElse(0f)));
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
        final List<TextLayoutEngine.LayoutLine> lines,
        final float            totalHeight
    ) {
        final float boundsY      = textBounds.location().y();
        final float boundsHeight = textBounds.size().heightOrElse(0f);
        /*
            ------------------------------------------------
            Phase 3 : Determine visible slice (overflow policy)
            ------------------------------------------------
         */
        final List<TextLayoutEngine.LayoutLine> visible = new ArrayList<>();
        float accumulated = 0;
        if (
            placement == UI.Placement.TOP ||
            placement == UI.Placement.TOP_LEFT ||
            placement == UI.Placement.TOP_RIGHT
        ) {
            for ( TextLayoutEngine.LayoutLine line : lines ) {
                float h = _lineHeight(line, font);
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
            final ListIterator<TextLayoutEngine.LayoutLine> it = lines.listIterator(lines.size());
            while ( it.hasPrevious() ) {
                TextLayoutEngine.LayoutLine line = it.previous();
                float h = _lineHeight(line, font);
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
            for ( TextLayoutEngine.LayoutLine line : lines ) {
                float h = _lineHeight(line, font);
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
        for ( TextLayoutEngine.LayoutLine line : visible ) {
            final TextLayout primary = line.primary().layout;
            if ( primary == null ) {
                y += font.getSize2D();
                continue;
            }
            y += primary.getAscent();

            // Draw all fragments at the same baseline; x positioning is relative
            // to each fragment's own obstacle-free region.
            for ( TextLayoutEngine.LayoutLine.Segment seg : line.segments ) {
                if ( seg.layout != null )
                    _drawLineFragment(g2d, placement, seg.layout, seg.regionX, seg.regionWidth, y);
            }

            y += primary.getDescent() + primary.getLeading();
        }
    }

    private static float _lineHeight( TextLayoutEngine.LayoutLine line, Font font ) {
        final TextLayout primary = line.primary().layout;
        return primary == null
                ? font.getSize2D()
                : primary.getAscent() + primary.getDescent() + primary.getLeading();
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
        final float      width  = size.widthOrElse(0f);
        final float      height = size.heightOrElse(0f);
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

    /**
     *  Caches the work needed to render a {@link NoiseConf} layer.
     *  <p>
     *  Two strategies are used depending on the size of the area being filled:
     *  <ul>
     *      <li>For <b>small</b> areas a {@link NoiseGradientPaint} is reused and the area
     *          is filled directly through {@code g2d.fill(..)}. The {@link NoiseGradientPaint}
     *          internally caches the 32x32 rasters Swing requests, which is optimal here.</li>
     *      <li>For <b>large</b> areas (e.g. window sized backgrounds) the per-pixel
     *          {@link Paint} pipeline becomes the bottleneck: Swing rasterizes and composites
     *          a {@link Paint} in fixed 32x32 chunks, so a large fill turns into thousands of
     *          tiny blits. To avoid this we pre-render the noise into large
     *          {@value #LARGE_TILE_SIZE}x{@value #LARGE_TILE_SIZE} {@link BufferedImage} tiles
     *          and blit those with a single {@code g2d.drawImage(..)} call per tile.</li>
     *  </ul>
     *  The large tiles are keyed in <i>noise space</i> (device space translated so the noise
     *  {@code center} is the origin) rather than device space. The noise pattern only depends
     *  on these noise-space coordinates, so a tile's pixels are invariant with respect to the
     *  component size and the noise offset. Resizing the component or scrolling merely changes
     *  <i>which</i> tiles are visible and <i>where</i> they are drawn - already rendered tiles
     *  stay valid, which keeps a dynamically resized UI responsive.
     */
    private static class NoisePaintCache {

        /** Side length of the pre-rendered large tiles, in (unscaled) device pixels. */
        private static final int LARGE_TILE_SIZE = 256;
        /** Areas larger than this (in pixels) use the large-tile blitting strategy. */
        private static final int LARGE_AREA_THRESHOLD = 64 * 64;
        /** Upper bound on retained large tiles (~256 KiB each), from this cache's slice of
         *  the shared {@link CacheBudget} byte budget. {@code 0} disables tile caching.
         *  Read live (not snapshotted) so a runtime cache-mode change takes effect at once. */
        private static int maxCachedTiles() {
            return CacheBudget.maxEntriesFor(CacheBudget.Kind.NOISE_TILE);
        }

        /** Absolute ceiling on retained per-{@code center} {@link NoiseGradientPaint}s. A static
         *  noise needs a single entry, so this never evicts in the common case; it exists purely
         *  to bound an animated/panning offset, which produces a fresh {@code center} per frame. */
        private static final int MAX_CACHED_PAINTS = 32;
        /** Per-{@code center} paints, evicted least-recently-used first. Unlike the large-tile grid
         *  (which lives in offset-independent noise space and so survives offset animation as-is),
         *  these are keyed by the offset-dependent {@code center}, so without a cap an animated
         *  offset would grow this map without bound. This path is also the fallback used when tile
         *  caching is off (incl. the {@code DISABLED} cache mode), so it must stay functional
         *  regardless of the byte budget — hence a fixed LRU cap rather than a budget-derived one. */
        private final Map<Point2D,NoiseGradientPaint> paintCache =
                new LinkedHashMap<Point2D,NoiseGradientPaint>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry( Map.Entry<Point2D,NoiseGradientPaint> eldest ) {
                        return size() > MAX_CACHED_PAINTS;
                    }
                };
        /** Large pre-rendered tiles, keyed by noise-space tile index, evicted least-recently-used first. */
        private final Map<Long,BufferedImage> largeTileCache =
                new LinkedHashMap<Long,BufferedImage>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry( Map.Entry<Long,BufferedImage> eldest ) {
                        return size() > maxCachedTiles();
                    }
                };


        void renderNoise(
            final BoxModelConf      boxModel,
            final Pooled<NoiseConf> noise,
            final Shape             areaToFill,
            final Graphics2D        g2d
        ) {
            final Color[] colors = noise.get().colors();
            if ( colors.length == 1 ) {
                g2d.setPaint(colors[0]);
                _fillShape(g2d, areaToFill);
                return;
            }

            final Outline insets = boxModel.insetsFor(noise.get().boundary());
            final Point2D.Float center = new Point2D.Float(
                    insets.left().orElse(0f) + noise.get().offset().x(),
                    insets.top().orElse(0f) + noise.get().offset().y()
            );

            final Rectangle bounds = areaToFill.getBounds();
            final long area = (long) bounds.width * bounds.height;

            if ( area <= LARGE_AREA_THRESHOLD || maxCachedTiles() <= 0 ) {
                // Small area (or tile caching disabled): the per-pixel Paint pipeline is fine here.
                g2d.setPaint(getCachedNoisePaint(center, noise));
                _fillShape(g2d, areaToFill);
            } else {
                // Large area: blit pre-rendered large tiles to dodge the 32x32 Paint pipeline.
                _renderWithLargeTiles(center, noise, areaToFill, bounds, g2d);
            }
        }

        /**
         *  Fills {@code areaToFill} by blitting cached large {@link BufferedImage} tiles.
         *  The tile grid lives in noise space (device space minus {@code center}), so the
         *  tiles survive component resizing without cache invalidation.
         */
        private void _renderWithLargeTiles(
            final Point2D.Float     center,
            final Pooled<NoiseConf> noise,
            final Shape             areaToFill,
            final Rectangle         bounds,
            final Graphics2D        g2d
        ) {
            final int size = LARGE_TILE_SIZE;

            // Bounds expressed in noise space (device space translated by 'center'):
            final double uMin = bounds.getMinX() - center.x;
            final double uMax = bounds.getMaxX() - center.x;
            final double vMin = bounds.getMinY() - center.y;
            final double vMax = bounds.getMaxY() - center.y;

            final int tileXMin = Math.floorDiv( (int) Math.floor(uMin), size );
            final int tileXMax = Math.floorDiv( (int) Math.ceil(uMax) - 1, size );
            final int tileYMin = Math.floorDiv( (int) Math.floor(vMin), size );
            final int tileYMax = Math.floorDiv( (int) Math.ceil(vMax) - 1, size );

            final Shape oldClip = g2d.getClip();
            try {
                g2d.clip(areaToFill); // Restricts the tile blits to the requested shape.
                for ( int tileY = tileYMin; tileY <= tileYMax; tileY++ ) {
                    for ( int tileX = tileXMin; tileX <= tileXMax; tileX++ ) {
                        final long key = ((long) tileX << 32) | (tileY & 0xFFFFFFFFL);
                        BufferedImage tile = largeTileCache.get(key);
                        if ( tile == null ) {
                            tile = _renderLargeTile(tileX, tileY, noise, g2d.getDeviceConfiguration());
                            largeTileCache.put(key, tile);
                        }
                        final int drawX = Math.round( tileX * (float) size + center.x );
                        final int drawY = Math.round( tileY * (float) size + center.y );
                        g2d.drawImage(tile, drawX, drawY, null);
                    }
                }
            } catch ( Exception e ) {
                log.error(SwingTree.get().logMarker(), "Failed to render noise using large tiles!", e);
            } finally {
                g2d.setClip(oldClip);
            }
        }

        /**
         *  Renders a single large tile of the noise into a fresh {@link BufferedImage}.
         *  A regular {@link NoiseGradientPaint} centered at {@code (-tileX*size, -tileY*size)}
         *  maps image pixel {@code (px,py)} to noise-space coordinate
         *  {@code (tileX*size + px, tileY*size + py)}, so the tile content depends only on the
         *  tile index - never on the component size or noise offset.
         */
        private BufferedImage _renderLargeTile(
            final int                             tileX,
            final int                             tileY,
            final Pooled<NoiseConf>               noise,
            final @Nullable GraphicsConfiguration gc
        ) {
            final int size = LARGE_TILE_SIZE;
            final NoiseGradientPaint paint = _createNoiseGradientPaint(
                    new Point2D.Float( -tileX * (float) size, -tileY * (float) size ),
                    noise
            );
            final boolean isOpaque = ( paint.getTransparency() == Transparency.OPAQUE );
            final BufferedImage tile = ( gc != null )
                    ? gc.createCompatibleImage(size, size, isOpaque ? Transparency.OPAQUE : Transparency.TRANSLUCENT)
                    : new BufferedImage(size, size, isOpaque ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
            tile.setAccelerationPriority(1.0f);
            final Graphics2D ig = tile.createGraphics();
            try {
                ig.setPaint(paint);
                ig.fillRect(0, 0, size, size);
            } finally {
                ig.dispose();
            }
            return tile;
        }

        Paint getNoisePaint(
            final BoxModelConf      boxModel,
            final Pooled<NoiseConf> noise
        ) {
            final Color[] colors = noise.get().colors();
            if ( colors.length == 1 ) {
                return colors[0];
            }
            final Outline insets = boxModel.insetsFor(noise.get().boundary());
            final Point2D.Float center = new Point2D.Float(
                    insets.left().orElse(0f) + noise.get().offset().x(),
                    insets.top().orElse(0f) + noise.get().offset().y()
            );
            return getCachedNoisePaint(center, noise);
        }

        private NoiseGradientPaint getCachedNoisePaint(
            final Point2D.Float     center,
            final Pooled<NoiseConf> noise
        ) {
            NoiseGradientPaint paint = paintCache.get(center);
            if ( paint != null ) {
                return paint;
            }
            paint = _createNoiseGradientPaint(center, noise);
            paintCache.put(center, paint);
            return paint;
        }

        private static NoiseGradientPaint _createNoiseGradientPaint(
            final Point2D           center,
            final Pooled<NoiseConf> noise
        ) {
            final Color[] colors    = noise.get().colors();
            final float[] fractions = _fractionsFrom(colors, noise.get().fractions());
            final Scale   scale     = noise.get().scale();
            return new NoiseGradientPaint(
                    center,
                    scale.x(),
                    scale.y(),
                    noise.get().rotation(),
                    fractions,
                    colors,
                    noise.get().function()
            );
        }

    }
}
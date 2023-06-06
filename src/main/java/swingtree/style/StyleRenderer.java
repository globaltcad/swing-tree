package swingtree.style;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.geom.*;
import java.util.Objects;
import java.util.function.Function;

/**
 *  This used to smoothly render
 *  custom graphics on top of Swing components without requiring
 *  the user to override the paint method of the component.
 *  This is especially important to allow for declarative UI.
 */
public class StyleRenderer<C extends JComponent>
{
    private final C _comp;
    private final Style style;


    public StyleRenderer( C comp, Style style ) {
        _comp = Objects.requireNonNull(comp);
        this.style = style;
    }

    public void renderBaseStyle(Graphics2D g2d)
    {
        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;

        // We enable antialiasing:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        style.background().foundationColor().ifPresent(outerColor -> {
            _fillOuterBackground(style, outerColor, g2d);
        });
        style.background().color().ifPresent(color -> {
            if ( color.getAlpha() == 0 ) return;
            g2d.setColor(color);
            g2d.fill(_calculateBaseArea(style));
        });

        Font componentFont = _comp.getFont();
        if ( componentFont != null && !componentFont.equals(g2d.getFont()) )
            g2d.setFont( componentFont );

        style.background().painter().ifPresent( backgroundPainter -> {
            backgroundPainter.paint(g2d);
        });

        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );
    }

    public void renderBorderStyle(Graphics2D g2d) {
        // We remember if antialiasing was enabled before we render:
        boolean antialiasingWasEnabled = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING ) == RenderingHints.VALUE_ANTIALIAS_ON;

        // We enable antialiasing:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        style.shadow().color().ifPresent(color -> {
            _renderShadows(style, _comp, g2d, color);
        });
        style.border().color().ifPresent( color -> {
            _drawBorder(style, color, g2d);
        });

        // Reset antialiasing to its previous state:
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasingWasEnabled ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );
    }

    public void renderForegroundStyle(Graphics2D g2d)
    {
        Font componentFont = _comp.getFont();
        if ( componentFont != null && !componentFont.equals(g2d.getFont()) )
            g2d.setFont( componentFont );

        style.foreground().painter().ifPresent( foregroundPainter -> {
            foregroundPainter.paint(g2d);
        });
    }

    private void _drawBorder(Style style, Color color, Graphics2D g2d) {
        if ( !Outline.none().equals(style.border().widths()) ) {
            // The background box is calculated from the margins and border radius:
            int left      = Math.max(style.margin().left().orElse(0),     0);
            int top       = Math.max(style.margin().top().orElse(0),      0);
            int right     = Math.max(style.margin().right().orElse(0),    0);
            int bottom    = Math.max(style.margin().bottom().orElse(0),   0);
            int width     = _comp.getWidth();
            int height    = _comp.getHeight();
            g2d.setColor(color);
            boolean allCornersShareTheSameArc = style.border().allCornersShareTheSameArc();
            boolean allSidesShareTheSameWidth = style.border().allSidesShareTheSameWidth();
            if ( allSidesShareTheSameWidth && allCornersShareTheSameArc ) {
                int arcWidth  = style.border().topLeftArc().map( a -> Math.max(0,a.width() ) ).orElse(0);
                int arcHeight = style.border().topLeftArc().map( a -> Math.max(0,a.height()) ).orElse(0);
                int borderWidth = style.border().widths().top().orElse(0);
                if ( borderWidth <= 0 )
                    return;
                g2d.setStroke(new BasicStroke(borderWidth));
                g2d.drawRoundRect(
                        left, top,
                        width - left - right - 1,
                        height - top - bottom - 1,
                        (arcWidth + (borderWidth == 1 ? 0 : borderWidth + 1)),
                        (arcHeight + (borderWidth == 1 ? 0 : borderWidth + 1))
                    );
            } else {
                Arc topLeftArc     = style.border().topLeftArc().orElse(null);
                Arc topRightArc    = style.border().topRightArc().orElse(null);
                Arc bottomRightArc = style.border().bottomRightArc().orElse(null);
                Arc bottomLeftArc  = style.border().bottomLeftArc().orElse(null);
                int leftBorderWidth   = style.border().widths().left().orElse(0);
                int topBorderWidth    = style.border().widths().top().orElse(0);
                int rightBorderWidth  = style.border().widths().right().orElse(0);
                int bottomBorderWidth = style.border().widths().bottom().orElse(0);
                // A rectangle with corners of different roundness is drawn by drawing
                // four separate arcs:
                // Top left:
                if ( topLeftArc != null ) {
                    float strokeWidth = (leftBorderWidth + topBorderWidth) / 2f;
                    g2d.setStroke(new BasicStroke(strokeWidth));
                    if ( strokeWidth > 0 )
                        g2d.drawArc(
                                left, top,
                                topLeftArc.width(), topLeftArc.height(),
                                90, 90
                            );
                }
                // Top right:
                if ( topRightArc != null ) {
                    float strokeWidth = (rightBorderWidth + topBorderWidth) / 2f;
                    g2d.setStroke(new BasicStroke(strokeWidth));
                    if ( strokeWidth > 0 )
                        g2d.drawArc(
                                width - right - topRightArc.width(), top,
                                topRightArc.width(), topRightArc.height(),
                                0, 90
                            );
                }
                // Bottom right:
                if ( bottomRightArc != null ) {
                    float strokeWidth = (rightBorderWidth + bottomBorderWidth) / 2f;
                    g2d.setStroke(new BasicStroke(strokeWidth));
                    if ( strokeWidth > 0 )
                        g2d.drawArc(
                                width - right - bottomRightArc.width(),
                                height - bottom - bottomRightArc.height(),
                                bottomRightArc.width(),
                                bottomRightArc.height(),
                                270, 90
                            );
                }
                // Bottom left:
                if ( bottomLeftArc != null ) {
                    float strokeWidth = (leftBorderWidth + bottomBorderWidth) / 2f;
                    g2d.setStroke(new BasicStroke(strokeWidth));
                    if ( strokeWidth > 0 )
                        g2d.drawArc(
                                left, height - bottom - bottomLeftArc.height(),
                                bottomLeftArc.width(), bottomLeftArc.height(),
                                180, 90
                            );
                }
                // The four arcs are connected by four lines:
                // Top:
                if ( topBorderWidth > 0 ) {
                    int topLeftArcWidth  = topLeftArc  == null ? 0 : topLeftArc.width()  + topBorderWidth;
                    int topRightArcWidth = topRightArc == null ? 0 : topRightArc.width() + topBorderWidth;
                    g2d.setStroke(new BasicStroke(topBorderWidth));
                    g2d.drawLine(
                            left + topLeftArcWidth / 2, top,
                            width - right - topRightArcWidth / 2, top
                        );
                }
                // Right:
                if ( rightBorderWidth > 0 ) {
                    int topRightArcHeight    = topRightArc    == null ? 0 : topRightArc.height()    + rightBorderWidth;
                    int bottomRightArcHeight = bottomRightArc == null ? 0 : bottomRightArc.height() + rightBorderWidth;
                    g2d.setStroke(new BasicStroke(rightBorderWidth));
                    g2d.drawLine(
                            width - right, top + topRightArcHeight / 2,
                            width - right, height - bottom - bottomRightArcHeight / 2
                        );
                }
                // Bottom:
                if ( bottomBorderWidth > 0 ) {
                    int bottomLeftArcWidth  = bottomLeftArc  == null ? 0 : bottomLeftArc.width()  + bottomBorderWidth;
                    int bottomRightArcWidth = bottomRightArc == null ? 0 : bottomRightArc.width() + bottomBorderWidth;
                    g2d.setStroke(new BasicStroke(bottomBorderWidth));
                    g2d.drawLine(
                            width - right - bottomRightArcWidth / 2, height - bottom,
                            left + bottomLeftArcWidth / 2, height - bottom
                        );
                }
                // Left:
                if ( leftBorderWidth > 0 ) {
                    int topLeftArcHeight    = topLeftArc    == null ? 0 : topLeftArc.height()    + leftBorderWidth;
                    int bottomLeftArcHeight = bottomLeftArc == null ? 0 : bottomLeftArc.height() + leftBorderWidth;
                    g2d.setStroke(new BasicStroke(leftBorderWidth));
                    g2d.drawLine(
                            left, height - bottom - bottomLeftArcHeight / 2,
                            left, top + topLeftArcHeight / 2
                    );
                }
            }
        }
    }

    private Area _calculateBaseArea( Style style )
    {
        // The background box is calculated from the margins and border radius:
        int left      = Math.max(style.margin().left().orElse(0), 0);
        int top       = Math.max(style.margin().top().orElse(0), 0);
        int right     = Math.max(style.margin().right().orElse(0), 0);
        int bottom    = Math.max(style.margin().bottom().orElse(0), 0);
        int width     = _comp.getWidth();
        int height    = _comp.getHeight();

        if ( style.border().allCornersShareTheSameArc() ) {
            int arcWidth  = style.border().topLeftArc().map( a -> Math.max(0,a.width() ) ).orElse(0);
            int arcHeight = style.border().topLeftArc().map( a -> Math.max(0,a.height()) ).orElse(0);
            // We can return a simple round rectangle:
            return new Area(new RoundRectangle2D.Float(
                    left, top, width - left - right, height - top - bottom,
                    arcWidth, arcHeight
                ));
        } else {
            Arc topLeftArc     = style.border().topLeftArc().orElse(null);
            Arc topRightArc    = style.border().topRightArc().orElse(null);
            Arc bottomRightArc = style.border().bottomRightArc().orElse(null);
            Arc bottomLeftArc  = style.border().bottomLeftArc().orElse(null);
            Area area = new Area();

            // Top left:
            if ( topLeftArc != null ) {
                area.add(new Area(new Arc2D.Float(
                        left, top,
                        topLeftArc.width(), topLeftArc.height(),
                        90, 90, Arc2D.PIE
                )));
            }
            // Top right:
            if ( topRightArc != null ) {
                area.add(new Area(new Arc2D.Float(
                        width - right - topRightArc.width(), top,
                        topRightArc.width(), topRightArc.height(),
                        0, 90, Arc2D.PIE
                )));
            }
            // Bottom right:
            if ( bottomRightArc != null ) {
                area.add(new Area(new Arc2D.Float(
                        width - right - bottomRightArc.width(), height - bottom - bottomRightArc.height(),
                        bottomRightArc.width(), bottomRightArc.height(),
                        270, 90, Arc2D.PIE
                )));
            }
            // Bottom left:
            if ( bottomLeftArc != null ) {
                area.add(new Area(new Arc2D.Float(
                        left, height - bottom - bottomLeftArc.height(),
                        bottomLeftArc.width(), bottomLeftArc.height(),
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
                int arcWidthLeft   = (int) Math.floor(topLeftArc  == null ? 0.0 : topLeftArc.width()   / 2.0);
                int arcHeightLeft  = (int) Math.floor(topLeftArc  == null ? 0.0 : topLeftArc.height()  / 2.0);
                int arcWidthRight  = (int) Math.floor(topRightArc == null ? 0.0 : topRightArc.width()  / 2.0);
                int arcHeightRight = (int) Math.floor(topRightArc == null ? 0.0 : topRightArc.height() / 2.0);
                topDistance = Math.max(arcHeightLeft, arcHeightRight);// This is where the center rectangle will start!
                int innerLeft   = left + arcWidthLeft;
                int innerRight  = width - right - arcWidthRight;
                int edgeRectangleHeight = topDistance;
                area.add(new Area(new Rectangle2D.Float(innerLeft, top, innerRight - innerLeft, edgeRectangleHeight)));
            }
            // right:
            if ( topRightArc != null || bottomRightArc != null ) {
                int arcWidthTop    = (int) Math.floor(topRightArc    == null ? 0.0 : topRightArc.width()    / 2.0);
                int arcHeightTop   = (int) Math.floor(topRightArc    == null ? 0.0 : topRightArc.height()   / 2.0);
                int arcWidthBottom = (int) Math.floor(bottomRightArc == null ? 0.0 : bottomRightArc.width() / 2.0);
                int arcHeightBottom= (int) Math.floor(bottomRightArc == null ? 0.0 : bottomRightArc.height()/ 2.0);
                rightDistance = Math.max(arcWidthTop, arcWidthBottom);// This is where the center rectangle will start!
                int innerTop    = top + arcHeightTop;
                int innerBottom = height - bottom - arcHeightBottom;
                int edgeRectangleWidth = rightDistance;
                area.add(new Area(new Rectangle2D.Float(width - right - edgeRectangleWidth, innerTop, edgeRectangleWidth, innerBottom - innerTop)));
            }
            // bottom:
            if ( bottomRightArc != null || bottomLeftArc != null ) {
                int arcWidthRight  = (int) Math.floor(bottomRightArc == null ? 0.0 : bottomRightArc.width()  / 2.0);
                int arcHeightRight = (int) Math.floor(bottomRightArc == null ? 0.0 : bottomRightArc.height() / 2.0);
                int arcWidthLeft   = (int) Math.floor(bottomLeftArc  == null ? 0.0 : bottomLeftArc.width()   / 2.0);
                int arcHeightLeft  = (int) Math.floor(bottomLeftArc  == null ? 0.0 : bottomLeftArc.height()  / 2.0);
                bottomDistance = Math.max(arcHeightRight, arcHeightLeft);// This is where the center rectangle will start!
                int innerLeft   = left + arcWidthLeft;
                int innerRight  = width - right - arcWidthRight;
                int edgeRectangleHeight = bottomDistance;
                area.add(new Area(new Rectangle2D.Float(innerLeft, height - bottom - edgeRectangleHeight, innerRight - innerLeft, edgeRectangleHeight)));
            }
            // left:
            if ( bottomLeftArc != null || topLeftArc != null ) {
                int arcWidthBottom = (int) Math.floor(bottomLeftArc == null ? 0.0 : bottomLeftArc.width() / 2.0);
                int arcHeightBottom= (int) Math.floor(bottomLeftArc == null ? 0.0 : bottomLeftArc.height()/ 2.0);
                int arcWidthTop    = (int) Math.floor(topLeftArc    == null ? 0.0 : topLeftArc.width()    / 2.0);
                int arcHeightTop   = (int) Math.floor(topLeftArc    == null ? 0.0 : topLeftArc.height()   / 2.0);
                leftDistance = Math.max(arcWidthBottom, arcWidthTop);// This is where the center rectangle will start!
                int innerTop    = top + arcHeightTop;
                int innerBottom = height - bottom - arcHeightBottom;
                int edgeRectangleWidth = leftDistance;
                area.add(new Area(new Rectangle2D.Float(left, innerTop, edgeRectangleWidth, innerBottom - innerTop)));
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

    private void _fillOuterBackground( Style style, Color color, Graphics2D g2d ) {
        // Check if the color is transparent
        if ( color.getAlpha() == 0 )
            return;

        int width     = _comp.getWidth();
        int height    = _comp.getHeight();

        Rectangle2D.Float outerRect = new Rectangle2D.Float(0, 0, width, height);

        Area outer = new Area(outerRect);
        Area inner = _calculateBaseArea( style);
        outer.subtract(inner);

        g2d.setColor(color);
        g2d.fill(outer);
    }

    private void _renderShadows(
        Style style,
        JComponent comp,
        Graphics2D g2d,
        Color shadowColor
    ) {
        // First let's check if we need to render any shadows at all
        // Is the shadow color transparent?
        if ( shadowColor.getAlpha() == 0 )
            return;

        ShadowStyle shadow = style.shadow();

        // The background box is calculated from the margins and border radius:
        int left      = Math.max(style.margin().left().orElse(0),   0);
        int top       = Math.max(style.margin().top().orElse(0),    0);
        int right     = Math.max(style.margin().right().orElse(0),  0);
        int bottom    = Math.max(style.margin().bottom().orElse(0), 0);
        int topLeftRadius     = Math.max(style.border().topLeftRadius(), 0);
        int topRightRadius    = Math.max(style.border().topRightRadius(), 0);
        int bottomRightRadius = Math.max(style.border().bottomRightRadius(), 0);
        int bottomLeftRadius  = Math.max(style.border().bottomLeftRadius(), 0);
        int cornerRadius = ( topLeftRadius + topRightRadius + bottomRightRadius + bottomLeftRadius ) / 4;
        int width     = comp.getWidth();
        int height    = comp.getHeight();
        int borderWidth = (int) style.border().widths().average();

        // Calculate the shadow box bounds based on the padding and border thickness
        int xOffset = shadow.horizontalOffset();
        int yOffset = shadow.verticalOffset();
        int x = left   + borderWidth / 2 + xOffset;
        int y = top    + borderWidth / 2 + yOffset;
        int w = width  - left - right  - borderWidth;
        int h = height - top  - bottom - borderWidth;

        int blurRadius   = Math.max(shadow.blurRadius(), 0);
        int spreadRadius = !shadow.isOutset() ? shadow.spreadRadius() : -shadow.spreadRadius();

        Area baseArea = _calculateBaseArea(style);

        int shadowInset  = blurRadius;
        int shadowOutset = blurRadius;
        int borderWidthOffset = borderWidth * ( shadow.isOutset() ? -1 : 0 );

        Rectangle outerShadowRect = new Rectangle(
                                        x - shadowOutset + spreadRadius + borderWidthOffset,
                                        y - shadowOutset + spreadRadius + borderWidthOffset,
                                        w + shadowOutset * 2 - spreadRadius * 2 - borderWidthOffset * 2,
                                        h + shadowOutset * 2 - spreadRadius * 2 - borderWidthOffset * 2
                                    );

        Function<Integer, Integer> offsetFunction = (radius) -> (int)((radius * 2) / ( shadow.isInset() ? 4.5 : 3.79) + ( style.shadow().isInset() ? 0 : borderWidth ));

        int gradientStartOffset = 1 + offsetFunction.apply(cornerRadius);

        Rectangle innerShadowRect = new Rectangle(
                                        x + shadowInset + gradientStartOffset + spreadRadius + borderWidthOffset,
                                        y + shadowInset + gradientStartOffset + spreadRadius + borderWidthOffset,
                                        w - shadowInset * 2 - gradientStartOffset * 2 - spreadRadius * 2 - borderWidthOffset * 2,
                                        h - shadowInset * 2 - gradientStartOffset * 2 - spreadRadius * 2 - borderWidthOffset * 2
                                    );

        Area outerMostArea;

        // Create the shadow shape based on the box bounds and corner arc widths/heights
        Rectangle outerShadowBox = new Rectangle(
                                        outerShadowRect.x,
                                        outerShadowRect.y,
                                        outerShadowRect.width,
                                        outerShadowRect.height
                                    );

        // Apply the clipping to avoid overlapping the shadow and the box
        Area shadowArea = new Area(outerShadowBox);

        if ( shadow.isOutset() )
            shadowArea.subtract(baseArea);
        else
            shadowArea.intersect(baseArea);

        // Draw the corner shadows
        _renderCornerShadow(shadow, Corner.TOP_LEFT,     shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
        _renderCornerShadow(shadow, Corner.TOP_RIGHT,    shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
        _renderCornerShadow(shadow, Corner.BOTTOM_LEFT,  shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
        _renderCornerShadow(shadow, Corner.BOTTOM_RIGHT, shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);

        // Draw the edge shadows
        _renderEdgeShadow(shadow, Edge.TOP,    shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
        _renderEdgeShadow(shadow, Edge.RIGHT,  shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
        _renderEdgeShadow(shadow, Edge.BOTTOM, shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
        _renderEdgeShadow(shadow, Edge.LEFT,   shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);

        outerMostArea = new Area(outerShadowBox);
        // If the base rectangle and the outer shadow box are not equal, then we need to fill the area of the base rectangle that is not covered by the outer shadow box!
        _renderShadowBody(shadow, baseArea, innerShadowRect, outerMostArea, g2d);

    }

    private static void _renderShadowBody(
        ShadowStyle shadowStyle,
        Area baseArea,
        Rectangle innerShadowRect,
        Area outerShadowBox,
        Graphics2D g2d
    ) {
        Graphics2D g2d2 = (Graphics2D) g2d.create();
        g2d2.setColor(shadowStyle.color().orElse(Color.BLACK));
        if ( !shadowStyle.isOutset() ) {
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
        ShadowStyle shadowStyle,
        Corner corner,
        Area areaWhereShadowIsAllowed,
        Rectangle innerShadowRect,
        Rectangle outerShadowRect,
        int gradientStartOffset,
        Graphics2D g2d
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
        Color shadowBackgroundColor = new Color(0,0,0,0);
        if ( shadowStyle.isOutset() ) {
            innerColor = shadowStyle.color().orElse(Color.BLACK);
            outerColor = shadowBackgroundColor;
        } else {
            innerColor = shadowBackgroundColor;
            outerColor = shadowStyle.color().orElse(Color.BLACK);
        }
        float gradientStart = (float) gradientStartOffset / cr;

        // The first thing we can do is to clip the corner box to the area where the shadow is allowed
        Area cornerArea = new Area(cornerBox);
        cornerArea.intersect(areaWhereShadowIsAllowed);

        // In the simplest case we don't need to do any gradient painting:
        if ( gradientStart == 1f || gradientStart == 0f ) {
            // Simple, we just draw a circle and clip it
            Area circle = new Area(new Ellipse2D.Float(cx - cr, cy - cr, cr * 2, cr * 2));
            if ( shadowStyle.isInset() ) {
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
            ShadowStyle shadowStyle,
            Edge edge,
            Area contentArea,
            Rectangle innerShadowRect,
            Rectangle outerShadowRect,
            int gradientStartOffset,
            Graphics2D g2d
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
        Color shadowBackgroundColor = new Color(0,0,0,0);
        if (shadowStyle.isOutset()) {
            innerColor = shadowStyle.color().orElse(Color.BLACK);
            outerColor = shadowBackgroundColor;
        } else {
            innerColor = shadowBackgroundColor;
            outerColor = shadowStyle.color().orElse(Color.BLACK);
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
                if ( shadowStyle.isOutset() )
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

    public Insets calculateBorderInsets(Insets formerInsets) {
        int left      = style.margin().left().orElse(formerInsets.left);
        int top       = style.margin().top().orElse(formerInsets.top);
        int right     = style.margin().right().orElse(formerInsets.right);
        int bottom    = style.margin().bottom().orElse(formerInsets.bottom);
        // Add padding:
        left   += style.padding().left().orElse(0);
        top    += style.padding().top().orElse(0);
        right  += style.padding().right().orElse(0);
        bottom += style.padding().bottom().orElse(0);
        // Add border widths:
        left   += Math.max(style.border().widths().left().orElse(0),   0)/2;
        top    += Math.max(style.border().widths().top().orElse(0),    0)/2;
        right  += Math.max(style.border().widths().right().orElse(0),  0)/2;
        bottom += Math.max(style.border().widths().bottom().orElse(0), 0)/2;
        return new Insets(top, left, bottom, right);
    }

}

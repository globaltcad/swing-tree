package swingtree.style;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;

/**
 *  This used to smoothly render
 *  custom graphics on top of Swing components without requiring
 *  the user to override the paint method of the component.
 *  This is especially important to allow for declarative UI.
 */
public class StyleRenderer<C extends JComponent>
{
    private enum Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
    private enum Side { TOP, RIGHT, BOTTOM, LEFT }

    private final Graphics2D _g2d;
    private final C _comp;


    public StyleRenderer( Graphics2D g2d, C comp ) {
        _g2d = Objects.requireNonNull(g2d);
        _comp = Objects.requireNonNull(comp);
    }

    public void renderBaseStyle(Style style )
    {
        style.background().foundationColor().ifPresent(outerColor -> {
            _fillOuterBackground(style, outerColor);
        });
        style.background().color().ifPresent(color -> {
            _fillBackground(style, color);
        });
        style.background().painter().ifPresent( backgroundPainter -> {
            backgroundPainter.paint(_g2d);
        });
        style.shadow().color().ifPresent(color -> {
            _renderShadows(style, _comp, _g2d, color);
        });
        style.border().color().ifPresent( color -> {
            _drawBorder(style, color);
        });
    }

    public void renderForegroundStyle(Style style )
    {
        style.foreground().painter().ifPresent( foregroundPainter -> {
            foregroundPainter.paint(_g2d);
        });
    }

    private void _drawBorder(Style style, Color color) {
        if ( style.border().width() > 0 ) {
            // The background box is calculated from the margins and border radius:
            int left      = Math.max(style.margin().left().orElse(0),     0);
            int top       = Math.max(style.margin().top().orElse(0),      0);
            int right     = Math.max(style.margin().right().orElse(0),    0);
            int bottom    = Math.max(style.margin().bottom().orElse(0),   0);
            int arcWidth  = Math.max(style.border().arcWidth(), 0);
            int arcHeight = Math.max(style.border().arcHeight(),0);
            int width     = _comp.getWidth();
            int height    = _comp.getHeight();
            _g2d.setColor(color);
            _g2d.setStroke(new BasicStroke(style.border().width()));
            _g2d.drawRoundRect(
                    left, top,
                    width  - left - right - 1,
                    height - top - bottom - 1,
                    (arcWidth  + (style.border().width() == 1 ? 0 : style.border().width()+1)),
                    (arcHeight + (style.border().width() == 1 ? 0 : style.border().width()+1))
                );
        }
    }

    private void _fillBackground( Style style, Color color ) {
        // Check if the color is transparent
        if ( color.getAlpha() == 0 )
            return;

        // The background box is calculated from the margins and border radius:
        int left      = Math.max(style.margin().left().orElse(0),     0);
        int top       = Math.max(style.margin().top().orElse(0),      0);
        int right     = Math.max(style.margin().right().orElse(0),    0);
        int bottom    = Math.max(style.margin().bottom().orElse(0),   0);
        int arcWidth  = Math.max(style.border().arcWidth(), 0);
        int arcHeight = Math.max(style.border().arcHeight(),0);
        int width     = _comp.getWidth();
        int height    = _comp.getHeight();

        _g2d.setColor(color);
        _g2d.fillRoundRect(
                left, top, width - left - right, height - top - bottom,
                arcWidth, arcHeight
            );
    }

    private void _fillOuterBackground( Style style, Color color ) {
        // Check if the color is transparent
        if ( color.getAlpha() == 0 )
            return;

        // The background box is calculated from the margins and border radius:
        int left      = Math.max(style.margin().left().orElse(0),   0);
        int top       = Math.max(style.margin().top().orElse(0),    0);
        int right     = Math.max(style.margin().right().orElse(0),  0);
        int bottom    = Math.max(style.margin().bottom().orElse(0), 0);
        int arcWidth  = Math.max(style.border().arcWidth(), 0);
        int arcHeight = Math.max(style.border().arcHeight(),0);
        int width     = _comp.getWidth();
        int height    = _comp.getHeight();

        Rectangle2D.Float outerRect = new Rectangle2D.Float(0, 0, width, height);
        RoundRectangle2D.Float innerRect = new RoundRectangle2D.Float(
                                                        left, top,
                                                        width - left - right,
                                                        height - top - bottom,
                                                        arcWidth, arcHeight
                                                    );

        Area outer = new Area(outerRect);
        Area inner = new Area(innerRect);
        outer.subtract(inner);

        _g2d.setColor(color);
        _g2d.fill(outer);
    }

    private static void _renderShadows(
        Style style,
        JComponent comp,
        Graphics2D g2d,
        Color shadowColor
    ) {
        // First let's check if we need to render any shadows at all
        // Is the shadow color transparent?
        if ( shadowColor.getAlpha() == 0 )
            return;

        // The background box is calculated from the margins and border radius:
        int left      = Math.max(style.margin().left().orElse(0),   0);
        int top       = Math.max(style.margin().top().orElse(0),    0);
        int right     = Math.max(style.margin().right().orElse(0),  0);
        int bottom    = Math.max(style.margin().bottom().orElse(0), 0);
        int arcWidth  = Math.max(style.border().arcWidth(), 0);
        int arcHeight = Math.max(style.border().arcHeight(),0);
        int width     = comp.getWidth();
        int height    = comp.getHeight();

        // Calculate the shadow box bounds based on the padding and border thickness
        int x = left   + style.border().width() / 2 + style.shadow().horizontalOffset();
        int y = top    + style.border().width() / 2 + style.shadow().verticalOffset();
        int w = width  - left - right  - style.border().width();
        int h = height - top  - bottom - style.border().width();

        int blurRadius   = Math.max(style.shadow().blurRadius(), 0);
        int spreadRadius = !style.shadow().isOutset() ? style.shadow().spreadRadius() : -style.shadow().spreadRadius();

        RoundRectangle2D.Float baseRect = new RoundRectangle2D.Float(
                                                        left + (float) style.border().width() / 2,
                                                        top  + (float) style.border().width() / 2,
                                                            w, h,
                                                            arcWidth, arcHeight
                                                    );

        int shadowInset  = blurRadius;
        int shadowOutset = blurRadius;
        int borderWidthOffset = style.border().width() * ( style.shadow().isOutset() ? -1 : 0 );

        Rectangle outerShadowRect = new Rectangle(
                                        x - shadowOutset + spreadRadius + borderWidthOffset,
                                        y - shadowOutset + spreadRadius + borderWidthOffset,
                                        w + shadowOutset * 2 - spreadRadius * 2 - borderWidthOffset * 2,
                                        h + shadowOutset * 2 - spreadRadius * 2 - borderWidthOffset * 2
                                    );

        int gradientStartOffset = (int)(( arcWidth + arcHeight ) / ( style.shadow().isInset() ? 4.5 : 3.79) );
        gradientStartOffset += ( style.shadow().isInset() ? 0 : style.border().width() );


        Rectangle innerShadowRect = new Rectangle(
                                        x + shadowInset + gradientStartOffset + spreadRadius + borderWidthOffset,
                                        y + shadowInset + gradientStartOffset + spreadRadius + borderWidthOffset,
                                        w - shadowInset * 2 - gradientStartOffset * 2 - spreadRadius * 2 - borderWidthOffset * 2,
                                        h - shadowInset * 2 - gradientStartOffset * 2 - spreadRadius * 2 - borderWidthOffset * 2
                                    );

        Area baseArea;
        Area outerMostArea;

        if ( blurRadius > 0 )
        {
            // Create the shadow shape based on the box bounds and corner arc widths/heights
            Rectangle outerShadowBox = new Rectangle(
                                            outerShadowRect.x,
                                            outerShadowRect.y,
                                            outerShadowRect.width,
                                            outerShadowRect.height
                                        );

            // Apply the clipping to avoid overlapping the shadow and the box
            Area shadowArea = new Area(outerShadowBox);
            baseArea = new Area(baseRect);

            if ( !style.shadow().isOutset() )
                shadowArea.intersect(baseArea);
            else
                shadowArea.subtract(baseArea);

            // Draw the corner shadows
            _renderCornerShadow(style, Corner.TOP_LEFT,     shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
            _renderCornerShadow(style, Corner.TOP_RIGHT,    shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
            _renderCornerShadow(style, Corner.BOTTOM_LEFT,  shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
            _renderCornerShadow(style, Corner.BOTTOM_RIGHT, shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);

            // Draw the edge shadows
            _renderEdgeShadow(style, Side.TOP,    shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
            _renderEdgeShadow(style, Side.RIGHT,  shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
            _renderEdgeShadow(style, Side.BOTTOM, shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);
            _renderEdgeShadow(style, Side.LEFT,   shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset, g2d);

            outerMostArea = new Area(outerShadowBox);
        }
        else // Easy, we simply fill a round rectangle with the shadow color
        {
            // Create the shadow shape based on the box bounds and corner arc widths/heights
            RoundRectangle2D.Float outerShadowBox = new RoundRectangle2D.Float(
                                                        outerShadowRect.x,
                                                        outerShadowRect.y,
                                                        outerShadowRect.width,
                                                        outerShadowRect.height,
                                                        (float)(arcWidth  + gradientStartOffset / 4),
                                                        (float)(arcHeight + gradientStartOffset / 4)
                                                    );

            // Apply the clipping to avoid overlapping the shadow and the box
            Area shadowArea = new Area(outerShadowBox);
            baseArea = new Area(baseRect);

            g2d.setColor(shadowColor);

            if ( !style.shadow().isOutset() ) {
                shadowArea.intersect(baseArea);
            } else {
                shadowArea.subtract(baseArea);
                g2d.fill(shadowArea);
            }

            outerMostArea = new Area(outerShadowBox);
        }
        // If the base rectangle and the outer shadow box are not equal, then we need to fill the area of the base rectangle that is not covered by the outer shadow box!
        _renderShadowBody(style, baseArea, innerShadowRect, outerMostArea, g2d);

    }

    private static void _renderShadowBody(
        Style style,
        Area baseArea,
        Rectangle innerShadowRect,
        Area outerShadowBox,
        Graphics2D g2d
    ) {
        Graphics2D g2d2 = (Graphics2D) g2d.create();
        g2d2.setColor(style.shadow().color().orElse(Color.BLACK));
        if ( !style.shadow().isOutset() ) {
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
        Style style,
        Corner corner,
        Area boxArea,
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
        Rectangle2D.Float cornerClipBox;

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
        if ( style.shadow().isOutset() ) {
            innerColor = style.shadow().color().orElse(Color.BLACK);
            outerColor = shadowBackgroundColor;
        } else {
            innerColor = shadowBackgroundColor;
            outerColor = style.shadow().color().orElse(Color.BLACK);
        }
        RadialGradientPaint cornerPaint;
        float gradientStart = (float) gradientStartOffset / cr;
        if ( gradientStart >= 1f || gradientStart <= 0f )
            cornerPaint = new RadialGradientPaint(
                             cx, cy, cr,
                             new float[] {0f, 1f},
                             new Color[] {innerColor, outerColor}
                         );
        else {
            cornerPaint = new RadialGradientPaint(
                             cx, cy, cr,
                             new float[] {0f, gradientStart, 1f},
                             new Color[] {innerColor, innerColor, outerColor}
                         );
        }

        // We need to clip the corner paint to the corner box
        Area cornerArea = new Area(cornerBox);
        cornerArea.intersect(boxArea);
        cornerArea.intersect(new Area(cornerClipBox));

        Graphics2D cornerG2d = (Graphics2D) g2d.create();
        cornerG2d.setPaint(cornerPaint);
        cornerG2d.fill(cornerArea);
        cornerG2d.dispose();
    }

    private static void _renderEdgeShadow(
            Style style,
            Side side,
            Area boxArea,
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
        switch (side) {
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
                throw new IllegalArgumentException("Invalid side: " + side);
        }

        if ( gradStartX == gradEndX && gradStartY == gradEndY ) return;

        Color innerColor;
        Color outerColor;
        Color shadowBackgroundColor = new Color(0,0,0,0);
        if (style.shadow().isOutset()) {
            innerColor = style.shadow().color().orElse(Color.BLACK);
            outerColor = shadowBackgroundColor;
        } else {
            innerColor = shadowBackgroundColor;
            outerColor = style.shadow().color().orElse(Color.BLACK);
        }
        LinearGradientPaint edgePaint;
        // distance between start and end of gradient
        float dist = (float) Math.sqrt(
                                    (gradEndX - gradStartX) * (gradEndX - gradStartX) +
                                    (gradEndY - gradStartY) * (gradEndY - gradStartY)
                                );
        float gradientStart = (float) gradientStartOffset / dist;
        if ( gradientStart >= 1f || gradientStart <= 0f )
            edgePaint = new LinearGradientPaint(
                               gradStartX, gradStartY,
                               gradEndX, gradEndY,
                               new float[] {0f, 1f},
                               new Color[] {innerColor, outerColor}
                           );
        else {
            edgePaint = new LinearGradientPaint(
                             gradStartX, gradStartY,
                             gradEndX, gradEndY,
                             new float[] {0f, gradientStart, 1f},
                             new Color[] {innerColor, innerColor, outerColor}
                         );
        }

        // We need to clip the edge paint to the edge box
        Area edgeArea = new Area(edgeBox);
        edgeArea.intersect(boxArea);
        if ( edgeClipBox != null )
            edgeArea.intersect(new Area(edgeClipBox));

        Graphics2D edgeG2d = (Graphics2D) g2d.create();
        edgeG2d.setPaint(edgePaint);
        edgeG2d.fill(edgeArea);
        edgeG2d.dispose();
    }

}

package swingtree;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;

/**
 *  This is a simple wrapper as well as delegate for a Graphics2D object and
 *  a JComponent. It provides a fluent API for painting a component.
 */
public class RenderDelegate<C extends JComponent>
{

    private enum Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
    private enum Side { TOP, RIGHT, BOTTOM, LEFT }

    private final Graphics2D g2d;
    private final C comp;

    RenderDelegate(Graphics2D g2d, C comp ) {
        Objects.requireNonNull(g2d);
        Objects.requireNonNull(comp);
        this.g2d = g2d;
        this.comp = comp;
    }

    public Graphics2D graphics2D() { return g2d; }

    public C component() { return comp; }

    private void _drawBorder(StyleCollector style) {
        if ( style.getBorderThickness() > 0 ) {
            g2d.setColor(style.getBorderColor());
            g2d.setStroke(new BasicStroke(style.getBorderThickness()));
            g2d.drawRoundRect(
                    style.getPaddingLeft(), style.getPaddingTop(),
                    comp.getWidth() - style.getPaddingLeft() - style.getPaddingRight(),
                    comp.getHeight() - style.getPaddingTop() - style.getPaddingBottom(),
                    (style.getBorderArcWidth()  + (style.getBorderThickness() == 1 ? 0 : style.getBorderThickness()+1)),
                    (style.getBorderArcHeight() + (style.getBorderThickness() == 1 ? 0 : style.getBorderThickness()+1))
                );
            g2d.drawRoundRect(
                    style.getPaddingLeft(), style.getPaddingTop(),
                    comp.getWidth() - style.getPaddingLeft() - style.getPaddingRight(),
                    comp.getHeight() - style.getPaddingTop() - style.getPaddingBottom(),
                    (style.getBorderArcWidth() +2),
                    (style.getBorderArcHeight()+2)
            );
        }
    }

    private void _fillBackground(StyleCollector style, Color color ) {
        // Check if the color is transparent
        if ( color.getAlpha() == 0 )
            return;

        g2d.setColor(color);
        g2d.fillRoundRect(
                style.getPaddingLeft(), style.getPaddingTop(),
                comp.getWidth() - style.getPaddingLeft() - style.getPaddingRight(),
                comp.getHeight() - style.getPaddingTop() - style.getPaddingBottom(),
                style.getBorderArcWidth(), style.getBorderArcHeight()
            );
    }

    private void _fillOuterBackground( StyleCollector style, Color color ) {
        // Check if the color is transparent
        if ( color.getAlpha() == 0 )
            return;

        Rectangle2D.Float outerRect = new Rectangle2D.Float(
                                            0, 0,
                                            comp.getWidth(),
                                            comp.getHeight()
                                        );
        RoundRectangle2D.Float innerRect = new RoundRectangle2D.Float(
                                            style.getPaddingLeft(), style.getPaddingTop(),
                                            comp.getWidth() - style.getPaddingLeft() - style.getPaddingRight(),
                                            comp.getHeight() - style.getPaddingTop() - style.getPaddingBottom(),
                                            style.getBorderArcWidth(), style.getBorderArcHeight()
                                        );

        Area outer = new Area(outerRect);
        Area inner = new Area(innerRect);
        outer.subtract(inner);

        g2d.setColor(color);
        g2d.fill(outer);

    }

    public void render(StyleCollector style){
        _fillOuterBackground(style, style.getOuterBackgroundColor());
        _fillBackground(style, style.getBackgroundColor());
        _renderShadows(style, comp, g2d);
        this._drawBorder(style);
    }

    private static void _renderShadows(
            StyleCollector style,
            JComponent comp,
            Graphics2D g2d
    ) {
        // First let's check if we need to render any shadows at all
        // Is the shadow color transparent?
        if ( style.getShadowColor().getAlpha() == 0 )
            return;

        // Calculate the shadow box bounds based on the padding and border thickness
        int x = style.getPaddingLeft() + style.getBorderThickness()/2 + style.getHorizontalShadowOffset();
        int y = style.getPaddingTop() + style.getBorderThickness()/2 + style.getVerticalShadowOffset();
        int w = comp.getWidth() - style.getPaddingLeft() - style.getPaddingRight() - style.getBorderThickness();
        int h = comp.getHeight() - style.getPaddingTop() - style.getPaddingBottom() - style.getBorderThickness();

        int blurRadius = style.getShadowBlurRadius();
        int spreadRadius = !style.isShadowInset() ? style.getShadowSpreadRadius() : -style.getShadowSpreadRadius();

        RoundRectangle2D.Float baseRect = new RoundRectangle2D.Float(
                                                        style.getPaddingLeft() + (float) style.getBorderThickness() /2,
                                                        style.getPaddingTop() + (float) style.getBorderThickness() /2,
                                                            w, h,
                                                            style.getBorderArcWidth(), style.getBorderArcHeight()
                                                    );

        int shadowInset  = blurRadius;
        int shadowOutset = blurRadius;

        int gradientStartOffset = ( style.getBorderArcWidth() + style.getBorderArcHeight() ) / 5;

        Rectangle innerShadowRect = new Rectangle(
                                        x + shadowInset + gradientStartOffset + spreadRadius,
                                        y + shadowInset + gradientStartOffset + spreadRadius,
                                        w - shadowInset * 2 - gradientStartOffset * 2 - spreadRadius * 2,
                                        h - shadowInset * 2 - gradientStartOffset * 2 - spreadRadius * 2
                                    );

        Rectangle outerShadowRect = new Rectangle(
                                        x - shadowOutset - gradientStartOffset + spreadRadius,
                                        y - shadowOutset - gradientStartOffset + spreadRadius,
                                        w + shadowOutset * 2 + gradientStartOffset * 2 - spreadRadius * 2,
                                        h + shadowOutset * 2 + gradientStartOffset * 2 - spreadRadius * 2
                                    );

        // Create the shadow shape based on the box bounds and corner arc widths/heights
        RoundRectangle2D.Float outerShadowBox = new RoundRectangle2D.Float(
                                                            outerShadowRect.x,
                                                            outerShadowRect.y,
                                                            outerShadowRect.width,
                                                            outerShadowRect.height,
                                                            style.getBorderArcWidth(), style.getBorderArcHeight()
                                                        );

        // Apply the clipping to avoid overlapping the shadow and the box
        Area shadowArea = new Area(outerShadowBox);
        Area baseArea = new Area(baseRect);

        if ( !style.isShadowInset() )
            shadowArea.intersect(baseArea);
        else
            shadowArea.subtract(baseArea);

        //if ( blurRadius != 0 || spreadRadius != 0 )
        {
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
        }

        // If the base rectangle and the outer shadow box are not equal, then we need to fill the area of the base rectangle that is not covered by the outer shadow box!
        _renderShadowBody(style, baseArea, innerShadowRect, outerShadowBox, g2d);
    }

    private static void _renderShadowBody(
            StyleCollector style,
            Area baseArea,
            Rectangle innerShadowRect,
            RoundRectangle2D.Float outerShadowBox,
            Graphics2D g2d
    ) {
        Graphics2D g2d2 = (Graphics2D) g2d.create();
        g2d2.setColor(style.getShadowColor());
        if ( !style.isShadowInset() ) {
            baseArea.subtract(new Area(outerShadowBox));
            g2d2.fill(baseArea);
        } else {
            Area innerShadowArea = new Area(innerShadowRect);
            innerShadowArea.subtract(baseArea);
            g2d2.fill(innerShadowArea);
        }
        g2d2.dispose();
    }

    private static void _renderCornerShadow(
        StyleCollector style,
        Corner corner,
        Area boxArea,
        Rectangle innerShadowRect,
        Rectangle outerShadowRect,
        int gradientStartOffset,
        Graphics2D g2d
    ) {
        // Draw the corner shadows
        Rectangle2D.Float cornerBox;
        float cx;
        float cy;
        float cr; // depending on the corner, this is either the corner box width or height
        switch (corner) {
            case TOP_LEFT:
                cornerBox = new Rectangle2D.Float(
                            outerShadowRect.x,
                            outerShadowRect.y,
                            innerShadowRect.x - outerShadowRect.x,
                            innerShadowRect.y - outerShadowRect.y
                            );
                cx = cornerBox.x + cornerBox.width;
                cy = cornerBox.y + cornerBox.height;
                cr = cornerBox.width;
                break;
            case TOP_RIGHT:
                cornerBox = new Rectangle2D.Float(
                                innerShadowRect.x + innerShadowRect.width,
                                outerShadowRect.y,
                            outerShadowRect.x + outerShadowRect.width - innerShadowRect.x - innerShadowRect.width,
                            innerShadowRect.y - outerShadowRect.y
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
                cx = cornerBox.x + cornerBox.width;
                cy = cornerBox.y;
                cr = cornerBox.width;
                break;
            case BOTTOM_RIGHT:
                cornerBox = new Rectangle2D.Float(
                            innerShadowRect.x + innerShadowRect.width,
                            innerShadowRect.y + innerShadowRect.height,
                            outerShadowRect.x + outerShadowRect.width - innerShadowRect.x - innerShadowRect.width,
                            outerShadowRect.y + outerShadowRect.height - innerShadowRect.y - innerShadowRect.height
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
        if (style.isShadowInset()) {
            innerColor = style.getShadowColor();
            outerColor = shadowBackgroundColor;
        } else {
            innerColor = shadowBackgroundColor;
            outerColor = style.getShadowColor();
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

        Graphics2D cornerG2d = (Graphics2D) g2d.create();
        cornerG2d.setPaint(cornerPaint);
        cornerG2d.fill(cornerArea);
        cornerG2d.dispose();
    }

    private static void _renderEdgeShadow(
            StyleCollector style,
            Side side,
            Area boxArea,
            Rectangle innerShadowRect,
            Rectangle outerShadowRect,
            int gradientStartOffset,
            Graphics2D g2d
    ) {
        Rectangle2D.Float edgeBox;
        float gradEndX;
        float gradEndY;
        float gradStartX;
        float gradStartY;
        switch (side) {
            case TOP:
                edgeBox = new Rectangle2D.Float(
                                innerShadowRect.x,
                                outerShadowRect.y,
                                innerShadowRect.width,
                                innerShadowRect.y - outerShadowRect.y
                            );
                gradEndX = edgeBox.x;
                gradEndY = edgeBox.y;
                gradStartX = edgeBox.x;
                gradStartY = edgeBox.y + edgeBox.height;
                break;
            case RIGHT:
                edgeBox = new Rectangle2D.Float(
                            innerShadowRect.x + innerShadowRect.width,
                            innerShadowRect.y,
                            outerShadowRect.x + outerShadowRect.width - innerShadowRect.x - innerShadowRect.width,
                            innerShadowRect.height
                            );
                gradEndX = edgeBox.x + edgeBox.width;
                gradEndY = edgeBox.y;
                gradStartX = edgeBox.x;
                gradStartY = edgeBox.y;
                break;
            case BOTTOM:
                edgeBox = new Rectangle2D.Float(
                                innerShadowRect.x,
                                innerShadowRect.y + innerShadowRect.height,
                                innerShadowRect.width,
                                outerShadowRect.y + outerShadowRect.height - innerShadowRect.y - innerShadowRect.height
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
        if (style.isShadowInset()) {
            innerColor = style.getShadowColor();
            outerColor = shadowBackgroundColor;
        } else {
            innerColor = shadowBackgroundColor;
            outerColor = style.getShadowColor();
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

        Graphics2D edgeG2d = (Graphics2D) g2d.create();
        edgeG2d.setPaint(edgePaint);
        edgeG2d.fill(edgeArea);
        edgeG2d.dispose();
    }

}

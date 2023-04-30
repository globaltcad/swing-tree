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

    // Padding
    private int paddingTop = 0;
    private int paddingLeft = 0;
    private int paddingRight = 0;
    private int paddingBottom = 0;

    // Border
    private int borderArcWidth = 0;
    private int borderArcHeight = 0;
    private int borderThickness = 0;
    private Color borderColor = Color.BLACK;

    // Background
    private Color backgroundColor = Color.WHITE;

    // Box Shadow
    private int horizontalShadowOffset = 0;
    private int verticalShadowOffset = 0;
    private int shadowBlurRadius = 5;
    private int shadowSpreadRadius = 5;
    private Color shadowColor = Color.BLACK;
    private Color shadowBackgroundColor = null;
    private boolean shadowInset = false;


    RenderDelegate(Graphics2D g2d, C comp ) {
        Objects.requireNonNull(g2d);
        Objects.requireNonNull(comp);
        this.g2d = g2d;
        this.comp = comp;
    }

    public Graphics2D graphics2D() { return g2d; }

    public C component() { return comp; }

    public RenderDelegate<C> pad(int top, int left, int right, int bottom ) {
        this.paddingTop = top;
        this.paddingLeft = left;
        this.paddingRight = right;
        this.paddingBottom = bottom;
        return this;
    }

    public RenderDelegate<C> pad(int padding ) {
        this.paddingTop = padding;
        this.paddingLeft = padding;
        this.paddingRight = padding;
        this.paddingBottom = padding;
        return this;
    }

    public RenderDelegate<C> padTop(int padding ) {
        this.paddingTop = padding;
        return this;
    }

    public RenderDelegate<C> padLeft(int padding ) {
        this.paddingLeft = padding;
        return this;
    }

    public RenderDelegate<C> padRight(int padding ) {
        this.paddingRight = padding;
        return this;
    }

    public RenderDelegate<C> padBottom(int padding ) {
        this.paddingBottom = padding;
        return this;
    }

    public RenderDelegate<C> border(int thickness, Color color ) {
        this.borderThickness = thickness;
        this.borderColor = color;
        return this;
    }

    public RenderDelegate<C> border( int thickness ) {
        this.borderThickness = thickness;
        return this;
    }

    public RenderDelegate<C> border(Color color ) {
        this.borderColor = color;
        return this;
    }

    public RenderDelegate<C> borderRadius(int radius ) {
        this.borderArcWidth = radius;
        this.borderArcHeight = radius;
        return this;
    }

    public RenderDelegate<C> borderRadius(int arcWidth, int arcHeight ) {
        this.borderArcWidth = arcWidth;
        this.borderArcHeight = arcHeight;
        return this;
    }

    public RenderDelegate<C> background(Color color ) {
        this.backgroundColor = color;
        return this;
    }

    public RenderDelegate<C> shadowHorizontalOffset(int offset ) {
        this.horizontalShadowOffset = offset;
        return this;
    }

    public RenderDelegate<C> shadowVerticalOffset(int offset ) {
        this.verticalShadowOffset = offset;
        return this;
    }

    public RenderDelegate<C> shadowOffset(int horizontalOffset, int verticalOffset ) {
        this.horizontalShadowOffset = horizontalOffset;
        this.verticalShadowOffset = verticalOffset;
        return this;
    }

    public RenderDelegate<C> shadowBlurRadius(int radius) {
        this.shadowBlurRadius = radius;
        return this;
    }

    public RenderDelegate<C> shadowSpreadRadius(int radius ) {
        this.shadowSpreadRadius = radius;
        return this;
    }

    public RenderDelegate<C> shadowColor(Color color ) {
        this.shadowColor = color;
        return this;
    }

    public RenderDelegate<C> shadowBackgroundColor(Color color ) {
        this.shadowBackgroundColor = color;
        return this;
    }

    public RenderDelegate<C> shadowInset(boolean b) {
        shadowInset = b;
        return this;
    }

    public void drawBorder() {
        if ( borderThickness > 0 ) {
            g2d.setColor(borderColor);
            g2d.setStroke(new BasicStroke(borderThickness));
            g2d.drawRoundRect(
                    paddingLeft, paddingTop,
                    comp.getWidth() - paddingLeft - paddingRight,
                    comp.getHeight() - paddingTop - paddingBottom,
                    (borderArcWidth  + (borderThickness == 1 ? 0 : borderThickness+1)),
                    (borderArcHeight + (borderThickness == 1 ? 0 : borderThickness+1))
                );
            g2d.drawRoundRect(
                    paddingLeft, paddingTop,
                    comp.getWidth() - paddingLeft - paddingRight,
                    comp.getHeight() - paddingTop - paddingBottom,
                    (borderArcWidth +2),
                    (borderArcHeight+2)
            );
        }
    }

    public void fill( Color color ) {
        g2d.setColor(color);
        g2d.fillRoundRect(
                paddingLeft, paddingTop,
                comp.getWidth() - paddingLeft - paddingRight,
                comp.getHeight() - paddingTop - paddingBottom,
                borderArcWidth, borderArcHeight
            );
    }

    public void renderShadows() {

        // First let's check if we need to render any shadows at all
        // Is the shadow color transparent?
        if ( shadowColor.getAlpha() == 0 )
            return;

        // Save the current graphics state
        Graphics2D savedG2d = (Graphics2D) g2d.create();

        // Calculate the shadow box bounds based on the padding and border thickness
        int x = paddingLeft + borderThickness/2 + horizontalShadowOffset;
        int y = paddingTop + borderThickness/2 + verticalShadowOffset;
        int w = comp.getWidth() - paddingLeft - paddingRight - borderThickness;
        int h = comp.getHeight() - paddingTop - paddingBottom - borderThickness;

        RoundRectangle2D.Float baseRect = new RoundRectangle2D.Float(
                                                    paddingLeft + (float) borderThickness /2,
                                                    paddingTop + (float) borderThickness /2,
                                                        w, h, borderArcWidth, borderArcHeight
                                                    );

        int shadowInset = this.shadowInset ? this.shadowBlurRadius : this.shadowSpreadRadius;
        int shadowOutset = this.shadowInset ? this.shadowSpreadRadius : this.shadowBlurRadius;

        int gradientStartOffset = ( borderArcWidth + borderArcHeight ) / 5;

        Rectangle innerShadowRect = new Rectangle(
                                        x + shadowInset + gradientStartOffset,
                                        y + shadowInset + gradientStartOffset,
                                        w - shadowInset * 2 - gradientStartOffset * 2,
                                        h - shadowInset * 2 - gradientStartOffset * 2
                                    );

        Rectangle outerShadowRect = new Rectangle(
                                        x - shadowOutset,
                                        y - shadowOutset,
                                        w + shadowOutset * 2,
                                        h + shadowOutset * 2
                                    );

        // Create the shadow shape based on the box bounds and corner arc widths/heights
        RoundRectangle2D.Float outerShadowBox = new RoundRectangle2D.Float(
                                                            outerShadowRect.x,
                                                            outerShadowRect.y,
                                                            outerShadowRect.width,
                                                            outerShadowRect.height,
                                                            borderArcWidth, borderArcHeight);

        // Apply the clipping to avoid overlapping the shadow and the box
        Area shadowArea = new Area(outerShadowBox);
        Area baseArea = new Area(baseRect);


        if ( !this.shadowInset )
            shadowArea.intersect(baseArea);
        else
            shadowArea.subtract(baseArea);


        savedG2d.setClip(shadowArea);

        if ( shadowSpreadRadius != 0  ) {
            // Draw the corner shadows
            _renderCornerShadow(Corner.TOP_LEFT, shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset);
            _renderCornerShadow(Corner.TOP_RIGHT, shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset);
            _renderCornerShadow(Corner.BOTTOM_LEFT, shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset);
            _renderCornerShadow(Corner.BOTTOM_RIGHT, shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset);

            // Draw the edge shadows
            _renderEdgeShadow(Side.TOP, shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset);
            _renderEdgeShadow(Side.RIGHT, shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset);
            _renderEdgeShadow(Side.BOTTOM, shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset);
            _renderEdgeShadow(Side.LEFT, shadowArea, innerShadowRect, outerShadowRect, gradientStartOffset);
        }

        // If the base rectangle and the outer shadow box are not equal, then we need to fill the area of the base rectangle that is not covered by the outer shadow box!
        {
            Graphics2D g2d2 = (Graphics2D) g2d.create();
            g2d2.setColor(shadowColor);
            Area baseRectArea = new Area(baseRect);
            if ( !this.shadowInset ) {
                baseRectArea.subtract(new Area(outerShadowBox));
                g2d2.fill(baseRectArea);
            }else {
                Area innerShadowArea = new Area(innerShadowRect);
                innerShadowArea.subtract(baseRectArea);
                g2d2.fill(innerShadowArea);
            }
            g2d2.dispose();
        }

        // Restore the original graphics state
        savedG2d.dispose();
    }

    private void _renderCornerShadow(
        Corner corner,
        Area boxArea,
        Rectangle innerShadowRect,
        Rectangle outerShadowRect,
        int gradientStartOffset
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
        if (shadowInset) {
            innerColor = shadowColor;
            outerColor = shadowBackgroundColor;
        } else {
            innerColor = shadowBackgroundColor;
            outerColor = shadowColor;
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

    private void _renderEdgeShadow(
            Side side,
            Area boxArea,
            Rectangle innerShadowRect,
            Rectangle outerShadowRect,
            int gradientStartOffset
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
        if (shadowInset) {
            innerColor = shadowColor;
            outerColor = shadowBackgroundColor;
        } else {
            innerColor = shadowBackgroundColor;
            outerColor = shadowColor;
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

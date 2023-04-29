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
    private float shadowBlurRadius = 5;
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

    public RenderDelegate<C> shadowBlurRadius(float radius ) {
        this.shadowBlurRadius = radius / 10;
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
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(borderThickness));
        g2d.drawRoundRect(
                paddingLeft, paddingTop,
                comp.getWidth() - paddingLeft - paddingRight,
                comp.getHeight() - paddingTop - paddingBottom,
                borderArcWidth, borderArcHeight
            );
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

    public void renderBoxShadow3() {
        // Save the current graphics state
        Graphics2D savedG2d = (Graphics2D) g2d.create();

        // Calculate the shadow box bounds based on the padding and border thickness
        int x = paddingLeft + borderThickness + horizontalShadowOffset;
        int y = paddingTop + borderThickness + verticalShadowOffset;
        int w = comp.getWidth() - paddingLeft - paddingRight - borderThickness * 2;
        int h = comp.getHeight() - paddingTop - paddingBottom - borderThickness * 2;

        int shadowInset = this.shadowSpreadRadius;
        int shadowOffset = 0; // TODO

        Rectangle innerShadowRect = new Rectangle(
                                        x + shadowInset,
                                        y + shadowInset,
                                        w - shadowInset * 2,
                                        h - shadowInset * 2
                                    );

        Rectangle outerShadowRect = new Rectangle(
                                        x - shadowOffset,
                                        y - shadowOffset,
                                        w + shadowOffset * 2,
                                        h + shadowOffset * 2
                                    );

        // Create the shadow shape based on the box bounds and corner arc widths/heights
        RoundRectangle2D.Float boxShape = new RoundRectangle2D.Float(x, y, w, h, borderArcWidth, borderArcHeight);

        // Apply the clipping to avoid overlapping the shadow and the box
        Area boxArea = new Area(boxShape);
        savedG2d.setClip(boxArea);

        // Draw the corner shadows
        _renderCornerShadow(Corner.TOP_LEFT, boxArea, innerShadowRect, outerShadowRect);
        _renderCornerShadow(Corner.TOP_RIGHT, boxArea, innerShadowRect, outerShadowRect);
        _renderCornerShadow(Corner.BOTTOM_LEFT, boxArea, innerShadowRect, outerShadowRect);
        _renderCornerShadow(Corner.BOTTOM_RIGHT, boxArea, innerShadowRect, outerShadowRect);

        // Draw the edge shadows
        _renderEdgeShadow(Side.TOP, boxArea, innerShadowRect, outerShadowRect);
        _renderEdgeShadow(Side.RIGHT, boxArea, innerShadowRect, outerShadowRect);
        _renderEdgeShadow(Side.BOTTOM, boxArea, innerShadowRect, outerShadowRect);
        _renderEdgeShadow(Side.LEFT, boxArea, innerShadowRect, outerShadowRect);

        // Restore the original graphics state
        savedG2d.dispose();
    }

    private void _renderCornerShadow( Corner corner, Area boxArea, Rectangle innerShadowRect, Rectangle outerShadowRect ) {
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
        RadialGradientPaint cornerPaint = new RadialGradientPaint(
                                                    cx, cy, cr,
                                                    new float[] {0f, 1f},
                                                    new Color[] {innerColor, outerColor}
                                                );

        // We need to clip the corner paint to the corner box
        Area cornerArea = new Area(cornerBox);
        cornerArea.intersect(boxArea);

        Graphics2D cornerG2d = (Graphics2D) g2d.create();
        cornerG2d.setPaint(cornerPaint);
        cornerG2d.fill(cornerArea);
        cornerG2d.dispose();
    }

    private void _renderEdgeShadow( Side side, Area boxArea, Rectangle innerShadowRect, Rectangle outerShadowRect ) {
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
        LinearGradientPaint edgePaint = new LinearGradientPaint(
                                                    gradStartX, gradStartY,
                                                    gradEndX, gradEndY,
                                                    new float[] {0f, 1f},
                                                    new Color[] {innerColor, outerColor}
                                                );

        // We need to clip the edge paint to the edge box
        Area edgeArea = new Area(edgeBox);
        edgeArea.intersect(boxArea);

        Graphics2D edgeG2d = (Graphics2D) g2d.create();
        edgeG2d.setPaint(edgePaint);
        edgeG2d.fill(edgeArea);
        edgeG2d.dispose();
    }

}
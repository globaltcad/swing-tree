package swingtree;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
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

    public RenderDelegate<C> border(int thickness ) {
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

    public void drawBorder( Color color ) {
        g2d.setColor(color);
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
        RoundRectangle2D.Float boxShape = new RoundRectangle2D.Float(
                x, y, w, h,
                borderArcWidth, borderArcHeight);

        // Apply the clipping to avoid overlapping the shadow and the box
        Area boxArea = new Area(boxShape);
        savedG2d.setClip(boxArea);

        // Draw the corner shadows
        _renderCornerShadow(Corner.TOP_LEFT, boxArea, x, y);
        _renderCornerShadow(Corner.TOP_RIGHT, boxArea, x + w, y);
        _renderCornerShadow(Corner.BOTTOM_LEFT, boxArea, x, y + h);
        _renderCornerShadow(Corner.BOTTOM_RIGHT, boxArea, x + w, y + h);

        // Draw the edge shadows
        _renderEdgeShadow(Side.TOP, boxArea, x, y, w, h);
        _renderEdgeShadow(Side.RIGHT, boxArea, x, y, w, h);
        _renderEdgeShadow(Side.BOTTOM, boxArea, x, y, w, h);
        _renderEdgeShadow(Side.LEFT, boxArea, x, y, w, h);

        // Restore the original graphics state
        savedG2d.dispose();
    }

    private void _renderCornerShadow( Corner corner, Area boxArea, int x, int y ) {
        // Draw the corner shadows
        if (borderArcWidth > 0 && borderArcHeight > 0) {
            // Create the corner paint for the shadow
            float blur = shadowBlurRadius * 10f;
            boolean isOnRightEdge = corner == Corner.TOP_RIGHT || corner == Corner.BOTTOM_RIGHT;
            boolean isOnBottomEdge = corner == Corner.BOTTOM_LEFT || corner == Corner.BOTTOM_RIGHT;
            float cx = 28f  * (shadowInset ? -1 : 1) * (isOnRightEdge  ? 1 : -1);
            float cy = 28f  * (shadowInset ? -1 : 1) * (isOnBottomEdge ? 1 : -1);
            cx += x - (borderArcWidth  / 4f)  * (shadowInset ? -1 : 1) * (isOnRightEdge  ? 1 : -1) + blur * (isOnRightEdge ? 1 : -1);
            cy += y - (borderArcHeight / 4f) * (shadowInset ? -1 : 1) * (isOnBottomEdge ? 1 : -1) + blur * (isOnBottomEdge ? 1 : -1);
            float cr = (borderArcWidth + borderArcHeight) / 2f * shadowSpreadRadius + 2f * blur;
            Color innerColor;
            Color outerColor;
            Color shadowBackgroundColor = new Color(0,0,0,0);
            if (shadowInset) {
                innerColor = shadowBackgroundColor;
                outerColor = shadowColor;
            } else {
                innerColor = shadowColor;
                outerColor = shadowBackgroundColor;
            }
            RadialGradientPaint cornerPaint = new RadialGradientPaint(
                                                cx, cy, cr,
                                                new float[] {0f, 1f},
                                                new Color[] {innerColor, outerColor});

            Graphics2D cornerG2d = (Graphics2D) g2d.create();
            cornerG2d.setPaint(cornerPaint);
            cornerG2d.fill(boxArea);
            cornerG2d.dispose();
        }
    }

    private void _renderEdgeShadow(Side side, Area boxArea, int x, int y, int w, int h) {
        float shadowBlurRadius = this.shadowBlurRadius * shadowSpreadRadius * 10f;
        if (shadowBlurRadius > 0) {
            // Create the edge paint for the shadow
            Point2D shadowStart;
            Point2D shadowEnd;
            float gradientStartHideOffset = this.shadowBlurRadius * (shadowInset ? 1 : -1);
            switch (side) {
                case TOP:
                    shadowStart = new Point2D.Float(x, y + gradientStartHideOffset);
                    shadowEnd = new Point2D.Float(x, y + shadowBlurRadius);
                    break;
                case RIGHT:
                    shadowStart = new Point2D.Float(x + w - gradientStartHideOffset, y);
                    shadowEnd = new Point2D.Float(x + w - shadowBlurRadius, y);
                    break;
                case BOTTOM:
                    shadowStart = new Point2D.Float(x, y + h - gradientStartHideOffset);
                    shadowEnd = new Point2D.Float(x, y + h - shadowBlurRadius);
                    break;
                case LEFT:
                    shadowStart = new Point2D.Float(x + gradientStartHideOffset, y);
                    shadowEnd = new Point2D.Float(x + shadowBlurRadius, y);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid side: " + side);
            }
            // We flip the colors if the shadow is inset:
            Color shadowBackgroundColor = new Color(0,0,0,0);
            Color innerColor = shadowInset ? shadowBackgroundColor : shadowColor;
            Color outerColor = shadowInset ? shadowColor : shadowBackgroundColor;
            LinearGradientPaint edgePaint = new LinearGradientPaint(shadowStart, shadowEnd,
                                                    new float[] {0f, 1f},
                                                    new Color[] {innerColor, outerColor});

            Graphics2D edgeG2d = (Graphics2D) g2d.create();
            edgeG2d.setPaint(edgePaint);
            edgeG2d.fill(boxArea);
            edgeG2d.dispose();
        }
    }

}

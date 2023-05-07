package swingtree;

import swingtree.style.Style;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;
import java.util.function.Function;

/**
 *  This is a simple wrapper as well as delegate for a Graphics2D object and
 *  a JComponent. It provides a fluent API for painting a component.
 */
public class RenderDelegate<C extends JComponent>
{
    private enum Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
    private enum Side { TOP, RIGHT, BOTTOM, LEFT }
    enum Layer { BACKGROUND, FOREGROUND }

    private final Graphics2D _g2d;
    private final C _comp;
    private final Layer _layer;


    RenderDelegate( Graphics2D g2d, C comp, Layer layer ) {
        _g2d = Objects.requireNonNull(g2d);
        _comp = Objects.requireNonNull(comp);
        _layer = Objects.requireNonNull(layer);
    }

    public Graphics2D graphics() { return _g2d; }

    public C component() { return _comp; }

    public void renderStyle() {
        this.renderStyle( s -> s );
    }

    public void renderStyle( Function<Style,Style> styler ){
        Style style = styler.apply(UI.SETTINGS().getStyleSheet().map( ss -> ss.run(_comp) ).orElse(UI.style()));

        if ( _layer == Layer.BACKGROUND ) {
            /*
                Note that in SwingTree we do not override the UI classes of Swing to apply styles.
                Instead, we use the "ComponentExtension" class to render styles on components.
                This is because we don't want to means with the current LaF of the application
                and instead simply allow users to carefully replace the LaF with a custom one.
                So when the user has set a border style, we remove the border of the component!
                And if the user has set a background color, we make sure that the component
                is not opaque, so that the background color is visible.
                ... and so on.
            */
            if ( style.border().color().isPresent() && style.border().thickness() > 0 )
                _comp.setBorder( BorderFactory.createEmptyBorder() );

            if ( style.background().color().isPresent() )
                _comp.setOpaque( false );

            if ( style.background().outerColor().isPresent() )
                _comp.setOpaque( false );

            if ( style.shadow().color().isPresent() )
                _comp.setOpaque( false );
        }

        style.background().outerColor().ifPresent(outerColor -> {
            _fillOuterBackground(style, outerColor);
        });
        style.background().color().ifPresent(color -> {
            _fillBackground(style, color);
        });
        style.shadow().color().ifPresent(color -> {
            _renderShadows(style, _comp, _g2d, color);
        });
        style.border().color().ifPresent( color -> {
            _drawBorder(style, color);
        });

        _comp.setFont( style.font().createDerivedFrom(_comp.getFont()) );
    }

    private void _drawBorder(Style style, Color color) {
        if ( style.border().thickness() > 0 ) {
            _g2d.setColor(color);
            _g2d.setStroke(new BasicStroke(style.border().thickness()));
            _g2d.drawRoundRect(
                    style.padding().left(), style.padding().top(),
                    _comp.getWidth() - style.padding().left() - style.padding().right(),
                    _comp.getHeight() - style.padding().top() - style.padding().bottom(),
                    (style.border().arcWidth()  + (style.border().thickness() == 1 ? 0 : style.border().thickness()+1)),
                    (style.border().arcHeight() + (style.border().thickness() == 1 ? 0 : style.border().thickness()+1))
                );
            _g2d.drawRoundRect(
                    style.padding().left(), style.padding().top(),
                    _comp.getWidth() - style.padding().left() - style.padding().right(),
                    _comp.getHeight() - style.padding().top() - style.padding().bottom(),
                    (style.border().arcWidth() +2),
                    (style.border().arcHeight()+2)
                );
        }
    }

    private void _fillBackground( Style style, Color color ) {
        // Check if the color is transparent
        if ( color.getAlpha() == 0 )
            return;

        _g2d.setColor(color);
        _g2d.fillRoundRect(
                style.padding().left(), style.padding().top(),
                _comp.getWidth() - style.padding().left() - style.padding().right(),
                _comp.getHeight() - style.padding().top() - style.padding().bottom(),
                style.border().arcWidth(), style.border().arcHeight()
        );
    }

    private void _fillOuterBackground( Style style, Color color ) {
        // Check if the color is transparent
        if ( color.getAlpha() == 0 )
            return;

        Rectangle2D.Float outerRect = new Rectangle2D.Float(
                0, 0,
                _comp.getWidth(),
                _comp.getHeight()
        );
        RoundRectangle2D.Float innerRect = new RoundRectangle2D.Float(
                style.padding().left(), style.padding().top(),
                _comp.getWidth() - style.padding().left() - style.padding().right(),
                _comp.getHeight() - style.padding().top() - style.padding().bottom(),
                style.border().arcWidth(), style.border().arcHeight()
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

        // Calculate the shadow box bounds based on the padding and border thickness
        int x = style.padding().left() + style.border().thickness()/2 + style.shadow().horizontalOffset();
        int y = style.padding().top() + style.border().thickness()/2 + style.shadow().verticalOffset();
        int w = comp.getWidth() - style.padding().left() - style.padding().right() - style.border().thickness();
        int h = comp.getHeight() - style.padding().top() - style.padding().bottom() - style.border().thickness();

        int blurRadius = style.shadow().blurRadius();
        int spreadRadius = !style.shadow().inset() ? style.shadow().spreadRadius() : -style.shadow().spreadRadius();

        RoundRectangle2D.Float baseRect = new RoundRectangle2D.Float(
                                                        style.padding().left() + (float) style.border().thickness() /2,
                                                        style.padding().top() + (float) style.border().thickness() /2,
                                                            w, h,
                                                            style.border().arcWidth(), style.border().arcHeight()
                                                    );

        int shadowInset  = blurRadius;
        int shadowOutset = blurRadius;

        int gradientStartOffset = ( style.border().arcWidth() + style.border().arcHeight() ) / 5;

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
        Rectangle outerShadowBox = new Rectangle(
                                        outerShadowRect.x,
                                        outerShadowRect.y,
                                        outerShadowRect.width,
                                        outerShadowRect.height
                                    );

        // Apply the clipping to avoid overlapping the shadow and the box
        Area shadowArea = new Area(outerShadowBox);
        Area baseArea = new Area(baseRect);

        if ( !style.shadow().inset() )
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
        Style style,
        Area baseArea,
        Rectangle innerShadowRect,
        Rectangle outerShadowBox,
        Graphics2D g2d
    ) {
        Graphics2D g2d2 = (Graphics2D) g2d.create();
        g2d2.setColor(style.shadow().color().orElse(Color.BLACK));
        if ( !style.shadow().inset() ) {
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
        if (style.shadow().inset()) {
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
        if (style.shadow().inset()) {
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

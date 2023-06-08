package example.styles;

import swingtree.style.StyleSheet;

import javax.swing.*;
import java.awt.*;

public class SimpleStyleSheet extends StyleSheet
{
    @Override
    protected void declaration() {
        apply(type(JComponent.class).group("soft base"), it ->
            it.style()
                .borderRadius(20)
                .backgroundColor(new Color(0.4f, 0.85f, 1))
                .pad(30)
                .margin(50)
        );
        apply(type(JComponent.class).group("frameShadow"), it ->
            it.style()
                .borderRadius(24)
                .backgroundColor(new Color(100,200,240))
                .foundationColor(Color.BLACK)
                .shadow("bright", s -> s
                    .color(new Color(0.7f, 0.95f, 1f))
                    .offset(-10)
                )
                .shadow("dark", s -> s
                    .color(new Color(0, 0.1f, 0.2f))
                    .offset(+10)
                )
                .shadowBlurRadius(9)
                .shadowSpreadRadius(-9)
                .shadowIsInset(true)
                .pad(20)
                .margin(12)
        );
        apply(type(JComponent.class).group("neumorphic").inherits("soft base"), it ->
            it.style()
                .shadow("bright", s -> s
                    .color(new Color(0.7f, 0.95f, 1f, 0.35f))
                    .offset(-10)
                )
                .shadow("dark", s -> s
                    .color(new Color(0, 0.1f, 0.2f, 0.35f))
                    .offset(+10)
                )
                .shadowBlurRadius(13)
                .shadowSpreadRadius(-2)
                .shadowIsInset(false)
                .backgroundPainter( g2d -> {
                    renderDiagonalShade(g2d, it.component(), new Insets(50,50,50,50), ShadeType.TOP_LEFT_TO_BOTTOM_RIGHT);
                    if ( it.component() instanceof AbstractButton ) {
                        AbstractButton button = (AbstractButton) it.component();
                        if ( button.getModel().isPressed() ) {
                            renderDiagonalShade(g2d, it.component(), new Insets(50,50,50,50), ShadeType.BOTTOM_LEFT_TO_TOP_RIGHT);
                        }
                        if ( button.getModel().isRollover() ) {
                            g2d.setColor(new Color(0,0,0,0.1f));
                            g2d.fillRoundRect(0,0, it.component().getWidth(), it.component().getHeight(), 20, 20);
                        }
                    }
                })
        );
        apply(type(AbstractButton.class).group("neumorphic button").inherits("soft base"), it ->
            it.style()
                .shadow("bright", s -> s
                    .color(new Color(0.7f, 0.95f, 1f, 0.35f))
                    .offset(-5)
                )
                .shadow("dark", s -> s
                    .color(new Color(0, 0.1f, 0.2f, 0.35f))
                    .offset(+5)
                )
                .shadowBlurRadius(13)
                .shadowSpreadRadius(-2)
                .shadowIsInset(false)
                .pad(30)
                .margin(50)
        );
        apply(type(JComponent.class).group("soft inwards").inherits("soft base"), it ->
            it.style()
                .shadow("bright", s -> s
                    .color(new Color(0.7f, 0.95f, 1f, 0.35f))
                    .offset(-5)
                )
                .shadow("dark", s -> s
                    .color(new Color(0, 0.1f, 0.2f, 0.35f))
                    .offset(+5)
                )
                .shadowBlurRadius(13)
                .shadowSpreadRadius(-2)
                .shadowIsInset(true)
                .pad(30)
                .margin(10)
        );
        apply(type(JComponent.class).group("soft slim").inherits("soft base"), it ->
            it.style()
                .shadow("bright", s -> s
                    .color(new Color(0.7f, 0.95f, 1f, 0.35f))
                    .offset(-3)
                )
                .shadow("dark", s -> s
                    .color(new Color(0, 0.1f, 0.2f, 0.25f))
                    .offset(+3)
                )
                .shadowBlurRadius(8)
                .shadowSpreadRadius(-5)
                .shadowIsInset(false)
                .pad(0)
                .margin(15)
        );
    }

    enum ShadeType { TOP_LEFT_TO_BOTTOM_RIGHT, BOTTOM_LEFT_TO_TOP_RIGHT }

    /**
     *  Renders a shade from the top left corner to the bottom right corner.
     *
     * @param g2d The graphics object to render to.
     * @param component The component to render the shade for.
     * @param margin The margin of the component.
     * @param type The type of shade to render.
     */
    private static void renderDiagonalShade(
        Graphics2D g2d,
        JComponent component,
        Insets margin,
        ShadeType type
    ) {
        Dimension size = component.getSize();
        size.width  -= (margin.right + margin.left);
        size.height -= (margin.bottom + margin.top);
        int width  = size.width;
        int height = size.height;
        int realX = margin.left;
        int realY = margin.top;

        int corner1X;
        int corner1Y;
        int corner2X;
        int corner2Y;
        int diagonalCorner1X;
        int diagonalCorner1Y;
        int diagonalCorner2X;
        int diagonalCorner2Y;

        if ( type == ShadeType.TOP_LEFT_TO_BOTTOM_RIGHT ) {
            corner1X = realX;
            corner1Y = realY;
            corner2X = realX + width;
            corner2Y = realY + height;
            diagonalCorner1X = realX;
            diagonalCorner1Y = realY + height;
            diagonalCorner2X = realX + width;
            diagonalCorner2Y = realY;
        } else {
            corner1X = realX + width;
            corner1Y = realY;
            corner2X = realX;
            corner2Y = realY + height;
            diagonalCorner1X = realX + width;
            diagonalCorner1Y = realY + height;
            diagonalCorner2X = realX;
            diagonalCorner2Y = realY;
        }

        int diagonalCenterX = (diagonalCorner1X + diagonalCorner2X) / 2;
        int diagonalCenterY = (diagonalCorner1Y + diagonalCorner2Y) / 2;

        double vector1X = diagonalCorner1X - diagonalCenterX;
        double vector1Y = diagonalCorner1Y - diagonalCenterY;
        double vector2X = diagonalCorner2X - diagonalCenterX;
        double vector2Y = diagonalCorner2Y - diagonalCenterY;

        double vectorLength = Math.sqrt(vector1X * vector1X + vector1Y * vector1Y);
        vector1X = (vector1X / vectorLength);
        vector1Y = (vector1Y / vectorLength);

        vectorLength = Math.sqrt(vector2X * vector2X + vector2Y * vector2Y);
        vector2X = (vector2X / vectorLength);
        vector2Y = (vector2Y / vectorLength);

        double nVector1X = -vector1Y;
        double nVector1Y = vector1X;
        double nVector2X = -vector2Y;
        double nVector2Y = vector2X;

        double distance1 = (corner1X - diagonalCenterX) * nVector1X + (corner1Y - diagonalCenterY) * nVector1Y;
        double distance2 = (corner2X - diagonalCenterX) * nVector2X + (corner2Y - diagonalCenterY) * nVector2Y;

        int gradientStartX = (int) (diagonalCenterX + nVector1X * distance1);
        int gradientStartY = (int) (diagonalCenterY + nVector1Y * distance1);
        int gradientEndX = (int) (diagonalCenterX + nVector2X * distance2);
        int gradientEndY = (int) (diagonalCenterY + nVector2Y * distance2);

        g2d.setPaint(
                new GradientPaint(
                        gradientStartX, gradientStartY, new Color(0.6f, 0.9f, 1f),
                        gradientEndX, gradientEndY, new Color(0.3f, 0.7f, 1f)
                    )
            );
        g2d.fillRect(realX, realY, width, height);
    }

}

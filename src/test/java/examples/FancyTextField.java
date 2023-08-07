package examples;

import swingtree.UI;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;

import static swingtree.UI.Cursor;
import static swingtree.UI.TextField;
import static swingtree.UI.*;

/**
 * A text field with optional icon and a plain icon based button.
 */
public final class FancyTextField extends TextField
{
    private Image seed = null;

    public FancyTextField() {
        try {
            seed = ImageIO.read(FancyTextField.class.getResourceAsStream("/img/seed.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        of(this).withLayout("fill, ins 0").withPrefWidth(220)
        .withStyle( it -> it
            .padding(0, 0, 0, 18)
            .marginRight(25)
            .paddingRight(-20)
            .image(image -> image
                .layer(Layer.BORDER)
                .image(seed)
                .placement(Placement.LEFT)
                .height(it.component().getHeight())
                .width(30)
                .padding(5)
            )
        )
        .add("right",
            button(20, 20, icon("img/trees.png"))
            .withStyle( it -> it
                .margin(0)
                .cursor(Cursor.HAND)
                .painter(Layer.BACKGROUND, g2d -> {
                    boolean isHovered = it.component().getModel().isRollover();
                    boolean isPressed = it.component().getModel().isPressed();
                    if ( isPressed ) {
                        g2d.setColor(new Color(0,100,200));
                        g2d.fillRoundRect(0, 0, it.component().getWidth(), it.component().getHeight(), 5, 5);
                    }
                    else if ( isHovered ) {
                        // When the button is hovered, we draw a radial gradient in the center
                        // of the button where the center is yellow and the outer part is transparent.
                        // We also scale the g2d so that the gradient is stretched vertically.
                        g2d.scale(1, 1.5);
                        g2d.setPaint(new RadialGradientPaint(
                            it.component().getWidth() / 2f,
                            it.component().getHeight() / 2f / 1.5f,
                            it.component().getWidth() / 4f,
                            new float[] { 0f, 1f },
                            new Color[] { new Color(0xffff00), new Color(0xffff00, true) }
                        ));
                        g2d.fillRect(0, 0, it.component().getWidth(), it.component().getHeight());
                    }
                })
            )
            .makePlain()
        );
    }

    public static void main(String[] args) {
        // First we set nimbus as the look and feel.
        // This is not necessary, but it looks better.
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ( "Nimbus".equals(info.getName()) ) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        UI.show(
            panel(FILL)
            .add(GROW_X, new FancyTextField())
        );
    }
}

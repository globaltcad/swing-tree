package examples;

import swingtree.UI;

import javax.swing.JPanel;

import static swingtree.UI.*;

/**
 * A text field with optional icon and a plain icon based button.
 * Although SwingTree encourages the use of composition over inheritance,
 * it is still easily possible to create custom components by extending
 * one of the many regular component types, like for example, a text field.
 * This example class demonstrates how the SwingTree API can be used
 * to create a heavily modified {@link javax.swing.JTextField} with
 * a custom button and an icon displayed in the text area.
 */
public final class FancyTextField extends TextField
{
    public FancyTextField() {
        of(this).withLayout("fill, ins 0").withPrefWidth(220)
        .withStyle( it -> it
            .padding(0, 0, 0, 18)
            .marginRight(25)
            .paddingRight(-20)
            .image(Layer.BORDER, image -> image
                .image("/img/seed.png")
                .placement(Placement.LEFT)
                .height(it.component().getHeight())
                .width(30)
                .padding(5)
            )
        )
        .add("right",
            button(20, 20, findIcon("img/trees.png").orElse(null))
            .withStyle( it -> it
                .margin(0)
                .cursor(Cursor.HAND)
                .painter(Layer.BACKGROUND, g2d -> {
                    boolean isHovered = it.component().getModel().isRollover();
                    boolean isPressed = it.component().getModel().isPressed();
                    if ( isPressed ) {
                        g2d.setColor(Color.GREEN);
                        g2d.fillRoundRect(0, 0, it.component().getWidth(), it.component().getHeight(), 5, 5);
                    }
                    else if ( isHovered ) {
                        // When the button is hovered, we draw a radial gradient in the center
                        // of the button where the center is yellow and the outer part is transparent.
                        // We also scale the g2d so that the gradient is stretched vertically.
                        g2d.scale(1, 1.5);
                        g2d.setPaint(new java.awt.RadialGradientPaint(
                            it.component().getWidth() / 2f,
                            it.component().getHeight() / 2f / 1.5f,
                            it.component().getWidth() / 4f,
                            new float[] { 0f, 1f },
                            new java.awt.Color[] { new java.awt.Color(0xffff00), new java.awt.Color(0xffff00, true) }
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
        UI.show( f ->
            panel(FILL)
            .add(GROW_X,
                UI.of(new FancyTextField()).withMaxWidth(80)
            )
            .get(JPanel.class)
        );
    }
}

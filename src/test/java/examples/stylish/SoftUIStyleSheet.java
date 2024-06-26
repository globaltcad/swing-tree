package examples.stylish;

import swingtree.UI;
import swingtree.style.StyleSheet;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 *  This {@link StyleSheet} subtype alongside the {@link SoftUIView}
 *  demonstrates how to build a so called <i>soft user interface</i>,
 *  a style trend also known as <i>Neumorphism</i>.<br>
 *
 *  This is also a great demonstration of how
 *  advanced styling looks like in SwingTree.
 */
public class SoftUIStyleSheet extends StyleSheet
{
    @Override
    protected void configure() {
        add(group(Soft.BASE), it -> it
            .borderRadius(20)
            .backgroundColor(new Color(0.4f, 0.85f, 1))
            .padding(12)
            .margin(16)
        );
        add(group(Soft.FRAME_SHADOW), it -> it
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
            .padding(20)
            .margin(12)
        );
        add(group(Soft.BANNER).inherits(Soft.BASE), it -> it
            .shadow("bright", s -> s
                .color(new Color(1f, 1f, 1f, 0.25f))
                .offset(-10)
            )
            .shadow("dark", s -> s
                .color(new Color(0, 0.1f, 0.2f, 0.25f))
                .offset(+6)
            )
            .shadowBlurRadius(13)
            .shadowSpreadRadius(-7)
            .shadowIsInset(false)
            .gradient("default", grad -> grad
                .span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                .type(UI.GradientType.LINEAR)
                .colors(gradientColorsFor(it.component()))
            )
        );
        add(type(AbstractButton.class).group(Soft.BUTTON).inherits(Soft.BASE), it -> it
            .borderRadius(12)
            .padding(6)
            .margin(8)
            .shadow("bright", s -> s
                .color(new Color(0.7f, 0.95f, 1f, 0.45f))
                .offset(-6)
            )
            .shadow("dark", s -> s
                .color(new Color(0, 0.1f, 0.2f, 0.25f))
                .offset(+3)
            )
            .shadowBlurRadius(6)
            .shadowSpreadRadius(-2)
            .shadowIsInset(false)
            .gradient("default", grad -> grad
                .span(UI.Span.TOP_LEFT_TO_BOTTOM_RIGHT)
                .colors(gradientColorsFor(it.component()))
            )
            .cursor(UI.Cursor.HAND)
        );
        add(group(Soft.SINK).inherits(Soft.BASE), it -> it
            .foundationColor(new Color(0.4f, 0.85f, 1))
            .shadow("bright", s -> s
                .color(new Color(0.7f, 0.95f, 1f, 0.35f))
                .offset(-11)
            )
            .shadow("dark", s -> s
                .color(new Color(0, 0.1f, 0.2f, 0.15f))
                .offset(+4)
            )
            .shadowBlurRadius(13)
            .shadowSpreadRadius(-2)
            .shadowIsInset(true)
        );
        add(group(Soft.SINK_ROOMY).inherits(Soft.SINK), it -> it
            .padding(30)
            .margin(10)
        );
        add(group(Soft.SINK_SLIM).inherits(Soft.SINK), it -> it
            .padding(0)
            .borderRadius(60)
        );
        add(group(Soft.RAISE).inherits(Soft.BASE), it -> it
            .foundationColor(new Color(0.4f, 0.85f, 1))
            .shadow("bright", s -> s
                .color(new Color(0.7f, 0.95f, 1f, 0.35f))
                .offset(-10)
            )
            .shadow("dark", s -> s
                .color(new Color(0, 0.1f, 0.2f, 0.20f))
                .offset(+6)
            )
            .shadowBlurRadius(13)
            .shadowSpreadRadius(-7)
            .shadowIsInset(false)
            .padding(30)
            .margin(10)
        );
        add(group(Soft.SLIM).inherits(Soft.BASE), it -> it
            .shadow("bright", s -> s
                .color(new Color(0.7f, 0.95f, 1f, 0.45f))
                .offset(-4)
            )
            .shadow("dark", s -> s
                .color(new Color(0, 0.1f, 0.2f, 0.25f))
                .offset(+3)
            )
            .shadowBlurRadius(8)
            .shadowSpreadRadius(-5)
            .shadowIsInset(false)
            .padding(-1)
            .margin(6)
        );

        add(type(JComboBox.class).inherits(Soft.SLIM), it -> it
            .padding(4)
            .cursor(UI.Cursor.HAND)
        );
        add(type(JCheckBox.class).inherits(Soft.SLIM), it -> it
            .padding(6)
            .cursor(UI.Cursor.HAND)
        );
        add(type(JRadioButton.class).inherits(Soft.SLIM), it -> it
            .padding(6)
            .cursor(UI.Cursor.HAND)
        );
        add(type(JSpinner.class).inherits(Soft.SLIM), it -> it
            .padding(4)
            .cursor(UI.Cursor.HAND)
        );
        add(type(JSlider.class), it -> it
            .cursor(UI.Cursor.HAND)
        );
        add(type(JProgressBar.class).inherits(Soft.SLIM), it -> it
            .padding(0)
        );
        add(type(JTextComponent.class).inherits(Soft.SLIM), it -> it
            .foundationColor(new Color(0.4f, 0.85f, 1))
            .shadow("bright", s -> s
                .color(new Color(0.7f, 0.95f, 1f, 0.35f))
                .offset(-5)
            )
            .shadow("dark", s -> s
                .color(new Color(0, 0.1f, 0.2f, 0.20f))
                .offset(+3)
            )
            .shadowBlurRadius(6)
            .shadowSpreadRadius(-4)
            .shadowIsInset(true)
            .padding(4)
            .margin(8)
        );
        add(group(Soft.UNDERLINE).inherits(Soft.SLIM), it -> it
            .borderWidthAt(UI.Edge.BOTTOM, 3)
            .borderColor(new Color(0, 139, 255))
            .borderRadius(4)
        );
    }

    private Color[] gradientColorsFor(JComponent c) {
        if ( c instanceof AbstractButton ) {
            AbstractButton button = (AbstractButton) c;
            if ( button.getModel().isPressed() )
                return new Color[] { new Color(0.3f, 0.7f, 1f), new Color(0.6f, 0.9f, 1f) };

            if ( button.getModel().isSelected() )
                return new Color[] { new Color(0.3f, 0.7f, 1f), new Color(0.6f, 0.9f, 1f) };

            if ( button.getModel().isRollover() )
                return new Color[] {
                            new Color(0.7f, 0.95f, 1f),
                            new Color(0.35f, 0.75f, 1f)
                        };

        }
        return new Color[] {
                new Color(0.6f, 0.9f, 1f),
                new Color(0.3f, 0.7f, 1f)
            };
    }

}

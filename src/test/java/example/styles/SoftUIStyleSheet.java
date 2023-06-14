package example.styles;

import swingtree.style.Edge;
import swingtree.style.ShadingStrategy;
import swingtree.style.StyleSheet;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class SoftUIStyleSheet extends StyleSheet
{
    @Override
    protected void declaration() {
        apply(type(JComponent.class).group("soft base"), it ->
            it.style()
                .borderRadius(20)
                .backgroundColor(new Color(0.4f, 0.85f, 1))
                .pad(12)
                .margin(16)
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
        apply(type(JComponent.class).group("soft banner").inherits("soft base"), it ->
            it.style()
                .shadow("bright", s -> s
                    .color(new Color(0.7f, 0.95f, 1f, 0.35f))
                    .offset(-10)
                )
                .shadow("dark", s -> s
                    .color(new Color(0, 0.1f, 0.2f, 0.25f))
                    .offset(+9)
                )
                .shadowBlurRadius(13)
                .shadowSpreadRadius(-2)
                .shadowIsInset(false)
                .backgroundShade("default",
                    shade -> shade.shade(ShadingStrategy.TOP_LEFT_TO_BOTTOM_RIGHT).colors(
                                gradientColorsFor(it.component())
                            )
                )
        );
        apply(type(AbstractButton.class).group("soft button").inherits("soft base"), it ->
            it.style()
                .borderRadius(12)
                .pad(6)
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
                .backgroundShade("default",
                    shade -> shade.shade(ShadingStrategy.TOP_LEFT_TO_BOTTOM_RIGHT).colors(
                                gradientColorsFor(it.component())
                            )
                )
        );
        apply(type(JComponent.class).group("soft sink").inherits("soft base"), it ->
            it.style()
                .foundationColor(new Color(0.4f, 0.85f, 1))
                .shadow("bright", s -> s
                    .color(new Color(0.7f, 0.95f, 1f, 0.35f))
                    .offset(-11)
                )
                .shadow("dark", s -> s
                    .color(new Color(0, 0.1f, 0.2f, 0.20f))
                    .offset(+4)
                )
                .shadowBlurRadius(13)
                .shadowSpreadRadius(-2)
                .shadowIsInset(true)
                .pad(30)
                .margin(10)
        );
        apply(type(JComponent.class).group("soft raise").inherits("soft base"), it ->
            it.style()
                .foundationColor(new Color(0.4f, 0.85f, 1))
                .shadow("bright", s -> s
                    .color(new Color(0.7f, 0.95f, 1f, 0.35f))
                    .offset(-11)
                )
                .shadow("dark", s -> s
                    .color(new Color(0, 0.1f, 0.2f, 0.20f))
                    .offset(+6)
                )
                .shadowBlurRadius(13)
                .shadowSpreadRadius(-5)
                .shadowIsInset(false)
                .pad(30)
                .margin(10)
        );
        apply(type(JComponent.class).group("soft slim").inherits("soft base"), it ->
            it.style()
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
                .pad(4)
                .margin(6)
        );

        apply(type(JComboBox.class).inherits("soft slim"), it ->
                it.style().pad(4)
        );
        apply(type(JCheckBox.class).inherits("soft slim"), it ->
                it.style().pad(6)
        );
        apply(type(JRadioButton.class).inherits("soft slim"), it ->
                it.style().pad(6)
        );
        apply(type(JSpinner.class).inherits("soft slim"), it ->
                it.style().pad(4)
        );
        apply(type(JProgressBar.class).inherits("soft slim"), it ->
                it.style().pad(0)
        );
        apply(type(JTextComponent.class).inherits("soft slim"), it ->
                it.style()
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
                .pad(4)
                .margin(8)
        );
        apply(type(JComponent.class).group("soft underline").inherits("soft slim"), it ->
                it.style()
                    .borderWidthAt(Edge.BOTTOM, 3)
                    .borderColor(new Color(0, 139, 255))
                    .borderRadius(4)
        );
    }

    private Color[] gradientColorsFor(JComponent c) {
        if ( c instanceof AbstractButton ) {
            AbstractButton button = (AbstractButton) c;
            if ( button.getModel().isPressed() ) {
                return new Color[] { new Color(0.3f, 0.7f, 1f), new Color(0.6f, 0.9f, 1f) };
            }
            if ( button.getModel().isSelected() ) {
                return new Color[] { new Color(0.3f, 0.7f, 1f), new Color(0.6f, 0.9f, 1f) };
            }
            if ( button.getModel().isRollover() ) {
                return new Color[] {
                            new Color(0.7f, 0.95f, 1f),
                            new Color(0.35f, 0.75f, 1f)
                        };
            }
        }
        return new Color[] {
                new Color(0.6f, 0.9f, 1f),
                new Color(0.3f, 0.7f, 1f)
            };
    }

}

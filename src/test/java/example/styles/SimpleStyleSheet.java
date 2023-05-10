package example.styles;

import swingtree.style.StyleSheet;

import javax.swing.*;
import java.awt.*;

public class SimpleStyleSheet extends StyleSheet
{
    @Override
    protected void declaration() {
        apply(type(JComponent.class), it ->
            it.style()
                .backgroundColor(new Color(0, 1, 1, 0.25f))
                .shadowColor(new Color(0, 0.5f, 0, 0.75f))
                .shadowBlurRadius(3)
                .shadowSpreadRadius(2)
                .shadowIsInset(false)
                .pad(0)
        );
    }
}

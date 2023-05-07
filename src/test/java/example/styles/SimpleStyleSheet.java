package example.styles;

import swingtree.style.StyleSheet;

import javax.swing.*;
import java.awt.*;

public class SimpleStyleSheet extends StyleSheet
{
    @Override
    protected void declaration() {
        apply(type(JComponent.class), style ->
            style.background(new Color(0, 1, 1, 0.25f))
                    .shadowColor(new Color(0, 0.5f, 0, 0.75f))
                    .shadowBlurRadius(3)
                    .shadowSpreadRadius(2)
                    .shadowBackgroundColor(new Color(0, 0, 0, 0.25f))
                    .shadowInset(false)
                    .pad(0)
        );
    }
}

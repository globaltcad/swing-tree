package examples.animated.example;

import sprouts.Val;
import sprouts.Var;
import swingtree.animation.LifeTime;
import swingtree.api.AnimatedStyler;

import javax.swing.*;

import java.awt.Color;
import java.util.concurrent.TimeUnit;

import static swingtree.UI.*;

/**
 *  This class is part of the {@link AnimatedFoldingPanelsExample}.
 *  It defines a foldable panel that consists of a toggle button
 *  at the top and a content panel below it. The content panel
 *  can be folded and unfolded by clicking the toggle button,
 *  whose selection state is controlled by a boolean property.<br>
 *  <p>
 *  Whenever the toggle button is clicked, the boolean flag
 *  of the bound property is toggled, which in turn triggers
 *  a transitional style animation that changes the height
 *  of the content panel from 0 to its preferred height and vice versa.
 *  <p>
 *  For more information about transitional style animations,
 *  see {@link examples.animated.TransitionalAnimation},
 *  or read the usage documentation on the method itself
 *  ({@link swingtree.UIForAnySwing#withTransitionalStyle(Val, LifeTime, AnimatedStyler)}).
 */
public class FoldingPanel extends JPanel {

    private static final int HEIGHT = 200;

    public FoldingPanel(Var<Boolean> isOpen, String content) {
        of(this).withLayout("wrap, fillx", "", "[][grow]")
            .add(GROW_X,
                panel()
                .add(toggleButton("Show", isOpen))
            )
            .add(GROW,
                panel().withLayout("fill")
                .withBackground(Color.lightGray)
                .add(label(content))
                .withPrefHeight(HEIGHT)
                .withTransitionalStyle(isOpen, LifeTime.of(0.5, TimeUnit.SECONDS), (status, style) -> {
                    double h = style.component().getPreferredSize().height * status.fadeIn();
                    style.component().setBackground(h == 0 ? Color.orange : Color.lightGray);
                    System.out.printf("TransitionalStyle[%s]: h=%f\n", content, h);
                    return style.minHeight(h).maxHeight(h);
                })
            );
    }

}


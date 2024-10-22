package examples.animated.example;

import sprouts.Var;
import swingtree.animation.LifeTime;

import javax.swing.*;

import java.awt.Color;
import java.util.concurrent.TimeUnit;

import static swingtree.UI.*;

public class Foldable extends JPanel {

    private static final int HEIGHT = 200;

    public Foldable(Var<Boolean> isOpen, String content) {
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
                        if (h < 5) h = 0;
                        //style.component().revalidate();
                        System.out.printf("TransitionalStyle[%s]: h=%f\n", content, h);
                        return style.minHeight(h).maxHeight(h);
                    })
            );
    }

}


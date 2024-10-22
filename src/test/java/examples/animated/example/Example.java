package examples.animated.example;

import sprouts.Var;
import swingtree.UI;

import javax.swing.*;
import java.awt.*;

public class Example {

    public static void main(String[] args) {
        example1();
        example2();
    }

    private static void example1() {
        Var<Boolean> isOpen = Var.of(true);

        JPanel panel = new JPanel();

        UI.of(panel).withLayout("fill")
            .add(UI.GROW,
                UI.tabbedPane().add(
                        UI.tab("Tab 1").add(
                            UI.panel().withBackground(new Color(0xF4A0A0))
                                .add(new Foldable(isOpen, "foldable 1"))
                        )
                    )
                    .add(
                        UI.tab("Tab 2").add(
                            UI.panel().withBackground(new Color(0xBBF4A0))
                                .add(new Foldable(isOpen, "foldable 2"))
                        )
                    )
            );

        UI.show(frame -> {
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(400, 600));
            return panel;
        });
    }

    private static void example2() {
        Var<Boolean> isOpen = Var.of(true);

        JPanel panel = new JPanel();

        UI.of(panel).withLayout("fill")
            .add(UI.GROW,
                UI.panel().withBackground(new Color(0xF4A0A0))
                    .add(new Foldable(isOpen, "foldable 1"))
            )
            .add(UI.GROW,
                UI.panel().withBackground(new Color(0xBBF4A0))
                    .add(new Foldable(isOpen, "foldable 2"))
            );

        UI.show(frame -> {
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(400, 600));
            return panel;
        });
    }

}

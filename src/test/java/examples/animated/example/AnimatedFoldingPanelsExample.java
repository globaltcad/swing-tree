package examples.animated.example;

import sprouts.Var;
import swingtree.UI;

import javax.swing.*;
import java.awt.*;

/**
 *  This example demonstrates how to create expandable panels
 *  that can be folded and unfolded and whose height or "selection"
 *  state is controlled by a property.
 *  <p>
 *  The example consists of four parts:
 *  <ol>
 *      <li> A tabbed pane with two tabs, each containing a foldable panel.
 *           The two foldable panels share the same selection state, in
 *           the form of a boolean property, "isOpen".
 *           So when one is folded, the other is folded as well.
 *      </li>
 *      <li> A panel with two foldable panels placed side by side.
 *          The two foldable panels also share the same selection state.
 *          So when one is folded, the other is folded as well.
 *      </li>
 *      <li> A panel with a horizontal slider at the top and a tabbed pane below it,
 *           containing two tabs, each with a panel whose (preferred) height is controlled
 *           by the slider. This example demonstrates how to use an integer based property
 *           to model and control the height, and ultimately layout, of components.
 *      </li>
 *      <li> A panel with a horizontal slider at the top and two panels below it placed
 *           next to each other, each with a height controlled by the slider. So when you
 *           move the slider, the height of both panels changes.
 *      </li>
 *  </ol>
 *  <p>
 *  The example uses the {@link Var} property type to model the state of the foldable panels,
 *  which would typically be part of a separate view model class in a real application,
 *  or it would be a property lens focusing on a specific property of an (immutable) view model.
 */
public class AnimatedFoldingPanelsExample {

    public static void main(String[] args) {
        UI.runLater(() -> {
            example1(); // tabbed pane with two tabs, each containing a foldable panel
            example2(); // panel with two foldable panels side by side

            example3(); // panel with a horizontal slider at the top and a tabbed pane below it
            example4(); // a slider at the top and two panels below it side by side
        });
    }

    /**
     *  Created a tabbed pane with two tabs, each containing a foldable panel.
     *  The two foldable panels share the same state, so when one is folded,
     *  the other is folded as well.
     */
    private static void example1() {
        Var<Boolean> isOpen = Var.of(true);
        UI.show(frame -> {
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(400, 600));
            return
                UI.panel("fill")
                .add(UI.GROW,
                    UI.tabbedPane().add(
                        UI.tab("Tab 1").add(
                            UI.panel().withBackground(new Color(0xF4A0A0))
                            .add(new FoldingPanel(isOpen, "foldable 1"))
                        )
                    )
                    .add(
                        UI.tab("Tab 2").add(
                            UI.panel().withBackground(new Color(0xBBF4A0))
                            .add(new FoldingPanel(isOpen, "foldable 2"))
                        )
                    )
                )
                .get(JPanel.class);
        });
    }

    /**
     *  Created a panel with two foldable panels side by side.
     *  The two foldable panels share the same selection state,
     *  so when one is folded the other is folded as well.
     */
    private static void example2() {
        Var<Boolean> isOpen = Var.of(true);
        UI.show(frame -> {
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(400, 600));
            return
                UI.panel("fill")
                .add(UI.GROW,
                    UI.panel().withBackground(new Color(0xF4A0A0))
                    .add(new FoldingPanel(isOpen, "foldable 1"))
                )
                .add(UI.GROW,
                    UI.panel().withBackground(new Color(0xBBF4A0))
                    .add(new FoldingPanel(isOpen, "foldable 2"))
                )
                .get(JPanel.class);
        });
    }

    /**
     *  Created a panel with a horizontal slider at the top and
     *  a tabbed pane below it, containing two tabs, each with a
     *  panel whose (preferred) height is controlled by the slider.<br>
     *  This example demonstrates how to use a property to model
     *  and control the height, and ultimately layout, of components.
     */
    private static void example3() {
        Var<Integer> height = Var.of(100);

        UI.show(frame -> {
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(400, 600));
            return
                UI.panel("fill, wrap", "", "[][grow]")
                .add("growx", UI.slider(UI.Align.HORIZONTAL, 10, 300, height))
                .add(UI.GROW,
                    UI.tabbedPane().add(
                        UI.tab("Tab 1").add(
                            UI.panel().withBackground(new Color(0xF4A0A0))
                            .add(UI.panel().withBackground(Color.lightGray)
                                .withPrefHeight(height)
                                .withPrefWidth(100)
                            )
                        )
                    )
                    .add(
                        UI.tab("Tab 2").add(
                            UI.panel().withBackground(new Color(0xBBF4A0))
                            .add(UI.panel().withBackground(Color.lightGray)
                                .withPrefHeight(height)
                                .withPrefWidth(100)
                            )
                        )
                    )
                )
                .get(JPanel.class);
        });
    }

    /**
     *  Created a panel with a horizontal slider at the top and
     *  two panels below it placed next to each other, each with
     *  a height controlled by the slider.
     *  So when you move the slider, the height of both panels changes.
     */
    private static void example4() {
        Var<Integer> height = Var.of(100);

        JPanel panel = new JPanel();

        UI.of(panel).withLayout("fill", "", "[][grow]")
            .add("wrap, growx, span 2", UI.slider(UI.Align.HORIZONTAL, 10, 300, height))
            .add(UI.GROW,
                UI.panel().withBackground(new Color(0xF4A0A0))
                .add(UI.panel().withBackground(Color.lightGray)
                    .withPrefHeight(height)
                    .withPrefWidth(100)
                )
            )
            .add(UI.GROW,
                UI.panel().withBackground(new Color(0xBBF4A0))
                .add(UI.panel().withBackground(Color.lightGray)
                    .withPrefHeight(height)
                    .withPrefWidth(100)
                )
            );

        UI.show(frame -> {
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(400, 600));
            return panel;
        });
    }

}

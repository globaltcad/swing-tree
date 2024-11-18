package examples.animated.onshown;

import sprouts.Action;
import sprouts.Var;
import sprouts.Vars;
import swingtree.UI;

import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Dimension;

/**
 *  This example demonstrates how you can hide and show
 *  certain panels through the use of toggle buttons bound to
 *  boolean based properties.<br>
 *  This class contains two examples:<br>
 *  <ul>
 *      <li>
 *          <b>Example 1:</b><br>
 *          A tabbed pane with two tabs,
 *          each containing a panel that can be shown or hidden by a toggle button.
 *          The right side of the frame contains an event log that logs
 *          when the panels receive {@link swingtree.UIForAnySwing#onShown(Action)}
 *          and {@link swingtree.UIForAnySwing#onHidden(Action)} events.
 *      </li>
 *      <li>
 *          <b>Example 2:</b><br>
 *          Two panels next to each other, both with colored sub-panels that can be shown or hidden by a toggle button,
 *          and a third panel that logs when the colored panels are shown or hidden.
 *          The event log panel logs messages when the colored panels receive
 *          {@link swingtree.UIForAnySwing#onShown(Action)} and {@link swingtree.UIForAnySwing#onHidden(Action)} events.
 *      </li>
 *  </ul>
 */
public class AnimatedOnShownPanelsExample {

    public static void main(String[] args) {
        UI.runLater(() -> {
            example1(); // tabbed pane with two tabs, each containing panel which can be shown or hidden, + event log on the right
            example2(); // two panels next to each other, both can be shown or hidden, also an event log on the right
        });
    }

    /**
     *  Created a tabbed pane with two tabs and an event log to its right,
     *  both occupying the entire height of the frame and sharing the
     *  width equally. Each tab contains a toggle button to show or hide
     *  an associated panel below it. These panels have a unique background
     *  color so that you can see when they are shown or hidden.<br>
     *  Each panel has show and hide event listeners that log a message
     *  to the event log when they are shown or hidden.
     *  So you can see on the right side of the frame when the
     *  event listeners are triggered.
     */
    private static void example1() {
        Var<Boolean> isShown = Var.of(true);
        Vars<String> eventLog = Vars.of(String.class);
        UI.show(frame -> {
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(400, 600));
            return
                UI.panel(UI.FILL.and(UI.WRAP(2)))
                .add(UI.GROW,
                    UI.tabbedPane().add(
                        UI.tab("Tab 1").add(
                            UI.panel(UI.FILL.and(UI.WRAP(1)))
                            .add(UI.TOP, UI.toggleButton("toggle visibility", isShown))
                            .add(UI.GROW.and(UI.PUSH),
                                UI.panel().withBackground(new Color(0xF4A0A0))
                                .isVisibleIf(isShown)
                                .onShown( it -> eventLog.add("Tab 1 shown"))
                                .onHidden( it -> eventLog.add("Tab 1 hidden") )
                            )
                        )
                    )
                    .add(
                        UI.tab("Tab 2").add(
                            UI.panel(UI.FILL.and(UI.WRAP(1)))
                            .add(UI.TOP, UI.toggleButton("toggle visibility", isShown))
                            .add(UI.GROW.and(UI.PUSH),
                                UI.panel().withBackground(new Color(0xBBF4A0))
                                .isVisibleIf(isShown)
                                .onShown( it -> eventLog.add("Tab 2 shown"))
                                .onHidden( it -> eventLog.add("Tab 2 hidden"))
                            )
                        )
                    )
                )
                .add(UI.GROW,
                    UI.scrollPanels().add( eventLog, UI::label )
                )
                .get(JPanel.class);
        });
    }

    /**
     *  Created three panels next to each other which all occupy the entire
     *  height of the frame, but share the width equally.
     *  The first two panels have a toggle button above them to show or hide
     *  a colored panel below them. The third panel is an event log that logs
     *  when the colored panels are shown or hidden.
     */
    private static void example2() {
        Var<Boolean> isShown1 = Var.of(true);
        Var<Boolean> isShown2 = Var.of(true);
        Vars<String> eventLog = Vars.of(String.class);
        UI.show(frame -> {
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(600, 600));
            return
                UI.panel(UI.FILL.and(UI.WRAP(3)))
                .add(UI.GROW,
                    UI.panel(UI.FILL.and(UI.WRAP(1)))
                    .add(UI.TOP, UI.toggleButton("toggle visibility", isShown1))
                    .add(UI.GROW.and(UI.PUSH),
                        UI.panel().withBackground(new Color(0xF4A0A0))
                        .isVisibleIf(isShown1)
                        .onShown( it -> eventLog.add("Panel 1 shown"))
                        .onHidden( it -> eventLog.add("Panel 1 hidden"))
                    )
                )
                .add(UI.GROW,
                    UI.panel(UI.FILL.and(UI.WRAP(1)))
                    .add(UI.TOP, UI.toggleButton("toggle visibility", isShown2))
                    .add(UI.GROW.and(UI.PUSH),
                        UI.panel().withBackground(new Color(0xBBF4A0))
                        .isVisibleIf(isShown2)
                        .onShown( it -> eventLog.add("Panel 2 shown"))
                        .onHidden( it -> eventLog.add("Panel 2 hidden"))
                    )
                )
                .add(UI.GROW,
                    UI.scrollPanels().add( eventLog, UI::label )
                )
                .get(JPanel.class);
        });
    }

}

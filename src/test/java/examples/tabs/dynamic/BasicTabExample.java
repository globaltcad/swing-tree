package examples.tabs.dynamic;

import org.jspecify.annotations.NullMarked;
import sprouts.Vars;
import swingtree.Tab;
import swingtree.UI;
import swingtree.UIForPanel;

import javax.swing.*;

import static swingtree.UI.*;

@NullMarked
public class BasicTabExample extends JPanel {

    private final Vars<String> tabs = Vars.of("tab 0", "tab 1", "tab 2", "tab 3", "tab 4");
    private int next = 5;

    public BasicTabExample() {

        of(this).withLayout("fill, wrap, ins 0", "", "[grow][][]")
            .add(
                GROW,
                tabbedPane().add(tabs, BasicTabExample::toTab)
            )
            .add(
                GROW_X,
                panel().add(tabs, BasicTabExample::toPanel)
            )
            .add(
                GROW_X,
                panel().withLayout("")
                    .add(
                        button("Add")
                            .onClick((a) -> tabs.add("new tab " + next++))
                    )
                    .add(
                        button("Add 2")
                            .onClick((a) -> tabs.addAll("new tab " + next++, "new tab " + next++))
                    )
                    .add(
                        button("Add at 2")
                            .onClick((a) -> tabs.addAt(2, "new tab " + next++))
                    )
                    .add(
                        button("Remove at 2")
                            .onClick((a) -> tabs.removeAt(2))
                    )
                    .add(
                        button("Remove 2 at 2")
                            .onClick((a) -> tabs.removeAt(2, 2))
                    )
                    .add(
                        button("Set at 2")
                            .onClick((a) -> tabs.setAt(2, "updated tab " + next++))
                    )
                    .add(
                        button("Clear")
                            .onClick((a) -> tabs.clear())
                    )
            );
    }

    private static Tab toTab(String title) {
        return tab(title).add(label(title));
    }

    private static UIForPanel<JPanel> toPanel(String title) {
        return panel().add(label(title));
    }

    public static void main(String[] args) {
        UI.show(f -> new BasicTabExample());
    }

}

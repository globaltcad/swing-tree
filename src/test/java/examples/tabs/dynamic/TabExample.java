package examples.tabs.dynamic;

import com.formdev.flatlaf.FlatLightLaf;
import org.jspecify.annotations.NullMarked;
import sprouts.Vars;
import swingtree.Tab;
import swingtree.UI;

import javax.swing.*;

import java.util.function.Consumer;

import static swingtree.UI.*;

@NullMarked
public class TabExample extends JPanel {

    private final Vars<TabViewModel> tabs = Vars.of(TabViewModel.class);
    private int next = 5;

    public TabExample() {

        tabs.add(new TabViewModel("tab 0", "content 0", this::close));
        tabs.add(new TabViewModel("tab 1", "content 1", this::close));
        tabs.add(new TabViewModel("tab 2", "content 2", this::close));
        tabs.add(new TabViewModel("tab 3", "content 3", this::close));
        tabs.add(new TabViewModel("tab 4", "content 4", this::close));

        of(this).withLayout("fill, wrap, ins 0", "", "[grow][][]")
            .add(
                GROW,
                tabbedPane().addAll(tabs, TabViewModel::createTab)
            )
            .add(
                GROW_X,
                panel().withLayout("")
                    .add(
                        button("Add")
                            .onClick((a) -> tabs.add(newTabViewModel("new tab " + next++)))
                    )
                    .add(
                        button("Add 2")
                            .onClick((a) -> tabs.addAll(newTabViewModel("new tab " + next++), newTabViewModel("new tab " + next++)))
                    )
                    .add(
                        button("Add at 2")
                            .onClick((a) -> tabs.addAt(2, newTabViewModel("new tab " + next++)))
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
                            .onClick((a) -> tabs.setAt(2, newTabViewModel("updated tab " + next++)))
                    )
                    .add(
                        button("Clear")
                            .onClick((a) -> tabs.clear())
                    )
            );
    }

    private void close(TabViewModel tab) {
        tabs.remove(tab);
    }

    private TabViewModel newTabViewModel(String title) {
        return new TabViewModel(title, "new content", this::close);
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        UI.show(f -> new TabExample());
    }

    private static class TabViewModel {

        private final Consumer<TabViewModel> onClose;

        private final String title;
        private final String content;

        private TabViewModel(String title, String content, Consumer<TabViewModel> onClose) {
            this.onClose = onClose;
            this.title = title;
            this.content = content;
        }

        public Tab createTab() {
            return tab(title)
                .onSelection(e -> {
                    System.out.println("Selected tab: " + title);
                })
                .withHeader(
                    button("x").onClick((a) -> onClose.accept(this))
                        .withStyle(style -> style.padding(0, 5, 0, 5))
                )
                .add(label(content));
        }

    }

}

package examples.tabs.mvvm;

import sprouts.Var;
import swingtree.UI;

import java.util.Locale;

import static swingtree.UI.*;

public final class TabSelectionExample3 extends Panel {

    enum TabType {
        STATUS,
        BIO,
        DETAILS
    }

    static class TabsViewModel {
        private final Var<TabType> currentTab = Var.of(TabType.DETAILS);
        private final Var<String> forename = Var.of("");
        private final Var<String> surname = Var.of("");
        private final Var<String> nickname = Var.of("");
        private final Var<String> email = Var.of("");

        public Var<TabType> currentTab() {
            return currentTab;
        }
        public Var<String> forename() {
            return forename;
        }
        public Var<String> surname() {
            return surname;
        }
        public Var<String> nickname() {
            return nickname;
        }
        public Var<String> email() {
            return email;
        }
        public void tabViewChanged(TabType tab) {
            currentTab.set(tab);
        }
    }

    public TabSelectionExample3(TabsViewModel vm) {
        of(this).withLayout(FILL.and(INS(0)))
        .add(GROW,
            tabbedPane(Side.TOP)
            .withEmptyBorder(2, 1, 8, 1)
            .add(
                tab(TabType.STATUS.name().toLowerCase(Locale.ENGLISH))
                .isSelectedIf(TabType.STATUS, vm.currentTab())
                .add(
                    panel(FILL_X.and(WRAP(1)), "[grow]")
                    .add(SHRINK, label("Status:"))
                    .add(GROW_X,
                        textField().withPlaceholder("Write something about how you feel!")
                    )
                )
            )
            .add(
                tab(TabType.BIO.name().toLowerCase(Locale.ENGLISH))
                .isSelectedIf(TabType.BIO, vm.currentTab())
                .add(
                    panel(FILL_X.and(WRAP(1)), "[grow]")
                    .add(SHRINK, label("Your Bio:"))
                    .add(GROW_X,
                        textArea("Write something about yourself!")
                    )
                )
            )
            .add(
                tab(TabType.DETAILS.name().toLowerCase(Locale.ENGLISH))
                .isSelectedIf(TabType.DETAILS, vm.currentTab())
                .add(
                    panel(FILL_X.and(WRAP(1)), "[grow]")
                    .add(SHRINK,label("Details:"))
                    .add(GROW_X,
                        textField(vm.forename()).withPlaceholder("Forename"),
                        textField(vm.surname()).withPlaceholder("Surname"),
                        textField(vm.nickname()).withPlaceholder("Nickname"),
                        textField(vm.email()).withPlaceholder("Email")
                    )
                )
            )
            .onChange(it -> {
                int index = it.getComponent().getSelectedIndex();
                TabType tab = vm.currentTab().get();
                if (index == 0) {
                    tab = TabType.STATUS;
                } else if (index == 1) {
                    tab = TabType.BIO;
                } else if (index == 2) {
                    tab = TabType.DETAILS;
                }
                vm.tabViewChanged(tab);
            })
        );
    }

    public static void main(String[] args) {
        TabsViewModel vm = new TabsViewModel();
        UI.show( f -> new TabSelectionExample3(vm) );
    }

}


package example;


import swingtree.UI;

import javax.swing.*;

import static swingtree.UI.*;

public class ListSearchView extends JPanel
{
    public ListSearchView(ListSearchViewModel vm) {
        of(this).withLayout(FILL)
        .add(
            UI.list(vm.lastSearchTimes())
            .withRenderer( delegate -> new JLabel(delegate.entry().toString()))
        )
        .add(WRAP,
            UI.list(vm.searchTerms())
            .withRenderer( delegate -> new JLabel(delegate.entry()))
        )
        .add(WRAP,
            panel(FILL)
            .add(PUSH_X,
                UI.textField(vm.keyword()).withBorder(vm.searchBorder())
            )
            .add(
                UI.button(vm.searchButtonText()).isEnabledIf(vm.searchEnabled()).onClick(it->vm.search())
            )
        )
        .add(WRAP,
            panel(FILL)
                .add(
                UI.label("Found: ").withForeground(vm.validityColor())
            )
            .add(
                UI.label(vm.found().viewAs(String.class, Object::toString))
            )
        );
    }
}

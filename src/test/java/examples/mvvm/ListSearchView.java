package examples.mvvm;


import swingtree.threading.EventProcessor;
import swingtree.UI;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.time.LocalDateTime;

import static swingtree.UI.*;

public class ListSearchView extends Panel
{
    public ListSearchView(ListSearchViewModel vm) {
        UI.use(EventProcessor.DECOUPLED, ()->
            of(this).withLayout(FILL)
            .add(PUSH_Y.and(GROW),
                panel(FILL)
                .add(SHRINK, label("Search terms:") )
                .add(SHRINK.and(WRAP), label("Last search:"))
                .add(PUSH.and(GROW),
                    UI.list(vm.lastSearchTimes())
                    .withRenderer(renderListItem(LocalDateTime.class).asText(cell -> cell.value().get().toString() ))
                    .withBorder(vm.listBorder())
                )
                .add(PUSH.and(GROW).and(WRAP),
                    UI.list(vm.searchTerms())
                    .withRenderer(renderListItem(String.class).asText(cell -> cell.value().get() ))
                )
                .add(SHRINK.and(WRAP), label("Search for:"))
                .add(PUSH.and(GROW).and(SPAN).and(WRAP),
                    listOf(vm.getRandomColors())
                    .withRenderer( it -> new Component() {
                        @Override
                        public void paint(Graphics g) {
                            g.setColor(it.entry().orElse(Color.BLACK));
                            g.fillRect(0, 0, getWidth(), getHeight());
                        }
                    } )
                )
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
            .add(
                panel(FILL)
                .add(
                    UI.label("Found: ").withForeground(vm.validityColor())
                )
                .add(
                    UI.label(vm.found().viewAs(String.class, Object::toString))
                )
                .add(
                    button().withText("Clear").onClick(it->vm.clearAll())
                )
            )
        );
    }

    // Here you can test the UI:
    public static void main(String... args) {
        UI.show(new ListSearchView(new ListSearchViewModel()));
        UI.joinDecoupledEventProcessor();
    }
}

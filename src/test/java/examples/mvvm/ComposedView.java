package examples.mvvm;

import swingtree.UI;
import swingtree.threading.EventProcessor;

import static swingtree.UI.*;

public class ComposedView extends Panel
{
    ComposedView(ComposedViewModel vm) {
        of(this).withLayout("fill, wrap 1")
        .add("center, north",
            panel().add(
                html("<h1>Composed View</h1>")
            )
            .add(toggleButton("Maybe?").onClick( it -> vm.toggleMaybeIExist() ))
            .add(vm.maybeIExist(),
                subViewModel ->
                    panel("fill").add(
                        label(subViewModel.text())
                    )
            )
        )
        .add("grow, push, wrap 3",
            panel("fill")
            .add("grow, push, wrap",
                scrollPanels()
                .addAll(vm.entries(), entry ->
                        panel("fill").add(
                            label(entry)
                        )
                )
            )
        );
    }

    public static void main(String[] args)
    {
        UI.showUsing(EventProcessor.DECOUPLED, frame -> new ComposedView(new ComposedViewModel()));
        EventProcessor.DECOUPLED.join();
    }
}

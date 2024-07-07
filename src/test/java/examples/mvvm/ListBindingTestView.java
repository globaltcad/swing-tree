package examples.mvvm;

import swingtree.UI;
import swingtree.threading.EventProcessor;

import static swingtree.UI.*;

/**
 *  This example / test view demonstrates how the {@link javax.swing.JList}
 *  component type can be bound to a view model.
 *  It shows a list of cells showing a colored square and respective name
 *  based on a property list of strings contained inside the view model.
 *  When the list of strings changes, the list of colors
 *  is updated accordingly.
 *  <p>
 *  On the right side, the view has 2 text fields which allow you to
 *  specify a modification range for the list of strings.
 *  <p>
 *  Underneath you can press either "Add" or "Remove" to modify the list
 *  of strings. The "Set Random" button will set a random list of strings
 *  and colors in the specified range.
 */
public class ListBindingTestView extends Panel
{
    public ListBindingTestView(ListBindingTestViewModel vm) {
        of(this).withLayout(FILL)
        .add(PUSH_Y.and(GROW),
            panel(FILL)
            .add(PUSH.and(GROW),
                listOf(vm.colorNames())
                .withCells(it -> it
                    .when(String.class)
                    .asText( cell -> cell.item().get() )
                )
            )
            .add(
                panel(FILL)
                .add(GROW_X, numericTextField(vm.rangeMin()))
                .add(label(" to "))
                .add(WRAP.and(GROW_X), numericTextField(vm.rangeMax()))
                .add(
                    button("Add").onClick(it->vm.add())
                )
                .add(
                    button("Remove").onClick(it->vm.remove())
                )
                .add(
                    button("Set Random").onClick(it->vm.setRandom())
                )
            )
        );
    }

    public static void main(String[] args)
    {
        UI.showUsing(EventProcessor.DECOUPLED, frame -> new ListBindingTestView(new ListBindingTestViewModel()));
        EventProcessor.DECOUPLED.join();
    }
}

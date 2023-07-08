package swingtree.mvvm

import examples.mvvm.ScrollPanelsViewModel
import spock.lang.Specification
import swingtree.SwingTreeContext
import swingtree.threading.EventProcessor
import swingtree.UI

class Scroll_Panels_Spec extends Specification
{
    def setupSpec() {
        SwingTreeContext.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def 'We can instantiate the scroll panels view model and build a view for it.'()
    {
        reportInfo """
            Note that we use a pre-made example view model here.
            Feel free to look at the source code of the view model
            to see what it is doing.
        """
        given : 'We instantiate the view model.'
            var vm = new ScrollPanelsViewModel()
        when : 'We create a view for our view model...'
            var ui =
                UI.panel("fill, wrap 1")
                .add( UI.label("Something to scroll:") )
                .add( UI.separator() )
                .add(
                    UI.scrollPanels().add(vm.entries(), evm ->
                        UI.panel("fill")
                        .add("pushx", UI.label(evm.text()))
                        .add(UI.label(evm.position().viewAs(String.class, s -> "Position: " + s)))
                        .add(UI.label(evm.position().viewAs(String.class, s -> "Selected: " + s)))
                        .add(UI.button("Delete me!").onClick(it -> {
                            System.out.println("Deleting " + evm.text().get());
                            int i = evm.entries().indexOf(evm);
                            evm.entries().removeAt(i);
                            if ( i != evm.position().get() )
                                throw new IllegalStateException("Index mismatch: " + i + " != " + evm.position().get());
                        }))
                        .add(UI.button("Duplicate").onClick( it -> {
                            int i = evm.entries().indexOf(evm);
                            evm.entries().addAt(i, evm.createNew(evm.text().get() + " (copy)"));
                        }))
                        .add(UI.button("up").onClick( it -> {
                            int i = evm.entries().indexOf(evm);
                            if ( i > 0 ) {
                                evm.entries().removeAt(i);
                                evm.entries().addAt(i - 1, evm);
                            }
                        }))
                        .add(UI.button("down").onClick( it -> {
                            int i = evm.entries().indexOf(evm);
                            if ( i < evm.entries().size() - 1 ) {
                                evm.entries().removeAt(i);
                                evm.entries().addAt(i + 1, evm);
                            }
                        }))
                        .add(UI.button("replace").onClick( it -> {
                            int i = evm.entries().indexOf(evm);
                            evm.entries().setAt(i, evm.createNew("Replaced!"));
                        }))
                    )
                )
                .add( UI.separator() )
        then : 'The view was successfully created.'
            ui != null
    }


}
